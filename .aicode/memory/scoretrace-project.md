---
name: scoretrace-project
description: ScoreTrace（高考备考 App）项目约定：包名拼写、技术栈版本、镜像与构建方式
---
# ScoreTrace 项目约定

- 用途：高考备考应用；应用名 ScoreTrace，根项目名 ScoreTrace。
- 包名 / namespace / applicationId：`com.fenji.scorcetrace`
  - **注意拼写是 `scorcetrace`，不要"纠正"成 `scoretrace`**（用户明确要求）。
- 技术栈：Kotlin + Jetpack Compose(Material 3) + MVVM + Hilt + Retrofit + Room + Coroutines/Flow。
- 版本（`gradle/libs.versions.toml`）：Gradle 8.11.1 / AGP 8.7.3 / Kotlin 2.0.21 /
  KSP 2.0.21-1.0.28 / Hilt 2.52 / Room 2.6.1 / Retrofit 2.11.0 / OkHttp 4.12.0 /
  Compose BOM 2024.10.01 / Navigation 2.8.3 / Lifecycle 2.8.7 / coroutines 1.9.0 /
  DataStore Preferences 1.1.1（高考日期 + 音乐偏好）/ Media3 1.9.4（音乐播放）。
- minSdk 24 / targetSdk 35 / compileSdk 35；`buildToolsVersion` 必须显式写 "35.0.0"。
- 仓库全走阿里云镜像（本容器 dl.google.com 不可达）：settings.gradle.kts 的
  pluginManagement 与 dependencyResolutionManagement 都用
  `https://maven.aliyun.com/repository/{google,central,gradle-plugin}`。
  Hilt / Retrofit / OkHttp / coroutines 等从 central 取，androidx 从 google 取。
- 构建命令：`JAVA_HOME=/usr/lib/jvm/java-17-openjdk-arm64 ./gradlew assembleDebug`
  （local.properties 里 sdk.dir=/root/android/sdk）。首次构建约 13 分钟，增量约 30 秒。
- 分层：`data/{local,remote,repository}`、`ui/{screen,component,theme,navigation}`、`di`、`util`。
  未单独建 domain 层——仓库以 interface + Default*Impl（@Binds 绑定）充当领域边界。
- Room：`java.util.Date` 由 `Converters` 转 epoch 毫秒；`ScoreTraceDatabase` version=1、exportSchema=false。
  建库回调里 `DatabaseModule.seedDefaultSubjects()` 播种 9 个默认科目（Constants.DEFAULT_SUBJECT_NAMES）。
- 屏幕不加嵌套 Scaffold：`AppNavHost` 的 Scaffold 负责状态栏/底部栏内边距，各页面用 `ScreenHeader` 作标题栏。
- 启动图标：`mipmap-anydpi-v26/ic_launcher(.round).xml` 自适应图标 + `mipmap-anydpi/` 矢量回退（API24/25）。
- 高考日期（倒计时目标）：存 DataStore —— `data/local/UserPreferences.kt`，preferences 文件名 `user_preferences`，
  key `gaokao_timestamp`，值 = 本地时区当天 0 点毫秒。默认值由 `DateUtils.defaultGaokaoTimestamp()` 推导（今年 6/7，已过则顺延次年）。
  `DateUtils.localMidnightFromUtcDate()` / `utcDateFromLocalTimestamp()` 负责 Material3 DatePicker 的 UTC 毫秒与本地 0 点互转。
- 首页倒计时：`HomeViewModel.countdownState: StateFlow<CountdownUiState>`（每秒 ticker 与 DataStore 流 combine，改日期自动重算）。
  `HomeScreen.CountdownCard` 用 `rememberInfiniteTransition`+`animateFloat` 做呼吸背景、秒数用 `AnimatedContent` 做缩放淡入淡出；卡片 `onClick` 目前是空实现（预留跳设置页）。
- 设置页顺序：深色主题 → 高考日期 → 关于 → 清除全部数据；「高考日期」点击弹 `DatePickerDialog` + `DatePicker`（需 `@OptIn(ExperimentalMaterial3Api::class)`）。
- 音乐播放：`ui/music/MusicPlayerViewModel.kt`（Hilt；`@ApplicationContext` 建 ExoPlayer，切歌源 `Constants.DEFAULT_MUSIC_URL`，
  `REPEAT_MODE_ALL` 循环，`onPlayerError` 暂停 + 提示，`onCleared()` release；暴露 `isPlaying`/`isBuffering`/`errorMessage`/`currentTrackTitle`/`player`）。
  `UserPreferences.autoPlayMusic`（key `auto_play_music`，默认 true）；设置页开关直接走 SettingsViewModel 写库（**不要在设置页再建 MusicPlayerViewModel，会多出一个 ExoPlayer**）。
  首页卡片用 `androidx.media3.ui.compose.material3.buttons.PlayPauseButton(player, modifier, painter, contentDescription, tint, onClick)`（1.9.x 起改为直接传 Player，不再是传 state）。
  **Media3 版本选择（已实测）**：`media3-ui-compose-material3` 从 **1.9.0** 才有（1.4/1.5/1.6/1.7/1.8 根本不存在）；
  1.9.3/1.9.4 的 aar `minCompileSdk=35`（可用），**1.11.x 要 minCompileSdk=36**（要升 AGP 才能用）→ 固定 1.9.4。
  Media3 的 `@UnstableApi` 是 `androidx.annotation.RequiresOptIn(ERROR)`，只由 lint 检查、编译器不拦；已在 app/build.gradle.kts 的 `lint { disable += "UnsafeOptInUsageError" }` 处理。
