# 空气与材料共用自然休眠冷却

- Time: `2026-09-14 16:22:45 +08:00`
- Updated: `2026-09-14 18:58:02 +08:00`
- Authors: `Codex; OpenAI GPT-6; primary engineering agent`
- Status: `completed`
- Scope: `休眠温度投影、材料时间锚点、保存/恢复/物体变更、既有随机tick相变及红外增量同步；实现及受控回归完成，真实多人负载验收仍由主材料计划跟踪`
- Related: [材料主计划](2026-09-12_23-53-20_thermal-material-enthalpy-lifecycle-repair.md)、[运行时文档](../docs/climate/thermal-runtime-architecture-and-optimization.md)、[生命周期文档](../docs/climate/data-lifecycle-and-integration.md)、[最近热路径优化](../diary/2026-09-14_15-48-25_material-mutation-copy-on-write.md)

## 后续架构入口

[统一参数与相变流程](2026-09-14_19-06-50_thermal-data-driven-phase-architecture.md)承接下一轮架构工作：材料差异放进数据，运行时共用规则。用户明确允许保留水边缘和岩浆高度，作为少量固定环境约束表达；不再继续堆物质专用分支。本文件保留已实施休眠基础的结果，新架构尚未实施。

## 1. 目标与取舍

空气和材料共享一个按需计算的自然环境松弛过程。查询、恢复、真实编辑或既有随机温度更新命中某个物体时，才根据保存状态及经过的游戏时间算出该物体的当前状态。没有这些访问时不运行冷却任务。

**2026-09-14 修订决定：复用原随机 tick 和地表冻结抽样，不新增 Section 轮询、候选位图、逐方块计时器或新的 tick 事件处理器。** 撤回先前“休眠相变必须等 Page 恢复”的限制。没有 Page 的材料也能在原更新入口被命中时提交转换；未加载或不参与原更新的区块不主动处理。仅加载 Chunk 不保证执行随机 tick，转换没有固定最长等待时间。

统一的是时间、环境采样、衰减与生命周期；空气和每个材料物体仍有自己的温度。活动节点继续原有六向能量交换，材料导热仍限制为受热表面及向内一层，不增加第三层、气隙节点、离线邻接图或新 Mixin。

**本方案明确选择休眠温差共用半衰期。** 复用 `FHConfig.COMMON.THERMAL_RUNTIME.dormantTemperatureHalfLifeSeconds`，源码默认 `1800 s`，不增加材料冷却配置。普通显热材料和空气经过同样时间保留相同比例的温差；相变材料额外受潜热平台约束。这不是按热容、暴露面及接触导热率复现活动 solver 的散热速度。

理由：当前持久化材料只有 H、branch、law、位置和身份，没有保存有效环境导热系数。仅凭热容不能唯一推导冷却速度。强行除以空气热容会引入任意参考体积，按六个暴露面计算又会新增几何读取/存储。此次只修复“空气会回归自然、材料永久冻结”，不引入这些模型。若以后要求休眠时石头与空气保持不同物理时间常数，应另行设计有效导热系数，不能宣称本方案已满足。

## 2. 已核实的现状与缺口

| 代码锚点 | 实施前行为 | 本次改变 |
|---|---|---|
| `DormantChunkThermalState.sample/admissionCut` | Air 保存自然温差，按指数衰减，加上当前自然温度 | 共用时钟/环境采样，避免自然温度变化直接平移全部初始温度 |
| `SectionEntry.rebase` | 保存时缩放并重新量化；sourceSustained 可在加载时一次保留热残差 | 保存不推进状态；移除休眠热源豁免 |
| `MaterialSectionState` / `Editor` | 保存 H/branch，缺少时间；局部编辑使用写时复制 | 增加实际状态时刻，按需投影，保留写时复制 |
| `MinecraftThermalInput.sampleMaterial` | 活动结果优先，否则直接读取保存 H | fallback 读取同一个休眠投影结果 |
| `dormantMaterialAdmissionCut` / `BrickMigrationKernel` | 保存 H 直接恢复 | 在确定的 admission cut 时刻投影一次后恢复 |
| `InfraredCapture.capture` | stored revision 改变才刷新，纯时间变化不可见 | 比较上次提交时刻与当前时刻的最终量化温度 |
| `ServerLevelMixin_TemperatureUpdate` | 随机抽样中的 `StateTransitionData` 温度阈值转换和地表水冻结仍存在，无 Page 也能运行 | 保留抽样时机，有材料记录时改读 H/潜热，无记录时保留自然玩法 |
| `BlockStateBaseMixin_RandomTick` | `willTransit()` 使配方物态方块参与随机 tick | 复用该覆盖，不另建相变候选索引 |
| `SnowLayerBlockMixin_Melt` / `LavaFluidMixin` | 还有直接按温度/光照/概率改变方块的独立入口 | 接入相同材料归属判断，防止绕过潜热；保留不相关原生行为 |

上表保留实施前调查以说明替换范围。现已实施，源码与同步更新的 docs 是现状依据；最终验证范围与未测项目见第10节。

## 3. 共用自然环境模型

### 时间与自然温度

- `t0` 为状态对应的 `ServerLevel.getGameTime()`，单位 tick；读取 `t` 时 `dt=max(0,t-t0)/20` 秒。不是墙钟时间，服务器停止期间不计时，掉 TPS 时随游戏时间变慢。
- capture 必须使用 QueryPublication 的真实 `sampleTick`，不能拿主线程保存时间冒充 H 的时刻。发生真实材料编辑则以该编辑 tick 为新时刻。
- 当前自然温度 `N` 使用 Section 中心的 `WorldTemperature.naturalAir`。不包含解析场、设备温度下限或局部 Air 热残差。共用既有 Section 自然温度缓存，每对齐 20 tick 最多采样一次；衰减使用真实 t，不把 t0 向前对齐。
- 不查无记录区域、不加载 Chunk、不为温度计或红外建立 Page。活动范围仍由原 residency 控制。
- 时间倒退时 dt 截为零；不重新写锚点，不积累负冷却。客户端基准时间大于当前时刻时刷新显示基准。

令 `lambda=ln(2)/halfLifeSeconds`，单位 `s^-1`，`f=exp(-lambda*dt)`。不经过潜热区间时：

```text
T(t) = N + (T0 - N) * f
```

