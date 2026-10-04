# ScoreTrace v4.6：首页数据真实化与 Compose 性能优化指令

> 本文档由 v4.5 任务中的只读探索（Explore 子代理 + 逐文件精读）产出，所有行号基于包名规范化（`com.fenji.scoretrace`）之后的代码。执行本指令前请先确认阶段一（包名修改）已合入。
>
> **重要事实先行**：首页四阶段优化（v4）已完成成绩概览/排名/AI重点/目标院校的真实数据接入，倒计时重组收口已完成（秒级 tick 只重组秒数 Text）。本指令不重复造轮子，只处理探索中实锤的残留问题。

---

## 零、执行边界与红线

- 禁止引入新依赖；禁止修改 `FloatingBottomBar.kt`（64dp 高度与内部约束）；禁止改 Room 版本号（**本指令全程不需要数据库迁移**）；禁止动 `signingConfigs` / `versionCode` / `versionName`；禁止动音乐播放逻辑。
- `gradle.properties` 中 `android.enableResourceOptimizations=false` 保持不变。
- 构建验证命令：`JAVA_HOME=/usr/lib/jvm/java-17-openjdk-arm64 ./gradlew assembleDebug`（严禁并发构建）。
- 每完成一个阶段做一次增量编译，失败最多修 2 次，第 3 次仍失败即停并上报。

---

## 一、倒计时重组作用域：现状已达标，无需改动（验证记录）

**结论：`derivedStateOf` 隔离方案在当前代码中已经落地，本指令不再重复实现，仅列出证据供回归核对。**

| 检查点 | 位置 | 现状 |
|---|---|---|
| 顶层只持 `State` 不解包 | `ui/screen/home/HomeScreen.kt:79`（`val countdown = viewModel.countdownState.collectAsStateWithLifecycle()`，无 `by`） | ✔ 函数体无 `countdown.value` 直读 |
| 卡片外壳零 state 读取 | `ui/component/CountdownCard.kt` `CountdownCard()` 函数体 | ✔ 全部下推子组件 |
| 年份订阅 | `CountdownCard.kt` `CountdownHeader` 内 `derivedStateOf { countdown.value.targetDateText.take(4)... }` | ✔ 全年不变 |
| 天数订阅 | `CountdownCard.kt` `DaysValue` 内 `derivedStateOf { countdown.value.days }` | ✔ 每天才变 |
| 时/分/秒各自订阅 | `CountdownCard.kt` `TimeUnitBox` 内 `remember(selector) { derivedStateOf { selector(countdown.value)...padStart } }` | ✔ 补零在 derived 内，显示值不变时天然去重 |
| 叶子组件可跳过 | `CountdownCard.kt` `CountdownUnit(label: String, value: String, highlight: Boolean)` | ✔ 全稳定基本类型 |
| 天数流去重 | `ui/screen/home/HomeViewModel.kt:159` `studyDayCount`（`map { it.days.toInt() }.distinctUntilChanged()`） | ✔ 隔离秒 tick 对顶栏的影响 |
| 音乐进度隔离 | `HomeScreen.kt` `musicPanel` lambda 仅在面板展开时组合 | ✔ 500ms 进度流不带动整页 |

**v4.6 唯一要做的重组相关动作**：见第五节 #7（HomeTopBar 无限动画在暂停时仍在跑）与 #8（QuickActions 列表每次重组重建），均为小项、可选。

---

## 二、硬编码数据替换为 ViewModel StateFlow：逐文件修改清单

### #1 问候语时段化（早上好 → 按时段问候）

- **现状**：`ui/screen/home/HomeScreen.kt:281` 写死 `subtitle = "早上好，备考第 $studyDay 天"`。
- **改法**（ViewModel 层产出、按分钟级更新足够）：
  1. `HomeViewModel.kt` 新增：
     ```kotlin
     val greeting: StateFlow<String> = ticker
         .map { now -> greetFor(now) }
         .distinctUntilChanged()
         .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), greetFor(System.currentTimeMillis()))

     private fun greetFor(nowMillis: Long): String {
         val hour = Instant.ofEpochMilli(nowMillis).atZone(ZoneId.systemDefault()).hour
         return when (hour) {
             in 5..10 -> "早上好"
             in 11..13 -> "中午好"
             in 14..17 -> "下午好"
             in 18..22 -> "晚上好"
             else -> "夜深了"
         }
     }
     ```
     注意：复用既有 `ticker`（每秒），但 `distinctUntilChanged` 后实际每小时才发射一次，代价可忽略；**不要**为问候语单独再起一条秒级流。
  2. `HomeScreen.kt` 顶层 collect（`collectAsStateWithLifecycle`），`:281` 改为 `subtitle = "$greeting，备考第 $studyDay 天"`。
