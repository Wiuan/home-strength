# 练习生 (Trainee)

人生练习生 App：档案 / 今日清单 / 多轨道轻练习；**力量**轨道沿用原居家力量深度模块（组次数、弹力带、进阶）。

## 技术栈

- Kotlin + Jetpack Compose + Material 3
- MVVM + Navigation Compose
- Room（Local First，无网络权限）
- Coroutines / Flow
- 手工 DI（`AppContainer`）

## Phase 6（已完成）· MVP 1.0.0

- 字号/配色与深浅色主题统一；首页分区更清晰
- 动效：未完成训练展开、休息结束切换动画
- Snackbar 错误提示（开训失败、保存失败）
- 补单测：单侧左右记录、删除后 A/B 重算
- UI Test：`NumberStepperUiTest`（需设备/模拟器：`connectedDebugAndroidTest`）

## Phase 5（已完成）

- 休息计时器：完成一组后弹出，60/90/120 秒可切换，跳过；用 `elapsedRealtime` 保证切后台仍准
- 动作详情：计划/训练页点动作名 → 当前阻力、最佳、历史列表
- 设置可改：每周目标、默认组数、默认休息；新训练组数跟设置走
- 中途恢复：离开确认会保存；有未完成时开新训练二次确认，可直接继续

## Phase 4（已完成）

- 双重进阶引擎 `ProgressionEngine`：先加次数，满组后再加阻力
- 训练页显示建议：`20 LB · 13 / 12` / 建议增加阻力 / 可考虑降低阻力 / 第一次训练
- 满组后可一键「应用下一档 LB」；次数目标回到 `targetMin–targetMin+2`
- 单测覆盖：无历史、12/11 不加阻、15/15 加阻、低于 min 提示

## Phase 3（已完成）

- 设置 → 我的弹力带：添加 / 修改 / 删除（同阻力合并数量）
- 训练页选择阻力组合（保存 totalResistance + band 关联）
- 新训练自动带入该动作上次阻力
- 训练页显示「上次 20 LB · 12 / 11」或「第一次训练」
- 历史列表 + 详情；删除记录后重算 nextWorkoutType

## Phase 2（已完成）

- 选择今天状态（正常 / 有点累 / 极度疲惫）并创建训练会话
- 训练页：大按钮 +/- 记录次数或秒数；支持跳过动作；单侧分左右
- 中途离开自动保留未完成会话，首页可「继续未完成训练」
- 完成页：摘要 + 1–5 星感觉；确认后切换 `nextWorkoutType` A↔B

## Phase 1（已完成）

- 可编译工程骨架
- Room 实体 / DAO / Database / 种子数据（Workout A/B + 默认弹力带）
- 首页读取 `nextWorkoutType`、本周进度、上次训练占位
- 设置 / 计划查看（A/B 动作列表）
- 核心领域：`BandCombinationCalculator`、`WorkoutPlanner`
- 单元测试：弹力带组合、A/B 顺序、疲劳模式筛选

## 运行

1. 用 Android Studio 打开本目录
2. JDK 使用 Android Studio 自带 JBR（17+）
3. `local.properties` 已指向 `D:\Env\sdk`（按本机修改）
4. Run `app`

命令行（需 JDK 17）：

```bat
set JAVA_HOME=D:\software\Android\jbr
gradlew.bat :app:assembleDebug
gradlew.bat :app:testDebugUnitTest
```

## 后续

按需求文档 Phase 2 → 6 推进：训练记录、弹力带 UI、双重进阶、疲劳模式、中途恢复、测试补齐。
