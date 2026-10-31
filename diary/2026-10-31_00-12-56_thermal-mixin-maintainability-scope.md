# 热模拟接入整理：明确减少实际侵入的目标

- Time: `2026-10-31 00:12:56 +08:00`
- Author: `Codex; OpenAI GPT-6; engineering plan refinement`
- Status: `completed plan refinement; implementation not started`
- Scope: `热模拟Mixin研究的执行口径与删除清单`

## Completed

- 根据用户澄清，更新[既有逐项研究](../plans/2026-10-30_23-51-15_thermal-mixin-integration-review.md)，以可读性、可维护性、入口合并和减少原版流程接管为主；不再把反射、ASM、AT或核心对象替换作为减少Mixin的推荐手段。
- BE判空建议改为公开API路径，明确packed NBT仍有复制代价；删除为工具方法增加的父类构造适配是目标，尚未实施。
- 以专用篝火通知、保存冷却字段依赖、完整破冰方法副本和重复业务规则的实际删除作为主要结果；移动目录与微观CPU优化不代替结构简化。

## Decisions

- 继续保留尚未闭合的随机循环替代、辐射触发器归并为候选，不以新增上下文状态或访问层凑删除数量。

## Validation

- 复核当前相关入口和既有研究；本轮没有改生产代码、编译或运行测试。
- Documentation impact: 更新现有计划。没有改变已实现行为，现行系统说明无需更新。

## Remaining

- 具体实施与验证尚未执行；各候选的准备程度见计划中的删除清单。
