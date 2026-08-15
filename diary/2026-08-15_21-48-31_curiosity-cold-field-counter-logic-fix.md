# Curiosity Boss 设计稿：反制逻辑修正（环境侧 vs 身体侧）

- Time: `$(date "+%Y-%m-%d %H:%M:%S %z")`
- Author: `DeepSeek v4 (deepseek-v4-pro); coding agent for TeamMoeg`
- Status: `completed`
- Scope: `docs/curiosity-boss-design.md` §1.1/§4.2

## Completed

- 用户纠正反制关系表述，设计稿 §4.2 重写为「环境侧 / 身体侧」两条路径。

## Decisions

- 玩家随身装备（热水/加热背包/衣物）**不生成世界热场**：只抬高体温（`HeatingDeviceContext`/
  `ITempAdjustFood`）或降低导热（`ClothData`）；只有能量塔/加热器（`HeatingLogic`）生成正值热区。
- 伤害链路：冷场 → `WorldTemperature.air`（环境）→ 体感温度 ≈ 体温 + 导热×(环境−体温)
  （`PlayerTemperatureData.updateWhenInsulated`）→ `HypothermiaEffect` 按体感阈值结算。
- 热区取最大值规则（`ChunkHeatData.getAdditionTemperatureAtBlock`）只关乎冷场与放置式热源的
  环境侧竞争；玩家装备与冷场不共享数值，在体感温度处汇合。

## Validation

- 核对 `PlayerTemperatureData.updateWhenInsulated` 公式、`HypothermiaEffect` 伤害结算、
  `HeatingLogic.addSphereTempAdjust` 热场注册点。

## Remaining

- 无。设计稿已定稿，待开始实现（§14 里程碑 M1–M5）。
