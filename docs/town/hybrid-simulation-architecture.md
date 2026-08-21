# 城镇居民混合模拟架构（Hybrid Citizen Simulation）

- Status: `Transitional`
- Last verified: `2026-08-21`
- Scope: `居民服务端模拟、睡眠展示、AOI 预算、同步、客户端缓存与渲染`
- Code anchors: `CitizenSim`, `CitizenSimScheduler.refreshRegistry`, `CitizenSimScheduler.rebuildActiveIds`, `BehaviorSystem.tick`, `MovementSystem.tickAll`, `SpatialGrid.rebuildVisibility`, `SpatialGrid.queryVisible`, `CitizenPresence`, `TownSimData.onSleepEntered`, `SyncEngine.collectPlayerCandidates`, `SyncEngine.flushDeltas`, `ClientCitizen`, `FakeCitizenManager`, `CitizenRenderCoordinator`, `CitizenRenderBackend`, `CpuBatchCitizenBackend`, `FlywheelCitizenBackend`, `CitizenInstanceData`, `CitizenInstanceType`, `CitizenFlywheelModels`, `assets/frostedheart/flywheel/shaders/citizen.vert`, `ClientCitizenRenderer`, `CitizenClientBenchmark`, `CitizenRenderMetrics`, `FHConfig.SERVER.TOWN`

> 适用环境：Minecraft 1.20.1 / Forge 47.3.0 / Java 17（本仓库 Frosted Heart）
> 目标规模：单机/联机下 **数千至上万** 个有独立行为的城镇居民，服务端近零实体开销，客户端流畅可见。
> 核心思想：**服务端纯数据模拟 + 客户端纯表现渲染，网络包同步，完全绕过 `Entity` 体系。**

---

## 1. 总体架构

```
┌─────────────────────────── 服务端 (Logical Server) ───────────────────────────┐
│                                                                               │
│  CitizenSimScheduler (per-level 运行期注册表, 不持久化)                        │
│   ├── TownSimData       每镇一份模拟（玩家镇按队伍 id / AI 镇内嵌，存全局文件）│
│   │                     居民增删由城镇事件直接驱动（无周期性对账同步器）        │
│   ├── UnmanagedCitizenData (SavedData, per-level) 命令生成的未托管居民+id分配  │
│   ├── CitizenSim        SoA 紧排数组存储（id/位置/速度/状态/目标/需求）        │
│   ├── active stable IDs 每 20t 按容器重建，只驱动玩家附近的运行期条目          │
│   ├── BehaviorSystem    消费 active IDs 的轻量状态机，按 id%20 分帧            │
│   ├── NavSystem         分层寻路：城镇路网图 + 局部流场(Flow Field)            │
│   │                      └── 异步线程池执行 BFS/路径请求，主线程消费结果        │
│   ├── SpatialGrid       2 格邻居网格 + 16 格 AOI 粗网格，均存稳定 id           │
│   └── SyncEngine        粗桶候选+精确 AOI；tracked union 脏检查与预算分包      │
│                                                                               │
└───────────────────────────────┬───────────────────────────────────────────────┘
                                 │ SimpleChannel（chorda CBaseNetwork）
                                 │ 全量(进入AOI) / 增量(移动批) / 事件(状态切换)
┌───────────────────────────────┴───────────────────────────────────────────────┐
│                          客户端 (Logical Client)                              │
│  ClientCitizenCache    id → 渲染状态（带双缓冲插值快照）                        │
│  CitizenRenderCoordinator 统一 packet/tick/render/reload/clear 与 backend 所有权 │
│   ├── CpuBatchCitizenBackend → AUTO 不可用时的低模与轮廓兼容回退              │
│   └── FlywheelCitizenBackend → AUTO 首选 / 显式诊断的动态持久实例            │
│  InteractionHooks      准星射线查空间网格 → C2S 交互请求 → 打开菜单             │
│  （换维度时清空缓存，与服务端 per-level id 空间对齐）                          │
└───────────────────────────────────────────────────────────────────────────────┘
```

建议包结构（挂在现有 town 内容下）：

```
frostedheart/content/town/citizen/
├── sim/            服务端模拟：CitizenSim, CitizenContainer(统一容器接口),
│                   TownSimData(镇容器,存全局文件), UnmanagedCitizenData(未托管容器),
│                   CitizenSimScheduler(per-level 调度器), BehaviorSystem
├── AITownData      独立 AI 镇（无队伍，居民+模拟自包含，implements ITownWithResidents）
├── AITownManager   AI 镇注册表 + 全局模拟存储（overworld SavedData "fh_ai_towns"）
├── nav/            寻路：TownRoadGraph, FlowField, NavJobExecutor
├── sync/           同步：SyncEngine 与各 S2C/C2S 包
├── client/         客户端：ClientCitizenCache, ClientCitizenRenderer, FakeCitizenEntity
└── data/           定义：CitizenType(Codec), JobDef 等 datapack 数据
```

