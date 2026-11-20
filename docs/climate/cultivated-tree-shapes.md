# 栽培树木：完整原版树形与木头产量期望

- 状态：原版结构重写、编译及36/36项专项通过；尚未进行客户端目视验收。
- 最后核对：2026-11-20。
- 范围：八种主世界栽培树苗的单棵及原版2×2变体。
- 代码锚点：`CultivatedOakProfile.Profile.atHeight`、`CultivatedNativeTrunkPlacer`、`CultivatedTreeYield`、`CultivatedNativeShapeGameTests`。

## 产量预算不是树干高度

用户已确认完整保留原版树形，允许每棵木头数围绕期望波动。`earnedHeight` 沿用旧方法名和产量参照字段，但返回值 B 表示单株总木头目标期望（主干＋枝杈），不再直接作为几何高度。单棵目标 B，2×2目标4B+8，其中B由四株已累积信用的平均值换算。原来的每株多2块改为期望优势，不保证每棵精确多8块。

`CultivatedTreeYield` 按所选原版树形，将目标木头数反算为尺寸，再在相邻整数尺寸间随机插值。原版分叉本身继续随机。换算使用少量系数和常数次计算；没有每随机刻反复预生成，也不存储树形或种子。系数依据默认Minecraft 1.20.1生成器的开放场地木头抽样，另用独立种子检验均值。根、藤蔓、树叶、蜂巢不计为木头；以LOGS标签统计木头。

全部尺寸使用本树种原版主干，不再以直干代替低预算分叉。樱花尺寸1的原版分叉也平均有7.7109375块木头，故最低目标预算为8；未达到时满进度继续攒信用，显示等待产量。其余可单生树种的最低预算仍是1。温度系数为0时仍不生成。

高度没有原版上限，也没有32格上限；世界建造高度仍遵守原版TreeFeature限制。模型对常见尺寸使用少量校准点的分段线性插值，大型橡树另保留低尺寸整数分叉台阶；32以上继续外推，大型橡树用二次木头模型，其余用线性木头模型。所有枝杈、弯干、树冠连接仍由相同原版算法处理。数组总共67个木头均值（约536字节），只在生成尝试时二分查找，不在随机刻预生成或构建大型查找表。

## 每种树实际调用的原版算法

| 树种 | 单棵／巨型配置与结构 |
| --- | --- |
| 橡木 | `oak`的Straight＋Blob；预算≤6时按原版10%概率选`fancy_oak`（不足4预算保持普通幼树），预算>6直接选Fancy。FancyTrunk生成中轴、斜枝和各枝叶冠，FancyFoliage保留原版冠形。选择大型形态不保证每个随机种子都有横向木头；不再人工加同高度单格横杈。 |
| 云杉 | 单棵`spruce`，Straight＋SpruceFoliage；2×2等概率`mega_spruce`／`mega_pine`，GiantTrunk＋MegaPineFoliage，分别保留长冠／短冠与灰化土地表装饰。 |
| 白桦 | `birch`的Straight＋Blob。 |
| 丛林 | 单棵`jungle_tree_no_vine`；2×2`mega_jungle_tree`保留MegaJungleTrunk上半部斜枝、独立叶冠、干藤与叶藤。 |
| 金合欢 | `acacia`的ForkingTrunk弯干、可选第二分叉及AcaciaFoliage扁冠。 |
| 深色橡木 | 仅2×2`dark_oak`，保留DarkOakTrunk整体弯折、冠下短枝和DarkOakFoliage；没有单棵回退。 |
| 红树 | 85%`tall_mangrove`、15%`mangrove`；UpwardsBranchingTrunk上升分叉、RandomSpread叶冠、MangroveRoot根与抬高主干原点，以及苔毯、叶藤、胎生苗和蜂巢装饰。 |
| 樱花 | `cherry`的CherryTrunk随机1～3枝、木头轴向、横伸与上升路径；CherryFoliage空洞和下垂叶。 |

