# Curiosity Boss 设计稿与世界观文档对齐

- Time: `2026-08-15 21:20 +08:00`
- Author: `DeepSeek v4 (deepseek-v4-pro); coding agent for TeamMoeg`
- Status: `completed`
- Scope: `docs/curiosity-boss-design.md`；`docs/design/`（只读）

## Completed

- 通读 `docs/design/lore.md`、`docs/design/world-design.md`、`docs/design/technicals.md`
  （均标注 AGENTS SHOULD NEVER MODIFY，只读未改）。
- 将 Boss 设计稿 `docs/curiosity-boss-design.md` 与世界观对齐，修订 §1.1/§4.1/§11/§13/§16。

## Decisions

- Boss 即世界设计文档点名的「匍匐纳米机器人集群」（胞体门类）；外观按胞体设定：
  半透明纱质、可模仿生物形态。
- 击杀方式（仅火伤）与世界设定一致：胞体无法被物理攻击伤害，只能通过提高温度驱散。
- 场地骤冷的设定诠释：纳米机器「无法凭空产热或制冷」但拥有「温度流动的控制」，冷场 =
  集群把场地热量搬运进冻土深处（热泵式排热）——同时解释了加热设备反制与火焰驱散。
- 掉落初案从自创 `nanite_core` 改为「纳米机器人原始粉末」`nanite_powder`，对齐世界设计的
  胞体收集产物（用于开启纳米机器人养殖线，需教会任务）。代码库尚无该物品，Boss 掉落可作
  为首个来源。
- 音乐遵循 technicals.md：OpenGameArts 开源素材；专属音轨须开源，否则 v1 关闭 Boss 音乐。
- 美术：BlockBench 低多边形、16×16、复古未来风（technicals.md）。

## Validation

- 检索代码库确认 `nanite_powder`/「原始粉末」物品与相关 lang 条目尚不存在。

## Remaining

- 等待用户确认修订后设计稿 §16 的 6 个待确认问题，随后按 §14 里程碑 M1–M5 实现。
