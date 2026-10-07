package org.futo.inputmethod.engine

import android.view.KeyEvent

/**
 * Detects a tap of a key on a hardware keyboard: the key is pressed and released with no other
 * key in between. This keeps combinations working when the key is a modifier, e.g. AltGr+E.
 */
class HardwareKeyTapDetector(private val keyCode: Int) {
    private var tapPending = false

    fun onKeyDown(keyEvent: KeyEvent) {
        if(keyEvent.keyCode == keyCode) {
            if(keyEvent.repeatCount == 0) tapPending = true
        } else {
            tapPending = false
        }
    }

    /** Returns true if this release completes a tap. */
    fun onKeyUp(keyEvent: KeyEvent): Boolean {
        val isTap = tapPending && keyEvent.keyCode == keyCode && !keyEvent.isCanceled
        if(keyEvent.keyCode == keyCode) tapPending = false
        return isTap
    }
}
