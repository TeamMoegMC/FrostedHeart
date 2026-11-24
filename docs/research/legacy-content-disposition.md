# 旧研究目录与新项目衔接

- Status: `Verified`（目录与现行替换映射）
- Last verified: `2026-11-24`（探矿工具配方公开）
- Scope: TWR 旧研究的当前处置、旧存档承接与验证边界。
- Code anchors: [LegacyResearchBridge](../../src/main/java/com/teammoeg/frostedresearch/knowledge/LegacyResearchBridge.java)、[TeamResearchData](../../src/main/java/com/teammoeg/frostedresearch/data/TeamResearchData.java)、[KnowledgeService](../../src/main/java/com/teammoeg/frostedresearch/knowledge/KnowledgeService.java)。

## 当前规则

正式旧目录现有80份JSON。新数据包有效时，下表所列项目的旧推进入口由新项目接替。`banner` 和 `improve_energy` 仍隐藏且不可完成。`banner` 继续封锁四个多方块，`improve_energy` 定义没有 `effects` 字段。

`millstone` 已退役，所有队伍都可在工作台合成石磨。已有队伍研究记录由 `TeamResearchData` 保留，已领取的木炭粉仍是普通物品。石磨任务改在工作台后开放。

| 旧ID | 新项目 | 旧效果到新成果 |
|---|---|---|
| alloy_kiln | the_winter_rescue:earthen_smelter | 350c485a4b92425c→earthen_smelter_design；1bb68ce233ef4f21→earthen_smelter_construction |
| iron | the_winter_rescue:iron_bloomery | c520be3259914225→iron_bloomery_design，含主冶铁炉和正常鼓风口两配方 |
| geology_understanding | the_winter_rescue:prospecting | e1b61496f49f4857→prospecting_method |
| hand_warmer | the_winter_rescue:hand_warmer | 69684c4aa1f4d7a→hand_stove_design。两种炭棒配方公开 |
| thermos_T1 | the_winter_rescue:thermos | 915e5759ba344d58→thermos_design。旧档案仍可领取完成后未领的实物奖励 |
| generator_T1 | the_winter_rescue:generator_t1 | 158adfb5d2a649d0→generator_t1_design，91c6a3ae87914f7a→generator_t1_construction。实物奖励沿用原领取状态 |

旧finished承接对应成果并记旧完成信用，不伪造新实验记录；仅已领取某效果则只承接该映射，不能冒充完成。部分旧投入只授予新想法，保留原材料/点数/线索记录，不把百分比换成实验成功。成功导入有回执，重复登录/重载不重放奖励，遗忘导入成果也不强行补回。有效替换映射停用时恢复旧路线，例如整包撤除或目标配方不可用；原投入可重新选择继续。

新系统真实完成且相关成果有效时可满足旧父项；外来成果可以提供技术权限，但不能单独伪造研究完成。详细来源并集、回执及生命周期见[状态与同步](state-persistence-and-sync.md)和[技术权限](results-and-access.md)。

普通耐火砖是新增项目，没有同名旧项目映射。旧blast_furnace的两个效果、三条线索、mixer父项和generator_T2/simple_machines下游均保留；新成果仅管理自有控温炉批量配方，原陶窑/IE高炉/工业窑工艺继续存在。`thermos_T2` 继续读取 `electrolyzer` 和一级研究的完成状态。

## 逐项处置

