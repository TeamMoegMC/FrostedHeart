# Frosted Heart — Sparse Thermal Runtime V1 工程实现规格

- Time: `2026-08-22 16:47:30 +08:00`
- Last revised: `2026-08-26 05:45:49 +08:00`
- Authors: `Codex; OpenAI; main engineering agent`; independent reviews by `minecraft_geometry_review` and `thermal_runtime_review`; Phase 0/Phase A/Phase B implementation with `phase0a_mutation_spike`, `phase0_writer_census`, `phase0_enabled_mod_census`, `phasea_core_contract`, and `phaseb_face_ownership` (`gpt-5.6-sol`, `ultra`); Phase C, combined Phase D solver/source integration, Phase E PR6, PR7 runtime, PR8 Minecraft input foundation, Phase G physical sources, dormant Phase H material boundaries, Phase I Brick-local phase reservoirs and recipe activation, Phase J radiation, Phase K player/crop/town consumers, Phase L external diagnostics, and the player air/radiation gameplay test connection by the primary engineering agent
- Status: `in-progress`
- Implementation gate: `Phase 0 and Phase A/B/C/D foundations, Phase E PR6 gate implementation, PR7 runtime correctness, Phase F Minecraft topology/resnapshot/dispatch, Phase G Campfire/Generator/Radiator/Fountain physical sources, dormant Phase H non-phase material boundaries, Phase I Brick-local phase reservoirs, Phase J radiation, Phase K player/crop/town consumers, Phase L external diagnostics, and the three-backend query compositor complete; player air/direct radiation, crop/town publication hits, recipe-compiled hot-side phase transitions, sky-proven FarField, and bounded one-Page underground continuation are connected for in-save testing with natural fallback, while production-like multiplayer evidence, non-phase material calibration, cold-side phase authority, and a real ordinary-machine consumer remain open`
- Scope: `Frosted Heart 气候、世界温度、玩家环境采样、解析控制场、局部热源、材料相变、地热、消费者迁移与多人服务器运行时`
- Related: [`docs/climate/README.md`](../docs/climate/README.md), [`world-climate-and-temperature.md`](../docs/climate/world-climate-and-temperature.md), [`player-temperature.md`](../docs/climate/player-temperature.md), [`heat-production-and-network.md`](../docs/climate/heat-production-and-network.md), [`data-lifecycle-and-integration.md`](../docs/climate/data-lifecycle-and-integration.md), `WorldTemperature`, `BlockTempData`, `StateTransitionData`, `PhysicalState`, `SurroundingTemperatureSimulator`, `TemperatureThreadingPool`, `MinecraftThermalInput.AnalyticField`, `MinecraftPhysicalSourceManager`, `GeneratorData`, `HeatNetwork`
- Architecture: `Tiered Sparse Section-Component Finite-Volume Thermal Runtime`
- Primary implementation target: `Codex / Engineering Agent; Minecraft 多人服务端整合包`

> 本文是交给工程 agent 开始 Phase 0 和 V1 原型的冻结候选规格，不是当前实现行为的依据，也不表示已经批准替换生产温度系统。

> `2026-08-26` 稳态成本收敛：gameplay runtime 只在 5-tick cadence 到期时求解；Page/区块/geometry/FarField 或 physical source 注册、启停、功率变化会封存 urgent frame 并立即求解。无 source power/pending energy 且热残差连续稳定后，维度不再 seal 或 republish，新的 input event 再唤醒；`topologyResolved=false` 继续令查询携带 degraded/fallback 语义，但不再把“等待未来外部证据”误当成持续工作。`MinecraftThermalTopologyApplier` 的稳定 Page 只保留 `appliedSignatureIds[4096]`，`desiredSignatureIds` 在 dirty 时 clone/接管并于 commit 后释放，每稳定 Page 约省 `16 KiB`。`QueryPublication` 从 `256` 槽起步，solve publication 前按 arena high-water mark 倍增并继续计入新旧 backing 双持峰值；`65,536` 只保留为 active-cell hard cap。普通 section 只注入真实 thermal owner；Phase 0 probe/reference/census 已退役并从源码删除。

> `2026-08-26` mutation 热路径收敛：普通 geometry mutation 立即令 Page publication stale，但首次 pending tick 固定 `+5 ticks` 的最晚 rebuild 截止时间，后续变更不延后截止时间；截止前发生的 source-only solve 继续使用已经释放的 geometry watermark，不会顺带消费 pending geometry。首次从 `8/16` coarse support 进入可变 topology 时仍完整拆成 `64` 个 world-aligned `4^3` fragment；此后只分配、迁移并替换 `dirtyBrickMask` 对应 fragment，未变 Brick 的 arena slot 与 `H` 保持不动。Air pair 按负轴拥有者缓存为每 Brick 的 `+X/+Y/+Z` fragment，只重编脏 Brick 与三个负方向接口拥有者；材料候选在同一次 64-block Brick signature 遍历中收集，材料接触只刷新脏 Brick 和六个面邻居。Page retirement 在 replacement sweep commit 后释放全部 fragment。该改动已有 slot stability、ledger、material/phase 和 gameplay batch 定向测试；真实 CPU/分配改善比例仍等待同 workload 的新 JFR，不以代码结构代替实测。

> `2026-08-26` 验证资产收缩：Phase 0 reference/probe/census、Phase A census、self-evidence JUnit、JMH/JFR/JOL fixture、专用 Gradle task 和旧报告生成物全部删除。只保留 `gameTest` source set 中 Phase A geometry 与真实 Minecraft thermal integration 两类 Forge GameTest，共 `13` 条 thermal 场景；发行 JAR 不包含这些测试。拓扑不变 frame、primitive section index 和固定六邻居传播优化保留，核心 `H/C/P*dt`、pair、migration、radiation 和 publication 语义未变。

> `2026-08-26` 实机测试接线：`TemperatureUpdate` 已通过 `MinecraftThermalInput.gameplayPlayerEnvironment` 消费新 Page publication 和同次有界 Phase J 辐射结果；首次 query 建立 runtime、启用 physical source/radiation 并 capture 玩家已加载 section，自然环境值只作为初温和明确 air miss 的兜底。作物与住宅/狩猎建筑也在完整 publication 命中时使用新空气温度，miss/partial 时回退同次 natural backend。`StateTransitionData` 的可编译热侧规则现进入同一 gameplay registry，只有 applied Page candidate 才屏蔽旧热侧随机转换，冷侧与 fallback 保留；普通机器没有现存温度 consumer。Campfire、Generator、Radiator 与 Fountain 只通过 physical source manager 进入 mesh/radiation，旧区块热场、capability 和同步失效路径已经删除。当前 dispatch 在 level tick 末主线程顺序执行，以先验证真实路径正确性，不作为最终异步性能结论。

> `2026-08-25` runtime 收敛：删除没有生产消费者的 player/machine/crop/town shadow comparison、machine observation API、总 snapshot、lookup/timing 计数和 input-owned mailbox。`enableDispatch` 只保留 Java `Executor` 调度边界；当前传入 `Runnable::run`，未来异步实现必须提供同一 input 内串行、按提交顺序且不重叠的 executor。

> `2026-08-25` FarField/稳定帧收敛：Page capture 增加固定 `16x16 byte` 天空暴露截面，只有天空暴露 component 可按实际 microface 面积接入各维度 `WorldTemperature.naturalAir`；开放方向数不再冒充室外证明。地下直接 Page 沿开放面 capture 一层已加载 continuation，每维度最多 `64` Page、不递归且不加载 chunk；剩余边缘保持 degraded，并以 `1/(1+16)` 距离因子使用弱边界。全局风力只连续缩放同一空气阻抗，不新增 topology/wind profile 矩阵。无 topology 变化的 frame 只 ACK watermark 并复用已安装 sweep，不再逐 tick 重编 pair/FarField。

> 本次整合以第 `0..91` 节为 V1 实现权威，并恢复误删前计划中的已验证现状、兼容合同、迁移边界、性能基线和未决参数。旧 `REDUCED_RC_ISLAND`、`ThermalSemanticGraph`、`CompiledThermalPlan`、`IslandRuntime`、`PartitionRuntime` 和 `PortalEdge` 只在附录 D 中说明替代关系，不得作为并行 V1 实现路径。

> `2026-08-24` 三方 Minecraft 工程复审后，V1 继续保留独立 source ledger/rebind、dimension-level `SolveEpoch`、无 epoch backlog、保守 geometry mutation 和分离的预分配 publication；同时补齐保守 shape raster、coarse/fine face ownership、空气体积变化账本、低层 section mutation spike、source segment replay、versioned publication、bounded dimension mailbox 和测试层级。`STEADY_SOURCE_SLEEP` 与细粒度 publication reuse 已移出 V1。

## 阅读和优先级

1. 第 `0..91` 节定义 V1 的实现合同、阶段和 Definition of Done。
2. 附录 A 记录已由源码和数据核对过的当前实现基线；Phase 0 必须复测其中的数值，不能把旧日志当成生产承诺。
3. 附录 B 记录迁移期间必须保留的玩法与 API 兼容合同。
4. 附录 C 给出 Phase 0 的基准矩阵、容量核算和竞争原型要求。
5. 附录 D 明确旧架构概念的保留、改名或淘汰结果；发生冲突时，第 `0..91` 节优先。
6. 附录 E 保存仍需通过原型回答的参数和文档影响。

源码、配置、数据包和存档数据仍是当前行为的最终权威；本文只拥有预实施架构权威。

---

## Minecraft 工程可实施性判定

本表是第 `0..91` 节的审查索引，不是完成状态。`✓` 表示方案可实施且工程上合理；`✓ 有条件` 表示只有通过指定 prototype/reference gate 后才能冻结参数；`✗` 表示原方案不成立，表中替代方案以及对应正文才是实施权威。

| 方案 | 判定 | 工程处理 | 覆盖章节 |
|---|---|---|---|
| `NaturalBackend + AnalyticFieldBackend + SparseThermalMeshBackend` | ✓ | 保留三种互不重复的真值职责，并补显式 query compositor | 2、64 |
| `CachedLocalSurface` 作为第四个世界后端 | ✗ | 只作为 receiver-local 的缓存表面贡献，不拥有 `H` 或世界空气真值 | 2、64 |
| 全局 `Room/Cave/Island/Partition/Portal` 对象 | ✗ | 使用按需 Page、Brick、相邻 face 与 Boundary 表达连续局部传播 | 3、81 |
| `16^3` Page、`4^3` Brick、`4/8/16` AirCell | ✓ 有条件 | 保留；LOD 与内存参数必须通过 geometry/reference/retained-memory gate | 4..9、19 |
| `int coverageRef[64]` 且不冻结 128-cell mask | ✓ | correctness 路径保留宽索引，不用未经 census 证明的窄 packing | 5、6 |
| 原型前冻结全部 packed SoA 宽度和 arena size class | ✗ | correctness 先用 primitive `int/double` arrays；JOL/JFR/JMH 后再冻结 packing | 6、10、13、76 |
| `BlockState + FluidState` resolved signature | ✓ | 主线程解析，worker 只接收 immutable primitive IDs | 10、41、61 |
| collision shape 同时作为 airflow/contact/radiation 真值 | ✗ | 三个独立 resolver channel，各自 profile、mask 与 fallback | 10、11 |
| 任意 state/shape 自动泛化成可通风 topology | ✗ | `hasDynamicShape == false` 的 state 走统一 state-static resolver；动态 shape 仅允许显式 profile/override 或有界 contextual resolver，其余 observable unsupported | 10、11 |
| 未定义规则的 `4x4` face aperture 转换 | ✗ | 使用坐标压缩或保守微体素补集；整块 quarter-face 确认开放才置 bit | 11、12、Phase A |
| 多 local air regions 与 census-derived `Rmax` | ✓ 有条件 | 保留；signature ID、region count 的 packed width 同样由 census 决定 | 10..14 |
| Door/Trapdoor/FenceGate 统一成 `GateOperator` | ✗ | 它们走 state/fluid resolve + Brick rebuild；Gate 只用于非拓扑可调 `G` | 15 |
| moving piston/Create contraption 精细热拓扑 | ✗ | moving piston 走动态 unresolved；Create contraption 组装后不属于静态 `Level` 方块网格，移动期固定按空气且不建 exclusion，只捕获 assemble/disassemble 的世界方块变化 | 10、41、Phase F |
| V1 只给 AIR 建 volumetric mesh | ✓ | fluid/solid 仍进入 geometry/contact/boundary，不建立 fluid volume solver | 16 |
| geometry trivial 即可 coarse merge | ✗ | 还必须通过 source/boundary/gradient/error estimator 与迟滞 | 8、19 |
| V1 强制 2:1 AMR balance | ✗ | 用对齐的 `FacePatchIterator` 直接处理 16-to-4 与 8-to-4 | 20..22 |
| 只写 `+X/+Y/+Z` 即完成 coarse/fine ownership | ✗ | 世界负轴侧拥有 canonical patch key；冻结 `A`、`d`、mixed aperture 与跨 Page 规则 | 20..22 |
| 永久 generic Air-Air edge graph | ✗ | 由 coverage/face ports 隐式枚举并在本 sweep 聚合同一 cell pair | 21 |
| `H/C/P/G` 量纲、指数 pair/boundary kernel | ✓ | 保留；kernel 只收 `dtSeconds`，单 pair 精确而全 sweep 有 splitting error | 17、18、51..54 |
| 所有 rebuild 无条件满足 `sum(Hbefore)=sum(Hafter)` | ✗ | 纯重新分区守恒；空气体积增减走 overlap + ingress/egress 环境交换账本 | 40、42 |
| 独立 source ledger、精确 `integral(P dt)` 与 event-time rebind | ✓ | 保留，旧 `HU/heat/power` 只能经 profile 显式映射 | 24..29 |
| resync 直接把 cumulative delta 注入最终 binding | ✗ | retained binding segments 顺序 replay；cumulative 只做 checksum | 26、60.1 |
| source-radius world scan | ✗ | emission ports + interest admission；stable solve 只读 node accumulator | 24..29 |
| 非零 source Page 在 V1 休眠 | ✗ | V1 一律 ACTIVE；`STEADY_SOURCE_SLEEP` 退到 V2 独立研究 | 55、81 |
| stateless/capacitive/phase material boundaries | ✓ 有条件 | 保留 Brick-local 聚合；材料参数由 reference/gameplay 校准 | 30..39 |
| 两格以上厚墙直接压成一条 `G` | ✗ | 两侧 surface/deep boundary 独立，只有经过模型证明才跨厚材料耦合 | 32..35 |
| Brick-local phase ownership 与 request/ACK | ✓ | 保留，ownership 与 random tick policy 分离 | 36..40 |
| `LevelChunk#setBlockState` 单点 hook 默认覆盖全部 mutation | ✗ | Phase 0 先验证低层 `LevelChunkSection#setBlockState(..., boolean)`、owner map 与兼容 adapter | 41、75、Phase 0 |
| tick-end source/geometry sealing 与 dirty interface 立即禁用 | ✓ | 保留 effective tick、watermark vector 与下一 epoch 生效语义 | 41、51、60 |
| 未加载/未解析邻区自动当 outdoor | ✗ | 保持 `UNRESOLVED/OPEN_CONTINUATION`；预算不足走可观察 degraded path | 45..47、75 |
| 未校准 `STATIC_IMPEDANCE` 直接作为 V1 已批准模型 | ✗ | 只作为 candidate；holdout reference gate 未过则不能进入 Minecraft integration | 48..50、Phase E |
| expansion 由 source 功率或总 heat-flow threshold 触发 | ✗ | 使用 profile error envelope、observable tolerance、迟滞与 cooldown | 50 |
| uniform cadence、每维一个 logical writer、无 epoch backlog | ✓ | 保留，但 dispatch 必须是有界 mailbox，不复用无界 executor queue | 51..57 |
| 未定义 dispatch 的 `newFixedThreadPool` | ✗ | `IDLE/QUEUED/RUNNING` CAS mailbox + bounded ready queue + sticky re-offer + fairness | 57、60 |
| worker 延迟时把 source 能量记为 `TIME_DEGRADED_LOSS` | ✗ | source 仍完整进入有效 `H`；只跳过未求解 transport/phase 时间并显式 rebase | 56 |
| ring buffer 是唯一 state authority | ✗ | owner-owned sticky recovery、reserved quota、oldest-age promotion 与 watermark replay | 60.1、77 |
| 仅靠 `volatile publishedIndex` 的双缓冲 | ✗ | 单调 `publicationVersion` seqlock 同时封存 geometry/thermal envelope | 62、63 |
| V1 细粒度 query footprint reuse | ✗ | V1 只保留 Page-wide revision fallback；数据证明必要后再做 V2 | 63 |
| record-only query API 同时承诺 hot path 零分配 | ✗ | hot API 使用 primitive 参数 + caller-owned output；record 只用于 cold/debug | 64、66 |
| passive body/crop/town query 与显式 interest lease | ✓ | 保留；普通 miss 不创建 mesh | 65、72、73 |
| radiation 进入空气运输 mesh | ✗ | 独立 receiver-side LOS/flux service，不重复向 player 注入 source energy | 67..70 |
| 每个玩家直接扫描所有 radiation sources | ✗ | packed section source index + 距离/flux cutoff + candidate/ray hard budget | 68、69 |
| 低 Y 直接给空气加地热 | ✗ | 只允许 exposed deep-rock MaterialBoundary 与空气交换 | 71 |
| unload 邻居作为开放 ambient | ✗ | 立即失效 transport/publication，使用 unloaded frontier 与 generation check | 75 |
| 静默回收有非平衡 `H` 的 sleeping Page | ✗ | 仅回收无 owner 状态并记录 signed eviction-environment exchange；另设 server-global cap | 78、79 |
| 仅用 pure-Java unit test 证明 Minecraft hook/lifecycle | ✗ | JUnit 5 + Forge GameTest + JMH/JFR + production-like shadow 四层验证 | 82、85 |
| `<8 MiB / 100 players` 作为无场景单一 gate | ✗ | Phase 0 冻结 workload-specific acceptance table；typical 与 stress 分开 | 84、附录 C |
| feature flag + shadow runtime 后再迁移 gameplay | ✓ | 保留；FarField、hook、correctness、memory 与 workload gate 缺一不可 | 82、89 |

---

## 0. 文档目的

本文定义 Frosted Heart 新一代局部热学系统的 **V1 工程实现合同**。

它不是物理模拟研究文档，也不是长期愿景文档。

V1 的目标是实现一个：

* 能表达墙、门、洞穴、狭窄竖井和复杂 Minecraft 方块几何；
* 能真实区分温度 `T`、功率 `P`、热容 `C`、焓/能量 `H`、导纳 `G`；
* 能让热量沿真实局部空间连续传播；
* 能支持空气积热、墙体导热、雪冰相变、地热等玩法；
* 不建立世界尺度温度场；
* 不按热源半径扫描世界；
* 不维护房间、洞穴、热岛等全局拓扑对象；
* 不让普通查询触发昂贵世界扫描；
* 能在多人服务器上通过硬预算控制 CPU、内存和 GC；
* 在服务器过载时宁可降低精度，也不能突破 TPS / memory hard cap；
* 可以逐阶段 benchmark、shadow run 和替换旧系统

的 **Minecraft-specific 稀疏有限体积热学运行时**。

本文冻结 V1 的核心结构。

在 V1 benchmark 证明现有结构存在实际问题之前，不新增新的宏观 thermal abstraction。

---

## 1. 最终核心定义

整个系统可以概括为：

> **以 Minecraft state/fluid 的有界保守局部拓扑作为几何依据，以惰性 `16³ ChunkSection` 作为分页，以 `4/8/16` 空气控制体作为热自由度，通过相邻面守恒交换运输热量，并通过 MaterialBoundary 与经 reference gate 批准的 FarField 接入固体和无限自然环境的稀疏有限体积运行时。**

核心表示：

```text
16³ ThermalPage
    运行时分页、生命周期、publication

4³ MixedGeometryBrick
    Minecraft 复杂局部 topology 真值

4³ / 8³ / 16³ ThermalCell
    空气有限体积热自由度

H / C / P
    动态热状态

implicit Air-Air face exchange
    空气内部运输

BoundaryOperator
    墙、门、雪、冰、地面、FarField、degraded 等特殊关系

NaturalBackend
    没有局部热状态的大世界

RadiationService
    与空气运输独立的直接辐射

StateTransitionSystem
    最终 Minecraft BlockState 改变
```

---

## 2. 顶层执行架构

必须区分“逻辑后端”和“执行成本层级”。系统保留三个逻辑后端：

```text
NaturalBackend
    自然气候、季节、海拔、天然空气和岩层温度

AnalyticFieldBackend
    Boss、脚本和管理员命令控制场
    不声明世界能量守恒

SparseThermalMeshBackend
    真实 H / C / P / G
    空气积热、空间运输、材料热惯性和局部相变
```

`CachedLocalSurface` 是查询/材料执行层级，不是第四个拥有独立世界真值的后端。查询和材料响应按三个成本层级执行：

```text
BACKGROUND_ANALYTIC
        ↓
CACHED_LOCAL_SURFACE
        ↓
SPARSE_THERMAL_MESH
```

### 2.1 `BACKGROUND_ANALYTIC`

默认世界状态。

适合：

```text
露天自然区域
普通雪原
没有物理热源的地下
普通自然空气
无需热历史的环境查询
```

成本目标：

```text
0 ThermalPage
0 ThermalCell
0 MixedGeometryBrick
```

### 2.2 `ANALYTIC_FIELD`

用于：

```text
移动 Boss 冷热场
脚本或管理员控制场
不要求局部能量守恒的剧情效果
```

一个解析场只保存一份 definition，不复制到覆盖的每个 chunk。移动场更新 bounds/generation，不触发 `MixedGeometryBrick` 重编。设备不得注册为解析场；Campfire、Generator、Radiator 与 Fountain 的对流、接触和辐射通道只进入第 `24..29` 节的统一物理功率路径。

### 2.3 `CACHED_LOCAL_SURFACE`

用于：

```text
legacy BlockTemp
玩家贴近冰 / 岩浆等表面
简单局部体感
无需空气积热的局部材料响应
```

不保存：

```text
H
C
动态空气场
```

### 2.4 `SPARSE_THERMAL_MESH`

只有真正需要：

```text
物理 Generator
物理篝火
封闭空间空气积热
洞穴 / 竖井热运输
材料余热
局部雪冰相变
地热传递
需要温度历史的机器
```

时存在。

禁止为了架构统一，把普通世界全部升级成 mesh。

### 2.5 Query Compositor

三个逻辑后端不是“命中谁就提前 return”的模糊链。V1 冻结以下 compositor：

```text
1. NaturalBackend 生成 background air/rock state
2. revision-valid SparseThermalMesh publication 替换局部 air state
3. AnalyticFieldBackend 按显式 combine mode 合成控制效果
4. CachedLocalSurface 追加 receiver-local surface/contact contribution
5. RadiationService 追加 receiver-local radiant flux
```

解析场 combine mode 只有：

```java
enum AnalyticCombineMode {
    OVERRIDE,
    MAX_HEAT,
    MIN_COOL,
    ADD_DELTA
}
```

重叠场按 `mode -> explicitPriority -> fieldId` 确定性合成。解析场只改变 query 输出，不回写 mesh `H`，不能驱动 `LOCAL_ENERGY_ACCOUNTED` phase。`CachedLocalSurface` 也不伪装成空气温度；它输出 `surfaceFlux` 或明确声明的 receiver contribution。所有设备热源只允许一个 physical source identity，不存在设备解析场分支。

---

## 3. V1 架构硬 Invariants

以下规则属于 correctness / architecture contract。

工程实现不得为了方便绕过。

1. 不存在全局 Thermal Octree。
2. 不存在全局 Geometry Octree。
3. 不存在 `Room`。
4. 不存在 `CaveZone`。
5. 不存在 `IslandRuntime`。
6. 不存在 `PartitionRuntime`。
7. 不存在 `PortalEdge`。
8. `ThermalPage` 永远与 Minecraft `16×16×16 ChunkSection` 对齐。
9. V1 全局 Thermal LOD 只有 `4³ / 8³ / 16³`。
10. `32³+` 不属于 V1。
11. 全局 `1³ / 2³ AMR` 不属于 V1。
12. 大于 `4³` 的 ThermalCell 必须是 topology trivial 的单一连续 transport medium。
13. Minecraft 复杂 topology 永远局限在一个 `4³ = 64 voxel` Brick，并以不产生 false opening 的有界保守近似表达；不宣称精确表示任意 modded shape。
14. `GeometryComponent != ThermalCell`。
15. Primary volumetric mesh V1 默认只承载 `AIR`。
16. 固体默认表示为 Boundary，而不是 volumetric node。
17. 普通 Air-Air connection 不保存永久 generic edge。
18. 所有长距离运输必须由连续 local face exchange 自然产生。
19. 不允许 source-radius heat field。
20. 不允许 Generator → Snow 等直接 gameplay link。
21. 不允许 world-wide snow / ice index。
22. 有状态 phase material V1 永远 Brick-local。
23. Passive query 不能创建 mesh。
24. Crop / Town aggregate 不能持有 mesh lease。
25. Thermal 系统不能主动加载 chunk。
26. 未验证 topology 不允许局部热量传播。
27. FarField 是降阶环境阻抗，不是普通空气边。
28. Field expansion 根据误差，而不是 source 功率决定。
29. Source 声明功率 `P`，不声明“温度影响半径”。
30. Solver 稳定路径不能遍历所有 source。
31. V1 不实现 multi-rate timestep。
32. V1 不实现 per-edge thermal clock。
33. 所有 participating cells 使用统一 thermal time interval。
34. Worker 永远不能读取 Minecraft World。
35. 普通 query 永远不能等待 worker。
36. Query cache hit 不能读取 Minecraft World。
37. Runtime state 必须 primitive、bounded、admission-controlled。
38. Overload 时只允许降低精度或显式丢失未建模能量，不允许突破 hard budget。
39. V1 一个 Block 的局部空气 region 数必须受有限 `Rmax` 约束；`Rmax` 由已启用 signature/resolver census、内存核算和 Brick compiler benchmark 冻结，不能把经验值 `4` 写成架构常数。
40. 一个 Brick 的编译 atom 上限为 `64 * Rmax`；实现使用扁平 primitive atom spans，不假设一 Block 只有一个空气 component，也不按 region 创建对象。correctness 路径的 signature/region/span ID 使用 `int`，窄 packing 必须等 census 后决定。
41. Door、Trapdoor、FenceGate 等改变连通性的 Minecraft 方块使用 `BlockState + FluidState -> resolved geometry -> Brick rebuild`；`GateOperator` 只表示机器阀门等不改变拓扑、只调整既有端口导纳的显式 profile。
42. Source power ledger 与 `ThermalCell` 存储分离；source rebind 必须先在新旧 accumulator 上结算到事件 tick。
43. Coverage correctness prototype 使用 `int[64]`；未用 benchmark 证明 page-local slot 上限前禁止压缩成 `short`。
44. V1 不冻结 `128`-cell active mask 或其他与物理无关的 per-page cell 上限。
45. 所有交换属于同一个 dimension-level `SolveEpoch`；每维度最多一个逻辑 in-flight epoch，不排队追赶多个错过的 epoch，也不把任意长延迟直接变成超出 `maxSolveDeltaTicks` 的 kernel `dt`。
46. topology dirty 后受影响 interface 立即进入 unresolved/disabled，新连接只从 rebuild 后的下一 epoch 生效。
47. Publication 将低频 geometry mapping 与高频 thermal values 分离；稳定 solve 使用预分配双缓冲和单调 `publicationVersion` seqlock，达到零 publication allocation。V1 只接受 Page-wide revision match，不实现细粒度 footprint reuse。
48. V1 中含非零 `POWER_SOURCE` 的 Page 不进入 `SLEEPING`；`STEADY_SOURCE_SLEEP` 不属于 V1。
49. Geometry signature 至少由 `BlockState + FluidState` 决定；contextual resolver 只能读取已声明的有限 dependency-offset mask，越界、未加载或不可解析输入必须得到 `UNRESOLVED`，不得触发 chunk load。
50. Airflow aperture、material contact 和 radiation occlusion 是三个独立 signature channel；不能把一个 collision shape 同时当作三者的真值。
51. Worker 只接收 main thread 已解析的 primitive signature/geometry ID，永远不能接收任意 `Level`、`VoxelShape` callback 或 BlockEntity 引用。
52. 每个 sealed epoch 都携带 geometry/source/chunk/profile/transition 输入 watermark；worker 只有应用到这些 watermark 后才能 solve 和 publication。
53. 同 tick 的 source rebind 与 geometry mutation 必须按“旧 binding 结算到 `t` -> 封存 state/fluid mutation -> resolve/rebuild -> 安装 `t` 之后的新 binding -> solve”排序。
54. Queue overflow 不能静默丢事件；每类 queue 必须有 Page/source/reservoir-owned sticky resync/retry 状态和可比较的 sequence/watermark。
55. “每维度单 writer”表示逻辑所有权，不表示为每个已加载维度永久占用一个 OS thread；执行使用有界 shared executor。
56. Air-Air exchange 的 exactly-once 由世界坐标负轴侧的 canonical face patch key 保证；`+X/+Y/+Z` 只是遍历方向，不是完整 ownership 证明。
57. 世界 geometry mutation 导致空气容量增减时，只有 overlap 部分保留旧温度；新增/移除体积必须进入 signed ingress/egress 环境交换账本。
58. Source resync 的 cumulative emitted energy 是 checksum，不是可再次注入最终 binding 的能量包；恢复按 retained binding segments 顺序 replay。
59. 调度延迟不能成为 source 能量出口；`TIME_DEGRADED` 可以跳过 transport/phase 时间，但完整可归属的 `integral(P dt)` 仍进入有效 `H` 或 profile 声明的真实 sink。
60. `LevelChunkSection#setBlockState(..., boolean)` 只是待 Phase 0 GameTest 证明的 primary hook candidate；任何 Minecraft integration 之前必须验证 mutation/lifecycle coverage matrix。
61. Chunk unload/reload 由 main-thread `lifecycleGeneration` 立即失效；worker 不能发布旧 incarnation 的任何结果，source 必须先 settle 到 unload tick。
62. Memory admission 同时受 per-dimension 与 server-global cap 约束；source registry、mailbox、publication resize 峰值和 radiation state 都必须入账。
63. 非平衡 `H` 只能通过有 reason 的 signed environment-exchange ledger 释放；active source、phase reservation 和 stateful material 不能作为普通 cache eviction。
64. Production query hot path 使用 primitive 参数和 caller-owned mutable output；immutable record 只允许 cold/debug wrapper。
65. Radiation discovery、candidate、ray 和 witness 全部有硬上界；被动观察不能修改 source ledger 或重复注入 radiation energy。

---

## 4. 空间分页：`ThermalPage`

### 4.1 Page 定义

```text
ThermalPage
=
Minecraft ChunkSection
=
16 × 16 × 16 blocks
```

Page 只是：

```text
addressing
storage locality
lifecycle
geometry cache ownership
thermal coverage
query publication
budget accounting
```