- **验收**：跨 11:00/14:00 等时段边界时副标题切换；其余时间无重组扩散（greeting 是 String，天级以下稳定）。

### #2 「985」标签真实化（接 schools.json 的 tags，无需迁移数据库）

- **现状**：`ui/component/TargetSchoolCardNew.kt:145` 写死 `Text("985")`，TODO 注释自认（`:137`）。
- **关键事实**：`TargetSchool` 实体（`data/local/entity/TargetSchool.kt`）没有 tag 字段，但 `SchoolInfo.tags: List<String>` 已存在（`data/model/SchoolInfo.kt:16`），且 `HomeViewModel` 已在 uiState combine 里调用 `schoolRepository.findByName(it.schoolName)` 取 logoUrl（`HomeViewModel.kt:108`）——**同一次调用就能拿全 logoUrl + tags，零新增查询、零迁移**。
- **改法**：
  1. `HomeViewModel.kt` 的 `HomeUiState` 增加字段：
     ```kotlin
     /** 目标院校层次标签（985/211/双一流…）；未匹配到院校为空列表，UI 隐藏标签 */
     val targetSchoolTags: List<String> = emptyList(),
     ```
  2. `HomeViewModel.kt:100-104` 处（现有 `targetSchoolLogoUrl = targetSchool?.let { schoolRepository.findByName(it.schoolName)?.logoUrl }` 在 `:102`）改为一次取整个 `SchoolInfo`：
     ```kotlin
     val schoolInfo = targetSchool?.let { schoolRepository.findByName(it.schoolName) }
     // ...
     targetSchool = targetSchool,
     targetSchoolLogoUrl = schoolInfo?.logoUrl,
     targetSchoolTags = schoolInfo?.tags.orEmpty(),
     ```
  3. `TargetSchoolCardNew()` 增加 `tags: List<String>` 入参；`HomeScreen.kt` 调用处传 `state.targetSchoolTags`。
  4. `TargetSchoolCardNew.kt:137-152` 的硬编码 Box 改为：
     ```kotlin
     tags.firstOrNull()?.let { tag ->
         Box(...) { Text(text = tag, ...) }   // 样式沿用现有 985 小胶囊
     }
     ```
     显示优先级建议 `tags.firstOrNull { it in listOf("C9", "985", "211", "双一流") } ?: tags.firstOrNull()`（SchoolRepository 的 tags 数据本就按优先级排序，直接取 first 也行，执行时二选一并保持简单）。
- **验收**：选「清华大学」显示 C9（或 985）；选普通本科显示「双一流」或无标签隐藏；空列表不渲染空胶囊。

### #3 六科得分率改为选科驱动（语数英 + 用户选科）

- **现状**：`HomeViewModel.kt:321` `SIX_SUBJECTS = listOf("语文","数学","英语","物理","化学","生物")` 写死（用于 `:174`/`:193`），无视 `UserPreferences.selectedSubjects`（`data/local/UserPreferences.kt:59-69`，默认「物理,化学,生物」，目标院校页已在用）。
- **改法**：
  1. `HomeViewModel.subjectRates` 的 `combine` 从 2 路改 3 路：加入 `userPreferences.selectedSubjects`。
  2. 科目集合改为 `listOf("语文", "数学", "英语") + selectedSubjects`（保持语数英在前、选科按用户顺序在后的展示顺序）。
  3. `SIX_SUBJECTS` 常量删除；`colorForSubject` 的 `else` 分支保留（政治/地理/生物/历史未配色时回退 SuccessGreen，可顺手补历史=橙、政治=红、地理=青，用现有 `ScoreTraceColors` 常量，不新增颜色）。
  4. `initialValue` 同步改为按默认选科生成，避免首帧闪现旧六科。
