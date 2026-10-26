# Eta Mod 待办

> 本 fork：`my-mods` 分支，applicationId `io.github.mangi.eta.mod`
> 远端：`origin` = Number444/Eta（推送目标），`upstream` = Mangi-11/Eta（官方源）
> 当前版本：3.3.3-005（versionCode 2026102603）
> 仅保留未实施、持续进行或暂缓事项；已完成记录见 git 历史与专题文档。排序：进行中 → 未开工 → 暂不考虑。

## 1. 首次安装引导向导（待真机验收，Compose 原生绘制）

- **已实现**：四步共用状态机“服务商 → 连接参数 → 模型 → 权限”，每页可跳过；真实完成或跳过后写首启标记，已有 API Key、OAuth、自定义服务商或手动/远端模型的老用户不强制进入。
- **复用与保存**：沿用 `ProviderConnectionFields`、`ProviderLoginStore`、`RemoteModelFetcher`、`ModelRepository.syncRemoteCatalog`、`SettingsDataStore` 和 `RuntimeConfigRepository`；页面复用 Miuix 骨架、标准间距和现有主题，无 HTML/WebView。
- **定稿排版**：左侧 33sp Bold“设置ETA”配右侧圆形进度，短内容垂直居中、长列表自然滚动；卡片与按钮使用假玻璃，主操作独占一行，Back / Skip 位于下一行，按钮区两侧与底边距一致；Four 已通过渲染图确认。
- **测试入口**：设置一级页新增“测试初始化向导”，复用同一页面但拦截持久化、登录、网络拉取和系统跳转；仅推进内存状态并发送不含密钥的通知，未授权通知时 Toast 明示。
- **自动验证**：首启策略、路由序列化、四步零写入、系统通知、真实模型落库和真实跳过均已有测试；四页渲染图已检查。
- **待 Four 验收**：新装真实远端配置、老版升级、各页跳过、测试前后配置不变和通知可达，以及明暗主题 / 三档模糊下的比例与键盘操作；验收后移除本项。

## 备注

- 岛通知机制：`docs/ISLAND-EXPANSION-INVESTIGATION.md`，修改通知代码前必读。
- 玻璃架构：`docs/UI-GLASS-ARCHITECTURE.md`。
- 真机：小米 14 Ultra，adb serial `dcc44b47`。
