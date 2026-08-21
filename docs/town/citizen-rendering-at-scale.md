# 千人同屏居民渲染技术方案

- Status: `Transitional`（M0/M1/M2 与 Flywheel 动态实例代码已实现；1024 人所有权计数、稳态脏写、F3+T 重建、睡眠迁移、基础画面、步态一致性、68/72 格 LOD 迟滞、Flywheel 原点清槽重建和 Billboard 头部轮廓已通过实机验证；快照时钟已统一到 Flywheel `uTime`，相关回归测试通过，实际 1024 moving 抽动复测待完成；moving 快照的无用 CPU 视锥盒分配已由 JFR 定位并改为 Flywheel 路径零物化，自动化通过、重启后 JFR 对照待完成；维度/Flywheel renderer 重绑定、Oculus CPU 兼容回退、首选 Flywheel 自动恢复和双路径 Billboard 同源布局已有自动化覆盖，实机复测待完成；GPU 性能与完整 Oculus 矩阵仍待验收）
- Last verified: `2026-08-21`
- Scope: `客户端居民渲染、LOD、Flywheel 实例化、资源生命周期、性能验收与回退路径`
- Current code anchors: `CitizenRenderCoordinator`, `CitizenRenderBackend`, `CpuBatchCitizenBackend`, `FlywheelCitizenBackend`, `CitizenInstanceData`, `CitizenInstanceType`, `CitizenFlywheelModels`, `CitizenBatchRenderLayout`, `assets/frostedheart/flywheel/shaders/citizen.vert`, `CitizenRenderOwnership`, `CitizenRenderOwner`, `ClientCitizenRenderer`, `FakeCitizenManager`, `DetailedCitizenSelector`, `CitizenClientBenchmark`, `CitizenBenchmarkLayout`, `CitizenRenderMetrics`, `CitizenDebugClientCommand`, `CitizenDebugOverlay`, `FakeCitizenRenderer`, `ClientCitizenCache`, `ClientCitizen`, `CitizenClientEvents`, `CitizenSkins`, `SyncEngine`, `CitizenDeltaPacketBatcher`, `FHConfig.CLIENT.maxDetailedCitizenEntities`, `FHConfig.SERVER.TOWN`

本文回答的是“一个客户端同时收到并看到约 1000 名普通居民时，如何稳定渲染”。服务端行为、移动、持久化和网络的总体架构见 [hybrid-simulation-architecture.md](hybrid-simulation-architecture.md)。本文中的“当前”表示已经存在于源码；“目标”或“拟”表示实施建议，不代表已经上线。

## 结论

最合适的方向不是继续扩大原版实体渲染，也不是单纯减少服务器同步人数，而是保留现有混合模拟，进一步把客户端表现拆成两条有严格边界的路径：

1. **高质量近景层**：只给最多 `64` 名需要交互或最靠近玩家的清醒居民创建 `FakeCitizenEntity`，继续使用原版 `PlayerModel`、原版光照和动作表现。
2. **数据驱动人群层**：其余清醒与睡眠居民都不创建实体；使用 Flywheel 管理的共享静态网格和紧凑实例数据，在 GPU 中完成位置插值、朝向和简化肢体动画。
3. **兼容回退层**：`CpuBatchCitizenBackend` 包装 `ClientCitizenRenderer`。Flywheel backend 不健康、资源重载失败或 Oculus 光影使 Flywheel 0.6.11 关闭实例化时，`CitizenRenderCoordinator` 临时切到 CPU，但保留 requested AUTO/Flywheel；光影关闭后的 `ReloadRenderersEvent` 自动重试 Flywheel。用户显式选择 `cpu_batch` 才取消该请求。

项目已经强制依赖 `flywheel-forge-1.20.1:0.6.11-13`，并同时支持 Embeddium/Oculus。该 Flywheel 版本在检测到正在使用 shader pack 时会把 backend 设为 `OFF`；Create 0.5.1 的兼容策略也是停止实例化并使用标准 `RenderType`/`VertexConsumer` 回退，而不是在 Oculus 下强行运行 Flywheel shader。Citizen 沿用同一边界，不绕过 Flywheel 的安全检查。

关键限制是：**不能把 1000 名居民各自注册为一个 Flywheel 实体实例**。那只减少模型提交成本，仍保留 1000 个客户端 `Entity`、世界实体表、tick、AABB 和生命周期。M2 实测 Flywheel 0.6.11 的世界 `InstanceManager.materialManager` 是公开入口，因此 `FlywheelCitizenBackend` 本身直接作为唯一人群 owner，不再创建无业务价值的载体实体；当前实现中单个批量居民始终只是一个 58 B `CitizenInstanceData`，不是实体或 `EntityInstance`。

## 目标场景与非目标

### 必须覆盖的场景

- 单客户端 `ClientCitizenCache` 中有 `1024` 名居民，其中至少 `1000` 名在视锥内、96 格 AOI 内。
- 白天下班潮：居民集中在 64 格内并持续移动、转向、停步。
- 夜间睡眠：约 1000 名居民处于 `SLEEP`，全部走水平模型或远距 billboard，不生成假实体。
- 玩家快速转身、传送、切维度、资源重载，以及可见预算热修改。
- Embeddium 开启；Oculus/光影开启与关闭分别验证。

### 本阶段不做

- 不把普通居民恢复成服务端原版 `Mob`。
- 不为 1000 人实现完整玩家骨骼、盔甲、手持物、披风和逐人复杂材质。
- 不先做 compute shader、GPU occlusion culling 或 multi-draw indirect；1000 个 144 顶点左右的实例不需要这些复杂度。
- 不以“必须单 draw call”为目标。七张原版皮肤最多拆成七个材质批次仍然足够便宜，兼容性优先于过早制作运行时纹理数组。
- 不通过把 `maxVisibleCitizensPerPlayer` 降到几十人来伪装性能达标。展示预算是容量保护，不是渲染架构。

## 当前实现审计

用户可见的“原版渲染”只发生在近景清醒居民上；当前实现已经是混合管线：

| 距离/状态 | 当前路径 | 实际代价 |
|---|---|---|
| 清醒、入选 Top-K：16 格进入/20 格保留 | `FakeCitizenEntity` + `FakeCitizenRenderer` + 原版宽臂 `PlayerModel` | 客户端配置默认严格限制为 64 个；交易、准星、距离和稳定 id 决定优先级 |
| 未由假实体接管的清醒居民：Body 进入 `<68` 格、保持到 `72` 格 | `ClientCitizenRenderer` 每帧写入 Steve 比例盒体 | 不是 GPU instancing；每居民每帧重建 6 个盒体的顶点 |
| 睡眠 Body：进入 `<68` 格、保持到 `72` 格 | 同一 CPU 批量路径中的水平盒体 | 不创建假实体，但仍每帧重建顶点 |
| 清醒/睡眠 Billboard：`>=68` 格至 `96` 格 | 竖直/水平 billboard | 身体和头部两个 quad，共 8 顶点；CPU/Flywheel 共用 `CitizenBatchRenderLayout` 与 LOD owner |
| `>96` 格 | 服务端 AOI 不追踪，客户端也不绘制 | 无客户端居民成本 |