> **挂靠语义（2026-08-12 重构）**：模拟完全挂靠 town、与队伍零关联——`TownSimData`
> 是共用模拟类，玩家镇与 AI 镇各持一份实例，统一存**全局单文件**
> （`AITownManager`，overworld SavedData "fh_ai_towns"：AI 镇列表 + 玩家镇模拟表
> key=队伍 holder id），不挂 TeamDataHolder（CITIZEN_SIM_DATA SpecialData 已删除）。
> 居民增删直接驱动模拟出生/移除（事件驱动，**门面事件单订阅**：
> `ObservableTownMap` 是 DataSyncCache 增量更新的专用组件（三个单回调钩子，职责
> 不扩张）；玩家镇居民生命周期事件由 `TeamTownData` 门面承载——单引用监听器
> （`ITownResidentListener`，模拟是唯一订阅者，adopt 时 `setResidentListener`），
> fire 点全在 `TeamTown`：`addResident` 成功路径/`debugAddResident` 在房屋分配
> 完成后 fire 一次 `onResidentAdded`（锚点必已就绪、无双触发）、`removeResident`
> 集合移除后 fire `onResidentRemoved`、tickMorning 末尾 `fireMorningDone`），
> 每日结算刷新锚点，调度器首次接管时一次性恢复（rebindAll）。
> **AI 镇是独立 Town**（`AITownData` implements ITownWithResidents）：不伪造
> AbstractTeam、不建队伍 holder、不参与每日结算（调试镇稳定不演化）；居民增删
> 直调 sim 事件回调 + 结构变更 dirty 标记。落盘遵循标准 SavedData 语义——
> 位置、朝向、行为状态、目标、锚点等权威字段变化也只设置 dirty 内存标志
> （不立即写盘），由 Minecraft 内建 6000t 自动保存/停服统一写盘，并在正常
> 停服前做一次兜底标记。不再有 1Hz 全局对账同步器；
> `/fhcitizen clear` 只清未托管命令居民。

---

## 2. 服务端数据层：SoA 紧排存储

万级单位的第一原则：**不用对象数组（AoS），用基本类型平行数组（SoA）**。一万个 `Citizen` 对象意味着一万次 GC 根扫描 + 指针跳转 cache miss；SoA 则是一段连续内存，顺序扫描，CPU 缓存友好，GC 几乎无感。

```java
/**
 * 居民模拟核心数据，SoA 布局。
 * <p>
 * Core citizen simulation data in Structure-of-Arrays layout.
 */
public final class CitizenSim {
    // 容量管理与 id 复用
    private int capacity;
    private int size;                 // 存活数量（高位压缩交换删除）
    private int[] ids;                // 外部稳定 id（自增，永不复用）
    private final Map<Integer, Integer> idToIndex = new Int2IntOpenHashMap(); // fastutil

    // 位置：定点数，单位 1/1024 方块（比 float 更稳、同步更省）
    private int[] px, py, pz;         // pos = value / 1024.0
    // 速度：1/1024 方块每 tick
    private short[] vx, vy, vz;
    // 朝向：yaw 用 byte（256 级，1.4° 精度，渲染插值后完全够用）
    private byte[] yaw;
    // 行为状态：0=睡觉 1=工作 2=通勤 3=吃饭 4=闲逛 5=聚集 ...
    private byte[] state;
    // 移动路径引用：指向 FlowField / 路网节点；0 表示静止
    private int[] navRef;
    // 归属与目标
    private int[] homeChunk;          // packed ChunkPos，用于活跃度判断
    private int[] targetX, targetZ;   // 当前行为目标（方块坐标）
    // 模拟节律：每单位下次逻辑 tick 的相位（分帧用）
    private byte[] tickPhase;
    // 需求条等低频数据可以另挂稀疏 Map，不进热数组
}
```

要点：

- **交换删除**：移除 index i 时把 `size-1` 处数据搬过来，O(1)，不移动其他元素，但需同步维护 `idToIndex`。
- **定点数**：位置用 `int` 定点（1024 细分），yaw 用 `byte`。同步时直接原样发包，客户端 `pos/1024.0` 还原并插值——省掉浮点序列化的带宽与精度抖动。
- **稀疏扩展**：名字、皮肤种子、对话状态等冷数据放 `Int2ObjectOpenHashMap<CitizenProfile>`，不污染热循环。
- 依赖已有 `fastutil`（Forge 自带 shaded / 直接可引），避免装箱。

**内存口径**：当前 `CitizenSim` 有 17 个 `int[]`、5 个 `long[]` 和 7 个 `byte[]`，合计 **115 B/capacity slot**。1 万个已分配槽位的原始数组元素约 **1.10 MiB**；该数字不含 29 个数组对象头、`idToIndex`、容器对象、容量余量和权威 `Resident`。`TownSimData` 与 `UnmanagedCitizenData` 初始容量为 16，达到上限后按 2 倍扩容。

---

## 3. 模拟调度：分帧 + 活跃度 LOD

万级总人口不能每 tick 全量扫描。当前运行期以活跃稳定 ID 列表和行为分片控制热路径：

### 3.1 分帧（Time Slicing）

`SLICE = 20`，`BehaviorSystem.tick` 以 `stableId % SLICE` 决定居民回合，意味着每个活跃居民以 **1Hz** 做一次行为决策。调度器每 20 tick 重建与 `containers` 顺序对齐的 `IntArrayList` 活跃 ID 列表；行为和移动消费该列表，不扫描非活跃行。列表保存稳定 ID，并在消费时用 `CitizenSim.indexOf` 解析当前行，避免 swap-remove 后误用旧索引。

```java
public void serverTick(ServerLevel level) {
    long gameTime = level.getGameTime();
    if (gameTime % 20 == 0) {
        refreshActivity(level);
        refreshRegistry(level);                    // 双缓冲 registry 完整构建后交换
        rebuildActiveIds();
    }
    behavior.tick(this, level, (int) (gameTime % 20), gameTime);
    movement.tickAll(this, level, gameTime);
    sync.flush(this, level, gameTime);             // 发送增量快照
}
```

