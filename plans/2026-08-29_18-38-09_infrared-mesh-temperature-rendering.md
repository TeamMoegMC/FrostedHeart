# 红外视野实际温度场网络方案

- Time: `2026-08-29 18:38:09 +0800`
- Last reviewed: `2026-08-29 19:22:55 +0800`
- Authors: `TeamMoeg; Codex (GPT-5, architecture review)`
- Status: `ready`
- Scope: `InfraredViewRenderer`, `infrared_view.fsh`, infrared packets, `MinecraftThermalInput`, `MinecraftPageManager`, `PagePublication`, `QueryPublication`
- Related: [world-climate-and-temperature.md](../docs/climate/world-climate-and-temperature.md), [thermal-runtime-architecture-and-optimization.md](../docs/climate/thermal-runtime-architecture-and-optimization.md)

## Goal

红外视野只显示 thermal runtime 已求解并发布的空气温度。Page 是服务端内部查询、revision 和缓存边界，不是客户端数据模型。

- 不解析 physical source。
- 不发送 source ID、功率、形状或伪温度 blob。
- source 已经通过 solver 改变的实际空气温度会自然出现在 render tiles 中；只删除直接 source 可视化，不删除 source 热学作用。
- 不叠加 `ThermalAnalyticField`。
- 不把 material/phase 温度伪装成可见空气温度。
- wire/client 不接收 sectionKey、Page slot、lifecycle generation 或 topology generation，只接收固定渲染网格中的温度 tile 和清除命令。
- 没有有效 Page publication 时不绘制热学 overlay。
- 红外 observer 不 admission、不 retain Page，也不加载区块；只读取现有 player/source/continuation interest 已产生的 publication。
- 以 100 人服务器为验收规模，优先减少稳定带宽、重复编码和大包分配。
- 新协议不兼容旧 HeatArea wire format；双端模组必须同版本。

## Verified Current State

- 当前 `MinecraftThermalInput.gameplayInfraredFields` 完全不读取 Page 温度，只把下面两类对象编码为 8-float `HeatArea`：
  - `ThermalAnalyticFieldIndex.appendInfrared`；
  - `PhysicalSourceSpatialIndex.appendInfraredFields`。
- `PhysicalSourceSpatialIndex.appendInfraredFields` 发送的是按额定功率推导的伪热量，不是 solver 输出；隔墙时会产生干扰和双重表达。
- 当前 C2S 包发送 `chunkPos + chunkRadius`；S2C 包发送 `VarIntArray(floatToIntBits)`；客户端只在跨 chunk 时请求一次。
- `PagePublication` 只发布 64 个 Brick 的空气覆盖 slot、slot generation、签名和 mixed geometry，不含温度。
- `QueryPublication` 按 arena slot 双缓冲发布真实温度；`tryRead` 是现有 gameplay 查询入口。
- `QueryPublication.sampleTick` 是整个维度的一份时间戳。每次非休眠 publication 都会推进它，不能据此判断具体哪个 Page 的温度发生变化。
- `QueryPublication.publish` 已经遍历所有 live arena slots；在该遍历内比较量化温度不需要第二次 arena/Page 扫描。
- 默认红外半径为 4 chunks。若垂直带为 +/-2 sections，可见区域最多有：

```text
(2 * 4 + 1)^2 * (2 * 2 + 1) = 405 Page positions
```

## Corrected Draft Errors

本次复查明确废弃前一版中的以下假设：

1. `sampleTick` 不是 Page revision；用它做 Page 增量会让所有 Page 每秒都被判定为变化。
2. mixed Brick 取 8 个八分程样本不等于“空气分量级完全保真”；小分量可能不经过样本点。它只能准确表示 2-block 采样网格上的真实 Page 温度。
3. “1 byte 同时保存 uniform 标志和 8-bit valid mask”需要 9 bits，wire layout 不成立。
4. 半径 4、垂直 +/-2、cell size 2 的纹理应为 `72 x 40 x 72`，不是 `288 x 40 x 288`。
5. 一个 Page 在该纹理中占 `8 x 8 x 8` texels，不是 `32 x 32 x 32`。
6. `glCopyTexSubImage3D` 不能完成计划中的同纹理 3D 平移；使用环形纹理坐标，不做 GPU 内复制。
7. memo 淘汰后不能再依赖 memo 生成 Page retirement 列表，否则客户端会保留幽灵 Page。
8. `8192 Page ~= 4 MiB` 的估算错误；slot mapping 本身就可能超过该数字。缓存必须按实际字节预算，而不是按虚构的固定项数。
9. 周期拉取即使返回 unchanged，也会让 100 个客户端持续产生 C2S/S2C 控制流量；网络优先时应改为订阅推送。