Citizen 启动默认请求 `AUTO`：当前世界可使用 Flywheel `INSTANCING` 时，未由详细实体接管的居民自动改由 Flywheel 动态实例路径绘制；否则保持 `cpu_batch`。Flywheel 每张皮肤共享一个 144 顶点 Body 和一个 8 顶点“身体 + 头部”Billboard 网格，每居民一个 58 B `CitizenInstanceData`；快照插值、最多 30 tick 外推、短路径朝向追赶、刚性肢体动画和睡眠姿态在 vertex shader 中执行。光影、驱动能力或运行故障使实例化不可用时，`requested=auto` 保持不变而 `active=cpu_batch`；renderer reload 后自动重试。`/citizen_debug backend flywheel` 保留为显式 Flywheel 诊断偏好。

当前已有的正确基础应保留：

- `CitizenSim`/`SyncEngine` 已经绕开服务端实体，客户端只接收紧凑快照。
- `SyncEngine` 以 96 格为 AOI，默认每玩家最多 `1024` 个展示关系、全服最多 `8192` 个展示关系。
- `ClientCitizen` 已有快照插值、限时外推、睡眠切换 snap 和复用的 `posBuf`；`renderPos()` 本身不会逐次分配数组。
- `ClientCitizenRenderer` 一帧只扫描一次缓存，按七张原版皮肤复用七个 `BufferBuilder`，最多七次批量提交；CPU 与 `CitizenFlywheelModels` 共用 `CitizenBatchRenderLayout` 中的六个 Body 部件和两个 Billboard quad，站立/睡眠尺寸、UV、锚点和四肢摆动符号不再重复定义。
- `DetailedCitizenSelector` 使用复用的原始类型数组和定长最大堆选择最多 64 个详细实体；已接管对象按 4 格距离优势保留，交易对象和准星对象优先，等距按稳定 id。
- `ClientCitizen` 在快照到达时只把 CPU 视锥盒标记为失效；仅 `ClientCitizenRenderer` 实际读取 `cullingBox()` 时才物化覆盖“上一快照→当前快照→最大外推终点”的扫掠 AABB。Flywheel 后端不读取该对象，因此 moving 快照不会创建无用 AABB；CPU 回退仍按快照最多物化一次并由后续帧复用。
- 环境光按居民缓存，跨方块立即刷新，静止时每 5–8 tick 错峰刷新。
- 睡眠居民不会创建 `FakeCitizenEntity`。
- `CitizenClientBenchmark` 可向缓存安全注入 32/64/256/1024 名确定性测试居民；使用独立高位 id，清理时按对象身份删除，不覆盖或清空服务端同步居民。
- `CitizenRenderMetrics` 用 256 帧复用环形缓冲记录 CPU 批量路径的分类计数与耗时；帧记录本身不创建统计对象。
- `FlywheelCitizenBackend` 已实现七皮肤持久 Body/Billboard 实例、动态快照脏更新、详细实体互斥、与 CPU 共用的 68/72 格 LOD 迟滞和 96 格上限、限频光照、世界清理、维度重绑定、Flywheel renderer 事件驱动的 manager 重建和世界渲染原点清槽后的实例重建。`CitizenRenderCoordinator` 分开维护 requested/active backend，并保存客户端临时的批量 LOD owner；默认 requested 为 `auto`。

### 千人场景的剩余瓶颈与已完成止损

1. **已完成：近景实体硬上限**。`maxDetailedCitizenEntities` 默认 64、范围 `0..128`，配置热变更在下一客户端 tick 生效；未入选居民继续走批量路径。
2. **CPU 回退路径剩余：批量不等于实例化**。站立模型由 6 个盒体组成，每盒 6 个 quad、每 quad 4 个顶点，即每居民约 144 个顶点。1000 名中距居民仍会让 Java 渲染线程每帧生成约 14.4 万个 `NEW_ENTITY` 顶点并重新上传。AUTO 在实例化可用时选择 Flywheel 消除这项成本；不支持 instancing 的设备仍使用该 CPU 兼容路径。
3. **已完成：后端按需剔除对象分配**。快照只使扫掠 AABB 失效；CPU 回退在下一次视锥读取时重建一次并覆盖最多 1.5 秒的 Dead Reckoning 外推，后续渲染帧直接复用。Flywheel 不执行 CPU 视锥读取，因而构造和 moving 快照均不物化该对象。
4. **已完成：近景候选单次缓存扫描**。一次遍历同时收集 Top-K 候选和准星命中，之后只遍历至多 64 个选中 id 与现存代理。
5. **Flywheel 动态表现已完成、待实机验收**。实例 backend 的动画、插值、朝向、睡眠和 Billboard 已移入 shader；CPU 每 tick 仍做所有权/LOD 检查和错峰光照采样。快照、LOD 或光照值变化只标脏对应实例；Flywheel 原点切换会使底层槽整体失效，因此按缓存整体重建一次。

### 基准场景与统计命令

基准场景是纯客户端开发工具，不发送居民生成包，也不修改服务端 `CitizenSim`。加载时以玩家位置和相机水平朝向为锚点，在前方 6 到约 29.25 格放置固定 0.75 格间距网格。`moving` 每 4 tick 生成确定性的往返快照，`sleeping` 固定覆盖四个主方向。重复加载会先按对象身份移除上一组测试居民；真实同步缓存保持不变。为获得可比结果，应在空旷、真实居民为零的测试世界使用同一位置、视角、分辨率和图形配置。

```text
/citizen_debug benchmark load 32 moving
/citizen_debug benchmark load 64 moving
/citizen_debug benchmark load 256 sleeping
/citizen_debug benchmark load 1024 moving
/citizen_debug benchmark status
/citizen_debug benchmark clear
/citizen_debug backend status
/citizen_debug backend auto
/citizen_debug backend flywheel
/citizen_debug backend cpu_batch
/citizen_debug metrics
/citizen_debug metrics reset
/citizen_debug overlay true
/citizen_debug overlay false
```

`/citizen_debug backend status` 同时报告当前绘制者与用户意图：

| 输出 | 含义 |
|---|---|
| `active=flywheel_instancing, requested=auto, compatibilityFallback=false` | AUTO 探测到实例化可用，Flywheel 实际持有批量居民 |
| `active=cpu_batch, requested=auto, compatibilityFallback=true` | AUTO 探测到实例化不可用或 Flywheel 暂时失败，CPU 安全接管并等待 renderer reload 重试 |
| `active=cpu_batch, requested=cpu_batch, compatibilityFallback=false` | 用户明确选择 CPU，后续 renderer reload 不会自动切到 Flywheel |
| `active=cpu_batch, requested=flywheel_instancing, compatibilityFallback=true` | Flywheel 仍是用户选择，但实例化当前不可用；Oculus shader pack 开启或 backend 健康检查失败时由 CPU 临时接管 |
| `active=flywheel_instancing, requested=flywheel_instancing, compatibilityFallback=false` | Flywheel 已恢复并实际持有批量居民 |

