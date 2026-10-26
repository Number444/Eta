# Eta Mod 待办

> 本 fork：`my-mods` 分支，applicationId `io.github.mangi.eta.mod`
> 远端：`origin` = Number444/Eta（推送目标），`upstream` = Mangi-11/Eta（官方源）
> 当前版本：3.3.3-003（versionCode 2026102601）
> 仅保留未实施、持续进行或暂缓事项；已完成记录见 git 历史与专题文档。排序：进行中 → 未开工 → 暂不考虑。

## 1. App 全面 UI 焕新：液态玻璃（进行中）

- 主页、侧边栏、设置页及主要弹层已完成并经真机验收，架构与材质决策见 `docs/UI-GLASS-ARCHITECTURE.md`，不在待办重复存档。
- [ ] 将 `ui/app/LocalNetworkPermissionUi.kt` 的局域网权限引导从裸 `WindowDialog` 收口进 `EtaWindowDialog`，保持三档模糊回退一致。
- 新发现的视觉问题单独立项，修复并经 Four 验收后及时移除，不累计完成记录。

## 2. 首次安装引导向导（未开工，Compose 原生绘制）

- **现状**：Eta 没有 onboarding；首次启动直接进主页，未配置模型仅在发送时提示，权限需在设置页手动开启。
- **方向（Four 已拍板）**：使用 Compose 原生绘制，不采用 HTML/WebView；复用液态玻璃材质与现有表单，避免机型比例适配、JS 桥和 WebView 冷启动成本。
- **实施要点**：
  - 新增 `AppRoute.Onboarding`、完成标记与启动路由判断；向导壳包含步骤指示和状态机。
  - provider/API key/模型表单复用 `ModelProviderListScreen`、`ModelProviderDetailScreen` 与 `ProviderConfigDraft`，不重写表单。
  - 权限步骤复用 `SettingsPermissionsScreen` 的原生 intent 逻辑，覆盖悬浮窗、无障碍和通知。
  - 配置落地继续使用 `SettingsDataStore` 与 `RuntimeConfigRepository.setSelection`；安全水位不低于现状。
- **下一步**：待 Four 指令后确定步骤划分、状态机和复用组件接入方式，再实施。

## 3.【暂不考虑实现】分享图片进 Eta → 模型识别 → 自主生成岛卡片

- **调研结论**：
  - Manifest 尚无 `ACTION_SEND`/`ACTION_SEND_MULTIPLE` 接收入口；需由 MainActivity 或透明 Bridge Activity 接收 `image/*`，解析 `EXTRA_STREAM`/`clipData`，并立即把临时授权的 `content://` 图片复制到私有目录。
  - 图片可通过 `ModelImage(reference=uri)` 接入现有 `AgentAppState` 发送链路，复用 `AgentImageCodec` 和模型消息编码。
  - 新增 `show_island_card` 工具可让模型按契约自主展示卡片；首选复用 `AgentIslandNotifier` 的岛通知通道，先验证模板能否表达大字码与固定排版，不足时再考虑自绘 overlay。
- **若重新启动，待实施**：分享接收与图片落盘、识别 JSON 契约、工具声明与执行、码卡片 UI、复制/收起/关闭及持久化。
- **注意**：MIUI 悬浮窗与后台弹出界面权限、Android 12+ overlay `alpha=1`、前台服务保活；动手前重读 `docs/ISLAND-EXPANSION-INVESTIGATION.md` 并重新确认方案。

## 备注

- 岛通知机制：`docs/ISLAND-EXPANSION-INVESTIGATION.md`，修改通知代码前必读。
- 玻璃架构：`docs/UI-GLASS-ARCHITECTURE.md`。
- 真机：小米 14 Ultra，adb serial `dcc44b47`。
