# 服务端低开销的真实白幕空间渲染计划

- Time: `2026-08-24 04:25:01 +08:00`
- Last revised: `2026-08-31 05:20:46 +08:00`
- Authors: `Codex; OpenAI GPT-5; architecture and implementation planning`、`Codex subagent; OpenAI gpt-5.6-sol ultra; V2 handoff performance review`
- Status: `in-progress`
- Scope: `com.teammoeg.frostedheart.content.climate.gamedata.climate`、`content.climate.network`、`content.climate.render`、天气相关 Mixin、客户端天气配置与资源
- Related: [`V2 电影级渲染计划`](2026-08-24_05-08-24_white-curtain-v2-cinematic-rendering.md)、[`docs/climate/weather-rendering.md`](../docs/climate/weather-rendering.md)、[`docs/climate/world-climate-and-temperature.md`](../docs/climate/world-climate-and-temperature.md)、[`docs/climate/data-lifecycle-and-integration.md`](../docs/climate/data-lifecycle-and-integration.md)、`WhiteCurtainInfo`、`WorldClimate`、`FHClimatePacket`、`PlayerTemperatureData.advanceWeatherCycle`、`LevelRendererMixin`、`FogModification`

## Goal

把当前“服务端按玩家把局部白幕翻译成 Vanilla rain/thunder 标量”的实现，迁移为“服务端同步稀疏、确定性的白幕描述，客户端连续重建空间天气场并渲染”的架构。

最终必须同时满足：

1. 玩家在白幕外能看见有方向、有宽度、会移动的远处前沿；进入后降雪、风声和能见度连续变化，室内外表现可区分。
2. 服务端不模拟雪花、雾、网格或逐区块移动状态，不因客户端画质增加工作量。
3. 稳定天气期间没有白幕视觉专用的逐 tick 网络包；白幕运动不产生持续网络流量。
4. 服务端温度、作物、伤害和预报仍以 `WorldClimate` 为权威，不让客户端视觉结果反向决定玩法。
5. 迁移期间保留现役渲染 fallback；只有新路径达到视觉、性能和兼容验收后，才移除逐 tick 天气强度同步和遗留渲染代码。
6. 客户端新增成本必须由固定 profile 上限控制，只随可见天气工作量变化，不随白幕覆盖总面积、服务端玩家数或显示器刷新率增长。

## Engineering Budgets

### Server And Network

| Metric | Required target |
|---|---|
| 稳定白幕视觉包 | `0 packets/player/second` |
| 白幕创建/清除/自然结束 | 每次状态变化向维度玩家发送 `1` 个完整 snapshot；不按白幕逐包发送 |
| 登录/换维度 | `1` 个完整 snapshot，空列表也显式替换客户端旧状态 |
| 周期校时 | 复用已有 `FHClimatePacket.sec + clockDayTime`；小时变化照常发送，昼夜时钟大跳在下一次一秒调度复用同包；不增加新 packet 类型或稳定频率 |
| 单个 descriptor 编码体积 | 目标 `< 1 KiB`；记录实际值，不为几十字节差异复制第二套序列化协议 |
| 常态服务端视觉计算 | `0`；客户端画质、粒子数和白幕切片数不得进入服务端代码 |
| Vanilla 兼容检查 | 最多 `1 sample/player/second`，只在离散天气类型变化时发包 |
| 世界空间维护 | 不扫描 loaded chunks，不创建逐区块移动实体，不复制白幕到 chunk capability |

### Client

| Metric | Required target |
|---|---|
| `ClientWeatherState.tick` CPU | 初始目标 P95 `<= 0.25 ms/client tick`；包括时钟、候选白幕和近场采样网格更新 |
| 天气 render-thread CPU | P95 目标 `<= min(0.5, 0.05 * 1000 / targetFps) ms`，绝对硬门 `<= 0.75 ms/frame`；不含 GPU 等待 |
| 天气 GPU | 同场景 median 不高于当前 `blizzardDensity=15`；P95 目标 `<= min(1.0, 0.08 * 1000 / targetFps) ms`，Embeddium/Oculus 分别验收 |
| Weather draw submissions | 远景墙、空间降雪和近景风吹雪合计不超过 `3` 个主要批次/frame |
| 稳定帧分配 | 预热后目标 `0 B/frame`；不按雪花、采样点、切片或 descriptor 创建 Java 对象 |
| 客户端增量工作集 | descriptor、双采样网格、地形缓存、顶点缓冲和新增天气纹理合计目标 `<= 8 MiB` |
| 描述符扫描 | 每 tick 对 snapshot 最多做一次轻量 AABB/相位预筛；每个采样点只遍历近场候选，不遍历全维度列表 |
| 地形查询 | 只查询已加载客户端区块；按固定预算渐进刷新，稳定相机不得每帧重扫所有降雪列 |
| 画质降级语义 | 只降低切片/雪花/地形采样密度，不删除前沿、白化、风向或声音层 |
| 空间连续性 | 跨 chunk 不发生一帧从晴朗跳到满白；保持约 `5s` 的进入/退出缓动 |

`targetFps` 取玩家当前有效 FPS cap，没有 cap 时取显示器刷新率。V1 GPU 目标因此在 60/144/240 Hz 分别约为 `1.0/0.56/0.33 ms`，render-thread CPU 目标约为 `0.5/0.35/0.21 ms`。玩家明确选择固定 Fast/Fancy 或 `COMPATIBILITY`；V1 不根据硬件自动改变 backend，也不因为检测到高端 GPU 而突破工作上限。

CPU、GPU 和 allocation 必须分开记录；总 FPS 只能作为玩家体验结果，不能代替归因。若 Phase 0 证明当前帧成本或 packet 基线与文档估算不同，以实测数据更新实现和 work caps 后再进入下一阶段，不根据绝对 FPS 猜测优化结果，也不得只提高预算来让实现过关。

### Initial Client Work Caps

这些是第一版实现上限，不是保证始终填满的目标。只有 Phase 0/4 的同机 profile 证明仍在预算内才允许上调。

| Work | Fast | Fancy | Hard rule |
|---|---:|---:|---|
| 近场世界对齐采样间距 | `8 blocks` | `4 blocks` | 固定 `9x9 / 17x17` 网格，各为 `81/289` cells |
| 可见降雪 column/quad | `<= 256` | `<= 1024` | Fancy 上限覆盖当前 `blizzardDensity=15` 的 `31 x 31` 数量级 |
| 远景墙 depth slices | `3` | `5` | 发布版本硬上限 `12`；V1.1 用纹理和前沿形状替代额外透明层 |
| 单白幕横向 segments | `<= 12` | `<= 20` | 与全局 slice 预算合成 `36/100 wall quads/frame` 硬上限 |
| 地面 impact/terrain query | `<= 12/tick` | `<= 32/tick` | 尊重 Vanilla particle status；近景飞雪不走粒子对象系统 |
| 相机天空暴露 | `1 canSeeSky/tick` | `1 canSeeSky/tick` | 复用 `MutableBlockPos`，以 `0.15/tick` 平滑；不是逐列 terrain/depth 模型 |

Fast/Fancy 只选择稳定的离散 profile，不在第一版根据瞬时 FPS 自动来回切换。动态自适应容易产生画质泵动和不可复现的 profile，等固定 profile 达标后再单独评估。

## Verified Current State

### What Already Exists