既可降温也可升温。温度低于自然环境时会回暖，不是只减温度。

Air 保留当前打包温差和空间分量，不新增逐 Air 节点 double。每个 `SectionEntry` 增加一个捕获时的自然温度 `savedNaturalC`，恢复 `T0=savedNaturalC+residual/16`，再使用上式。这样 dt=0 时仍为保存温度；删除旧的 `N_current + residual*f` 直接平移语义。

**气候近似边界：** 以查询时缓存的 N 近似整个未结算区间的外界温度，不保存天气历史、不补跑历史 tick。气候恒定时指数解与时间拆分一致；气候变化时，当前 N 会影响整段历史的估计，不能保证与实际逐 tick 气候积分相同。只读查询及纯保存不改锚点，所以多看几次温度计不会改变结果。活动运行产生新的真实状态时才更换锚点。

配置热重载采用当前半衰期重新解释尚未结算的区间，与既有 Air 配置的按当前参数计算方式一致；不增加参数历史或扫描所有存档。需在配置说明中明确可能即时改变休眠读数。材料 law 先按保存 law 投影，再通过现有 law 适配规则输出为当前 law，不能混用新 C 与旧 H。

### 材料焓与潜热

共用自然环境换热方程：

```text
dH/dt = G * (N - T(H, branch))
G = lambda * C
```

`H` 单位 J，`C=law.capacityJPerK` 单位 J/K，`G` 单位 W/K。这里 G 是由休眠半衰期定义的等效系数，不是几何导热系数。无需存储 G，也不用给 Air 创建材料对象；Air 是同一方程的显热特例，C 在温度解中约去。

实现一个无 Minecraft 世界访问的 `DormantThermalCooling` 数值工具，放在 `thermal.persistence`。显热快速路径使用 `expm1` 稳定计算能量变化，写入调用者已有 mutable sample，不为每个物体分配结果对象。

相变只做当前 law 的分段解析积分：

1. 先根据热流方向选择/退出 branch，再重算 T 和实际方向；复用 `MaterialThermalLaw` 的转换端点。
2. 显热段直接指数推进；若抵达相变起点，解析计算抵达时间，再处理剩余时间。
3. 潜热平台温度为 `Tp`，此段 `dH/dt=G*(N-Tp)` 为常数。N=Tp 时热流为零，保留原潜热进度，不能自行完成相变。
4. 潜热退回源端点时退出 branch，剩余时间继续显热段；前进到目标端点时停止。一次投影至多处理当前状态的退回平台、显热、进入平台这些有界段，不按 elapsed ticks 循环。
5. 抵达目标端点后保留 H 和 branch，不越过尚未执行的 BlockState 转换；剩余时间不形成“补热债”。既有随机更新命中时可在主线程提交一次转换，无需 Page；若先恢复活动节点，则交由原 phase request/ACK。成功后用目标 law、同一 H 和提交 tick 开始下一段。

不在只读温度查询中改方块，不在未加载 Chunk 中离线结冰/融化，不新增休眠相变队列。长期休眠状态只投影到当前转换端点；只有原随机更新/地表冻结实际命中，或活动流程接管后，才修改世界。不回放等待期间的全部转换链，不立即补算目标物态的历史冷却。因此它是按游戏原更新机会落地的休眠近似，不是完整离线相变。source 是否仍存在不再是提交前提。

`ThermalCellArena.materialEnergyLimitJ` 当前内含 branch 边界规则。仅将必要的 branch 选择/能量距离纯计算移入 `MaterialThermalLaw`，供 arena 和休眠工具共同调用；arena 的布局等待、request/ACK 仍留在 arena。不要复制第二套相变状态机，不把整个 live solver 改成通用积分框架。

自然环境是外界热库，休眠 H 的变化不是邻居收到/失去的热，不写热源 ledger。恢复以投影后的 H 作为初态，避免 solver 把离线变化误算成凭空出现的 source 能量。

## 4. 时间存储：避免局部更新重置全 Section

不能给整个 Section 只有一个可随任意方块更新的时间戳。旧保存材料与新捕获 Brick 可以不同龄；重置共用时间会使无关材料少冷却，重复应用旧时间又会多冷却。

采用 `MaterialSectionState` 内局部、简单的两种时间表示：

- `long savedTick`：一次完整 capture 的所有记录共用该值。
- 可空 `long[] savedTicks`：第一次出现不同龄记录时才创建，先用 savedTick 填充，随后仅修改目标索引。`savedTick(index)` 封装这一处选择。
- `merge` 复制已有位置/能量时同时带上各自时间；在已有遍历内判断是否全同龄，同龄结果恢复单 scalar 表示，不额外扫描一遍。不是旧格式兼容，也不是两套冷却逻辑。
- Editor 首次共享后复制时把存在的 ticks 一并复制；原地更新、删除标记、压紧及 palette 复用规则不变。删除后压紧同步搬运 ticks。
- `contentEquals`、capture、merge、NBT 和 law 适配全部包含时间语义。H 相等但 t0 不同仍可能表示不同预测，不能当作无变化。

内存增量（原始 payload，不含对象头、对齐、保留容量和同时存活的快照）：

| 情形 | 时间数据增量 |
|---|---:|
| 无材料 Section | 0 |
| 同龄材料 Section | 8 bytes scalar，另有一个可空引用字段 |
| 混合龄 N 条记录 | 8 + 8N bytes，另有数组头及引用 |
| 混合龄 64 条 | 约 520 bytes |
| 混合龄 4096 条 | 约 32 KiB + 8 bytes |
| 有 Air 的 Section | 额外 8 bytes 的 savedNaturalC |

当前主要逐材料 payload 为 position 2 + H 8 + branch 1 + palette index 4 = 15 bytes；完全混合龄时增加 8 bytes/记录，不能称为可以忽略。1000 个满存储且混合龄 Section 的新增时间数组约 31.25 MiB，不含 COW 双份峰值。通常热数据是稀疏的，但验收必须包含此最坏情况。

为什么不选择固定 `long[64]` 的每 Brick 时钟：其满 Section 时间 payload 仅 512 bytes，但一次单块编辑会迫使结算同 Brick 的其他记录，否则共用 t0 错误。当前记录按 Section 位置排序，最简单实现还要额外查找最多 64 个位置。为减少内存而引入这些强制结算/索引结构会扩大 setBlockState 热路径；本方案选择同龄压缩加异龄逐记录时间，保持单点编辑。两者是成本取舍，不宣称所有内存/CPU 负载下绝对最优。

