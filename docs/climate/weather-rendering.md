# 暴风雪与白幕天气渲染架构

- Status: `Current`
- Last verified: `2026-08-24`
- Scope: 暴风雪、普通降雪、白幕局部天气从服务端气候状态到客户端降水、雾、粒子和声音的现役实现
- Primary code anchors: `WorldClimate`, `WhiteCurtainInfo`, `ServerLevelMixin_WeatherCycle`, `PlayerTemperatureData.advanceWeatherCycle`, `PlayerListMixin`, `LevelRendererMixin`, `FogModification`, `FHClimatePacket`, `BlizzardRenderer`

本文只描述当前已经执行的天气视觉链路。世界气候事件的生成与温度公式见 [world-climate-and-temperature.md](world-climate-and-temperature.md)，能力、存档和通用网络生命周期见 [data-lifecycle-and-integration.md](data-lifecycle-and-integration.md)。

## 1. 当前结论

当前没有独立的“白幕渲染器”，也没有正在执行的自定义暴风雪几何渲染器。现役方案是：

1. `WorldClimate` 在服务端合成全局气候和玩家所在区块的白幕局部气候。
2. `PlayerTemperatureData` 把每位玩家的局部 `ClimateType` 平滑转换成该连接独有的 Vanilla `rainLevel` / `thunderLevel`。
3. 服务端用 `ClientboundGameEventPacket` 把这两个强度发给客户端。
4. 客户端继续调用 Vanilla `LevelRenderer.renderSnowAndRain` 和 `LevelRenderer.tickRain`，`LevelRendererMixin` 修改其降水类型、纹理、光照、半径、落地点粒子和声音。
5. `FogModification` 根据同一组 Vanilla rain/thunder 状态修改雾颜色和近远平面。

因此，“暴风雪”和“白幕”不是两套渲染技术。暴风雪是一种局部或全局 `ClimateType`，白幕是让该气候沿区块走廊传播的服务端空间模型；二者最终共用同一套客户端天气渲染。

```text
long-term ClimateEventTrack -------------------------+
                                                     |
WhiteCurtainInfo(position-shifted local event) --+   |
                                                 v   v
                                     WorldClimate.getClimate(playerChunk)
                                                     |
                         ServerLevelMixin_WeatherCycle, every server tick
                                                     |
                         PlayerTemperatureData.advanceWeatherCycle
                         rainLevel / thunderLevel += or -= 0.01
                                                     |
                  ClientboundGameEventPacket, per player connection
                                                     |
                                      ClientLevel weather state
                        +----------------------------+------------------+
                        |                            |                  |
           renderSnowAndRain + Mixin      tickRain + Mixin     ViewportEvent
           precipitation sheets          particles/sounds     fog color/range

WorldClimate -- FHClimatePacket, hourly/login/dimension --> ClientClimateData
                                                             |
                                                             +--> HUD/forecast
                                                                  not rendering
```

## 2. 服务端气候与白幕空间模型

### 2.1 全局气候和局部气候

`WorldClimate.getGlobalClimate()` 返回当前小时的全局 `ClimateType`。`WorldClimate.getClimate(ChunkPos)` 先查询白幕，再通过 `ClimateType.merge` 与全局气候合并：`BLIZZARD` 的优先级最高，其次是 `SNOW_BLIZZARD`、`SNOW`，然后是其他天气。局部温度则取全局与白幕结果的较低值。

白幕只保存在服务端 `WorldClimate.whitecurtains` 中；客户端没有白幕区域、方向或前沿几何。`WorldClimate` 作为 `FHCapabilities.CLIMATE_DATA` 附着在非 fixed-time 的 `Level`，并把白幕以 NBT 键 `whiteCurtainInfos` 持久化。

### 2.2 `WhiteCurtainInfo` 如何产生移动天气

`/climate white_curtain add [pos]` 调用 `WorldClimate.addWhiteCurtain`：

