package org.futo.inputmethod.keyboard.internal

import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import androidx.test.filters.SmallTest
import androidx.test.runner.AndroidJUnit4
import org.futo.inputmethod.event.Event
import org.futo.inputmethod.latin.common.Constants
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A key with a `!code/keyevent_*` code acts like a character key for the layout page: after it,
 * an alt page (e.g. an Fn layer) and a manual shift return to the base page. Modifier keys do not.
 */
@SmallTest
@RunWith(AndroidJUnit4::class)
class KeyboardStateKeyEventTests {
    private class RecordingSwitchActions : SwitchActions {
        var element: KeyboardLayoutElement? = null

        override fun setKeyboard(element: KeyboardLayoutElement) {
            this.element = element
        }

        override fun requestUpdatingShiftState(autoCapsFlags: Int) {}
    }

    private val capsOff = Constants.TextUtils.CAP_MODE_OFF
    private lateinit var switchActions: RecordingSwitchActions
    private lateinit var state: KeyboardState

    @Before
    fun setUp() {
        switchActions = RecordingSwitchActions()
        state = KeyboardState(switchActions)
        state.onLoadKeyboard(EditorInfo(), capsOff, "qwerty", null)
    }

    private fun press(code: Int) {
        state.onPressKey(code, true, capsOff)
        val event = if(code > 0) {
            Event.createSoftwareKeypressEvent(code, Event.NOT_A_KEY_CODE,
                Constants.NOT_A_COORDINATE, Constants.NOT_A_COORDINATE, false)
        } else {
            Event.createSoftwareKeypressEvent(Event.NOT_A_CODE_POINT, code,
                Constants.NOT_A_COORDINATE, Constants.NOT_A_COORDINATE, false)
        }
        state.onEvent(event, capsOff)
        state.onReleaseKey(code, false, capsOff)
    }

    private fun keyEvent(keyCode: Int) = Constants.CODE_KEYEVENT_0 + keyCode

    private val page: KeyboardLayoutPage?
        get() = switchActions.element?.page

    @Test
    fun testAltPageReturnsAfterKeyEventKey() {
        press(Constants.CODE_TO_ALT_0_LAYOUT)
        assertEquals(KeyboardLayoutPage.Alt0, page)

        press(keyEvent(KeyEvent.KEYCODE_FORWARD_DEL))
        assertEquals(KeyboardLayoutPage.Base, page)
    }

    @Test
    fun testAltPageReturnsAfterLetter() {
        press(Constants.CODE_TO_ALT_0_LAYOUT)
        press('a'.code)
        assertEquals(KeyboardLayoutPage.Base, page)
    }

    @Test
    fun testAltPageStaysAfterModifierKey() {
        press(Constants.CODE_TO_ALT_0_LAYOUT)
        press(keyEvent(KeyEvent.KEYCODE_CTRL_LEFT))
        assertEquals(KeyboardLayoutPage.Alt0, page)
    }

    @Test
    fun testManualShiftReturnsAfterKeyEventKey() {
        press(Constants.CODE_SHIFT)
        assertEquals(KeyboardLayoutPage.ManuallyShifted, page)

        press(keyEvent(KeyEvent.KEYCODE_ESCAPE))
        assertEquals(KeyboardLayoutPage.Base, page)
    }

    // The end of a pad gesture with steps reaches the keyboard state as an event of the pad code
    private fun padGesture(code: Int, withSteps: Boolean) {
        state.onPressKey(code, true, capsOff)
        if(withSteps) {
            state.onEvent(Event.createSoftwareKeypressEvent(Event.NOT_A_CODE_POINT, code,
                Constants.NOT_A_COORDINATE, Constants.NOT_A_COORDINATE, false), capsOff)
        }
        state.onReleaseKey(code, false, capsOff)
    }

    @Test
    fun testAltPageReturnsAfterPadGesture() {
        listOf(Constants.CODE_CURSOR_PAD, Constants.CODE_NAV_PAD).forEach { pad ->
            press(Constants.CODE_TO_ALT_0_LAYOUT)
            assertEquals(KeyboardLayoutPage.Alt0, page)
            padGesture(pad, withSteps = true)
            assertEquals(KeyboardLayoutPage.Base, page)
        }
    }

    @Test
    fun testManualShiftReturnsAfterPadGesture() {
        press(Constants.CODE_SHIFT)
        assertEquals(KeyboardLayoutPage.ManuallyShifted, page)
        padGesture(Constants.CODE_CURSOR_PAD, withSteps = true)
        assertEquals(KeyboardLayoutPage.Base, page)
    }

    @Test
    fun testPadTapChangesNothing() {
        press(Constants.CODE_TO_ALT_0_LAYOUT)
        padGesture(Constants.CODE_NAV_PAD, withSteps = false)
        assertEquals(KeyboardLayoutPage.Alt0, page)

        press(Constants.CODE_TO_ALT_0_LAYOUT)
        assertEquals(KeyboardLayoutPage.Base, page)
        press(Constants.CODE_SHIFT)
        padGesture(Constants.CODE_CURSOR_PAD, withSteps = false)
        assertEquals(KeyboardLayoutPage.ManuallyShifted, page)
    }
}
