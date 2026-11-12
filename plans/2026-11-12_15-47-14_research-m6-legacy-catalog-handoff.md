# M6 旧研究目录与三项目衔接清单

- Time: 2026-11-12 15:47:14 +08:00
- Authors: Codex / OpenAI GPT-6 Luna；M6 只读准备
- Status: completed；目录处置已落实并有对应机制验证，当前目录由docs维护。
- Scope: TWR config/fhresearches/*.json 旧目录、三条新研究线的父子关系/权限交接；不实施迁移。
- Related: [M0 旧目录](2026-11-12_12-34-00_research-m0-legacy-catalog.md)、[M3 合金炉桥接](2026-11-12_14-00-00_research-m3-legacy-alloy-kiln-bridge.md)、[M3-B 玩家路线](../docs/knowledge/earthen-smelter.md)、[M6 总计划](2026-11-11_23-38-07_research-system-playable-development-plan.md)。

## 核对结论

当前 TWR 配置目录仍是 **81 个**旧 JSON；逐项与 M0 清单比对，ID 集合 **81/81 一致**。父子项取 parents 反查，效果为旧 JSON effects 的类别和实际目标。下表 R/I/M/U/S/X 分别表示 recipe/item reward/multiblock/use/stats/experience；空父项表示没有显式边，不能代替材料依赖审查。

| 旧 ID | 显式父项 | 原 effect 类别 → 目标 |
|---|---|---|
| advanced_geological_tools | geology_understanding | R → frostedheart:copper_core_spade, frostedheart:copper_geologists_hammer |
| alloy_kiln | — | M → immersiveengineering:multiblocks/alloy_smelter; R → immersiveengineering:alloybrick |
| animal_cage | crucible | R → supplementaries:cage; I → supplementaries:cage |
| arc_furnace | heavy_engineering | M → immersiveengineering:multiblocks/arcfurnace |
| assembler | multiblock_structure | M → immersiveengineering:multiblocks/assembler |
| assembly_line | electromagnetics | R → immersiveengineering:circuit_board, kubejs:servo_core, immersiveengineering:capacitor_lv/mv/hv |
| auto_workbench | assembler | M → immersiveengineering:multiblocks/auto_workbench |
| banner | — | hidden=true、locked=true，图标 barrier；M → immersiveengineering:multiblocks/excavator, lightning_rod, refinery, diesel_generator（按既有禁用设备策略停用） |
| better_fluid_transport | electrolyzer | R → immersiveengineering:fluid_pump |
| big_mixer | multiblock_structure | M → immersiveengineering:multiblocks/mixer |
| blast_furnace | mixer | M → immersiveengineering:multiblocks/blast_furnace; R → immersiveengineering:blastbrick |
| blast_furnace_upgrade | crucible | M → immersiveengineering:multiblocks/improved_blast_furnace; R → immersiveengineering:blastbrick_reinforced |
| bottling_machine | chemical_reactor | M → immersiveengineering:multiblocks/bottling_machine |
| car_kiln | metal_press, squeezer | M → immersiveindustry:multiblocks/car_kiln |
| changing_mask | villager_language | S → vlg_forgive |
| chemical_reactor | big_mixer | M → immersiveindustry:multiblocks/chemical_reactor |
| coke_oven | — | M → immersiveengineering:multiblocks/coke_oven; R → immersiveengineering:cokebrick |
| create_structure | steam_powerd | R → create:super_glue, clockwork_bearing, gantry_carriage/shaft, mechanical_piston, sticky_mechanical_piston, piston_extension_pole |
| crucible | electromagnetics, heating_basin | M → immersiveindustry:multiblocks/crucible; R → immersiveindustry:burning_chamber |
| crusher | heavy_engineering | M → immersiveengineering:multiblocks/crusher |
| crushing_wheel | mechanical_crafter, mechanical_calculator | R → create:crushing_wheel |
| electrolyzer | crucible, assembly_line | R → immersiveindustry:electrolyzer |
| electromagnetics | mechanical_crafter, mechanical_calculator | R → immersiveengineering:wirecoil_copper, steampowered:alternator, kubejs:motor |
| electron_tube | simple_machines | R → create:deployer, mechanical_arm, electron_tube |
| enchantment_table | tetra, simple_machines | R → minecraft:enchanting_table; U → minecraft:enchanting_table |
| fermenter | multiblock_structure | M → immersiveengineering:multiblocks/fermenter |
| fluid_transport | transmission_machinery | R → create:fluid_pipe, mechanical_pump, fluid_tank |
| gain_experience | enchantment_table, mechanical_calculator | X → 250 experience |
| generator_T1 | — | R → frostedheart:generator_core_t1; I → frostedheart:energy_core; M → frostedheart:multiblocks/generator |
| generator_T2 | generator_T1, steam_powerd, blast_furnace | M → frostedheart:multiblocks/generator_t2; R → frostedheart:heat_pipe, charger |
| generator_efficiency_1 | generator_T1, steam_powerd | S → generator_effi |
| generator_efficiency_2 | generator_T2, generator_efficiency_1, car_kiln | S → generator_effi |
| generator_heat_1 | steam_heater, steam_turbine | S → generator_heat |
| generator_regain | generator_T1 | I → frostedheart:energy_core |
| geology_understanding | — | R → frostedheart:copper_pro_pick |
| hand_warmer | — | R → frostedheart:hand_stove, coal_stick, charcoal_stick |
| heat_incubator | generator_T2, incubator | R → frostedheart:heat_incubator; I → frostedheart:heat_incubator |
| heating_basin | mechanical_crafter, mechanical_calculator | R → create:blaze_burner; I → minecraft:bucket, frostedheart:cooled_magma_block |
| heating_vest | generator_T2 | R → frostedheart:heater_vest |
| heavy_engineering | industrial_electrolyzer | R → immersiveengineering:heavy_engineering |
| improve_energy | transmission_machinery | 无 effects 字段（按空集合）；hidden=true、locked=true，既有隐藏且不可完成项目 |
| incubator | — | R → frostedheart:incubator; I → frostedheart:straw_briquette_brown_mushroom |
| industrial_electrolyzer | metal_press, squeezer | M → immersiveindustry:multiblocks/industrial_electrolyzer |
| iron | — | R → charcoal_pit:main_bloomery |
| lining_upgrade | — | R → frostedheart:rabbit_fur_{hat,jacket,pants,socks,gloves}, fox_fur_{hat,jacket,pants,socks,gloves}, wolf_fur_{hat,jacket,pants,socks,gloves}, polar_bear_fur_{hat,jacket,pants,socks,gloves}, hide_{hat,jacket,pants,boots,gloves}, buff_coat |
| mechanical_bellows | — | R → charcoal_pit:mechanical_bellows |
| mechanical_calculator | mechanical_crafter, steam_powerd, precision_mechanism | R → frostedresearch:mechanical_calculator; U/I → frostedresearch:mechanical_calculator |
| mechanical_crafter | steam_powerd, electron_tube | R → create:mechanical_crafter |
| mechanical_press | simple_machines | R → create:mechanical_press |
| mechanical_saw | simple_machines | R → create:mechanical_saw |
| mechanics_measurement | mechanics_tools | R → create:speedometer, stressometer |
| mechanics_tools | transmission_machinery | R → create:wrench, goggles |
| metal_press | multiblock_structure | M → immersiveengineering:multiblocks/metal_press |
| millstone | — | R → stone_age:millstone; I → kubejs:charcoal_dust |
| mixer | transmission_machinery | R → create:mechanical_mixer, basin; I → create:basin |
| multiblock_structure | electrolyzer | R → immersiveengineering:light_engineering, rs_engineering |
| precision_mechanism | electron_tube | R → create:precision_mechanism, rotation_speed_controller |
| propaganda | rescue_plan_initiated | S → vlg_forgive |
| prospecting_upgrade | advanced_geological_tools | R → frostedheart:iron_pro_pick/core_spade/geologists_hammer, steel_pro_pick/core_spade/geologists_hammer |
| rescue_plan_initiated | villager_language | S → vlg_relationship |
| revolver | — | R → immersiveengineering:revolver, gunpart_barrel/hammer/drum, empty_casing; twr:blueprint_bullet |
| rotary_kiln | squeezer | M → immersiveindustry:multiblocks/rotary_kiln |
| rubbing_tool | mechanical_calculator | R → frostedresearch:rubbing_pad, rubbing_tool |
| sauna_vent | heating_vest, steam_heater | R/U/I → frostedheart:sauna_vent |
| sawmill | mechanical_saw, heavy_engineering | M → immersiveengineering:multiblocks/sawmill |
| simple_machines | blast_furnace | R → create:millstone, encased_fan |
| soil_thermometer | precision_mechanism | R → frostedheart:soil_thermometer |
| squeezer | multiblock_structure | M → immersiveengineering:multiblocks/squeezer |
| steam_heater | generator_T2 | M → frostedheart:multiblocks/heat_radiator; I → immersiveengineering:sheetmetal_constantan |
| steam_powerd | fluid_transport, simple_machines | I → steampowered:bronze_steam_engine, bronze_flywheel; R → steampowered:bronze_flywheel, cast_iron_flywheel, steel_flywheel, bronze_steam_engine, cast_iron_steam_engine, steel_steam_engine |
| steam_turbine | squeezer, fermenter | M → immersiveindustry:multiblocks/steam_turbine |
| stering_generator | generator_T2, steam_powerd | I/R → frostedheart:steam_core |
| storage_bucket | — | R/I → minecraft:barrel |
| storage_drawers | — | R → storagedrawers:compacting_drawers_3 |
| tetra | — | U → tetra:basic_workbench |
| thermos_T1 | — | R/I → frostedheart:thermos |
| thermos_T2 | electrolyzer, thermos_T1 | R → frostedheart:advanced_thermos |
| transmission_machinery | — | R → steampowered:bronze_large_cogwheel, bronze_cogwheel; create:shaft, hand_crank |
| villager_language | — | I → minecraft:book |
| wind_utilize | transmission_machinery | R → frostedheart:fabric_vawt, metal_vawt, alloy_vawt |
| wool_armor | generator_T1 | R → frostedheart:wool_hat/jacket/pants/boots/gloves, gambeson |

原 namespace 在表内按目标完整列出；同一行逗号分隔的同类目标沿用前面的 namespace，花括号列出完整后缀集合。旧 JSON 没有 effects 字段时按空集合记录；improve_energy 当前没有 effects 字段。

## 三条新研究线与旧权限交叉

| 新项目 | 可确认旧 ID | 旧父/子项 | 旧 effect nonce 与目标 | M6 处理 |
|---|---|---|---|---|
| 土制冶炼炉（M3） | alloy_kiln | 无显式父项或子项 | 350c485a4b92425c R → immersiveengineering:alloybrick；1bb68ce233ef4f21 M → immersiveengineering:multiblocks/alloy_smelter | **事实：** FH LegacyResearchBridge 已实现定义映射、旧完成/部分投入承接和旧入口替换；TWR 项目同时声明两结果。M6核对实际加载、旧队伍及结果权限，不再另造第二个 alloy_kiln 映射。原 TWR alloy_kiln.json 和 nonce 保留。新入口为矿物触发想法或该想法专属零输入联络；不能把这视为所有旧研究自动公开。 |
| 冶铁炉（M4） | iron | 无显式父项或子项 | c520be3259914225 R → charcoal_pit:main_bloomery | **已采用、待实现（[M4 决定](2026-11-12_15-45-00_research-m4-stage-tuyere-interface-decisions.md)）：**旧 finished 或已领取此 nonce，映射到一个含 the_winter_rescue:research/main_bloomery 与 the_winter_rescue:research/sandy_tuyere 两配方的 Design；只有旧 finished 写入 completion_credit。仅领取 effect 的队伍取得双配方权限，但不伪造项目完成；部分投入只承接想法并保留原投入。新入口仍是 M4 项目自己的矿物/制品想法路径，不靠手动 grant。 |
| 普通耐火砖（M5） | **无同名旧研究 ID** | 不得将 blast_furnace 当作同义旧 ID | 未发现旧 effect 直接授予 frostedheart:refractory_bricks 配方；blast_furnace clue 67c2d1728d8a4af1 将一块该物品作为线索。该项目自身 effect 是 8a9d01be7d574eaa M → immersiveengineering:multiblocks/blast_furnace，7a59b074753747e6 R → immersiveengineering:blastbrick。 | **待 M5：**普通砖为新项目；检查其最终成果/配方与旧 clue 的可达交叉。保留旧 blast_furnace 两项权限、父项 mixer 和子项 generator_T2、simple_machines。不要整项映射/停用 blast_furnace。arc_furnace 的 frostedheart:high_refractory_bricks 是另一种耐材，旧权限为电弧炉多方块；不得合并。 |

thermos_T1、thermos_T2 **均沿用原项目、原权限和父项**；保温杯改版延后只表示不在本轮重做，不是取消或松锁。除 alloy_kiln、iron 两项拟替换外，其余 **79 个旧 ID** 均按现状保留配置、父子关系及已有 effects；缺失 effects 字段按空集合，不代表新增效果。其中 improve_energy 与 banner 是既有隐藏/封锁条目，按现有状态保留，不作为可玩性缺陷或擅自开放；不表示其余 79 项全部可玩。本清单不删除、不移除权限、不取消研究限制。

## M6 立即执行清单

1. 把上表作为 81 项处置基线；每个实施批次前重新从 TWR config/fhresearches/ 与新数据计算父子图，ID 集合或目标变化后先复核。M0 与当前是 81/81 同 ID，不代表以后目录数量固定。
2. 对 M3 只验已有 alloy_kiln 桥接：旧完成导入两个正确结果，单效果按 nonce 只导入对应权限，旧部分投入保留原数据并按 bridge 规则给想法，旧入口不再重复推进；新完成不伪造旧完成；分别确认配方和多方块权限可用。
3. M4 实施 iron 已采用的桥接：旧 finished 或 nonce c520be3259914225 领取记录都学入同一个双配方 Design；只有旧 finished 记录获得 completion_credit。确认单 nonce 到一个 Design 的桥接验证可通过，并核对两个配方 ID/权限均有效；部分投入继续保留并只承接想法。
4. M5 不设置旧 refractory_brick 迁移键。确认普通耐火砖新成果和当前 blast_furnace clue 的交互；旧高炉权限、mixer 父项、generator_T2/simple_machines 子链保持可完成。核对 arc_furnace/crucible 的高耐火砖线索独立存在。
5. 对其余 79 项逐项登记沿用状态及其当前可玩/停用状态；保温杯旧配方/物品效果保持有效。improve_energy 按现状保留为 hidden=true、locked=true 且无 effects 定义的不可完成项目，不将其列为可玩研究或内容意图待查。
6. 使用存档副本验证旧完成、旧单效果、旧部分投入、新队伍、新系统完成和升级后已选旧研究；确认不同 effect 来源独立、重复登录/重载幂等，旧父子链没有断裂。对应行为和正式包目录验证前保留所有配置文件。

## 简短验证命令

在 FH 根运行配置目录 preflight（只校验旧目录解析/引用，不替代存档运行验收）：

~~~powershell
.\gradlew.bat validateResearchCatalog '-PresearchCatalogDir=D:/games/minecraft/Project TWR/TheWinterRescue/config/fhresearches' --no-daemon --console=plain
~~~

TWR 根静态核对数量与筛选关键 ID（PowerShell）：

~~~powershell
(Get-ChildItem config/fhresearches -Filter '*.json').Count
Get-ChildItem config/fhresearches -Filter '*.json' | ForEach-Object BaseName | Where-Object { $_ -in @('alloy_kiln','iron','thermos_T1','thermos_T2','blast_furnace') }
~~~

M0 已证明本次 ID 集合相同；命令不启动游戏。实际权限来源、RecipeManager 与迁移行为需要对应 FH GameTest/正式包存档副本验证。此准备只记录事实与 M6 建议；除 M3 桥接已有测试外，不表示 M4/M5 或 81 项迁移已实施。

## Outcome

81项逐项处置、两项替换映射和验证范围已转入[当前目录文档](../docs/research/legacy-content-disposition.md)。其余79项保留原定义（含两个原停用条目），新耐火砖沿新增路线；没有把目录核对冒称81项人工通关。
