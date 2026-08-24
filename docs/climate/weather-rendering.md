# 暴风雪与白幕天气渲染架构

- Status: `Current`
- Last verified: `2026-08-24`
- Scope: 普通降雪、暴风雪与局部白幕从服务端权威状态到 V1 客户端空间重建、降水、雾、地面粒子和声音的现役实现
- Primary code anchors: `WhiteCurtainDescriptor`, `WhiteCurtainFieldModel`, `WhiteCurtainInfo`, `WorldClimate`, `FHWhiteCurtainSnapshotPacket`, `FHClimatePacket`, `ClientWeatherState`, `ClientWeatherFrame`, `SpatialWeatherRenderer`, `WeatherSoundLoop`, `WeatherRenderingMode`, `PlayerWeatherCompatibilityModel`, `LevelRendererMixin`, `FogModification`, `FHClientEvents`

本文只描述当前已经执行的 V1 链路。气候事件和温度公式见 [world-climate-and-temperature.md](world-climate-and-temperature.md)，生命周期和 packet 总表见 [data-lifecycle-and-integration.md](data-lifecycle-and-integration.md)。V2 电影级体积天气仍是未实现计划，不属于当前运行时。

## 1. 当前架构

V1 已经把玩法权威与视觉表现分开。服务端只保存稀疏白幕描述并计算玩家、作物和温度所需的区块级结果；客户端从同一描述连续重建近场天气，不接收雪花、雾、网格、逐帧强度或移动前沿坐标。

```text
ServerLevel / WorldClimate
  WhiteCurtainDescriptor[] + logical climate clock
           |
           +--> WhiteCurtainFieldModel.sampleGameplay
           |      authoritative chunk climate/temperature/forecast
           |
           +--> FHWhiteCurtainSnapshotPacket (state changes only)
           +--> FHClimatePacket (existing hourly clock/global climate)
                         |
                         v
ClientWeatherState
  prepared VisualKernel[]
  dayTime clock + bounded correction/discontinuity re-anchor
  once-per-tick candidate filter
  previous/current fixed weather grids
  tick camera sample + tick precipitation ownership
                         |
              +----------+-----------+
              |                      |
              v                      v
ClientWeatherFrame              WeatherSoundLoop / tickRain
current-camera render owner     shared tick sample
     +-------------------+
     |                   |
SpatialWeatherRenderer  FogModification
wall + snow columns     shared frame sample
     |
LevelRendererMixin cancels Vanilla precipitation only for CUSTOM

PlayerWeatherCompatibilityModel
  authoritative local ClimateType -> low-frequency Vanilla rain/thunder
  retained for compatibility and fallback
```

`BlizzardRenderer` 仍在源码中，但没有现役调用。V1 使用 `SpatialWeatherRenderer`；旧的注释式 `renderSnowAndRain` 接入已从 `LevelRendererMixin` 删除。

## 2. 服务端权威模型

### 2.1 Descriptor 与存档兼容

`WhiteCurtainDescriptor` 是一个白幕的稀疏权威数据：

| Field | Type | Meaning |
|---|---|---|
| `area` | `Rect`，单位 chunks | 白幕可传播的矩形走廊 |
| `move` | 水平 `Direction` | `NORTH/SOUTH/WEST/EAST` 传播方向 |
| `climate` | `ClimateEvent` | 温度与 `ClimateType` 的时间曲线 |

`WhiteCurtainInfo.CODEC` 通过 `xmap` 包装 `WhiteCurtainDescriptor.CODEC`，仍使用旧 NBT 字段 `area`、`move`、`climate`；`WorldClimate` 的列表键仍为 `whiteCurtainInfos`。旧存档不需要迁移命令。`WhiteCurtainInfo` 继续拥有预报缓存，实际空间公式委托给 `WhiteCurtainFieldModel`。

### 2.2 玩法传播公式

源常量与单位：

```text
HOURS_PER_CHUNK   = 6 climate hours/chunk
SECONDS_PER_CHUNK = 300 logical seconds/chunk
deltaSeconds      = deltaChunks * 300
localSeconds      = climateSeconds - deltaSeconds
gameplayHour      = trunc((localSeconds - event.startTime) / 50)
```

`deltaChunks` 从传播起始边计算；四个方向保持旧 `WhiteCurtainInfo.getDelta` 语义。`sampleGameplay` 仍按整气候小时读取事件，因此服务器玩法结果没有改成连续插值。`WorldClimate.getClimate(ChunkPos)` 把白幕类型与全局类型合并，温度查询取全局与白幕温度的较低值。

