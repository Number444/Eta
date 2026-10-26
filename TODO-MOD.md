# Eta Mod 待办

> 本 fork：`my-mods` 分支，applicationId `io.github.mangi.eta.mod`
> 远端：`origin` = Number444/Eta（推送目标），`upstream` = Mangi-11/Eta（官方源）
> 当前版本：3.3.3-002（versionCode 2026102402；merge 上游 v3.3.3 + 更新 APK 直链下载 + 玻璃控件适配 + Review 修补）

> 已完成归档（2026-10-08 精简，2026-10-21 追加第 7 条）：1. merge 上游 v3.2.0（30 commits，16 冲突手工裁决，出包真机回归通过）；2. 更新检查/关于页链接指向 Number444/Eta（`isNewer` 支持 `-NNN` 修订号）；3. 删官方搜索只留 Exa（官方服务本机超时不可用）；4. 消息操作行按钮放大 1.4 倍；5. 分享成图按新 markdown 层重写（含 `ShareCardRenderTest` 渲染基线）；6. merge 上游 v3.3.0（2026-10-13 完成：21 commits，7 冲突裁决 + `AgentAppState` 四功能块移植；暗礁=双方 `MIGRATION_21_22` 撞车，上游 `model_id` 迁移顺延 22_23、DB 升 v23；48 个单测失败对撞全为 Windows 环境基线零回归，出包真机冒烟通过）；7. merge 上游 v3.3.1+3fix（2026-10-21 完成：17 commits / ~150 文件，21 冲突文件 ~49 hunks 全裁决——三子代理语义对撞先行；关键适配：`setCapsuleYielded` 恢复逻辑归一到我方常驻 FLAG_NOT_TOUCHABLE 基线（仅补充输入态例外）、`AnswerActionButton` 新操作行体系保留我方 42dp/21dp+分享按钮+触感、刻度条四滚动点分页 +1 偏移、`AgentModelClient` 前缀缓存重构上重贴 webSearchTools/compactConfig/suppressReasoning、我方 stdin 测试补丁被上游 -4 退出码方案覆盖而放弃；DB 零迁移撞车（上游停 v22）；单测 49 失败对撞零回归（新增 1 个上游 PTY 测试 Windows 环境失败入基线）；merge 后玻璃尾巴：新模型选择器 EtaListPopup + 厂商余额移植、新增 `EtaGlassBottomSheet` 真玻璃覆盖模型抽屉与远端模型导入抽屉、朗读开关套玻璃）。详细过程见 git 历史。

## 19. 普通任务岛「补充」冷启动与关闭状态异常（已修复，待真机验证）

- **现象与原因**：普通任务原本无胶囊，首次点击创建悬浮窗时 `expanded=false`，展开输入与初始收起副作用竞争，导致第二次点击才出现输入框；外部补充结束又只收起而不移除窗口，恢复 `FLAG_NOT_TOUCHABLE` 后用户无法关闭残留胶囊。
- **修复**：冷启动先置展开态再创建窗口；岛输入结束改用独立回调，普通任务移除胶囊和边缘光但不停止 Runtime，GUI 任务仍收拢保留；新 run 与输入结束清零请求计数，避免旧请求重放。发送先于窗口销毁，避免 Compose 协程取消导致补充内容丢失。
- **验证**：新增 `AgentOverlayCapsuleTest` 覆盖冷启动输入、取消结束、发送先于结束回调，3 项均通过；ADB 未连接，普通/GUI 任务的完整交互仍需真机验证。

## 18. 002 发布后聚焦 Review 修补（2026-10-24）

- `EtaSearchField` 非液态玻璃分支恢复 Miuix 原生 `surfaceContainerHigh`，撤销封装时误改成 `surfaceContainer` 的视觉差异；液态玻璃行为不变。
- `AgentIslandNotifier.onRunStarted()` 立即撤销 `DONE_NOTIFICATION_ID`（1208），修复任务完成后 8 秒内启动新任务时旧「已完成」常驻通知与新胶囊并存的问题；其余通知 ID、首发 alert 和 8 秒收尾机制不变。
- 仅两个源码文件各一行修改；`AgentIslandNotifierTest`、`ProviderComponentsTest`、测试源码编译、`lintDebug` 与 Release v2 签名验证通过。

## 17. 玻璃主题搜索框与选项卡适配（2026-10-24 完成，Four 真机验收）

