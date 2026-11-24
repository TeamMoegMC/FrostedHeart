# 锻铁炉研究移除锭类观察门槛

- Time: `2026-11-24 16:46:30 +08:00`
- Author: `Codex / OpenAI，实现与验证`
- Status: `completed`
- Scope: TWR 锻铁炉研究开启条件、FrostedHeart 对应的服务端测试与玩家文档。

## Completed

- 在 TWR 的 `iron_bloomery.json` 中移除铜锭或青铜锭观察条件，并从绘图台的条件组移除该项。队伍掌握构想、粗铁观察和任一种燃料观察即可开启。
- 同步定向测试数据、[冶铁炉路线](../docs/knowledge/iron-bloomery.md)和[玩家指南](../docs/knowledge/player-guide.md)。

## Decisions

- 保留铜锭或青铜锭观察对构想触发概率和联络难度的辅助效果。此次修改的范围是项目开启。

## Validation

- `frostedresearch_m4_cp` 的 8 项 Forge 服务端测试全部通过。覆盖三种燃料各自在无锭类观察时开启、清单省略锭类条件、联络辅助效果及风道和试吹流程。
- `validateResearchCatalog` 通过 TWR 的 80 项旧研究定义检查。正式项目 JSON 与测试数据的开启条件一致，项目其他字段与修改前一致。两仓相关改动通过 `git diff --check`。

## Remaining

- 本轮验证在 Forge 服务端完成。客户端未实玩。