- 行为候选复杂度为 `O(active citizens)`，实际决策约为活跃人口的 `1/20`；移动候选同样只随活跃人口增长。
- `refreshRegistry` 复用 active/scratch 两套 `List` 和 primitive map/set；完整构建下一代注册表后才交换引用，避免每秒创建重建集合或发布半成品。

### 3.2 活跃度 LOD（关键）

| 层级 | 条件 | 行为频率 | 移动 |
|------|------|----------|------|
| ACTIVE | 当前 2 格 cell 位于任一玩家 128 格范围（另有 2 格缓冲） | 1Hz | 每 tick 积分；清醒居民参与分离 |
| INACTIVE | 不在 active cells | 不进入行为热循环 | 冻结；移动状态写 `halt=1` |

`CitizenSimScheduler.refreshActivity` 每 20 tick 依据当前玩家位置重建 `activeCells`，随后 `rebuildActiveIds` 扫描一次注册人口。居民在两次刷新之间自行走出 active cell 时，`MovementSystem` 的防御检查会立即移除该 ID 并置 halt；玩家移动造成的新活跃区域在下一次既有 20 tick 刷新生效。经济和每日结算仍由城镇权威模型负责，本层没有另设 WARM/COLD 追赶模拟。

---

## 4. 行为系统：整数状态机

不要 BehaviorTree，万级规模用**扁平状态机 + 任务表**：

```java
enum CitizenState { SLEEP, WORK, COMMUTE, EAT, IDLE, SOCIAL }

void tick(ServerLevel level, int i) {
    switch (sim.state[i]) {
        case SLEEP  -> { if (isDay(level))  startCommute(i, workplaceOf(i)); }
        case COMMUTE-> { if (arrived(i))    sim.state[i] = WORK; }
        case WORK   -> { if (isHungry(i))   startCommute(i, kitchenOf(i));
                         else if (isNight(level)) startCommute(i, homeOf(i)); }
        case EAT    -> { consumeFood(i); sim.state[i] = WORK; }
        ...
    }
}
```

- 状态转移时设置 `targetX/targetZ` 并申请寻路（§5），状态字节本身就是要同步给客户端驱动动画的字段。
- 复杂需求（职业、生产链）作为“任务提供者”注册进 `BehaviorSystem`，仍走同一状态机，只是转移条件更丰富——保持热路径简单。
- 随机性用 `id` 播种的 xorshift，保证**确定性**（重放/断线重连后一致，且便于调试）。

---

## 5. 寻路与移动：路网图 + 流场 + 分离

上万单位**绝不能**各自跑原版 A*。分层方案：

### 5.1 第一层：城镇路网图（全局）

城镇道路/建筑门在建造时登记为 `TownRoadGraph` 的节点与边（图很小：一个镇几百节点）。跨镇/跨区寻路 = 图上 Dijkstra，结果是一串路径点。**每个单位只在自己状态转移时请求一次**（1Hz × 500/tick 中仅极少数），成本可忽略。

### 5.2 第二层：流场（局部，本方案核心）

对于“去往同一目的地”的大量单位（下班潮、去食堂），用 **Flow Field**：

1. 以目标方块为源，在局部区域（如 64×64）做**反向 BFS**，得到每格到目标的距离场 `d[x][z]`。
2. 单位移动 = 读当前格与邻居格的距离场，沿梯度下降方向走。**一次 BFS 服务所有同目标单位**。
3. 流场按 `(目标区块, 目的地类别)` 缓存复用，地形变化（方块破坏/放置事件）时局部失效重算。
4. BFS 放**异步线程池**（`NavJobExecutor`，线程数 = 核数-1），结果用并发队列交还主线程，下一 tick 生效——主线程零阻塞。

```java
/**
 * 流场：对某目的地反向 BFS 出的距离场，所有同目标单位共享。
 * <p>
 * Flow field shared by all citizens heading to the same destination.
 */
public final class FlowField {
    public static final int SIZE = 64;
    private final short[] dist = new short[SIZE * SIZE]; // Short.MAX_VALUE = 不可达
    private final int originX, originZ;

    /** 返回该位置应走的方向（量化到 16 向），-1 表示不可达。 */
    public int sampleDir(int x, int z) {
        int best = dirIndex(x, z), bestD = dist[best];
        // 8 邻居取距离场最小者；预计算方向表，无三角函数
        ...
    }
}
```

### 5.3 第三层：分离力（碰撞）

单位间不互相推挤物理碰撞，只做**视觉分离**：用 `SpatialGrid`（cell = 2 格）查半径 1.5 格内邻居，叠加一个微弱的排斥向量到速度上。每 tick 每 ACTIVE 单位一次网格查询，邻居网格使用 `Long2ObjectOpenHashMap<IntArrayList>` 并每 5 tick 从活跃且空间可见的居民重建，桶列表循环复用。

### 5.4 移动积分（每 tick 仅活跃居民）

```java
void tickAll() {
    for (int id : activeIds) {
        int i = sim.indexOf(id);                    // swap-remove 后重新解析
        if (i < 0 || !isActive(i)) continue;
        int dir = currentField(i).sampleDir(px[i] >> 10, pz[i] >> 10);
        vx[i] = DIR_X[dir]; vz[i] = DIR_Z[dir];     // 查表，无浮点
        px[i] += vx[i]; pz[i] += vz[i];             // 定点加法
        yaw[i] = DIR_YAW[dir];                      // 朝向直接由方向表给
        separation(i);                              // 仅 ACTIVE
    }
}
```

该路径复杂度由总人口的 `O(total)` 降为 `O(active)`；实际毫秒数必须用目标服务器的 `spark`/JFR 基线验证，本文不声明未经测量的固定耗时。

---

## 6. 朝向处理

