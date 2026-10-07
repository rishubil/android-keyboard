package org.futo.inputmethod.v2keyboard

import org.futo.inputmethod.event.Combiner
import org.futo.inputmethod.event.DeadKeyCombiner
import org.futo.inputmethod.event.HardwareHangulKeyMap
import org.futo.inputmethod.event.HardwareKeyMap
import org.futo.inputmethod.event.combiners.NFCNormalizingCombiner
import org.futo.inputmethod.event.combiners.DeadKeyPreCombiner
import org.futo.inputmethod.event.combiners.KoashurCombiner
import org.futo.inputmethod.event.combiners.KoreanCombiner
import org.futo.inputmethod.event.combiners.vietnamese.VNICombiner
import org.futo.inputmethod.event.combiners.vietnamese.VietTelexCombiner
import org.futo.inputmethod.event.combiners.wylie.WylieCombiner

/**
 * @param hardwareKeyMap If set, a hardware keyboard types through this combiner, with keys
 * mapped by [hardwareKeyMap] (see GeneralIME.onHardwareKeyDown). Otherwise the system handles
 * hardware keys.
 */
enum class CombinerKind(val factory: () -> Combiner, val hardwareKeyMap: HardwareKeyMap? = null) {
    DeadKey({ DeadKeyCombiner() }),
    DeadKeyPreCombiner({ DeadKeyPreCombiner() }),
    NFCNormalize({ NFCNormalizingCombiner() }),
    Korean({ KoreanCombiner() }, HardwareHangulKeyMap),
    KoreanCombineInitials({ KoreanCombiner(combineInitials = true) }, HardwareHangulKeyMap),
    VietTelex( { VietTelexCombiner() }),
    VNI( { VNICombiner() }),
    Wylie({ WylieCombiner() }),
    Koashur({ KoashurCombiner() }),
}