- **注意**：`ScoreOverviewCard` 的雷达/条形布局按列表长度自适应，无需改动（`ui/component/ScoreOverviewCard.kt` 遍历 `subjects`）。若卡片右侧 `size(130.dp)` 雷达在 9 科时拥挤，可缩至 120dp——执行时以真机观感为准，不强制。
- **验收**：改选科（目标院校页有选科入口）后回首页，六科条目随选科变化。

### #4 死入口清理（错题本 / 学习计时器）

- **现状**：`HomeScreen.kt:357-375` 三个 QuickAction：
  - `:365`「学习计时器」`onClick = {}` 空——功能未实现；
  - `:370`「错题本」`onClick = {}` 空——**该功能四阶段优化已整体删除**，属于死入口，必须移除；
  - AI 录成绩真实可用。
- **改法**：
  1. 删除「错题本」QuickAction 及其 `ic_book` 引用（若 `ic_book.xml` 无其他引用点则一并删除 drawable，删前全局搜引用）。
  2. 「学习计时器」两种处置，执行时二选一：
     - **A（推荐，保守）**：保留入口但移入 v4.7 待办，onClick 暂时接 `AppToast`（`util/AppToast.kt`）提示「功能开发中」——用户可感知而非静默无反应；
     - B：直接删除，快捷功能区剩两项（AI 录成绩 + 学习计时器走 A 则三项）。
  3. 顺手修正 `ui/component/QuickActions.kt:36-37` 的过时注释（写着「四个入口…学习计划（紫）」，实际既没有学习计划也删了错题本）——注释改为与实际列表一致。
- **验收**：快捷区无静默无反应按钮；点击学习计时器有 Toast 反馈（方案 A）。

### #5 「备考第 N 天」语义修正

- **现状**：`HomeViewModel.studyDayCount`（`:159-163`）= 倒计时 `days`（`countdownState.map { it.days.toInt() }`）——这是「距高考还剩 N 天」，被当成「备考第 N 天」显示（`HomeScreen.kt:281`）。两个数互为倒数关系，语义错。
- **改法**：`studyDayCount` 改为与 `yearPassedPercent` 同口径：
  ```kotlin
  val studyDayCount: StateFlow<Int> = userPreferences.gaokaoTimestamp
      .map { target ->
          val examDate = Instant.ofEpochMilli(target).atZone(ZoneId.systemDefault()).toLocalDate()
          ChronoUnit.DAYS.between(examDate.minusYears(1), LocalDate.now()).toInt().coerceAtLeast(0)
      }
      .distinctUntilChanged()
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
  ```
  （备考开始日 = 高考日 − 1 年，与 `computeYearPassedPercent` 一致；天级变化，无重组风险。）
- **验收**：若高考日为 2027-06-07，今天 2026-10-04 应显示「备考第 120 天」左右（而非倒计时 246 天）。

---

## 三、ScoreRecordRepository 数据对接方案（本指令的主数据链）

**现状结论：首页与 ScoreRecordRepository 的对接已完成，无需新对接。** 供回归核对的链路：

```
ScoreRecordDao.observeRecent(200) ──> ScoreRecordRepository.observeRecent ──┐
ExamRecordDao.observeAll ──> ExamRecordRepository.observeAll ──────────────┼─> summarizeScores()
UserPreferences.autoPlayMusic ─────────────────────────────────────────────┤      （HomeViewModel）
TargetSchoolRepository.observeLatest() ────────────────────────────────────┘          │
                                                                                       v
   latestTotalScore（按 examName 聚合求和）/ latestRankText（ExamRecord.classRank）
   / latestGradeRankText / scoreDelta（最近两次总分差） ──> HomeUiState ──> ScoreOverviewCard
```

`ScoreOverviewCard`（`ui/component/ScoreOverviewCard.kt`）已全部消费可空真实字段，空态「暂无成绩」已就绪。

**唯一新增的数据动作是第二节 #2 的 tags**（走 `SchoolRepository.findByName`，已有调用点扩字段），**不涉及 ScoreRecordRepository 改动**。

