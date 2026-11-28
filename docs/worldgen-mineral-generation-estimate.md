# 整合包矿物生成量粗估

- 状态：已核对配置的静态估算；不是实测矿石方块数量。
- 最后核对：2026-10-21。
- 作者：OpenAI Codex；配置调查与计算。
- 范围：TheWinterRescue 当前 normal 世界预设的 45 个主世界群系、5 个下界群系，以及矿物相关的圆盘、晶洞和复合矿床。末地没有普通矿石生成，未另列空表。
- 代码／数据锚点：`worldgen/biome/*`、`worldgen/placed_feature/*`、`worldgen/configured_feature/*`、`forge/biome_modifier/*`、`#minecraft:is_overworld`、`defaultconfigs/immersiveengineering-server.toml` 的 `ores.*`、`FHFeatures.DISK`、`ore_veins_enabled`。

## 计算口径

**粗估指标 Q = Σ（每区块平均尝试次数 N × 该档矿脉规模参数 S）**。不同大小的矿脉先分别相乘，再相加；结果用作相对生成预算，不应读成每区块实际能挖到的矿石块数。

`N` 的单位为次／区块，`S` 为配置中的 `size` 或 IE 的 `vein_size`。`size` 是生成算法的规模控制量，不是保证放置的方块数；Q 也不是严格的方块数上限。计数和稀有度取期望，例如均匀 0～1 次取 0.5 次，1/9 概率取 1/9 次。

本表不扣除越界高度、地形空气、可替换岩石不足、空气暴露丢弃、群系边界过滤和矿脉重叠。尤其铁矿高层档、绿宝石及钻石的部分采样高度可能不在可生成地形内，直接相乘会高估。这里严格沿用用户要求的简单乘法，不额外加入没有实测依据的折扣。

基准使用整合包仓库的默认配置；已有存档自己的 serverconfig 可能不同。模组内置数据读取自本地开发实例：Minecraft 1.20.1、Create 0.5.1.j、ImmersiveEngineering 10.1.0-171、Tetra 6.7.0；整合包数据优先覆盖内置同名数据。

## 普通矿脉与矿团：按 Q 从高到低

同一矿物只在生成配方不同的群系之间拆行。岩盐／菱镁矿、石灰岩／方解石是共享替换分支，独立列为混合矿脉，避免把一份预算重复计入两种矿物。表中小数保留四位。

