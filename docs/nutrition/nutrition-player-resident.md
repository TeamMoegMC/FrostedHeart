# 玩家与居民营养系统

- Status: `Current`
- Last verified: `2026-08-21`
- Scope: 玩家营养、统一食物画像、居民住宅菜单、当前营养支持、属性成长、持久化与界面
- Primary code anchors: `FoodNutritionProfile`, `FoodNutritionResolver`, `GatherFoodNutritionEvent`, `PlayerNutritionState`, `NutritionCapability`, `ResidentNutrition`, `ResidentPublicMenuModel`, `TownHousingMealService`, `ResidentNutritionSupportModel`, `ResidentAttributeModel.settleDailyAttribute`, `ResidentNutritionSnapshot`, `TeamTownData.tickMorning`

本文描述当前已实现行为。食物原始数据仍由 `new_food_value.xlsx` 及生成的 `frostedheart:diet_override` JSON 提供；运行时百分比语义和消费者公式以本文列出的代码为准。

## 1. 系统边界

玩家与居民共享同一个食物事实，但保留不同的结算节奏：

| 层级 | 权威模型 | 语义 |
|---|---|---|
| 食物 | `FoodNutritionResolver.resolve(Level, ItemStack)` | 四项 `0..100` 营养画像 |
| 玩家 | `PlayerNutritionState`, owned by `NutritionCapability` | 实时四项营养状态 `0..100` |
| 居民 | `ResidentNutrition` | 每日结算的四项营养储备 `0..100` |
| 居民解释数据 | `ResidentNutritionSnapshot` | 最近一次日结算结果，不参与后续计算 |

四个通道的存储顺序仍为脂质、碳水、蛋白质、蔬果；属性矩阵使用更易读的蛋白质、脂质、蔬果、碳水顺序。代码中的 `vegetable` 表示设计语义中的“蔬果”。

类型边界与单位一一对应：`NutritionRecipe` 保存生成数据的 raw 值，`FoodNutritionProfile` 只表示食物百分比，`PlayerNutritionState` 只表示玩家百分比状态，`ResidentNutrition` 只表示居民百分比储备。系统不再使用无单位的 `Nutrition`、`MutableNutrition` 或 `ImmutableNutrition` 通用向量。

`DailyKitchen` 的想吃菜和奖励逻辑与四通道营养相邻但独立，不属于本文公式。

## 2. 统一食物营养画像

### 2.1 原始数据与公开语义

现有 Excel 和生成 JSON 数值不改。`FoodNutritionProfile.fromRecipeValues` 对四个明确的 recipe raw 通道执行：

```text
profile_i = clamp(recipe_i / 400, 0, 100)
```

四项分别限制，不要求总和为 `100`。例如：

- 熟牛肉原始蛋白质 `24000`，公开画像为蛋白质 `60%`；
- 烤马铃薯原始碳水/蔬果 `16000/8000`，公开画像为 `40%/20%`；
- 没有静态匹配且没有动态兼容结果时返回 `FoodNutritionProfile.ZERO`，不返回 `null`。

数据位置和生成入口：

- `src/datagen/resources/data/frostedheart/data/new_food_value.xlsx`
- `src/generated/resources/data/frostedheart/recipes/diet_value/**/*.json`
- `NutritionRecipe`, `FHRecipeProvider`, `NutritionRecipeBuilder`

### 2.2 唯一解析入口

Tooltip、玩家进食、居民菜单和 Caupona 动态食物都调用：

```java
FoodNutritionResolver.resolve(Level, ItemStack)
```

Resolver 先找静态 `NutritionRecipe`，再发布 `GatherFoodNutritionEvent` 以解析实际 `ItemStack` 中的动态内容。事件的 original/result 都是 `FoodNutritionProfile`，监听器只能通过 `setProfile` 整体替换百分比画像，不再暴露无单位的可变容器。事件不带玩家参数，因此同一汤品对玩家和居民产生同一画像。

