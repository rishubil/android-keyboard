package org.futo.inputmethod.keyboard.internal

import android.graphics.Rect
import android.util.Base64
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import org.futo.inputmethod.event.Event
import org.futo.inputmethod.latin.LatinIMELegacy
import org.futo.inputmethod.latin.common.Constants
import org.futo.inputmethod.latin.uix.actions.ActionRegistry
import org.futo.inputmethod.latin.uix.actions.AllActions
import org.futo.inputmethod.latin.uix.settings.pages.CustomLayout
import org.futo.inputmethod.v2keyboard.KeyboardLayoutSetV2
import org.futo.inputmethod.v2keyboard.KeyboardLayoutSetV2Params
import org.futo.inputmethod.v2keyboard.LayoutManager
import org.futo.inputmethod.v2keyboard.RegularKeyboardSize
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

@SmallTest
@RunWith(AndroidJUnit4::class)
class KeyEventCodeTests {
    private fun code(keyCode: Int) = Constants.CODE_KEYEVENT_0 + keyCode

    @Test
    fun testKeyEventCodesResolve() {
        mapOf(
            "keyevent_escape" to KeyEvent.KEYCODE_ESCAPE,
            "keyevent_f1" to KeyEvent.KEYCODE_F1,
            "keyevent_f12" to KeyEvent.KEYCODE_F12,
            "keyevent_sysrq" to KeyEvent.KEYCODE_SYSRQ,
            "keyevent_scroll_lock" to KeyEvent.KEYCODE_SCROLL_LOCK,
            "keyevent_break" to KeyEvent.KEYCODE_BREAK,
            "keyevent_num_lock" to KeyEvent.KEYCODE_NUM_LOCK,
            "keyevent_insert" to KeyEvent.KEYCODE_INSERT,
            "keyevent_forward_del" to KeyEvent.KEYCODE_FORWARD_DEL,
            "keyevent_caps_lock" to KeyEvent.KEYCODE_CAPS_LOCK,
            "keyevent_tab" to KeyEvent.KEYCODE_TAB,
            "keyevent_ctrl_left" to KeyEvent.KEYCODE_CTRL_LEFT,
            "keyevent_ctrl_right" to KeyEvent.KEYCODE_CTRL_RIGHT,
            "keyevent_alt_left" to KeyEvent.KEYCODE_ALT_LEFT,
            "keyevent_alt_right" to KeyEvent.KEYCODE_ALT_RIGHT,
            "keyevent_meta_left" to KeyEvent.KEYCODE_META_LEFT,
            "keyevent_dpad_up" to KeyEvent.KEYCODE_DPAD_UP,
            "keyevent_dpad_down" to KeyEvent.KEYCODE_DPAD_DOWN,
            "keyevent_dpad_left" to KeyEvent.KEYCODE_DPAD_LEFT,
            "keyevent_dpad_right" to KeyEvent.KEYCODE_DPAD_RIGHT,
            "keyevent_page_up" to KeyEvent.KEYCODE_PAGE_UP,
            "keyevent_page_down" to KeyEvent.KEYCODE_PAGE_DOWN,
            "keyevent_move_home" to KeyEvent.KEYCODE_MOVE_HOME,
            "keyevent_move_end" to KeyEvent.KEYCODE_MOVE_END,
        ).forEach { (name, keyCode) ->
            val code = KeyboardCodesSet.getCode(name)
            assertEquals(name, code(keyCode), code)
            assertTrue(name, Constants.isKeyEventCode(code))
        }
    }

