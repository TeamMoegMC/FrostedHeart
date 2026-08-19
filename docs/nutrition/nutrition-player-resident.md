# 玩家与居民营养系统现状

- Status: `Current`
- Last verified: `2026-08-19`
- Scope: 玩家营养、居民营养、共享食物数据、结算入口、数值后果、界面与当前已知问题
- Primary code anchors: `NutritionCapability`, `NutritionRecipe`, `HealthCommonEvents`, `ResidentNutrition`, `TownHousingMealService`, `HouseDailyModel`, `TeamTownData.tickMorning`, `TownModelParameters.Defaults`

本文替代最初导入的对话整理，作为下一轮营养设计的实现基线。它描述的是当前源码和附带整合包中的实际行为，不代表这些行为已经平衡或应当保留。

## 1. 一句话结论

目前不是一套营养系统，而是**共享一份四维食物营养数据的两套状态机**：

| 维度 | 玩家 | 城镇居民 |
|---|---|---|
| 状态归属 | 每个 `Player` 的 Forge Capability | 每个 `Resident` 的持久字段 |
| 四项营养 | 脂肪、碳水、蛋白质、蔬菜 | 脂肪、碳水、蛋白质、蔬菜 |
| 数值范围 | 实现允许 `0..100000`，主要玩法刻度围绕 `0..10000` | `0..100`，默认健康线 `70`、严重线 `20` |
| 时间尺度 | 玩家吃东西及原版饥饿值下降时 | 每次城镇早晨结算一次 |
| 食物数量口径 | 原版饥饿值；饱和度只延后下次掉饥饿 | `饥饿值 + 名义饱和度` 的食物资源单位 |
| 主要后果 | 最大生命值、贫血效果 | 健康/精神恢复、属性成长、住宅与口粮优先级 |
| 动态食物支持 | 会触发 `GatherFoodNutritionEvent`，Caupona 汤可动态计算 | 直接读取静态 `NutritionRecipe`，不会触发该事件 |

两者之间没有玩家状态到居民状态的换算，也没有共同的“每日所需营养”模型。它们只共享字段名称和 `frostedheart:diet_override` 食谱数据。

同一进食事件还会运行 `DailyKitchen` 的想吃菜和奖励逻辑，但 `DailyKitchen` 不读写这四项营养，属于相邻而独立的玩家饮食系统，本文不把它并入营养公式。

## 2. 共享食物数据层

### 2.1 `diet_override` 食谱

静态营养数据由 `NutritionRecipe` 读取，类型为 `frostedheart:diet_override`：

```json
{
  "type": "frostedheart:diet_override",
  "group": {
    "fat": 0.0,
    "carbohydrate": 0.0,
    "protein": 24000.0,
    "vegetable": 0.0
  },
  "item": "minecraft:cooked_beef"
}
```

当前生成资源中有 `110` 个此类食谱，位于：

- `src/generated/resources/data/frostedheart/recipes/diet_value/**/*.json`
- 数据源：`src/datagen/resources/data/frostedheart/data/new_food_value.xlsx`
- 生成器：`FHRecipeProvider`、`NutritionRecipeBuilder`

生成器按以下方式归一化：

```text
scale = Base / (Grain + Vegetable + Fat + Protein) * 40000
各通道食谱值 = 对应原始权重 * scale
食谱四通道总值 = Base * 40000
```

因此 `Base` 决定总量，四个分类列只决定构成。原版饥饿值和饱和度没有参与静态营养总量的生成。

### 2.2 两套消费者的读取差异

玩家侧 `NutritionRecipe.getRecipe` 返回第一个匹配食谱，再发布 `GatherFoodNutritionEvent`。Caupona 兼容会在此事件中按汤的内容动态合成营养。

居民侧 `TownHousingMealService.nutrition` 和 `TownFoodNutritionModel.getNutritionPerItem` 直接遍历静态食谱，并把所有匹配项相加。居民配餐不发布 `GatherFoodNutritionEvent`，所以动态汤品与玩家侧可能得到不同结果。

### 2.3 居民食物资格是另一层数据

有营养食谱不等于能被城镇居民食用。物品还必须属于 `town_resource_resident_food_level_0..4` 之一并进入城镇仓库。

当前附带整合包源码只找到 `frostedheart:town_resource_resident_food_level_0` 的 KubeJS Tag，包含 Caupona、Create、Stone Age、Supplementaries 的部分食物；没有找到等级 `1..4` 的定义。代码支持五级优先级，但在当前整合包配置中这一层级机制基本未展开。

