# FrostedHeart — `town` 包开发者参考（AI Generated）

> 路径：`src/main/java/com/teammoeg/frostedheart/content/town`
> 适用版本：当前仓库 `main`（最近修改 2026-08；含增量同步重构，见 §9 与 §14）

本文件汇总 `town`（城镇）系统的关键架构、常用类与典型扩展方式，供日常开发与排查使用。
内容基于源码阅读，所有路径相对于 `town` 包根目录。

---

## 1. 一句话架构

城镇系统采用 **「数据 / 视图分离」+ 分层接口** 的设计：

- **`TeamTownData`（`implements SpecialData`）** —— 唯一的、可持久化的城镇数据，挂在**队伍数据**（`TeamDataHolder`）上，用 Mojang `Codec` 序列化。
- **`TeamTown`** —— `TeamTownData` 的轻量门面/视图（`implements ITown, ITownWithResidents, ITownWithBuildings`），只暴露读写入口，不存状态。
- **`AbstractTownBuildingBlockEntity`** —— 方块放置时创建对应的 `AbstractTownBuilding`，并注册进城镇的 `buildings` 映射。
- 每个游戏日（`tickMorning`）统一校验结构、分配居民、链接矿场、执行建筑工作、更新资源。
- **增量同步（服务端 `tick()`）** —— 集合增删 / 建筑与居民字段变更 / 资源数量变化经 **Listener → `DataSyncCache` 脏键集** 统一捕获，按 tick flush 为三类增量包；登录 / 切维度 / 开 GUI / 镇长印章时以 `TeamTownDataS2CPacket` 全量兜底（详见 §14）。

---

## 2. 目录结构（精简）

```
town/
├── ITown.java / ITownWithBuildings / ITownWithResidents / ITownWithResources.java   # 核心接口
├── TeamTown.java                # 门面
├── TeamTownData.java            # 持久化数据 + 每日主流程 tickMorning + 增量同步 tick
├── ChunkTownResourceCapability.java
├── TownMathFunctions.java       # 评分/温度工具
├── TownHistoryEntry.java        # 每日快照历史（随存档持久化、随全量包下发）
├── util/ObservableTownMap.java  # 集合层自动 fire 的 LinkedHashMap（增量同步基础设施）
├── block/                       # 方块与方块实体基类、占用体积
│   ├── AbstractTownBuildingBlock / ...BlockEntity / TownBlockEntity(接口)
│   ├── OccupiedVolume.java
│   └── blockscanner/            # 结构扫描（Floor/ConfinedSpace/Building）
├── building/                    # 建筑逻辑抽象
│   ├── AbstractTownBuilding / AbstractTownResidentWorkBuilding
│   ├── ITownBuilding(Codec分派) / ITownResidentBuilding / ITownResidentWorkBuilding
│   ├── TownProductionReportItem / TownProductionStopReason
│   └── buildings/{house,hunting,mine,warehouse}/   # 四件套具体建筑
├── resource/                    # 资源系统（Holder / Action / Type / Attribute）
│   ├── TeamTownResourceHolder / TownResourceManager(已废弃)
│   ├── TeamTownResourceActionExecutorHandler
│   ├── action/                  # 命令式资源动作
│   └── watcher/                 # 仓库库存监视器（IWarehouseStockWatcher 等）
├── resident/                    # Resident / Family / WanderingRefugee / ResidentEntity
├── terrainresource/             # TerrainResourceType / TerrainResourceData
├── event/                       # 三类变更事件 + 监听器 + 客户端 ITownDataUpdateListener
├── network/                     # S2C / C2S 数据包
├── provider/                    # 队伍→城镇 关联与存档
└── tabs/                        # GUI Tab
```

---

## 3. 核心接口与类

### 3.1 `ITown` 与三个子接口

`ITown` 只是三个子接口的「并集」，代表功能完整的城镇：

```java
public interface ITown extends ITownWithResources, ITownWithBuildings, ITownWithResidents {
    boolean DEBUG_MODE = false;   // 发布前须置 false/删除
}
```

| 子接口 | 职责 | 关键方法 |
|---|---|---|
| `ITownWithBuildings` | 建筑与方块映射 | `getTownBuildings()`、`getTownBuilding(pos)`、`addTownBlock(pos, te)`、`removeTownBlock(level, pos)` |
| `ITownWithResidents` | 居民 | `getAllResidents()`、`getResident(uuid)`、`addResident/removeResident` |
| `ITownWithResources` | 资源 | `getActionExecutorHandler()`（只暴露动作执行器） |

### 3.2 `TeamTown`（门面）

- `TeamTown.create(TeamTownData)` / `TeamTown.from(Player)`（`from` 从队伍数据取 `FHSpecialDataTypes.TOWN_DATA`）。
- 建筑：`addTownBlock` 内部调用 `townBlockEntity.createBuilding()` 后放入 `data.buildings`；`removeTownBlock` 调用 `building.onRemoved(this)`。
- 资源：`getActionExecutorHandler()` → `data.resources.actionExecutor`；`getResourceHolder()` → `data.resources`。
- 地形资源：`pickTerrainResource(type, maxPick)` / `pickTerrainResource(type, chunkPos, maxPick)` / `unpickTerrainResource` / `maypickTerrainResource`。

