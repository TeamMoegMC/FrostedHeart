# 传动机械

- 状态为已通过 Forge 服务端验证，正式整合包的生存流程待实玩。
- 核对日期为 2026-11-24。
- 涵盖冶铁炉成果接续、排样、手摇传动试装、四项设计和旧档桥接。
- 以[正式项目定义](../../../TheWinterRescue/kubejs/data/the_winter_rescue/frostedresearch/knowledge/projects/transmission_machinery.json)、`ResearchStages.update`、`ExperimentTableBlockEntity.start` 和 `LegacyResearchBridge` 为锚点。

## 游戏流程

1. 完成冶铁炉研究，取得 `the_winter_rescue:iron_bloomery_design`，在绘图台开启「传动机械」。从旧研究迁入的进行中队伍也可凭迁移时领取的「手摇传动」想法开启。
2. 铁棒和青铜板的对应观察记录只要在队伍来件或档案中，就会自动计入。新记录可以在绘图台检视实物取得，也可以手持实物长按观察键并保留。铁棒由 3 个铁锭斜排合成，一次做 4 根。青铜锭可用锤在树桩上敲成青铜板。
3. 在绘图台完成一次 `frostedresearch:nesting_pack`。所有零件装进凹槽后确认定稿。第一阶段三项任务就绪后，`the_winter_rescue:research/shaft_from_iron_rods` 临时开放，2 根铁棒竖排出 8 根传动轴。手动进入下一阶段后仍可使用这条配方，临时权限随本轮研究结束而撤销。
4. 在实验台选择「齿轮形状验证」，投入 10 块青铜板 `frostedheart:bronze_sheet` 和 1 根传动轴 `create:shaft`。开工即扣料，运行 200 游戏刻后记录一次实验完成。实验无需房间条件，没有固定产物或原物返还。材料不足时可补齐再开工，开工后停机需重新备料，已完成的检视和拼图进度保留。
5. 回绘图台完成研究。队伍同时获得传动轴、手摇曲柄、青铜大小齿轮四项设计，铁棒制轴由临时权限接为永久权限。任务书「点燃锅炉，令其狂吼」按传动轴、手摇曲柄、青铜小齿轮、青铜大齿轮依次引导制作。

实验台按[现有工作台配方和操作](workbench.md#实验台)获取。青铜小齿轮属于活塞接受的 `#the_winter_rescue:cogwheels`，可接入机械风箱支线。

## 配方和旧档

稳定配方 ID 都以 `the_winter_rescue:research/` 开头。轴设计包含 `shaft_from_iron_rods`、`shaft_from_cast_iron_rods` 和 `shaft_from_cast_iron_cutting`，其余设计分别包含 `hand_crank`、`bronze_cogwheel`、`bronze_large_cogwheel`。Create 机械锯通过 `RecipeFinder` 选配方，研究的工作台合成限制不覆盖该机器执行路径。

旧 `transmission_machinery` 的四个配方效果各自映射到同一部件的新设计。已完成的队伍获得全部设计和旧完成资格。仍在研究的队伍保留旧记录和已领取的配方，通过迁移想法从新版排样阶段开始。新研究完成且四项成果有效时，旧研究的下游父项判定认可其完成。
