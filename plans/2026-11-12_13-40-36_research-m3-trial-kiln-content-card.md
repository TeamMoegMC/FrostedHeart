# M3 土制冶炼炉首版内容卡

- Time: `2026-11-12 13:40:36 +08:00`
- Authors: `Codex / OpenAI GPT-6.1 Sol medium；独立内容准备`
- Status: `completed`；已采用并实施，当前操作由docs中的正式路线维护。
- Scope: 仅 M3 的三组小炉、材料、试烧、提交和正式解锁；不改 M1/M2 框架，不设计 M4/M5。
- Related: [总计划 M3](2026-11-11_23-38-07_research-system-playable-development-plan.md)、[M0 内容卡](2026-11-12_12-36-00_research-m0-content-baseline.md)。

## 首版实施推荐参数

主协调本次已采用 **3:1、1:3、2:2；4 木棍/400 tick；仅提交本次成功样** 作为首版实施默认。其余 ID 和接口安排按下文落地；配比决定好坏是固定玩法结果，不模拟真实陶瓷配方，也不引入温度控制或小游戏。真实生存可达性仍待验证。

| 试样 | 无序合成输入 → 1 座未烧小炉 | 建议方块及同名 BlockItem ID | 试烧结果及提示 |
|---|---|---|---|
| 黏土偏多 | 3 `minecraft:clay_ball` + 1 `minecraft:sand` | `frostedresearch:trial_kiln_clay_rich` | 损坏：“炉壁收缩开裂，试试调整黏土与沙的配比。” |
| 沙偏多 | 1 黏土球 + 3 沙 | `frostedresearch:trial_kiln_sand_rich` | 损坏：“炉壁松散崩解，试试调整黏土与沙的配比。” |
| 均衡配比 | 2 黏土球 + 2 沙 | `frostedresearch:trial_kiln_balanced` | 完好：“炉壁保持完整，可以收取并提交试样。” |

- 三组均使用随身 **2×2** 合成格；不加水桶、砖、砂浆、板材或工具。三组全部试做共需 6 黏土球、6 沙和 12 木棍；只走成功路线需 2 黏土球、2 沙和 4 木棍。
- 每组试烧消耗 **4 `minecraft:stick`，400 tick（20 tick/s 时为 20 秒）**。放下后持至少 4 根木棍右键，服务端一次扣除并开始；不足时提示需求，不扣除。不另要打火石、铁工具、外部炉体或热网。这里的右键生火是新小炉交互推荐，不声称是现有设备行为。
- 三方块共用一套方块/BlockEntity 逻辑及基础模型，按配比和状态换贴图/裂纹；显示未烧、进行中、完好/损坏及剩余时间。燃料固定批次，不增加通用燃料库存或完整冶炼功能。
- 完成后空手右键收取 1 件“完好小炉” `frostedresearch:fired_trial_kiln` 或“破损小炉” `frostedresearch:cracked_trial_kiln`，并移除世界中的小炉；两者首版用普通试样物品承载，不作为正式生产设备。结果不可逆，不退黏土/沙，不让坏样变好样。
- 未烧/进行中小炉可徒手拆除，返还对应未烧小炉；进行中燃料不退、进度清零，重放需重新供燃料。烧完后拆除只掉对应结果试样，与右键收取共用一次性结算。区块卸载/退出保存配比和剩余进度，重新加载继续，不补算离线时间。

## 发现、观察、开始与完成

