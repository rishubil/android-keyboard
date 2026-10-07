package org.futo.inputmethod.engine

import org.futo.inputmethod.latin.R
import org.futo.inputmethod.latin.uix.settings.UserSettingsMenu
import org.futo.inputmethod.latin.uix.settings.useDataStore
import org.futo.inputmethod.latin.uix.settings.userSettingToggleDataStore

object KoreanIMESettings {
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
            )
        )
    )
}
