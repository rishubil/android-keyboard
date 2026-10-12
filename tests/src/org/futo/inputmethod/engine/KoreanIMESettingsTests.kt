package org.futo.inputmethod.engine

import androidx.test.filters.SmallTest
import androidx.test.runner.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class KoreanIMESettingsTests {
    @Test
    fun testSyllablesAreCommittedOnlyInTermuxWithTheSettingOn() {
        assertTrue(KoreanIMESettings.commitsCompletedSyllables("com.termux", true))
        assertFalse(KoreanIMESettings.commitsCompletedSyllables("com.termux", false))
        assertFalse(KoreanIMESettings.commitsCompletedSyllables("com.android.chrome", true))
        assertFalse(KoreanIMESettings.commitsCompletedSyllables(null, true))
    }
}