| Existing component | Reuse decision |
|---|---|
| `WhiteCurtainInfo` 的区域、方向、`ClimateEvent` 和传播相位 | 保留语义，抽出不可变 descriptor 和纯计算模型 |
| `ClimateEventTrack.CODEC` | 继续作为多态 `ClimateEvent` 的结构化 codec，不复制手写字段协议 |
| `WorldClockSource` / `FHClimatePacket.sec` | 作为客户端气候时钟锚点，不新增逐 tick clock sync |
| `WorldClimate.whitecurtains` 和 NBT `whiteCurtainInfos` | 保持存档字段兼容，内部改为 descriptor + runtime wrapper |
| `WorldClimate.getClimate(ChunkPos)` | 保持服务端玩法入口和当前 chunk 级结果 |
| `LevelRendererMixin` | 迁移期提供 fallback 与 Vanilla weather cancel 点，最终缩减职责 |
| `FogModification` | 改为读取连续 client weather sample，不另建第二个雾事件订阅器 |
| `FHParticleTypes.SNOW`、雪纹理和 `wind.ogg` | 继续用于地面效果和风声，近景风吹雪另建明确的客户端表现层 |
| `BlizzardRenderer` | 不接入；两套未使用 quad 实现不构成可维护基础 |
| `DimensionSpecialEffectsMixin` | 不在本计划注册；它当前按冷群系而非天气判断，不能表示白幕 |

### Current Cost And Coupling

```text
every server tick
  ServerLevelMixin_WeatherCycle
    -> for every player
       -> WorldClimate.getClimate(player chunk)
       -> PlayerTemperatureData rain/thunder += or -= 0.01
       -> up to two ClientboundGameEventPacket packets during transition

client
  one connection-local rain/thunder pair
    -> uniform Vanilla precipitation around camera
    -> no curtain geometry, direction, distance or per-column boundary
```

当前白幕列表只在小时边界清除 `whitecurtainCache`。`addWhiteCurtain` 和 `clearWhiteCurtain` 不立即失效已查询区块，客户端 `FHClimatePacket` 也只携带当前位置结果而不携带空间描述。这三点必须在新渲染接入前解决。

## Locked Decisions

1. **解析式空间场，不做服务端网格模拟。** 白幕任意时刻的位置和相位由 descriptor、坐标和逻辑时钟直接计算。
2. **一个共享事实模型、两类采样。** `sampleGameplay` 保持当前 chunk 离散语义；`sampleVisual` 使用连续 block 坐标和柔化边缘，但不能改变服务端玩法结果。
3. **完整 snapshot 优先于增量协议。** 当前白幕数量低、重叠又被拒绝；add/clear/end 时发送整个列表比引入 ID、增量排序和重放状态更简单。只有测量证明 snapshot 过大才引入增量。
4. **codec 优先。** descriptor 沿用 `area`、`move`、`climate` 字段和 `ClimateEventTrack.CODEC`，保证旧存档可读并避免 NBT 与网络出现两套字段定义。
5. **客户端只重建表现，不成为玩法权威。** 服务端不接收客户端白幕结果，也不接收视觉质量或粒子状态。
6. **不依赖逐 tick Vanilla 强度包。** 新 renderer 完成后，Vanilla 包只保留低频离散兼容；Frosted Heart 的 `5s` 视觉缓动在客户端实现。
7. **第一版使用 Minecraft/Forge 常规世界渲染批次。** 不引入自定义 framebuffer、全屏后处理链或 shader-only 实现，先保持 Embeddium/Oculus 和无 shader 环境可用。
8. **用分阶段 ownership 切换避免双重降雪。** 新空间降雪取得天气 pass 所有权后才取消对应 Vanilla 降水；wall-only 阶段不取消现役降雪。
9. **Frosted Heart 气候独立于 `doWeatherCycle` 推进。** 该 gamerule 只约束 Vanilla 兼容桥，不冻结 `WorldClimate` descriptor 驱动的白幕视觉；最终文档必须明确这一变化。
10. **客户端只维护一个天气状态和一个帧上下文。** `ClientWeatherState` 统一拥有 snapshot、时钟锚点、预计算 descriptor 和双近场网格；`ClientWeatherFrame` 是该网格的帧内只读视图，统一提供 camera sample、LOD 和 render ownership。雾、声音、墙和雪不得各建缓存或各自遍历 descriptor。
11. **采样计算按 client tick 计费，不按 FPS 计费。** 近场天气网格以 `20 Hz` 更新前后两份 primitive arrays，渲染帧只按 partial tick 插值；远景前沿位置每帧用一次解析式时间更新以保持移动平滑。
12. **预筛一次，局部采样。** 每 tick 先把全 snapshot 预筛为近场候选和可见墙候选；网格 cell 只采样近场候选，避免 `columns x allDescriptors`。常见数量先用 allocation-free 线性扫描，只有实测 routine snapshot `> 32` 才引入空间索引。
13. **透明 fill-rate 是首要 GPU 预算。** 通过切片硬上限、屏幕/视锥裁剪、距离 fade 和预乘合成控制 overdraw；视觉细节来自噪声纹理、UV 运动和空间插值，不靠无限叠加透明层。
14. **无渲染线程外任务。** snapshot 在客户端主线程原子替换，tick 和 render 在已有客户端线程执行；第一版不增加 worker、timer、锁、`volatile` 状态或异步 VBO 上传。

## Target Architecture

```text
SERVER AUTHORITY

WorldClimate clock + global ClimateEventTrack
                    |
                    +--> WhiteCurtainDescriptor list (persisted, sparse)
                                |
                                +--> WhiteCurtainFieldModel.sampleGameplay
                                |      temperature / ClimateType / forecast
                                |
                                +--> FHWhiteCurtainSnapshotPacket
                                      login | dimension | add | clear | prune
                                                     |
=====================================================|======================
                                                     v
CLIENT PRESENTATION

FHClimatePacket.sec + snapshot anchor ------+
FHClimatePacket global climate/wind --------+--> ClientWeatherState
WhiteCurtainDescriptor snapshot ------------+    clock + prepared descriptors
                                                     |
                                              once per client tick
                                                     v
                              previous/current primitive sample grids
                                                     |
camera + partial tick + graphics profile ------------+
                                                     v
                                          ClientWeatherFrame
                                sample interpolation + LOD + ownership
                                    /                  |             \
                    SpatialWeatherRenderer      FogModification    sound loop
                    wall + snow, <= 3 batches   camera sample      tick sample
```

### Common Data Contract

Introduce `WhiteCurtainDescriptor` under `gamedata.climate`:

```java
record WhiteCurtainDescriptor(
    Rect affectedArea,
    Direction moveDirection,
    ClimateEvent climate
) {}
```

Its `CODEC` must emit and accept the current keys:

```text
area    -> Rect.CODEC
move    -> Direction.CODEC
climate -> ClimateEventTrack.CODEC
```

`WhiteCurtainInfo` becomes a runtime wrapper around the descriptor and keeps forecast/daily caches. Existing `whiteCurtainInfos` saves must decode without migration or field renaming. Visual randomness is derived deterministically from immutable descriptor values; it does not require a persisted seed or server-generated particle list.

### Pure Field Model

Introduce `WhiteCurtainFieldModel` with no `Level`, capability, network or renderer dependencies:

```java
ClimateResult sampleGameplay(
    WhiteCurtainDescriptor curtain,
    long climateSeconds,
    ChunkPos chunk
);

VisualKernel prepareVisual(WhiteCurtainDescriptor curtain);

void sampleVisual(
    VisualKernel curtain,
    double climateSeconds,
    double blockX,
    double blockZ,
    WhiteCurtainVisualProfile profile,
    MutableVisualWeatherSample out
);
```

The gameplay method must reproduce current `WhiteCurtainInfo.getClimate(seconds, pos)` for all four directions. `VisualKernel` is a nested immutable type of `WhiteCurtainFieldModel`, created only when a descriptor snapshot changes; it contains primitive block-space bounds, direction basis, phase/timeline coefficients and deterministic visual seed. The visual method consumes this kernel, uses continuous along/cross coordinates, an explicit edge width and `smoothstep`; at each chunk center its phase must agree with gameplay sampling.