## 5. 生命周期与调用边界

| 路径 | 必须实现的行为 |
|---|---|
| 温度计/普通 Air fallback | 投影到当次 t，返回结果；不修改 H/t0、不 snapshot 整材料表、不标记 Chunk dirty |
| 材料红外读取 | 只投影真正由 stored 补充的物体；活动节点优先，不对活动备份额外冷却 |
| Page 存在但 Brick 未驻留 | 按真实节点所有权决定 fallback，不能仅凭 Page 存在禁止休眠冷却 |
| 拓扑暂时失效、材料身份改变 | 保留既有未变物体最后发布/变化日志过滤，不把预算等待当成整个 Page 进入休眠 |
| Admission | 主线程捕获 t、N、当前配置和不可变材料快照；worker 仅对真正从 stored 恢复的节点投影一次，不访问世界或主线程可变缓存 |
| 恢复中的已有 live span | 迁移原 H，不再应用 dormant dt；配置/拓扑重建不得双重冷却 |
| 新节点无记录 | 继续以真实初始化自然温度建立状态；不存在的材料记录不为显示凭空创建 |
| Checkpoint / 退休 | H 和 sampleTick 同 cut；仅以 capturedMaterials.sampledBricks 替换捕获范围，范围外 H/t0 原样保留 |
| 保存/停止/卸载 | 先完成现有一致发布 checkpoint；其余 stored 原样编码，不为了保存推进、重新量化或逐材料冷却 |
| 加载 | 解码并挂载；不全段投影、不产生 Page；第一笔真实消费者工作按需计算 |
| 原随机tick/地表冻结 | 仅处理已被原逻辑选中的位置；按活动/休眠/无记录归属分流，休眠相变可直接提交，见下一节 |

材料编辑顺序：判定真正被追踪 → 区分实际 live 物体与 saved-only 物体 → 取得对应的当下 H → 应用原 REPLACE/MASS_CHANGE/GAMEPLAY_TRANSITION/普通属性规则 → 将该记录标为编辑 tick。

- saved-only 的保温属性/质量/玩法变更，先以旧 law 投影旧物体再交接；不能沿用冻结 H。REPLACE 可以直接以新物体初始化值覆盖，删除可以直接移除，不必先计算已被丢弃物体的冷却。
- live 物体使用既有一致发布和 checkpoint journal，不能把旧保存副本按休眠公式推进后盖住 live H。发布暂不可读时沿用 journal 的交接机制，不能用自然温度伪造 live 状态。
- checkpoint journal 当前没有时间字段。加入真正需要的 `gameTick`，使编辑后仍未驻留的记录有正确 t0；整批同 tick 可以由批信息提供，禁止把迟到的 checkpoint 时刻冒充原编辑时刻。新增成本至多一个 long/待处理记录，仍按原生命周期裁剪，不增加独立日志。
- 日志中的 REPLACE 初始化及质量交接继续使用已有玩法自然温度语义；新休眠段从编辑结果及编辑 tick 开始。不能把物质交接和休眠缓存精度混成一套未经验证的新初始化规则。

Air 部分同时补齐部分捕获：保留未捕获 Brick；由于 Air 仍共用 Section 时刻与捕获自然基准，在真实新 sampleTick 的局部 capture 中，按上述投影把保留部分统一到该 cut，再与新捕获 Air 合并/量化。只处理已有预算内 Air 记录，不复制每方块浮点温度图。相同 publication/sampleTick 的重复保存跳过此过程，避免保存频率造成重复量化衰减。原 640-byte 空间编码预算和量化精度不扩张。

### 5.1 复用原随机更新：确定的入口与分工

最低开销原则是沿用已发生的抽样，不增加抽样次数、全区块扫描或候选表。实现限定在既有类中的小方法和必要结果枚举；不创建第二个 phase controller 实例，不为了提交而调用 `activeOrCreate` 或启动 dimension worker。

| 世界位置的实际状态 | 自然相变处理者 |
|---|---|
| 有真实活动材料节点，或该物体仍由活动布局/交接流程负责 | 原 worker/ACK；随机路径禁止以环境温度替代物体 H |
| 没有活动归属，有匹配当前身份的保存材料记录 | 休眠 H 投影；能量到达转换端点后主线程提交 |
| 没有材料记录，也没有待交接归属 | 既有环境阈值/自然玩法；不为世界里的每块水建立能量记录 |
| 现有配方明确允许、但不属于可编译物理边的非互逆转换 | 保留正式配方玩法；不伪造潜热边或禁用该配方 |
| 发布暂不可读、布局等待、保存身份待交接 | 本次相变暂缓；不能把“读失败”当作无记录，转而走阈值分支 |

归属必须按 block/Brick 判定。不能复用“Page 存在即拥有整个 Section”的粗判断，也不能把 `sampleMaterial()==false` 当成无主证明。复用现有布局、changed-material journal、保存 identity 查询；先做无分配的归属判定，再按必要路径读取温度、profile 或解析场。

在 `MinecraftThermalInput` / `MinecraftPhaseController` 增加明确的单点尝试入口，例如 `tryMaterialPhaseAtRandomTick(level, loadedChunk, position, currentState)`。返回一个无逐次分配的枚举，语义固定：

- `GAMEPLAY`：没有材料记录，或明确属于现有非物理配方转换，调用者执行原自然/配方玩法。实现时采用此名称，避免把“存在材料H但转换不是物理边”错误称为无材料记录。
- `DEFERRED`：已由材料模型负责，本次尚未转换（含活动所有、潜热未完成、条件不符、临时不可读）；调用者跳过旧温度转换。
- `CHANGED`：已经改了方块；不能再对旧 BlockState 执行本次后续随机回调。

这三个结果是必要控制流，不是兼容分支。`DEFERRED` 不等于“整个 randomTick 已处理”：仍要保留该方块/流体的非相变随机行为，例如岩浆点火。不要仅把旧 `handled` boolean 置 true 后跳过全部回调。

### 5.2 各入口如何接入，避免旧逻辑漏穿

