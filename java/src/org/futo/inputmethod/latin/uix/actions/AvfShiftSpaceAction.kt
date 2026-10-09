package org.futo.inputmethod.latin.uix.actions

import android.os.SystemClock
import android.text.InputType
import android.view.InputDevice
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.Toast
import org.futo.inputmethod.latin.R
import org.futo.inputmethod.latin.uix.Action

const val AVF_TERMINAL_PACKAGE = "com.android.virtualization.terminal"

// Linux input-event-codes.h
private const val SCAN_CODE_LEFT_SHIFT = 42
private const val SCAN_CODE_SPACE = 57

// The Android Terminal app shows the Linux VM display with inputType TYPE_NULL. The native
// terminal of the same app uses a text inputType, and must not receive this shortcut.
private fun isAvfDisplay(editorInfo: EditorInfo): Boolean =
    editorInfo.packageName == AVF_TERMINAL_PACKAGE && editorInfo.inputType == InputType.TYPE_NULL

/**
 * Sends Shift+Space to the AVF Linux VM display, which toggles the guest input method (for
 * example fcitx5 Hangul). The display forwards key events to the guest by their Linux scan code
 * and ACTION_DOWN/ACTION_UP, and ignores the key code and the meta state, so this sends real
 * Shift and Space transitions with scan codes. The normal IME key path sends scan code 0.
 *
 * Returns false without sending anything if the target is not the AVF display. Keys that were
 * pressed are always released, even if a send fails.
 */
fun sendAvfShiftSpaceKeys(
    connectionOverridden: Boolean,
    editorInfo: EditorInfo?,
    inputConnection: InputConnection?
): Boolean {
    if(connectionOverridden || editorInfo == null || !isAvfDisplay(editorInfo)) return false
    val ic = inputConnection ?: return false

    val shiftMeta = KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON
    val flags = KeyEvent.FLAG_SOFT_KEYBOARD or KeyEvent.FLAG_KEEP_TOUCH_MODE

    fun send(action: Int, keyCode: Int, scanCode: Int, metaState: Int, downTime: Long): Boolean =
        runCatching {
            ic.sendKeyEvent(KeyEvent(
                downTime, SystemClock.uptimeMillis(), action, keyCode, 0, metaState,
                KeyCharacterMap.VIRTUAL_KEYBOARD, scanCode, flags, InputDevice.SOURCE_KEYBOARD
            ))
        }.getOrDefault(false)

    val shiftDownTime = SystemClock.uptimeMillis()
    var ok = send(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_SHIFT_LEFT, SCAN_CODE_LEFT_SHIFT, shiftMeta, shiftDownTime)
    if(ok) {
        val spaceDownTime = SystemClock.uptimeMillis()
        ok = send(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_SPACE, SCAN_CODE_SPACE, shiftMeta, spaceDownTime)
        ok = send(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_SPACE, SCAN_CODE_SPACE, shiftMeta, spaceDownTime) && ok
    }
    return send(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_SHIFT_LEFT, SCAN_CODE_LEFT_SHIFT, 0, shiftDownTime) && ok
}

val AvfShiftSpaceAction = Action(
    icon = R.drawable.code,
    name = R.string.action_avf_shift_space_title,
    simplePressImpl = { manager, _ ->
        if(!manager.sendAvfShiftSpace()) {
            Toast.makeText(
                manager.getContext(),
                R.string.action_avf_shift_space_unavailable,
                Toast.LENGTH_SHORT
            ).show()
        }
    },
    windowImpl = null,
)
