package org.futo.inputmethod.engine

import android.os.SystemClock
import android.view.InputDevice
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.inputmethod.InputConnection
import org.futo.inputmethod.latin.common.Constants

enum class ModifierState { Off, Latched, Locked }

/**
 * Modifier keys of the soft keyboard: Ctrl, Alt, Meta (Super) and Shift, left and right. The
 * values are Android key codes. Each modifier is off, latched (one-shot) or locked:
 * - A tap latches an off modifier. A second tap within the double-tap timeout locks it, a later
 *   one turns it off. A tap of a locked modifier turns it off.
 * - The next key press consumes the latched modifiers, which then go off. Locked ones stay on.
 * - A modifier that is held down while other keys are pressed (a chord) applies to all of them,
 *   and goes off when it is released, unless it was locked.
 */
class StickyModifiers(private val doubleTapTimeoutMs: Long = DEFAULT_DOUBLE_TAP_TIMEOUT_MS) {
    private val states = mutableMapOf<Int, ModifierState>()
    private val lastTapTimes = mutableMapOf<Int, Long>()
    private val held = mutableSetOf<Int>()
    private val chorded = mutableSetOf<Int>()

    /**
     * Held modifiers that are down on the AVF display, with the downTime of their down event.
     * They stay down there until their key is released, so that e.g. Alt+Tab works.
     */
    val avfDownTimes = mutableMapOf<Int, Long>()

    fun stateOf(keyCode: Int): ModifierState = states[keyCode] ?: ModifierState.Off

    fun isHeld(keyCode: Int): Boolean = keyCode in held

    /** The modifiers that apply to the next key: latched, locked or held, in press order */
    val active: List<Int>
        get() = MODIFIER_ORDER.filter { it in held || stateOf(it) != ModifierState.Off }

    val isEmpty: Boolean
        get() = active.isEmpty()

    /** The key of a modifier went down. Returns false for other keys. */
    fun press(keyCode: Int): Boolean {
        if(!isModifier(keyCode)) return false
        held.add(keyCode)
        chorded.remove(keyCode)
        return true
    }

    /**
     * The key of a modifier went up. A release that ends a chord turns the modifier off unless
     * it is locked. Otherwise a tap changes the state, and a release that is not a tap (e.g. a
     * cancelled key) changes nothing. Returns false for other keys.
     */
    fun release(keyCode: Int, timeMs: Long, tap: Boolean): Boolean {
        if(!isModifier(keyCode)) return false
        val wasHeld = held.remove(keyCode)
        if(wasHeld && chorded.remove(keyCode)) {
            if(stateOf(keyCode) != ModifierState.Locked) states.remove(keyCode)
            lastTapTimes.remove(keyCode)
        } else if(tap) {
            when(stateOf(keyCode)) {
                ModifierState.Off -> {
                    states[keyCode] = ModifierState.Latched
                    lastTapTimes[keyCode] = timeMs
                }
                ModifierState.Latched -> {
                    val lastTapTime = lastTapTimes.remove(keyCode)
                    if(lastTapTime != null && timeMs - lastTapTime <= doubleTapTimeoutMs) {
                        states[keyCode] = ModifierState.Locked
                    } else {
                        states.remove(keyCode)
                    }
                }
                ModifierState.Locked -> states.remove(keyCode)
            }
        }
        return true
    }

    /**
     * A key other than a modifier is pressed. Returns the modifiers that apply to it in press
     * order. The latched modifiers go off, and the held ones become part of a chord.
     */
    fun consume(): List<Int> {
        val result = active
        chorded.addAll(held)
        states.entries.removeAll { it.value == ModifierState.Latched }
        lastTapTimes.clear()
        return result
    }

    /** Turns all modifiers off, e.g. when the input starts in another editor */
    fun clear() {
        states.clear()
        lastTapTimes.clear()
        held.clear()
        chorded.clear()
        avfDownTimes.clear()
    }

