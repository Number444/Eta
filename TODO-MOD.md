# Eta Mod 待办

> 本 fork：`my-mods` 分支，applicationId `io.github.mangi.eta.mod`
> 远端：`origin` = Number444/Eta（推送目标），`upstream` = Mangi-11/Eta（官方源）
> 当前版本：3.3.0-009（versionCode 2026101601；液态玻璃自 Eta-alpha 试验仓移植定稿，Four 真机验收通过）

> 已完成归档（2026-10-08 精简）：1. merge 上游 v3.2.0（30 commits，16 冲突手工裁决，出包真机回归通过）；2. 更新检查/关于页链接指向 Number444/Eta（`isNewer` 支持 `-NNN` 修订号）；3. 删官方搜索只留 Exa（官方服务本机超时不可用）；4. 消息操作行按钮放大 1.4 倍；5. 分享成图按新 markdown 层重写（含 `ShareCardRenderTest` 渲染基线）；6. merge 上游 v3.3.0（2026-10-13 完成：21 commits，7 冲突裁决 + `AgentAppState` 四功能块移植；暗礁=双方 `MIGRATION_21_22` 撞车，上游 `model_id` 迁移顺延 22_23、DB 升 v23；48 个单测失败对撞全为 Windows 环境基线零回归，出包真机冒烟通过）。详细过程见 git 历史。

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
- **待办：下拉弹层全真玻璃化（Four 2026-10-17 定方向1）**：window 变体 8 处（`SettingsModelScreen` 3 处、`SettingsAssistantTakeoverScreen` 1、`LanguagePreference` 1、`LinuxEnvironmentSections` 2、`ModelProviderDetailScreen` 1）切 `useWindow=false`，验收各页 overlay 弹层层级与宽屏表现；另 `LocalNetworkPermissionUi.kt:52` 裸 WindowDialog（局域网权限引导）待收口进 `EtaWindowDialog`
- **第二步：主页弹层真玻璃（2026-10-17 v3.3.0-010 已实施，Four 真机验收通过）**：`EtaListPopup` overlay 分支内容侧注入 `EtaGlassPopupContainer`（盖在 miuix 容器实色底上，恰好覆盖弹层可视区；backdrop 为 null 时零修饰回退原生外观）；轮廓 16dp 圆角——曾上 `EtaSquircleShape` 对齐容器 squircle clip，但 Kyant lens 只支持 CornerBasedShape/RoundedRectangularShape，通用 Shape 抛 UnsupportedOperationException 致全弹层闪退（010 首包事故，已回退删除）；squircle 与圆角 16dp 下差异亚像素级，真机观察。自动命中主页 4 处（顶栏更多/附件+/思考强度/模型列表）；侧栏 window 分支与「输入路径」弹窗不动（后者归设置页阶段随 `EtaOverlayDialog` 统一收口）。同日补漏：上下文用量 tooltip（压缩卡片，`AgentChatModelControls.kt` AgentContextUsageButton）走 Compose Popup 独立 Window 采不到 backdrop，假玻璃化（containerColor 72% 半透 + 新增 `etaPseudoGlassBorder` 高光描边复用件）；2026-10-17 Four 修订：tooltip 同长按菜单改**不透明描边玻璃**（恢复默认不透明底，仅留高光描边，半透明观感差）；其余 3 处 TooltipBox（消息气泡 1 + 重新生成/删除悬停提示 2）Four 决定不做。已排除方案：MiuixTheme 局部覆写容器背景（`MiuixPopupHost` 在宿主上下文调起 content，调用点 CompositionLocal 覆写传不进容器层）。观察点：玻璃比 dim 后的周围略鲜亮（采样层不含 dim），必要时调 Popup tintAlpha。调用点零改动，merge 冲突面零新增
- **第三步：侧边栏（2026-10-17 v3.3.0-011 已实施，Four 真机验收通过）**：搜索框+设置钮 = 假玻璃芯片（`PaneActionBar`，Four 拍板不做布局重构）；选中行 = 假玻璃（`ConversationTextRow`，深浅色均与搜索框/设置钮同浓度默认 surface×0.72，行在录制层内按嵌套禁令只能假玻璃）；长按会话菜单 = `EtaListPopup` window 分支**不透明描边玻璃**（原生不透明底 + `etaPseudoGlassBorder`，半透明假玻璃实测观感差，Four 修订）；浅色液态玻璃档主页+侧边栏底色改灰白 `EtaGlassLightPageBase`（0xFFE6E6EA 约 10% 灰，6% 实测不可见，`etaGlassPageBaseColor` 收口于 `conversationPaneContainerColor` + `ConversationSidePaneScaffold` 聊天区底，替代控件级底部阴影方案）。配套：`etaPseudoGlass` 新增 `pseudoBackground` 自定义底色参数。三档回退均保持原实色外观

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
