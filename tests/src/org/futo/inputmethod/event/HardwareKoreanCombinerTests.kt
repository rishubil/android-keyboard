package org.futo.inputmethod.event

import android.view.KeyEvent
import androidx.test.filters.SmallTest
import androidx.test.runner.AndroidJUnit4
import org.futo.inputmethod.v2keyboard.CombinerKind
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class HardwareKoreanCombinerTests {
    private fun type(combinerKind: CombinerKind, vararg keyCodes: Int): String {
        val decoder = HardwareKeyboardEventDecoder(-1, combinerKind.hardwareKeyMap)
        val chain = CombinerChain(listOf(combinerKind), "")
        val previousEvents = ArrayList<Event>()
        for (keyCode in keyCodes) {
            val event = decoder.decodeHardwareKey(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
            chain.applyProcessedEvent(chain.processEvent(previousEvents, event))
            previousEvents.add(event)
        }
        return chain.getComposingWordWithCombiningFeedback().toString()
    }

    @Test
    fun testKoreanCombinersHaveTheHangulKeyMap() {
        assertEquals(HardwareHangulKeyMap, CombinerKind.Korean.hardwareKeyMap)
        assertEquals(HardwareHangulKeyMap, CombinerKind.KoreanCombineInitials.hardwareKeyMap)
        assertEquals(null, CombinerKind.DeadKey.hardwareKeyMap)
    }

    @Test
    fun testHardwareKeysComposeSyllables() {
        assertEquals("가나", type(CombinerKind.Korean,
            KeyEvent.KEYCODE_R, KeyEvent.KEYCODE_K, KeyEvent.KEYCODE_S, KeyEvent.KEYCODE_K))
        assertEquals("한글", type(CombinerKind.Korean,
            KeyEvent.KEYCODE_G, KeyEvent.KEYCODE_K, KeyEvent.KEYCODE_S,
            KeyEvent.KEYCODE_R, KeyEvent.KEYCODE_M, KeyEvent.KEYCODE_F))
    }

    @Test
    fun testBackspaceRemovesTheLastJamo() {
        assertEquals("가", type(CombinerKind.Korean,
            KeyEvent.KEYCODE_R, KeyEvent.KEYCODE_K, KeyEvent.KEYCODE_S, KeyEvent.KEYCODE_DEL))
    }
}