    companion object {
        const val DEFAULT_DOUBLE_TAP_TIMEOUT_MS = 300L

        /** Modifier key codes, in the order in which they are pressed */
        val MODIFIER_ORDER = listOf(
            KeyEvent.KEYCODE_CTRL_LEFT, KeyEvent.KEYCODE_CTRL_RIGHT,
            KeyEvent.KEYCODE_ALT_LEFT, KeyEvent.KEYCODE_ALT_RIGHT,
            KeyEvent.KEYCODE_META_LEFT, KeyEvent.KEYCODE_META_RIGHT,
            KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_SHIFT_RIGHT,
        )

        @JvmStatic
        fun isModifier(keyCode: Int): Boolean = keyCode in MODIFIER_ORDER

        /** Returns whether a layout key code is a modifier key event, e.g. !code/keyevent_ctrl_left */
        @JvmStatic
        fun isModifierCode(code: Int): Boolean =
            Constants.isKeyEventCode(code) && isModifier(code - Constants.CODE_KEYEVENT_0)
    }
}

/** The meta state while the given modifier keys are held */
fun modifierMetaState(modifiers: Collection<Int>): Int = modifiers.fold(0) { meta, keyCode ->
    meta or when(keyCode) {
        KeyEvent.KEYCODE_CTRL_LEFT -> KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON
        KeyEvent.KEYCODE_CTRL_RIGHT -> KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_RIGHT_ON
        KeyEvent.KEYCODE_SHIFT_LEFT -> KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON
        KeyEvent.KEYCODE_SHIFT_RIGHT -> KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_RIGHT_ON
        KeyEvent.KEYCODE_ALT_LEFT -> KeyEvent.META_ALT_ON or KeyEvent.META_ALT_LEFT_ON
        KeyEvent.KEYCODE_ALT_RIGHT -> KeyEvent.META_ALT_ON or KeyEvent.META_ALT_RIGHT_ON
        KeyEvent.KEYCODE_META_LEFT -> KeyEvent.META_META_ON or KeyEvent.META_META_LEFT_ON
        KeyEvent.KEYCODE_META_RIGHT -> KeyEvent.META_META_ON or KeyEvent.META_META_RIGHT_ON
        else -> 0
    }
}

/** Adds Left Shift after the modifiers if the key needs Shift and no Shift is held yet */
fun withShift(modifiers: List<Int>, shift: Boolean): List<Int> =
    if(shift && KeyEvent.KEYCODE_SHIFT_LEFT !in modifiers && KeyEvent.KEYCODE_SHIFT_RIGHT !in modifiers) {
        modifiers + KeyEvent.KEYCODE_SHIFT_LEFT
    } else {
        modifiers
    }

private val US_LETTER_SCAN_CODES = intArrayOf(
    // a  b   c   d   e   f   g   h   i   j   k   l   m   n   o   p   q   r   s   t   u   v   w   x   y   z
    30, 48, 46, 32, 18, 33, 34, 35, 23, 36, 37, 38, 50, 49, 24, 25, 16, 19, 31, 20, 22, 47, 17, 45, 21, 44
)