`MutableVisualWeatherSample` is caller-owned scratch state and contains at least:

```text
insideAffectedCorridor
signedDistanceToActiveFrontBlocks
snowIntensity        0..1
whiteoutIntensity    0..1
windIntensity        0..1
windDirection        horizontal vector
visibilityBlocks
```

The render hot path must not return a new record/object per sample, traverse codec/event object graphs or construct `ClimateResult`. `ClientWeatherState` stores `VisualKernel[]` plus merged fields in structure-of-arrays form such as `float[] snow`, `whiteout`, `windX`, `windZ` and `visibility`; callers provide scratch output only for infrequent direct samples and tests. Large-scale state is deterministic across clients. Individual near-camera flakes may remain client-local because their exact identity has no gameplay effect.

Field merging is explicit: global climate supplies the uniform base; local candidates take the maximum snow/whiteout/wind intensity and minimum visibility. Wind direction comes from the candidate with greatest wind intensity, with snapshot order as the deterministic tie-break. Overlap is not expected from normal creation but must produce bounded deterministic output rather than double the effect.

### Snapshot And Clock Contract

`FHWhiteCurtainSnapshotPacket` contains:

```text
dimension ResourceKey
climateSeconds anchor
list<WhiteCurtainDescriptor>
```

The packet always replaces the dimension snapshot atomically. It is sent on login, dimension change, respawn, successful add, clear and natural prune. `FHClimatePacket` continues to provide the hourly `sec` correction; both packet handlers update the one clock anchor inside `ClientWeatherState`. Do not add a timer or a separate `ClientClimateClock` service.

Both packets carry `(serverClimateSeconds, serverClockDayTime)`. The client advances only from `level.getDayTime()`: `doDaylightCycle=false` therefore freezes the wall. Normal small deltas advance analytically; sleep or command discontinuities are ignored locally until the existing climate packet re-anchors them, independent of packet ordering. Corrections below `5 logical seconds` replace the remaining error and consume at most `0.10 seconds/client tick`; larger errors re-anchor immediately. Render time adds partial-tick motion only after a normal one-tick daylight advance.

The packet handler must discard a snapshot for a dimension other than the currently loaded client level. Decode failure keeps the last valid snapshot and records a bounded diagnostic rather than replacing the client state with an empty list.

### Client Tick And Frame Contract

```text
packet/client level change
  -> ClientWeatherState.replaceSnapshot()
       -> prepare immutable VisualKernel array once
       -> bump snapshotGeneration

client tick, exactly once
  -> advance climate seconds from shared dayTime anchor
  -> compatibility: stop here and invalidate the old spatial grid
  -> spatial: prefilter snapshot -> near candidates + visible-wall candidates
  -> if camera crossed grid cell or phase changed:
       fill CURRENT primitive weather grid from near candidates
       keep PREVIOUS grid for interpolation
  -> refresh <= profile terrain/exposure sample budget
  -> update one looping weather sound

LevelRenderer.renderLevel HEAD, after Camera.setup, exactly once rendered world frame
  -> ClientWeatherFrame.begin(camera, partialTick, profile)
       -> interpolate camera and grid values
       -> decide CUSTOM / WALL_ONLY / FALLBACK ownership
       -> expose immutable-for-frame primitives
  -> wall + snow + fog consume the same frame
```

The grid is world-aligned so camera movement inside one cell does not force a rebuild. The state keeps fixed-capacity primitive arrays sized to the active profile and reuses them across dimension ticks; dimension unload clears logical contents without churning the buffers. Descriptor preparation may allocate only when a snapshot actually changes.

### Client Component Boundaries

| Component | Owns | Must not do |
|---|---|---|
| existing `ClientClimateData` | forecast plus synchronized global climate/wind/humidity | local descriptor storage, render caches or timers |
| `ClientWeatherState` | current-dimension snapshot, clock anchor, prepared `VisualKernel[]`, candidate indices, previous/current primitive grids, terrain/exposure refresh budget | OpenGL calls, per-frame object creation or a second forecast copy |
| `ClientWeatherFrame` | reusable frame-local primitives, interpolated camera sample, active profile and ownership enum | mutate snapshot/grid, traverse descriptors or query chunks |
| `SpatialWeatherRenderer` | wall and snow passes, persistent CPU/GPU buffers, render-state restoration | gameplay sampling, packet handling, Minecraft `Particle` objects for flying flakes |
| existing `FHClientEvents` | call state tick at `ClientTickEvent.START`, sample one `canSeeSky`, drive sound lifecycle | contain weather math, geometry or a second lifecycle state |
| existing `FogModification` | apply frame camera sample to Vanilla fog when CUSTOM ownership allows it | wall/grid sampling, its own clock or `Util.getMillis()` transition state |
| `LevelRendererMixin` | begin the render frame with the setup current Camera; Vanilla cancel/fallback bridge; consume tick ownership for bounded ground impacts | traverse descriptors, become a second renderer or own weather truth |

Keep these as ordinary client-owned instances scoped to the loaded level/session, not mutable public statics spread across render hooks. Existing `ClientClimateData` remains static only for compatibility with its current consumers; the new local weather state exposes one lifecycle-controlled access point.

### Client Cost Model

Define:

```text
N = descriptors in the dimension snapshot
V = descriptors passing the near/visible prefilter
G = fixed near-grid cell count selected by profile
T = terrain/exposure samples allowed this tick
Q = visible snow columns/flakes submitted this frame
W = visible wall slices x visible width segments
```

Required scaling:

```text
tick CPU       = O(N) prefilter + O(G x V) field math + O(T) loaded-terrain reads
render CPU     = O(Q + W) primitive interpolation and vertex submission
working memory = O(N + G + profile buffer caps)
GPU work       = bounded Q/W plus measured translucent screen coverage
```

None of these terms may contain total curtain-covered chunks, server player count or client FPS. `N` is scanned once per client tick, not once per grid cell; `V` is expected to be small after corridor/front bounds checks. If a future implementation cannot demonstrate these counters, it has not implemented this architecture even if the picture looks correct.

### Client Rendering Ownership

```text
Phase 3: wall-only
  Vanilla snow/fog/sound remain active
  SpatialWeatherRenderer adds distant front only

Phase 4: spatial weather enabled
  SpatialWeatherRenderer owns FH precipitation
  LevelRendererMixin cancels Vanilla precipitation only for owned FH weather
  FogModification and sound read the shared ClientWeatherFrame/State sample

Fallback mode
  custom renderer disabled or initialization unavailable
  current Vanilla/Mixin rendering continues
```

Render precipitation and fog share one current-camera frame ownership frozen after `Camera.setup`. `tickRain`, ground impacts and sound consume the state tick's own camera sample/ownership, so minimized, paused, first-frame and teleport paths never read a stale render frame. Custom precipitation ownership is derived from the unattenuated previous/current grid footprint, while indoor exposure only scales effects; shelter must never hand work back to the legacy `tickRain` path or hide a snow band that is visible just beyond the camera. No component independently samples descriptors or guesses the weather source. A missing/invalid render frame selects fallback without partially executing custom passes.

## Implementation Steps

### Phase 0: Baseline And Instrumentation