| 旧ID | 处置 | 权威定义 |
|---|---|---|
| `advanced_geological_tools` | 已退役，原两项铜配方公开 | [现行流程](../knowledge/prospecting.md) · [保留定义](../../../TheWinterRescue/config/fhresearches/advanced_geological_tools.json) |
| `alloy_kiln` | 由土制冶炼炉新项目接替 | [原定义](../../../TheWinterRescue/config/fhresearches/alloy_kiln.json) |
| `animal_cage` | 沿用原研究 | [原定义](../../../TheWinterRescue/config/fhresearches/animal_cage.json) |
| `arc_furnace` | 沿用原研究 | [原定义](../../../TheWinterRescue/config/fhresearches/arc_furnace.json) |
| `assembler` | 沿用原研究 | [原定义](../../../TheWinterRescue/config/fhresearches/assembler.json) |
| `assembly_line` | 沿用原研究 | [原定义](../../../TheWinterRescue/config/fhresearches/assembly_line.json) |
| `auto_workbench` | 沿用原研究 | [原定义](../../../TheWinterRescue/config/fhresearches/auto_workbench.json) |
| `banner` | 保持既有隐藏/封锁 | [原定义](../../../TheWinterRescue/config/fhresearches/banner.json) |
| `better_fluid_transport` | 沿用原研究 | [原定义](../../../TheWinterRescue/config/fhresearches/better_fluid_transport.json) |
| `big_mixer` | 沿用原研究 | [原定义](../../../TheWinterRescue/config/fhresearches/big_mixer.json) |
| `blast_furnace` | 沿用原研究 | [原定义](../../../TheWinterRescue/config/fhresearches/blast_furnace.json) |
| `blast_furnace_upgrade` | 沿用原研究 | [原定义](../../../TheWinterRescue/config/fhresearches/blast_furnace_upgrade.json) |
| `bottling_machine` | 沿用原研究 | [原定义](../../../TheWinterRescue/config/fhresearches/bottling_machine.json) |
| `car_kiln` | 沿用原研究 | [原定义](../../../TheWinterRescue/config/fhresearches/car_kiln.json) |
| `changing_mask` | 沿用原研究 | [原定义](../../../TheWinterRescue/config/fhresearches/changing_mask.json) |
| `chemical_reactor` | 沿用原研究 | [原定义](../../../TheWinterRescue/config/fhresearches/chemical_reactor.json) |
| `coke_oven` | 沿用原研究 | [原定义](../../../TheWinterRescue/config/fhresearches/coke_oven.json) |
| `create_structure` | 沿用原研究 | [原定义](../../../TheWinterRescue/config/fhresearches/create_structure.json) |
| `crucible` | 沿用原研究 | [原定义](../../../TheWinterRescue/config/fhresearches/crucible.json) |
| `crusher` | 沿用原研究 | [原定义](../../../TheWinterRescue/config/fhresearches/crusher.json) |
| `crushing_wheel` | 沿用原研究 | [原定义](../../../TheWinterRescue/config/fhresearches/crushing_wheel.json) |
| `electrolyzer` | 沿用原研究 | [原定义](../../../TheWinterRescue/config/fhresearches/electrolyzer.json) |
| `electromagnetics` | 沿用原研究 | [原定义](../../../TheWinterRescue/config/fhresearches/electromagnetics.json) |
| `electron_tube` | 沿用原研究 | [原定义](../../../TheWinterRescue/config/fhresearches/electron_tube.json) |
| `enchantment_table` | 沿用原研究 | [原定义](../../../TheWinterRescue/config/fhresearches/enchantment_table.json) |
| `fermenter` | 沿用原研究 | [原定义](../../../TheWinterRescue/config/fhresearches/fermenter.json) |
| `fluid_transport` | 沿用原研究 | [原定义](../../../TheWinterRescue/config/fhresearches/fluid_transport.json) |
| `gain_experience` | 沿用原研究 | [原定义](../../../TheWinterRescue/config/fhresearches/gain_experience.json) |
| `generator_efficiency_1` | 沿用原研究 | [原定义](../../../TheWinterRescue/config/fhresearches/generator_efficiency_1.json) |
| `generator_efficiency_2` | 沿用原研究 | [原定义](../../../TheWinterRescue/config/fhresearches/generator_efficiency_2.json) |
| `generator_heat_1` | 沿用原研究 | [原定义](../../../TheWinterRescue/config/fhresearches/generator_heat_1.json) |
| `generator_regain` | 沿用原研究 | [原定义](../../../TheWinterRescue/config/fhresearches/generator_regain.json) |
| `generator_T1` | 接续为阶段研究，保留塔芯配方、搭建权限和核心奖励 | [能量核心再利用](../knowledge/generator-t1.md) · [原定义](../../../TheWinterRescue/config/fhresearches/generator_T1.json) |
| `generator_T2` | 沿用原研究 | [原定义](../../../TheWinterRescue/config/fhresearches/generator_T2.json) |
| `geology_understanding` | 由探矿镐使用研究接替 | [正式流程](../knowledge/prospecting.md) |
| `hand_warmer` | 由阴燃利用新项目接替 | [正式流程](../knowledge/hand-warmer.md) |
| `heat_incubator` | 沿用原研究 | [原定义](../../../TheWinterRescue/config/fhresearches/heat_incubator.json) |
| `heating_basin` | 沿用原研究 | [原定义](../../../TheWinterRescue/config/fhresearches/heating_basin.json) |
| `heating_vest` | 沿用原研究 | [原定义](../../../TheWinterRescue/config/fhresearches/heating_vest.json) |
| `heavy_engineering` | 沿用原研究 | [原定义](../../../TheWinterRescue/config/fhresearches/heavy_engineering.json) |
| `improve_energy` | 保持既有隐藏/不可完成 | [原定义](../../../TheWinterRescue/config/fhresearches/improve_energy.json) |
| `incubator` | 沿用原研究 | [原定义](../../../TheWinterRescue/config/fhresearches/incubator.json) |
| `industrial_electrolyzer` | 沿用原研究 | [原定义](../../../TheWinterRescue/config/fhresearches/industrial_electrolyzer.json) |
| `iron` | 由冶铁炉新项目接替 | [原定义](../../../TheWinterRescue/config/fhresearches/iron.json) |
| `lining_upgrade` | 沿用原研究 | [原定义](../../../TheWinterRescue/config/fhresearches/lining_upgrade.json) |
| `mechanical_bellows` | 已迁移 | [机械风箱研究](../knowledge/mechanical-bellows.md) |
| `mechanical_calculator` | 沿用原研究 | [原定义](../../../TheWinterRescue/config/fhresearches/mechanical_calculator.json) |
| `mechanical_crafter` | 沿用原研究 | [原定义](../../../TheWinterRescue/config/fhresearches/mechanical_crafter.json) |
| `mechanical_press` | 沿用原研究 | [原定义](../../../TheWinterRescue/config/fhresearches/mechanical_press.json) |
| `mechanical_saw` | 沿用原研究 | [原定义](../../../TheWinterRescue/config/fhresearches/mechanical_saw.json) |
| `mechanics_measurement` | 沿用原研究 | [原定义](../../../TheWinterRescue/config/fhresearches/mechanics_measurement.json) |
| `mechanics_tools` | 沿用原研究 | [原定义](../../../TheWinterRescue/config/fhresearches/mechanics_tools.json) |
| `metal_press` | 沿用原研究 | [原定义](../../../TheWinterRescue/config/fhresearches/metal_press.json) |
| `millstone` | 退役，石磨配方公开 | [工作台配方](../../../TheWinterRescue/kubejs/server_scripts/src/recipes/shaped/new.js) |
| `mixer` | 沿用原研究 | [原定义](../../../TheWinterRescue/config/fhresearches/mixer.json) |
| `multiblock_structure` | 沿用原研究 | [原定义](../../../TheWinterRescue/config/fhresearches/multiblock_structure.json) |
| `precision_mechanism` | 沿用原研究 | [原定义](../../../TheWinterRescue/config/fhresearches/precision_mechanism.json) |
| `propaganda` | 沿用原研究 | [原定义](../../../TheWinterRescue/config/fhresearches/propaganda.json) |
| `prospecting_upgrade` | 已退役，原六项铁钢配方公开 | [现行流程](../knowledge/prospecting.md) · [保留定义](../../../TheWinterRescue/config/fhresearches/prospecting_upgrade.json) |
| `rescue_plan_initiated` | 沿用原研究 | [原定义](../../../TheWinterRescue/config/fhresearches/rescue_plan_initiated.json) |
| `revolver` | 沿用原研究 | [原定义](../../../TheWinterRescue/config/fhresearches/revolver.json) |
| `rotary_kiln` | 沿用原研究 | [原定义](../../../TheWinterRescue/config/fhresearches/rotary_kiln.json) |
| `rubbing_tool` | 沿用原研究 | [原定义](../../../TheWinterRescue/config/fhresearches/rubbing_tool.json) |
| `sauna_vent` | 沿用原研究 | [原定义](../../../TheWinterRescue/config/fhresearches/sauna_vent.json) |
| `sawmill` | 沿用原研究 | [原定义](../../../TheWinterRescue/config/fhresearches/sawmill.json) |
| `simple_machines` | 沿用原研究 | [原定义](../../../TheWinterRescue/config/fhresearches/simple_machines.json) |
| `soil_thermometer` | 沿用原研究 | [原定义](../../../TheWinterRescue/config/fhresearches/soil_thermometer.json) |
| `squeezer` | 沿用原研究 | [原定义](../../../TheWinterRescue/config/fhresearches/squeezer.json) |
| `steam_heater` | 沿用原研究 | [原定义](../../../TheWinterRescue/config/fhresearches/steam_heater.json) |
| `steam_powerd` | 沿用原研究 | [原定义](../../../TheWinterRescue/config/fhresearches/steam_powerd.json) |
| `steam_turbine` | 沿用原研究 | [原定义](../../../TheWinterRescue/config/fhresearches/steam_turbine.json) |
| `stering_generator` | 沿用原研究 | [原定义](../../../TheWinterRescue/config/fhresearches/stering_generator.json) |
| `storage_bucket` | 沿用原研究 | [原定义](../../../TheWinterRescue/config/fhresearches/storage_bucket.json) |
| `storage_drawers` | 沿用原研究 | [原定义](../../../TheWinterRescue/config/fhresearches/storage_drawers.json) |
| `tetra` | 沿用原研究 | [原定义](../../../TheWinterRescue/config/fhresearches/tetra.json) |
| `thermos_T1` | 由基础隔热容器新项目接替 | [正式流程](../knowledge/thermos.md) |
| `thermos_T2` | 沿用原研究 | [原定义](../../../TheWinterRescue/config/fhresearches/thermos_T2.json) |
| `transmission_machinery` | 接续为阶段研究，保留旧成果映射 | [传动机械](../knowledge/transmission-machinery.md) · [原定义](../../../TheWinterRescue/config/fhresearches/transmission_machinery.json) |
| `villager_language` | 沿用原研究 | [原定义](../../../TheWinterRescue/config/fhresearches/villager_language.json) |
| `wind_utilize` | 沿用原研究 | [原定义](../../../TheWinterRescue/config/fhresearches/wind_utilize.json) |
| `wool_armor` | 沿用原研究 | [原定义](../../../TheWinterRescue/config/fhresearches/wool_armor.json) |