Page 不是物理控制体。

Page 本身没有：

```text
room identity
cave identity
thermal island identity
```

---

## 5. Page 内固定基础索引

每个 Page 固定划分为：

```text
4 × 4 × 4

=

64 个 4³ base bricks
```

基础索引：

```java
int bx = (x & 15) >> 2;
int by = (y & 15) >> 2;
int bz = (z & 15) >> 2;

int baseIndex =
      bx
    | (bz << 2)
    | (by << 4);
```

V1 correctness prototype 使用：

```java
int coverageRef[64];
```

如果 benchmark 证明 page-local slot 范围可以安全压缩，再把 ref 改为 `short`。不得提前引入隐式 cell-count 上限。

每个 base brick 指向当前覆盖它的 thermal support。

例如：

```text
整个16³ section都是空气：

coverageRef[0..63]
→ 同一个16³ AirCell
```

复杂区域：

```text
墙附近
→ 4³ supported cells

远离墙
→ 8³ / 16³ AirCells
```

Query 因此可以做到：

```text
BlockPos
→ sectionKey
→ ThermalPage
→ baseIndex
→ coverageRef
```

目标：

```text
O(1)
```

---

## 6. 推荐 `ThermalPage` 逻辑结构

以下只是逻辑结构。Phase A-C correctness prototype 使用直接的 primitive `int/double` arrays；生产候选仍禁止 per-cell/per-boundary Java object，但具体 SoA 分组、packed width、arena size class 和 free-list 只能在 JOL/JFR/JMH 后冻结。

```java
final class ThermalPage {
    long sectionKey;

    int[] coverageRef;           // 64 fixed entries

    long mixedBrickMask;
    long dirtyBrickMask;

    int geometryRevision;
    int topologyGeneration;

    int firstCellSlot;
    int cellCount;

    int firstBoundarySlot;
    int boundaryCount;

    byte activeState;

    long lastCompletedSolveEpoch;
    long sealedInputWatermark;
    long appliedInputWatermark;

    int publishedGeometryRevision;
    long publishedSolveEpoch;

    int flags;
}
```

`flags` 至少能表达 Page-owned sticky：

```text
FULL_GEOMETRY_RESYNC_REQUIRED
TIME_DEGRADED
```

`FULL_GEOMETRY_RESYNC_REQUIRED` 不依赖 `GeometryDeltaRing` 中还有空位；一旦 geometry event 无法入队，main thread 仍必须立即递增 live revision、设置该 flag 并让旧 topology publication 失去 baseline 有效性。

必须区分：

```text
live geometryRevision

和

publishedGeometryRevision
```

这是 query correctness 的重要合同。

### 6.1 不冻结固定 active-cell bit mask

禁止把 Page 活跃状态冻结成：

```java
long activeCellMaskLo;
long activeCellMaskHi;
```

mixed component 数量没有 `<= 128` 的架构保证。V1 使用：

```text
dense active page list
+ page-owned primitive cell-index span
+ cellFlags[cellSlot]
```

如果 benchmark 证明可变长度 primitive bitset 有净收益，可以后续加入；不得为了 bit mask 人工设置 `MAX_CELLS_PER_PAGE = 128`。

---

## 7. Geometry 不建立第二套世界结构

Geometry 只有：

```text
implicit homogeneous proof

或

MixedGeometryBrick
```

两种表示。

如果 Minecraft 能廉价证明：

```text
整个 section = AIR
```

允许直接创建：

```text
16³ AirCell
```

而不是为了证明它去读取：

```text
4096 BlockState
```

禁止：

```text
“我要创建16³ cell”
→ recursively materialize all children
→ scan section
```

---

## 8. Coarse Geometry Proof

Coarse support 只允许通过两种方式出现。

### 8.1 Cheap proof

例如底层 Minecraft chunk storage 已明确给出：

```text
section completely empty
```

则直接得到：

```text
16³ ALL_AIR
```

不能为了产生 cheap proof 主动扫描整个 section。

### 8.2 Bottom-up merge

如果 child summaries 已经存在，并且全部证明：

```text
single medium
single connected component
no internal gate
no material interface
no phase
no source core
compatible topology
```

才允许形成 parent summary。

---

## 9. Geometry Summary Cache

每个 active Page 最多维护固定：

```text
64 × 4³ summary
8  × 8³ summary
1  × 16³ summary
```

一共：

```text
73 summaries / Page
```

它们不是 geometry authority。

只用于：

```text
coarse merge
topology trivial proof
thermal refinement
query support
```

Summary 基础状态：

```text
UNKNOWN
SINGLE_AIR
SINGLE_MEDIUM
MIXED
```

以及少量 topology flags。

---

## 10. `ThermalSignatureRegistry`

Solver 不直接处理 Minecraft `VoxelShape`，也不假设 Minecraft 提供统一的“热空气形状”API。

普通方块只允许通过以下三条路径解析：

```text
1. explicit thermal profile
2. generic state-static resolver
   - state.hasDynamicShape() == false
   - fixed CollisionContext.empty()
3. declared DependencyOffsetMask contextual resolver
```

Door、Trapdoor 和绝大多数 FenceGate 的 stored state 已包含 open/facing/hinge 等必要属性，可以走 `SELF_ONLY` state geometry。V1 对所有 `hasDynamicShape() == false` 的 Vanilla/modded state 使用同一 generic resolver，不做逐模组 allowlist 或专用适配；只有实际需要纠正玩法语义时才通过保留的 explicit override/contextual registration 接口接入。动态 shape 若没有显式 profile/override 或有界 contextual resolver，或其输出依赖 BlockEntity/entity/collision context、超出声明 mask，则进入 `CONSERVATIVE_UNSUPPORTED`。Worker 只接收 resolved signature ID，不能读取 `Level`、邻域、BlockEntity 或 context-sensitive callback。

逻辑合同：

```java
record ResolvedThermalSignature(
    int mediumId,
    int materialProfileId,

    int localAirRegionCount,
    LocalAirRegionPattern[] airRegions,

    int materialContactPatternId,
    int radiationOcclusionPatternId,

    int sourceProfileId,
    int gateKind,
    int flags
) {}
```

以上是 correctness width，不是最终 raw-byte layout。PR 2 必须输出总 BlockState 数、唯一 signature 数、context output 数、最大 local-region 数和 reload 峰值后，才可证明某个字段能改成 `short/byte`；任何 packed overflow 都必须变成 observable unsupported，不能 wrap。

每个非 `SELF_ONLY` resolver 必须注册精确的 `DependencyOffsetMask`、`maxOutputRegions` 和 deterministic resolver ID。V1 允许的 offset 位于以目标 block 为中心的 `3^3` 范围内，并可使用：

```text
SELF_ONLY
NEIGHBOR_6
NEIGHBOR_26
explicit finite subset of NEIGHBOR_26
```

Main thread 从 immutable、loaded-only snapshot 读取 mask 内的 `BlockState + FluidState`；resolver 访问未声明 offset、未加载 chunk、缺失 snapshot 数据或需要 BlockEntity/entity state 时，结果必须是：

```text
UNRESOLVED / CONSERVATIVE_UNSUPPORTED
```

注册 resolver 必要时可以在 main thread 调用 shape API，但传入的只能是 snapshot-backed `ResolverBlockView`；该 view 对未声明 offset、未加载位置和 BlockEntity 查询返回 unresolved/error sentinel，不能透传真实 `Level`。resolver 不得发布未经证明的 opening，也不得直接访问 World。依赖关系由 offset mask 反向推导，不建立无界 geometry dependency graph。

单点 mutation 的 invalidation 不能把 `NEIGHBOR_26` 错算成只读 `27` 个位置：该 mutation 可影响 `3^3` 个 resolver center，而重新解析这些 center 的 union read closure 可达 `5^3 = 125` 个位置。`4^3` Brick cold build 在同类 mask 下的 union snapshot 最多为带一格 halo 的 `6^3 = 216` 个位置。预算按去重后的实际 snapshot read 计数。

以下动态几何不进入 V1 精细 resolved topology：moving piston 的瞬态 BlockEntity shape、Create contraption 内部 block topology、任意 BlockEntity NBT 驱动的 shape。静态 piston base/head BlockState 正常解析；moving piston 使用 `UNRESOLVED_DYNAMIC`。

Create 采用明确的静态世界网格语义：assemble 把原位置方块移出 `Level`，按普通 `block -> air` mutation 处理；contraption 作为实体移动期间固定按空气，不进入热几何、不维护 AABB exclusion、不携带 V1 热状态；disassemble 在目标位置按普通 `air -> block` mutation 处理。目标位置的新空气/材料状态继续服从第 40 节 geometry ingress/egress 合同。不得为 Create movement 增加专用 adapter 或 Mixin。

Airflow、接触和辐射必须分别解析。例如 waterlogged partial block 可以同时具有受限空气 aperture、fluid/material contact 与不同的辐射遮挡；任何一个 channel 不得从另一个 channel 的 mask 隐式推导。

---

## 11. Face Aperture

每个 local air region 的每个 block face 使用：

```text
每个 block face
=
4 × 4 sub-face mask
=
16 bits
```

`4x4` mask 是保守 port 投影，不是任意 `VoxelShape` 的精确真值。registry compile 使用 AABB coordinate-compressed complement，或经过 reference 对照的保守微体素分解，先求 block 内空气 components；禁止中心点采样、面积超过阈值即开放，以及“tile 内存在任意空隙即开放”。

一个 quarter-face bit 只有同时满足以下条件才置 `1`：

```text
整个 1/4 x 1/4 face tile 都为空
AND tile 内所有点都属于同一个 local air component 的边界
AND tile 向 block 内的保守 support 不与 airflow blockage 相交
```

shape 与 tile 只有零面积边界接触时不算 blockage；任何正面积/正体积相交都关闭该 bit。内部 component decomposition 可以比 `4x4` face mask 更细，face mask 只负责跨 block 对齐。Phase A 必须用高分辨率/reference shape 比较证明：允许少开口，不允许多开口。

相邻 region 的有效 opening：

```java
int opening =
    aRegion.posXOpenMask()
    & bRegion.negXOpenMask();
```

因此两个：

```text
50% open
```

但开口位置互相错开的 shape：

```text
不会产生 false connection
```

这用于保守处理：

```text
slab
stairs
door
trapdoor
fence
pane
snow layers
modded partial shapes
```

---

## 12. Block 内局部 Air Region

V1 从第一天支持一个 Block 内多个互不连通的局部空气 region。`pane`、`fence`、薄障碍和部分 modded shape 不能安全压成 `one voxel = one air component`。

每个 `LocalAirRegionPattern` 至少保存：

```text
faceOpenMask[6]       // 每面 16 bit
permeability[6]
volumeFraction
```

冻结的是“有限且已测量”，不是固定经验值：

```text
Rmax = max supported local-air-region count
```

Forge resolver census 必须枚举已启用 BlockState/其实际 FluidState 与注册 resolver fixture，校验每个 resolver 的 `maxOutputRegions`，输出 region-count census、最大值、频率分布、atom arena bytes 和 compile p95/p99，再冻结生产 `Rmax`。context 组合无法穷举时必须做 property/fuzz fixture，并让运行时输出超过声明上限时直接转为 observable unsupported，不能写越 arena。`FULL_AIR` 是一个全部 face 连通的 region，`FULL_SOLID` 是零 region。超过已冻结 `Rmax` 或无法在声明依赖内可靠解析时使用 `CONSERVATIVE_UNSUPPORTED`，关闭不确定 transport。registry reload 必须重新验证 hard cap；不满足时保持可观察的 unsupported fallback，而不是扩张 arena 或写坏 packed state。

原则不变：false-negative opening 会降低传播精度；false-positive opening 会穿墙，因此不可接受。

---

## 13. `MixedGeometryBrick`

只有包含复杂 topology 的 `4³` 区域才创建 Brick。

逻辑结构：

```java
final class MixedGeometryBrick {
    long brickKey;

    int[] resolvedSignatureId;        // 64, correctness layout

    int atomSpanOffset;
    int atomCount;

    int compiledComponentOffset;
    int compiledComponentCount;

    int facePortOffset;
    int facePortCount;

    int materialInterfaceOffset;
    int materialInterfaceCount;

    int generation;
}
```

生产候选禁止真的按 component/cell 产生大量对象。correctness prototype 先保持可检查的 primitive arrays；measurement 通过后再选择：

```text
primitive arena
fixed spans
bitsets
packed arrays
blockAtomOffset[65] + flattened atom arrays
```

---

## 14. ComponentBrick Compiler

Compiler 的 primitive atom 是：

```text
(blockLocalIndex, localAirRegionId)
```

输入：

```text
64 ResolvedThermalSignature IDs
```

输出：

```text
local air components
air component volumes
air component centroids
external face ports
material interfaces
gate interfaces
topology generation
```

算法 V1 可直接使用：

```text
bounded BFS
union-find
fixed queue
```

最大 atom 数：

```text
64 blocks * Rmax local regions
= 64 * Rmax atoms
```

因此动态复杂 topology 工作量必须满足：

```text
O(64 * Rmax) finite upper bound
```

这是一条重要性能 invariant。

不要在 V1 过早实现复杂 bit-parallel flood fill。

先 benchmark。

---

## 15. Brick Face Ports

一个 Brick 每个外表面有：

```text
4 × 4 block face slots
```

每个 slot 至少知道：

```text
local component
aperture mask
permeability
material interface
optional non-topological gate metadata
```

跨 Brick 连接只比较：

```text
共享面
```

不做：

```text
room search
pathfinding
graph traversal
```

### 15.1 Gate 语义冻结

`GateOperator` 只表示不改变几何连通分区、仅调整两个已确认 transport port 之间导纳的设备，例如机器阀门、风门或 profile 明确声明的可调换热器：

```text
Air Component A
      |
 GateOperator
      |
Air Component B
```

closed 时 `G = 0`，open 时 `G` 取 profile 校准值；Coarse cell 永远不能跨这种显式 Gate merge。

Minecraft Door、Trapdoor、FenceGate 的开关首先是 `BlockState` geometry mutation，必须重新 resolve 受依赖 mask 影响的 state、重编相关 Brick，并按 overlap 迁移 `H/C`。Door 两半可以跨 Brick 或 ChunkSection；同 tick batch 必须让两侧 Page 使用同一 sealed mutation watermark，不能一半先发布。不能为了少一次 rebuild 把这些拓扑变化伪装成 `G` 参数更新。

---

## 16. Primary Thermal Mesh

V1 volumetric transport medium：

```text
AIR
```

未来可以考虑：

```text
WATER / FLUID
```

但不进入 V1。

`FluidState` 仍然是 V1 geometry/signature 输入：它决定 waterlogged/flowing partial block 的空气占据、接触界面和遮挡。这里“不实现 fluid transport”只表示 V1 不创建 WATER volumetric `ThermalCell`，不表示可以忽略 `FluidState`。

普通：

```text
stone
wood
snow
ice
soil
ground
```

全部优先作为：

```text
BoundaryOperator
```

---

## 17. `ThermalCell`

逻辑结构：

```java
struct ThermalCell {
    double enthalpyH;
    double capacityC;

    int pageSlot;
    int support;

    byte level;
    byte medium;
    byte flags;
}
```

Source ledger 不直接存入 `ThermalCell`。只有确实接收物理功率的 node 才在独立 arena 中拥有：

```java
struct NodePowerAccumulator {
    double currentPowerW;
    double pendingEnergyJ;
    long lastIntegralTick;
}
```

这避免所有 cell 为 source bookkeeping 付费，并把 source identity/lifecycle 与 cell H/C 状态分开。

温度：

```text
T = Tref + H / C
```

建议：

```text
Tref = 0°C
```

动态 authority：

```text
H
```

不是：

```text
cached T
```

---

## 18. Cell Capacity

空气：

```text
C = cEffective × Volume
```

这里：

```text
cEffective
```

是 gameplay-calibrated volumetric heat capacity。

它不要求严格等于现实空气物理常数。

系统目标是：

```text
一致
稳定
可校准
能量语义明确
```

而不是 CFD。

---

## 19. Thermal LOD

V1：

```text
4³
8³
16³
```

大于 `4³` 的 ThermalCell 必须同时满足：

```text
TopologyTrivial
AND
ThermalErrorAcceptable
```

`ThermalErrorAcceptable` 不能是永远返回 true 的占位 boolean。V1 至少冻结以下 mandatory refine 条件：

```text
support 内有 physical source port / stateful boundary / phase / explicit Gate
OR 任一 face forcing bucket 超出该 support 的 calibration domain
OR neighbor/boundary temperature spread 的 profile error bound > REFINE_HIGH
OR unresolved/dynamic topology 与该 support 相交
```

只有没有 mandatory trigger、error bound 连续 `N` epochs 小于 `COARSEN_LOW` 且 topology revision 不变时才 coarsen。`REFINE_HIGH > COARSEN_LOW`，并有最小 residency/cooldown。若某 profile 没有可验证 estimator，就保持 `4^3` 或拒绝 optional expansion，不能只因 geometry trivial 合并。

例如：

```text
16³ fully open air
温度基本均匀
→ 可以16³
```

但：

```text
16³ fully open air
左侧100kW source
右侧cold opening
→ 需要thermal refine
```

即使 geometry 完全简单，也不能忽略 thermal gradient。

---

## 20. V1 不要求 2:1 Balance

允许：

```text
16³
直接邻接
4³
```

一个 `16 x 16` face 按世界对齐的 `4 x 4-block` base patch 分解为：

```text
4 × 4
=
16 macro patches
```

V1 直接处理即可。

`FacePatchIterator` 冻结以下合同：

```text
world negative-axis side owns the patch
axis + packed world patch coordinate = canonical key
16 <-> 4  : 16 base patches
8  <-> 4  : 4 base patches
A          : exact overlap area of the current patch
d          : normal half-width A + normal half-width B
```

负侧 coarse cell 枚举正侧 fine/mixed decomposition；负侧 fine/mixed 由各自 port 发出，正侧永远不反向再算。跨 Page 使用相同 world key。mixed face 继续下分到 block face 和 `4x4` aperture bits，每个 bit 只能归属一个 local-component contact。未 admission 或已 unload 的相邻 Page 是 frontier boundary，不是隐式 Air-Air neighbor。

不要为了形式上的 AMR balance 创建大量：

```text
8³ refinement skirt
```

以后如果加入：

```text
32³ / 64³
```

再 benchmark 2:1 balance。

---

## 21. Air-Air 邻接不保存 Generic Edge

普通：

```text
AirCell A | AirCell B
```

邻接由：

```text
position
level
coverageRef[]
face direction
FacePatchIterator
```

运行时推导。

不保存：

```java
ThermalEdge
```

Solver 只发出：

```text
+X
+Y
+Z
```

三个 canonical directions，但 exactly-once 还依赖第 20 节的 world patch owner/key。相同 `(cellA, cellB, operatorClass)` 的连续 patches 在 fixed scratch accumulator 中先求和 `G` 再执行 pair kernel；不把结果保存成跨 epoch generic edge。

因此每个开放 atomic patch 必须满足：

```text
exactly once / thermal step
```

---

## 22. Air-Air Effective Conductance

空气运输使用 gameplay effective mixing：

```text
Gair =
kMix
× Apatch / (dA + dB)
× permeability
× buoyancyFactor
```

其中 `Apatch` 来自 canonical overlap，规则 cell 的 `dA/dB` 是 face 到 cell center 的法向距离；mixed component 使用 compiler centroid 的正法向距离并设置 profile-defined positive lower bound。两侧 permeability 通过对称 rule 合成，交换 A/B 不得改变 `Gair`。

`kMix`：

```text
不是单纯现实空气分子导热率
```

而是：

```text
游戏尺度上的有效混合参数
```

这样允许 coarse finite-volume cell 在合理成本下模拟：

```text
空气混合
自然对流
弱流动
```

而不用实现压力 CFD。

---

## 23. Buoyancy

V1 只允许：

```text
state-dependent effective G
```

`buoyancyFactor` 使用该 pair substep 开始时的两端温度与确定的世界竖直方向计算，并 clamp 到经 reference 验证的非负 `[bMin, bMax]`。交换 A/B 或反转 traversal order 不能改变同一物理 pair 的 factor；它不能生成负 `G` 或单向额外能量。

例如垂直空气交换：

```text
hot below cold
→ G increased

cold below hot
→ G reduced
```

但是每个 exchange 仍然严格：

```text
-H
+H
```

成对守恒。

禁止 V1 声明：

```text
massFlow kg/s
pressure
air velocity field
```

因为系统并没有实现质量 / 动量守恒。

---

## 24. Source 与 Thermal Mesh 分离

物理 source registry 保存 source identity：

```java
record ThermalSourceEntry(
    long sourceId,
    long packedPos,
    int profileId,
    int sourceRevision,
    long eventWatermark,
    boolean enabled
) {}
```

Source Registry 用于：

```text
add
remove
move
on/off
profile reload
```

Solver 正常路径不遍历 source registry。

Registry 同时维护按 loaded ChunkSection 分桶的 packed source spatial index，只用于 receiver-side radiation candidate discovery、debug 和生命周期定位；它不把一个 source 复制到影响范围内的所有 chunk，也不主动获取或加载覆盖区块。

Main-thread authoritative registry 还必须为每个 source 保存 event-time ledger：

```text
currentPowerW
currentPort/binding state
cumulativeEmittedEnergyJ
lastLedgerTick
lastWorkerAckWatermark
bounded binding-energy segments since ACK
```

它只在该 source 发生 event、生成 resync snapshot 或被显式对账时惰性积分；epoch seal 只记录 source stream watermark，不遍历全部 registry。Worker 侧的 packed registry/accumulator 是该 authority 的已确认投影，不是唯一可恢复副本。

---

## 25. Source Power 聚合

多个 source 绑定同一 cell 的 `NodePowerAccumulator`：

```text
Pcell = Σ Pi
```

所以：

```text
100 generators
绑定同一个 AirCell
```

稳定 solve 仍然只读取该 accumulator 的：

```text
currentPowerW / pendingEnergyJ
```

一次。

Source change event：

```text
accumulator.currentPowerW += newContribution - oldContribution
```

---

## 26. Source 精确能量积分

即使 V1 采用统一 cadence，也必须保留 event-time-correct source accounting。任何 `POWER_SOURCE` event 必须携带 authoritative game tick；只允许同一 source、同一 tick 的 `first old / final new` coalesce，不同 tick 的变化不能丢失时间信息。

每个确实接收物理功率的 `NodePowerAccumulator`：

```text
currentPowerW
pendingEnergyJ
lastIntegralTick
```

Source 在 tick `t` 改变：

```java
pendingEnergyJ +=
    currentPowerW
    * (t - lastIntegralTick)
    / 20.0;

lastIntegralTick = t;

currentPowerW += newPowerW - oldPowerW;
```

Solver 推进到：

```text
targetTick
```

时：

```java
pendingEnergyJ +=
    currentPowerW
    * (targetTick - lastIntegralTick)
    / 20.0;

lastIntegralTick = targetTick;

enthalpyH += pendingEnergyJ;
pendingEnergyJ = 0;
```

核心 invariant：

```text
Source cumulative emitted-energy ledger
=
∫ P dt
```

在没有 declared/degraded loss 的正常物理路径上，进入 mesh/internal reservoir 的能量同样等于该积分；发生显式 loss 时必须按本节总账拆分，不能仍把“全部进入 mesh”写成 invariant。

不能让：

```text
worker什么时候处理event
```

改变 source 总能量。

这也保证以后 cadence 从：

```text
1 tick
```

变成：

```text
2 / 5 / 10 ticks
```

时不需要重新设计 source semantics。

Source 在 tick `t` 从 Node A rebind 到 Node B 时必须：

```text
integrate accumulator A to t
integrate accumulator B to t

A.currentPowerW -= contribution
B.currentPowerW += contribution
```

禁止只移动 current power 而不结算 rebind 前的能量。add/remove/move/on/off/profile reload 都走同一事件合同。

Queue resync 必须保持同一个总能量合同。`SourceResyncSnapshot` 至少包含：

```text
sourceId
sourceRevision
eventWatermark
cumulativeEmittedEnergyJ at snapshotTick
snapshotTick
currentPowerW
current port/binding state
retained per-binding energy segments since worker ACK, when available
```

Resync 不能把 `cumulativeEmittedEnergyJ - lastAppliedCumulativeEnergyJ` 直接注入当前或最终 binding；aggregate accumulator 可能已经积分过其中一部分，这样会双算。Worker 在检测到 source watermark gap 后停止越过 gap tick，并从最后 ACK 开始按 retained segment 顺序 replay：

```text
(startTick, endTick, signedEnergyJ, binding/port revision)
```

每段只向当时有效的 binding、internal reservoir 或 profile-declared sink 应用一次。`cumulativeEmittedEnergyJ` 只作为 replay 后 checksum，比较：

```text
last acknowledged cumulative
+ replayed segment energy
== snapshot cumulative at snapshotTick
```

如果多次 rebind 的 segment history 已超出有界保留容量，只有无法重建的具体 tick interval/能量进入显式：

```text
SOURCE_RESYNC_LOSS
```

并记录 source、tick interval、J 和原因。总账冻结为：

```text
cumulative emitted source energy
= mesh-applied energy
  + machine/internal reservoir delta
  + declared port/degraded losses
  + SOURCE_RESYNC_LOSS
```

`SOURCE_RESYNC_LOSS` 是恢复能力用尽后的可观察降级，不是允许常规丢能量；任何 state-only“最后值覆盖”或 cumulative-delta reinjection 实现都不满足本合同。冷源/负功率的 segment 与 loss 保持 signed J，不能取绝对值掩盖方向。

Normal event 与 resync 通过 watermark 去重：resync snapshot 覆盖 `eventWatermark` 及以前的全部 source event，worker 在 replay/checksum 成功后原子记录 `lastAppliedWatermark/lastAppliedCumulativeEnergyJ`；晚到或重放的旧 event 必须成为 no-op，不能再次积分。

---

## 27. Physical Source Profile

Source 本身经常是 solid。

Source mode 先冻结为：

```java
enum ThermalSourceMode {
    POWER_SOURCE,       // W, 进入 source ledger
    BOUNDARY,           // boundary condition
    IMPULSE,            // J
    ANALYTIC_CONTROL,   // 无局部能量账本
    LEGACY_CONTROL,
    BODY_DEVICE
}
```

`ANALYTIC_CONTROL`/`LEGACY_CONTROL` 不得直接驱动 `LOCAL_ENERGY_ACCOUNTED` 相变；只有 `POWER_SOURCE`、`BOUNDARY` 和 `IMPULSE` 进入物理 ownership 路径。

因此禁止：

```text
source position
→ containing AirCell
```

Source profile 必须明确 emission ports：

```java
ThermalSourceProfile {
    EmissionPort[] convectionPorts;
    EmissionPort[] contactPorts;

    RadiationOrigin[] radiationOrigins;

    double convectiveFraction;
    double contactFraction;
    double radiativeFraction;

    MissingPortPolicy missingPortPolicy;
}
```

原则：

```text
Pconv + Pcontact + Prad + Punmodeled
=
Ptotal
```

合法 `MissingPortPolicy` 只有：

```text
REDISTRIBUTE_TO_VALID_PORTS
INTERNAL_HEAT
EXPLICIT_LOSS
```

若 topology 因预算仍 unresolved，则使用可计量的 `DEGRADED_LOSS`，不能向不存在的 AirCell 注入或积累无界 energy debt。

---

## 28. Generator 示例

例如：

```text
Ptotal = 10 kW

Pconv = 7 kW
Pcontact = 1 kW
Prad = 2 kW
```

绑定：

```text
top / exhaust
→ nearby Air component

bottom
→ MaterialBoundary

radiation origin
→ RadiationService
```

如果 exhaust 被堵：

允许 profile 决定：

```text
redistribute to valid ports

或

machine internal reservoir

或

explicit unmodeled loss
```

禁止：

```text
往不存在的AirCell注入热量
```

---

## 29. Physical Source 自己拥有 Interest

如果 source profile 声明：

```text
worldTransport = true
```

并且包含：

```text
Pconvective
Pcontact
world material heating
```

则：

```text
PHYSICAL_SOURCE
```

自身就是 thermal interest。

不能要求：

```text
玩家必须在附近
```

才能模拟。

否则无人区域 Generator 永远不能：

```text
加热空气
融雪
影响机器
```

如果 mesh admission 因预算失败：

```text
source power
→ explicit degraded / unmodeled sink
```

禁止：

```text
无限积累pending energy
等以后mesh创建后一次性释放
```

成本合同必须写实：registry event 为 `O(1)`；cold admission 是 bounded geometry + mesh work；stable source 是 `O(0)` topology work，但不等于 `O(0)` solver work。

---

## 30. BoundaryOperator

只有特殊 thermal 关系才保存 operator。

V1 建议：

```java
enum BoundaryType {
    MATERIAL,
    GATE,
    FAR_FIELD,
    UNKNOWN_DEGRADED
}
```

后续必要时可以加入：

```text
SPECIAL_AIRFLOW
```

但不属于 V1 基础架构。

---

## 31. MaterialBoundary

固体默认不拥有 volumetric mesh。

V1 三类：

```text
STATELESS_CONDUCTANCE
CAPACITIVE_SURFACE
PHASE_RESERVOIR
```

---

## 32. 薄墙

V1 的 stateless thin-wall bridge 只允许跨越 **恰好一个已确认 barrier block**：

```text
Air A
  |
one confirmed barrier block / Gwall
  |
Air B
```

不创建：

```text
StoneNode
```

适合：

```text
profile 明确允许的单格墙
单格薄隔热结构
```

确认必须同时证明 barrier 两侧空气、材料 profile、接触面和依赖 footprint；Door/Trapdoor/FenceGate 不走这个 shortcut，而走第 `10..15` 节的 resolved geometry mutation。

两个或更多 block 厚度一律不建立跨墙 bridge：

```text
1 confirmed barrier block
→ optional STATELESS_CONDUCTANCE bridge

2+ barrier blocks
→ independent material surfaces / deep material model
```

这样避免把跨 Brick 的不同材料、墙体热容和拐角组合错误压成一个 `G`。

---

## 33. 厚固体

禁止：

```text
Air A
██████████████
Air B
```

直接简化成：

```text
Air A -- G -- Air B
```

厚材料应该是：

```text
Air
 |
Gsurface
 |
surface thermal state
 |
Gdeep
 |
deep material / Natural boundary
```

另一侧空间拥有自己独立的 material surface。

只有模型证明热穿透确实需要耦合另一侧时才允许建立耦合。

---

## 34. Capacitive Surface

需要墙体余热时：

```text
Air
 |
Gsurface
 |
Wall H/C
 |
Gdeep
 |
Deep Reservoir / Natural
```

不需要余热：

```text
只保存等效导纳
```

---

## 35. 大型材料降阶

大型：

```text
rock
ice
snow
ground
```

不铺 volume thermal mesh。

使用热穿透深度思想：

```text
δ ≈ sqrt(alpha × tau)
```

降阶为最多：

