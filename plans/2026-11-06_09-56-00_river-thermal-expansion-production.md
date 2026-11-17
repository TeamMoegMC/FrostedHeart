# 河道远距离热色的生产复现与修正

- Time: `2026-11-06 09:56:00 +08:00`
- Authors: `OpenAI Codex / GPT-6; primary implementation and production-validation agent`
- Status: `completed`
- Scope: 能量塔物理供热、水土初始相态、材料邻接准入及实际红外回包
- Related: [余温生命周期](../docs/climate/thermal-residual-lifecycle.md)、`ThermalCellArena.stageMaterialLaw`、`WorkerPageStore.collectMaterialFrontier`

## Goal

解释并处理用户报告的能量塔运行后，百米外河岸和水面仍呈橙色的问题。以真实 Forge 世界、燃料机器、材料 H/branch、空气和红外协议为证据；单独公式调用不作为空间传播实测。

## Verified Current State

- 自然背景 -30°C 下，当前生产材料律将初始水 H 解为 -5°C 冻结平台，普通泥土解为 -10°C；尚未证明这一点足以产生百米传播。
- 活跃 Brick 请求相邻接触材料；材料相变和相对背景温差维持活跃。实际生产测试需区分塔输入、自然冻结及纯几何纳入。
- 本地工作区已有其他热生命周期和红外修改；在其上增量修改并保留，独立生成测试世界和输出。

## Steps

1. 新增真实河道场景：160m 水/河岸、实际 `minecraft:frozen_river`、燃料能量塔、短时真实篝火和未点火对照，保留正常材料/源/worker/相变路径。
2. 记录 16/32/64/96/128m 的材料、空气、Page 及最终红外回包，先运行当前代码保存复现结果。
3. 依复现选择最小修正，不用任意功率削减、固定传播半径或直接清除潜热隐藏问题。
4. 相同场景复测，并验证正常局部供暖、既有相变与保存/余温相关合同。

## Documentation Impact

核对并更新现行材料/准入文档，保存可提交的生产验证摘要；完成后追加开发日记并更新本计划结论。

## Outcome

- 平岸首轮真实20TPS、3600tick通过：128m红外水/岸均INVALID，远处材料无记录；不能据初始化公式单独断言百米传播。
- 高岸（岸高于水面1格）真实Forge复现：128m标记附近泥土-10°C、水-5°C，空气仍接近-23.7°C自然值；短时篝火熄灭后仍接力扩张。首次长时间lockstep日志止于6700tick，无正常完成/关闭证据，不冒充完整通过。
- 选定最小修正是材料邻接扩张读取已提交的具体表面材料扰动，并减去同材料的自然初始化相变温度；潜热保留资格不再自动授权招募下一批自然材料。源功率、H、潜热、相变过程及空气扩张规则保持其现有含义。
- 同一高岸正常20TPS、3600tick旧代码按预期失败，修正后1/1通过；修正后12000tick行为1/1通过。9项材料/接触图与4项余温/红外回归通过。地面夹具先明确放置并验证可读相变泥土，原熄火后H下降要求保持。
- 当前工作区生产打包和3734个main class完整性、测试代码排除检查通过。现行行为见[余温生命周期](../docs/climate/thermal-residual-lifecycle.md)，数值/时钟/失败和范围见[验证JSON](../docs/climate/validation/thermal-river-expansion-2026-11-06.json)，完成记录见[日记](../diary/2026-11-06_16-19-12_river-natural-phase-expansion.md)。
