# 城镇居民混合模拟架构（Hybrid Citizen Simulation）

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
│   ├── BehaviorSystem    轻量状态机（工作/吃饭/睡觉/闲逛），纯整数运算          │
│   ├── NavSystem         分层寻路：城镇路网图 + 局部流场(Flow Field)            │
│   │                      └── 异步线程池执行 BFS/路径请求，主线程消费结果        │
│   ├── SpatialGrid       空间哈希网格（跨容器共享，条目存稳定 id）：邻居/分离   │
│   └── SyncEngine        AOI 兴趣管理 + 脏标记增量快照 + 带宽预算分包           │
│                                                                               │
└───────────────────────────────┬───────────────────────────────────────────────┘
                                 │ SimpleChannel（chorda CBaseNetwork）
                                 │ 全量(进入AOI) / 增量(移动批) / 事件(状态切换)
┌───────────────────────────────┴───────────────────────────────────────────────┐
│                          客户端 (Logical Client)                              │
│  ClientCitizenCache    id → 渲染状态（带双缓冲插值快照）                        │
│  ClientCitizenRenderer 近处少量假实体 / 其余 Flywheel 实例化批渲染              │
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
├── sync/           同步：SyncEngine, CitizenAOI, 各 S2C/C2S 包
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
> 直调 sim 事件回调 + 结构变更 dirty 标记。落盘遵循标准 SavedData 语义——结构
> 变更处 `setDirty()`，Minecraft 内建 6000t 自动保存/停服写盘（无自定义存盘
> 调度器），位置数据为瞬态不主动标记。不再有 1Hz 全局对账同步器；
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

**内存估算**：1 万居民 × 约 60 字节热数据 ≈ **600 KB**，加上稀疏档案约 2–3 MB。服务端“近零开销”由此而来。

---

## 3. 模拟调度：分帧 + 活跃度 LOD

万级单位绝不能每 tick 全量更新。用两级手段：

### 3.1 分帧（Time Slicing）

每个居民出生时分配 `tickPhase = id % SLICE`，`SLICE = 20` 意味着**每单位 1Hz 逻辑频率**，每 tick 只扫描 `1/20` 的居民做行为决策：

```java
public void serverTick(ServerLevel level) {
    long gameTime = level.getGameTime();
    int slice = (int) (gameTime % SLICE);          // 本 tick 负责的分片
    for (int i = 0; i < sim.size(); i++) {
        if (sim.tickPhase[i] != slice) continue;   // 不是本单位的回合，跳过
        behavior.tick(level, i);                   // 1Hz 行为决策
    }
    nav.consumeResults(level);                     // 消费异步寻路结果
    movement.tickAll(level, sim);                  // 移动积分是全量但极廉价（见 §5）
    sync.flush(gameTime);                          // 发送增量快照
}
```

- 1 万居民 → 每 tick 只跑 **500 个**行为决策，单次决策是纯整数运算（< 1μs），合计 **< 0.5 ms**。
- 行为 1Hz 完全够用：人走路速度下，1 秒才移动约 4 格，决策延迟不可感知。

### 3.2 活跃度 LOD（关键）

| 层级 | 条件 | 行为频率 | 移动 |
|------|------|----------|------|
| ACTIVE | 所在区块被加载且有玩家在 128 格内 | 1Hz | 连续积分，参与分离 |
| WARM | 区块已加载但无玩家附近 | 0.2Hz（每 5 秒） | 大步长瞬移插值 |
| COLD | 区块未加载 | 0.02Hz 或按需 | 只推进需求/经济数值，不动位置 |

活跃度用 `homeChunk` + Forge 的 `ChunkWatchEvent`/`ServerChunkCache` 判断；COLD 单位重激活时做一次“追赶结算”（按流逝时间折算需求变化），避免卡顿尖峰。

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

单位间不互相推挤物理碰撞，只做**视觉分离**：用 `SpatialGrid`（cell = 2 格）查半径 1.5 格内邻居，叠加一个微弱的排斥向量到速度上。每 tick 每 ACTIVE 单位一次网格查询，网格本身用 `Long2ObjectOpenHashMap<IntArrayList>` + 每 tick 增量重建活跃区。

### 5.4 移动积分（每 tick 全量，但极廉价）

```java
void tickAll() {
    for (int i = 0; i < size; i++) {
        if (navRef[i] == 0) continue;               // 静止，零成本跳过
        int dir = currentField(i).sampleDir(px[i] >> 10, pz[i] >> 10);
        vx[i] = DIR_X[dir]; vz[i] = DIR_Z[dir];     // 查表，无浮点
        px[i] += vx[i]; pz[i] += vz[i];             // 定点加法
        yaw[i] = DIR_YAW[dir];                      // 朝向直接由方向表给
        separation(i);                              // 仅 ACTIVE
    }
}
```

