# Eta Mod 待办

> 本 fork：`my-mods` 分支，applicationId `io.github.mangi.eta.mod`
> 远端：`origin` = Number444/Eta（推送目标），`upstream` = Mangi-11/Eta（官方源）
> 当前版本：3.3.0-001（versionCode 2026101301，merge 官方 v3.3.0 后第一个 Mod 修订）

> 已完成归档（2026-10-08 精简）：1. merge 上游 v3.2.0（30 commits，16 冲突手工裁决，出包真机回归通过）；2. 更新检查/关于页链接指向 Number444/Eta（`isNewer` 支持 `-NNN` 修订号）；3. 删官方搜索只留 Exa（官方服务本机超时不可用）；4. 消息操作行按钮放大 1.4 倍；5. 分享成图按新 markdown 层重写（含 `ShareCardRenderTest` 渲染基线）；6. merge 上游 v3.3.0（2026-10-13 完成：21 commits，7 冲突裁决 + `AgentAppState` 四功能块移植；暗礁=双方 `MIGRATION_21_22` 撞车，上游 `model_id` 迁移顺延 22_23、DB 升 v23；48 个单测失败对撞全为 Windows 环境基线零回归，出包真机冒烟通过）。详细过程见 git 历史。

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

## 8. GUI run 胶囊吞点击 → 运行控制三键移交超级岛（方案已定，待实施）

- 问题（2026-10-13 v3.3.0-001 冒烟发现）：上游 v3.3.0 新运行胶囊窗口**可触摸**（有展开/暂停/停止按钮，窗口固定宽 × 分档高、顶部居中），agent 注入的坐标点按落在胶囊区域即被吞，点不到下层应用内容；上游自己在 `AgentRuntimeService.kt:1007` 注释里承认会吞但未真解决。边缘流光（全屏 `FLAG_NOT_TOUCHABLE`）与 `GestureIndicator` 均清白；`ui_nodes` 走 `rootInActiveWindow`，胶囊不可获焦不进节点树，故仅坐标点按受影响
- 方案（Four 拍板）：GUI run 期间胶囊 + 结果卡片切 `FLAG_NOT_TOUCHABLE` 纯展示，控制职能移交超级岛；岛 action 按钮渲染已经 Four 真机验证可行（`dispatchGesture` 无目标窗口概念，「点到下层」只能让胶囊穿透实现）
- 实施要点：
  - `AgentIslandNotifier` 运行中通知加三个 action：暂停/继续（随暂停态翻转）+ 插入 + 停止——复用现有停止 action 模式（`AgentIslandNotifier.kt:330-343`）
  - 岛按钮 PendingIntent 须打到 `AgentRuntimeService`（GUI run 会话所在，`requestPause/requestResume/requestSupplement` 现成于 `:768/:776/:784`）；现有岛停止 action 只打 `AgentExecutionService`（App 侧 run），需统一广播或双服务分发
  - 「插入」键收不了文本，唤起浮层直进 supplement 输入模式（`enterSupplementMode` 现成）
  - run 结束恢复窗口可触摸（结果卡片弹出需要交互，时机衔接）
  - 兜底：即便岛不渲染，通知栏通知的 action 按钮是标准 Android 能力必可用
- 备忘：属上游设计缺陷，找时机给上游提 issue

## 9. 侧栏设置圆形按钮无点击震动（Four 标：先不修）

- 现象（2026-10-13 冒烟发现）：侧栏搜索行右侧设置按钮点击大概率无震动。非 merge 回归——`ConversationPanePanel.kt:324` 裸 `Modifier.clickable` 从未挂显式震动（同文件会话行 `:406` 有 `KeyboardTap`），merge 前后逐字节相同
- 修法（一行）：`onClick` 里补 `performHapticFeedback(HapticFeedbackType.KeyboardTap)` 与会话行对齐

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