## 3. 玩家营养系统

### 3.1 状态与生命周期

`HealthCommonEvents.attachToPlayer` 给非 FakePlayer 附加 `FHCapabilities.PLAYER_NUTRITION`。`NutritionCapability` 保存四个 `float`：

| 通道 | NBT 字段 | 初始值 |
|---|---|---:|
| 脂肪 | `fat` | 7000 |
| 碳水 | `carbohydrate` | 7000 |
| 蛋白质 | `protein` | 7000 |
| 蔬菜 | `vegetable` | 7000 |

正常读档、进食和消耗会把各项限制到 `0..100000`。玩家死亡克隆时四项数值被复制，因此死亡不会重置营养。

`/frostedheart nutrition <channel> get|add|set|fill` 等管理员命令可以检查或修改数值。`fill` 写入 `10000`；`add` 和 `set` 本身不立即执行范围校验，之后的正常进食、消耗或重载才会重新限制。

### 3.2 进食增加

`LivingEntityUseItemEvent.Finish` 处理器调用：

```text
NutritionCapability.eat(player, itemStack)
```

对每个通道 `x`：

```text
增加量_x = 食谱值_x * 食物原版饥饿值 * nutritionGainRate
```

源码默认：

```text
nutritionGainRate = 0.0025
```

例如熟牛肉的蛋白质食谱值为 `24000`、原版饥饿值为 `8`，一次完整进食增加：

```text
24000 * 8 * 0.0025 = 480 蛋白质
```

食物没有静态食谱且没有兼容事件补充营养时，不改变玩家营养。

### 3.3 饥饿下降时消耗

每个服务端玩家 tick 的 END 阶段，代码比较 `FoodData.lastFoodLevel` 与当前 `foodLevel`。只有原版饥饿值实际下降时才消耗营养；单独损失饱和度不会立即消耗。

若本次下降 `d` 点饥饿值，则每个通道按当前值同比例衰减：

```text
新值_x = 旧值_x * (1 - d * nutritionConsumptionRate)
```

源码默认：

```text
nutritionConsumptionRate = 0.0025
```

这不是每点扣固定数值。当前池越高，同一次掉饥饿扣得越多；四项的相对构成在纯消耗时不变。

### 3.4 过量进食

玩家吃下的饥饿值超过当前空缺时，超出部分会先按上面的比例消耗当前四项营养，然后再加入整份食物的营养。也就是说，吃满或溢出并不会只截断食物收益，而会触发一次额外的当前营养同比例衰减。

### 3.5 “营养效率”应如何理解

旧文档使用的：

```text
食谱四项总和 / 食物饥饿值
```

**不对应当前玩家增益公式**，因为实现已经把食谱值乘了一次食物饥饿值。继续除以饥饿值会把它错误地抵消两次。

当前可直接从静态数据计算的口径是：

```text
单件食物总增益 = 食谱四项总和 * 饥饿值 * nutritionGainRate
每点恢复饥饿的增益 = 食谱四项总和 * nutritionGainRate
```

但这仍不是完整的长期效率：饱和度会影响下一次饥饿下降的时间，过量进食会额外衰减，当前池值又决定每次消耗的绝对量。若要比较长期饮食，应该模拟固定活动量下的稳态和缺乏/过量时间，而不是再做一个简单的静态除法。

食物 Tooltip 中的“最大营养维持”使用：

```text
食谱值_x * nutritionGainRate / nutritionConsumptionRate / 10000
```

它表达的是在 `10000` 池值附近，每恢复/消耗一饥饿点时该食物对某通道的维持比例，不是整件食物实际增加的百分比。

### 3.6 对玩家的实际影响

非创造/旁观玩家每 `200` 个服务端 tick 更新最大生命值；同一时机还会检查贫血惩罚，但和平难度会跳过贫血。

最大生命值方面，每个通道独立贡献约 `-5..+5` 点：

- `0` 时贡献 `-5`；
- `3000..7000` 是中性区，贡献 `0`；
- `10000` 及以上贡献 `+5`；
- 四通道相加并四舍五入，理论修正范围为 `-20..+20` 最大生命值。

贫血方面，当前启用的条件只有：

- 蛋白质 `< 2000`：记 2 点；
- 蔬菜 `< 2000`：记 2 点；
- 脂肪 `> 10000`：记 1 点；
- 蛋白质 `> 10000`：记 1 点。