| 矿物／矿团 | 维度 | 群系 | 分档计算 N×S | 粗估指标 Q |
|---|---|---|---|---:|
| 黏土矿团 | 主世界 | 繁茂洞穴 | 46×33 | **1518** |
| 煤矿 | 主世界 | 化石矿床 | 20×17＋30×17＋12×40 | **1330** |
| 铁矿 | 主世界 | 火成矿床 | 10×9＋10×4＋90×9＋8×26 | **1148** |
| 铁矿 | 主世界 | 除冰盖、火成矿床、潮水冰川、山谷冰川外的全部群系 | 10×9＋10×4＋90×9 | **940** |
| 煤矿 | 主世界 | 除活火山、化石矿床、冰盖、潮水冰川、海底火山、山谷冰川外的全部群系 | 20×17＋30×17 | **850** |
| 金矿 | 主世界 | 恶地 | 4×9＋50×9＋0.5×9 | **490.5** |
| Tetra 晶洞方块 | 主世界 | 除短叶松林地、被摧毁的白桦林外的全部群系 | 150×3 | **450** |
| 下界石英矿 | 下界 | 玄武岩三角洲 | 32×14 | **448** |
| 锡矿 | 主世界 | 火成矿床 | 7×25＋17×4＋16×8 | **371** |
| 煤矿 | 主世界 | 活火山、冰盖、潮水冰川、海底火山、山谷冰川 | 20×17 | **340** |
| 镍矿 | 主世界 | 地壳裂隙 | 11×6＋8×6＋9×25 | **339** |
| 铜矿 | 主世界 | 火成矿床 | 16×10＋7×25 | **335** |
| 岩盐矿 | 主世界 | 盐卤矿床 | 8×40 | **320** |
| 钾盐矿 | 主世界 | 盐卤矿床 | 8×40 | **320** |
| 铜矿 | 主世界 | 滴水石洞穴 | 16×20 | **320** |
| 绿宝石矿 | 主世界 | 冰封山峰、雪林、尖峭山峰、积雪山坡、活火山、岩屑松林、海底火山 | 100×3 | **300** |
| 冷却岩浆块矿团 | 主世界 | 地壳裂隙、火成矿床 | 6×25＋2×64 | **278** |
| 岩浆块矿团 | 主世界 | 地壳裂隙 | 2×64＋6×25 | **278** |
| 黄铁矿 | 主世界 | 热液矿床 | 6×30＋8×8 | **244** |
| 镍矿 | 主世界 | 热液矿床 | 11×6＋8×6＋5×25 | **239** |
| 下界石英矿 | 下界 | 除玄武岩三角洲外的全部群系 | 16×14 | **224** |
| 锌矿 | 主世界 | 热液矿床 | 8×12＋6×20 | **216** |
| 下界金矿 | 下界 | 玄武岩三角洲 | 20×10 | **200** |
| 锡矿 | 主世界 | 除短叶松林地、火成矿床、被摧毁的白桦林外的全部群系 | 17×4＋16×8 | **196** |
| 铜矿 | 主世界 | 除滴水石洞穴、火成矿床外的全部群系 | 16×10 | **160** |
| 金矿 | 主世界 | 地壳裂隙 | 4×9＋4×25 | **136** |
| 岩浆块矿团 | 下界 | 全部群系 | 4×33 | **132** |
| 铁矿 | 主世界 | 冰盖、潮水冰川、山谷冰川 | 10×9＋10×4 | **130** |
| 镍矿 | 主世界 | 除地壳裂隙、热液矿床、短叶松林地、被摧毁的白桦林外的全部群系 | 11×6＋8×6 | **114** |
| 下界金矿 | 下界 | 除玄武岩三角洲外的全部群系 | 10×10 | **100** |
| 红石矿 | 主世界 | 全部群系 | 4×8＋8×8 | **96** |
| 锌矿 | 主世界 | 除热液矿床、短叶松林地、被摧毁的白桦林外的全部群系 | 8×12 | **96** |
| 菱镁矿 | 主世界 | 盐卤矿床 | 5×18 | **90** |
| 银矿 | 主世界 | 热液矿床 | 10×6＋1×20 | **80** |
| 钻石矿 | 主世界 | 活火山、海底火山 | 7×4＋4×8＋(1/9)×12＋2×8 | **77.3333** |
| 方解石／石灰岩 | 主世界 | 除短叶松林地、被摧毁的白桦林外的全部群系 | 7×10 | **70** |
| 黄铁矿 | 主世界 | 除热液矿床、短叶松林地、被摧毁的白桦林外的全部群系 | 8×8 | **64** |
| 岩盐矿／菱镁矿 | 主世界 | 除短叶松林地、被摧毁的白桦林外的全部群系 | 7×9 | **63** |
| 钻石矿 | 主世界 | 恶地 | 7×4＋4×8＋(1/9)×12 | **61.3333** |
| 银矿 | 主世界 | 除热液矿床、短叶松林地、被摧毁的白桦林外的全部群系 | 10×6 | **60** |
| 钻石矿 | 主世界 | 地壳裂隙 | 7×4＋(1/9)×12＋2×8 | **45.3333** |
| 青金石矿 | 主世界 | 全部群系 | 2×7＋4×7 | **42** |
| 金矿 | 主世界 | 除恶地、地壳裂隙外的全部群系 | 4×9＋0.5×9 | **40.5** |
| 铅矿 | 主世界 | 热液矿床 | 2×20 | **40** |
| 钻石矿 | 主世界 | 火成矿床 | 7×4 | **28** |
| 远古残骸 | 下界 | 全部群系 | 1×3＋1×2 | **5** |