1. **通用 `StateTransitionData` 路径。** 在旧 `std.heatCapacity()` 概率和 `ambientBlockStateUpdateDivisor` 之前进行材料分流。材料 C/潜热已经控制物理快慢，不能再乘一次旧“heat_capacity 越大越难抽中”的模拟惯性。`GAMEPLAY` 才继续原概率、温度阈值和自然玩法；已接管材料被原随机抽样命中后直接判断 H。保留 `willTransit` 等禁用语义，不将 disabled 配方强制变为候选。
2. **地表水冻结。** 保留 `tempBlockstateUpdateIntervalTicks`、高度图抽样、`WATER_DO_NOT_FREEZE` 和已加载邻域检查。用同一尝试入口代替“有热归属就直接 return”的粗判断；休眠水可以在此完成转换。无记录时仍按原 source/flowing water 与薄冰规则。tracked 的目标必须来自当前 law，不能任意把 law 的完整结冰端点改成另一种薄冰目标。
3. **雪层融化。** `SnowLayerBlockMixin_Melt` 的直接光照/环境温度减层必须经过相同分流。tracked 自然相变只使用当前 law 的目标和能量端点；不再把方块亮度大于 11 当成跳过潜热的依据。未跟踪雪层保留既有光照/温度减层玩法。本次不增加逐层潜热模型、不扩展雪层物质量模拟。
4. **岩浆冷却。** `LavaFluidMixin` 的转玄武岩分支也分流。既有岩浆界面高度玩法限制在所有相关入口一致执行；tracked 不用 `nextInt(1000)` 再模拟一次惯性。`DEFERRED` 仅跳过本模组冷却分支，继续原生 LavaFluid randomTick；`CHANGED` 才取消旧流体回调。无记录时保留原概率/阈值玩法。
5. **冰和薄冰。** `IceBlockMixin_Melt`、`ThinIceBlock`、`LayeredThinIceBlock` 中空的原生融化回调不再补建第二套逻辑，由既有 `StateTransitionData` 抽样入口处理。核实 `BlockStateBaseMixin_RandomTick` 的注册及 `LevelChunkSection.isRandomlyTicking()` 对这些状态的实际覆盖。

通用入口与特定方块回调可能在一次未转换的抽样中都被调用。两者用相同的无副作用投影，不增加全局“本 tick 处理过哪些方块”集合；一次成功转换立即终止旧状态回调，避免重复转换。可先用便宜的材料归属检查跳过明显重复的自然分支，不能为了消除少量重复检查新增跨回调缓存生命周期。

配方热重载必须复查已有 Section 的随机 ticking 计数是否会随新 `willTransit` 生效；若计数滞后，只在现有配方重载边界对受影响已加载 Section 复算计数，不能每 tick 扫世界。此项属于覆盖验收，不恢复已移除的旧热几何类。

### 5.3 共用世界转换规则，保留解析场玩法

将 `MinecraftPhaseController.apply` 中与 Page/request 无关的规则及 `setMaterialBlock` 提交提取为直接的共用方法。worker 入口保留 lifecycle、request sequence 和 ACK；休眠入口核对保存记录/current state/current law，随后调用同一世界规则。休眠入口不伪造 worker 请求、页号或 ACK。

- `randomTickSpeed=0` 不通过另一个后台任务偷偷执行自然转换。
- 冰不融化 biome、水不冻结 biome、水边缘、岩浆高度限制在有相应语义的转换上统一。修复目前分散入口保护条件不一致的地方；不把水/冰条件施加到无关材料。
- 邻居与目标 Chunk 通过已加载引用或 `getChunkNow` 查询；必要邻居不在内存时本次 `DEFERRED`。沿用已有世界更新/邻居通知，不另起异步世界修改。
- 保留解析场的既有正式玩法：保证温度下限阻止冻结；明确的强制升温转换可走受控玩法能量路径。普通 delta 场或显示合成不能把 H 推过潜热。
- 活动节点的明确玩法升温继续 `requestGameplayPhase`；休眠节点无需 Page，由同一单点提交过程把所需 H 推到已选择的 heating 端点，标记为玩法强制转换，而不是声称自然冷却已完成潜热。只在原随机更新机会触发，不在温度计/红外读取时触发。
- 读取失败/身份不符不得转为温度阈值分支。实施复核发现当前 compiler 本来只积分互逆边；显式非互逆配方继续正式玩法，不能因为它具有普通材料H而将其禁用。原生雪层、源岩浆、流水和配方到Air的阶段在现有compiler中补齐边界：tracked雪层作为当前整个物体融为Air；岩浆与玄武岩端点共用H参考；流水保留原薄冰目标。新增边按原材料法则提交，不另建图求解器、气隙状态或逐层质量模拟。该修正取代先前“所有无边law都暂缓”的过度限制，不增加旧版本兼容读取。

### 5.4 H、方块变更与保存状态的一次性交接

主线程单次提交顺序必须明确，否则 `onMaterialBlockChanged` 会拿旧冻结 H 处理新方块：

1. 捕获当前 state identity、保存记录的 H/t0、law 和当前 tick，确认无活动归属；投影到当前 tick，判断目标端点以及共用世界条件。
2. 尚未满足条件则直接结束；不修改保存 H/t0、不标记 Chunk dirty、不发布整 Section 快照，也不增加重试任务。下一次原抽样自然再试。
3. 准备目标 BlockState、投影后的精确 H、目标 law 和当前 tick。利用既有 `MinecraftPhaseController.APPLYING` 的作用域，在**实际尝试转换时**携带这几个提交数据；普通查询/未完成相变不分配该上下文。保留原作用域嵌套恢复，不用线程级永久缓存。
4. 通过既有 `setMaterialBlock` 改方块。`LevelChunk.setBlockState` 成功返回的既有钩子核对作用域位置、旧/新 identity，直接向原 Editor 写入传入的 H、目标 law、SENSIBLE branch 和提交 tick。物理转换沿用 `THERMAL_TRANSITION` 的同 H 语义，不走保温的 `GAMEPLAY_TRANSITION` 重新构造 H，也不再扣一次潜热。明确玩法强制转换携带其已补足的 H，用同一个能量交接方式记录。
5. 没有真正发生预期转换时，不预先覆盖旧记录、不倒回世界方块。嵌套的真实替换按原物体变更规则处理，不能在外层 setter 结束后再盲目覆盖；成功检查以实际旧/新 state 和作用域匹配为准。
6. 有 Page 但该 Brick 未驻留时，原 Section journal 仍记录这次变更及正确 tick，之后 capture/merge 不得恢复旧身份。已有 materialRevision 和 chunk dirty 由这一次成功编辑产生，红外沿原增量路径更新。
7. 目标仍是材料时从当前 tick 的新 law/H 继续休眠；目标没有材料 law 时删除旧材料记录。不在一个抽样中循环回放数小时的多级相变链，也不把被截断的历史时间套到刚产生的目标物体上。

