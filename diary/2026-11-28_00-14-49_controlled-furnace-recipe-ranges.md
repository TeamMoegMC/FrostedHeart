# 控温实验炉的区间配方

- Time: `2026-11-28 00:14:49 +08:00`
- Author: Codex（OpenAI，配方实现与验收）
- Status: completed
- Scope: 控温烧制配方、样品停炉结算、保存与配方同步。

## Completed

- 每条配方用一个温区、一个时长区间和一个产物定义结果。同一个样品可配置多条配方。没有匹配时保留原物品。耐火砖的三个温区各有一条配方。更新了[控温实验炉说明](../docs/knowledge/controlled-furnace.md)和[耐火砖说明](../docs/knowledge/refractory-brick.md)。

## Decisions

- 保留温区包含两端、时长超过下限且可等于上限的规则。区间重叠时取配方 ID 排序的第一条。
- 开炉保存符合设定温度的候选，停炉按时长结算。旧存档中已经开炉的样品继续使用开炉时保存的结果。联机两端使用同版模组。

## Validation

- 17 项 M5/M6 Forge GameTest 通过。覆盖同物品多配方、未匹配时保留物品、时长区间端点、存档恢复和配方同步。
- Research Playtest 已同步模组和三条正式配方。临时世界中确认客户端加载全部三条配方，以 1107°C、21 秒烧出普通耐火砖，并核对 JEI。验收记录存于 `build/furnace-ranges-client/`。

## Remaining

- 无。