// Android key code -> Linux scan code (input-event-codes.h), as in AOSP Generic.kl
private val LINUX_SCAN_CODES: Map<Int, Int> = buildMap {
    US_LETTER_SCAN_CODES.forEachIndexed { i, scanCode -> put(KeyEvent.KEYCODE_A + i, scanCode) }
    for(i in 1..9) put(KeyEvent.KEYCODE_0 + i, i + 1)
    put(KeyEvent.KEYCODE_0, 11)
    for(i in 0..9) put(KeyEvent.KEYCODE_F1 + i, 59 + i)
    put(KeyEvent.KEYCODE_F11, 87)
    put(KeyEvent.KEYCODE_F12, 88)

    put(KeyEvent.KEYCODE_ESCAPE, 1)
    put(KeyEvent.KEYCODE_MINUS, 12)
    put(KeyEvent.KEYCODE_EQUALS, 13)
    put(KeyEvent.KEYCODE_DEL, 14)
    put(KeyEvent.KEYCODE_TAB, 15)
    put(KeyEvent.KEYCODE_LEFT_BRACKET, 26)
    put(KeyEvent.KEYCODE_RIGHT_BRACKET, 27)
    put(KeyEvent.KEYCODE_ENTER, 28)
    put(KeyEvent.KEYCODE_CTRL_LEFT, 29)
    put(KeyEvent.KEYCODE_SEMICOLON, 39)
    put(KeyEvent.KEYCODE_APOSTROPHE, 40)
    put(KeyEvent.KEYCODE_GRAVE, 41)
    put(KeyEvent.KEYCODE_SHIFT_LEFT, 42)
    put(KeyEvent.KEYCODE_BACKSLASH, 43)
    put(KeyEvent.KEYCODE_COMMA, 51)
    put(KeyEvent.KEYCODE_PERIOD, 52)
    put(KeyEvent.KEYCODE_SLASH, 53)
    put(KeyEvent.KEYCODE_SHIFT_RIGHT, 54)
    put(KeyEvent.KEYCODE_ALT_LEFT, 56)
    put(KeyEvent.KEYCODE_SPACE, 57)
    put(KeyEvent.KEYCODE_CAPS_LOCK, 58)
    put(KeyEvent.KEYCODE_NUM_LOCK, 69)
    put(KeyEvent.KEYCODE_SCROLL_LOCK, 70)
    put(KeyEvent.KEYCODE_CTRL_RIGHT, 97)
    put(KeyEvent.KEYCODE_SYSRQ, 99)
    put(KeyEvent.KEYCODE_ALT_RIGHT, 100)
    put(KeyEvent.KEYCODE_MOVE_HOME, 102)
    put(KeyEvent.KEYCODE_DPAD_UP, 103)
    put(KeyEvent.KEYCODE_PAGE_UP, 104)
    put(KeyEvent.KEYCODE_DPAD_LEFT, 105)
    put(KeyEvent.KEYCODE_DPAD_RIGHT, 106)
    put(KeyEvent.KEYCODE_MOVE_END, 107)
    put(KeyEvent.KEYCODE_DPAD_DOWN, 108)
    put(KeyEvent.KEYCODE_PAGE_DOWN, 109)
    put(KeyEvent.KEYCODE_INSERT, 110)
    put(KeyEvent.KEYCODE_FORWARD_DEL, 111)
    put(KeyEvent.KEYCODE_BREAK, 119)
    put(KeyEvent.KEYCODE_META_LEFT, 125)
    put(KeyEvent.KEYCODE_META_RIGHT, 126)
    put(KeyEvent.KEYCODE_MENU, 139)
}

/** Returns the Linux scan code of an Android key code, or null if it has none */
fun linuxScanCodeForKeyCode(keyCode: Int): Int? = LINUX_SCAN_CODES[keyCode]

/** A key of the US layout: the Android key code, and whether the character needs Shift */
data class UsKey(val keyCode: Int, val shift: Boolean)

