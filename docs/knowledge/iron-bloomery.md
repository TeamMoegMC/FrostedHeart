# 冶铁炉正式内容与操作路线

- Status: Verified：正式包单客户端及双客户端同队流程已验证；世界、材料和起始知识使用夹具，完整生存获取仍未实玩。
- Last verified: 2026-11-12
- Scope: iron_smelting 想法、iron_bloomery 两阶段项目、单 Design 双配方成果、铁材料触发标签和旧 iron 映射。
- Anchors: [M4 采用决定](../../plans/2026-11-12_15-45-00_research-m4-stage-tuyere-interface-decisions.md)、[正式项目](../../../TheWinterRescue/kubejs/data/the_winter_rescue/frostedresearch/knowledge/projects/iron_bloomery.json)、[正式配方脚本](../../../TheWinterRescue/kubejs/server_scripts/src/recipes/shaped/new.js)、[风口脚本](../../../TheWinterRescue/kubejs/server_scripts/src/recipes/shapeless.js)。

## 玩家路线

1. 获取铁矿或常见铁制品可产生构想，也可使用绘图台中本想法的零输入联络入口。构想正常自动入档。普通观察键观察铜锭或青铜锭中的任一种，再观察煤、木炭或焦炭中的任一种；保留记录并手动学习两项观察，满足三项开始条件后开始研究。观察是匹配档案物品对象，不存在固定观察知识 ID。
2. 在绘图台连通风道，免纸笔、无评分。通关立即临时开放原型配方，点击进入试吹阶段。原型用1砂砖方块、1青铜板、1砂浆制作；阶段开始后放在风箱出风侧前方一格，使其绑定本队本次任务。若提前放置，进入阶段后重新放置。普通风箱仍按原公开配方制作：4防腐木、2防腐木台阶、3皮革；没有新增研究锁。
3. 操作风箱并等待实际吹气，原型转为真实砂质鼓风口；不用预先建造冶铁炉。绑定队伍任意当前成员现场完成该方块的新鲜观察即可完成任务，不要求观察者是放置者，不要求把观察保留/入档。手持风口、旧笔记或别处普通风口不算。试件丢失可重制、重放、重吹，不重玩已经通过的风道。
4. 手动完成研究，单 Design 正常自动入档，永久开放主冶铁炉和砂质鼓风口配方；只有确实留在来件未学时才手动学习。原型配方到本 run 结束即撤回；已造物品不删除，但旧试件不顶替新 run。

## 正式数据与权限

知识定义位于 TWR kubejs/data/the_winter_rescue/frostedresearch/knowledge/。想法为 the_winter_rescue:iron_smelting，项目为 iron_bloomery，结果为 iron_bloomery_design。想法 acquisition.required=[]；两个 optional 是合并标签 iron_copper_or_bronze、iron_carbon_fuel 的物品观察。项目 start_all=[idea,copper_or_bronze,carbon_fuel]，两组之间 AND、各组标签成员之间 OR。未指定 chance，沿用 M2 服务器概率/optional 加成和联络时长配置。

airflow 阶段的 completion_recipes 只有 the_winter_rescue:research/prototype_tuyere；专用配方产出 frostedresearch:prototype_tuyere，原型必须由 FH 临时权限管理，不能因尚无人开始项目而默认为公开。trial 的 observe_tuyere 是 observation 任务，experiment=frostedresearch:tuyere_trial、team_current_run=true、对象为 block charcoal_pit:sandy_tuyere。数据未增加个人 owner 条件或额外阶段/变体。

最终 Design 的两条稳定 ID：

| 配方 ID（the_winter_rescue:research/） | 保留的生产材料/数量 |
|---|---|
| main_bloomery | 4砂浆 + 4青铜板 + 1砂砖方块 → 1主冶铁炉 |
| sandy_tuyere | 1砂砖方块 + 1青铜板 + 1砂浆 → 1正常风口 |

new.js 只针对主冶铁炉选择稳定 ID，其他数组条目继续原索引。shapeless.js 通过 helper 的既有 id 参数固定风口 ID，其他索引不变；按产物清理正常风口全部原模组/conditional 路线，覆盖 sandy_tuyere 和 sandy_tuyere2，不仅移除一个旧 ID。主冶铁炉同样清理同产物旧路线。正式客户端已核对实际 RecipeManager 中原型、正式风口和主冶铁炉的唯一配方及权限。

旧 config/fhresearches/iron.json 保留。legacy_replacement 将 c520be3259914225 映射单 Design；旧 finished 或已领取该效果者获得双配方结果，只有 finished 获得 completed_credit。桥接、重复初始化、权限并集与独立进程重启见[M4服务端记录](../../diary/2026-11-12_17-27-52_research-m4-full-stage-tuyere-handoff.md)。

## 触发覆盖与材料证据