## 验证范围

- 2026-11-12 建表时逐项读取81份JSON，并与基线 `2d0fefd349f65f3118828c9712f68a8c0782ab75` 比对。2026-11-18 移除 `millstone` 后，目录保留80份定义。
- M3/M4的旧完成、单效果、部分投入、重复初始化、缺定义恢复、权限并集与父项投影有真实服务端测试；M3/M4正式包客户端已检查替换入口及正常完成权限。详见[M3桥接](../../diary/2026-11-12_15-29-57_research-m3b-legacy-bridge-handoff.md)、[M4服务端](../../diary/2026-11-12_17-27-52_research-m4-full-stage-tuyere-handoff.md)和[M4客户端](../../diary/2026-11-12_17-35-19_research-m4-client-fixed-pack-handoff.md)。
- M4与M5的独立进程保存/恢复已验证；两个真实客户端的同队实验、放置者离线后继续和重连同步也已通过，见[M7记录](../../diary/2026-11-12_19-04-00_research-m7-two-client-validation.md)。这些证据不等于崩溃恢复、完整生存材料获取或所有旧世界均已人工通关。
- 显式知识merge保留同项目多份记录，研究页与设备统一按创建时间、UUID排序并可选择记录。完成一份且成果仍有效时，其余进度保留为备用，暂停任务和临时权限；遗忘成果后恢复。普通加入队伍不会自动merge，世界设备和未交样品也不会随知识合并改绑。见[当前状态规则](../knowledge/state-and-api.md#m6-合并记录排序与备用进度)。相关32项测试及最后快照/多人交错修正的16项定向测试通过；[最终客户端](../../diary/2026-11-12_20-12-00_research-m5-m6-client-validation.md)另验证两份构造记录的序号/切换、炉目标、实际出样与备用显示，不冒称客户端真实中途合队。