该作用域只扩展真实变更所需的数据，使用已有钩子，不新增 Mixin、事务框架或第二份长期能量状态。COW 首次写入和不同龄 ticks 的既有成本仍需计入，不承诺成功转换零分配。

### 5.5 开销与时间语义

- 调度新增持久内存为零：没有 per-Section 候选位图（撤回此前建议的 512-byte 位图）、轮询队列或 per-block 任务。第 4 节为休眠冷却所需的时间存储仍然存在，不能把“无调度内存”说成“整个功能零新增内存”。
- 已有抽样命中后才有单点判断；未跟踪材料短路，tracked dormant 的标量检索沿原排序数组为 O(log N)，数值推进为有界段数。只在真有 stored 记录时采样自然温度；复用 Section 自然缓存和 mutable sample，不 snapshot 材料表。
- 未完成相变时不执行 Editor 写入，因而不会因为随机抽样频繁触发写时复制、ticks 升级或保存脏标记。成功变更的成本与原方块更新及本计划 H 交接一起计量。
- 不新增独立 `64 checks / 4 mutations` 每 tick 预算。这组数字属于已撤回的轮询提议；本方案工作量随原随机 tick/地表抽样设置和实际命中数变化，没有新的全维度硬上限。
- 若 `randomTickSpeed=r`，一个参与标准 Section 均匀抽样的特定方块，单靠该入口平均约 `4096/r` tick 命中一次。r=3、20 TPS 时约 68.3 秒；这是示例参数下的期望值，不是最坏时限，地表抽样另有机会，区块停 ticking 时无限等待。不得宣传“加载后一秒内全部完成转换”。
- source 拆掉后，只要区块仍正常参与原随机更新且配方允许，该位置可以在没有 Page 的情况下完成自然相变；无需玩家查看温度计。未加载/不 tick 时只保留记录，未来命中时再计算。

### 5.6 随机更新热路径：一次定位，按必要程度计算

本节细化实现顺序，不添加新的管理层。默认沿用原抽样频率；不新增节流配置、Section 检查时间戳、玩家监听表或每 tick 去重集合。

1. **复用调用者已有信息。** 通用入口已经拥有 `LevelChunk`、`BlockState`、`StateTransitionData`、当前 Section 和 tick，直接传入需要的参数；不通过通用 `sampleMaterial(level,pos)` 再取一遍 Chunk/BlockState。特定方块回调缺少 Chunk 时只做一次 `getChunkNow`。只传此次计算需要的值，不打包新的逐次 context 对象。
2. **复用原候选判断。** 通用入口保留 `std != null && std.willTransit()` 的快筛，特定入口保留各自适用方块判断；无相变资格时不调用休眠数值计算。不遍历材料 palette 查候选，也不扩大原抽样范围。明确禁用条件与原来仅用作概率的 `heat_capacity` 分开处理。
3. **用已有 attachment 判定归属。** 从当前 Section 的 `MinecraftThermalSectionAttachment` 取得 owner，结合原布局/变更 journal 判定本 block 的实际归属；从 Chunk attachment 取得 dormant 容器。两者都不存在即可返回 `UNTRACKED`。不为每次抽样构造 SectionPos/BlockPos，不给所有 Section 新增 Chunk 反向引用，也不预先启动或查询整套物理 runtime。
4. **活动/交接状态先分流。** 普通自然相变属于 live/pending 时直接 `DEFERRED`，不读旧 stored H、不计算 dormant 指数。若现有解析场索引在此位置具有明确玩法强制转换可能，再走原受控玩法入口；不能为了省查询把解析场强制路径短路掉。
5. **保存材料只定位一次。** 在原 `DormantChunkThermalState` / Editor 读取内部完成 Section 范围判断、一次 `find(position)`、tombstone 和 state identity 核对，然后读 H/law/branch/t0。避免 `hasMaterial(position)` 后接 `readMaterial(position)` 的两次二分查找。用一个紧凑的内部方法完成存在性与读取结果，不暴露 Editor 数组或引入长寿命 index handle。成功世界提交时仍要重新核对目标记录，不能把读阶段索引保留到可重入的改块之后。
6. **只有确定需要才读参数和自然环境。** 已命中记录后再取得当前 law；记录不存在时不编译/适配 law、不做休眠 natural 查询。旧 law 和当前 law 使用已有快路径，未变化时不构造新 law。共用 Section 自然温度缓存，首次查询才分配原有缓存；不要让每位玩家分别缓存同一区域的 N。
7. **标量投影使用已有 sample。** `QueryPublication.MutableMaterialSample` 已有 H/law/branch/sampleTick；内部读取可用 sampleTick 携带保存 t0，投影成功后再设为本次 t，不新增第二个逐物体结果类型。调用者复用原 mutable sample，入场清理全部标志，防止把前一次成功数据当作本次结果。law 适配仍放在保存 law 的能量投影之后。
8. **延迟昂贵的世界条件。** 未到自然相变端点且没有明确强制升温可能时，直接 `DEFERRED`；只有有机会真正转换才查水边缘、biome、目标状态和表现效果。解析场存在性先走既有空间索引快筛，无覆盖时不查询自然方块温度或重复计算合成场。所有必要玩法条件最终仍需核对。
9. **成功路径隔离。** H/branch/目标/tick 在调用世界 setter 前复制到必要局部值及已有 APPLYING 作用域，避免邻居更新等重入覆盖共享 sample。未完成/禁止转换路径不创建 Mutation、不写 Editor、不标 dirty、不更新材料修改编号。自然缓存的显示失效编号仍按第 7 节处理，与物体真实编辑区分。

不要把既有 Editor 的 `snapshot()` 当标量查询。当前 `materials(sectionY)` 会导出共享快照并使下次编辑进入 COW，随机更新/温度计不得为了方便走该入口；worker cut、NBT 和确实需要批量读的红外路径才使用快照。