    @Test
    fun testUnknownCodesAreUnspecified() {
        // A layout for a newer version must not break the whole page, so unknown codes give a
        // disabled key instead of an exception
        listOf(
            "keyevent_", "keyevent_nonsense", "keyevent_unknown", "keyevent_-5", "keyevent_5000",
            "key_from_the_future", "", "action_no_such_action", "action_99", "action_-1",
            "action_${AllActions.size}",
        ).forEach {
            assertEquals(it, Constants.CODE_UNSPECIFIED, KeyboardCodesSet.getCode(it))
        }
        assertEquals(Constants.CODE_UNSPECIFIED, KeySpecParser.getCode("Future|!code/key_from_the_future"))

        // Known actions keep working, by name and by number
        assertEquals(Constants.CODE_ACTION_0 + ActionRegistry.parseAction("emoji"),
            KeyboardCodesSet.getCode("action_emoji"))
        assertEquals(Constants.CODE_ACTION_0 + AllActions.size - 1,
            KeyboardCodesSet.getCode("action_${AllActions.size - 1}"))
    }

    @Test
    fun testLayoutWithUnknownCodesStillBuilds() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        LayoutManager.init(context)

        val layoutYaml = """
            name: Unknown Codes Test
            rows:
              - letters:
                  - {type: base, spec: "Future|!code/key_from_the_future"}
                  - {type: base, spec: "Esc|!code/keyevent_escape", moreKeys: "Bad|!code/keyevent_nonsense,Ins|!code/keyevent_insert"}
                  - {type: base, spec: "Act|!code/action_99"}
                  - {type: base, spec: "q"}
        """.trimIndent()
        val customLayout = Json.encodeToString(CustomLayout.serializer(), CustomLayout("en", layoutYaml))
        val encoded = Base64.encodeToString(customLayout.toByteArray(), Base64.NO_WRAP).replace("=", "_")

        val layoutSet = KeyboardLayoutSetV2(context, KeyboardLayoutSetV2Params(
            computedSize = RegularKeyboardSize(1024, 1024, Rect()),
            keyboardLayoutSet = "qwerty",
            locale = Locale.ENGLISH,
            editorInfo = EditorInfo().apply {
                privateImeOptions = "org.futo.inputmethod.latin.ForceCustomLayoutYamlB64=$encoded"
            },
            numberRow = false,
            arrowRow = false,
            bottomActionKey = null,
            multilingualTypingLocales = emptyList(),
            numberRowMode = 0,
            useLocalNumbers = false,
            alternativePeriodKey = false
        ))
        val keys = layoutSet.getKeyboard(
            KeyboardLayoutElement(kind = KeyboardLayoutKind.Alphabet0, page = KeyboardLayoutPage.Base)
        ).sortedKeys.associateBy { it.label }

        assertEquals(Constants.CODE_UNSPECIFIED, keys["Future"]!!.code)
        assertFalse(keys["Future"]!!.isEnabled)
        assertEquals(Constants.CODE_UNSPECIFIED, keys["Act"]!!.code)
        assertEquals('q'.code, keys["q"]!!.code)

