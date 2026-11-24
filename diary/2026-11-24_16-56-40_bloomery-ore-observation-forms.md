# 锻铁炉研究接受不同形态的铁矿料

- Time: `2026-11-24 16:56:40 +08:00`
- Author: `Codex / OpenAI，实现与验证`
- Status: `completed`
- Scope: TWR 锻铁炉研究前置、绘图台联络和中英文提示，以及 FrostedHeart 服务端验证。

## Completed

- 研究开启与构想联络共用 `the_winter_rescue:research/iron_ore_materials` 物品标签。它接受铁矿和黄铁矿的原矿、粗矿、粉碎矿、凝结矿等形态，以及粗铁块、铁粉、铁矿浆和铁矿泥。
- 同步中英文研究提示、任务书和[玩家指南](../docs/knowledge/player-guide.md)，并更新[冶铁炉正式路线](../docs/knowledge/iron-bloomery.md)。

## Decisions

- 保留联络槽位名 `raw_iron`，让已有联络记录继续对应当前联络规则。铁锭仍走铁制品触发构想的路线。

## Validation

- `frostedresearch_m4_cp` 与 `frostedresearch_m2` 共 20 项 Forge 服务端测试全部通过。19 种铁矿料分别和三种燃料组成 57 个组合，均可联络和开启研究。测试同时验证铁锭无法满足铁矿料前置。
- 正式数据、测试夹具与中英文键核对通过。旧研究目录 80 项定义校验通过。两个仓库的相关改动通过 `git diff --check`。

## Remaining

- 客户端未实玩。