- 服务端 yaw 只存 `byte`（256 级），由移动方向查表得到，**不算 atan2**。
- 静止面对目标时（如对话），转移状态时显式写一个目标 yaw。
- 客户端做角度插值（`Mth.rotLerp`）平滑到 60fps，256 级量化在插值后肉眼不可辨。
- pitch 基本用不上，城镇地面行走；需要时同样用 byte。

---

## 7. 网络同步

这是混合模拟的成败关键。原则：**发送“状态与速度”，不发送“每帧位置”**；客户端外推 + 周期校正。

### 7.1 AOI 兴趣管理

`SyncEngine` 以每个在线玩家为中心、固定 96 格平面半径维护展示集合；每 20 tick 或服务端配置热变更时统一重选：

- `SpatialGrid.rebuildVisibility` 每轮选择前把全部合法状态居民登记到 16 格粗桶，包含睡眠居民；`queryVisible` 只生成附近桶候选并多取一圈 halo。`collectPlayerCandidates` 随后按当前状态、有效床标记和当前坐标执行精确 96 格圆形判定，粗桶不会改变最终展示语义。
- `FHConfig.SERVER.TOWN.maxVisibleCitizensPerPlayer`：默认 `1024`，范围 `0..4096`，严格限制单个玩家的 `tracked` 集合和 `ClientCitizenCache`。
- `FHConfig.SERVER.TOWN.maxVisibleCitizensPerServer`：默认 `8192`，范围 `0..65536`，严格限制服务器全部玩家、全部维度的追踪关系总数。同一居民被两名玩家看到会占两个名额，因为客户端缓存和渲染成本实际发生两次。
- 清醒居民与“已验证并定位到有效床头”的 `SLEEP` 居民共享预算；无床、床失效或区块不可验证的睡眠居民不进入候选集。
- 每玩家先用定长最大堆选最近的 Top-K；当前打开 `TradeContainer` 的居民优先但仍占名额，已追踪居民具有 4 格保留优势，等距时按稳定 citizen id 决定，从而减少边界 spawn/despawn 抖动。
- 服务器 tick 末尾再对各维度候选统一裁决总量预算，先发送 despawn 再发送 spawn，客户端应用包时也不会瞬时超过上限。
- 任一上限为 `0` 时对应范围内不展示居民；真实人口、行为、床位、存档和每日结算不受展示预算影响。

### 7.2 三类包

| 包 | 时机 | 内容 |
|----|------|------|
| `S2CCitizenSpawnPacket` | 进入预算集合；已追踪居民年龄组变化 | id、绝对定点位置、打包的 state+16向 dir+halt、年龄组、姓名 |
| `S2CCitizenBatchPacket` | 每 4 tick（5Hz） | chunk 分组的局部量化位置、id、打包 state+dir+halt；单包最多 240 条，同一玩家一次 flush 可发多个包 |
| `S2CCitizenDespawnPacket` | 离开预算、床失效或居民移除 | varint id 列表 |

- **误差驱动脏标记**：服务端镜像客户端 16 向外推模型；状态、方向、halt 变化或位置误差超过 `0.2` 格才到期发送，移动心跳按 4/8/20 tick 距离档位重锚。
- **多包分片**：`SyncEngine` 先为每个玩家收集全部仍被追踪且到期的 chunk groups，再由 `CitizenDeltaPacketBatcher.forEachPacket` 按 `MAX_ENTRIES_PER_PACKET=240` 逐包产出并立即发送；跨越 240 的单个 chunk group 使用原 entries 的 `subList` 视图切分，未跨界的 group 直接复用，不复制条目引用。生产路径不保留完整 packet 外层集合，也不能丢弃尾部居民。
- **规范状态提交**：共享 Dead Reckoning canonical 只在包含该 ID 的 packet 调用 `FHNetwork.INSTANCE.sendPlayer` 并正常返回后推进；这表示本地网络 API 交付，不是客户端 ACK。`due` 中没有交给任何玩家的 ID 不会被伪记为已发送。
- **增量候选**：`flushDeltas` 先合并所有玩家的 `tracked` 集合，再仅检查唯一 tracked ID；距离档位仍对本维度全部在线玩家求最近距离。到期 ID 在该次扫描中解析为复用的 `ResolvedDelta`（持有 `CitizenSim`、index、chunk key 和一份不可变 `S2CCitizenBatchPacket.Entry`），各玩家只按自身 tracked set 过滤并共享该 Entry，组包和 canonical 回写不再重复 `findById`/`indexOf`。
- **组包 scratch 生命周期**：per-player chunk map、chunk entry lists、group list 与已选 resolved list 按高水位复用。Forge 47.3.0 `SimpleChannel.send` 在 `FHNetwork.INSTANCE.sendPlayer` 返回前完成编码，因此只在 `CitizenDeltaPacketBatcher.forEachPacket` 返回后清空这些列表；packet 外层 list 和 `FriendlyByteBuf` 仍不池化。
- **离散切换**：入睡、醒来和有效床位置刷新进入 `pendingImmediate`，在下一次 4 tick flush 绕过距离限频；无效床直接进入 `pendingHidden`。
- **年龄外观**：`TownSimData` 把权威 `Resident.age` 镜像到 `CitizenSim.presentationFlags` 的 2 个瞬态位；初次进入 AOI 时随 `S2CCitizenSpawnPacket` 下发。幼儿成长为儿童、儿童成长为成人时，`SyncEngine.pendingAppearance` 对已追踪 id 重发同一种 spawn 条目；`ClientCitizenCache.applySpawn` 只原位更新年龄，不替换双快照、位置、朝向或行为状态。移动批包不增加年龄字段。
- **静止零带宽**：睡眠与到岗停止居民完成状态切换后不再发送移动心跳。
- 走现有 `chorda` 的 `CBaseNetwork`/SimpleChannel 封装，包体手写 `FriendlyByteBuf` 序列化，不走 NBT。

