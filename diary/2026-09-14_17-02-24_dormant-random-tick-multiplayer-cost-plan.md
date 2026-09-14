# 休眠随机更新：热路径与多玩家成本计划

- Time: `2026-09-14 17:02:24 +08:00`
- Author: `Codex; OpenAI GPT-6; primary engineering agent`
- Status: `completed`
- Scope: `工程方案细化；未修改运行代码`

## Completed

- 更新[休眠计划](../plans/2026-09-14_16-22-45_thermal-unified-dormant-natural-cooling.md)第5.6–5.7节和验收，主材料计划同步入口。
- 明确复用调用者Chunk/state与Section owner、一次标量定位、推迟自然/世界条件查询、使用已有MutableMaterialSample、成功改块才交接H。
- 补充多玩家按实际更新Section/候选命中数计费，以及红外按玩家执行的独立成本。

## Decisions

- 继续原随机抽样，不增加每玩家相变数据、Section候选队列、到期堆、默认节流或新配置。
- 无相变落地时不snapshot/写Editor，不触发COW；共享sample在世界修改前捕获必要标量，避免重入污染。
- 成本比较必须同时报告转换延迟；不通过暗中降低更新频率宣称无损优化。
- Documentation impact: 仅计划与本调查日记；现状docs和Java保持原状。

## Validation

- 核对DormantChunkThermalState的has/read/snapshot语义、Editor定位、现有Section attachment及MutableMaterialSample.sampleTick。
- 当前映射版本ServerChunkCache.tickChunks的区块级调用已在前一轮读取核实；本轮将结论写入计划，未进行负载实测。
- 文档格式、链接和改动检查；功能与性能回归留待实施。

## Remaining

- 按修订计划实现后执行集中/分散玩家与稀疏/密集候选四组测量；分别报告热更新、红外、分配/COW峰值和转换延迟。