## Architecture

```text
client infrared toggle
        |
        v
C2S subscription on/off
        |
        v
InfraredObserverManager (level thread)
        |
        +--> current Page presence / Page revisions
        |
        +--> shared InfraredTilePayloadCache
                         |
                         v
              PagePublication + QueryPublication
                         |
                         v
              quantized Brick payload blobs
                         |
                         v
        S2C initial / movement / changed-Page frames
                         |
                         v
          client toroidal GL_R16I 3D texture
```

## Decisions

### D1 Actual Page Temperature Semantics

- 红外值来自 `PagePublication.resolveAirPoint` 解析出的 Air slot，再从 D2 的一致性 read cursor 读取对应 QueryPublication 温度；现有单点 gameplay 继续使用 `tryRead`。
- material pole、deep pole、phase reservoir、source 功率和 analytic field 不进入红外 payload。
- 采样网格固定为 2-block 分辨率，即每个 Page `8 x 8 x 8` texels。
- 均匀 Brick：
  - 读取一个有效 Air slot；
  - 将该真实温度复制到该 Brick 的 8 个 2-block texels。
- mixed Brick：
  - 每个 2-block texel 检查其 8 个方块中心（固定 block-center microcell 42）；
  - 选择出现次数最多的实际 Air slot，平票时取最小 slot，保证确定性；
  - 只读取并发送该 slot 的真实温度，不平均不同空气分量；
  - 8 个方块中心都无 Air 时保持 invalid。
- mixed mapping 最多执行 64 次 `resolveAirPoint`/Brick，但只在 geometry revision 变化或 cache miss 时计算；温度更新沿缓存 slot mapping 读取最多 8 个样本。
- 该表示准确的是“2-block 网格样本温度”，不是所有微小空气分量。任何实现和文档都不得再次声称 component-exact。
- 从未 admission、已 retirement、worker reset 或维度 publication 超龄的 Page 表示未知；shader 保留原始画面，不回退 source blob、analytic field 或自然温度。普通 geometry mutation 的短暂 stale 窗口保留上一份不超过 40 ticks 的客户端 texels，等待新 commit 直接覆盖，避免整 Page 清空后重发。

### D2 Page/Brick-Local Quantized Revisions

网络增量需要真实的 Page/Brick revision，不能使用维度级 `sampleTick`。

- `PagePublication` 增加 worker Page slot；生命周期仍由 `ThermalPageHandle.lifecycleGeneration` 校验。
- `QueryPublication` 增加：
  - 全局单调 `infraredRevision`；
  - 按 worker Page slot 保存的 `pageTemperatureRevision`、lifecycle generation 和 `64` 个 `brickTemperatureRevision`；
  - caller-owned、可复用的 `InfraredReadCursor`，一次固定 published buffer、Page/Brick revisions、sample tick 和 publication version。
- `QueryPublication` 只在该维度 `infraredObserverCount > 0` 时执行量化比较。第一个 observer 启用 tracking 并要求下一次正常 publish reseed；最后一个 observer 关闭后只保留一次 volatile flag 检查，不再执行逐 Air-cell 量化/revision 工作。
- 初始快照直接读取当前 published buffer，不依赖 revision tracking 已经运行；reseed 只保证初始快照之后的变化不会遗漏。
- revision backing 在 engine 创建时按 `ThermalDimensionLimits.maximumPages() * 64` 一次预分配并计入 query publication budget；topology prepare/commit 和 observer 请求都不得扩容。使用 `long` revision 时 3200 Page 上限约占 1.6MiB，属于固定可计算成本。
- 正常 `QueryPublication.publish` 已遍历 live slots。该循环内：
  - 只处理 Air cells；
  - 将新温度量化到 `0.05 C`；
  - 与当前 published buffer 中相同 slot/generation 的量化值比较；
  - 从 Air cell 的 Page slot 和 Brick minimum 直接得到 `brickIndex`；
  - 同一 Brick 只要一个量化值变化，就把该 Brick 和所属 Page 标为本次 infrared revision。
