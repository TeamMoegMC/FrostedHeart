# 暴风雪与白幕天气渲染架构

- Status: `Current`
- Last verified: `2026-11-19`
- Scope: 普通降雪、暴风雪与局部白幕从服务端权威状态到 V1 客户端空间重建、降水、雾、地面粒子和声音的现役实现
- Primary code anchors: `WhiteCurtainDescriptor`, `WhiteCurtainFieldModel`, `WhiteCurtainInfo`, `WorldClimate`, `FHWhiteCurtainSnapshotPacket`, `FHClimatePacket`, `ClientWeatherState`, `ClientWeatherFrame`, `SpatialWeatherRenderer`, `CurtainVolume`, `CurtainExtinction`, `WeatherSoundLoop`, `WeatherRenderingMode`, `PlayerWeatherCompatibilityModel`, `LevelRendererMixin`, `FogModification`, `FHClientEvents`

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
depth extinction / wall + snow columns    ambient fog
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

区块中心与服务端玩法相位一致；走廊边缘按 profile 平滑，雪量和白化相位都在相位切换后用 `WhiteCurtainVisualProfile.phaseTransitionSeconds=5` 做 `5 logical seconds` 平滑，不改变服务端整小时玩法结果。视觉结果写入调用者复用的 `MutableVisualWeatherSample`，包含 `snowIntensity`、`whiteoutIntensity`、`windIntensity`、风向和 `visibilityBlocks`。Fast/Fancy 的核心最低能见度都固定为 `16 blocks`；画质选项不会改变玩家在白幕内获得的信息范围。

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

`FHClimatePacket.climate` 仍是玩家所在区块的全局/白幕合并结果，供现有 HUD 和预报使用。新增的 `globalClimate` 只表示 `WorldClimate.getGlobalClimate()`，作为存在可见白幕时的客户端天气网格底色，避免把玩家当前位置的局部白幕错误铺满整个近场网格。全局普通雪和暴风雪不构成白幕空间 footprint，也不会单独创建网格或取得自定义降水 ownership。

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
2. 没有 descriptor 时直接发布 fallback；否则对所有 descriptor 做一次距离/相位预筛。没有近场或 wall candidate 时同样不创建 grid。
3. `CurtainVolume.set` 取 `leadingSnowDeltaChunks` 与 `trailingSnowDeltaChunks` 之间的降雪带，与走廊矩形求交。`ClientWeatherState.snowBandDistanceSquared` 按相机到这个矩形的距离稳定排序，保留 `512 blocks` 内的候选。即使前沿已越出走廊，留在原地的雪带仍可遮住远景。近场网格另按自身半径选取候选。
4. 交换固定容量 previous/current grid backing arrays；只让近场 candidates 填充 world-aligned grid，并发布复用的 tick camera sample 和 tick ownership。renderer、雾、声音和地面效果之后都不遍历 descriptor。

| V1 profile | Grid | Field cells/tick | Near prefilter radius | Wall slices | Wall segments | Snow columns/frame | Terrain queries/tick |
|---|---:|---:|---:|---:|---:|---:|---:|
| Fast | `9x9`, spacing `8 blocks` | `81` | `48 blocks` | `3` | `12` | `<=256` | `<=12` |
| Fancy | `17x17`, spacing `4 blocks` | `289` | `56 blocks` | `5` | `20` | `<=1024` | `<=32` |

每个 grid cell 复用六个 `float[]` 字段，前后两张网格在 tick 间交换。完整 camera/fog sample 使用六通道；雪柱使用不计算 `visibility` 的五通道专用 sampler；声音直接复用 tick camera sample。采样使用空间双线性插值，再按 `partialTick` 在 previous/current grid 间插值。屏外 descriptor 只付一次预筛，不触发 field evaluation 或 draw work。

## 5. 每帧 Ownership 与渲染

`LevelRendererMixin` 在 `LevelRenderer.renderLevel` 的 `HEAD` 使用 `Camera.setup` 后的当前相机和 `partialTick` 冻结帧，避免 `RenderTickEvent.START` 读取上一帧相机。`ClientWeatherFrame` 每个实际世界渲染帧只选择一次：

