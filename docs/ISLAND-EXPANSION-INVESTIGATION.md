# Eta 岛通知「自动展开」全链路调查（2026-10-02）

> 调查对象：v3.0.6-008 起观察到的「开始任务 → 退出 App → 岛闪现展开 → 收起 → 已完成」链路。
> 方法：设置页「岛通知诊断测试」（T1–T10 变量隔离实验）+ logcat 全量抓取 + dumpsys notification 取证。
> 设备：小米 14 Ultra，HyperOS 3（Android 16），焦点通知权限已授予 Eta Mod。

## 一、最终结论

**岛自动展开不是我们的代码行为，也不是频道状态污染，而是 MIUI 系统行为的组合：**

```
App 在 eta_island 频道发带 miui.focus.param 的 ongoing 通知（+promoted/shortCriticalText）
  → MIUI FocusPlugin 识别为焦点通知 → 岛挂上胶囊（任意重要性频道均可）
  → 若频道在系统设置中「悬浮通知」已开启（eta_island 被历史修改为 imp=4 满足此条件）：
      首发 alert → HyperOS 3 把 heads-up 渲染为「岛展开」约 2 秒（声音/振动仅此时触发）
  → 收起为胶囊；同 id 更新（onlyAlertOnce）只改胶囊文字；终态「已完成」→ 8s 后撤销
```

**三个实锤（第一轮）+ 两个修正（第二轮）：**

1. ~~MIUI 自动提升频道~~（第一轮误判）。**修正**：新建频道跑完后 importance 与创建值完全一致，
   MIUI 不自动提升。`eta_island` 的 3→4 是历史设置修改，经 Android「已删频道设置复活」机制
   （删除后同 id 重建，系统恢复用户改过的字段）在 T8 重建后依然保留。
2. **焦点参数的「安静」标志压不住展开**：系统日志显示 `islandFirstFloat:false, enableFloat:false`
   被正确解析，岛照样展开——heads-up 岛化渲染与焦点协议的 float 标志是串联关系，不是互斥。
3. **展开的真正门槛**：频道在系统设置中「悬浮通知」实际开启（eta_island 满足，新建 HIGH 频道不满足）。
   首发 alert → 岛化 heads-up 展开约 2 秒 → 收起胶囊；声音/振动只在 alert 时触发。
4. **复位频道无法恢复「安静胶囊」**：删除重建会复活历史设置；只有用户手动调回或换新频道 id。

**推论**：展开与否完全取决于 eta_island 频道的历史设置状态，与 008 的代码改动无关——
「008 引入回归」不成立；频道设置一旦被调高，任何版本代码都会展现同样的首发展开。

## 二、T1–T10 观测对照表

| 步骤 | 配置 | 用户观测 | 系统日志佐证 | 结论 |
|---|---|---|---|---|
| T1 | 生产首发（eta_island 现状） | 自动展开「思考中」 | FocusPlugin addDynamicIslandView | 首发 alert → 岛化 heads-up |
| T2/T3 | 同 id 更新（onlyAlertOnce） | 收起，胶囊文字变「输出中/工具调用」 | 无新 alert | 更新不再触发展开 |
| T4 | 生产终态「已完成」 | 收起，胶囊文字变「已完成」 | 同上 | 终态也是更新，不展开 |
| T5 | 新建 DEFAULT 频道+焦点参数（缺 ongoing/promoted） | 无任何响应 | 模板已建但无 addDynamicIslandData；`focus notify not in list` | 上岛需 ongoing「活体」通知（生产三件套） |
| T6 | 新建 HIGH 频道+无焦点参数 | 无任何响应（未拉通知栏确认） | FocusPlugin 不处理；无 heads-up 痕迹 | 无焦点参数不上岛；新建频道横幅疑似被 MIUI 默认压制 |
| T7 | 新建 HIGH 频道+焦点参数（缺三件套） | 无任何响应 | 同 T5 | 同 T5 |
| T8 | eta_island 删重建(DEFAULT) 后生产首发 | 自动展开「思考中」后收起 | 重建后频道立即恢复 imp=4（已删频道设置复活，非 MIUI 再提升） | 展开与代码设定的频道重要性无关 |
| T9 | 重建后生产终态 | 「已完成」 | 同 T4 | — |
| T10 | 清理 | 保活通知消失 | — | — |

## 三、过程中发现并修复的问题

1. **v008 首版完成通知静默丢失**：`AgentCompletionNotifier.post()` 漏调 `ensureChannel()`，
   Android 8+ 对不存在的频道静默丢弃通知，无任何异常。v008 第二版修复。
2. **release 构建 logcat 空白**：proguard `maximumremovedandroidloglevel=3` 裁掉 VERBOSE/DEBUG；
   关键诊断日志须用 `Log.i` 及以上。
3. **MIUI 进程冻结导致后台定时任务中断**：岛终态 8s 自动撤销后，应用无前台组件即被冻结，
   Handler 延迟任务全部丢失。诊断运行器改用 `AgentExecutionService` 前台租约保活。