Oculus 实机回归使用以下固定顺序：

1. 开启 shader pack，执行 `/citizen_debug backend flywheel`、`/citizen_debug benchmark load 1024 moving` 和 `/citizen_debug backend status`。预期为 CPU 临时接管、requested Flywheel 保留；确认近距 Body 和远距 Billboard 均可见，Billboard 身体与头部完整。
2. 保持场景不变关闭 shader pack，等待 Flywheel renderer reload 完成，再执行 `/citizen_debug backend status`。预期 active/requested 均为 `flywheel_instancing` 且 `compatibilityFallback=false`；确认没有位置错乱、局部空白、移动后消失或 LOD 切换重影。
3. 再次开启 shader pack，确认 active 回到 `cpu_batch`、requested 仍是 Flywheel，并重复检查 Billboard 头部。最后用 `/citizen_debug metrics` 记录 `cache`、`detailed`、`body`、`billboard` 和 backend。

CPU 兼容路径解决的是光影期间居民缺失和 Billboard 头部消失；批量 Body 不进入 Oculus 的实体阴影重放，因此仍不投原版实体阴影。近景 `FakeCitizenEntity` 继续保留正常实体阴影，批量阴影若需要应作为独立 Oculus 可选集成实现。

同一命令也注册在 `/frostedheart citizen_debug ...`、`/fh citizen_debug ...` 和 `/twr citizen_debug ...` 下。每次 `benchmark load` 和 `benchmark clear` 都会重置统计历史。统计定义如下：

| 字段 | 精确定义 |
|---|---|
| `cache` | 当前 `ClientCitizenCache` 总数，包含真实同步居民与测试居民 |
| `detailed` | `FakeCitizenManager.activeCount()`；客户端详细假实体总数 |
| `batchFrustum` | CPU backend：通过距离/后向/frustum 测试的批量居民；Flywheel：当前持有的实例总数，首版不做逐实例 frustum |
| `body` / `billboard` | CPU backend：本帧写入的完整盒体/轮廓；Flywheel：持久 Body/Billboard 实例数 |
| `batchDraws` | CPU backend：本帧 `RenderType.end` 次数；Flywheel：有实例的皮肤 × LOD 材质批次数，最多 14；不等价于 RenderDoc 所见底层 GL draw call |
| `lightSamples` | CPU backend 与 Flywheel：本帧发生的限频光照查询数；Flywheel 只有光照值改变才标脏实例 |
| `instanceDirtyBytes` / `peakInstanceDirtyBytes` | 最新一帧/自 reset 后峰值逻辑脏字节；Flywheel 每次创建、快照写、LOD 删除/创建或光照变化按 `58 B` 计，CPU backend 恒为 `0` |
| `Backend hook CPU latest/average/p95` | coordinator 调用 backend hook 的最近一帧/平均/p95。CPU backend 包含 `ClientCitizenRenderer.render`；Flywheel 的 render hook 只包含统计，**不包含 Flywheel 自己的 render-layer CPU/GPU 时间** |

HUD 文本每 20 tick 才重建一次，但绘制 HUD 本身仍会干扰严格的 allocation/GPU capture。正式采样时关闭 overlay，等待至少 256 帧后用 `/citizen_debug metrics` 读取结果。Flywheel 的 hook CPU 接近零只说明业务 backend 不再逐帧生成顶点；Flywheel 引擎 CPU 和 GPU pass 仍必须用 JFR/RenderDoc 或图形后端计时器测量。

2026-08-20 的 1024 moving 内存调查在实际整合包客户端上确认 active backend 为 `flywheel_instancing`，并记录到 `1024 ClientCitizen`、`960 CitizenInstanceData`、`960 FlywheelCitizenBackend.Entry` 和 `64 FakeCitizenEntity`；这些类本身的直方图浅大小合计不足 `0.3 MiB`，不是数百 MiB 的常驻泄漏。JFR 的 DirectBuffer 统计在 20 秒窗口内稳定为约 `5.0 MiB`，也排除了 Citizen 顶点/实例直接缓冲持续扩容。分配样本则直接命中两条 `AABB` 热路径：`ClientCitizen.update -> createCullingBox` 权重约 `9.9 MiB`，以及 64 个详细代理的原版 `Entity.setPos` 权重约 `10.1 MiB`。前者原本按 1024 人、每 4 tick 线性增长，现已通过延迟物化消除于 Flywheel 路径；后者由详细实体上限约束，不随总人数继续增长，并保留原版实体包围盒语义。

M2 已关闭基础 instancing/F3+T 决策门。Flywheel 动态代码和自动化现已完成，1024 人 moving/sleeping 的实例所有权、稳态零脏写和 F3+T 重建计数也已通过实机验证；基础 Body/睡眠画面无 UV、方向、重影、深度或光照问题，详细假实体与实例 Body 的行走速度一致，68/72 格 LOD 迟滞切换没有重影、空帧或抖动。Flywheel 在相机任一轴离渲染原点超过 100 格时会清空所有 `GPUInstancer` 槽；Citizen backend 现已注册原点 listener，在清槽后用新原点立即重建实例并保留光照缓存与步态相位，跨界实机复验确认画面正常且 Body 不再整体消失。原单 quad Billboard 因缺少头部存在明显轮廓跳变，改为身体与头部双 quad 后，715 个远景 Billboard 的实机复验确认头部效果正常。之后的决策门是 Embeddium/Oculus 图形兼容与 JFR/RenderDoc 性能预算。

## 目标架构

```text
S2C spawn / batch / despawn
            |
            v
   ClientCitizenCache
   authoritative render state
            |
            v
 CitizenRenderCoordinator (client tick)
  - stable id -> slot
  - promotion Top-K
  - LOD + dirty flags
  - light refresh budget
       |                 |
       | <= 64 promoted  | remaining citizens
       v                 v
 FakeCitizenManager   CitizenRenderBackend
 vanilla PlayerModel    |              |
                        v              v
               Flywheel instances   CPU batch fallback
               shared static mesh   current renderer
```

`ClientCitizenCache` 继续拥有网络与插值状态；渲染层不复制姓名、行为或权威位置。`CitizenRenderCoordinator` 拥有生命周期、唯一 owner 决策、跨 backend 的 LOD 迟滞状态和 backend 句柄；`FlywheelCitizenBackend` 按稳定 id 持有实例引用、当前 GPU 网格 owner、dirty 写入和光照采样状态。正常 backend 关闭会删除有效实例；维度或 renderer 已先销毁 Flywheel manager 时只丢弃失效句柄，再绑定新世界资源。

### 近景高质量层

当前客户端配置 `maxDetailedCitizenEntities` 默认 `64`，允许范围 `0..128`。该值与服务端 `maxVisibleCitizensPerPlayer` 分离：前者只控制使用原版渲染的数量，后者控制网络和客户端缓存总量。

候选与排序规则：