- dirty 收集使用预分配的 `touchedPageSlots[] + pendingBrickMask[pageSlot] + stamp[pageSlot]`；第一次触碰 Page 才加入列表，publish 结束只遍历 touched Pages 写 revisions。不得每次清空/扫描 `maximumPages * 64` revision backing。
- 不增加第二次 live-slot、无关 Page 或 high-water 遍历；收口成本严格为 O(changed pages)。
- `republishUnchanged` 不推进 infrared revision，保持睡眠路径 O(1)。
- admission、任何 PagePublication geometry/topology mapping change 和 lifecycle replacement 由 `ThermalDimensionEngine` 从现有 `PreparedTopologyChange.PageWrite` 传入精确 dirty Brick mask，强制推进对应 Brick/Page revisions；不能只看 topology generation，也不能把一个 Brick 变化扩成整 Page dirty。
- retirement 不依赖 revision；observer 的 presence bitmap 负责删除客户端 Page。
- revision 是候选变化信号。Brick packer 重新编码后若字节与旧 blob 完全一致，则保留旧 blob revision，不产生网络更新。
- 主线程读取顺序固定为：begin cursor，取得当前 `PagePublication`，读取匹配 `(pageSlot, lifecycleGeneration)` 的 Page/Brick revisions 和 slots，完成临时编码，最后同时验证 cursor publication version 与 handle publication identity。验证成功后才提交 cache entry；任一步失败就丢弃 staging 并留到下一次 dispatch，不循环自旋，也不拼接跨版本数据。
- 一个 observer dispatch 尽量共享一个 cursor，而不是每个 slot 单独进入 seqlock；cursor 由 manager 复用，不产生每 Page/请求对象。
- 生产代码不增加统计计数器、测试 getter 或诊断集合。

### D3 Server-Owned Observer Subscription

为了让静态场景达到零红外网络流量，采用订阅推送。

- 客户端打开/关闭红外时只发送一次 `C2SInfraredSubscriptionPacket`：
  - `clientSessionId`；
  - `enabled`。
- 服务器从 `ServerPlayer` 的实际位置计算中心 chunk 和 center section Y；客户端不发送当前位置、半径或垂直带。
- 半径和 yBand 由服务端配置/协议常量决定，避免每个请求携带重复字段。
- observer 枚举只访问 `MinecraftPageManager` 已存在的 handles/current publications；红外视野本身不调用 `retain`、不创建 primary lease、不扩大 continuation，也不触发 chunk load。
- `InfraredObserverManager` 在 level thread 保存每个开启玩家的最小状态：
  - client session ID；
  - 上次中心 chunk / section Y；
  - last delivered infrared revision；
  - last observed dimension infrared revision / Page presence revision；
  - 当前区域 presence bitmap；
  - 当前正在发送的 frame ID、target infrared revision 和无数组 continuation cursor。
- 不保存 per-player Page 温度、副本、hash 或完整已发送 payload。
- 更新时机：
  - 开启红外：发送当前区域初始快照；
  - 玩家跨 chunk/section：发送新的 presence bitmap、进入环带 Page 和重叠区域中尚未送达的变化 Page；
  - Page 量化温度/topology 变化：只向覆盖该 Page 的 observer 发送其中 changed Bricks；
  - Page retirement / worker reset：presence 变化时清除客户端对应位置；普通 mutation stale 不改变 presence；
  - 关闭、logout、dimension change、level close：删除 observer 状态；
  - worker generation restart / recipe reload：清空 observer watermark 和 payload cache，等待新 Page publications 后发送新的初始快照。