`WorldClimate.whitecurtainCache` 的 entry 现在保存 `ClimateResult`、`validUntilSeconds` 和 `whiteCurtainGeneration`。缓存精确失效到该区块的下一玩法相位，而不再只能等待全局小时边界。以下操作会立即增加 generation 并清缓存：

- 成功添加白幕；
- 清除白幕；
- NBT 载入；
- 每秒检查发现白幕已完全结束并自然移除。

创建仍拒绝与现有白幕走廊相交。自然结束条件为：

```text
event.calmEndTime + maxDeltaChunks * 300 < climateSeconds
```

### 2.3 视觉采样

`WhiteCurtainFieldModel.prepareVisual` 在 snapshot 替换时把事件按小时预计算为 `VisualKernel`，包括走廊边界、方向、相位数组、首尾降雪小时和稳定 seed。客户端使用 block 坐标连续计算传播延迟：

```text
continuousChunk = (blockCoordinate - 8) / 16
localSeconds    = climateSeconds - continuousDeltaChunks * 300
```

区块中心与服务端玩法相位一致；走廊边缘按 profile 平滑，雪量和白化相位都在相位切换后用 `WhiteCurtainVisualProfile.phaseTransitionSeconds=5` 做 `5 logical seconds` 平滑，不改变服务端整小时玩法结果。视觉结果写入调用者复用的 `MutableVisualWeatherSample`，包含 `snowIntensity`、`whiteoutIntensity`、`windIntensity`、风向和 `visibilityBlocks`。

## 3. 网络与客户端时钟

### 3.1 白幕 snapshot

`FHWhiteCurtainSnapshotPacket` 的 payload 是：

```text
dimension ResourceKey
climateSeconds VarLong
clockDayTime VarLong
List<WhiteCurtainDescriptor> encoded through NBT Codec
```

发送时机：玩家登录、换维度、所有重生路径（包括末地通关返回）、成功创建、清除以及自然结束。空列表也会发送，以原子替换旧维度状态。稳定移动只由客户端时钟重建，白幕专用网络成本是 `0 packets/player/second`。snapshot 和原有 `FHClimatePacket` 各增加一个 `clockDayTime VarLong`，没有新增周期 packet 类型。

解码失败时 packet 被标为无效，客户端保留最后一个有效 snapshot；诊断日志全进程最多输出 4 次。packet 在客户端线程应用，维度不匹配时拒绝；目标 level 尚未存在时只保留一个 bounded pending snapshot。

### 3.2 全局与局部气候

`FHClimatePacket.climate` 仍是玩家所在区块的全局/白幕合并结果，供现有 HUD 和预报使用。新增的 `globalClimate` 只表示 `WorldClimate.getGlobalClimate()`，作为客户端天气网格底色，避免把玩家当前位置的局部白幕错误铺满整个近场网格。

`WorldClockSource` 的唯一时间源是服务端 `dayTime`。`FHWhiteCurtainSnapshotPacket` 和 `FHClimatePacket` 同步同一对 `(sec, clockDayTime)`；客户端不再用持续增长的 `gameTime` 推动白幕。正常 client tick 只接受 `0..20` 的前向 `dayTime` 增量，关闭 `doDaylightCycle` 时增量为零；睡眠或 `/time` 的大跳由服务端在下一次一秒气候调度时复用 `FHClimatePacket` 重锚，客户端不会把同一跳变重复应用。

```text
normal tick: tickClimateSeconds += dayTimeDelta / 20
normal frame: frameSeconds = tickClimateSeconds + partialTick / 20
frozen/jump frame: frameSeconds = tickClimateSeconds
```

校时误差 `<5 logical seconds` 时，每个 client tick 最多修正 `0.10 logical seconds`，新观测会替换而不是累加未完成误差；误差 `>=5 seconds` 时立即重锚。小时变化照常发包，`WorldClockSource.update` 检出大跳时也会在下一次 `20-tick` 调度复用一次现有气候包。`ClientWeatherState.tickClock` 在 Compatibility 和空间模式都只推进这些标量；候选预筛和网格填充只在空间模式执行，因此玩家切回 V1 时不会读取停止期间的旧前沿，也不会为 Compatibility 支付场采样成本。每帧只读取标量，工作量不随显示刷新率增加。

### 3.3 Vanilla 兼容桥

