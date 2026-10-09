/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.futo.inputmethod.keyboard.internal;

import static org.futo.inputmethod.latin.common.Constants.CODE_ACTION_0;
import static org.futo.inputmethod.latin.common.Constants.CODE_ACTION_MAX;
import static org.futo.inputmethod.latin.common.Constants.CODE_KEYEVENT_0;
import static org.futo.inputmethod.latin.common.Constants.CODE_UNSPECIFIED;

import android.util.Log;
import android.view.KeyEvent;

import org.futo.inputmethod.latin.common.Constants;
import org.futo.inputmethod.latin.uix.actions.ActionRegistry;
import org.futo.inputmethod.latin.uix.actions.RegistryKt;

import java.util.HashMap;
import java.util.Locale;

public final class KeyboardCodesSet {
    private static final String TAG = "KeyboardCodesSet";
    public static final String PREFIX_CODE = "!code/";
    public static final String ACTION_CODE_PREFIX = "action_";
    public static final String KEYEVENT_CODE_PREFIX = "keyevent_";

    private static final HashMap<String, Integer> sNameToIdMap = new HashMap<>();

    private KeyboardCodesSet() {
        // This utility class is not publicly instantiable.
    }

    // An unknown code (e.g. from a layout for a newer version) gives a disabled key, so that it
    // does not break the whole layout
    private static int unknownCode(final String name) {
        Log.w(TAG, "Unknown key code: " + name);
        return CODE_UNSPECIFIED;
    }

    public static int getCode(final String name) {
        if(name.startsWith(ACTION_CODE_PREFIX)) {
            final int actionId;
            try {
                actionId = ActionRegistry.INSTANCE.parseAction(name);
            } catch (IllegalArgumentException e) {
                return unknownCode(name);
            }
            // A numeric ID must name an existing action, or a press would trigger no action
            if(actionId < 0 || actionId >= RegistryKt.getAllActions().size()
                    || CODE_ACTION_0 + actionId > CODE_ACTION_MAX) {
                return unknownCode(name);
            }
            return CODE_ACTION_0 + actionId;
        }
        if(name.startsWith(KEYEVENT_CODE_PREFIX)) {
            // e.g. keyevent_escape is KeyEvent.KEYCODE_ESCAPE
            final String keyName = name.substring(KEYEVENT_CODE_PREFIX.length());
            final int keyCode = keyName.isEmpty() ? KeyEvent.KEYCODE_UNKNOWN
                    : KeyEvent.keyCodeFromString("KEYCODE_" + keyName.toUpperCase(Locale.ROOT));
            final int code = CODE_KEYEVENT_0 + keyCode;
            if (!Constants.isKeyEventCode(code)) return unknownCode(name);
            return code;
        }
        Integer id = sNameToIdMap.get(name);
        if (id == null) return unknownCode(name);
        return DEFAULT[id];
    }

    private static final String[] ID_TO_NAME = {
        "key_tab",
        "key_enter",
        "key_space",
        "key_shift",
        "key_capslock",
        "key_switch_alpha_symbol",
        "key_output_text",
        "key_delete",
        "key_settings",
        "key_shortcut",
        "key_action_next",
        "key_action_previous",
        "key_shift_enter",
        "key_language_switch",
        "key_emoji",
        "key_alpha_from_emoji",
        "key_to_number_layout",
        "key_to_alt_0_layout",
        "key_to_alt_1_layout",
        "key_to_alt_2_layout",
        "key_to_alpha_0_layout",
        "key_to_alpha_1_layout",
        "key_to_alpha_2_layout",
        "key_to_alpha_3_layout",
        "key_cursor_pad",
        "key_nav_pad",
        "key_unspecified",
    };

    private static final int[] DEFAULT = {
        Constants.CODE_TAB,
        Constants.CODE_ENTER,
        Constants.CODE_SPACE,
        Constants.CODE_SHIFT,
        Constants.CODE_CAPSLOCK,
        Constants.CODE_SWITCH_ALPHA_SYMBOL,
        Constants.CODE_OUTPUT_TEXT,
        Constants.CODE_DELETE,
        Constants.CODE_SETTINGS,
        Constants.CODE_SHORTCUT,
        Constants.CODE_ACTION_NEXT,
        Constants.CODE_ACTION_PREVIOUS,
        Constants.CODE_SHIFT_ENTER,
        Constants.CODE_LANGUAGE_SWITCH,
        Constants.CODE_EMOJI,
        Constants.CODE_ALPHA_FROM_EMOJI,
        Constants.CODE_TO_NUMBER_LAYOUT,
        Constants.CODE_TO_ALT_0_LAYOUT,
        Constants.CODE_TO_ALT_1_LAYOUT,
        Constants.CODE_TO_ALT_2_LAYOUT,
        Constants.CODE_TO_ALPHA_0_LAYOUT,
        Constants.CODE_TO_ALPHA_1_LAYOUT,
        Constants.CODE_TO_ALPHA_2_LAYOUT,
        Constants.CODE_TO_ALPHA_3_LAYOUT,
        Constants.CODE_CURSOR_PAD,
        Constants.CODE_NAV_PAD,
        Constants.CODE_UNSPECIFIED,
    };

    static {
        for (int i = 0; i < ID_TO_NAME.length; i++) {
            sNameToIdMap.put(ID_TO_NAME[i], i);
        }
    }
}