1. Add development-only counters around `ServerLevelMixin_WeatherCycle`, `PlayerTemperatureData.advanceWeatherCycle`, weather packet sends, `WorldClimate.getClimate` and white-curtain cache hits/recomputes.
2. Add client profiler sections for Vanilla `renderSnowAndRain`, injected `tickRain`, fog callbacks and the future `stateTick`, `gridFill`, `terrainRefresh`, `wallSubmit` and `snowSubmit` paths. Counters must include descriptor candidates, field evaluations, heightmap reads, submitted quads/batches and ground particles.
3. Record scenarios with `0/1/8/32` white curtains and `1/20/100` simulated or real players where available; distinguish server CPU, packet count/bytes, client tick CPU, render-thread CPU, GPU/total frame time, allocation and incremental working set.
4. Fix measurement scenarios: global clear, global snow, global blizzard, player outside curtain, approaching front, inside core, retreating edge and indoor camera.
5. Record current fast/fancy particle attempts, heightmap/biome/block queries, rendered columns and draw submissions. Capture one no-shader and one shipped Embeddium/Oculus profile on the same camera path. Do not change behavior in this phase.
6. Use JFR/allocation profiling for CPU and allocations, and an external GPU capture or non-blocking profiler supported by the test environment for translucent-pass cost. Do not ship synchronous GL timer reads that stall the render thread.

Exit condition: a diary-ready baseline with packet counts, CPU/GPU attribution, allocation, query counts and profiler section timings. If runtime multiplayer or a GPU capture is unavailable, keep packet/call/quad counters as hard evidence and mark the missing wall-clock/GPU result pending; Phase 4 cannot claim final client acceptance without the real client run.

### Phase 1: Descriptor And Pure Spatial Model

1. Add `WhiteCurtainDescriptor.CODEC` with the exact current NBT field names.
2. Refactor `WhiteCurtainInfo` to wrap a descriptor while retaining its forecast caches and public behavior.
3. Move direction delta, max delta, invalidation, affected-area and climate sampling math into `WhiteCurtainFieldModel`; `WhiteCurtainInfo` delegates.
4. Preserve chunk-level gameplay output byte-for-byte for representative saved descriptors and all four movement directions.
5. Add `prepareVisual` plus allocation-free continuous kernel sampling calibrated so chunk centers share the gameplay phase, with configurable client-only edge width and visibility profile. Kernel preparation may traverse event data; grid sampling may not.
6. Replace hourly-only cache validity with entries carrying the exact next relevant phase time and a `whiteCurtainGeneration`; add/clear increments the generation immediately.
7. Make pruning report whether the descriptor list changed so the caller can schedule one snapshot.

Exit condition: no client rendering changes; old save fixtures load; current server climate queries and forecasts pass regression tests; add/clear becomes immediately visible to server queries.

### Phase 2: Snapshot Sync And Client State

1. Register `FHWhiteCurtainSnapshotPacket` next to `FHClimatePacket` using descriptor codec data, not a second hand-maintained field list.
2. Add `WorldClimate.syncWhiteCurtains(ServerLevel)` and call it from every mutation path: command add/clear, login, dimension change, respawn and natural prune.
3. Add one `ClientWeatherState`, keyed by the current dimension, that owns the clock anchor, immutable descriptor snapshot, snapshot generation, prepared `VisualKernel[]` and fixed-capacity previous/current primitive grids. It continues to read global climate/wind from existing `ClientClimateData` rather than copying forecast state.
4. Anchor snapshot and `FHClimatePacket` with both `sec` and the authoritative `WorldClockSource` dayTime. Advance from client `level.getDayTime`, freeze with `doDaylightCycle=false`, replace repeated small corrections, and immediately re-anchor large discontinuities. Query time once per tick and read one scalar per render frame, never per sample.
5. During snapshot replacement, call `WhiteCurtainFieldModel.prepareVisual` once per descriptor. Normal ticks perform an allocation-free linear prefilter over `VisualKernel[]` into reusable candidate index arrays; they never query polymorphic event/codec structures.
6. Add `ClientWeatherFrame`, prepared from the existing `LevelRendererMixin.renderLevel` HEAD after `Camera.setup`, exactly once per rendered world frame. `ClientTickEvent.START` calls the state tick and publishes separate tick ownership. The frame references the shared grid, interpolates by partial tick, selects the fixed Fast/Fancy profile and publishes one render ownership result without a new `GameRenderer` Mixin.
7. Clear logical state on disconnect and level unload while reusing buffers where safe. A snapshot received before the target client level exists may occupy one bounded pending slot and is applied only when that exact dimension loads; mismatched late packets are discarded. A null level/camera skips work and publishes fallback; a paused integrated client freezes the last completed frame without advancing tick work. Neither path may expose a half-replaced snapshot.
8. Add a development debug renderer for affected corridor bounds, direction, active front, grid cells, candidate counts, sampled intensities and current work caps; it must be disabled by default and excluded from release measurements.
9. Measure packet encoding size and snapshot-prepare time for `0/1/8/32` curtains; confirm no packet is sent and no descriptor array is rebuilt while descriptors only advance through time.

Exit condition: two clients reconstruct the same large-scale front from one snapshot; login/add/clear/prune update immediately; steady motion generates zero snapshot traffic, zero per-frame allocation and no per-effect descriptor scan. Clock/grid unit tests and `0/1/8/32` counters satisfy their client budgets before rendering work proceeds.

### Phase 3: Distant White Curtain And Camera Whiteout

1. Make `ClientWeatherState.tick` merge `ClientClimateData.climate/wind` with the prefiltered local descriptors into previous/current world-aligned primitive grids. Fill at `20 Hz`; render consumers use bilinear spatial interpolation plus partial-tick temporal interpolation.
2. Implement the wall pass inside one `SpatialWeatherRenderer` at the world render stage nearest weather. Use frustum/screen-width culling, render-distance fade, cached cross-front geometry and one batched translucent submission.
3. Use `3` Fast / `5` Fancy noise-textured depth slices perpendicular to movement, with a hard release cap of `12`. Sort back-to-front, keep depth test on and depth writes off, and derive visual detail from deterministic front shape, endpoint fade and per-layer UV flow instead of additional transparent layers.
4. Segment only visible curtain width. Refresh loaded heightmaps through the per-tick work budget; never request or retain an unloaded chunk. Missing terrain uses the last valid sample briefly, then fades to horizon instead of synchronously filling holes.
5. Keep far-wall detail in reusable textures, deterministic multi-scale UV motion, vertex alpha and front-shape noise. Do not increase slice count to hide weak assets; GPU capture must show the pass stays inside the translucent budget.
6. Change `FogModification` to read the frame's already-interpolated camera `whiteoutIntensity`, preserve fluid fog ownership and interpolate to client-configured visibility. Exposure uses cached sky visibility/light as an input, not as the storm's source of truth, and must not call `Util.getMillis()` or resample weather independently.
7. Implement one tickable looping wind sound whose volume and pitch follow the tick camera sample and cached exposure. Keep existing random wind fallback when custom rendering is disabled; stop the loop on dimension unload, pause and fallback ownership loss.
8. Add the client flag `spatialWhiteCurtainRendering`; keep it disabled during development and set its shipped default to true only after Phase 3/4 validation. Map existing `snowDensity`/`blizzardDensity` only to fallback until their migration semantics are documented.

Exit condition: outside players can see and hear an approaching front; entering/retreating and indoor/outdoor transitions are continuous; current Vanilla snowfall remains underneath as the temporary precipitation implementation. Stable camera records zero allocation, no repeated heightmap sweep and one wall submission; no-shader and Oculus GPU captures meet the Phase 3 provisional budget.

### Phase 4: Spatial Snow And Render Ownership Transfer

