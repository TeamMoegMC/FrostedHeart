# 材料修复方案与早期架构的成本对照

- Time: `2026-09-13 00:28:00 +08:00`
- Author: `Codex; OpenAI GPT-6; primary engineering agent`
- Status: `completed`
- Scope: `解释架构演变和128节点成本，并补充现有plan；未修改代码`

## Completed

- 只读核对93f40924f微几何版与f3f6b0e8c整块版，确认早期Air/材料本来分离；新计划恢复状态分工而不恢复微格几何。
- 明确V100 Air合并与M存在独立，避免所有通风材料都保留独立Air导致不必要128槽；V75仍保持阻力节点。
- 在[计划12.1](../plans/2026-09-12_23-53-20_thermal-material-enthalpy-lifecycle-repair.md#121-与最初架构的区别及数量级预算2026-09-13补充)记录Air、薄墙、V75、V100、满实心和相变墙的节点对照，以及内部边和当前槽数组载荷参考。

## Decisions

- 不把节点翻倍说成整机性能减半，也不把新方案增加内部材料/逐块phase的成本隐藏在“仅部分方块拆分”中。
- 当前约124 B/槽容量只作量级参考；新PhaseStore/law布局、边、记录与容量增长仍需实际测量。
- Documentation impact: 更新现有实施plan与本日记。living docs描述当前实现，无行为变化无需改动。

## Validation

- 静态计数当前arena字段、PhaseStore数组和QueryPublication双缓冲，核对全V75内部144个邻接面。
- 没有运行无意义的旧版本重编对比；新方案尚未实施，未给出TPS/FPS固定损失比例。

## Remaining

- 实施后按计划目标场景测worker/主线程/heap/网络/GPU成本。
