# FrostedHeart — `town` 包开发者参考（AI Generated）

> 路径：`src/main/java/com/teammoeg/frostedheart/content/town`
> 适用版本：当前仓库 `main`（最近修改 2026-07）

本文件汇总 `town`（城镇）系统的关键架构、常用类与典型扩展方式，供日常开发与排查使用。
内容基于源码阅读，所有路径相对于 `town` 包根目录。

---

## 1. 一句话架构

城镇系统采用 **「数据 / 视图分离」+ 分层接口** 的设计：

- **`TeamTownData`（`implements SpecialData`）** —— 唯一的、可持久化的城镇数据，挂在**队伍数据**（`TeamDataHolder`）上，用 Mojang `Codec` 序列化。
- **`TeamTown`** —— `TeamTownData` 的轻量门面/视图（`implements ITown, ITownWithResidents, ITownWithBuildings`），只暴露读写入口，不存状态。
- **`AbstractTownBuildingBlockEntity`** —— 方块放置时创建对应的 `AbstractTownBuilding`，并注册进城镇的 `buildings` 映射。
- 每个游戏日（`tickMorning`）统一校验结构、分配居民、链接矿场、执行建筑工作、更新资源。

---

## 2. 目录结构（精简）

```
town/
├── ITown.java / ITownWithBuildings / ITownWithResidents / ITownWithResources.java   # 核心接口
├── TeamTown.java                # 门面
├── TeamTownData.java            # 持久化数据 + 每日主流程 tickMorning
├── ChunkTownResourceCapability.java
├── TownMathFunctions.java       # 评分/温度工具
├── block/                       # 方块与方块实体基类、占用体积
│   ├── AbstractTownBuildingBlock / ...BlockEntity / TownBlockEntity(接口)
│   ├── OccupiedVolume.java
│   └── blockscanner/            # 结构扫描（Floor/ConfinedSpace/Building）
├── building/                    # 建筑逻辑抽象
│   ├── AbstractTownBuilding / AbstractTownResidentWorkBuilding
│   ├── ITownBuilding(Codec分派) / ITownResidentBuilding / ITownResidentWorkBuilding
│   └── buildings/{house,hunting,mine,warehouse}/   # 四件套具体建筑
├── resource/                    # 资源系统（Holder / Action / Type / Attribute）
│   ├── TeamTownResourceHolder / TownResourceManager(已废弃)
│   ├── TeamTownResourceActionExecutorHandler
│   └── action/                  # 命令式资源动作
├── resident/                    # Resident / Family / WanderingRefugee
├── terrainresource/             # TerrainResourceType / TerrainResourceData
├── event/                       # 三类变更事件 + 监听器
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

`implements SpecialData`，`CODEC` 字段：`name / resources / blocks / residents / terrainResource / labour / maxLabour`。
所有「永久状态」都在此处；`TeamTown` 只是指向它的视图。

**每日主流程 `tickMorning(ServerLevel)`** 依次：

1. `checkBlocks` —— 移除已失效/方块被改的建筑（与 `TownBlockEntity.getBuilding` 一致性校验）。
2. `checkOccupiedAreaOverlap` —— 用 `OccupiedVolume.intersects` 两两比对，重叠的双方 `occupiedAreaOverlapped=true`（导致 `isBuildingWorkable` 失败）。
3. `tickResidentsMorning` —— `health<=5` 或 `mental<=5` 判定死亡（`DEBUG_MODE` 下跳过）；无房者 `costHealth(10)`。
4. `residentAllocatingCheck` —— 清空所有居民的 house/work 位置后从建筑回写，并剔除超限/已亡居民。
5. `allocateHouse` —— 按 `HouseBuilding.getRating()` **降序** 优先分配无房居民。
6. `assignWork` —— 工作建筑入优先级队列（`getResidentPriority()` 降序），对每个建筑挑 `getResidentScore` 最高的可用居民。
7. `linkMinesToBases` —— 按 `MineBaseBuilding.getConnectionRadius()` 把范围内的 `MineBuilding` 链到矿基。
8. `recalcOreChunkResources` —— 设定 `ORE` 的活跃区块（`setTerrainResourceTypeActiveChunks`）。
9. `buildingsWork` —— `reloadMaxCapacity()` 后按 `getWorkPriority()` 降序执行 `building.work(town, world)`。
10. `recoverResources` —— 地形资源按配置的 `recoverSpeed` 恢复。

---

## 4. 建筑系统

### 4.1 抽象层

- **`ITownBuilding`**（旧称 TownWorker，建筑逻辑核心）：自带**类型分派 `Codec`**，新增建筑必须在此登记（见 §6）。关键方法：
  ```java
  boolean isBuildingWorkable();
  boolean work(ITownWithBuildings town, ServerLevel world);
  default int getWorkPriority();   // 默认 0，越大越先工作
  void onRemoved(ITownWithBuildings town);
  ```
- **`AbstractTownBuilding`** 关键状态：
  ```java
  protected final BlockPos pos;
  public boolean initialized = false;            // 是否完成首次扫描
  public boolean occupiedAreaOverlapped = false; // 是否与别处重叠
  public boolean isStructureValid = false;       // 结构是否合法
  public OccupiedVolume occupiedVolume;          // 占用体积
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
| House | `buildings/house/HouseBuilding` | 否（住宅） | 仅 `implements ITownResidentBuilding`；`work()` 每日按配置消耗一次 `RESIDENT_FOOD_LEVEL`，由食物满足度、营养质量、有效温度和综合舒适度线性计算生命/精神的损失与恢复，不再改变力量；最近一次结算写入 `DailyReport`，`getRating()` 决定分房优先级。 |
| Hunting Base | `buildings/hunting/HuntingBaseBuilding` | 是 | 继承 `AbstractTownResidentWorkBuilding`；按居民 score 总和决定投掷次数，受 `TerrainResourceType.HUNT` 限制，用战利品表 `town/hunting` 产出并 ADD 进仓库。 |
| Mine Base | `buildings/mine/MineBaseBuilding` | 是 | 持有 `Set<BlockPos> linkedMines`；汇总有效 `MineBuilding` 权重，按区块向 `ORE` 开采。 |
| Mine | `buildings/mine/MineBuilding` | 否（标记） | 仅扫描/标记；`BiomeMineResourceRecipe` 提供生物群系矿产权重（`getWeights(biome)`）。 |
| Warehouse | `buildings/warehouse/WarehouseBuilding` | 否 | `addCapacity(ITown)` 把 `capacity` 作为 `MAX_CAPACITY` 资源加入城镇；每日 `reloadMaxCapacity()` 先清零再累加。配套 `WarehouseMenu`/`WarehouseScreen`/`WarehouseBlockScanner`。 |

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
- **`ItemResourceAmountRecipe`**：配方类（IE 配方），定义「某物品 → 某资源 Tag 的转化量」；`TeamTownResourceHolder.loadItemResourceAmounts()` 加载进缓存。

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

