# 修复残缺 JAR 启动失败与遗留 JUnit 编译入口

- Time: `2026-11-05 15:24:50 +08:00`
- Author: `OpenAI Codex / GPT-6; primary engineering agent`
- Status: `completed`（本次启动缺类及遗留 JUnit 入口修复；GameTest 编译限制见下）
- Scope: 两份遗留 JUnit 源码、温泉验证入口文档、当前工作区编译产物与用户日志对应的本地游戏实例

## Completed

- 首份构建日志的失败点是 `compileTestJava` 引用不存在的 `org.junit.jupiter.api`。按现有项目约定，删除 `PlayerThermalModelHotSpringTest.java` 与 `IceCaveSpaceTest.java`，并从 [hot-spring.md](../docs/climate/hot-spring.md) 移除旧验证入口。现有 Forge GameTest 源码保留。
- 用户随后明确最新日志为 15:14:54 启动记录，故停止处理先前误传的 2026-10-24 世界生成排序崩溃。当前致命错误为 `chorda.mixins.json` 无法加载 `com.teammoeg.chorda.mixin.CapabilityManagerAccess`。
- 检查实际安装 JAR：8,684,529 字节、942 个类，缺少 `CapabilityManagerAccess`、`SaunaBlock` 和 `FHMain`，三个对应源码仍存在。资源和 Mixin 声明存在而类缺失，确认是残缺编译输出进入打包。
- 用 Java 17 在当前工作区强制重新执行 `compileJava`，随后完成生产 `build` 任务与重新混淆。新 JAR 为 14,913,483 字节、3,728 个类；所有当前编译输出类、135 个配置声明的 Mixin，以及 FH、FR、Chorda 三个模组入口均存在。
- 将已核对的新 JAR 替换到 `D:/MC/.minecraft/versions/TWR ZDGX/mods/frostedheart-1.20.1-0.7.7.jar`，逐字节比较与构建产物一致。异常旧包备份于本仓库 ignored `build/launch-repair-20261105/incomplete-installed-frostedheart-1.20.1-0.7.7.jar`。

## Decisions

- 首次验证曾报告无法清理 `build/classes/java/main`，并发现其他进程仍写入新文件；后续构建对残缺目录报告 `compileJava UP-TO-DATE`。这些记录支持构建输出并发读写导致不一致，不能确定最早的具体进程或调用。
- 修复现有工作区产物，不复制历史源码、不改 Mixin 声明、不添加 JUnit 依赖、不修改世界生成或配套仓库数据。
- Documentation impact: 修正温泉文档中的失效测试入口。生产玩法、配置和持久化合同未变化，无需修改其他系统说明。

## Validation

- Java 17：`gradlew.bat compileJava --rerun-tasks --offline --no-daemon --console=plain`，`BUILD SUCCESSFUL in 44s`。日志：`build/build-classes-repair-20261105-compile.log`。
- 后续 `gradlew.bat build compileGameTestJava --offline --no-daemon --console=plain` 中生产 `build` 任务完成，`compileTestJava` 与 `test` 为 `NO-SOURCE`。许可证与重复资源提示沿用原有警告策略。
- 上述组合调用整体失败于额外的 `compileGameTestJava`，剩余 4 处错误：`DepositClientValidation` 两处调用缺失的 `stateCounts(int[])`，`DepositGameTests` 两处 `ParameterList<Reference<Biome>>` / `ParameterList<Holder<Biome>>` 泛型不匹配。它们位于本轮未修改的矿床验证源码，不影响普通生产 JAR 打包；未将这次调用报告为整体构建通过。日志：`build/build-classes-repair-20261105-package.log`。
- JAR 与全部主源码编译输出逐路径核对：3,728 / 3,728，无缺失。记录：`build/launch-repair-20261105/jar-class-check.json`；本地实例替换结果：`build/launch-repair-20261105/installed-result.json`。
- 本轮变更的 `git diff --check` 通过；当前 `src/` 无 JUnit 引用。没有启动游戏或运行 GameTest。

## Remaining

- 用户重新启动该实例确认完整游戏加载；本轮内容核对只证明所报缺类问题已修复，不代表所有运行时行为已验证。
- 矿床 GameTest 的 4 处既有编译错误仍需单独修复。