1. Add the spatial snow pass to `SpatialWeatherRenderer` with one reusable column/flake buffer. Each rendered column bilinearly reads the shared grid at its world coordinate; no column or flake calls `WhiteCurtainFieldModel` or walks descriptor lists.
2. Preserve global snow by treating `ClientClimateData.globalClimate` as a uniform base field and local white curtains as a stronger spatial overlay; player-local merged `climate` must not fill the whole grid.
3. Render near-camera wind flakes as deterministic slots in the same reusable weather buffer, not Minecraft `Particle` objects. Apply wind direction to inclination and UV motion. Only bounded ground impacts reuse `FHParticleTypes.SNOW` and respect `ParticleStatus` plus the profile cap.
4. Implement the Initial Client Work Caps as explicit immutable Fast/Fancy profiles selected only when settings change. Both retain wall, fog, wind and precipitation; they differ only in spacing, slices, segment/flake caps and terrain refresh budget.
5. Add a single ownership result to `ClientWeatherFrame`. When custom FH weather owns precipitation, cancel Vanilla `renderSnowAndRain`; otherwise execute the current fallback unchanged. Wall-only and custom-snow modes are explicit enum states, not combinations of booleans.
6. Prevent duplicate fog, wind and ground particles by gating render paths with frame ownership and tick paths with the tick camera ownership from the same state/grid. Tick code never consumes a render frame.
7. Restore all `RenderSystem`, light layer, pose and buffer state in `finally`, including empty/cull/error paths. A custom-pass exception quarantines custom ownership for the rest of that level session and returns to fallback without retrying every frame.
8. Measure each budget separately: state tick CPU, render-thread CPU, GPU, allocations, working set, descriptor/field evaluations, heightmap reads, quads and batches. Compare the same camera recording against current Fast/Fancy and `blizzardDensity=15`.

Exit condition: a player can see a spatial snow gradient across the front; there is no double precipitation; global snow and Vanilla fallback remain correct. Fast and Fancy both pass CPU/GPU/allocation/memory caps on the reference matrix; missing any one measurement blocks ownership transfer and Phase 5 packet removal.

### Phase 5: Remove Per-Tick Visual Packets

1. Replace `PlayerTemperatureData` rain/thunder interpolation fields with a small transient compatibility state containing the last sent discrete `ClimateType`.
2. Piggyback compatibility sampling on the existing one-second climate/player update cadence; do not add a new per-tick player loop.
3. Send Vanilla `START_RAINING`/`STOP_RAINING` and target rain/thunder strengths only when the discrete local state changes. Custom rendering ignores these strengths and keeps its own continuous interpolation.
4. Keep `ServerLevelMixin_WeatherCycle` responsible for global Vanilla world state and command compatibility, but remove its per-player visual interpolation loop.
5. Correct the existing `climateSnowing = climate.isBlizzard() || climateBlizzard` expression as a separately asserted regression within this phase so ordinary `SNOW`/`SNOW_BLIZZARD` map consistently.
6. Define `doWeatherCycle=false` as disabling only the Vanilla compatibility bridge; descriptor-driven Frosted Heart climate and rendering continue with `WorldClimate`.
7. Compare before/after packet counts and server profiler results with Phase 0. Do not delete fallback until the counters prove the new path is quiet at steady state.

Exit condition: stable weather sends zero visual transition packets; entering/exiting a curtain sends only discrete compatibility events at most once per state change; client detail and `5s` smoothing are unchanged.

### Phase 6: Cleanup, Documentation And Final Profile

1. Remove `BlizzardRenderer` after confirming no runtime or planned fallback references remain.
2. Remove dead commented injection code and rename density settings or document/deprecate them according to the final fallback contract; do not silently reinterpret existing user config.
3. Keep the old renderer behind fallback for one release only if shader/mod compatibility QA still has unresolved environments; record an explicit removal condition rather than leaving permanent parallel code.
4. Run the full server/network/client matrix and archive before/after counters, packet bytes and frame-time results.
5. Update living docs and create the completion diary. Mark this plan completed with the actual outcome and deferred work.

## Test Coverage Plan

```text
CODE PATHS                                             PLAYER FLOWS

descriptor codec                                      login / dimension
  +-- old NBT fields -> descriptor                       +-- empty snapshot clears old world
  +-- descriptor -> old field shape                      +-- active curtain appears immediately
  +-- malformed logical payload -> keep last valid       +-- late old-dimension packet ignored

ClientWeatherState tick                               white curtain movement
  +-- anchor + level ticks -> bounded correction         +-- outside sees distant front
  +-- snapshot -> prepared coefficients, once            +-- cross front without chunk pop
  +-- all descriptors -> near/wall candidates, once      +-- inside core gets whiteout + wind
  +-- candidates -> previous/current primitive grids     +-- retreat restores visibility
  +-- terrain refresh hits per-profile budget             +-- build/break roof updates exposure

ClientWeatherFrame                                    graphics / ownership
  +-- begin once -> grid interpolation + camera sample    +-- Fast keeps wall/fog/wind/snow
  +-- WALL_ONLY -> wall submit, Vanilla precipitation     +-- Fancy adds density, not semantics
  +-- CUSTOM -> wall + snow, cancel Vanilla once          +-- renderer fault returns to fallback
  +-- FALLBACK -> no custom fog/snow/sound                 +-- global snow + local curtain merge

SpatialWeatherRenderer                               compatibility
  +-- culled wall -> bounded slices/segments              +-- no shader / Embeddium / Oculus
  +-- snow columns -> grid reads, no descriptor walk      +-- state change sends finite packets
  +-- near flakes -> reusable slots                       +-- stable motion sends no packets
  +-- ground impacts -> bounded Particle objects          +-- disconnect stops sound/clears state
  +-- all exits -> restore render state
```

### Automated Tests

| Test | Required assertions |
|---|---|
| `WhiteCurtainDescriptorCodecTest` | current NBT fixture decodes; round-trip retains `area/move/climate`; list order stable |
| `WhiteCurtainFieldModelTest` | four directions, exact current chunk parity, kernel/raw phase agreement before/after event, continuous chunk-center alignment, edge smoothing bounds, repeated kernel sample reuses caller output |
| `WhiteCurtainCacheTest` | cache hit before `validUntil`, exact invalidation at transition, add/clear generation invalidates immediately, prune reports change |
| `FHWhiteCurtainSnapshotPacketTest` | `0/1/8/32` list round-trip, dimension key, empty replacement, malformed logical payload preserves last valid state, encoded size reported and bounded |
| `ClientWeatherStateTest` | initial anchor, hourly correction, backward/forward bounded adjustment, snapshot atomicity, prepared arrays rebuild only on generation, disconnect/dimension reset, no visible single-frame phase jump |
| `ClientWeatherCandidateTest` | `0/1/8/32` snapshot prefilter; near/wall sets are complete; grid cells never visit excluded descriptors; candidate capacity grows only during snapshot replacement and never silently drops input |
| `ClientWeatherGridTest` | world alignment, previous/current swap, spatial bilinear and temporal interpolation, global-only/local-only/overlap merge priority, camera cell crossing, profile resize, indoor exposure stays visual-only, stable backing-array identity |
| `ClientWeatherFrameTest` | exactly one begin per frame, Fast/Fancy immutable caps, camera sample reuse, WALL_ONLY/CUSTOM/FALLBACK transitions, null level/camera selects fallback, paused client freezes the last complete frame, invalid frame selects fallback |
| `WeatherRenderPlanTest` | frustum/screen culling, hard slice/segment/quad caps, back-to-front order, unloaded-terrain fade, bounded terrain refresh, stable buffer reuse, no per-column descriptor walk or per-flake object path |
| `WeatherRenderOwnershipTest` | wall-only keeps Vanilla; spatial mode cancels once; fallback and exception paths restore state and do not double particles/fog/sound |
| `WeatherPerformanceCountersTest` | deterministic worst-case work counters cannot exceed active profile caps; `32` offscreen descriptors add prefilter checks but no grid evaluations or quads; stable frames report zero rebuild/terrain work |
| `PlayerWeatherCompatibilityModelTest` | unchanged state sends nothing, each discrete transition emits finite events, ordinary snow maps to rain, blizzard maps to rain+thunder |