### 7.3 客户端缓存与插值

```java
/**
 * 客户端居民渲染状态，双快照插值。
 * <p>
 * Client-side citizen render state with double-buffered interpolation.
 */
public final class ClientCitizen {
    public int id, type, skinSeed;
    public double x0, y0, z0;        // 上一快照
    public double x1, y1, z1;        // 最新快照
    public float yaw0, yaw1;
    public long t0, t1;              // 快照到达时间（nanoTime）
    public byte state;
    public double vx, vz;            // 外推速度（快照间隔内）

    public double renderX(float partial) { /* lerp(x0,x1) + 外推，clamp */ }
}
```

普通移动快照使用按实测包间隔调整的插值窗口并在窗口后限时外推。进入或离开 `SLEEP` 时直接 snap 到床头或住宅出口，并重置插值窗口，避免穿墙滑动。`ClientCitizen.renderPos(double)` 与 `visualYaw(double)` 接受入口已解析的游戏时间；CPU render、Flywheel tick、详细假实体 tick 和单个移动批包各自只采样一次时钟，整批居民使用同一时刻。

---

## 8. 客户端渲染：假实体、CPU 批量与 Flywheel Instancing

按**可见数量**选渲染路径，两者共存、按距离切换：

| 路径 | 适用 | 做法 |
|------|------|------|
| **假实体** `FakeCitizenEntity` | 清醒且入选详细 Top-K；16 格进入、20 格退出 | 无 AI 的客户端实体，由 `ClientCitizen` 驱动位置、朝向和步行动画；模型、阴影、碰撞箱与选取体积按年龄缩放；`FHConfig.CLIENT.maxDetailedCitizenEntities` 默认严格限制为 64，交易/准星目标优先 |
| **批量低模** | 未由假实体接管的清醒居民：Body `<68` 格进入、`<=72` 格保持；睡眠同样适用 | 原版 `RenderType.entityCutoutNoCull` 绘制头、躯干、双臂、双腿；CPU/Flywheel 均按年龄缩放站立与睡眠模型，不创建假实体 |
| **轮廓 LOD** | Billboard `>=68` 格至 `96` 格，具体 owner 由协调器迟滞保持 | 清醒使用身体+头部竖直 billboard，睡眠使用贴近床面的水平身体+头部 quad；两种后端均按年龄缩放 |

要点：

