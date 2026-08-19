# 玩家营养系统对话记录

> 本文件整理自一次关于 Frosted Heart 玩家营养系统、城镇居民营养系统以及食物营养效率的讨论。

## 目录

1. 玩家营养系统如何实现和运行
2. 城镇居民是否有实际投入使用的营养系统
3. Frosted Heart 中的食物营养数据
4. 玩家营养扣除时机
5. 营养效率口径：总营养 / 饱食度
6. 完整食物营养效率表（含中文名）
7. 营养数据中看起来不合理的地方
8. 关于“总营养 / (饱食度 + 饱和度)”的纠正

---

## 1. 玩家营养系统如何实现和运行

### 1.1 数据模型

玩家营养使用 Forge Capability 实现：

- 类：`NutritionCapability`
- 注册：`FHCapabilities.PLAYER_NUTRITION`
- 附加：`HealthCommonEvents.attachToPlayer`

每个玩家存储四项营养：

| 字段 | 含义 |
|---|---|
| `fat` | 脂肪 |
| `carbohydrate` | 碳水/谷物 |
| `protein` | 蛋白质 |
| `vegetable` | 蔬菜/维生素 |

默认每项为 `7000`，范围为 `[0, 100000]`，通过 NBT 随玩家存档保存。

### 1.2 食物营养配方

食物营养由数据包配方 `frostedheart:diet_override` 定义：

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

- 配方类：`NutritionRecipe`
- 注册位置：`FHRecipes`
- 生成来源：`new_food_value.xlsx`
- 查询后还会触发 `GatherFoodNutritionEvent`，允许 Caupona 等 mod 修改营养。

### 1.3 增加营养

玩家吃完食物时：

```text
LivingEntityUseItemEvent.Finish
  → NutritionCapability.eat(player, stack)
```

增加量：

```text
增加量 = 食物配方营养 × 食物原版饥饿值 × nutritionGainRate
```

默认 `nutritionGainRate = 0.0025`。

### 1.4 扣除营养

服务端每 tick 检查：

```text
如果本 tick 原版饥饿值下降，则扣除营养
```

扣除量：

```text
营养扣除 = 当前营养 × 下降饥饿点数 × nutritionConsumptionRate
```

默认 `nutritionConsumptionRate = 0.0025`。

另外，吃东西溢出到满饱食度空位时，也会扣除溢出部分的营养。

### 1.5 营养对玩家的影响

- 通过 `addAttributes()` 修改最大生命值
- 通过 `punishment()` 在营养过低/过高时给予贫血等负面效果
- 当前惩罚逻辑仍有 TODO，尚未完全完善

---

## 2. 城镇居民是否有实际投入使用的营养系统

**有，且已经接入实际每日城镇结算。**

### 2.1 运行入口

```text
ClimateCommonEvents
  → TeamTownData.tickMorning()
  → buildingsWork()
  → HouseBuilding.work()
```

`HouseBuilding.work()` 会：

1. 计算当天所需食物
2. 从城镇仓库消耗食物
3. 累计消耗食物的营养值
4. 用 `HouseDailyModel.evaluateSettlement()` 计算营养质量
5. 更新居民健康/精神

### 2.2 居民营养不是玩家四项营养池

城镇居民没有像玩家那样的持久营养池，而是每日结算食物质量：

```text
营养质量 = (营养总价值 / 实际消耗食物单位) / nutritionReferencePerFoodUnit
```

默认 `nutritionReferencePerFoodUnit = 7000`。

```text
营养恢复倍率 = minimumNutritionRecoveryMultiplier
              + (1 - minimumNutritionRecoveryMultiplier) × 营养质量
```

默认最低恢复倍率 `0.5`。

### 2.3 关键代码

- `HouseBuilding.java`
- `HouseDailyModel.java`
- `TownFoodNutritionModel.java`
- `TownFoodInventoryModel.java`
- `TeamTownResourceActionExecutorHandler.java`

---

## 3. Frosted Heart 中的食物营养数据

有，共 110 个 `diet_value` JSON 配方：

- 位置：`src/generated/resources/data/frostedheart/recipes/diet_value/**/*.json`
- 数据源：`src/datagen/resources/data/frostedheart/data/new_food_value.xlsx`
- 字段：`id / name / heal / sat / Base / Grain / Vegetable / Fat / Protein`