1. 正在打开交易界面的居民固定为最高优先级。
2. 准星当前选中的居民次优先。
3. 其余清醒居民按与摄像机的距离排序，等距按稳定 citizen id。
4. 进入高质量层使用 16 格阈值，退出使用 20 格阈值；已提升居民获得保留优势，避免反复创建/销毁。
5. 睡眠居民永不提升为假实体。
6. 高质量名额耗尽时，未提升的近景居民仍由实例层绘制，不能消失。

当前近景阈值为 16 格进入、20 格退出，使 64 个名额覆盖真正能看清模型细节的对象。该数字仍需由基准测试校准，但不能取消数量上限。

### 实例层 LOD

第一版只需要两种共享网格，避免一次引入过多资产与 shader 分支：

| LOD | 建议范围 | 网格与表现 |
|---|---|---|
| Body | 新进入 `<68` 格；已有 Body 保持到 `<=72` 格，且未被假实体接管 | 预烘焙 Steve 比例网格；站立/睡眠共用顶点，shader 按状态变换部件 |
| Billboard | 新进入 `>=68` 格；已有 Billboard 保持到 `>=68` 格；最远 `96` 格 | 身体使用皮肤躯干正面 UV，头部使用头部正面 UV；站立时两个 quad 朝向摄像机，睡眠时共同转到床面 |

CPU 批量与 Flywheel 使用同一个 68/72 格 Body 迟滞和 96 格 Billboard 上限；迟滞 owner 保存在 `CitizenRenderCoordinator`，切换 backend 时不重置，且只有 owner 真正变化时才写入 map，因此不会因为 backend 不同而改变居民的 LOD，也不在稳态 tick/frame 重写相同值。CPU render、Flywheel tick 与详细假实体 tick 各自在入口采样一次游戏时间，并通过 `ClientCitizen.renderPos(double)`/`visualYaw(double)` 传给本轮全部居民。切换时必须先让目标槽位 ready，再在同一帧撤销来源所有权；不能让假实体和实例同时显示，也不能产生空帧。只有 profile 证明 Body 顶点着色成为瓶颈后，才增加独立低模网格。

### 静态网格与 GPU 动画

Body 与 Billboard 网格只上传一次。Flywheel 0.6.11 的 instancing 模型池固定转换为 `Formats.BLOCK`，不能保留任意自定义顶点属性；实现因此把 `partId` 编码进静态网格未使用的顶点颜色 R 通道，shader 先解码部件号再把颜色恢复为白色。`partId` 标识躯干、头、左右臂、左右腿、Billboard 身体与 Billboard 头部；睡眠 shader 据此分别翻转躯干和头部的正确 V 区域。vertex shader 根据实例状态与动画相位执行简化变换：

- 实际快照位移或外推速度非零：按原版 `walkAnimation` 的 `0.6662 rad/block` 相位标度驱动双臂/双腿反相摆动；手臂幅度为每 tick 步速，腿为 `1.4` 倍步速。
- 静止：部件不摆动；实例 Body 不增加详细假实体不存在的整体上下 bob。
- `SLEEP`：整体旋转为水平姿态，不执行步行动画。
- 朝向：使用客户端平滑后的 256 级视觉 yaw，不在 shader 中重新推导行为方向。
- 年龄：幼儿、儿童、成人/老人分别使用 `0.4`、`0.5`、`1.0` 整体比例；站立与睡眠的 Body/Billboard 都以脚底或床面为固定基准缩放。CPU fallback 与详细假实体使用同一组比例。

不需要 bone texture。当前只有 6 个刚性部件，按 `partId` 在 shader 中计算少量旋转比维护骨骼纹理更简单，也更容易兼容 Flywheel 0.6。

### 实例数据

`CitizenInstanceType.FORMAT` 的实际 stride 是 `58 B`，1024 人常驻实例缓冲为 `59,392 B`，低于 `64 KiB` 合同：

| 字段 | 用途 | 更新时机 |
|---|---|---|
| `light` | 2 B；方块光、天空光 | 创建、跨采样格或 5–8 tick 限频刷新且值改变 |
| `pos0`, `pos1` | 24 B；上一/当前快照的 Flywheel 原点相对位置 | 收包、资源/原点重建 |
| `timing` | 8 B；Flywheel `uTime` 时钟下的开始 tick 与窗口长度 | 收包 |
| `velocity` | 8 B；XZ 方块/tick，halt/静止时为 0 | 收包 |
| `yaw` | 12 B；起始 8-bit yaw、短路径差值、开始 tick | 收包 |
| `flags` | 4 B；moving、sleeping、量化累计步态 phase、年龄组 | 创建、快照、状态或年龄变化 |

render frame 只更新全局时间和摄像机 uniform；位置插值与动画在 GPU 完成。网络快照到达时只标脏对应槽位，不允许每帧把 1024 个实例全量重写。

年龄复用 `flags` 原来的末尾保留字节，因此 `CitizenInstanceType.FORMAT` 仍为 `58 B`。年龄改变时只把既有实例标脏一次；不会扩展移动快照，也不会创建按年龄拆分的实例类型或渲染类。

Flywheel timing contract: `FlywheelCitizenBackend.writeSnapshot` samples
`com.jozufozu.flywheel.util.AnimationTickHolder.getRenderTime()`, the same
clock domain that Flywheel exposes to shader `uTime`. Create's same-named
`AnimationTickHolder` is not interchangeable; using it advances a fresh
4-tick segment before the shader sees its true start and produces a visible
first-frame position/yaw jump.

### Flywheel 接入方式

Flywheel 0.6 的常规注册入口以 `Entity`/`BlockEntity` 为宿主，但 `InstanceManager.materialManager` 在 0.6.11 是公开 API。静态验证与当前动态实现因此都采用更小的“单 backend owner、多数据槽”适配：