### 3.3 `TeamTownData`（持久化数据 + 每日主流程）

`implements SpecialData`，`CODEC` 字段：`name / resources / blocks / residents / terrainResource / labour / maxLabour / history / lastRefugeeSpawnDay`。
所有「永久状态」都在此处；`TeamTown` 只是指向它的视图。

**同步相关状态**：
- `buildings` / `residents` 是 `util/ObservableTownMap`（集合增删自动 fire 到脏标记）。
- `dataSyncCache`（`DataSyncCache`，TeamTownData 内部类）—— 脏状态**唯一真相源**：实现三个 `XxxChangeEventListener`，持 `changedBuildingPos / changedResidentUUID / changedResourceKey` 三脏键集 + 资源发送端值级去重快照（`lastSyncedResources`）。
- `clientListeners`（static `Set<ITownDataUpdateListener>`）—— 客户端 GUI 数据刷新用（全量包替换实例后监听不丢失）。

**每 tick 增量同步 `tick(ServerLevel, TeamDataHolder)`**：首 tick 把监听器注入既有建筑/居民（R1 修复点），此后每个 tick 检查脏键集：
- 资源：先做**发送端值级去重**（当前值 == 上次已同步值则跳过）→ `TownResourceUpdatePacket`
- 居民 / 建筑：按脏键查 Map 分「变更 / 移除」→ `TownResidentUpdatePacket` / `TownBuildingUpdatePacket`
- 空转抑制三道防线：setter 值守卫（值未变不 fire）+ `reloadMaxCapacity` 净变化守卫 + 资源值级去重（见 §14）。

**每日主流程 `tickMorning(ServerLevel, TeamDataHolder)`** 依次：

1. `checkBlocks` —— 移除已失效/方块被改的建筑（与 `TownBlockEntity.getBuilding` 一致性校验）。
2. `checkOccupiedAreaOverlap` —— 用 `OccupiedVolume.intersects` 两两比对，重叠的双方 `occupiedAreaOverlapped=true`（导致 `isBuildingWorkable` 失败）。
3. `tickResidentsMorning` —— `health<=5` 或 `mental<=5` 判定死亡（`DEBUG_MODE` 下跳过）；无房者 `costHealth(10)`。
4. `tickResidentsAging` —— 居民老化结算（见 §7）：`ageDays+1`，幼儿/儿童达标后成长，各年龄组按 `FHConfig.SERVER.TOWN.RESIDENT_AGING` 每日增减属性并封顶。
5. `residentAllocatingCheck` —— 清空所有居民的 house/work 位置后从建筑回写，并剔除超限/已亡居民。
6. `allocateHouse` —— 按 `HouseBuilding.getRating()` **降序** 优先分配无房居民。
7. `assignWork` —— 工作建筑入优先级队列（`getResidentPriority()` 降序），对每个建筑挑 `getResidentScore` 最高的可用居民（幼儿 `AGE_INFANT` 被 `canResidentWork` 与 `assignWork` 双重排除）。
8. `tickRefugeeSpawnAndDespawn` —— 难民刷新与清场（见 §7）：先清场（塔旁无房/超时的 `townSpawned` 难民消失），塔开启且当天未结算时按天气概率刷一批到塔附近（`REFUGEE_SPAWN` 配置，`lastRefugeeSpawnDay` 按世界日防重复）。
9. `linkMinesToBases` —— 按 `MineBaseBuilding.getConnectionRadius()` 把范围内的 `MineBuilding` 链到矿基。
10. `recalcOreChunkResources` —— 设定 `ORE` 的活跃区块（`setTerrainResourceTypeActiveChunks`）。
11. `buildingsWork` —— `reloadMaxCapacity()` 后筛选 `shouldRunDailySettlement()`，再按 `getWorkPriority()` 降序执行 `building.work(town, world)`；默认资格等于 `isBuildingWorkable()`，住宅会单独忽略温度门槛。
12. `recoverResources` —— 地形资源按配置的 `recoverSpeed` 恢复。

CODEC 字段：`name / resources / blocks / residents / terrainResource / labour / maxLabour / history / lastRefugeeSpawnDay`；其中 `lastRefugeeSpawnDay` 使用逻辑气候日的 `Codec.LONG`，缺省为 `-1`，兼容首日结算。

---

## 4. 建筑系统

### 4.1 抽象层

- **`ITownBuilding`**（旧称 TownWorker，建筑逻辑核心）：自带**类型分派 `Codec`**，新增建筑必须在此登记（见 §6）。关键方法：
  ```java
  boolean isBuildingWorkable();
  default boolean shouldRunDailySettlement(); // 默认等于 isBuildingWorkable()
  boolean work(ITownWithBuildings town, ServerLevel world);
  default int getWorkPriority();   // 默认 0，越大越先工作
  void onRemoved(ITownWithBuildings town);
  ```
