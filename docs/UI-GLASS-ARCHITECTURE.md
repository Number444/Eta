# Eta Mod UI 玻璃架构（液态玻璃焕新）

> 2026-10-16 焕新计划第一步（架构期）产物。Four 定调：Window 系弹层 = 只带高光的半透明假玻璃；其余 = 真玻璃（为后续自定义图片背景铺路）；先架构后样式。
> 架构期验收标准：视觉与 v3.3.0-009 完全一致。

## 双层模糊体系

| 体系 | 库 | 门控 | 用途 |
|---|---|---|---|
| miuix 渐进模糊 | `top.yukonga.miuix.kmp.blur` | `LocalBlurEnabled`（blurMode ≠ NONE） | 顶栏滚动消隐（TopBarBackdrop） |
| 液态玻璃 | `com.kyant.backdrop` | `LocalLiquidGlassEnabled`（blurMode == LIQUID_GLASS） | 玻璃控件/弹层 |

两套录制层可嵌套共存（主页已验证）：miuix 的 `captureForTopBar` 与 Kyant 的 `liquidGlassSource` 标在不同/相同节点均可用。

## 材质 Token 层（`ui/components/EtaGlassTokens.kt`）

- `EtaGlassStyle`：形状 / blur 半径 / lens 折射参数 / tint 与假玻璃透明度。
- 预设：`FloatingButton`（圆钮）、`InputBar`（圆角条）、`Popup`（弹层，对齐 miuix 16dp）、`DialogCard`（弹窗 32dp）、`ListRow`（弱化材质，长列表防掉帧）。
- `Modifier.etaGlass(style, backdrop, tint)`：真玻璃，backdrop 为 null 时 no-op。
- `Modifier.etaPseudoGlass(style)`：假玻璃（半透明底 + 顶部高光渐变描边），纯 Compose，独立 Window 可用；仅液态玻璃档启用。
- 硬约束：Kyant lens 只支持 `CornerBasedShape`/`RoundedRectangularShape`，通用 Shape 抛 `UnsupportedOperationException`（010 弹层闪退事故）——玻璃轮廓只能用圆角/圆形。
- `LiquidGlass.kt` 的 `liquidGlassCircle`/`liquidGlassRounded` 是预设的薄封装，现存 9 块首页玻璃控件走这里。

## 多采样源 Surface 模式

**规矩：每个 surface 一个 backdrop 提供者；录制层内不得有任何 drawBackdrop 消费者（渲染回路爆炸）；消费者读就近的 `LocalLiquidGlassBackdrop`（嵌套 surface 天然遮蔽外层）。**

| Surface | backdrop 创建处 | 采样源（liquidGlassSource） | 消费者 |
|---|---|---|---|
| 主页 | `AgentAppShell.kt`（HomeSurface） | 消息列表/空态（`AgentChatBody.kt`） | 顶栏双按钮、输入栏 7 控件 |
| 侧边栏面板 | `ConversationPanePanel.kt` | 会话 LazyColumn | （阶段二：搜索框/设置钮等） |
| 设置骨架 | `MiuixScaffoldPage.kt`（MiuixScaffoldPage + MiuixScaffold 两处，31 个二级页全覆盖） | 内容 LazyColumn / 内容 Box | （阶段三：顶栏返回钮等固定元素；卡片在录制层内，只能假玻璃） |

未来自定义图片背景：在 `AgentAppRoot` 层增设根 surface backdrop 录制背景层，各 surface 玻璃直接采样（`AgentAppShell.kt` 有注释锚点）。

## 弹层收口与宿主约束（`ui/components/EtaPopups.kt`）

全 app 列表弹层只走 `EtaListPopup`：

- 默认 overlay 实现（真玻璃能力）：**调用点上方必须有 miuix Scaffold 提供 overlay 宿主**，否则状态进了无人渲染的默认列表，弹层静默不出现（miuix `LocalDialogStates`/`LocalPopupStates` 默认空列表）。
- `useWindow = true` 走 Window 变体：无 Scaffold 宿主的区域（侧边栏面板、根级）使用；Window 采不到应用内 backdrop，材质只能假玻璃。

当前接线：

- overlay（主页 Scaffold 内）：顶栏溢出菜单、附件菜单、思考强度、模型选择（`AgentAppShell` / `AgentChatFileAttachments` / `AgentChatInputBar` / `AgentChatModelControls`）。
- useWindow（面板在 Scaffold 之外）：侧边栏会话长按菜单（`ConversationPanePanel.kt`）。
- 弹窗：路径输入（`AgentChatFileAttachments`）→ `EtaOverlayDialog`（Scaffold 内，IME 行为需真机盯）；根级 5 弹窗（角色通知/重命名/删除会话/删除消息/重新生成，`AgentAppRoot.kt`）在 NavDisplay 之外无宿主，**保持 WindowDialog 不动**，阶段二套假玻璃时统一收进 `EtaWindowDialog`。

## 真玻璃 vs 假玻璃决策矩阵

| 场景 | 材质 | 原因 |
|---|---|---|
| 主页固定控件 + overlay 弹层 | 真玻璃 | 消息层在录制中，有真东西可模糊 |
| Window 系弹层（侧边栏/设置页/根级弹窗） | 假玻璃 | 独立 Window 采不到应用内 backdrop |
| 设置页卡片 | 假玻璃 | 卡片在录制层内（嵌套禁令），且背后是纯色页面底 |
| 长列表项 | ListRow 弱化或假玻璃 | 整列 drawBackdrop 滚动掉帧风险 |
| 设置页顶栏返回钮等固定元素 | 真玻璃 | 在录制层外，可采样滚动列表 |

## 性能与降级

- 三档模糊设置（无/默认/液态玻璃）即降级路径；`etaGlass` backdrop 为 null 时 no-op。
- 每个录制层都是 GPU 开销：侧边栏与设置骨架的源在液态玻璃档随滚动持续重录，滚动掉帧为观察项。
- 假玻璃零采样成本，任意位置可用。

## 阶段路线图

1. ~~架构期（本次）：Token 层 + 弹层收口 + 多采样源，零视觉变化。~~
2. ~~主页（2026-10-17，v3.3.0-010）：overlay 弹层真玻璃注入。~~ 最终方案 = **内容侧注入**（`EtaGlassPopupContainer` 包住弹层内容，恰好覆盖弹层可视区，盖在容器实色底上），未走 MiuixTheme 覆写——`MiuixPopupHost` 在宿主上下文调起 content，调用点 CompositionLocal 覆写传不进容器层，容器背景无法经主题改透明。轮廓用 16dp 圆角（曾尝试 squircle 对齐容器 clip，但 Kyant lens 只支持 CornerBasedShape，通用 Shape 闪退，已回退；squircle 与圆角在 16dp 下差异亚像素级）。已知结构事实：容器实色底残留在玻璃下方（被完全遮住）；dim 遮罩不在录制层内，玻璃观感比周围略鲜亮（调整手段：Popup tintAlpha）。
3. 侧边栏：搜索框/设置钮/选中行/长按弹窗套材质（面板内 = 面板 backdrop 真玻璃或假玻璃，按效果定）。
4. 设置页：返回钮真玻璃（1 处组件）、卡片假玻璃（2 个定义点）、弹窗假玻璃（3 个收口点）。
5. 自定义图片背景：根 surface backdrop + 各页面背景透明化。