数值实现保持简单：半衰期对应的 `lambda` 在已有 tuning/profile 重建时派生一次，或在已有批次中局部计算一次，不在每条记录重复 `log(2)`。同龄显热批次复用本次调用内的因子；分段进入潜热后按真实剩余时间计算，不能误用整段 dt 的因子。不持久化 lambda、不维护跨 tick 的因子 HashMap，也不增加每个材料的 G 数组。

接口尺度：一个随机相变尝试入口、一个三结果枚举、现有 law 中必要的纯函数，以及现有 controller 中的共用规则/提交方法即可。按“归属 → 读取 → 投影 → 条件 → 提交”组织直线控制流；不增加策略注册器、通用 scheduler、回调链、Optional/Stream 或为了消除几个重复参数而创建逐次对象。复用函数不等于在每层都再做同一组查找。

### 5.7 多玩家负载：按更新区域计费，不按玩家复制模拟

已核对当前映射版本 `ServerChunkCache.tickChunks`：遍历区块集合，符合条件后调用一次 `level.tickChunk(chunk, randomTickSpeed)`。同一区块不会因附近有多个玩家而分别执行多份随机温度模拟；玩家分散扩大参与更新区块集合才增加此部分工作。已有强制 ticking 等条件也会影响集合，不能简单用玩家数乘一个固定半径来估算。

用于规划的成本模型（不代替实际测量）：

```text
S = 本 tick 实际参与随机抽样的 Section 数
r = randomTickSpeed
n_i = 第 i 个 Section 中可能命中的有效休眠相变记录数
M = 本 tick 实际成功转换数

原随机抽样次数 = r * S
休眠候选命中期望 ≈ r * sum(n_i) / 4096
新增CPU ≈ 实际进入归属判断次数 * 单次判断成本
         + 休眠候选命中次数 * 单次读取/投影成本
         + M * 状态交接及世界更新成本
```

地表冻结及特定回调按实际额外次数单列；不能把总工作全部归入上述均匀抽样估计。多阶段回调也可能重复检查同一位置，记录真实尝试数。改块邻居通知/流体传播不一定是常数耗时。该模型没有“每玩家一份材料数组”或“每玩家一份相变队列”项。

选择随机入口的理由与边界：

| 路径 | 当前决定 |
|---|---|
| 复用随机 tick | 默认实施；无新调度结构，复用本来存在的稀疏抽样 |
| 定期扫描全部已存相变候选 | 不实施；需要候选维护，固定时间轮询可能访问远多于原随机抽样的记录 |
| 预测下次完成时刻、到期队列 | 不实施；稳定环境可能少算，但气候/配方/身份变化与卸载需要维护，增加内存和复杂度 |
| 降低休眠尝试频率 | 默认不实施；只会减少工作并增加落地延迟，不是无损优化，不能偷偷把现有地表 20-tick 间隔套到所有随机相变 |
| 仅温度计/红外读取时转换 | 不实施；转换会依赖观察者，无法满足无人查看也自然结冰 |

上述选择针对当前“最低额外内存、沿用玩法更新机会、允许随机延迟”的需求，不宣称在所有密集负载中 CPU 绝对最优。低频批处理若要比较，必须同时报告其转换等待时间，不能用更慢的生效速度换取较小 CPU 数字后称为同等性能提升。

多玩家红外另行计量：请求仍按玩家发生，共用 scratch 只减少内存和分配，不会自动免掉每位玩家的 capture/编码/网络工作。本次不增加跨玩家响应缓存、观察者列表或窗口温度镜像；保持已有 scratch 及 Section 自然缓存共享，协议增量规则见第 7 节。若真实测量发现红外主导耗时，需将其作为那个入口的热点处理，不能通过改相变调度掩盖。

## 6. 删除被替代行为，不留下双轨

- 删除仅用于 dormant 的 `sourceSustained` 字段、NBT supported 位、`activateLoaded` 保温分支，以及 dormant source support 更新链。附近有机器不再使卸载热残差获得一次冻结豁免。
- 保留真实 `PhysicalSourceSpatialIndex`、source admission/residency 和机器玩法；机器在活动时仍通过原 solver 供热。解析场仍作为正式玩法在查询/显示末端合成，不改写保存 H。
- 删除 `rebaseForSave` 的时间推进及其旧调用；仅有物理新 cut 或真实物体编辑才形成新状态锚点。
- 替换旧 Air 独用衰减函数/缓存因子逻辑，避免同时保留新旧算法开关。复用其自然温度缓存；时间因子用共享数值工具按 t0/t 计算。
- 删除绕过投影的面向消费者“直接保存温度”入口；原始 H 访问仅用于编码、身份交接及内部数值处理，并明确命名。
- 删除 tracked 材料在随机更新中的环境阈值直通、重复惯性概率和只判 heating 的旧接管分支，以单点归属结果统一冷热两向。保留无记录区域的自然环境规则和明确解析场玩法，这是当前功能分工，不是旧模型兼容开关。
- 存储 `FORMAT_VERSION` 从 3 改为 4；当前版本只读写 v4，不增加 v3 转换/兼容 reader。版本不匹配的旧热记录按现有版本不匹配策略不恢复，世界方块数据不变。实施说明必须提前列明开发存档旧热状态将重置，不能暗中丢弃而不说明。

## 7. 红外：让纯时间变化可见，但不每次重发所有材料

继续现有 40 tick 请求、精确地形归属、144³ R16I、AFTER_LEVEL、色标和 LAST 原子提交；无需 shader、Embeddium 或深度目标改动。

给现有请求、响应及客户端基准增加一个 `storedSampleTick`（long，固定编码至多 8 bytes/头；分片按实际头数计）。它表示客户端最后完整提交响应对应的休眠计算时刻，不是客户端自行推进的时钟。LAST 到齐才一起提交；没有响应时不更新基准。无新增服务端 per-player 缓存。

增量处理顺序：