| Ownership | Condition | Precipitation owner |
|---|---|---|
| `CUSTOM` | 未衰减的 previous/current 近场网格内存在由白幕 descriptor 产生的 `snowIntensity` 或 `whiteoutIntensity > 0.01` | V1；frame owner 取消 `renderSnowAndRain`；tick owner 取消 `tickRain`；相机尚未穿过前沿时也能画出逼近的空间雪带 |
| `WALL_ONLY` | 近场网格无白幕降雪足迹，但附近存在 wall candidate | Vanilla 保留，包括普通雪和原暴风雪；V1 只画远处墙 |
| `FALLBACK` | compatibility、没有可见白幕、无有效 grid 或 renderer 已隔离 | Vanilla；全局普通雪和原暴风雪在空间模式下也走这条路径 |

### 5.1 视线穿过白幕的厚度

`SPATIAL_V1_FAST` 和 `SPATIAL_V1_FANCY` 在主渲染目标、有深度纹理、相机在空气中且未启用外部光影包时使用 `CurtainExtinction`。在 `AFTER_BLOCK_ENTITIES`，地形、树叶的实心像素、实体和方块实体已经写入场景深度。`curtain_extinction.fsh` 从深度还原可见表面，用相机与表面之间的线段裁切雪带。幕前地标保留原色，薄雪中的地标保留部分对比度，透过浓雪看的地标融入日夜雪雾色。站在幕内向晴空看时使用同一次线段裁切。

水平边界来自 `CurtainVolume.set`。垂直范围沿用相机下方 `64` 格至上方 `96` 格。内部浓雪区从各水平面向内收 `4` 格，从顶底向内收 `16` 格，每个方向的单边收缩最多为该方向总宽度的 `1/4`。着色器作两次解析线段求交。设 `Lout` 为线段在整个雪带中的长度，`Lin` 为在内部浓雪区的长度，两者都以方块计，范围从零到可见表面的距离。源代码定标如下。

```text
L = 0.28 × Lout + 0.72 × Lin
V = WhiteCurtainVisualProfile.minimumVisibilityBlocks() = 16 格（Fast、Fancy）
T = max(0, (2^(-6.643856 × L/V) - 0.01) / 0.99)
输出颜色 = T × 原场景色 + (1 − T) × 雪雾色
```

`T` 是透射率，范围 `[0,1]`。`L=0` 时原色完全保留，`L≥V` 时远景对比度为零。从室内向外看时，积分从离眼睛 `(1 − cameraExposure) × 16` 格处开始。`white_curtain.png` 只调制雪雾颜色的明暗，材质的透明区域不能在浓雪中打出透视孔。每个可见雪带至多画一个覆盖其屏幕范围的四边形，Fast/Fancy 上限分别为 `3/5`。颜色专用 framebuffer 借用主颜色纹理，着色器读取独立的场景深度，分别按世界卸载和原版资源生命周期管理 framebuffer 和 shader。

外部光影包、相机在流体中或缺少可采样的主深度时，V1 使用原有纹理墙与相机局部雾。`SpatialWeatherRenderer.renderWalls` 的 Fast/Fancy 仍共享每帧 `3/5` 层，每层 `12/20` 段的预算，墙与近场雪使用原版 `particle` shader。`COMPATIBILITY` 使用原版降水和旧天气修饰。

### 5.2 近场降雪、雾与声音

`SpatialWeatherRenderer.renderSnow` 在 `AFTER_WEATHER` 绘制近景雪。Fast/Fancy 的世界对齐网格为 `16×16`、间距 `2` 格和 `32×32`、间距 `1` 格。列坐标和纹理相位由世界单元决定，半径外列在采样前剔除。`tickGroundEffects` 使用 `MOTION_BLOCKING` 高度图，每 tick 至多 `12/32` 次地面查询，粒子设置 `DECREASED` 减半，`MINIMAL` 禁用。