Pure model, codec, cache, packet-planning, candidate selection, grid interpolation, work caps and ownership branches require JUnit tests. Wall-clock CPU/GPU time, JFR allocation, actual OpenGL state, shader compatibility, sound attenuation and two-player spatial divergence require game integration QA because mocks would hide the relevant engine behavior. Tests assert deterministic work counts; they must not use flaky elapsed-time assertions as a substitute for runtime profiling.

### Runtime Matrix

| Dimension/state | Positions | Client modes |
|---|---|---|
| Overworld global clear | outside/front/core/retreat/indoors | Fast/Fancy, `30/60/144+ FPS` caps |
| Overworld global snow | outside/front/core | Fast/Fancy, all particle statuses |
| Overworld global blizzard | outside/core | Fast/Fancy, existing density baseline |
| `0/1/8/32` descriptor snapshot | none visible / one visible / several candidates | Fast/Fancy |
| non-skylight dimension with capability behavior | login/change dimension/unload | Fast |
| shader environment | no shader, Embeddium, Oculus supported shader | Fancy |
| terrain churn | walk/sprint/teleport, build/remove roof, chunk unload edge | Fast/Fancy |
| renderer recovery | forced custom-pass exception, config toggle | custom -> fallback |

Run with one client, two clients on opposite sides of the front, and the largest practical multiplayer simulation. Replay the same camera path, resolution, FPS cap, seed and descriptor when comparing before/after. Capture server tick profile, packet count/bytes, client tick CPU, render-thread CPU, GPU/total median/P95/P99 frame time, draw submissions, allocations, working set, work counters and visual recordings. A 144+ FPS run specifically verifies that field evaluation remains tick-scaled rather than frame-scaled.

## Failure Modes

| Failure | Prevention/recovery | Test | Visible result if regression |
|---|---|---|---|
| Old saves cannot decode after descriptor extraction | preserve codec keys and old NBT fixture | codec test | world load failure; release blocker |
| Add/clear keeps stale gameplay result | generation invalidation in every mutation | cache test | storm persists or fails to appear for up to an hour |
| Client receives prior-dimension snapshot | dimension key check and atomic replacement | packet test | wrong world's wall appears silently |
| Snapshot arrives before target level exists | one bounded pending slot applied only on matching level load; clear on disconnect | state/packet test | joining player never sees an already-active curtain |
| Client clock drifts, jumps or scales with FPS | one state anchor, tick estimate, one frame query and bounded correction | state/clock test + 30/144 FPS runtime | wall teleports, moves at wrong speed or high-FPS clients pay more CPU |
| Snapshot decode fails | preserve last valid state and bounded diagnostic | packet test | old wall continues temporarily instead of disappearing/crashing |
| Snapshot replacement exposes mixed old/new arrays | prepare temporary immutable arrays, then replace on client thread | state test | wall bounds and climate phase disagree for one or more frames |
| Each snow column walks all descriptors | tick prefilter + shared primitive grid; counter asserts zero renderer field calls | candidate/performance test | CPU grows as columns multiplied by world curtain count |
| Candidate buffer overflows | grow only during snapshot replacement; never truncate; record count | candidate test | a white curtain disappears silently |
| Grid recenters with a visible seam | world-aligned cells, previous/current grids and bilinear interpolation | grid test + crossing recording | weather pops when crossing a 2/4-block boundary |
| Terrain cache is stale after movement/building | fixed refresh budget, cell invalidation and fade from old to new height | render-plan test + terrain-churn QA | snow passes through a new roof or wall floats briefly |
| Transparent wall saturates fill-rate | slice/segment hard caps, screen culling and shader-specific GPU gate | render-plan test + GPU capture | GPU P95 exceeds budget while CPU appears healthy |
| High FPS repeats tick work | grids update only from client tick; frame reads/interpolates | state/frame counters + 144 FPS run | CPU cost rises linearly with refresh rate |
| Stable frame allocates samples/particles | primitive arrays, caller-owned scratch and reusable flake slots | backing identity tests + JFR | GC spikes while standing still in a storm |
| Custom and Vanilla precipitation both run | one shared render ownership result | ownership test | doubled snow and frame cost |
| Render exception leaks global state or retries forever | `try/finally`, quarantine custom ownership for the level session, fallback | ownership test + integration | later rendering corrupts or the same exception stalls every frame; release blocker |
| Front requests unloaded chunks | only sample already-loaded client terrain and fade | render-plan test | network/chunk load spikes avoided |
| Low graphics removes the defining effect | semantic LOD contract | render-plan + visual QA | fewer flakes allowed; missing wall/fog/wind is a failure |
| Weather loop survives unload/fallback | one state-owned loop stopped on lifecycle/ownership transition | state test + dimension QA | wind continues in menus or a clear dimension |
| Server packet count remains tick-scaled | counters and Phase 0/5 gate | compatibility test + runtime profile | implementation cannot enter cleanup phase |
| Other weather mods conflict with fog/weather cancel | standard render path, fallback flag, compatibility matrix | integration QA | fallback remains available until environment is classified |

No planned silent path may both lack a test and lack recovery. Malformed client snapshot, late dimension packet, render-state leakage and stale cache are release-blocking cases, not follow-up work.

## Acceptance Criteria

1. Existing `whiteCurtainInfos` save data loads without conversion commands or loss of active curtains.
2. `WhiteCurtainFieldModel.sampleGameplay` matches current output for all four directions and event phases.
3. Add/clear/prune updates server queries and client snapshot immediately; no hourly stale-cache interval remains.
4. Stable white-curtain motion sends no snapshot or strength packets; hourly clock correction reuses `FHClimatePacket`.
5. Login and dimension change need one snapshot, with `0/1/8/32` descriptor encoded sizes and snapshot-prepare times recorded; typical snapshot remains under the stated network budget.
6. Server performs no weather work proportional to client render distance, snowflake count, wall slice count or graphics quality.
7. Per-player Vanilla compatibility sampling runs no faster than once per second and sends only on discrete state changes.
8. From outside, the player can identify front position, width and travel direction; the wall fades at render-distance and unloaded-terrain boundaries.
9. Crossing the front continuously changes snow, wind and visibility; two clients on opposite sides see different states from the same descriptor.
10. Indoors reduces camera whiteout and sound exposure without changing authoritative outdoor climate/temperature at that coordinate.
11. Global snow, global blizzard, local curtain, Vanilla fallback and supported shader environments do not produce double precipitation, fog or sound.
12. `ClientWeatherState.tick` and render-thread CPU remain within their separate P95 budgets; a `144+ FPS` run proves descriptor filtering and grid fill remain tick-scaled.
13. Fancy weather GPU median is no worse than current `blizzardDensity=15`，P95 满足 `min(1.0, 0.08 * 1000 / targetFps) ms`；render-thread CPU P95 满足 `min(0.5, 0.05 * 1000 / targetFps) ms` 且绝对不超过 `0.75 ms`，无 shader 和 supported Oculus 分别验收。
14. Stable camera/weather records `0 B/frame` after warmup, no descriptor/geometry rebuild, no full terrain rescan and no per-flake/column Java allocation.
15. Fast/Fancy obey their slice, segment, quad, particle and terrain-query caps; `32` offscreen descriptors add only one prefilter scan and no grid evaluation or draw work.
16. The client weather incremental working set remains `<= 8 MiB`; dimension switches and repeated snapshots do not grow retained memory.
17. Render state is restored after empty frames, culling, dimension unload and injected test exceptions; the latter moves cleanly to Vanilla fallback without repeated per-frame failure.
18. Fast mode retains the visible front, spatial snow gradient, whiteout, wind direction and weather sound; only density and sampling resolution may differ.
19. All pure model, codec, cache, packet, state, grid, counter and ownership tests pass; full `gradlew.bat test` and `git diff --check` pass.
20. Living documentation, config descriptions, CPU/GPU/allocation evidence and completion diary describe the shipped path rather than this intended plan.