- 首页自动播放：`LaunchedEffect(Unit)` + `viewModel.uiState.filter { !it.isLoading }.first()` 等偏好读出后再 `play()`（直接用 `LaunchedEffect(Unit)` 配初始值会有竞态）；离页用 `DisposableEffect` 调 `pause()`。
- 播放器卡片 UI（已重构，2026-09）：已抽到 `ui/music/component/MusicPlayerCard.kt`（公开组件，HomeScreen 只负责调用）。
  设计令牌：深色渐变 `0xFF1E1E2E → 0xFF2D2D44`、`RoundedCornerShape(28.dp)`、`BorderStroke(1.dp, White 20%)`、`shadowElevation = 8.dp`；
  封面 70dp/16dp 圆角 + `0xFF5B86E5 → 0xFF36D1DC` 渐变 + `Icons.Rounded.MusicNote`；胶囊状态标签（循环播放/加载中/已暂停/加载失败）；
  进度条 `LinearProgressIndicator(progress = {..}, height=4.dp, strokeCap=Round, drawStopIndicator = {})`；三键居中（prev/play/next）+ MoreHoriz 靠右。
  动效：`animateFloatAsState` 做封面缩放、`animateColorAsState` 做播放按钮底色/图标色过渡。
- **依赖新增**：`material-icons-extended`（`Icons.Rounded.*` 必需；aar 35.7MB，debug APK 因此 +7MB，dex 变 15 个）。未引入 Coil（当前没有任何封面图 URL，按“如果”条件跳过）。
- `Constants.DEFAULT_MUSIC_TITLE` 已拆分：TITLE = "Whisper Of Hope"，ARTIST = "备考轻音乐"（旧值 "备考轻音乐 - Whisper Of Hope" 已不存在）。
- `MusicPlayerViewModel` 新增 `progress: StateFlow<Float>`（500ms 轮询 position/duration，**必须在主线程访问 ExoPlayer**，viewModelScope 是 Main 所以安全）与 `currentTrackArtist`；
  已删除供旧 `PlayPauseButton` 用的 `player` 访问器。**`media3-ui` / `media3-ui-compose-material3` 现在已无人使用**（保留未删，如需瘦身可移除这两个依赖）。
- 已知待办：设置页"深色主题"开关目前只存内存，未持久化也未驱动根主题（可复用 UserPreferences 接 DataStore）。

## 已安装技能（项目级 `~/workspace/.aicode/skills/`，2026-09）
- 11 个：`android-mad`、`android-kotlin-development`、`android-development`、`android-ux`、`android-compose-performance`、`android-di`、`grill-me`、
  以及 `analyze-app` / `app-map` / `check-logs` / `run-test`（后 4 个来自同一个 `panicgit/android-test-pilot` 仓库，是一条 adb 设备测试流水线）。
- 来源仓库：sunqing37/ai-rules、aj-geddes/useful-ai-prompts、peterbamuhigire/skills-web-dev、rcosteira79/android-skills、
  krutikJain/android-agent-skills、HoangNguyen0403/agent-skills-standard、codebygarv/Ai-skills、panicgit/android-test-pilot。
- **用户点名的 `a5c-ai/Room-Database` 与 `explainx-ai/android-retrofit` 在 GitHub 上是 404（仓库不存在）**，未安装；
  同名技能可从 `krutikJain/android-agent-skills`（`android-room-database`）与 `rcosteira79/android-skills`（`android-retrofit`）取。
- 安装方式：`npx skills add` 在本容器不可用（见全局记忆），改用 `git clone --depth 1` + 把含 `SKILL.md` 的整个目录拷进 `.aicode/skills/<name>/`。
- 已验证 `loadSkill("android-mad")` 能立即取到正文（无需重启，App 约 2 秒轮询技能目录）。
- 遗留：`npx github:codebygarv/Ai-skills add grill-me` 成功但装到了 `<项目>/.claude/skills/grill-me`（AiCode 不读该目录）——**已于 2026-09 清除**（连同变空的 `.claude/` 一起删了；`.aicode/skills/grill-me` 那份完好）。

## 已创建的子代理（项目级 `~/workspace/.aicode/agents/`，2026-09）
- `feature-builder` / `perf-ux-optimizer` / `qa-pilot` / `ui-ux-designer` 共 4 个，**已实测被 AiCode 识别**
  （用 `task(action=create, agent="__probe__")` 探测，返回的可用清单里包含这 4 个）。
- **AiCode 子代理目录是 `.aicode/agents/`（项目级）或 `~/.aicode/agents/`（全局），不是 Claude Code 的 `.claude/agents/`**；
  frontmatter 只认 `name/description/provider/model/reasoningEffort/tools/disallowedTools/inject`，**没有 `color`**；
  工具名是 `readFile/list/search/writeFile/editFile/Bash/loadSkill` 这套（不是 Read/Glob/Grep/Write/Edit）；
  `inject: [base, skills, memory, projectRules]` 才会给子代理注入技能清单（配 `loadSkill` 才用得上）。
- 4 个代理提示词里的约定：技能目录写 `.aicode/skills/`（不是 .claude/skills/）；
  构建命令 `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-arm64 ./gradlew assembleDebug`（长时间无输出则加 `--no-daemon`）；
  qa-pilot 只写测试、不改生产代码与构建文件（项目只有 junit，没有 mockk/turbine/coroutines-test，所以让它用手写 Fake）。
- 提示词引用的都是**实际已装**的技能；未装的 `android-room-database`/`android-retrofit`/`android-di-hilt` 已在提示词里显式声明“没有，别 load”。