---

## 4. 玩家营养扣除时机

### 4.1 每 tick：饥饿值下降时

```java
// Phase.START
nutrition.calculatedFoodLevel = player.getFoodData().getFoodLevel();

// Phase.END
nutrition.consume(player);
```

```java
if (fd.getLastFoodLevel() > fd.getFoodLevel()) {
    consume(fd.getLastFoodLevel() - fd.getFoodLevel());
}
```

只有原版饥饿值下降才会触发营养扣除。

### 4.2 吃东西溢出时

```java
int filling = 20 - calculatedFoodLevel;
if (filling < nutrition) {
    consume(nutrition - filling);
}
```

### 4.3 饱和度下降不扣营养

当前实现只看 `getLastFoodLevel() > getFoodLevel()`，不检查饱和度，因此：

- 只消耗饱和度、饥饿值不掉 → 不扣营养
- 饥饿值下降 → 扣营养

---

## 5. 营养效率口径：总营养 / 饱食度

在当前“只有饱食度下降才扣营养”的规则下，正确的玩家营养效率是：

```text
营养效率 = 所有营养总和 / 饱食度
```

其中：

```text
所有营养总和 = fat + carbohydrate + protein + vegetable
```

`总营养 / (饱食度 + 饱和度)` 是城镇食物资源/居民营养质量口径，不适合直接用于玩家营养效率。

---

## 6. 完整食物营养效率表（含中文名）

按 `总营养 / 饱食度` 排序：

