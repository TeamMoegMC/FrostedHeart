# 土制冶炼炉：正式整合包路线

- Status: `Verified`：三配比观察的服务端和正式客户端定向检查通过；使用准备好的材料，不代表完整生存通关。
- Last verified: `2026-11-28`（开题条件按正式包数据核对。客户端试烧见既有验证记录）
- Scope: TWR `earthen_smelting` 想法、`earthen_smelter` 项目、两项成果及小炉比较观察路线。
- Code/data anchors: [小炉与实物提交](trial-kilns-and-samples.md)、[正式项目](../../../TheWinterRescue/kubejs/data/the_winter_rescue/frostedresearch/knowledge/projects/earthen_smelter.json)、[正式想法](../../../TheWinterRescue/kubejs/data/the_winter_rescue/frostedresearch/knowledge/ideas/earthen_smelting.json)、[合金配方](../../../TheWinterRescue/kubejs/server_scripts/src/recipes_types/immersiveengineering/alloy.js)。

## 玩家操作

1. **准备并开始研究。** 获取铜/锡原矿或碎矿可产生“土制冶炼炉的构想”。也可先观察粗铜并学会入档，把这份物品观察放到桌面联络，在绘图台通用页面完成连连看。黏土球和沙观察每缺一项增加一级难度，全缺为2级，齐全为0级。沙可观察世界中的方块或手里的物品，任一形式入档即可。辅助记录只需学会，摆到桌面也可以正常联络。掌握构想后即可开题。
2. **拼出炉壁截面。** 在绘图台用零件铺满凹槽，可以剩件。确认定稿后，队伍在本次研究期间取得三种未烧小炉的配方。点击进入下一阶段。
3. **在本次研究中试烧。** 随身 2×2 合成黏土球与沙：3:1 为黏土偏多小炉，1:3 为沙偏多小炉，2:2 为均衡小炉。进入试烧阶段后放置小炉，主手持至少 4 根木棍右键，消耗 4 根，烧制 400 个加载中的游戏 tick（20 TPS 时 20 秒）。均衡炉完好，另两种损坏；三种配比都需要试烧并观察结果。无进行中研究时也能烧，但未绑定的旧样不能用于之后的研究。若多个研究目标同时开放，先空手右键选择，再供燃料。
4. **比较样品并完成研究。** 烧完后空手右键收取三份样品，按物品提示中的配比区分两种坏样和一种好样，再逐一观察物品。观察会直接完成对应任务，不消耗样品，不必保留或学习入档；也不使用实验台。重复观察同一配比不能代替另一配比。随后完成研究，“土制炉砖设计”和“土制冶炼炉建造”正常自动入档，立即分别取得炉砖配方和正式炉成型权限，无需再找成果来件；只有成果确实留在来件中未学时才手动学习。未烧、未关联、其他队伍或旧研究run的样品不能顶替本次样品；丢失可在当前任务重烧。

## 材料与后续生产

小炉仅用黏土球、沙和木棍，不需要正式炉砖、金属板、打火石或铁工具。TWR `recipes/defrost/melt_clay.json` 定义冻黏土在营火上 200 tick 变成 4 黏土球；`recipes/shapeless.js` 定义普通黏土块拆 4 球、2 腐木变 2 木棍。营火由 `recipes/shaped/new.js` 的稻草和原木配方制作。沙与冻黏土的世界供应、营火点燃和采集过程仍需实际生存确认。

正式土制炉砖保持原材料与产量：4 砖 + 4 `#twr:plates/structural` → 4 `immersiveengineering:alloybrick`。配方 ID 固定为 `the_winter_rescue:research/alloybrick`。工程锤使用 2 结构金属锭、2 木杆和高级线材；结构金属包含青铜，铜不在该结构金属集合中。现有 `recipes_types/charcoal_pit/pottery.js` 可用三份铜类材料和一份锡类材料，经窑坑产 4 青铜锭；`recipes_types/stone_age/tree_stump.js` 可用 `hammer_dig` 将青铜锭制青铜板，铜板同理。因此源码有不依赖正式合金炉的铜板/青铜路线，但砖、采矿工具、窑坑供热及整条后续生产尚未实测。

## 定义与权限边界