- `FlywheelCitizenBackend` 从当前客户端世界的 entity `InstanceManager` 获取共享 `MaterialManager`，不向世界添加载体实体。
- 七张皮肤分别创建共享 Body 与 Billboard `Instancer<CitizenInstanceData>`；模型每材质/LOD 只烘焙一次，每个居民任一时刻只属于一个 58 B 实例。
- backend 用稳定 citizen id 管理创建与删除；详细实体 owner 和 `>96` 格对象不会保留实例。
- `CitizenClientEvents.onFlywheelRenderersReloaded` 以 `EventPriority.LOWEST` 监听 `ReloadRenderersEvent`。Flywheel 默认优先级先替换当前世界的 `InstanceWorld`，Citizen 随后立即取得新的 `MaterialManager`，丢弃已脱离旧 manager 的逻辑句柄并从 `ClientCitizenCache` 重建实例；每 tick 的 manager 身份检查只作为遗漏事件的兜底。执行 `/flywheel backend instancing` 不再等待下一 tick 才发现 manager 改变。
- `CitizenRenderCoordinator` 在 tick 和 render 的健康检查之前比较当前 `ClientLevel`。维度变化先调用 `onClientLevelChanged`，保留 requested AUTO/Flywheel 并绑定新世界，再清 benchmark、cache、假实体和统计；只有新世界不支持 instancing 或重绑定异常才把 active backend 临时回退 CPU。旧世界的 `InstanceWorld` 可能已由 Flywheel 销毁，因此该路径不能对旧 `CitizenInstanceData` 调用 `delete()`。
- Flywheel `InstancingEngine.beginFrame` 在原点切换时先对全部 material 调用 `clear()`，旧 `CitizenInstanceData` 句柄即使继续 `markDirty()` 也不会重新进入 `GPUInstancer.data`；因此 backend 实现并注册 `InstancingEngine.OriginShiftListener`，在清槽回调中删除逻辑 Entry、切换到新原点并从 `ClientCitizenCache` 重新 `createInstance()`。重建复用 `packedLight` 和累计步态 phase，tick 中的原点身份检查作为遗漏回调的兜底。
- `clear`/`close` 删除仍有效的全部 `CitizenInstanceData`；已失效的 manager、原点槽或旧世界句柄只清逻辑 Entry。Citizen Flywheel backend 只接受 `BackendType.INSTANCING`；Flywheel 为 `BATCHING`/`OFF`、shader pack 禁用 instancing、候选构造失败或运行期异常时，coordinator 回退 CPU，但不会把 requested AUTO/Flywheel 改写成 CPU。Flywheel renderer reload 后若实例化重新可用，候选 backend 先初始化并重放 cache，成功后再原子替换 CPU。BATCHING 会把实例变换烘进 CPU 顶点，不能满足 `uTime` 动画和“快照才脏”的合同。
- 禁止“一个居民一个载体”或“一个居民一个 `EntityInstance`”。如果未来 Flywheel API 收紧公开 manager，再退回单载体适配，不能改成千实体。

先保留七张 `textures/entity/player/wide/*.png` 对应的七个材质批次。Body 与 Billboard 各最多七次提交；如果 RenderDoc 证明 draw call 仍是瓶颈，再在资源重载时构建运行时 atlas/array texture。原版纹理仍只从资源管理器读取，不复制进模组资产。

### 剔除与光照

第一版不需要 GPU culling。96 格 AOI 上限只有 1024 人，即使全部进入 Flywheel 实例缓冲，GPU 处理量也很低。应先依赖 Flywheel 后端剔除能力或绘制整个实例批次，以换取简单、稳定的主路径。

只有 JFR/RenderDoc 证明不可见实例开销显著时，再维护客户端渲染 cell：

- 建议 cell 为 `8x8` 或 `16x16` 格，只在客户端 tick/快照到达时更新成员。
- 每帧对 cell AABB 做一次视锥测试，不能继续为每名居民 `new AABB`。
- 室内光照可按 `4x4x4` 采样 cell 共享，近景假实体仍使用原版逐实体光照。
- 光照 cell 必须在区块卸载、资源重载和跨维度时失效；不要把光照写入居民持久化或网络包。

### 阴影、名称与交互

- 只有假实体层保留原版阴影；实例层默认不投原版实体阴影。若视觉需要，可另为约 24 格内的 Body 实例批量绘制贴地 blob shadow；该阴影范围是后续视觉策略，不改变详细假实体的 16/20 格迟滞与 64 个上限。
- 不批量绘制 1000 个姓名牌。姓名只在准星命中、交互锁定或界面明确请求时显示。
- `ClientCitizenCache.pick` 继续返回 citizen id，服务端仍以 `tracked`、距离和状态校验交互。渲染后端绝不能成为权限来源。
- 点击检测发生频率低，第一版可以继续线性扫描；只有交互 profile 证明有问题时才接入渲染 cell。

## 后端与回退契约

已实现最小接口，业务代码不直接依赖 Flywheel 0.6 内部类型：

```java
interface CitizenRenderBackend extends AutoCloseable {
    String name();
    boolean initialize();
    void onCitizenAdded(ClientCitizen citizen);
    void onCitizenUpdated(ClientCitizen citizen);
    void onCitizenRemoved(int citizenId);
    void tick(Minecraft minecraft);
    void render(RenderLevelStageEvent event);
    void clear();
    void onResourceReload();
    boolean onClientLevelChanged(ClientLevel level);
    boolean onRenderersReloaded(ClientLevel level);
    boolean isHealthy();
    void close();
}
```

`CitizenRenderCoordinator` 是 packet、benchmark 注入、客户端 tick、render、资源重载、renderer 重载、切维度和退出世界的唯一编排入口。它分别维护 requested backend（`auto`、显式 Flywheel 或显式 CPU）与 active backend（当前实际绘制者）。候选 backend 必须先 `initialize`、重放当前 `ClientCitizenCache` 并通过 `isHealthy`，随后才替换当前引用并关闭旧 backend；因此初始化失败不会造成空帧或双 backend 同时提交。维度切换不再被当作永久取消 Flywheel：coordinator 先调用 `onClientLevelChanged`，失败时只把 active backend 临时切到 CPU。Flywheel renderer 热重载通过 `onRenderersReloaded` 在事件当帧切换 manager；若当前是兼容 CPU 回退且 requested 为 AUTO 或 Flywheel，则在 Flywheel 默认 handler 重建 `InstanceWorld` 后自动创建候选 backend。非 CPU backend 的增量、render、level/renderer rebind、reload 或健康检查失败会记录错误并切回 CPU。render 已经部分提交后抛错时，当帧不再执行 CPU 二次绘制，以避免重影；下一帧恢复 CPU，最坏一个空洞帧。CPU backend 自身异常继续向上抛出，不能被回退逻辑掩盖。

当前与目标后端：

- `FlywheelCitizenBackend`：**动态实例代码已实现，基础实例生命周期计数已通过实机验证，完整图形/性能验收待完成**，负责动态 `CitizenInstanceData`、共享 Body/Billboard、七皮肤材质、原点、光照和资源生命周期；AUTO 在能力检查通过后选择它。
- `CpuBatchCitizenBackend`：**已实现且为 AUTO 的安全回退**，包装当前 `ClientCitizenRenderer`，用于不支持 instancing 的驱动、Oculus/Flywheel OFF 和 Flywheel backend 故障；Billboard 身体/头部与 Flywheel 共用同一布局。

当前已提供 `AUTO`、`FLYWHEEL`、`CPU_BATCH` 三种运行偏好，启动默认 `AUTO`。`AUTO` 仅在 Flywheel 后端成功初始化且当前渲染后端支持目标能力时启用实例化；运行中失败会记录明确日志、释放半初始化资源并切回 CPU 批量路径，不能让居民静默消失。调试命令可在本次客户端会话中切换偏好。

CPU 回退路径仍要应用 `maxDetailedCitizenEntities`，并可把 Body/Billboard 阈值调近以保护帧率，但不能改变服务端权威人口或交互校验。

## 实现路径

### M0：可测基线与近景止损

目标：在更换渲染后端前先消除最坏退化，并获得可重复数据。

