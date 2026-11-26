# 独立栽培橡树原型

- 状态：完整原版树形重写、生产及测试源码编译通过；栽培树木专项36/36通过，含实际64格云杉；未进行本轮客户端目视验收。
- 最后核对：2026-11-20。
- 范围：Minecraft 1.20.1 / Forge；本文描述 `frostedheart:cultivated_oak_sapling`；共享机制及新增云杉示例见 [栽培云杉](cultivated-spruce.md)，原版橡树苗物品放置时转换为内部栽培方块；不扫描或替换世界生成的树木。
- 代码锚点：`CultivatedSaplingPlacement.placed`、`CultivatedOakBlock.getCloneItemStack`、`CultivatedOakBlockEntity.advance`、`CultivatedOakProfile`、`CultivatedOakGameTests`。

## 试用

直接使用原版橡树苗（`minecraft:oak_sapling`），种在泥土或施肥泥土上，放置后自动转换为栽培方块。没有独立栽培树苗物品或创造栏条目；拆除和中键选取均返回原版橡树苗，重新种植不携带进度。确保光照至少 9，周围留足空间。手持或装备土壤温度计并瞄准树苗，悬浮信息按普通作物样式显示阶段名称、生长进度条、温度位置条与等待原因；不显示温度度数、累计进度数值或百分比，肥料储量也只用图标条表示，按 Shift 展开无数值的温度/理想区间及光照要求条；不再提供空手右键消息。阶段 `age=0..4` 使用同一张原版橡树苗贴图逐渐放大。

自然随机刻推进，没有每 tick 的方块实体 ticker，也不使用原版骨粉接口。默认理想温度为 15～25°C，正常生长范围为 5～35°C；随机刻通过 `WorldTemperature.getTempAtBlock(level, pos)` 读取树苗所在格，可读的树苗材料温度优先，缺少读数时取该格的环境气温。可以临时提高 `randomTickSpeed` 加快观察，但这也会影响世界中的其他随机刻方块。

温度计不显示目标产量、高度或随机种子。进度满后仅在累计产量够一格且当前温度产量系数大于零时尝试生成；环境之后变冷不会撤销已经完成的生长。树木使用原版橡木和橡树叶，树叶仍按原版掉落橡树苗；本原型未添加合成或独立木材。

## 数据包

覆盖 `data/frostedheart/cultivated_trees/oak.json` 后执行 `/reload`。新增目录由服务端资源重载监听器解析，不是原版 configured feature 注册表，也不是通用脚本语言。移除或无法解析配置时树苗保留并等待修正，错误写入服务端日志。

默认 `vanilla_tree=minecraft:oak`，从原版 configured-feature 注册表读取整套结构。原型的内嵌 `tree` 配置仍可解析以兼容旧数据包，但推荐改用原版注册表来源。完整树形及期望换算见[树形与产量模型](cultivated-tree-shapes.md)。

| 字段 | 默认值与含义 |
|---|---|
| `growth.progress_per_stage` | 4，每一阶段所需进度，共四次阶段转换；允许 1～127，以确保 short 定点进度及最高肥料贡献可表示 |
| `growth.branch_probability` | 旧配置兼容字段（0～1），不再控制人工短枝；分叉随机性由原版生成器决定 |
| `growth.min_light` | 9，推进进度、累积信用及生成所需的最低有效光照 |
| `yield.base_logs` | 6，旧版兼容字段；新产量以高度参照换算 |
| `yield.logs_per_height` | 1，线性换算斜率 |
| `yield.fixed_logs` | 0，线性换算固定木头项 |
| `yield.min_height` / `max_height` | 4 / 12，边界／理想温区的总木头目标期望参照；支持范围 4～24，不是几何高度硬限制 |
| `temperature.min` / `ideal_min` / `ideal_max` / `max` | 5 / 15 / 25 / 35，单位 °C |
| `temperature.minimum_yield_factor` | 0.5，温度在生长边界时的产量系数 |
| `temperature.preserved_degrees_per_grade` | 5，每单位保温等级向下扩展的温度范围，单位 °C |