- observer 调度与 thermal completion 对齐到最多每 20 ticks 一次，不按 render frame 或玩家体温查询频率发送。
- dispatch 位于 main thread 应用 thermal completion、Page continuation/resync 和即时 retirement 之后；不从网络线程枚举 `MinecraftPageManager.pages`。
- Page 发送顺序使用一份按距离排序的共享静态 local-index 表，初始/移动快照先近后远；不为每个 observer 分配排序数组。
- `MinecraftPageManager` 维护一个 O(1) 的 Page presence revision，只在 admission、retirement 和 worker-generation reset 推进，不因普通 geometry mutation 的临时 stale 推进。observer 中心、维度 infrared revision 和 presence revision 均未变化时，跳过区域枚举。
- 红外沿用 gameplay 的 `MAX_PUBLICATION_AGE_TICKS = 40`。维度 sample 超龄时发送 presence 清除并停止温度 payload；新 publication 恢复后重新发送当前区域。age 可用维度级 sample tick O(1) 门控，不逐 Page 计算。
- 默认区域最多 405 Page positions。静态维度每个 observer 只做 O(1) 门控；存在温度/Page 生命周期变化时，100 人每次 dispatch 最多约 40,500 次 Page presence/revision 读取，这些是 primitive/volatile 查询，不分配 render payload。
- 稳定且不移动的 observer 在初始快照后不发送 C2S、S2C 或 unchanged 心跳。

### D4 Shared Render-Tile Payload Cache

- 每个维度使用一个按实际字节计费的 `InfraredTilePayloadCache`。key 仍利用服务端 Page/Brick 生命周期，但 value 和 wire 都是 render-tile 数据。
- key 为：

```text
sectionKey
+ lifecycleGeneration
+ geometryRevision
+ topologyGeneration
+ pageTemperatureRevision / per-Brick revision
```

- entry 只保存：
  - topology-stable sample slot mapping；
  - Page lifecycle generation（所有 Page cells 共用，不为每个 slot 重复保存）；
  - 64 个按 Brick revision 复用的 immutable Brick blobs；
  - 实际计费字节数和 LRU 链接。
- 缓存复用该 engine 已传给 QueryPublication 的 dimension budget，使用 `ThermalMemoryBudget.AllocationClass.OPTIONAL`，默认硬预算 `4 MiB / dimension`；不另建一套“红外维度预算”，也不使用“8192 项”等与真实 entry 大小无关的上限。
- 淘汰只影响下次重新编码，不影响 presence、revision 或客户端清理正确性。
- 同一个 Brick revision 在 cache entry 驻留期间只编码一次。entry 被预算淘汰后允许按当前 publication 重新编码；正确性不依赖命中率。
- 100 个玩家观看同一 Page 时，共享对应 changed Brick blobs；每个连接仍需发送一份，这是不可消除的网络下限。
- observer count 从 1 变 0 时关闭 tracking 并释放全部 tile payload cache entries；无人观看期间 revision 不推进，因此旧 blob 禁止跨该边界复用。
- observer count 从 0 变 1 时从当前 QueryPublication 做全量初始编码，并要求下一次正常 publish reseed Brick revisions。无 observer 时仅保留固定 revision backing，不保留可选 payload cache。

### D5 Wire Format

#### C2S Subscription

```text
VarInt clientSessionId
boolean enabled
```

没有周期请求包。

#### S2C Frame Header

```text
VarInt clientSessionId
VarLong frameId
VarLong targetInfraredRevision
optional ZigZag VarInt centerChunkX
optional ZigZag VarInt centerChunkZ
optional ZigZag VarInt centerSectionY
VarInt partSequence
byte flags
VarInt clearBlockCount
VarInt fullBlockCount
VarInt tileUpdateCount
```

- `clientSessionId` 使客户端在关闭、重开或维度切换后丢弃旧帧。
- `frameId` 在一个 client session 内单调递增，负责区分温度帧、presence-only 帧和被 supersede 的旧帧。
- `targetInfraredRevision` 是本组更新完成后可提交的 observer 温度 watermark；presence-only frame 允许它与上一次相同。
- `partSequence` 从 0 单调递增；flags 的 `MORE` 位表示后续仍有 Page。客户端只在收到不含 `MORE` 的 final frame 后提交 watermark。
- center 只在初始/区域移动 frame 出现；稳定区域的温度增量不重复发送中心坐标。
- flags 至少包含 `CLEAR_ALL`、`REGION_CHANGED` 和 `MORE`。初始、worker reset、publication 超龄时用 `CLEAR_ALL`；普通更新不重复发送 presence bitmap。
- 客户端和 wire 只认识两层固定渲染坐标：
  - render block：一个 `16 x 16 x 16` 世界区域，对应 `8 x 8 x 8` texels；默认区域最多 405 个；
  - render tile：一个 `4 x 4 x 4` 世界区域，对应 `2 x 2 x 2` texels；默认网格为 `36 x 20 x 36 = 25,920` tiles。