- **`AbstractTownBuilding`** 关键状态（字段已私有化，统一经 **setter 变更**；setter 带**值守卫**——值未变直接 return，不 fire，避免空转脏标记；`changeListener` 由 `TeamTownData` 注入，解码阶段为 null 不误触发）：
  ```java
  protected final BlockPos pos;
  private boolean initialized = false;            // 是否完成首次扫描
  private boolean occupiedAreaOverlapped = false; // 是否与别处重叠
  private boolean isStructureValid = false;       // 结构是否合法
  private OccupiedVolume occupiedVolume = OccupiedVolume.EMPTY; // 占用体积
  // setter: setInitialized / setOccupiedAreaOverlapped / setIsStructureValid / setOccupiedVolume
  //   → if (this.x == x) return; this.x = x; fireChange();
  isBuildingWorkable() = initialized && !occupiedAreaOverlapped && isStructureValid;
  work(ITownWithBuildings) 默认返回 true;
  ```
- **`ITownResidentBuilding`**：居民型建筑（`addResident/removeResident/getMaxResidents/getResidentsID/ getResidents`）。
- **`ITownResidentWorkBuilding extends ITownResidentBuilding`**：工作建筑，定义 `getResidentPriority()`（满员返回 `Double.NEGATIVE_INFINITY` 表示不可用）与 `getResidentScore(Resident)`（效率分）、`canResidentWork`/`canResidentBeAssigned`。
- **`AbstractTownResidentWorkBuilding`**：持有 `Set<UUID> residentsID` + `maxResidents`，`addResident`/`removeResident` 同步设置 `resident.setWorkPos`。

### 4.2 具体建筑（四件套）

每种建筑 = **`<Name>Block` + `<Name>BlockEntity` + `<Name>BlockScanner`(可选) + `<Name>Building`**，置于 `buildings/<type>/`。

| 建筑 | Building 类 | 是否工作 | 关键点 |
|---|---|---|---|
| House | `buildings/house/HouseBuilding` | 否（住宅） | 仅 `implements ITownResidentBuilding`；`work()` 每日按配置消耗一次 `RESIDENT_FOOD_LEVEL`，由食物满足度、营养质量、有效温度和综合舒适度线性计算生命/精神的损失与恢复，不再改变力量；低于 `0°C` 或高于 `50°C` 时仍为不可工作、不会分配新人，但 `shouldRunDailySettlement()` 仍使已有居民吃饭并结算状态；最近一次结算写入 `DailyReport`，`getRating()` 决定分房优先级。 |
| Hunting Base | `buildings/hunting/HuntingBaseBuilding` | 是 | 继承 `AbstractTownResidentWorkBuilding`；按居民 score 总和决定投掷次数，受 `TerrainResourceType.HUNT` 限制，用战利品表 `town/hunting` 产出并 ADD 进仓库。 |
| Mine Base | `buildings/mine/MineBaseBuilding` | 是 | 持有 `Set<BlockPos> linkedMines`；汇总有效 `MineBuilding` 权重，按区块向 `ORE` 开采。 |
| Mine | `buildings/mine/MineBuilding` | 否（标记） | 仅扫描/标记；`BiomeMineResourceRecipe` 提供生物群系矿产权重（`getWeights(biome)`）。 |
| Warehouse | `buildings/warehouse/WarehouseBuilding` | 否 | `addCapacity(ITown)` 把 `capacity` 作为 `MAX_CAPACITY` 资源加入城镇；`TeamTownData.reloadMaxCapacity()` 带**净变化守卫**（先累加所有可工作仓库容量，与当前 `MAX_CAPACITY` 相同则直接返回，否则才清零重加——避免 `WarehouseBlockEntity.refresh` 定时刷新造成的空转资源包）。配套 `WarehouseMenu`/`WarehouseScreen`/`WarehouseBlockScanner`，以及 `WarehouseInterface*`（接口箱）与 `WarehouseLevelEmitter*`（红石输出）。 |

---

## 5. 方块与结构扫描

- **`AbstractTownBuildingBlock`**（`extends CBlock`）：`LIT` + `FACING` 属性；`setPlacedBy` 中若 `ChunkHeatData.hasActiveAdjust(world,pos)` 或 `DEBUG_MODE`，则 `TeamTown.from(placer).addTownBlock(pos, te)`，设置 `townProvider`（队伍 ID）与方块主人（`IOwnerTile`）。
- **`TownBlockEntity<T>` 接口**（方块实体必须实现）：
  ```java
  void refresh(@NotNull T building);          // 置 initialized=true 并 scanStructure
  Optional<T> getBuilding();
  @Nullable T getBuilding(AbstractTownBuilding); // 类型匹配转换，不匹配返回 null（用于一致性校验）
  @NotNull T createBuilding();                 // 返回新的具体 Building 实例
  ```
