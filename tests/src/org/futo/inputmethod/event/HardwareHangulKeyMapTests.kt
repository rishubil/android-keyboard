package org.futo.inputmethod.event

import android.view.KeyEvent
import androidx.test.filters.SmallTest
import androidx.test.runner.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class HardwareHangulKeyMapTests {
    @Test
    fun testDubeolsikLetters() {
        val expected = mapOf(
            KeyEvent.KEYCODE_Q to 'ㅂ', KeyEvent.KEYCODE_W to 'ㅈ', KeyEvent.KEYCODE_E to 'ㄷ',
            KeyEvent.KEYCODE_R to 'ㄱ', KeyEvent.KEYCODE_T to 'ㅅ', KeyEvent.KEYCODE_Y to 'ㅛ',
            KeyEvent.KEYCODE_U to 'ㅕ', KeyEvent.KEYCODE_I to 'ㅑ', KeyEvent.KEYCODE_O to 'ㅐ',
            KeyEvent.KEYCODE_P to 'ㅔ', KeyEvent.KEYCODE_A to 'ㅁ', KeyEvent.KEYCODE_S to 'ㄴ',
            KeyEvent.KEYCODE_D to 'ㅇ', KeyEvent.KEYCODE_F to 'ㄹ', KeyEvent.KEYCODE_G to 'ㅎ',
            KeyEvent.KEYCODE_H to 'ㅗ', KeyEvent.KEYCODE_J to 'ㅓ', KeyEvent.KEYCODE_K to 'ㅏ',
            KeyEvent.KEYCODE_L to 'ㅣ', KeyEvent.KEYCODE_Z to 'ㅋ', KeyEvent.KEYCODE_X to 'ㅌ',
            KeyEvent.KEYCODE_C to 'ㅊ', KeyEvent.KEYCODE_V to 'ㅍ', KeyEvent.KEYCODE_B to 'ㅠ',
            KeyEvent.KEYCODE_N to 'ㅜ', KeyEvent.KEYCODE_M to 'ㅡ'
        )
        assertEquals(26, expected.size)
        for ((keyCode, jamo) in expected) {
            assertEquals(KeyEvent.keyCodeToString(keyCode), jamo, HardwareHangulKeyMap.charFor(keyCode, false))
        }
    }

    @Test
    fun testShiftedDubeolsikLetters() {
        val expected = mapOf(
            KeyEvent.KEYCODE_Q to 'ㅃ', KeyEvent.KEYCODE_W to 'ㅉ', KeyEvent.KEYCODE_E to 'ㄸ',
            KeyEvent.KEYCODE_R to 'ㄲ', KeyEvent.KEYCODE_T to 'ㅆ', KeyEvent.KEYCODE_O to 'ㅒ',
            KeyEvent.KEYCODE_P to 'ㅖ'
        )
        for ((keyCode, jamo) in expected) {
            assertEquals(KeyEvent.keyCodeToString(keyCode), jamo, HardwareHangulKeyMap.charFor(keyCode, true))
        }
    }

    @Test
    fun testShiftWithoutShiftedJamoKeepsTheBaseJamo() {
        assertEquals('ㅏ', HardwareHangulKeyMap.charFor(KeyEvent.KEYCODE_K, true))
        assertEquals('ㅁ', HardwareHangulKeyMap.charFor(KeyEvent.KEYCODE_A, true))
    }

    @Test
    fun testNonLetterKeysAreNotMapped() {
        assertNull(HardwareHangulKeyMap.charFor(KeyEvent.KEYCODE_1, false))
        assertNull(HardwareHangulKeyMap.charFor(KeyEvent.KEYCODE_SPACE, false))
        assertNull(HardwareHangulKeyMap.charFor(KeyEvent.KEYCODE_DEL, false))
        assertNull(HardwareHangulKeyMap.charFor(KeyEvent.KEYCODE_COMMA, true))
    }
}