高度直接按累计产量系数 f 换算：默认 minimum_yield_factor=0.5 时，f≤0.5 用 B=floor(min_height × f/0.5)，f>0.5 用 B=floor(min_height+(max_height-min_height)×(f-0.5)/0.5)。f 为累计信用除以满阶段所需进度。不再有饱和曲线或额外高度上限；不足一格继续等待。整数木头与逐次信用取整仍会产生小台阶。

增产肥仅保留额外效果的 20%，常量 CultivatedOakBlockEntity.INCREASING_BONUS_SCALE=0.2。有效倍率为 1+(productivity-1)×0.2，基础/高级/终极由原 ×2/×3/×4 折算成 ×1.2/×1.4/×1.6，不削减无肥料的基础产量。连续公式下三档高度预算为 15/18/21。实际每随机刻信用按 1/16 取整：理想温度下三档每次记入 19/22/26 个定点单位，16 次累计 304/352/416，实际高度预算为 15/18/22，含主干和全部原版枝杈的目标期望为15/18/22块。生成时再按原版树形模型换算尺寸，单棵实际数量允许波动。

## 橡木树形与枝杈

预算不超过6时以普通原版橡树为主，预算至少4时保留10%的大型橡树选择；预算超过6时使用完整 `fancy_oak`。不再向直干旁人工添加1～3格木头，也不再按0.6扣主干或通过补柱满足配额。FancyTrunk保留原版中轴、斜枝、木头轴向和多处叶冠；低预算幼树仍确保至少一格主干。具体尺寸校准及验证边界见[完整原版树形](cultivated-tree-shapes.md)。

## 生长与肥料

所有温度下基础速度恒为 1，温度仅影响产量；理想区间产量系数为 1，两侧线性降到能长边界的 0.5。停长带每侧宽度为 max-min，默认 30°C（-25～5 与 35～65）；产量系数线性降到 0，带外产量为 0，不能生成树木。累计进度不倒退。

增产肥料将当次产量系数乘以 `1+(FertilizerGrade.productivity-1)×INCREASING_BONUS_SCALE`；加速肥料将当次进度除以 `0.5 × FertilizerGrade.growSpeed`；保温肥料将最低温度降低 `preserved_degrees_per_grade × FertilizerGrade.preserve`。只识别根部 `FERTILIZED_DIRT`，不增加直接对树苗施肥入口；可先施肥泥土，再种树，或对根部泥土补肥。

肥料改为成功生成后一次性结算：单棵在自身根部扣4格；2×2整棵只在西北生成原点的根部扣4格，其他三处土壤保持原样，不是每株各扣4。生长、满进度等待产量、缺伙伴及生成失败重试均不扣。储量不足4时耗尽并转普通泥土，无施肥土壤不收费；旧 SaplingGrowTreeEvent 扣费代码继续注释，避免恢复旧入口重复结算。三类肥料和三个等级同样收费，没有新增存档字段。

依据：旧树事件每次生长放行在生成原点扣4格，巨型事件也仅一次。以用户指定的“成功生长”为对齐口径，新规则在根部储量s充足时满足 E[消耗|生成成功]=4，存量不足时为min(4,s)，无肥土壤为0。按默认阶段进度，原先逐随机刻方案在全程供肥时，无加速/基础加速/高级加速/终极加速分别耗16/8/4/2格；现在统一4格。若每随机刻固定1/4概率扣一格，只有无加速能得到4格期望，加速后变2/1/0.5，所以不采用该做法。直接成功结算无方差，也不会被额外等待随机刻放大消耗。

中途补肥只影响之后取得的进度。累计 `yieldCredit += growthStep × yieldFactor`；进度满前 growthStep 为本次实际进度，进度满后为同样速度对应的增长量，进度本身封顶，每次贡献同样四舍五入到 1/16 单位。每次生成尝试现算平均产量系数 `f = yieldCredit / (4 × progress_per_stage)`（数值均换算到相同单位），再按高度参照公式计算高度；随机过程使用随机刻传入的 RandomSource，不保存独立种子。进度满后若不足一格，进度保持满值，继续按随机刻累积产量信用；此时补肥影响后续贡献，不追溯既有贡献。已有足够产量但空间受阻时仅重试生成，不再累计或扣肥。