## 混合矿脉如何归入单种矿物

- 通用盐类矿脉 Q=7×9=63：匹配普通石质替换标签时放置岩盐，匹配深板岩替换标签时放置深层菱镁矿。63 是两种产物共享的预算，不能各加 63 后再汇总。
- 盐卤矿床：独立岩盐 Q=8×40=320，独立菱镁矿 Q=5×18=90，独立钾盐 Q=8×40=320，另有通用共享预算 63。若只做单种矿物的条件极值，则岩盐为 320～383、菱镁矿为 90～153；这些范围仅表示如何分配共享指标，不是实际方块数范围，两项不能同时取上端。
- 石灰岩／方解石通用混合矿脉 Q=7×10=70：普通石质分支为石灰岩，深板岩分支为方解石。共享指标 70 不能各算一遍。另有复合矿床、晶洞及地形表层规则，见下文；70 不是这两种岩石的总储量。

## 圆盘矿床：用理想几何容量另外估算

这几类没有矿脉 `size`，改用圆盘的候选方块容量 V。`minecraft:disk` 以及注册为同一 `DiskFeature` 的 `frostedheart:disk` 使用整数圆盘；半径 r 的横截面为满足 x²+z²≤r² 的格点，`half_height=1` 对应三层。r=1、2、3 的格点数分别为 5、13、29。半径均匀取 2 或 3 时，平均容量为 (13+29)/2×3=63。容量估算假设所有候选方块均可替换；地表积雪通常不能填满三层。

| 资源 | 群系 | N | 理想容量 V | N×V |
|---|---|---:|---:|---:|
| 铝土矿块 | 主世界标签覆盖的 43 个自然群系 | 5 | 15 | **75** |
| 高岭土块 | 同上 | 5 | 15 | **75** |
| 含铜砂砾 | 冻河 | 1/4 | 63 | **15.75** |
| 铜矿霜 | 雪斑标签覆盖的 23 个自然群系 | 1/32 | 63 | **1.96875** |
| 锡矿霜 | 同上 | 1/32 | 63 | **1.96875** |

圆盘候选容量与普通矿脉的 `size` 控制量不是同一种模型，因此不强行混排。矿霜单层积雪条件下的容量指标会变为 21/32=0.65625，仍须扣除不满足替换条件的部分。

## 无法直接乘矿脉规模的生成

| 生成类型 | 群系 | 配置频率 | 为什么未并入 Q |
|---|---|---|---|
| 热液复合晶洞 | 热液矿床 | (1×3+2×2)/5=1.4 次／区块 | 铅、黄铁矿、银分别占填充和壳层，没有单一 ore size；不能把 1.4 分别当作三种普通矿脉相加 |
| 紫水晶晶洞 | 全部 45 个主世界群系 | 1/24 次／区块 | 多层空心几何体，缺少单一 size |
| Create 主世界层状矿床 | 主世界标签覆盖的 43 个自然群系 | 1/18 次／区块，复合 size=32 | 32/18≈1.7778 仅为整个复合体的规模预算，不能分摊为每种矿物各有 1.7778 |
| Create 下界层状矿床 | 全部下界群系 | 1/18 次／区块，复合 size=32 | 32/18≈1.7778 为整个复合体的规模预算 |
| 活火山地表钻石 | 活火山 | 8 个初始候选位置，再经过环境扫描和噪声计数 | 普通钻石 Q 不包含这一项；额外量需要按噪声和地表条件采样 |
| 大型铁／铜矿脉 | 主世界 | ore_veins_enabled=true | 三维噪声决定矿带，不通过固定 N×S 生成，主表不含这一项 |
| IE 挖掘机虚拟矿藏 | 按矿藏定义的维度 | 噪声阈值与矿藏权重 | 不放置对应实体矿石方块，不能与普通矿脉混算 |

