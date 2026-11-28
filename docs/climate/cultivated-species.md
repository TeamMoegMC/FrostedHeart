# 原版木种栽培与深色橡木 2×2 专属生长

- 状态：完整原版树形重写、生产及测试源码编译通过；栽培树木专项36/36通过，含实际64格云杉；未进行本轮客户端目视验收。
- 最后核对：2026-11-20。
- 范围：主世界八种原版树苗的放置接管；内部使用栽培方块，不扫描或替换自然树木。
- 代码锚点：`CultivatedSaplingPlacement.placed` / `originalFor`、`CultivatedOakProfile.parse` / `Profile.giantOnly`、`CultivatedNativeTrunkPlacer`、`CultivatedOakBlockEntity.advance` / `tryGenerate`、`CultivatedGiantTree.height` / `generate`、`CultivatedSpeciesGameTests`。

## 物种和配置

玩家继续使用原版八种树苗/红树胎生苗。服务端 `CultivatedSaplingPlacement.placed` 在未取消的 Forge EntityPlaceEvent（LOWEST 优先级）中，把原版树苗方块替换为对应内部 `frostedheart:cultivated_<物种>_sapling` 方块；多方块放置事件逐位置处理。内部方块不注册 BlockItem，不在创造物品栏新增条目。拆除掉落及创造中键选取均返回对应的原版树苗，红树林返回 `minecraft:mangrove_propagule`，重新放置进度和信用归零。

原版物品正常执行放置、扣除和放置事件，转换代码不重复扣物品，也不取消再手动模拟放置。已取消的事件不转换，Forge 后续取消仍由原有方块快照回滚。普通世界生成、区块加载、命令直接写入方块没有该事件，不会被全局扫描转换。花盆中的盆栽方块不是原版树苗方块，也不在转换表中。

红树胎生苗转换时保留含水状态与黏土种植支持；其他树苗不新增注水入口。内部方块仍有五阶段模型、语言名称、原版物品掉落表和 `data/frostedheart/cultivated_trees/<物种>.json`。共享土壤温度计、光照门槛、恒定基础速度、温度产量、肥料信用、进度满后等产量及 short p/v 存档。详细温度与肥料规则见[栽培橡树](cultivated-oak.md)，云杉示例及2×2记账见[栽培云杉](cultivated-spruce.md)。

| 配置 ID | 原版单棵树形来源 | 高度参照 min/max | 原版 2×2 来源 | 单干回退 |
|---|---|---|---|---|
| oak | minecraft:oak／fancy_oak | 4 / 12 | 无 | 是 |
| spruce | minecraft:spruce | 5 / 13 | mega_spruce / mega_pine | 是 |
| birch | minecraft:birch | 4 / 12 | 无 | 是 |
| jungle | minecraft:jungle_tree_no_vine | 5 / 14 | mega_jungle_tree | 是 |
| acacia | minecraft:acacia | 4 / 12 | 无 | 是 |
| dark_oak | 无单干；原版 dark_oak 只作巨型来源 | 5 / 13 | minecraft:dark_oak | **否** |
| mangrove | minecraft:mangrove／tall_mangrove | 5 / 13 | 无 | 是 |
| cherry | minecraft:cherry | 4 / 12 | 无 | 是 |

这里的高度数值仍是产量参照，不是硬上限。全部沿用理想15～25°C、能长5～35°C和默认光照9。增产肥的额外效果保留20%，不设产量饱和。树冠、根生成器、土壤提供器和装饰器从各自原版 configured feature 读取。

## 完整原版树形与木头期望

已改为保留原版主干、分叉、轴向、全部树冠附件、根与装饰器，不再按候选顺序裁木头、补直柱或删树冠。`CultivatedNativeTrunkPlacer`只固定换算后的尺寸，并实时委托原版生成器。各树种算法、变体概率与校准方法见[树形与产量模型](cultivated-tree-shapes.md)。

预算B是单株主干加枝杈的目标期望，实际数量随原版树形随机变化；2×2总目标为4B+8，保留原版巨树收尖，不再保证逐棵精确配额。低尺寸也直接调用原版分叉；樱花目标预算不足8时继续攒信用，不改成直杆。人工枝杈0.6／0.15成本已被完整原版木头期望模型替代。红树的根不是木头，不计入木头期望。

## 深色橡木：足够产量与2×2缺一不可

深色橡木使用 `Profile.giantOnly=true`，普通单干配置入口返回空，不存在单棵回退。

1. 自身光照等条件正常时推进阶段，进度满但自身预算低于 max_height=13 时保留阶段4，**继续积累产量信用**。温度计显示等待产量；不会因为进度满就卡在低产量，也不会先长成小单干。
2. 自身预算达到13后，按现有规则扫描同种2×2，先跳过未加载位置，挑总已赚信用最高的组合。找不到完整组合时显示等待同种2×2树苗，不消耗已积累信用。
3. 即使凑齐四株，组平均信用换算的基础预算 H 也必须达到13；否则显示等待产量，不生成小型2×2。其他三株仍不额外要求个别成熟、光照或空间，只计各自已赚信用。
4. 达标才生成原版深色橡木巨型树，成功移除四株不掉落，失败恢复四株及信用，根部肥料沿用现有规则。

默认全程20°C，无肥16个有效随机刻攒满，四株各v=256时H=13，2×2总木头目标期望60；三档增产分别v=304/352/416、H=16/19/23、2×2总木头目标期望72/84/100。若全程5°C，无肥16刻时进度已满但v=128、H=5，继续等随机刻积累到v=256后才能满足自身门槛，仍要等待同种2×2及组平均达标。以上是源码推导，不是实测结果。

## 本轮验证边界

新增 `CultivatedSpeciesGameTests` 的四项用例文本：六物种配置/原生形态/巨型资格，深色橡木满进度继续攒信用且孤立不回退，低平均信用及失败四苗恢复，成功深色橡木完整原版分叉及产量波动。已随完整专项运行通过，生产与测试源码编译通过。原专项 tree 包入口自动包含这些测试文本。另新增 `CultivatedSaplingPlacementGameTests` 三项放置用例，覆盖八种真实原版物品放置、扣物品一次、无独立物品注册、原版掉落/中键、取消及非放置写入、多位置放置和含水红树胎生苗。

原版菌类和竹子不是这套树苗配置；本轮未改它们。原生复杂树形的视觉与空间表现仍待实际客户端和游戏测试验收，不将静态资源检查视为游戏验证。

补充 `CultivatedGrowthGameTests` 七项实际生长用例：白桦、丛林、金合欢、红树林、樱花逐种从阶段0推进到实际树木，丛林2×2实际生成，以及三档加速肥在非理想温度下按整个加速进度结算信用。与前述20项一并通过，共27项；日志 `build/cultivated-release-growth-validation.log`。

## 完整原版树形验证（2026-11-20）

`compileJava`、`compileGameTestJava`及专用`runGameTestServer`成功，36/36项通过；日志`build/cultivated-native-final-validation.log`。包括2912组同种子同尺寸逐块原版对照、75组预算×512独立种子的木头均值、完整树冠／根／装饰／蜂巢来源保留、实际生成64格云杉及樱花不足完整分叉预算继续攒信用。保留温度计、p/v存档、随机刻、肥料和2×2既有回归。详见[完整原版树形与验证数据](cultivated-tree-shapes.md)。先前27／28／30项通过记录是旧版本历史，不代表本次重写的视觉验收。
