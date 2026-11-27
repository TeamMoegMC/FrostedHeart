# 机械风箱

- Status: 齐备，正式整合包生存游玩待验
- Last verified: 2026-11-23
- Scope: 《冬季救援》机械风箱研究与旧档接续
- Code anchors: `BlockBellowsMixin_ResearchTrial`、`IdeaAcquisitionService`、`the_winter_rescue:mechanical_bellows`

普通风箱经鼓风口为块炼炉有效送风时，操作者的队伍有 1% 基础概率获得想法。档案里的普通风箱和普通活塞观察分别加 2 和 10 个百分点。两者均可检视手持或物品栏中的物品，也可检视已摆放的方块。同一对象的两种观察共用一次加成和同一个联络槽。齿轮设计可降低联络难度。

队伍同时掌握想法和齿轮设计（`the_winter_rescue:bronze_cogwheel_design`）后可开题。先在绘图台完成一局 `frostedresearch:nesting_pack` 零件排样，无物品消耗。推进后，在实验台所在的封闭房间内摆放普通风箱和普通活塞各 1 个，留出与室内空气接触的一面。实验持续 100 游戏刻（20 TPS 时为 5 秒），两个方块保留在原位。

完成研究取得 `the_winter_rescue:mechanical_bellows_design`。手工配方 `the_winter_rescue:research/mechanical_bellows` 用 6 石头、1 普通活塞和 1 普通风箱。接好红石后通电送风，断电复位，再通电可再次送风。

旧研究完成效果 `b814b77435e341c5` 承接到图纸。已投入旧研究的队伍获得入口想法。旧研究线索已扣除的活塞未纳入本次迁移。