- 正式定义位于 TWR `kubejs/data/the_winter_rescue/frostedresearch/knowledge/`，随 KubeJS 数据加载；不依赖手动装知识示例包。
- `the_winter_rescue:research/alloy_kiln_minerals` 标签收录当前合金脚本的四种矿物输入标签：铜/锡 `raw_materials` 和 `crushed_raw_materials`。`kiln_dust` 是配料；石、沙、玻璃原料和黄铜配方中的金属锭不作为本项目矿物获取触发。正式客户端已核对实际 RecipeManager 中唯一的炉砖配方及其材料、产量。
- 想法 `acquisition.required=[]`，行为获取不强制先有黏土球/沙观察；它们仍是 acquisition.optional，增加行为概率并降低联络连连看难度。主动联络另由 LinkRule.inputs 要求粗铜（minecraft:raw_copper）物品观察，显式 required_knowledge=[]，optional_knowledge 为黏土与沙；不再有本项目的零输入入口。未指定 `chance`，源码默认 `knowledgeBehaviorChance=0.15`，每项已掌握 optional 加 `knowledgeBehaviorOptionalBonus=0.1`，上限 1。联络不计时；缺0/1/2项辅助时，需要完成0/1/2数字的连连看，通关立即结算。
- `access.start_all` 只引用 `idea`。联络中的粗铜和黏土球匹配物品观察的 `object`，沙通过 `block_object` 匹配 `minecraft:sand`，用于想法加成和联络减难。
- `earthen_smelter_design` 为 Design，只解锁稳定 recipe ID；`earthen_smelter_construction` 为 Construction，只解锁 `immersiveengineering:multiblocks/alloy_smelter`。本项目的想法和两成果均为 `understanding=[]`，行为/联络和完成研究通过 `produce` 正常自动学入档；不会要求先使用尚未解锁的设备，也不额外要求手动学习一次成果。
- 项目声明 `legacy_replacement` 对应旧 `alloy_kiln` 和原两项 effect nonce；旧 JSON 保留。旧完成承接两成果，旧部分投入只授予想法，原投入留存；映射撤除可继续旧路线。详见[状态与 API](state-and-api.md)及[桥接验证记录](../../diary/2026-11-12_15-29-57_research-m3b-legacy-bridge-handoff.md)。

旧版正式客户端曾验证槽位提交消耗一件、完成前后配方和成型权限变化、旧入口承接；这不能代替当前三观察路线的客户端验证。见[M3客户端记录](../../diary/2026-11-12_16-58-51_research-m3c-client-prism-handoff.md)。成型验证调用真实IE事件，不等于手摆多方块挥锤。世界、材料及初始知识使用夹具，完整采集和下游生产仍未实玩；不以这些记录代替生存体验。

## 观察任务与升级

正式项目先在 `drawing` 完成 `kiln_wall_drawing`（`frostedresearch:nesting_fill`），再进入 `main`。试烧阶段要求观察黏土偏多、沙偏多和均衡配比的烧后物品。阶段`method`说明供燃料和收样，三项`tasks[].method`各给出对应【未烧小炉】的配比；这些准备说明不增加任务数量。实验 ID 分别为 `frostedresearch:trial_kiln/clay_rich`、`sand_rich`、`balanced`（后两项同前缀）；条件同时检查结果物品与 `item.trial_kiln_variant`。两种坏样物品 ID 相同，但配比字段不同。

均衡配比沿用任务 ID `submit_intact` 以承接旧进度，任务类型已经改成 observation。旧单阶段队伍继续在 `main` 试烧，并取得图纸阶段的临时配方。`TrialKilnSamples.reconcile` 继续承接早期提交记录：旧未完成main会补齐两项比较任务，重新计算就绪状态；已提交的完好样保留对应任务进度。已有三观察进度直接沿用相同阶段和任务ID。已完成研究、成果和权限不重置。不带配比记录的旧坏样无法分辨配比，需要重新试烧。

## 当前验收

早期三配比版本的45项服务端回归全部通过；补充旧schema4试样回执承接后，6项小炉测试再次通过。正式Playtest客户端通过实际计时的手持观察C2S，完成三种配比任务，保留全部三件样品，并验证完成前后真实配方与IE成型事件权限。人工核对任务界面中的三项完成显示。使用客户端按钮绑定回调、准备好的材料和新建测试世界；不是物理鼠标键盘或完整生存通关。详细证据见[本轮开发记录](../../diary/2026-11-16_13-46-10_research-template-and-three-kiln-observations.md)。