- `InterpolationClimateEvent.getBlizzardClimateEvent` 生成一条时长在 `[2, 7)` 游戏日的寒潮/暴风雪事件，随后带 `[1, 3)` 游戏日平静期。
- 随机选择 `NORTH`、`SOUTH`、`WEST` 或 `EAST` 方向。
- `WhiteCurtainInfo.generateArea` 生成一个按区块计的矩形走廊。横向两侧各随机扩展 4-9 chunks；一端随机扩展 6-11 chunks，另一端在容纳整个事件传播所需的长度上再随机增加 0-7 chunks。
- 新白幕只要与已有白幕矩形相交就拒绝加入。

矩形不是一个同时激活的天气区。每个区块根据它到传播起始边的 `getDelta` 获得时间偏移：

```text
HOURS_PER_CHUNK   = 6 game hours
SECONDS_PER_CHUNK = 300 logical seconds
localEventTime    = WorldClimate seconds - deltaChunks * 300
```

正常 20 TPS、未跳时钟时，一个游戏日为 1200 秒，一个白幕相位每 5 分钟向前推进一格区块。玩家所在区块只是对同一条 `ClimateEvent` 读取不同相位，所以能依次经历 `SNOW_BLIZZARD`、`BLIZZARD` 和结束后的平静段。

`getSnowRect`、`getBlizzardRect` 和 `getPartialRect` 可以计算理论前沿矩形，但当前没有运行时消费者；渲染、温度和预报都通过逐区块的 `getClimate` / `getFrames` 查询。

### 2.3 白幕缓存和刷新粒度

`WorldClimate.whitecurtainCache` 以 packed `ChunkPos` 缓存 `ClimateResult`。`WorldClimate.updateCache` 每秒被检查，但只在全局气候小时变化时清空该缓存；一个气候小时为 50 秒。

这带来两个当前语义：

- 白幕事件本身可以从任意秒开始，但局部气候缓存只按全局小时失效，因此相位变化最多可延迟到下一个全局小时边界。
- `addWhiteCurtain` 和 `clearWhiteCurtain` 不清空 `whitecurtainCache`。已查询区块中的新增或清除效果可能继续使用旧值，直到下次小时刷新。

过期白幕也只在小时刷新时由 `whitecurtains.removeIf(WhiteCurtainInfo::isInvalid)` 清理。

## 3. 从局部气候到客户端天气状态

### 3.1 全局 Vanilla 天气桥

`ServerLevelMixin_WeatherCycle` 完全覆写 `ServerLevel.advanceWeatherCycle`，并把 `resetWeatherCycle` 覆写为空，所以睡眠不会重置天气。该方法只在 `doWeatherCycle=true` 且维度有 skylight 时继续执行。

它先把全局 `ClimateType` 转换为服务端 `ServerLevelData.isRaining/isThundering`，再以每 tick `0.01` 更新世界级 `rainLevel` 和 `thunderLevel`。Vanilla `/weather` 的 clear/rain/thunder 计时器仍能覆盖这一步的全局布尔值。

当前源码中的：

```java
boolean climateBlizzard = climate.isBlizzard();
boolean climateSnowing = climate.isBlizzard() || climateBlizzard;
```

两个条件实际相同。因此没有 Vanilla 命令覆盖时，服务端世界级 `isRaining` 只会为 `BLIZZARD` 打开，普通 `SNOW` 和 `SNOW_BLIZZARD` 不会打开它。这与变量名和注释表达的意图不同，但属于当前实际行为。

世界级 rain/thunder 广播代码已被注释。真正驱动各客户端的是随后对每位 `ServerPlayer` 调用的局部天气桥。

### 3.2 每玩家局部插值与发包

`PlayerTemperatureData` 持有未写入其 NBT 的运行时字段 `oRainLevel`、`rainLevel`、`oThunderLevel`、`thunderLevel`。`advanceWeatherCycle` 每个服务端 tick 查询玩家当前 `ChunkPos` 的 `WorldClimate.getClimate`：

- `BLIZZARD`: `rainLevel` 和 `thunderLevel` 各增加 `0.01`。
- `SNOW` / `SNOW_BLIZZARD`: `rainLevel` 增加、`thunderLevel` 减少，各 `0.01`。
- 其他天气: 两者都减少 `0.01`。
- 所有值限制在 `[0, 1]`。

