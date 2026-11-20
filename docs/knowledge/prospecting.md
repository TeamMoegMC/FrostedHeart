# 探矿镐的使用方法

- 状态 `Current`
- 核对日期 `2026-11-19`
- 范围为 TWR 的基础探矿研究及旧「岩石的性质」承接。
- 源码锚点为 `ProspectorPick#doProspect`、`KnowledgeUseAcquisitionMixin`、`ProjectRequirement.Material` 和 `LegacyResearchBridge`。正式项目为 `the_winter_rescue:prospecting`。

铜探矿镐可直接合成。普通探矿镐与带探矿部件的 Tetra 工具都可以观察、投入实验台和用于探测。实验与观察都识别同一个 Forge 工具动作 `prospector_pick`。

右键尝试探测时先检查队伍权限。若缺少权限，且档案与来件中都没有探矿研究的想法，队伍会当场获得想法，触发概率为 100%。这次操作会停在研究提示，完成研究后即可探测。

1. 玩家取得 Create 进度 `create:copper` 后，每成功挖掉一个 `#forge:ores` 方块产生一次 `frostedheart:ore_mined_after_copper` 事件。队伍获得想法的基础概率为 1%，档案中每有一类有效观察再加 1 个百分点，三类齐全时为 4%。三类是探矿镐物品、`#forge:ores` 方块及 `#forge:stone` 方块。
2. 在绘图台把探矿镐观察放入联络栏也可以获得想法。矿石与石头观察每缺一项增加一级联络难度。开题需要队伍档案中的想法与全部三类观察。旧的探矿镐观察没有记录工具动作字段时，需要重新观察。
3. 实验台所在的封闭房间须有至少 16 块 `#forge:stone` 和 1 块 `#forge:ores` 与室内空气相邻。可以利用矿道中已有的天然矿石。放入一把探矿镐，运行 600 游戏 tick（20 TPS 时为 30 秒）。成功后在领取槽取回同一把镐，名称、部件、NBT 和耐久均保留。房间里的方块留在原处。
4. 回绘图台完成研究后，队伍获得 `the_winter_rescue:prospecting_method` 成果。右键矿石旁的普通岩石，沿岩壁换点可比较附近的粗略读数。直接右键矿石会显示其名称。探测前会核对队伍使用权限。

旧 `geology_understanding` 完成或已有旧配方效果 `e1b61496f49f4857` 的队伍直接承接探测权限。已有部分投入会承接想法。后续旧研究 `advanced_geological_tools` 接受原项目完成或真实完成新项目且掌握有效成果。已有原始旧进度由旧档继续保存。

参见[正式项目](../../../TheWinterRescue/kubejs/data/the_winter_rescue/frostedresearch/knowledge/projects/prospecting.json)、[实验台](workbench.md#实验台)和[任务书](../../../TheWinterRescue/config/ftbquests/quests/chapters/t1.snbt)。
