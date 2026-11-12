# M1-B 项目知识条件、多来源与逐项披露交接

- Time: `2026-11-12 13:29:45 +08:00`
- Author: `Codex / OpenAI；M1-B 实现者`
- Status: `completed`（本阶段实现及服务端自动验证；客户端画面、真实多人和独立进程重启未验）
- Scope: FH knowledge 条件、项目访问、历史、快照、绘图台/图谱、命令、示例和 Forge GameTest；TWR 无修改。

## Completed

- 按 [M1-A 决定](../plans/2026-11-12_13-02-00_research-m1-interface-decisions.md) 实现 `KnowledgeCondition`、`ProjectAccess`、`matchKnowledge`、`projectAccess` 和无起源参数的 `startResearch(project)`。
- access 缺席保持旧项目任一合法想法入口；显式 access 只检查作者的 start_all，不展开想法获取前置。外部授予/学习流程未加门槛。来件不满足发现或开始；开始后遗忘来源不撤销 run。
- 全目录 access 观察条件逐项加入独立单槽用途，保留原 evidence 双槽/跨字段/UUID 独立性语义；M2 概率和 M4 阶段未提前实现。
- `ResearchHistory.origins` 支持多观察/想法/成果；schema 3 兼容 v2 与无版本 idea，保留全部进度；已有 origins 优先，缺来源保留进度并诊断，原始输入副本不变。
- 服务端快照裁剪新历史 origins 与 sources；J/C 各占一个未知必要行，B 共享去重；组引用行 ID。行 ID 从全部条件的固定顺序生成，参考项披露不移动其他行 ID。仅历史项目 active=false，普通研究列表/图谱不显示已放弃且失去发现来源的项目。
- UI 显示必要知识、逐项未知/参考、组引用行号、来件跳转和五种研究状态；START_RESEARCH 仅项目 ID。纯成果命令可用，complete 优先复用 open run。图谱支持所有来源类型。
- 想法/成果条件直接构造 key 查询 isActive；观察才扫描观察条目。KnowledgeView 单次生成内复用每项目 access evaluation，无跨 tick 缓存。
- 更新实际受影响的 definitions/state-and-api/workbench/observations-and-notes 与示例 README；新增 example 命名空间两项目、两想法、三成果及测试 fixture。没有修改 design、总开发计划、coordination 或环境验收文档。

## Decisions

- 条件同谓词按 kind/definition/type/无序去重字段条件合并；局部名排序第一项文案优先，有差异输出作者诊断。缺覆盖时补默认组，避开作者已有 default ID；未知引用仍报告加载错误。
- 新 access 与 condition 可选字段使用仓库 strictOptionalField / ProjectRequirement.field，错误类型不会静默变成空门槛；加载器沿用原快照替换规则。
- 组是关联引用，普通前置不消费观察、不提高独立记录额度。观察动作与入档事件继续分离；未新增阶段事件总线。

## Validation

在 FH 根，进程 `$env:JAVA_HOME='C:/Program Files/Java/jdk-17.0.2'`，未改全局 Java 环境。

```powershell
.\gradlew.bat compileJava compileGameTestJava runGameTestServer '-PgameTestNamespaces=frostedresearch_m1,frostedresearch,frostedresearch_knowledge_login,frostedresearch_observation' '-PgameTestDirectory=build/m1-gametest-20261112' '-PthermalTestHeap=3G' '-PthermalTestProcessors=4' --offline --no-daemon --console=plain '-Dnet.minecraftforge.gradle.check.certs=false'
```

