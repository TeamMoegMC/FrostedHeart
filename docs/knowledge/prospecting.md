# 探矿镐的使用方法

- 状态 `Current`
- 核对日期 `2026-11-24`
- 范围为 TWR 的探矿镐使用权限、铜铁钢工具配方与旧档承接。
- 源码锚点为 `ProspectorPick#doProspect`、`KnowledgeUseAcquisitionMixin`、`ProjectRequirement.Material` 和 `LegacyResearchBridge`。正式项目为 `the_winter_rescue:prospecting`。

铜、铁、钢制探矿镐、探铲和地质锤备齐材料即可合成。普通探矿镐与带探矿部件的 Tetra 工具都可以观察、投入实验台和用于探测。实验与观察都识别同一个 Forge 工具动作 `prospector_pick`。

右键尝试探测时先检查队伍权限。若缺少权限，且档案与来件中都没有探矿研究的想法，队伍会当场获得想法，触发概率为 100%。这次操作会停在研究提示，完成研究后即可探测。

1. 玩家取得 Create 进度 `create:copper` 后，每成功挖掉一个 `#forge:ores` 方块产生一次 `frostedheart:ore_mined_after_copper` 事件。队伍获得想法的基础概率为 1%，档案中每有一类有效观察再加 1 个百分点，三类齐全时为 4%。三类是探矿镐物品、`#forge:ores` 方块及 `#forge:stone` 方块。
2. 在绘图台把探矿镐观察放入联络栏也可以获得想法。矿石与石头观察每缺一项增加一级联络难度。开题需要队伍档案中的想法与全部三类观察。旧的探矿镐观察没有记录工具动作字段时，需要重新观察。
3. 实验台所在的封闭房间须有至少 16 块 `#forge:stone` 和 1 块 `#forge:ores` 与室内空气相邻。可以利用矿道中已有的天然矿石。放入一把探矿镐，运行 600 游戏 tick（20 TPS 时为 30 秒）。成功后在领取槽取回同一把镐，名称、部件、NBT 和耐久均保留。房间里的方块留在原处。
4. 回绘图台完成研究后，队伍获得 `the_winter_rescue:prospecting_method`，可用探矿镐右键矿石旁的普通岩石探测，或直接右键矿石查看名称。

项目接替旧「岩石的性质」`geology_understanding`。旧研究已完成或已领取旧效果 `e1b61496f49f4857` 时，队伍承接探测权限。旧研究只完成了部分投入时承接想法。旧「地质学初步」`advanced_geological_tools` 和「勘矿升级」`prospecting_upgrade` 已退役，既有研究记录保存在旧档中。

参见[正式项目](../../../TheWinterRescue/kubejs/data/the_winter_rescue/frostedresearch/knowledge/projects/prospecting.json)、[实验台](workbench.md#实验台)和[任务书](../../../TheWinterRescue/config/ftbquests/quests/chapters/t1.snbt)。
