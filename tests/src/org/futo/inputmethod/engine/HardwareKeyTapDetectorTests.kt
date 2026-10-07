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
class HardwareKeyTapDetectorTests {
    private fun down(keyCode: Int, repeat: Int = 0) =
        KeyEvent(0L, 0L, KeyEvent.ACTION_DOWN, keyCode, repeat)

    private fun up(keyCode: Int, flags: Int = 0) =
        KeyEvent(0L, 0L, KeyEvent.ACTION_UP, keyCode, 0, 0, -1, 0, flags)

    private val detector = HardwareKeyTapDetector(KeyEvent.KEYCODE_ALT_RIGHT)

    @Test
    fun testPressAndReleaseAloneIsATap() {
        detector.onKeyDown(down(KeyEvent.KEYCODE_ALT_RIGHT))
        assertTrue(detector.onKeyUp(up(KeyEvent.KEYCODE_ALT_RIGHT)))
    }

    @Test
    fun testHoldingTheKeyIsStillATap() {
        detector.onKeyDown(down(KeyEvent.KEYCODE_ALT_RIGHT))
        detector.onKeyDown(down(KeyEvent.KEYCODE_ALT_RIGHT, repeat = 1))
        assertTrue(detector.onKeyUp(up(KeyEvent.KEYCODE_ALT_RIGHT)))
    }

    @Test
    fun testCombinationWithAnotherKeyIsNotATap() {
        // e.g. AltGr+E for the euro sign
        detector.onKeyDown(down(KeyEvent.KEYCODE_ALT_RIGHT))
        detector.onKeyDown(down(KeyEvent.KEYCODE_E))
        assertFalse(detector.onKeyUp(up(KeyEvent.KEYCODE_E)))
        assertFalse(detector.onKeyUp(up(KeyEvent.KEYCODE_ALT_RIGHT)))
    }

    @Test
    fun testCanceledReleaseIsNotATap() {
        detector.onKeyDown(down(KeyEvent.KEYCODE_ALT_RIGHT))
        assertFalse(detector.onKeyUp(up(KeyEvent.KEYCODE_ALT_RIGHT, KeyEvent.FLAG_CANCELED)))
    }

    @Test
    fun testReleaseWithoutPressIsNotATap() {
        assertFalse(detector.onKeyUp(up(KeyEvent.KEYCODE_ALT_RIGHT)))
    }

    @Test
    fun testOtherKeysAreNeverATap() {
        detector.onKeyDown(down(KeyEvent.KEYCODE_ALT_LEFT))
        assertFalse(detector.onKeyUp(up(KeyEvent.KEYCODE_ALT_LEFT)))
    }
}
