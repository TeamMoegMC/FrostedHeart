# 土制冶炼炉的沙观察

- Time: `2026-11-24 18:58:20 +08:00`
- Author: `Codex（OpenAI，早期研究流程实现与验证）`
- Status: `completed`
- Scope: 土制冶炼炉材料观察、任务说明和测试夹具。核对普通耐火砖、手炉及保温杯的观察入口。

## Completed

- 土制冶炼炉以 `block_object` 匹配 `minecraft:sand`。同一沙条件用于构想概率加成、联络减难和开题。可记录原地的沙，也可检视背包中的沙。更新了[土制炉流程](../docs/knowledge/earthen-smelter.md)和中英文任务书。
- 普通耐火砖的联络和开题已接受砂砖单砖或砂砖方块物品，高岭土、铝土矿已接受矿块和冻土块物品。耐火砖温度提示已接受世界方块和物品观察。

## Decisions

- 土炉和耐火砖的烧成证明仍读取本次研究产出的样品物品。手炉联络读取实际炭类物品的取样温度，保温杯构想读取实际饮水行为或木杯物品观察。这些条件分别对应具体材料、实验和行为。

## Validation

- Forge 1.20.1 的 `frostedresearch_m2`、`frostedresearch_m3`、`frostedresearch_m3b` 共 25 项必需 GameTest 通过，覆盖两种沙观察减难、真实放置的沙开题和后续试烧。日志为 `build/early-material-observation-20261124.log`。
- 整合包三处沙条件和四份测试夹具一致，两个语言文件可解析。旧目录 80 份定义通过预检。

## Remaining

- 正式整合包客户端的观察和任务文案待实测。
