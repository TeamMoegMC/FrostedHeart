# 基础隔热容器

- Status: `Current`（实现与整合包数据）
- Last verified: `2026-11-20`
- Scope: 普通保温杯的发现、实验台产出、队伍配方权限和旧档承接。
- Code anchors: [WaterCommonEvents](../../src/main/java/com/teammoeg/frostedheart/content/water/event/WaterCommonEvents.java)、[LegacyResearchBridge](../../src/main/java/com/teammoeg/frostedresearch/knowledge/LegacyResearchBridge.java)、[正式项目](../../../TheWinterRescue/kubejs/data/the_winter_rescue/frostedresearch/knowledge/projects/thermos.json)。

## 玩家流程

1. 用木杯、金属杯或皮水袋喝完冷水，有机会得到「便携热饮的构想」。也可观察木杯、学入队伍档案，再用这份观察在绘图台联络。掌握构想后，在研究页开启「基础隔热容器」。
2. 合成实验台。台面需要玻璃瓶、工作台和木板。选中「组装木质外壳与铜内胆」，放入 1 张铜板和 1 块木板，点击开工。连续运行 200 tick（20 TPS 时 10 秒）后，从领取槽取出 1 只空保温杯。
3. 回到绘图台完成研究。队伍学会「保温杯设计」，可用 3 张铜板、4 根原木和 2 块木板再做一只，亦可给已有的染色杯去染色。实验产出的杯子可以立即使用，装满可喝六口。T1 的保温杯任务接受实验产出的普通杯。

## 触发与权限

- `frostedheart:drank_cold_water` 读取服务端饮用完成事件里的那一口流体。冷水匹配 `minecraft:water` 和已装在杯中的 `frostedheart:purified_water`。容器按已有流体标签取液，开局可直接接原水。一次有效饮用判定一次。木杯、金属杯和皮水袋由 `the_winter_rescue:research/cold_water_containers` 匹配。
- `chance=0.05`，`optional_bonus=0.45`。队伍档案中一条或多条 `#frostedheart:thermos` 实物观察都只计一个加成，最终概率为 50%。联络只使用 `the_winter_rescue:research/wooden_drinking_cups` 对应的木杯观察。开启条件只有构想。
- `the_winter_rescue:thermos_design` 授予 `the_winter_rescue:research/thermos` 和 `frostedheart:crafting/thermos_from_dyed`。实验台交付实物，绘图台完成项目后授予设计。热饮补水沿用原流体规则。
- 旧 `thermos_T1` 完成承接设计和二级父项。完成但漏领旧赠杯的队伍可在旧研究档案中领取，使用原效果回执。旧部分进度授予构想，保存原投入。新队伍需真实完成项目并保有有效设计才能满足 `thermos_T2` 的一级父项。二级还需 `electrolyzer`。
