# 城镇系统 NBT -> POJO 迁移指南

目前我们的城镇系统（`TeamTownData` 和 `TownWorker`）严重依赖 `CompoundTag` (NBT) 来在运行时存储和传递数据。虽然这满足了“区块卸载后依然运行逻辑”的需求，但也导致了代码难以调试、类型不安全以及充斥着大量的 Hardcoded String (魔术字符串)。

本指南详细说明了将现有系统迁移到基于 **POJO (Plain Old Java Objects)** 的内存模型（In-Memory Model）的策略。

## 核心目标

1.  **类型安全**: 将 NBT 读写替换为 Java 对象的字段访问。
2.  **调试友好**: 可以直接在 IDE 中查看对象状态，而不是对着 NBT 字符串发呆。
3.  **保持离线运行**: 数据依然存储在全局的 `TeamTownData` 中，不依赖 `BlockEntity` 的存在。
4.  **按需序列化**: NBT 仅在存档/读档时使用，不再作为运行时的内存载体。

---

## 1. 架构重构蓝图

### 第一步：创建状态基类 (`WorkerState`)

我们需要一个基类来统一管理数据的序列化和同步状态。

```java
// src/main/java/com/teammoeg/frostedheart/content/town/state/WorkerState.java

public abstract class WorkerState {
    protected boolean isDirty = false; // 标记数据是否改变，用于决定是否需要保存

    /**
     * 从 NBT 读取数据 (Load)
     */
    public abstract void load(CompoundTag nbt);

    /**
     * 将数据保存为 NBT (Save)
     */
    public abstract CompoundTag save(CompoundTag nbt);

    /**
     * (可选) 当对应的区块加载时，从 BlockEntity 同步数据到由于 State
     * 例如同步温度、装饰度等实时数据
     */
    public void syncFromBlockEntity(CompoundTag teData) {
        // 默认实现为空
    }
    
    public void markDirty() {
        this.isDirty = true;
    }
}
```

### 第二步：为每种 Worker 实现具体的 State 类

把所有的 "Magic Strings" 变成具体的 Java 字段。以 `HOUSE` 为例：

**迁移前 (NBT):**
```java
// 需要记得 key 是 "rating" 还是 "Rating"
double rating = nbt.getDouble("rating"); 
```

**迁移后 (POJO):**
```java
// src/main/java/com/teammoeg/frostedheart/content/town/state/HouseState.java

public class HouseState extends WorkerState {
    // 直接使用强类型的 List，而不是 NBTTagList
    private List<UUID> residents = new ArrayList<>();
    
    // 具体的业务字段
    private double rating;
    private double temperatureRating;
    private double decorationRating;

    @Override
    public void load(CompoundTag nbt) {
        // 反序列化逻辑
        this.rating = nbt.getDouble("rating");
        this.temperatureRating = nbt.getDouble("temperatureRating");
        // ... list loading ...
    }

    @Override
    public CompoundTag save(CompoundTag nbt) {
        // 序列化逻辑
        nbt.putDouble("rating", this.rating);
        // ... list saving ...
        return nbt;
    }

    // Getters
    public List<UUID> getResidents() { return residents; }
    public double getRating() { return rating; }

    // Setters (自动标记 dirty)
    public void setRating(double rating) {
        this.rating = rating;
        this.markDirty();
    }
}
```

### 第三步：改造 `TownWorkerData`

`TownWorkerData` 不再持有 `CompoundTag workData`，而是持有 `WorkerState`。

```java
public class TownWorkerData {
    // [DELETE] private CompoundTag workData;
    // [NEW]    private WorkerState state;
    
    private TownWorkerType type;
    private BlockPos pos;

    // 构造函数 (从 NBT 加载时)
    public TownWorkerData(CompoundTag nbt) {
        this.type = TownWorkerType.valueOf(nbt.getString("type"));
        this.pos = BlockPos.of(nbt.getLong("pos"));
        
        // 工厂模式创建对应的 State 对象
        this.state = this.type.createState(); 
        
        // 加载数据
        if (nbt.contains("data")) {
            this.state.load(nbt.getCompound("data"));
        }
    }

    // 序列化
    public CompoundTag serialize() {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", type.name());
        tag.putLong("pos", pos.asLong());
        
        // 只有当 state 不为空时才保存
        if (state != null) {
            tag.put("data", state.save(new CompoundTag()));
        }
        return tag;
    }
    
    // 获取泛型的 State，方便强转
    public WorkerState getState() {
        return state;
    }
}
```

### 第四步：改造 `TownWorkerType` 枚举

在枚举中注册 `State` 的构造工厂。

```java
public enum TownWorkerType {
    // 传入构造器引用
    HOUSE(..., HouseState::new),
    MINE(..., MineState::new),
    DUMMY(..., null);

    private final Supplier<WorkerState> stateFactory;

    TownWorkerType(..., Supplier<WorkerState> stateFactory) {
        // ...
        this.stateFactory = stateFactory;
    }

    public WorkerState createState() {
        return stateFactory != null ? stateFactory.get() : null; // 或者返回一个 EmptyState
    }
}
```

---

## 2. 逻辑层迁移示例 (`HouseWorker.java`)

逻辑代码将变得非常清晰。

**迁移前 (NBT Hell):**
```java
public boolean work(Town town, CompoundTag workData) {
    ListTag list = workData.getCompound("tileEntity").getList("residents", Tag.TAG_STRING);
    // 还要手动转换 UUID...
    double temp = workData.getCompound("tileEntity").getDouble("temperatureRating");
    // ...
    // 如果忘记 put 回去，修改就丢失了
    workData.put("residents", list); 
}
```

**迁移后 (Clean Java):**
```java
public boolean work(Town town, TownWorkerData data) {
    // 1. 类型检查与转换
    if (!(data.getState() instanceof HouseState house)) return false;

    // 2. 直接访问字段
    List<UUID> residents = house.getResidents();
    double temp = house.getTemperatureRating();

    // 3. 业务逻辑
    if (conditionsMet) {
        // 直接调用方法修改状态
        house.setRating(house.getRating() + 1.0); 
        // 甚至可以写更高级的业务方法
        house.addResident(newResidentUUID);
    }
    
    return true; // 不需要手动保存，State 对象内部管理 dirty 状态
}
```

## 3. 注意事项

1.  **区块加载同步 (`syncToBlockEntity` / `updateFromBlockEntity`)**:
    *   目前逻辑中，`TownWorkerData.updateFromTileEntity` 会把 TE 的 NBT 拷过来。
    *   迁移后，需要实现 `HouseState.updateFromTileEntity(HouseTileEntity te)`。
    *   **关键**: 只有当 chunk 加载时才调用此方法。`TownWorkerData` 中保留 `boolean loaded` 标记。如果 `loaded == true`，则从 TE 读取最新的“环境数据”（如机器温度、方块状态），更新到 POJO 中，用于接下来的模拟。

2.  **兼容性**:
    *   存档格式本质上没有发生结构性变化（依然是 `type`, `pos`, `data`），只是 `data` 内部的字段由 State 类的 `save/load` 控制。
    *   建议在 `load` 方法中做一些兼容性检查，防止旧存档的 NBT 结构与新代码不匹配导致的空指针。

3.  **渐进式迁移**:
    *   不需要一次性迁移所有 Worker。
    *   可以先修改 `TownWorkerData` 让它同时支持 `CompoundTag` (旧) 和 `WorkerState` (新)。
    *   逐个将 `House`、`Mine` 等迁移到新的 State 系统。