- **`AbstractTownBuildingBlockEntity<T>`**：持有 `ITownProviderSerializable<? extends ITownWithBuildings> townProvider`；`tick()` 把自己加入 `SchedulerQueue` 延迟重扫；NBT 序列化 `townProvider`；`scanStructure(T)` 为抽象方法（子类实现结构合法性判断并填充 `occupiedVolume`）。
- **扫描器**（`block/blockscanner/`）：
  - `AbstractBlockScanner`：模板方法 `boolean scan()`（final，BFS），钩子 `nextScanningBlocks`/`processBlock`/`shouldStopAt`；常量 `MAX_HEIGHT=320, MIN_HEIGHT=-64, DEFAULT_MAX_SCAN_BLOCKS=4096`；静态工具 `countBlocksAdjacent`、`countBlocksAbove`(返回 `HeightCheckingInfo`)、`getDoorAdjacent`、`isAirOrLadder` 等。
  - `FloorBlockScanner`：扫描连通地板（`isFloorBlock`：方块/楼梯/slab；`isWallBlock`：墙/门/栅栏/玻璃板/`FHTags.Blocks.TOWN_WALLS`），支持梯子跨层。
  - `ConfinedSpaceScanner`：扫描密闭空气空间。
  - `BuildingBlockScanner`：**组合** `FloorBlockScanner` + `ConfinedSpaceScanner`，产出 `area / volume / occupiedVolume / airs(LongSet 缓存)`。`HouseBlockScanner extends BuildingBlockScanner` 覆写以统计床、装饰物（`FHTags.Blocks.TOWN_DECORATIONS` 或 `cfm` 模组方块）与温度。
- **`OccupiedVolume`**：`WorldMarker`（压缩方块集合）+ 包围盒；`intersects()` 先比包围盒再比具体方块，用于重叠检测；`EMPTY` 单例。

---

## 6. 资源系统

### 6.1 键 / 类型 / 属性层级

`ITownResourceType`(枚举) + `level` → `ITownResourceAttribute`；直接存盘的是 `ITownResourceKey`。

- **`ItemResourceType`**（有对应物品）：`OTHER, WOOD, STONE, ORE, METAL(10级), FUEL, TOOL, RESIDENT_FOOD_LEVEL(4级)`。每个有 `maxLevel`。
- **`ItemResourceAttribute`**：`(type, level)`，用 `Interner` 缓存；与 `FHTags.Items` 的 `MAP_TAG_TO_TOWN_RESOURCE_ATTRIBUTE` / `MAP_TOWN_RESOURCE_ATTRIBUTE_TO_TAG` 双向映射（TagKey ↔ Attribute）；`fromItemStack(itemStack)` 取物品所有属性。
- **`ItemStackResourceKey`**：`Item + tag`（count 固定 1）的 Map 键，自定义 `hashCode/equals`。
- **`VirtualResourceType`**（无物品、长期存盘）：目前仅 `MAX_CAPACITY`（不占容量、是 service、level 0）。`VirtualResourceAttribute` 既是属性也是直接存盘的 Key。
- **`ItemResourceAmountRecipe`**：配方类（IE 配方），定义「某物品 → 某资源 Tag 的转化量」；`TeamTownResourceHolder.loadItemResourceAmounts()` 加载进缓存。显式配方优先；居民食物没有显式值时按 `饥饿值 + 2 × 饥饿值 × 饱和度系数` 换算，其他资源仍默认 `1`。
- 居民食物 Tag 使用互斥的 level 0–4：危险/未建模原料、基础生食、普通熟食与主食、复合/高密度餐食、完整军粮与稀有强化食物。住宅先按 level 4 → 0 消耗；同等级内再按 `NutritionRecipe` 营养标量除以该物品的居民食物资源量降序消耗，平局按物品注册名与 NBT 稳定排序。

### 6.2 `TeamTownResourceHolder`（资源持有者）

```java
private final Object2DoubleOpenHashMap<ITownResourceKey> resources;
public final TeamTownResourceActionExecutorHandler actionExecutor;
private double occupiedCapacity;

double get(IGettable);                 // 支持 Key / Attribute / Type
double get(ItemStack);
Map<ItemStackResourceKey,Double> getAllItems();
Map<VirtualResourceAttribute,Double> getAllVirtualResources();
double getCapacityLeft();             // MAX_CAPACITY - occupiedCapacity
void resetMaxCapacity();              // 每日重建仓库容量用
```
- 仅暴露 `addUnsafe` / `costUnsafe`（**不做容量/余量校验**），同步维护 `occupiedCapacity`：物品每个占 1 容量、`needCapacity` 的虚拟资源占 1 容量。
- 两个静态缓存：`ITEM_RESOURCE_ATTRIBUTE_CACHE`（Attribute→物品集合）、`ITEM_RESOURCE_AMOUNTS`（物品→(Attribute→数量)）。

### 6.3 `TownResourceManager` —— ⚠️ 已废弃

`@Deprecated`，仅历史遗留。新代码一律走 §6.4 的 **Action 系统**。

### 6.4 Action 系统（推荐路径，命令模式）

`Action(纯数据)` → `IActionExecutorHandler` 找到对应 `ITownResourceActionExecutor` → 返回 `ITownResourceActionResult`。

- `ResourceActionType`：`ADD` / `COST`
- `ResourceActionMode`：`ATTEMPT`（不够就取消）/ `MAXIMIZE`（尽可能多）
- `ResourceActionOrder`：`ASCENDING` / `DESCENDING`（按 Type 消耗时的优先级顺序）
- **`TownResourceActions`**（Action 与工厂）：
  - `ItemResourceAttributeCostAction(attr, amount, mode)`
  - `ItemResourceAction(itemStack, type, amount, mode)`
  - `ItemStackAction(itemStack, type, mode)`（数量取 stack 的 count）
  - `TownResourceTypeCostAction(type, amount, minLevel, maxLevel, mode, order)`
  - `VirtualResourceAttributeAction(attr, amount, actionType, mode)`
  - `GetAction(toGet)` + 静态 `get(handler, toGet)`
