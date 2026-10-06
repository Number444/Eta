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

- [x] `AppUpdateChecker.kt` 的 `LATEST_RELEASE_URL` 改为 `https://api.github.com/repos/Number444/Eta/releases/latest`（2026-10-08 完成）
- [x] **版本比较**：`isNewer` 重写——两侧统一剥 v 前缀/`+`元数据/`-`后缀，基础版本按段比较，相同再比 Mod 修订号（`-NNN`，缺省 0）；`AppUpdateCheckerTest` 新增 7 条修订号用例
- [x] 设置页「关于」的源代码仓库与问题反馈链接指向 Number444/Eta（`SettingsScreen.kt` 两处 `openUrl`）

## 3. 删除官方 search，只留 Exa

- [x] 背景：官方免 Key 搜索在本机网络正常的情况下 `web_search` 连续三次超时（WEB_TIMEOUT）——设备联网没问题，是官方搜索服务这一环连不上（大概率墙/路由问题）
- [x] 移除「搜索引擎」选择条目与 `AGENT_WEB_SEARCH_ENGINE` 相关代码（`Prefs.kt` 3 个 key、`exaSelected()`→`exaConfigured()`、`AgentToolCatalog`、`AgentLocalTools` 分发、SettingsScreen UI、三语言字符串）
- [x] 恢复为纯 Exa 通道：`web_search` 仅在「网页搜索开关开 + Exa key 已配置 + 非托管搜索」时注册；prompt 里的本地 web_search 提示同样按此门控（`AgentPromptBuilder`）
- [x] `fetch_url` 与上游 `agent/web/` 保留（读网页正文不依赖搜索服务）；官方搜索仅不注册，代码未删
- [x] 注意：上游以后继续动这块时，merge 冲突高发区就是这些文件（已改测试：`AgentModelClientLoopTest`/`AgentToolCatalogTest` 断言）

## 4. 恢复对话输出下方按钮大小

- [x] 助手消息操作行放大到上游 1.4 倍：图标 15→21dp、触控 30→42dp（`ChatMessageItem.kt` 复制/编辑/重试/删除 + `SpeechControls.kt` 朗读同步 42/21dp，停止图标 18dp）
- [x] 行偏移 `-8dp→-6dp` 保持图标中心线对齐
- [ ] 真机截图对比确认间距/对齐（等 Four 过目）

## 5. 重写分享成图功能（先调研再定方案）

- [x] 调研结论（2026-10-07）：**老路子依然是最简方案**，且新自建 markdown 层让它更顺——详见下方要点
- 调研要点：
  - 老实现 = 离屏 ComposeView（INVISIBLE 挂 decorView）→ 预解析 markdown → UNSPECIFIED 测量 → `draw(Canvas(bitmap))` → FileProvider 分享；与 UI 层解耦，merge 只删除了文件，技术本身没失效
  - 新 markdown 层完全适配：`StreamingGfmParserSession().parse(content, isComplete=true)` 同步解析（同 module internal 可直接调），`MarkdownContent(document, style)` 纯 Compose 渲染；替代 mikepenz 的 `State.Success` 预解析即可
  - 离屏依赖扫描干净：无 Coil/网络图/WindowInsets；块类型仅段落/标题/代码/引用/提示/列表/表格/分割线（无图片块）
  - 消息 UI 模型未变（`AgentMessageUi.renderMarkdown`、`ToolActivityMessageUi`、`ToolSummaryMessageUi` 都在），`ShareTurn.kt` 的组装逻辑可直接复活
  - 唯一要小改的：代码块右上角有复制按钮（`MarkdownContent.kt` CopyButton），分享图里应隐藏——加一个静态模式开关
- [x] 实现（2026-10-07）：`ShareTurn.kt`/`eta_share_paths.xml`/manifest FileProvider/`share_as_image` 三语言原样复活；`MessageShareImage.kt` 换用新 markdown 层（`StreamingGfmParserSession` 同步预解析 + `MarkdownContent` 渲染 + `LocalMarkdownStaticExport` 隐藏复制按钮）；操作行加分享按钮（与放大后操作行同套 42/21dp），`AgentChatBody` 按轮组装
- [ ] 真机验证出图效果（等 Four 过目）

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