- 新增共享 `EtaSearchField`：液态玻璃档使用透明输入层 + 假玻璃底色/高光，其他档保持 Miuix 原色；覆盖供应商、社区目录、模型管理/选择/导入与角色库搜索框，修复灰白页面底下边界不可辨。
- 新增共享 `EtaTabRow`：非液态玻璃档保留原生 Miuix TabRow；液态玻璃档使用等尺寸独立选项按钮，每项各自绘制 squircle 背景与高光，外层不再铺底色或描边。
- 关键教训：Miuix TabRow 的外层 modifier 属于整行容器，选中块是内部独立动画 Box；在外层加圆角或 squircle 高光都会产生顶部溢线，不能靠更换曲线或调整选中颜色修复。
- 最终效果经 Four 真机确认；编译、定向测试、`compileDebugUnitTestKotlin`、`lintDebug` 与 Release v2 签名验证通过。

## 16. 检查更新直接下载 APK（2026-10-24 实施）

- GitHub Release 恰好包含一个 APK 时，更新弹窗「前往下载」通过默认浏览器直接打开其 `browser_download_url`；无 APK 或有多个 APK 时回退 Release 页面，避免误下附件。
- 不引入应用内下载器，不新增权限；浏览器与系统负责重定向、下载和后续安装。直链已实测 302 后返回 200、`application/vnd.android.package-archive` 与 attachment 响应头。
- 版本 3.3.3-002（versionCode 2026102402）；覆盖安装与同版本更新检测已真机验证，真实更新弹窗的 APK 直链下载留待后续更高版本 Release 验证。

## 15. merge 上游 v3.3.3（2026-10-24 完成）

- **实施现状（2026-10-24）**：已合入 `ed022f5`，全部冲突与自动合并语义冲突已裁决。Room 升 v25，21–25 迁移按 schema 检查并补齐双方字段；官方新队列唯一保留，临时会话/计时/删除取消 run 已接回；岛补充经 `MSG_INTERRUPT_REQUEST` 回传 AppState 后按官方插话执行；搜索仅官方链路，旧 Exa/独立搜索实现已移除（文件入回收站），DeepSeek 配置做一次性加密迁移；OAuth 供应商保留 API Key 余额；新队列条与输入栏恢复玻璃。`compileDebugUnitTestKotlin`、`lintDebug` 与数据库/队列/岛 wire/常亮/搜索迁移/供应商/工具目录定向测试通过。全量 1791 项单测为 50 failed / 2 skipped：49 项既有 Windows/Robolectric 环境基线，另 1 项为上游新增 OAuth 备份测试的同类文件行为环境失败；工具需求测试修正后通过，无其他新增失败。Four 已覆盖安装并完成真机冒烟验收。
- **目标**：完整合并官方 `upstream/main`（评估时 HEAD=`ed022f5`，v3.3.3 + README 更新），不做选择性移植；保留 Mod 既有架构与功能。预演为 17 个文本冲突文件，另有数据库、队列、搜索三类自动合并后的语义冲突，禁止解完冲突标记就直接出包。
- **消息队列（Four 已拍板）**：以官方新队列为唯一实现，支持附件、草稿恢复、跨会话发送与插话；删除本地旧纯文本队列及旧 drain/Action/UI。重新接入本地临时会话、单轮计时，切走/删除临时会话后不得落库、复活或续发队列。
- **插话与岛补充（Four 已拍板）**：统一为官方插话语义——取消当前 run 后发送新消息；岛「补充」入口保留，但提交也走官方插话，不再保留运行中 steering 的产品行为。官方 `AgentMessageQueueReducer` 为队列状态唯一主干，注意 AppState 与服务浮层之间的接线。
- **自动朗读（Four 已拍板）**：本次沿用官方实现，不额外增加逐 run 完成边界触发；连续队列是否漏读列入真机观察项，后续确有问题再单独立项。
- **搜索（Four 已拍板）**：完全采用官方 `AgentWebTools` + `WebToolsSettingsRepository` + 官方设置页及工具契约，使用官方内置、DeepSeek、自定义接口；放弃 Exa，不保留本地 `AgentSearchProvider`/独立 `web_search` 执行器作为第二链路，不保留旧搜索设置入口。迁移现有 DeepSeek Key 与选择；原 Exa 用户回退官方默认后端，旧 Exa 配置不迁移、不留备用运行路径。
- **供应商/OAuth（Four 已拍板）**：引入官方 Codex/Grok 订阅登录、账号绑定与远端模型同步，以其 Provider/Repository/UI 重构为骨架；保留本地余额字段、编辑器与 API Key 供应商余额查询。OAuth 模式不直接复用依赖 Bearer API Key 的旧余额请求，避免静默 401。远端模型导入采用官方 `RemoteModelCatalog`，保留本地 `EtaGlassBottomSheet`。
- **数据库**：目标 Room v25；双方 v22–v24 migration 分叉，必须按实际 schema 幂等补齐 `model_id`、`auth_mode`、余额列与计时列，禁止整边选择或清库。所有已发布 Mod 的覆盖升级为必测路径，另覆盖全新安装最终 schema；先完成迁移方案与测试再处理上层功能。
- **运行时与岛**：保留本地 `.mod` action、胶囊 `FLAG_NOT_TOUCHABLE`、岛暂停/停止及输入浮层触摸例外，合入官方 `keepScreenOnState/keepScreenOn`；暂停、恢复、取消、完成与 Service 销毁时常亮和岛状态均须正确回收。岛补充按上述官方插话语义重接，不恢复旧 steering。
- **UI/语音（Four 已拍板）**：接受官方新图标、移除自定义启动动画及语音设置重构；官方新增队列条、供应商页、语音页和 Web 工具页按本地液态玻璃规范 Mod 化。保留本地一级分类设置页、数据驱动更多菜单、临时会话/删除会话/单轮计时；旧顶栏自动朗读按钮不恢复，入口并入更多菜单。连续队列朗读不额外改架构。
- **构建**：保留 `applicationId=io.github.mangi.eta.mod`、签名与 Number444/Eta 更新源；目标 versionName `3.3.3-001`，versionCode 按实际发布日期递增。接受官方 ARM64-only、Nimbus/ProGuard 与 `buildConfig=true`。
- **验收顺序**：数据库迁移测试 → 编译与重复声明检查 → 官方队列 reducer/AppState 集成 → DeepSeek/自定义搜索 → Codex/Grok OAuth 与余额 → 岛插话/暂停/停止/触摸穿透/常亮 → 三档玻璃与深浅色 → `lintDebug` + `compileDebugUnitTestKotlin` + 全量单测基线对撞 → Release 出包与真机回归。