默认全程理想环境下，无肥料需 16 次有效随机刻，总木头目标期望12（实际主干高度由完整原版树形决定）；基础加速肥料需 8 次；全程基础增产肥料且持续补足储量则总木头目标期望15。进度满后最早在下一个有效随机刻生成；不足一格时额外等待随机刻，零产量温区内始终不生成。

## 空间、保存与限制

随机刻入口先执行 Forge `SaplingBlock` 同款 `isAreaLoaded(pos, 1)`，在温度、光照查询之前返回。去掉整树范围 `hasChunksAt`，不要求外围树冠所涉及的区块全部已加载；跨区块读取和生成行为交给原版。

核心空间与世界高度限制交给 `Feature.TREE.place` / `TreeFeature`，沿用所选普通／大型橡树配置的 `minimum_size`，包含原版允许的局部裁低规则。外围树冠遇到石块时跳过相应叶片，不因此禁止整棵树生成，也不会覆盖石块。

成熟以方块状态 `age=4` 判定，不持久化目标高度、产量或种子。空间不足时恢复成熟树苗及生长 NBT，后续随机刻重新计算并尝试生成，不扣肥。正常成功生成后保留根部土壤，避免覆盖剩余肥料。未引入通用放置事务。

温度计通过现有请求/响应通道取得服务端状态，附带目标坐标避免切换目标时混用树苗信息；持续瞄准树苗时每 20 tick 重新请求一次。栽培树苗状态与普通作物的温度、天气暴露标志共用 `SoilThermometerUpdatePacket.sample`。响应格式对应网络协议后缀 `soil-thermometer-4`，服务端与客户端需共同更新。

方块实体仅保存两个自定义数值：`p`（progress）和 `v`（yieldCredit），均为 short，数值单位为 1/16；避开方块实体保留坐标键 x/y/z。成熟标记仍在方块状态中，不额外保存。旧版 double `Progress` / `YieldCredit` 读取时转换；旧目标字段忽略且不再写出。打掉树苗只掉新树苗，不携带进度。重载配置会影响下一次尝试的高度和树冠，因为这些参数不再固定在树苗存档中。

## 验证入口

从工作区父目录执行：`bash FrostedHeart/gradlew -p FrostedHeart --offline runGameTestServer -I Scripts/cultivated-oak-validation.init.gradle -PgameTestNamespaces=frostedheart_cultivated_oak -PgameTestDirectory=build/cultivated-oak-gametest`。

覆盖 short 类型与旧存档迁移、温度计状态数据及网络序列化、真实随机刻入口，以及原版核心障碍等待与树冠外围障碍容忍、配置解码与线性换算、五阶段状态、恒定速度和保温、满进度等待产量与恢复、零产量不生成、加速与增产、边界温度减产、NBT 恢复、障碍重试不扣肥、成熟后补肥不追溯，以及高度换算单调/无饱和上限/肥料效果按比例缩减、完整原版树形、树冠连接点与木头产量统计期望。测试使用显式温度调用同一生长逻辑，不替代自然气候全周期统计、客户端渲染或独立进程存档重启验收。

2026-11-18 客户端补充验证：使用真实树苗物品右键泥土放置，空手右键收到阶段状态；临时世界内以 heat_adjust 提供 20°C 环境、randomTickSpeed=500 加快自然随机刻，旧版成熟并受阻时目标固定为 6，移除障碍后实际生成六格橡木树干，客户端持续正常渲染。截图与命令记录位于 `run/agent/screenshots/`、`build/cultivated-oak-client-check.json`，详见客户端验证日记。这些临时参数不修改生产配置。