- `CitizenSkins` 按稳定 citizen id 确定性选择 Minecraft 1.20.1 内置的宽臂 `Makena`、`Efe`、`Noor`、`Kai`、`Ari`、`Zuri`、`Sunny` 皮肤；近景假实体和批量 LOD 共用该映射，跨 LOD、离线重进和重新生成均不换肤。资源直接引用 `textures/entity/player/wide/*.png`，模组不复制原版贴图。
- 渲染入口是 `RenderLevelStageEvent.AFTER_ENTITIES`；每帧只遍历并剔除一次缓存，按七张皮肤写入七个复用 `BufferBuilder`，仅对本帧实际可见的皮肤提交，最多 7 次 draw call，不创建每帧居民分组集合。
- 批量顶点使用 `RenderType.entityCutoutNoCull` 的 `DefaultVertexFormat.NEW_ENTITY`，同时提交皮肤 UV、`OverlayTexture.NO_OVERLAY`、居民位置的天空光/方块光和面法线。该 RenderType 的实体 shader 会实际采样 lightmap 并执行原版方向光计算；`POSITION_COLOR_TEX_LIGHTMAP` 的同名 `UV2` 在 Minecraft 1.20.1 对应片元 shader 中没有被采样，不能用于环境明暗。光照值缓存在 `ClientCitizen`：跨方块时立即重采样，静止时按 citizen id 错峰每 5–8 tick 刷新；采样复用单个 `MutableBlockPos`，避免逐帧对象分配。
- `CitizenBatchRenderLayout` 是 CPU/Flywheel 的共享表现合同：定义六个 Body 部件、两个 Billboard quad、皮肤 UV、睡眠比例/锚点、四肢摆动符号，并预计算 256 向站立/睡眠模型轴。站立轴复现 `LivingEntityRenderer` 的 `scale(-1, -1, 1)` 约定，使皮肤局部 `-Z` 始终朝居民前方、局部 `-Y` 朝世界上方；睡眠时局部 `-Z` 朝上、局部 `-Y` 朝床头。每个面的世界法线再经过当前 `PoseStack` normal matrix 一次后复用于四个顶点，不产生逐顶点临时向量。
- `ClientCitizen.modelScale()` 将 `AGE_INFANT`、`AGE_CHILD`、`AGE_ADULT/AGE_ELDER` 分别映射为 `0.4`、`0.5`、`1.0`。详细假实体、CPU Body/Billboard、Flywheel Body/Billboard、视锥 AABB、光照采样高度和准星选取共享该语义；年龄只影响表现，不改变 Citizen 行为、速度、LOD 距离或服务端碰撞。
- 睡眠使用低矮 AABB 做视锥剔除，关闭行走起伏，并使用同步的床朝向而非客户端软转向。
- `DetailedCitizenSelector` 通过复用的原始类型数组和最大堆执行稳定 Top-K；已接管居民按 4 格距离优势保留，等距按稳定 id。配置降低或设为 0 时，下一客户端 tick 立即释放超额代理，未入选居民仍由批量路径绘制。
- `ClientCitizen` 缓存覆盖双快照与最大外推终点的扫掠 AABB；只在 spawn/网络快照时重建，渲染帧不再逐居民分配剔除对象。
- `CitizenClientBenchmark` 通过 `/citizen_debug benchmark load <32|64|256|1024> <moving|sleeping>` 注入纯客户端确定性场景；高位 id 和按对象身份清理保证真实同步缓存不被覆盖。`CitizenRenderMetrics` 记录 backend 分类数量、光照采样、材质批次、实例脏字节与最近 256 帧 hook 耗时；Flywheel 引擎本身的 CPU/GPU 时间不在 hook 内，详细口径见 [citizen-rendering-at-scale.md](citizen-rendering-at-scale.md)。
- `CitizenRenderCoordinator` 统一接管网络 spawn/update/despawn、benchmark 注入、客户端 tick/render、资源重载、Flywheel renderer 重载、切维度和退出清理。网络 Batch 由 cache 直接返回已更新对象，并保持“整批 cache 更新完成后再通知 backend”的两阶段顺序。`CitizenRenderOwnership` 对每个 id 返回唯一的 `DETAILED_ENTITY`、`BODY_BATCH`、`BILLBOARD_BATCH` 或 `NONE`；共享迟滞 map 只在 owner 实际切换时写入。启动默认 requested 为 `auto`。coordinator 分别记录 requested 与 active backend：新 backend 先初始化并从 cache 预热后再原子替换，非 CPU backend 故障、驱动不支持 instancing 或 Oculus shader pack 使 Flywheel 关闭实例化时，只有 active 临时回退 CPU；requested AUTO/Flywheel 保留，并在 `ReloadRenderersEvent` 中于 Flywheel 替换 `InstanceWorld` 后自动恢复。维度变化在健康检查前通过 `onClientLevelChanged` 重绑定，旧 manager 的失效句柄不会再调用 `delete()`。
- `FlywheelCitizenBackend` 是 AUTO 首选、也可显式请求的动态实例 backend：七张皮肤分别共享 144 顶点 Body 与 8 顶点身体/头部 Billboard，每个批量居民一个 58 B `CitizenInstanceData`。CPU/Flywheel 都消费 `CitizenBatchRenderLayout`；累计步态 phase 属于 `ClientCitizen`，切换 backend、Body/Billboard 或重建 Flywheel 槽不会重置。`citizen.vert` 使用 Flywheel `uTime`，而 `FlywheelCitizenBackend` 写入时也必须取 `com.jozufozu.flywheel.util.AnimationTickHolder`，完成快照插值/限时外推和短路径朝向；四肢幅度按步速对齐原版 `walkAnimation`，并支持睡眠姿态。Body 在 `<68` 格进入并保持到 `72` 格，Billboard 最远 `96` 格；当前 owner 由 `CitizenRenderCoordinator` 共享给 CPU/Flywheel。它实现 `InstancingEngine.OriginShiftListener`：Flywheel 切换渲染原点并清空底层槽后，同帧从 cache 重新创建实例。它只接受 Flywheel `INSTANCING`，BATCHING/OFF、Oculus shader pack 或运行期故障时 active 回退 CPU；完整限制、命令和待完成的图形验收见 [citizen-rendering-at-scale.md](citizen-rendering-at-scale.md)。

---

## 9. 交互

玩家右键一个“居民”时，无论它是假实体还是实例化画的：

1. **选取**：客户端 raycast 先撞假实体；没撞到时，`ClientCitizenCache.pick` 线性扫描当前缓存并做射线-圆柱近似检测，拿到 `citizen id`。这是低频点击路径；若未来 profile 证明需要，再复用渲染 cell 做候选裁剪。
2. **请求**：发 `C2SCitizenActionPacket(id, action)`。服务端校验：id 存活、属于该玩家当前 `tracked` 集合、状态允许交互且距离不超过 8 格。`SLEEP` 和预算隐藏居民都不可交互。
3. **响应**：服务端按 id 取数据（对话/交易/雇佣），走现有 `CBaseMenu` 体系开 GUI——**菜单数据源是 CitizenSim，不是实体**，全部读写都在服务端权威数据上。
4. 攻击/伤害同理：C2S 请求 → 服务端改数值 → 死亡即发 Despawn + 事件包（尸体表现纯客户端）。

这套“一切交互都是 RPC”的模式，也正是反作弊正确的姿势。

---

## 10. 持久化

- 镇容器 `TownSimData` 与队伍零关联，统一存**全局单文件**（`AITownManager`，overworld SavedData "fh_ai_towns"：AI 镇列表 + 玩家镇模拟表 key=队伍 holder id）；所有落盘字段变化（位置/朝向/行为状态/目标/锚点/出生/移除）经 dirty 回调 `setDirty()`，只设置内存标志，不产生逐 tick 磁盘 I/O；Minecraft 内建 6000t 自动保存/停服负责真正写盘。除 `CitizenSim` 外还保存最近所属维度，确保跨重启换维度时重建 per-level 会话 id；tradeData/nameCache 等运行期状态不落盘。
- 未托管容器 `UnmanagedCitizenData extends SavedData`（per-level，沿用旧文件名 `fh_citizen_sim`），采用同样的变更标脏语义；同时承载本维度稳定 id 分配器（nextId 持久化、永不复用）。首次接管会以本维度全部容器的最大现存 id 校准分配器；若坏档或不完整写盘已造成跨容器 id 冲突，只重分配冲突的会话 id，位置/状态/居民 UUID 保持不变。
- 序列化按 SoA 数组直写 NBT `int[]/byte[]` 数组字段，遵循项目 Codec-first 约定可用 `Codec.INT_STREAM`/`BYTE_BUFFER`；1 万居民落盘 < 1MB，毫秒级。
- 冷数据档案另存一个 compound。版本号字段预留迁移空间（参考仓库根 `NBT_MIGRATION_GUIDE.md`）。
- `presentationFlags`、`homePos`、`homeSlot` 等展示数据是运行期瞬态 SoA 字段，不写入 NBT。`presentationFlags` bit 0 是每次住宅布局/床方块验证后重建的 `PRESENT_ON_VALID_BED`，bit 1..2 镜像 `Resident.age`；编码 `0/1/2/3` 分别表示成人/幼儿/儿童/老人，使旧存档和未托管居民的全零默认仍表现为成人。

