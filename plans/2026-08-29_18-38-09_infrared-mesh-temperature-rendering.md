# 红外视野实际温度场最小增量实现计划

- Time: `2026-08-29 18:38:09 +0800`
- Last revised: `2026-09-01 21:20:55 +08:00`
- Authors: `TeamMoeg; Codex (GPT-5, original architecture and final minimal-increment revision)`
- Status: `in-progress`
- Scope: `InfraredViewRenderer`, `infrared_view.fsh`, existing infrared packets, `MinecraftThermalInput`, `MinecraftPageManager`, `PagePublication`, `QueryPublication`
- Related: [world-climate-and-temperature.md](../docs/climate/world-climate-and-temperature.md), [thermal-runtime-architecture-and-optimization.md](../docs/climate/thermal-runtime-architecture-and-optimization.md), [thermal async/runtime topology plan](2026-08-28_01-18-39_thermal-async-runtime-topology-refactor.md#brick-residency-and-source-independent-propagation-correction)

## Goal

红外视野显示 thermal runtime 已求解并发布的实际空气温度，并满足：

- 不周期重复发送未变化的温度 Pages。
- 不为减少少量控制流量引入服务端 observer 或 per-player 状态。
- 不 admission、不 retain Page，也不加载区块。
- physical source 只通过 solver 改变空气温度，不直接进入红外 payload。
- `ThermalAnalyticField` 不进入红外纹理。
- shader 每个可见像素只做一次温度纹理采样，不循环 HeatArea。
- 复用现有请求包、响应包和网络注册。

## Verified Current State

- `InfraredViewRenderer` 当前使用 4-chunk、即 64-block 球形扫描半径。
- 当前请求只在玩家跨 chunk 时发送；响应携带 analytic fields 和 physical sources
  生成的最多 512 个 `HeatArea`，不是 solver 输出。
- 当前 fragment shader 对每个可见像素循环最多 512 个 HeatArea。
- `PagePublication` 可以把 `16 x 16 x 16` Page 内的位置解析到实际 Air arena slot。
- `QueryPublication` 已用双缓冲发布 slot 温度和 lifecycle generation。
- `MinecraftPageManager` 已维护 `pagesByChunk`，可按 9 x 9 chunk buckets 枚举
  实际存在的 Page entries。
- thermal Page 是稀疏 admission 的运行时对象；候选 section 位置不等于已存在 Page。
- 同一维度 `ThermalDimensionLimits.maximumPages()` 当前为 3200。

## Workload Assumption

目标规模是同一维度 100 名开启红外的玩家，玩家观察区域不重叠。红外不创建额外
Pages，因此所有玩家能读取的唯一 Pages 总数仍受维度级 3200 上限约束。

`maximumPages=3200` 只是红外可枚举 Page 的硬上限，不证明 100 个 source 的
thermal residency、cell、pair 或 boundary 成本已经通过。该容量结论归上面的 thermal
runtime plan 和其 100-source fixture；本文只计算已有 publication 的查询与网络成本。

本计划允许每两秒约 5 KiB 的全服 C2S 精确状态查询，以换取：

- 静态场景 S2C 为零；
- 服务端不保存玩家红外状态；
- 不增加订阅、frame、continuation、cache 或 hash。

## Cross-System Residency Dependency

红外是 thermal publication 的只读消费者，绝不反向拥有 Page/Brick 生命周期。当前
Page-wide player/source/continuation residency 已被源码和实机行为否决；后续实现以
thermal runtime plan 的 Brick correction 为唯一权威：

- source 以精确目标 Brick 作为 seed；
- worker 根据已求解温度、真实开放面和统一 gameplay error gate 持有/扩张 Brick；
- 非天空缺页不再自动接弱自然温度 FarField；
- 玩家离开或跨 section 不能清除 source/余热拥有的状态；
- Page 只有在 worker desired mask 与主线程 seed 都为零后才允许退休；
- infrared poll、presence 和 `pagesByChunk` 枚举不会 admission、续租或影响这些 mask。

因此 `invalid/MIN_TEMP` 只表示当前没有 coherent thermal Brick publication；它不能由
观察者移动直接制造。对同一世界位置，只要 source、拓扑和求解状态未变，玩家跨
X/Y/Z section 后重新观察必须得到同一 Brick 温度。

## Required Result

```text
client open / move / staggered 40-tick poll
        |
        v
existing C2S:
requestId + forceFull + lastTemperatureChangeId + knownPresence
        |
        v
server derives actual center
        |
        v
MinecraftPageManager pagesByChunk
81 bucket lookups + actual Page entries
        |
        v
compare exact presence and temperature change IDs
        |
        +--> identical: no S2C
        |
        +--> presence mismatch / forceFull: full current Page snapshot
        |
        +--> temperature changed: changed Page records only
        |
        +--> changes outside view: header-only ACK
        |
        v
client full rebuild or delta patch
        |
        v
one GL_R16I texture upload
        |
        v
shader: one isampler3D fetch
```

## Decisions

### D1 Candidate Cube And Existing Pages

当前扫描半径是 64 blocks。以玩家当前 chunk 和 eye section 为中心，完整包围该球体的
section cube 为：

```text
horizontal: center +/- 4 chunks   = 9
vertical:   center +/- 4 sections = 9
candidate positions               = 9 * 9 * 9 = 729
```

客户端 presence 使用 12 个 longs 精确表示 729 个 local Page positions。

服务端不逐个查询 729 个 section keys，而是复用现有 `pagesByChunk`：

1. 遍历中心周围 `9 x 9 = 81` 个 chunk buckets。
2. bucket 不存在时直接跳过。
3. 只遍历 bucket 中实际存在的 section keys。
4. 过滤 `centerSectionY +/- 4`。
5. `ThermalPageHandle` 存在、`lastPublication()` 非空且 QueryPublication 未超龄时，
   设置对应 presence bit。

presence 表示“该 Page lifecycle 已经拥有至少一个 coherent worker cut”，不把普通
geometry mutation 的短暂 stale 当成 Page retirement。温度编码优先使用
`currentPublication()`；它暂时为 null 时使用 `lastPublication()`，仍由 read cursor
校验 topology、slot generation 和 sample age。真正 retirement 后 handle 消失，
presence 才清除。

复杂度为 `O(81 + actual Page entries)`，不增加新索引、cache 或 Page admission。

### D2 One Actual Temperature Per Thermal Brick

> Superseded on `2026-09-01`: D2-D5, D7-D8, the old Explicit Non-Goals,
> Complexity/Implementation/Validation sections, and the old Revision
> Outcome/Outcome describe the implemented Brick-representative protocol but no
> longer define the next implementation. They are retained as investigation
> history only. D1 Page enumeration, D6 40-tick staggering, and the
> cross-system residency boundary remain valid. The authoritative correction is
> `Block-Resolved Actual Air-Cell Correction` below.

一个 Page 没有单一温度。Page 内有 64 个 `4 x 4 x 4` thermal Bricks，不同 Brick
可以属于不同空气区域并具有不同温度。每个 Page record 因此固定发送 64 个 Brick
texels。

- 普通 Brick 直接读取 coverage Air slot。
- mixed Brick 检查八个八分体中心的 block-center 样本，从有效 Air slots 中选择
  出现次数最多者；平票时选择最小 slot。
- mixed Brick 最多执行 8 次 `resolveAirPoint`，不发送 component 或 geometry。
- 没有有效 Air slot 时写 `Short.MIN_VALUE`。
- 有效温度编码为
  `clamp(round(temperatureC * 4), -32767, 32767)`，即 0.25 degC 精度。
- `GL_NEAREST` 采样，不插值 invalid texel 或相邻 Brick。

该表示是 Brick 级实际空气温度，不承诺逐方块或 component-exact 显示。

### D3 Minimal Temperature Change Tracking

`revision` 在本计划中只是运行时温度变化编号，不是软件版本、构建版本或 hash。
实现命名使用 `temperatureChangeId` 和 `pageChangeIds`。

`QueryPublication` 增加：

- 一个维度级 `temperatureChangeId`；
- `long[maximumPages] pageChangeIds`，3200 Pages 时约 25 KiB；
- 一个维度级 `infraredActiveUntilTick`；
- 一个 caller-owned `InfraredReadCursor`。

`MinecraftThermalInput.createWorker` 先构造 `ThermalDimensionLimits`，再调用
`QueryPublication.tryCreate(dimensionBudget, initialCellCapacity, maximumPages)`。
`pageChangeIds` 的 25,600 bytes 和可增长 cell buffers 分别持有一个 reservation
token，但都向同一个 dimension/server budget 计费；cell capacity 扩容只替换 cell
buffer reservation，不重复计费或重建固定 Page backing。任一初始 reservation 失败
时释放另一 token 并返回 null，close 时释放两者。不另建预算或事后扩容。

每次红外请求把 `infraredActiveUntilTick` 延长到当前 tick + 80。inactive 转 active
时，在一个 QueryPublication write 中推进一次维度 change ID，并把完整
`pageChangeIds` backing 设为该值；触发 reactivation 的请求发送 full snapshot，
其他仍持有旧 change ID 的客户端会在下一 poll 收到全部可见 Page delta。之后
worker publish：

- deadline 已过时只检查一次标量条件，不执行红外比较；
- deadline 有效时复用现有 live-slot publish 循环；
- 对 Air cells 比较新旧 0.25 degC 量化值；
- slot generation 变化也视为改变；
- 一个 Page 内任意 Air cell 改变时，只把该 Page 标记为本次
  `temperatureChangeId + 1`；
- publish 结束有 Page 改变时才提交新的维度 change ID；
- `republishUnchanged` 不推进 change ID；
- 不增加第二次 cell/Page 遍历。

为确定 cell 所属 Page，只恢复 QueryPublication 生产使用的只读 arena
`pageSlot`/Air-kind 访问；不恢复测试接口或诊断 metadata。

`PagePublication` 增加仅服务端内部使用的 `workerPageSlot`。主线程据此从 cursor
读取对应 `pageChangeIds`；该 identity 不进入 wire 或客户端数据模型。

Page geometry/topology publication 改变时，`ThermalDimensionEngine` 通过现有
Page writes 把对应 Page 标记为温度数据已变化。Page admission、retirement 由客户端
presence 精确发现，不需要全局 presence revision。

`InfraredReadCursor`：

- 固定 published buffer、publication version、topology generation、sample tick、
  temperature change ID 和 Page change IDs；
- 对响应内多个 slots 做无分配读取并校验 slot generation；
- 响应完成后验证一次 publication version；
- 验证失败时不发送响应，下一次 40-tick poll 自然重试，不循环自旋。

worker restart 后新 QueryPublication 的 change ID 可以重新从零开始。服务端发现
客户端 change ID 大于当前值时直接发送 full snapshot；新 publication 的首次请求
同时按上述 inactive-to-active 规则重建 change-ID 基线，不增加 generation/session
或 per-player 状态。

### D4 Exact Client-Carried Presence

服务端不保存玩家上次看见的 Page set。客户端在每次稳定 poll 中携带自己当前纹理的
精确 presence：

```text
12 longs = 768 bits
used bits = 729
unused high bits = 0
```

服务端用 D1 的 `pagesByChunk` 枚举构造当前 12 longs，并做精确比较：

- presence 相同：允许发送 temperature delta。
- presence 不同：发送 full snapshot；客户端清空后重建。
- Page retirement、首次 publication 和 worker replacement 会自然形成 mismatch；
  普通 geometry mutation 继续使用 last coherent publication，不改变 presence。
- 不使用 checksum、hash、概率判断或全局 presence revision。

### D5 Existing Request/Response Packets

不新增 packet 类，不改变 `FHNetwork` 的两条现有注册。原位修改现有请求和响应。

C2S：

```text
VarInt requestId
boolean forceFull
VarLong lastTemperatureChangeId
12 longs knownPresence
```

- 开启红外、跨 chunk、跨 eye section 或维度切换时 `forceFull=true`。
- forceFull 请求仍写固定 12 longs，保持单一路径 codec；服务端忽略其内容。
- 服务端始终从 `ServerPlayer` 实际位置派生中心和范围。

S2C：

```text
VarInt requestId
int centerChunkX
int centerChunkZ
int centerSectionY
VarLong temperatureChangeId
boolean fullSnapshot
VarInt pageRecordCount
repeated:
    VarInt localPageIndex
    64 signed shorts
```

服务端决策固定为：

1. `forceFull=true`、客户端 change ID 大于当前值、presence mismatch 或
   QueryPublication 从无效恢复：发送 full snapshot。
2. change ID 相同且 presence 相同：不发送 S2C。
3. change ID 已推进且 presence 相同：只发送
   `pageChangeIds[pageSlot] > lastTemperatureChangeId` 的可见 Pages。
4. change ID 已推进但当前区域没有 changed Page：发送 pageRecordCount 为 0 的
   delta ACK，使客户端推进 change ID。
5. QueryPublication 整体无效或超过现有 40-tick age：不发送响应，保留客户端最后
   有效 snapshot；publication 恢复后由现有 change ID/Page baseline 发送重建数据，
   worker replacement 的 change-ID rollback 仍自动发送 full。

响应 payload 只持有一个最终长度的 flat `short[] pageRecords`：

- 每条内存记录为一个 unsigned local Page index 加 64 个 signed temperatures，
  共 65 shorts；
- wire 编码把首个 short 转为 VarInt，其余 64 个直接写 short；
- 禁止 `List<PageRecord>`、Page record 对象、每 Page 数组或装箱集合。

单个 full response 的协议级上限：

```text
729 * (2-byte local index upper bound + 64 * 2-byte temperature)
= 94,770 bytes
```

加 header 后仍小于 96 KiB，远低于 Forge 1.20.1 的 1 MiB clientbound custom
payload 上限。不增加分片、自定义压缩或发送预算。

### D6 Forty-Tick Staggered Polling

- 开启、跨 chunk 或跨 eye section 时立即请求。
- 稳定视野每 40 ticks poll 一次，即 2 seconds。
- 周期 poll 使用 entity ID 错峰：

```java
Math.floorMod(clientGameTime + player.getId(), 40) == 0
```

- 同一 client tick 已发送开启/移动请求时，不重复发送周期 poll。
- 关闭红外或切换维度时递增本地 request ID 并停止请求，使在途响应失效。
- 40-tick poll 把维度 tracking deadline 延长到 80 ticks；这是一个维度级活跃窗口，
  不保存玩家 observer、引用计数、heartbeat 状态或 cleanup 生命周期。

100 人静态场景的固定应用层 C2S 上限约为：

```text
requestId + flag + changeId + 12 longs ~= 100-111 bytes/request
100 players / 2 seconds ~= 50 requests/s
total ~= 5-5.5 KiB/s
S2C = 0
```

这是为避免服务端 observer/per-player 生命周期状态而保留的唯一周期控制流量。

### D7 Linear Client Texture

每个 Page 对应 `4 x 4 x 4` texels。9-section cube 对应：

```text
texture = 36 x 36 x 36
texels = 46,656
GL_R16I bytes = 93,312
CPU short[] bytes = 93,312
combined fixed data = 186,624 bytes, about 182.25 KiB
```

- texture 使用普通线性坐标，锚定最近接受响应的中心。
- full snapshot 先用 `Short.MIN_VALUE` 清空 mirror，再写入 records。
- delta 只覆盖 records 对应的 Pages；presence 已验证相同，因此不需要 clear。
- pageRecordCount 为 0 的 ACK 只推进 change ID，不上传纹理。
- 有温度 records 的响应结束后执行一次完整 `glTexSubImage3D`。
- shader 从世界坐标计算 texel；64-block sphere 外或 cube 外保持原画面，
  扫描球内的 invalid/无 Page texel 继续沿用旧红外语义，按 `MIN_TEMP` 显示冷蓝。
- depth 重建点在 `4-block` Brick 边界上没有唯一体素归属；采样点沿 camera ray
  向摄像机偏移 `1/2048` 的相对距离，稳定选择可见表面前方的 Air texel。该路径
  只有一次常量向量乘加，不增加 texture fetch、法线重建或过滤。
- shader 不读取 UBO，不循环 source、analytic field 或 Page records。
- level unload、resource reload 和客户端关闭时释放 3D texture。
- 不实现 toroidal/ring、CPU 区域平移或 texture copy。

### D8 Reuse And Deletion Boundary

修改并复用：

- `FHRequestInfraredViewDataSyncPacket`
- `FHResponseInfraredViewDataSyncPacket`
- `InfraredViewRenderer`
- `infrared_view.fsh`
- 现有 `FHNetwork` packet registrations
- `MinecraftPageManager.pagesByChunk`

增加：

- `QueryPublication.InfraredReadCursor`
- 维度/Page temperature change IDs 和 80-tick active deadline
- `PagePublication.workerPageSlot`
- QueryPublication 所需的最小 arena Page ownership 读取
- `MinecraftPageManager` 的 main-thread region enumeration 方法
- `MinecraftThermalInput` 的 full/delta snapshot 入口

确认无其他调用后删除：

- `MinecraftThermalInput.gameplayInfraredFields`
- `ThermalAnalyticFieldIndex.appendInfrared`
- `ThermalAnalyticField.writeInfrared` 和 infrared shape mode
- `PhysicalSourceSpatialIndex.appendInfraredFields`
- HeatArea UBO、`adjustNum` 和旧 `updateData` overloads
- shader 中的 `HeatArea`、`Adjusts` 和 source/analytic 循环
- 不再使用的 `FHConfig.CLIENT.infraredViewUBOOffset`

保留 physical source 对 solver/radiation 的输入和 analytic field 对 gameplay
temperature compositor 的作用。

## Explicit Non-Goals

本次不实现：

- 周期 full Page snapshot；
- 全局 presence revision 或 Page retirement event log；
- `InfraredObserverManager`、subscription packet 或服务端 per-player 状态；
- Brick 级 change IDs；
- payload memo、cache、LRU、hash 或跨玩家请求合并；
- 异步/后台 packet encoder；
- 自定义压缩、分片、frame/part/continuation；
- Page admission、红外 lease 或 chunk load；
- 2-block/1-block 纹理；
- source/analytic field 图形 fallback；
- Page record 对象、列表、每 Page 子数组或装箱 payload；
- 生产统计计数器、测试 getter 或诊断集合。

## Complexity And Performance Contract

设：

- `N`：开启红外的玩家数；
- `V_i`：第 i 名玩家区域内实际有效 Pages；
- `C_i`：第 i 名玩家本次可见 changed Pages；
- `A = sum(V_i)`；
- `C = sum(C_i)`。

则：

| Path | Bound |
|---|---|
| 红外 inactive | 每次 publish 一次 deadline 判断；零 Page 红外比较/编码 |
| 静态 poll Page 枚举 | `N * 81 / 2` chunk bucket lookups/s 加实际 entries |
| 静态网络 | 约 `N * 50-55 bytes/s` C2S；零 S2C |
| active publish tracking | 复用 live-slot pass，最多 65,536 次量化比较/s/维度 |
| inactive-to-active | 每次活跃窗口开始一次 `O(maximumPages)` change-ID 基线填充 |
| changed Page sampling | `C * 64 / 2` samples/s，按 2-second poll 上界 |
| changed Page S2C | `C * 130 / 2` bytes/s |
| 固定服务端红外内存 | 约 25 KiB Page change IDs/active dimension |
| 单客户端固定温度数据 | 约 182.25 KiB |
| fragment shader | 每个有效世界像素一次 integer 3D texture fetch |

在“100 人、同一维度、区域不重叠”目标条件下，红外查询侧的 `A <= 3200`。即使所有实际 Pages
每个周期都改变：

```text
chunk bucket lookups <= 100 * 81 / 2 = 4,050/s
Brick samples        <= 3200 * 64 / 2 = 102,400/s
S2C payload          <= 3200 * 130 / 2 = 208,000 bytes/s
                     ~= 203 KiB/s ~= 1.66 Mbps
```

静态场景没有 Brick sampling 和 S2C temperature payload。不同维度分别应用各自
maximumPages 上限。这些数字不包含 thermal capture、topology 或 solver 工作，不能
再作为 Page-wide source domain、固定 27-Page cube 或 100-source thermal capacity 的证据。

## Implementation Steps

1. 先构造 `ThermalDimensionLimits`，再给 `QueryPublication.tryCreate` 传入
   `maximumPages`；增加 read cursor、temperature/Page change IDs 和 80-tick
   active deadline，复用现有 publish live-slot loop。
2. 给 `MinecraftPageManager` 增加基于现有 `pagesByChunk` 的 81-bucket region
   enumeration，不增加新索引。
3. 在 `MinecraftThermalInput` 实现 presence 比较以及 full/delta/ACK 决策，
   payload 使用一个 flat `short[]`。
4. 原位修改现有 C2S/S2C packet codec 和 handler，不增加网络类或注册。
5. 将 `InfraredViewRenderer` 改为 40-tick staggered polling、精确 presence、
   线性 `GL_R16I 36 x 36 x 36` 纹理和一次上传。
6. 将 shader 改为一次温度 fetch，并删除 D8 中的旧 HeatArea/UBO 路径。
7. 更新 living docs，运行定向测试和一次性能验证，记录 diary。

## Validation

### Automated

- 0.25 degC 量化边界只推进所属 Page 和维度 change ID。
- `QueryPublication.tryCreate` 对固定 Page backing 和可增长 cell buffers 分别计费；
  cell capacity 扩容不重复计费 Page backing，失败/close 释放全部 tokens。
- `republishUnchanged` 和量化值不变不推进 change ID；inactive-to-active 恰好推进
  一次 change ID 并重建完整 Page 基线，连续 poll 不重复初始化。
- Page topology/geometry replacement 即使温度相同也推进对应 Page change ID。
- `PagePublication.owned` 和 `withIdentities` 保留同一 `workerPageSlot`，EMPTY 使用
  invalid sentinel，worker identity 不进入 wire。
- geometry mutation 期间 `currentPublication()` 暂时为 null 时，presence 保持且读取
  last coherent publication；retirement 才清除 presence。
- cursor 不混合两个 published buffers；版本变化时本次响应不发送。
- `pagesByChunk` 枚举与 729-position brute-force 测试 fixture 得到相同 presence，
  生产代码不保留 brute-force 路径。
- forceFull、presence mismatch 和 change-ID rollback 产生 full snapshot。
- presence/change ID 相同不产生 S2C。
- 只有温度变化时只发送 changed Pages。
- 区域外变化产生零-record ACK。
- Page retirement/admission 通过 presence mismatch 在下一 poll 完整重建。
- C2S/S2C codec 可确定往返；729 Page full packet 小于 96 KiB。
- packet 只持有一个 flat `short[]`，不存在 per-Page payload 对象。
- 40-tick poll 按 entity ID 错峰；即时请求和周期请求同 tick 不重复。
- 现有 thermal JUnit 和 Forge GameTest 继续通过。

### Rendering

- shader 中不存在 HeatArea 数组和按 source 数量循环。
- source 隔墙时不出现直接功率图形，只显示 solver 已传播到 Brick 的温度。
- 同一固定 world position 在玩家跨越 X/Y/Z section、离开再返回且 source/拓扑未变时，
  保持同一温度量化值；不得因 observer lease 改变而在橙色与 invalid 蓝色之间跳变。
- source domain 穿过 section 时没有 Page 形状的颜色边界；真正 Page retirement 才通过
  presence mismatch 清除客户端数据。
- `/heat_adjust` 等 analytic field 不直接出现在红外纹理。
- 扫描球内的 invalid 和无 Page 区域按 `MIN_TEMP` 显示冷蓝；cube 外和
  64-block sphere 外保持原始画面。
- 负坐标、chunk/section 边界、快速移动、切换视野和维度切换映射正确。
- 位于 `X/Y/Z = 4n` 的墙面、地板和天花板从两侧观察时，各自稳定读取观察侧
  Air texel；静止或移动镜头均不出现相邻体素争用闪烁。
- depth、逆视图矩阵和世界坐标原点使用同一个 main `Camera`；潜行眼高平滑、
  第一/第三人称切换不会移动温度场。
- 篝火烟雾等写 depth 的粒子沿用统一世界坐标采样，与所在 Air texel 的温度颜色
  融合；不增加粒子专用 mask、pass、shader 或渲染阶段分支。
- Iris/Oculus 和原版 depth 路径继续正常工作。

### Performance

只做一次目标负载验证，不增加生产诊断：

- 100 个不重叠 fixture 视野的静态 poll 只产生约 5-5.5 KiB/s C2S 和零 S2C。
- 3200 个实际 Pages 全部变化时，两秒周期折算不超过约 203 KiB/s S2C 和
  102,400 Brick samples/s。
- active deadline 过期与开启静止场景各采集一次服务端 JFR，确认 inactive 路径没有
  逐 cell 红外工作。
- 对相同画面比较旧 HeatArea shader 与新 3D texture shader，确认 fragment shader
  不再随 source 数量增长。

## Documentation Impact

实施完成后：

- 更新 [world-climate-and-temperature.md](../docs/climate/world-climate-and-temperature.md)，
  将 source/analytic HeatArea 描述改为 Brick 级 Page publication 温度。
- 更新 [data-lifecycle-and-integration.md](../docs/climate/data-lifecycle-and-integration.md)，
  记录 40-tick client-carried presence poll、Page change IDs 和无 Page admission。
- 删除其他 living docs 中失效的 HeatArea/UBO/source blob 描述。
- 新增 diary 条目，记录实际 Page 数、静态/变化流量、测试和一次性能结果。

## Revision Outcome

此前的两个极端方案均已废弃：

- observer/cache/frame 方案为了静态零控制流量引入过多长期状态；
- 周期完整快照方案虽然简单，但会重复发送未变化 Pages。

最终方案保留一个维度温度 change ID、每 Page change IDs 和客户端携带的精确
presence。它以约 5 KiB/s 的 100-player 静态 C2S 为代价，实现静态零 S2C、
changed-Page-only payload、无服务端玩家状态，并复用现有 `pagesByChunk` 把稳定
Page 枚举降为 `O(81 + actual entries)`。Page/Brick 是否存在及其热状态寿命明确归
thermal worker residency，而不是红外观察范围；本计划不再承担或暗示 Page admission
架构结论。

## Outcome

生产实现、协议 codec、定向 JUnit、Forge GameTest 和 living docs 已完成。旧
HeatArea/source/analytic/UBO 红外路径已删除；客户端 smoke 中 fragment shader
成功加载，full response 到达 render thread。两次上传暴露同一个 NVIDIA
`glTexSubImage3D` native crash；第二次已绑定 PBO 0，排除了 PBO 假设。源码调查
确认 Embeddium 插值动画进入 Vanilla `NativeImage.upload`，其全局 unpack
row-length/skip/alignment 状态不会在上传后恢复；Oculus
`TextureUploadHelper` 会在自定义纹理上传前精确重置这四项。红外上传现在遵循同一
contract，并只恢复原 3D texture binding。

验证结果：Java 17 production/test/GameTest source 编译通过；thermal + infrared
codec JUnit `103/103`；Forge GameTest `14/14`。post-fix live toggle 和目标负载
JFR 因窗口自动化被用户中止而尚未执行，完成后再将状态改为 `completed`。

## Block-Resolved Actual Air-Cell Correction

- Time: `2026-09-01 16:46:23 +08:00`
- Status: `ready; implementation pending`
- Precedence: this section supersedes every Brick-representative temperature,
  changed-Page-only payload, presence-mismatch full rebuild, fixed 65-short Page
  record, `36^3` texture, and matching cost/validation rule above.

### Confirmed Defect And Correctness Contract

The current mixed-Brick encoder chooses one representative Air slot and paints
the entire `4 x 4 x 4` Brick with that value. When a wall inside one Brick
separates two Air components, the solver and point query own two temperatures
but infrared discards one. This is a correctness defect, not a filtering issue.

The corrected first implementation has this exact semantic:

> One client texel represents one world block position. The server resolves the
> center of that position through the immutable `PagePublication` topology and
> writes the temperature of the resulting actual `QueryPublication` Air slot.
> A full-block wall inside one Brick must therefore display different Air-cell
> temperatures on its two sides.

This is block-position exact, not arbitrary sub-block component exact. A stair,
door, fence, or other shape may contain multiple Air components inside one
block; the block-center sample represents only the component containing that
center. Arbitrary quarter-microcell accuracy would require a client component
topology/atlas or a much denser texture and is explicitly outside this minimal
implementation. The implementation and docs must not call the result
component-exact without that qualification.

Brick remains only the capture/residency/topology and wire-record address. It no
longer defines rendering resolution. This correction does not change solver
cells, source binding, sparse residency, cross-section propagation, or the
20-tick transport step. `airMixingWPerBlockK` calibration remains owned by the
thermal runtime plan and must be benchmarked separately.

### Existing Dirty Events, Minimal Retained History

Normal delta must send changed Bricks only. Existing dirty information is the
event source and must be reused:

- `replacementMask`, `cellReplacementMask`, and exact changed Brick indexes
  stamp topology/publication changes;
- the existing infrared-active `QueryPublication.publish` live-slot comparison
  stamps quantized temperature changes using the already computed
  `(pageSlot, brickIndex)`;
- admission/new lifecycle stamps every Brick whose old client contents must be
  overwritten;
- retirement remains Page presence state.

Those masks are cut-local and cannot answer an arbitrarily delayed stateless
client after the commit scratch is cleared. Do not duplicate temperatures or
add a dirty-history ring. Retain only the last worker batch epoch for each Page
and Brick:

```text
int[maximumPages]      pageChangeEpochs
int[maximumPages * 64] brickChangeEpochs
```

The worker batch sequence is the epoch. Every topology and temperature change
from one processed batch stamps the same epoch; it does not increment once per
Page. A request carries the last accepted epoch. Page epoch is the O(1) first
level, and only a changed Page scans its 64 Brick epochs:

```text
pageChangeEpoch <= clientEpoch       -> skip Page
brickChangeEpoch > clientEpoch       -> encode current Brick
```

The epoch arrays are history indexes, not a second dirty authority. They add no
cell traversal and support clients delayed by any number of publications. A new
engine generation, infrared reactivation, or the practically unreachable
nonnegative-int epoch boundary clears the arrays and forces one full response.
At one normal batch per second the int domain lasts about 68 years.

Raw fixed memory at `maximumPages = 3,200`:

```text
Page epochs:    3,200 * 4      = 12,800 bytes
Brick epochs:   3,200 * 64 * 4 = 819,200 bytes
combined:                         832,000 bytes ~= 0.794 MiB/dimension
```

Charge both arrays to the existing dimension/server memory budget. Do not add
per-player masks, observers, revision histories, packet caches, or a second
temperature buffer.

### Presence Delta And Full Boundaries

Keep the existing client-carried twelve presence words. With an unchanged
center, presence mismatch is a Page lifecycle delta rather than a reason to
clear all `9^3` Pages:

```text
added   = currentPresence & ~knownPresence
removed = knownPresence & ~currentPresence
```

- Clear each removed Page's `16^3` client texture region.
- Encode every currently valid Brick for each added Page.
- For Pages present on both sides, encode only Bricks newer than the client
  epoch.
- Replace the client's presence words only after the response is accepted.

A full-view rebuild remains necessary only for first open, center chunk/eye
section change, dimension/engine generation change, infrared tracking
reactivation, epoch reset, or explicit client invalidation. A temporarily
invalid/over-age publication still produces no response and preserves the
client's last coherent mirror.

Same-section lifecycle replacement may leave the Page presence bit set. It must
stamp all 64 Brick epochs so current INVALID/valid records overwrite every old
Brick. Page-slot reuse is server-internal and never enters local client indexes.

### Changed-Brick Wire Format

Reuse the two registered packet classes. C2S keeps request ID, force-full,
client epoch, and twelve presence words. S2C carries request/center/current
epoch, full/presence flags, current presence when needed, and one flat encoded
Brick payload.

Pack Page and Brick identity into one unsigned short:

```text
localBrickIndex = localPageIndex * 64 + brickIndex
range = 0..46,655
```

After resolving the 64 world-block centers of a Brick, quantize each actual Air
slot temperature to the existing signed quarter-degree short and use one of
four packet-local modes:

| Mode | Payload | Purpose |
|---|---|---|
| `INVALID` | none | delta clears a Brick with no displayable Air |
| `UNIFORM` | one short | all 64 positions have the same encoded value |
| `INDEXED` | unique shorts plus 64 packed indexes | repeated actual Air-cell values |
| `RAW` | 64 shorts | dictionary encoding is not smaller |

`INDEXED` builds a dictionary from the final encoded shorts, including invalid
when present. Use the existing Minecraft bit-storage convention for the 64
indexes. Select it only when its complete encoded byte count is smaller than
RAW, so compression can never expand a record. It is one-response scratch, not
a retained component dictionary or cache.

For a full response, clear the client mirror first and omit all-INVALID Brick
records. For a delta, encode an explicit INVALID record when a previously
displayable Brick becomes invalid. One added Page may likewise omit invalid
Bricks because the client clears that Page before applying its records.

The server freezes the immutable `PagePublication` references used by one
response, writes records into one dimension-owned geometrically grown primitive
byte scratch, validates the `InfraredReadCursor` once, and then creates one
exact owned packet `byte[]`. It creates no Page/Brick record objects or
per-record arrays. Query/publication validation failure discards the response
before packet allocation where possible and retries on the next normal poll.

### Structural Packet Bound

There are at most `729 * 64 = 46,656` local Brick addresses and the dimension
has at most `65,536` live cells. Invalid block positions may add one dictionary
value without consuming a live cell, so the bound must combine both limits
rather than assume one uniform component count.

```text
46,656 one-cell mixed Bricks with invalid positions:
  record = 2 local ID + 1 mode + 1 dictionary count
         + 8 packed-index bytes + 4 dictionary bytes
         = 16 bytes
  base = 46,656 * 16 = 746,496 bytes

remaining live-cell budget = 65,536 - 46,656 = 18,880
the largest per-extra-cell record increase is 10 bytes
  extra = 18,880 * 10 = 188,800 bytes

maximum encoded Brick payload = 935,296 bytes ~= 0.892 MiB
```

Other dictionary sizes or RAW mode do not exceed that mixed allocation. Header,
epoch, flags, and optional presence words must be included by the codec test;
the completed packet must remain below the existing 1 MiB transport boundary.
Add no packet fragmentation, continuation, compression library, or frame
assembly. If the actual codec cannot prove the complete bound with margin, this
wire format is rejected rather than relying on the comparison estimate.

### Server Encoding Cost

Page enumeration remains D1 `O(81 + actual entries)`. Page/Brick epoch checks
occur only for a valid active infrared snapshot:

```text
unchanged Page: O(1)
changed Page:   64 int comparisons
regular Brick: one QueryPublication slot read
mixed Brick:   64 existing resolveAirPoint mappings
               plus one read per unique referenced Air slot
```

Mixed encoding uses fixed caller-owned arrays for 64 resolved slots, encoded
values, dictionary values, and indexes. It reads no BlockState, section, chunk,
heightmap, source, or solver state and never creates residency. Temperature
change detection remains fused into the existing live-slot publication pass.

Expected retained server overhead is approximately:

```text
Page/Brick epoch arrays       0.794 MiB/dimension
largest retained encode scratch <= complete bounded payload (~0.90 MiB)
small Page-publication/sampling scratch
```

The exact packet byte array is transient ownership passed to networking. No
per-player retained server payload exists.

### Block-Resolution Client Texture

Replace the `36^3` Brick texture with one `144^3` block texture. Keep one
persistent direct `ShortBuffer` as the CPU mirror and remove the parallel heap
`short[]`. Raw fixed storage is:

```text
144^3 = 2,985,984 texels
R16I GPU texture          = 5,971,968 bytes ~= 5.70 MiB
direct ShortBuffer mirror = 5,971,968 bytes ~= 5.70 MiB
Page upload scratch       = 4,096 shorts = 8 KiB
combined                  ~= 11.40 MiB/client
```

Decode Brick records directly into the mirror. Track affected local Pages with
one fixed twelve-long upload mask. Delta uploads copy each affected Page from
the mirror into the 8 KiB scratch and perform one `16^3` `glTexSubImage3D`;
they never upload the complete texture. A full response performs one complete
texture upload.

The shader retains one integer `texelFetch` per valid pixel:

```glsl
ivec3 temperatureTexel = ivec3(floor(
    temperatureCameraOffset + relativeWorldPos.xyz * surfaceScale));
```

Compute `temperatureCameraOffset = cameraPosition - textureOrigin` in CPU double
precision before converting that small relative value to a float uniform. Keep
the existing camera-relative surface nudge, depth-source ownership,
Oculus/Embeddium unpack-state reset, scan sphere, and original-texture color
composition unless a separate rendering defect proves a change. Do not add
linear filtering, multiple temperature fetches, mipmaps, toroidal texture
movement, or a client topology compiler.

On presence removal/addition, clear only the affected `16^3` Page region before
applying new records. On center change/full, clear the complete direct mirror so
old local indexes cannot leak into the new origin.

### Implementation Order

1. Replace dimension/Page long temperature change IDs with batch-epoch Page and
   Brick int arrays in `QueryPublication`; include them in memory reservation
   and expose coherent cursor comparisons.
2. Feed exact topology changed-Brick masks into the same epoch path and stamp
   quantized Air temperature changes inside the existing live-slot loop.
3. Replace full-on-presence-mismatch with added/removed Page delta calculation;
   retain full only at the named center/generation/reactivation boundaries.
4. Replace the fixed Page `short[]` snapshot with one flat changed-Brick byte
   payload and INVALID/UNIFORM/INDEXED/RAW encoder using reusable scratch.
5. Modify the existing request/response packet codec in place; add no packet
   registration, packet family, observer, subscription, or framing protocol.
6. Replace the client Brick mirror/texture with one block-resolution direct
   mirror, one `R16I 144^3` texture, one Page upload scratch, and Page-local
   subimage uploads.
7. Update the shader coordinate scale from one texel per four blocks to one per
   block while preserving one fetch and existing depth compatibility.
8. Delete the old representative mixed-slot voting scratch, fixed 65-short Page
   record, Page-wide delta encoder, complete-delta texture upload, and superseded
   tests/docs in the same implementation.

### Required Validation

Automated correctness:

- A full-block wall inside one Brick has two solver Air cells and produces
  different left/right block texels from one response.
- A regular one-cell Brick uses UNIFORM; all-invalid uses INVALID; a two-value
  wall uses INDEXED; incompressible values use RAW; every codec round-trip is
  byte-deterministic.
- INDEXED is selected only when smaller than RAW, and the exact maximum packet
  including headers remains below the transport cap.
- Temperature-only changes stamp only their Brick epoch; topology masks stamp
  only affected Bricks except lifecycle/full invalidation.
- Arbitrarily old client epochs still receive every changed Brick without a
  history ring or per-player state.
- Added/removed Page presence clears or initializes only that Page; unchanged
  Pages retain their texture; same-section lifecycle replacement overwrites all
  64 Bricks.
- Quantized unchanged temperature and unchanged presence produce zero S2C.
  Changed data outside the view also produces zero S2C; the client keeps its
  older epoch, which remains sufficient to select any later visible changes.
- Negative coordinates, all Page/Brick/block edges, center changes, quick
  movement, dimension/engine restart, stale publication, and request-ID races
  preserve coherent mapping.

Rendering and compatibility:

- Full and Page-delta uploads render correctly on Vanilla and Oculus/Embeddium;
  PBO and pixel-store state remain valid.
- Wall sides remain different while moving or crouching; no old-origin texels
  survive a full response.
- Shader remains one temperature texture fetch and does not introduce source,
  Page-record, or component loops.
- The documented sub-block limitation is tested with a partial-shape fixture so
  it cannot be mistaken for component-exact behavior.

Performance gates, measured without production counters:

- 100 non-overlapping infrared clients retain 40-tick entity-ID staggering and
  static zero S2C.
- Record main-thread snapshot/encode P99, actual Brick mode counts, bytes per
  response, aggregate S2C, retained epoch/scratch heap, and packet allocation.
- Measure client full response decode/upload and changed-Page delta frame-time
  impact on the target low-end renderer path.
- Run the same workload with the separately measured
  `airMixingWPerBlockK = 384` candidate because faster propagation can increase
  resident and quantized-changing Bricks. Do not accept either tuning or this
  protocol from the current `96` workload alone.

### Documentation Impact And Outcome

Implementation must update the climate living docs from Brick-representative to
block-position actual Air-cell temperature, record Page/Brick batch epochs,
presence delta, packet modes, client texture ownership, exact costs, and the
sub-block limitation. Append a new diary entry with final tests and measured
results; do not rewrite the prior Brick-protocol history.

Current outcome: the production implementation now uses Page/Brick epochs,
presence deltas, changed-Brick modes, block-center Air-cell sampling, a single
direct `144^3 R16I` client mirror, and Page-local delta uploads. Production,
JUnit, and GameTest sources compile, focused thermal lifecycle suites pass
`32/32`, and the final Forge run passes `16/16` required GameTests. The old
private-state reflection test was deleted because it did not execute rendering;
the exact structural payload remains `935,296` bytes and the encoded response
including headers remains below 1 MiB. Live Vanilla/Oculus rendering
and target-load JFR remain the final external gates, so the plan stays
`in-progress` rather than claiming those measurements.

The implementation review correction keeps center/full polling forced until a
matching response installs the new origin, computes the shader camera offset in
CPU double precision before float conversion, and writes known INVALID/UNIFORM
records directly. Omitting an invalid record is a successful full/added-Page
operation rather than a response failure. These corrections add no protocol
field, cache, observer, texture, or render pass.

The response-center correction rejects a delta unless its server-selected
center matches the installed texture center. A full response may install a new
center, and every accepted response updates the existing requested-center
baseline so the next client tick detects any later movement. This uses only the
existing center and availability fields; no center was added to C2S.

The retained-memory correction keeps the existing fixed budget and wire
protocol but allocates dimension Page/Brick epoch arrays only on the first
infrared request. The client class no longer allocates or clears its 5.70 MiB
direct mirror during ordinary client startup; the mirror and an all-INVALID GPU
texture are created on the first actual infrared render, while the 8 KiB upload
scratch is created on the first Page delta, then all are reused. Initialization
therefore does not wait for a server response. A later section
move clears `deltaBaselineValid` for protocol purposes but continues
rendering the old texture/origin until full replacement, avoiding a one-frame
pass disappearance. Reset detaches static GPU handles before scheduling deletion
of captured old resources, so delayed cleanup cannot delete a newer texture.
User-owned live and load validation remains pending.

The initialization retry correction separates an unresolved full request from
the stable 40-tick poll. A missing full response now retries after an
entity-ID-spread `41..59` ticks while suppressing the stable poll; the retry
delays deliberately exclude multiples of the 20-tick thermal cut. This breaks
the previously permanent Page/Query publication cadence collision without a
server observer, response cache, immediate re-encode, extra packet field, or
normal steady-state request.

The dormant-neighbor correction adds no wire field or stable-poll work. A full
response may append existing `UNIFORM` records containing source-supported
dormant Brick means. These temporary Brick-resolution records do not set Page
presence, participate in delta epochs, admit topology, or load chunks. Real Page
admission clears/replaces the region through the existing added-Page path with
block-position exact values.
The consumed disk-support flag is retained in a lazy transient section bitset
until live source discovery replaces it. Full encoding reads this O(1) hint,
not the not-yet-populated source index, so login ordering cannot omit the
neighbor and leave the client on a permanently incomplete baseline.
