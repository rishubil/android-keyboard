package org.futo.inputmethod.event

import android.view.KeyEvent

/**
 * Maps the letter keys of a hardware keyboard to Hangul jamo with the standard
 * Dubeolsik (KS X 5002) arrangement. Physical Korean keyboards print this
 * arrangement on top of QWERTY, whatever on-screen layout is active.
 */
object HardwareHangulKeyMap : HardwareKeyMap {
    private val jamo = mapOf(
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

    // Shift gives the tense consonants and ㅒ/ㅖ. Other keys keep their jamo with Shift.
    private val shiftedJamo = mapOf(
        KeyEvent.KEYCODE_Q to 'ㅃ', KeyEvent.KEYCODE_W to 'ㅉ', KeyEvent.KEYCODE_E to 'ㄸ',
        KeyEvent.KEYCODE_R to 'ㄲ', KeyEvent.KEYCODE_T to 'ㅆ', KeyEvent.KEYCODE_O to 'ㅒ',
        KeyEvent.KEYCODE_P to 'ㅖ'
    )

    /** Returns the jamo for a letter key, or null for any other key. */
    override fun charFor(keyCode: Int, shift: Boolean): Char? =
        (if (shift) shiftedJamo[keyCode] else null) ?: jamo[keyCode]
}