---

## 11. 性能预算与优化清单

**1 万居民、活跃 3000 的服务端单 tick 预算（目标 < 2 ms）：**

| 项 | 频率 | 估算 |
|----|------|------|
| 行为决策 | 500/tick（分帧） | ~0.4 ms |
| 移动积分 | 3000 ACTIVE 全量 | ~0.1 ms |
| 分离力 | 3000 × 网格查询 | ~0.5 ms |
| 同步脏检查+打包 | 每 4 tick | ~0.2 ms（摊销） |
| 寻路 | 异步线程 | 主线程 0 |

**客户端现状：** 清醒居民的原版质量代理默认严格限制为 64；启动 requested 为 AUTO，可用时由 Flywheel instancing 绘制其余低模/billboard，不支持 instancing 时由 CPU 每帧生成顶点。Flywheel backend 已完成动态实例代码、自动化，以及 1024 moving/sleeping 的所有权、稳态零脏写、F3+T 重建计数和基础画面实机验证。初次画面验收发现实例 Body 的固定摆动呈跑步，改为按实际位移对齐详细实体的原版步态后，实机复验已确认两层行走一致；68/72 格迟滞切换也没有重影、空帧或抖动。随后发现 Flywheel 原点切换会清空自定义实例槽，而旧逻辑仅改写已脱离槽表的数据，造成计数仍在但 Body 全部消失；现已改为监听清槽事件并整体重建，自动化和跨 100 格实机复验均已通过，Body 不再整体消失。原 Flywheel Billboard 缺少头部导致轮廓跳变，扩展为身体/头部双 quad 后，715 个远景 Billboard 的画面复验确认正常；CPU 回退现与 Flywheel 共用同一双 quad 布局。Flywheel 0.6.11 在 Oculus shader pack 开启时主动关闭实例化，Citizen 会保留 requested AUTO/Flywheel、临时以 CPU 绘制，并在关闭光影后的 renderer reload 自动恢复 Flywheel。GPU 预算与完整 Embeddium/Oculus 支持矩阵仍需作为发布验收项。

**优化清单（按收益排序）：**

1. SoA + 定点数，杜绝装箱与 `HashMap<Integer,...>`（用 fastutil 原始类型 map）。
2. 分帧 + 活跃度 LOD，COLD 单位彻底休眠。
3. 流场共享寻路，禁绝逐单位 A*。
4. 方向/三角函数全部查表。
5. 网络只发 dirty + 速度外推，快照 5Hz。
6. 仅复用不逃逸当前同步编码调用的 scratch；当前 `flushDeltas` 已复用 resolved、chunk map/list 和 group scratch，packet 外层 list/`FriendlyByteBuf` 不池化。若网络实现不再在 `sendPlayer` 返回前完成编码，必须撤销 chunk list 回收。
7. 异步寻路线程池，主线程只消费结果。
8. 渲染实例化 + 距离 LOD + 预烘焙动画。
9. 每 tick 时间片哨兵：单次 tick 超预算自动降低本 tick 处理量，绝不卡服。
10. JFR/`spark`  profiler 定期回归，重点看 `serverTick` 与发包线程。

---

## 12. 实施路线图

| 阶段 | 内容 | 验收 |
|------|------|------|
| P1 | CitizenSim SoA + Manager(SavedData) + 分帧 tick + 状态机 | 服务端 1 万单位 tick < 1ms，无渲染 |
| P2 | 同步三件套 + 客户端缓存插值 + Billboard 占位渲染 | 联机看到人群移动，带宽达标 |
| P3（进行中） | 已完成有硬上限的假实体 Top-K、稳态 AABB、确定性基准、backend/coordinator 边界、静态实例验证、Flywheel 动态实例/GPU 动画代码和基础实例生命周期计数实机验证；人工图形兼容与性能验收尚未完成，详见 [citizen-rendering-at-scale.md](citizen-rendering-at-scale.md) | 以 1000 人固定场景的 render-thread/GPU p95 预算验收 |
| P4 | 路网图 + 流场 + 异步寻路 + 分离 | 下班潮千人同路不卡 |
| P5 | 交互 RPC + 菜单接入 + 需求/经济低频系统 | 可对话、可交易、可雇佣 |

每阶段独立可用，P1+P2 即可支撑"远处有活人在动"的观感，风险后置。

---

## 13. 特殊 NPC：双轨制（重要架构决策）

混合模拟不是"禁绝实体"，而是**按职责分轨**：

| 轨道 | 对象 | 规模 | 实现 |
|------|------|------|------|
| 数据模拟轨 | 普通城镇居民（通勤、干活、氛围） | 数千~上万 | 本文档 §2–§10，零实体 |
| 实体轨 | 特殊 NPC（任务发布者、店主、剧情角色、可雇佣名人） | 几十级 | 真正的 `Entity`，常驻、深度交互 |

