package org.futo.inputmethod.engine

import android.view.KeyEvent
import androidx.test.filters.SmallTest
import androidx.test.runner.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class HardwareLanguageSwitchKeyTests {
    private fun keyDown(keyCode: Int, metaState: Int = 0) =
        KeyEvent(0L, 0L, KeyEvent.ACTION_DOWN, keyCode, 0, metaState)

    private val hangulKey = keyDown(KeyEvent.KEYCODE_KANA)
    private val shiftSpace =
        keyDown(KeyEvent.KEYCODE_SPACE, KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON)

    private fun isSwitchKey(keyEvent: KeyEvent) =
        isHardwareLanguageSwitchKey(keyEvent, hangulKeyEnabled = true, shiftSpaceEnabled = true)

    @Test
    fun testHangulKeySwitchesLanguage() {
        assertTrue(isSwitchKey(hangulKey))
    }

    @Test
    fun testShiftSpaceSwitchesLanguage() {
        assertTrue(isSwitchKey(shiftSpace))
    }

    @Test
    fun testOtherKeysDoNotSwitchLanguage() {
        assertFalse(isSwitchKey(keyDown(KeyEvent.KEYCODE_SPACE)))
        assertFalse(isSwitchKey(
            keyDown(KeyEvent.KEYCODE_SPACE, KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON)))
        assertFalse(isSwitchKey(keyDown(KeyEvent.KEYCODE_SPACE,
            KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON or KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON)))
        assertFalse(isSwitchKey(keyDown(KeyEvent.KEYCODE_R)))
        assertFalse(isSwitchKey(keyDown(KeyEvent.KEYCODE_ALT_RIGHT)))
    }

    @Test
    fun testEachKeyCanBeDisabled() {
        assertFalse(isHardwareLanguageSwitchKey(hangulKey, hangulKeyEnabled = false, shiftSpaceEnabled = true))
        assertTrue(isHardwareLanguageSwitchKey(shiftSpace, hangulKeyEnabled = false, shiftSpaceEnabled = true))
        assertFalse(isHardwareLanguageSwitchKey(shiftSpace, hangulKeyEnabled = true, shiftSpaceEnabled = false))
        assertTrue(isHardwareLanguageSwitchKey(hangulKey, hangulKeyEnabled = true, shiftSpaceEnabled = false))
    }
}