private val US_KEYS: Map<Int, UsKey> = buildMap {
    for(i in 0 until 26) {
        put('a'.code + i, UsKey(KeyEvent.KEYCODE_A + i, false))
        put('A'.code + i, UsKey(KeyEvent.KEYCODE_A + i, true))
    }
    for(i in 0..9) put('0'.code + i, UsKey(KeyEvent.KEYCODE_0 + i, false))

    fun key(char: Char, keyCode: Int, shift: Boolean) = put(char.code, UsKey(keyCode, shift))
    key(' ', KeyEvent.KEYCODE_SPACE, false)
    key('\n', KeyEvent.KEYCODE_ENTER, false)
    key('\t', KeyEvent.KEYCODE_TAB, false)

    key('`', KeyEvent.KEYCODE_GRAVE, false)
    key('-', KeyEvent.KEYCODE_MINUS, false)
    key('=', KeyEvent.KEYCODE_EQUALS, false)
    key('[', KeyEvent.KEYCODE_LEFT_BRACKET, false)
    key(']', KeyEvent.KEYCODE_RIGHT_BRACKET, false)
    key('\\', KeyEvent.KEYCODE_BACKSLASH, false)
    key(';', KeyEvent.KEYCODE_SEMICOLON, false)
    key('\'', KeyEvent.KEYCODE_APOSTROPHE, false)
    key(',', KeyEvent.KEYCODE_COMMA, false)
    key('.', KeyEvent.KEYCODE_PERIOD, false)
    key('/', KeyEvent.KEYCODE_SLASH, false)

    key('~', KeyEvent.KEYCODE_GRAVE, true)
    key('!', KeyEvent.KEYCODE_1, true)
    key('@', KeyEvent.KEYCODE_2, true)
    key('#', KeyEvent.KEYCODE_3, true)
    key('$', KeyEvent.KEYCODE_4, true)
    key('%', KeyEvent.KEYCODE_5, true)
    key('^', KeyEvent.KEYCODE_6, true)
    key('&', KeyEvent.KEYCODE_7, true)
    key('*', KeyEvent.KEYCODE_8, true)
    key('(', KeyEvent.KEYCODE_9, true)
    key(')', KeyEvent.KEYCODE_0, true)
    key('_', KeyEvent.KEYCODE_MINUS, true)
    key('+', KeyEvent.KEYCODE_EQUALS, true)
    key('{', KeyEvent.KEYCODE_LEFT_BRACKET, true)
    key('}', KeyEvent.KEYCODE_RIGHT_BRACKET, true)
    key('|', KeyEvent.KEYCODE_BACKSLASH, true)
    key(':', KeyEvent.KEYCODE_SEMICOLON, true)
    key('"', KeyEvent.KEYCODE_APOSTROPHE, true)
    key('<', KeyEvent.KEYCODE_COMMA, true)
    key('>', KeyEvent.KEYCODE_PERIOD, true)
    key('?', KeyEvent.KEYCODE_SLASH, true)
}

/** Returns the US layout key that types the code point, or null if there is none */
fun usKeyForCodePoint(codePoint: Int): UsKey? = US_KEYS[codePoint]

// Keys whose output does not change with Shift. Shift is added to them when the soft keyboard
// is manually shifted, so that e.g. Shift+Space, Shift+Tab and Shift+Arrow work.
private fun isCaselessKey(keyCode: Int): Boolean =
    keyCode == KeyEvent.KEYCODE_SPACE || keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_TAB

/**
 * Returns the key press for a soft key code, as in LatinIMELegacy.createSoftwareKeypressEvent: a
 * code point if positive, a key code otherwise. Returns null for keyboard-internal codes (shift,
 * layout switches, actions, ...). A code point without a US layout key gives KEYCODE_UNKNOWN.
 */
fun softKeyPress(code: Int, manuallyShifted: Boolean): UsKey? = when {
    code > 0 -> usKeyForCodePoint(code)?.let {
        if(isCaselessKey(it.keyCode)) it.copy(shift = manuallyShifted) else it
    } ?: UsKey(KeyEvent.KEYCODE_UNKNOWN, false)
    code == Constants.CODE_DELETE -> UsKey(KeyEvent.KEYCODE_DEL, manuallyShifted)
    code == Constants.CODE_SHIFT_ENTER -> UsKey(KeyEvent.KEYCODE_ENTER, true)
    Constants.isKeyEventCode(code) -> UsKey(code - Constants.CODE_KEYEVENT_0, manuallyShifted)
    else -> null
}

/**
 * Sends one key press as raw key events with Linux scan codes: the modifiers go down in the given
 * order, the key goes down and up, and the modifiers go up in reverse order. Each up event reuses
 * the downTime of its down event, and the meta state is that of the modifiers that are held.
 *
 * The AVF Linux VM display forwards key events to the guest by their scan code and
 * ACTION_DOWN/ACTION_UP only. If a send fails, the keys that come after it are not pressed, but
 * the keys that were pressed are always released.
 */
