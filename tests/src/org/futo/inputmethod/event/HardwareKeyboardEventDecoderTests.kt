package org.futo.inputmethod.event

import android.view.KeyEvent
import androidx.test.filters.SmallTest
import androidx.test.runner.AndroidJUnit4
import org.futo.inputmethod.latin.common.Constants
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class HardwareKeyboardEventDecoderTests {
    private fun keyDown(keyCode: Int, metaState: Int = 0) =
        KeyEvent(0L, 0L, KeyEvent.ACTION_DOWN, keyCode, 0, metaState)

    @Test
    fun testLetterUsesTheSystemCharacterWithoutKeyMap() {
        val event = HardwareKeyboardEventDecoder(0).decodeHardwareKey(keyDown(KeyEvent.KEYCODE_R))
        assertEquals('r'.code, event.mCodePoint)
    }

    @Test
    fun testLetterUsesTheKeyMap() {
        val decoder = HardwareKeyboardEventDecoder(0, HardwareHangulKeyMap)
        assertEquals('ㄱ'.code, decoder.decodeHardwareKey(keyDown(KeyEvent.KEYCODE_R)).mCodePoint)
        assertEquals('ㄲ'.code, decoder.decodeHardwareKey(
            keyDown(KeyEvent.KEYCODE_R, KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON)).mCodePoint)
    }

    @Test
    fun testKeyOutsideTheKeyMapUsesTheSystemCharacter() {
        val decoder = HardwareKeyboardEventDecoder(0, HardwareHangulKeyMap)
        assertEquals('1'.code, decoder.decodeHardwareKey(keyDown(KeyEvent.KEYCODE_1)).mCodePoint)
        assertEquals(Constants.CODE_DELETE, decoder.decodeHardwareKey(keyDown(KeyEvent.KEYCODE_DEL)).mKeyCode)
    }
}
