package org.futo.inputmethod.event

import androidx.test.filters.SmallTest
import androidx.test.runner.AndroidJUnit4
import org.futo.inputmethod.latin.common.Constants
import org.futo.inputmethod.v2keyboard.CombinerKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class KoreanCombinerCompletedSyllablesTests {
    /** What the editor holds: the committed text and the composing text. */
    private data class Result(val committed: String, val composing: String)

    /**
     * Types [keys] like InputLogic does when it commits completed syllables: after each key, the
     * completed text leaves the chain and goes to the editor. [Constants.CODE_DELETE] is backspace.
     */
    private fun type(vararg keys: Int, combinerKind: CombinerKind = CombinerKind.Korean): Result {
        val chain = CombinerChain(listOf(combinerKind), "")
        val previousEvents = ArrayList<Event>()
        val committed = StringBuilder()
        for (key in keys) {
            val event = if (key == Constants.CODE_DELETE) {
                Event.createSoftwareKeypressEvent(Constants.NOT_A_CODE, Constants.CODE_DELETE,
                    Constants.NOT_A_COORDINATE, Constants.NOT_A_COORDINATE, false)
            } else {
                Event.createSoftwareKeypressEvent(key, Event.NOT_A_KEY_CODE,
                    Constants.NOT_A_COORDINATE, Constants.NOT_A_COORDINATE, false)
            }
            chain.applyProcessedEvent(chain.processEvent(previousEvents, event))
            previousEvents.add(event)
            committed.append(chain.takeCompletedText())
        }
        return Result(committed.toString(), chain.getComposingWordWithCombiningFeedback().toString())
    }

    private fun jamo(text: String): IntArray = text.map { it.code }.toIntArray()

    @Test
    fun testSyllableIsCommittedWhenTheNextOneStarts() {
        assertEquals(Result("", "한"), type(*jamo("ㅎㅏㄴ")))
        assertEquals(Result("한", "ㄱ"), type(*jamo("ㅎㅏㄴㄱ")))
        assertEquals(Result("한", "글"), type(*jamo("ㅎㅏㄴㄱㅡㄹ")))
    }

    @Test
    fun testFinalConsonantMovesToTheNextSyllableBeforeCommit() {
        assertEquals(Result("하", "나"), type(*jamo("ㅎㅏㄴㅏ")))
        assertEquals(Result("", "닭"), type(*jamo("ㄷㅏㄹㄱ")))
        assertEquals(Result("달", "가"), type(*jamo("ㄷㅏㄹㄱㅏ")))
        assertEquals(Result("닭", "ㄷ"), type(*jamo("ㄷㅏㄹㄱㄷ")))
    }

    @Test
    fun testMergedVowelsAndDoubledInitialsStayComposing() {
        assertEquals(Result("", "와"), type(*jamo("ㅇㅗㅏ")))
        assertEquals(Result("", "ㄲ"),
            type(*jamo("ㄱㄱ"), combinerKind = CombinerKind.KoreanCombineInitials))
        assertEquals(Result("", "까"),
            type(*jamo("ㄱㄱㅏ"), combinerKind = CombinerKind.KoreanCombineInitials))
    }

    @Test
    fun testJamoThatCannotFormASyllableAreCommitted() {
        assertEquals(Result("ㄱ", "ㄴ"), type(*jamo("ㄱㄴ")))
        assertEquals(Result("ㅏ", ""), type(*jamo("ㅏ")))
    }

    @Test
    fun testBackspaceOnlyEditsTheComposingSyllable() {
        assertEquals(Result("한", "그"), type(*jamo("ㅎㅏㄴㄱㅡㄹ"), Constants.CODE_DELETE))
        assertEquals(Result("한", ""), type(*jamo("ㅎㅏㄴㄱ"), Constants.CODE_DELETE))
    }

    @Test
    fun testBackspaceAfterTheComposingSyllableIsEmptyGoesToTheEditor() {
        val chain = CombinerChain(listOf(CombinerKind.Korean), "")
        val previousEvents = ArrayList<Event>()
        for (key in jamo("ㅎㅏㄴㄱ")) {
            val event = Event.createSoftwareKeypressEvent(key, Event.NOT_A_KEY_CODE,
                Constants.NOT_A_COORDINATE, Constants.NOT_A_COORDINATE, false)
            chain.applyProcessedEvent(chain.processEvent(previousEvents, event))
            previousEvents.add(event)
            chain.takeCompletedText()
        }
        val delete = Event.createSoftwareKeypressEvent(Constants.NOT_A_CODE, Constants.CODE_DELETE,
            Constants.NOT_A_COORDINATE, Constants.NOT_A_COORDINATE, false)
        assertEquals(true, chain.processEvent(previousEvents, delete).isConsumed)
        assertFalse(chain.processEvent(previousEvents, delete).isConsumed)
    }

    @Test
    fun testWithoutTakingTheWholeWordStaysComposing() {
        val chain = CombinerChain(listOf(CombinerKind.Korean), "")
        val previousEvents = ArrayList<Event>()
        for (key in jamo("ㅎㅏㄴㄱㅡㄹ")) {
            val event = Event.createSoftwareKeypressEvent(key, Event.NOT_A_KEY_CODE,
                Constants.NOT_A_COORDINATE, Constants.NOT_A_COORDINATE, false)
            chain.applyProcessedEvent(chain.processEvent(previousEvents, event))
            previousEvents.add(event)
        }
        assertEquals("한글", chain.getComposingWordWithCombiningFeedback().toString())
    }
}