fun sendRawKeyPress(
    ic: InputConnection,
    keyCode: Int,
    scanCode: Int,
    modifiers: List<Int>,
    baseMetaState: Int = 0
): Boolean {
    val pressed = mutableListOf<Pair<Int, Long>>()
    fun heldMeta() = baseMetaState or modifierMetaState(pressed.map { it.first })

    var ok = true
    for(modifier in modifiers) {
        val modifierScanCode = linuxScanCodeForKeyCode(modifier) ?: continue
        val downTime = SystemClock.uptimeMillis()
        pressed.add(modifier to downTime)
        if(!sendRawKeyEvent(ic, KeyEvent.ACTION_DOWN, modifier, modifierScanCode, heldMeta(), downTime)) {
            ok = false
            break
        }
    }

    if(ok) {
        val meta = heldMeta()
        val downTime = SystemClock.uptimeMillis()
        ok = sendRawKeyEvent(ic, KeyEvent.ACTION_DOWN, keyCode, scanCode, meta, downTime)
        ok = sendRawKeyEvent(ic, KeyEvent.ACTION_UP, keyCode, scanCode, meta, downTime) && ok
    }

    while(pressed.isNotEmpty()) {
        val (modifier, downTime) = pressed.removeAt(pressed.size - 1)
        ok = sendRawKeyEvent(ic, KeyEvent.ACTION_UP, modifier, linuxScanCodeForKeyCode(modifier)!!, heldMeta(), downTime) && ok
    }
    return ok
}

private fun sendRawKeyEvent(ic: InputConnection, action: Int, keyCode: Int, scanCode: Int, metaState: Int, downTime: Long): Boolean =
    runCatching {
        ic.sendKeyEvent(KeyEvent(
            downTime, SystemClock.uptimeMillis(), action, keyCode, 0, metaState,
            KeyCharacterMap.VIRTUAL_KEYBOARD, scanCode,
            KeyEvent.FLAG_SOFT_KEYBOARD or KeyEvent.FLAG_KEEP_TOUCH_MODE, InputDevice.SOURCE_KEYBOARD
        ))
    }.getOrDefault(false)

/**
 * Sends a key with the active modifiers to the AVF display. Held modifiers go down once and stay
 * down until their key is released (see [releaseAvfModifier]), like on a hardware keyboard, so
 * that holding Alt and tapping Tab switches windows. Latched and locked modifiers wrap the key.
 * A modifier that is only tapped is never sent.
 */
private fun sendAvfKey(ic: InputConnection, keyCode: Int, shift: Boolean, modifiers: StickyModifiers) {
    val scanCode = linuxScanCodeForKeyCode(keyCode) ?: return
    val down = modifiers.avfDownTimes
    for(modifier in modifiers.active) {
        if(!modifiers.isHeld(modifier) || modifier in down) continue
        val downTime = SystemClock.uptimeMillis()
        val meta = modifierMetaState(down.keys + modifier)
        if(sendRawKeyEvent(ic, KeyEvent.ACTION_DOWN, modifier, linuxScanCodeForKeyCode(modifier)!!, meta, downTime)) {
            down[modifier] = downTime
        }
    }

    val wrapping = modifiers.active.filter { it !in down }
    val shiftIsDown = KeyEvent.KEYCODE_SHIFT_LEFT in down || KeyEvent.KEYCODE_SHIFT_RIGHT in down
    sendRawKeyPress(ic, keyCode, scanCode, withShift(wrapping, shift && !shiftIsDown), modifierMetaState(down.keys))
}

/**
 * Releases a modifier key: changes its state (see [StickyModifiers.release]), and sends its up
 * event to the AVF display if it went down there during a chord.
 */
fun releaseAvfModifier(ic: InputConnection?, modifiers: StickyModifiers, keyCode: Int, timeMs: Long, tap: Boolean): Boolean {
    if(!modifiers.release(keyCode, timeMs, tap)) return false
    val downTime = modifiers.avfDownTimes.remove(keyCode) ?: return true
    if(ic != null) {
        sendRawKeyEvent(ic, KeyEvent.ACTION_UP, keyCode, linuxScanCodeForKeyCode(keyCode)!!,
            modifierMetaState(modifiers.avfDownTimes.keys), downTime)
    }
    return true
}

