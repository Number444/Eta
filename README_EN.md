# Eta Mod

[简体中文](README.md) | **English**

A personal mod of [Mangi-11/Eta](https://github.com/Mangi-11/Eta). It **installs alongside** the official app without conflicts (applicationId `io.github.mangi.eta.mod`, app name "Eta Mod", separate signature).

<p><a href="https://github.com/Number444/Eta/releases"><img src="https://img.shields.io/github/downloads/Number444/Eta/total?logo=github&amp;label=Downloads&amp;color=1677FF" alt="GitHub Releases downloads"></a> <img src="https://img.shields.io/badge/minSdk-33-3DDC84?logo=android&amp;logoColor=white" alt="minSdk 33"> <img src="https://img.shields.io/badge/Kotlin-2.4.20-7F52FF?logo=kotlin&amp;logoColor=white" alt="Kotlin 2.4.20"> <img src="https://img.shields.io/badge/AGP-9.4.1-3DDC84?logo=android&amp;logoColor=white" alt="AGP 9.4.1"></p>

**[Download APK](https://github.com/Number444/Eta/releases)** · In-app "Settings → Check for updates" tracks this repository's releases.

**Version scheme**: `upstream version - three-digit mod revision`, e.g. `3.3.0-008`. The left part tracks the upstream release; the right part increments per mod iteration and resets after each upstream merge.

## Mod Features

All changes relative to the official app:

1. **Super Island run controls** (Xiaomi HyperOS): task status (thinking / responding / tool calls) is mirrored to the island in real time. During GUI runs, pause/resume, supplement and stop buttons render directly on the island, while the floating capsule becomes display-only and no longer swallows the agent's taps on apps underneath. "Supplement" submits through a floating input card (Live Updates have no input-field mapping, verified on-device). During foreground GUI control the island reads "Taking over"; completions expand the island once with a banner notification, while manual stops/failures stay silent. Enable "floating notifications" for the "Task status island" channel in system notification settings.
2. **Thinking & compaction control**: toggle for auto-expanding thinking streams, manual default thinking depth, a dedicated compaction/summary model, and compaction requests that explicitly strip reasoning parameters per provider protocol.
3. **Provider balance display**: expandable "balance query" per provider (API URL + JSON path, off by default, with a test button); the balance appears next to the provider name in the model picker, tinted with the theme accent color.
4. **Exa-only search**: `web_search` keeps Exa only (bring your own API key) — the official key-free search times out on this device and is no longer registered (upstream code retained); the official `fetch_url` tool is unaffected.
5. **Conversation management**: a red "Delete conversation" entry (with confirmation) in the top-right overflow menu, and "New temporary conversation" — a full-capability session that auto-destructs when you leave or switch away, never touches the database, and shows a banner above the input bar.
6. **Per-turn timing**: each reply shows its finish time and duration below the action row, e.g. `14:03 · Time: 8.6s`, rendered as "Xm Xs" beyond 60 seconds.
7. **Message queueing**: queue messages while the model is running; send anytime.
8. **Share-as-image rewrite**: rebuilt on the new markdown rendering layer — tables and code blocks render correctly, with haptic feedback and success/failure indication on the share button.

## Screenshots (Mod)

| Overflow menu: temporary session / delete (with per-turn timing) | Temporary session (input-area banner) |
| :--------------------------------------------------------------: | :-----------------------------------: |
| ![Overflow menu](docs/Screenshots/mod_menu_timing.jpg) | ![Temporary session](docs/Screenshots/mod_temp_conversation.jpg) |

| Model & chat settings (Mod area) | Settings home |
| :------------------------------: | :-----------: |
| ![Model settings](docs/Screenshots/mod_model_settings.png) | ![Settings](docs/Screenshots/mod_settings.png) |

## Upstream Capabilities

Eta itself is a system-level AI agent for Android: cross-app GUI operation, terminals (Android user/root shell, Alpine / Debian Linux), a built-in browser, file tools, Skills and MCP extensions, personal context retrieval, long-term memory, character cards, and system assistant entries (power button / Breeno / Super XiaoAi takeover via Xposed). AI features require your own model API key (BYOK), with built-in presets for OpenAI, Anthropic, Alibaba Bailian, DeepSeek, Kimi and more, plus custom providers.

See the upstream repository [Mangi-11/Eta](https://github.com/Mangi-11/Eta) and its [README](https://github.com/Mangi-11/Eta/blob/main/README_EN.md) for the full introduction, documentation and design notes.

| GUI Agent | Breeno BYOK (upstream demos) |
| :-------: | :--------------------------: |
| <img src="docs/Screenshots/demo_gui_agent.gif" width="320" alt="Eta GUI Agent demo"> | <img src="docs/Screenshots/demo_tools.gif" width="320" alt="Launching Eta tasks from Breeno"> |

## Documentation in This Fork

- [Island-notification investigation](docs/ISLAND-EXPANSION-INVESTIGATION.md): complete mapping results for Xiaomi Super Island / Live Updates (the basis of this mod's island features).
- [TODO & merge decisions](TODO-MOD.md): roadmap, completed items and upstream merge rulings.
- Upstream docs: [Agent Runtime](docs/AGENT_RUNTIME.md) · [Technical](docs/TECHNICAL.md) · [Device support & permission boundaries](docs/ROOTLESS_SUPPORT.md) · [HyperOS system entry](docs/HYPEROS_SYSTEM_ENTRY.md) · [Terminal native components](docs/TERMINAL_NATIVE.md)

## Credits & License

- Upstream author [Mangi-11](https://github.com/Mangi-11) and all of Eta's reference projects (see the upstream README).
- This repository follows the upstream [PolyForm Noncommercial License 1.0.0](LICENSE): redistribution, resale, paid installation services and any other commercial use are prohibited without the upstream author's written permission.