Caupona compat 在自己的包内使用局部累加器，对各原料已经解析完成的百分比画像按 healing 加权，最后一次性写回 Event。该过程不会再次执行 `/400`。

若多个静态 recipe 同时匹配，Resolver 会：

1. 记录包含所有 recipe ID 的错误诊断；
2. 按 recipe ID 的稳定字典序选唯一结果；
3. 不再让不同消费者分别“取第一项”或“累加全部”。

离线 Stage 3/4 模拟器从同一生成 JSON 读取，并复用 `FoodNutritionProfile.RAW_TO_PERCENT` 和居民纯数值公式。它不能发布依赖实际 `Level` 的动态食物事件，因此动态汤品一致性属于游戏运行时路径。

### 2.3 居民食物资格与数量

营养画像不决定物品能否进入居民菜单。物品仍须属于 `town_resource_resident_food_level_0..4`，等级 `4 -> 0` 是绝对优先级。

一件食物提供的仓库食物资源量保持：

```text
foodUnits = hunger + hunger * saturationModifier * 2
```

营养积分只使用原版 hunger，不使用 saturation。由此，同 hunger、同画像但 saturation 不同的食物具有相同营养积分，却提供不同数量的食物资源。

## 3. 玩家营养

### 3.1 状态与迁移

`NutritionCapability` 持有 immutable `PlayerNutritionState`；四个 `float` 每项范围固定为 `0..100`，新玩家默认 `70`。进食和 hunger 损失通过 `PlayerNutritionState.afterEating` / `afterHungerLoss` 返回新状态，所有 setter、命令写入和状态转移都会立即限制范围。

NBT 字段增加 `version=2`：

- version 2 直接读取百分比；
- 缺少版本的旧状态按 `clamp(old / 100, 0, 100)` 迁移；
- 完全缺少四项字段时初始化为 `70`。

死亡克隆继续复制玩家营养。`/frostedheart nutrition ... fill` 现在写入 `100`。

### 3.2 进食

`HealthCommonEvents.finishUsingItems` 只在服务端调用玩家营养结算。设食用前 hunger 为 `foodLevelBeforeEating`：

```text
effectiveHunger = min(itemHunger, 20 - foodLevelBeforeEating)
gain_i = effectiveHunger * profile_i / 100 * nutritionGainRate
```

`nutritionGainRate` 的运行时默认值为 `1.0`，含义是：画像为 `100%` 时，每实际恢复一点 hunger，增加一点对应营养状态。

完整食用熟牛肉时，蛋白质增加 `8 * 60% = 4.8`；若食用前只缺 `2` hunger，则只增加 `1.2`。饥饿溢出部分不提供营养，也不会反向消耗已有营养。

### 3.3 消耗

只有原版 hunger 实际下降时才扣除营养。设下降量为 `hungerLost`：

```text
loss_i = hungerLost * nutritionConsumptionRate
```

`nutritionConsumptionRate` 的运行时默认值为 `0.25`，即每损失一点 hunger，四项各固定减少 `0.25` 状态点。当前库存比例不参与计算。

### 3.4 配置版本

配置键 `nutritionScaleVersion` 控制一次性迁移：

```text
old nutritionGainRate        * 400
old nutritionConsumptionRate * 100
```

版本 1 的规范初值仍使用旧 `0.0025/0.0025`。专服世界和单人世界的磁盘型 `frostedheart-server.toml` 加载后立即得到版本 2 的 `1.0/0.25` 并保存，之后不再重复迁移。远程连接期间由 Forge 创建或由服务器同步的内存型 SERVER 配置不在客户端迁移或保存；客户端直接使用服务端权威配置，避免登录服的 vanilla 回退流程对 `SimpleCommentedConfig` 调用 `ModConfig.save()`。

### 3.5 玩家后果和界面

本轮只迁移尺度，没有重新设计玩家后果：