1 万单位全量积分 ≈ 一次连续数组扫描，**< 0.3 ms**。

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

`CitizenAOI` 以每个在线玩家为中心、半径 R（默认 96 格，可配置）维护可见居民集合：

- 用 `SpatialGrid` 每 20 tick 刷新一次各玩家的可见集合，进出触发**全量包/移除包**。
- 服务端只模拟可见区 + 其外一圈缓冲，客户端永远不知道 AOI 外有任何居民存在。

### 7.2 三类包

| 包 | 时机 | 内容 |
|----|------|------|
| `S2CCitizenSpawn` | 进入 AOI | id, 类型, 位置, yaw, state, 皮肤种子（全量，LZ4 前的原始约 20B/人） |
| `S2CCitizenBatch` | 每 4 tick（5Hz） | 本批 dirty 单位的 `[varint id, int px, py, pz, short vx,vz, byte yaw, byte state]` ≈ 16B/人 |
| `S2CCitizenEvent` | 状态切换即时 | id + 新 state（驱动动画切换/音效，低延迟） |
| `S2CCitizenDespawn` | 离开 AOI | varint id 列表 |

- **脏标记**：位置变化 < 0.5 格或状态不变的单位不进 Batch。静止的单位零带宽。
- **带宽预算**：每玩家每 tick 最多发 N 字节（默认 8KB），超出按距离优先级截断，下 tick 补——近处优先。
- **5Hz 快照 + 客户端外推**：客户端按 `pos + vel * dt` 外推，收到快照用 `lerp(0.3)` 收敛。万级单位下带宽 ≈ `可见数百人 × 16B × 5Hz ≈ 几十 KB/s`，可接受；若仍紧张，把位置差分化（varint delta 相对上次快照）可再砍一半。
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

快照间隔 200ms，插值窗口固定滞后一个间隔（类似 Source 引擎 cl_interp），移动平滑且永不回退。

---

## 8. 客户端渲染：假实体 vs 实例化

按**可见数量**选渲染路径，两者共存、按距离切换：

| 路径 | 适用 | 做法 |
|------|------|------|
| **假实体** `FakeCitizenEntity` | 距玩家 < 24 格（通常 < 50 个） | 真正的 `Entity`（`canUpdate = false`、无 AI、无碰撞箱查询注册），用原版模型/动画管线，可互动高亮、名牌、阴影 |
| **实例化批渲染** | 其余全部 | 本包已带 **Flywheel 0.6.11**（Create 依赖）：把居民模型做成 `InstancedModel`，一次 draw call 画几千个实例，每实例数据 = 变换矩阵 + 动画相位 + state |
| **Billboard LOD** | > 64 格 | 一张图集 quad，近平面剔除 |

要点：

- **动画不做骨骼重算**：每个 state 预烘焙若干关键姿态矩阵，实例着色器按 `time * speed + phase(id)` 在两个姿态间插值（类似 Imposter/群演方案）。行走循环摆动在 shader 里做，CPU 零开销。
- 渲染入口：`RenderLevelStageEvent`（AFTER_ENTITIES 阶段）提交 Flywheel 实例；假实体走原版实体渲染自然混入。
- 距离切换带迟滞（22↔26 格）防抖。假实体的客户端位置由 `ClientCitizenCache` 驱动，`tick()` 空实现。
- 光照：按所在方块 `level.getBrightness` 每 0.5s 采样一次写入实例属性。

---

## 9. 交互

玩家右键一个“居民”时，无论它是假实体还是实例化画的：

1. **选取**：客户端 raycast 先撞假实体；没撞到时，用客户端空间网格（同步时顺带维护）做射线-圆柱近似检测，拿到 `citizen id`。
2. **请求**：发 `C2SCitizenInteract(id)`。服务端校验：id 存活、距离 < 8 格、所在维度一致。
3. **响应**：服务端按 id 取数据（对话/交易/雇佣），走现有 `CBaseMenu` 体系开 GUI——**菜单数据源是 CitizenSim，不是实体**，全部读写都在服务端权威数据上。
4. 攻击/伤害同理：C2S 请求 → 服务端改数值 → 死亡即发 Despawn + 事件包（尸体表现纯客户端）。

这套“一切交互都是 RPC”的模式，也正是反作弊正确的姿势。

---

## 10. 持久化