```text
Surface Pole
+
optional Deep Pole
```

这保持：

```text
材料热惯性
```

但避免：

```text
整座山
=
百万solid thermal nodes
```

---

## 36. Phase Reservoir 永远 Brick-local

V1 有状态相变：

```text
不得跨4³ MixedGeometryBrick
```

Ownership key 可以包含：

```text
brickKey
airComponentId
materialComponentId
materialProfileId
boundaryClass
```

即使一大片连续雪最终拥有：

```text
20个局部reservoir
```

也不合并成：

```text
SnowPatch
```

核心 invariant：

```text
material topology split / merge / mutation
<= 64 voxel local work
```

---

## 37. StateTransition Ownership

必须保留两种互斥 owner：

```text
AMBIENT_KINETIC

LOCAL_ENERGY_ACCOUNTED
```

### Ambient

天然世界远离 active mesh：

```text
native random tick

或

bounded ambient candidate sampling
```

处理。

不拥有局部 H。

### Local Energy Accounted

只有 mesh 真正接触到材料时：

```text
snow
ice
permafrost
special material
```

才创建 local phase state。

同一个 material interface 不能同时被两个 owner 修改。

`StateTransitionSystem` 必须把“热学 ownership”和“是否遵守 Minecraft random tick gamerule”分开。每个 transition profile 显式声明：

```java
enum TransitionMutationPolicy {
    RESPECT_RANDOM_TICK_SPEED,
    IGNORE_RANDOM_TICK_SPEED,
    SCRIPT_CONTROLLED
}
```

天然 ambient snow/ice 默认 `RESPECT_RANDOM_TICK_SPEED`。迁移期 Generator-driven local transition 可以为兼容旧玩法继续选择 `RESPECT_RANDOM_TICK_SPEED`；明确由机器能量驱动的新 profile 可以选择 `IGNORE_RANDOM_TICK_SPEED`。因此 `randomTickSpeed = 0` 不是 thermal core invariant，而是 profile/compatibility policy。

---

## 38. Local Phase State

逻辑：

```java
struct LocalPhaseState {
    int boundarySlot;

    double enthalpyH;

    short profileId;

    long candidateMask;

    int generation;

    long requestSequence;

    double reservedEnergy;
}
```

生产仍使用 primitive arena。

---

## 39. Phase Transition Request

每个 phase reservoir：

```text
最多一个 outstanding request
```

Worker：

```text
H 达到一个可见转换单位
→ reserve transition energy
→ 从local candidateMask选择位置
→ enqueue packed request
```

Main thread：

```text
validate chunk loaded
validate current BlockState
validate generation
validate interface
→ setBlockAndUpdate()
```

随后正常：

```text
BlockState mutation hook
→ Geometry delta
→ Brick rebuild
→ H remap
```

成功 / 失败通过 ack 返回 worker。

禁止：

```text
per-block timer
global phase queue
N pending requests per reservoir
```

---

## 40. Brick Rebuild 状态迁移

每个受影响 Brick 在 BlockState/FluidState mutation 后，先用与 geometry compiler 相同的保守分解建立 old/new air-volume overlap matrix。若旧 support 是 `8^3/16^3`，先做不改变总空气容量的 LOD split，把受影响局部状态隔离到 Brick，再进行 world-geometry remap。

### Pure LOD split / merge

只有 support 重新分区且总空气容量不变时，才使用：

```text
Hi = Hold * Ci / Cold
sum(Hi) = Hold
```

Merge：

```text
Hnew = sum(Hi)
Cnew = sum(Ci)
```

这是 LOD/state partition invariant，不是所有 Minecraft mutation 的 invariant。

### World geometry overlap

对每个 old region `i` 和 new region `j`，compiler 计算共同空气体积 `Vij`。重叠部分保持旧温度：

```text
Cij = cEffective * Vij
Hij = Cij * (Told_i - Tref)
```

旧空气中没有任何 new overlap 的容量是 `Cegress_i`，其 signed sensible energy 记入：

```text
GEOMETRY_EGRESS_J
= Cegress_i * (Told_i - Tref)
```

新增且没有 old overlap 的空气容量按以下确定顺序初始化：

```text
1. 与新增 region 已确认连通的 surviving-air face，按 opening conductance/area 加权
2. 没有 surviving neighbor 时使用 NaturalBackend.airTemperature(position, effectiveTick)
```

初始化产生的 signed energy 记入：

```text
GEOMETRY_INGRESS_J
= CnewOnly * (Tinit - Tref)
```

因此 world mutation 的总账是：

```text
sum(Hafter) - sum(Hbefore)
= GEOMETRY_INGRESS_J - GEOMETRY_EGRESS_J
+ visible phase/source/boundary exchange at the same effective tick
```

`air -> stone`、`stone/snow -> air`、Door 旋转和不同温度下的正/负 `H` 都走此合同。已经消费在 visible phase transition 中的 reserved energy 不能再次迁移。任何 residual 超过浮点容差都是 correctness failure，不能用 loss bucket 吞掉。

---

## 41. BlockState / FluidState Mutation 热路径

primary hook 不能先假定为 `LevelChunk#setBlockState`。Phase 0 的 candidate 是注入五参数低层方法：

```text
LevelChunkSection#setBlockState(int, int, int, BlockState, boolean)
at RETURN
```

四参数 overload 已委托到五参数方法，不能重复注入并双报。运行时维护 main-thread-owned：

```text
IdentityHashMap<LevelChunkSection, LoadedSectionOwner>
```

owner 保存 dimension、section world coordinate、chunk generation 和 active thermal/radiation interest。mapped section 的每次实际 old/new change 都转换为 world position；unmapped ProtoChunk/worldgen write 产生零 thermal work，等 LevelChunk load/admission 时以 snapshot 为准。Unload 必须先使 publication/transport generation 失效，再撤销 owner mapping。

若 mapped section 在非 main thread 被写入，hook 不读取 World、不运行 resolver，只原子 bump live revision 并设置 `FULL_GEOMETRY_RESYNC_REQUIRED`，由下一 main tick bounded resnapshot。直接改 `PalettedContainer` 的 writer 会绕过该 hook；冻结的常见路径中，已知 raw writer 必须调用显式 adapter，fingerprint 只保留给 GameTest/人工 debug，不进入 production 周期扫描。整个 `LevelChunkSection` identity 替换还会让 owner/fingerprint 留在旧对象上，必须显式 rebind owner 并对新 section 做 full resnapshot。热学读取依赖的 biome container 也需要独立 revision/resnapshot；block-state fingerprint 不能检测原始 biome data 替换。Phase 0a 不再枚举所有 enabled mod 作为 gate；未知第三方 bypass 在复现玩家报告后增加专用 adapter。

`/resetchunks` 调查结论保留为延期兼容说明，不属于 Phase 0a 或当前 production integration gate。该命令用 writable `ImposterProtoChunk` 包装同一个已加载 `LevelChunk`，原地重跑 BIOMES/NOISE 等阶段而不产生 unload/load lifecycle；以后若正式支持，必须一次性拒绝旧 chunk/Page publication、抑制逐方块 thermal delta，刷新后丢弃旧 compiled geometry、biome snapshot 和旧 thermal Page，仅在仍有 interest 时懒重建，绝不保留已经被刷新区块对应的旧热状态。`FastNoiseEngine` 的低层 raw block/biome 通知只作为 loaded-owner 诊断兜底；普通 unmapped worldgen 始终产生零 thermal work。

正常 main-thread path 已知：

```text
oldState
newState
oldFluidState
newFluidState
```

转换成 cached：

```text
oldResolvedSignatureId
newResolvedSignatureId
```

无 thermal relevance：

```text
return
```

同 tick 同位置多次 state/fluid 改变：

```text
first old
final new
```

coalesce。

对 contextual resolver，单点 mutation 还必须通过反向 `DependencyOffsetMask` 找到所有受影响 resolver center；这些 center 可以跨 Brick、ChunkSection 或 chunk。只允许读取 mask union closure 内已加载的 snapshot，不能为完成 batch 加载邻区块。

Create 不发布移动期 geometry event：assemble 的 world removal 与 disassemble 的 world placement 必须通过同一个五参数 section hook，移动实体本身按空气且不产生 delta。Vanilla moving piston、递归 `onRemove/onPlace` 写入、whole-section replacement，以及 Frosted Heart 已知 raw block/biome container bypass 必须进入 Phase 0/Phase F common-path coverage matrix；`/resetchunks` 仅在以后决定正式支持该管理命令时另加 operation-level adapter。

### 41.1 Geometry mutation 的保守时间语义

Topology mutation 不做昂贵 sub-tick global re-solve。冻结规则：

> 新 transport connection 不 retroactively 应用于 mutation 前的时间；可能已失效的旧 connection 不继续被信任。

Active Page 捕获 relevant mutation 时立即递增 live `geometryRevision`，并把受影响 coarse support、gate/interface 标为 dirty + unresolved/disabled。worker rebuild 完成前，该 interface 不产生 transport；新 topology 只从 rebuild 完成后的下一个 `SolveEpoch` 参与。这会暂时低估传热，但不会穿新墙或在门打开前提前传热。

### 41.2 Tick 末 mutation sealing 与 source rebind 顺序

Minecraft main thread 在 tick `t` 内可以连续改 BlockState、FluidState、门的上下两半和 source BlockEntity。运行时在 tick 末建立 immutable batch，并冻结以下逻辑顺序：

```text
1. settle old source binding to tick t and advance source-owned cumulative checksum once
2. coalesce + seal BlockState / FluidState mutations and dependency invalidations
3. resolve loaded-only geometry; invalidate/rebuild affected Brick/Page support
4. install new source binding and port state for time > t
5. solve only after the epoch's sealed input watermarks are applied
```

这些阶段可以由 main/worker 异步流水执行，但语义顺序不能颠倒。Source event、geometry batch 和 rebind 记录必须共享 `effectiveTick` 与可比较 watermark；同 tick Door 两半跨 Section、waterlogging 与 source move 只能作为一个 sealed epoch 被 publication 观察。

---

## 42. Active Page Mutation

如果当前 section 有 ThermalPage：

```text
geometryRevision++
```

然后：

```text
position
→ baseIndex
→ coverageRef
```

如果该位置此前属于：

```text
16³ / 8³ coarse AirCell
```

立即：

```text
invalidate coarse support
→ refine affected 4³ brick
```

例如：

```text
16³ ALL_AIR
```

通过 cheap proof 创建。

现在发生：

```text
air -> stone
```

若 changed state 是 `SELF_ONLY`，无需读另外 `63` 格：

```java
fill(resolvedSignatureId, AIR_SIGNATURE);
resolvedSignatureId[changedIndex] = newResolvedSignature;
```

然后只 local compile。

若 changed state 或相邻 center 使用 contextual resolver，则改为读取第 `10/41` 节定义的 dependency union closure，并只重编由反向 mask 命中的 Brick；不能套用上述 `SELF_ONLY` shortcut。

旧 parent H 按 child capacity 保守拆分：

```text
Hchild = Hparent * Cchild / Cparent
sum(Hchild) = Hparent
```

这一步只完成 coarse-to-Brick LOD split。随后 `air -> stone` 等容量变化仍必须执行第 40 节 overlap/ingress/egress 账本，不能把上述等式误当作 world mutation 的最终 `sum(H)`。

---

## 43. Mixed Brick Mutation

已经存在：

```text
resolvedSignatureId[64]
```

则：

```java
resolvedSignatureId[index] = newResolvedSignature;
generation++;
dirtyBrickMask |= brickBit;
```

contextual resolver 不允许只改一个 array slot；必须重算 dependency invalidation 得到的全部 center，并把它们涉及的 Brick/跨 Page interface 一并标记 dirty。`NEIGHBOR_26` 单点 mutation 的 union read closure 上限按 `5^3 = 125` 预算和测试。

同 tick 一个 Brick 改 20 格：

```text
20 × O(1) delta

+

1 × O(64) rebuild
```

Worker 不重新读取 World。

---

## 44. Inactive World Mutation

如果：

```text
无 ThermalPage
无 physical source
无 active transition
无 radiation witness
```

普通世界 BlockState / FluidState change：

```text
0 persistent thermal work
```

不能因为：

```text
snow
ice
rock
wall
lava
```

普通变化就建立 global thermal index。

---

## 45. Topology Guard Frontier

Thermal Mesh 不允许：

```text
先传播进未知区域
再以后修geometry
```

但也不能：

```text
提前扫描完整room/cave
```

所以采用：

> **Minimal Topology Guard**

任何即将发布的新 thermal transport face：

```text
紧邻 topology 必须已确定
```

只沿真正需要的 frontier 获取 geometry support。

不固定提前：

```text
2层
3层
完整section ring
```

---

## 46. Frontier Classification

```java
enum FrontierClass {
    MATERIAL,
    OPEN_AMBIENT,
    OPEN_CONTINUATION,
    UNRESOLVED
}
```

### `MATERIAL`

安装：

```text
MaterialBoundary
```

### `OPEN_AMBIENT`

只有可靠证据：

```text
loaded-only sky/opening evidence
已有 coarse geometry summary
局部多个开放方向
Natural environment classification
cheap outdoor proof
```

才允许：

```text
FarFieldOperator
```

除此以外，还必须存在与该 classification key 匹配、已通过 Phase E holdout gate 的 FarField profile。没有已批准 profile 时，即使局部看起来开阔也保持 `OPEN_CONTINUATION`，不能安装任意 `G∞`。`OPEN_AMBIENT` classification 不得为了证明“大型开放空间”执行大范围 World scan；证据不足就保持 `OPEN_CONTINUATION` 或 `UNRESOLVED`。

### `OPEN_CONTINUATION`

例如：

```text
洞穴
矿道
狭窄竖井
走廊
```

不能直接当无限 ambient。

如果 thermal error 仍然有意义：

```text
继续申请下一层geometry/mesh
```

### `UNRESOLVED`

没有充分 topology evidence。

禁止：

```text
Air-Air transport
```

也禁止：

```text
自动当Natural outdoor
```

---

## 47. `UNKNOWN_DEGRADED`

预算不足时如果 topology 暂时 unresolved，完全封闭：

```text
G = 0
```

可能导致大功率 source 局部无限升温。

所以允许安装：

```text
UNKNOWN_DEGRADED
```

含义：

> 服务器当前没有足够预算解析真实 topology，因此相关未解析功率显式流入 unmodeled sink。

它不声称物理守恒到世界。

目标：

```text
不穿墙
不虚假远距离加热
不无限累积source energy
budget恢复后可替换为真实topology
```

Query flags：

```text
DEGRADED_TOPOLOGY
```

Metrics 必须统计这类 operator。

---

## 48. FarField

`OPEN_AMBIENT` 不能简单使用：

```text
G = kAir A / d
```

冒充无限外界。

FarField 表示：

> 当前未解析无限环境的降阶热阻 / 热容量。

V1 candidate：

```text
STATIC_IMPEDANCE
```

形式：

```text
Q∞ = G∞ × (Tlocal - Tnatural)
```

其中：

```text
G∞
```

来自离线 reference simulation / gameplay calibration，而不是手写常数。

建议 key：

```text
cell level
opening class
opening area bucket
orientation
wind bucket
dimension/environment class
topology class
```

每个 key 还必须携带适用域与 signed error envelope。拟合数据和 holdout 数据严格分开；至少在未参与拟合的 open space、half-open space、cavern、tunnel exit、不同 wind/source power 下验证：

```text
local temperature trajectory error
gameplay threshold crossing-time error
signed boundary-energy error
phase received-power error
```

只有四项都在 Phase 0 冻结的 workload-specific tolerance 内，bucket 才能标为 `APPROVED_STATIC_IMPEDANCE`。没有可验证上界的 bucket 保持 `OPEN_CONTINUATION`；若基础 outdoor buckets 都无法通过，V1 Minecraft integration 停在 gate，不能用 degraded loss 宣称 FarField 已解决。

---

## 49. Optional FarField V2

只有 reference benchmark 证明 static impedance 的瞬态误差不可接受，并另开 V2 architecture work，才加入：

```text
ONE_POLE_IMPEDANCE
```

形式：

```text
Local Cell
    |
   G1
    |
   Cfar
    |
   G2
    |
 Natural
```

它不是 V1 前置条件。

---

## 50. Mesh Expansion 必须 Error-driven

绝对禁止：

```text
sourcePower > threshold
→ expand
```

因为稳定的：

```text
10 kW source
```

无论边界离多远，总要排出接近：

```text
10 kW
```

同样禁止：

```text
boundary total heat flow > threshold
→ expand
```

真正判断：

> 把外部继续压缩成 FarField，会不会让 gameplay observable 误差超过容差？

runtime 不自行猜误差公式。已批准 `FarFieldProfile` 为当前 key 提供由 holdout 校准的 conservative upper bound：

```text
predicted local ΔT error
receiver threshold error
phase received-power error
gradient error
topology uncertainty
```

其中 topology uncertainty 不是数值误差 bucket：只要 opening classification 本身未证明，就保持 `UNRESOLVED/OPEN_CONTINUATION`。对已证明 topology，runtime 用当前 `|Tlocal - Tnatural|`、opening/level/wind bucket、receiver/phase sensitivity 和 profile envelope 得到 observable error bound。

定义：

```text
REFINE_HIGH
COARSEN_LOW
```

并要求：

```text
REFINE_HIGH > COARSEN_LOW
```

形成 hysteresis。

Refine 还需要 minimum residency/cooldown 和 mesh admission；连续 `N` 次低于 `COARSEN_LOW` 才允许 coarsen。预算拒绝 refine 时必须保留 error estimate、query confidence/flag 和 metric，不能把“没预算”改写成“误差可接受”。

---

## 51. Solver V1 时间模型与 `SolveEpoch`

Dimension runtime 维护：

```java
record SolveEpoch(
    long previousTick,
    long targetTick,
    long epochId,
    long dimensionGeneration,
    long geometryWatermark,
    long sourceWatermark,
    long chunkWatermark,
    long profileWatermark,
    long transitionAckWatermark
) {}
```

Main thread 实际封存的是 immutable `SealedInputFrame(effectiveTick, dimensionGeneration, watermarkVector)`；`SolveEpoch` 引用一个或多个按 effective tick 排序的 frame cut。这样 source-old settle、geometry mutation、source-new binding 的跨 stream 顺序由同一 frame 冻结，而不是依赖不同 ring 恰好按墙钟先后到达。

第一原型 benchmark：

```text
1 tick
5 ticks
10 ticks
20 ticks
```

默认初始集成值冻结为：

```text
5 ticks
```

最终生产值由 benchmark 决定，但所有 active mesh 始终使用：

```text
同一个 global thermal cadence
```

每个 dimension 同时最多一个逻辑 in-flight `SolveEpoch`。Main thread 只更新“最新可封存 target + input watermarks”，不为 worker 已错过的 cadence 建立 epoch 队列。Worker 完成当前 epoch 后，才从最新 sealed target 建立下一个 epoch。

Phase 0/PR 3 还必须冻结：

```text
maxSolveDeltaTicks
```

它是 pair/boundary/phase kernels 已由 reference benchmark 验证的最大总时间间隔，不等同目标 cadence。tick 只在 solver 入口转换一次：

```text
dtSeconds = (targetTick - previousTick) / 20.0
```

任何 kernel 都不能接收 tick 单位或大于 `maxSolveDeltaTicks / 20.0` 的 `dtSeconds`。

V1 不允许：

```text
4³ = 1tick
8³ = 5tick
16³ = 20tick
```

---

## 52. Common Time Invariant

任何真正发生 thermal exchange 的两个 cell：

```text
必须属于同一 thermal interval
```

也就是两者的 `SolveEpoch.previousTick`、`targetTick` 与 `epochId` 一致。

禁止：

```text
Cell A state @ tick 100
Cell B state @ tick 80

→ direct exchange
```

Canonical spatial face ownership 只解决：

```text
空间重复
```

不能解决：

```text
时间重复
```

---

## 53. Pair Exchange Kernel

两个 finite-capacity nodes：

```text
A
B
```

定义：

```text
Ceq = Ca × Cb / (Ca + Cb)

lambda =
G × (1/Ca + 1/Cb)

ΔH =
Ceq
× -expm1(-lambda × dtSeconds)
× (Ta - Tb)
```

执行：

```text
Ha -= ΔH
Hb += ΔH
```

因此单 pair exchange：

```text
严格能量守恒
```

输入 owner 必须保证 finite `Ca > 0`、`Cb > 0`、`G >= 0`。profile reload 的非法值进入 observable unsupported fallback；若 runtime 仍发现 non-finite state，则该 operator 本 epoch 不执行、Page 标 `NUMERIC_DEGRADED`，并从 last-finite publication/Natural state 做 bounded recovery，不能让 NaN 扩散。对很大的 `lambda * dtSeconds`，exchange fraction 数值饱和为 `1`。

V1 只宣称“单 pair 解析精确、全 sweep 内部能量守恒”；多 interface 是 operator splitting。完整正序固定为：

```text
page key ascending
-> cell slot ascending
-> axis +X, +Y, +Z
-> canonical patch key ascending
```

逆序是上述序列的严格 reverse，physical patch owner 不变；相邻 epoch 交替正/逆。buoyancy `G` 在该 pair 被访问时从当前两端 pre-pair `H/C` 计算。reference solver 必须量化多面 cell、source + fixed boundary 和 traversal reversal 的 splitting error。

---

## 54. Boundary Kernel

固定温度边界：

```text
Tnext =
Tb
+
(Told - Tb)
× exp(-G × dtSeconds / C)
```

Source energy：

```text
ΔHsource
=
integrated source energy
```

V1 thermal step 推荐固定 deterministic order：

```text
1. finalize source integration
2. canonical Air-Air exchanges
3. gate/material/farfield/degraded boundaries
4. phase accounting
5. residual/sleep evaluation
6. query publication
```

Boundary/operator 同样只接收 `dtSeconds`。Reference solver 用于测量 operator-splitting error；pair 对称性、`C <= 0`、NaN、极大 `G*dtSeconds/C` 与解析 fixed-boundary trajectory 都是 required tests。

---

## 55. Active / Sleeping

满足：

```text
no non-zero physical source
no pending impulse
no active phase
temperature residual < epsilon
boundary prediction stable
topology generation unchanged
natural boundary bucket unchanged
no unresolved topology
持续 N epochs
```

可以：

```text
SLEEPING
```

Sleeping state 保留：

```text
H
C
topology
query projection
```

但不进入 thermal sweep。

以下事件 wake：

```text
source change
geometry change
gate change
phase state
natural bucket change
neighbor residual
```

V1 冻结：**含非零 `POWER_SOURCE` 的 Page 不进入 `SLEEPING`。** stable source 仍可做到零 topology work，但继续参与 admitted solve。跨 Page、state-dependent buoyancy、FarField 与 neighbor revision 的 fixed-point certificate 会重新引入分布式依赖状态；把正常稳态边界通量写成 `steadyLossEnergyJ` 也会破坏账本语义。因此 `STEADY_SOURCE_SLEEP` 整体移出 V1。后续只有 source-heavy benchmark 证明必要时，才可从“单 node + fixed boundary”独立 V2 prototype 重新论证。

---

## 56. V1 不实现 Partition Scheduler

优先使用：

```text
dense active page list
+
page-owned primitive cell-index span
+
cellFlags[cellSlot]
```

工程策略：

> 不接受无限 active thermal state 再让 solver backlog 生存，而是通过 admission hard cap 控制每 step 的工作量。

Admission 必须保证当前 admitted active state 的一个完整 `SolveEpoch` sweep 不超过配置的 operation/memory hard cap；这不是“必须在一个 cadence 的 wall-clock 时间内完成”的承诺。至少限制：

```text
maxActiveCells
maxImplicitFaceOps
maxBoundaryOps
maxBrickRebuilds
```

如果 refinement 会突破 hard cap：

```text
1. 不refine
2. 保持coarse representation
3. 使用FarField
4. 使用degraded approximation
5. 拒绝optional admission
```

不允许 solver 永久落后几十秒，也不允许：

```text
epoch 100 未完成
epoch 110 入队
epoch 120 再入队
```

若 worker 错过 cadence，但：

```text
latestSealedTarget - lastCompletedTarget <= maxSolveDeltaTicks
```

下一次可以直接选择 latest sealed target，仍然不创建追赶队列。

如果间隔超过 `maxSolveDeltaTicks`，禁止任意拉长统一 `dt`，进入显式 `TIME_DEGRADED` branch：

```text
1. 不把超限 dt 交给 pair/boundary/phase kernel
2. 只对 input history 完整、按 effective tick 连续的 prefix 执行 hard-cap 允许的 bounded substeps
3. 每个 substep dt <= maxSolveDeltaTicks，并在 frame cut 应用对应 source/geometry/profile event
4. 未覆盖 interval 不伪造 transport 或 phase exchange，分别累计 skippedTransportTicks/skippedPhaseTicks
5. 所有可归属 source segments 仍精确积分并进入当时有效 H/internal reservoir；worker 忙不是能量 sink
6. 只有真实 missing port、unresolved topology、admission refusal 或 source-history exhaustion
   才进入各自 profile-declared loss / SOURCE_RESYNC_LOSS
7. 应用 targetTick 的最终 state/revision，rebase dimension thermal clock 到 targetTick
8. publication 标记 TIME_DEGRADED，wake 相关 Page，并从下一正常 epoch 继续
```

如果某个 geometry/source gap 使连续 history 不完整，substep 在 gap 前停止；不得把 final topology retroactively 应用于 gap。`TIME_DEGRADED` 是可观察的 transport/phase fidelity loss，不是正常 fast-forward，也不是 source-loss bucket。它必须记录 skipped transport/phase ticks、执行 substep 数、sourceAppliedJ、sealed/applied watermarks、history-gap reason 和受影响 Page；不得用旧名称 `STALE_TIMESTEP` 掩盖不同语义。

---

## 57. Worker 模型

默认执行模型：

```text
bounded shared thermal executor
+ one serial logical owner/mailbox per dimension
+ at most one in-flight SolveEpoch per dimension
```

逻辑 owner 是该 dimension Thermal Mesh state 的：

```text
single writer
```

每个 dimension mailbox 使用：

```text
IDLE -> QUEUED -> RUNNING -> IDLE/QUEUED
```

CAS 状态机。Main thread 更新 latest sealed frame 后只尝试把 dimension ID 放入 bounded ready-dimension queue；queue 满时设置 sticky `DISPATCH_REOFFER_REQUIRED`，不阻塞、不创建另一个 task。Worker 完成后若发现更新 target 则重新 offer。调度器为 recovery 保留 quota，并对 oldest waiting dimension 做 round-robin/age promotion，防止主世界持续输入饿死其他维度或 resync。Unload 递增 dimension generation；旧 generation worker result 只能丢弃并完成资源回收，不能 publication。

这避免大量细粒度 lock，同时不会因为加载多个维度就永久创建同数量的 OS thread。不得直接使用带无界 work queue 的 `Executors.newFixedThreadPool` 作为“bounded executor”。thread count、ready queue capacity、全服并发 dimension 数和最大 wait age 由 benchmark/config 硬限制；同一 dimension 的 epoch 不并发执行。worker exception 使 publication 立即失效并保留 mailbox/sticky recovery，由 fallback 接管，不能丢掉 owner state。

---

## 58. Main Thread Responsibilities

仅 main thread：

```text
Minecraft World read
BlockState / FluidState change capture
loaded-only resolver snapshot 与 contextual resolution
initial 4³ geometry core + declared halo cold read
source BlockEntity lifecycle
phase BlockState mutation
radiation DDA
chunk lifecycle
tick-end input sealing / watermarks
SealedInputFrame construction
```

---

## 59. Worker Responsibilities

Worker：

```text
ComponentBrick compile
geometry summary rebuild
thermal cell split / merge
boundary compilation
source binding
source energy integration
thermal solve
phase H accounting
query projection build
memory reclamation
per-dimension applied-watermark publication
```

Worker 永远不得持有：

```text
Level
LevelChunk
BlockEntity
PalettedContainer
```

引用。

---

## 60. Main → Worker Event Transport

使用：

```text
double buffer
primitive ring buffer
primitive arrays
bounded ready-dimension queue
```

不要：

```text
每个事件new Runnable
每个block change创建task object
```

主要 batch：

```text
GeometryBuildResult
GeometryDeltaBatch
SourceEventBatch
ChunkLifecycleBatch
ProfileGenerationEvent
TransitionAckBatch
```

Geometry delta 同 tick：

```text
first old
final new
```

coalesce。

跨 stream 的 batch 不独立“碰巧有序”。Main thread 先形成一个 `SealedInputFrame`，其中保存 effective tick、dimension/chunk generation 和完整 watermark vector，再触发 mailbox；worker 按 frame cuts 执行 old source settle -> geometry/state -> new binding。frame 本身使用 primitive fixed storage 或 admitted resize，不按 tick 创建对象图。

### 60.1 Sequence、watermark 与 queue-specific recovery

每个 producer stream 使用单调递增 sequence；每个 sealed `SolveEpoch` 保存必须应用到的 watermark，worker completion/publication 回写实际 applied watermark。Ring capacity 只限制传输，不得成为 authoritative state 的唯一副本。

| Stream / queue | Overflow 后的 sticky recovery |
|---|---|
| Geometry delta | main 立即递增相关 Page live revision，并设置 Page-owned `FULL_GEOMETRY_RESYNC_REQUIRED`；旧 publication 走 baseline fallback。恢复任务从 Page sticky 集合直接发现，预算允许时重新 snapshot/resolve 受影响 Brick 与 dependency closure，成功后以新的 geometry watermark 清 flag。 |
| Source event | authoritative registry 设置 source-owned `SOURCE_RESYNC_REQUIRED`；发送包含 revision、event watermark、cumulative emitted energy、当前 power/ports/binding 以及可用 binding segments 的 resync snapshot。无法恢复空间归属的 J 进入 `SOURCE_RESYNC_LOSS`。 |
| Transition request | reservoir 保持 `reservedEnergy`、request sequence 与 `REQUEST_RETRY_REQUIRED`，以后按预算重试；只暂停该 reservoir，不暂停其他 transition。 |
| Transition ACK | main 保存 per-reservoir `(requestSequence, outcome)` sticky ACK，直到 worker applied watermark 确认；该 reservoir 不提交下一 mutation，其他 reservoir 可继续。 |
| Chunk lifecycle | dimension runtime 设置 chunk-owned resync state；unload 的 transport/publication invalidation 在 main thread 立即生效，不能等待 ring。 |
| Profile generation | generation 是 authority；worker 发现 watermark/generation gap 时对 admitted active owners 执行 bounded profile rebind/re-resolve，旧结果按 revision fallback；不得扫描全部 loaded world。 |
| Radiation invalidation | section revision 即恢复 authority；cache witness revision mismatch 后重 trace，不保留必达事件。 |

所有 overflow 都必须分别计数 queue、affected owners、recovery latency 和 loss；一个全局 `thermal.queue.overflows` 计数器不能替代上述状态机。Recovery budget 必须有 reserved minimum quota，并在 owner oldest age 超阈值时提升优先级；持续正常流量不能让 `FULL_GEOMETRY_RESYNC_REQUIRED`、source replay 或 transition ACK 永久饥饿。