- 最大生命值仍由四项营养共同修正，内部输入从除以 `10000` 改为除以 `100`；
- 蛋白质、蔬果低于 `20` 的贫血计分逻辑保持；
- 由于状态不允许超过 `100`，旧的 `>10000` 过量分支按尺度迁移为 `>100` 后不会触发；
- 健康菜单环形条直接按 `value / 100` 显示；
- 食物 Tooltip 显示解析后的营养画像百分比。

系统没有独立的玩家营养同步包；权威存档和消耗在服务端，打开健康菜单时通过容器数据槽同步。

## 4. 居民每日摄入与住宅菜单

### 4.1 默认值和顺序

主要配置默认值：

| 配置键 | 默认值 | 单位/含义 |
|---|---:|---|
| `foodConsumptionPerResidentDay` | 20 | 食物资源单位/居民/日 |
| `residentNutritionReferencePoints` | 200 | 一个 coverage 的 hunger 加权百分点 |
| `residentNutritionReserveLossPerDay` | 1 | 每通道状态点/日 |
| `residentNutritionGainAtReference` | 2 | 每 coverage 的状态点 |
| `residentNutritionMaximumCoverage` | 2 | 单餐单通道 coverage 上限 |
| `residentNutritionMaximumReserve` | 100 | 单通道存储上限 |
| `Resident Generation.initialNutritionMinimum` | 30 | 普通难民单通道初始生成下界 |
| `Resident Generation.initialNutritionMaximum` | 70 | 普通难民单通道初始生成上界 |
| `residentNutritionHealthyReserve` | 70 | 满满足度健康线 |
| `residentNutritionSevereReserve` | 20 | 严重缺乏线 |
| `residentNutritionMealSelectionChunks` | 8 | 每栋住宅菜单选择片段数 |

`TeamTownData.tickMorning` 的相关顺序为：

```text
每名居民四项先固定衰减
-> 住宅和照护顺序确定每名居民的食物额度
-> 采矿、狩猎和加工完成
-> TownHousingMealService 按住宅优先级逐栋选餐并扣仓库
-> 每栋住宅内部按额度比例分配同构成餐食
-> 生命/精神恢复
-> 力量/智力结算
-> 年龄转换与居民退出检查
-> 保存结算快照与城镇统计
```

普通难民招募和管理员居民指令不会再把四项储备统一设为 `residentNutritionInitialReserve`。四个通道各自独立取四个 `[0,1]` 均匀样本的平均值并映射到默认 `[30,70]`；因此期望值均为 `50`，但同一居民的四个通道通常不同。`residentNutritionInitialReserve` 继续服务于旧构造器、固定场景和兼容路径；Citizen/AI 调试构造器本轮未接入新招募模型。旧存档缺少营养字段时仍按 Codec/NBT 兼容默认 `70` 读取，不重新随机。

无家可归居民也会发生营养储备衰减和属性结算，但不会从住宅口粮获得食物；其既有无家可归健康惩罚保持不变。

### 4.2 食物积分和储备

对物品 `j`：

```text
points_i = sum(itemFraction_j * hunger_j * profile_ij)
coverage_i = clamp(points_i / residentNutritionReferencePoints,
                   0, residentNutritionMaximumCoverage)
newReserve_i = clamp(oldReserve_i - lossPerDay
                     + gainAtReference * coverage_i,
                     0, residentNutritionMaximumReserve)
```

熟牛肉蛋白质画像 `60%`、hunger `8`，得到 `480` 蛋白质积分；coverage 限制为 `2`，一餐最多增加 `4`，计入当日衰减后净增加 `3`。烤马铃薯得到碳水/蔬果 `200/100` 积分，对应 coverage `1/0.5`。

### 4.3 住宅菜单算法

`ResidentPublicMenuModel` 是游戏和 Stage 3/4 模拟器共享的纯菜单算法。游戏把每栋住宅作为一个 recipient group；Stage 3/4 当前只有一栋抽象住宅，因此调用一次等价：