只要值变化，就分别发送 `RAIN_LEVEL_CHANGE` 和 `THUNDER_LEVEL_CHANGE`。`rainLevel` 穿过 `0.2` 时再发送 `START_RAINING` 或 `STOP_RAINING`，并重发两种强度。完整的 0 到 1 过渡需要 100 ticks，即正常 TPS 下约 5 秒；开始/停止布尔状态约在过渡 1 秒后翻转。

这使同一维度中的两名玩家可以收到不同的天气画面，也使白幕的区块级骤变在视觉强度上具有约 5 秒缓动。代价是过渡期间每位玩家每 tick 最多常规发送两个天气强度包。

`PlayerListMixin` 在 `PlayerList.sendLevelInfo` 中拦截 Vanilla 初始天气片段，改由 `PlayerTemperatureData.sendInitWeather` 发送初值。该方法只有在全局与局部天气相同时才复制世界级强度；玩家首次连接在局部白幕而全局晴朗时，会从默认零值开始由后续 tick 渐入。

如果 `doWeatherCycle=false` 或维度没有 skylight，`ServerLevelMixin_WeatherCycle` 会在调用每玩家更新前返回，客户端最后收到的局部天气状态可能保持不变。

### 3.3 `FHClimatePacket` 不驱动渲染

`FHClimatePacket` 携带玩家当前位置的 `ClimateType`、40 个 `ForecastFrame`、逻辑时钟、风速和湿度，在登录、换维度、重生及气候小时变化时更新 `ClientClimateData`。当前渲染类不读取 `ClientClimateData`；它用于 HUD、预报和其他玩法消费者。

因此玩家移动跨越白幕边界时：

- rain/thunder 视觉状态可在下一个服务端天气 tick 开始变化；
- HUD 中的当前气候和白幕预报通常要等下一次小时包才刷新。

## 4. 客户端现役渲染路径

### 4.1 Vanilla 降水几何与 `LevelRendererMixin`

`LevelRendererMixin` 以 priority `1` 注册到 `LevelRenderer`。它没有取消 `renderSnowAndRain`，而是在方法内部修改四处行为：

| Hook | `weatherRenderChanges=true` 时的行为 | 关闭主开关时 |
|---|---|---|
| `Biome.getPrecipitationAt` redirect | 对所有查询返回 `RAIN` | 使用群系原结果 |
| `LevelRenderer.getLightColor` redirect | 用 Vanilla 雪的启发式提高两个 light 分量 | 原 packed light |
| `BufferBuilder.begin` inject | 把 shader texture 设为 `minecraft:textures/environment/snow.png` | 保留 Vanilla texture |
| render radius constant modify | 见下文；不受该开关控制 | 仍然修改 |

所以默认画面本质上是“Vanilla 雨路径的几何和 UV 动画 + Vanilla 雪纹理 + 增亮光照”，不是自定义雪花网格。强制返回 `RAIN` 也意味着降水片本身不再按每列群系的 `NONE/SNOW/RAIN` 结果过滤；地面粒子、风和雾仍各自检查 `Biome.coldEnoughToSnow`，几条视觉支路的空间条件并不一致。

`snowDensity` 和 `blizzardDensity` 名为 density，实际替换的是 Vanilla 方形降水采样的半径常量。两种图形质量的原始常量都会被替换，因此默认配置下 fast/fancy 不再改变半径：

| 状态 | Radius `r` | 方形列数 `(2r+1)^2` |
|---|---:|---:|
| Vanilla fast | `5` | `121` |
| Vanilla fancy | `10` | `441` |
| FH normal snow default | `10` | `441` |
| FH blizzard default | `15` | `961` |

半径选择只读取客户端 `level.isThundering()`。渲染循环不会逐列查询 `WorldClimate` 或白幕区域；整个相机周围方形区域共享玩家连接当前的 rain/thunder 状态。因此玩家无法在远处看见白幕前沿，也无法在渲染半径内看到一侧晴、一侧暴风雪的空间边界。

### 4.2 `tickRain` 的地面粒子和声音

`LevelRendererMixin.addExtraSnowParticlesAndSounds` 注入 `LevelRenderer.tickRain` 的 HEAD，之后 Vanilla 方法仍会继续执行。

