# Eta Mod 待办

> 本 fork：`my-mods` 分支，applicationId `io.github.mangi.eta.mod`
> 远端：`origin` = Number444/Eta（推送目标），`upstream` = Mangi-11/Eta（官方源）
> 当前版本：3.2.0-002（versionCode 2026100702，merge 官方 v3.2.0 后第二个 Mod 修订）

> 已完成归档（2026-10-08 精简）：1. merge 上游 v3.2.0（30 commits，16 冲突手工裁决，出包真机回归通过）；2. 更新检查/关于页链接指向 Number444/Eta（`isNewer` 支持 `-NNN` 修订号）；3. 删官方搜索只留 Exa（官方服务本机超时不可用）；4. 消息操作行按钮放大 1.4 倍；5. 分享成图按新 markdown 层重写（含 `ShareCardRenderTest` 渲染基线）。详细过程见 git 历史。

## 6. merge 上游 v3.3.0（执行中，2026-10-08 开工）

- [x] 评估结论（2026-10-08 第二次实测，`git merge-tree` 干跑）：上游领先 21 个提交（最新 release v3.3.0，2026-10-07 发布；main 在其后 2 个提交：CI 修复 + `3b21d9f` 工具简化/持久后台命令）
  - 冲突共 7 个文件，6 处琐碎：`app/build.gradle.kts`（版本号，升左段归零 → 3.3.0-001/2026100802）、`AndroidManifest.xml`（+2 行）、`AgentModelClient.kt`（上游 `promptRootState` vs 我们 compactConfig validate，两边保留）、`AgentRuntimeRunExecutor.kt`（`requireContextWindow`→`knownContextWindow` 改名 + 保留岛上岛块）、`AgentLocalTools.kt`（上游 `terminalExecutionContext` vs 我们 Exa 注释块，两边保留）、`AgentToolRequirementsTest.kt`（上游 executionOnly 新断言 + 保留 web_search 排除）
  - 唯一真活：`AgentAppState.kt`——上游 `0da0056` 重构拆出 `AgentMemoryStore`/`AgentSkillsStore`/`PermissionHealthStore`/`AgentToolsCatalogUi`，本文件瘦身至 1898 行；我们的 126 行增量 = 4 个功能块（默认思考深度/专用压缩模型/排队插入消息/import 并集），移植到新结构，消化钩子挂上游新 `applyRunResult`
  - 自动合并零冲突：`AgentRuntimeClient.kt`（sendSupplement 为 Mod 私有增量）、`AgentContextSession`/`AgentLoop`、三语言 `strings.xml`、岛-通知区、`EtaAssistantOverlayService`
- [ ] 执行：merge → 裁决 → 移植 → 双过 + 基线对撞 → 出包冒烟（备份分支 `backup/pre-v3.3.0-merge`）

## 7.【暂不考虑实现】分享图片进 Eta → 模型识别 → 自主生成「岛」卡片（取餐码/取件码等）

- [ ] 调研结论（2026-10-08，两条子线调研完成）：
  - **接收分享（缺口）**：Manifest 无任何 `ACTION_SEND`/`ACTION_SEND_MULTIPLE` intent-filter，App 只能外发分享（`MessageShareImage.kt:197`）。补齐 = MainActivity 或新建透明 bridge Activity 加 filter（`image/*`），`onCreate/onNewIntent` 解析 `EXTRA_STREAM` + `clipData` 双路；拿到的是临时授权的 `content://` URI，**必须立即复制到私有目录**（`takePersistableUriPermission` 对分享 URI 无效）
  - **图片→模型链路（现成）**：复制后构造 `ModelImage(reference=uri)` 走 `AgentAppState` 发送路径即可，编码/拼接/三 provider 适配全部复用（`AgentImageCodec`、`AgentConversationCodec`）
  - **工具机制（现成）**：「模型自主决定何时展示卡片」天然契合现有架构——新增 `show_island_card` 工具 = 一个 catalog（`agent/model/` 下，声明 JSON Schema：类型/号码/商家/有效期等字段）+ `AgentLocalTools` 挂执行器
  - **岛 UI（首选复用现有岛机制，2026-10-08 修正）**：本 fork 已有岛-通知机制 `AgentIslandNotifier.kt`——本地在标准通知 extras 写 `miui.focus.param` JSON（焦点参数 + ongoing + promoted + shortCriticalText 三件套），HyperOS FocusPlugin 直接上岛，**无需官方企业审核**，已在 Four 的 HyperOS 3 上 T1-T14 全链路验证（档案 `docs/ISLAND-EXPANSION-INVESTIGATION.md`，动手前必读）。首选路径 = 复用该通道渲染取餐码卡片，**第一验证点：`miui.focus.param` 模板能力能否表达「大字码 + 固定格式排版」**；表达不了再退自绘 overlay（基建现成：`EtaAssistantOverlayService.kt:335` 全屏 `TYPE_APPLICATION_OVERLAY` + Compose、`GestureIndicator` 小悬浮窗，自由度满但无系统岛一体感）
  - ~~小米超级岛/焦点通知官方 API 不适用~~（该结论针对官方开发者平台的企业审核接入路径；Eta 现有本地 extras 方案已绕过，不构成本事项的排除理由）
- [ ] 待实施（未开始，动手前须 Four 确认方案）：
  - Bridge Activity + intent-filter + 图片落盘
  - 识别输出契约：提示词约定 JSON 格式（类型/号码/来源/有效期）
  - `show_island_card` 工具（声明 + 执行两端）
  - 卡片 UI：大字码 + 点击复制 + 收起/关闭；数据持久化（进程重启恢复）
  - 坑位备忘：MIUI 悬浮窗权限与「后台弹出界面」两个开关；Android 12+ overlay 保持 `alpha=1` 否则触摸被拦截；保活靠 FGS

## 备注

- 发布流程由 Four 手动执行（GitHub Releases 页面），艾薇只负责出包与提交
- ~~无 root 系统级唤醒调研~~：Four 调研结论为基本不可行，待办已删（2026-10-07）
- 岛-通知机制结论见 `docs/ISLAND-EXPANSION-INVESTIGATION.md`，动通知相关代码前必读
- 真机：小米 14 Ultra，adb serial `dcc44b47`
- v3.2.0 merge 裁决记录（D1-D5）：
  - D1 搜索：保留 Exa，设置页新增「搜索引擎」选择（官方免 Key / Exa）→ **已被事项 3 推翻并执行**：官方搜索在本机超时不可用，已回退为纯 Exa（仅不注册官方搜索，上游 `agent/web/` 代码保留）
  - D2 分享图片：本次砍除（`ui/share/` 已删），后续按新 UI 重写
  - D3 思考折叠：开关合并进上游结构（`rememberAutoExpandThinking()` 挂上游 ThinkingRow 与 AgentWorkProcess）
  - D4 侧栏：保留我们的精简版（未引入上游 PaneDock；Skills 仍可从设置页进入）
  - D5 压缩模型：专用压缩模型在上游新 `compact()` 结构上重写（无超限重试循环；专用失败回退主模型）
