package org.futo.inputmethod.latin.uix.actions

import android.text.InputType
import android.view.InputDevice
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnectionWrapper
import androidx.test.filters.SmallTest
import androidx.test.runner.AndroidJUnit4
import org.futo.inputmethod.latin.common.Constants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class AvfShiftSpaceTests {
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

    private fun avfDisplayEditorInfo() = EditorInfo().apply {
        packageName = AVF_TERMINAL_PACKAGE
        inputType = InputType.TYPE_NULL
        imeOptions = 0x12000000
    }

    private fun send(
        connection: RecordingInputConnection?,
        editorInfo: EditorInfo? = avfDisplayEditorInfo(),
        connectionOverridden: Boolean = false,
    ) = sendAvfShiftSpaceKeys(connectionOverridden, editorInfo, connection)

    private fun summary(events: List<KeyEvent>) = events.map {
        Triple(it.action, it.keyCode, it.scanCode)
    }

    private val shiftDown = Triple(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_SHIFT_LEFT, 42)
    private val spaceDown = Triple(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_SPACE, 57)
    private val spaceUp = Triple(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_SPACE, 57)
    private val shiftUp = Triple(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_SHIFT_LEFT, 42)

    @Test
    fun testSendsShiftSpaceAsRawKeys() {
        val connection = RecordingInputConnection()
        assertTrue(send(connection))

        val events = connection.events
        assertEquals(listOf(shiftDown, spaceDown, spaceUp, shiftUp), summary(events))

        val shiftMeta = KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON
        assertEquals(listOf(shiftMeta, shiftMeta, shiftMeta, 0), events.map { it.metaState })

        assertEquals(events[0].downTime, events[3].downTime)
        assertEquals(events[1].downTime, events[2].downTime)

        events.forEach {
            assertEquals(KeyCharacterMap.VIRTUAL_KEYBOARD, it.deviceId)
            assertEquals(InputDevice.SOURCE_KEYBOARD, it.source)
            assertEquals(0, it.repeatCount)
            assertTrue(it.flags and KeyEvent.FLAG_SOFT_KEYBOARD != 0)
        }
    }

    @Test
    fun testFailedShiftDownSendsNoSpace() {
        listOf(
            RecordingInputConnection(failAt = 0),
            RecordingInputConnection(throwAt = 0),
        ).forEach { connection ->
            assertFalse(send(connection))
            assertEquals(listOf(shiftDown, shiftUp), summary(connection.events))
        }
    }

    @Test
    fun testFailureAfterShiftDownStillReleasesKeys() {
        for(step in 1..3) {
            listOf(
                RecordingInputConnection(failAt = step),
                RecordingInputConnection(throwAt = step),
            ).forEach { connection ->
                assertFalse("step $step", send(connection))
                assertEquals("step $step",
                    listOf(shiftDown, spaceDown, spaceUp, shiftUp), summary(connection.events))
            }
        }
    }

    @Test
    fun testRejectsOtherTargets() {
        val nativeTerminal = avfDisplayEditorInfo().apply { inputType = 0x80091 }
        val otherApp = avfDisplayEditorInfo().apply { packageName = "com.example.app" }

        listOf<(RecordingInputConnection) -> Boolean>(
            { send(it, editorInfo = nativeTerminal) },
            { send(it, editorInfo = otherApp) },
            { send(it, editorInfo = null) },
            { send(it, connectionOverridden = true) },
        ).forEach { attempt ->
            val connection = RecordingInputConnection()
            assertFalse(attempt(connection))
            assertTrue(connection.events.isEmpty())
        }

        assertFalse(send(null))
    }

    @Test
    fun testActionIsAppendedWithoutMovingExistingActions() {
        val existingKeys = listOf(
            "emoji", "settings", "paste", "text_edit", "themes", "undo", "redo",
            "voice_input", "system_voice_input", "switch_language", "clipboard_history",
            "mem_dbg", "cut", "copy", "select_all", "more", "bugs", "keyboard_modes",
            "up", "down", "left", "right", "font_typer",
        )
        assertEquals(existingKeys + "avf_shift_space", AllActionKeys)
        assertSame(AvfShiftSpaceAction, AllActionsMap["avf_shift_space"])
        assertEquals(Constants.CODE_ACTION_0 + existingKeys.size, AvfShiftSpaceAction.keyCode)
        assertEquals(existingKeys.size, ActionRegistry.parseAction("avf_shift_space"))
    }
}