## 14. 首次安装引导向导（Compose 原生绘制，2026-10-19 调研存档并定方向，未开工）

- **关键事实：Eta 目前没有任何 onboarding**——`MainActivity.kt:66` 首次启动直接进主页，无欢迎页/向导/「已完成初始化」标记位；未配置模型仅在发消息时弹错误（`AgentAppState.kt:1252`）；权限（悬浮窗/无障碍/通知）全靠设置页手动点。故本事项是**从零新建引导功能**，非改造现有流程
- **方向（2026-10-19 Four 拍板）：Compose 原生绘制，放弃 HTML/WebView 方案**。比较结论：Compose 无机型适配成本、无 JS 桥安全面、无 WebView 冷启动开销（首次初始化数百 ms 恰好打在「第一印象」场景）、液态玻璃材质继承 `EtaGlassTokens` 无割裂；HTML 唯一优势（改文案不发版/花哨动效）在本场景兑现不了价值
- **实施要点**：
  - 新增 `AppRoute.Onboarding` 路由 + 「已完成引导」prefs key + `MainActivity` 开头一行判断，不碰现有架构
  - 向导外壳 = 步骤指示 + 步骤状态机（新写的唯一大件）
  - **provider/API key/模型表单直接复用现有 Composable**：`ModelProviderListScreen`/`ModelProviderDetailScreen`/`ProviderConfigDraft`——不重写表单，只包向导壳
  - 材质直接继承 `EtaGlassTokens` 五套预设 + miuix，视觉体系零新增
  - 权限申请步骤留原生：复用 `SettingsPermissionsScreen` 现有 intent 逻辑（悬浮窗/无障碍/通知均 WebView 无法承载，且本来就在原生侧）
  - 落地存储现成：provider/API key/默认模型全走 `SettingsDataStore`（`data/datastore/SettingsDataStore.kt:47`），Repository 一次调用写完（`RuntimeConfigRepository.setSelection` :52/:63）
