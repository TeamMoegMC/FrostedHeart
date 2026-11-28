# 焦炉研究

- Status: Current
- Last verified: 2026-11-28
- Scope: T1 可选焦炉研究、控温试样、权限和旧档承接。
- Code anchors: `KnowledgeUseAcquisitionMixin`、`IdeaAcquisitionService`、`ControlledFiringRecipe`、`LegacyResearchBridge`。

## 玩家路线

1. 从原始木炭坑取得木炭，从煤炭坑取得焦煤，检视两种普通产物。每成功挖掉一块烧成的木炭堆或焦炭堆，队伍有 1% 概率获得「可复用的焦炉」构想。队伍已学习砂砖单砖或砂砖方块的观察时，概率为 11%。也可在绘图台联络木炭和焦煤的观察。原木、煤炭和砂砖观察各降低一级联络难度。
2. 队伍掌握构想和普通木炭、焦煤的物品观察后，在绘图台开启研究。控温实验炉可用黏土球、沙子和营火合成。设为 400°C 或更高，分别试烧原木和煤炭。定时或手动停炉后，根据产物调整下一次试烧的时长。
3. 新鲜检视一份烧透的木炭试样和一份烧透的焦煤试样。成功判据是试样物品 ID，检视保留实物。回绘图台完成研究，获得焦炉砖配方和焦炉成形权限。T1 可选任务 `5287CFB7B7D5285E` 在研究之后引导收集 27 块焦炉砖并搭炉。

## 配方、触发和存档

- `frostedresearch:charcoal_pit_harvest` 在服务端成功挖掘 `#frostedresearch:charcoal_pit_harvest` 时发出，标签接受 `charcoal_pit:wood_ash` 和 `charcoal_pit:coal_ash`。砂砖加成为一次 0.10。焦煤观察接受 `#forge:coal_coke`。
- [焦炉项目](../../../TheWinterRescue/kubejs/data/the_winter_rescue/frostedresearch/knowledge/projects/coke_oven.json)检查 `kubejs:fired_charcoal_sample` 和 `kubejs:fired_coke_sample`。试样只作为实验产物和观察对象。
- [试烧配方](../../../TheWinterRescue/kubejs/data/the_winter_rescue/recipes/research/coke_oven/)使用 400–1400°C（含两端）。原木输入为 `#minecraft:logs`，煤炭输入为 `minecraft:coal`。正常游戏速度下，原木的三个时长区间分别为 (20, 45]、(45, 90]、(90, ∞) 秒，煤炭为 (40, 90]、(90, 180]、(180, ∞) 秒。其他时长保留原料。游戏内说明给出温度门槛，玩家根据试样探索时长。
- 成果 `coke_brick_design` 开放 `the_winter_rescue:research/coke_brick`。每批用 5 块烧成砂砖单砖、2 份砂浆和 2 块结构板制成 3 块焦炉砖，结构板可用青铜板。成果 `coke_oven_construction` 开放 `immersiveengineering:multiblocks/coke_oven` 成形。
- 旧 `coke_oven` 已完成时承接两项成果及完成状态。单项效果按其原记录承接。部分投入承接构想。映射见[旧研究目录](../research/legacy-content-disposition.md)。