| 效率 | 中文名 | 食物ID | 总营养 | 饱食度 |
|---:|---|---|---:|---:|
| 40000.0 | 煎饼 | supplementaries:pancake | 40000 | 1 |
| 20000.0 | 枸杞子 | frostedheart:dried_wolfberries | 20000 | 1 |
| 20000.0 | 甜菜根 | minecraft:beetroot | 20000 | 1 |
| 20000.0 | 蛋糕 | minecraft:cake | 40000 | 2 |
| 20000.0 | 煮熟的脂肪 | stone_age:cooked_fat | 40000 | 2 |
| 20000.0 | 脂肪 | stone_age:fat | 20000 | 1 |
| 20000.0 | 糖果 | supplementaries:candy | 20000 | 1 |
| 16000.0 | 马铃薯 | minecraft:potato | 16000 | 1 |
| 14000.0 | 曲奇 | minecraft:cookie | 28000 | 2 |
| 12000.0 | 枸杞叶 | caupona:fresh_wolfberry_leaves | 24000 | 2 |
| 12000.0 | 蜗牛 | caupona:snail | 24000 | 2 |
| 12000.0 | 绒球葱 | minecraft:allium | 12000 | 1 |
| 12000.0 | 干海带 | minecraft:dried_kelp | 12000 | 1 |
| 12000.0 | 蜜脾 | minecraft:honeycomb | 24000 | 2 |
| 10000.0 | 每日坚果 | frostedheart:packed_nuts | 20000 | 2 |
| 10000.0 | 西瓜片 | minecraft:melon_slice | 20000 | 2 |
| 9333.3 | 肥满蜗牛 | caupona:plump_snail | 28000 | 3 |
| 8000.0 | 蜗牛卵 | caupona:snail_block | 24000 | 3 |
| 8000.0 | 鱿鱼须 | frostedheart:squid_tentacles | 16000 | 2 |
| 8000.0 | 胡萝卜 | minecraft:carrot | 24000 | 3 |
| 8000.0 | 海带 | minecraft:kelp | 8000 | 1 |
| 8000.0 | 河豚 | minecraft:pufferfish | 8000 | 1 |
| 8000.0 | 小麦 | minecraft:wheat | 24000 | 3 |
| 6666.7 | 雕刻南瓜 | minecraft:carved_pumpkin | 20000 | 3 |
| 6666.7 | 鸡蛋 | minecraft:egg | 20000 | 3 |
| 6666.7 | 小麦种子 | minecraft:wheat_seeds | 20000 | 3 |
| 6000.0 | 无花果 | caupona:fig | 24000 | 4 |
| 6000.0 | 核桃 | caupona:walnut | 24000 | 4 |
| 6000.0 | 枸杞 | caupona:wolfberries | 24000 | 4 |
| 6000.0 | 冻干蔬菜包 | frostedheart:dried_vegetables | 24000 | 4 |
| 6000.0 | 苹果 | minecraft:apple | 24000 | 4 |
| 6000.0 | 生鳕鱼 | minecraft:cod | 12000 | 2 |
| 6000.0 | 附魔金苹果 | minecraft:enchanted_golden_apple | 24000 | 4 |
| 6000.0 | 蕨 | minecraft:fern | 12000 | 2 |
| 6000.0 | 金苹果 | minecraft:golden_apple | 24000 | 4 |
| 6000.0 | 大型蕨 | minecraft:large_fern | 12000 | 2 |
| 6000.0 | 生羊肉 | minecraft:mutton | 12000 | 2 |
| 6000.0 | 毒马铃薯 | minecraft:poisonous_potato | 12000 | 2 |
| 5600.0 | 压缩饼干袋 | frostedheart:compressed_biscuits_pack | 28000 | 5 |
| 5333.3 | 棕色蘑菇 | minecraft:brown_mushroom | 16000 | 3 |
| 5333.3 | 南瓜 | minecraft:pumpkin | 16000 | 3 |
| 5333.3 | 红色蘑菇 | minecraft:red_mushroom | 16000 | 3 |
| 5000.0 | 巧克力? | frostedheart:chocolate | 20000 | 4 |
| 4800.0 | 奶酪 | charcoal_pit:cheese | 24000 | 5 |
| 4800.0 | 烤马铃薯 | minecraft:baked_potato | 24000 | 5 |
| 4666.7 | 军用口粮MR19937 | frostedheart:military_rations | 28000 | 6 |
| 4666.7 | 甜甜卷 | create:sweet_roll | 28000 | 6 |
| 4400.0 | 压缩饼干 | frostedheart:compressed_biscuits | 22000 | 5 |
| 4000.0 | 蜜渍苹果 | create:honeyed_apple | 32000 | 8 |
| 4000.0 | 巧克力棒 | create:bar_of_chocolate | 24000 | 6 |
| 4000.0 | 黑面包 | frostedheart:black_bread | 20000 | 5 |
| 4000.0 | 白萝卜 | frostedheart:white_turnip_block | 12000 | 3 |
| 4000.0 | 生牛肉 | minecraft:beef | 12000 | 3 |
| 4000.0 | 甜菜汤 | minecraft:beetroot_soup | 24000 | 6 |
| 4000.0 | 骨头 | minecraft:bone | 4000 | 1 |
| 4000.0 | 面包 | minecraft:bread | 20000 | 5 |
| 4000.0 | 熟鸡肉 | minecraft:cooked_chicken | 24000 | 6 |
| 4000.0 | 熟羊肉 | minecraft:cooked_mutton | 24000 | 6 |
| 4000.0 | 发光浆果 | minecraft:glow_berries | 8000 | 2 |
| 4000.0 | 蜂蜜瓶 | minecraft:honey_bottle | 24000 | 6 |
| 4000.0 | 生猪排 | minecraft:porkchop | 12000 | 3 |
| 4000.0 | 甜浆果 | minecraft:sweet_berries | 8000 | 2 |
| 4000.0 | 热带鱼 | minecraft:tropical_fish | 4000 | 1 |
| 4000.0 | 烤野羊肉 | stone_age:cooked_mouflon_meat | 12000 | 3 |
| 4000.0 | 烤鹿肉 | stone_age:cooked_venison | 12000 | 3 |
| 4000.0 | 鹿肉 | stone_age:venison | 4000 | 1 |
| 3500.0 | 南瓜派 | minecraft:pumpkin_pie | 28000 | 8 |
| 3428.6 | 巧克力包层浆果 | create:chocolate_glazed_berries | 24000 | 7 |
| 3333.3 | 生黑麦面包 | frostedheart:raw_rye_bread | 20000 | 6 |
| 3333.3 | 黑麦面包 | frostedheart:rye_bread | 20000 | 6 |
| 3333.3 | 金胡萝卜 | minecraft:golden_carrot | 20000 | 6 |
| 3200.0 | 熟鱿鱼须 | frostedheart:cooked_squid_tentacles | 16000 | 5 |
| 3000.0 | 牛排 | minecraft:cooked_beef | 24000 | 8 |
| 3000.0 | 熟猪排 | minecraft:cooked_porkchop | 24000 | 8 |
| 3000.0 | 烤野鸡肉 | stone_age:cooked_fowl_meat | 12000 | 4 |
| 2666.7 | 熟狐狸肉 | frostedheart:cooked_fox_meat | 16000 | 6 |
| 2666.7 | 熟狼肉 | frostedheart:cooked_wolf_meat | 16000 | 6 |
| 2666.7 | 黑麦粥 | frostedheart:rye_porridge | 16000 | 6 |
| 2666.7 | 蔬菜汤 | frostedheart:vegetable_soup | 16000 | 6 |
| 2666.7 | 蘑菇煲 | minecraft:mushroom_stew | 16000 | 6 |
| 2666.7 | 谜之炖菜 | minecraft:suspicious_stew | 16000 | 6 |
| 2500.0 | 熟北极熊肉 | frostedheart:cooked_polar_bear_meat | 20000 | 8 |
| 2400.0 | 熟鳕鱼 | minecraft:cooked_cod | 12000 | 5 |
| 2400.0 | 熟兔肉 | minecraft:cooked_rabbit | 12000 | 5 |
| 2400.0 | 烤野牛肉 | stone_age:cooked_auroch_meat | 12000 | 5 |
| 2000.0 | 熟鲸肉 | frostedheart:cooked_whale_meat | 16000 | 8 |
| 2000.0 | 狐狸肉 | frostedheart:fox_meat | 4000 | 2 |
| 2000.0 | 狼肉 | frostedheart:wolf_meat | 4000 | 2 |
| 2000.0 | 生鸡肉 | minecraft:chicken | 4000 | 2 |
| 2000.0 | 熟鲑鱼 | minecraft:cooked_salmon | 12000 | 6 |
| 2000.0 | 生鲑鱼 | minecraft:salmon | 4000 | 2 |
| 2000.0 | 蜘蛛眼 | minecraft:spider_eye | 4000 | 2 |
| 2000.0 | 野牛肉 | stone_age:auroch_meat | 4000 | 2 |
| 2000.0 | 野鸡肉 | stone_age:fowl_meat | 4000 | 2 |
| 1714.3 | 烤野猪肉 | stone_age:cooked_boar_meat | 12000 | 7 |
| 1500.0 | 烤猛犸象肉 | stone_age:cooked_mammoth_meat | 12000 | 8 |
| 1333.3 | 北极熊肉 | frostedheart:polar_bear_meat | 4000 | 3 |
| 1333.3 | 生兔肉 | minecraft:rabbit | 4000 | 3 |
| 1333.3 | 野猪肉 | stone_age:boar_meat | 4000 | 3 |
| 1333.3 | 烤犀牛肉 | stone_age:cooked_rhino_meat | 12000 | 9 |
| 1333.3 | 野羊肉 | stone_age:mouflon_meat | 4000 | 3 |
| 1200.0 | 兔肉煲 | minecraft:rabbit_stew | 12000 | 10 |
| 1200.0 | 烤剑齿虎肉 | stone_age:cooked_tiger_meat | 12000 | 10 |
| 1000.0 | 生鲸肉 | frostedheart:raw_whale_meat | 4000 | 4 |
| 1000.0 | 腐肉 | minecraft:rotten_flesh | 4000 | 4 |
| 1000.0 | 猛犸象肉 | stone_age:mammoth_meat | 4000 | 4 |
| 1000.0 | 犀牛肉 | stone_age:rhino_meat | 4000 | 4 |
| 800.0 | 剑齿虎肉 | stone_age:tiger_meat | 4000 | 5 |
| 666.7 | 锯末黑麦粥 | frostedheart:rye_sawdust_porridge | 4000 | 6 |
| 666.7 | 锯末蔬菜汤 | frostedheart:vegetable_sawdust_soup | 4000 | 6 |

