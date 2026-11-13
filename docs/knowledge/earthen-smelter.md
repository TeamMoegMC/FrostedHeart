# 土制冶炼炉：正式整合包路线

- Status: `Verified`：正式包加载、实物提交、成果权限及旧入口已验证；材料使用测试夹具，不表示完整生存通关。
- Last verified: `2026-11-13`（正式联络改为物品观察组合；此前实物实验的客户端证据见文末）
- Scope: TWR `earthen_smelting` 想法、`earthen_smelter` 项目、两项成果及小炉提交路线。
- Code/data anchors: [小炉与实物提交](trial-kilns-and-samples.md)、[正式项目](../../../TheWinterRescue/kubejs/data/the_winter_rescue/frostedresearch/knowledge/projects/earthen_smelter.json)、[正式想法](../../../TheWinterRescue/kubejs/data/the_winter_rescue/frostedresearch/knowledge/ideas/earthen_smelting.json)、[合金配方](../../../TheWinterRescue/kubejs/server_scripts/src/recipes_types/immersiveengineering/alloy.js)。

## 玩家操作

1. **准备并开始研究。** 获取铜/锡原矿或碎矿可产生“土制冶炼炉的构想”。也可先观察黏土球和沙，保留记录并在绘图台学习入档；在「档案」把这两份物品观察放到桌面，点击联络，再选择构想并等待。构想正常自动入档，已取得的构想不再列为候选。研究页要求想法、黏土球观察、沙观察三者都已掌握；只有条目确实留在来件中尚未学会时，才需手动学习它。绘图台仍按整合包原有木板、台阶、铜板配方制作。
2. **在本次研究中试烧。** 随身 2×2 合成黏土球与沙：3:1 为黏土偏多小炉，1:3 为沙偏多小炉，2:2 为均衡小炉。三条配方直接可制作。开始研究后放置小炉，主手持至少 4 根木棍右键，消耗 4 根，烧制 400 个加载中的游戏 tick（20 TPS 时 20 秒）。均衡炉完好，另两种损坏；只交一件本次成功样，不必故意试坏。无进行中研究时也能烧，但未绑定的旧样不能用于之后的研究。若多个研究目标同时开放，先空手右键选择，再供燃料。
3. **交样并完成研究。** 完成后空手右键收取完好小炉，放进绘图台“抄录台”的物件槽（源码 `EXAMINE_SLOT`；界面提示也称检视物槽），回研究页点提交，消耗一件。无需检视或学习这个实物，不使用实验台。随后完成研究，“土制炉砖设计”和“土制冶炼炉建造”正常自动入档，立即分别取得炉砖配方和正式炉成型权限，无需再找成果来件；只有成果确实留在来件中未学时才手动学习。坏样、其他队伍样品和旧研究 run 的样品不能顶替。

## 材料与后续生产

小炉仅用黏土球、沙和木棍，不需要正式炉砖、金属板、打火石或铁工具。TWR `recipes/defrost/melt_clay.json` 定义冻黏土在营火上 200 tick 变成 4 黏土球；`recipes/shapeless.js` 定义普通黏土块拆 4 球、2 腐木变 2 木棍。营火由 `recipes/shaped/new.js` 的稻草和原木配方制作。沙与冻黏土的世界供应、营火点燃和采集过程仍需实际生存确认。

正式土制炉砖保持原材料与产量：4 砖 + 4 `#twr:plates/structural` → 4 `immersiveengineering:alloybrick`。配方 ID 固定为 `the_winter_rescue:research/alloybrick`。工程锤使用 2 结构金属锭、2 木杆和高级线材；结构金属包含青铜，铜不在该结构金属集合中。现有 `recipes_types/charcoal_pit/pottery.js` 可用三份铜类材料和一份锡类材料，经窑坑产 4 青铜锭；`recipes_types/stone_age/tree_stump.js` 可用 `hammer_dig` 将青铜锭制青铜板，铜板同理。因此源码有不依赖正式合金炉的铜板/青铜路线，但砖、采矿工具、窑坑供热及整条后续生产尚未实测。

## 定义与权限边界

- 正式定义位于 TWR `kubejs/data/the_winter_rescue/frostedresearch/knowledge/`，随 KubeJS 数据加载；不依赖手动装知识示例包。
- `the_winter_rescue:research/alloy_kiln_minerals` 标签收录当前合金脚本的四种矿物输入标签：铜/锡 `raw_materials` 和 `crushed_raw_materials`。`kiln_dust` 是配料；石、沙、玻璃原料和黄铜配方中的金属锭不作为本项目矿物获取触发。正式客户端已核对实际 RecipeManager 中唯一的炉砖配方及其材料、产量。
- 想法 `acquisition.required=[]`，行为获取不强制先有黏土球/沙观察；它们仍是 acquisition.optional，增加行为概率并缩短联络时间。主动联络另由 LinkRule.inputs 要求这两份观察，不再有本项目的零输入入口。未指定 `chance`，源码默认 `knowledgeBehaviorChance=0.15`，每项已掌握 optional 加 `knowledgeBehaviorOptionalBonus=0.1`，上限 1。联络基准 200 tick，每项 optional 减20 tick，最少20 tick；此组合默认160 tick（20TPS时8秒），以服务器配置和界面计时为准。
- `access.start_all` 引用局部条件 `idea`、`clay_material`、`sand_material`；观察按 `item` 的 `object` 匹配，不存在固定观察定义 ID。
- `earthen_smelter_design` 为 Design，只解锁稳定 recipe ID；`earthen_smelter_construction` 为 Construction，只解锁 `immersiveengineering:multiblocks/alloy_smelter`。本项目的想法和两成果均为 `understanding=[]`，行为/联络和完成研究通过 `produce` 正常自动学入档；不会要求先使用尚未解锁的设备，也不额外要求手动学习一次成果。
- 项目声明 `legacy_replacement` 对应旧 `alloy_kiln` 和原两项 effect nonce；旧 JSON 保留。旧完成承接两成果，旧部分投入只授予想法，原投入留存；映射撤除可继续旧路线。详见[状态与 API](state-and-api.md)及[桥接验证记录](../../diary/2026-11-12_15-29-57_research-m3b-legacy-bridge-handoff.md)。

正式客户端已验证正常槽位提交消耗一件、完成前后配方和成型权限变化、旧入口承接；见[M3客户端记录](../../diary/2026-11-12_16-58-51_research-m3c-client-prism-handoff.md)。成型验证调用真实IE事件，不等于手摆多方块挥锤。世界、材料及初始知识使用夹具，完整采集和下游生产仍未实玩；不以这些记录代替生存体验。