Create 主世界层状矿床含绯红岩、辉绿岩、蔚蓝石、赭金砂、石灰岩及其他伴生岩石；下界层状矿床主要含焦黑浮渣、熔渣及下界伴生岩石。普通花岗岩、闪长岩、安山岩、凝灰岩、泥土、砂砾和地形表层材料不作为单一矿物矿脉排名；繁茂洞穴黏土因为使用普通 ore 配置，保留在主表供比较。

## 群系例外与零生成项

1. `the_winter_rescue:jack_pine_woodland`（短叶松林地）和 `the_winter_rescue:nature/destroyed_birch_forest`（被摧毁的白桦林）不在当前 `#minecraft:is_overworld` 内。依赖该标签的普通锡、锌、镍、银、黄铁矿、盐类混合矿脉、石灰岩／方解石、铝土矿块、高岭土块和 Tetra 晶洞都未挂载到这两群系；自身列出的原版矿物保留。短叶松林地仍在雪斑标签内，因此保留地表铜／锡矿霜。这里是配置挂载结论，不保证边界邻近的其他群系生成不会跨入。
2. 主世界仅恶地、活火山、海底火山、地壳裂隙、火成矿床这 5 个自然群系挂载普通钻石矿脉，另 40 个为 0；活火山另有地表钻石。
3. 普通 IE 铅、铀和铝土矿的 `vein_size=0`，对应 Q=0；热液专属铅矿、热液晶洞及 FrostedHeart 铝土矿块不受这几个关闭项影响。
4. 高山冰川、火山坡、板岩山体、大陆冰川、风蚀恶地、平原等虽然存在群系配置，但不在当前 normal 主世界预设的 45 个群系列表内，不纳入本次自然生成排名。

## 逐群系核对表

以下仅汇总普通矿脉指标 Q；0 表示没有挂载对应矿脉。盐类共享分支、石灰岩／方解石共享分支、岩浆矿团及其他资源见主表；圆盘与晶洞见独立章节。