- **`Resident`**（抽象模拟数据，非实体）：字段 `uuid, firstName, lastName, health, mental, strength, intelligence`(0~100), `educationLevel`, `Map<String,Double> workProficiency`（key = 建筑类 `getSimpleName()`，如 `"HuntingBaseBuilding"`）, `housePos`, `workPos`。提供 `get/add/cost` 系列（越界抛异常/夹取）。`setDeath(town)` 调用 `town.removeResident(uuid)`。
- **`Family`**：简单包装 `Resident[] + lastName`，目前仅数据容器。
- **`ResidentEntity extends Mob`**：预留实体类，目前为空（居民以 `Resident` 数据形式活在 `TeamTownData`，不生成单独实体）。
- **`WanderingRefugee`**：流浪难民实体（`extends AbstractVillager implements NeutralMob, VillagerDataHolder`），可被招募为居民或交易；`mobInteract` 打开交易界面，`WanderingRefugeeRecruitMessage` 处理招募（生成粒子、移除实体、写入 `TeamTown.addResident`）。

---

## 8. 地形资源

- **`TerrainResourceType`**（枚举）：`WOOD, ORE, HUNT, POI, SALVAGE`，各自由 `FHConfig` 提供 `recoverSpeed` 与 `resourcePerSq`。代表野外可再生资源。
- **`TerrainResourceData`**：单类型储量（`extracted/total/radius`，半径由 `extracted` 与 `resourcePerSq` 反算）。`ChunkResourceTracker`（`Map<ChunkPos,Double>` + 临时 `activeChunks`）仅 `MineBaseBuilding` 使用以确定逐区块可采量。
- 访问入口：`TeamTown.pickTerrainResource/unpickTerrainResource/maypickTerrainResource`（全局或按区块）。

---

## 9. 事件 / 网络 / GUI

- **事件**（`event/`）：基于 `java.util.EventObject` 的轻量事件：`TownBuildingChangeEvent` / `TownResidentChangeEvent` / `TownResourceChangeEvent`，及对应监听器接口。当前 `TeamTownData.dataSyncCache` 虽实现了三个监听器，方法体为空（细粒度同步尚未启用）。`TownCommonEvents` 负责给区块挂 `chunk_town_resource` Capability，以及（调试期）向玩家全量同步 `TeamTownDataS2CPacket`。
- **网络**（`network/`，均为 `CMessage`）：
  - `TeamTownDataS2CPacket`：S→C 全量同步（GUI 实际依赖它）。
  - `TownBuildingUpdatePacket` / `TownResidentUpdatePacket` / `TownResourceUpdatePacket`：**空实现占位**，尚未使用。
  - `WarehouseUpdatePacket` / `WarehouseInteractPacket`：仓库物品同步与存取（C→S 改资源并回写玩家手持物）。
  - `WanderingRefugeeOpenTradeGUIMessage` / `WanderingRefugeeRecruitMessage`：难民交易/招募。
- **GUI**（`tabs/` + `AbstractTownWorkerBlockScreen`）：通用工人方块界面，左侧 Tab 列表；仓库使用 `TownInformationTab` / `TownResourceTab`。住宅使用 `HouseScreen`，概览页展示最近一次日结，居民页从客户端当前的整份城镇快照读取居民属性并支持逐人查看；住宅没有额外同步包。

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
7. **细粒度同步未启用**：`TownBuilding/Resident/ResourceUpdatePacket` 目前为空实现，GUI 暂时依赖 `TeamTownDataS2CPacket` 全量同步。
8. **居民以数据形式存在**：`Resident` 存于 `TeamTownData.residents`，不生成独立实体（`ResidentEntity` 预留为空）。

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

---

> 注：`util/` 目录当前为空；`TeamTownData.dataSyncCache` 监听器方法体为空，细粒度客户端同步待实现。
