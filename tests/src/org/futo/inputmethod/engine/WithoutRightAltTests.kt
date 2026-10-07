package org.futo.inputmethod.engine

import android.view.KeyEvent
import androidx.test.filters.SmallTest
import androidx.test.runner.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class WithoutRightAltTests {
    private fun keyDown(keyCode: Int, metaState: Int) =
        KeyEvent(0L, 0L, KeyEvent.ACTION_DOWN, keyCode, 0, metaState)

    @Test
    fun testRightAltIsRemoved() {
        val event = withoutRightAlt(keyDown(KeyEvent.KEYCODE_E,
            KeyEvent.META_ALT_ON or KeyEvent.META_ALT_RIGHT_ON))
        assertFalse(event.isAltPressed)
        assertEquals(KeyEvent.KEYCODE_E, event.keyCode)
        assertEquals('e'.code, event.unicodeChar)
    }

    @Test
    fun testOtherModifiersAreKept() {
        val event = withoutRightAlt(keyDown(KeyEvent.KEYCODE_E,
            KeyEvent.META_ALT_ON or KeyEvent.META_ALT_RIGHT_ON
                    or KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON))
        assertFalse(event.isAltPressed)
        assertTrue(event.isShiftPressed)
        assertEquals('E'.code, event.unicodeChar)
    }

    @Test
    fun testLeftAltIsKept() {
        val event = withoutRightAlt(keyDown(KeyEvent.KEYCODE_E,
            KeyEvent.META_ALT_ON or KeyEvent.META_ALT_RIGHT_ON or KeyEvent.META_ALT_LEFT_ON))
        assertTrue(event.isAltPressed)
        assertEquals(KeyEvent.META_ALT_LEFT_ON, event.metaState and KeyEvent.META_ALT_LEFT_ON)
        assertEquals(0, event.metaState and KeyEvent.META_ALT_RIGHT_ON)
    }
}