- **`TeamTownResourceActionExecutorHandler`**：构造时注册 6 个 Executor，内部调用 `resourceHolder.addUnsafe/costUnsafe` 并处理容量/余量、`MAXIMIZE/ATTEMPT`、以及把 `TownResourceTypeCostAction` 分解为按 level 的 attribute 动作（`executeFuzzy`）。

**调用示例**（HouseBuilding 消耗食物）：
```java
TownResourceActions.TownResourceTypeCostAction action =
    new TownResourceActions.TownResourceTypeCostAction(
        ItemResourceType.RESIDENT_FOOD_LEVEL, toCost / residentNum,
        0, 4, ResourceActionMode.MAXIMIZE, ResourceActionOrder.DESCENDING);
TownResourceActionResults.TownResourceTypeCostActionResult result =
    town.getActionExecutorHandler().execute(action);
```

---

## 7. 居民系统

- **`Resident`**（抽象模拟数据，非实体）：字段 `uuid, firstName, lastName, health, mental, strength, intelligence`(0~100), `educationLevel`, `Map<String,Double> workProficiency`（key = 建筑类 `getSimpleName()`，如 `"HuntingBaseBuilding"`）, `housePos`, `workPos`，以及 **`age`（0 幼儿 / 1 儿童 / 2 青壮年 / 3 老人，默认 2）与 `ageDays`**（CODEC/NBT 均带默认值向后兼容）。提供 `get/add/cost` 系列（越界抛异常/夹取）。`setDeath(town)` 调用 `town.removeResident(uuid)`。**所有 setter 均带值守卫并 `fireChange()`**（值未变不 fire），由 `TeamTownData` 注入的 `changeListener` 驱动增量同步（见 §14）。
  - 按年龄组生成：`initializeAttributesForAge(age)` —— 幼儿力量/智力 center 20/30、儿童 40/40、老人 35/65（spread 0.8）、青壮年沿用 `generateAdultAttribute`；初始工作熟练度幼儿 0、儿童 [0,25]、老人 [50,100]（`generateElderInitialWorkProficiency`）、青壮年 [0,50]。
  - 每日老化由 `TeamTownData.tickResidentsAging` 结算：`growStrengthDaily/growIntelligenceDaily`（封顶各年龄组 cap，儿童力量/智商上限 80/85 可超过直接招募的成年难民）、`decayStrengthDaily`（老人力量萎缩至 floor 25）。
  - 寒流高质量难民：`applyColdSurvivorBuffs()` —— 血量 20~40、力量/智商 +15、初始熟练度 ×1.5，招募时应用。
  - 静态辅助：`randomAgeDaysForAge(int)`（招募时按年龄组随机成长进度）、`ageLangKey(int)`（年龄显示翻译键）。
- **`Family`**：简单包装 `Resident[] + lastName`，目前仅数据容器。
- **`ResidentEntity extends Mob`**：预留实体类，目前为空（居民以 `Resident` 数据形式活在 `TeamTownData`，不生成单独实体）。
- **`WanderingRefugee`**：流浪难民实体（`extends AbstractVillager implements NeutralMob, VillagerDataHolder`），可被招募为居民或交易；`mobInteract` 打开交易界面，`WanderingRefugeeRecruitMessage` 处理招募（生成粒子、移除实体、写入 `TeamTown.addResident`，**年龄/成长进度/寒流 buff 随招募传入**）。
  - 年龄：同步字段 `AGE`（`EntityDataAccessor<Integer>`，客户端可读）；`getAgeScale()` 幼儿 0.4 / 儿童 0.5 / 其余 1.0，`getDimensions` 随年龄缩放（视线高度自动跟随），渲染器 `WanderingRefugeeRenderer.scale` 同步缩小模型与阴影。**不使用** `isBaby()`（会触发 `mobInteract` 守卫导致儿童无法招募）。
  - 城镇刷新：`townSpawned / waitingDays / townOwner(UUID) / coldSurvivor` 为服务端 NBT 字段（不同步），`TeamTownData.tickRefugeeSpawnAndDespawn` 每日按天气概率在开启的能量塔（`GeneratorData.isWorking`，非 `isActive`）8~24 格内刷批（`/town spawn_refugees` 可强制触发）；清场时无房位或等待超 `maxWaitDays` 天即消失。天气判定复用 `WeatherForecast.getTemperatureLevel`（public）+ `WorldTemperature.climate` + `WorldClimate.isSun/isBlizzard`：暖流（≥1 级且晴天）+30% 概率 +1 数量、寒流（≤-1 级或暴风雪）-30% 概率 -1 数量且 `coldQualityChance` 概率高质量低血量、平稳为基准 60%。

---

## 8. 地形资源

