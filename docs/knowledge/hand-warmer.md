# 阴燃利用

- Status: `Current`
- Last verified: `2026-11-20`
- Scope: 铜制手炉的构想、实验台制作、工作台配方和旧档接续。
- Code anchors: `PlayerTemperatureUpdate.regulateTemperature`、`KnowledgeBehaviorBridge.coldAfterCopper`、`ResearchStages.enter`、`ExperimentTableBlockEntity`、`LegacyResearchBridge`、`CoalHandStove`。

## 发现与开启

在低于 0°C 的地点检视煤炭、木炭或焦炭，再检视铜锭。把两份已学会的物品观察放到绘图台联络，取得「阴燃利用」构想。已学会的干草块观察降低联络难度。开题条件为构想、铜锭和同一寒冷条件下的炭类观察。记录保存检视完成时的现场环境温度，缺温度的旧炭记录需要重新采样。

玩家完成 `create:copper` 进度后也可能在受冷时想到这个构想。服务端在该玩家手部低于 36°C 或核心体温低于 36.5°C 时，每隔 200 游戏刻发出一次 `frostedheart:cold_after_copper` 判定。手部沿用挖掘惩罚的分界，核心体温沿用失温蓝条的分界。蓝条使用平滑后的客户端读数。抽取的基础概率约为每次 0.9955%，按 20 TPS 等效于每秒约 1/1000。队伍已学会的铜锭、黏土、炭类、干草块观察各加 1 个百分点，炭类加成接受任意记录温度。

`the_winter_rescue:research/hand_stove_carbon` 包含可进入手炉燃料加工路线的煤炭、木炭和焦炭。干草块 `kubejs:straw_briquette` 是独立的辅助知识，也是手炉燃料。

## 实验与图纸

新开启的研究先在绘图台完成 0 级折线「校核手炉图纸」，确认定稿后手动进入试制阶段。此时已学会 `#forge:clay` 物品观察的队伍进入省料分支。实验台消耗 4 块铜板和 1 份黏土。普通分支消耗 8 块铜板和 1 份黏土。分支随本次研究保存。

在实验台选择本次研究，放入材料并开工。两个分支都运行 600 tick，按 20 TPS 约 30 秒，成功后从领取槽取出一只空铜制手炉。回绘图台完成研究，获得 `the_winter_rescue:hand_stove_design`。它开放工作台配方 `the_winter_rescue:research/hand_stove`，用 4 块铜板围住 1 份黏土。

干草块、煤炭棒和木炭棒公开制作。把任意一件燃料与手炉放进合成格，每次补充 400 单位燃料，最多容纳 800 单位。拿在主手暖手，拿在副手暖躯干。灰满后，手持手炉对地面长按右键清灰。

## 旧档承接

旧 `hand_warmer` 已完成的队伍承接图纸和研究完成资格。仅取得旧手炉配方效果 `69684c4aa1f4d7a` 的队伍承接图纸。有旧研究部分投入的队伍收到构想，按新版条件开题。炭棒效果退出配方锁定，原始旧进度仍随队伍保存。实验台仅在新的实验成功后交付实物。

正式数据见 [构想](../../../TheWinterRescue/kubejs/data/the_winter_rescue/frostedresearch/knowledge/ideas/hand_warmer.json)、[联络](../../../TheWinterRescue/kubejs/data/the_winter_rescue/frostedresearch/knowledge/links/hand_warmer.json)、[项目](../../../TheWinterRescue/kubejs/data/the_winter_rescue/frostedresearch/knowledge/projects/hand_warmer.json)和[图纸](../../../TheWinterRescue/kubejs/data/the_winter_rescue/frostedresearch/knowledge/results/hand_stove_design.json)。