每个客户端 tick 的尝试数为：

```text
particleAttempts = floor(100 * rainStrength^2)
particleAttempts *= 2
DECREASED particle setting: particleAttempts /= 2
```

满强度、fancy graphics 时最多为 200 次，`DECREASED` 为 100 次；fast graphics 会先把 `rainStrength` 减半，因此对应上界约为 50 和 24 次。`MINIMAL` 会在找到第一个合格生成点后退出。这里无 `isThundering` 条件，“乘 2”同时作用于普通雪和暴风雪。每次尝试会随机取相机水平 21x21 blocks 内的位置，并查询 motion-blocking heightmap、群系；满足冷到可下雪和相机高度范围后，还会查询碰撞形状与流体高度，在表面生成 `frostedheart:snow`，热表面则生成 smoke。`SnowParticle` 继承 `WaterDropParticle`，使用 `assets/frostedheart/particles/snow.json` 中的四张 sprite。

声音分为两条：

- `snowSounds=false` 通过持续把 `rainSoundTime` 设为 `-1` 抑制雨雪落地声；开启时本注入会播放低音量的 Vanilla rain/rain-above 声音。
- `windSounds=true` 时，只在相机位置天空光大于 3、客户端正在下雨且群系足够冷时播放 `frostedheart:wind`。普通雪间隔约 120-149 ticks，暴风雪约 60-79 ticks；暴风雪音量乘 2，镜头在流体内时降低 pitch。

粒子与声音逻辑都不读取 `weatherRenderChanges`。主视觉开关关闭后，它们仍由各自配置继续运行。

### 4.3 雾颜色和可见距离

`FogModification` 通过 client-side Forge event subscriber 监听 `ViewportEvent.ComputeFogColor` 和 `ViewportEvent.RenderFog`。它同样不读取 `weatherRenderChanges`。

相机不在流体中，且 `level.isRaining()` 且相机所在群系 `coldEnoughToSnow` 时：

```text
expectedFogDensity = clampMap(skyLight, 0..15, 0..0.5)
if level.isThundering(): expectedFogDensity *= 2
```

此处天空光越强，雾目标越强；室内低天空光会降低雾，而不是增加遮蔽。`prevFogDensity` 使用 `Util.getMillis` 的真实毫秒差插值，增强速率是减弱速率的 4 倍。相机进入任意流体时状态重置；换世界时没有显式重置钩子。

有雾时，颜色在 `fogColorDay=0xbfbfd8` 与 `fogColorNight=0x0c0c19` 之间按太阳角度插值。近远平面使用：

```text
scaledDelta   = 1 - (1 - prevFogDensity)^2
farPlaneScale = lerp(scaledDelta, 1, fogDensity)
nearScale     = lerp(scaledDelta, 1, 0.3 * fogDensity)
```

默认 `fogDensity=0.1`，满暴风雪目标下远平面缩至原值 `0.1`，近平面缩至 `0.03`。处理器随后 cancel `RenderFog`，所以其他雾修改器的组合顺序需要在实际模组环境中验证。

## 5. 未接入或无效的渲染代码

### 5.1 `BlizzardRenderer` 当前不执行

`BlizzardRenderer` 中存在 `render` 和 `renderBlizzard` 两套直接构建 `PARTICLE` quads 的实现，但仓库中唯一调用位于 `LevelRendererMixin.inject$renderWeather` 的整段注释代码中。当前没有构造 `BlizzardRenderer`，`flakeDensity`、`flakeSize` 和预计算数组也没有运行时消费者。

这部分代码不应作为当前性能基线，也不应被误认为白幕渲染。若未来重新接入，需要先决定它是替换还是叠加 Vanilla 路径，并重新核对 shader、buffer、light layer 和 render state 的所有权。

### 5.2 `skyRenderChanges` 当前没有运行时效果

`DimensionSpecialEffectsMixin` 的源码会在 Overworld 冷群系中把 `getSunriseColor` 返回值改为 `null`，但它未列入 `frostedheart.mixins.json` 的 `client` 数组，因此不会加载。客户端配置 `skyRenderChanges` 当前没有生效的消费者。