`ClimateCommonEvents.onServerTick` 的现有 `gameTime % 20 == 0` 分支每玩家解析一次 capability，并把已经取得的 `WorldClimate` 和 `player.chunkPosition()` 交给 `PlayerTemperatureData.advanceWeatherCycle`。外层调用、capability 解析和气候采样都不超过 `1/player/second`。类型不变时不发包；发生 `clear/snow/blizzard` 离散变化时，`PlayerWeatherCompatibilityModel` 才发送有限的 Vanilla `ClientboundGameEventPacket`：

| Local climate | raining | thundering | rain strength | thunder strength |
|---|---:|---:|---:|---:|
| clear | false | false | `0.0` | `0.0` |
| snow / snow-blizzard | true | false | `0.8` | `0.0` |
| blizzard | true | true | `0.8` | `0.8` |

这条桥用于 `COMPATIBILITY`、其他模组观察到的 Vanilla 天气状态以及自定义 renderer 故障恢复；它不再承担逐 tick 视觉渐变。

## 4. 客户端状态与固定工作量

`WeatherRenderingMode` 是玩家固定选择，不根据 FPS 自动改变：

| Mode | Behavior |
|---|---|
| `COMPATIBILITY` | Vanilla precipitation 加现有 Mixin 修饰 |
| `SPATIAL_V1_FAST` | V1 固定 Fast profile；默认值 |
| `SPATIAL_V1_FANCY` | V1 固定 Fancy profile |

`ClientWeatherState.tick` 每 client tick 至多执行一次：

1. 应用匹配维度的 pending snapshot 和有界时钟修正。
2. 对所有 descriptor 做一次距离/相位预筛；活动前沿 `512 blocks` 内的 wall candidates 按前沿到相机距离稳定排序，近场半径只保留会影响网格的 candidates。
3. 交换固定容量 previous/current grid backing arrays。
4. 只让近场 candidates 填充 world-aligned grid；发布复用的 tick camera sample 和 tick ownership。renderer、雾、声音和地面效果之后都不遍历 descriptor。

| V1 profile | Grid | Field cells/tick | Near prefilter radius | Wall slices | Wall segments | Snow columns/frame | Terrain queries/tick |
|---|---:|---:|---:|---:|---:|---:|---:|
| Fast | `9x9`, spacing `8 blocks` | `81` | `48 blocks` | `4` | `16` | `<=256` | `<=12` |
| Fancy | `17x17`, spacing `4 blocks` | `289` | `56 blocks` | `8` | `32` | `<=1024` | `<=32` |

每个 grid cell 复用六个 `float[]` 字段，前后两张网格在 tick 间交换。完整 camera/fog sample 使用六通道；雪柱使用不计算 `visibility` 的五通道专用 sampler；声音直接复用 tick camera sample。采样使用空间双线性插值，再按 `partialTick` 在 previous/current grid 间插值。屏外 descriptor 只付一次预筛，不触发 field evaluation 或 draw work。

## 5. 每帧 Ownership 与渲染

`LevelRendererMixin` 在 `LevelRenderer.renderLevel` 的 `HEAD` 使用 `Camera.setup` 后的当前相机和 `partialTick` 冻结帧，避免 `RenderTickEvent.START` 读取上一帧相机。`ClientWeatherFrame` 每个实际世界渲染帧只选择一次：

| Ownership | Condition | Precipitation owner |
|---|---|---|
| `CUSTOM` | 未衰减的 previous/current 近场网格内存在 `snowIntensity` 或 `whiteoutIntensity > 0.01` | V1；frame owner 取消 `renderSnowAndRain`；tick owner 取消 `tickRain`；相机尚未穿过前沿时也能画出逼近的空间雪带 |
| `WALL_ONLY` | 近场网格无降雪足迹，但附近存在 wall candidate | Vanilla 保留；V1 只画远处墙 |
| `FALLBACK` | compatibility、无有效 grid、无可见天气或 renderer 已隔离 | Vanilla |

### 5.1 风暴墙与近场降雪

`SpatialWeatherRenderer` 在 `RenderLevelStageEvent.Stage.AFTER_WEATHER` 提交有界 geometry；墙使用 `assets/frostedheart/textures/environment/white_curtain.png`，近场 streak 继续使用 Minecraft `textures/environment/snow.png`：