> 注：`frostedheart:chocolate` 的中文名来自原表，显示为“巧克力?”。

---

## 7. 营养数据中看起来不合理的地方

### 7.1 小份低饱食度食物效率爆炸

| 食物 | 饱食度 | 总营养 | 效率 |
|---|---:|---:|---:|
| 煎饼 | 1 | 40000 | 40000 |
| 蛋糕 | 2 | 40000 | 20000 |
| 甜菜根 | 1 | 20000 | 20000 |
| 马铃薯 | 1 | 16000 | 16000 |
| 曲奇 | 2 | 28000 | 14000 |

对比：

| 食物 | 饱食度 | 总营养 | 效率 |
|---|---:|---:|---:|
| 熟牛肉 | 8 | 24000 | 3000 |
| 军用口粮 | 6 | 28000 | 4667 |
| 兔肉煲 | 10 | 12000 | 1200 |

### 7.2 烹饪反而降低“每饱食度营养效率”

| 食物 | 生 | 熟 |
|---|---:|---:|
| 牛肉 | 4000 | 3000 |
| 羊肉 | 6000 | 4000 |
| 鳕鱼 | 6000 | 2400 |
| 猪肉 | 4000 | 3000 |

### 7.3 非食物/基础原料营养过高

- 绒球葱：12000
- 蜜脾：12000
- 蕨/大型蕨：6000
- 骨头：4000
- 毒马铃薯：6000
- 蜘蛛眼：2000
- 腐肉：1000