---

## 61. Geometry Cold Read

首次需要 MixedBrick：

Main thread 读取：

```text
64 core BlockState + FluidState
+ resolver dependency halo 内去重后的 loaded-only snapshot
```

立即转换成：

```text
resolvedSignatureId[64]
```

交给 worker。

`SELF_ONLY` cold build 仍是 `64` 个 core position；完整 `NEIGHBOR_26` mask 的 `4^3` Brick union closure 上限是 `6^3 = 216` 个 position。单点 mutation 的受影响 center 重新解析 closure 上限是 `5^3 = 125`。这些是 resolver read budget，不是允许跨出声明 mask 扫描世界。

之后 active Brick block mutation 已经具有：

```text
oldResolvedSignature
newResolvedSignature
localIndex
```

所以只发送 delta。

Worker 不再读 world。

---

## 62. Query Publication

Query 不直接读取 worker 正在修改的 `H[]`。

Publication 分离为：

```text
PublishedGeometryPage
PublishedThermalBuffers
```

`PublishedGeometryPage` 只在 topology generation 改变时重建，保存：

```text
geometryRevision
coverageRef[64]
mixed local-region/component lookup
published cell mapping
```

热值使用每 Page 固定双缓冲，并由一个 publication envelope 同时封存 geometry 与 thermal identity：

```text
thermalBufferA
thermalBufferB

PublicationEnvelope
    lifecycleGeneration
    geometryRevision
    solveEpoch
    sampleTick
    publishedBufferIndex

volatile long publicationVersion
```

每份 thermal buffer 只保存 query 所需的 primitive projection：

```text
temperature
medium/flags
sampleTick
capturedGeometryRevision
```

仅有 `publishedBufferIndex` 不足以保证正确性：reader 读取 A 时，writer 可能先发布 B、再开始覆盖 A，形成 buffer ABA。V1 使用单调 `publicationVersion` seqlock；version 的奇数表示 writer 正在修改，偶数表示 envelope 稳定。

Worker publication：

```text
publicationVersion = next odd       // release
→ write inactive thermal buffer
→ write/swap PublishedGeometryPage when generation changed
→ write the complete PublicationEnvelope
→ publicationVersion = next even    // release; publication point
```

Query：

```text
v1 = acquire publicationVersion
→ if odd: bounded retry or fallback
→ read envelope + selected primitive buffer + geometry mapping
→ v2 = acquire publicationVersion
→ accept only when v1 == v2 && even
→ also require envelope lifecycleGeneration and geometryRevision to match live values
→ otherwise bounded retry once, then fallback
```

`geometryRevision`、thermal values、`SolveEpoch`、`sampleTick` 与 chunk/Page `lifecycleGeneration` 必须属于同一 envelope，不能分别发布。`publicationVersion` 使用 `long` 且只单调递增；arena slot 被释放后必须换 generation。若 version/generation 重用、溢出或 reader 检测到任何 ABA 可疑状态，结果一律拒绝并走 Page fallback，不能猜测 buffer 仍有效。

禁止每个 thermal step：

```text
new PublishedPageSnapshot()
new double[]
new Map
```

生产硬要求：thermal buffers 预分配、双缓冲、versioned；topology 不变时 `0 geometry publication allocation`，稳定 solve `0 publication allocation`。不得复制 signature、Brick topology、candidate masks 或 solver H/C arenas 到 query view。

---

## 63. Geometry Revision / Publication Correctness

这是强制 correctness invariant。

`PublishedGeometryPage` 与每个 thermal buffer 都必须保存：

```text
capturedGeometryRevision
```

Main thread 当前 Page 有：

```text
liveGeometryRevision
```

Baseline query 使用 mesh 结果前：

```java
if (capturedGeometryRevision != liveGeometryRevision) {
    return fallback;
}
```

原因：

```text
BlockState 或 FluidState 已经改变
但worker还没有完成对应mesh rebuild
```

时，旧 topology 不能继续被 query 当成有效世界状态。

Fallback：

```text
CachedLocalSurface
或
NaturalBackend
```

并可以返回：

```text
STALE_GEOMETRY
```

flag。

这是 V1 必须先实现和始终保留的 Page-wide correctness fallback。V1 **不实现** query-level dependency footprint reuse：只要 Page 的 live revision 与 publication envelope 不同，就拒绝该 Page 的 mesh 值。这样会在频繁 mutation 的 Page 上增加 fallback，但 correctness、retained memory 和 publication 并发合同都可直接证明。

细粒度 reuse 只保留为附录 E 的 V2 数据问题。以后若 benchmark 证明 Page fallback 是实际瓶颈，必须先证明 compiler 能完整发布 supporting Bricks、跨 Page interfaces、contextual resolver centers 与各自 revision，再经过独立 memory/correctness review；不得在 V1 手写“同 Brick 才相关”等近似规则。

---

## 64. Thermal Query

生产 hot-path query 使用 primitive 参数和调用方持有的可复用输出对象；不能依赖 HotSpot 逃逸分析消除 `record`、`Vec3` 或 `AABB` 分配：

```java
void sample(
    double x,
    double y,
    double z,
    double receiverMinX,
    double receiverMinY,
    double receiverMinZ,
    double receiverMaxX,
    double receiverMaxY,
    double receiverMaxZ,
    ThermalQueryPurpose purpose,
    int maxAgeTicks,
    SharedNaturalFrame naturalFrame,
    MutableThermalSample out
);
```

`MutableThermalSample` 至少包含：

```java
double airTemperatureC;
double radiantFluxWPerM2;
double surfaceFluxW;
ThermalMedium medium;
float confidence;
long sampleTick;
int flags;
```

`surfaceFluxW` 是 receiver 与 `CachedLocalSurface`/contact surface 的独立合成结果，不塞进空气温度或辐射通量。`record ThermalQuery` / immutable `ThermalSample` 只允许作为 cold/debug API wrapper，且不得被 crop、player body、Town 或 block tick 热路径调用。

`flags` 至少区分 `STALE_GEOMETRY`、`DEGRADED_TOPOLOGY`、`TIME_DEGRADED`、`RADIATION_BUDGET_LIMITED`、`NATURAL_CALLER_FRAME`、`NATURAL_MAIN_THREAD_FALLBACK` 和具体 fallback source；不能把不同降级原因压成一个模糊 stale bit。

---

## 65. Query Permission

Query purpose 明确决定是否具有 interest 权限。

```text
PLAYER_BODY
    默认 passive，只读取已有 publication/surface/natural

PLAYER_HIGH_ACCURACY
    admission-controlled、带 TTL 的显式 mesh lease

PHYSICAL_SOURCE
    可以产生mesh

REGISTERED_MACHINE
    默认 passive；只有 profile 明确声明的 stateful gameplay lease 可 admission

PASSIVE_BLOCK_TICK
    不创建mesh

PASSIVE_TRANSITION
    不创建mesh

REGION_AGGREGATE
    不创建mesh

DIAGNOSTIC
    debug budget controlled，可申请短 TTL mesh
```

因此玩家数量本身不等于 mesh creator 数量。普通 body/HUD 查询 miss 直接使用 surface/natural fallback；只有 `PLAYER_HIGH_ACCURACY`、显式 gameplay lease、`PHYSICAL_SOURCE` 或受预算 `DIAGNOSTIC` 能创建 mesh。

---

## 66. Query Hot Path

```text
primitive position/receiver bounds
→ acquire publicationVersion
→ sectionKey
→ primitive page lookup
```

如果已有 Page：

```text
stable seqlock envelope?
    no  → bounded retry once, then fallback

lifecycleGeneration + Page geometryRevision valid?
    yes → normal lookup
    no  → Page-wide fallback

baseIndex
→ coverageRef
→ query buffer value
→ second acquire publicationVersion
→ version changed? reject and fallback
```

Mixed Brick：

```text
local voxel
→ local component
→ thermal cell
```

Query compositor：

```text
Natural background
→ revision-valid SparseMesh air replacement, when present
→ AnalyticField combine mode
→ CachedLocalSurface receiver contribution
→ bounded RadiationService contribution, when requested
```

Natural background 优先从调用方提供的同 tick `SharedNaturalFrame` 读取。没有 frame 时，只允许在 main thread 执行有硬上界、不会加载 chunk 的 biome/climate fallback；off-thread caller 必须使用已捕获 frame 或纯配置 natural 值，绝不能等待 main thread。publication/surface hit 的硬合同是：

```text
0 world reads
0 allocations
0 worker waits
```

Natural fallback 不承诺 `0 world reads`，但必须记录 `NATURAL_MAIN_THREAD_FALLBACK`，且每次只执行 bounded lookup。所有路径都禁止 worker wait、同步 mesh build 和 chunk load；production benchmark 分别报告 publication hit、surface hit、caller-frame natural hit 与 main-thread natural fallback，不能用混合平均值掩盖 miss 成本。

Radiation witness cache hit 可以保持 `0 world reads`；revision mismatch 的 DDA retrace 是独立预算的 main-thread loaded-only read path，也必须与普通 thermal query hit 分开计量。

---

## 67. Radiation

Radiation 与空气 transport 完全独立：

```text
PhysicalSource
   │
   ├─ convection → Thermal Mesh
   ├─ contact    → MaterialBoundary
   └─ radiation  → RadiationService
```

V1 优先只实现玩家直接辐射。

每个有非零 radiation channel 的 `PhysicalSource` 在注册/移动/卸载时更新 packed section source spatial index。查询只枚举硬上限半径内的 section buckets，并对 candidate 先计算保守 flux upper bound：

```text
qUpperBound = directionalUpperBound * Prad / (4 * pi * max(rSquared, rMinSquared))
```

`r > maxRadiationRange` 或 `qUpperBound < minRadiantFlux` 的 source 不发 ray。其余 candidate 按 `qUpperBound` 降序、packed source key 稳定打破同值，受 `maxCandidatesPerReceiver` 与 `maxRaysPerReceiver` 双重 hard cap。达到任一 cap 时仍返回已累计的有界结果，同时设置 `RADIATION_BUDGET_LIMITED` 并降低 confidence；不得退化成 source-radius world scan。

---

## 68. Player Radiation

每个获准 candidate 对玩家使用少量 deterministic receiver rays：

```text
feet
torso
head
```

Source → Player：

```text
DDA LOS
```

DDA 只穿过已加载 section，并同时查询 vanilla block occlusion 与 section-indexed dynamic exclusion。遇到 unloaded、unresolved 或 budget exhaustion 时按保守遮挡处理并降低 confidence，不能主动加载 chunk，也不能把未知空间当透明。

墙阻挡：

```text
radiation contribution = 0
```

不需要把 radiation 放入空气 mesh。

`RadiationService.sampleFlux(...)` 是只读观察：无论 player/HUD/crop 调用多少次，都不能修改 source accumulator、扣减或再次注入 `Prad`。source ledger 每个时间区间只把 radiation channel 记一次 `RADIATION_EMITTED_J`；玩家体温系统按自身唯一 update cadence 对接收 flux 积分，重复观测不产生额外能量。V1 未命中的 radiation 进入 profile 声明的 ambient radiation sink，不由 query 次数决定。

---

## 69. Radiation Cache

Radiation cache 不依赖 `ThermalPage.geometryRevision`。主线程维护独立的 packed `SectionOcclusionRevision`，只为 radiation source/witness 涉及的 loaded sections 建 entry；任何会改变射线遮挡的 BlockState、FluidState、moving-piston 或 dynamic-exclusion mutation 都递增 revision。

每条 active ray witness 保存：

```text
receiver/source generation
经过的 section keys
每个 SectionOcclusionRevision
ray result / sampled flux
```

任意 revision mismatch：

```text
cache stale
→ retrace
```

Source move/remove、receiver bucket change、section unload/reload generation change 或任一 witness revision mismatch 都触发 bounded retrace。cache 只保留 active receiver 的 witness，不建立 radiation reverse graph；unload 立即使相关 witness 失效。

---

## 70. Material Radiation

不是 V1 必须功能。

以后如果 gameplay 明确要求：

```text
火直接辐射融雪
```

可以实现固定角 quadrature：

```text
Prad
→ N fixed weighted rays

Pi = wi × Prad

Σwi = 1
```

每 ray：

```text
first hit material
→ material energy

miss
→ ambient sink
```

但没有 benchmark / gameplay requirement 前不进入 V1。V1 的 player observation、source index、ray witness 和 ambient radiation sink 不能偷偷演化成 material energy deposition；材料辐射必须作为独立后续阶段重新定义 receiver ownership、能量积分和 budget。

---

## 71. Geothermal

地热不能写成：

```text
air temperature += f(Y)
```

只允许：

```text
Air
↔ exposed deep rock MaterialBoundary
```

Deep rock temperature：

```text
NaturalBackend.rockTemperature(...)
```

随后热量通过普通 Air-Air cell 自然向上传输。

狭窄竖井由：

```text
4³ ComponentBrick topology
```

自然保存。

不建立：

```text
ShaftLink
```

---

## 72. Crop

Crop 永远 passive：

```text
existing published mesh?
    yes → read

no
    → Natural
```

不能创建：

```text
Page
Brick
Cell
Interest
```

同 tick 大量作物可以使用：

```text
SharedQueryFrame
```

共享结果。

---

## 73. Town

Thermal 系统不得为了 Town：

```text
扫描建筑内部
扫描房间
逐voxel query
```

如果 Town gameplay 本来已经执行 BuildingScanner：

可以顺便生成：

```text
TownThermalProjection
```

把建筑内部压成少量：

```text
weighted groups
```

稳定 town refresh 只读取这些 groups。

Town：

```text
不能创建mesh
不能保持mesh lease
```

---

## 74. Machine

机器 thermal role：

```text
NONE
QUERY_ONLY
POWER_SOURCE
MACHINE_CAPACITY
BOUNDARY
```

普通机器：

```text
NONE
```

环境敏感：

```text
QUERY_ONLY
```

废热机器：

```text
POWER_SOURCE
```

自身需要过热：

```text
MACHINE_CAPACITY
```

Generator：

```text
POWER_SOURCE
```

不要让：

```text
machine count
=
thermal node count
```

---

## 75. Chunk Lifecycle

### Load

Main thread 为每次 chunk/section load 分配新的单调 `lifecycleGeneration`，注册 `LevelChunkSection` owner mapping，然后只恢复明确：

```text
physical source BlockEntities
```

不扫描：

```text
snow
ice
wall
rock
lava
air
```

### Unload

Unload 必须在 main thread 按以下顺序执行：

```text
1. bump/invalidate chunk + Page lifecycleGeneration
2. remove LevelChunkSection owner mappings
3. reject query publication immediately
4. settle every affected source binding to unload effectiveTick
5. seal source-remove + chunk-unload watermarks
6. disable cross-boundary transport and dynamic exclusions
7. invalidate SectionOcclusionRevision/radiation witnesses
8. release worker arenas only after generation-checked handoff
```

Worker 完成旧任务后，只有 task、live owner 与 publication envelope 的 `lifecycleGeneration` 全部相等才可提交；不相等则丢弃计算结果，绝不能让旧 chunk incarnation 覆盖 reload 后的新 Page。source settle 和 retained binding segments 必须先完成，unload 不能把尚未积分的 `P * dt` 静默丢掉。

未加载邻居不是：

```text
OPEN_AMBIENT
```

而是：

```text
UNRESOLVED / UNLOADED
```

thermal query 永远不能触发 chunk load。

V1 不持久化 transient local `H`，但 unload 也不是无账本删除：相对 Natural/deep boundary reference 的 signed residual 进入 `EVICTION_ENVIRONMENT_EXCHANGE_J(reason=CHUNK_UNLOAD)`。outstanding phase request 由 generation mismatch 拒绝并归还 reservation，随后 phase/material residual 同样结算到该 ledger。此路径必须与普通 cache reclaim 分开统计。

---

## 76. Memory Layout

禁止：

```text
one object per Cell
one object per Boundary
one object per Brick component

BlockPos hashmap
nested HashMap
ArrayList per Cell
persistent generic edge objects
```

使用：

```text
primitive page table
packed long coordinates
SoA arenas
fixed spans
bitsets
primitive ring buffers
```

例如：

```java
double[] cellH;
double[] cellC;

byte[] cellLevel;
byte[] cellFlags;

int[] cellPage;
int[] cellSupport;

double[] powerCurrentW;
double[] powerPendingEnergyJ;
long[] powerLastIntegralTick;
```

`power*` 属于独立 `NodePowerAccumulatorArena`，只为实际接收 `POWER_SOURCE` 的 node 分配；它们不是按 `ThermalCell` 一一分配的字段。

Boundary 同样使用 tagged SoA。

所有 arena、registry、mailbox、source history、radiation index/witness 与 publication buffer 都同时计入：

```text
server-global ThermalMemoryBudget
+ owning dimension ThermalMemoryBudget
```

per-dimension `16..32 MiB` 只允许作为 workload-specific 初始候选，不能替代 server-global cap。任何 resize 必须先 reserve 新 backing storage、切换 generation、再释放旧 storage；峰值双持同样计入 admission。

---

## 77. 独立 Work Budgets

至少拥有：

```text
geometryColdReadBudget
geometryDeltaBudget
brickCompileBudget

meshAdmissionBudget
meshRefineBudget

thermalSolveMaxCells
thermalSolveMaxFaceOps
thermalSolveMaxBoundaryOps

radiationRayBudget
radiationCandidateBudget

phaseCandidateBudget
phaseMutationBudget

ambientCandidateBudget
ambientMutationBudget

publishBudgetBytes

dimensionMailboxCapacity
dimensionMemoryBudgetBytes
serverMemoryBudgetBytes
```

这些必须独立统计。

不能只设一个：

```text
thermalBudget
```

然后不知道成本来自哪里。

---

## 78. Memory Admission

任何：

```text
new Page
new Brick
refinement
source registry entry / binding history
dimension mailbox / ready-queue growth
phase reservoir
boundary state
SectionOcclusionRevision / radiation witness
query publication growth
```

必须经过：

```text
server-global ThermalMemoryBudget
+ dimension ThermalMemoryBudget
```

压力回收顺序：

```text
1. reclaim recomputable disposable cache
2. coarsen optional refinement with exact pure-LOD ΣH transfer
3. reclaim no-interest state proven within equilibrium eviction epsilon
4. reject optional interest / optional publication growth
5. Natural / surface fallback
```

不能把有意义的非平衡 `H` 当普通 cache 静默回收。若在 hard cap 下必须移除无 interest 的非平衡空气状态，先计算相对当前 Natural/frontier reference 的 signed residual，并记入 `EVICTION_ENVIRONMENT_EXCHANGE_J` 后再释放。该事件有 reason、Page key、generation 和 joule value；正负方向都保留，不能只累计绝对“loss”。

以下状态不参与普通 cache eviction：

```text
active source registry / NodePowerAccumulator
outstanding source rebind history
active or reserved phase state
stateful material reservoir
pending transition request/ack ownership
```

它们只能通过正常 source removal、phase completion、chunk unload 或 profile reload 的 generation-checked lifecycle 结算。critical source/queue/recovery storage 使用独立 reserved quota，不能被 optional refinement/publication 占满。

Physical source 无法 admission：

```text
registry/ledger critical reserve first
→ mesh admission refused if necessary
→ profile-declared external environment exchange
→ exact source interval remains observable
```

不能产生无限 energy debt。

---

## 79. Overload Policy

系统优先级：

```text
TPS / memory safety
>
simulation fidelity
```

预算不足允许：

```text
停止optional lookahead
停止refinement
维持coarse cell
使用FarField
使用UNKNOWN_DEGRADED
使用旧且 revision-valid 的 query publication
Natural fallback
回收已证明 equilibrium 的 sleeping state
对获准 eviction 的非平衡空气记 signed environment exchange
拒绝optional admission
```

Queue/recovery 预算必须给 sticky full-resync 预留 quota，并对各 dimension/queue 的 oldest age 做提升；持续正常流量不能永久压住 geometry/source/transition recovery。bounded dimension mailbox 的 `IDLE/QUEUED/RUNNING` re-offer 不能因 ready queue 一次满而丢失，且 server-global scheduler 必须轮转 dimension，避免主世界长期饿死下界或末地。

绝对不允许：

```text
突破主线程budget
无限queue backlog
OOM
强制load chunk
穿墙
使用未经验证的topology
无限source energy debt
静默丢弃非平衡 H
旧 lifecycleGeneration publication 覆盖 reload 后状态
持续 overflow 使 recovery 永久饥饿
```

---

## 80. 推荐 Package Structure

```text
thermal/

  api/
    ThermalQueries
    MutableThermalSample
    ThermalQueryDebugView
    ThermalQueryPurpose
    ThermalMedium

  profile/
    ResolvedThermalSignature
    ThermalSignatureRegistry
    ThermalSignatureResolver
    DependencyOffsetMask
    LocalAirRegionPattern
    ThermalMaterialProfile
    ThermalSourceProfile
    LocalTransitionProfile

  natural/
    NaturalBackend

  analytic/
    AnalyticFieldBackend

  geometry/
    GeometryDelta
    GeometryDeltaCoalescer
    GeometryResolverSnapshot
    GeometryBuildResult
    GeometryResyncState
    MixedGeometryBrickArena
    GeometryBrickCompiler
    GeometrySummary
    SectionGeometryRevision

  mesh/
    ThermalPageTable
    ThermalPageArena
    ThermalCellArena
    BoundaryOperatorArena
    GateOperator
    CoverageLookup
    ThermalMeshCompiler
    MeshRefinementPolicy
    FarFieldProfileRegistry

  source/
    ThermalSourceRegistry
    SourceEnergyLedger
    SourceResyncSnapshot
    SourceBinding
    NodePowerAccumulatorArena

  solver/
    SolveEpoch
    DimensionEpochCoordinator
    ThermalExecutor
    TimeDegradedPolicy
    ThermalSolver
    PairExchangeKernel
    BoundaryKernel
    BuoyancyMixingModel

  radiation/
    RadiationService
    RadiationSourceIndex
    SectionOcclusionRevisionTable
    RadiationCache

  transition/
    StateTransitionSystem
    LocalPhaseArena
    TransitionMutationPolicy
    TransitionRequestRing
    TransitionAckRing
    AmbientTransitionSampler

  query/
    QueryPublication
    PublicationEnvelope
    PublishedGeometryPage
    PublishedThermalBuffers
    SharedQueryFrame

  runtime/
    DimensionThermalRuntime
    DimensionThermalMailbox
    ThermalMemoryBudget
    ThermalWorkBudget
    EnvironmentExchangeLedger
    GeometryDeltaRing
    SourceEventRing
    StickyResyncRegistry
    ChunkLifecycleRing
    ProfileEventRing

  consumer/
    PlayerThermalAdapter
    PassiveBlockThermalAdapter
    MachineThermalAdapter
    TownThermalProjection

  compat/
    WorldTemperatureCompat
    BlockTempCompat
    GeneratorCompat

  benchmark/
    ReferenceFiniteVolumeSolver
    ThermalScenarioHarness
```

类名可以根据项目现状调整。

职责边界不能随意改变。

---

## 81. V1 NOT in scope（明确不实现）

以下东西在 V1 implementation PR 中出现，应默认视为需要 architecture review：

```text
Room
RoomScanner for thermal

CaveZone
LeakyCave
CorridorLink
ShaftLink

IslandRuntime
PartitionRuntime
PortalEdge

global ThermalOctree
global GeometryOctree

32³+ global thermal LOD

global 1³ / 2³ AMR

global MaterialPatch
SnowPatch graph

source-radius world scan

source→receiver graph traversal

per-block thermal state

per-edge Java object

per-cell Java object

pressure airflow solver

full CFD

multi-rate timestep

per-edge thermal clock

thermal-driven chunk loading

worker-side World / arbitrary VoxelShape callback

moving contraption/entity/BlockEntity-driven thermal topology

query dependency-footprint publication reuse

material radiation energy deposition

one permanent OS thread per dimension

unbounded stretched solver dt

non-zero source Page sleep
```

如果工程实现觉得某功能“必须增加一个新的 room/cave/region 对象”，首先重新检查是否可以由：

```text
4³ local topology
+
adjacent faces
+
ThermalCell
+
BoundaryOperator
```

表达。

---

## 82. 实施阶段

### Phase 0 — Contract Freeze + Existing System Baseline

Phase 0 分成两个都必须通过的子门；`0a` 只验证 Minecraft mutation/lifecycle 事实，`0b` 冻结数值、reference 与 workload acceptance。二者可以并行取数，但在两个 gate 都通过前不得开始 production integration。

#### Phase 0 实施快照 — 2026-08-24

| 子门 | 状态 | 已有可执行证据 | 尚未通过的 gate |
|---|---|---|---|
| `0a` mutation/lifecycle | `complete (common-path scope)` | GameTest-only 五参数 section hook、loaded-section owner map、generation/revision/publication token、tick coalesce/watermark、off-thread/raw-bypass sticky resync；`DebugCommand restore_backup` whole-section rebind、raw block/biome distinct resnapshot reason、section/generation/requiredRevision-bound `ResyncToken` ACK 与 replacement-identity unload cleanup 已实现；真实 ticket load/unload/reload 与 Create assemble `block -> air`、移动期零 delta、disassemble `air -> block` 已通过；`7` 条 Phase 0a GameTest 与 `16` 条 writer/adapter JUnit 已通过 | 无；21-runtime 穷举调查、dynamic exclusion prototype 和旧断言保留为非执行注释，未知第三方 bypass 按玩家报告补 adapter；`/resetchunks` 是延期管理命令兼容项，不是 gate |
| `0b` units/reference/workloads | `partial` | Java 17 reference contracts、`28` 条 JUnit、`14` 个 workload descriptors；覆盖 tick-to-second、`H=C(T-Tref)`、`integral(P dt)`、解析 pair/boundary、pure-LOD 与 geometry ingress/egress、typical/stress 判定、历史 routing，以及 benchmark evidence provenance；另有已删除区块热场的首轮本机 JMH/JFR、allocation 与隔离 retained-object-graph 迁移基线 | 生产模组列表中的 1/10/50/100 玩家新 runtime server baseline、玩家采样 main/worker 分位数、整服 retained heap、作物/城镇/forced-random-tick/网络调用量、真实 workload threshold |

`0a` 已按冻结的常见路径范围通过，`0b` 仍是 `partial`。本段是 Phase 0 当时的实施快照，不描述当前 gameplay authority；当前接线与删除结果以本文 Outcome 和 living docs 为准。`PhaseZeroThermalRouting` 与 benchmark evidence matrix 只能验证历史决策/证据格式，不能被引用为 runtime gate。

#### Phase 0a — Mutation / Lifecycle GameTest Spike

先把 GameTest run 的 namespace 配置从当前单一 `frostedresearch` 扩为：

```groovy
property 'forge.enabledGameTestNamespaces', 'frostedresearch,frostedheart'
```

只做最小 hook/adapter spike：

```text
candidate @Inject RETURN:
LevelChunkSection#setBlockState(int, int, int, BlockState, boolean)

main-thread owner map:
IdentityHashMap<LevelChunkSection, LoadedSectionOwner>

mapped off-thread write:
sticky FULL_GEOMETRY_RESYNC_REQUIRED

unmapped worldgen write:
zero thermal work

raw palette bypass:
adapter or active-page fingerprint/debug probe
→ sticky full resync, never silent acceptance
```

四参数 overload 若只委托五参数版本，不重复注入。`LevelChunk#setBlockState` 可以作为调用路径观测点，但不能作为唯一 authoritative hook 冻结。Phase 0a 的 GameTest matrix 至少覆盖：

```text
setBlockAndUpdate
direct LevelChunk write
direct mapped LevelChunkSection write
water flow + waterlogged BlockState/FluidState delta
Door two halves across y=15/16 with one sealed watermark
Trapdoor + FenceGate state changes
moving piston extend/retract
Create assemble: payload `block -> air`
Create movement: no thermal geometry delta; moving entity is air
Create disassemble: destination `air -> block`
recursive onRemove/onPlace mutation
unmapped worldgen section write
unload followed by stale old section identity write/publication
raw PalettedContainer bypass detection and sticky recovery
raw block/biome container notifier with distinct sticky reason
whole-section identity replacement owner rebind + unload cleanup
stale R1 resync ACK cannot clear a newer R2 requirement
```

每条测试都必须断言 delta 次数、owner/generation、effective tick、watermark、是否允许 worker publication，以及“无主动 chunk load”。冻结 common-path 范围内的 raw bypass 必须由低层 hook 或显式 adapter 捕获；active-page fingerprint 只用于 GameTest/人工 debug。未知第三方 writer 不做预防性穷举，出现可复现玩家报告后再补专用 adapter。

#### Phase 0b — Units / Reference / Workload Acceptance

##### 2026-08-24 已删除区块热场的历史微基线（非 acceptance completion）

第一轮可重复证据曾落地到旧 JMH 类和 `thermalLegacyBaseline` 任务；该系统及专属 benchmark 现已删除。环境清单为 Windows 11、Oracle JDK `17.0.12`、G1、`-Xms2G -Xmx2G`、24 logical processors、`16,898,166,784 B` physical memory；该进程使用 JMH main runtime classpath，不等价于生产 modpack server。以下数字只保留为迁移历史，不能重建、不能作为当前性能结果。

已删除 `ChunkHeatData.queryAdjust` 的历史结果为：

| adjusters | hit average | miss average | `gc.alloc.rate.norm` | isolated retained graph |
|---:|---:|---:|---:|---:|
| `0` | `0.606 +/- 0.005 ns/op`（empty fast path） | 同一路径 | `0 B/op` | `72 B` |
| `1` | `3.177 +/- 0.036 ns/op` | `2.684 +/- 0.251 ns/op` | `0 B/op` | `240 B` |
| `10` | `21.134 +/- 0.463 ns/op` | `13.948 +/- 0.756 ns/op` | `0 B/op` | `1,032 B` |
| `100` | `198.132 +/- 3.970 ns/op` | `135.606 +/- 14.312 ns/op` | `0 B/op` | `9,912 B` |

解释边界：查询仍是 `O(adjusters in chunk)` 线性扫描；当前调用立即消费 `HeatQueryResult` 字段，因此 HotSpot 能标量替换 record，JMH 测量期没有 per-op allocation 或 GC。retained 值是孤立 fixture 的 JOL object-graph 估算，不含 class metadata/shared statics，也不能直接乘成整服内存，因为一个世界热源会复制进多个覆盖区块。15 秒诊断 JFR（约 `1.32 MB`）包含 `737` 个 execution samples；其整段 GC 还包括 Minecraft bootstrap 和 JMH 准备工作，不能归因给查询，查询测量窗口以 JMH GC profiler 的 `0 B/op`/`0` GC 为准。

同轮 harness calibration 测得解析 fixed-boundary/pair exchange 为 `14.18/16.37 ns/op`、`0 B/op`；synthetic `4^3` Brick 的 all-air/solid-wall compile 为 `35.58/26.86 us/op`、约 `7,688/6,120 B/op`。Brick 结果只有单 fork 且置信区间较宽，只证明 harness 可执行并暴露 allocation，不作为 Phase A 或 V1 性能门槛。

