# M0 三项目内容卡与运行基线

- Time: `2026-11-12 12:36:00 +08:00`
- Authors: `Codex / OpenAI，主代理；GPT-6 Luna 只读材料与接口调查`
- Status: `in-progress`；本记录为实施准备，不表示三个项目已实现。
- Scope: FH 机制与 TWR 正式内容；关联[开发计划](2026-11-11_23-38-07_research-system-playable-development-plan.md)。

## 证据口径

架构摘要及后续明确决定优先；讨论中的具体材料入口仅作内容细化依据。以下新设备和新任务尚不存在；配方路线是源码证据，未进行生存游玩。旧目录见[81项全量清单](2026-11-12_12-34-00_research-m0-legacy-catalog.md)。

## 共用入口与进度可达性

- TWR `kubejs/server_scripts/src/recipes/shaped/new.js` 的 `frostedresearch:drawing_desk`：木板、木台阶、铜板。`recipes_types/stone_age/tree_stump.js` 可把铜锭用 hammer_dig 工具制铜板，不要求先用金属冲压机。`kubejs/data/charcoal_pit/recipes/ore_kiln_recipes/copper.json` 提供前期铜矿烧炼路线。
- 普通观察键可做必需观察，不依赖放大镜。观察先形成预览/来件，再学习入档；不要求研究用纸才能观察。演算用纸笔与观察不混同；`recipes/shapeless.js` 的 quill_and_ink 使用羽毛、玻璃瓶、墨料；`stone_age/drying_rack.js` 提供纸浆晾纸路线。纸浆与墨料具体早期供应仍需正式包生存验收。
- 成果学习条件不得要求先使用自己的解锁物。三个新成果尚未配置，M3–M5 必须验证这个条件，不能从旧定义推定可达。

## 土制冶炼炉（计划替换 alloy_kiln）

- 入口：获取可由正式合金炉处理的矿物；必要知识无，观察粘土/沙子作为非必要知识。开启知识按 M1 项目配置独立表达，具体 ID 待配置，不把非必要知识自动展开为统一门槛。
- 确认任务：三种不同配比/成分小炉均有制作与试烧路线，两个坏、一个好；提交完好炉；不要求提交坏炉。是否必须三种都烧过才可完成尚未决定。
- 现有材料依据：`recipes/shaped/change.js` 的 alloybrick 是砖与 `#twr:plates/structural`；小炉不得沿用成品炉砖作为原料。建议用前期粘土/沙及可达填料做三组固定配方，具体比例、燃料、时长、交互与损坏回收待 M3 内容定稿；此处不授权任意比例模拟。
- 矿物集合从 `recipes_types/immersiveengineering/alloy.js` 及实际加载配方核对：铜/锡原矿、碎矿与 kiln_dust；黄铜配方用金属锭，不应把石、玻璃原料也统称矿物。不要只写死两件物品，需按有效标签/配方和作者限定集合构造。
- 最终权限必须同时承接 `immersiveengineering:alloybrick` 配方与 `immersiveengineering:multiblocks/alloy_smelter` 成型。旧 `alloy_kiln.json` 已有两种 effect；只替换 recipe 会遗漏成型锁。
- 待验证：小炉前置材料不依赖正式炉；产物/好坏状态持久化；输出与提交只计一次；最后真的能建造并使用正式炉。

## 冶铁炉（计划替换 iron；不是 IE blast_furnace）

- 入口：获取铁矿/铁制品；无必要知识。铜/青铜观察、煤/木炭/焦炭观察为相关知识；组内任选或累计尚待 M2 配置，不自行设成开始硬门槛。
- 确认流程：风道游戏通关→临时开放鼓风口原型配方→风箱首次有效供气转正常鼓风口→观察验证→永久解锁 main_bloomery 与 sandy_tuyere。游戏只看通关，不引入质量评分。
- 现有风箱 `recipes/shaped/change.js`：防腐木、防腐木台阶、皮革。树桩可切防腐木台阶。防腐木的前期来源要和木焦油/杂酚油路线一起验，不能只见配方就断言无循环。
- 正式 main_bloomery 在 `recipes/shaped/new.js`：砂砖、砂浆、青铜板；sandy_tuyere 在 `recipes/shapeless.js`：砂砖、青铜板、砂浆。砂砖坯在 change.js 用沙和粘土制作。原型建议沿用这些可达材料，但具体配方尚未决定。
- `the_winter_rescue:bloom_iron` 包含 raw_iron、黄铁矿、锈铁、失败铁坯等；`kubejs/data/charcoal_pit/recipes/bloomery_recipes/iron.json` 是现有输入集合。最终触发要覆盖实际铁制品，不把输入 tag 当全部触发范围。
- 旧 iron 只解锁 main_bloomery；新成果必须补 sandy_tuyere。临时授权与放弃、重开、原型丢失规则留 M4-A；本阶段不擅自定义存档语义。

## 普通耐火砖（不是 high_refractory_brick）

