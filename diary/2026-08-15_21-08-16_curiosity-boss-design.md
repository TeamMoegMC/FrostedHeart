# Curiosity Boss 设计文档（雪原深处的好奇心）

- Time: `2026-08-15 21:08 +08:00`
- Author: `DeepSeek v4 (deepseek-v4-pro); coding agent for TeamMoeg`
- Status: `completed`（设计稿；实现未开始）
- Scope: `docs/curiosity-boss-design.md`；`content/world/entities`；`content/climate/gamedata/chunkheat`

## Completed

- 调研了温度系统与既有原型，产出 Boss 完整设计文档 `docs/curiosity-boss-design.md`。
- 关键事实确认：
  - 命名 ID 为 `frostedheart:curiosity_entity`（`FHEntityTypes.CURIOSITY`），生成蛋 `curiosity_spawn_egg`；
    属性/放置规则注册点在 `FHCommonEventsMod`，渲染注册在 `FHClientEventsMod`。
  - `ChunkHeatData.addTempAdjust` + 负值 `SphereHeatArea` 即可实现「场地冷场」，自动流入
    `WorldTemperature.air/block` → `SurroundingTemperatureSimulator`；清理用 `removeTempAdjust`。
  - 重叠热区**取最大值**（`getAdditionTemperatureAtBlock` 中 `tmp > ret`）：正值加热设备天然
    反制负值冷场，无需新温度管线——这是「热水/加热背包反制」的设计依据。
  - 加热器先例：`HeatingLogic.addSphereTempAdjust`。
  - 玩家反制手段：热水（`ITempAdjustFood`/`CupTempAdjustProxy`）、加热设备（`HeatingDeviceContext`）、
    衣物（`ClothData`）。
  - 自然生成入口：`FHBiomeModifiers.Instance.modify()` 的 `spawns.addSpawn(...)`（现有
    `wandering_refugee` 先例）。

## Decisions

- 保留注册 ID `curiosity_entity`，单实体多阶段状态机（DORMANT→RISING→HUNT→MAZE→EXPOSED→DISPERSED/BURROW），
  地表隆起用短寿命 `CuriosityMoundEntity` 表现。
- 冷场阶梯：tier1 -15 / tier2 -30 / 每轮 -15 / 上限 -75，全部进 `FHConfig.SERVER.CURIOSITY`。
- 方块操作限空气/雪层，快照 + 指纹校验还原；细雪斑块滚动窗口上限 40。
- 迷宫确定性生成（种子 = world seed + arenaCenter + round），不新增布局同步包。

## Validation

- 阅读并核对源码：`WorldTemperature`、`ChunkHeatData`、`IHeatArea`、`FHBiomeModifiers`、
  `FHEntityTypes`、`FHCommonEventsMod`、`FHClientEventsMod`、`SurroundingTemperatureSimulator`、
  `HeatingDeviceContext`。

## Remaining

- 等待用户确认文档 §16 的 6 个待确认问题（奖励、音乐、生成频率、找核心引导、死亡重置、点火方式）。
- 确认后按文档 §14 里程碑 M1–M5 实现。