/** Turns all modifiers off, and releases the ones that are down on the AVF display */
fun resetModifiers(ic: InputConnection?, modifiers: StickyModifiers) {
    if(ic != null) {
        val down = modifiers.avfDownTimes
        for(modifier in StickyModifiers.MODIFIER_ORDER.reversed()) {
            val downTime = down.remove(modifier) ?: continue
            sendRawKeyEvent(ic, KeyEvent.ACTION_UP, modifier, linuxScanCodeForKeyCode(modifier)!!,
                modifierMetaState(down.keys), downTime)
        }
    }
    modifiers.clear()
}

/**
 * Handles a soft key code while the soft keys go to the AVF Linux VM display as raw key events.
 * A modifier key changes its state. Another key is sent with the active modifiers, and consumes
 * the latched ones. A character without a US layout key is dropped, and is never committed as
 * text. timeMs is the time of the key, for the double tap of a modifier.
 *
 * Returns false for keyboard-internal codes, which need the normal handling.
 */
fun handleAvfSoftKey(ic: InputConnection, code: Int, manuallyShifted: Boolean, modifiers: StickyModifiers, timeMs: Long): Boolean {
    // A pad key sends keys only while it is dragged (see sendAvfPadSteps)
    if(Constants.isPadCode(code)) return true
    val press = softKeyPress(code, manuallyShifted) ?: return false
    if(releaseAvfModifier(ic, modifiers, press.keyCode, timeMs, tap = true)) return true

    sendAvfKey(ic, press.keyCode, press.shift, modifiers)
    modifiers.consume()
    return true
}

/**
 * Sends text to the AVF Linux VM display as raw key presses. The active modifiers apply to each
 * character, and the latched ones are consumed. Characters without a US layout key are dropped.
 */
fun handleAvfSoftText(ic: InputConnection, text: CharSequence, modifiers: StickyModifiers) {
    text.codePoints().forEach { codePoint ->
        val key = usKeyForCodePoint(codePoint) ?: return@forEach
        sendAvfKey(ic, key.keyCode, key.shift, modifiers)
    }
    modifiers.consume()
}

/**
 * Sends cursor key presses to the AVF Linux VM display, one press per step: right or down for
 * positive steps, left or up for negative steps. The active modifiers apply to every step and
 * are not consumed here: the swipe consumes the latched ones when it ends.
 */
fun sendAvfCursorKeys(ic: InputConnection, steps: Int, vertical: Boolean, manuallyShifted: Boolean, modifiers: StickyModifiers) {
    val keyCode = when {
        vertical && steps > 0 -> KeyEvent.KEYCODE_DPAD_DOWN
        vertical -> KeyEvent.KEYCODE_DPAD_UP
        steps > 0 -> KeyEvent.KEYCODE_DPAD_RIGHT
        else -> KeyEvent.KEYCODE_DPAD_LEFT
    }
    repeat(Math.abs(steps)) {
        sendAvfKey(ic, keyCode, manuallyShifted, modifiers)
    }
}

sealed interface SoftKeyRoute {
    /** The normal handling of the event */
    data object Normal : SoftKeyRoute

    /** Nothing more to do: the state of a modifier changed */
    data object Consumed : SoftKeyRoute

    /** Send a down/up key event with this meta state instead */
    data class KeyEvent(val keyCode: Int, val metaState: Int) : SoftKeyRoute
}

/**
 * Routes a soft key code when the soft keys do not go to the AVF display. Key event keys become
 * key events with the meta state of the active modifiers. A key that follows active modifiers
 * becomes a key event with their meta state if it has a key code, e.g. Ctrl+W. Without key event
 * keys and active modifiers, every code keeps the normal handling.
 */