点数先做整数除以 `2`，大于零才施加持续 `200` tick 的 `FHMobEffects.ANEMIA`。低脂肪、低碳水、高碳水和坏血病相关代码目前被注释；`punishment()` 仍有 TODO。因此玩家营养后果是部分实现，并非完整的四通道缺乏/过量模型。

### 3.7 玩家界面与同步

- 按“打开饮食均衡”键会请求服务端打开 `HealthStatMenu`。
- 菜单用数据槽同步四项当前值，并以 `值 / 10000` 显示环形条。
- 食物 Tooltip 按住 `N` 显示食物营养构成和维持比例。
- `PlayerNutritionSyncPacket` 类仍存在，但网络注册和 `callOnChange` 主动同步均被注释。当前可靠同步路径是打开菜单后的容器数据槽，不是持续推送的独立营养包。
- 玩家 capability 同时附加在逻辑服务端和客户端；进食事件没有逻辑侧限制，因此两侧都可能执行本地增益。饥饿下降消耗、惩罚、属性更新和存档权威状态在服务端执行。

### 3.8 玩家侧当前风险

1. **尺度不统一**：存储上限是 `100000`，界面、属性高值线、命令 `fill` 和 Tooltip 基准都围绕 `10000`。
2. **惩罚不对称**：四通道只有部分高低阈值实际生效，碳水几乎只影响最大生命值。
3. **过量进食语义特殊**：溢出饥饿会衰减已有营养，再加入整份营养，玩家很难从界面推断。
4. **缺少专门测试**：当前测试集中没有覆盖 `NutritionCapability` 的玩家增益、消耗、惩罚和溢出路径。
5. **静态表不能代表汤品**：Caupona 动态营养通过事件生成，不在 110 个 JSON 的简单排行榜中。

## 4. 城镇居民营养系统

### 4.1 当前不是“每日质量分”而是个人持久储备

每个 `Resident` 持有一个 `ResidentNutrition`：

```text
fat, carbohydrate, protein, vegetable
```

四项都持久化并随居民同步。源码默认：

| 配置锚点 | 默认值 | 含义 |
|---|---:|---|
| `residentNutritionMaximumReserve` | 100 | 单项上限 |
| `residentNutritionInitialReserve` | 70 | 新居民初值 |
| `residentNutritionHealthyReserve` | 70 | 满可用度/正常成长基准 |
| `residentNutritionSevereReserve` | 20 | 严重缺乏线 |
| `residentNutritionReserveLossPerDay` | 10 | 每日每项先扣除 |
| `residentNutritionGainAtReference` | 10 | 一份参考餐的单项恢复 |
| `residentNutritionMaximumCoverage` | 2 | 单日单项最多按两份参考餐计入 |

旧存档没有营养字段时回退到每项 `70`。新居民读取运行时配置的初值和上限。

### 4.2 每日结算顺序

实际入口为：

```text
ClimateCommonEvents
  -> TeamTownData.tickMorning()
     -> 所有居民四项储备先衰减
     -> 住宅重分配
     -> 工作分配、生产与加工
     -> TownHousingMealService.settle()
     -> 居民成长
     -> 无家可归惩罚与死亡/离开判定
     -> 记录每日历史
```

`TeamTownData.buildingsWork()` 明确跳过 `HouseBuilding`。因此 `HouseBuilding.work()` 中旧的整栋住宅统一消费路径仍保留在源码里，但当前早晨结算使用的是 `TownHousingMealService` 的中心化、逐居民配餐路径。

无家可归居民也会先损失每日营养，但不在住宅 household 中，不会获得这次城镇配餐；之后还会接受无家可归健康惩罚。

### 4.3 食物资源单位与口粮

默认每名居民每天需要：

```text
foodConsumptionPerResidentDay = 6.5 食物资源单位
```

没有显式资源配方覆盖时，一件食物提供：

```text
食物资源单位 = hunger + hunger * saturationModifier * 2
```

例如熟牛肉为 `8 + 8 * 0.8 * 2 = 20.8` 单位。城镇支持小数件消耗，因此一名居民的一餐可以只消耗一件物品的一部分。

口粮先按住宅计划中的“保障人数”和当前照护法则分配保障餐，再把余粮在未保障居民间等分。照护排序会考虑：

- 健康风险；
- 精神风险；
- 四项营养中的最低储备；
- 低于严重线的通道数；
- 当前 `TownCareLaw` 对依赖人口或劳动力的偏好。

### 4.4 逐居民选餐

每位居民的默认完整口粮被拆成 `residentNutritionMealSelectionChunks = 8` 次选择。每次先选择更高的 `RESIDENT_FOOD_LEVEL`，同级内再根据个人需求计算效用：