1. 先由既有住宅保障和 `TownCareLaw` 确定每名居民的食物额度；
2. 按 `TownHousingPlan` 顺序逐栋处理住宅，序号更小者先选择食物；
3. 把当前住宅获配的食物量拆为默认 `8` 个片段；
4. 每个片段只比较该时刻剩余库存中最高可用等级的候选；
5. 模拟候选片段按本住宅居民额度占比分配后的实际 reserve gain；
6. 选择令本住宅相对健康线 `70` 的四通道总缺口下降最多的候选；
7. 用稳定物品 ID/NBT 处理平局，并从规划库存中移除已选物品，后续住宅只能使用剩余库存；
8. 游戏按住宅依次执行仓库扣除，营养摄入和菜单快照只读取资源执行器返回的实际 `modifiedAmount`。

同一住宅内的获配居民获得相同食物构成比例，仅总量随额度变化；不同住宅可以获得完全不同的等级和构成。住宅优先级由此控制质量，保障与第二轮分享控制数量。评分不额外引入年龄、健康或属性权重。旧的逐居民贪心配餐、`nutritionQuality` 和标量恢复倍率均已删除；`HouseBuilding.DailyReport` 的旧 `nutritionQuality` Codec 字段只用于兼容读取。

本轮没有增加“昨日膳食覆盖率”。

## 5. 居民当前营养支持矩阵

### 5.1 当前满足度

餐后当前满足度：

```text
n_i = clamp(reserve_i / healthyReserve, 0, 1)
```

默认健康线为 `70`，因此 `70..100` 都提供相同的满支持，只作为库存缓冲。

### 5.2 权重

默认列顺序为蛋白质、脂质、蔬果、碳水：

| 结果 | 蛋白质 | 脂质 | 蔬果 | 碳水 |
|---|---:|---:|---:|---:|
| 生命 | 0.50 | 0.10 | 0.30 | 0.10 |
| 精神 | 0.10 | 0.30 | 0.20 | 0.40 |
| 力量 | 0.75 | 0.08 | 0.03 | 0.14 |
| 智力 | 0.05 | 0.30 | 0.40 | 0.25 |

16 项权重都在 `FHConfig.SERVER.TOWN.HOUSING` 下配置。运行时把负值视为零并逐行归一化；整行全零时回退该行默认值。

```text
Q_r = sum(normalizedWeight_ri * n_i)
```

生命和精神的营养恢复倍率为：

```text
healthNutrition = 0.25 + 0.75 * Q_health
mentalNutrition = 0.35 + 0.65 * Q_mental
```

它们继续乘既有食物满足度、温度、舒适度和当前属性缺口。营养为零时仍保留 `25%/35%` 基础恢复；四项超过 `75` 没有额外奖励。

## 6. 活动和力量/智力结算

### 6.1 当日活动

`ResidentActivity(physical, learning)` 是当日 transient 状态。建筑只有实际完成工作才记录活动；同日多次工作逐通道取最大值并限制到 `0..1`。采矿、狩猎和货运站的运行时默认活动向量均为 `(1.0, 0.25)`；货运站只有实际建立了非零运力时才记录活动。

年龄提供无需完成工作的基础活动量，实际活动只补足剩余部分：

```text
T = baseActivity(age) + (1 - baseActivity(age)) * actualActivity
```

| 年龄 | 基础活动 `T` | 力量成长率/日 | 智力成长率/日 | 力量/智力成长上限 |
|---|---:|---:|---:|---:|
| 幼儿 | 1.0 | 1.8 | 1.6 | 40 / 40 |
| 儿童 | 0.7 | 3.9 | 4.2 | 80 / 85 |
| 成人 | 0.3 | 0.05 | 0.05 | 100 / 100 |
| 老人 | 0.1 | 0.06 | 0.05 | 100 / 100 |

