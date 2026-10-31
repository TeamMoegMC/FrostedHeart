# 公共源维护链复查与完成范围更正

- Time: `2026-10-31 18:13:54 +08:00`
- Author: `Codex; OpenAI GPT-6; source inspection`
- Status: `partial; architecture requirement remains unfinished`
- Scope: `篝火通知、公共Section源同步与验收表述`

## Completed

- 确认onCampfireIgnited的显式生产调用者是CampfireBlock自身onPlace；公共Chunk的onMaterialStateWritten没有篝火分支。
- 确认仍存在另一条公共源维护链：Section写入→mutationFlagsFromCodes比较sourceFlags→SOURCE_MUTATION→MinecraftPageManager.drainMutations→PhysicalSourceSpatialIndex.resyncBlock。sourceFlags当前专门来自campfireFlags，后端识别篝火并更新或移除源；完整Section重同步仍调用resyncSection。
- 对照当前源码修正运行合同、计划状态和验收范围，不再宣称完整篝火生命周期已退出全部公共写入路径。已有12项玩法结果保持原资格，不改写历史日记。

## Decisions

- 显式点火入口收窄与完整源维护链退出是不同的完成条件；不能只搜索Mixin里的Campfire分支就确认后者。
- 熄灭/移除当前仍依赖公共源维护，不能直接删除而破坏玩法。

## Validation

- 只读追踪Mixin、状态表生成、源变化分类、Page变更消费和物理源索引的实际调用者；本轮未修改生产或测试源码，未重新运行测试。
- Documentation impact: [运行合同](../docs/climate/thermal-runtime-control.md)明确残留路径；[原计划](../plans/2026-10-31_13-56-49_thermal-specific-hooks-and-snow.md)改回未完成；[原验收](../docs/climate/validation/thermal-specific-hooks-2026-10-31.json)补充架构限制。

## Remaining

- 公共Section源维护与篝火完整生命周期职责需要一并处理并验证；现阶段不能确认用户要求的公共路径退出已经完成。
- BlockStateBase随机更新资格、LiquidBlock岩浆通知以及真正共有的材料/几何/保存接入仍存在，不能笼统称为没有公共链路。
