# Eta Mod

[简体中文](README.md) | **English**

A personal mod of [Mangi-11/Eta](https://github.com/Mangi-11/Eta). It **installs alongside** the official app without conflicts (applicationId `io.github.mangi.eta.mod`, app name "Eta Mod", separate signature).

<p><a href="https://github.com/Number444/Eta/releases"><img src="https://img.shields.io/github/downloads/Number444/Eta/total?logo=github&amp;label=Downloads&amp;color=1677FF" alt="GitHub Releases downloads"></a> <img src="https://img.shields.io/badge/minSdk-33-3DDC84?logo=android&amp;logoColor=white" alt="minSdk 33"> <img src="https://img.shields.io/badge/Kotlin-2.4.20-7F52FF?logo=kotlin&amp;logoColor=white" alt="Kotlin 2.4.20"> <img src="https://img.shields.io/badge/AGP-9.4.1-3DDC84?logo=android&amp;logoColor=white" alt="AGP 9.4.1"></p>

**[Download APK](https://github.com/Number444/Eta/releases)** · In-app "Settings → Check for updates" tracks this repository's releases and opens the APK directly in the default browser when a newer release has exactly one APK asset.

**Version scheme**: `upstream version - three-digit mod revision`, e.g. `3.3.0-008`. The left part tracks the upstream release; the right part increments per mod iteration and resets after each upstream merge.

## Mod Features

Main changes relative to the official app:

1. **Super Island run controls** (Xiaomi HyperOS): task status is mirrored to the island in real time. Pause/resume, supplement and stop actions render directly on the island, while the floating capsule stays display-only and does not swallow the agent's taps. Supplement uses a floating input card, then follows the official interruption behavior: cancel the current turn and send the new message.
2. **Liquid-glass UI**: shared material tokens, backdrop sources and popup architecture across home, the conversation pane and Settings, with three blur modes and light/dark themes.
3. **Thinking & compaction control**: auto-expanding thinking, default reasoning depth, a dedicated compaction model, and provider-specific reasoning handling for compaction.
4. **Provider balance display**: API-key providers can configure a balance endpoint and JSON path, shown beside the provider name in the model picker.
5. **Conversation management & timing**: delete conversations or create temporary sessions that self-destruct on exit without touching the database. Each reply shows its finish time and duration.
6. **Share-as-image rewrite**: tables and code blocks render correctly through the current markdown layer, with haptic and success/failure feedback.
7. **Configuration compatibility**: preserves existing Mod database/settings upgrades and migrates legacy DeepSeek search credentials into the official encrypted store. The former Exa path has been removed in favor of the official web-tool architecture.

The current upstream baseline is **v3.3.3**, including Codex/Grok subscription login, attachment-aware queues and interruption, keep-screen-on, Gemini/MiniMax speech, configurable web search/fetch, and ARM64-only packaging.

## Screenshots (Mod)

| Overflow menu: temporary session / delete (with per-turn timing) | Temporary session (input-area banner) |
| :--------------------------------------------------------------: | :-----------------------------------: |
| ![Overflow menu](docs/Screenshots/mod_menu_timing.jpg) | ![Temporary session](docs/Screenshots/mod_temp_conversation.jpg) |

| Model & chat settings (Mod area) | Settings home |
| :------------------------------: | :-----------: |
| ![Model settings](docs/Screenshots/mod_model_settings.png) | ![Settings](docs/Screenshots/mod_settings.png) |

## Upstream Capabilities

Eta itself is a system-level AI agent for Android: cross-app GUI operation, terminals (Android user/root shell, Alpine / Debian Linux), a built-in browser, file tools, Skills and MCP extensions, personal context retrieval, long-term memory, character cards, and system assistant entries through Xposed. Use your own model API key (BYOK) or sign in with a Codex (ChatGPT) or Grok subscription; built-in providers and custom services are supported.

See the upstream repository [Mangi-11/Eta](https://github.com/Mangi-11/Eta) and its [README](https://github.com/Mangi-11/Eta/blob/main/README_EN.md) for the full introduction, documentation and design notes.

| GUI Agent | Breeno BYOK (upstream demos) |
| :-------: | :--------------------------: |
| <img src="docs/Screenshots/demo_gui_agent.gif" width="320" alt="Eta GUI Agent demo"> | <img src="docs/Screenshots/demo_tools.gif" width="320" alt="Launching Eta tasks from Breeno"> |

## Documentation in This Fork

- [Island-notification investigation](docs/ISLAND-EXPANSION-INVESTIGATION.md): complete mapping results for Xiaomi Super Island / Live Updates (the basis of this mod's island features).
- [Current TODO](TODO-MOD.md): active, planned and paused items; completed work is recorded in git history and topic documents.
- Upstream docs: [Agent Runtime](docs/AGENT_RUNTIME.md) · [Technical](docs/TECHNICAL.md) · [Device support & permission boundaries](docs/ROOTLESS_SUPPORT.md) · [HyperOS system entry](docs/HYPEROS_SYSTEM_ENTRY.md) · [Terminal native components](docs/TERMINAL_NATIVE.md)

## Credits & License

- Upstream author [Mangi-11](https://github.com/Mangi-11) and all of Eta's reference projects (see the upstream README).
- This repository follows the upstream [PolyForm Noncommercial License 1.0.0](LICENSE): redistribution, resale, paid installation services and any other commercial use are prohibited without the upstream author's written permission.
