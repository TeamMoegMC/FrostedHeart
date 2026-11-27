# 控温实验炉的时长上限与 JEI 展示

- Time: `2026-11-27 23:57:36 +08:00`
- Author: Codex（OpenAI，炉子实现与验收）
- Status: completed
- Scope: 控温烧制配方、正在烧制的样品、JEI 注册和客户端验收。

## Completed

- `controlled_firing` 增加可选的 `duration_max` 和 `overfire_result`。移除 JEI 中的烧制分类、配方和炉体用途入口。更新了[控温实验炉说明](../docs/knowledge/controlled-furnace.md)与[研究配方访问说明](../docs/research/results-and-access.md)。

## Decisions

- 样品在停炉时判断是否超时。达到上限仍按温度结算，超出时优先使用超时产物。设有上限且省略超时产物时使用 `failure`。省略上限且超过下限时，按温度结算。
- 炉内运行记录保存上限和产物。旧运行记录按无上限读取。配方同步格式增加了字段，联机两端使用同版模组。

## Validation

- 17 项 M5/M6 Forge GameTest 通过。新增验收覆盖时长边界、手动和定时停炉、运行记录恢复及配方同步。
- 生产包部署至 Research Playtest。在临时世界核对 JEI 不再注册控温烧制配方，并以 1107°C、21 秒原格烧出耐火砖。临时世界与验收包已归档至 `build/furnace-duration-client/`。

## Remaining

- 无。
