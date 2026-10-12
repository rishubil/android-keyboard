package org.futo.inputmethod.engine

import android.content.Context
import android.view.inputmethod.EditorInfo
import androidx.datastore.preferences.core.booleanPreferencesKey
import org.futo.inputmethod.latin.R
import org.futo.inputmethod.latin.uix.SettingsKey
import org.futo.inputmethod.latin.uix.getSetting
import org.futo.inputmethod.latin.uix.settings.UserSettingsMenu
import org.futo.inputmethod.latin.uix.settings.useDataStore
import org.futo.inputmethod.latin.uix.settings.userSettingToggleDataStore

object KoreanIMESettings {
    val TermuxCommitSyllables = SettingsKey(
        booleanPreferencesKey("ime_ko_termux_commit_syllables"),
        false
    )

    val menu = UserSettingsMenu(
        title = R.string.korean_settings_title,
        navPath = "ime/ko", registerNavPath = true,
        settings = listOf(
            userSettingToggleDataStore(
                title = R.string.keyboard_settings_hardware_hangul_key_switches_language,
                subtitle = R.string.keyboard_settings_hardware_hangul_key_switches_language_subtitle,
                setting = HardwareKeyboardHangulKeySwitchesLanguage
            ),
            userSettingToggleDataStore(
                title = R.string.keyboard_settings_hardware_shift_space_switches_language,
                subtitle = R.string.keyboard_settings_hardware_shift_space_switches_language_subtitle,
                setting = HardwareKeyboardShiftSpaceSwitchesLanguage
            ),
            userSettingToggleDataStore(
                title = R.string.keyboard_settings_hardware_right_alt_switches_language,
                subtitle = R.string.keyboard_settings_hardware_right_alt_switches_language_subtitle,
                setting = HardwareKeyboardRightAltSwitchesLanguage
            ),
            userSettingToggleDataStore(
                title = R.string.keyboard_settings_hardware_disable_altgr,
                subtitle = R.string.keyboard_settings_hardware_disable_altgr_subtitle,
                setting = HardwareKeyboardDisableAltGr,
                disabled = { !useDataStore(HardwareKeyboardRightAltSwitchesLanguage).value }
            ),
            userSettingToggleDataStore(
                title = R.string.korean_settings_toggle_termux_commit_syllables,
                subtitle = R.string.korean_settings_toggle_termux_commit_syllables_subtitle,
                setting = TermuxCommitSyllables
            )
        )
    )

    fun commitsCompletedSyllables(packageName: String?, settingEnabled: Boolean): Boolean =
        settingEnabled && packageName?.startsWith("com.termux") == true

    /**
     * Whether Hangul syllables are committed as soon as they are complete in this editor, instead
     * of when the word ends. Termux does not show composing text, so without this the word stays
     * invisible until a space or Enter.
     */
    @JvmStatic
    fun commitsCompletedSyllables(context: Context, editorInfo: EditorInfo?): Boolean =
        commitsCompletedSyllables(editorInfo?.packageName, context.getSetting(TermuxCommitSyllables))
}