        val esc = keys["Esc"]!!
        assertEquals(code(KeyEvent.KEYCODE_ESCAPE), esc.code)
        assertEquals(listOf(Constants.CODE_UNSPECIFIED, code(KeyEvent.KEYCODE_INSERT)),
            esc.moreKeys.map { it.mCode }.take(2))
    }

    @Test
    fun testRangeDoesNotCollide() {
        assertTrue(Constants.CODE_KEYEVENT_MAX < Constants.CODE_ALT_ACTION_0)
        assertTrue(Constants.CODE_KEYEVENT_0 + KeyEvent.getMaxKeyCode() <= Constants.CODE_KEYEVENT_MAX)
        listOf(
            Constants.CODE_SHIFT, Constants.CODE_DELETE, Constants.CODE_UNSPECIFIED,
            Constants.CODE_ACTION_0, Constants.CODE_ACTION_MAX,
            Constants.CODE_ALT_ACTION_0, Constants.CODE_ALT_ACTION_MAX,
            Constants.CODE_KEYEVENT_0, 'a'.code, 0,
        ).forEach { assertFalse("$it", Constants.isKeyEventCode(it)) }
    }

    @Test
    fun testKeySpecWithKeyEventCode() {
        val spec = "Esc|!code/keyevent_escape"
        assertEquals("Esc", KeySpecParser.getLabel(spec))
        assertEquals(code(KeyEvent.KEYCODE_ESCAPE), KeySpecParser.getCode(spec))
        assertNull(KeySpecParser.getOutputText(spec))
    }

    @Test
    fun testMoreKeySpecWithKeyEventCode() {
        listOf(false, true).forEach { needsToUpperCase ->
            val spec = MoreKeySpec("Ins|!code/keyevent_insert", needsToUpperCase, Locale.US)
            assertEquals(code(KeyEvent.KEYCODE_INSERT), spec.mCode)
            assertEquals(if(needsToUpperCase) "INS" else "Ins", spec.mLabel)
            assertNull(spec.mOutputText)
        }
    }

    @Test
    fun testSoftwareKeypressEventKeepsKeyEventCode() {
        val code = code(KeyEvent.KEYCODE_F5)
        val event = LatinIMELegacy.createSoftwareKeypressEvent(code, 0, 0, false)
        assertEquals(code, event.mKeyCode)
        assertEquals(Event.NOT_A_CODE_POINT, event.mCodePoint)
        assertTrue(event.isFunctionalKeyEvent)
    }

    @Test
    fun testPadCodesResolve() {
        assertEquals(Constants.CODE_CURSOR_PAD, KeyboardCodesSet.getCode("key_cursor_pad"))
        assertEquals(Constants.CODE_NAV_PAD, KeyboardCodesSet.getCode("key_nav_pad"))
        assertEquals(Constants.CODE_CURSOR_PAD, KeySpecParser.getCode("Cur|!code/key_cursor_pad"))
        assertEquals("Cur", KeySpecParser.getLabel("Cur|!code/key_cursor_pad"))
        assertEquals(Constants.CODE_NAV_PAD,
            MoreKeySpec("Nav|!code/key_nav_pad", false, Locale.US).mCode)
        // Unknown names still fall back
        assertEquals(Constants.CODE_UNSPECIFIED, KeyboardCodesSet.getCode("key_other_pad"))
    }

    @Test
    fun testPadCodesDoNotCollide() {
        val pads = listOf(Constants.CODE_CURSOR_PAD, Constants.CODE_NAV_PAD)
        pads.forEach {
            assertTrue(Constants.isPadCode(it))
            assertTrue(it < 0)
            assertFalse(Constants.isKeyEventCode(it))
            assertFalse(Constants.isLetterCode(it))
            assertFalse(it >= Constants.CODE_ACTION_0 && it <= Constants.CODE_ACTION_MAX)
            assertFalse(it >= Constants.CODE_ALT_ACTION_0 && it <= Constants.CODE_ALT_ACTION_MAX)
        }
        assertFalse(Constants.CODE_CURSOR_PAD == Constants.CODE_NAV_PAD)
        // No other named code uses them
        val names = listOf(
            "key_tab", "key_enter", "key_space", "key_shift", "key_capslock", "key_switch_alpha_symbol",
            "key_output_text", "key_delete", "key_settings", "key_shortcut", "key_action_next",
            "key_action_previous", "key_shift_enter", "key_language_switch", "key_emoji",
            "key_alpha_from_emoji", "key_to_number_layout", "key_to_alt_0_layout", "key_to_alt_1_layout",
            "key_to_alt_2_layout", "key_to_alpha_0_layout", "key_to_alpha_1_layout",
            "key_to_alpha_2_layout", "key_to_alpha_3_layout", "key_unspecified",
        )
        names.forEach { assertFalse(it, Constants.isPadCode(KeyboardCodesSet.getCode(it))) }
        listOf(Constants.CODE_SYMBOL_SHIFT, Constants.CODE_OUTPUT_TEXT_WITH_SPACES, Constants.CODE_UNSPECIFIED,
            Constants.CODE_KEYEVENT_0 + KeyEvent.KEYCODE_DPAD_LEFT).forEach {
            assertFalse("$it", Constants.isPadCode(it))
        }
    }
}
