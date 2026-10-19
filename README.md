# Eta Mod

**简体中文** | [English](README_EN.md)

[Mangi-11/Eta](https://github.com/Mangi-11/Eta) 的个人修改版，与官方版**共存安装**互不影响（applicationId `io.github.mangi.eta.mod`，应用名「Eta Mod」，独立签名）。

<p><a href="https://github.com/Number444/Eta/releases"><img src="https://img.shields.io/github/downloads/Number444/Eta/total?logo=github&amp;label=%E4%B8%8B%E8%BD%BD%E9%87%8F&amp;color=1677FF" alt="GitHub Releases 累计下载量"></a> <img src="https://img.shields.io/badge/minSdk-33-3DDC84?logo=android&amp;logoColor=white" alt="minSdk 33"> <img src="https://img.shields.io/badge/Kotlin-2.4.20-7F52FF?logo=kotlin&amp;logoColor=white" alt="Kotlin 2.4.20"> <img src="https://img.shields.io/badge/AGP-9.4.1-3DDC84?logo=android&amp;logoColor=white" alt="AGP 9.4.1"></p>

**[下载 APK](https://github.com/Number444/Eta/releases)** · App 内「设置 → 检查更新」直接检测本仓库 Release。

**版本号规则**：`官方版本号-三位 Mod 修订号`，如 `3.3.0-008`——左段跟随上游官方版本，右段为本 Mod 的迭代号；跟随上游升级后右段归零重排。

## Mod 特性

相对官方版的全部改动：

1. **超级岛运行控制**：任务状态（思考中 / 输出中 / 工具调用）实时上岛；GUI 运行期间暂停/继续、补充、停止三个控制键直接渲染在岛上，悬浮胶囊切换为纯展示、不再吞掉 Agent 对下层应用的点击；「补充」经悬浮窗输入卡片提交（实测系统实况通知无输入框映射）；GUI 前台操控时岛文案显示「接管中」，与普通工具调用区分；完成时岛展开一次并弹出横幅通知，手动停止/失败静默收尾。需在系统通知设置中为「任务状态岛」渠道开启「悬浮通知」。
2. **思考与压缩控制**：「自动展开思考过程」开关（关 = 全程折叠、可手动展开）；可手动设置默认思考深度；可单独选择压缩/摘要模型；压缩请求按各厂商协议显式关闭思考或剥离全部思考参数。
3. **厂商余额显示**：厂商配置页可展开「余额查询」（API 地址 + JSON 路径，默认关闭，附测试按钮）；余额显示在模型选择弹窗厂商名右侧，随主题强调色。
4. **Exa 搜索**：`web_search` 仅保留 Exa（自配 API Key）——官方免 Key 搜索服务在本机超时不可用，已不注册（上游代码保留）；官方 `fetch_url` 网页正文读取不受影响。
5. **会话管理**：右上角更多菜单新增红色「删除会话」（带确认对话框）；新增「新建临时会话」——Agent 拥有一切能力，退出或切换会话即自动销毁、永不写入数据库，输入区有标识条提示。
6. **单轮消息计时**：每条回复的操作行下方显示完成时间与用时，如 `14:03 · 用时: 8.6秒`，超过 60 秒显示为「x分x秒」。
7. **消息排队**：模型运行中可先把消息排入队列，随停随发。
8. **分享成图重写**：按新 markdown 渲染层重写，表格/代码块等内容正常成图；分享按钮带触感反馈与成功/失败指示。

## 界面预览（Mod 实拍）

| 更多菜单：临时会话 / 删除会话 | 临时会话（输入区标识条） |
| :-------------------------------------------: | :----------------------: |
| ![更多菜单](docs/Screenshots/mod_menu_timing.jpg) | ![临时会话](docs/Screenshots/mod_temp_conversation.jpg) |

| 模型与对话（Mod 设置区） | 设置主页 |
| :----------------------: | :------: |
| ![模型与对话设置](docs/Screenshots/mod_model_settings.png) | ![设置主页](docs/Screenshots/mod_settings.png) |

## 上游能力

Eta 本体是面向 Android 的系统级 AI Agent：GUI 跨应用操作、终端（Android user/root Shell、Alpine / Debian Linux）、内置浏览器、文件工具、Skills 与 MCP 扩展、个人上下文检索、长期记忆、角色卡，以及通过 Xposed 接管电源键 / 小布 / 超级小爱的系统助手入口。使用 AI 功能需自备模型 API Key（BYOK），内置 OpenAI、Anthropic、阿里百炼、DeepSeek、Kimi 等提供商配置，也可添加自定义服务。

完整功能介绍、使用文档与设计理念见上游仓库 [Mangi-11/Eta](https://github.com/Mangi-11/Eta) 及其 [README](https://github.com/Mangi-11/Eta/blob/main/README.md)。

| GUI Agent | 小布助手 BYOK（上游演示） |
| :-------: | :----------------------: |
| <img src="docs/Screenshots/demo_gui_agent.gif" width="320" alt="Eta GUI Agent 执行演示"> | <img src="docs/Screenshots/demo_tools.gif" width="320" alt="从小布助手入口发起 Eta 任务"> |

## 本仓库文档

- [岛-通知机制调研](docs/ISLAND-EXPANSION-INVESTIGATION.md)：小米超级岛 / LiveUpdate 映射的完整测定结论（本 Mod 岛功能的实现依据）。
- [待办与 merge 裁决记录](TODO-MOD.md)：Mod 路线、已完成事项与上游 merge 裁决。
- 上游文档：[Agent Runtime](docs/AGENT_RUNTIME.md) · [技术实现](docs/TECHNICAL.md) · [设备支持与权限边界](docs/ROOTLESS_SUPPORT.md) · [HyperOS 系统入口](docs/HYPEROS_SYSTEM_ENTRY.md) · [终端原生组件](docs/TERMINAL_NATIVE.md)

## 致谢与许可证

- 上游作者 [Mangi-11](https://github.com/Mangi-11) 及 Eta 的所有参考项目（见上游 README）。
- 本仓库沿用上游 [PolyForm Noncommercial License 1.0.0](LICENSE)：未经上游作者书面授权，禁止二次分发、贩卖、收费代装及其他商业使用。