- 缺哪个通道，就提高对应食物的价值；
- 精神较低时，在碳水仍有缺口的前提下提高碳水价值；
- 健康较低时，在蔬菜仍有缺口的前提下提高蔬菜价值；
- 非老人提高脂肪对智力成长的权重；
- 幼儿和儿童提高蛋白质对力量成长的权重。

当前附带整合包只定义了食物等级 `0`，所以实际选择主要由个人营养效用和稳定 ID 排序决定，而不是跨等级品质。

### 4.5 一餐如何转为储备

当日先衰减，吃完后每个通道独立增加：

```text
referencePerChannel = 完整口粮单位 * nutritionReferencePerFoodUnit
coverage_x = clamp(当餐食谱摄入_x / referencePerChannel, 0, maximumCoverage)
增加_x = residentNutritionGainAtReference * coverage_x
```

按源码默认：

```text
referencePerChannel = 6.5 * 7000 = 45500
增加_x = 10 * clamp(当餐摄入_x / 45500, 0, 2)
```

要抵消默认每日 `10` 点损失，一个通道每天需要吃到 `45500` 的该通道原始食谱值。四项分别计算，不要求一餐总值平均分配。

### 4.6 对健康、精神和成长的影响

每项可用度为：

```text
availability_x = clamp(储备_x / healthyReserve, 0, 1)
```

精神恢复以碳水为直接通道，健康恢复以蔬菜为直接通道；脂肪与蛋白质只是支撑项：

```text
support = (fatAvailability + proteinAvailability) / 2
supplied = 0.6 * direct + 0.4 * direct * support
recoveryMultiplier = 0.5 + 0.5 * supplied
```

其中 `0.5`、`0.6`、`0.4` 都是默认配置。直接通道为零时，支撑项不能单独恢复该效果，但仍保留最低 `0.5` 恢复倍率。

最终恢复还会乘以食物满足度、温度/舒适度和当前健康或精神缺口。缺粮与极端温度另有独立惩罚，所以“有营养”不能抵消没有热量或恶劣住房造成的直接损失。

成长方面：

- 蛋白质缩放幼儿、儿童的力量增长；
- 脂肪缩放幼儿、儿童和成人的智力增长；
- `0` 储备默认仍有 `0.5x` 增长，`70` 为 `1x`，`100` 为 `1.25x`；
- 营养不改变年龄天数和年龄阶段转换；
- 成人力量增长和老人力量衰退不使用营养倍率。

### 4.7 观测与界面

- 住宅居民详情和镇长印章劳动力详情显示每名居民四项 `0..100` 数值。
- 每次结算后，`TownNutritionHistory` 保存四项的全镇平均值和 P10 弱势人口值。
- 镇长印章统计页显示四项历史图，并绘制健康线 `70`、严重线 `20`。
- 严重缺乏会进入居民照护风险与排序，但不会像玩家一样直接施加某个药水效果。

### 4.8 仍保留的住宅聚合营养质量

`HouseBuilding.DailyReport` 仍保存：

```text
nutritionQuality
  = clamp((住宅食谱总值/4) / 已消费食物单位 / nutritionReferencePerFoodUnit, 0, 1)
```

住宅概览仍显示这个“营养质量”。但中心化结算对每位居民调用 `settleResident()` 时，健康和精神恢复使用的是该居民四通道储备计算出的两个倍率，不使用这项聚合 `nutritionRecoveryMultiplier`。

因此它现在主要是兼容旧报告和住宅概览的汇总指标，不是居民营养状态的权威来源。下一轮设计必须决定保留、重定义还是移除它，否则界面会同时呈现两种“营养好坏”。

### 4.9 当前平衡状态

源码默认仍是：

```text
nutritionReferencePerFoodUnit = 7000
reserveLossPerDay = 10
gainAtReference = 10
```

2026-08-17 的 P50 狩猎肉食模拟中，脂肪、碳水、蔬菜在第 `5` 天全部跌破严重线，蛋白质多数也在第 `5..6` 天跌破；最后 30 天中位数约为脂肪 `0.301`、碳水 `0`、蛋白质 `1.838`、蔬菜 `0`。这说明当前默认值与肉食供应不构成可持续平衡。

实验参数 `reference=200, loss=1, gain=2` 加每日烤马铃薯补给曾在 P50 模拟中维持四项，但这些值**没有**写入 `TownModelParameters.Defaults` 或 `FHConfig`，也不是当前玩法默认。

## 5. 两套系统为何显得复杂

