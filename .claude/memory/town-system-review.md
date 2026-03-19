# Town系统代码Review

> 审阅日期：2026-03-19
> 审阅范围：`com.teammoeg.frostedheart.content.town` 全部约96个文件

## 整体评价：架构有想法，但执行粗糙，处于半成品状态

系统涵盖建筑、居民、资源、地形资源等子系统，设计意图是类似《冰汽时代》风格的城镇模拟。思路不错，但实现层面问题不少。

| 维度 | 评分 | 说明 |
|------|------|------|
| 架构设计 | 6/10 | 有模式意识，但抽象层级混乱，存在上帝类 |
| 代码健壮性 | 4/10 | 多处NPE/CME隐患，线程安全缺失 |
| 代码整洁度 | 4/10 | 大量注释残留、命名不统一、dead code |
| 完成度 | 5/10 | MineBuilding空壳、DEBUG永远开着 |
| 可扩展性 | 5/10 | 建筑类型硬编码，缺少事件系统 |

---

## 一、架构设计（6/10）

### 做得好的

- **资源Action系统**（Command模式）设计得比较完整，`ITownResourceAction` → `ITownResourceActionExecutor` → `ITownResourceActionResult` 三件套，支持 ATTEMPT/REQUIRE/MAXIMIZE 三种模式，考虑了部分消耗的场景。
- **Facade模式**：`TeamTown` 包装 `TeamTownData`，避免外部直接操作持久化数据。
- **Codec序列化**与Minecraft生态一致。

### 问题

#### 1. `Town` 接口过于贫瘠

只有一个 `getActionExecutorHandler()` 方法，作为顶层抽象几乎没有约束力。`ITownWithResidents`、`ITownWithBuildings` 被拆成独立接口，导致下游代码到处 `instanceof` 判断：

```java
// HouseBuilding.java:162
if(! (town instanceof ITownWithResidents)){
    throw new IllegalArgumentException(...);
}
// HouseBuilding.java:245
if(town instanceof ITownWithResidents residentTown){...}
```

一个 `work(Town)` 方法里就有两处 instanceof，说明接口拆分方式不对。住宅本来就需要居民，不该在运行时再去判断。

#### 2. `TeamTownData` 是个"上帝类"

tick逻辑、方块检查、居民分配、建筑工作、资源回收全在一个类里（约400行），每加一个功能就要往里塞。`tickMorning()` 串行调用7个方法，逻辑耦合严重。

```java
// TeamTownData.java:155-166
public void tickMorning(ServerLevel world) {
    this.checkBlocks(world, town);
    this.checkOccupiedAreaOverlap();
    this.tickResidentsMorning();
    this.residentAllocatingCheck(town);
    this.allocateHouse();
    this.assignWork();
    this.buildingsWork();
    this.recoverResources();
}
```

#### 3. 建筑类型硬编码在Codec里

```java
// ITownBuilding.java:18-25
CodecUtil.dispatch(ITownBuilding.class)
    .type("house", HouseBuilding.class, HouseBuilding.CODEC)
    .type("huntingCamp", HuntingCampBuilding.class, HuntingCampBuilding.CODEC)
    .type("mine", MineBuilding.class, MineBuilding.CODEC)
    // ...
    .buildByInt();
```

加新建筑类型就要改这个接口文件，违反开闭原则。应该用注册表模式。

---

## 二、具体Bug与隐患

### Bug 1: `getResident()` 会NPE

**文件**：`TeamTown.java:137`

```java
public Optional<Resident> getResident(UUID id){
    return Optional.of(data.residents.get(id));
}
```

`Optional.of(null)` 会抛 `NullPointerException`。应该用 `Optional.ofNullable()`。

### Bug 2: `removeResident(String, String)` 遍历时修改集合

**文件**：`TeamTown.java:191`

```java
for (Entry<UUID, Resident> entry : getResidents().entrySet()) {
    if (...) {
        getResidents().remove(entry.getKey()); // ConcurrentModificationException!
        removed = true;
    }
}
```

对 `LinkedHashMap` 在 for-each 中调 `remove()`，必定抛 `ConcurrentModificationException`。应使用 `Iterator.remove()` 或先收集待删除key再批量删除。

### Bug 3: `removeTownBlock()` 无null检查

**文件**：`TeamTown.java:124`

```java
AbstractTownBuilding building = data.buildings.remove(pos);
building.onRemoved(this); // building可能为null
```

如果 `pos` 不在 map 中，`remove()` 返回 null，下一行直接 NPE。

### Bug 4: `Resident.deserialize()` 返回 null

**文件**：`Resident.java:265`

方法签名返回 `Resident`，但最后 `return null`。同时存在 Codec 和手动 NBT 序列化两套方案，明显是遗留代码没清理。

### Bug 5: 静态缓存线程不安全