- [最终运行日志](../build/m1-validation-3.log)：BUILD SUCCESSFUL，5m33s，完整 compileJava / compileGameTestJava，**11 个 required GameTest 全通过**（新增 5 组 M1 + 原有 6 项）。测试世界及服务器日志在 build/m1-gametest-20261112；未排除测试、未引入 JUnit。
- 新测试覆盖：I/J/A/B/C 逐项披露与去重、来件/档案界限、作者省略 A、纯成果与观察入口、item 标签/block/entity/未知字段、隐藏用途与未来 registerObservationUse、双槽独立证据/跨地点/复制 UUID、重复 start、来源遗忘、纯成果完成与重复完成、历史裁剪、稳定行 ID、历史 active、严格字段解码、default 组冲突、v2/无版本/v3/来源优先/缺来源/全部进度往返。
- 重复 start 是同服务端顺序调用验证，不是双客户端并发实测；complete 测试走真实 service API，不冒充真实玩家命令输入。
- [最初编译](../build/m1-compile.log)成功；[第一次增量](../build/m1-validation.log)因来件跳转误及普通阅读页编译失败，已修正；[第二次运行](../build/m1-validation-2.log)编译通过，因测试模板重复命名空间在断言前退出，已改回现有 empty 写法。失败未隐去。
- 最终增量编译 `compileJava compileGameTestJava`（同一 Java17/offline/no-daemon 参数）成功，2m33s；该构建在主协调避免重复验证消息到达前已启动，之后未再启动构建，也未再改生产或测试代码。日志：[m1-final-compile.log](../build/m1-final-compile.log)。JSON 解析通过；git diff --check 在本阶段路径通过。GameTest 仍有 M0 已知客户端类/依赖/配方告警，11项通过不代表完整整合包无告警。
- 未启动客户端或关闭用户游戏；当前无覆盖新 M1 绘图台路线的现成自动客户端驱动，本阶段只完成客户端源集编译，未把 NBT 断言当作实际画面。
- Git 操作仅 status/diff/branch/rev-parse/ls-files 只读查询，无 add/commit/push/reset/checkout。交接时主协调已将 HEAD 更新至 abb51a127；分支 research-261108。TWR 仍只有既有 staged config/.gitignore 与未跟踪 .workbuddy/。

## Changed files

可供主协调选择暂存的完整本阶段列表（不包含其 coordination 改动或既有未跟踪文件）：

- `docs/knowledge/definitions.md`
- `docs/knowledge/example-datapack/README.md`
- `docs/knowledge/example-datapack/data/example/frostedresearch/knowledge/ideas/i.json`
- `docs/knowledge/example-datapack/data/example/frostedresearch/knowledge/ideas/j.json`
- `docs/knowledge/example-datapack/data/example/frostedresearch/knowledge/projects/followup.json`
- `docs/knowledge/example-datapack/data/example/frostedresearch/knowledge/projects/joint_study.json`
- `docs/knowledge/example-datapack/data/example/frostedresearch/knowledge/results/followup_finding.json`
- `docs/knowledge/example-datapack/data/example/frostedresearch/knowledge/results/joint_finding.json`
- `docs/knowledge/example-datapack/data/example/frostedresearch/knowledge/results/smelting_finding.json`
- `docs/knowledge/observations-and-notes.md`
- `docs/knowledge/state-and-api.md`
- `docs/knowledge/workbench.md`
- `src/gametest/README.md`
- `src/gametest/java/com/teammoeg/frostedresearch/knowledge/ProjectAccessGameTests.java`
- `src/gametest/resources/m1-fixtures/ideas/i.json`
- `src/gametest/resources/m1-fixtures/ideas/j.json`
- `src/gametest/resources/m1-fixtures/projects/followup.json`
- `src/gametest/resources/m1-fixtures/projects/joint_study.json`
- `src/gametest/resources/m1-fixtures/results/followup_finding.json`
- `src/gametest/resources/m1-fixtures/results/joint_finding.json`
- `src/gametest/resources/m1-fixtures/results/smelting_finding.json`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/KnowledgeCommands.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/KnowledgeService.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/KnowledgeView.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/client/KnowledgeGraphModel.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/client/KnowledgeJournalData.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/client/KnowledgeLayer.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/client/ui/KnowledgeNodeSheet.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/definition/KnowledgeCondition.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/definition/KnowledgeDefinitions.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/definition/ProjectAccess.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/definition/ProjectDefinition.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/network/KnowledgeWorkbenchActionPacket.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/state/KnowledgeState.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/state/ResearchHistory.java`
- `src/main/resources/assets/frostedresearch/lang/en_us.json`
- `src/main/resources/assets/frostedresearch/lang/zh_cn.json`
- `diary/2026-11-12_13-29-45_research-m1-access-origins-handoff.md`

## Remaining

- 主协调复核并统一暂存/提交；本实现者未提交。
- 新 UI 的真实画面/小窗口、键鼠操作、来件跳转与纯成果图谱需客户端验收；没有部署到 Prism 或正式包。
- 双客户端、换队/合并综合回归和独立服务器进程保存重启仍待验；本轮是内存状态 NBT 往返，未冒充重启验证。
- M2/M4 后续加载器须遍历其全部潜在观察用途；这里只测试模拟注册，不声称已有阶段加载器。TWR 正式项目与生存材料路线继续按后续里程碑处理。