- 风暴墙沿 `VisualKernel.leadingSnowDeltaChunks` 移动；Fast/Fancy 分别最多选择最近 `4/8` 个活动前沿，按墙公平分配全局 `4/8` 个 slice，最终仍是 `64/256 wall quads` 上限，远 descriptor 不会按 snapshot 顺序饿死近墙。距离裁剪逐固定 segment 计算，超宽走廊不会再因整墙中心过远而误删近端；垂直顶点使用 level build height 的 camera-relative 坐标。
- 专用 `64x256 RGBA` 墙纹理具有连续雪雾 alpha，而不是原版雪纹理的稀疏 flake atlas。每层顶点 alpha 为 `a = (0.24 + 0.30s)d`：`s` 是 `[0,1]` 的 slice fade，`d` 是 `[0,1]` 的距离 fade；不同 slice 使用确定性的 UV 偏移，横向 UV 按每 `24 blocks` 一次平铺，纵向按世界 Y 每 `32 blocks` 一次平铺并连续滚动。Fast 的完整四层在满距离权重下由资源回归测试约束为合成 opacity `>=0.55`。这没有新增墙 pass、draw、quad 或 shader。
- 近场雪柱使用固定 `16x16 @ 2-block spacing` 或 `32x32 @ 1-block spacing` 网格。设间距为 `p blocks`、边长为 `N`，起始单元是 `c0 = floor(floor(cameraCoord) / p) - N/2`；每列位置由世界单元坐标和固定 hash 决定，并加入范围 `[-0.28p, 0.28p]` 的确定性水平偏移。因此相机在同一单元内移动不会改变任何雪柱位置，跨单元时只有径向淡出区的一排退出、另一排进入，不再整体换奇偶格。每列仍只采样共享天气 grid，不查询 descriptor 或地形，并按局部风向/风强倾斜 streak。
- `tickGroundEffects` 只查询 `Heightmap.Types.MOTION_BLOCKING`，Fast/Fancy 最多 `12/32` 次每 tick；Vanilla `DECREASED` 粒子选项减半，`MINIMAL` 禁用。
- 没有逐雪花对象、逐列 descriptor 扫描、自定义 framebuffer、compute shader 或客户端 worker thread。

渲染 pass 在进入时保存 shader、texture unit 0 和 shader color，并在 `finally` 精确恢复；同时把 `AFTER_WEATHER` 的 canonical 后置状态恢复为 depth test/write 开、cull 开、blend 关和默认 blend func。V1 不修改 viewport、scissor、blend equation 或 depth func。若自定义 pass 抛出运行时异常，会释放仍打开的 `BufferBuilder`、隔离当前维度，并从下一帧统一切回 compatibility ownership 和声音；不会因为测得 FPS 较低而自动换档。正常、空批和异常出口仍必须在无 shader/Oculus 的运行时 GL capture 中验证，当前自动化测试不能证明驱动状态。

### 5.2 雾与声音

`FogModification` 在 `CUSTOM` 下直接读取同一帧 `cameraSample`，不再独立计算白幕位置或时钟。非 custom 模式继续按 Vanilla rain/thunder 使用旧的平滑雾逻辑。`fogDensity`、`fogColorDay` 和 `fogColorNight` 仍控制最终雾强度与颜色。

`WeatherSoundLoop` 最多持有一个非定位循环 `frostedheart:wind`，每 tick 直接读取已经做过室内暴露衰减的 tick camera sample 并平滑音量/音调。室内 exposure 只衰减效果，不改变 custom ownership，因此不会在屋内重新启用旧 `tickRain` 工作。新 loop 以首个非零目标音量提交，避免声音引擎拒绝零音量实例。禁用空间模式、`windSounds=false`、卸载世界或进入 renderer 隔离状态会停止循环。Compatibility 继续使用 `LevelRendererMixin.tickRain` 的旧雪声/风声逻辑。

## 6. 配置

配置路径是 Forge client config 的 `Weather` 分组：

| Java anchor | Default | Current meaning |
|---|---:|---|
| `weatherRenderChanges` | `true` | 总开关；false 强制 compatibility ownership，并停止 V1 loop |
| `weatherRenderingMode` | `SPATIAL_V1_FAST` | 玩家固定 backend/profile；不按 FPS 自动改变 |
| `fogDensity` | `0.1`, range `[0,1]` | 雾距离缩放强度 |
| `fogColorDay` / `fogColorNight` | `0xbfbfd8` / `0x0c0c19` | 雪天气雾色 |
| `snowDensity` / `blizzardDensity` | `10` / `15`, range `[1,15]` | 只影响 compatibility 的 Vanilla column 半径，不改变 V1 caps |
| `snowSounds` | `true` | compatibility 的地面雪声 |
| `windSounds` | `true` | V1 循环风声以及 compatibility 暴风雪风声 |

