package org.futo.inputmethod.engine

import android.view.KeyEvent
import android.view.inputmethod.InputConnection
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.futo.inputmethod.annotations.UsedForTesting
import org.futo.inputmethod.engine.general.ActionInputTransactionIME
import org.futo.inputmethod.engine.general.GeneralIME
import org.futo.inputmethod.engine.general.ChineseIME
import org.futo.inputmethod.engine.general.JapaneseIME
import org.futo.inputmethod.latin.LatinIME
import org.futo.inputmethod.latin.Subtypes
import org.futo.inputmethod.latin.settings.Settings
import org.futo.inputmethod.latin.settings.SettingsValues
import org.futo.inputmethod.latin.uix.ActionInputTransaction
import org.futo.inputmethod.latin.uix.SettingsKey
import org.futo.inputmethod.latin.uix.actions.isAvfDisplay
import org.futo.inputmethod.latin.uix.actions.throwIfDebug
import org.futo.inputmethod.latin.uix.dataStore
import org.futo.inputmethod.latin.uix.deferSetSetting
import org.futo.inputmethod.latin.uix.getSetting
import org.futo.inputmethod.latin.uix.isDirectBootUnlocked
import org.futo.inputmethod.v2keyboard.KeyboardLayoutSetV2

enum class IMEMessage {
    ReloadResources,
    ReloadPersonalDict,
}

val GlobalIMEMessage = MutableSharedFlow<IMEMessage>(
    replay = 0,
    extraBufferCapacity = 8
)

private val ImesEverUsedWithDictionaryPersonalization = SettingsKey(
    stringSetPreferencesKey("ImesEverUsedWithDictionaryPersonalization"),
    emptySet()
)

val HardwareKeyboardHangulKeySwitchesLanguage = SettingsKey(
    booleanPreferencesKey("hardwareKeyboardHangulKeySwitchesLanguage"),
    true
)

val HardwareKeyboardShiftSpaceSwitchesLanguage = SettingsKey(
    booleanPreferencesKey("hardwareKeyboardShiftSpaceSwitchesLanguage"),
    true
)

// Off by default: Right Alt is AltGr on many layouts, and a tap of it toggles the symbols layout
val HardwareKeyboardRightAltSwitchesLanguage = SettingsKey(
    booleanPreferencesKey("hardwareKeyboardRightAltSwitchesLanguage"),
    false
)

// Makes Right Alt only a language switch key, like the 한/영 key. Needs HardwareKeyboardRightAltSwitchesLanguage
val HardwareKeyboardDisableAltGr = SettingsKey(
    booleanPreferencesKey("hardwareKeyboardDisableAltGr"),
    false
)

// In the Linux VM display of the Android Terminal app (AVF), the guest input method gets the
// language switch keys instead of this keyboard
val HardwareKeyboardAvfKeyPassthrough = SettingsKey(
    booleanPreferencesKey("hardwareKeyboardAvfKeyPassthrough"),
    false
)

// In the Linux VM display of the Android Terminal app (AVF), the soft keys go to the guest as raw
// key events with Linux scan codes
val SoftKeyboardAvfKeyPassthrough = SettingsKey(
    booleanPreferencesKey("softKeyboardAvfKeyPassthrough"),
    false
)

/**
 * Keys of a hardware keyboard that the guest input method of the AVF Linux display (e.g. fcitx5)
 * can use to switch between Korean and English: the 한/영 key, Right Alt and Shift+Space. The display
 * forwards key events to the guest by their scan codes, so the keys must reach it unchanged.
 */
fun isAvfGuestLanguageSwitchKey(keyEvent: KeyEvent): Boolean =
    keyEvent.keyCode == KeyEvent.KEYCODE_KANA || keyEvent.keyCode == KeyEvent.KEYCODE_ALT_RIGHT
            || (keyEvent.keyCode == KeyEvent.KEYCODE_SPACE && keyEvent.hasModifiers(KeyEvent.META_SHIFT_ON))

/** Returns the key event as if Right Alt were not pressed. Left Alt stays pressed. */
fun withoutRightAlt(keyEvent: KeyEvent): KeyEvent {
    var metaState = keyEvent.metaState and KeyEvent.META_ALT_RIGHT_ON.inv()
    if(metaState and KeyEvent.META_ALT_LEFT_ON == 0) {
        metaState = metaState and KeyEvent.META_ALT_ON.inv()
    }
    return KeyEvent(
        keyEvent.downTime, keyEvent.eventTime, keyEvent.action, keyEvent.keyCode,
        keyEvent.repeatCount, metaState, keyEvent.deviceId, keyEvent.scanCode,
        keyEvent.flags, keyEvent.source
    )
}