- **已实现**：确定性纯客户端开发场景，支持 32/64/256/1024 四档、全移动和全睡眠；高位测试 id 与服务端同步居民隔离，切维度和退出世界自动清理。
- **已实现**：可查询及可开关 HUD 的渲染统计，包括 cache、backend-owned 对象、假实体、Body/Billboard、材质批次、实例脏写字节、光照采样数，以及最近最多 256 帧的 backend hook CPU latest/average/p95。
- **已实现**：`FakeCitizenManager` 使用固定容量 Top-K，默认最多 64；交易/准星目标优先，保留 16/20 格迟滞。
- **已实现**：一次缓存扫描同时完成候选收集和准星命中；未提升对象继续交给批量渲染。
- **已实现**：CPU 路径复用按需物化的扫掠 `AABB`，消除稳态逐居民逐帧包围盒构造；Flywheel 路径只记录失效状态，不为永远不会读取的 CPU 视锥盒分配对象。

自动化已覆盖四档布局、移动周期、Top-K 容量和统计窗口；游戏内仍需按本文测试矩阵执行 1024 人图形与 JFR/RenderDoc capture。核心验收不变：高密度场景中 `FakeCitizenManager` 的活跃实体始终不超过配置，且没有居民因名额不足而消失。

### M1：渲染所有权与回退边界

目标：先把“谁负责绘制一个 citizen”变成可测试的单一决策。

- **已实现**：新增 `CitizenRenderCoordinator`、`CitizenRenderBackend` 与 `CpuBatchCitizenBackend`。
- **已实现**：spawn/batch/despawn、benchmark 注入、客户端 tick、render、世界卸载和资源重载统一转交 coordinator；despawn 立即释放相同 id 的假实体。
- **已实现**：`CitizenRenderOwnership` 保证每个 id 每帧只有 `DETAILED_ENTITY`、`BODY_BATCH`、`BILLBOARD_BATCH`、`NONE` 中一个 owner；睡眠永不进入详细实体层。
- **已实现**：候选 backend 先初始化并从当前 cache 预热，再原子替换旧 backend；不健康或运行期异常把 active backend 回退 `cpu_batch`，半初始化资源会关闭。requested AUTO/Flywheel 保留并在 renderer reload 后自动重试，显式 `backend cpu_batch` 才取消重试。
- **已实现**：维度变化在 tick/render 健康检查前通知当前 backend 重绑定；Flywheel renderer 重载在其默认 handler 替换 `InstanceWorld` 后立即通知当前 backend，避免使用旧 manager 句柄。
- **已实现**：当前 `ClientCitizenRenderer` 由 CPU backend 调用，渲染阶段和网络包格式不变。

自动化已验证所有权边界、初始化拒绝、原子替换、资源重载健康回退、render 故障回退、世界切换保留 backend、level/renderer 重绑定失败回退，以及 Flywheel reload listener 的 `LOWEST` 优先级。仍需在实际客户端执行维度往返和 `/flywheel backend instancing` 热重载 smoke test；当前 CPU backend 的静态复用 `BufferBuilder` 不维护 citizen id 槽，退出世界后 cache、假实体和 backend id 状态归零，但缓冲容量按设计保留供下一世界复用。

### M2：Flywheel 静态实例验证（已由动态 backend 取代）

目标：先验证依赖和兼容性，不接动态网络状态。

- **已实现**：以单个 `FlywheelCitizenBackend` 直接持有共享 `MaterialManager`，没有载体实体和逐居民 `EntityInstance`。
- **已实现**：烘焙 6 盒体/144 顶点 Body，共享到七张原版宽臂皮肤材质；每居民使用 Flywheel `OrientedData`，stride `46 B`。
- **已实现**：候选 backend 缓存预热、实例创建/删除、详细 owner 互斥、96 格范围、一次性光照、Flywheel manager 重建、世界原点重写、世界清理和 CPU 回退。
- **已完成并由动态 backend 取代**：M2 静态路径创建时冻结位姿，不处理网络更新、肢体动画、睡眠旋转或 Billboard；当前代码不再包含该 backend 或命令入口。
- **已验收**：Flywheel instancing 下的 UV、cutout、深度、光照与 F3+T；Flywheel batching、Embeddium/Oculus 无光影/有光影仍属于独立支持矩阵。

M2 历史验收口径（当前代码已不能切回静态 backend）：64 名额的首次加载可能记录 `1024 × 46 = 47,104 B`，也可能把同帧 64 次提升删除合并为 `(1024 + 64) × 46 = 50,048 B`。F3+T 重建 960 个实例为 `(960 删除 + 960 创建) × 46 = 88,320 B`，常驻实例缓冲为 `960 × 46 = 44,160 B`。这些数值只用于解释已归档 M2 实机日志，当前动态实例的 58 B 预期见下一节。

2026-08-19 实机结果：Flywheel `INSTANCING` backend 下，1024 moving/64 详细名额稳定为 `detailed=64`、`body=960`、七个皮肤材质批次，稳态 `instanceDirtyBytes=0`；首次峰值 `50048 B`。F3+T 后数量、backend 和画面均保持正确，重建峰值精确为 `88320 B`。用户确认没有倒置、UV 错位、重影、穿透或异常光照。该结果关闭 M2 的基础 instancing 与资源重载门；Flywheel batching、Oculus 无/有 shader pack 仍是支持矩阵中的独立待测项。

M2 决策门已关闭并进入动态实例实现。Flywheel 0.6.11 可承载自定义实例 struct/program，但 instancing 模型池固定使用 `Formats.BLOCK`；当前 backend 通过静态顶点颜色通道传递 `partId`，没有引入直接 OpenGL 调用或 Flywheel 内部修改。

### Flywheel 动态实例、动画与睡眠

目标：替换实例 backend 中的每帧 CPU 盒体生成。代码、自动化和基础实例生命周期实机计数已完成，人工图形和性能门尚未关闭。

- **已实现**：`id -> Entry -> CitizenInstanceData` 稳定引用；实际 GPU 槽复用/删除由 Flywheel `Instancer` 管理。
- **已实现**：接入 `ClientCitizen` 双快照、0.05–1.0 秒窗口、最多 1.5 秒外推、视觉 yaw、state/dir/halt，并换算到 Flywheel `com.jozufozu.flywheel.util.AnimationTickHolder` 的循环 tick 时钟。
- **已实现**：订阅 `InstancingEngine.OriginShiftListener`；Flywheel 原点切换清槽后同帧重新创建全部批量实例，复用光照缓存与步态 phase，避免逻辑 `entries` 存在但 GPU 槽已消失。
- **已实现**：`citizen.vert` 完成位置插值、短路径分段速率转向、按实际快照位移/外推速度对齐原版标度的反相四肢摆动，以及睡眠 Body 与水平 Billboard 变换；实例 Body 不额外做整体 bob。
- **已实现**：packed light 只在客户端 tick 跨格或 5–8 tick 错峰查询；值改变才脏写，不在 render hook 查询世界。
- **已实现**：Body 进入 `<68` 格、保持到 `<=72` 格，Billboard 到 `96` 格；CPU 与 Flywheel 共享 `CitizenRenderCoordinator` 中的 LOD owner，睡眠切换沿用 `ClientCitizen` snap 语义，任一 id 同时只有一个实例 owner。
- **已实现**：仅 `BackendType.INSTANCING` 启用 Flywheel backend；BATCHING/OFF、Oculus shader pack、候选构造失败或运行期错误明确把 active backend 回退 `cpu_batch`，requested AUTO/Flywheel 不变。
- **已实现**：维度切换重新绑定新 `ClientLevel` 而不取消 Flywheel 请求；Flywheel backend/光影开关触发 renderer 重建时，在旧 manager 被销毁后丢弃失效句柄。实例化不可用时 CPU 接管；重新可用时从 cache 原子预热并恢复 Flywheel。

