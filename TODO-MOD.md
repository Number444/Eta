# Eta Mod 待办

> 本 fork：`my-mods` 分支，applicationId `io.github.mangi.eta.mod`
> 远端：`origin` = Number444/Eta（推送目标），`upstream` = Mangi-11/Eta（官方源）
> 当前版本：3.2.0-001（versionCode 2026100701，merge 官方 v3.2.0 后）

## 1. 跟进官方版本：选择性 merge + 提升版本号

- [x] ~~`git fetch upstream`，对比官方最新 tag 与 `my-mods` 的差异~~（2026-10-07 已跟进至 v3.2.0，30 commits）
- [x] ~~选择性 merge/cherry-pick~~（采用 `git merge --no-commit --no-ff upstream/main` 整体合并 + 16 冲突手工裁决）
- [x] 冲突高发区（本次 mod 改过）：
  - `agent/runtime/AgentIslandNotifier.kt`（双通道重写）→ 上游未动，零冲突保留
  - `agent/runtime/AgentCompletionNotifier.kt`（新增）→ 同上
  - `agent/runtime/AgentRuntimeRunExecutor.kt`（钩子调用）→ 冲突已解（island onRunStarted 保留）
  - `ui/SettingsScreen.kt`（诊断入口）→ 自动合并 + 新增搜索引擎选择条目
  - `res/values*/strings.xml`（新增字符串）→ 自动合并（撞名 `tool_web_search` 已去重）
- [x] 提升版本号：`versionCode 2026100701`、`versionName "3.2.0-001"`
- [ ] 出包验证：`gradlew.bat :app:assembleRelease --no-configuration-cache --console=plain`，装真机回归岛-通知链路

## 2. 检查更新与外链指向自己的仓库

- [ ] `app/src/main/kotlin/io/github/mangi/eta/data/update/AppUpdateChecker.kt:67`
      `LATEST_RELEASE_URL` 改为 `https://api.github.com/repos/Number444/Eta/releases/latest`
- [ ] **版本比较注意（v3.2.0 新行为）**：`AppUpdateChecker` 现在比较"当前版本"前会剥掉 `-`/`+` 后缀（`3.2.0-001` → `3.2.0`），但"最新版本"一侧不剥。改指向自己仓库后，我们的 release tag（如 `3.2.0-001`）作为 latest 传入比较时要确认语义：理想是 latest 侧也剥后缀或按我们的版本号规则单独处理，改前读该文件全文。
- [ ] 设置页「关于」里的**源代码仓库**与**问题反馈**链接一并指向 Number444/Eta（`SettingsScreen.kt` 搜 `github.com/Mangi-11`）

## 3. 删除官方 search，只留 Exa

- [ ] 背景：官方免 Key 搜索在本机网络正常的情况下 `web_search` 连续三次超时（WEB_TIMEOUT）——设备联网没问题，是官方搜索服务这一环连不上（大概率墙/路由问题）
- [ ] 移除 D1 加的「搜索引擎」选择条目与 `AGENT_WEB_SEARCH_ENGINE` 相关代码（`Prefs.kt`、`AgentWebSearchToolCatalog.exaSelected()`、`AgentToolCatalog`、`AgentLocalTools` 分发、SettingsScreen UI、三语言字符串）
- [ ] 恢复为纯 Exa 通道（合并前 mod 的行为）：`web_search` 仅在 Exa key 已配置时注册
- [ ] 上游 `agent/web/`（PublicWebSearch 等）与 `fetch_url` 是否一并裁掉再评估：fetch_url 读网页正文不依赖搜索服务，可保留
- [ ] 注意：上游以后继续动这块时，merge 冲突高发区就是这些文件

## 4. 恢复对话输出下方按钮大小

- [ ] 助手消息下方的操作按钮（复制/重试/朗读等）恢复为当前直径的约 **1.4 倍**（上游 3.2.0 缩小了，找回 mod 之前的大小）
- [ ] 位置：`ui/components/ChatMessageItem.kt`（或消息操作行所在组件），先定位上游把尺寸写在哪，改回 mod 值
- [ ] 顺手确认间距/对齐不被放大撑坏（Four 对视觉敏感，改完截图对比）

## 5. 重写分享成图功能（先调研再定方案）

- [ ] 背景：旧实现（`ui/share/MessageShareImage.kt` + `ShareTurn.kt` + FileProvider）已随 v3.2.0 merge 删除
- [ ] 调研方向：
  - 新 UI 结构下从哪拿渲染数据（上游自建 `ui/markdown/` 渲染层，不再有 mikepenz）
  - 方案 A：Compose 离屏渲染（ComposeView + software bitmap），保真度最高
  - 方案 B：WebView/Canvas 自绘，灵活但维护重
  - 长图分页、深色模式、代码块/表格渲染一致性
- [ ] 与 Four 一起定方案后再动工，不抢跑

## 6. 无 root 下的 Eta 系统级唤醒方案调研

- [ ] 背景：本机未 root，无法替换小爱作为系统默认助手（电源键/手势唤醒默认助理这条路过不去）
- [ ] 调研候选：
  - 快捷设置磁贴（Quick Settings Tile，下拉控制中心一键唤起）
  - 桌面快捷方式 / App Shortcuts（长按图标直达语音面板）
  - 小米/澎湃 OS 侧：背部轻敲、悬浮球、自由窗口等系统手势能否绑定第三方 App 动作
  - `android.app.role.ASSISTANT` / `VoiceInteractionService` 在无 root 下能成为默认数字助理到什么程度（长按 Home/手势导航下的助理按钮）
  - 辅助功能/音量键长按等（评估合规与稳定性）
- [ ] 产出结论：哪条路在 HyperOS 上可行、体验最接近"电源键唤小爱"，再实现

## 备注

- 发布流程由 Four 手动执行（GitHub Releases 页面），艾薇只负责出包与提交
- 岛-通知机制结论见 `docs/ISLAND-EXPANSION-INVESTIGATION.md`，动通知相关代码前必读
- 真机：小米 14 Ultra，adb serial `dcc44b47`
- v3.2.0 merge 裁决记录（D1-D5）：
  - D1 搜索：保留 Exa，设置页新增「搜索引擎」选择（官方免 Key / Exa）→ **已被事项 3 推翻**：官方搜索在本机超时不可用，将回退为纯 Exa
  - D2 分享图片：本次砍除（`ui/share/` 已删），后续按新 UI 重写
  - D3 思考折叠：开关合并进上游结构（`rememberAutoExpandThinking()` 挂上游 ThinkingRow 与 AgentWorkProcess）
  - D4 侧栏：保留我们的精简版（未引入上游 PaneDock；Skills 仍可从设置页进入）
  - D5 压缩模型：专用压缩模型在上游新 `compact()` 结构上重写（无超限重试循环；专用失败回退主模型）