橡木／大型橡木／白桦／樱花在原点周围5×3×5范围存在花卉时，选对应`*_bees_005`配置，和原版树苗一致；花卉查询跳过未加载位置。

## 保留TreeFeature的完整生成顺序

选择尺寸→计算原版树冠参数→确定根抬升后的主干原点→原版空间检查→根→主干与全部分叉实时写入→全部FoliageAttachment对应树冠→装饰器→树叶距离更新。`CultivatedNativeTrunkPlacer`只固定尺寸并委托原版主干；不截断候选、不补柱、不删除树冠附件，不改变木头轴向。

原版GiantTrunk有三根柱比最高柱短一格，完整保留，不再补平顶部。例如高度h的巨型云杉为4h-3块木头；随机选择相邻h，使平均产量接近目标4B+8。分叉树则将原版全部枝杈一起纳入期望模型，旧人工枝杈的0.6／0.15费用不再使用。

空间、障碍、树叶避让、世界高度仍由原版TreeFeature处理。原版允许的局部枝杈受阻或树形裁低可能减少实产；模型只承诺默认配置、空间充足时的统计期望。数据包可覆盖原版configured feature；树形随之更新，但若改变主干算法或分叉参数，需要重新抽样校准期望模型，不能假定旧系数仍精确。

## 验证和图表

`CultivatedNativeShapeGameTests.nativeAdapterPreservesEveryWriteAndAttachment`使用同种子、同尺寸，逐块比较原版和适配器的写入、木头轴向及全部树冠附件。`nativeYieldReferenceSamples`生成原版尺寸抽样CSV；`wholeNativeTreesTrackExpectedWoodAcrossSeeds`用独立种子检查实际原生主干＋枝杈均值。实际世界生成仍由原有八物种生长、空间失败恢复、2×2和肥料用例覆盖。这些是服务端结构与统计检查，不替代客户端目视验收。

### 本次结果

最终日志`build/cultivated-native-final-validation.log`，BUILD SUCCESSFUL in 3m 12s，36/36项通过。13种原版配置×7种尺寸（1/4/8/12/20/32/64）×32种子，共2912组逐块对照一致。12种配置各35种尺寸×128种子的原版参考均值见[native-tree-yield-reference.csv](validation/native-tree-yield-reference.csv)，包含48/64/96格；75组预算×512独立种子的校准结果见[cultivated-native-yield-validation.csv](validation/cultivated-native-yield-validation.csv)。预算覆盖1～256、单棵及巨型；低于樱花最低预算的组合验证继续等待，不纳入均值。

均值检查容差为max(0.5块，目标的8%)，本次最大相对偏差6.69%，出现在目标256块的大型橡树（均值238.87）。这是近似期望模型，不能把目标数当成逐棵配额或精确解析均值。常用尺寸和超原版尺寸均已覆盖，未用测试结果宣称完整模组包或客户端视觉验收。

新增实际世界64格云杉生长检查、花卉蜂巢及树冠／根／装饰器来源检查、樱花阶段满后继续累积到8块预算检查；既有生长、2×2、肥料结算与存档专项全部通过。

图表：[橡木温度与总木头期望](images/cultivated-oak-yield-vs-temperature.png)、[云杉单棵与2×2](images/cultivated-spruce-yield-vs-temperature.png)。Python入口分别为`Scripts/plot-cultivated-oak-yield.py`与`Scripts/plot-cultivated-spruce-yield.py`，共用`Scripts/cultivated_tree_plot.py`。橡木图使用原版参考均值和同样的尺寸插值，云杉使用精确4h-3期望；图表按肥料类型分图、含无肥基线与三档实线，不把主干高度当总产量。

2026-11-20提交前整合远端bcd55face后，编译与36/36树木专项再次通过，日志`build/cultivated-upstream-integration-validation.log`。温度计协议合并普通作物天气字段和树苗TreeInfo，版本为`soil-thermometer-4`；树形模型未改变。详见[整合记录](../../diary/2026-11-20_02-28-40_cultivated-upstream-integration.md)。