- **矿物获取触发集合**：建议 TWR 新增物品标签 `the_winter_rescue:research/alloy_kiln_minerals`，包含 `#forge:raw_materials/copper`、`#forge:raw_materials/tin`、`#forge:crushed_raw_materials/copper`、`#forge:crushed_raw_materials/tin`。依据是当前 `recipes_types/immersiveengineering/alloy.js` 的四条矿物配方；每条还消耗 `#kubejs:kiln_dust`。后者是配料，不纳入“矿物获取”触发；石、沙、玻璃原料和黄铜配方的金属锭也不因此纳入。此标签按有效配方的矿物输入维护，不写死两件物品；实施时再核对加载后的原生/数据包配方，补齐新增的合格矿物输入。
- 建议定义 ID：想法 `the_winter_rescue:earthen_smelting`；项目 `the_winter_rescue:earthen_smelter`；配方成果 `the_winter_rescue:earthen_smelter_design`（`ResearchResult.Design`）；成型成果 `the_winter_rescue:earthen_smelter_construction`（`ResearchResult.Construction`）。若 M2 已选同义 ID 就复用，不另造重复想法。观察没有静态知识定义 ID，记录身份为 UUID；项目 M1 `access.conditions` 使用局部条件名 `idea`、`clay_material`、`sand_material`，后两项按 `kind=observation`、`observation_type=item` 和 `object` 分别匹配 `minecraft:clay_ball`、`minecraft:sand`。
- 想法沿 M2 的矿物获取入口；**必要想法知识为空**，黏土与沙观察作为非必要知识，不阻止首次发现/联络。概率和联络细节复用 M2，不在 M3 再造规则。
- 开始条件明确为三项全部掌握：**想法本身＋黏土球观察＋沙观察**；`access.start_all=[idea, clay_material, sand_material]`，其中 `idea` 按定义 ID 匹配想法，观察按上述物品对象匹配有效档案。普通观察键足够，不要求放大镜；观察后通过现有绘图台学习，采样与入档分开表达。项目披露/想法入口复用 M1/M2。
- **三种小炉配方首版直接可制作**，丢失可重做，不依赖 M4 临时配方引擎，不添加前置演算或额外阶段。研究进行中开始试烧时绑定当前队伍及研究 `run`，产出的成功样携带该归属供提交判定；研究开始前制作的小炉可用于本次试烧，但未绑定当前 `run` 的既有成功样不能提交。
- **最小默认：不强制三组均试烧，只要求提交 1 件本次研究产出的完好小炉并消耗它。** 三组均有完整可玩路线和确定反馈；坏炉不提交。这样只需成功试样任务与原子提交，不需“三种已烧”位集或额外阶段；若改为三组必做，则需多记录三个配比的试烧事件和提示，材料/燃料总量随之上升。
- 成功试样沿用当前研究任务接点记录队伍及研究记录归属，防止同一件样品/同一次收取重复计数；提交前展示消耗，服务端扣除与完成同次结算。成果学习不再要求使用尚未解锁的正式炉。
- 项目完成后关联两个不同类型的最终成果：**`earthen_smelter_design` 的 `ResearchResult.Design.recipes` 解锁 alloybrick 的实际配方 ID**；**`earthen_smelter_construction` 的 `ResearchResult.Construction.multiblocks` 解锁 `immersiveengineering:multiblocks/alloy_smelter` 成型权限**。前者以产物 `immersiveengineering:alloybrick` 定位实际配方 ID，不能把物品 ID 默认当配方 ID；不在单个 Design 中塞成型权限。保留原料配方本身。旧 `alloy_kiln` 已完成者映射到这两个成果；具体迁移接 M3/M6，避免两次研究或两个来源互相残留锁定。

## 正常材料路线与源码依据

一条源码支持的原料路线：在正式世界寻找冻黏土/沙与腐木 → 营火解冻冻黏土得到黏土球 → 腐木合成木棍 → 2×2 做任一配比 → 放置、供木棍试烧 → 收取好样并在研究页提交。小炉链本身不要求冶炼或铁工具。