`FogModification.snowColor` 从 `fogColorDay`、`fogColorNight` 和太阳高度得到雪色。在解析消光路径中，局部白幕由 `CurtainExtinction` 单独着色。`CUSTOM` 时只有同时发生的全局天气会收缩原版雾距，幕外 `WALL_ONLY` 继续原版天气雾。纹理墙路径仍按 `cameraSample` 调节雾距，满白化的远端上限为 `16` 格。

`WeatherSoundLoop` 最多持有一个非定位循环 `frostedheart:wind`，每 tick 直接读取已经做过室内暴露衰减的 tick camera sample 并平滑音量/音调。室内 exposure 只衰减效果，不改变 custom ownership，因此不会在屋内重新启用旧 `tickRain` 工作。新 loop 以首个非零目标音量提交，避免声音引擎拒绝零音量实例。禁用空间模式、`windSounds=false`、卸载世界或进入 renderer 隔离状态会停止循环。Compatibility 继续使用 `LevelRendererMixin.tickRain` 的旧雪声/风声逻辑。

## 6. 配置

配置路径是 Forge client config 的 `Weather` 分组：

| Java anchor | Default | Current meaning |
|---|---:|---|
| `weatherRenderChanges` | `true` | 总开关；false 强制 compatibility ownership，并停止 V1 loop |
| `weatherRenderingMode` | `SPATIAL_V1_FAST` | 玩家固定 backend/profile；不按 FPS 自动改变 |
| `fogDensity` | `0.1`, range `[0,1]` | 原版与回退路径的雾距缩放。解析消光的核心可见距离由 `minimumVisibilityBlocks` 确定 |
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
| Level unload | reset state/frame/quarantine, delete the borrowed-colour framebuffer and stop loop |
| Resource/config mode change | 下一 client tick/frame 读取固定选择；不需要重建服务端状态 |
| Render exception | 当前维度 session quarantine；下一帧 compatibility |

## 8. 性能边界与验证状态

代码已经建立工作量硬上限，但 CPU/GPU 数字必须来自真实客户端 profile，不能从上限反推：

- 服务端没有按渲染距离、雪柱、墙切片或客户端画质扩展的视觉工作。
- 稳定白幕没有专用周期 packet；现有小时 `FHClimatePacket` 同时校时。
- Fast/Fancy grid、wall、snow column 和 terrain query 上限由 enum 固定。
- 主路径复用帧缓冲的两张现有纹理，逐像素工作限制在投影到屏幕的矩形内。每个覆盖像素读取一次深度，对雪带外框和密集核心各求交一次，命中后读取一次雪纹理。服务端只维护原有气候事件。
- 候选按雪带距离筛选并执行屏幕矩形剔除。相机或近裁面与包围盒相交时，矩形保守扩展到全屏。近场降雪与纹理墙回退继续使用原有绘制预算。低端 GPU 帧耗、4K、启用外部光影包的画面仍待独立测量。
- 2026-11-19 的普通 Forge 客户端在同一冻结事件、机位和两座塔上验证了 Fast。删除后塔在晴天影响 1,881 个像素，在浓雪遮挡下影响 0 个。幕前塔的对照有 2,021 个像素发生变化。实机条件和采样区域见[开发记录](../../diary/2026-11-20_03-02-02_temperature-gameplay-and-weather-history.md#white-curtain-sightline-extinction)。

## 9. V1 表现边界与 V2

解析消光以不透明场景深度结束积分。它适用于地形和已写深度的地标。玻璃、水面和粒子稍后合成，半透明层之间的消光关系尚未建模。室内使用每 tick 一次眼位 `canSeeSky` 和 `0.15/tick` 的暴露平滑。垂直边界随相机高度，云底、沿坡卷起的雪浪、光照散射和时域重投影仍属于后续表现工作。

这些效果属于未实现的 [`V2 电影级渲染计划`](../../plans/2026-08-24_05-08-24_white-curtain-v2-cinematic-rendering.md)。V2 必须复用 V1 descriptor、时钟、候选和网格，保持服务端与稳定网络相对 V1 零增量；V1 的实现状态、未完成 profile 和验收项记录在 [`V1 工程计划`](../../plans/2026-08-24_04-25-01_white-curtain-server-efficient-spatial-rendering.md)。