- **`TerrainResourceType`**（枚举）：`WOOD, ORE, HUNT, POI, SALVAGE`，各自由 `FHConfig` 提供 `recoverSpeed` 与 `resourcePerSq`。代表野外可再生资源。
- **`TerrainResourceData`**：单类型储量（`extracted/total/radius`，半径由 `extracted` 与 `resourcePerSq` 反算）。`ChunkResourceTracker`（`Map<ChunkPos,Double>` + 临时 `activeChunks`）仅 `MineBaseBuilding` 使用以确定逐区块可采量。
- 访问入口：`TeamTown.pickTerrainResource/unpickTerrainResource/maypickTerrainResource`（全局或按区块）。

---

## 9. 事件 / 网络 / GUI

- **事件**（`event/`）：基于 `java.util.EventObject` 的轻量事件：`TownBuildingChangeEvent` / `TownResidentChangeEvent` / `TownResourceChangeEvent`，及对应监听器接口（`ITownBuildingChangeEventListener` 等）。
  - **服务端**：`TeamTownData.dataSyncCache` 实现三个监听器，是增量同步的脏标记入口（见 §14）；建筑 / 居民对象在装入 Map 后由 `TeamTownData` 注入监听（`setChangeEventListener`）。
  - **客户端**：`ITownDataUpdateListener`（三回调均默认空实现），GUI 打开时 `addClientListener`、关闭时 `removeClientListener`；增量包 `applyXxxUpdate` 按类别触发，全量包替换实例后触发全部三类。
  - `TownCommonEvents`：给区块挂 `chunk_town_resource` Capability，以及**全量兜底**触发（登录 / 切维度 / 开城镇建筑 GUI → `TeamTownDataS2CPacket`）。
- **网络**（`network/`，均为 `CMessage`，在 `FHNetwork` 统一注册）：
  - `TeamTownDataS2CPacket`：S→C **全量兜底**（登录 / 切维度 / 开 GUI / 镇长印章 / `/townsync fullsync` 手动），客户端解码后替换整份 `TeamTownData` 实例并 `fireClientDataChanged()`。
  - `TownBuildingUpdatePacket` / `TownResidentUpdatePacket` / `TownResourceUpdatePacket`：**增量包**（完整实现：encode/decode/handle + 客户端 `applyXxxUpdate` 覆盖式 merge + 移除），服务端 `tick()` 每 tick flush 脏键集发出（见 §14）。
  - `WarehouseUpdatePacket` / `WarehouseInteractPacket`：仓库物品同步与存取（C→S 改资源并回写玩家手持物）。
  - `WanderingRefugeeOpenTradeGUIMessage` / `WanderingRefugeeRecruitMessage`：难民交易/招募。
- **GUI**（`tabs/` + `StandardTownBuildingScreen` / `AbstractTownWorkerBlockScreen`）：通用工人方块界面，左侧 Tab 列表；仓库使用 `TownInformationTab` / `TownResourceTab`。住宅使用 `HouseScreen`，概览页展示最近一次日结，居民页从客户端快照读取居民属性并支持逐人查看。
  - **数据刷新约定**：数据面板（`TownBuildingsPanel` / `TownResidentsPanel` / `TownInfoPanel` / `TownStatisticsPanel` / `TownWorkforcePanel` / `BuildingInfoElement` / `VirtualItemGridElement` / `HouseResidentPanel`）在 `render()` 阶段经 **Supplier 从客户端快照实时取数**（每帧解析最新实例）；**收包时禁止 `contentLayer.refresh()` 重建界面**（会重置滚动位置/选中状态）。`ITownDataUpdateListener` 回调可留空实现——内容每帧自动更新。

---

## 10. 外部接线点（新增/排查时必看）

| 位置 | 内容 |
|---|---|
| `bootstrap/common/FHBlocks.java` | `HOUSE / WAREHOUSE / MINE / MINE_BASE / HUNTING_BASE` 方块注册（`REGISTRATE.block(...)`） |
| `bootstrap/common/FHBlockEntityTypes.java` | 对应方块实体注册：`makeType(XxxBlockEntity::new, FHBlocks.XXX::get)` |
| `bootstrap/common/FHMenuTypes.java` | `WAREHOUSE` / `HOUSE` 菜单注册 |
| `bootstrap/client/FHScreens.java` | `WarehouseScreen` / `HouseScreen` 客户端界面注册 |
| `bootstrap/reference/FHTags.java` | `Blocks.TOWN_DECORATIONS` / `Blocks.TOWN_WALLS`；`Items.MAP_TAG_TO_TOWN_RESOURCE_ATTRIBUTE` / `MAP_TOWN_RESOURCE_ATTRIBUTE_TO_TAG`（Tag ↔ ItemResourceAttribute 双向映射） |
| `infrastructure/gen/FHRegistrateTags.java` | 各建筑方块、装饰/墙 Tag、资源 Tag 的实际条目 |
| `bootstrap/common/FHCapabilities.java` | `CHUNK_TOWN_RESOURCE`（区块资源 Capability） |
| `bootstrap/common/FHSpecialDataTypes.java` | `TOWN_DATA`（城镇 SpecialData 类型，挂在队伍数据上） |
| `FHNetwork.java` | 注册 `TeamTownDataS2CPacket` + 三增量包 + 仓库/难民包（新增数据包必须在此 `registerMessage`，否则运行时崩 "does not registered in this channel"） |

---

## 11. 如何新增一个建筑（标准步骤）

假设新建筑名为 `Bakery`（烘焙坊，工作建筑）：