## 四、对「任务完成通知」需求的结论

- `eta_completion`（HIGH、有声有振、无焦点参数）**不会上岛**（T6 证实 FocusPlugin 只处理带焦点参数的通知）。
- 横幅形态取决于 MIUI 对该频道的悬浮通知策略，新建频道大概率被压制 → 实际表现为「声音+振动+通知栏静默收录」。
- 若要求澎湃上视觉确定可见，完成提醒需岛化（带焦点参数 + ongoing + 数秒后撤销），即复用生产岛机制。

## 五、遗留模糊点（第一轮后提出，第二轮已全部定案）

| # | 模糊点 | 第二轮结论 |
|---|---|---|
| Q1 | T6 的 HIGH 无参数通知是否有横幅/声/振 | **已解**：之前是用户通知权限未完全打开。权限补齐后 T11（eta_completion 复测）横幅+声音+振动全部生效 |
| Q2 | MIUI 是否自动提升频道重要性 | **推翻第一轮的「自动提升」结论**：eta_test2_low/default/high 三个新建频道跑完后 importance 与创建值完全一致（2/3/4，orig 相同），MIUI 不提升新建频道。eta_island 的 3→4 是历史人工/系统设置修改，经 Android「已删频道设置复活」机制在删除重建后依然保留（dumpsys 的 mUserLockedFields=14 与此吻合） |
| Q3 | 上岛必要条件 | **已解**：焦点参数 + ongoing + requestPromotedOngoing + shortCriticalText 三件套即可，与频道 id、频道重要性无关（LOW/DEFAULT/HIGH 新建频道均成功上岛为胶囊） |
| Q4 | MIUI 解析 updatable:false/reopen:close 与发送值不符 | **无实际影响**：T12b/T13b/T14b 同 id 更新文字均正常跟手 |
| Q5 | 真实完成通知的声/振 | **已解**：T11 验证通过（权限齐全前提下） |

## 七、第二轮新增结论（T11–T14，2026-10-02）

1. **胶囊（收起态岛）门槛**：焦点参数 + 生产三件套，任意重要性频道均可，LOW 频道也正常。→ 「安静胶囊」在技术上成立。
2. **展开（首发岛化 heads-up）门槛**：光 HIGH 不够——T14（新建 HIGH+参数+声音）只有胶囊无展开无声。
   展开需要频道在系统设置里「悬浮通知」实际开启（eta_island 的 imp=4+持久设置满足此条件）。
   **声音只在 alert（展开）时触发**，胶囊更新一律无声。
3. **eta_island 无法被 App 复位**：Android 对已删频道保留用户修改过的设置，同 id 重建即复活。
   想回安静胶囊只有两条路：用户在系统设置里调回，或换新频道 id 重建。
4. **完成通知（方案 A）验收通过**：eta_completion 频道 HIGH+声+振+横幅，权限齐全时全要素生效，代码无需再改。

## 六、环境备注

- 早期实验遗留频道 `eta_island_done`（HIGH + 自定义铃声 Message3）仍在系统中，当前代码不发往该频道。
  **处置决策（2026-10-02 Four 拍板）：保留**，仅设置页多一行，无害。
- 测试频道 `eta_test_*` / `eta_test2_*` 共 6 个已被删除（Android 仅为「已删频道设置复活」留档，设置页不可见）。
- 诊断日志已清理（三轮 logcat 留存文件用完已删）。

## 八、第三轮：目标链路模拟与实装（2026-10-02 定稿，v3.0.6-013）

**T20–T24 模拟（012 诊断包）**：LOW 安静频道胶囊「思考中→输出中→工具调用」（不展开）→ 完成瞬间
eta_island 换 id 1208 首发「已完成」（岛展开）+ eta_completion 横幅（声+振）同时出现 → 8s 撤销。
**真机全链路一次通过**，随后 013 实装、真实任务复验通过（含中途停止安静收尾）。

**实装架构（AgentIslandNotifier 双通道）**：

| 阶段 | 频道 | 通知 id | 行为 |
|---|---|---|---|
| 进行中（思考/输出/工具） | `eta_island_run`（LOW 安静） | 1207 | 胶囊跟手更新，不展开不发声，1.5s 节流 |
| 已完成 | `eta_island`（用户悬浮通知设置在此） | 1208（新 id 首发→展开） | 岛展开「已完成」，8s 撤销 |
| 失败/手动停止 | `eta_island_run` | 1207 | 安静收尾，不展开 |
| 已完成（并行） | `eta_completion`（HIGH+声+振） | 3400+runId | 横幅：标题=会话名，正文=回复摘要（AgentCompletionNotifier） |

**设计要点**：展开只能跟着「首发 alert」走（同 id 更新永不展开），所以完成态必须撤销胶囊换新 id；
展开与否的最终开关在用户系统设置（eta_island 的悬浮通知），App 只能选择发到哪个频道。
- 诊断日志留存：`D:\Agent Space\temp_inspect\eta-diag-live.log`（两轮完整 T1–T10）。