fun routeSoftKey(code: Int, manuallyShifted: Boolean, modifiers: StickyModifiers, timeMs: Long): SoftKeyRoute {
    // A pad key sends keys only while it is dragged (see padKeyEvents)
    if(Constants.isPadCode(code)) return SoftKeyRoute.Consumed
    val press = softKeyPress(code, manuallyShifted) ?: return SoftKeyRoute.Normal
    if(modifiers.release(press.keyCode, timeMs, tap = true)) return SoftKeyRoute.Consumed

    if(!Constants.isKeyEventCode(code) && modifiers.isEmpty) return SoftKeyRoute.Normal

    val active = modifiers.consume()
    if(press.keyCode == KeyEvent.KEYCODE_UNKNOWN) return SoftKeyRoute.Normal
    return SoftKeyRoute.KeyEvent(press.keyCode, modifierMetaState(withShift(active, press.shift)))
}

data class SwipeSteps(val x: Int, val y: Int)

/**
 * Returns the cursor steps of a spacebar swipe on its dominant axis, which is x on a tie. The
 * steps on the other axis are 0. dx and dy are the distance from the swipe start.
 */
fun cursorSwipeSteps(dx: Int, dy: Int, pointerStep: Int): SwipeSteps =
    if(Math.abs(dx) >= Math.abs(dy)) SwipeSteps(dx / pointerStep, 0) else SwipeSteps(0, dy / pointerStep)

/** The key that a step of a pad sends, and how many times */
data class PadKey(val keyCode: Int, val count: Int)

/**
 * Returns the key for steps of a dragged pad key (see [cursorSwipeSteps]), or null if there are
 * no steps or the code is not a pad. The cursor pad sends arrow keys. The navigation pad sends
 * Home and End for left and right, and Page Up and Page Down for up and down.
 */
fun padKeyForSteps(code: Int, steps: SwipeSteps): PadKey? {
    val horizontal = steps.x != 0
    val count = Math.abs(if(horizontal) steps.x else steps.y)
    if(count == 0) return null
    val forward = (if(horizontal) steps.x else steps.y) > 0
    val keyCode = when(code) {
        Constants.CODE_CURSOR_PAD -> when {
            horizontal -> if(forward) KeyEvent.KEYCODE_DPAD_RIGHT else KeyEvent.KEYCODE_DPAD_LEFT
            else -> if(forward) KeyEvent.KEYCODE_DPAD_DOWN else KeyEvent.KEYCODE_DPAD_UP
        }
        Constants.CODE_NAV_PAD -> when {
            horizontal -> if(forward) KeyEvent.KEYCODE_MOVE_END else KeyEvent.KEYCODE_MOVE_HOME
            else -> if(forward) KeyEvent.KEYCODE_PAGE_DOWN else KeyEvent.KEYCODE_PAGE_UP
        }
        else -> return null
    }
    return PadKey(keyCode, count)
}

/**
 * Sends the steps of a dragged pad key to the AVF display. The active modifiers and a manual
 * shift apply to every step. The end of the gesture consumes the latched modifiers.
 */
fun sendAvfPadSteps(ic: InputConnection, code: Int, steps: SwipeSteps, manuallyShifted: Boolean, modifiers: StickyModifiers) {
    val padKey = padKeyForSteps(code, steps) ?: return
    repeat(padKey.count) {
        sendAvfKey(ic, padKey.keyCode, manuallyShifted, modifiers)
    }
}

/**
 * Returns the key events for the steps of a dragged pad key outside the AVF display, with the
 * meta state of the active modifiers and a manual shift. The end of the gesture consumes the
 * latched modifiers.
 */
fun padKeyEvents(code: Int, steps: SwipeSteps, manuallyShifted: Boolean, modifiers: StickyModifiers): List<SoftKeyRoute.KeyEvent> {
    val padKey = padKeyForSteps(code, steps) ?: return emptyList()
    val metaState = modifierMetaState(withShift(modifiers.active, manuallyShifted))
    return List(padKey.count) { SoftKeyRoute.KeyEvent(padKey.keyCode, metaState) }
}