年龄转换日先使用转换前的年龄参数完成结算，再更新年龄；不会再强制零成长。

### 6.2 统一属性公式

`ResidentAttributeModel.settleDailyAttribute` 是游戏和 Stage 3/4 的共享纯数值入口。对力量或智力 `X`：

```text
E = growthEfficiencyAtZero + (1 - growthEfficiencyAtZero) * Q
growth = growthRate(age) * T * E * max(0, 1 - X / ageGrowthCap)

D = max(0, (maintenanceThreshold - Q) / maintenanceThreshold)
nutritionDecay = decayAtZeroSupport * D^deficiencyExponent * X / 100
ageDecay = elder ? elderAgeDecay : 0

nextX = clamp(X + growth - nutritionDecay - ageDecay, 0, 100)
```

成长上限只停止正向成长，不裁剪已经获得的属性。营养支持低于维护阈值时才开启永久衰减，净变化由成长、营养衰减和老人年龄衰退竞争决定。没有力量下限。

| 配置语义 | 力量 | 智力 |
|---|---:|---:|
| `growthEfficiencyAtZeroSupport` | 0.20 | 0.40 |
| `maintenanceThreshold` | 0.40 | 0.30 |
| `decayAtZeroSupport` | 0.70/日 | 0.17/日 |
| 老人 `ageDecay` | 0.0048/日 | 0.002/日 |

共享 `residentNutritionDeficiencyExponent` 默认 `1.5`。力量权重使只有蛋白质完全缺失、其他三项全满时 `Q_strength=0.25`，低于 `0.40` 维护线；蛋白满足度达到 `0.20` 时恰好回到维护线。智力维护阈值更低、零支持衰减更慢，需要更广泛的严重缺乏才会永久下降。

对应运行时键位于 `FHConfig.SERVER.TOWN.HOUSING`：`residentStrengthGrowthEfficiencyAtZeroSupport`、`residentIntelligenceGrowthEfficiencyAtZeroSupport`、`residentStrengthMaintenanceThreshold`、`residentIntelligenceMaintenanceThreshold`、`residentNutritionDeficiencyExponent`、`residentStrengthDecayAtZeroSupport` 和 `residentIntelligenceDecayAtZeroSupport`。四年龄基础活动、成长率、成长上限和老人固定衰退位于 `FHConfig.SERVER.TOWN.RESIDENT_AGING`。`residentAttributeModelVersion` 在旧的磁盘型服务端配置首次加载时把已有同名成长参数和力量权重迁移到版本 2 默认值，随后只执行一次；远程客户端的内存型配置不执行该迁移，新引入的键由 Forge 配置默认值补齐。

生产、岗位评分和模拟预测直接使用存储的力量与智力。营养不足不再生成单独的“有效智力”；其长期后果通过真实智力的日变化体现。

## 7. 持久化、快照与解释界面

`Resident.CODEC` 和原始 NBT 路径持久保存：

- 四项 `ResidentNutrition`；
- 最近一次 `ResidentNutritionSnapshot`。

旧档兼容行为：

- 旧 `nutritionDevelopment` 字段直接忽略，已有力量、智力和营养不变；
- 缺少快照时界面显示“暂无营养结算数据”。

最近快照只用于解释，不参与下一日结算。它包含餐后四项满足度、四种当前支持、实际活动、生命/精神恢复，以及力量/智力各自的有效 `T`、正向成长、营养衰减、年龄衰退和净变化。

`HouseBuilding.DailyReport.meal` 另外保存住宅最近一个结算日的实际菜单：`settlementDay` 与按 `ItemStackResourceKey` 聚合的 `MealEntry(item, amount)`。物品键保留完整 NBT，数量使用非负有限 `double` 以匹配城镇虚拟仓库。`DailyMeal.hasData=false` 表示尚无菜单快照；`hasData=true` 且条目为空表示该结算日没有出餐。住宅界面的“今日餐食”页直接渲染这些物品；物品格数量四舍五入为整数，悬浮提示显示实际小数数量。数据通过现有建筑快照同步，不增加独立网络包。

