# M2 手动联络回环兼容更正与最终交接清单

- Time: `2026-11-12 14:18:30 +08:00`
- Author: `Codex / OpenAI；M2 实现者`
- Status: `completed`
- Scope: 提交前发现的手动联络已知输出兼容回归；其余 M2 实现见 [先前交接](2026-11-12_14-07-13_research-m2-behavior-contact-handoff.md)。

## Completed

- 复核基线 23b08d3c1 的 KnowledgeService.link：先按 rule ID 记录首次 LinkDiscovery，再处理知识产出；已知输出并不妨碍另一规则形成历史。M2 将手动候选的输出已持有检查与自动行为去重混用，造成示例 playthrough 回环中断。本记录更正先前交接中“已在来件/档案的输出不重复列为候选”的描述；先前17项通过不能证明当时没有这项遗漏。
- linkAllowed 只保留手动联络的必要知识/定义检查，恢复已知或来件输出的有效规则候选。link 保留新规则路径，但已有档案/来件时分别返回 ALREADY_OWNED / ALREADY_RECEIVED，跳过 produce，不新增知识实体、获得经历或学习尝试，也不把来件自动入档。
- 同规则仍由 KnowledgeState.discover 的 putIfAbsent 保留首次绑定，重复不新增历史或 mutation revision；其他规则可各保存自己的实际路径。自动 IdeaAcquisitionService 对档案/来件已有想法的跳过逻辑完全保留。
- 中文/英文的联络完成提示改为中性“关系已记录”，不把手动整理已知路径说成产生新想法。同步更正 state-and-api / workbench，并在 playthrough 回环段说明正常等待、来件与历史去重语义。
- 新服务 GameTest 使用原示例 materials_revisited 及 metallurgy_finding / climbing_finding JSON（3份刻意 fixture），验证预览、候选查询、正常200tick等待、另一规则历史、重复同规则、既有实体/获得经历/学习事件计数，以及来件保持未学习。

## Validation

```powershell
$env:JAVA_HOME='C:/Program Files/Java/jdk-17.0.2'
.\gradlew.bat compileJava compileGameTestJava runGameTestServer '-PgameTestNamespaces=frostedresearch_m2' '-PgameTestDirectory=build/m2-manual-loop-20261112' '-PthermalTestHeap=3G' '-PthermalTestProcessors=4' --offline --no-daemon --console=plain '-Dnet.minecraftforge.gradle.check.certs=false'
```

- [m2-manual-loop-validation.log](../build/m2-manual-loop-validation.log)：BUILD SUCCESSFUL，6m5s，**7 个 required M2 GameTest 全通过**（原6项＋新增手动回环1项）。完整 compileJava / compileGameTestJava 均包含；服务器正常保存退出，构建后未再改生产或测试源码，未重复构建。
- 三份 legacy-loop fixture 与原示例 JSON 相等，git diff --check 通过。新增回环是 Forge 环境中的真实 KnowledgeService / KnowledgeLinkSessions API 测试，不冒充客户端输入/图谱视觉验证。
- 上轮全量17项通过的证据仍是 [m2-validation-3.log](../build/m2-validation-3.log)，本轮按委派仅重跑相关 M2 定向7项，不宣称修正后又重跑了全部18项。
- 仅修本回归与准确文档，无 Git 暂存/提交/推送，无总计划/coordination/design/TWR 修改。待验范围沿用上一交接：真实客户端/键鼠、双客户端、独立进程重启等综合验收。

## Updated complete M2 file list

以下为供主协调选择暂存的完整本阶段清单，含上一交接与本更正；不含其 coordination 或其他既有未跟踪内容：

- `diary/2026-11-12_14-07-13_research-m2-behavior-contact-handoff.md`
- `diary/2026-11-12_14-18-30_research-m2-manual-loop-compatibility.md`
- `docs/knowledge/definitions.md`
- `docs/knowledge/example-datapack/README.md`
- `docs/knowledge/example-datapack/data/example/frostedresearch/knowledge/ideas/field_idea.json`
- `docs/knowledge/example-datapack/data/example/frostedresearch/knowledge/ideas/learned_hint.json`
- `docs/knowledge/example-datapack/data/example/frostedresearch/knowledge/links/field_legacy.json`
- `docs/knowledge/example-datapack/data/example/frostedresearch/knowledge/links/field_one.json`
- `docs/knowledge/example-datapack/data/example/frostedresearch/knowledge/links/field_zero.json`
- `docs/knowledge/example-datapack/data/example/frostedresearch/knowledge/projects/behavior_study.json`
- `docs/knowledge/observations-and-notes.md`
- `docs/knowledge/playthrough.md`
- `docs/knowledge/state-and-api.md`
- `docs/knowledge/workbench.md`
- `src/gametest/README.md`
- `src/gametest/java/com/teammoeg/frostedresearch/knowledge/IdeaAcquisitionGameTests.java`
- `src/gametest/java/com/teammoeg/frostedresearch/knowledge/ObservationGameTests.java`
- `src/gametest/resources/m2-fixtures/ideas/field_idea.json`
- `src/gametest/resources/m2-fixtures/ideas/learned_hint.json`
- `src/gametest/resources/m2-fixtures/legacy-loop/links/materials_revisited.json`
- `src/gametest/resources/m2-fixtures/legacy-loop/results/climbing_finding.json`
- `src/gametest/resources/m2-fixtures/legacy-loop/results/metallurgy_finding.json`
- `src/gametest/resources/m2-fixtures/links/field_legacy.json`
- `src/gametest/resources/m2-fixtures/links/field_one.json`
- `src/gametest/resources/m2-fixtures/links/field_zero.json`
- `src/gametest/resources/m2-fixtures/projects/behavior_study.json`
- `src/main/java/com/teammoeg/frostedresearch/FRConfig.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/IdeaAcquisitionService.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/KnowledgeBehaviorBridge.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/KnowledgeLinkSessions.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/KnowledgeRuntime.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/KnowledgeService.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/client/KnowledgeLayer.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/definition/IdeaAcquisition.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/definition/IdeaDefinition.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/definition/KnowledgeDefinitions.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/event/KnowledgeBehaviorEvent.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/item/ItemRetrieval.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/link/LinkMatcher.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/link/LinkRule.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/network/KnowledgeWorkbenchActionPacket.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/observation/ObservationSessions.java`
- `src/main/java/com/teammoeg/frostedresearch/mixin/minecraft/research/KnowledgeItemUseAcquisitionMixin.java`
- `src/main/java/com/teammoeg/frostedresearch/mixin/minecraft/research/KnowledgeMenuAcquisitionMixin.java`
- `src/main/java/com/teammoeg/frostedresearch/mixin/minecraft/research/KnowledgeUseAcquisitionMixin.java`
- `src/main/resources/assets/frostedresearch/lang/en_us.json`
- `src/main/resources/assets/frostedresearch/lang/zh_cn.json`
- `src/main/resources/frostedresearch.mixins.json`

## Remaining

主协调复核并统一提交。除前述客户端等综合验收外，本回归没有未完成的实现或服务端验证。
