package org.futo.inputmethod.engine

import android.view.InputDevice
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.inputmethod.InputConnectionWrapper
import androidx.test.filters.SmallTest
import androidx.test.runner.AndroidJUnit4
import org.futo.inputmethod.latin.common.Constants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class SoftKeyPassthroughTests {
    // Wraps no target, so any call other than sendKeyEvent (commitText, setComposingText,
    // deleteSurroundingText, ...) throws a NullPointerException and fails the test.
    private class RecordingInputConnection(
        private val failAt: Int = -1,
        private val throwAt: Int = -1,
    ) : InputConnectionWrapper(null, false) {
        val events = mutableListOf<KeyEvent>()

        override fun sendKeyEvent(event: KeyEvent): Boolean {
            val index = events.size
            events.add(event)
            if(index == throwAt) throw IllegalStateException("send failed")
            return index != failAt
        }
    }

    private fun down(keyCode: Int, scanCode: Int) = Triple(KeyEvent.ACTION_DOWN, keyCode, scanCode)
    private fun up(keyCode: Int, scanCode: Int) = Triple(KeyEvent.ACTION_UP, keyCode, scanCode)

    private fun summary(events: List<KeyEvent>) = events.map { Triple(it.action, it.keyCode, it.scanCode) }

    private val ctrlDown = down(KeyEvent.KEYCODE_CTRL_LEFT, 29)
    private val ctrlUp = up(KeyEvent.KEYCODE_CTRL_LEFT, 29)
    private val altDown = down(KeyEvent.KEYCODE_ALT_LEFT, 56)
    private val altUp = up(KeyEvent.KEYCODE_ALT_LEFT, 56)
    private val shiftDown = down(KeyEvent.KEYCODE_SHIFT_LEFT, 42)
    private val shiftUp = up(KeyEvent.KEYCODE_SHIFT_LEFT, 42)
    private val wDown = down(KeyEvent.KEYCODE_W, 17)
    private val wUp = up(KeyEvent.KEYCODE_W, 17)

    private val ctrlMeta = KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON
    private val altMeta = KeyEvent.META_ALT_ON or KeyEvent.META_ALT_LEFT_ON
    private val shiftMeta = KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON

    private fun key(code: Int) = Constants.CODE_KEYEVENT_0 + code

    @Test
    fun testCharacterTable() {
        mapOf(
            'w' to Pair(17, false), 'W' to Pair(17, true), 'a' to Pair(30, false), 'z' to Pair(44, false),
            'q' to Pair(16, false), 'm' to Pair(50, false), '1' to Pair(2, false), '0' to Pair(11, false),
            '!' to Pair(2, true), '@' to Pair(3, true), ')' to Pair(11, true), '\n' to Pair(28, false),
            ' ' to Pair(57, false), '\t' to Pair(15, false), '-' to Pair(12, false), '_' to Pair(12, true),
            '=' to Pair(13, false), '+' to Pair(13, true), '[' to Pair(26, false), '{' to Pair(26, true),
            ']' to Pair(27, false), '}' to Pair(27, true), ';' to Pair(39, false), ':' to Pair(39, true),
            '\'' to Pair(40, false), '"' to Pair(40, true), '`' to Pair(41, false), '~' to Pair(41, true),
            '\\' to Pair(43, false), '|' to Pair(43, true), ',' to Pair(51, false), '<' to Pair(51, true),
            '.' to Pair(52, false), '>' to Pair(52, true), '/' to Pair(53, false), '?' to Pair(53, true),
        ).forEach { (char, expected) ->
            val usKey = usKeyForCodePoint(char.code)!!
            assertEquals("$char", expected, Pair(linuxScanCodeForKeyCode(usKey.keyCode), usKey.shift))
        }
    }

    @Test
    fun testAllPrintableAsciiIsMapped() {
        for(codePoint in 32..126) {
            val usKey = usKeyForCodePoint(codePoint)
            assertNotNull("${codePoint.toChar()}", usKey)
            assertNotNull("${codePoint.toChar()}", linuxScanCodeForKeyCode(usKey!!.keyCode))
        }
        // Distinct characters never share a key and a shift state
        val keys = (32..126).map { usKeyForCodePoint(it) }
        assertEquals(keys.size, keys.toSet().size)

        assertNull(usKeyForCodePoint('ㅎ'.code))
        assertNull(usKeyForCodePoint('×'.code))
        assertNull(usKeyForCodePoint('é'.code))
    }

    @Test
    fun testKeyCodeTable() {
        mapOf(
            KeyEvent.KEYCODE_ESCAPE to 1, KeyEvent.KEYCODE_F1 to 59, KeyEvent.KEYCODE_F2 to 60,
            KeyEvent.KEYCODE_F9 to 67, KeyEvent.KEYCODE_F10 to 68, KeyEvent.KEYCODE_F11 to 87,
            KeyEvent.KEYCODE_F12 to 88, KeyEvent.KEYCODE_SYSRQ to 99, KeyEvent.KEYCODE_SCROLL_LOCK to 70,
            KeyEvent.KEYCODE_BREAK to 119, KeyEvent.KEYCODE_NUM_LOCK to 69, KeyEvent.KEYCODE_INSERT to 110,
            KeyEvent.KEYCODE_FORWARD_DEL to 111, KeyEvent.KEYCODE_CAPS_LOCK to 58,
            KeyEvent.KEYCODE_CTRL_LEFT to 29, KeyEvent.KEYCODE_CTRL_RIGHT to 97,
            KeyEvent.KEYCODE_ALT_LEFT to 56, KeyEvent.KEYCODE_ALT_RIGHT to 100,
            KeyEvent.KEYCODE_META_LEFT to 125, KeyEvent.KEYCODE_META_RIGHT to 126,
            KeyEvent.KEYCODE_SHIFT_LEFT to 42, KeyEvent.KEYCODE_SHIFT_RIGHT to 54,
            KeyEvent.KEYCODE_DPAD_UP to 103, KeyEvent.KEYCODE_DPAD_DOWN to 108,
            KeyEvent.KEYCODE_DPAD_LEFT to 105, KeyEvent.KEYCODE_DPAD_RIGHT to 106,
            KeyEvent.KEYCODE_PAGE_UP to 104, KeyEvent.KEYCODE_PAGE_DOWN to 109,
            KeyEvent.KEYCODE_MOVE_HOME to 102, KeyEvent.KEYCODE_MOVE_END to 107,
            KeyEvent.KEYCODE_TAB to 15, KeyEvent.KEYCODE_ENTER to 28, KeyEvent.KEYCODE_DEL to 14,
            KeyEvent.KEYCODE_SPACE to 57,
        ).forEach { (keyCode, scanCode) ->
            assertEquals(KeyEvent.keyCodeToString(keyCode), scanCode, linuxScanCodeForKeyCode(keyCode))
        }
        assertNull(linuxScanCodeForKeyCode(KeyEvent.KEYCODE_BACK))
        assertNull(linuxScanCodeForKeyCode(KeyEvent.KEYCODE_UNKNOWN))
    }

    private val ctrl = KeyEvent.KEYCODE_CTRL_LEFT
    private val alt = KeyEvent.KEYCODE_ALT_LEFT
    private val shift = KeyEvent.KEYCODE_SHIFT_LEFT

    private fun StickyModifiers.tap(keyCode: Int, timeMs: Long = 0L) =
        assertTrue(release(keyCode, timeMs, tap = true))

    private fun send(connection: RecordingInputConnection, code: Int, modifiers: StickyModifiers,
                     shifted: Boolean = false, timeMs: Long = 0L) =
        handleAvfSoftKey(connection, code, shifted, modifiers, timeMs)

    @Test
    fun testTapLatchesAndALateSecondTapTurnsOff() {
        val modifiers = StickyModifiers(doubleTapTimeoutMs = 300L)
        assertTrue(modifiers.isEmpty)
        modifiers.tap(ctrl, 1000L)
        assertEquals(ModifierState.Latched, modifiers.stateOf(ctrl))
        assertEquals(listOf(ctrl), modifiers.active)
        modifiers.tap(ctrl, 1301L)
        assertEquals(ModifierState.Off, modifiers.stateOf(ctrl))
        assertTrue(modifiers.isEmpty)
    }

    @Test
    fun testDoubleTapLocksAndTapUnlocks() {
        val modifiers = StickyModifiers(doubleTapTimeoutMs = 300L)
        modifiers.tap(ctrl, 1000L)
        modifiers.tap(ctrl, 1300L)
        assertEquals(ModifierState.Locked, modifiers.stateOf(ctrl))
        // A locked modifier stays locked however long it waits, and a tap turns it off
        modifiers.tap(ctrl, 99999L)
        assertEquals(ModifierState.Off, modifiers.stateOf(ctrl))

        // The double-tap window starts again after the modifier turned off
        modifiers.tap(ctrl, 100000L)
        modifiers.tap(ctrl, 100100L)
        assertEquals(ModifierState.Locked, modifiers.stateOf(ctrl))
    }

    @Test
    fun testLatchedIsConsumedAndLockedStays() {
        val modifiers = StickyModifiers()
        modifiers.tap(alt, 0L)
        modifiers.tap(alt, 10L)
        modifiers.tap(ctrl, 20L)
        assertEquals(listOf(ctrl, alt), modifiers.consume())
        assertEquals(ModifierState.Off, modifiers.stateOf(ctrl))
        assertEquals(ModifierState.Locked, modifiers.stateOf(alt))
        assertEquals(listOf(alt), modifiers.consume())
        assertEquals(listOf(alt), modifiers.consume())
    }

    @Test
    fun testModifiersComeOutInAFixedOrder() {
        val modifiers = StickyModifiers()
        listOf(KeyEvent.KEYCODE_SHIFT_RIGHT, KeyEvent.KEYCODE_META_LEFT, alt, KeyEvent.KEYCODE_CTRL_RIGHT)
            .forEach { modifiers.tap(it) }
        assertEquals(
            listOf(KeyEvent.KEYCODE_CTRL_RIGHT, alt, KeyEvent.KEYCODE_META_LEFT, KeyEvent.KEYCODE_SHIFT_RIGHT),
            modifiers.consume())
        assertTrue(modifiers.isEmpty)
        assertEquals(emptyList<Int>(), modifiers.consume())
    }

    @Test
    fun testOtherKeysAreNotModifiers() {
        val modifiers = StickyModifiers()
        assertFalse(modifiers.press(KeyEvent.KEYCODE_W))
        assertFalse(modifiers.release(KeyEvent.KEYCODE_W, 0L, tap = true))
        assertTrue(modifiers.isEmpty)
        assertTrue(StickyModifiers.isModifierCode(key(ctrl)))
        assertTrue(StickyModifiers.isModifierCode(key(KeyEvent.KEYCODE_META_RIGHT)))
        assertFalse(StickyModifiers.isModifierCode(key(KeyEvent.KEYCODE_ESCAPE)))
        assertFalse(StickyModifiers.isModifierCode(ctrl))
    }

    @Test
    fun testHoldWithoutOtherKeysIsATap() {
        val modifiers = StickyModifiers()
        assertTrue(modifiers.press(ctrl))
        assertTrue(modifiers.isHeld(ctrl))
        assertEquals(listOf(ctrl), modifiers.active)
        modifiers.tap(ctrl)
        assertFalse(modifiers.isHeld(ctrl))
        assertEquals(ModifierState.Latched, modifiers.stateOf(ctrl))
        // The release after the code input changes nothing
        assertTrue(modifiers.release(ctrl, 0L, tap = false))
        assertEquals(ModifierState.Latched, modifiers.stateOf(ctrl))
    }

    @Test
    fun testCancelledHoldChangesNothing() {
        val modifiers = StickyModifiers()
        modifiers.press(ctrl)
        modifiers.release(ctrl, 0L, tap = false)
        assertTrue(modifiers.isEmpty)
    }

    @Test
    fun testChord() {
        val modifiers = StickyModifiers()
        modifiers.press(ctrl)
        // The held modifier applies to every key during the hold
        assertEquals(listOf(ctrl), modifiers.consume())
        assertEquals(listOf(ctrl), modifiers.consume())
        // and goes off when it is released, instead of latching
        modifiers.tap(ctrl)
        assertEquals(ModifierState.Off, modifiers.stateOf(ctrl))
        assertTrue(modifiers.isEmpty)

        // A latched modifier that is then held in a chord goes off too
        modifiers.tap(alt)
        modifiers.press(alt)
        modifiers.consume()
        modifiers.tap(alt)
        assertEquals(ModifierState.Off, modifiers.stateOf(alt))

        // A locked one stays locked
        modifiers.tap(alt, 0L)
        modifiers.tap(alt, 1L)
        modifiers.press(alt)
        modifiers.consume()
        modifiers.tap(alt, 5000L)
        assertEquals(ModifierState.Locked, modifiers.stateOf(alt))

        // A new hold after a chord is a tap again
        modifiers.press(ctrl)
        modifiers.tap(ctrl)
        assertEquals(ModifierState.Latched, modifiers.stateOf(ctrl))
    }

    @Test
    fun testClearResetsEverything() {
        val modifiers = StickyModifiers()
        modifiers.tap(ctrl)
        modifiers.tap(alt, 0L)
        modifiers.tap(alt, 1L)
        modifiers.press(shift)
        modifiers.avfDownTimes[shift] = 0L
        modifiers.clear()
        assertTrue(modifiers.isEmpty)
        assertFalse(modifiers.isHeld(shift))
        assertTrue(modifiers.avfDownTimes.isEmpty())
        listOf(ctrl, alt, shift).forEach { assertEquals(ModifierState.Off, modifiers.stateOf(it)) }
    }

    @Test
    fun testModifierMetaState() {
        assertEquals(0, modifierMetaState(emptyList()))
        assertEquals(ctrlMeta or altMeta, modifierMetaState(listOf(ctrl, alt)))
        assertEquals(KeyEvent.META_META_ON or KeyEvent.META_META_RIGHT_ON,
            modifierMetaState(listOf(KeyEvent.KEYCODE_META_RIGHT)))
        assertEquals(KeyEvent.META_ALT_ON or KeyEvent.META_ALT_RIGHT_ON,
            modifierMetaState(listOf(KeyEvent.KEYCODE_ALT_RIGHT)))
    }

    @Test
    fun testTappedModifierSendsNothing() {
        val modifiers = StickyModifiers()
        val connection = RecordingInputConnection()
        modifiers.press(ctrl)
        assertTrue(send(connection, key(ctrl), modifiers))
        assertTrue(releaseAvfModifier(connection, modifiers, ctrl, 0L, tap = false))
        assertTrue(connection.events.isEmpty())
        assertEquals(ModifierState.Latched, modifiers.stateOf(ctrl))
    }

    @Test
    fun testLatchedCtrlAndW() {
        val modifiers = StickyModifiers()
        val connection = RecordingInputConnection()
        assertTrue(send(connection, key(ctrl), modifiers))
        assertTrue(connection.events.isEmpty())

        assertTrue(send(connection, 'w'.code, modifiers))
        val events = connection.events
        assertEquals(listOf(ctrlDown, wDown, wUp, ctrlUp), summary(events))
        assertEquals(listOf(ctrlMeta, ctrlMeta, ctrlMeta, 0), events.map { it.metaState })
        assertEquals(events[0].downTime, events[3].downTime)
        assertEquals(events[1].downTime, events[2].downTime)
        events.forEach {
            assertEquals(KeyCharacterMap.VIRTUAL_KEYBOARD, it.deviceId)
            assertEquals(InputDevice.SOURCE_KEYBOARD, it.source)
            assertEquals(0, it.repeatCount)
            assertTrue(it.flags and KeyEvent.FLAG_SOFT_KEYBOARD != 0)
            assertTrue(it.flags and KeyEvent.FLAG_KEEP_TOUCH_MODE != 0)
        }

        // The modifier was consumed
        assertTrue(modifiers.isEmpty)
        connection.events.clear()
        send(connection, 'w'.code, modifiers)
        assertEquals(listOf(wDown, wUp), summary(connection.events))
    }

    @Test
    fun testLockedCtrlWrapsEveryKey() {
        val modifiers = StickyModifiers(doubleTapTimeoutMs = 300L)
        val connection = RecordingInputConnection()
        send(connection, key(ctrl), modifiers, timeMs = 0L)
        send(connection, key(ctrl), modifiers, timeMs = 100L)
        send(connection, 'w'.code, modifiers)
        send(connection, 'w'.code, modifiers)
        assertEquals(listOf(ctrlDown, wDown, wUp, ctrlUp, ctrlDown, wDown, wUp, ctrlUp), summary(connection.events))
        assertEquals(ModifierState.Locked, modifiers.stateOf(ctrl))

        connection.events.clear()
        send(connection, key(ctrl), modifiers, timeMs = 200L)
        send(connection, 'w'.code, modifiers)
        assertEquals(listOf(wDown, wUp), summary(connection.events))
    }

    @Test
    fun testHeldCtrlStaysDownDuringTheChord() {
        val modifiers = StickyModifiers()
        val connection = RecordingInputConnection()
        val eDown = down(KeyEvent.KEYCODE_E, 18)
        val eUp = up(KeyEvent.KEYCODE_E, 18)

        modifiers.press(ctrl)
        send(connection, 'w'.code, modifiers)
        send(connection, 'e'.code, modifiers)
        assertEquals(listOf(ctrlDown, wDown, wUp, eDown, eUp), summary(connection.events))
        assertTrue(connection.events.all { it.metaState == ctrlMeta })

        // The code input of the released modifier sends its up event, with the downTime of its down
        send(connection, key(ctrl), modifiers)
        releaseAvfModifier(connection, modifiers, ctrl, 0L, tap = false)
        assertEquals(listOf(ctrlDown, wDown, wUp, eDown, eUp, ctrlUp), summary(connection.events))
        assertEquals(0, connection.events.last().metaState)
        assertEquals(connection.events.first().downTime, connection.events.last().downTime)
        assertTrue(modifiers.isEmpty)
        assertTrue(modifiers.avfDownTimes.isEmpty())
    }

    @Test
    fun testAltTab() {
        val modifiers = StickyModifiers()
        val connection = RecordingInputConnection()
        val tabDown = down(KeyEvent.KEYCODE_TAB, 15)
        val tabUp = up(KeyEvent.KEYCODE_TAB, 15)

        modifiers.press(alt)
        send(connection, '\t'.code, modifiers)
        send(connection, '\t'.code, modifiers)
        send(connection, '\t'.code, modifiers)
        send(connection, key(alt), modifiers)
        assertEquals(listOf(altDown, tabDown, tabUp, tabDown, tabUp, tabDown, tabUp, altUp),
            summary(connection.events))
    }

    @Test
    fun testHeldAndLatchedModifiersTogether() {
        val modifiers = StickyModifiers()
        val connection = RecordingInputConnection()
        modifiers.tap(ctrl)
        modifiers.press(alt)
        send(connection, '\t'.code, modifiers)
        send(connection, '\t'.code, modifiers)
        val tabDown = down(KeyEvent.KEYCODE_TAB, 15)
        val tabUp = up(KeyEvent.KEYCODE_TAB, 15)
        // Ctrl wraps only the first key, Alt stays down
        assertEquals(listOf(altDown, ctrlDown, tabDown, tabUp, ctrlUp, tabDown, tabUp), summary(connection.events))
        assertEquals(ctrlMeta or altMeta, connection.events[2].metaState)
        assertEquals(altMeta, connection.events[5].metaState)
    }

    @Test
    fun testRightModifiersUseTheirScanCodes() {
        val modifiers = StickyModifiers()
        val connection = RecordingInputConnection()
        send(connection, key(KeyEvent.KEYCODE_CTRL_RIGHT), modifiers)
        send(connection, key(KeyEvent.KEYCODE_ALT_RIGHT), modifiers)
        send(connection, key(KeyEvent.KEYCODE_META_RIGHT), modifiers)
        send(connection, 'w'.code, modifiers)
        assertEquals(listOf(
            down(KeyEvent.KEYCODE_CTRL_RIGHT, 97), down(KeyEvent.KEYCODE_ALT_RIGHT, 100),
            down(KeyEvent.KEYCODE_META_RIGHT, 126), wDown, wUp,
            up(KeyEvent.KEYCODE_META_RIGHT, 126), up(KeyEvent.KEYCODE_ALT_RIGHT, 100),
            up(KeyEvent.KEYCODE_CTRL_RIGHT, 97),
        ), summary(connection.events))
        assertEquals(
            KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_RIGHT_ON or KeyEvent.META_ALT_ON or
                    KeyEvent.META_ALT_RIGHT_ON or KeyEvent.META_META_ON or KeyEvent.META_META_RIGHT_ON,
            connection.events[3].metaState)
    }

    @Test
    fun testPressOrderIsCtrlAltSuperShift() {
        val modifiers = StickyModifiers()
        val connection = RecordingInputConnection()
        val superDown = down(KeyEvent.KEYCODE_META_LEFT, 125)
        val superUp = up(KeyEvent.KEYCODE_META_LEFT, 125)
        listOf(KeyEvent.KEYCODE_META_LEFT, alt, ctrl).forEach { send(connection, key(it), modifiers) }
        send(connection, 'W'.code, modifiers)
        assertEquals(listOf(ctrlDown, altDown, superDown, shiftDown, wDown, wUp, shiftUp, superUp, altUp, ctrlUp),
            summary(connection.events))
    }

    @Test
    fun testCtrlAndShiftedLetter() {
        val modifiers = StickyModifiers()
        val connection = RecordingInputConnection()
        send(connection, key(ctrl), modifiers)
        send(connection, 'W'.code, modifiers, shifted = true)
        assertEquals(listOf(ctrlDown, shiftDown, wDown, wUp, shiftUp, ctrlUp), summary(connection.events))
        assertEquals(ctrlMeta or shiftMeta, connection.events[2].metaState)
    }

    @Test
    fun testShiftedCharacters() {
        val connection = RecordingInputConnection()
        send(connection, 'W'.code, StickyModifiers())
        assertEquals(listOf(shiftDown, wDown, wUp, shiftUp), summary(connection.events))
        assertEquals(listOf(shiftMeta, shiftMeta, shiftMeta, 0), connection.events.map { it.metaState })

        // A manual shift does not add a second Shift
        connection.events.clear()
        send(connection, '!'.code, StickyModifiers(), shifted = true)
        assertEquals(listOf(shiftDown, down(KeyEvent.KEYCODE_1, 2), up(KeyEvent.KEYCODE_1, 2), shiftUp),
            summary(connection.events))

        // A manual shift does not change characters that do not need Shift
        connection.events.clear()
        send(connection, '1'.code, StickyModifiers(), shifted = true)
        assertEquals(listOf(down(KeyEvent.KEYCODE_1, 2), up(KeyEvent.KEYCODE_1, 2)), summary(connection.events))

        // A latched Shift is not pressed twice
        connection.events.clear()
        send(connection, 'W'.code, StickyModifiers().apply { tap(shift) })
        assertEquals(listOf(shiftDown, wDown, wUp, shiftUp), summary(connection.events))

        // Neither is a held Shift
        connection.events.clear()
        send(connection, 'W'.code, StickyModifiers().apply { press(shift) })
        assertEquals(listOf(shiftDown, wDown, wUp), summary(connection.events))
    }

    @Test
    fun testManualShiftAddsShiftToCaselessKeys() {
        val space = listOf(down(KeyEvent.KEYCODE_SPACE, 57), up(KeyEvent.KEYCODE_SPACE, 57))
        val enter = listOf(down(KeyEvent.KEYCODE_ENTER, 28), up(KeyEvent.KEYCODE_ENTER, 28))
        val tab = listOf(down(KeyEvent.KEYCODE_TAB, 15), up(KeyEvent.KEYCODE_TAB, 15))
        val backspace = listOf(down(KeyEvent.KEYCODE_DEL, 14), up(KeyEvent.KEYCODE_DEL, 14))
        val arrow = listOf(down(KeyEvent.KEYCODE_DPAD_LEFT, 105), up(KeyEvent.KEYCODE_DPAD_LEFT, 105))

        mapOf(
            ' '.code to space, '\n'.code to enter, '\t'.code to tab,
            Constants.CODE_DELETE to backspace, key(KeyEvent.KEYCODE_DPAD_LEFT) to arrow,
        ).forEach { (code, keys) ->
            val unshifted = RecordingInputConnection()
            assertTrue(send(unshifted, code, StickyModifiers()))
            assertEquals("$code", keys, summary(unshifted.events))

            val shifted = RecordingInputConnection()
            assertTrue(send(shifted, code, StickyModifiers(), shifted = true))
            assertEquals("$code", listOf(shiftDown) + keys + shiftUp, summary(shifted.events))

            // Modifiers apply to these keys too
            val withCtrl = RecordingInputConnection()
            send(withCtrl, code, StickyModifiers().apply { tap(ctrl) })
            assertEquals("$code", listOf(ctrlDown) + keys + ctrlUp, summary(withCtrl.events))
        }

        val shiftEnter = RecordingInputConnection()
        send(shiftEnter, Constants.CODE_SHIFT_ENTER, StickyModifiers())
        assertEquals(listOf(shiftDown) + enter + shiftUp, summary(shiftEnter.events))
    }

    @Test
    fun testCtrlAltForwardDelete() {
        val modifiers = StickyModifiers()
        val connection = RecordingInputConnection()
        send(connection, key(alt), modifiers)
        send(connection, key(ctrl), modifiers)
        send(connection, key(KeyEvent.KEYCODE_FORWARD_DEL), modifiers)

        val del = KeyEvent.KEYCODE_FORWARD_DEL
        assertEquals(listOf(ctrlDown, altDown, down(del, 111), up(del, 111), altUp, ctrlUp),
            summary(connection.events))
        assertEquals(
            listOf(ctrlMeta, ctrlMeta or altMeta, ctrlMeta or altMeta, ctrlMeta or altMeta, ctrlMeta, 0),
            connection.events.map { it.metaState })
        assertEquals(connection.events[0].downTime, connection.events[5].downTime)
        assertEquals(connection.events[1].downTime, connection.events[4].downTime)
        assertTrue(modifiers.isEmpty)
    }

    @Test
    fun testFailureStillReleasesPressedKeys() {
        val modifiers = listOf(ctrl, alt)
        val full = listOf(ctrlDown, altDown, wDown, wUp, altUp, ctrlUp)

        listOf(
            0 to listOf(ctrlDown, ctrlUp),
            1 to listOf(ctrlDown, altDown, altUp, ctrlUp),
            2 to full, 3 to full, 4 to full, 5 to full,
        ).forEach { (step, expected) ->
            listOf(
                RecordingInputConnection(failAt = step),
                RecordingInputConnection(throwAt = step),
            ).forEach { connection ->
                assertFalse("step $step", sendRawKeyPress(connection, KeyEvent.KEYCODE_W, 17, modifiers))
                assertEquals("step $step", expected, summary(connection.events))
            }
        }

        assertTrue(sendRawKeyPress(RecordingInputConnection(), KeyEvent.KEYCODE_W, 17, modifiers))
    }

    @Test
    fun testResetReleasesModifiersThatAreDown() {
        val modifiers = StickyModifiers()
        val connection = RecordingInputConnection()
        modifiers.press(ctrl)
        modifiers.press(alt)
        modifiers.tap(KeyEvent.KEYCODE_META_LEFT)
        send(connection, 'w'.code, modifiers)
        connection.events.clear()

        resetModifiers(connection, modifiers)
        assertEquals(listOf(altUp, ctrlUp), summary(connection.events))
        assertTrue(modifiers.isEmpty)
        assertFalse(modifiers.isHeld(ctrl))

        // Without a connection the state is still reset
        modifiers.tap(ctrl)
        resetModifiers(null, modifiers)
        assertTrue(modifiers.isEmpty)
    }

    @Test
    fun testUnmappedCharactersSendNothing() {
        val modifiers = StickyModifiers().apply { tap(ctrl) }
        listOf('ㅎ'.code, '×'.code, 'é'.code).forEach {
            val connection = RecordingInputConnection()
            assertTrue(send(connection, it, modifiers))
            assertTrue(connection.events.isEmpty())
        }
        // A dropped key still consumes the latched modifiers
        assertTrue(modifiers.isEmpty)

        // Key event keys without a Linux key are dropped too
        val connection = RecordingInputConnection()
        assertTrue(send(connection, key(KeyEvent.KEYCODE_BACK), StickyModifiers()))
        assertTrue(connection.events.isEmpty())
    }

    @Test
    fun testKeyboardInternalCodesAreNotHandled() {
        val modifiers = StickyModifiers().apply { tap(ctrl) }
        listOf(
            Constants.CODE_SHIFT, Constants.CODE_SWITCH_ALPHA_SYMBOL, Constants.CODE_SYMBOL_SHIFT,
            Constants.CODE_CAPSLOCK, Constants.CODE_LANGUAGE_SWITCH, Constants.CODE_EMOJI,
            Constants.CODE_SETTINGS, Constants.CODE_TO_NUMBER_LAYOUT, Constants.CODE_TO_ALT_0_LAYOUT,
            Constants.CODE_TO_ALPHA_0_LAYOUT, Constants.CODE_ACTION_0 + 3,
            Constants.CODE_ALT_ACTION_0 + 3, Constants.CODE_UNSPECIFIED,
        ).forEach {
            val connection = RecordingInputConnection()
            assertFalse("$it", send(connection, it, modifiers))
            assertTrue(connection.events.isEmpty())
            assertEquals("$it", SoftKeyRoute.Normal, routeSoftKey(it, false, modifiers, 0L))
        }
        // Modifiers stay latched across them, e.g. across a switch to the Fn page and back
        assertEquals(ModifierState.Latched, modifiers.stateOf(ctrl))
    }

    @Test
    fun testAvfText() {
        val modifiers = StickyModifiers()
        val connection = RecordingInputConnection()
        handleAvfSoftText(connection, "wW한", modifiers)
        assertEquals(listOf(wDown, wUp, shiftDown, wDown, wUp, shiftUp), summary(connection.events))

        connection.events.clear()
        modifiers.tap(ctrl)
        handleAvfSoftText(connection, "ww", modifiers)
        assertEquals(listOf(ctrlDown, wDown, wUp, ctrlUp, ctrlDown, wDown, wUp, ctrlUp), summary(connection.events))
        assertTrue(modifiers.isEmpty)
    }

    @Test
    fun testNormalRouting() {
        val modifiers = StickyModifiers(doubleTapTimeoutMs = 300L)
        fun route(code: Int, shifted: Boolean = false, timeMs: Long = 0L) = routeSoftKey(code, shifted, modifiers, timeMs)

        // Without active modifiers and key event keys nothing changes
        listOf('w'.code, 'W'.code, ' '.code, '\n'.code, 'ㅎ'.code, Constants.CODE_DELETE,
            Constants.CODE_SHIFT_ENTER).forEach {
            assertEquals("$it", SoftKeyRoute.Normal, route(it))
            assertEquals("$it", SoftKeyRoute.Normal, route(it, shifted = true))
        }

        // Key event keys
        assertEquals(SoftKeyRoute.KeyEvent(KeyEvent.KEYCODE_ESCAPE, 0), route(key(KeyEvent.KEYCODE_ESCAPE)))
        assertEquals(SoftKeyRoute.KeyEvent(KeyEvent.KEYCODE_DPAD_LEFT, shiftMeta),
            route(key(KeyEvent.KEYCODE_DPAD_LEFT), shifted = true))
        assertEquals(SoftKeyRoute.KeyEvent(KeyEvent.KEYCODE_BACK, 0), route(key(KeyEvent.KEYCODE_BACK)))

        // Ctrl+W
        assertEquals(SoftKeyRoute.Consumed, route(key(ctrl)))
        assertEquals(SoftKeyRoute.KeyEvent(KeyEvent.KEYCODE_W, ctrlMeta), route('w'.code))
        assertTrue(modifiers.isEmpty)

        // Ctrl+Shift+W, Ctrl+Backspace
        route(key(ctrl))
        assertEquals(SoftKeyRoute.KeyEvent(KeyEvent.KEYCODE_W, ctrlMeta or shiftMeta), route('W'.code))
        route(key(ctrl))
        assertEquals(SoftKeyRoute.KeyEvent(KeyEvent.KEYCODE_DEL, ctrlMeta), route(Constants.CODE_DELETE))

        // Ctrl+Alt+Forward delete
        route(key(ctrl))
        route(key(alt))
        assertEquals(SoftKeyRoute.KeyEvent(KeyEvent.KEYCODE_FORWARD_DEL, ctrlMeta or altMeta),
            route(key(KeyEvent.KEYCODE_FORWARD_DEL)))
        assertTrue(modifiers.isEmpty)

        // Locked Ctrl applies to every key
        route(key(ctrl), timeMs = 1000L)
        route(key(ctrl), timeMs = 1100L)
        assertEquals(SoftKeyRoute.KeyEvent(KeyEvent.KEYCODE_A, ctrlMeta), route('a'.code))
        assertEquals(SoftKeyRoute.KeyEvent(KeyEvent.KEYCODE_C, ctrlMeta), route('c'.code))
        route(key(ctrl), timeMs = 5000L)
        assertEquals(SoftKeyRoute.Normal, route('a'.code))

        // Held Ctrl applies during the chord, and goes off when released
        modifiers.press(ctrl)
        assertEquals(SoftKeyRoute.KeyEvent(KeyEvent.KEYCODE_A, ctrlMeta), route('a'.code))
        assertEquals(SoftKeyRoute.KeyEvent(KeyEvent.KEYCODE_C, ctrlMeta), route('c'.code))
        assertEquals(SoftKeyRoute.Consumed, route(key(ctrl)))
        assertTrue(modifiers.isEmpty)

        // A character without a key code is typed normally, and consumes the modifiers
        route(key(ctrl))
        assertEquals(SoftKeyRoute.Normal, route('ㅎ'.code))
        assertTrue(modifiers.isEmpty)
    }

    @Test
    fun testCursorSwipeSteps() {
        val step = 10
        assertEquals(SwipeSteps(0, 0), cursorSwipeSteps(9, 5, step))
        assertEquals(SwipeSteps(2, 0), cursorSwipeSteps(25, 5, step))
        assertEquals(SwipeSteps(-2, 0), cursorSwipeSteps(-25, -5, step))
        assertEquals(SwipeSteps(0, 3), cursorSwipeSteps(4, 31, step))
        assertEquals(SwipeSteps(0, -1), cursorSwipeSteps(-4, -12, step))
        // The dominant axis wins even if the other axis has steps too, and x wins a tie
        assertEquals(SwipeSteps(0, 2), cursorSwipeSteps(15, 20, step))
        assertEquals(SwipeSteps(1, 0), cursorSwipeSteps(15, -15, step))
    }

    @Test
    fun testAvfCursorKeys() {
        val left = listOf(down(KeyEvent.KEYCODE_DPAD_LEFT, 105), up(KeyEvent.KEYCODE_DPAD_LEFT, 105))
        val right = listOf(down(KeyEvent.KEYCODE_DPAD_RIGHT, 106), up(KeyEvent.KEYCODE_DPAD_RIGHT, 106))
        val upKey = listOf(down(KeyEvent.KEYCODE_DPAD_UP, 103), up(KeyEvent.KEYCODE_DPAD_UP, 103))
        val downKey = listOf(down(KeyEvent.KEYCODE_DPAD_DOWN, 108), up(KeyEvent.KEYCODE_DPAD_DOWN, 108))

        fun send(steps: Int, vertical: Boolean, shifted: Boolean = false, modifiers: StickyModifiers = StickyModifiers()) =
            RecordingInputConnection().also { sendAvfCursorKeys(it, steps, vertical, shifted, modifiers) }.events.let { summary(it) }

        assertEquals(right + right, send(2, false))
        assertEquals(left, send(-1, false))
        assertEquals(downKey + downKey + downKey, send(3, true))
        assertEquals(upKey, send(-1, true))
        assertEquals(emptyList<Triple<Int, Int, Int>>(), send(0, true))
        assertEquals(listOf(shiftDown) + upKey + shiftUp, send(-1, true, shifted = true))

        // Latched modifiers apply to each step and stay latched until the swipe ends
        val latched = StickyModifiers().apply { tap(ctrl) }
        assertEquals(listOf(ctrlDown) + right + ctrlUp + ctrlDown + right + ctrlUp, send(2, false, modifiers = latched))
        assertEquals(ModifierState.Latched, latched.stateOf(ctrl))
        latched.consume()
        assertTrue(latched.isEmpty)

        // Locked ones stay locked after the swipe
        val locked = StickyModifiers().apply { tap(ctrl, 0L); tap(ctrl, 1L) }
        assertEquals(listOf(ctrlDown) + upKey + ctrlUp, send(-1, true, modifiers = locked))
        locked.consume()
        assertEquals(ModifierState.Locked, locked.stateOf(ctrl))

        // A held one goes down once
        val held = StickyModifiers().apply { press(ctrl) }
        assertEquals(listOf(ctrlDown) + left + left, send(-2, false, modifiers = held))
    }

    @Test
    fun testPadKeysForSteps() {
        val cursor = Constants.CODE_CURSOR_PAD
        val nav = Constants.CODE_NAV_PAD
        assertEquals(PadKey(KeyEvent.KEYCODE_DPAD_RIGHT, 2), padKeyForSteps(cursor, SwipeSteps(2, 0)))
        assertEquals(PadKey(KeyEvent.KEYCODE_DPAD_LEFT, 1), padKeyForSteps(cursor, SwipeSteps(-1, 0)))
        assertEquals(PadKey(KeyEvent.KEYCODE_DPAD_DOWN, 3), padKeyForSteps(cursor, SwipeSteps(0, 3)))
        assertEquals(PadKey(KeyEvent.KEYCODE_DPAD_UP, 1), padKeyForSteps(cursor, SwipeSteps(0, -1)))
        assertEquals(PadKey(KeyEvent.KEYCODE_MOVE_END, 2), padKeyForSteps(nav, SwipeSteps(2, 0)))
        assertEquals(PadKey(KeyEvent.KEYCODE_MOVE_HOME, 1), padKeyForSteps(nav, SwipeSteps(-1, 0)))
        assertEquals(PadKey(KeyEvent.KEYCODE_PAGE_DOWN, 1), padKeyForSteps(nav, SwipeSteps(0, 1)))
        assertEquals(PadKey(KeyEvent.KEYCODE_PAGE_UP, 4), padKeyForSteps(nav, SwipeSteps(0, -4)))
        assertNull(padKeyForSteps(cursor, SwipeSteps(0, 0)))
        assertNull(padKeyForSteps(nav, SwipeSteps(0, 0)))
        assertNull(padKeyForSteps(Constants.CODE_SPACE, SwipeSteps(1, 0)))
        assertNull(padKeyForSteps(Constants.CODE_DELETE, SwipeSteps(1, 0)))
    }

    @Test
    fun testAvfPadSteps() {
        val home = listOf(down(KeyEvent.KEYCODE_MOVE_HOME, 102), up(KeyEvent.KEYCODE_MOVE_HOME, 102))
        val pageDown = listOf(down(KeyEvent.KEYCODE_PAGE_DOWN, 109), up(KeyEvent.KEYCODE_PAGE_DOWN, 109))
        val right = listOf(down(KeyEvent.KEYCODE_DPAD_RIGHT, 106), up(KeyEvent.KEYCODE_DPAD_RIGHT, 106))
        val upKey = listOf(down(KeyEvent.KEYCODE_DPAD_UP, 103), up(KeyEvent.KEYCODE_DPAD_UP, 103))

        fun send(code: Int, steps: SwipeSteps, shifted: Boolean = false, modifiers: StickyModifiers = StickyModifiers()) =
            RecordingInputConnection().also { sendAvfPadSteps(it, code, steps, shifted, modifiers) }.events.let { summary(it) }

        assertEquals(right + right, send(Constants.CODE_CURSOR_PAD, SwipeSteps(2, 0)))
        assertEquals(upKey, send(Constants.CODE_CURSOR_PAD, SwipeSteps(0, -1)))
        assertEquals(home, send(Constants.CODE_NAV_PAD, SwipeSteps(-1, 0)))
        assertEquals(pageDown + pageDown, send(Constants.CODE_NAV_PAD, SwipeSteps(0, 2)))
        assertEquals(emptyList<Triple<Int, Int, Int>>(), send(Constants.CODE_NAV_PAD, SwipeSteps(0, 0)))

        // Manual shift and latched modifiers apply to every step, and stay until the gesture ends
        assertEquals(listOf(shiftDown) + home + shiftUp, send(Constants.CODE_NAV_PAD, SwipeSteps(-1, 0), shifted = true))
        val latched = StickyModifiers().apply { tap(ctrl) }
        assertEquals(listOf(ctrlDown) + home + ctrlUp + ctrlDown + home + ctrlUp,
            send(Constants.CODE_NAV_PAD, SwipeSteps(-2, 0), modifiers = latched))
        assertEquals(ModifierState.Latched, latched.stateOf(ctrl))
        // The end of the gesture consumes the latched ones, and keeps the locked ones
        latched.tap(alt, 0L)
        latched.tap(alt, 1L)
        latched.consume()
        assertEquals(ModifierState.Off, latched.stateOf(ctrl))
        assertEquals(ModifierState.Locked, latched.stateOf(alt))

        // Ctrl+Shift+End, with the modifiers in the fixed order
        val modifiers = StickyModifiers().apply { tap(ctrl) }
        val end = listOf(down(KeyEvent.KEYCODE_MOVE_END, 107), up(KeyEvent.KEYCODE_MOVE_END, 107))
        assertEquals(listOf(ctrlDown, shiftDown) + end + listOf(shiftUp, ctrlUp),
            send(Constants.CODE_NAV_PAD, SwipeSteps(1, 0), shifted = true, modifiers = modifiers))

        // A held modifier goes down once
        val held = StickyModifiers().apply { press(alt) }
        assertEquals(listOf(altDown) + upKey + upKey, send(Constants.CODE_CURSOR_PAD, SwipeSteps(0, -2), modifiers = held))
    }

    @Test
    fun testNormalPadSteps() {
        fun events(code: Int, steps: SwipeSteps, shifted: Boolean = false, modifiers: StickyModifiers = StickyModifiers()) =
            padKeyEvents(code, steps, shifted, modifiers)

        assertEquals(List(2) { SoftKeyRoute.KeyEvent(KeyEvent.KEYCODE_DPAD_LEFT, 0) },
            events(Constants.CODE_CURSOR_PAD, SwipeSteps(-2, 0)))
        assertEquals(listOf(SoftKeyRoute.KeyEvent(KeyEvent.KEYCODE_PAGE_UP, 0)),
            events(Constants.CODE_NAV_PAD, SwipeSteps(0, -1)))
        assertEquals(listOf(SoftKeyRoute.KeyEvent(KeyEvent.KEYCODE_DPAD_DOWN, shiftMeta)),
            events(Constants.CODE_CURSOR_PAD, SwipeSteps(0, 1), shifted = true))
        assertEquals(emptyList<SoftKeyRoute.KeyEvent>(), events(Constants.CODE_CURSOR_PAD, SwipeSteps(0, 0)))

        val modifiers = StickyModifiers().apply { tap(ctrl) }
        assertEquals(List(2) { SoftKeyRoute.KeyEvent(KeyEvent.KEYCODE_MOVE_END, ctrlMeta or shiftMeta) },
            events(Constants.CODE_NAV_PAD, SwipeSteps(2, 0), shifted = true, modifiers = modifiers))
        // The steps do not consume the modifiers; the end of the gesture does
        assertEquals(ModifierState.Latched, modifiers.stateOf(ctrl))
    }

    @Test
    fun testPadTapIsNotAKey() {
        // A tap of a pad reaches neither the AVF display nor the active IME
        val modifiers = StickyModifiers().apply { tap(ctrl) }
        listOf(Constants.CODE_CURSOR_PAD, Constants.CODE_NAV_PAD).forEach {
            val connection = RecordingInputConnection()
            assertTrue(send(connection, it, modifiers))
            assertTrue(connection.events.isEmpty())
            assertEquals(SoftKeyRoute.Consumed, routeSoftKey(it, false, modifiers, 0L))
        }
        assertEquals(ModifierState.Latched, modifiers.stateOf(ctrl))
    }
}