| 群系 ID | 铁矿 | 煤矿 | 铜矿 | 金矿 | 锡矿 | 镍矿 | 黄铁矿 | 锌矿 | 银矿 | 铅矿 | 钻石矿 | 绿宝石矿 | 红石矿 | 青金石矿 | Tetra 晶洞方块 |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| `minecraft:badlands` | 940 | 850 | 160 | 490.5 | 196 | 114 | 64 | 96 | 60 | 0 | 61.3333 | 0 | 96 | 42 | 450 |
| `minecraft:deep_frozen_ocean` | 940 | 850 | 160 | 40.5 | 196 | 114 | 64 | 96 | 60 | 0 | 0 | 0 | 96 | 42 | 450 |
| `minecraft:desert` | 940 | 850 | 160 | 40.5 | 196 | 114 | 64 | 96 | 60 | 0 | 0 | 0 | 96 | 42 | 450 |
| `minecraft:dripstone_caves` | 940 | 850 | 320 | 40.5 | 196 | 114 | 64 | 96 | 60 | 0 | 0 | 0 | 96 | 42 | 450 |
| `minecraft:frozen_ocean` | 940 | 850 | 160 | 40.5 | 196 | 114 | 64 | 96 | 60 | 0 | 0 | 0 | 96 | 42 | 450 |
| `minecraft:frozen_peaks` | 940 | 850 | 160 | 40.5 | 196 | 114 | 64 | 96 | 60 | 0 | 0 | 300 | 96 | 42 | 450 |
| `minecraft:frozen_river` | 940 | 850 | 160 | 40.5 | 196 | 114 | 64 | 96 | 60 | 0 | 0 | 0 | 96 | 42 | 450 |
| `minecraft:grove` | 940 | 850 | 160 | 40.5 | 196 | 114 | 64 | 96 | 60 | 0 | 0 | 300 | 96 | 42 | 450 |
| `minecraft:ice_spikes` | 940 | 850 | 160 | 40.5 | 196 | 114 | 64 | 96 | 60 | 0 | 0 | 0 | 96 | 42 | 450 |
| `minecraft:jagged_peaks` | 940 | 850 | 160 | 40.5 | 196 | 114 | 64 | 96 | 60 | 0 | 0 | 300 | 96 | 42 | 450 |
| `minecraft:lush_caves` | 940 | 850 | 160 | 40.5 | 196 | 114 | 64 | 96 | 60 | 0 | 0 | 0 | 96 | 42 | 450 |
| `minecraft:snowy_beach` | 940 | 850 | 160 | 40.5 | 196 | 114 | 64 | 96 | 60 | 0 | 0 | 0 | 96 | 42 | 450 |
| `minecraft:snowy_plains` | 940 | 850 | 160 | 40.5 | 196 | 114 | 64 | 96 | 60 | 0 | 0 | 0 | 96 | 42 | 450 |
| `minecraft:snowy_slopes` | 940 | 850 | 160 | 40.5 | 196 | 114 | 64 | 96 | 60 | 0 | 0 | 300 | 96 | 42 | 450 |
| `minecraft:snowy_taiga` | 940 | 850 | 160 | 40.5 | 196 | 114 | 64 | 96 | 60 | 0 | 0 | 0 | 96 | 42 | 450 |
| `minecraft:stony_shore` | 940 | 850 | 160 | 40.5 | 196 | 114 | 64 | 96 | 60 | 0 | 0 | 0 | 96 | 42 | 450 |
| `the_winter_rescue:active_volcano` | 940 | 340 | 160 | 40.5 | 196 | 114 | 64 | 96 | 60 | 0 | 77.3333 | 300 | 96 | 42 | 450 |
| `the_winter_rescue:andesite_caves` | 940 | 850 | 160 | 40.5 | 196 | 114 | 64 | 96 | 60 | 0 | 0 | 0 | 96 | 42 | 450 |
| `the_winter_rescue:brine_deposits` | 940 | 850 | 160 | 40.5 | 196 | 114 | 64 | 96 | 60 | 0 | 0 | 0 | 96 | 42 | 450 |
| `the_winter_rescue:crust_chasms` | 940 | 850 | 160 | 136 | 196 | 339 | 64 | 96 | 60 | 0 | 45.3333 | 0 | 96 | 42 | 450 |
| `the_winter_rescue:darkfang_caves` | 940 | 850 | 160 | 40.5 | 196 | 114 | 64 | 96 | 60 | 0 | 0 | 0 | 96 | 42 | 450 |
| `the_winter_rescue:destroyed_forest` | 940 | 850 | 160 | 40.5 | 196 | 114 | 64 | 96 | 60 | 0 | 0 | 0 | 96 | 42 | 450 |
| `the_winter_rescue:diorite_caves` | 940 | 850 | 160 | 40.5 | 196 | 114 | 64 | 96 | 60 | 0 | 0 | 0 | 96 | 42 | 450 |
| `the_winter_rescue:fossil_deposits` | 940 | 1330 | 160 | 40.5 | 196 | 114 | 64 | 96 | 60 | 0 | 0 | 0 | 96 | 42 | 450 |
| `the_winter_rescue:frozen_forest` | 940 | 850 | 160 | 40.5 | 196 | 114 | 64 | 96 | 60 | 0 | 0 | 0 | 96 | 42 | 450 |
| `the_winter_rescue:glacial_fungus_caves` | 940 | 850 | 160 | 40.5 | 196 | 114 | 64 | 96 | 60 | 0 | 0 | 0 | 96 | 42 | 450 |
| `the_winter_rescue:glacial_ices` | 940 | 850 | 160 | 40.5 | 196 | 114 | 64 | 96 | 60 | 0 | 0 | 0 | 96 | 42 | 450 |
| `the_winter_rescue:glacial_lakes` | 940 | 850 | 160 | 40.5 | 196 | 114 | 64 | 96 | 60 | 0 | 0 | 0 | 96 | 42 | 450 |
| `the_winter_rescue:hydrothermal_deposits` | 940 | 850 | 160 | 40.5 | 196 | 239 | 244 | 216 | 80 | 40 | 0 | 0 | 96 | 42 | 450 |
| `the_winter_rescue:ice_cap` | 130 | 340 | 160 | 40.5 | 196 | 114 | 64 | 96 | 60 | 0 | 0 | 0 | 96 | 42 | 450 |
| `the_winter_rescue:ice_caves` | 940 | 850 | 160 | 40.5 | 196 | 114 | 64 | 96 | 60 | 0 | 0 | 0 | 96 | 42 | 450 |
| `the_winter_rescue:jack_pine_woodland` | 940 | 850 | 160 | 40.5 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 96 | 42 | 0 |
| `the_winter_rescue:magmatic_deposits` | 1148 | 850 | 335 | 40.5 | 371 | 114 | 64 | 96 | 60 | 0 | 28 | 0 | 96 | 42 | 450 |
| `the_winter_rescue:mycelium_caves` | 940 | 850 | 160 | 40.5 | 196 | 114 | 64 | 96 | 60 | 0 | 0 | 0 | 96 | 42 | 450 |
| `the_winter_rescue:nature/destroyed_birch_forest` | 940 | 850 | 160 | 40.5 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 96 | 42 | 0 |
| `the_winter_rescue:nature/destroyed_marsh` | 940 | 850 | 160 | 40.5 | 196 | 114 | 64 | 96 | 60 | 0 | 0 | 0 | 96 | 42 | 450 |
| `the_winter_rescue:nature/frostbough_forest` | 940 | 850 | 160 | 40.5 | 196 | 114 | 64 | 96 | 60 | 0 | 0 | 0 | 96 | 42 | 450 |
| `the_winter_rescue:nature/frostpine_grove` | 940 | 850 | 160 | 40.5 | 196 | 114 | 64 | 96 | 60 | 0 | 0 | 0 | 96 | 42 | 450 |
| `the_winter_rescue:nature/ironwinter_hollow` | 940 | 850 | 160 | 40.5 | 196 | 114 | 64 | 96 | 60 | 0 | 0 | 0 | 96 | 42 | 450 |
| `the_winter_rescue:nature/scree_pine_grove` | 940 | 850 | 160 | 40.5 | 196 | 114 | 64 | 96 | 60 | 0 | 0 | 300 | 96 | 42 | 450 |
| `the_winter_rescue:nature/snowy_shrubland` | 940 | 850 | 160 | 40.5 | 196 | 114 | 64 | 96 | 60 | 0 | 0 | 0 | 96 | 42 | 450 |
| `the_winter_rescue:tidewater_glacier` | 130 | 340 | 160 | 40.5 | 196 | 114 | 64 | 96 | 60 | 0 | 0 | 0 | 96 | 42 | 450 |
| `the_winter_rescue:tundra` | 940 | 850 | 160 | 40.5 | 196 | 114 | 64 | 96 | 60 | 0 | 0 | 0 | 96 | 42 | 450 |
| `the_winter_rescue:underwater_volcano` | 940 | 340 | 160 | 40.5 | 196 | 114 | 64 | 96 | 60 | 0 | 77.3333 | 300 | 96 | 42 | 450 |
| `the_winter_rescue:valley_glacier` | 130 | 340 | 160 | 40.5 | 196 | 114 | 64 | 96 | 60 | 0 | 0 | 0 | 96 | 42 | 450 |