即使将该 Mixin 注册，其源码也只检查冷群系，不检查 rain/thunder，实际作用会是始终隐藏所有 Overworld 冷群系的日出/日落颜色，而非仅在雪暴时隐藏。

## 6. 配置实际作用域

配置位于客户端 Frosted Heart config 的 `Weather` 分组。

| Anchor | Default | 当前实际作用 |
|---|---:|---|
| `weatherRenderChanges` | `true` | 只控制降水类型强制、光照增亮和雪纹理替换 |
| `snowDensity` | `10` | 普通雪 `renderSnowAndRain` 半径，范围 `1..15` |
| `blizzardDensity` | `15` | thunder/暴风雪半径，范围 `1..15` |
| `snowSounds` | `true` | 雨雪落地声音 |
| `windSounds` | `true` | 冷群系露天风声 |
| `fogDensity` | `0.1` | 满雾时近远平面的目标比例，范围 `0..1` |
| `fogColorDay` | `0xbfbfd8` | 日间雪雾 RGB |
| `fogColorNight` | `0x0c0c19` | 夜间雪雾 RGB |
| `skyRenderChanges` | `true` | 当前无运行时效果，Mixin 未注册 |

这些开关不是一个统一的天气渲染 feature gate。测试“关闭 Frosted Heart 天气渲染”时必须分别核对降水半径、粒子、声音、雾和天空，而不能只切换 `weatherRenderChanges`。

## 7. 优化前应测量的边界

| Path | 扩展维度 | 当前默认暴风雪上界或频率 | 建议观测点 |
|---|---|---|---|
| `renderSnowAndRain` | 客户端每帧、半径平方 | `r=15`, 961 columns/frame | CPU render section、GPU draw、height/biome/light 查询、shader/mod compatibility |
| injected `tickRain` | 客户端每 tick、强度平方 | fancy graphics 下最多 200 random attempts/tick at strength 1 | heightmap、collision shape、粒子创建、Vanilla 后续工作是否重复 |
| fog events | 客户端每帧 | 两个 viewport callbacks | cancel/组合顺序、真实帧时间、可见距离稳定性 |
| per-player weather bridge | 服务端每 tick、在线玩家数 | 过渡时最多 2 个常规强度包/player/tick | `WorldClimate.getClimate`、packet count/bytes、边界来回移动 |
| white curtain cache | 查询区块数、每小时失效 | packed chunk lookup；每 50 秒全清 | cache size/hit rate、add/clear 延迟、跨小时尖峰 |
| `FHClimatePacket` | 每小时、在线玩家数 | 40 forecast frames/player | 与渲染包分开统计，避免把 HUD 同步归入天气几何 |

开始重构前，应至少锁定以下可见行为：普通雪/暴风雪的 5 秒强度过渡、两名玩家处于白幕内外时的独立画面、快/高画质半径、室内外雾、跨流体相机、降水声音以及白幕跨区块移动。当前 `src/test` 没有覆盖 `WhiteCurtainInfo`、天气包插值、`LevelRendererMixin`、`FogModification` 或 `BlizzardRenderer`；现有行为主要依赖集成烟测和性能采样。

## 8. 已确认的架构约束

- 白幕是服务端局部气候模型，不是客户端空间特效；客户端看不到天气墙的位置或方向。
- 所有现役视觉消费者共享 Vanilla rain/thunder 两个标量，但它们的群系、配置和强度条件不统一。
- `snowDensity` / `blizzardDensity` 控制半径而非同面积内的粒子密度，是当前最直接的平方级渲染成本控制点。
- 服务端每玩家发包实现了局部天气，但把视觉平滑、网络频率和 Vanilla 天气状态绑在同一个 `PlayerTemperatureData` 状态机中。
- HUD/预报同步和天气渲染同步是两条独立网络链路，刷新粒度分别为小时级和 tick 级。
- `BlizzardRenderer`、白幕矩形计算以及未注册的天空 Mixin 都不是现役渲染路径，优化工作应先从 Vanilla Mixin、`tickRain`、雾和每玩家天气包开始。
