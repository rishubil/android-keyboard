package org.futo.inputmethod.engine

import android.text.InputType
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import androidx.test.filters.SmallTest
import androidx.test.runner.AndroidJUnit4
import org.futo.inputmethod.latin.uix.actions.AVF_TERMINAL_PACKAGE
import org.futo.inputmethod.latin.uix.actions.isAvfDisplay
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class AvfKeyPassthroughTests {
    private fun key(action: Int, keyCode: Int, metaState: Int = 0) =
        KeyEvent(0L, 0L, action, keyCode, 0, metaState)

    private fun keyDown(keyCode: Int, metaState: Int = 0) = key(KeyEvent.ACTION_DOWN, keyCode, metaState)

    private val shift = KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON
    private val ctrl = KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON

    @Test
    fun testLanguageSwitchKeysGoToTheGuest() {
        listOf(KeyEvent.ACTION_DOWN, KeyEvent.ACTION_UP).forEach { action ->
            assertTrue(isAvfGuestLanguageSwitchKey(key(action, KeyEvent.KEYCODE_KANA)))
            assertTrue(isAvfGuestLanguageSwitchKey(key(action, KeyEvent.KEYCODE_ALT_RIGHT)))
            assertTrue(isAvfGuestLanguageSwitchKey(key(action, KeyEvent.KEYCODE_SPACE, shift)))
        }
    }

    @Test
    fun testOtherKeysAreNotLanguageSwitchKeys() {
        assertFalse(isAvfGuestLanguageSwitchKey(keyDown(KeyEvent.KEYCODE_SPACE)))
        assertFalse(isAvfGuestLanguageSwitchKey(keyDown(KeyEvent.KEYCODE_SPACE, ctrl)))
        assertFalse(isAvfGuestLanguageSwitchKey(keyDown(KeyEvent.KEYCODE_SPACE, shift or ctrl)))
        assertFalse(isAvfGuestLanguageSwitchKey(keyDown(KeyEvent.KEYCODE_R, shift)))
        assertFalse(isAvfGuestLanguageSwitchKey(keyDown(KeyEvent.KEYCODE_ALT_LEFT)))
    }

    @Test
    fun testOnlyTheAvfDisplayIsDetected() {
        val display = EditorInfo().apply {
            packageName = AVF_TERMINAL_PACKAGE
            inputType = InputType.TYPE_NULL
        }
        val nativeTerminal = EditorInfo().apply {
            packageName = AVF_TERMINAL_PACKAGE
            inputType = 0x80091
        }
        val otherApp = EditorInfo().apply {
            packageName = "com.example.app"
            inputType = InputType.TYPE_NULL
        }

        assertTrue(isAvfDisplay(display))
        assertFalse(isAvfDisplay(nativeTerminal))
        assertFalse(isAvfDisplay(otherApp))
    }
}
