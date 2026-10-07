# FUTO Keyboard

> [!NOTE]
> **About the `0.1.30-nesswit` branch**
>
> This branch is a personal build of FUTO Keyboard for my own devices. It is
> not an official FUTO release, and FUTO does not support it. It starts from
> the upstream `0.1.30` tag and adds these changes:
>
> | Change | Source |
> |---|---|
> | Configurable flick gesture sensitivity (Long-Press Keys & Spacebar settings) | `feat/flick-gesture-sensitivity` (one commit), cherry-picked |
> | Hangul input from hardware keyboards, through the same Korean combiner as the on-screen keyboard, and language switch keys: the Hangul (한/영) key, Shift+Space, and Right Alt, each with a setting, plus an option to disable AltGr when Right Alt switches the language | `feat/hardware-keyboard-hangul` (one commit), cherry-picked |
> | Suggestion toolbar while a hardware keyboard hides the touch keyboard | [PR #2138](https://github.com/futo-org/android-keyboard/pull/2138) (`pr-2138`, two commits), merged as a branch |
> | "Guest 한/영" toolbar action: sends raw Shift+Space key events (with Linux scan codes) to the Linux VM display of the Android Terminal app (AVF), so that the guest input method switches between Korean and English | `feat/avf-shift-space` (one commit), cherry-picked |
> | Option to send the hardware 한/영 key, Right Alt and Shift+Space unhandled to the AVF Linux display, so that the guest input method gets them | `feat/avf-hardware-key-passthrough` (last commit), cherry-picked |
> | Option to send the touch keyboard's keys to the AVF Linux display as hardware key events with Linux scan codes, so that shortcuts such as Ctrl+W work there; `!code/keyevent_*` layout keys, sticky Ctrl/Alt/Super keys, and cursor and navigation pad keys (`!code/key_cursor_pad`, `!code/key_nav_pad`) | `feat/avf-soft-key-passthrough` (last commit), cherry-picked |
>
> The feature branches are based on upstream `master`, so that they can be
> proposed upstream. Each one holds a single commit, except as noted. The history of this
> branch, from oldest to newest:
>
> 1. The upstream `0.1.30` tag.
> 2. The `feat/flick-gesture-sensitivity` commit, cherry-picked.
> 3. The `feat/hardware-keyboard-hangul` commit, cherry-picked.
> 4. A merge of `pr-2138`, fetched from `pull/2138/head` of the upstream
>    repository. In `strings-uix.xml` and `Typing.kt`, both sides add
>    settings after "Hide when USB keyboard is detected". The merge keeps all
>    of them: the toolbar toggle first, then the language switch key toggles.
> 5. The `feat/avf-shift-space` commit, cherry-picked.
> 6. The last commit of `feat/avf-hardware-key-passthrough`, cherry-picked.
>    That branch is `feat/hardware-keyboard-hangul` with the
>    `feat/avf-shift-space` commit cherry-picked, plus this commit, because
>    the option needs the code of both.
> 7. The last commit of `feat/avf-soft-key-passthrough`, cherry-picked. That
>    branch is `feat/avf-hardware-key-passthrough` plus this commit, because
>    the option reuses its AVF check and setting.
> 8. This note.
>
> When a feature branch changes, rebuild this branch in the same order from
> `0.1.30` instead of adding fix-up commits.
>
> Release builds of this branch use `VERSION_NAME=0.1.30-nesswit` and
> `VERSION_CODE=11751` (the `git rev-list --first-parent --count` value of
> `0.1.30`). Without a personal `keystore.properties`, the build signs them with the
> public debug key, so they cannot update an official FUTO Keyboard install.
> The custom layouts used for testing are in
> [rishubil/nesswit-futo-layout](https://github.com/rishubil/nesswit-futo-layout).
>
> The rest of this README is the upstream README.

The goal is to make a good modern keyboard that stays offline and doesn't spy on you. This keyboard is a fork of [LatinIME, The Android Open-Source Keyboard](https://android.googlesource.com/platform/packages/inputmethods/LatinIME), with significant changes made to it.

Check out the [FUTO Keyboard website](https://keyboard.futo.tech/) for downloads and more information.

The code is licensed under the [FUTO Source First License 1.1](LICENSE.md).

## Issue tracking and contributing

Please check the GitHub repository to report issues: [https://github.com/futo-org/android-keyboard/](https://github.com/futo-org/android-keyboard/)

The source code is hosted on our [internal GitLab](https://gitlab.futo.org/keyboard/latinime) and mirrored to [GitHub](https://github.com/futo-org/android-keyboard/). As registration is closed on our internal GitLab, we use GitHub instead for issues and pull requests.

Due to custom license, pull requests to this repository require signing a [CLA](https://cla.futo.org/) which you can do after opening a PR. Contributions to the [layouts repo](https://github.com/futo-org/futo-keyboard-layouts) don't require CLA as they're Apache-2.0

If you want to help translate the app, please do so via our Pontoon instance: https://i18n-keyboard.futo.org/

## Layouts

If you want to contribute layouts, check out the [layouts repo](https://github.com/futo-org/futo-keyboard-layouts).

## Building

When cloning the repository, you must perform a recursive clone to fetch all dependencies:
```
git clone --recursive https://gitlab.futo.org/keyboard/latinime.git
```

If you forgot to specify recursive clone, use this to fetch submodules:
```
git submodule update --init --recursive
```

You can then open the project in Android Studio and build it that way, or use gradle commands:
```
./gradlew assembleUnstableDebug
./gradlew assembleStableRelease
```

## APK signing

For official FUTO Keyboard versions, you can verify the APK's signing key fingerprint for integrity.

```
Signing key fingerprint for all versions except Google Play:

MD5: 3A:BB:71:C6:BB:E4:92:27:B1:E3:5D:81:01:48:6A:B0
SHA1: 5D:15:B3:6E:C9:6A:96:28:41:09:DD:62:93:0D:9C:39:9F:5F:06:43
SHA-256: 74:3F:AD:58:64:AB:C4:26:50:0B:2D:C2:C4:7C:8A:D3:24:CB:CD:16:03:3F:80:16:99:48:41:35:63:74:F9:95

```