- 确认任务：砖坯放入控温实验炉；适温好砖、错温碎裂砖；观察本次成功样完成，不提交、不要求故意失败/遍历温度。
- 想法入口：观察耐火砖、观察材料含耐火砖的物品/方块。直接/递归配方关系未决；没有成品砖时的入口尚未确认。建议增加高岭土/铝土矿相关知识入口，但这是建议，不能当现有确认规则。
- 现有早期材料路线：树桩处理铝土矿/高岭土，`stone_age/millstone.js` 磨粉；`create/mixing.js` 用高岭土粉、铝土矿粉、粘土球制 fire_clay_ball。该 mixing 路线是否适合新研究的前期位置需验，不默认已有 Create 设备。
- 更早烧成已有 `kubejs/data/the_winter_rescue/recipes/pottery_recipes/fire_brick.json`：fire_clay_ball→refractory_brick，故不是只能通过 IE 高炉烧成。另有 IE blastfurnace 与工业窑路线；不能混作控温实验炉。
- `recipes/shapeless.js`：砂浆+4 单砖→2 refractory_bricks。新研究最终具体解锁单砖、砖块还是哪些批量配方尚未确认；保留现有路线直到 M6 有明确处置。
- 控温炉自身配方、供热与砖坯配方、温度区间、时长尚未定。建议炉体采用普通砂砖等已可得耐材，避免依赖待研究的耐火砖。新设备与样品身份必须区分既有随手获得的砖。
- 额外知识只影响后续分支，进入后固定，中途补知识只影响未来选择；最终成果相同。

## 运行环境差异（当前现场）

FH `research-261108` / `9d2dab38d02fb49d0c0f1f475836a1ffc682e3d9`；TWR `1.20` / `2d0fefd349f65f3118828c9712f68a8c0782ab75`。

- PATH java 是 17.0.2，但 JAVA_HOME 为 32 位 Java 8；Gradle wrapper 8.1.1 首次失败于 -Xmx6G。用进程级 JAVA_HOME 指向 JDK17；不改持久设置。
- run/mods 为空不代表没有模组：Gradle runtimeClasspath 提供开发依赖。build.gradle 中 Charcoal Pit/Immersive Industry 为 clientRuntimeOnly，GameTest 服务端不涵盖其真实设备行为。
- run/config/fhresearches 有87 JSON，TWR正式目录81；开发侧额外6项：651c5ccef8644e50、818f481cb6054eb5、8ccc0c53a62b4ff6、e5258b4acf3547b4、e88dd5bbf80643ce、f6e48b0034446e0。未复制/删除任何配置。
- run/kubejs/server_scripts 只有 example.js，正式仓有57脚本。开发客户端不能充当正式包内容验收。知识示例数据包也不会自动加载。开发世界仅验界面/机制；正式包需独立新测试世界验证材料、配方与下游生产，不使用玩家原存档。
- 最近交接：2026-11-09 放大镜观察已做历史自动检查；2026-11-11 手持遮罩仅源码核对、当时编译内存失败。本次重新验证，不沿用历史通过数字。

## 第三方接点与需要决定的循环

已由主代理核对 FH `ResearchCommonEvents.onIEMultiBlockForm`→`ResearchHooks.canFormMultiblock`→队伍 `TechnologyAccessResolver` 投影。成型使用 `multiblock.getUniqueName()`；数据目标必须继续保留 `immersiveengineering:multiblocks/alloy_smelter`。

Charcoal Pit 7748097 本地 mapped JAR 的 `javap -p -c charcoalPit.block.BlockBellows` 已核验，输出在 `build/m0-bellows-bytecode.txt`：`blow` 从 FACING 找前方 `charcoal_pit:tuyere_blocks`，找相邻真实 Bloomery，再调用 `TileBloomery.blow(140 / count)`。仅右键/红石启动不等于实际供气。建议在该实际调用处接原型转化，捕获鼓风口坐标，不复制完整 BlockEntity。原型必须加入 tuyere_blocks；当前 TWR tag 仅含 sandy_tuyere。

**未决循环：** 上述有效供气要求真实 Bloomery，但研究完成才永久解锁 Bloomery。已向用户建议 M4 通关后同时临时开放实验炉的制作能力，与原型同生命周期，完成后转永久；备选为原型独立试吹。两者玩法不同，当前均未实施，不能假定默认选项已批准。此点不妨碍 M1 接口设计，但 M4-A 前必须决定。

另查得 TWR `kubejs/data/the_winter_rescue/recipes/barrel_recipes/treated_wood.json`：木板+125mB杂酚油，1200tick产防腐木；无需 Create 搅拌。杂酚油供应及新研究前实际材料获取仍留正式包验收。

### 用户后续确认（2026-11-12，本次对话）

已选择 **原型独立接受风箱试吹**。上段“临时开放实验炉”建议不采用。M4-A 应在 `BlockBellows.blow` 的风箱实际吹气动作中识别前方原型，独立完成转化，不依赖扫描到 Bloomery，也不调用不存在炉体的 TileBloomery。仍需防止客户端回调/重复供气导致重复推进。正常鼓风口继续走原模组供气；无需提前开放正式冶铁炉配方。具体实现与定向测试留 M4，不在 M0 改生产代码。

## M0 已执行的自动基线

2026-11-12：完整 `compileGameTestJava`、生产 `compileJava`（UP-TO-DATE）、正式81项 `validateResearchCatalog`、研究/登录/观察6项必需 Forge GameTest 通过。编译历史阻塞由两份测试源码的小修复解除；没有再排除矿脉文件。日志与精确命令见[本次 diary](../diary/2026-11-12_12-40-00_research-m0-baseline-handoff.md)。不代表矿脉运行测试、新研究机制或正式包生存流程通过。

客户端补充：`runObservationUiClient` 已完成，11条自动回调路由通过（手持/副手/放大镜/掉落物/箱子/背包/创造背包）；4m39s。完成记录截图已查看。仍不把自动回调和完成窗口截图当人工手感/过程遮罩验收。