- **提醒**：API key 现状明文存 DataStore（无 EncryptedSharedPreferences），引导方案安全水位与现状持平、不恶化
- 下一步（待 Four 指令）：出实施计划（向导步骤划分、状态机设计、复用件的接入方式）

## 13. DeepSeek Search 接入 + 搜索引擎可切换架构（2026-10-17 实施）

- **起因**：DeepSeek 开放 Search/Fetch API（[官方文档](https://api-docs.deepseek.com/guides/search)），Kimi Code 已配置的 deepseek key 实测 200 可用；Search $0.75/千次成功请求，`need_content=false` 时 `summary`（600-800 字）官方定位即"给模型读"
- **架构调整（为后续更多搜索接口铺路）**：原实现零抽象硬编码 Exa，本次引入 `agent/tool/AgentSearchProvider.kt` 枚举抽象（prefValue/displayName/apiKeyPrefKey + `buildRequest`/`parseResults` 两分支），统一映射 `results[{title,url,published,text}]`；截断预算（1000 字/条、25000 总量）与打包格式留在 `AgentWebSearchTool` 不变。新增引擎 = 枚举值 + 两分支 + Prefs key + 设置页入口
- **对模型透明**：工具名 `web_search`、参数 schema（query/num_results）不变；description 与 prompt 去 Exa 化；`exaConfigured()` → `searchConfigured()`
- **配置**：新增 `AGENT_SEARCH_PROVIDER`（缺省/未知回退 exa，兼容存量）+ `AGENT_DEEPSEEK_API_KEY`，两 key 独立保存切换不丢；设置→工具新增「搜索引擎」下拉（EtaOverlayDropdownPreference）+ 当前引擎 key 入口；`ExaApiKeyDialog` 泛化为 `ApiKeyDialog(title,...)`
- **映射决策**：DeepSeek `date→published`、`summary→text`（空则回退 snippet）、`need_content` 固定 false、query 超长截 128 字符；DeepSeek Fetch API 本次不接（已有 fetch_url 工具）
- **测试**：新增 `AgentSearchProviderTest`（解析映射/回退/请求构造/query 截断）；既有 web_search 断言均基于"无 key 不注册"，行为不变
- **出包**：v3.3.0-016（手机未连接只出包不装机）；commit/push 待 Four 指令

## 12. App 全面 UI 焕新计划：液态玻璃（2026-10-16 Four 拍板启动，进行中）

- **Four 三条定调**：
  1. Window 系弹层（WindowDialog/WindowListPopup/WindowDropdownPopup，独立 Window 采不到样）统一做**只带高光的半透明假玻璃**
  2. 其余部分全部做**真玻璃**——后续可能上自定义图片背景，采样能力必须现在就位，不留欠账
  3. 为稳定性先优化架构，再谈样式
- **开发顺序（Four 指令）**：主页 → 侧边栏 → 设置页，逐步来
- **第一步：三项架构调整（计划模式执行，2026-10-16 已实施，Four 真机验收通过）**：
  1. ~~玻璃材质 Token 层~~：`ui/components/EtaGlassTokens.kt`（`EtaGlassStyle` + 5 预设 FloatingButton/InputBar/Popup/DialogCard/ListRow + `etaGlass` 真玻璃 / `etaPseudoGlass` 假玻璃两工厂）；`LiquidGlass.kt` 两 modifier 改为预设薄封装，参数原样像素级不变
  2. ~~主页弹窗收口~~：新建 `EtaPopups.kt` 的 `EtaListPopup`（默认 overlay 真玻璃能力，`useWindow=true` 走 Window 变体）；迁移 6 处：顶栏溢出/附件菜单/思考强度/模型选择（overlay）+ 侧栏长按菜单（useWindow，面板在 Scaffold 外无 overlay 宿主）+ 路径输入弹窗（→EtaOverlayDialog，**底色对齐 surfaceContainer 有微色差 + IME 需真机盯**）；**根级 5 弹窗（AgentAppRoot 角色通知/重命名/删除会话/删除消息/重新生成）在 NavDisplay 之外无 overlay 宿主（miuix LocalDialogStates 默认空列表会静默不渲染），保持 WindowDialog 不动**，阶段二套假玻璃时再收口
  3. ~~多采样源模式~~：侧边栏面板自建 backdrop（`ConversationPanePanel`，baseColor=面板容器色，源=会话 LazyColumn）；设置骨架两处（`MiuixScaffoldPage`/`MiuixScaffold`，源=内容列表，与 miuix captureForTopBar 同节点叠加——主页已验证双录制可共存，真机回归盯顶栏模糊）；模式文档 `docs/UI-GLASS-ARCHITECTURE.md`（含嵌套禁令、真假玻璃决策矩阵、根 surface 背景锚点）
- **调研关键结论存档（2026-10-16，三子代理全量摸排）**：
  - 主页弹层清单：顶栏溢出菜单（`AgentAppShell.kt:290`）、附件菜单（`AgentChatFileAttachments.kt:122`）、路径输入（`:165` 原生 WindowDialog）、思考强度（`AgentChatInputBar.kt:572`）、模型选择 OverlayListPopup（`AgentChatModelControls.kt:128`，最复杂）、上下文 TooltipBox（`:319`）、侧栏会话操作（`ConversationPanePanel.kt:455`）、重命名/删除会话/删除消息/重新生成 4 个 WindowDialog（`AgentAppRoot.kt:797/826/846/874`）
  - 侧边栏：推移式非覆盖（`ConversationSidePaneScaffold.kt:215`），自身背景不透明实色，不在采样层内；搜索框 `ConversationPanePanel.kt:288`、设置圆钮 `:320`、选中行实色 `:401`
  - 设置页：16 设置页 + 5 供应商页全屏不透明路由；返回钮统一 `MiuixBackButton`（真实使用点仅 3 处）；卡片背景全 app 仅 2 个定义点（`EtaCard` 默认色 + `EtaPreferenceGroupItem.kt:28,34`）；弹窗收口 3 点（`EtaWindowDialog` 22 处调用 / `EtaOverlayDialog` 6 处 / `EtaDropdownPreference` 18 处，改 3 个函数覆盖 46 个弹窗点）
  - 风险点：含 IME 的弹窗（重命名、API key、路径输入）从 Window 迁 overlay 需重点测键盘行为；设置页长列表逐卡 drawBackdrop 有滚动掉帧风险，列表项宜用简化材质，玻璃重头戏留给弹窗与固定控件；性能降级挂现有三档模糊设置（无/默认/液态玻璃）
- **第四步：设置页（2026-10-17 v3.3.0-012 实施、013 修订，Four 真机验收通过）**：返回钮真玻璃（`MiuixBackButton` 读就近 backdrop，44dp 圆对齐主页顶栏按钮；壳内非主页路由无采样源，`AgentAppShell` 传 `enableGlass=false`）；卡片假玻璃（`EtaCard` 默认色 surface×0.72 + 新增 `etaSquircleGlassBorder` squircle 渐变描边，`EtaPreferenceGroupItem` 只换半透明底不描边）；弹窗统一不透明描边玻璃（`EtaWindowDialog`/`EtaOverlayDialog` 加 squircle 描边，根级 5 弹窗同步收口进 `EtaWindowDialog`；`EtaDropdownPreference` 迁 `EtaListPopup`：window 变体不透明描边、overlay 变体真玻璃）；浅色液态玻璃档灰白底扩展到设置骨架 Scaffold，三处 backdrop 采样基色同步。**013 修订**：顶栏发白修复（`TopBarBackdrop` 渐进模糊预填色/混合色两处 `surfaceColor` 换 `etaGlassPageBaseColor(...)`）+ 主页全身灰白化（miuix `Scaffold` 默认 `containerColor=surface` 白底盖住聊天槽位灰底，`AgentAppShell` 显式传 `containerColor = etaGlassPageBaseColor(...)`，顶/身统一）。三档回退保持原实色外观
- **下拉弹层全真玻璃化（Four 2026-10-17 定方向1，v3.3.0-015 验收通过）**：`EtaDropdownPreference` 默认 `useWindow` 翻转为 false（overlay 真玻璃），删除 `EtaWindowSpinnerPreference` 包装器，3 处调用点（`LinuxEnvironmentSections` 2、`ModelProviderDetailScreen` 1）改调 `EtaDropdownPreference`；`SpeechSettingsFields`/`LanguagePreference` 冗余显式 false 清理。落地后 window 变体 7 处全部转 overlay（`SettingsModelScreen` 3、`SettingsAssistantTakeoverScreen` 1、`LinuxEnvironmentSections` 2、`ModelProviderDetailScreen` 1），全部宿主页已确认有 miuix Scaffold 弹层宿主；侧栏长按菜单（面板在 Scaffold 外无 overlay 宿主）保持 window 不透明描边玻璃不动。验收重点：各页 overlay 弹层层级与宽屏表现。**015 换包追加（bfcd78e）**：默认模糊档改液态玻璃——`AppearanceSettings.blurMode` 默认值、`fromPersistedValue` 缺省回退、`fromLegacyBlurEnabled`（开/未设置）三处 `DEFAULT`→`LIQUID_GLASS`；显式关过模糊的老用户保持「无」不动；release 同版本号换包。**仍待办**：`LocalNetworkPermissionUi.kt:52` 裸 WindowDialog（局域网权限引导）收口进 `EtaWindowDialog`
- **第二步：主页弹层真玻璃（2026-10-17 v3.3.0-010 已实施，Four 真机验收通过）**：`EtaListPopup` overlay 分支内容侧注入 `EtaGlassPopupContainer`（盖在 miuix 容器实色底上，恰好覆盖弹层可视区；backdrop 为 null 时零修饰回退原生外观）；轮廓 16dp 圆角——曾上 `EtaSquircleShape` 对齐容器 squircle clip，但 Kyant lens 只支持 CornerBasedShape/RoundedRectangularShape，通用 Shape 抛 UnsupportedOperationException 致全弹层闪退（010 首包事故，已回退删除）；squircle 与圆角 16dp 下差异亚像素级，真机观察。自动命中主页 4 处（顶栏更多/附件+/思考强度/模型列表）；侧栏 window 分支与「输入路径」弹窗不动（后者归设置页阶段随 `EtaOverlayDialog` 统一收口）。同日补漏：上下文用量 tooltip（压缩卡片，`AgentChatModelControls.kt` AgentContextUsageButton）走 Compose Popup 独立 Window 采不到 backdrop，假玻璃化（containerColor 72% 半透 + 新增 `etaPseudoGlassBorder` 高光描边复用件）；2026-10-17 Four 修订：tooltip 同长按菜单改**不透明描边玻璃**（恢复默认不透明底，仅留高光描边，半透明观感差）；其余 3 处 TooltipBox（消息气泡 1 + 重新生成/删除悬停提示 2）Four 决定不做。已排除方案：MiuixTheme 局部覆写容器背景（`MiuixPopupHost` 在宿主上下文调起 content，调用点 CompositionLocal 覆写传不进容器层）。观察点：玻璃比 dim 后的周围略鲜亮（采样层不含 dim），必要时调 Popup tintAlpha。调用点零改动，merge 冲突面零新增
- **第三步：侧边栏（2026-10-17 v3.3.0-011 已实施，Four 真机验收通过）**：搜索框+设置钮 = 假玻璃芯片（`PaneActionBar`，Four 拍板不做布局重构）；选中行 = 假玻璃（`ConversationTextRow`，深浅色均与搜索框/设置钮同浓度默认 surface×0.72，行在录制层内按嵌套禁令只能假玻璃）；长按会话菜单 = `EtaListPopup` window 分支**不透明描边玻璃**（原生不透明底 + `etaPseudoGlassBorder`，半透明假玻璃实测观感差，Four 修订）；浅色液态玻璃档主页+侧边栏底色改灰白 `EtaGlassLightPageBase`（0xFFE6E6EA 约 10% 灰，6% 实测不可见，`etaGlassPageBaseColor` 收口于 `conversationPaneContainerColor` + `ConversationSidePaneScaffold` 聊天区底，替代控件级底部阴影方案）。配套：`etaPseudoGlass` 新增 `pseudoBackground` 自定义底色参数。三档回退均保持原实色外观。**014 补漏**：列表底部「更多选项 · N」旧会话胶囊（`OlderConversationsCapsule`）遗漏实色，改假玻璃芯片（`etaPseudoGlass(FloatingButton)`，非玻璃档保持原实色）

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

## 8. ~~GUI run 胶囊吞点击 → 运行控制三键移交超级岛~~（2026-10-14 v3.3.0-005 已实装，Four 真机验证通过：岛三键功能正常、悬浮窗输入正常）

- 问题（2026-10-13 v3.3.0-001 冒烟发现）：上游 v3.3.0 新运行胶囊窗口**可触摸**（有展开/暂停/停止按钮，窗口固定宽 × 分档高、顶部居中），agent 注入的坐标点按落在胶囊区域即被吞，点不到下层应用内容；上游自己在 `AgentRuntimeService.kt:1007` 注释里承认会吞但未真解决。边缘流光（全屏 `FLAG_NOT_TOUCHABLE`）与 `GestureIndicator` 均清白；`ui_nodes` 走 `rootInActiveWindow`，胶囊不可获焦不进节点树，故仅坐标点按受影响
- 方案（Four 拍板）：GUI run 期间胶囊 + 结果卡片切 `FLAG_NOT_TOUCHABLE` 纯展示，控制职能移交超级岛；岛 action 按钮渲染已经 Four 真机验证可行（`dispatchGesture` 无目标窗口概念，「点到下层」只能让胶囊穿透实现）
- 映射测定（2026-10-14，v3.3.0-002，**已出结论**，映射表见 `docs/ISLAND-EXPANSION-INVESTIGATION.md` 第九节）：新增「岛映射诊断测试」`AgentIslandMappingDiagnostics`（设置页入口在岛通知诊断测试下方），T30–T35 变量隔离序列真机跑完——**纯 LiveUpdate 即上岛（T30），三个标准按钮渲染+回调全通（T31），RemoteInput 无输入框映射（T32），BigTextStyle 正常（T33a），ProgressStyle 渲染分段进度条（T33b），chronometer 计时正常（T34）**；T35 对照 Four 未回报差异
- 实施要点（已按映射结论修正）：
  - 三键岛**不依赖 miui.focus.param**：纯 LiveUpdate + addAction 即可渲染按钮并回调，实装时可考虑精简私有参数（T35 对照结论补齐后再定）
  - `AgentIslandNotifier` 运行中通知加三个 action：暂停/继续（随暂停态翻转）+ 插入 + 停止——复用现有停止 action 模式（`AgentIslandNotifier.kt:330-343`）；按钮文案宜 2–3 字（实测长文案全显不截断但排版松散，诊断测试用的 `execution_stop`「停止运行任务」五字偏长）
  - 岛按钮 PendingIntent 须打到 `AgentRuntimeService`（GUI run 会话所在，`requestPause/requestResume/requestSupplement` 现成于 `:768/:776/:784`）；现有岛停止 action 只打 `AgentExecutionService`（App 侧 run），需统一广播或双服务分发
  - 「插入」键确认走悬浮窗输入 fallback（T32 实锤 RemoteInput 无映射）：唤起浮层直进 supplement 输入模式（`enterSupplementMode` 现成）；**交互链路已经 Four 真机验证通过**（2026-10-14，v3.3.0-003「岛插入悬浮窗测试」：岛「补充」按钮 getService 直发服务唤起浮窗，复用生产 `OverlaySupplementInput` 组件与胶囊窗口参数，提交文本回报正确）
  - run 结束恢复窗口可触摸（结果卡片弹出需要交互，时机衔接）
  - 兜底：即便岛不渲染，通知栏通知的 action 按钮是标准 Android 能力必可用
- 备忘：属上游设计缺陷，找时机给上游提 issue；三轮诊断工具（岛通知 T20-T24 / 岛映射 T30-T35 / 岛插入悬浮窗）使命完成，已随 v3.3.0-004 清理，结论全文在 `docs/ISLAND-EXPANSION-INVESTIGATION.md`
- 实装（2026-10-14，v3.3.0-005）：胶囊窗口常驻 `FLAG_NOT_TOUCHABLE` 纯展示（仅补充输入时经 `setCapsuleInputMode` 临时恢复可触摸）；岛运行通知三键 = 暂停/继续（`onPauseChanged` 翻转文案）+ 补充 + 停止；暂停/补充 getService 直发 `AgentRuntimeService` 新 action（`ACTION_ISLAND_TOGGLE_PAUSE`/`ACTION_ISLAND_SUPPLEMENT`），停止沿用 `AgentExecutionService` 租约回收；岛「补充」经 `islandSupplementRequests` 计数触发胶囊展开直进补充输入，提交/取消后自动收拢并恢复纯展示；GUI 前台工具（`AgentOverlayVisibilityPolicy.isForegroundOperationTool`）岛文案单独显示「接管中」（`State.TAKEOVER`），普通工具仍「工具调用」

## 9. ~~侧栏设置圆形按钮无点击震动~~（2026-10-14 已修复，待真机确认）

- 现象（2026-10-13 冒烟发现）：侧栏搜索行右侧设置按钮点击大概率无震动。非 merge 回归——`ConversationPanePanel.kt` 裸 `Modifier.clickable` 从未挂显式震动（同文件会话行有 `KeyboardTap`），merge 前后逐字节相同
- 已修：`PaneActionBar` 设置按钮 `onClick` 补 `performHapticFeedback(HapticFeedbackType.KeyboardTap)`，与会话行对齐

## 10. ~~会话管理与单轮计时三功能~~（2026-10-14 v3.3.0-006 实装 + v3.3.0-008 排版定稿，008 排版 Four 离场前装机、未回报确认）

- 删除会话：右上角更多菜单新增红色危险项（`TopBarOverflowMenu` 重构为数据驱动 `TopBarMenuEntry` 列表，根治 index 分发耦合），复用既有 `conversationDeleteTarget` 确认对话框，仅当前有会话时显示
- 新建临时会话：菜单第二项；`AgentAppState` 纯内存实现（`temporaryConversationIds`/`pendingTemporaryConversation`），切换/退出会话即销毁（运行中一并 `stopCurrentRun()`），`persistConversations()` 过滤临时 id 永不落库；输入区顶部有标识条
- 单轮计时落库：`AgentMessageUi` 加 `runDurationMs`/`runFinishedAtMillis`；`launchConversationRun` 记 elapsedRealtime，非 rewriting 的 `applyRunResult` 调 `stampRunTiming`；DB v23→v24（`MIGRATION_23_24` 两条 ALTER TABLE ADD COLUMN）
- 排版定稿（008，Four 拍板）：按钮行纯净，计时独占一行在按钮行下方贴左，格式 `HH:mm · 用时: x.x秒`（≥60 秒为 `x分x秒`），footnote1 + 75% 灰
- 未来上游若升 DB v24，merge 时我方 migration 顺延重排（撞车先例：MIGRATION_21_22）

## 11. ~~液态玻璃（Kyant0 Backdrop）自 Eta-alpha 试验仓移植~~（2026-10-16 v3.3.0-009 实装，Four 真机验收通过）

- 来源：`D:\Agent Space\Eta-alpha`（独立试验仓，交接文档 `HANDOVER-LIQUID-GLASS.md`）；alpha 基线与移植时正式版 HEAD（2c1d1c9）全树一致，A+C 类补丁 `git apply` 零冲突落地；B 类 alpha 共存改动（applicationIdSuffix/manifest 占位符/AgentRuntimeWire packageName）按交接约定不移植
- 三档模糊：`blurEnabled: Boolean` → `blurMode: AppearanceBlurMode`（NONE/DEFAULT/LIQUID_GLASS，字符串持久化）；DataStore 新旧键同步写、读时无新键按旧布尔映射（迁移逻辑提为 `AppearanceBlurMode.fromLegacyBlurEnabled()` 可单测 helper），回滚旧版不丢设置
- 玻璃基建 `ui/components/LiquidGlass.kt`（`io.github.kyant0:backdrop:2.0.1`，Apache-2.0）：`LocalLiquidGlassEnabled` + `isRuntimeShaderSupported()` 双门控，null 时全退化；采样源只包消息内容层，玻璃控件全为其兄弟节点（glass-on-glass 崩溃条件已规避）；液态玻璃档关闭 miuix 底部磨砂为有意互斥
- 玻璃落点 9 块：顶栏 2 按钮（44dp 圆）+ 输入栏 6 控件（含发送按钮，`LiquidGlassCircleBehind` 垫层不动控件本体）+ 刻度胶囊（spring 1f/300f 放大动效，底边上收量随动画 lerp）
- alpha 遗留坑已补：两个单测文件残留 `blurEnabled` 引用（alpha 侧从未编译测试源码），移植时同步修正并新增枚举 round-trip + 旧布尔映射两条单测；全量单测 48 失败全为 Windows 环境基线零回归
- 性能待观察项：输入框聚焦掉帧在 alpha debug 包出现过，009 release 包 Four 验收未回报异常，持续观察

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