iron_materials 标签显式聚合 forge:ores/iron、raw_materials/iron、crushed_raw_materials/iron、ingots/iron、nuggets/iron、storage_blocks/iron，以及现有 the_winter_rescue:bloom_iron、twr:rusted_iron。后两者覆盖黄铁矿、铁坯、锈铁和铁质遗物；另外列入原版铁工具/护甲、桶、剪刀、炼药锅、漏斗、铁轨、锁链、铁栏杆、铁门及铁活板门。范围是这些标签和明确列出的常见制品，不递归扫描所有含铁配方，也不把沙/玻璃当矿物。以后新增材料按此文件维护，实际标签成员由集成检查。

当前正式包已有窑坑铜锡制青铜、树桩 hammer_dig 制青铜板、沙/黏土制砂砖坯、砂浆合成砂砖方块等源码路线。防腐木已有 barrel 配方，风箱不使用铁；但杂酚油、皮革、早期工具、砖烧成及所有实际获取均需正式包生存核验。本次不以这些配方存在宣称材料闭环已经实玩通过。

## M4 风箱前置材料补核（源码静态证据，游玩待验）

无铁的短路线可用铜/锡窑坑配方一批3铜类+1锡类→4青铜锭；树桩配方每1青铜锭经 hammer_dig 三次 choptime 得1青铜板。stone_hammer 由石料、木杆和线制作，FH HammerItem 明确执行 hammer_dig；TWR 的 minecraft:bucket 配方只要3块 #twr:plates/structural，结构金属集合含青铜，所以一批青铜锭足够做一个空桶。Charcoal Pit 原生 sandy_collector 合成用铁桶；TWR change.js 的同产物替换改为接受 frostedheart:ceramic_bucket 或 minecraft:bucket，陶桶可用黏土制。Ponder 显示 sandy_collector 收集燃烧 log_pile 后的杂酚油，并用 immersiveengineering:creosote_bucket 取液。就已核对配方而言，桶不要求铁或冶铁炉。

处理链为 log_pile（9原木）+charcoal_pit:fire_starter（2木棍+1稻草）；collector 周围按 Ponder 用4个固体方块并在上方放木堆。TWR 收集器图案为 `A A / ABA`，需要4个 sandy_brick_item 和一个陶桶或普通桶。barrel 配方每批1木板+125mB杂酚油，1200tick→1 immersiveengineering:treated_wood_horizontal。TWR 风箱图案 `ABA / CCC / ABA` 需要4防腐木+2防腐木台阶+3皮革；树桩用 axe_dig 两次 choptime 将1防腐木切成2个水平台阶，因此至少需5个防腐木、625mB杂酚油。Ponder 展示用 immersiveengineering:creosote_bucket 从 collector 取液；正式Charcoal Pit 3.12.0依赖的BlockBarrel.use检查桶的FLUID_HANDLER_ITEM能力，并经FluidUtil.interactWithFluidHandler灌入桶槽，配方读取该槽。因此源码支持手持杂酚油桶右键barrel灌液；这个结论仍不是实际生存加工记录。

砂砖烧成有原生 Charcoal Pit 陶窑路线，不依赖 IE blast furnace 或 Car Kiln：依赖 JAR 7748097 的 pottery_recipes/sandy_bricks.json 把 unfired_sandy_brick 烧成 sandy_brick_item（exp=0.3）。原生坯料配方被 TWR change.js 按同产物移除，正式保留的是5沙+4黏土球→4未烧砖；不能用原生1沙的数量计算正式材料。remove.js 移除的 charcoal_pit:sandy_bricks 是普通组块配方，不会移除陶窑烧制。TWR 组块配方为1砂浆+4 sandy_brick_item→2 charcoal_pit:sandy_brick。两批正式坯料共需10沙+8黏土球，烧成8单砖：4给collector，4加砂浆合成两个砂砖方块，其中一个用于原型，另需原型配方自身的1青铜板和1砂浆。IE blast furnace/Car Kiln 是替代烧成路径。Coke Oven 即使旧目录 parents 为空，实体仍需焦炉砖和工程锤；TWR 焦炉砖也使用已烧砂砖、砂浆及结构金属板，不能据无父项推断材料零门槛。

主协调已将现有配方引用的 #forge:plates/iron、#forge:rods/iron、#forge:dusts/iron 补入 iron_materials.json，覆盖铁板、铁杆和铁粉；实际解析出的标签成员仍应集成时核对。

## 客户端验证范围

[单客户端](../../diary/2026-11-12_17-35-19_research-m4-client-fixed-pack-handoff.md)已通过风道、临时配方、原型放置、10tick风箱通气、现场新鲜观察和最终权限；[双客户端](../../diary/2026-11-12_19-04-00_research-m7-two-client-validation.md)进一步验证队友真实2×2合成及放置后离线、另一人继续、重连同步与重复操作不重复发奖。后一次固定M5产物的截图已显示完整管线和进出口箭头。这些操作使用实际客户端与网络端点，但材料和起始知识来自夹具，不表示原料获取、全套生存加工或物理键鼠手感已验收。