## 单条生成配置与核算参数

下表保留可搜索的 placed feature ID；同一资源的不同档位均单独列出。高度和空气暴露参数保存在对应源数据中，未作为本次简单乘法的折扣。

| placed feature ID | 资源 | N | S | N×S |
|---|---|---:|---:|---:|
| `create:zinc_ore` | 锌矿 | 8 | 12 | 96 |
| `immersiveengineering:bauxite` | IE 铝土矿 | 16 | 0 | 0 |
| `immersiveengineering:deep_nickel` | 镍矿 | 11 | 6 | 66 |
| `immersiveengineering:lead` | 铅矿 | 12 | 0 | 0 |
| `immersiveengineering:nickel` | 镍矿 | 8 | 6 | 48 |
| `immersiveengineering:silver` | 银矿 | 10 | 6 | 60 |
| `immersiveengineering:uranium` | 铀矿 | 9 | 0 | 0 |
| `minecraft:ore_ancient_debris_large` | 远古残骸 | 1 | 3 | 3 |
| `minecraft:ore_clay` | 黏土矿团 | 46 | 33 | 1518 |
| `minecraft:ore_coal_lower` | 煤矿 | 20 | 17 | 340 |
| `minecraft:ore_coal_upper` | 煤矿 | 30 | 17 | 510 |
| `minecraft:ore_copper` | 铜矿 | 16 | 10 | 160 |
| `minecraft:ore_copper_large` | 铜矿 | 16 | 20 | 320 |
| `minecraft:ore_debris_small` | 远古残骸 | 1 | 2 | 2 |
| `minecraft:ore_diamond` | 钻石矿 | 7 | 4 | 28 |
| `minecraft:ore_diamond_buried` | 钻石矿 | 4 | 8 | 32 |
| `minecraft:ore_diamond_large` | 钻石矿 | 0.1111 | 12 | 1.3333 |
| `minecraft:ore_diamond_medium` | 钻石矿 | 2 | 8 | 16 |
| `minecraft:ore_emerald` | 绿宝石矿 | 100 | 3 | 300 |
| `minecraft:ore_gold` | 金矿 | 4 | 9 | 36 |
| `minecraft:ore_gold_deltas` | 下界金矿 | 20 | 10 | 200 |
| `minecraft:ore_gold_extra` | 金矿 | 50 | 9 | 450 |
| `minecraft:ore_gold_lower` | 金矿 | 0.5 | 9 | 4.5 |
| `minecraft:ore_gold_nether` | 下界金矿 | 10 | 10 | 100 |
| `minecraft:ore_iron_middle` | 铁矿 | 10 | 9 | 90 |
| `minecraft:ore_iron_small` | 铁矿 | 10 | 4 | 40 |
| `minecraft:ore_iron_upper` | 铁矿 | 90 | 9 | 810 |
| `minecraft:ore_lapis` | 青金石矿 | 2 | 7 | 14 |
| `minecraft:ore_lapis_buried` | 青金石矿 | 4 | 7 | 28 |
| `minecraft:ore_magma` | 岩浆块矿团 | 4 | 33 | 132 |
| `minecraft:ore_quartz_deltas` | 下界石英矿 | 32 | 14 | 448 |
| `minecraft:ore_quartz_nether` | 下界石英矿 | 16 | 14 | 224 |
| `minecraft:ore_redstone` | 红石矿 | 4 | 8 | 32 |
| `minecraft:ore_redstone_lower` | 红石矿 | 8 | 8 | 64 |
| `tetra:geode_ore` | Tetra 晶洞方块 | 150 | 3 | 450 |
| `the_winter_rescue:calcite_limestone` | 方解石／石灰岩 | 7 | 10 | 70 |
| `the_winter_rescue:cave/brine_halitel_ore` | 岩盐矿 | 8 | 40 | 320 |
| `the_winter_rescue:cave/brine_magnesite_ore` | 菱镁矿 | 5 | 18 | 90 |
| `the_winter_rescue:cave/brine_sylvite_ore` | 钾盐矿 | 8 | 40 | 320 |
| `the_winter_rescue:cave/cooled_magma_ore` | 冷却岩浆块矿团 | 6 | 25 | 150 |
| `the_winter_rescue:cave/fossil_coal_ore` | 煤矿 | 12 | 40 | 480 |
| `the_winter_rescue:cave/hydrothermal_lead_ore` | 铅矿 | 2 | 20 | 40 |
| `the_winter_rescue:cave/hydrothermal_nickel_ore` | 镍矿 | 5 | 25 | 125 |
| `the_winter_rescue:cave/hydrothermal_pyrite_ore` | 黄铁矿 | 6 | 30 | 180 |
| `the_winter_rescue:cave/hydrothermal_silver_ore` | 银矿 | 1 | 20 | 20 |
| `the_winter_rescue:cave/hydrothermal_zinc_ore` | 锌矿 | 6 | 20 | 120 |
| `the_winter_rescue:cave/large_cooled_magma_ore` | 冷却岩浆块矿团 | 2 | 64 | 128 |
| `the_winter_rescue:cave/large_magma_ore` | 岩浆块矿团 | 2 | 64 | 128 |
| `the_winter_rescue:cave/magma_ore` | 岩浆块矿团 | 6 | 25 | 150 |
| `the_winter_rescue:cave/magmatic_copper_ore` | 铜矿 | 7 | 25 | 175 |
| `the_winter_rescue:cave/magmatic_gold_ore` | 金矿 | 4 | 25 | 100 |
| `the_winter_rescue:cave/magmatic_iron_ore` | 铁矿 | 8 | 26 | 208 |
| `the_winter_rescue:cave/magmatic_nickel_ore` | 镍矿 | 9 | 25 | 225 |
| `the_winter_rescue:cave/magmatic_tin_ore` | 锡矿 | 7 | 25 | 175 |
| `the_winter_rescue:halitel_magnesite_ore` | 岩盐矿／菱镁矿 | 7 | 9 | 63 |
| `the_winter_rescue:pyrite_ore` | 黄铁矿 | 8 | 8 | 64 |
| `the_winter_rescue:tin_ore` | 锡矿 | 17 | 4 | 68 |
| `the_winter_rescue:tin_ore_large` | 锡矿 | 16 | 8 | 128 |