- render block index 固定为 `((dy + yBand) * width + (dz + radius)) * width + (dx + radius)`；tile index 使用相同的 Y/Z/X 顺序。wire 不发送 sectionKey、Page slot 或任何 lifecycle/topology 字段。

#### Commands

- `CLEAR_BLOCKS`：按升序写 render block index 的非负 VarInt delta。服务端内部 presence bitmap 只用于生成该列表，不上网。
- `FULL_BLOCKS`：每项写 render block index delta、64 个 tile 的 2-bit tags（固定 16 bytes），再按 tile index 写 payload。用于初始快照、新 admission 和进入环带；不为每个 tile 重复发送 1-byte tag。
- `TILE_UPDATES`：每项写 tile index delta，随后一个 tile 编码。用于稳定区域的 Brick 温度/geometry 增量。
- Page 只是服务端把 section/Brick 映射到 render block/tile index 的实现细节。客户端无法也无需还原 Page identity。

#### Tile Encoding

`TILE_UPDATES` 每个 tile 使用一个 byte tag；`FULL_BLOCKS` 使用同值的 packed 2-bit tag：

```text
0 EMPTY
1 UNIFORM
2 SAMPLED_8
```

- `UNIFORM`：随后一个 signed short 温度。
- `SAMPLED_8`：随后一个 8-bit valid mask，再按 bit 顺序写 signed short 温度。
- 温度编码为 `clamp(round(temperatureC * 20), -32767, 32767)`，精度 `0.05 C`；极端值饱和而不是溢出或中止整帧。
- `Short.MIN_VALUE` 保留为客户端纹理 invalid sentinel，不作为有效温度发送。
- topology 变化为 EMPTY 时发送 `TILE_UPDATE(EMPTY)` 清除旧 texels；retirement 使用一个 `CLEAR_BLOCK`，worker reset/publication 超龄使用 `CLEAR_ALL`。
- 不发送 float、source ID、功率、mode、radius、pillar、analytic field 或 material 状态。

#### Framing

- 单个 S2C frame payload 硬上限 `32 KiB`。
- 一个 observer 每次 20-tick dispatch 最多发送一个 frame；每个维度另有按实际连接字节计费的总发送预算，默认 `512 KiB / dispatch`，observer 使用 round-robin 公平续传。
- 直接根据共享 tile blobs 计算 command 长度并分配到 frame；不先构造一个大区域数组再切片。frame 只能在 command 边界切分。
- 常见 uniform tile 增量约为 `1-3 byte index delta + 1 byte tag + 2 byte temperature = 4-6 bytes`；mixed/full-valid tile 约为 `19-21 bytes`。
- 全 uniform render block 约为 `1-2 byte block delta + 16 byte tags + 64 * 2 = 145-146 bytes`；全 mixed/full-valid 最坏约为 `1-2 + 16 + 64 * 17 = 1105-1106 bytes`。默认 405 block 区域的协议级最坏值约 438KiB，因此必须允许跨 dispatch continuation。
- 初始/移动快照的所有 parts 入队后，服务器才推进 observer 的 last delivered revision；预算耗尽时保留 cursor，不能跳过未发送 Page。玩家移动或 session 变化会废弃旧 cursor 并从新区域重新开始。
- continuation 固定一个 frame ID 和 target infrared revision，只选择 `(lastDelivered, target]` 内的 Brick revisions。发送过程中出现的更新留给下一 frame；若读取到更新后的 blob，可发送最新值但仍只提交旧 target，下一 frame允许保守重发，不能漏发。
- region center、session 或 presence revision 在 continuation 中途变化时，废弃旧 cursor 并以新的 presence/frame 开始；温度 revision 单独变化不打断当前 continuation。
- 客户端只按序应用当前 session/frame 的 parts；缺号、较旧 session/frame 或被新区域 supersede 的旧 continuation 直接丢弃。
- Forge 连接自身可能压缩数据，但协议不依赖连接压缩，也不引入自定义压缩库。