### 7.4 越高级食物营养效率反而越低

| 食物 | 效率 |
|---|---:|
| 煎饼 | 40000 |
| 甜菜根 | 20000 |
| 金苹果 | 6000 |
| 军用口粮 | 4667 |
| 金胡萝卜 | 3333 |
| 兔肉煲 | 1200 |
| 烤剑齿虎肉 | 1200 |

### 7.5 数据生成公式未考虑平衡

`FHRecipeProvider` 生成逻辑：

```java
dvb.based(base / (Grain + Vegetable + Fat + Protein) * 40000);
dvb.nutrition(Grain, Vegetable, Fat, Protein);
```

没有把 `heal` 和 `sat` 纳入归一化，导致最高效率 40000 与最低 667 相差约 60 倍。

---

## 8. 关于“总营养 / (饱食度 + 饱和度)”的纠正

### 8.1 正确口径

当前玩家营养扣除只发生在饱食度下降时，因此：

```text
营养效率 = 总营养 / 饱食度
```

这是与当前机制直接对应的效率。

### 8.2 为什么不推荐用 /(饱食度 + 饱和度)

`总营养 / (饱食度 + 饱和度)` 会把高饱和食物评价得更低：

```text
食物A：饱食度 4，饱和度 0，总营养 24000
食物B：饱食度 4，饱和度 8，总营养 24000

按 总营养/饱食度：
A = 6000
B = 6000

按 总营养/(饱食度+饱和度)：
A = 6000
B = 24000 / 12 = 2000
```

但实际游玩中，B 的饱和度更高，饥饿值下降更慢，营养消耗会被延后，因此 B 不应被评价得比 A 低。

### 8.3 如果想把饱和度收益算进去

不能简单放在分母里，更适合：

```text
营养效率 = 总营养 / 饱食度 + 饱和度收益修正
```

或者通过模拟“固定活动量下营养下降多少”来评估，而不是直接从数据表公式算出。

---

## 相关代码位置

- 玩家营养 Capability：`src/main/java/com/teammoeg/frostedheart/content/health/capability/NutritionCapability.java`
- 营养配方：`src/main/java/com/teammoeg/frostedheart/content/health/recipe/NutritionRecipe.java`
- 玩家营养事件：`src/main/java/com/teammoeg/frostedheart/content/health/handler/HealthCommonEvents.java`
- 居民营养模型：`src/main/java/com/teammoeg/frostedheart/content/town/buildings/house/HouseDailyModel.java`
- 居民食物排序：`src/main/java/com/teammoeg/frostedheart/content/town/resource/TownFoodNutritionModel.java`
- 食物资源量：`src/main/java/com/teammoeg/frostedheart/content/town/resource/TownFoodResourceAmount.java`
- 营养数据生成：`src/datagen/java/com/teammoeg/frostedheart/data/FHRecipeProvider.java`
- 食物营养配方 JSON：`src/generated/resources/data/frostedheart/recipes/diet_value/**/*.json`
- 食物营养数据源：`src/datagen/resources/data/frostedheart/data/new_food_value.xlsx`