1. 仍用已有 materialRevision、profileRevision、presence 和 live changedBricks 判断真实变化。真实编辑/身份/参数变更按原路径刷新。
2. 对本次请求覆盖、已加载、实际由 saved material 提供的 Brick，比较该记录在 knownStoredSampleTick 与本次 t 的投影。两个结果使用本次相同的 N 和配置。
3. 只有最终显示量化码发生变化才设置该 Brick 的 changed bit，并发送当前结果。复用现有 64 项/4096 项 scratch，不新增窗口级温度数组。不把 stored 存在直接转换成每次整 Page 刷新。
4. 比较使用相同当前 N 只在客户端基准之后 N 未改变时有效。因此共用 Section 自然缓存刷新时，若 N 数值实际改变，为该 Section 更新已有 materialRevision；先完成所有相关自然缓存刷新，再固定此次返回的 storedEpoch。缓存初建/Chunk 重载/配置重载也需使旧基准失效。这样气候变更走真实 revision 刷新，不需要保存历史 N 或为每个玩家建立气候数组。
5. 解析场覆盖的 Page 继续既有 refresh 规则，按原顺序合成后量化；无解析场时才使用直接材料码的双时刻比较。不存在有效材料的数据仍按原缺值规则，不拿 dormant Air 伪装表面材料。
6. 纯时间变化不递增材料修改编号、不写 H/NBT、不制造 Page ownership。N 缓存实际变化产生的编号只是现有显示失效机制，不是一次物体修改。

同环境同 law 的显热批次，可在当前循环中缓存最后一组 `(t0,t)` 的因子；capture 常常同龄，只需一次指数计算。异龄逐记录计算也有明确 O(N) 上界，不建立 HashMap 或长期因子表。基准比较需要最多两次投影/记录，这是节省网络和服务端观察者缓存的明确 CPU 成本，应实测，不称为零成本。

## 8. 实施顺序与文件职责

1. `MaterialThermalLaw` + `DormantThermalCooling`：共享 phase 边界纯计算、显热与平台解析投影；活动 arena 保持原等待/ACK 语义。
2. `MaterialSectionState`/Editor：同龄 scalar/异龄 ticks、COW/压紧/merge/capture/NBT v4；`DormantChunkThermalState`：共用自然缓存、Air 初始基准和读取投影。
3. `MinecraftThermalInput`、`ThermalInputBatch`、`BrickMigrationKernel`、既有 checkpoint journal：一致 t/N cut、单次恢复、编辑交接；删除旧保存推进和 source support 分支。
4. `MinecraftPhaseController`、`ServerLevelMixin_TemperatureUpdate`、雪层/岩浆既有入口：单点三结果分流、共用世界规则、作用域精确 H 交接；先验证无 Page 的 source 拆除场景，再接回通用随机抽样。复查 `BlockStateBaseMixin_RandomTick` 覆盖和重载计数，不增加新 Mixin。
5. 请求/响应、客户端 LAST 基准、`InfraredCapture`：时间字段、双时刻量化比较、自然变化 revision；相变成功沿既有 materialRevision 刷新，不改渲染器。
6. 添加下述针对性 Forge GameTest 和性能夹具，移除被替代的旧语义断言；更新现状文档、主计划状态及开发日记。

只新增必要数值工具；不新增 cooling manager、每物体任务、后台扫描线程、兼容接口、独立温度缓存或第二份权威 H。可读性调整限于该功能实际需要的边界提取。

## 9. 验收与实际成本

必须覆盖的功能场景：

- N=-20°C、T0=20°C、halfLife=1800s：显热空气/材料在 1800s 为 0°C，3600s 为 -10°C；比环境冷时反向回暖。
- 查询次数 0/1/1000、保存/加载次数不同，在相同固定 N、t0 和 t 下给出同一材料 H/T；Air 在没有新物理 capture 时也不因保存次数重复量化。
- 相变平台能量推进、N=Tp 零热流、退回平台、目标端点等待和恢复后一次 ACK；与相同 G 的单外界热库细步积分比较，不只验证实现公式的镜像。
- 拆 source 后移除 Page：通过确定性调用原抽样选中位置的实际处理入口验证休眠水结冰，不依赖随机等待；再做真实 `tickChunk` 集成覆盖验证。没有温度计/红外请求也能转换，没有 worker/Page 新建。
- 原入口覆盖矩阵：通用固/液/气转换、source/flowing water 地表冻结、冰/薄冰、雪层、岩浆；分别覆盖 active、saved-only、untracked、身份/布局 pending。潜热中途不得落入旧阈值或光照直改路径，成功后不得再次执行旧状态随机回调；岩浆未转换时原生非相变回调仍运行。
- 休眠转换前后精确 H 连续、branch 重置、提交 tick 正确、重复命中不重复扣潜热；setter 未改变目标、嵌套替换、同 Brick 未变记录、COW 快照及部分 Page journal/迟到 checkpoint 不恢复旧物体。
- 解析场温度下限禁止冻结；明确强制升温可以无 Page 转换并正确记录补足的 H，纯 delta 不越过潜热。water/ice biome 与岩浆高度限制所有相关入口一致。
- Chunk 已加载但不 ticking、未加载邻居、randomTickSpeed=0、配方 `willTransit` 关闭和热重载计数；没有新后台任务兜底，没有额外区块加载。实际 vanilla 邻居通知/流体更新也纳入 Chunk 加载观测，不能只检查显式 `getChunkNow`。
- 一个 Brick 更新，另一 Brick 或同 Brick 未变记录仍按自己的旧 t0 投影；部分捕获、同龄到异龄、删除压紧、旧 COW 快照不可变和 NBT 往返。
- 有 Page 的 stored-only Brick、活动节点优先、拓扑预算等待、活动/保存交替、多次 admission、不重复冷却；真实 REPLACE 和 A-Air-A 不继承旧热。
- 完全没有 dimension worker 时，已加载 Chunk 的温度计和红外照常投影；未加载 Chunk 读取不加载世界；停止服务器不累计现实时间。
- 热源附近卸载后自然松弛，加载时无保温跳变；能量塔解析场仍按既有玩法独立合成。
- 红外只有温度随时间变化也会更新；未跨量化格时零 payload；重复请求、长期无响应、多个玩家、自然变化、多段 LAST、profile reload、Chunk 重载和时间回退的基准正确。

性能对比使用当前代码作为基线，记录：