## V2 Handoff Contract

V1 必须把天气表现实现为可替换的客户端 backend，而不是把状态采样、GL 调用和 Mixin ownership 焊在一起。V1 发布时至少保留：

```text
ClientWeatherState / ClientWeatherFrame
                 |
        WeatherRenderBackend
          /               \
 COMPATIBILITY         SPATIAL_V1
 Vanilla fallback      V1 wall + snow

future, same contract: CINEMATIC_V2
```

交接边界：

1. `ClientWeatherState` 继续唯一拥有 snapshot、时钟锚点、`VisualKernel[]`、候选和双近场网格。
2. `ClientWeatherFrame` 每帧只冻结一次，并额外提供固定容量、只读的 visible-volume primitive view；它由现有 visible-wall candidates 派生，不为 V2 再扫 snapshot。
3. backend 生命周期至少覆盖 begin-frame、opaque 后的大气阶段、现有天气阶段、resize、resource reload 和 close。所有 hook 委托同一个 coordinator。
4. V1 的 `SPATIAL_V1` 和 `COMPATIBILITY` 必须在 V2 完成后继续存在；性能策略不自动改变玩家 backend，只有 V2 无法正确渲染的功能故障才恢复到 V1，不能让 shader 或 depth 错误造成黑屏。
5. V2 不得修改 V1 的服务端 descriptor、packet、时钟、gameplay sampler 或稳定期网络预算。需要改变这些契约时必须先回到本计划重新验证，而不是在 renderer 中旁路。
6. V1 首次完成时默认 backend 为 `SPATIAL_V1`。V2 的详细低分辨率体积、时域、深度合成、性能控制和发布 gate 只属于独立的 [`V2 电影级渲染计划`](2026-08-24_05-08-24_white-curtain-v2-cinematic-rendering.md)，不进入 V1 验收范围。

这个边界只提前引入一个真实的替换点，不要求 V1 创建 framebuffer、history、Oculus depth provider 或 V2 shader。V1 仍按本计划的 `<= 8 MiB`、GPU P95 `8%` 帧预算（最高 `1 ms`）和固定 Fast/Fancy profile 独立验收。

## NOT In Scope

- 不重写长期 `ClimateEventTrack` 的生成概率、温度曲线、白幕速度或平衡数值；本计划只抽取并表现现有事实。
- 不改变作物、玩家温度、积雪放置、伤害或预报的玩法公式，除非为保持现有白幕结果所需的纯委托重构。
- 不建立通用第三方空间天气 API；第一版只保留低频 Vanilla rain/thunder 兼容桥。
- 不同步服务器生成的雪花、粒子、网格、噪声纹理、逐帧强度或前沿坐标。
- 不实现逐玩家 render-distance 兴趣管理；白幕列表低频且规模小，完整维度 snapshot 更便宜、更可验证。
- 不创建逐 chunk 持久天气 capability、移动实体或加载区块索引。
- 第一版不使用自定义 framebuffer、全屏后处理、compute shader 或只能在特定 shader pack 下工作的效果。
- 第一版不增加客户端 worker thread、timer、异步 VBO 上传或锁；当前固定工作预算不需要跨线程复杂度。
- 第一版不做基于瞬时 FPS 的动态 LOD；先让 Fast/Fancy 固定 profile 可复现地达标，避免画质泵动掩盖性能问题。
- 不为了极端 descriptor 数量预建按覆盖面积展开的 macro-chunk 索引；先做一次线性预筛和 `32` 条压力验证，routine 数量超过该边界再用测量决定索引形式。
- 不要求不同客户端看到完全相同的单片近景雪花；前沿、强度、方向和大尺度噪声必须一致。
- 不修改 `design/`、KubeJS、配方、任务、数据包或伴生整合包配置。

## Documentation Impact

完成后更新：

- `docs/climate/weather-rendering.md`: 将现役链路改为 descriptor、client field、render ownership 和低频兼容桥，删除已不成立的逐 tick 描述。
- `docs/climate/data-lifecycle-and-integration.md`: 更新 capability 内容、snapshot 时机、packet 频率、缓存失效和性能边界。
- `docs/climate/world-climate-and-temperature.md`: 仅在 descriptor 抽取改变代码锚点时更新，不把视觉 profile 写成服务端温度公式。
- `docs/climate/README.md`: 保持入口与状态为 Current。
- `diary/`: 记录各阶段完成情况、旧存档验证、packet/call counters、服务器 profile、客户端 before/after 和未启用的 fallback。

实现完成、放弃或被替代时更新本计划 `Status` 和 `Outcome`，不得让 `ready` 计划被误认为已实现架构。

## Execution Strategy

### Dependencies

| Workstream | Modules | Depends on |
|---|---|---|
| A. Baseline and common field model | `gamedata/climate`, tests | none |
| B. Snapshot and `ClientWeatherState` | `network`, climate events, client state/tests | A |
| C. Frame contract and wall/fog/sound | `render`, client config/assets/tests | A and the state/grid interface fixed by B |
| D. `SpatialWeatherRenderer` snow ownership | `render`, client Mixin/tests | C |
| E. Low-frequency Vanilla compatibility | server weather Mixin, player climate state | B and D |
| F. Full validation/docs/cleanup | tests, docs, diary | B through E |

Recommended order:

```text
A baseline -> A descriptor/model
                  |
                  +--> B snapshot/state ----+
                  |                         |
                  +--> C renderer skeleton -+--> D spatial snow
                                                 |
                                                 +--> E packet migration
                                                      |
                                                      +--> F cleanup/docs
```

After A fixes the common interfaces, B and the renderer's geometry/assets work may proceed in parallel worktrees. Their integration points are the primitive grid layout and `ClientWeatherFrame`; freeze those contracts before parallel work. B owns client state/grid code, while C owns render/config/assets, so neither lane should create a second weather-field abstraction. D and E remain sequential because packet removal is only valid after custom precipitation owns the frame.

Recommended commit boundaries:

1. baseline counters and recorded current behavior;
2. descriptor, field model, cache invalidation and regression tests;
3. snapshot packet, client state/tick grid and state tests;
4. frame contract plus wall/fog/sound behind disabled client flag;
5. spatial renderer snow pass, fixed profiles and render ownership;
6. low-frequency Vanilla compatibility and server/network profile;
7. cleanup, configs, living docs, diary and plan outcome.

## Inline Diagram Requirements

Add short ASCII comments only where they protect non-obvious contracts:

- `WhiteCurtainFieldModel`: direction/distance/time mapping and chunk-center parity.
- `FHWhiteCurtainSnapshotPacket`: server snapshot to client atomic replacement and dimension rejection.
- `ClientWeatherState`: snapshot preparation -> tick candidate prefilter -> previous/current primitive grid; call out that render consumers never walk descriptors.
- `ClientWeatherFrame`: one begin per frame, interpolation and WALL_ONLY/CUSTOM/FALLBACK ownership.
- `FHClientEvents`: client tick writes state, render tick freezes a read frame, later callbacks consume it.
- `LevelRendererMixin`: custom-versus-Vanilla ownership decision; remove the existing stale commented renderer block.
- Render ownership tests: show which path owns precipitation/fog/sound in each mode.

Do not copy the full plan into source comments. Comments must explain invariants that a future edit could otherwise break.

## Open Assumptions To Recheck Before Implementation