复杂度来自五个重叠但不一致的口径：

1. 同一份食谱数据，在玩家侧是“每饥饿点即时增益”，在居民侧是“每日个人配餐原始摄入”。
2. 玩家和居民都叫四项营养，但数值尺度分别围绕 `10000` 与 `100`。
3. 玩家使用动态 `GatherFoodNutritionEvent`，居民只读静态食谱。
4. 居民同时存在个人四通道储备和住宅聚合 `nutritionQuality` 两种可见指标。
5. 城镇食物还有独立的资格 Tag、食物等级、热量/饱和度资源单位和个人配餐效用，不是只有营养 JSON。

可以把当前依赖关系概括为：

```text
new_food_value.xlsx
  -> 110 个 diet_override JSON
     -> 玩家：事件修正 -> 按饥饿值即时增益 -> 玩家四池 -> 生命值/贫血
     -> 居民：静态食谱读取 -> 仓库食物资格与资源单位 -> 逐居民配餐
              -> 居民四储备 -> 恢复/成长/照护排序/历史统计
              -> 同时生成住宅聚合 nutritionQuality（报告兼容层）
```

## 6. 下一轮设计前应先决定的边界

本文不替下一轮设计作决定，但以下问题会直接改变数据结构和公式，应优先明确：

1. 玩家与居民是否应该共享同一数值尺度和同一“每日需求”语义，还是只共享食物分类？
2. 食谱值究竟表示食物的绝对营养、每饥饿点营养，还是长期稳态目标？
3. 玩家营养是鼓励均衡、惩罚缺乏，还是允许高储备带来正向成长？当前最大生命值机制同时奖励高值并惩罚部分过量。
4. 居民的个人储备是否已经足够；住宅聚合 `nutritionQuality` 是否应删除或改成可解释的餐食摘要？
5. 动态食物（尤其 Caupona 汤）是否必须在玩家和居民两侧得到一致结果？
6. 城镇食物等级是否仍有设计价值；如果有，整合包需要补齐等级 `1..4`，如果没有，应简化选择链。
7. 营养缺乏应直接造成损失，还是只降低恢复和成长？目前玩家与居民采取了不同答案。
8. 平衡目标应以单件食物、每点饥饿、每城镇日，还是固定活动量下的长期稳态为准？

## 7. 权威代码与数据锚点

玩家侧：

- `src/main/java/com/teammoeg/frostedheart/content/health/capability/NutritionCapability.java`
- `src/main/java/com/teammoeg/frostedheart/content/health/handler/HealthCommonEvents.java`
- `src/main/java/com/teammoeg/frostedheart/content/health/recipe/NutritionRecipe.java`
- `src/main/java/com/teammoeg/frostedheart/content/health/tooltip/FoodNutritionStats.java`
- `src/main/java/com/teammoeg/frostedheart/content/health/screen/HealthStatMenu.java`
- `src/main/java/com/teammoeg/frostedheart/infrastructure/command/NutritionCommand.java`

居民侧：

- `src/main/java/com/teammoeg/frostedheart/content/town/resident/ResidentNutrition.java`
- `src/main/java/com/teammoeg/frostedheart/content/town/buildings/house/TownHousingMealService.java`
- `src/main/java/com/teammoeg/frostedheart/content/town/buildings/house/HouseDailyModel.java`
- `src/main/java/com/teammoeg/frostedheart/content/town/buildings/house/HouseBuilding.java`
- `src/main/java/com/teammoeg/frostedheart/content/town/TeamTownData.java`
- `src/main/java/com/teammoeg/frostedheart/content/town/model/TownResidentCareModel.java`
- `src/main/java/com/teammoeg/frostedheart/content/town/resident/ResidentAgingModel.java`
- `src/main/java/com/teammoeg/frostedheart/content/town/observation/TownNutritionHistory.java`
- `src/main/java/com/teammoeg/frostedheart/content/town/resource/TownFoodResourceAmount.java`
- `src/main/java/com/teammoeg/frostedheart/content/town/model/TownModelParameters.java`
- `src/main/java/com/teammoeg/frostedheart/infrastructure/config/FHConfig.java`

数据与整合包：

- `src/datagen/resources/data/frostedheart/data/new_food_value.xlsx`
- `src/generated/resources/data/frostedheart/recipes/diet_value/**/*.json`
- 附带整合包：`kubejs/server_scripts/src/tags/item_tags.js`

更完整的居民公式、住宅、照护法则、模拟与历史观测说明见 `docs/town/town-model.md`。