## 数据来源与复核边界

- [整合包世界预设](../../TheWinterRescue/kubejs/data/minecraft/worldgen/world_preset/normal.json)：实际参与自然生成的群系列表。
- [整合包自定义群系](../../TheWinterRescue/kubejs/data/the_winter_rescue/worldgen/biome/)与[原版群系覆盖](../../TheWinterRescue/kubejs/data/minecraft/worldgen/biome/)：群系直接挂载的 feature。
- [群系追加规则](../../TheWinterRescue/kubejs/data/the_winter_rescue/forge/biome_modifier/)和[群系标签](../../TheWinterRescue/kubejs/data/the_winter_rescue/tags/worldgen/biome/)：通用矿脉与地表矿霜。
- [placed feature](../../TheWinterRescue/kubejs/data/the_winter_rescue/worldgen/placed_feature/)与[configured feature](../../TheWinterRescue/kubejs/data/the_winter_rescue/worldgen/configured_feature/)：计数、稀有度、规模与替换目标。
- [IE 默认服务端配置](../../TheWinterRescue/defaultconfigs/immersiveengineering-server.toml)：`ores.*.attempts_per_chunk`、`ores.*.vein_size`，镍矿两档 8×6+11×6。
- [Create 通用配置](../../TheWinterRescue/config/create-common.toml)：`worldgen.disableWorldGen=false`。
- [FHFeatures.java](../src/main/java/com/teammoeg/frostedheart/content/world/FHFeatures.java)：`frostedheart:disk` 复用原版 DiskFeature。
- 本地模组 JAR 内的 `data/<modid>/worldgen/`、`data/<modid>/forge/biome_modifier/`，以及 Minecraft 1.20.1 `client-extra.jar` 中原版数据；未覆盖的配置取这些内置值。
- 校验：按群系逐条合并直接 feature 和标签追加 feature；对 45 个主世界群系生成矩阵，分别按档位计算并从高到低排序。人工核对火成铁矿 1148、化石煤矿 1330、热液黄铁矿 244、火成锡矿 371、繁茂洞穴黏土 1518、滴水石洞穴铜矿 320。
- 未生成新世界采样，也未构建 JAR；未改变任何矿物生成配置。
