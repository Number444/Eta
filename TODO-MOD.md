# Eta Mod 待办

> 本 fork：`my-mods` 分支，applicationId `io.github.mangi.eta.mod`
> 远端：`origin` = Number444/Eta（推送目标），`upstream` = Mangi-11/Eta（官方源）
> 当前版本：3.3.3-005（versionCode 2026102603）
> 仅保留未实施、持续进行或暂缓事项；已完成记录见 git 历史与专题文档。排序：进行中 → 未开工 → 暂不考虑。

## 1. 首次安装引导向导（未开工，Compose 原生绘制）

- **现状**：Eta 没有 onboarding；首次启动直接进主页，未配置模型仅在发送时提示，权限需在设置页手动开启。
- **方向（Four 已拍板）**：使用 Compose 原生绘制，不采用 HTML/WebView；复用液态玻璃材质与现有表单，避免机型比例适配、JS 桥和 WebView 冷启动成本。
- **实施要点**：
  - 新增 `AppRoute.Onboarding`、完成标记与启动路由判断；向导壳包含步骤指示和状态机。
  - provider/API key/模型表单复用 `ModelProviderListScreen`、`ModelProviderDetailScreen` 与 `ProviderConfigDraft`，不重写表单。
  - 权限步骤复用 `SettingsPermissionsScreen` 的原生 intent 逻辑，覆盖悬浮窗、无障碍和通知。
  - 配置落地继续使用 `SettingsDataStore` 与 `RuntimeConfigRepository.setSelection`；安全水位不低于现状。
- **下一步**：待 Four 指令后确定步骤划分、状态机和复用组件接入方式，再实施。

## 备注

- 岛通知机制：`docs/ISLAND-EXPANSION-INVESTIGATION.md`，修改通知代码前必读。
- 玻璃架构：`docs/UI-GLASS-ARCHITECTURE.md`。
- 真机：小米 14 Ultra，adb serial `dcc44b47`。