延伸（**非本指令范围**，列入 v4.7 候选，执行前先向用户确认边界）：
- `ui/screen/score/ScoreViewModelNew.kt:139-158`：成绩页趋势图三组 `TrendPoint` 硬编码（5月508 … 10月562 / 9月460 …），TODO 自认应从考试聚合取——对接方案：复用 `HomeViewModel.summarizeScores` 的分组逻辑，按 `byExam` 输出 `TrendPoint(examDate月份, total)`，`TrendRange` 取最近 6/10/全部；
- `ui/screen/mine/MineViewModel.kt:44`：「我的」页选科 `listOf("物理","化学","生物")` 写死，应接 `userPreferences.selectedSubjects`。

---

## 四、`derivedStateOf` 重组隔离方案

**首页无需新增任何 `derivedStateOf`**（证据见第一节）。需要提防的是**误用**（官方最佳实践）：

1. 两个 state 简单拼接（如 `"$greeting，备考第 $studyDay 天"`）**不要**包 `derivedStateOf`——输入输出变化频率相同，包了反而加开销。本指令 #1 的拼串留在组合期即可。
2. `derivedStateOf` 只用于「输入变化频率 > UI 需要的变化频率」的场景（如每秒 tick → 只关心天/时/分/秒每一位）——这正是 CountdownCard 现有写法。
3. 每处 `derivedStateOf` 都必须 `remember { }` 包裹（否则每次重组新建实例，读取失效）——现有代码已满足，回归时核对即可。
4. 全项目已无裸 `collectAsState()`（本轮全局搜索零命中），新代码一律 `collectAsStateWithLifecycle`（需 `androidx.lifecycle:lifecycle-runtime-compose:2.11.0`，项目已引入）。

---

## 五、其他性能项（小、可选、不阻塞验收）

1. **HomeTopBar 音柱动画常驻**：`HomeTopBar.kt` `AnimatedMusicBars` 用 `rememberInfiniteTransition`，`isPlaying=false` 时动画仍在跑（仅显示值锁在 5dp）。改法：动画 keyframes 保持不动，把三根柱子的 `height` 读取换成 `if (isPlaying) animatedValue else 5f` 只省测量不省动画帧；彻底方案是把 infinite transition 包进 `if (isPlaying)` 重组分支——执行时用后者（暂停时整个 transition 离开组合，动画停跑）。**注意动画对象重建的首帧跳变，真机确认观感再合入**。
2. **QuickActions 列表重建**：`HomeScreen.kt:357-375` 每次重组新建 3 个 `QuickAction` + 3 次 `painterResource`。改法：`val actions = remember(onOpenAiScore) { listOf(...) }`（`painterResource` 有缓存，实耗可忽略，属洁癖项）。
3. **`SubjectScore` 携带 `Color` 于 ViewModel 层**：层级异味（`colorForSubject` 在 `HomeViewModel` companion）。改法（可选）：把配色移到 `ScoreOverviewCard` 内按 `name` 查表；ViewModel 只吐 `name/rate`。改动面小但触碰卡片签名，**列为可选，默认不做**。
4. **`MusicPlayerViewModel` 进度轮询协程常驻**（VM 层 500ms 轮询 position/duration，面板关闭时无人订阅仍轮询）：属 VM 层耗电点，改法是把轮询改为「有订阅者才启动」（`WhileSubscribed` 化）。**本指令不动音乐模块（红线），列入 v4.7 候选。**

---

## 六、执行顺序与验收清单

建议顺序（每步增量编译）：
1. #2 tags（一次 SchoolInfo 取双字段） → 2. #1 问候语 → 3. #5 studyDay 语义 → 4. #3 六科选科驱动 → 5. #4 死入口清理 → 6. 五节可选项（真机确认后决定）

静态验收：
- [ ] `assembleDebug` BUILD SUCCESSFUL，零新告警（`grep -c "warning" 构建输出`对比改动前基线）
- [ ] 全局无新增 `collectAsState()`（裸）
- [ ] 无新依赖、无 Room 版本变化、`FloatingBottomBar.kt` 零 diff

真机验收：
- [ ] 问候语随时段变化；「备考第 N 天」与倒计时天数不再是同一数字的两种说法
- [ ] 清华显示 C9/985、普通院校无空标签胶囊
- [ ] 改选科后首页六科跟随
- [ ] 快捷区无静默按钮
- [ ] 首页 UI 与 v4.5 完全一致（除上述数据项）；底栏玻璃效果回归