这轮数据只能保留为已删除系统的历史迁移基线，不能生成当前 measured pass/fail。test-source `ThermalBenchmarkEvidence` 只验证证据格式，不代表真实 benchmark report；Phase 0b 继续保持 `partial`。

在写新 runtime 前锁定：

```text
T unit
H unit
C unit
P unit
G unit

source ∫Pdt invariant
pure-LOD split/merge H invariant
geometry-volume ingress/egress ledger invariant
query fallback semantics
geometry revision semantics
phase ownership semantics
BlockState + FluidState resolver semantics
dependency-offset mask / unresolved semantics
queue sequence, watermark and resync semantics
maxSolveDeltaTicks / TIME_DEGRADED semantics
publicationVersion / lifecycleGeneration semantics
geometry ingress/egress/environment-exchange ledgers
```

并测旧系统：

```text
main thread ms
worker ms
allocations/tick
retained bytes
query count
world reads
source update cost
passive query calls / lookup misses / ticking chunks
workload-specific 1/10/50/100-player scenarios
```

这是后续所有优化比较的 baseline。Phase 0b 必须在固定硬件、JVM、模组列表、视距、simulation distance 与配置上冻结附录 C 的 acceptance table，而不是冻结一个脱离场景的总 MiB 或平均 tick 数：

```text
typical workloads:
    SOURCE_RESYNC_LOSS = 0 J
    source admission/external-loss interval = 0
    TIME_DEGRADED count = 0
    skippedTransportTicks = 0
    skippedPhaseTicks = 0

stress workloads:
    hard caps remain effective
    degradation is bounded, attributed and observable
    sticky recovery eventually converges
    source energy is still applied exactly to an effective H or declared sink
```

Phase 0 原计划同时执行附录 A、B、C，复测当时的粒子采样、区块热场、作物/城镇查询、forced random-ticking sections、网络同步、CPU、retained heap、allocation/GC 和多人毕业基地。该历史前置条件已经被后续生产接线取代；当前只继续测新 runtime，不恢复已删除系统或其回退语义。

---

### Phase A — Synthetic Geometry Kernel + Forge Resolver Census

#### Phase A 实施快照 — 2026-08-24

状态为 `complete (PR 1 / PR 2 correctness prototype)`。除 conservative `4x4` geometry/Brick kernel、`int` IDs、bounded loaded-only snapshot 和 generic state-static resolver 外，现已增加 immutable `ThermalSignatureResolverDispatcher`：moving piston hard exclusion 后按 explicit override、自动 static fallback、registered contextual、observable unsupported 的固定顺序分派；注册时冻结 canonical resolver ID、`DependencyOffsetMask` 和 `maxOutputRegions`，拒绝重复 binding/ID。explicit 路径使用 non-neutral material/contact/radiation fixture，contextual 路径使用一个只读 `SELF + EAST` 且始终保守闭合的 synthetic dynamic fixture；它们证明 registration/dispatch 合同，不是最终 gameplay 物理参数或逐模组兼容层。

2026-08-24 最终启用 registry census 枚举 `2,392` 个 Block、`84,147` 个 BlockState：`82,197` 个 state 走 generic static、`1` 个走 explicit、`12` 个走 contextual，合计 `82,210` resolved；`1,925` 个未注册 dynamic state 为 `NOT_REGISTERED`，`12` 个 moving-piston state 为 `UNRESOLVED_DYNAMIC`。结果为 `262` 个完整 resolved signatures、`259` 个唯一 geometry patterns、`2` 个 contextual outputs，最大观测 local-air-region count 仍为 `4`。首轮 full-dispatch census 为 `480,784,500 ns`；同输入 reload pass 为 `219,870,300 ns`，旧/新 registry 同时保留 `524` 个 signature slots 且 ID 完全确定。报告位于 `build/reports/thermal-phase-a/resolver-census.json`。

Java 17 thermal JUnit 为 `89/89`，Forge GameTest 为 `15/15` required。JMH sample 的 resolver p95/p99 为 explicit `100/100 ns`、contextual `100/100 ns`、generic air `1.5/1.9 us`、generic fence `1.6/2.0 us`；对应 average allocation 约 `304/464/1,168/1,448 B/op`。Brick compile p95/p99 为 all-air `24.58/28.48 us`、solid-wall `19.39/23.68 us`、split-regions `15.09/20.00 us`，allocation 约 `7,694/6,126/14,476 B/op`。JOL 的 correctness layout 为单代 `262`-signature registry `41,000 B`、双代 overlap `79,936 B`；all-air/solid-wall/split-regions Brick retained size 为 `2,280/1,960/3,816 B`。这些是一次性 resolve/compile/reload 路径和 object-heavy correctness prototype 的本机证据，不是每 tick steady-state 或整服 retained heap，也不能据此冻结 production packing。

真实 GameTest 继续覆盖 air、solid、slab、stairs、Door、Trapdoor、fence、pane、snow layer、waterlogged partial、static piston base/head、moving piston、完整 `NEIGHBOR_26`、越界/missing sentinel 和远端 unloaded dependency；远端 capture 前后均未加载 chunk。pure-Java closure tests 固定 `27` affected centers、`5^3 = 125` mutation read closure 与 `6^3 = 216` cold-Brick halo，registry 测试证明 correctness ID 可超过 `65,535`。production `Rmax` 和窄 packing 仍不按观测值 `4` 冻结；真正 gameplay material/source profiles 属于 PR 9/10，datapack listener 和 world/runtime wiring 属于 PR 8，不反向阻塞 Phase A prototype 的完成，也不得提前启用 production thermal authority。

先实现不依赖 Minecraft `World` 的 synthetic kernel：

```text
SyntheticShape/AABB input
conservative 4x4 face-aperture raster
bounded complement-component resolver
MixedGeometryBrick
64 Block core / 64 * Rmax atom component compiler
flattened primitive atom spans
face port compiler
non-topological GateOperator fixture
int correctness IDs throughout
```

随后用只在 main thread 运行的 Forge census adapter 枚举实际启用的 `BlockState + FluidState`，验证三类合法 resolver：explicit thermal profile/override、generic state-static resolver、declared dependency-mask contextual resolver。generic state-static 路径统一接受 `hasDynamicShape() == false` 的状态，不做逐模组兼容审查。记录 state 数、signature 数、context output 数、最大 local region 数、unsupported 数和 datapack reload 峰值；在 census 通过前不得把 ID/region count 窄化为 `short`/`byte`。

Fixtures：

```text
full air
full solid
wall
door
trapdoor
two-half door across Brick/Section
slab
stairs
fence
pane
pane/fence local-region separation
synthetic waterlogged partial shapes
synthetic SELF_ONLY / NEIGHBOR_6 / NEIGHBOR_26 closure
Forge unloaded dependency boundary → UNRESOLVED without chunk load
Forge moving piston/BlockEntity dynamic exclusion classification
Create moving contraption ignored-as-air classification
1×1 shaft
2×2 tunnel
snow layer
partial opening
```

记录：

```text
ns / brick compile
bytes / brick
allocation / op
local-region count histogram and unsupported signatures
unique resolver reads / dependency closure
```

Geometry：

```text
conservative raster 不能出现 false opening
4x4 aperture tile 只有整 tile 可证明为空且连通时才能开放
Door/Trapdoor/FenceGate topology change → Brick rebuild
GateOperator 只改变已确认端口的 G
```

---

### Phase B — Page + Coverage + Coarse/Fine Ownership

实现：

```text
ThermalPage
int coverageRef[64]
geometry summaries
mixedBrickMask
geometryRevision
dense active page/cell spans; no fixed 128-cell mask
Page-owned FULL_GEOMETRY_RESYNC_REQUIRED
FacePatchIterator with negative-axis ownership
geometry ingress/egress ledgers
```

必须通过：

```text
16³ ALL_AIR
→ place stone
→ coarse support立即失效
→ materialize一个4³ Brick
```

以及：

```text
同tick同brick修改20格
→ 一个rebuild
```

以及：

```text
NEIGHBOR_26 单点 mutation
→ 最多 5³ union resolver read closure

GeometryDeltaRing overflow
→ live revision 立即失效
→ sticky full resync 最终收敛

16↔16, 16↔4, 8↔4 and direction mirrors
→ canonical patch key exactly once
→ A = overlap area
→ d = halfWidthA + halfWidthB

pure LOD split/merge
→ ΣH exact

air-volume-changing rebuild
→ overlap temperature transfer
→ GEOMETRY_INGRESS_J / GEOMETRY_EGRESS_J / residual explicit
```

---

### Phase C — Air Mesh

#### Phase C 实施快照 — 2026-08-24

状态为 `complete (regular + mixed Air Mesh correctness foundation)`。`ThermalCellArena` 已使用可增长 primitive SoA 保存规则 4/8/16 AirCell 与 compiled mixed-Brick component 的 `H/C`、Page slot、lifecycle generation、world support、medium 与 flags；`H` 是唯一动态 authority，`T = Tref + H/C`。mixed component 直接复用 `ComponentBrickCompiler.CompiledBrick` 的 volume、centroid 与 face ports，不建立第二套 geometry model。

Pure-LOD split/merge 现在由 arena 拥有完整 Page dense-span replacement：先预分配新 span，使用 production-owned `GeometryMigrationLedger` 按容量 split 或求和 merge，旧、新 span 在 coverage handoff 期间同时存活；只有 `ThermalPage` 已安装完整新 coverage/cell span 后才能 commit 释放旧 span，失败则 rollback。`ThermalMigrationReference` 反向委托给 ledger，不再形成 `mesh -> phase0.reference` 依赖。

`ThermalPage.tryQueryPublishedCoverage` 已提供 caller-owned mutable result 的 O(1) `baseIndex -> coverageRef/width` 查询；live/published revision、topology 或 dirty 状态不一致时清空结果并要求 Page-wide fallback。`ImplicitAirAdjacency.compileOwnedPairs` 从 world negative-axis support 枚举 `+X/+Y/+Z`：规则面计算精确 overlap，mixed 面直接相交 compiled aperture mask，并按 cell pair 聚合 primitive `ThermalSweep.PairOperation`。`CoverageCellResolver` 已删除；regular/mixed fractional aperture、mixed/mixed overlap/closed aperture、跨 Page `16↔4`、Page 内 `8↔4`、完整 `4^3` grid 和 stale frontier 都有定向测试。

2026-08-24 foundation repair 后，Java 17 thermal JUnit 为 `177/177`、全仓 JUnit 为 `705/705`，Forge GameTest 为 `14/14` required；`gradlew test runGameTestServer --no-daemon --console=plain` 通过。当前 pair compilation 仍允许 rebuild-time temporary aggregation allocation，尚未取得 PR7 steady-solve allocation、cell-arena retained bytes 或整服 CPU 证据；这些数字不能从 Phase A compile benchmark 外推。

实现：

```text
4/8/16 AirCells
split
merge
coverage lookup
implicit Air-Air adjacency
canonical face traversal
H / C
```

场景：

```text
closed box
open air
wall
door
shaft
tunnel
cave
```

暂时没有 phase material。

---

### Phase D — Solver

#### Phase D 实施快照 — 2026-08-24

状态为 `complete (combined solver/source correctness foundation)`。原 `PR 4` 与 `PR 5` 不再作为可独立实现或并行 review 的代码单元：source event 的 tick 边界、`integral(P dt)`、bounded time degradation 与 sweep 顺序由同一个 `ThermalStepExecutor` / `ThermalSourceTimeline` 路径验收。

已实现 `ThermalExchangeKernel`、`BuoyancyConductance`、arena-bound primitive `ThermalSweep`、`SolveEpoch`、`InputWatermarks`、`SealedInputFrame`、latest-only single-in-flight scheduler contract、`ThermalTimePolicy`、packed `ThermalSourceRegistry`、ports/bindings、`NodePowerAccumulatorArena`、retained segment replay/checksum 和 bounded source command timeline。source zero-cut 先于 transport；正常和 `TIME_DEGRADED` 路径都完整应用 sealed source energy，transport 只使用不超过 `maxSolveDeltaTicks` 的 `dtSeconds`。

旧 PR4 为独立编译引入的通用 `SourceEnergyApplier`、`IntervalOperator` 和 phase callback 已删除；registry 不再为每个 event 创建无人读取的 `SourceMutation`，executor report 也不再暴露恒为零的 phase execution counter。source stream 的 applied watermark 只由 timeline 推进，executor 只预检其他 stream，完成后回写真实 source watermark。联合测试还发现并修复了 source offer watermark 只计算 `+1` 却未回写字段的问题。

combined path 不再通过 `NodeEnergyConsumer` 交付 source；timeline 直接写 arena，并在清空 accumulator 前预检全部 non-zero target 的 slot generation 与数值可写性。`ThermalSweep` 绑定同一 arena、捕获 endpoint generation，旧 span 释放/复用后会在首次 mutation 前整体拒绝；executor 也拒绝 timeline/sweep arena 不一致。`NodeEnergyConsumer` 只保留为 accumulator 的独立测试/诊断入口，`SourceResyncReplayer.ReplayTarget` 只负责 retained recovery segment。跨 geometry/source 的同 tick frame-cut、PR7 mailbox/publication、sleep/wake、hard active-state caps 和 production source adapters 仍未实现。

Java 17 验证为 thermal JUnit `177/177`、全仓 JUnit `705/705`、Forge GameTest `14/14` required。solver/source 包共有 `47` 条定向 JUnit，包括真实 non-empty source + non-empty sweep 共享同一个 arena、queue-full retry、same-tick impulse/rebind、stale source generation 保留能量和 stale sweep generation 原子拒绝。

实现：

```text
combined source timeline + solver execution with exact integration
pair exchange kernel using dtSeconds and -expm1
boundary kernel using dtSeconds and -expm1
frozen forward/reverse sweep order
uniform thermal step
SolveEpoch
no epoch backlog
sealed input watermarks
maxSolveDeltaTicks
TIME_DEGRADED branch
```

`sleep / wake`、hard active-state caps 和其 memory admission 不属于这个纯数值/source correctness 单元；它们必须与 PR 7 的 dimension runtime ownership 一起实现和验收。

属性测试：

```text
closed system ΣH constant

source:
ΔH = ∫Pdt

split/merge:
ΣH conserved

each face:
exactly once

different SolveEpoch cells:
never exchange

delayed epoch beyond maxSolveDeltaTicks:
no oversized kernel dt
all source events and integral P dt still applied to effective H
only transport/phase time is skipped and rebased
skippedTransportTicks / skippedPhaseTicks explicit
```

单 pair kernel 必须与解析解在浮点容差内一致；完整 sequential sweep 只承诺守恒并显式测量 operator-splitting error，不宣称全图解析精确。Baseline 保持 source-bearing Page active；`STEADY_SOURCE_SLEEP` 不在 V1 实现范围内。

---

### Phase E — FarField

#### Phase E 实施快照 — 2026-08-24

状态为 `implemented (approval gate pending Phase 0b tolerances/evidence)`。`TopologyGuard` 已把一个 loaded-only frontier snapshot 明确分类为 `MATERIAL`、`OPEN_AMBIENT`、`OPEN_CONTINUATION` 或 `UNRESOLVED`。它不读取 World、不扩大扫描范围；immediate topology 未确定时禁止 transport，缺少 cheap outdoor proof、approved profile 或当前 operating point 超出 profile calibration domain 时保持 continuation。只有 approved profile 会直接生成现有 arena-bound `ThermalSweep.BoundaryOperation`，没有新增边界求解器。

`FarFieldProfileRegistry` 保存完整 classification key、static `Ginf`、功率/温差适用域、signed boundary-energy envelope 和其余 holdout error envelope；`CANDIDATE` 永远不能被 topology compiler 使用。`FarFieldReferenceHarness` 使用独立 multi-cell RK4 finite-volume reference，对同一 key 的 fit/holdout fixture 强制分离：fit 只选择 `Ginf`，holdout 独立检查 temperature trajectory、gameplay threshold crossing、signed natural-boundary energy integral 和 phase received power。标准 synthetic matrix 覆盖 open space、half-open space、cavern、tunnel exit、calm/windy 与 `1/10/100 kW`，其中 `1/100 kW` 为 fit、`10 kW` 为 holdout。

PR6 没有把手写常数或 synthetic test tolerance 伪装成生产校准。Phase 0b 仍缺 production workload 的四项 gameplay tolerance 和对应 reference evidence，因此当前 production registry 保持空，尚无 bucket 可称为 `APPROVED_STATIC_IMPEDANCE`。这不阻止 PR7 runtime correctness 实现，但继续阻止 PR8 Minecraft production integration。取得真实 tolerance/evidence 后只需运行同一 gate 并安装通过的 profile；未通过的 cavern/tunnel 或其他 bucket 继续保持 `OPEN_CONTINUATION`。

PR6 新增 `9` 条定向 JUnit；Java 17 完整验证为 thermal JUnit `186/186`、全仓 JUnit `714/714`、Forge GameTest `14/14` required，`gradlew test runGameTestServer --no-daemon --console=plain` 通过。

建立独立 reference fixtures：

```text
large open air domain
half-open space
large cavern
tunnel exit
different wind buckets
```

比较：

```text
large explicit domain

vs

local mesh + STATIC_IMPEDANCE
```

Source powers：

```text
1 kW
10 kW
100 kW
```

每个 topology/wind bucket 将 reference cases 分成 fit set 与 holdout set。approval gate 同时检查 holdout 的：

```text
local temperature delta over time
gameplay threshold crossing time
signed boundary-energy integral
phase-boundary delivered power
```

只有四项都进入 Phase 0b workload-specific tolerance 才标 `APPROVED_STATIC_IMPEDANCE`；不能证明误差上界的 bucket 继续使用 `OPEN_CONTINUATION`。不得用参与拟合的数据作为唯一验收，也不得把 `UNKNOWN_DEGRADED` 的 external exchange 伪装成 FarField 通过。

FarField 是架构最大数值风险之一。

如果：

```text
必须展开几十层16³ cells
```

才能获得合理结果，应暂停继续功能开发并重新修 FarField。

如果 open/half-open 基础 bucket 的 holdout 都不通过，Minecraft production integration gate 失败；不得继续用更多 adapter 掩盖 mesh 尺度不可行。

---

### PR7 — Runtime Ownership, Publication And Admission

#### PR7 实施快照 — 2026-08-24

状态为 `complete (pure-Java runtime correctness; not Minecraft-wired)`。实现收敛为四个 ownership unit，没有重新拆出 callback framework：

```text
ThermalMemoryBudget
    server-global + dimension admission
    CRITICAL / OPTIONAL reserve separation
    replacement backing is admitted before old reservation release

QueryPublication
    admitted preallocated thermal double buffers
    monotonic odd/even publicationVersion seqlock
    one retry then fallback
    lifecycle / geometry / topology / epoch envelope

DimensionThermalRuntime
    one logical dimension writer
    LatestSolveEpochScheduler + ThermalStepExecutor + arena/source/sweep
    explicit non-source watermark acknowledgement
    conservative whole-solve-set sleep/wake

ThermalRuntimeCoordinator
    fixed primitive ready-dimension queue
    IDLE -> QUEUED -> RUNNING mailbox state
    sticky queue-full re-offer
    FIFO/age promotion + reserved recovery quota
```

`DimensionThermalRuntime` 不复制 `H/C`；source timeline 仍把 `integral(P dt)` 写入同一个 `ThermalCellArena`，随后同一个 arena-bound `ThermalSweep` 执行。publication 只保存 query primitive projection。worker 启动时冻结 applied watermark 与 geometry/topology identity；若 main thread 在 solve 期间确认更新 revision，旧 solve 只能发布旧 envelope，query 通过 live revision mismatch fallback，applied watermark 也保持 component-wise monotonic，不能把旧 solve 冒充新 geometry。

sleep 只按整个 solve set 进入：无 continuous power/pending energy、当前 topology resolved、normal time plan、无更新 frame pending，且所有 compiled pair/fixed-boundary 当前温差残差持续低于 epsilon。当前 aggregate `ThermalSweep` 不支持安全的 partial Page skipping，因此 PR7 没有伪造按 Page 休眠；sleeping epoch 只推进空 source interval 并 O(1) 更新已有 publication envelope。任何 source/geometry/chunk/profile/transition watermark 或 topology revision 进展都会 wake。

coordinator 不创建 `Runnable` 或使用无界 `ExecutorService`。每个 dimension 最多一个 queued/running entry；ready queue 一次满只留下 `DISPATCH_REOFFER_REQUIRED`，后续 dequeue 用 round-robin cursor 自动重投。normal request 不能占用 recovery reserve，recovery quota 和 oldest-age promotion 防止连续主世界流量饿死其他 dimension。unload 先 retire publication；旧 generation worker 完成时不能再 publish，退出后再释放 admitted publication storage。

新增 `20` 条 PR7 定向 JUnit，覆盖两级 memory cap/critical reserve、resize peak double ownership、seqlock envelope 与并发 ABA、revision/generation fallback、single in-flight/latest target、explicit input ACK、source/solver/publication 同 arena、whole-set sleep/wake、hard work cap、queue-full sticky convergence、recovery quota 和 unload replacement generation。PR8 现在可以复用这些 concrete owner，但仍受 Phase E approved FarField profile 和 Phase 0b production-like evidence gate 阻止，不能据此替换 legacy gameplay authority。

---

### Phase F — Minecraft Geometry Integration

#### Phase F 输入与显式拓扑应用快照 — 2026-08-24

状态为 `complete (topology/runtime foundation)`。本阶段接通常见增量与完整恢复世界输入到 arena-native sweep，并提供可注入串行 `Executor` 的 dispatch；后续 Phase K 已按明确 fallback 接入 gameplay query：

```text
LevelChunkSection mutation Mixin (always present, null-owner fast path)
    -> explicit interest/admission owner
    -> immediate Page revision invalidation
    -> main-thread loaded-only 5^3 lazy snapshot union
    -> deterministic dispatcher + frozen shared signature table
    -> bounded primitive ResolvedGeometryInputRing
       + independently capped int[4096] full-Page snapshots
    -> tick-end five-stream SealedInputFrame
    -> explicit apply(frame) or latest-only shadow executor submission
    -> 4^3 regular/mixed/NO_AIR rebuild + sparse H migration
    -> replacement ThermalSweep + atomic non-source ACK
    -> old/retired Page span release
```

`MinecraftThermalInput` 只在显式构造后成为某个 `ServerLevel` 的 input owner；正常玩法不构造实例，因此 legacy 查询仍是唯一 authority。无 owner 的 section write 只执行现有 probe property 检查和一次 null owner read。主线程 snapshot 可短暂持有 `BlockState/FluidState`，worker-facing ring 的常见事件只含 section/generation/revision/tick/index/status/reason/signature ID 等 primitive；full resync 额外转移一份冻结的 `int[4096]` signature ID Page cut，在途 full snapshot 有独立小上限，超限 Page 保持 sticky 并在后续 tick 重试。signature ID 来自冻结的共享 `ThermalSignatureRegistry`，不按各维度 mutation 首见顺序动态 intern。

chunk unload 会先使相邻 Page sticky resync，再撤销 section owner，并移除 unload chunk 内 admitted Page；whole-section replacement 与已知 raw block-container replacement 进入相同失效合同。moving Create contraption 继续按空气，只有 assemble/disassemble 的普通世界写入被捕获，不建立 movement exclusion。`hasDynamicShape=false` 的静态状态继续自动走 generic resolver；不支持的 dynamic state 返回 unresolved。

`MinecraftThermalTopologyApplier` 是唯一 concrete topology writer：它保留每 Page 的 primitive signature/coverage 状态，把完整空气 Brick 编译为 regular cell、部分空气编译为 `ComponentBrickCompiler` mixed components、完整固体编译为 `NO_AIR/NO_COVERAGE`；不支持或 mixed-medium Brick 保守关闭并留下 unresolved topology。旧、新 proven-air microcell 以 `airCapacity/64` 形成稀疏 overlap，`GeometryMigrationLedger` 迁移唯一 arena 内的 `H`。所有 Page publication 和 canonical pairs 编译完成后，`DimensionThermalRuntime.finishTopologyUpdate` 才在同一 logical-writer transaction 内安装 replacement `ThermalSweep` 并推进 non-source ACK；随后才释放旧 span 或已经跨过 chunk watermark 的 retired Page span。

该路径仍由调用者显式 `enableTopologyApplication` / `applyTopology`。`enableDispatch` 接受 Java `Executor` 作为唯一未来调度边界；当前 gameplay 传入 `Runnable::run`，在 tick 末同步完成 topology apply、coordinator request 和 solve drain。替换为异步实现时，executor 必须对同一 input 串行、按提交顺序且不重叠；input 不维护另一套 mailbox/worker/snapshot。full-resync event 携带 token 与完整 `int[4096]` signature cut，applier 只通过匹配 section/lifecycle/revision/reason 的 `tryInstallFullGeometryResync` 清除 sticky requirement。source 保活只检查 live/queued port 是否引用待替换 arena span；Phase G 已提供具体 profile/rebind，并在受影响 source 尚未成功离开旧 span 时先完成 empty unresolved epoch 而不释放旧 span。没有 approved FarField profile 时，开放的 admission 外边界保持 topology unresolved 且不生成伪 boundary transport。

接：

```text
BlockState / FluidState mutation hook
FluidState mutation capture
LevelChunkSection owner map + lifecycleGeneration
main-thread loaded-only resolver snapshots
GeometryDelta coalescing
dependency-mask invalidation
geometryRevision
coarse invalidation
dirty interface immediate disable
unsupported dynamic state unresolved; moving structures are air with no exclusion index
Create assemble/disassemble endpoint mutation capture; no movement exclusion
tick-end sealed mutation/source ordering
per-stream sequence/watermarks
PublishedGeometryPage
preallocated PublishedThermalBuffers
publicationVersion seqlock + Page-wide revision fallback
bounded shared executor + per-dimension logical writer
bounded dimension mailbox + fair sticky re-offer
chunk lifecycle
```

验证：

```text
worker world read = 0
worker Minecraft World reference = 0
stale generation publication accepted = 0
all Phase 0a mutation/lifecycle GameTests remain green
```

---

### Phase G — Physical Sources

#### Phase G 实施快照 — 2026-08-25

状态为 `complete (player air and direct-radiation gameplay test path active)`。`MinecraftPhysicalSourceProfile` 暂定 Campfire `8,000 W` 与 Generator `10,000 W * TLevel`：Campfire 为 `6,400 W (80%)` convection + `1,600 W (20%)` radiation declared loss，Generator 为 `70%` convection + `10%` internal heat + `20%` radiation declared loss。blocked Campfire convection 进入 declared loss，blocked Generator exhaust 进入 internal heat；unloaded、unresolved 或单端口无法唯一表达的多 component face 一律进入 `DEGRADED_LOSS`。Phase J 已将 direct source radiation 接入玩家；材料 radiation、Phase H/I 与普通机器 gameplay authority 仍不启用。该 Campfire 功率是恢复密闭小屋取暖体验的玩法校准值，不改变功率分配、能量记账或相变合同。

`MinecraftPhysicalSourceManager` 只在显式 `enablePhysicalSources` 后存在；Campfire 由放置、section mutation 与 loaded chunk block entities 观察，Generator 由 `GeneratorLogic` tick 报告并在 disassemble 时移除。source 自身拥有 independently capped cold Page interest，稳定 source 不做 world scan；profile/anchor 改变以旧 lifecycle unload + 新 lifecycle register 表达，同一 source ID 在 packed registry 内原位 revival，不随重复 load/unload 增长 storage。

拓扑替换现在先执行 source zero-cut 与 `(previousTick,targetTick]` 精确积分，再检查旧 span 是否仍被 live/queued port 引用。rebind 完成后才替换/release；若 source queue 尚未提供 rebind，或 preapply 后 Page revision 竞争使安装失败，则当前 frame 安装 empty unresolved sweep 并推进 non-source ACK，让该 source epoch 恰好完成一次，下一 frame 再重建。`ThermalStepExecutor` 识别 pre-applied epoch，不会重复注入 `sourceAppliedJ`。正常 gameplay 已构造该 runtime；`GeneratorData.power` 和 `HeatEndpoint.heat` 仍只保留原玩法库存语义，设备产热通过 explicit physical profile 映射，不双写第二个空间热场。

验证实现包含 profile partition、`100 * P/100 == P` 基础合同、unload/revival、topology-cut exactly-once、blocked/unresolved sink，以及真实 `ServerLevel` 上 Campfire/Generator shadow ledger GameTest；最终计数以本阶段 diary 的离线全量结果为准。

先实现：

```text
Campfire
Generator
```

功能：

```text
SourceRegistry
emission ports
NodePowerAccumulator
source rebind
retained binding-segment replay
cumulative emitted-energy checksum only
bounded per-binding resync history
SOURCE_RESYNC_LOSS fallback
cold/unload settle before binding removal
cold admission
blocked-port policy
```

测试：

```text
100 × P/100
==
1 × P
```

在相同 cell / channel 下 thermal result 必须等价。

---

### Phase H — MaterialBoundary

实现：

```text
STATELESS_CONDUCTANCE
CAPACITIVE_SURFACE
deep natural boundary
geothermal rock
```

测试：

```text
wall insulation
wall residual heat
thick rock not shortcut
deep shaft geothermal
```

---

### Phase I — Phase Reservoir

#### Phase I 实施快照 — 2026-08-25

状态为 `complete; hot-side gameplay authority enabled for applied candidates`。`MaterialBoundaryRegistry.PHASE_RESERVOIR` 将同一 world-aligned `4x4x4` Brick、同一 profile 且与 proven air 精确接触的 candidates 聚合为一个 primitive reservoir；候选位置使用 `long mask`，不建立全局 patch、逐方块 timer 或逐方块 thermal node。潜热、预留能量和单一 outstanding request 都保存在 Page-owned `ThermalCellArena` span，普通 Air adjacency 不遍历 reservoir。

`ThermalSweep` 通过 `PhaseTransitionRuntime` 保守地把空气显热转入 latent reservoir。达到一个 visible-unit threshold 后，worker 预留能量并通过固定 request ring 发给主线程；ring 满时 reservoir 保持 sticky retry。主线程在 tick sealing 前按 `maximumPhaseMutationsPerTick` 处理，并验证 Page generation、loaded chunk、当前 thermal profile 以及与编译器一致的精确 `4x4x4` material-air microface。内置 action 支持减少一层雪和冰转水，`CUSTOM` 只通过窄 `MinecraftPhaseTransitionHandler` 扩展。ACK ring 满时 outcome 保存在固定表；APPLIED ACK 才扣除预留潜热并增加 committed ledger，REJECTED 保留已吸收热量，RETRY 保持原 request。拓扑重建按 Brick/profile key 迁移潜热和 request metadata，Page generation 改变时 stale ACK 不能命中新 reservoir。