/**
 * Keys of a hardware keyboard that switch to the next language: the Korean 한/영 key (HID Lang1,
 * which Generic.kl maps to KANA) and Shift+Space. The system handles LANGUAGE_SWITCH and Ctrl+Space
 * itself, as they switch Android input method subtypes.
 */
fun isHardwareLanguageSwitchKey(
    keyEvent: KeyEvent,
    hangulKeyEnabled: Boolean,
    shiftSpaceEnabled: Boolean
): Boolean =
    (hangulKeyEnabled && keyEvent.keyCode == KeyEvent.KEYCODE_KANA)
            || (shiftSpaceEnabled && keyEvent.keyCode == KeyEvent.KEYCODE_SPACE
                && keyEvent.hasModifiers(KeyEvent.META_SHIFT_ON))

enum class IMEKind(val factory: (IMEHelper) -> IMEInterface) {
    General({ GeneralIME(it) }),
    Chinese({ ChineseIME(it) }),
    Japanese({ JapaneseIME(it) })
}

class IMEManager(
    private val service: LatinIME,
) {
    private val helper = IMEHelper(service)
    private val settings = Settings.getInstance()
    private val imes: MutableMap<IMEKind, IMEInterface> = mutableMapOf()
    private var activeIme by mutableStateOf<IMEInterface?>(null)

    @Composable fun isImeLoading(): Boolean = activeIme?.getLoadingState()?.value == true

    private fun getActiveIMEKind(settingsValues: SettingsValues): IMEKind =
        when(settingsValues.mLocale.language) {
            "zh" -> IMEKind.Chinese
            "ja" -> IMEKind.Japanese
            else -> IMEKind.General
        }

    private fun onImeChanged(old: IMEInterface?, new: IMEInterface) {
        if(old != null && inInput) {
            activeIme?.onFinishInput()
            startIme(new)
        }

        helper.setPreedit(null)

        service.latinIMELegacy.mKeyboardSwitcher?.mainKeyboardView?.setImeAllowsGestureInput(
            new.isGestureHandlingAvailable())
    }

    fun getActiveIME(
        settingsValues: SettingsValues,
    ): IMEInterface {
        currentActionInputTransactionIME?.let { return it }

        val kind = getActiveIMEKind(settingsValues)

        if(
            settingsValues.isPersonalizationEnabled
                && !service.getSetting(ImesEverUsedWithDictionaryPersonalization).contains(kind.name)
        ) {
            service.lifecycleScope.launch(Dispatchers.IO) {
                service.dataStore.edit {
                    it[ImesEverUsedWithDictionaryPersonalization.key] =
                        (it[ImesEverUsedWithDictionaryPersonalization.key] ?: emptySet()) +
                                setOf(kind.name)
                }
            }
        }

        return imes.getOrPut(kind) {
            kind.factory(helper).also {
                if(created) it.onCreate()
            }
        }.also {
            if(activeIme != it) {
                onImeChanged(activeIme, it)
            }
            activeIme = it
        }
    }

    private var created = false
    fun onCreate() {
        created = true
        imes.forEach { it.value.onCreate() }
    }

    fun onDestroy() {
        created = false
        imes.forEach { it.value.onDestroy() }
    }

    fun onDeviceUnlocked() {
        imes.forEach { it.value.onDeviceUnlocked() }
    }

    private var inInput = false
    fun onStartInput() {
        val ime = getActiveIME(settings.current)
        inInput = true
        startIme(ime)
        prevSelection = null
    }

    fun onFinishInput() {
        val ime = getActiveIME(settings.current)
        inInput = false
        ime.onFinishInput()

        currentActionInputTransactionIME?.let { endInputTransaction(it) }
    }

    /** When enabled, a tap of Right Alt no longer toggles the symbols layout (EmojiAltPhysicalKeyDetector) */
    fun isRightAltLanguageSwitchEnabled(): Boolean =
        service.getSetting(HardwareKeyboardRightAltSwitchesLanguage)

    // Right Alt is AltGr on many layouts, so only a tap of it switches the language
    private val rightAltTapDetector = HardwareKeyTapDetector(KeyEvent.KEYCODE_ALT_RIGHT)

    private fun isAltGrDisabled(): Boolean =
        isRightAltLanguageSwitchEnabled() && service.getSetting(HardwareKeyboardDisableAltGr)

    // The app gets a key without Right Alt from the input connection instead of the original key
    private fun sendToApp(keyEvent: KeyEvent): Boolean =
        service.currentInputConnection?.sendKeyEvent(keyEvent) ?: false

    /** When active, Right Alt, the 한/영 key and Shift+Space go to the AVF display unhandled */
    fun isAvfKeyPassthroughActive(): Boolean =
        service.getSetting(HardwareKeyboardAvfKeyPassthrough)
                && service.currentInputEditorInfo?.let { isAvfDisplay(it) } == true

    /**
     * Returns the base InputConnection when the soft keys go to the AVF display as raw key events,
     * or null otherwise. An editor that this keyboard overrides is never the AVF display.
     */
    fun getAvfSoftKeyConnection(): InputConnection? {
        if(!service.getSetting(SoftKeyboardAvfKeyPassthrough) || service.isInputConnectionOverridden) return null
        val editorInfo = service.getBaseInputEditorInfo() ?: return null
        if(!isAvfDisplay(editorInfo)) return null
        return service.getBaseInputConnection()
    }

    fun onHardwareKeyDown(keyEvent: KeyEvent): Boolean {
        if(!inInput) return false

        if(isAvfKeyPassthroughActive()) {
            // Returning false lets the system deliver the original event, with its scan code.
            // Right Alt combinations also stay as they are, so that the guest sees AltGr.
            if(isAvfGuestLanguageSwitchKey(keyEvent)) return false
            return handleHardwareKeyDown(keyEvent)
        }

        if(isAltGrDisabled()) {
            if(keyEvent.keyCode == KeyEvent.KEYCODE_ALT_RIGHT) {
                if(keyEvent.repeatCount == 0) Subtypes.switchToNextLanguage(service, 1)
                return true
            }
            if(keyEvent.metaState and KeyEvent.META_ALT_RIGHT_ON != 0) {
                val event = withoutRightAlt(keyEvent)
                return handleHardwareKeyDown(event) || sendToApp(event)
            }
        }

        return handleHardwareKeyDown(keyEvent)
    }

    fun onHardwareKeyUp(keyEvent: KeyEvent): Boolean {
        if(!inInput) return false

        if(isAvfKeyPassthroughActive()) {
            if(isAvfGuestLanguageSwitchKey(keyEvent)) return false
            return handleHardwareKeyUp(keyEvent)
        }

        if(isAltGrDisabled()) {
            if(keyEvent.keyCode == KeyEvent.KEYCODE_ALT_RIGHT) return true
            if(keyEvent.metaState and KeyEvent.META_ALT_RIGHT_ON != 0) {
                val event = withoutRightAlt(keyEvent)
                return handleHardwareKeyUp(event) || sendToApp(event)
            }
        }

        return handleHardwareKeyUp(keyEvent)
    }

    private var consumedLanguageSwitchKeyCode: Int? = null
    private fun handleHardwareKeyDown(keyEvent: KeyEvent): Boolean {
        rightAltTapDetector.onKeyDown(keyEvent)

        if(isHardwareLanguageSwitchKey(
                keyEvent,
                hangulKeyEnabled = service.getSetting(HardwareKeyboardHangulKeySwitchesLanguage),
                shiftSpaceEnabled = service.getSetting(HardwareKeyboardShiftSpaceSwitchesLanguage)
            )) {
            // Holding the key switches only once. With a single language the key goes to the app
            if(keyEvent.repeatCount > 0) return consumedLanguageSwitchKeyCode == keyEvent.keyCode
            val switched = Subtypes.switchToNextLanguage(service, 1) != null
            consumedLanguageSwitchKeyCode = if(switched) keyEvent.keyCode else null
            return switched
        }

        return getActiveIME(settings.current).onHardwareKeyDown(keyEvent)
    }

    private fun handleHardwareKeyUp(keyEvent: KeyEvent): Boolean {
        // Shift may already be released, so match the key code only
        if(consumedLanguageSwitchKeyCode == keyEvent.keyCode) {
            consumedLanguageSwitchKeyCode = null
            return true
        }

        if(rightAltTapDetector.onKeyUp(keyEvent) && isRightAltLanguageSwitchEnabled()) {
            Subtypes.switchToNextLanguage(service, 1)
        }

        return getActiveIME(settings.current).onHardwareKeyUp(keyEvent)
    }

    fun clearUserHistoryDictionaries() {
        if(!created) {
            throwIfDebug(IllegalStateException("Cannot clear user history dictionaries before being created."))
            return
        }

        if(!helper.context.isDirectBootUnlocked) return

        val imesToClear = service.getSetting(ImesEverUsedWithDictionaryPersonalization)
        IMEKind.entries.forEach { kind ->
            if(imesToClear.contains(kind.name)) {
                imes.getOrPut(kind) {
                    kind.factory(helper).also {
                        if (created) it.onCreate()
                    }
                }.clearUserHistoryDictionaries()
            }
        }
        service.deferSetSetting(service, ImesEverUsedWithDictionaryPersonalization.key, emptySet())
    }

    private var currentActionInputTransactionIME: ActionInputTransactionIME? = null
    fun createInputTransaction(): ActionInputTransaction {
        if(currentActionInputTransactionIME != null) {
            throwIfDebug(IllegalStateException("Cannot create an input transaction while one is already active."))
            endInputTransaction(currentActionInputTransactionIME!!)
        }
        if(!inInput) {
            throwIfDebug(IllegalStateException("Cannot create an input transaction while outside of input."))
        }

        val existingIme = getActiveIME(settings.current)
        val ime = ActionInputTransactionIME(helper)
        currentActionInputTransactionIME = ime

        var selectionUpdated = false
        prevSelection?.apply {
            if(currHash() == hash) {
                ime.onUpdateSelection(
                    -1, -1,
                    newSelStart,
                    newSelEnd,
                    composingSpanStart,
                    composingSpanEnd
                )
                selectionUpdated = true
            }
        }

        if(!selectionUpdated) {
            ime.onUpdateSelection(
                -1, -1,
                helper.getCurrentEditorInfo()?.initialSelStart ?: -1,
                helper.getCurrentEditorInfo()?.initialSelEnd ?: -1,
                -1, -1
            )
        }

        existingIme.onFinishInput()

        return ime
    }

    private fun startIme(ime: IMEInterface) {
        ime.onStartInput()

        // We need to apply previous selection in the event of a switch, because IC.requestCursorUpdates isn't always reliable
        prevSelection?.apply {
            if(currHash() == hash) {
                ime.onUpdateSelection(
                    oldSelStart,
                    oldSelEnd,
                    newSelStart,
                    newSelEnd,
                    composingSpanStart,
                    composingSpanEnd
                )
            }
        }
    }

    fun endInputTransaction(inputTransactionIME: ActionInputTransactionIME) {
        if(inputTransactionIME == currentActionInputTransactionIME) {
            currentActionInputTransactionIME = null

            inputTransactionIME.ensureFinished()

            if (inInput) {
                val existingIme = getActiveIME(settings.current)
                startIme(existingIme)
            }
        }
    }

    data class Selection(
        val oldSelStart: Int,
        val oldSelEnd: Int,
        val newSelStart: Int,
        val newSelEnd: Int,
        val composingSpanStart: Int,
        val composingSpanEnd: Int,
        val hash: Int
    )

    private var prevSelection: Selection? = null

    private fun currHash(): Int {
        return (helper.getCurrentEditorInfo()?.hashCode() ?: 0) xor (helper.getCurrentInputConnection()?.hashCode() ?: 0)
    }

    private var pendingUpdateSelection: Pair<Job, Selection>? = null
    fun ensureUpdateSelectionFinished() {
        pendingUpdateSelection?.let {
            it.first.cancel()

            val s = it.second
            getActiveIME(settings.current).onUpdateSelection(
                s.oldSelStart, s.oldSelEnd,
                s.newSelStart, s.newSelEnd,
                s.composingSpanStart, s.composingSpanEnd
            )
        }
        pendingUpdateSelection = null
    }

    fun onUpdateSelection(
        oldSelStart: Int,
        oldSelEnd: Int,
        newSelStart: Int,
        newSelEnd: Int,
        composingSpanStart: Int,
        composingSpanEnd: Int
    ) {
        val sel = Selection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, composingSpanStart, composingSpanEnd, currHash())
        pendingUpdateSelection?.first?.cancel()
        pendingUpdateSelection = service.lifecycleScope.launch {
            delay(20L)
            withContext(Dispatchers.Main) {
                prevSelection = sel
                getActiveIME(settings.current).onUpdateSelection(
                    oldSelStart, oldSelEnd,
                    newSelStart, newSelEnd,
                    composingSpanStart, composingSpanEnd
                )
                pendingUpdateSelection = null
            }
        } to sel
    }

    fun setLayout(layout: KeyboardLayoutSetV2) {
        imes.values.forEach { it.onLayoutUpdated(layout) }
    }

    @UsedForTesting
    fun recycle() {
        imes.values.forEach { it.recycle() }
    }
}