自动化已验证 58 B stride/字段偏移、六盒体 `partId`、Billboard 身体/头部双 quad 标签与 UV、时钟回绕、短路径 yaw、累计步态 phase、原版步速/步幅 shader 公式、68/72/96 LOD 边界和协调器 owner 保留、原点失效 listener 合同和 shader 资源打包。F3+T 后的实例释放/重建计数、基础 Body/睡眠画面、详细/实例行走一致性、68/72 格边界切换、Flywheel 100 格原点边界后的持续可见性以及新增 Billboard 头部的视觉连续性已通过实机验证；仍需确认停步、转向和远距 UV/朝向的完整组合。render frame 已不再为实例居民生成 Body 顶点。

Flywheel 实机预期：1024 moving/64 详细名额且测试网格全在 68 格内时为 `detailed=64`、`body=960`、`billboard=0`、`batchDraws=7`。每 4 tick 的 benchmark 快照会产生 `960 × 58 = 55,680 B` 脏写，两个快照之间应回到 `0`；首次“1024 创建 + 64 提升删除”可达 `63,104 B`。F3+T 或 Flywheel 原点切换删除并重建 960 个实例为 `960 × 58 × 2 = 111,360 B`，常驻缓冲仍为 `960 × 58 = 55,680 B`；1024 sleeping 的原点重建峰值为 `1024 × 58 × 2 = 118,784 B`。1024 sleeping 在近距应为 `detailed=0`、`body=1024`。若直接从 1024 moving/64 detailed 切换到 sleeping，960 个旧行走 Body 会先删除，再创建 1024 个睡眠 Body，因此迁移帧峰值为 `(960 + 1024) × 58 = 115,072 B`；这不是常驻缓冲或逐帧上传。将相机移到 68 格外、72 格内时，已有 Body 应保持；超过 72 格后应切为 Billboard，回到 68 格内才恢复 Body。

2026-08-19（历史：当时仍使用 68/72 迟滞）Flywheel 基础实机结果：1024 moving/64 详细名额稳定为 `detailed=64`、`body=960`、`billboard=0`、`batchDraws=7`，稳态 `instanceDirtyBytes=0`，首次峰值 `63,104 B`。F3+T 后保持相同所有权计数，峰值精确为 `111,360 B`。随后直接切换到 1024 sleeping，稳定为 `detailed=0`、`body=1024`、`billboard=0`、`batchDraws=7`，稳态再次回到 `0 B`，迁移峰值精确为 `115,072 B`。这些结果验证 shader/program 能加载、owner 数量正确、资源重建和模型迁移没有产生持续全量上传；`No active uniform 'uWindowSize'` 仅表示未使用 uniform 被驱动优化，是无害 DEBUG。人工观察确认 UV、方向、睡眠姿态、重影、深度和光照没有问题，但初版详细假实体呈走路、实例 Body 呈跑步。根因是旧 shader 固定使用 `0.6 rad/tick`、`0.55 rad` 摆幅和 `0.05` 格整体 bob，而假实体按实际位移驱动原版 `walkAnimation`。实现改为累计每段快照位移、按 `0.6662 rad/block` 推进相位、按实际步速缩放四肢并移除整体 bob 后，实机复验确认详细假实体与实例 Body 的行走已一致。68/72 格切换实机也未发现重影、空帧或抖动，但原 4 顶点 Billboard 只把躯干 UV 拉伸到全身，缺少头部导致远距仍能明显看出 LOD 跳变；现已改为 8 顶点身体/头部双 quad，实例数据、批次和 dirty-byte 合同不变。

2026-08-20 原点失效修复实机结果：跨越 Flywheel 渲染原点边界后画面保持正常，Body 没有再次闪灭或整体消失，不再需要通过 FakeEntity/Billboard 所有权切换恢复。该次反馈未附带 metrics 数值，因此只关闭画面与生命周期门，不作为脏字节峰值的测量证据。

2026-08-20 Billboard 头部实机结果：`cache=1024`、`detailed=0`、`body=0`、`billboard=715`、`batchDraws=7`，远景头部画面确认正常。稳态 `instanceDirtyBytes=0`；记录到的 `peakInstanceDirtyBytes=118784` 精确等于 `1024 × 58 × 2`，补充验证了全量原点重建的逻辑字节公式。`Backend hook CPU` 的 latest/average/p95 均为 `0.000 ms`，但该 hook 不包含 Flywheel render-layer 与 GPU 时间，因此不关闭 GPU 性能门。Oculus 兼容按用户安排留到后续验收。

2026-08-20 生命周期缺陷调查：用户实测切维度后 Flywheel backend 因仍绑定旧 `ClientLevel` 而在 render 健康检查回退 `cpu_batch`；执行 `/flywheel backend instancing` 后，Flywheel 同步替换 `InstanceWorld`，Citizen 在下一 tick 前仍持有旧 manager 的实例句柄，表现为 Body 位置错乱、局部空白和移动后消失。实现已改为健康检查前处理世界变化，并以 `ReloadRenderersEvent` 事件驱动重绑定 manager；相关 coordinator 自动化与完整 239 项测试通过。该修复仍需按下文步骤完成实际客户端复测，不能仅凭 JUnit 关闭图形生命周期门。

2026-08-20 Oculus 兼容回退修复：确认 Flywheel 0.6.11 在 shader pack 使用中主动选择 `OFF`，Create 0.5.1 同样改走标准渲染回退。Citizen 现已分离 requested/active backend，AUTO/Flywheel 因光影临时回退 CPU 后不会丢失用户意图，光影关闭后的 `ReloadRenderersEvent` 自动恢复 Flywheel；调试状态同时报告两者。CPU 与 Flywheel 改为共用 `CitizenBatchRenderLayout`：Billboard 站立/睡眠均绘制独立身体和头部；Body 共用六部件尺寸、UV、睡眠锚点和步态语义，累计 phase 由 `ClientCitizen` 持有，backend/LOD 重建不再重置。CPU Body 同时移除 Flywheel 已取消的整体 bob，并按相同快照速度驱动反相四肢。自动化已覆盖共享布局、临时回退恢复、Flywheel 健康失败后的恢复和显式 CPU 取消恢复；Oculus 实机画面与性能仍需按测试矩阵复测。

### M4：近景提升与交互连续性

目标：让少量原版高质量模型与实例人群稳定共存。

- 将 M0 的 Top-K 结果接入 coordinator 所有权迁移。
- 目标假实体完全 ready 后再删除对应实例；降级时反向执行。
- 固定交易对象和准星目标，验证 64 人上限下的优先级与迟滞。
- 关闭实例层名称牌和阴影，保留 citizen id 的 RPC 交互。