1. **建四件套**：在 `buildings/bakery/` 下新建
   - `BakeryBlock extends AbstractTownBuildingBlock`
   - `BakeryBlockEntity extends AbstractTownBuildingBlockEntity<BakeryBuilding>`
   - `BakeryBlockScanner extends BuildingBlockScanner`（可选，需要扫描结构时）
   - `BakeryBuilding extends AbstractTownResidentWorkBuilding`（或 `AbstractTownBuilding`）
2. **`BakeryBuilding` 必须提供**：
   - `public static final Codec<BakeryBuilding> CODEC`（用 `RecordCodecBuilder`，常见字段 `pos, isStructureValid, occupiedVolume, ...`）。
   - 实现 `work(ITownWithBuildings, ServerLevel)` —— 通过 `town.getActionExecutorHandler().execute(new TownResourceActions.XxxAction(...))` 读写资源、改 `Resident` 属性。
   - 若为工作建筑：`getResidentPriority()`（满员返回 `NEGATIVE_INFINITY`）与 `getResidentScore(Resident)`。
3. **在 `ITownBuilding.CODEC` 分派中登记**（关键、易漏）：
   ```java
   .type("bakery", BakeryBuilding.class, BakeryBuilding.CODEC)
   ```
   注意 dispatch 的 key（如 `"bakery"`）要与存档中一致。
4. **`BakeryBlockEntity` 必须实现**：
   - `createBuilding()` → `new BakeryBuilding(pos)`
   - `getBuilding(AbstractTownBuilding)` → 类型匹配则返回强转实例，否则 `null`
   - `scanStructure(BakeryBuilding)` → 用 `BakeryBlockScanner` 填充 `building.occupiedVolume` 等并返回合法性
5. **注册接线**：在 `FHBlocks` / `FHBlockEntityTypes` / （若要 GUI）`FHMenuTypes` / `FHScreens` / `FHRegistrateTags` 分别登记（见 §10）。
6. **资源 Tag（可选）**：若产出/消耗特定物品资源，在 `FHRegistrateTags` 给对应 `ItemResourceAttribute` 的 Tag 添加物品（走 `FHTags.Items.MAP_TAG_TO_TOWN_RESOURCE_ATTRIBUTE`）。

> 住宅类（`HouseBuilding` 模式）只 `implements ITownResidentBuilding`，不继承工作建筑基类；其 `work()` 直接消费食物/增益居民。

---

## 12. 命名约定与坑位速查

1. **四件套命名**：`<Name>Block` / `<Name>BlockEntity` / `<Name>BlockScanner`(可选) / `<Name>Building`，放在 `buildings/<type>/`。
2. **Building 三要素**：自有 `Codec` + 在 `ITownBuilding.CODEC` dispatch 注册 + `BlockEntity` 实现 `createBuilding/getBuilding/scanStructure`。
3. **改资源一律走 Action**：`town.getActionExecutorHandler().execute(new TownResourceActions.XxxAction(...))`，**不要**直接调用 `TeamTownResourceHolder.addUnsafe/costUnsafe`（`TownResourceManager` 已废弃）。
4. **容量**：物品与 `needCapacity` 虚拟资源各占 1 容量，由 `WarehouseBuilding.addCapacity` 每日累加、`resetMaxCapacity` 每日清零。
5. **`isBuildingWorkable()`** = `initialized && !occupiedAreaOverlapped && isStructureValid`。结构重叠或被其它建筑占位的方块不会工作。
6. **`DEBUG_MODE`**（`ITown.DEBUG_MODE=false`）：控制是否跳过温度校验、居民死亡等。**正式发布前务必置 false/删除**。
7. **增量同步已启用**：`TownBuilding/Resident/ResourceUpdatePacket` 为完整实现，服务端 `tick()` 每 tick flush 脏键集；全量包仅作兜底（登录 / 切维度 / 开 GUI / 镇长印章）。详见 §14。
8. **新增 setter 必须带值守卫**：任何会改变建筑/居民字段的 setter 都要先判断「值未变直接 return」，再 `fireChange()`；否则会造成空转脏标记 → 每 tick 重复发包。资源层增量发包前有值级去重兜底，但建筑/居民层**没有**（内存敏感，未做指纹），依赖 setter 守卫在源头拦截。
9. **居民以数据形式存在**：`Resident` 存于 `TeamTownData.residents`，不生成独立实体（`ResidentEntity` 预留为空）。

---

## 13. 关键文件索引