### D6 Client Texture And Rendering

- 默认区域的纹理尺寸：

```text
X = (2 * radius + 1) * 8 = 72
Y = (2 * yBand + 1) * 8 = 40
Z = (2 * radius + 1) * 8 = 72
```

- 使用单张 `GL_R16I` 3D integer texture：
  - `72 x 40 x 72 x 2 bytes ~= 0.40 MiB`；
  - 保存与 wire 完全一致的 int16 量化值；
  - `Short.MIN_VALUE` 表示 invalid；
  - `GL_NEAREST` 采样，避免 sentinel 插值、隔墙温度混合和第二张 validity texture。
- 客户端另保留一份同尺寸 `short[]` CPU mirror（约 0.40MiB），用于应用 tile delta、ring 清除和合并上传；总固定温度场存储约 0.80MiB。
- 一个 tile 更新 `2 x 2 x 2` texels；一个 full/clear block 更新 `8 x 8 x 8` texels。
- 纹理采用 toroidal/ring 坐标：
  - 玩家移动时只改变逻辑世界原点；
  - 清除进入环带；
  - 写入新的 Page blocks；
  - 不调用 `glCopyTexSubImage3D` 或依赖 OpenGL 4.3 的 texture copy。
- render block 跨 ring 边界时最多拆成 8 个 `glTexSubImage3D` box。
- 解包期间只修改 CPU mirror 并标记受影响 render blocks；一帧结束后按 render block 合并上传，避免每个 tile 各发一次 GL call。
- shader 从重建的世界坐标映射到 ring texel：
  - valid 温度映射为配置色标；
  - invalid 保持原始场景颜色；
  - 不读取 HeatArea UBO，不循环 source/analytic 列表。
- 2-block nearest 网格是明确的视觉分辨率选择；若未来需要平滑，只能在不跨 invalid/wall 的前提下另行设计，不能伪称当前方案 component-exact。

### D7 Legacy Infrared Deletion

实施时删除红外专用旧路径：

- `MinecraftThermalInput.gameplayInfraredFields`；
- `ThermalAnalyticFieldIndex.appendInfrared`（确认无其他调用后）；
- `PhysicalSourceSpatialIndex.appendInfraredFields`（确认无其他调用后）；
- `FHRequestInfraredViewDataSyncPacket`；
- `FHResponseInfraredViewDataSyncPacket`；
- `InfraredViewRenderer` 的 HeatArea UBO、`adjustNum` 和旧 updateData；
- `infrared_view.fsh` 的 `HeatArea`、`Adjusts` 和 source/analytic 循环；
- `FHNetwork` 中旧 packet registrations。

保留：

- physical source 对 Page solver 和 radiation 的正常输入；
- analytic field 对 gameplay 温度的正常合成；
- 其他非红外消费者使用的 source/analytic APIs。

## Rejected Alternatives

| Alternative | Reason |
|---|---|
| 周期 C2S 拉取 + unchanged 响应 | 100 人静态场景仍持续产生双向控制流量；订阅推送可做到初始快照后 0 bytes |
| Page 级温度增量 | 一个 Brick 变化就重发整个 Page；Brick revision 可在现有 publish pass 内精确获得 |
| 在 wire/client 暴露 Page identity/revisions | 渲染只需要固定网格坐标和值；Page 只留作服务端内部索引和压缩边界 |
| per-player Page/Brick 已发副本 | 内存与 observer 数相乘；全局 revisions + 小 watermark 足以判断增量 |
| source/analytic/HeatArea 红外通道 | 不是 Page 求解温度，会产生伪热区、隔墙干扰和额外 wire 分支 |
| 每个 worker cut 预生成完整红外网格 | 无 observer 时仍产生常驻编码成本；只维护廉价量化 revision，payload 按需编码 |
| float 温度或逐方块 1-block 网格 | 带宽分别约为 int16/2-block 方案的 2 倍和 8 倍，且超出当前视觉需求 |
| 自定义压缩库 | 小增量和 Forge 连接压缩已覆盖主要收益；增加依赖、缓冲和延迟不值得 |
| 依赖 memo 推断 Page retirement | cache eviction 会造成幽灵 Page；presence bitmap 才是独立正确性来源 |