`TransitionMutationPolicy` 已与 thermal core 分离：天然兼容 profile 可遵守 `randomTickSpeed`，明确的机器驱动 profile 可忽略它，script-controlled profile 不自动执行。gameplay profile cut 现从旧 `StateTransitionData` 编译热侧阈值、目标状态和相对 `heat_capacity` 倍率，按 `38,000 J * heat_capacity` 与统一 `20 W/K` full-face conductance 运行；只有静态、存在保守 material mask 且已安装到 Page 的候选归新系统所有。固体多阈值按先到阈值逐阶段执行，每个 replacement state 重新进入自己的 recipe/profile，阈值相等时保留 gas 优先。最终 mutation 继续遵守 `ICE_DO_NOT_SMELT` 群系标签。冻结/凝结、动态/空 contact 和 Page 外状态继续由 legacy 路径处理，数值仍需实机校准。

实现：

```text
snow
ice
optional permafrost prototype

local phase state
candidate mask
energy reservation
request / ack
per-reservoir sticky retry
TransitionMutationPolicy
```

测试：

```text
wall blocks melting
more P melts faster
weak P may reach equilibrium and fail
multiple sources add correctly
```

---

### Phase J — Radiation

#### Phase J 实施快照 — 2026-08-25

状态为 `complete (player gameplay receiver enabled; material radiation deferred)`。`RadiationService` 直接包含 source-origin section index、bounded discovery/top-K、inverse-square flux、feet/torso/head samples 和 bounded receiver witness cache；没有拆成 source-index/cache/resolver 回调类。生产参数冻结为 `128 sources / 1,024 tracked sections / 128 receivers / 64 visits / top-8 / 24 rays / 8 witness sections / 256 DDA steps / 16 blocks / 0.1 W/m² cutoff`，最坏 optional reservation 为约 `729,408 B/dimension`。达到 source/candidate/ray cap 时返回已累计结果并设置 `RADIATION_BUDGET_LIMITED`；unloaded/unresolved path 保守遮挡并设置较低 confidence。

`MinecraftRadiationOcclusion` 是 concrete loaded-only quarter-block DDA。正式玩家路径只采用二值遮挡，因此直接读取已加载 section 的 `BlockState`：动态形状按空气，静态状态只按 `canOcclude()` 判断透明或整块不透明，不再为每个新方块构造 resolver 快照；它不复用 collision、airflow aperture 或 material contact。active ray 保存经过 section 的独立 revision，BlockState/raw replacement、section identity 和 chunk load/unload lifecycle 会使 witness stale。Campfire/Generator 继续由同一个 `MinecraftPhysicalSourceManager` 持有 source ID、generation、启停和总功率，radiative power 只取现有 `RADIATION` port share 与冻结 origin。晚于 source manager 启用 radiation 时会 replay live sources。

player sample 不调用 `ThermalSourceTimeline`，不扣 source accumulator，也不把 `Prad` 再注入 Air Mesh；Phase G 的 radiation port 仍在 interval ledger 中只记一次 declared ambient loss。生产射线直接对 loaded `BlockState` 使用独立二值 `BlockState.canOcclude()` 通道，不为部分 shape 支付额外编译、resolver 快照或 mask 成本。`TemperatureComputation.radiantBodyTemperatureDelta` 使用 `q * 0.7 m² * 0.8 * seconds / 5,000 J/K` 将 flux 转为现有五部位体温增量；`q * 0.8 / 6 W/m²/K` 只形成 HUD 体感，不重复增加身体能量。材料 radiation 继续后置；数值仍需 production-like workload 与玩法校准。

首先只实现：

```text
packed section source spatial index
max range + flux cutoff
candidate/ray hard caps
player feet/torso/head LOS
independent SectionOcclusionRevision witness cache
RADIATION_BUDGET_LIMITED confidence/flag
read-only player observation; no source energy mutation
```

材料 radiation 后置。

---

### Phase K — Consumer Migration

#### Phase K 实施快照 — 2026-08-25

状态为 `complete (player air/radiation plus crop/town gameplay authority; no machine API without a real consumer; HUD remains downstream presentation)`。

玩家第一步已经沿真实生产对象接通，不新增 resolver callback：

```text
TemperatureUpdate legacy air fallback
    -> MinecraftThermalInput.gameplayPlayerEnvironment
    -> ensure one loaded player ThermalPage
    -> regular coverage or exact mixed microcell component
    -> DimensionThermalRuntime-validated QueryPublication
    -> enabled bounded Phase J radiation
    -> absorbed-energy body delta
```

`MutableEnvironmentSample` 保留 `airTemperatureC`、`radiantFluxWPerM2`、`surfaceFluxW`、`mediumId`、`confidence`、`sampleTick`、cell/query flags。query 在 runtime logical writer 活跃、Page stale、dimension/geometry/topology envelope 不一致、publication 超龄、无 Page 或无 air component 时显式 miss；Air Mesh 分支不等待 worker、不读取世界、不加载 chunk、不 admission，player wrapper 启用的 Phase J radiation 仍按独立 loaded-only DDA 预算执行。mixed Brick 通过 applier 已保存的 compiled component mapping 定位 arena slot，不能把 support ref 当作具体空气 component。

machine 第二步没有虚构 gameplay consumer。当前普通机器没有直接读取 `WorldTemperature.air/block`，因此删除了没有调用者的 `sampleMachineEnvironment`、`observeRegisteredMachineEnvironment` 和 `MachineShadowSnapshot`。将来只有出现真实 `QUERY_ONLY` consumer 后，才在它既有 cadence 和显式 receiver point 增加窄查询入口；不得扫描 BlockEntity、创建无关 interest，或把 Campfire/Generator 重复接成 machine receiver。

crop 第三步接在真实 `WorldTemperature.checkPlantStatus` 边界：生长事件、树苗事件和 `ServerLevel.tickChunk` 的存活/死亡预计算温度路径，只要真正需要温度就调用 `gameplayCropEnvironment`。crop receiver 使用现有 BlockPos 的中心点，只读已有 regular/mixed publication；天气提前返回时不虚增 query。命中时 new air 直接进入作物阈值，miss 时 legacy block temperature 回退，不创建 Page/Brick/Cell/Interest 或 legacy/new 比较状态。

town 第四步没有增加第二次室内扫描。现有 `BuildingBlockScanner` 在同一次空气遍历中生成 `TownThermalProjection`，按 world-aligned `4×4×4` base Brick 保存一个确定的真实内部空气代表点和整数 voxel 权重，不保留逐体素坐标。住宅与狩猎扫描成功后调用 `gameplayTownEnvironment`，每个 weighted group 只读一次已有 publication；所有 group 命中时 new air 加权平均成为建筑温度，部分或全部 miss 时整体回退 legacy 全体素平均，并通过 `QUERY_PARTIAL_REGION` 与 voxel coverage confidence 显式报告。全程不创建 Page/Brick/Cell/Interest、不持有 mesh lease，也不读取或加载世界。矿井基地主路径没有 gameplay 温度，停用的 `MineBlockScanner` 未重新接活。

没有新增 `SharedQueryFrame` 对象层。当前不同作物位置之间没有可复用的 publication value，盲目 cache 只会增加失效合同；10,000 次 passive miss 直接验证 retained thermal state 不随作物调用量增长，不在 production input 常驻累计 query 计数。如果 Phase 0b 真实 workload 证明同 tick 同坐标重复显著，再在现有 primitive hot path 内增加 revision-safe frame cache。

玩家空气 publication 与直接 source radiation 已进入现有五部位体温 authority；衣物、设备、食物、效果、伤害、HUD 阈值、网络包和更新 cadence 未改。作物 `PlantStatus` 与城镇建筑温度/评分/日结算在 publication 完整命中时也使用 new air，明确 miss 时回退 legacy；工程内没有真实普通机器温度 consumer，所以不保留 dormant machine observation 接口。surface compositor 尚未实现并以 flag 暴露。HUD 不是第五个 thermal query consumer：温度球、环境温度和冷热效果继续读取由 `FHBodyDataSyncPacket` 同步的 `PlayerTemperatureData`，位于玩家 consumer 下游，不增加第二次玩家采样。production-like Phase 0b、FarField approval 和 gameplay/reference calibration 仍约束数值冻结与后续 material/machine authority。

验证基线：定向 Java 17 JUnit `239/239`（`238` thermal + `1` player radiation conversion）、Forge GameTest `19/19` required。Minecraft 场景确认 legacy `5°C`、publication `0°C` 时，真实 `PlantStatus` 按 `0°C` 进入 survive 而不是 grow，完整 town projection 也返回 `0°C`；player/crop 可读取已有 mixed 空气 component、solid microcell 不会 alias support、未 admission miss 不增加 Page、超龄 publication 显式 fallback。town 的两个内部 voxel 压成一个 weighted Brick lookup，10,000 次 crop miss 与 4,096 个 town group miss 都不增加 Page、arena high-water、live cell 或 `H`；runtime JUnit 确认 topology envelope 前进后旧 publication 被拒绝。此次 shadow 清理后的最终验证见对应 diary。

顺序：

```text
player
crop
town
HUD presentation remains downstream of player; no separate query
```

保持 invariant：

```text
crop passive
town passive
```

---

### Phase L — Runtime Validation

#### Phase L 实施快照 — 2026-08-25

状态为 `in progress`。曾加入 `MinecraftThermalInput` 的 production shadow observability 已删除：没有命令、HUD 或日志消费者的 per-consumer error、lookup/timing 计数、mailbox 状态和 last dispatch report 不再作为每维度常驻状态。真实 gameplay query、fallback、topology apply、solve 和 publication 路径全部保留。

底层 `DimensionThermalRuntime.Diagnostics` 仍供定向测试和 JMH retained-size 使用。当 logical writer 正在修改 arena/topology 时，它返回 `writerBusy=true`，并把需要读取 arena/sweep 的瞬时计数置为 `-1`；不会为诊断等待 worker 或无同步读取可变 SoA。JFR/JMH、retained-size 和完整 `1/10/50/100` player workload 仍属于 Phase L 后续验收。

首批切片验证保留 runtime writer-busy 测试；原先只验证 shadow snapshot 自洽的 Minecraft 断言随无用 API 一并删除，GameTest 改为直接验证 executor 收到 frame、topology 状态落入 Page、solve 完成、fallback 和 passive miss 不增长 thermal state。

第二批 synthetic query diagnostics 已实现但不构成 Phase L acceptance。`thermalPhaseLQueryDiagnostics` 一次生成 forked JMH、100-receiver diagnostic JFR、JOL retained graph 与不含路径/源码哈希要求的环境限制 manifest；fixture 直接组合 production `ThermalPage + ThermalCellArena + DimensionThermalRuntime + QueryPublication`，覆盖共享 Page 与分散 Page 的 `1/10/50/100` 顺序 query batch。首轮测量暴露 `HashMap<Long, ThermalPage>` 在非缓存 section key 上产生约 `24 B/query` 装箱分配，因此唯一 hot Page index 改为现有 fastutil `Long2ObjectOpenHashMap`；source/witness 等其他 Map 未被泛化重写。最终两种布局约 `0.01 B/query`，100-query batch 均值约 `4.9 us`、p95 `5.0 us`、p99 `12.69/13.30 us`；JOL isolated retained graph 为共享 100 receivers `11,264 B`、分散 100 Pages/Cells `313,472 B`。

第三批用外部 `jcmd JFR.start` 附着真实 Java 17 Forge GameTestServer JVM，没有增加 production capture code。录制覆盖 `55 s` 的模组加载、世界启动、`19/19` required GameTests 和关服，因此只能验证 Forge 生命周期中的路径可运行并提供热点线索，不能作为稳态 TPS 或多人验收。`5,300` 个 execution samples 中有 `97` 个栈包含 thermal package（`1.83%` sample share，其中 `50` 个在 Server thread）；主要是 Phase 0 mutation probe、resolver census、几何解析和 topology rebuild，Phase L `samplePublishedAir` 只出现 `1` 个执行栈。`13,913` 个 allocation samples 的采样权重估算为整段 JVM `33.86 GB`、thermal 栈 `765.27 MB`（`2.26%`），thermal 部分同样由 GameTest 触发的 `ConservativeAirGeometry`、resolver snapshot 和 mutation-probe fixture 主导；Phase L published-air query 没有 allocation sample 命中。sampling weight 不是精确分配计数或稳态 B/s。

同一录制包含 `93` 个 GC pause event，总计约 `851.5 ms`、p95 `21.626 ms`、最大 `25.456 ms`；`13` 个 contended monitor-enter event 均无 thermal package frame。全生命周期 JVM user/system CPU 平均约 `6.91%/0.27%`、峰值约 `35.26%/1.25%`。`minecraft.ServerTickTime` 的 `42` 个周期样本中只有 `15` 个非零，且混入启动与 GameTest 长 tick，因此明确不用于推导 TPS。原始录制位于本机构建产物 `build/reports/thermal-phase-l/forge-integration-diagnostic.jfr`，不进入源码或提交物。

这里的 `receiverCount` 只是顺序 query batch 标签，不是真实玩家。该 bundle 不含 `ServerPlayer/ServerLevel`、radiation receiver cache、topology rebuild、worker solve、TPS、chunk lifecycle 或生产模组集合；因此真实 `1/10/50/100` 玩家服务器 capture、`100p/10b`、`100p/100b`、exploration、dynamic base 与 source workloads 仍未通过，Phase L 保持 `in progress`。

旧 thermal system：

```text
继续驱动gameplay
```

新系统：

```text
不控制世界结果
只shadow计算和记录
```

记录：

```text
temperature difference
page count
cell count
brick count
implicit face ops
boundary ops
worker time
main-thread work
memory
allocation
query fallback
degraded boundary
geometry reads
publication age
publication seqlock retry/ABA fallback
resolver reads / unsupported signatures
geometry ingress/egress/environment exchange
source replay/checksum mismatch/resync loss interval
epoch watermarks / in-flight state
actual/max solve dt / TIME_DEGRADED skipped transport/phase ticks
dimension mailbox queue/wait/re-offer/age
radiation candidates/rays/retrace/budget-limited
passive query calls/lookup misses
```

场景：

```text
1 player
10 players
50 players
100 players

100 players / 10 bases

100 players / 100 bases

100 outdoor players

high-speed exploration

dynamic piston / door base

100 stable sources

100 changing sources
```

Shadow 数据通过后才能考虑替换旧 gameplay path。

### 82.1 PR 切分合同

实施按以下 PR 顺序交付；每个 PR 都应可独立 review、benchmark 和回退，不能把 Minecraft integration 提前塞入纯 Java 基础 PR：

| PR | Scope | Required gate |
|---|---|---|
| `PR 0a` | low-level section mutation/lifecycle spike、owner map、GameTest namespace + coverage matrix | hook、recursive write、fluid、Door、piston、Create、unload generation、raw palette bypass 全部有结论；不接 production gameplay |
| `PR 0b` | units、reference/harness、old-system profiling、workload-specific acceptance、feature-flag shell | typical/stress gates 冻结；不改 gameplay |
| `PR 1` | synthetic conservative raster、local complement components、`int` signatures/regions、`64*Rmax` compiler、face ports | 无 false opening；pure-Java property/fixture tests |
| `PR 2` | Forge state/fluid resolver census、explicit/static/contextual classification、dependency masks、unsupported fallback | 真实模组集合 census；resolver 不越界、不加载 chunk；窄 packing 仍未启用 |
| `PR 3` | `ThermalPage`、`int coverageRef[64]`、4/8/16 cells、`FacePatchIterator`、split/merge、geometry overlap ledgers | 16↔16/16↔4/8↔4 exactly-once；纯 LOD ΣH；geometry ingress/egress residual |
| `PR 4+5` | pair/boundary kernel、buoyancy `G`、sealed `SolveEpoch`、bounded time model，以及 packed source registry/ports/accumulator/exact replay 的同一执行路径 | `dtSeconds`/`-expm1`、sweep error、no backlog、mid-cadence/rebind/overflow/history-exhaustion/unload settle、TIME_DEGRADED 保留全部 source energy、无 cumulative double injection |
| `PR 6` | Topology Guard、`OPEN_CONTINUATION`、candidate `STATIC_IMPEDANCE` + holdout reference harness | FarField holdout gate；未批准 bucket 不进入 static impedance |
| `PR 7` | bounded executor/mailbox、per-dimension logical owner、single in-flight、seqlock publication、global/dimension memory admission | ABA/generation、multi-dimension fairness、queue-full re-offer、continuous-overflow recovery |
| `PR 8` | production state/fluid mutation capture、loaded-only incremental/full resolver snapshots、unsupported dynamic fallback、chunk lifecycle、runtime wiring | topology apply/ACK、full-resync recovery 与可注入串行 executor dispatch 已完成；worker 无 World reference |
| `PR 9` | Campfire/Generator physical source adapters | source lifecycle/ports、physical-vs-legacy exclusivity、blocked/unresolved-port policy、topology-cut exactly-once |
| `PR 10` | stateless/capacitive wall、natural rock、surface/deep material | wall/thick-rock/geothermal tests |
| `PR 11` | ownership、candidate mask、energy reservation、per-reservoir request/ack retry、`TransitionMutationPolicy` | explicit phase energy、generation rejection、gamerule policy |
| `PR 12` | radiation source index、3-point player rays、occlusion revisions、bounded witness cache | discovery/LOS/cache/budget limits；重复观察不重复能量 |
| `PR 13` | gameplay query compositor、player、machine、crop、town、HUD/debug adapters | passive miss 不创建 mesh；legacy output calibration |
| `PR 14` | JFR/JMH 与 production-like workloads | 用真实 workload 验证 CPU、retained memory、publication age 与 fallback |

第一批工程任务止于 `PR 0a..3 + PR 4+5`：证明 hook/lifecycle、数值合同、synthetic geometry、真实 resolver census、Page/face ownership，以及同一 solver/source path 的 exact replay。`PR 6` 是独立 FarField 可行性门；它不通过时停止 production Minecraft integration。`PR 7` 先证明 coordinator/publication/executor，之后才允许 `PR 8` 把 runtime 接入世界。

### 82.2 Worktree Parallelization

| Workstream | Modules touched | Depends on |
|---|---|---|
| `PR 0a` mutation/lifecycle spike | `build.gradle`, Forge GameTest/support packages | none |
| `PR 0b` units/reference/workloads | thermal benchmark/reference packages | none |
| `PR 1 -> PR 2 -> PR 3` geometry lane | thermal geometry/profile/mesh packages | `PR 0b`; `PR 2` also uses `PR 0a` findings |
| `PR 4+5` combined numerics/source lane | thermal solver/source packages | `PR 0b`; final face integration uses `PR 3` |
| `PR 6` FarField lane | thermal reference/mesh packages | `PR 3`, `PR 4+5` kernel |
| `PR 7` runtime lane | thermal runtime/query packages | `PR 3`, `PR 4+5` |
| `PR 8` Minecraft integration | Forge hooks plus thermal runtime/geometry | `PR 0a`, `PR 2`, `PR 6`, `PR 7` |
| `PR 9..13` gameplay lanes | thermal source/material/transition/radiation/consumer packages | `PR 8` plus each row's subsystem prerequisites |
| `PR 14` shadow validation | benchmark/compat/runtime packages | `PR 9..13` |

Parallel lanes：

```text
Lane A: PR 0a -----------------------------------------> PR 8
Lane B: PR 0b -> PR 1 -> PR 2 -> PR 3 -> PR 6 -------> PR 8
Lane C: PR 0b -> PR 4+5 -> PR 7 ----------------------> PR 8
After PR 8: PR 9 + PR 10 + PR 12 may start in parallel;
            PR 11 follows PR 10; PR 13 follows PR 9/10/11/12; PR 14 last.
```

`PR 3`/`PR 6` 都触及 mesh，`PR 4+5`/`PR 7` 都触及 runtime input envelopes，不能在未冻结接口时长期分叉；`PR 4+5` 内部不得再按 numerics/source 人为拆开。`PR 0a` 与 `PR 8` 都触及 Forge hook 包，前者必须先合并，后者复用 spike 而不是复制另一套 hook。

---

## 83. 必须长期暴露的 Metrics

```text
thermal.pages.total
thermal.pages.active
thermal.pages.sleeping
thermal.pages.rebuilding

thermal.cells.4
thermal.cells.8
thermal.cells.16

thermal.bricks.mixed
thermal.bricks.compiledPerTick

thermal.geometry.coldReads
thermal.geometry.deltas
thermal.geometry.staleQueries
thermal.geometry.resolverReads
thermal.geometry.resolverUniquePositions
thermal.geometry.dependencyClosure.max
thermal.geometry.unresolvedResolverReads
thermal.geometry.fullResyncs
thermal.geometry.fullResyncLatency
thermal.geometry.recovery.oldestAgeTicks
thermal.geometry.ingressEnergyJ
thermal.geometry.egressEnergyJ
thermal.geometry.transferResidualJ

thermal.signatures.localRegionCount.max
thermal.signatures.localRegionCount.histogram
thermal.signatures.unsupported

thermal.faces.implicitPerStep
thermal.faces.patchesPerStep
thermal.faces.duplicateAssertions
thermal.faces.missingAssertions
thermal.boundaries.perStep

thermal.sources.total
thermal.sources.changed
thermal.sources.events
thermal.sources.pendingEnergy
thermal.sources.appliedEnergyJ
thermal.sources.resyncs
thermal.sources.replayedSegments
thermal.sources.cumulativeMismatchJ
thermal.sources.resyncLoss.count
thermal.sources.resyncLoss.energyJ
thermal.sources.resyncLoss.intervalTicks
thermal.sources.cumulativeEmittedEnergyJ
thermal.sources.externalEnvironmentExchangeJ

thermal.phase.states
thermal.phase.requests
thermal.phase.accepted
thermal.phase.rejected
thermal.phase.requestRetries
thermal.phase.ackRetries

thermal.radiation.sourcesIndexed
thermal.radiation.candidates.p50
thermal.radiation.candidates.p95
thermal.radiation.candidates.p99
thermal.radiation.raysPerReceiver
thermal.radiation.cacheRetraces
thermal.radiation.budgetLimited
thermal.radiation.occlusionRevisionEntries
thermal.radiation.emittedEnergyJ

thermal.query.background
thermal.query.surface
thermal.query.mesh
thermal.query.fallback
thermal.query.passiveCalls
thermal.query.lookupMisses
thermal.query.naturalCallerFrame
thermal.query.naturalMainThreadFallback
thermal.query.staleGeometry
thermal.query.pageRevisionFallback
thermal.query.publicationSeqlockRetries
thermal.query.publicationAbaFallbacks

thermal.worker.ms
thermal.main.ms

thermal.executor.readyQueueDepth
thermal.executor.queueWaitTicks.p50
thermal.executor.queueWaitTicks.p95
thermal.executor.queueWaitTicks.p99
thermal.executor.mailboxReoffers
thermal.executor.dimensionOldestAgeTicks
thermal.executor.dimensionStarvationPromotions

thermal.solver.inFlightDimensions
thermal.solver.sealedInputLag
thermal.solver.appliedInputLag
thermal.solver.targetCadenceTicks
thermal.solver.maxDeltaTicks
thermal.solver.actualDeltaTicks
thermal.solver.timeDegraded.count
thermal.solver.timeDegraded.skippedTransportTicks
thermal.solver.timeDegraded.skippedPhaseTicks

thermal.watermark.geometryLag
thermal.watermark.sourceLag
thermal.watermark.chunkLag
thermal.watermark.profileLag
thermal.watermark.transitionAckLag

thermal.bytes.pages
thermal.bytes.cells
thermal.bytes.bricks
thermal.bytes.boundaries
thermal.bytes.publication
thermal.bytes.sourceRegistry
thermal.bytes.mailboxes
thermal.bytes.radiation
thermal.bytes.dimensionTotal
thermal.bytes.serverTotal

thermal.alloc.bytes
thermal.gc.pauseMs

thermal.energy.residual
thermal.energy.evictionEnvironmentExchangeJ
thermal.energy.unloadEnvironmentExchangeJ
thermal.queue.overflows
thermal.queue.geometry.overflows
thermal.queue.source.overflows
thermal.queue.transitionRequest.overflows
thermal.queue.transitionAck.overflows
thermal.queue.chunk.overflows
thermal.queue.profile.overflows
thermal.queue.recovery.attempts
thermal.queue.recovery.successes
thermal.queue.recovery.oldestAgeTicks
thermal.queue.recovery.budgetStarvation

thermal.degradedUnknownBoundaries
thermal.degradedLoss.count
thermal.degradedLoss.energyJ
thermal.farFieldRefinements
thermal.refusedAdmissions

thermal.publicationAge.p50
thermal.publicationAge.p95
thermal.publicationAge.p99
```

没有这些指标，不允许凭：

```text
“感觉挺快”
```

进入生产迁移。

---

## 84. 原型性能目标

以下是 benchmark target，不是生产承诺。

```text
publication/surface hit:
    O(1)
    0 world reads
    0 allocations
    0 worker waits

caller-frame Natural hit:
    O(1)
    0 world reads
    0 allocations

main-thread Natural fallback:
    bounded lookup
    0 chunk loads
    measured separately from hits

stable source:
    0 topology work

stable solve/query publication:
    0 allocation

missed cadence:
    0 queued catch-up epochs
    single in-flight epoch
    dt <= maxSolveDeltaTicks
    complete source energy application
    explicit skipped transport/phase ticks beyond bound

inactive world:
    0 thermal state

inactive snowfield:
    0 thermal state

ordinary machine:
    0 thermal state
```

`< ~8 MiB` 只能作为某个冻结场景的首选候选，不是“100 players”这一行文字即可通过的总 gate。Phase 0b 必须分别给 outdoor passive、`10` shared bases、`100` physical-source bases、高速探索和多维度负载冻结：main-thread/worker p95/p99、TPS、retained bytes、allocation/GC、fallback、publication age、queue age 和能量账本阈值。per-dimension 与 server-global cap 也必须由同一表批准。

```text
typical workload gate:
    SOURCE_RESYNC_LOSS = 0 J
    source external-loss interval = 0
    TIME_DEGRADED = 0
    skippedTransportTicks = 0
    skippedPhaseTicks = 0

stress workload gate:
    caps hold
    no unbounded backlog or chunk load
    degradation/loss/exchange is bounded and attributed
    recovery oldest age returns below threshold
```

最终数字必须由真实：

```text
JFR
JMH
retained-size measurement
production-like server benchmark
```

决定。

进入生产候选前，以下是硬门槛：

1. Publication/surface/caller-frame query hit 为 `0 World reads`；main-thread Natural fallback 单独计量。
2. Query hit 与 steady-state publication 为 `0 allocation`。
3. Worker 持有 `0 Minecraft World references`。
4. Stable source 产生 `0 topology work`。
5. 不存在 source-radius scan。
6. 不存在 room/cave flood fill。
7. 不存在 loaded-section thermal root。
8. 不存在 generic Air-Air edge graph。
9. Mixed Brick compile 的 atom 上限固定为 benchmark 冻结的 `64 * Rmax`，并使用 primitive flattened spans。
10. 所有 event queues 都有 hard cap、owner-owned sticky recovery、reserved recovery quota、oldest-age promotion、sequence/watermark 和 queue-specific metric；source gap 由 retained segments replay，cumulative 只作 checksum。
11. Solver 不存在 unbounded backlog；每维度最多一个 in-flight epoch，所有 kernel `dt <= maxSolveDeltaTicks`；超限仍完整应用 source energy，只跳过并记录 transport/phase 时间。
12. Memory admission 同时有 dimension 与 server-global hard cap；source registry、mailbox、publication resize 与峰值双持都入账。
13. FarField 的 open/half-open/cavern/tunnel holdout reference gate 通过；未批准 bucket 保持 `OPEN_CONTINUATION`。
14. 一格孔、矿道和竖井不被 LOD 吞掉。
15. Crop/Town miss 不创建 mesh。
16. State/Fluid mutation 后旧 topology 不会继续参与 transport；V1 publication revision mismatch 一律 Page-wide fallback。
17. Source mid-step 与 rebind energy error 为零或浮点容差级。
18. 每个已冻结 `100-player` workload 分别通过目标 JVM 的 retained-memory、allocation/GC、TPS、fallback 与 loss/degradation gate。
19. Feature flag 可以切回旧 runtime。
20. Shadow validation 无 blocker。
21. Resolver 不越过声明 dependency mask、不加载 chunk；`NEIGHBOR_26` closure 成本已在目标模组集合上测量。
22. 普通 `PLAYER_BODY` miss 不创建 mesh；只有显式高精度/玩法 lease、物理 source 和受预算诊断可 admission。
23. V1 non-zero source-bearing Page 始终 ACTIVE；没有 `STEADY_SOURCE_SLEEP` production path。
24. Shared executor 保持 per-dimension single-writer、bounded mailbox、fair re-offer 和 generation-safe unload，且不按维度永久占用 OS thread。
25. Publication seqlock 通过 ABA、buffer reuse、geometry swap 和 unload/reload stress；失败只产生 bounded retry/fallback。
26. Radiation discovery、candidate、ray 与 witness cache 都有 hard cap；budget-limited sample 可观察，passive query 不改变 source energy。
27. `QpassiveCalls`、`QlookupMisses` 与 `NtickingChunks` 在成本报告中独立出现；不得声称 CPU 与作物调用或 ticking chunk 数量无关。

---

## 85. 核心 Correctness Tests

测试按故障边界分层，不能用一种 harness 假装覆盖全部集成点：

| Layer | Owns |
|---|---|
| JUnit 5 pure Java | conservative raster/components、`FacePatchIterator`、H/C migration、pair/boundary kernels、source ledger/replay、mailbox、publication seqlock |
| Forge GameTest / `runGameTestServer` | Mixin hook、Door/Fluid/piston/Create、recursive callbacks、raw palette bypass、chunk unload/reload lifecycle |
| JMH + JFR/JOL | hot-path zero allocation、raster/compile/query ns、arena retained bytes、publication resize peak |
| production-like shadow server | `1/10/50/100` players、TPS、multi-dimension fairness、queue recovery、typical/stress acceptance |

每个 public branch、fallback、generation rejection、overflow recovery 和 ledger reason 都要有行为断言。JUnit 不能替代 Forge call-path 证明，GameTest 也不能替代并发 ABA stress 或 retained-heap 测量。

### Geometry

```text
wall prevents false Air-Air connection

conservative 4x4 aperture raster properties:
open bit implies whole tile proven empty + same local component
random AABB unions never create a false opening

Door/Trapdoor/FenceGate state change
→ resolved geometry + local component rebuild

two-half Door across Brick/ChunkSection changed in one tick
→ both Pages share sealed mutation watermark
→ no half-open publication

explicit machine GateOperator
→ toggles G without component split/merge

pane/fence local air regions remain distinct

registry census freezes finite Rmax
and signatures above Rmax become observable unsupported fallback

NEIGHBOR_26 single-block mutation
→ affected centers complete
→ union reads <= 5³
→ no out-of-mask reads

contextual resolver at unloaded chunk boundary
→ UNRESOLVED
→ no chunk load

waterlogged slab/stair/fence
→ airflow aperture, material/fluid contact and radiation occlusion
   are independently correct

moving piston / BlockEntity-driven shape
→ section-indexed conservative dynamic exclusion, no entity/BE read from worker

Create contraption
→ assemble removal and disassemble placement use world mutation capture;
  moving entity is air and emits no thermal geometry delta

1×1 shaft survives

partial face aperture aligns correctly

air→stone inside16³ coarse cell
immediately invalidates coarse support

stone→air eventually restores path

FacePatchIterator:
16↔16, 16↔4, 8↔4, both axis directions, cross-Page,
mixed↔coarse and randomized legal tilings
→ canonical patch key unique
→ no duplicate/missing patch
→ summed overlap area exact
```