**实体轨继承路径（1.20.1）：**
- 地面行走 NPC → 继承 `PathfinderMob`（白得 Goal 选择器、属性、地面导航）
- 需要原版交易界面 → 继承 `AbstractVillager`
- 纯站桩对话 NPC → 继承 `Mob` + `setNoAi(true)` + `setPersistenceRequired()`

**为什么特殊 NPC 必须用实体**：区块级持久化、原版右键交互链路、伤害/战利品/队伍体系、刷怪蛋与 summon 命令、数据包兼容——全部免费。数量只有几十，实体开销可忽略；把强交互 NPC 硬塞进数据模拟反而要重造整条原版交互链，得不偿失。

**两轨协作约定：**
1. 共享同一份 `CitizenProfile` 与行为/职业定义（Codec 数据），特殊 NPC 只是"有实体外壳的居民"。
2. 特殊 NPC 注册进 `SpatialGrid`，人群分离力会避开它们；反之人群对特殊 NPC 的互动（排队、围观、打招呼）在数据层结算。
3. 玩家 ↔ 特殊 NPC 走原版实体交互；玩家 ↔ 人群居民走 §9 的 RPC。两条链路在菜单层（`CBaseMenu`）汇合，GUI 不感知差异。

**性能结论的边界说明**：混合模拟是"大规模 + 独立行为"前提下的服务端吞吐上限方案，瓶颈会转移到客户端渲染与带宽；对无独立行为需求的氛围人群，纯统计模拟（只存数量、近玩家才生成表现）更便宜；规模仅数百时轻量实体方案综合成本最低。三者可在同一存档内按区域/职能混用。

---

## 14. 进阶优化：带宽与渲染深挖

§7/§8 是"够用"的基线；本节是可继续深入的进阶手段，按收益排序。建议基线落地并 profile 后按需引入，不要提前全做。

### 14.1 网络带宽（16B/人 → 约 5B/人）

1. **Dead Reckoning（误差驱动同步，收益最大）**：服务端镜像运行与客户端完全一致的外推模型，仅当真实位置与外推位置误差超过阈值（约 0.2 格）才发包。匀速行走的居民全程零带宽，只有转向/停止/状态切换触发同步，实测可省 70–80% 的移动流量。
2. **区块相对坐标**：Batch 按 chunk 分组，包头发一次基准坐标，包内每人只发 chunk 内偏移（x/z 各 1B，1/16 格精度），位置字段 12B → 3B。
3. **Varint 差分**：相对上一快照 zigzag varint 差分，慢速移动每字段 1–2B。
4. **位打包**：state(3bit) + 16 向 dir(4bit) + halt 标志位合 1B；静止者只发 id + 事件。
5. **按距离分频**：近 10Hz / 中 5Hz / 远 2Hz / AOI 边缘 1Hz，与 Dead Reckoning 叠加。
6. **强制合包**：一个 Batch 塞数百人，摊掉包头发与帧头固定开销；禁止一人一包。
7. **视锥近似裁剪**：按玩家朝向粗算视锥，背后居民降频至 1–2Hz（不可完全停发，防止快速转身穿帮）。

目标量级：可见 500 人时约 **2.5 KB/s/玩家**，低于原版 50 个村民的同步流量。

### 14.2 客户端渲染（CPU 稳态每帧趋零）

1. **快照 + 脏更新**：已实现的 58 B 实例数据只在快照、LOD 或光照值变化时更新对应实例；Flywheel 渲染原点变化会先清空底层槽，再从 cache 整体重建一次。普通 render frame 不重算居民矩阵或生成盒体顶点。
2. **GPU 刚性部件动画**：六个盒体用静态 `partId` 驱动 vertex shader，不需要 bone texture；CPU 仅在快照到达时累计步态 phase，GPU 按实际快照位移/外推速度驱动反相四肢，频率与幅度对齐原版假实体且不增加整体 bob。
3. **有限材质批次**：当前保留七张原版皮肤，Body/Billboard 各最多七个批次。只有 RenderDoc 证明这里是瓶颈后才增加 atlas/array texture，不以单 draw call 为先决条件。
4. **当前 LOD**：详细假实体候选在 16 格进入、20 格退出且最多 64 个；未入选者使用共享的 68/72 格 Body 迟滞，`>72..<=96` 格稳定为 Billboard，回到 `<68` 格才恢复 Body。CPU/Flywheel 共用协调器中的 owner 状态，第三档低模只在 GPU profile 证明有必要时增加。
5. **分级视锥剔除**：按空间网格 cell 剔除，一次测试剔除 16–64 实例。
6. **光照摊销**：按 cell 采样亮度而非按人，每 tick 轮询刷新部分 cell。
7. **阴影降级**：中远距实例关闭阴影投射或用贴地 blob shadow。
8. **动画降频**：远距实例相位推进降至 10Hz。

基准认知：做好后客户端 1000 实例的渲染成本**低于原版 50 个村民**（原版每实体都要走完整 tick、AABB 与部件矩阵栈，实例化路径全部绕过）。

### 14.3 何时不做

- 可见人数 < 100：基线方案已足够，Dead Reckoning 与 GPU 骨骼的收益被复杂度抵消。
- 单人存档：带宽优化全部可跳过（本地通道几乎免费），只做渲染侧。
- 一切以 profile 为准：`spark` 看服务端 tick 与发包，Flywheel/RenderDoc 看客户端 draw call 与 GPU 耗时。
