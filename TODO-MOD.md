# Eta Mod 待办

> 本 fork：`my-mods` 分支，applicationId `io.github.mangi.eta.mod`
> 远端：`origin` = Number444/Eta（推送目标），`upstream` = Mangi-11/Eta（官方源）
> 当前版本：3.3.3-004（versionCode 2026102602）
> 仅保留未实施、持续进行或暂缓事项；已完成记录见 git 历史与专题文档。排序：进行中 → 未开工 → 暂不考虑。

## 1. 分享长图新增手机 / 平板 / 电脑比例（待真机验收）

- **已实现**：分享按钮先打开三档选择，点击手机 / 平板 / 电脑即按 420dp / 600dp / 840dp 宽度离屏渲染；高度随内容增长，保留 12000px 高度上限并新增 1800 万像素预算。
- **自动验证**：`ShareCardRenderTest` 已覆盖三档宽度 PNG、选项回调与位图上限；三档渲染图已人工检查，未发现气泡、表格、代码块或长链接裁切。
- **待 Four 验收**：三档选择弹窗、实际 PNG 观感及系统分享流程，验收后移除本项。

## 2. App 全面 UI 焕新：液态玻璃（待真机验收）

- 主页、侧边栏、设置页及主要弹层已完成并经真机验收，架构与材质决策见 `docs/UI-GLASS-ARCHITECTURE.md`，不在待办重复存档。
- **已实现**：`ui/app/LocalNetworkPermissionUi.kt` 的局域网权限拒绝引导已从裸 `WindowDialog` 收口进 `EtaWindowDialog`，权限申请与设置跳转逻辑未改。
- **待 Four 验收**：拒绝局域网权限后检查设置引导在无模糊 / 默认 / 液态玻璃三档下的外观及取消、跳转行为，验收后移除本项；新视觉问题仍单独立项。

## 3. 首次安装引导向导（未开工，Compose 原生绘制）

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