| 我想了解… | 看这个文件 |
|---|---|
| 城镇整体接口 | `ITown.java` + 三个 `ITownWithXxx.java` |
| 门面/读写入口 | `TeamTown.java` |
| 持久化数据 + 每日流程 | `TeamTownData.java`（`tickMorning`） |
| 建筑逻辑抽象 | `building/AbstractTownBuilding.java`、`building/ITownBuilding.java` |
| 工作建筑协议 | `building/AbstractTownResidentWorkBuilding.java`、`building/ITownResidentWorkBuilding.java` |
| 方块实体基类 | `block/AbstractTownBuildingBlockEntity.java`、`block/AbstractTownBuildingBlock.java` |
| 结构扫描 | `block/blockscanner/*`、`block/OccupiedVolume.java` |
| 资源持有/动作 | `resource/TeamTownResourceHolder.java`、`resource/TeamTownResourceActionExecutorHandler.java`、`resource/action/*` |
| 资源类型映射 | `resource/ItemResourceType.java`、`resource/VirtualResourceType.java`、`bootstrap/reference/FHTags.java` |
| 居民 | `resident/Resident.java`、`resident/WanderingRefugee.java` |
| 地形资源 | `terrainresource/*` |
| 队伍关联/存档 | `provider/*`、`ChunkTownResourceCapability.java` |
| GUI | `tabs/*`、`buildings/warehouse/WarehouseScreen.java` |
| 评分/温度 | `TownMathFunctions.java` |
| 增量同步总览 | `TeamTownData.java`（`tick()` / `DataSyncCache` / `applyXxxUpdate`）、`util/ObservableTownMap.java`、`event/ITownDataUpdateListener.java` |

---

> 注：`util/` 目录存放增量同步基础设施（`ObservableTownMap`）；`TeamTownData.dataSyncCache` 为脏状态唯一真相源，细粒度客户端同步已启用（见 §14）。

---

## 14. 客户端增量同步（2026-08 重构完成）

> 目标：把「每 tick 给每个玩家发整份 `TeamTownData` 全量」改成「只发变化」。原 per-tick 全量同步已移除，全量包仅作兜底。

### 14.1 变更检测链路（三层捕获）

| 层 | 触发源 | 实现 | 进入脏标记 |
|---|---|---|---|
| ① 集合增删 | `buildings` / `residents` 的 put / remove / replace / clear / 迭代器 remove | `util/ObservableTownMap`（LinkedHashMap 子类，纯 fire 中继，不持脏状态） | `onChange(key)` → `DataSyncCache.onXxxChange` |
| ② 对象字段 | 建筑 / 居民内部字段 setter | setter 值守卫 + `fireChange()`（`changeListener` 由 `TeamTownData` 注入，解码阶段 null 不误触） | `onXxxChange` |
| ③ 资源 | `TeamTownResourceHolder.addSigned` 等唯一资源入口 | `addSigned` 内 `amount ≠ 0 && changeListener ≠ null` 时 fire | `onResourceChange` |

### 14.2 脏状态唯一真相源 `DataSyncCache`

- `TeamTownData` 内部类，实现三个 `XxxChangeEventListener`；持 `changedBuildingPos / changedResidentUUID / changedResourceKey` 三脏键集 + `lastSyncedResources` 值级去重快照。
- **不在** `ObservableTownMap` 内维护第二份脏状态（纯 fire 中继）；add / replace / remove 进**同一个** changed 集，发包时查 Map 判增/删。
- 接线时机：`setOnAttach/onDetach` 在批量 put **之前**绑定（反序列化对象也自动接/解监听），`setOnChange` 在批量 put **之后**绑定（只做脏标记，避免加载存档误标脏）。

### 14.3 flush 与发包（`tick()` 每 tick）

1. 资源：对脏键做**发送端值级去重**（当前值 ≈ 上次已同步值则跳过）→ `TownResourceUpdatePacket(changed, occupiedCapacity)` → `markResourceSynced`。
2. 居民 / 建筑：脏键查 Map，`null` → removed 集，否则 changed 集 → 各自包。
3. 全量兜底触发点：`TownCommonEvents`（登录 / 切维度 / 开城镇建筑 GUI）+ `TownManagerItem.use`（镇长印章）→ `TeamTownDataS2CPacket`；客户端解码后**替换整份实例**并 `fireClientDataChanged()`。

### 14.4 空转抑制（三道防线，2026-08-01）

| 防线 | 位置 | 作用 |
|---|---|---|
| A. setter 值守卫 | `AbstractTownBuilding` / `HouseBuilding` / `WarehouseBuilding` / `Resident` 全部 setter | 值未变不 fire，从源头消除字段层空转 |
| B. `reloadMaxCapacity` 净变化守卫 | `TeamTownData.reloadMaxCapacity()` | 容量总和与当前 `MAX_CAPACITY` 差 < DELTA 直接 return，消除「清零+加回」操作级 fire |
| C. 资源发送端值级去重 | `DataSyncCache.lastSyncedResources` | 当前值 == 上次已同步值跳过发包（仅资源层；建筑/居民不设指纹——服务器规模大、内存敏感） |

### 14.5 客户端侧

- 三增量包 `handle()` → `applyXxxUpdate`（覆盖式 merge + 移除，**不回 fire**）。
- GUI 数据面板 render 阶段经 Supplier 实时取数，收包不重建界面（见 §9）；`TeamTownDataS2CPacket` 替换实例不影响（Supplier 每帧重新解析）。
- 全量包序列化成功后调 `markFullSynced()` **清空**资源去重基线：基线空 → `isResourceUnchanged` 恒 false → 下一轮 flush 对所有脏资源键强制发包（2026-08-07 修复：全量包仅单播给单个玩家而基线全队共享，若按当前值重建基线会吞掉其他玩家窗口内（已标记未 flush）的资源增量；清空则双向安全，代价是首次 flush 多一次冗余资源包）。