### Energy

```text
closed system:
ΣH constant

source:
ΔH = ∫Pdt

split/merge:
pure LOD with unchanged air volume
→ ΣH before = ΣH after

positive- and negative-temperature air↔stone rebuild:
overlap keeps old temperature
removed air → GEOMETRY_EGRESS_J
new air → neighbor/Natural initialization + GEOMETRY_INGRESS_J
ledger identity residual within floating tolerance

pair/boundary direction:
hot air↔cold stone and sub-zero air↔warmer stone
→ heat sign always follows temperature difference

forward/reverse multi-face sweep:
ΣH conserved; operator-splitting error measured against reference
```

### Source

```text
1 source × P
=
10 sources × P/10

mid-cadence on/off:
injected energy = exact integral P dt

source rebind at tick t:
old and new accumulators settle to t before power moves

SourceEventRing overflow during on/off/rebind:
retained binding segments replay from last ACK in order
cumulative emitted energy remains checksum only
normal and replay trajectories match without double injection

binding history exhaustion:
unattributable energy → SOURCE_RESYNC_LOSS
never final-binding injection or silent discard

long worker delay:
all source segments through effectiveTick apply exactly once to valid H
no source energy becomes TIME_DEGRADED loss

chunk unload:
every binding settles to unload effectiveTick before removal
old-generation replay/publication cannot affect reload incarnation
```

### Time

```text
cells from different SolveEpoch values never exchange

one dimension has at most one in-flight epoch
and no queued catch-up epochs

sealed input watermarks must all be applied before solve/publication

delay within maxSolveDeltaTicks
→ latest sealed target with bounded dt

delay beyond maxSolveDeltaTicks
→ no oversized kernel dt
→ all source energy still applied
→ explicit TIME_DEGRADED + skippedTransportTicks/skippedPhaseTicks
→ thermal clock rebased without backlog
```

### Mutation / Queue Ordering

```text
same tick source move + Door/FluidState mutation:
old binding settles to t
→ state/fluid batch resolves
→ new binding applies only after t
→ solve sees one coherent sealed epoch

GeometryDeltaRing overflow:
live Page revision invalid immediately
→ FULL_GEOMETRY_RESYNC_REQUIRED survives full ring
→ bounded resnapshot eventually clears sticky flag

bounded dimension ready queue full:
IDLE/QUEUED/RUNNING mailbox retains sticky work
→ re-offer eventually succeeds
→ no duplicate concurrent dimension owner

main world continuously busy while Nether/End have work:
oldest-age promotion eventually runs every dimension

transition request/ACK overflow:
reservation and outcome retry per reservoir
→ unrelated reservoirs continue
→ no duplicate world mutation
```

### Boundary

```text
wall blocks airflow

exactly one confirmed thin wall conducts

thick rock is not treated as thin wall

two-block wall is never compiled as one stateless bridge
```

### Geothermal

```text
low Y alone does not heat air

exposed deep rock can heat air
```

### Snow

```text
Generator has no Snow block list

Air→Snow boundary transfers energy

phase mutation only rebuilds local4³ brick

TransitionMutationPolicy:
ambient RESPECT pauses at randomTickSpeed=0
machine IGNORE continues when profile declares it
```

### Chunk

```text
query never loads chunk

unload immediately invalidates lifecycleGeneration/publication
and disables cross-boundary transport

worker finishes old-generation task after reload
→ publication rejected

source/phase/stateful material is never ordinary cache eviction

non-equilibrium air eviction/unload
→ signed environment-exchange ledger exactly matches removed residual

three active dimensions
→ combined retained bytes obey server-global cap

reload safely rebuilds support
```

### Query

```text
published geometryRevision mismatch
→ mesh result rejected

any Page geometryRevision mismatch
→ Page-wide fallback; no V1 fine reuse

publication buffer ABA / rapid A→B→A reuse
→ seqlock retry or fallback; never mixed envelope/value

geometry swap + thermal publish + unload/reload stress
→ geometry/thermal/epoch/generation always from one envelope

cache hit
→ 0 world reads

steady cache hit and thermal publication
→ 0 allocation

PLAYER_BODY miss
→ surface/natural fallback, 0 mesh admission

PLAYER_HIGH_ACCURACY lease
→ admission controlled + TTL expiry

Natural miss with SharedNaturalFrame
→ no world read

Natural miss without frame on main thread
→ bounded lookup + explicit fallback flag

Natural miss without frame off thread
→ no wait/no chunk load; captured/config fallback only
```

### Radiation

```text
packed source index discovery
→ sources outside max range or below flux cutoff emit no ray

candidate/ray caps under dense source field
→ bounded work + RADIATION_BUDGET_LIMITED + lower confidence

BlockState/FluidState/dynamic exclusion mutation on witnessed section
→ SectionOcclusionRevision mismatch + bounded retrace

unloaded/unresolved section on DDA
→ conservative occlusion, no chunk load

repeat same player observation N times
→ identical sampled flux
→ source accumulator and RADIATION_EMITTED_J unchanged by query count
```

### Performance

```text
no source radius scan

no room scan

no global snow index

stable source
→ 0 topology work

baseline non-zero POWER_SOURCE Page
→ remains ACTIVE

10,000 / 50,000 passive crop calls
→ retained thermal state does not grow with crop count
→ QpassiveCalls and QlookupMisses still scale and are measured

continuous queue overflow with ordinary traffic
→ reserved recovery quota + oldest-age promotion converges

publication/source/mailbox resize
→ peak double-buffer bytes remain within global admission

inactive region
→ 0 persistent thermal state
```

---

## 86. Architecture Failure Conditions

如果 prototype 出现以下任一现象，应停止继续堆功能并重新评估架构：

```text
复杂基地大量退化成per-block ThermalCell

FarField必须扩展几十/上百个16³层级

一个block change需要扫描room/cave

material phase必须跨大量brick维护global topology

普通Air-Air必须建立永久edge graph

solver必须依靠长期backlog才能运行

必须引入大量per-edge clocks

100-player retained memory无法受hard cap控制

query cache hit仍然读取Minecraft World

source数量线性进入稳定solver热路径

GC成本主要来自thermal runtime对象创建

contextual resolver需要未声明World/BlockEntity访问才能工作

source overflow只能靠最终power/binding状态恢复

missed cadence必须把超出验证范围的dt交给thermal kernel

每个dimension必须永久占用一个OS thread才能维持single-writer
```

这些现象意味着实现正在重新滑回已经否决的架构。

### 86.1 新路径 failure-mode matrix

| Code path / realistic failure | Runtime containment | Required test | Observable result |
|---|---|---|---|
| common-path low-level hook 漏掉已知 raw write | 显式 adapter 置 sticky resync；fingerprint 只用于 GameTest/人工 debug；未知第三方路径按复现报告补 adapter | Phase 0a raw bypass + known adapter GameTest | hook coverage report、raw-bypass metric、Page stale flag |
| contextual resolver 在 chunk 边界缺少依赖，或尝试读 mask 外位置 | 返回 `UNRESOLVED`，禁用不确定 transport，不加载 chunk | unloaded boundary + out-of-mask fixture | query flag、unresolved resolver metric；不是 silent opening |
| conservative raster 把局部缝隙误判为整 tile opening | tile 不能完整证明为空时保持 closed/unsupported | randomized AABB union property test | unsupported/signature metric；不出现 false opening |
| Door 两半跨 Page，只有一个 delta 先到 worker | shared sealed watermark 阻止 solve/publication；相关 interface 保持 disabled | same-tick cross-Section Door test | stale/fallback flag 与 watermark lag |
| `GeometryDeltaRing` 满且事件无法传输 | main 立即 bump live revision，Page sticky full resync | forced overflow/recovery test | geometry overflow、full-resync count/latency |
| source on/off/rebind events overflow | 从最后 ACK 按 retained binding segments replay；cumulative 只校验 | normal-vs-overflow trajectory equivalence | replayed segments、cumulative mismatch；不双注能 |
| source history 已过期 | 只把缺失 interval 记为 `SOURCE_RESYNC_LOSS`，不向最终 binding 补 cumulative delta | forced history exhaustion | loss interval/count/J 与 source key |
| worker 延迟超过可验证 `dt` | 不执行 oversized kernel；完整应用 source energy，只跳过 transport/phase 并 rebase | delayed executor + source trajectory test | sample flag、sourceAppliedJ、两类 skipped ticks；无 time-loss J |
| dimension ready queue 满或主世界持续占用 | mailbox sticky re-offer、reserved recovery quota、round-robin + oldest-age promotion | queue-full + three-dimension fairness stress | re-offer、dimension age、starvation promotion metrics |
| reader 遇到 buffer ABA、geometry swap 或旧 chunk generation | seqlock 前后 version/generation 不同即 bounded retry/fallback | concurrent publication ABA + unload/reload stress | retry/ABA fallback metric；永不返回 mixed sample |
| eviction/unload 移除非平衡空气 `H` | 先写 signed `EVICTION_ENVIRONMENT_EXCHANGE_J`，source/phase/material 不走普通 eviction | positive/negative residual + global-cap test | reasoned signed joule ledger 与 energy residual |
| dense radiation source 或 ray witness 大量失效 | range/flux prefilter、candidate/ray cap、bounded retrace | dense-source discovery + revision churn test | budget-limited flag、confidence、candidate/ray/retrace metrics |
| transition request/ACK ring 满 | reservation/outcome 保留为 per-reservoir sticky retry | overflow + duplicate-delivery test | retry metric；不重复 mutation，不停全局 transition |

上述新增路径都必须同时具有测试、bounded containment 和 query flag/metric。若实现出现“无测试 + 无 containment + silent failure”的组合，视为 blocker。

---

## 87. 典型 Gameplay 行为

### 露天篝火

```text
Campfire
→ SourceRegistry
→ convection port
→ local AirCell
→ sparse Air transport
→ FarField

radiation
→ direct Player LOS
```

没有 radius scan。

### 隔墙篝火

```text
Campfire
   |
 Air A
   X
 Wall
   X
 Air B
```

Air 不直接穿墙。

如果墙导热：

```text
Air A
 |
MaterialBoundary
 |
Air B
```

慢慢传热。

Player radiation：

```text
0
```

### 普通房间

系统不知道：

```text
“这是一个房间”
```

只知道：

```text
空气 component
墙 boundary
门 opening
```

所以内部自然积热。

### 巨型洞穴

系统不知道：

```text
“这是一个洞穴”
```

而是：

```text
open interior
→ 8/16 cells

complex border
→ 4³ bricks

far frontier
→ continuation / FarField
```

### 一格竖井

```text
4³ Brick
→ 1×1 local air component
```

不会因为 coarse cell 消失。

热量逐 face 自然向上传输。

### Generator + Snow

```text
Generator
→ Pconv
→ Air mesh
→ Snow MaterialBoundary
→ local H
→ transition request
→ BlockState mutation
→ local Brick rebuild
```

Generator 永远不知道：

```text
SnowPatch
Snow block list
```

### 多 Generator

```text
GEN1
GEN2
GEN3
  ↓
same AirCell
  ↓
Pcell = P1 + P2 + P3
```

稳定 solver 不遍历三台机器。

---

## 88. Engineering Agent 实现规则

Codex 在实现过程中如果需要新增核心概念，必须回答：

1. **它解决了哪个已经被 benchmark 或 behavior test 证明存在的问题？**
2. **为什么现有 `Page + Brick + Cell + Boundary` 无法表达？**
3. **它增加多少 persistent bytes、hot-path operations 和 invalidation complexity？**

如果无法回答：

```text
不进入V1核心架构
```

实现时以下复杂状态机/ownership 点必须在对应 class 或 Mixin 顶部保留与本文一致的短 ASCII comment，并在行为变更时同 PR 更新：

```text
FacePatchIterator              negative-axis owner + coarse/fine patch decomposition
SourceEnergyLedger             event/segment replay + checksum-only resync
DimensionThermalMailbox        IDLE -> QUEUED -> RUNNING -> IDLE/QUEUED
QueryPublication              odd/even seqlock + envelope generation validation
section mutation Mixin         owner lookup -> delta/resync -> sealed watermark
Chunk lifecycle adapter        invalidate -> source settle -> generation-checked release
```

---

## 89. 最终 Definition of Done

V1 只有同时满足以下条件才算完成：

```text
Geometry correctness tests passed

Conservative shape raster proof and real Forge resolver census passed

Phase 0a mutation/lifecycle GameTest matrix passed

Energy invariants passed

FacePatchIterator exactly-once and coarse/fine area/distance tests passed

Pure LOD ΣH and world-geometry ingress/egress ledger identities passed

Source integral invariant passed

Geometry/thermal publication validation passed

Publication seqlock ABA + lifecycle generation stress passed

FarField fit + holdout reference errors within workload-specific gameplay tolerance;
unapproved buckets remain OPEN_CONTINUATION

No world read from worker

Query cache hit has zero world reads

Query cache hit and stable publication allocate zero bytes

Stable source causes zero topology work

Pane/fence local-region separation passed

Rmax census and 64*Rmax Brick bound passed

Door/Trapdoor/FenceGate rebuild resolved topology; explicit machine Gate only updates G

State + Fluid contextual resolver dependency-mask and unloaded-boundary tests passed

No exchange across different SolveEpoch values

Sealed input watermarks applied before solve/publication

Mid-cadence source changes and source rebind preserve exact integral P dt

Missed cadence creates no epoch backlog, never exceeds maxSolveDeltaTicks,
applies all source energy, and exposes TIME_DEGRADED plus skipped transport/phase ticks

All non-zero POWER_SOURCE Pages remain ACTIVE in V1

Geometry/source/transition queue overflow recovery converges from sticky owner state

Source resync replays retained binding segments in order;
cumulative emitted energy is checksum-only and never direct reinjection;
unrecoverable binding attribution is explicit SOURCE_RESYNC_LOSS

PLAYER_BODY is passive unless explicitly upgraded to PLAYER_HIGH_ACCURACY

V1 publication revision mismatch always uses Page-wide fallback

Per-dimension logical single-writer runs on bounded mailboxes/shared executor,
with fair re-offer, oldest-age promotion and generation-safe unload

Inactive world has zero mesh state

No source-radius scan

Radiation source discovery/ray work is bounded;
passive observations never mutate or duplicate source energy

No room/cave scan

No world snow index

Dimension and server-global memory hard caps enforceable;
source/mailbox/publication/radiation resize admission included

Non-equilibrium eviction/unload is explicit signed environment exchange;
source/phase/stateful material is never ordinary cache eviction

Work admission hard cap enforceable

Overload degradation observable

Resolver closure, unsupported signatures, rebuilding Pages, geometry/source resync,
geometry ingress/egress/residual, source replay/checksum/loss interval, sourceAppliedJ,
TIME_DEGRADED skipped transport/phase ticks, epoch watermarks/in-flight state,
actual/max solve dt, seqlock retry/ABA fallback, executor/dimension age,
recovery oldest age/starvation, signed eviction exchange, bounded radiation,
passive calls/lookup misses, allocation/GC, energy residual and queue metrics exposed

JUnit 5, Forge GameTest, JMH/JFR and production-like shadow layers passed in their owned scopes

Shadow benchmark completed

Workload-specific 100-player scenarios measured;
typical scenarios have zero source/resync loss and zero TIME_DEGRADED
```

只有在这些条件成立以后，才允许继续讨论：

```text
32³ macro cells
brick-local 2³ refinement
one-pole FarField
material radiation
fluid transport
advanced airflow
multi-worker solver
```

---

## 90. 最终架构图

```text
Minecraft main thread, end of tick t
      │
      ├── settle old source bindings to t
      ├── seal BlockState + FluidState mutations
      ├── validate section owner + lifecycleGeneration
      ├── bump SectionOcclusionRevision where needed
      ├── loaded-only resolver snapshot
      │       └── declared DependencyOffsetMask
      └── immutable batches + stream watermarks
                          │
                          ▼
             bounded shared thermal executor
                          │
             one logical writer / dimension
             one in-flight SolveEpoch / dimension
                          │
              apply all sealed watermarks
                          │
          ┌───────────────┴────────────────┐
          │                                │
          ▼                                ▼
resolved geometry IDs                 source ledger/rebind
          │                          retained segment replay
          │                          cumulative checksum only
          ▼                                │
 lazy 16³ ThermalPage                      ▼
          │                        NodePowerAccumulator
    ┌─────┴──────────────┐                 │
    │                    │                 │
8/16 homogeneous    mixed 4³               │
    AirCell       MixedGeometryBrick       │
    │              64 * Rmax atoms         │
    └──────────┬─────────┘                  │
               └──────────────┬─────────────┘
                              ▼
                             H/C
                              │
                   implicit Air-Air faces
                              │
    ┌───────────────┬─────────┼────────────────┬──────────────────┐
    │               │         │                │                  │
    ▼               ▼         ▼                ▼                  ▼
MaterialBoundary  FarField  UNKNOWN       explicit machine   TIME_DEGRADED /
 G / H / phase   impedance  DEGRADED       GateOperator       signed ledgers
    │               │                       G-only control       ledgers
    ▼               ▼
StateTransition  NaturalBackend
MutationPolicy

Door / Trapdoor / FenceGate
    └── state/fluid mutation -> dependency invalidation -> Brick rebuild

PhysicalSource
    ├── convection ports -> NodePowerAccumulator -> Air H
    ├── contact ports ---------------------------> MaterialBoundary
    └── radiation origins -> packed source index -> bounded RadiationService

Completed SolveEpoch
    ├── topology generation change -> PublishedGeometryPage
    └── thermal values ------------> preallocated double buffers
                                              │
                                  publicationVersion seqlock
                               geometry/thermal/epoch/generation envelope
                                              │
                                 Page-wide revision validation
                                              │
             ┌──────────────────────┬──────────┼───────────────┐
             ▼                      ▼          ▼               ▼
       NaturalBackend        AnalyticField  Cached Surface  Sparse Mesh
             └──────────────────────┴──────────┴───────────────┘
                                              ▼
                              primitive query + reusable sample
```

---

## 91. 一句话冻结定义

> **Frosted Heart V1 Thermal Runtime 是一个按需存在、局部拓扑有界保守、空气有限体积守恒、固体边界降阶、时间语义统一、硬预算驱动，并且绝大多数 Minecraft 世界永远不拥有 thermal state 的稀疏热学运行时。**

V1 的成功标准不是“能模拟更多东西”。

而是：

> **在不引入世界级拓扑、不引入 per-block thermal state、不依赖 backlog、不突破服务器预算的前提下，把玩家真正能观察到的局部热传播做正确。**

---

## 附录 A：已验证的当前实现基线

本附录来自误删前计划中已核对源码、生成数据和当时文档的部分。它只描述 `2026-08-22` 的迁移起点；其中旧区块热场、旧玩家采样和相关专属基准均已删除，不是当前可用后端。当前行为以 source 和 living docs 为准。

### A.1 What already exists

| Existing code/flow | V1 treatment |
|---|---|
| `WorldClimate` + `WorldTemperature.air/block` | 复用自然气候输出和兼容阈值，封装进 `NaturalBackend`；不把旧采样值重解释为 `H/P/C/G` |
| `BlockTempData` | 复用 datapack/legacy 表面贡献，进入 `CachedLocalSurface`；不生成 volumetric node |
| `SurroundingTemperatureSimulator` | 只作为旧行为/成本 baseline；不复用随机粒子、section copies 或 collision-shape shortcut |
| `TemperatureThreadingPool` | 复用 shared-executor 与请求去重经验；不复用当前 executor queue/lifecycle 作为新 runtime authority |
| 当时的区块热场 | 现已彻底删除；Boss/脚本/管理员按真实需求直接创建 `AnalyticField`，任何设备都不迁入解析后端 |
| `GeneratorData` / `HeatNetwork` | 保留 gameplay inventory/priority，通过 explicit profile 映射；不把旧 `power/heat/HU/tempLevel` 直接当 SI 单位 |
| random-tick / transition hooks | 复用 Minecraft 原生候选与现有玩法 recipe；local phase 另建 energy owner，二者不双重提交 |
| existing Forge GameTest run | 复用 `runGameTestServer` 基础设施，并把 namespace 扩到 `frostedresearch,frostedheart`；新增 thermal hook/lifecycle matrix |

迁移前实现不是统一热力学模型，而是数套相邻模型：

```text
WorldClimate + biome/dimension/altitude
                  |
                  v
          WorldTemperature.air/block
                  |
                  +--> ChunkHeatData 常值温度控制场 [已删除的迁移前路径]
                  |
                  +--> SurroundingTemperatureSimulator 随机粒子采样
                                      |
                                      v
                         PlayerTemperatureData 五部位体温

GeneratorData/HeatEndpoint/HeatNetwork
                  |
                  +--> 任意 heat 缓冲和 tempLevel
                  +--> 部分设备再创建 ChunkHeatData [已删除的迁移前路径]
```

- `SurroundingTemperatureSimulator.getBlockTemperatureAndWind` 会反复读取附近方块和碰撞形状；异步模式会复制若干 `PalettedContainer`。`computeBlockInfo` 对 `hasDynamicShape()` 直接使用 full shape，否则调用 `BlockState#getCollisionShape(null, pos)`，异常也回退 full；这证明 V1 需要显式 bounded resolver，而不能把当前空 context 调用误当精确几何。
- 默认 `simulationDivision = 10` 生成 `4,168` 个粒子方向，每个方向最多推进 `20` 轮，即一次查询最多 `83,360` 次粒子更新；还会清空 `32^3 = 32,768` 个 cell kind 并读取 `32^2 = 1,024` 个高度列。
- 异步构造最多复制八个 `PalettedContainer`；每个 worker 的 `WorkBuffer` 原始数组约 `164 KiB`，尚未计对象头、section 副本和任务对象。
- `TemperatureThreadingPool` 当前通过 `Executors.newFixedThreadPool(threadNum, ...)` 使用固定线程数、**无界 work queue** 的 global shared executor，并在同一维度、精确坐标和 `gameTime >> 6` 种子窗口不变时跳过提交；静止玩家通常最多每 `64` ticks 重算一次。V1 只复用 shared-executor/去重经验，不能复用无界 queue；新 runtime 使用 bounded dimension mailbox、per-dimension logical ownership 和 geometry/publication revision 精确失效。
- 旧日志曾记录约 `6.03 ms/query`、约 `1,237` 个不同方块访问；这不是可复现实验基线。
- 迁移前的 `WorldTemperature.air/block`、玩家环境采样、区块热场和热网没有共享能量状态。
- `BlockTempData` 中雪层/雪块、普通冰、浮冰、蓝冰/细雪、岩浆和岩浆块的默认贡献分别为 `-5`、`-10`、`-20`、`-30`、`1000`、`500`；它们是采样贡献，不是材料绝对温度或功率。
- `snow.json` 声明 `level_divide = true`，但现行缩放只处理 `LEVEL`、`LEVEL_COMPOSTER`、`LEVEL_FLOWING` 和 `LEVEL_CAULDRON`，不处理 `SnowLayerBlock.LAYERS`。兼容测试必须锁定实际行为。
- 当前岩浆条目未启用 `level_divide`，源岩浆和不同流动 level 使用相同的 `1000` 基础贡献。
- 当前“热上升、冷下沉”只是采样轨迹权重，不保存空气质量、焓或速度。
- `GeneratorData.power`、`HeatEndpoint.heat` 和 `tempLevel` 不能直接解释为 SI 功率、焦耳或摄氏度。
- `TemperatureUpdate` 当前在每个玩家 START tick 发送 `FHBodyDataSyncPacket`，即使主体温度没有每 tick 更新。
- `temperatureUpdateIntervalTicks` 与 `envTempUpdateIntervalTicks` 默认都是 `20` ticks；`envTempThreadCount` 当前与环境更新间隔重复使用配置 key `environmentTempMinTicks`。Phase 0 必须做配置加载兼容测试。
- 已删除的区块热场曾把一个 registration 复制到覆盖的每个 chunk；半径 `24` 的场按对齐不同约写入 `16..25` 个 chunk。该数字只说明删除理由，不定义当前解析场生命周期。
- 默认 random tick speed `r = 3` 时，作物随机刻调用率期望约为 `3 * loadedCropCount / 4096`：`10,000` 株约 `7.3/tick`，`50,000` 株约 `36.6/tick`；`CropGrowEvent.Pre` 还会再次查询。
- `ServerLevelMixin_TemperatureUpdate` 只遍历随机刻 section 元数据并抽取 `r` 个位置，不扫描每个 section 的 `4096` 方块。
- 同一 mixin 的随机刻路径受 `pRandomTickSpeed > 0` 门控，并在 BlockState 未被温度转换接管时继续读取、随机 tick `FluidState`；V1 mutation/resolver 合同因此必须显式包含 FluidState，同时把是否遵守 gamerule 留给 transition profile。
- 单块至少一次被抽中的每 tick 概率为 `p = 1-(1-1/4096)^r`；`r = 3` 时平均约 `1/p = 1,366` ticks，即 `68.3 s`。这是兼容节奏，不是每块每 tick 更新。
- `BlockStateBaseMixin_RandomTick` 会让具有 `StateTransitionData` 的常见地形状态进入随机刻候选。大量 `will_transit = true` 的泥土、冰、雪和冻土使成本上界包含 `O(Sforced*r)`；V1 ambient sampler 接管后应移除这项全局强制。
- `IceBlockMixin_Melt#randomTick`、`ThinIceBlock#randomTick`、`LayeredThinIceBlock#randomTick` 当前为 no-op。仅有温度副作用的状态可在迁移 feature flag 下退出强制随机刻；仍有其他随机刻副作用的状态不能这样处理。
- `SchedulerQueue` 面向 `BlockEntity`，不适合作为 thermal scheduler。城镇 scanner 的室内上限为 `4,096` 个位置，并可能对每个空气体素调用 `WorldTemperature.block`；仅把底层查询改成 `O(1)` 不能消除重复扫描。
- `StateTransitionData.heat_capacity` 默认 `1`，当前只是约 `1/heat_capacity` 的尝试概率因子，不保存质量、能量或潜热，永远不能重解释为 `J/K`。
- 通用状态变化在候选方块达到阈值后直接 `setBlockAndUpdate`，不会从空气或热源扣除能量。
- 水面结冰单独执行：每个到期 ticking chunk 默认每 `20` ticks 抽一个 heightmap 地表列，在 `WATER_FREEZES = -5 degC` 以下从边缘逐步形成薄冰/分层薄冰。
- `PhysicalState` 与 recipe 目标是玩法状态机语义，不必等同现实物相；例如 `minecraft:ice` recipe 的 `liquid` 标记服务于既有 thin-ice 转换链。
- 当前 production gameplay runtime 仍不创建或查询 Phase B correctness prototype 的 `ThermalPage`；实际 `MixedGeometryBrick` arena、`ThermalCell`、`BoundaryOperator`、材料焓热库和守恒相变也尚未实现。任何 V1 prototype 名称都不能反向写成现有玩家温度行为。

---

## 附录 B：保留的兼容与迁移合同

### B.1 量纲和游戏时间

| Symbol/API | Meaning | Unit |
|---|---|---|
| `T` | 绝对温度 | `degC` |
| `DeltaT` | 温差 | `K`；数值上与摄氏温差相同 |
| `H` | 相对固定参考温度的焓/能量 | `J` |
| `C` | 集中热容 | `J/K` |
| `P` | 连续热功率，冷源为负 | `W = J/s` |
| `G` | 导热或等效换热导纳 | `W/K` |
| `qRad` | 辐射热流密度 | `W/m^2` |
| `cEffective` | gameplay-calibrated 体积热容 | `J/(m^3*K)` |
| `HU` | 旧热网库存量 | 独立 gameplay unit |

```text
DeltaTimeSeconds = elapsedGameTicks / 20.0
H = C * (T - Tref)
T = Tref + H / C
```

TPS 下降只让现实时间中的模拟变慢；不能按墙钟补发额外能量。旧 `HU`、`power`、`heat`、`tempLevel` 必须通过设备 profile 显式映射，V1 不全局声明 `1 HU = N J`。

### B.2 查询与玩家

- production hot path 使用 primitive 参数和调用方复用的 `MutableThermalSample`，至少保留 `airTemperatureC`、`radiantFluxWPerM2`、`surfaceFluxW`、`medium`、`confidence`、`sampleTick` 和 flags；publication/surface/caller-frame miss path 不读世界，main-thread Natural fallback 只允许 bounded loaded lookup。所有 miss 都不等 worker、不加载 chunk、不自动创建 mesh。
- 第一迁移阶段不改现有五部位玩家体温、衣物、设备、食物、效果、HUD 阈值和基础更新节奏；先替换环境输入，再用兼容测试校准映射。
- 普通 `PLAYER_BODY`/HUD read、作物、普通 block tick 和 Town aggregate 只能读取已有 publication/surface/natural；玩家只有显式申请 admission-controlled、带 TTL 的 `PLAYER_HIGH_ACCURACY` 才能创建 mesh。
- `SharedQueryFrame` 可复用同 tick 的自然 bucket、解析场 generation 和 page publication；作物不能拥有永久 binding，Town projection 不能维持 mesh lease。
- 玩家网络同步应从每 tick body packet 迁移到 dirty mask、量化阈值和 heartbeat，但必须保持登录、重生、维度切换和玩法提示时序。

### B.3 `BlockTempData` 与材料

- 迁移期间保留 `temperature`、`level_divide`、`must_lit` 的实际兼容输出；这些字段只服务 `CACHED_LOCAL_SURFACE`/legacy 适配，不自动生成 `P/H/C/G`。
- 材料 profile 必须把 legacy 表面贡献、无状态 boundary、有限热容 surface 和 phase reservoir 分开声明。
- 大型冰雪、岩石和岩浆不能退化成逐方块 emitter 或 volumetric node；V1 使用确认界面、surface/deep boundary 和 Brick-local phase state。
- 未进入局部能量路径的天然材料继续使用 `AMBIENT_KINETIC`；已由 Brick-local reservoir 接管的材料使用 `LOCAL_ENERGY_ACCOUNTED`。同一方块同一时刻只能有一个 owner。

### B.4 状态变化