住宅居民详情和镇长印章居民详情显示：

- 实际四属性和四项营养；
- 昨日实际活动和力量/智力有效活动量；
- 最近恢复、成长、营养衰减、年龄衰退和净变化；
- 由当前配置权重计算出的两个主要限制通道。

“主要限制”按归一化权重与缺失满足度的乘积排序，因此修改矩阵配置后说明会同步改变。`TownNutritionHistory` 仍保存全镇四项平均和 P10，并使用 `70/20` 健康线和严重线。

## 8. 验证与当前平衡事实

自动测试覆盖：

- 原始数据到百分比画像、空画像和范围限制；
- 玩家旧 NBT 迁移、完整/部分 hunger 增益；
- 熟牛肉与烤马铃薯居民积分；
- 住宅菜单等级、优先级库存递减、住宅内同构成比例和缺口优化；
- 住宅实际菜单 NBT、小数数量和空快照 Codec；
- 权重归一化/全零回退、恢复倍率和主要限制排序；
- 四年龄基础活动、活动最大值、阈值、`D^1.5` 和成长/衰减竞争；
- 三个 15 日成人缺乏样例、幼儿/儿童成长、老人平衡与无力量下限；
- Resident Codec、旧 `nutritionDevelopment` 忽略和扁平结算快照。

2026-08-19 使用上一版 EMA/潜力属性模型执行了两个 `50 residents x 120 days x 100 trials` Stage 4 场景：

| 场景 | 食物潜在自给率 P50 | 出现缺粮的 trials | 生存率 |
|---|---:|---:|---:|
| 纯捕猎 `stage4-t1-p50-quick` | 0.615 | 100% | 0% |
| 每日补给 6.25 个烤马铃薯 | 0.651 | 100% | 6% |

报告位于：

- `build/reports/town-model/simulations/2026-08-19-nutrition-redesign-p50-baseline`
- `build/reports/town-model/simulations/2026-08-19-nutrition-redesign-p50-potato`

这反映每日需求从 `6.5` 提高到 `20` 后，原场景的捕猎产能和马铃薯补给规模不再闭环。它是后续粮食经济调参的基线，不是本次重构的验收失败；本次验收关注公式一致性、范围、确定性和无非有限值。

2026-08-20 在简化属性模型下，以相同 seed 对纯捕猎 50 人基线重新运行 `120 days x 100 trials`。食物潜在自给率 P50 从 `0.6146` 变为 `0.6296`，与取消有效智力生产折扣后产出略升相符；`100%` trials 仍发生缺粮，生存率仍为 `0%`。第 `0/15/30` 日跨 trial 的全镇平均力量 P50 为 `43.35/48.39/47.47`，平均智力 P50 为 `47.69/51.57/50.83`；对应平均存活人口为 `50/48/25.5`。后半程多数 trial 已灭绝，因此末 30 日属性“均衡值”为零，不应用作属性公式校准。

新报告位于 `build/reports/town-model/simulations/2026-08-20-resident-attribute-simplification-p50-baseline`。所有日度指标均为有限值；`summary.json` 中的 `Infinity` 仅表示场景明确配置的无限加工容量，不是营养或属性计算结果。

## 9. 已知边界

- 玩家贫血/最大生命值仍是旧后果的尺度迁移，并非完整重设计；部分注释中的高低营养效果仍未实现。
- 玩家营养只在打开健康菜单时通过容器数据槽同步，没有常驻主动同步。
- 离线模拟器只能读取静态生成 recipe，不能执行依赖实际世界和 NBT 的动态食物事件。
- 当前 50 人供给场景需按每日 `20` 的新需求重新设计生产和外部补给，不能沿用旧 `6.5` 平衡结论。