## Complexity And Network Contract

### Concrete Dual-Side Budget

服务端（按当前 `maximumPages = 3200`、`maximumLiveCells = 65,536`）：

- Page x 64 的 `long` Brick revisions 约 `3200 * 64 * 8 = 1,638,400 bytes ~= 1.56MiB / active dimension`，engine 创建时固定分配；Page aggregate/lifecycle/touched scratch 另为几十 KiB。
- 无 observer 时每次 QueryPublication 只检查一次 tracking flag，不执行逐-cell 红外量化；无 tile cache、observer 枚举或网络发送。
- 有 observer 且 worker 正常 publish 时，最多在已有 live-slot pass 内增加 65,536 次 Air-cell 量化/比较；没有第二次 cell traversal。
- level thread 静态场景每秒约 O(observer) 标量门控；dimension revision 变化时最坏为 `100 * 405 = 40,500` 次 Page revision/presence 读取。
- `InfraredTilePayloadCache` 最多 4MiB/维度，仅 observer 存在时保留；编码 scratch 复用，单个 outbound frame <=32KiB。

客户端（默认 radius 4、yBand 2）：

- `GL_R16I` texture 约 0.40MiB，CPU mirror 约 0.40MiB；固定温度场存储合计约 0.80MiB。
- packet/frame staging 上限 32KiB；不存在 source/analytic 数组或 512-entry HeatArea UBO。
- shader 每个有效世界像素执行一次 integer 3D texture fetch 和颜色映射，不再循环最多 512 个 HeatArea。
- 静态且不移动时没有周期网络、纹理上传或 payload 解码。

设：

- `O`：开启红外的玩家数；
- `P`：每个 observer 区域 Page positions，默认上限 405；
- `C`：本次真正变化且可见的 Brick 数；
- `S`：这些 tile/full-block/clear commands 的实际 encoded bytes。

正常成本：

| Path | Bound |
|---|---|
| 无 observer | 每次 publication 一次 tracking flag 分支；零逐-cell 红外量化、observer 枚举、tile 编码和网络流量 |
| 稳定 observer tick | revision/position 未变时 `O(O)` 标量门控，零区域枚举和 payload allocation |
| 维度有可见候选变化 | 最坏 `O(O * P)` primitive presence/revision reads |
| Brick 编码 | `O(C * sampled slots)`，驻留 cache 内同一 Brick revision 每维度一次 |
| S2C bytes | `O(S * receiving observers)`；每个客户端必须收到自己的数据 |
| 静态网络 | 初始快照后 0 bytes |
| 移动 1 chunk | presence + 进入环带 Page + 重叠区未送达变化 |
| retirement | presence bitmap 变化，不发送伪温度 |
| 瞬时发送 | 每 observer 每 dispatch <= 32KiB；每维度默认 <= 512KiB/dispatch |

协议级估算（不含 Forge channel/连接头）：

- 200 个全 uniform render blocks 的初始区域约 `200 * 146 = 29,200 bytes ~= 28.5KiB / client`。
- 一个 uniform tile 增量约 4-6 bytes；100 个同区域客户端的必要 payload 约 0.4-0.6KiB，加各连接 frame header。编码只做一次，但发送副本无法消除。
- 一个 mixed/full-valid tile 增量约 19-21 bytes。
- 100 人同时首次开启的总量由 512KiB/dispatch 的维度预算摊平，不形成单 tick 全量突发。

不允许：

- 每 20/40 ticks 向所有 observer 发送全量 Page；
- 为红外范围 admission/retain Page 或加载 chunk；
- 客户端周期轮询；
- 按 source 数量发送条目；
- 为每个玩家重复解析/编码相同 Brick；
- per-player Page 温度副本；
- 大于 32KiB 的单个红外 frame；
- 生产计数器或测试专用 getter。