**文件**：`TeamTownResourceHolder.java:74`

```java
private static final Map<ItemResourceAttribute, HashSet<ItemStackResourceKey>>
    ITEM_RESOURCE_ATTRIBUTE_CACHE = new HashMap<>();
```

`static` 缓存被多个 Town 实例共享，在多线程（Minecraft服务端tick与网络线程）环境下没有同步保护。

### Bug 6: `MineBuilding.work()` 是空的

**文件**：`MineBuilding.java:98-100`

```java
@Override
public boolean work(Town town) {
    return super.work(town); // 即 return true; 矿场不产出任何东西
}
```

### Bug 7: DEBUG_MODE 硬编码为 true

**文件**：`Town.java:37`

```java
public static final boolean DEBUG_MODE = true;//todo: 正式发布记得删掉
```

Debug模式下：
- 居民不会死亡（`TeamTownData.java:238`）
- 温度检查被跳过（`HouseBuilding.java:140`）

---

## 三、代码质量

### 1. 注释和命名混乱

- 中英文混杂，部分注释过时
- 被注释掉的代码块大量残留（`HouseBuilding.java` 里的 foodTypes 数组、balanceScore 公式等）
- 变量名不一致：`maypickTerrainResource` 缺少驼峰（应为 `mayPickTerrainResource`）

### 2. 资源系统的类型层级过于复杂

- `ITownResourceType` → `ITownResourceAttribute` → `ITownResourceKey` 三层抽象
- `IGettable` 接口让 `get()` 方法需要三重 `instanceof` 判断
- 为了区分"物品资源"和"虚拟资源"引入了大量平行类（`ItemResourceType`/`VirtualResourceType`、`ItemResourceAttribute`/`VirtualResourceAttribute`）
- 这套东西的复杂度和它解决的问题不成正比

### 3. `HouseBuilding.work()` 过于臃肿

一个方法约120行，混合了食物消耗、营养计算、居民属性更新三个完全不同的关注点。内部有大量被注释掉的旧逻辑残留。

### 4. `TeamTownResourceActionExecutorHandler` 内部类膨胀

所有6个Executor作为内部类写在一个文件里（约200行）。每个Executor的逻辑高度相似（检查→判断模式→执行→返回结果），但都是独立手写的，没有提取公共模板。

### 5. 可见性控制缺失

`AbstractTownBuilding` 中多个字段是 `public`：

```java
public boolean initialized = false;
public boolean occupiedAreaOverlapped = false;
public boolean isStructureValid = false;
public OccupiedArea occupiedArea = OccupiedArea.EMPTY;
```

被外部直接赋值：
```java
building.occupiedAreaOverlapped = overlappedWorkers.contains(building);
```

---

## 四、设计缺失

### 1. 没有事件/回调机制

居民加入/离开、建筑建造/拆除、资源变化等都没有触发事件，未来要做UI更新或成就系统会很痛苦。

### 2. 没有日志/审计

资源消耗只返回结果对象，没有任何持久化记录。调试城镇经济平衡会很困难。

### 3. 居民分配算法效率

`assignWork()` 用 PriorityQueue + 全量遍历匹配，每次 poll 后重新 add。N个居民 M个建筑时间复杂度接近 O(NM)，居民多了会有性能问题。

### 4. 缺少单元测试

这么复杂的经济模拟系统，没有看到对应的测试，纯靠游戏内手动测试不现实。

---

## 五、优先修复建议

### P0 — 立即修复（会导致崩溃）

| # | 问题 | 文件 | 行号 |
|---|------|------|------|
| 1 | `Optional.of()` → `Optional.ofNullable()` | `TeamTown.java` | 137 |
| 2 | for-each中remove导致CME | `TeamTown.java` | 191 |
| 3 | `removeTownBlock()` null检查 | `TeamTown.java` | 124 |

### P1 — 尽快修复（逻辑错误/隐患）

| # | 问题 | 文件 |
|---|------|------|
| 4 | 静态缓存线程安全 | `TeamTownResourceHolder.java` |
| 5 | `Resident.deserialize()` 返回null | `Resident.java` |
| 6 | DEBUG_MODE 开关机制 | `Town.java` |

### P2 — 代码清理

| # | 问题 |
|---|------|
| 7 | 清除 `HouseBuilding.work()` 中的注释残留代码 |
| 8 | 删除 `Resident` 中的手动NBT序列化（已有Codec） |
| 9 | 统一命名风格（`maypick` → `mayPick`） |

### P3 — 架构改进

| # | 问题 |
|---|------|
| 10 | 拆分 `TeamTownData` tick逻辑到独立 handler |
| 11 | 建筑类型注册表化 |
| 12 | 简化资源类型三层抽象 |
| 13 | 实现 `MineBuilding.work()` |
| 14 | 添加单元测试 |