验收：在 LOD 边界绕行 2 分钟不频繁创建/销毁；与任意居民交互时不会点到重影或因提升失败丢失目标。

### M5：性能收口与默认启用

目标：以数据决定是否需要 cell 剔除、运行时 atlas 和更低 LOD。

- 用 JFR 检查客户端 tick/render allocation，用 RenderDoc 检查 draw call、buffer upload 和 GPU pass。
- 如果逐人不可见绘制仍有明显成本，再加 cell 级视锥剔除。
- 如果十四个 Body/Billboard 皮肤批次仍显著，再加运行时皮肤 atlas/array texture。
- 如果 GPU Body pass 超预算，再引入第三档低模；不要在没有证据时先做。
- 完成游戏内故障注入：Flywheel backend 初始化失败、资源重载失败、切维度中断、配置热切换；level/renderer 重绑定的成功与失败路径已有自动化覆盖。

`AUTO` 现已成为发布默认并保留 `CPU_BATCH` 诊断开关；完整 Oculus 图形矩阵与 JFR/RenderDoc 预算仍是发布验收项，若失败可显式锁定 CPU 而不影响居民正确性。

## 性能验收合同

性能结果必须记录 CPU、GPU、内存、分辨率、渲染距离、Java 版本、Embeddium/Oculus/Flywheel 配置和 shader pack；只报 FPS 没有可比性。

推荐在 1080p、渲染距离 12 chunks、固定相机与固定种子的开发世界中，对每个场景预热 30 秒、采样 120 秒，报告 p50/p95/p99：

| 指标 | 1024 cache / 1000 视锥内目标 |
|---|---|
| 活跃 `FakeCitizenEntity` | `<= maxDetailedCitizenEntities`，默认 `<=64` |
| citizen render-thread CPU | p95 `<=2.0 ms/frame` |
| citizen GPU pass | p95 `<=3.0 ms/frame` |
| 稳态 render allocation | p95 `<=1 KiB/frame`（居民路径归因） |
| 实例缓冲 | `<=64 KiB / 1024 residents`，不含共享静态网格 |
| crowd LOD 批次 | 首版 `<=14`，不计最多 64 个近景原版实体 |
| 世界卸载后资源 | 0 假实体、0 instance slot、0 retained cache |
| LOD 切换 | 无可见双影；无超过 1 render frame 的空洞 |

还必须保留对照组：当前 CPU batch、仅加入近景上限（32 与 64 两档）、完整 Flywheel backend。这样可以区分收益来自“限制实体”还是“真正实例化”，并用相同场景判断 64 是否适合作为通用默认。

## 测试矩阵

### 自动化

- Top-K：交易对象、准星对象、距离、稳定 id、迟滞、容量 0/1/32/64。
- 槽位：随机 spawn/update/remove、swap-remove/free-list、重复 despawn、资源重建。
- 所有权：任一 id 在同一 frame 最多只有一个绘制 owner。
- LOD：68/72/96 和 16/20 边界来回移动不抖动，并且 CPU/Flywheel 的同一居民共享同一批量 LOD owner。
- 数据布局：`StructType` 字节布局、packed flags、光照、yaw 与睡眠姿态。
- 生命周期：退出世界、切维度、资源重载、backend 故障回退后集合为空；level/renderer 重绑定失败时 active 回退 CPU 但 requested AUTO/Flywheel 保留；Flywheel reload listener 在默认 renderer reset 后执行并自动恢复 Flywheel；显式 CPU 选择取消恢复。
- 现有 citizen 网络、插值、睡眠与交互测试必须继续通过。

### 游戏内与图形验证

- 白天 1024 人同向移动、交叉移动、拥堵停步。
- 夜间 1024 人四种床朝向，床破坏与早晨错峰醒来。
- 第一/第三人称快速转身，16/20、68、72、96 格边界。
- 室内、室外、昼夜、方块光、天空光与阴影。
- GUI/交易打开期间配置切换和资源重载。
- Vanilla Forge、Embeddium、Embeddium + Oculus，光影开关各一轮。

图形测试需要截图与 RenderDoc capture；JUnit 只能验证数据布局和坐标变换，不能证明 GPU shader、UV、lightmap 或 shader-pack 兼容正确。

## 风险与应对

| 风险 | 应对 |
|---|---|
| Flywheel 0.6 API 常规入口以实体/方块实体为主 | M2 已验证 0.6.11 可由单 backend owner 使用公开 world `MaterialManager`；若后续 API 收紧再退回单载体，业务层始终只依赖自有 backend 接口 |
| Oculus/shader pack 改写渲染阶段 | 遵循 Flywheel 0.6.11/Create 0.5.1 的双路径策略，不绕过 shader-pack 禁用检查；active 自动回退标准 CPU batch，requested AUTO/Flywheel 保留并在 renderer reload 后恢复 |
| 载体或批次被错误视锥剔除 | M2 已验证载体 bounds/原点移动；必要时按 cell 拆少量载体，而不是按居民拆 |
| Flywheel 原点切换清空自定义实例槽 | backend 注册 `OriginShiftListener`，清槽后从 cache 同帧重建并保留光照/步态；tick 原点检查兜底，使用跨 100 格实机用例验收 |
| 维度或 `/flywheel backend` 重载使旧 manager 句柄失效 | 健康检查前重绑定 `ClientLevel`；以 `EventPriority.LOWEST` 监听 `ReloadRenderersEvent`，在 Flywheel 替换 `InstanceWorld` 后重建，失效句柄只丢弃、不再调用旧 instancer API |
| 实例槽删除导致 id 指向错误 | 集中封装 slot table，随机序列测试 swap/remove 与 free-list |
| LOD 切换重影/空洞 | coordinator 单一所有权；目标 ready 后原子迁移；边界迟滞 |
| 运行时 atlas 增加资源重载复杂度 | 首版保留七材质批次；只有 profile 证明需要才做 atlas |
| 光照按 cell 共享造成室内外串光 | 首版可继续逐居民限频缓存；cell 光照单独作为有视觉验收的后续优化 |
| 回退路径长期腐化 | CI 保留 CPU backend 的纯逻辑测试，每个发布周期执行一次游戏内回退 smoke test |

## 最终决策

- 保留服务端 `CitizenSim`、`SyncEngine`、96 格 AOI 与紧凑包协议；千人同屏的首要改动在客户端。
- 立即为 `FakeCitizenManager` 增加独立的近景数量硬上限；距离阈值不能替代数量上限。
- 主人群使用共享静态网格 + 紧凑 `InstanceData`，动画和插值移到 GPU。
- 优先复用项目强制依赖的 Flywheel，通过单 backend owner 和自有接口隔离 0.6 API；不创建载体实体或千个 Flywheel `EntityInstance`。
- AUTO 默认优先使用 Flywheel，并永久保留 CPU 批量渲染作为不支持实例化、Oculus 光影和 Flywheel backend 故障时的兼容回退。
- 以 1000 视锥内居民的组件级 p95 预算验收，不以主观“看起来不卡”或单次 FPS 截图验收。