## Implementation Steps

1. 在 `PagePublication` / `QueryPublication` 增加 Page slot/lifecycle 校验和量化后的 Page/Brick revisions，并在 `MinecraftPageManager` 增加 O(1) presence revision；复用现有 publish live-slot pass。
2. 增加字节预算受限的 `InfraredTilePayloadCache` 和无分配 Brick sampler/render-command writer。
3. 增加 `InfraredObserverManager`，实现订阅、服务器位置派生、presence、移动环带和 revision dispatch。
4. 定义新的 subscription/update packets、clear/full-block/tile codec 和 32KiB command-boundary framing；删除旧红外 packet 注册。
5. 将 `InfraredViewRenderer` 改为 `GL_R16I` toroidal 3D texture；重写 shader 为单一 Page 温度采样。
6. 删除 D7 列出的红外 source/analytic/HeatArea dead path。
7. 更新 living docs，完成网络/JFR/渲染验证后记录 diary。

## Validation

### Automated

- 量化变化小于 `0.05 C` 不推进 payload revision；跨量化边界只标记所属 Brick 和 Page aggregate revision。
- `republishUnchanged` 不推进 infrared revision。
- topology/lifecycle replacement 即使温度相同也刷新对应 Brick payload。
- geometry revision 改变但 topology generation 不变时也重建采样 mapping；publication 超过 40 ticks 时清除 overlay，恢复后重发。
- Page retirement/worker reset/publication 超龄通过 presence 清理，且在 memo 已淘汰时仍然正确；普通 mutation stale 不触发清空重发。
- `CLEAR_BLOCK`、`FULL_BLOCK`、均匀/mixed/invalid `TILE_UPDATE` 的 tags、valid mask、int16 顺序和局部坐标 delta 可确定往返。
- 分片从不拆开 command，单 frame 不超过 32KiB；`partSequence/MORE` 可跨 dispatch 流式续传，旧 session/frame 不覆盖新纹理。
- per-observer 和 per-dimension 发送预算耗尽后从正确 cursor 续传，不提前推进 watermark；round-robin 不饿死后续 observer。
- 同一驻留 cache entry 的 immutable Brick blobs 可供多个 observer 复用；淘汰后重编码不改变 wire 结果。
- 测试只通过现有 production contracts 构建 fixture，不增加生产测试钩子或计数器。

### Multiplayer Network Scenarios

使用外部网络捕获和 JFR，不向生产代码加入统计：

1. 100 个 observer 同一静态基地：初始快照后红外 C2S/S2C 流量为零。
2. 100 个 observer 同一区域、一个 Brick 跨量化温度边界：tile 编码共享，向覆盖它的连接各发送一个 `TILE_UPDATE`。
3. 100 个 observer 分散基地：编码和发送只涉及各自可见的 changed Bricks。
4. 玩家移动 1 chunk：只发送 presence 和进入环带，不发送完整重叠区域。
5. Page retirement、worker reset 或 publication 超龄：客户端在下一 observer dispatch 收到对应 render-block/CLEAR_ALL 清除，无 source blob fallback。
6. 快速开关红外、维度切换和分片交错：旧 session/frame 不再可见。

### Rendering

- 密闭房间的墙内外读取对应 2-block Page samples。
- source 隔墙时不出现直接功率 blob。
- `/heat_adjust` 等 analytic field 不出现在红外中。
- invalid/no admission 区域保持原始场景颜色。
- ring wrap、负坐标、跨 chunk/section 和视野半径边缘映射正确。

## Documentation Impact

实施完成后：

- 更新 [world-climate-and-temperature.md](../docs/climate/world-climate-and-temperature.md)，把当前 source/analytic 红外描述改为 Page 温度订阅协议。
- 更新 [data-lifecycle-and-integration.md](../docs/climate/data-lifecycle-and-integration.md)，记录 observer 生命周期、Page revision 和网络边界。
- 若其他 living docs 提到 HeatArea/UBO/source blob，同步删除旧描述。
- 新增 diary 条目，记录协议字节数、100 人场景、JFR、渲染结果及剩余限制。

## Outcome

待实施。