- 镇容器 `TownSimData` 与队伍零关联，统一存**全局单文件**（`AITownManager`，overworld SavedData "fh_ai_towns"：AI 镇列表 + 玩家镇模拟表 key=队伍 holder id）；结构变更（出生/移除条目）经 dirty 回调 `setDirty()`，Minecraft 内建 6000t 自动保存/停服写盘（无自定义存盘调度器）。仅落盘 `CitizenSim`，tradeData/nameCache 等运行期状态不落盘。
- 未托管容器 `UnmanagedCitizenData extends SavedData`（per-level，沿用旧文件名 `fh_citizen_sim`），结构变更处 `setDirty()`（无周期调度）；同时承载全局稳定 id 分配器（nextId 持久化、永不复用）。
- 序列化按 SoA 数组直写 NBT `int[]/byte[]` 数组字段，遵循项目 Codec-first 约定可用 `Codec.INT_STREAM`/`BYTE_BUFFER`；1 万居民落盘 < 1MB，毫秒级。
- 冷数据档案另存一个 compound。版本号字段预留迁移空间（参考仓库根 `NBT_MIGRATION_GUIDE.md`）。

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

**客户端（可见 500）：** Flywheel 1–3 个 draw call + 50 个假实体，GPU 端开销远低于 50 个原版村民实体。

**优化清单（按收益排序）：**

1. SoA + 定点数，杜绝装箱与 `HashMap<Integer,...>`（用 fastutil 原始类型 map）。
2. 分帧 + 活跃度 LOD，COLD 单位彻底休眠。
3. 流场共享寻路，禁绝逐单位 A*。
4. 方向/三角函数全部查表。
5. 网络只发 dirty + 速度外推，快照 5Hz。
6. 发包对象池（`FriendlyByteBuf` 复用、避免每包 new byte[]）。
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
| P3 | Flywheel 实例化 + 假实体近距切换 + 动画状态机 | 帧率与 50 原版村民持平或更好 |
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

1. **Dead Reckoning（误差驱动同步，收益最大）**：服务端镜像运行与客户端完全一致的外推模型，仅当真实位置与外推位置误差超过阈值（约 0.3 格）才发包。匀速行走的居民全程零带宽，只有转向/停止/状态切换触发同步，实测可省 70–80% 的移动流量。
2. **区块相对坐标**：Batch 按 chunk 分组，包头发一次基准坐标，包内每人只发 chunk 内偏移（x/z 各 1B，1/16 格精度），位置字段 12B → 3B。
3. **Varint 差分**：相对上一快照 zigzag varint 差分，慢速移动每字段 1–2B。
4. **位打包**：state(3bit) + yaw(8bit) + 标志位合 2B；静止者只发 id + 事件。
5. **按距离分频**：近 10Hz / 中 5Hz / 远 2Hz / AOI 边缘 1Hz，与 Dead Reckoning 叠加。
6. **强制合包**：一个 Batch 塞数百人，摊掉包头发与帧头固定开销；禁止一人一包。
7. **视锥近似裁剪**：按玩家朝向粗算视锥，背后居民降频至 1–2Hz（不可完全停发，防止快速转身穿帮）。

目标量级：可见 500 人时约 **2.5 KB/s/玩家**，低于原版 50 个村民的同步流量。

### 14.2 客户端渲染（CPU 稳态每帧趋零）

1. **矩阵缓存 + 脏更新**：实例数据（变换/光照/动画相位）存 SoA 渲染缓冲，仅快照到达或状态变化时更新对应槽位，每帧只做 buffer 上传，不重算矩阵。
2. **GPU 骨骼动画**：骨骼矩阵烘成纹理（bone texture），vertex shader 按 `time + phase(id)` 采样插值，CPU 完全不参与动画。
3. **单 Draw Call**：全类型烘入一张纹理图集/数组纹理，皮肤职业用 per-instance 属性切换，整个居民系统 1 次 draw call。
4. **四级 LOD**：<24 格假实体 → 24–48 完整实例模型 → 48–80 低模（约 100 面）→ >80 格 billboard impostor。
5. **分级视锥剔除**：按空间网格 cell 剔除，一次测试剔除 16–64 实例。
6. **光照摊销**：按 cell 采样亮度而非按人，每 tick 轮询刷新部分 cell。
7. **阴影降级**：中远距实例关闭阴影投射或用贴地 blob shadow。
8. **动画降频**：远距实例相位推进降至 10Hz。

基准认知：做好后客户端 1000 实例的渲染成本**低于原版 50 个村民**（原版每实体都要走完整 tick、AABB 与部件矩阵栈，实例化路径全部绕过）。

### 14.3 何时不做

- 可见人数 < 100：基线方案已足够，Dead Reckoning 与 GPU 骨骼的收益被复杂度抵消。
- 单人存档：带宽优化全部可跳过（本地通道几乎免费），只做渲染侧。
- 一切以 profile 为准：`spark` 看服务端 tick 与发包，Flywheel/RenderDoc 看客户端 draw call 与 GPU 耗时。