| 工作负载 | 要看什么 |
|---|---|
| 没有消费者、没有编辑、原随机更新未命中 | 没有新增周期冷却遍历/对象分配/网络工作 |
| 原随机tick未跟踪/潜热中途/完成转换 | 新增单点 CPU、定位/自然查询次数、分配及 phase 尝试次数；确认同一次标量读没有 has+read 双查找，不导出快照，未完成不标存档脏；成功变更/COW 峰值单独统计 |
| 单点温度计 | 同龄/异龄查询延迟、预热后分配；缓存命中不重复 naturalAir |
| 4096 材料、1024 次实际 setBlockState | 对照现有 p50 1.050μs、p95 1.181μs 的原夹具；新增时间数组首次创建/COW 单独报告，不能藏在预热外只报零分配 |
| 1/64/4096 条记录，1/100/1000 个保存 Section | retained heap、COW 峰值、序列化字节、同龄/异龄比例；不得只报数组 payload |
| 固定及移动红外窗口、1/多玩家 | capture p50/p95、投影次数、自然查询次数、真实发包 bytes、稳态量化无变化零响应 |
| 长时间相变休眠 | dt 从秒到数天，数值循环段数仍常数，不与时间长度成正比 |

已运行新增的受控随机相变夹具以及原真实setBlockState夹具，最终数值见第10节。上述旧数字仅作历史参考，不是同进程配对性能对照。普通 JUnit 源集仍有此前已删除几何类等旧引用，本轮使用可运行的 Forge GameTest 集合；更新了被本次替代的Air休眠测试，没有恢复旧兼容类型。

多玩家验收必须用“玩家集中/分散 × 相变材料稀疏/密集”四组场景，固定游戏配置、同龄/异龄比例和数据集；记录实际 S、候选数、抽样次数与完成转换数。先关闭红外测热更新，再以同一组玩家开启红外测增量，分别报告：

- tick 中热更新增量耗时与完整服务器 MSPT 的 p50/p95，样本 tick 数、实际 TPS、成功转换数和抽样命中到提交的延迟。不要只报一个 getter 的纳秒数。
- 保留堆、每秒分配量、首次自然缓存初始化/异龄 ticks 升级/首次共享后 COW 峰值；时间数组、共享 scratch、已有存档数据分开计数。
- 开启红外后的 capture/编码耗时、每玩家及总发送字节，不把每玩家工作误报成只执行一次。
- 相同区块集合内增加玩家时，相变抽样次数不随玩家数倍增；分散玩家时，成本随实际新增参与更新范围与命中量变化。必要计数放在 GameTest/测量夹具中，功能实现不为验收常驻一套逐方块统计表。

实施验收先满足不增加候选调度数据、标量只读不触发 COW、无额外 Page/Chunk 加载、冷热相变语义正确这些可检查约束。性能结果对照当前版本按上述负载报告；出现明确热点才继续局部优化，不预先添加到期堆、节流参数或结果缓存。保持原抽样率下移动旧概率 gate 可能增加投影调用数，必须把这部分计入，不能以“复用了随机 tick”推断 CPU 没有增加。

## 10. 文档影响与结果

Outcome: 代码实现与本轮受控验收完成。空气/材料按需冷却、潜热积分、同龄/异龄时间、COW、一次恢复、编辑交接、随机相变、原生入口覆盖和红外时间增量均已接通。未新增Mixin、轮询、候选调度或每玩家能量缓存；旧source支持链、save-time衰减及不再使用的整套PhaseCandidates发布数组/构建scratch已删除。生产Java本轮净增85行（含新增68行数值工具，测试/文档另计），行数仅说明替换规模，不作性能证明。

- 最终Java17 `runGameTestServer --offline --no-daemon --console=plain -Dnet.minecraftforge.gradle.check.certs=false`：82/82通过，日志[最终验证](../build/dormant-final-validation.log)。涵盖半衰期/保存无漂移、潜热及独立细步积分对照、时间隔离、无Page提交、明确解析场、原生雪层/岩浆、流水目标、IR时间增量，以及原有拓扑/材料/源/协议回归。
- 四组直接随机入口夹具：1/8个Section × 1/64条记录，每组8192次测量、每批256次，预热后本轮均0 bytes/attempt；p50分别649.61、596.88、94.92、102.34 ns，p95分别743.75、670.31、98.83、117.19 ns。顺序/JIT影响明显，不能以8个Section更快推导负载越大性能越好。首次COW及异龄写入分别224/1536 bytes，未藏作“全部写入零分配”。
- 原4096条记录、1024次真实setBlockState夹具：本轮p50 981.25 ns、p95 1331.25 ns、24.1484375 bytes/call（包括其实际Minecraft调用路径）；不将这个结果误报为零分配或同进程确定性加速。
- 修正了测试范围问题：性能夹具恢复模板外的方块与保存状态；岩浆原生测试放在允许冷却的Y范围。拓扑churn测试通过真实PhaseIntent建立完成能量后的待ACK请求，并在放开世界转换前验证requestSequence，再维持原600tick/21tick拓扑扰动条件；不再把到达9°C平台当成已完成潜热。
- 已更新runtime、生命周期、世界温度、热源文档和配置注释。完整真人/机器人多玩家MSPT、网络总负载和长期retained heap未测；现有1/8区域夹具不冒充这些验收，继续由主材料计划跟踪。不声称物理散热完全等价、随机完成有最长时限或全局绝对最佳。
- 实施记录：[日记](../diary/2026-09-14_18-16-17_unified-dormant-cooling-implementation.md)。

### 2026-09-14 复查与范围收拢

- 保留已验证修复：相变作用域核对旧/新身份并记录同位置后续替换；迟到外层回调不覆写新物体，过期提交不改新方块。岩浆液位共用比焓参考，减量按目标offset接续，避免参考不同造成跳温。
- 按用户最新要求停止扩展边缘规则，撤回尚未通过验证的单向配方专项升温分支；自然生成岩浆的初温规则也不在本轮继续修改。这两个问题保留为未处理项，不宣称已修复。
- `MinecraftThermalInput`由本轮整理前1986行减至1905行：随机相变只委托给既有`MinecraftPhaseController.tryAtRandomTick`，投影归`DormantChunkThermalState.projectMaterial`，场下限采样归`MinecraftGameplayFields.guaranteedFloor`。不新增拆分类、调度器或适配层；并非对整个入口类的全面重构。
- 整理后84/84 Forge GameTest通过，含嵌套替换与岩浆液位回归：[日志](../build/thermal-input-responsibility-cleanup.log)。详细记录：[复查与收拢日记](../diary/2026-09-14_18-58-02_thermal-input-responsibility-cleanup.md)。