- 原生 random-ticking 状态继续复用 Minecraft 候选；非原生 terrain recipe 使用到期 ticking chunk 的常数个 surface/subsurface 候选，不扫描 section，不为每块材料建 timer/deadline。
- `TransitionMutationPolicy.RESPECT_RANDOM_TICK_SPEED` 的 ambient/compat 状态变化在 `randomTickSpeed = 0` 时暂停；机器能量驱动 profile 可以显式使用 `IGNORE_RANDOM_TICK_SPEED`，gamerule 不是 thermal core invariant。
- local phase request 必须先预留能量，主线程验证 state/generation/interface 后提交，worker 处理 success/rejected ack；每个 reservoir 至多一个 outstanding request。
- 雪消退可以使用 `ENERGY_GATED_REPLACEMENT`；冰水等可逆过程使用连续/分段焓 profile。蒸发、凝结、升华在没有质量输运前仍是 legacy 状态机，不宣称质量守恒。

### B.5 Generator、Boss、热网和生命周期

- Campfire、Generator、Radiator 与 Fountain 只使用 emission ports、source accumulator 和独立辐射；不得进入 `AnalyticFieldBackend`，也不存在第二份区块热场。
- Boss、脚本与管理员场保持解析控制语义；移动只更新 field bounds/generation，不重编地形。
- 第一版不重定义现有 `HeatNetwork` 的库存、优先级、`tempLevel` 或设备续航玩法。
- chunk load 只恢复明确 BlockEntity/source，不扫描普通材料；unload 立即使跨边界 transport 与 publication 失效。V1 不要求局部 `H` 跨 unload/restart 持久化。
- datapack/profile reload 必须递增 generation，使旧 signature、source binding、boundary 和 publication 失效；查询不能因 reload 同步重建世界。

---

## 附录 C：Phase 0 基准、容量与竞争原型

### C.1 必测负载

Phase 0 固定目标硬件、JVM、模组列表、视距、simulation distance、random tick speed 和服务端配置，并分别记录：

| Workload | Required outputs |
|---|---|
| `1 / 10 / 50 / 100` 户外玩家 | 主线程/worker p50、p95、p99、world reads、publication age、allocations |
| `100 players / 10 shared bases` | page/cell/brick sharing、source accumulators、QueryFrame、Town projections |
| `100 players / 100 bases` | retained bytes、global/dimension admission refusals、sleep/wake latency、hard-cap behavior |
| 高速探索 | cold/resolver reads、fallback ratio、in-flight/watermark lag、`TIME_DEGRADED`、skipped transport/phase ticks、sourceAppliedJ、无主动 chunk load |
| 动态门/活塞/流体基地 | state/fluid deltas、dependency closure、Brick rebuilds、dynamic exclusion、Page fallback、seqlock retries、publish bytes |
| `100` 稳定与 `100` 变化 source | registry/event cost、稳定 topology work、`integral P dt` 误差、segment replay/checksum、overflow loss interval |
| 玩家周围高密度 radiation sources | section candidates、flux rejects、rays/player、cache retrace、budget-limited confidence |
| `10,000 / 50,000` 作物 | `QpassiveCalls`、`QlookupMisses`、唯一 frame/lookup regions、无 crop-owned state |
| 稳定/变化城镇建筑 | scanner 成本、projection group refresh、结构 revision rebuild |
| 雪原/地下冰层/普通地形 | loaded sections、native/forced random-ticking sections、ambient candidates |
| 主世界/下界/末地同时活跃 | mailbox wait/age、fairness promotion、server-global bytes、generation-safe unload |

历史迁移曾记录旧粒子步骤、section 副本、Generator chunk registrations、body packet burst、区块热场候选、作物和城镇隐藏调用。旧系统已经删除，后续性能结论直接以当前 runtime 的 retained heap、allocation/GC、结果误差、陈旧度和 tick time 为准，不恢复旧实现做重复对照。

### C.2 V1 成本模型

定义：

```text
Ppage       active + sleeping ThermalPages
N4/N8/N16  各 LOD ThermalCells
Bmix        MixedGeometryBricks
Oboundary   BoundaryOperators
Fface       本周期 implicit Air-Air face operations
Sreg        source registry entries
DeltaS      本周期 source changes
Aphase      active local phase states
Qrequest    ready/queued phase requests
Mmiss       本周期获准 cold admission 的新 interest
Qepoch      输入 epoch 变化后必须重新合成的查询
Uframe      本 tick 唯一 SharedQueryFrame keys
Gtown       retained TownThermalProjection groups
Rresolve    本周期需要重新运行的 signature resolver centers
Uread       resolver dependency closure 去重后的 loaded snapshot positions
Qresync     sticky geometry/source/transition recoveries
Dinflight   当前有逻辑 in-flight epoch 的 dimensions
QpassiveCalls 本周期所有 passive consumer 调用
QlookupMisses passive query 的 page/surface/frame miss
NtickingChunks 本周期参与 Minecraft ticking 的 chunks
Rcandidate   radiation candidates after section lookup
Rray         actual radiation DDA rays
```

```text
main demand ~= O(state/fluid deltas + Rresolve + Uread + phase validations
                  + NtickingChunks + ambient candidates
                  + Rcandidate + Rray + publish bytes)

worker demand ~= O(brick compiles * (64 * Rmax)
                    + admitted cell/face/boundary work
                    + DeltaS + Qresync + active phase accounting)

query demand ~= O(QpassiveCalls + QlookupMisses + Qepoch
                   + Uframe + Gtown refresh)

memory ~= O(Ppage + N4 + N8 + N16 + Bmix + Oboundary
             + Sreg + Aphase + preallocated publication buffers
             + bounded source binding history + dimension mailboxes
             + radiation index/witnesses + bounded caches)
```

retained thermal state 不按世界冰雪总体积、作物总数或普通 passive machine 总数直接增长；这不代表 CPU 与它们无关。每次作物/普通消费者调用仍进入 `QpassiveCalls`，miss 进入 `QlookupMisses`，Minecraft tick/ambient candidate 成本至少受 `NtickingChunks` 影响。真实改变的方块数也有不可消除的提交成本，因此 `phaseMutationBudget` 必须独立限制。

### C.3 原始字节核算工作表

以下只作为 primitive SoA/JOL/JFR 的起始核算项，不是冻结容量承诺：

| State | Initial raw target or measurement requirement |
|---|---:|
| thermal cell H/C/support/flags | measure raw bytes per admitted cell |
| `NodePowerAccumulator` P/pending/time | measure raw bytes per powered node; never charge every cell |
| packed source registry entry | `24..48 B/source` |
| Brick-local phase span incl. one candidate | `32..64 B/state` beyond boundary state |
| `SharedQueryFrame` | `32..80 B/entry` |
| Town projection group | `24..64 B/group` |
| page/brick/boundary/publication arenas | measure retained bytes and allocator slack by size class |
| publication storage | include topology-generation geometry storage and preallocated thermal double buffers |
| mailbox / ready queue / sticky recovery | include configured capacity and resize peak even when mostly empty |
| radiation source index / occlusion revisions / witnesses | measure per indexed source, revision entry and active ray |
| fixed rings and page tables | include configured capacity even when empty |

`< ~8 MiB` 只作为 Phase 0b 可评估的 provisional target，例如“`100 players / 10 shared bases`、固定 source/page 数与三维度分布”的一个明确 workload；未附场景参数的数值无验收意义。per-dimension 与 server-global hard cap 都由 workload table 冻结。达不到时先降低 optional admission/refinement 或回退 surface/background，不提前引入 `32^3+`、partition scheduler 或模态压缩。

### C.4 必须并列的原型

| Prototype | Purpose |
|---|---|
| `CachedAnalyticSurface` | 最低成本兼容基线；无空气积热和跨空间运输 |
| `SparseThermalRuntimeV1` | 本文 Page + Brick + Cell + Boundary + uniform time 主候选 |
| `PerBlockSparseGraph` | 受控规模反例，量化逐块节点在密集材料下的成本 |
| `ReferenceFiniteVolume` | 离线精度/能量参考，不进入生产服务器 |

四者使用相同场景、材料参数、source event 序列和 query outputs。V1 只有在需要的玩法能力存在、误差在批准容差内、且主线程/worker/内存预算都通过时才胜出。

---

## 附录 D：误删前架构的整合与替代映射

| 旧计划概念 | V1 处理 | 结论 |
|---|---|---|
| `NaturalBackend` | 保持自然气候、天然空气和岩层边界 | 保留 |
| `AnalyticFieldBackend` | Boss/脚本/管理员单份解析控制场；设备禁止进入 | 保留并补回顶层架构 |
| `CACHED_LOCAL_SURFACE` | legacy `BlockTempData` 和无历史局部表面 | 保留为执行层级 |
| interest 权限与被动消费者 | source/stateful machine 可 admission；crop/town/HUD miss 不创建 mesh | 保留 |
| receiver-driven discovery | Page/Topology Guard 只沿获准 interest 和 open frontier 获取最小支持 | 原则保留，机制替换 |
| `4^3 GeometryRevisionMicrotile` | `4^3 MixedGeometryBrick` + `int resolvedSignatureId[64]` + conservative raster/local component compile | 收敛为 V1 有界保守几何近似，不宣称任意 VoxelShape 精确 |
| `MaterialReservoirAggregate` / `SurfacePatch` | `STATELESS_CONDUCTANCE`、`CAPACITIVE_SURFACE`、Brick-local `PHASE_RESERVOIR` | 替换；禁止跨 Brick global patch |
| packed source registry / node accumulator / `integral P dt` | `SourceRegistry`、emission ports、independent `NodePowerAccumulator`、精确积分 | 完整保留 |
| `ThermalQueryBinding` | Page coverage lookup、immutable publication、可选 consumer hint、`SharedQueryFrame` | 简化；不为作物逐个绑定 |
| `TownThermalProjection` | bounded weighted groups，结构变化时重建 | 保留 |
| `AMBIENT_KINETIC` / `LOCAL_ENERGY_ACCOUNTED` | ambient sampler 与 Brick-local phase owner 互斥 | 保留并改为 Brick ownership |
| `ThermalSemanticGraph` / exact reducer | V1 stable runtime 不编译或解释通用 RC graph | 移出 V1；只可作为离线 reference 研究 |
| `CompiledThermalPlan` / plan cache / affine fast-forward | uniform V1 timestep + pair/boundary kernels + active/sleeping | 移出 V1，不提前实现 |
| `IslandRuntime` | 连续局部 face exchange，不维护热岛身份 | 删除 |
| `PartitionRuntime` / partition publication | `ThermalPage` 生命周期、`PublishedGeometryPage` 与预分配 `PublishedThermalBuffers` | 删除并替换 |
| `ConservativePortalEdge` / per-edge clock | canonical face traversal + common time invariant | 删除并替换 |
| fixed guarded partition buckets | V1 bounded shared executor、每维度 logical single-writer/single in-flight、统一 thermal interval、hard active-state/face caps | 删除并替换 |
| `TopologyMigrationPlan` | Brick overlap、cell split/merge 和 generation-checked local migration | 保留守恒目标，删除通用图迁移器 |
| `DORMANT_CAPSULE` / modal compression | 不进入 V1 | 明确后置 |

旧计划仍然有价值的部分已经进入附录 A、B、C 和上述保留项；与 V1 硬 invariant 冲突的旧实现细节不得以“兼容”为由重新引入。

---

## 附录 E：原型问题、文档影响与 Outcome

### E.1 必须由数据回答

1. `cEffective`、`G_TABLE`、薄墙/厚墙 profile 如何校准，才能保持现有 BlockTemp 与玩家体感节奏？
2. `4/8/16` thermal refine/coarsen 的误差阈值和迟滞应取多少？
3. `STATIC_IMPEDANCE` FarField 按 level、opening、orientation、wind、topology class 的参数如何由 reference domain 拟合？
4. 哪些材料只允许 `CACHED_LOCAL_SURFACE`，哪些明确需要 `CAPACITIVE_SURFACE` 或 `PHASE_RESERVOIR`？
5. 雪、冰、含冰冻土的有效质量、显热、转换能量、deep boundary 和可逆映射如何校准？
6. uniform thermal interval 在 `1 / 5 / 10 / 20` ticks benchmark 下如何权衡误差与成本？首个集成候选为 `5 ticks`；`maxSolveDeltaTicks`、bounded substeps 与允许跳过的 transport/phase 时间必须由最大合法 `G*dt/C`、pair/boundary/phase kernel 误差和目标负载共同决定，source energy 不属于可跳过项。
7. `UNKNOWN_DEGRADED` 的显式损失如何计量和暴露，才能避免 energy debt 又保留可观测性？
8. source port 全部堵塞时，各 profile 选择重分配、机器内部 reservoir 还是 external loss？
9. `PLAYER_HIGH_ACCURACY` 的 admission、TTL、sleep/wake threshold 和公平区域 token 如何设置？
10. `[当前实机答案，待校准]` 玩家 `radiantFlux` 按 `0.7 m²` 投影面积、`0.8` 吸收率、实际更新秒数和 `5,000 J/K` 有效热容直接形成五部位 body delta；另按 `6 W/m²/K` 线性化系数形成已有 HUD 体感，HUD/状态阈值不改。
11. primitive arena size classes、空闲页回收、publication 双持和 hard cap 在目标 JVM 上的真实 retained bytes 是多少？
12. 各 workload 的 retained-memory/TPS gate 是多少；`< ~8 MiB` 对哪一个明确的 players/bases/sources/dimensions 场景有意义，哪个 optional admission/refinement 旋钮收益最高？
13. ambient candidate cadence 如何在移除 `Sforced*r` 后保持旧自然冻结/融化节奏？
14. `temperatureUpdateIntervalTicks`、`envTempUpdateIntervalTicks` 和重复配置 key 的兼容迁移方案是什么？
15. 首版 unload 以 signed environment exchange 结算 transient `H` 对玩法是否可接受；若不可接受，后续 persistence 的容量和恢复误差合同是什么？
16. 已启用 vanilla/modded BlockState + FluidState 的 local-air-region census 是什么，生产 `Rmax` 应取多少，哪些 signature 会进入 unsupported fallback？
17. `SELF_ONLY / NEIGHBOR_6 / NEIGHBOR_26` 各自覆盖哪些方块；`5^3` mutation closure、`6^3` cold-build halo 在真实基地的 resolver read p95/p99 是多少？
18. 哪些 moving piston、entity 或 BlockEntity-driven shape 必须保持 V1 dynamic boundary exclusion，玩家可见误差是否需要专门 profile？Create 已冻结为移动期按空气，不进入此参数研究。
19. source-heavy benchmark 是否证明 V2 需要“单 node + fixed boundary”的 source-sleep prototype；若需要，其 fixed-point epsilon、mandatory revalidation 和正常 boundary exchange 账本如何定义？
20. source resync 的 per-binding history 应保留多少 segment/多少 tick，才能让 `SOURCE_RESYNC_LOSS` 在压力测试中接近零且内存有界？
21. compiler-emitted query dependency footprint 的 retained bytes、rebuild cost 与减少的 Page fallback 比率，是否足以支持 V2 重新评审 fine publication reuse？V1 答案固定为否。
22. shared executor 的 thread count、dimension fairness 和 in-flight age hard threshold 在主世界/下界/末地同时活跃时如何设置？
23. 哪些现有 transition profile 使用 `RESPECT_RANDOM_TICK_SPEED`，哪些新机器 profile 明确使用 `IGNORE_RANDOM_TICK_SPEED`？
24. publication seqlock 的实际 retry/ABA fallback p95/p99 是多少，是否需要超过一次 bounded retry？
25. `[当前实机候选，待 workload 复核]` radiation 使用 `16 blocks / 0.1 W/m² / 64 visits / top-8 / 24 rays / 256 DDA steps`；多人 TPS/误差证据决定是否调整。
26. equilibrium eviction epsilon、dimension cap 与 server-global cap 在三维度压力下如何冻结？

只有 benchmark 证明 V1 基础结构存在实际瓶颈后，才讨论 `32^3+` macro cells、Brick-local `2^3` refinement、one-pole FarField、材料辐射、流体 transport、多 worker 或高级 airflow。

### E.2 文档影响

本文仍在 `plans/`，不得作为当前生产行为引用。当前 gameplay 已接入新 runtime，三后端 compositor、physical source 与旧区块热场删除结果已同步到 living docs。每个后续生产阶段落地时继续同步更新：

- `docs/climate/world-climate-and-temperature.md`：自然后端、解析场、mesh 合成、FarField 与地热；
- `docs/climate/player-temperature.md`：环境查询、辐射、异步 publication 和网络兼容；
- `docs/climate/heat-production-and-network.md`：source profile、`P/H/C/G` 与旧 `HU` 映射；
- `docs/climate/data-lifecycle-and-integration.md`：signature、Page/Brick/cell/boundary、mutation、ownership、线程、预算、配置、reload、query publication 和 metrics；
- `docs/climate/README.md`：实施完成后的系统入口。

### E.3 Outcome

`2026-08-26` 已实现稳态调度与常驻内存收缩：topology drain 直接维护 `topologyDirty`，稳定 frame 不再线性扫描全部 Page 寻找 dirty state；稳态每 5 ticks 合并一次求解，input event 可通过 urgent frame 提前启动，equilibrium sleep 不再因 unresolved frontier 永久失效。Page 的 desired signature cut 改为 dirty-only 生命周期，publication 改为 `256` 槽起步并按 arena admission 增长。普通游戏不再为每个 `LevelChunkSection` 注入 probe owner/counter；随后 Phase 0 reference/census/events、probe Mixin、resolver census、自洽 evidence 测试和全部 JMH/JFR/JOL 包装均从源码与 Gradle 构建删除，只保留 `gameTest` 中 `12` 条真实 geometry/runtime 场景。未重写 `ThermalCellArena`、source timeline、radiation 或 FarField scratch。

`2026-08-26` 已完成顶层后端收敛：`WorldTemperature.air/block` 只合成 natural backend、revision-valid sparse mesh 和 analytic control fields；旧区块热场 capability、形状类、同步失效包、周期 revalidation 与专属 JMH/JFR/JOL 均已删除。Campfire、Generator、Radiator 与 Fountain 只由 `MinecraftPhysicalSourceManager` 注册为 physical source，Boss/脚本/管理员控制效果只使用 `AnalyticField`，不存在设备排除表、shadow backend 或双重热源路径。红外直接读取 analytic field 与 physical source 快照，保持按视图区块变化请求，不增加周期轮询或 source-to-chunk invalidation 表。误改为 `SLUDGEBLOCKS` 的标签常量已恢复为既有 `SLUDGE`，避免改变 `frostedheart:sludge` 资源 ID。普通材料分类已删除全部 `SoundType` 推断和 air 特判，只使用明确 block tags，未命中标签的状态按 `blocksMotion()` 自然落入 generic solid 或无 profile。Java 17 `compileJava` 与四组定向 JUnit 共 `29/29` 通过，release/deobf JAR 均构建成功；两个 JAR 都包含真实 thermal events/Mixin，且配置排除的验证条目计数均为 `0`。`git diff --check` 通过，仅有既有行尾警告。

`2026-08-25` Phase 0 当前为 `0a complete / 0b partial`，Phase A 的 PR 1/2、Phase B 的 PR 3 Page/ownership foundation、Phase C regular Air Mesh correctness foundation、Phase D combined PR 4+5 solver/source correctness foundation和 PR7 runtime correctness 均为 `complete`；Phase E PR6 为 `implemented / approval pending`，Phase H / PR10 的非相变材料仍 dormant，Phase I / PR11 热侧候选及 Phase F/G/J/K 的玩家空气、source、crop 与 town 路径已进入实机测试 authority：

- `0a` 已证明五参数 `LevelChunkSection#setBlockState(..., boolean)` 能捕获 common-path GameTest 中的 `setBlockAndUpdate`、直接 chunk/section write、water flow/waterlogged、Door/Trapdoor/FenceGate、递归 Sponge 写入和 moving piston；unmapped worldgen、off-thread sticky resync、raw palette debug fingerprint、generation/publication rejection、synthetic dynamic exclusion 与真实 ticket load/unload/reload 也已执行。真实 Create bearing 已证明 assemble 产生 `stone -> air`、移动期不产生热几何 delta、disassemble 在目标位置产生 `air -> stone`，因此 Create 不需要移动 adapter 或 exclusion。`DebugCommand restore_backup` 现已显式作废旧 section owner、在同一 chunk generation 下绑定 replacement identity，并要求 ACK 前 full resnapshot；raw block/biome container notifier 也有独立 sticky reason。resync 开始时捕获 section identity、lifecycle generation、required revision 与 reason，ACK 只 CAS 清除同一个 requirement，旧 R1 重建不能清除期间产生的 R2。21-runtime 穷举清单与旧断言仅以注释保存，不参与 gate；`/resetchunks` 是延期管理命令兼容项，若以后支持则丢弃整区旧 thermal Page 并懒重建。按这一冻结范围，`0a` 已通过。
- `0b` 已把 SI 单位、source 积分、解析交换、迁移账本、workload 分类、legacy/shadow route 和 benchmark-evidence provenance 变成 `29` 条可执行 JUnit，并列出 `14` 个 workload descriptor；首轮 Java 17 本机 legacy query JMH/JFR、allocation、隔离 retained-object-graph 与一次 Forge GameTest player-constructor capture 也已取得。Forge diagnostic 同时记录 legacy constructor 会把远端缺失的 `4/4` footprint chunks 同步加载；这里只保留事实，不继续优化旧 sampler。该证据仍不含生产模组列表的多人 workload 或整服 retained heap，四候选也尚无完整可执行实现，因此 acceptance gate 仍未冻结。
- Phase A 已实现 conservative geometry/Brick kernel、bounded dependency/snapshot core、真实 `VoxelShape` adapter、generic state-static resolver、explicit/contextual dispatcher、loaded-only capture、Vanilla fixtures、两代 reload prototype 与 JMH/JFR/JOL evidence。最终 census 的 `84,147` states 中 `82,210` resolved，得到 `262` 个完整 signatures、`259` 个唯一 geometry patterns 和 `2` 个 contextual outputs；最大观测 local regions 为 `4`。未注册 dynamic 与 moving piston 保持 observable unsupported/unresolved，correctness IDs 仍为 `int`。
- Phase B 已实现 section-aligned `ThermalPage`、固定 `int coverageRef[64]` 与 4/8/16 coverage width、`73` 个 primitive geometry summaries、`mixedBrickMask`/`dirtyBrickMask`、无 `128-cell` 上限的 dense arena spans，以及 live/published `long` revision 分离。active mutation O(1) 失效旧 publication 并 materialize/mark base Brick；同 tick 同 Brick 的 voxel changes 合并成一个 bounded primitive ring delta。overflow 使用 Page-owned sticky `FULL_GEOMETRY_RESYNC_REQUIRED`，新 mutation 会推进 required revision，旧 token 不能清除新 requirement；粗 support 在完整合法 coverage repartition 前禁止 publication。
- `FacePatchIterator` 已冻结 world negative-axis ownership，`16↔16`、`16↔4`、`8↔4` 在 X/Y/Z、粗 cell 位于任一侧及负坐标/跨 Page 时都产生唯一 canonical key、精确 overlap area 与 `d = halfWidthA + halfWidthB`。`GeometryMigrationLedger` 现直接拥有 pure LOD 与 overlap 公式，Phase 0 reference 反向调用它；signed ingress/egress/residual 包含完整空气消失和从零创建的边界。
- Phase C 已实现 primitive `ThermalCellArena`、规则 4/8/16 与 compiled mixed-component `H/C` state、Page-owned dense-span replacement、transactional pure-LOD split/merge、caller-owned O(1) published coverage query，以及不持久化 generic edge 的 concrete pair compiler。mixed support 直接使用 `CompiledBrick` ports，不再保留 resolver callback；旧、新 Page span 在 commit 前双持，旧 coverage 不会指向已释放 cell。
- Phase D 已把原 PR4/PR5 合并成一个 arena-native 具体执行路径：source timeline 把完整 `integral(P dt)` 直接写入 `ThermalCellArena`，随后 arena-bound `ThermalSweep` 执行 transport。source 与 sweep target generation 都在 mutation 前验证，executor 拒绝双 arena；Minecraft geometry/source frame producers 和 gameplay adapters 仍未接线。
- Phase E 已实现 loaded-only `TopologyGuard`、不可把 candidate 当 approved 使用的 `FarFieldProfileRegistry`，以及独立 RK4 explicit-domain 与 analytic static-impedance 对比的 `FarFieldReferenceHarness`。fit 与 holdout 严格分离，四项 holdout observable 和 signed envelope 都进入 gate；标准 synthetic matrix 覆盖四种 topology、两个 wind bucket 和 `1/10/100 kW`。gameplay 当前只启用已经过 reference holdout 的 open-space 基础阻抗：Page 天空截面允许真实露天单出口闭合，开放方向数不再作为 outdoor proof；各维度只替换自然温度，风力连续缩放导纳。地下 continuation 只使用同一 approved profile 的距离衰减弱边界，并始终保持 degraded。
- PR7 已实现 `ThermalMemoryBudget`、`QueryPublication`、`DimensionThermalRuntime` 与 `ThermalRuntimeCoordinator`。两级 critical/optional admission、preallocated seqlock double buffer、worker-start revision envelope、single-writer/latest-only execution、whole-set conservative sleep、fixed ready queue、sticky re-offer、recovery quota、age promotion 和 generation-safe unload 已进入具体代码；没有第二份 `H/C`、resolver callback、Page partial sleep 或无界 executor queue。
- PR8 已在显式 interest/admission、loaded-only resolver、shared signature IDs、bounded primitive ring 与五流 frame 之上增加唯一的 `MinecraftThermalTopologyApplier`。常见增量 mutation 与 independently capped full-Page `int[4096]` resnapshot 都能重建 regular/mixed/no-air Page、以 sparse microcell overlap 守恒迁移 arena `H`、生成 canonical pair sweep、原子安装 sweep + non-source ACK，并在安装后释放旧/退役 Page span。`enableDispatch` 只保留调用者提供的串行 `Executor` 边界；当前 gameplay 同步执行。source gate 只保活实际被 live/queued port 引用的待替换 span；稳定 frame 复用已安装 sweep。玩家/物理 source 的直接地下 Page 可带入一层 `getChunkNow` continuation，总量固定上限 `64`，continuation Page 不递归扩张。
- Phase G / PR9 已增加 Campfire/Generator/Radiator/Fountain 的 frozen power/port profiles、dirty-only main-thread producer、bounded cold Page ownership、exact face-component binding、blocked/degraded sinks、unload/revival 和 topology-cut preapply/recovery。source 起点事件先走 zero-cut；post-preapply deferred frame 以 empty unresolved sweep ACK 并继续 solve，避免重复注能或卡住 single in-flight epoch。旧 heat/power/tempLevel 仍只表示设备与热网玩法库存；首次玩家温度查询建立 runtime 后启用 physical source manager，所有设备空间产热只走这一条路径。
- Phase H / PR10 已增加 immutable `MaterialBoundaryRegistry`、独立 `4x4x4` contact mask、完整单格 `STATELESS_CONDUCTANCE` bridge、sparse `CAPACITIVE_SURFACE` 与 `NATURAL_ROCK` surface/deep pole。material `H/C` 与空气共用 Page-owned `ThermalCellArena` span，稳定 key 参与同一 sparse migration；两格厚墙无 shortcut，地热只通过 exposed rock natural boundary，邻区变化只 dirty 直接相邻 Page。gameplay registry 当前只含 Phase I profiles，非相变材料参数尚待 reference/gameplay 校准。
- Phase I / PR11 已增加 Brick-local `PHASE_RESERVOIR`、candidate mask、潜热阈值、单 outstanding request、固定 request/ACK rings、per-reservoir sticky retry、generation-safe ACK、committed latent ledger、重建迁移和精确 main-thread interface revalidation。gameplay profile cut 从 `StateTransitionData` 一次编译热侧规则，mutation 复用原目标方块链并按阈值逐阶段扣潜热；同阈值保持 gas 优先，`ICE_DO_NOT_SMELT` 群系内的冰 mutation 暂缓且不扣潜热。只有 applied Page candidate 屏蔽旧热侧分支，冷侧与无法编译的状态保留 legacy authority。datapack reload 会关闭旧 cut 并在下一次 query 懒重建。当前生产 registry 把 `707` 个热侧 BlockState 去重为 `6` 个共享 profile、`4` 个 contact pattern，`1` 个无保守材料接触的状态继续走 legacy。
- Phase J / PR12 已增加并为玩家实机路径启用 main-thread `RadiationService`、source-origin section index、range/flux prefilter、deterministic top-K、feet/torso/head rays、loaded-only quarter-block DDA、独立二值 static occlusion/revision、bounded witness cache、budget/confidence flags 和约 `729,408 B/dimension` optional-memory admission。Campfire/Generator 从既有 radiation port share 同步 origin/power/lifecycle；receiver observation 不写 source timeline 或 accumulator。玩家按吸收能量换算进入五部位 body delta；材料 radiation 未实现。
- Java 17 thermal JUnit `240/240`、玩家辐射换算 `1/1`、`StateTransitionDataTest` `3/3`、`compileJava` 和 Forge GameTest `20/20` required 均通过；完整仓库 JUnit 最近一次执行 `817` 条，其中 `816` 条通过，唯一失败是 `TeamTownActualSaveCodecProbeTest.actualSaveSurvivesTheFullSyncCodec` 缺少外部存档 fixture，与 thermal 改动无关。GameTest 的 radiation 场景使用生产遮挡分类和生产预算，锁定 Campfire 可见、石墙阻断、拆墙恢复、witness cache 与只读 source ledger；新增 underground admission 场景锁定 loaded-only 单层 continuation 不递归。Phase A evidence bundle 还通过 p95/p99 JSON 校验并生成 JFR/JOL artifact。最终 Phase A 数值以该轮 diary 和 `build/reports/thermal-phase-a/` 为准。Architectury 的 dev runtime scope 已覆盖 dedicated GameTest 所需的 FTB/Item Filters 依赖。

Phase E / PR 6 的 Topology Guard、candidate registry 和 fit/holdout reference gate 已实现，PR7 的 dimension runtime、coordinator/publication、whole-set sleep/wake 与 memory admission也已完成。PR8 的增量/full-resync frame -> topology -> sweep -> ACK -> Page release -> executor dispatch、稳定 frame sweep reuse、天空证明 open-space FarField、单层 loaded-only underground continuation 和 degraded 弱边界，Phase G / PR9 的四类 physical source lifecycle -> exact port -> topology-cut settle/rebind，以及 Phase J / PR12 的 radiation source index -> bounded 3-point DDA -> revision witness cache -> absorbed-energy body delta 已进入玩家实机测试路径。Phase I / PR11 的 recipe-compiled hot-side latent mutation 也已进入 applied Page gameplay authority；Phase H / PR10 的非相变材料仍 dormant。Phase K / PR13 的 player、crop 与 town consumer 已按 publication-hit/natural-fallback 切换 gameplay authority；当前没有真实普通机器温度 consumer，也不保留 dormant machine API。HUD 是 player 数据同步的下游展示，不是独立 consumer。Phase L 的历史 synthetic `1/10/50/100` query/JFR/JOL 结果只留在 diary，包装源码、Gradle task 和报告已退役；production input 中无消费者的 shadow comparison/snapshot/计数也已删除。下一步是取得真实 `1/10/50/100` player production-like server、worker solve、TPS 与 retained evidence。