- Active white curtains are expected to remain a small list because creation rejects intersecting corridors. The client must still pass the `32` descriptor prefilter stress case. If profiling finds routine snapshots above `32`, evaluate an index that does not expand storage by curtain area; do not pre-build a per-covered-chunk map.
- The reference client profile must include the actual Embeddium/Oculus versions shipped with the pack before removing fallback.
- Runtime multiplayer capacity determines whether the `100` player server scenario is measured in a real server or a deterministic call/packet simulation; the plan requires evidence either way.

## Outcome

V1 主体已实现，但尚未通过完整运行时性能与兼容验收，因此本计划保持 `in-progress`。

已完成：

- `WhiteCurtainDescriptor` / `WhiteCurtainFieldModel` 抽取，旧 `area`、`move`、`climate` 和 `whiteCurtainInfos` 存档 contract 保持；区块玩法采样与旧语义一致。
- `WorldClimate` generation + 精确相位 cache expiry；add、clear、load、自然 prune 立即失效。
- `FHWhiteCurtainSnapshotPacket`、登录/换维度/所有重生（含末地通关）/创建/清除/自然结束同步；snapshot 和 `FHClimatePacket` 同步 `sec + clockDayTime`，小时与昼夜大跳复用现有气候包。
- `ClientWeatherState.tickClock` 在所有玩家模式下推进常数成本的 dayTime 锚点，Compatibility 不做候选/网格工作且使旧网格失效；切回 V1 不会等待下一次小时包或先显示旧前沿。空间路径包含 descriptor 预计算、每 tick 活动前沿候选排序、固定容量双网格、未衰减网格天气足迹、tick camera sample/ownership 和五通道 precipitation sampler；`ClientWeatherFrame` 使用 `Camera.setup` 后相机发布 `CUSTOM/WALL_ONLY/FALLBACK` render ownership。室内 exposure 只衰减表现，不会把工作交回旧 `tickRain`；玩家在前沿外也能看到网格内逼近的雪带。
- `SPATIAL_V1_FAST`、`SPATIAL_V1_FANCY` 和 `COMPATIBILITY` 固定玩家选择；`SpatialWeatherRenderer` 的有界墙、雪柱、地面粒子、共享雾和单 loop 风声。render pass 精确恢复进入时的 shader、texture 0 和 shader color，并声明 `AFTER_WEATHER` 的 depth/blend/cull canonical 后置状态；Oculus GL capture 仍是发布门。
- 每玩家 Vanilla bridge 的调度、capability 解析和局部采样均降为最多每秒一次，状态不变零包；自定义 renderer 功能故障按维度隔离到 compatibility，不做 FPS 驱动自动换档。
- 雪量与白化相位统一 `5 logical seconds` 平滑；低成本室内模型每 tick 只做一次眼位 `canSeeSky`，统一衰减近场雪、雾、风与声音；风声以非零初始音量启动。
- 活动墙按前沿距离稳定排序，Fast/Fancy 最近 `4/8` 个候选公平分配全局 slice 预算，不再由 snapshot 首项吃满全部 quads。
- 修复首次人工穿越发现的 V1 表现回归：墙体不再把仅约 `0.8%` 像素不透明的 Vanilla `snow.png` 当连续幕布，而使用一张可平铺的致密 `white_curtain.png`；仍复用原 wall batch 和 `64/256` quad 上限，不增加 pass/draw。近场雪柱以世界单元而不是逐方块相机原点锚定，并使用基于单元 hash 的固定偏移；Fast 相机移动一格不再让全部 `256` 列换奇偶格，跨 `2-block` 单元只在径向淡出外圈换入一排。
- 修复 ownership 边界：普通雪和原暴风雪不再因 `globalClimate` 网格底色触发 `CUSTOM`，继续使用 Vanilla/旧暴风雪渲染；只有白幕 descriptor 的近场贡献能取消 `renderSnowAndRain`。没有 descriptor 或可见候选时空间状态不再重建网格，普通天气不支付 V1 网格与雪柱成本。
- V1.1 移除“世界屏障”式规则墙面：在现有 segment 内加入共享端点的低幅确定性前沿起伏、`48 blocks` 走廊端点淡出和逐层 U/V 风动；墙纹理改为单一风向的细尺度云雪噪声。首轮 `12/9 blocks` 起伏与交叉斜线纹理经用户截图证明会产生巨大地面多边形和纱网感，因此收敛为 `2.5/2 blocks` 起伏、`6/4.5 blocks` 层间距和更细的 `12x24 blocks` UV 平铺。Fast/Fancy 从 `4x16 / 8x32` 收缩为 `3x12 / 5x20`，wall quad 上限由 `64/256` 降为 `36/100`；每 wall 先做一次 frustum cull。雪柱世界坐标/hash 按相机单元缓存，圆外单元在双网格采样前剔除。没有新增 draw、shader、framebuffer、服务端或网络工作。
- 夜间截图继续暴露两个 V1 材质错误：低 alpha 区域把夜空读成黑洞，未使用 lightmap 的墙色保持白天亮度。最终纹理 alpha 收敛到 `[51,193]`，墙色按一次 `ClientLevel.getSkyDarken` 读取调制；后续人工反馈表明首轮 `34.4%` 夜间亮度过暗，因此提高为典型夜间 `64%`。垂直墙面限制为相机 `-64/+96 blocks`，顶部 alpha 保持 `8%`，fog far plane 继续限制远处辉光。这些变化不增加 draw、shader、光照查询或服务端状态。
- 进入白幕后，Fog callback 仍负责最终 terrain `farPlaneDistance`，但 wall pass 不再在 CPU 端按该值删除 geometry。墙和近场雪改用 Minecraft 原版 `particle` shader/format，直接复用 `fog_distance` / `linear_fog` 与 light texture；固定 `544 blocks` 只保留为工作量上限。这消除了动态 far plane 和 Y 轴阈值导致的整段突隐。
- 最终 V1 表现目标明确为“雪版沙尘暴”：外部前沿遮住内部，穿越后核心视野极低。墙纹理 alpha 收敛为 `[154,235]`、Fast 三层合成 opacity `[0.82,0.94]`；Fast/Fancy 的 `minimumVisibilityBlocks` 统一为 `16`，Fog event 在比例缩放后再执行该绝对 far-plane 上限。画质档不改变玩法信息范围，且没有增加切片、draw 或地形工作。
- Y 轴不能复用水平 `16-block` far plane做 CPU 硬裁剪，否则墙会早于仍可见的地面消失。墙恢复固定相机相对 `-64/+96 blocks` geometry，真正可见部分完全交给与原版地面相同的 particle fog shader 连续消隐，不增加垂直 segment 或 quad。
- `38` 条 V1 定向 JUnit 已通过；Java 17 全量结果为 `588 tests, 0 failures, 0 errors`。

未完成：

- 进入实际世界后观察/穿越白幕、Fast/Fancy/Compatibility 切换、登录/换维度/清除/末地返回和异常注入的人工 smoke matrix；当前只完成到主菜单启动。
- 固定轨迹下 server tick/JFR、packet bytes、client tick、render-thread、GPU P50/P95/P99、稳态 allocation、retained memory 和 `30/60/144+ FPS` profile。
- Embeddium/Oculus、无 shader、4K、低端硬件和多人 opposite-side 兼容/性能验收。
- 墙体已按 wall-level frustum 和固定 segment 做距离裁剪，并使用 level build height 的 camera-relative 垂直坐标；segment 级屏幕区间裁剪、地形贴合、持久 VBO/复用 staging buffer 和 V2 backend 生命周期交接仍未实现。空批走 `endOrDiscardIfEmpty()`；非空 `Tesselator.end()` 仍会创建 batch 包装，`0 B/frame` 尚未达成。V1 墙仍是有界透明 geometry，不是电影级体积天气。