Fast/Fancy 不读取 Vanilla graphics 的 fast/fancy 状态；这是为了让玩家选择可复现的固定工作上限。Minecraft 自带 particle status 仍约束 V1 地面粒子。

## 7. 生命周期

| Event | V1 action |
|---|---|
| Client login | reset state/frame/renderer quarantine/sound；等待 snapshot |
| Snapshot for loaded dimension | prepare immutable descriptor list and `VisualKernel[]`，原子替换 |
| Snapshot before level | 保存一个 pending slot，匹配维度首 tick 应用 |
| Dimension mismatch | 拒绝旧维度 packet；tick 清空旧 kernels/grid |
| Level unload | reset state/frame/quarantine and stop loop |
| Resource/config mode change | 下一 client tick/frame 读取固定选择；不需要重建服务端状态 |
| Render exception | 当前维度 session quarantine；下一帧 compatibility |

## 8. 性能边界与验证状态

代码已经建立工作量硬上限，但 CPU/GPU 数字必须来自真实客户端 profile，不能从上限反推：

- 服务端没有按渲染距离、雪柱、墙切片或客户端画质扩展的视觉工作。
- 稳定白幕没有专用周期 packet；现有小时 `FHClimatePacket` 同时校时。
- Fast/Fancy grid、wall、snow column 和 terrain query 上限由 enum 固定。
- 专用墙纹理替换只改变现有 wall batch 的 texture binding；雪柱世界锚定只改变现有列坐标生成。两者均不增加服务端工作、packet、field sample、quad 上限或 draw 数。
- snapshot 替换会分配 descriptor list、kernel phase arrays 和 candidate arrays；状态、sample 和 grid 数组在稳定 tick/frame 复用。空批使用 `BufferBuilder.endOrDiscardIfEmpty()`，但非空 `Tesselator.end()` 仍会为 wall/snow batch 生成 `DrawState/RenderedBuffer` 包装，因此不能宣称已经达到 `0 B/frame`；JFR 后若不达门，需改持久 VBO/复用 staging buffer，而不是只凭数组复用通过验收。
- 当前墙已做活动前沿距离排序、公平 slice 和逐 segment 半径裁剪，但还没有真实 frustum/屏幕区间裁剪、持久墙 VBO 或地形贴合；这些仍是 V1 profile/release gate。
- 当前没有完成 30/60/144+ FPS、1080p/1440p/4K、Embeddium/Oculus 和低端 GPU 的 render-thread/GPU P95 测量，因此计划中的毫秒门仍是 release gate，不是已达成结果。

现有 V1 定向 JUnit 共 `38` 条，覆盖旧 Codec fixture、四方向传播、玩法等价、含末端边界、`5s` 雪/白化过渡、cache add/clear/prune、snapshot `0/1/8/32` round-trip 与 malformed payload、dayTime freeze/前后跳/重复校时、Compatibility 1000-tick 时钟连续性、候选排序、屏外工作量、双网格/专用 sampler、室内与前沿外 ownership、墙段距离、固定 caps、正负坐标雪柱锚定、墙纹理覆盖/接缝/合成 opacity 和 Vanilla compatibility 映射。Java 17 全量结果为 `588 tests, 0 failures, 0 errors`。

## 9. V1 表现边界与 V2

V1 已经是“真实空间白幕”的工程底座：远处存在会移动的致密纹理墙，玩家穿越时降雪、白化、雾和风声按位置连续变化，两名玩家可从同一 descriptor 得到不同局部画面。每 client tick 只用一个复用 `MutableBlockPos` 在玩家眼位执行一次 `canSeeSky`，并以 `0.15/tick` 平滑成 `cameraExposure`，统一衰减近场雪、雾、风和声音；远墙仍可从室内看见。这是低成本单点、二值 shelter 模型，不是逐列、多射线或 depth-aware 室内遮蔽。当前墙仍是有界半透明 geometry，没有地形吞没、体积云底、光照消光或 temporal history。

这些效果属于未实现的 [`V2 电影级渲染计划`](../../plans/2026-08-24_05-08-24_white-curtain-v2-cinematic-rendering.md)。V2 必须复用 V1 descriptor、时钟、候选和网格，保持服务端与稳定网络相对 V1 零增量；V1 的实现状态、未完成 profile 和验收项记录在 [`V1 工程计划`](../../plans/2026-08-24_04-25-01_white-curtain-server-efficient-spatial-rendering.md)。