橡树使用普通／大型原版树形，自身没有 2×2 巨型变体。已新增[栽培云杉及其原版巨型变体](cultivated-spruce.md)，实现与本轮验证范围见该文档。

原版其他木种及深色橡木2×2专属生长已接入共享流程，见[原版木种栽培](cultivated-species.md)；本轮已编译并通过完整栽培树木专项。

## 最终服务端验证（2026-11-19）

`compileJava`、`compileGameTestJava` 与专用 `runGameTestServer` 串行成功，`frostedheart_cultivated_oak` 共 27/27 通过。包含原有温区/肥料/枝杈/存档/空间重试，八种原版物品放置/掉落，多木种完整生长、丛林巨型、深色橡木专属门槛及每单位加速进度按当时温度结算信用。最新日志 `build/cultivated-release-growth-validation.log`，不代表客户端视觉验收或完整模组包兼容性验证。

当前图表：[橡树温度与总木头期望](images/cultivated-oak-yield-vs-temperature.png)、[云杉单棵与2×2巨型对比](images/cultivated-spruce-yield-vs-temperature.png)。云杉图用 `python3 Scripts/plot-cultivated-spruce-yield.py` 按默认配置重画，需要本机 FreeType 与 Noto CJK 字体，不使用 Java 或编译步骤。

树苗温度计使用 `CultivatedTreeTempStats` 复用 `PlantTempStats` 的图标、颜色、环境范围条与根部肥料条，默认摘要与 Shift 展开保持作物布局；树苗行不显示具体数值。环境范围、阶段与进度通过服务端 `TreeInfo` 快照提供，保温肥的低温扩展来自当前树苗根部，专用服务器客户端不依赖服务端配置静态缓存。目标产量、目标高度和随机种子不发送。原有普通作物调用仍保留原展示，新增无数值模式仅用于树苗。

本轮验证：`compileJava`、`compileGameTestJava` 及专项28/28通过，日志 `build/cultivated-thermometer-bars-validation.log`；普通作物原有分隔符样式校正后再次 `compileJava` 通过。首轮旧等待时限下10个用例在光源传播前超时，调整测试夹具等待上限后完整通过，不改变生产光照要求。新增保温肥服务端范围与非树苗空快照检查，并扩展原有快照网络往返检查。未进行客户端截图或视觉验收。

## 成功生成扣肥回归（2026-11-19）

`compileJava`、`compileGameTestJava`、`runGameTestServer` 成功，完整树木专项30/30通过，日志 `build/cultivated-fertilizer-open-sky-validation.log`。新增 `CultivatedFertilizerGameTests` 在实际生成中覆盖三种肥料×三个等级统一扣4、阻挡重试不收费与不足4格耗尽；原巨树用例验证四根中仅生成原点扣一次。测试共用 `CultivatedTestEnvironment` 将场地放到世界高度上部并清空其上方，设置晴天正午，消除地下模板及自然地形对真实光照/空间的干扰；没有降低生产光照要求。此前两轮因场地遮光/阻挡失败，不计为通过。

## 完整原版树形验证（2026-11-20）

`compileJava`、`compileGameTestJava`及专用`runGameTestServer`成功，36/36项通过；日志`build/cultivated-native-final-validation.log`。包括2912组同种子同尺寸逐块原版对照、75组预算×512独立种子的木头均值、完整树冠／根／装饰／蜂巢来源保留、实际生成64格云杉及樱花不足完整分叉预算继续攒信用。保留温度计、p/v存档、随机刻、肥料和2×2既有回归。详见[完整原版树形与验证数据](cultivated-tree-shapes.md)。先前27／28／30项通过记录是旧版本历史，不代表本次重写的视觉验收。

### 放置预测同步（2026-11-26）

`BlockItemMixin_CultivatedSapling` 在 `BlockItem.getPlacementState` 返回时，将有效的原版树苗放置状态转换为初始栽培状态，客户端预测与服务端一致，避免先显示原版树苗再切换。保留原版放置失败结果与含水状态。`CultivatedSaplingPlacement.placed` 继续为其他触发放置事件的途径提供转换。