- TWR `kubejs/data/the_winter_rescue/forge/biome_modifier/clay_patch.json` 将冻黏土斑块加入指定群系；`worldgen/configured_feature/clay_permafrost_patch.json` 使用 `frostedheart:clay_permafrost`。FH `FHBlocks.CLAY_PERMAFROST` 继承黏土属性，生成掉落表 `src/generated/resources/data/frostedheart/loot_tables/blocks/clay_permafrost.json` 直接掉冻黏土，不要求精准采集。
- TWR `kubejs/data/frostedheart/recipes/defrost/melt_clay.json`：1 冻黏土经 `frostedheart:defrost_campfire`，200 tick → 4 黏土球。`recipes/shapeless.js` 另有 1 普通黏土块 → 4 黏土球，以及 2 `frostedheart:rotten_wood` → 2 木棍；FH 腐木方块掉落表给 4 腐木。`recipes/shaped/new.js` 的营火由 2 稻草 + 2 原木制作；`recipes/shaped/change.js` 已使用沙与黏土球制作砂砖坯。沙生成可追 `kubejs/data/minecraft/worldgen/placed_feature/disk_sand.json`。
- 绘图台仍是既有共用入口：`recipes/shaped/new.js` 用木板、台阶和铜板；`recipes_types/stone_age/tree_stump.js` 可用 `hammer_dig` 将铜锭制铜板，`kubejs/data/charcoal_pit/recipes/ore_kiln_recipes/copper.json` 提供铜矿烧铜路线，不用正式合金炉。
- **未实测边界**：群系生成文件/配方存在不等于玩家已在出生地获得材料；营火点燃、冻黏土采集手感、沙/腐木供应、早期 `hammer_dig` 工具及铜矿窑坑实际获取均需正式包新世界核验。正式 alloybrick 仍消耗砖和 `#twr:plates/structural`，IE 工程锤仍消耗结构金属锭、木杆和高级线材；结构金属标签包括青铜而非铜，不能把“已有铜板”当正式炉已可造。M3 游玩需另验既有青铜/结构金属、砖和工程锤路线，不能用创造材料替代。

## TWR 实施文件清单

以下是建议新增/调整位置，按 M2 实际 ID 合并；本次没有修改它们。

- 新增 `kubejs/server_scripts/src/recipes/research_trial_kiln.js`：三条直接可制作的 4 格无序配方，不加临时配方锁。
- 新增 `kubejs/data/the_winter_rescue/tags/items/research/alloy_kiln_minerals.json`：上述矿物集合。
- 在 `kubejs/data/the_winter_rescue/frostedresearch/knowledge/` 下增加/复用 `ideas/earthen_smelting.json`、`projects/earthen_smelter.json`、`results/earthen_smelter_design.json`、`results/earthen_smelter_construction.json`。项目 `access` 以局部条件匹配想法和两种物品观察；不新增静态观察定义文件，不复制核心观察实现。
- 调整 `config/fhresearches/alloy_kiln.json` 的旧研究处置与双重权限来源；同步核对 `kubejs/server_scripts/src/recipes/shaped/change.js` 中 alloybrick 的研究锁接法，保留原配方材料与产量，不把小炉设为正式炉砖原料。
- 更新 `kubejs/assets/twr_researches/lang/zh_cn.json`、`en_us.json` 旧 alloy_kiln 文案，并在 TWR 对应语言资源中添加新项目提示；模组注册物品/方块的中英文名称、模型和贴图由 FH 实施。此次检索 `config/ftbquests/` 未命中 `alloy_kiln/alloybrick/alloy_smelter`，不编造任务文件路径；实施时按实际任务 ID/展示文案补引导。

## 交接结果

主协调补充源码证据（2026-11-12）：`kubejs/server_scripts/src/recipes_types/charcoal_pit/pottery.js` 已有三份 copperLike 加一份 tinLike 在 orekiln 产出4青铜锭的配方，输入允许铜/锡原矿和碎矿，故青铜并非必须先用正式合金炉获得。青铜锭经 tree_stump 配方压板；工程锤使用结构金属锭、木杆、advanced_string，后者在 item_tags.js 中允许原版线、皮条或铜线。这补全了源码层的候选材料路线，窑坑及工具实际获取仍需正式包游玩验证。

本次实施默认已采用 **3:1、1:3、2:2；4 木棍/400 tick；仅本次成功样必交**。后续实施需覆盖三组试烧、拆除/收取一次性结算、保存重载、当前 `run` 绑定与提交消耗，以及 Design 配方和 Construction 成型两个成果。本次仅新增并修订此计划，未改生产/TWR 数据或其他文档，未构建、启动客户端或执行 Git 操作。

## Outcome

M3-A/B机制与正式内容已完成，正式客户端已通过实物提交和权限变化。当前材料与操作见[土制冶炼炉](../docs/knowledge/earthen-smelter.md)，本卡保留设计时记录，不作为未实施任务重复执行。
