package org.futo.inputmethod.event

/**
 * Maps a hardware keyboard key to the character that a combiner expects, for scripts whose
 * characters the system key character map cannot produce (e.g. Hangul jamo for composition).
 * See [org.futo.inputmethod.v2keyboard.CombinerKind.hardwareKeyMap].
 */
fun interface HardwareKeyMap {
    /** Returns the character for the key, or null to keep the character from the system. */
    fun charFor(keyCode: Int, shift: Boolean): Char?
}
