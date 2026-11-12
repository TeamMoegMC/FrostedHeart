# M0 基线与内容调查交接

- Time: `2026-11-12 12:40:00 +08:00`
- Author: `Codex / OpenAI；主代理，GPT-6 Luna 两项只读调查`
- Status: `partial`
- Scope: M0；两份既有矿脉测试编译修复、研究验证入口、内容卡与旧目录；未实施 M1–M7。

## Completed

- FH `research-261108` / `9d2dab38d02fb49d0c0f1f475836a1ffc682e3d9`；TWR `1.20` / `2d0fefd349f65f3118828c9712f68a8c0782ab75`，没有切换、重置、暂存、提交、推送或发布。
- [三项目内容卡与环境差异](../plans/2026-11-12_12-36-00_research-m0-content-baseline.md)；[81项全量旧目录](../plans/2026-11-12_12-34-00_research-m0-legacy-catalog.md)，保留完整 effects、上下游及未定处置。
- 修复完整 GameTest 编译入口：`DepositGameTests` 显式指定 `Holder<Biome>` 泛型；`DepositClientValidation` 补回按 block registry ID 统计的输出方法。未排除测试、未改矿脉生产逻辑。
- 只读核对 IE 成型权限与 Charcoal Pit 实际风箱调用，主代理复核 FH 方法和 mapped JAR 字节码，见内容卡。

## Decisions

- 用户本轮明确选择：**原型鼓风口独立接受风箱试吹**，无需提前解锁/制作冶铁炉。M4 不能以必须连接 Bloomery 的 `TileBloomery.blow` 作为原型成功的唯一判据。此前临时开放实验炉建议已被此决定替代。
- 尚未确认：三种小炉是否都必须试烧、具体配方/温度/时长、耐火砖无成品入口与成果配方范围。均未擅自落生产实现。
- 新数据字段、存档及权限语义：本阶段没有新增。新项目需求见计划，尚非当前行为。

## Changed files and protected work

本次：上述两个测试 Java；两份 M0 plans；本 diary；开发计划实际状态。TWR 无修改，根及父级适用位置/脚本子树未找到 AGENTS.md。

必须保留 FH 原有已修改 discussion 草稿、大量未跟踪 diary/docs/plans、`.workbuddy/`、run/config 的额外研究、town GameTest、sim 脚本等；TWR 已暂存 `config/.gitignore` 与未跟踪 `.workbuddy/`。不把这些纳入本阶段提交，不回滚任何既有内容。

## Validation

所有命令在 FH 根执行；`JAVA_HOME` 仅进程内设为 `C:/Program Files/Java/jdk-17.0.2`。未改全局 Java/Gradle 配置。Gradle wrapper 8.1.1，Java17.0.2，Forge47.3.0。

1. 原环境 `.\gradlew.bat --version` 显示32位 Java8；首轮构建失败于6G堆不可表示，日志 `build/m0-baseline-20261112.log`。
2. Java17 下 `compileJava compileGameTestJava validateResearchCatalog` 重现矿脉4处错误，日志 `build/m0-java17-20261112.log`；未复现历史原生内存不足。
3. 修复后命令：

```powershell
$env:JAVA_HOME='C:/Program Files/Java/jdk-17.0.2'
.\gradlew.bat compileJava compileGameTestJava validateResearchCatalog runGameTestServer '-PresearchCatalogDir=D:/games/minecraft/Project TWR/TheWinterRescue/config/fhresearches' '-PgameTestNamespaces=frostedresearch,frostedresearch_knowledge_login,frostedresearch_observation' '-PgameTestDirectory=run-m0-gametest' '-PthermalTestHeap=3G' '-PthermalTestProcessors=4' --offline --no-daemon --console=plain '-Dnet.minecraftforge.gradle.check.certs=false'
```

`build/m0-validation-20261112.log`：BUILD SUCCESSFUL，4m11s；正式81定义 preflight 通过；6个 required GameTest 通过。生产 compileJava 为 UP-TO-DATE；完整 compileGameTestJava 实际执行。测试运行目录已在进程退出后移入 `build/m0-gametest-20261112`，未来重跑建议直接指定 build 下的新目录。GameTest 加载专门 fixture 目录，不是完整正式包，且服务端未加载 Charcoal Pit；日志含既有配方/客户端类错误，测试通过不代表全包启动无告警。

4. 观察客户端：`.\gradlew.bat runObservationUiClient --offline --no-daemon --console=plain '-Dnet.minecraftforge.gradle.check.certs=false'`，日志 `build/m0-client-20261112.log`。BUILD SUCCESSFUL，4m39s；11条观察输入路由全部 passed，见 `build/observation-ui-validation/result.json` 与同目录本日截图。已查看 hand-key.png，完成记录窗口正常；此图不是观察过程遮罩图。测试客户端已自行退出，未关闭用户程序。其输入是模拟 Minecraft 回调，不是人工键鼠或完整生存游玩。
5. `git diff --check` 通过（既有 discussion 有换行警告）。正式 TWR 配方/材料路线为源码核对；没有发布、部署或改玩家原存档。

## Remaining / handoff

- M0 不标全部完成：新设备/砖坯材料、供热、耐火砖入口等尚未定稿，不能声称三项目已完整证明生存可达；正式包材料与工具实际获取待验。内容卡已列出可用现有路线和具体循环，不用指令代替。
- 手持遮罩需画面复核；客户端自动路由截图主要是完成记录窗口，并不直接证明整个观察过程遮罩正确。真实键鼠、多人、正式包三项目生存游玩均未验。
- 下一步建议 M1-A（Astra）：先读本交接、内容卡、两份总计划、知识 state-and-api/definitions/workbench 与实际 research 定义/执行源码，定共用条件、披露、多来源和兼容。不提前实现 M4 阶段引擎。
- 不必重复定位 Java8差异、4处矿脉编译错误、81项清单、IE权限链、Charcoal Pit供气是否需炉体；若依赖版本变化再复核。
- 文档影响：没有生产行为变化，未改 living docs 行为描述；实施准备与已完成验证分别记录于 plans 和本 diary。
