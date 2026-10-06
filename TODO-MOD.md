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

## 2. 检查更新指向自己的仓库

- [ ] `app/src/main/kotlin/io/github/mangi/eta/data/update/AppUpdateChecker.kt:67`
      `LATEST_RELEASE_URL` 改为 `https://api.github.com/repos/Number444/Eta/releases/latest`
- [ ] **版本比较注意（v3.2.0 新行为）**：`AppUpdateChecker` 现在比较"当前版本"前会剥掉 `-`/`+` 后缀（`3.2.0-001` → `3.2.0`），但"最新版本"一侧不剥。改指向自己仓库后，我们的 release tag（如 `3.2.0-001`）作为 latest 传入比较时要确认语义：理想是 latest 侧也剥后缀或按我们的版本号规则单独处理，改前读该文件全文。
- [ ] 可选：设置页「关于」里的仓库/反馈链接（`SettingsScreen.kt` 搜 `github.com/Mangi-11`）是否一并指向 Number444/Eta

## 备注

- 发布流程由 Four 手动执行（GitHub Releases 页面），艾薇只负责出包与提交
- 岛-通知机制结论见 `docs/ISLAND-EXPANSION-INVESTIGATION.md`，动通知相关代码前必读
- 真机：小米 14 Ultra，adb serial `dcc44b47`
- v3.2.0 merge 裁决记录（D1-D5）：
  - D1 搜索：保留 Exa，设置页新增「搜索引擎」选择（官方免 Key / Exa）；Exa 选中但无 key 时运行时回退官方
  - D2 分享图片：本次砍除（`ui/share/` 已删），后续按新 UI 重写
  - D3 思考折叠：开关合并进上游结构（`rememberAutoExpandThinking()` 挂上游 ThinkingRow 与 AgentWorkProcess）
  - D4 侧栏：保留我们的精简版（未引入上游 PaneDock；Skills 仍可从设置页进入）
  - D5 压缩模型：专用压缩模型在上游新 `compact()` 结构上重写（无超限重试循环；专用失败回退主模型）
