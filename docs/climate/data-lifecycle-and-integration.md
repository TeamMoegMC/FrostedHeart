# 数据、生命周期与集成边界

- Status: `Current`
- Last verified: `2026-08-25`
- Scope: 温度与天气数据配方、缓存、能力、持久化、tick 阶段、网络、配置、命令、性能入口和测试覆盖
- Primary code anchors: `FHRecipes`, `FHRecipeCachingReloadListener`, `FHCapabilities`, `ClimateCommonEvents`, `FHServerEvents`, `ServerLevelMixin_TemperatureUpdate`, `FHNetwork`, `FHWhiteCurtainSnapshotPacket`, `ClientWeatherState`, `FHConfig.SERVER.CLIMATE`, `FHConfig.SERVER.SIMULATION`, `FHConfig.CLIENT.weatherRenderingMode`, `Phase0aMutationProbe`, `Phase0aMutationWriterCensus`, `FrostedHeartPhase0aGameTests`, `ThermalSignatureResolverDispatcher`, `FrostedHeartPhaseAResolverCensusGameTests`, `ThermalPage`, `ThermalCellArena`, `MaterialBoundaryRegistry`, `ComponentBrickCompiler`, `GeometryDeltaCoalescer`, `FacePatchIterator`, `ImplicitAirAdjacency`, `GeometryMigrationLedger`, `TopologyGuard`, `FarFieldProfileRegistry`, `FarFieldReferenceHarness`, `SolveEpoch`, `ThermalStepExecutor`, `ThermalSweep`, `PhaseTransitionRuntime`, `ThermalSourceTimeline`, `ThermalSourceRegistry`, `NodePowerAccumulatorArena`, `ThermalMemoryBudget`, `QueryPublication`, `DimensionThermalRuntime`, `ThermalRuntimeCoordinator`, `MinecraftThermalInput`, `MinecraftThermalTopologyApplier`, `MinecraftPhaseTransitionHandler`, `MinecraftPhysicalSourceManager`, `MinecraftPhysicalSourceProfile`, `MinecraftThermalSectionAttachment`, `ResolvedGeometryInputRing`, `TownThermalProjection`, `BuildingBlockScanner`, `HouseBlockScanner`, `HuntingBaseBlockScanner`, `FrostedHeartMinecraftThermalInputGameTests`

本文描述温度系统如何接入服务端生命周期。各数值模型和公式分别见 [world-climate-and-temperature.md](world-climate-and-temperature.md)、[player-temperature.md](player-temperature.md) 和 [heat-production-and-network.md](heat-production-and-network.md)。

## 1. 数据入口与权威顺序

运行时行为的权威顺序是：源码与 Mixin、服务端配置、已加载的数据包 recipe、存档能力。`src/generated/resources` 是本项目 data generator 的产物；外部数据包可以按普通 recipe 覆盖或增加同类型条目。

`FHRecipes` 注册以下与温度直接相关的 recipe type/serializer：

| Recipe type | Data class | 主键与主要字段 | 当前消费者 |
|---|---|---|---|
| `frostedheart:world_temp` | `WorldTempData` | `world`, `temperature` | `WorldTemperature` 的维度基温 `D` |
| `frostedheart:biome_temp` | `BiomeTempData` | `biome`, `temperature` | `WorldTemperature` 的群系修正 `B` |
| `frostedheart:block_temp` | `BlockTempData` | `block`, `temperature`, `level_divide`, `must_lit` | `SurroundingTemperatureSimulator` 的方块热/冷贡献与 tooltip |
| `frostedheart:armor_temp` | `ArmorTempData` | `item`, 可选 `slot`, `factor`, `heat_proof`, `wind_proof` | 衣物保温、耐热/抗风属性、衣物槽与 tooltip |
| `frostedheart:food_temp` | `FoodTempData` | `item`, `heat`, `min`, `max` | 食用时直接修改玩家部位体温 |
| `frostedheart:cup_temp` | `CupData` | `item`, `efficiency` | `FoodTempData.getTempAdjustFood` 的容器效率代理 |
| `frostedheart:drink_temp` | `DrinkTempData` | `fluid`, `heat` | `CupTempAdjustProxy` 查询容器内流体温度修正 |
| `frostedheart:plant_temp` | `PlantTempData` | 生长/存活温区、天气脆弱性、死亡方块、`heat_capacity`、光照范围 | 植物生长、施肥、死亡和 tooltip |
| `frostedheart:state_transition` | `StateTransitionData` | 当前物态、三相目标、四个阈值、`heat_capacity`, `will_transit` | admitted Page 的热侧 phase profile 编译；其余方向/状态使用方块随机 tick |
| `frostedheart:generator` | `GeneratorRecipe` | `input`, `result`, `time` | T1/T2 能量塔燃料 process ticks |

`PlantTempData.heat_capacity` 和 `StateTransitionData.heat_capacity` 的数据语义都是随机更新等待因子，旧路径尝试概率近似 `1 / heat_capacity`；它们没有质量或能量单位，也不储存热量。值不大于零时，状态转变路径会直接跳过。新热侧 phase compiler 只用 `38,000 J * heat_capacity` 保留相对快慢，该乘积是明确的玩法换算结果，不改变旧字段的单位。固体同时具有较低 melt 和较高 evaporation 阈值时先执行较低阈值阶段，replacement `BlockState` 再按自己的 recipe 编译下一阶段；阈值相等时保留旧实现 gas 优先级。

内置条目主要位于：

- `src/generated/resources/data/frostedheart/recipes/level_temperature/`
- `src/generated/resources/data/frostedheart/recipes/biome_temperature/`
- `src/generated/resources/data/frostedheart/recipes/block_temperature/`
- `src/generated/resources/data/frostedheart/recipes/armor_insulation/`
- `src/generated/resources/data/frostedheart/recipes/plant_temperature/`
- `src/generated/resources/data/frostedheart/recipes/state_transition/`
- `src/generated/resources/data/frostedheart/recipes/drink_temperature/`
- `src/main/resources/data/frostedheart/recipes/generator/`

目录名只是生成代码的分类；实际解析由 JSON 的 `type` 字段和上表中的注册 ID 决定。

## 2. Recipe 缓存与 `/reload`

`FHRecipeCachingReloadListener.buildRecipeLists` 在服务端资源重载后重建静态索引；客户端收到 recipe 更新时也调用同一方法。索引形态为：

```text
item -> armor data by body part
biome id -> biome temperature
block -> block temperature
BlockState -> state transition
item -> food/cup data
fluid -> drink data
block -> plant data
dimension id -> world temperature
```

`BlockTempData.updateCache` 会同时调用 `CachedBlockTempInfo.clear()`，因此玩家采样所用的全局 `BlockState` 碰撞形状/温度缓存会在 recipe 重载时清空。当前实现不是 generation 标记失效。

`WorldTemperature` 另有 `worldCache` 和 `biomeCache`，分别按运行时 `Level` 和 `Biome` 对象缓存查找结果。虽然类中存在 `WorldTemperature.clear()`，当前 `FHRecipeCachingReloadListener` 没有调用它。因此 `/reload` 可以替换 `WorldTempData.cacheList` 和 `BiomeTempData.cacheList`，但已经访问过的维度或群系可能继续使用旧值，直到显式清缓存或相应对象/进程结束。

## 3. 运行时状态与持久化

| State owner | Registration/attachment | Persistence | Current contents |
|---|---|---|---|
| `WorldClimate` | `FHCapabilities.CLIMATE_DATA`; 挂到所有非 fixed-time `Level` | NBT capability | 逻辑时钟、日缓存、`WhiteCurtainDescriptor` 包装列表、初始化标志；不保存长期事件轨道本身 |
| `PlayerTemperatureData` | `FHCapabilities.PLAYER_TEMP`; 挂到非 FakePlayer | NBT capability | 五部位体温/体感/衣物、核心与环境数据、采样结果、可选难度；另有不写 NBT 的每玩家离散 Vanilla weather compatibility 状态 |
| `ClientWeatherState` | client singleton；登录、维度卸载时 reset | 不持久化 | 当前维度 descriptor snapshot、预计算 kernel、时钟锚点、候选索引和固定容量双天气网格 |
| `ChunkHeatData` | `FHCapabilities.CHUNK_HEAT`; 挂到服务端非空 `LevelChunk` | Codec capability | 复制到该区块的 `IHeatArea` 列表 |
| `HeatEndpoint` | `FHCapabilities.HEAT_EP`; 由具体方块实体或多方块状态暴露 | NBT capability | heat 缓冲、流量、温度等级、优先级和显示统计 |
| item heat | `FHCapabilities.ITEM_HEAT`; 由具体物品暴露 | transient capability | 穿戴/充能设备自己的 heat storage |
| equipment heating | `FHCapabilities.EQUIPMENT_HEATING`; 由具体物品暴露 | transient capability | 对玩家部位 effective temperature 的修改接口 |
| `GeneratorData` | `FHSpecialDataTypes.GENERATOR_DATA` | team `SpecialData` Codec | 每队一个燃料、等级、位置、维度、power 和工作状态 |

玩家死亡 clone 会复制 `PlayerTemperatureData` 后调用温度重置；衣物 inventory 仍保留。区块热区添加、删除和周期校验都会令受影响区块变为未保存，因此热区副本跟随 chunk capability 存档。

## 4. 服务端生命周期与 tick 顺序

```text
ServerAboutToStart
  SurroundingTemperatureSimulator.init()
  TemperatureUpdate.init() -> create TemperatureThreadingPool

ServerStarted
  MinecraftThermalInput.prepareGameplayProfiles()
    resolve every automatically trusted hasDynamicShape=false BlockState once

Server tick START
  TemperatureThreadingPool.tick() -> currently idle for player environment sampling

Level tick START, each server level
  WorldClimate.updateClock()
  town/team updates
  every 20 ticks: climate cache/forecast update and event trimming
    prune naturally completed white curtains and send one replacement snapshot
    sample one local ClimateType/player and update Vanilla compatibility only on change
  distributed loaded-chunk heat-source revalidation (~200 ticks per chunk)

Player tick START
  food temperature polling
  forecast messages
  every temperatureUpdateIntervalTicks:
    start/reuse MinecraftThermalInput for this level
    admit the player's loaded section when absent
    read published thermal air, with legacy value only as bootstrap/fallback
    body-temperature update
  FHBodyDataSyncPacket

Player tick END
  every temperatureUpdateIntervalTicks: effects and direct hot/cold damage

ServerStopped
  TemperatureUpdate.shutdown() -> close pool and simulator caches
```

`ServerLevelMixin_TemperatureUpdate` 注入 `ServerLevel.tickChunk`，在原版 `iceandsnow` 前执行温度冻结/降雪，然后手工重放原版 `tickBlocks` 并取消原方法。它对每个可随机 tick 的区段按 `pRandomTickSpeed` 取样；有 `StateTransitionData` 或 `PlantTempData` 时先运行温度规则，否则执行原方块/流体 random tick。这是温度系统与 Minecraft tick 实现耦合最深的入口，升级版本或重构时必须验证原版 tick 语义没有漂移。

## 5. 网络同步

| Packet registration | Direction and trigger | Payload/purpose |
|---|---|---|
| `body_data` / `FHBodyDataSyncPacket` | S2C；当前每个玩家 START tick | 玩家核心前值/当前值、环境温度和总体验温 |
| `climate_data` / `FHClimatePacket` | S2C；登录、换维度、气候小时变化、昼夜时钟大跳及部分事件 | 当前天气/预报、`sec` 和同源 `clockDayTime` |
| `white_curtain_snapshot` / `FHWhiteCurtainSnapshotPacket` | S2C；登录、换维度、所有重生（含末地通关）、创建、清除、自然结束 | 维度、`sec`、`clockDayTime` 和完整稀疏 descriptor 列表；原子替换客户端空间天气状态 |
| Vanilla `ClientboundGameEventPacket` weather events | S2C；初始化；之后每玩家最多每秒采样且只在离散状态变化时发送 | 把当前位置的全局/白幕 `ClimateType` 转成连接独有的 clear/snow/blizzard compatibility 状态 |
| `temperature_display` / `FHTemperatureDisplayPacket` | S2C；工具或提示调用 | 格式化的温度数值显示 |
| `soil_thermometer_request/update` | C2S/S2C；温度镜查看方块 | 服务端 `WorldTemperature.block` 查询结果 |
| `infrared_view_c2s/s2c` | C2S/S2C；红外视图按区块请求 | 指定区块范围内的热区形状 |
| `notify_chunk_heat_update` | S2C；热区新增、删除或校验变化 | 令客户端丢弃并重新请求对应区块红外数据 |
| `heat_endpoint` and heat-network request/response | 双向按 GUI/调试需求 | 端点统计与热网拓扑显示；不是世界温度同步 |

世界/方块/空气温度不做连续状态广播；需要显示时由服务端查询或由客户端结合已同步数据渲染。`FHBodyDataSyncPacket` 的发送频率高于默认主体计算频率，见玩家文档的同步约束。

V1 白幕稳定移动不发送 snapshot 或逐帧强度。`FHClimatePacket` 现在同时携带玩家局部合并 `climate`、独立 `globalClimate`、`sec` 和 `clockDayTime`；客户端用全局类型填充天气底色，并按同源 `dayTime` 推进。小时变化照常发送；睡眠或 `/time` 大跳在下一次一秒气候调度复用一次同包，不新增长期频率。`FHWhiteCurtainSnapshotPacket` 只在 descriptor 集合或玩家维度生命周期变化时发送。完整 ownership 和 fallback 链路见 [weather-rendering.md](weather-rendering.md)。

## 6. 高影响配置及实际语义

以下默认值来自 `FHConfig` 或 `TownModelParameters.Defaults`。这里只列影响现有兼容输出或性能的入口；长期事件的完整默认值见世界气候文档。

| Java config anchor | Default | Actual consumer/meaning |
|---|---:|---|
| `temperatureUpdateIntervalTicks` | `20` ticks | 玩家换热和伤害/效果检查周期；公式没有按周期归一化 |
| `envTempUpdateIntervalTicks` | intended `20` ticks | 成功提交周围环境采样后的最短间隔 |
| `envTempThreadCount` | intended `min(2, processors/2)` | `0` 同步执行；正数创建 daemon worker pool |
| `heatExchangeTimeConstant` | `1000` | 每轮换热的 `unit=1/value`；`0` 被配置允许但会导致除零 |
| `heatExchangeTempConstant` | `10` | 环境温差除数；`0` 被配置允许但会导致除零 |
| `temperatureChangeRate` | `1` | 当前只乘食物直接体温修改及其 tooltip，不控制环境换热 |
| `tempBlockstateUpdateIntervalTicks` | `20` ticks | 每区块地表冻结/降雪检查的错峰周期 |
| `tempRandomTickSpeedDivisor` | `1` | 当前仅出现在已注释代码中，不影响执行 |
| `ambientRandomTickSpeedDivisor` | `10` | 无活动热区时，固体/液体状态转变的附加抽样除数 |
| `snowTempModifier`, `blizzardTempModifier` | `-5`, `-10` | 玩家环境公式执行减法，所以默认实际升温 `5`/`10degC` |
| `onFireTempModifier` | `150` | 已定义，但玩家着火分支当前硬覆盖为 `300degC`，不读取此值 |
| `stoneInterfaceLevel`, `seaLevel` | `0`, `63` | 只用于方块温度气候权重；空气温度使用硬编码 `0/63` |
| `blockMaximumClimateAffection` | `0.5` | 方块温度的最大气候系数 |
| `blockHeatApplicationMultiplier` | `2` | 方块热区升温倍率，随后仍受热区值上限约束 |
| `absoluteZeroCelsius` | `-273` | 世界空气/方块温度下限 |
| `overworldBaselineCelsius` | `-10` | 缺少 `world_temp` 条目时所有维度共用的 fallback |
| `simulationRange` | `8` blocks | 玩家周围采样范围；在 simulator 类加载时读入 static final |
| `simulationDivision` | `10` | 发射网格分度，粒子数按立方增长；在类加载时固定 |
| `simulationParticleInitialSpeed` | `0.4` | 采样射线初速度；在类加载时固定 |
| `weatherRenderChanges` | `true` | 客户端天气渲染总开关；false 使用 compatibility |
| `weatherRenderingMode` | `SPATIAL_V1_FAST` | 玩家固定选择 `COMPATIBILITY`、`SPATIAL_V1_FAST` 或 `SPATIAL_V1_FANCY`；不按 FPS 自动改变 |

当前 `envTempUpdateIntervalTicks` 和 `envTempThreadCount` 都通过 `defineInRange("environmentTempMinTicks", ...)` 注册到同一个 Forge 配置键。这不是两个可独立配置的键：spec 构建、默认值、范围或读取结果可能互相冲突。任何性能测试都应先记录运行时这两个 Java 字段的实际值，不能仅依赖配置注释。

## 7. 管理员与诊断入口

命令均要求权限等级 `2`，并同时注册顶级简写及模组前缀形式。主要语法为：

```text
/climate get
/climate init
/climate rebuild              # resetTempEvent
/climate rebuild cache
/climate resetVanilla
/climate append <track> [warm|cold|blizzard]
/climate white_curtain clear
/climate white_curtain add [pos]

/temperature get
/temperature set difficulty <easy|normal|hard|hardcore>
/temperature set bodyTemp <value>   # 相对 37degC 的偏移
/temperature set envTemp <value>    # PlayerTemperatureData 的绝对显示值
/temperature set feelTemp <value>   # 绝对体感值

/heat_adjust set <position>                         # 当前等同 remove
/heat_adjust set <position> <range> <temperature>  # cubic
/heat_adjust set <position> <range> <temperature> sphere
/heat_adjust set <position> <range> <temperature> <top> <bottom>  # pillar
/heat_adjust get [position]
/heat_adjust remove <position>
```

`/climate rebuild cache` 重建的是 `WorldClimate` 小时/日缓存，不会调用 `WorldTemperature.clear()`，也不会重建数据包 recipe 索引。`/reload` 才触发 `FHRecipeCachingReloadListener`，但仍有前述世界/群系二级缓存边界。

## 8. 当前性能边界

| Hot path | Current mitigation | Remaining boundary |
|---|---|---|
| 玩家周围方块采样 | 可配置 worker pool；复制采样输入；`BlockState -> CachedBlockTempInfo` 全局并发缓存 | 粒子数随 `simulationDivision^3` 增长；碰撞形状遍历仍是主要成本；动态方块实体温度无法进入全局 state 缓存 |
| 世界方块 random tick | 先用 `StateTransitionData`/`PlantTempData` 静态缓存 gate；复用 chunk heat 和 climate base；按区块错峰地表检查 | Mixin 手工复制原版 `tickBlocks`；每个随机 tick 样本仍会做两次数据 gate |
| 世界温度查询 | 维度、群系、recipe 和热区查询均有快速重载/缓存；`WorldClimate.getTemp(BlockPos)` 的白幕 cache hit 直接使用 packed chunk key，不再先分配 `ChunkPos` | `WorldTemperature.air` 每次仍取气候、热区并生成高斯噪声；缓存失效与 `/reload` 不完整 |
| 热区维护 | 热源复制到覆盖区块，查询只扫描当前位置区块；约每 200 ticks 分散校验；Java 17 本机 JMH 中 `ChunkHeatData.queryAdjust` 的 1/10/100 热区命中约为 `3.18/21.13/198.13 ns/op`，未命中约为 `2.68/13.95/135.61 ns/op`，当前消费方式为 `0 B/op` | 大半径热源会加载并写入全部覆盖区块；查询仍线性扫描；无空间索引或距离衰减；变更会发红外失效包；微基线不能替代真实每 tick 调用量和多人服务端 profile |
| 玩家同步 | packet 只含四个聚合字段 | 当前每玩家每 tick 发送，即使默认每 20 ticks 才更新主体值 |
| 局部天气同步 | descriptor 集合变化时发完整 snapshot；稳定移动零专用包；Vanilla bridge 的调度、capability 解析和采样均为最多 `1/player/second`，状态不变零包 | snapshot 编码与 multiplayer packet bytes 已有单测/counter，真实多人 packet profile 尚未完成 |
| V1 客户端天气状态 | 每 tick 一次 descriptor 距离/相位预筛；只对近场 candidates 填固定 `81/289` cell 双网格；声音和 tick ownership 复用 camera sample | 真实 client-tick P95、稳态 allocation 和 retained memory 尚未完成 JFR 验证 |
| V1 客户端渲染 | Fast/Fancy 固定 wall `64/256` quads、snow `256/1024` columns、terrain `12/32` queries 上限；当前相机 frame ownership 与 tick ownership 分离 | `Tesselator.end()` 仍分配 batch 包装；frustum/屏幕段裁剪、持久 VBO、render-thread/GPU P95、4K、高刷新率和 Embeddium/Oculus 仍是 release gate |
| 热网 | 网络重建延迟合并；端点按优先队列分配 | DFS 拓扑与每 tick 分配独立于世界热区；无统一 profiler/benchmark 覆盖 |

后续性能重构应分别测量这些路径，不宜用单个“温度 tick”指标掩盖玩家采样、区块 random tick、热区维护和热网分配的不同扩展维度。

`src/jmh/java/com/teammoeg/frostedheart/content/climate/thermal/benchmark` 和 Gradle 任务 `thermalLegacyJmh`、`thermalLegacyJfr`、`thermalLegacyRetainedHeap`、`thermalBenchmarkEnvironmentManifest` 提供可重复的 Phase 0b 本机微基线。`thermalLegacyBaseline` 串联这些任务；原始 JSON、文本和 JFR 写入 `build/reports/thermal-phase0b/`。2026-08-24 的隔离对象图估算中，含 0/1/10/100 个热区的 `ChunkHeatData` 分别保留约 `72/240/1032/9912 B`。这些数字只描述当前 legacy 查询 fixture；reference exchange 和 synthetic Brick compiler 只校准 harness，不是 V1 runtime 或四候选的性能证据。

Gradle 任务 `thermalPhaseAEvidence` 生成 Phase A resolver/Brick 的 JMH average + sample p95/p99、GC allocation、JFR、active-registry census 和 JOL retained-size 报告，位置为 `build/reports/thermal-phase-a/`。2026-08-24 本机结果中，generic air/fence resolver 的 sample p95/p99 为 `1.5/1.9 us` 与 `1.6/2.0 us`，explicit/contextual fixture 均为 `100/100 ns`；Brick compile 的 sample p99 为 `20.00..28.48 us`。当前 correctness registry 单代约 `41,000 B`，reload 双代 overlap 约 `79,936 B`，三个 synthetic Brick object graph 为 `1,960..3,816 B`。这些路径尚未进入 gameplay tick；allocation 和 retained 数字描述 object-heavy correctness prototype，不能外推为生产服务器每 tick CPU、整服内存或最终 packed arena。

## 9. 现有自动化验证

新热学架构目前包含 gated Phase 0 probe、Phase A geometry/profile、Phase B/C Page 与 regular/mixed Air Mesh、Phase D arena-native solver/source、Phase E guard、Phase F Minecraft topology、Phase G Campfire/Generator sources、Phase H/I/J 可选材料/相变/辐射，以及 Phase K/L consumers/diagnostics。玩家空气/直接 source 辐射、publication 完整命中的作物和住宅/狩猎建筑，以及 admitted Page 内可编译的热侧物态转换已进入存档内实机测试权威；普通机器没有现存温度 consumer，非相变材料 profile 仍保持 dormant。`PhaseZeroThermalRouting` 与 `ThermalBenchmarkEvidence` 只保留在 test source set，不能作为 production route 或 benchmark report 的代码锚点。

`LevelChunkSectionMixin_Phase0aMutationProbe` 始终应用，但 `frostedheart.phase0aMutationProbe` 仍只控制原 Phase 0a evidence 记录。首次玩家温度查询为该 level 创建 `MinecraftThermalInput`；只自动信任 `hasDynamicShape=false` 的 state-static resolver 输出，并在玩家 section 缺失时对已加载 chunk capture 一个 Page。辐射遮挡不走热签名解析：DDA 直接读取已加载 section 的 `BlockState`，动态形状按空气，静态状态只按 `BlockState.canOcclude()` 判断透明/整块不透明；不执行逐模组检查或部分形状栅格化。section mutation 随后经 owner 进入增量 invalidation。chunk unload 会撤销该 chunk 的 Page，服务端停止时 `closeAll` 释放 runtime/coordinator。

默认 `sealTick` 仍只封存 frame；显式 `enableShadowDispatch` 的 API 仍接受调用者 executor。当前玩家测试接线使用 `Runnable::run`，因此 tick 末在服务端主线程顺序完成 topology apply、coordinator request、solve 与 publication，避免 Page admission 和 arena writer 并发；这是便于实机验证的正确性接线，不是最终异步性能方案。

玩家测试 runtime 会启用 `MinecraftPhysicalSourceManager` 和 `RadiationService`。physical manager 只刷新发生变化的 Campfire/Generator source，并保留固定 cold Page 上限；blocked/unloaded/unresolved port 仍走既有显式 sink，不产生第二份 `H/C`。radiation 的 `128 sources / 1,024 sections / 128 receivers / top-8 / 24 rays` 上限一次性保留约 `729,408 B` optional memory per dimension。没有 approved FarField profile 的开放边界仍 unresolved，因此这次接线只能验证真实几何、局部传播、source 和 HUD/体温消费链，不能代表完整室外气候模型。

Phase H 通过 `MinecraftThermalInput.enableTopologyApplication(parameters, materialBoundaries)` 显式安装 immutable `MaterialBoundaryRegistry`；单参数 overload 保持空 registry。signature 中的 `materialProfileId` 与 `materialContactPatternId` 在 worker 上只解析为已封存的 `4x4x4` material mask 和 SI 参数，不读取 `Level`。完整单格 barrier 可生成 `STATELESS_CONDUCTANCE` air-air pair；两格及以上固体不会压成一条 `G`。`CAPACITIVE_SURFACE` 与 `NATURAL_ROCK` 只为已确认接触空气的 material interface 创建 sparse surface/deep pole，这些 pole 和空气 cell 共用 Page-owned `ThermalCellArena` span，因此 `H/C` 仍只有一个权威，重建按 material position/face/plane/depth key 迁移，Page retirement 整段释放。自然边界只接 deep pole（无 deep capacity 时接 surface pole），不按低 Y 直接给空气加热。相邻 section 的 admission、mutation、resync 或 retirement 只使直接邻居 Page dirty；稳定 tick 不扫描材料。缺失 profile/contact 或 mask 与 proven air 重叠会令 shadow topology unresolved。`PHASE_RESERVOIR` 不属于 Phase H。

Phase I 在同一 material registry 中增加 `PHASE_RESERVOIR` profile，每个有状态 reservoir 永远限制在一个 world-aligned `4x4x4` Brick 和一个 profile。gameplay 启动在 `StateTransitionData` cache 建立后枚举自动信任的静态状态，用旧热侧阈值和 `heat_capacity` 倍率生成共享 profile，并直接从同一个 state-static 保守几何的非空气 microcell 生成 contact pattern；没有 material mask 的碰撞空形状和动态形状不接管。只有与 proven air microcell 精确共面的候选才进入 `long candidateMask`，不会建立全局 patch、逐方块 timer 或逐方块 thermal node。

潜热 `H`、reserved energy、request sequence/state 与候选 mask 都保存在 Page-owned `ThermalCellArena` span，普通 Air adjacency 显式跳过 reservoir。`ThermalSweep` 将空气显热保守转入 reservoir；达到一个可见单位后，每个 reservoir 至多发出一个 mutation request。主线程在 tick sealing 前有界处理 request，并再次验证 Page generation、loaded chunk、当前 profile、recipe 和精确 `4x4x4` 接触界面，再应用旧 recipe 的目标 `BlockState`。若当前方块属于 `BlockTags.ICE` 且群系命中 `FHTags.Biomes.ICE_DO_NOT_SMELT`，request 暂缓且不扣潜热，保持旧群系规则。request ring 满时由 reservoir sticky retry，ACK ring 满时由固定表保留 outcome；拓扑重建按 Brick/profile key 迁移潜热和 outstanding request。只有当前 Page 的 applied reservoir 确认拥有精确候选时，旧 random-tick Mixin 才跳过热侧分支；冷侧和所有 fallback 仍由旧逻辑负责。`/reload` 在 cache 更新后关闭旧 runtime/profile cut，下一次查询懒重建。

Phase J 通过 `MinecraftThermalInput.tryEnableRadiation` 创建 main-thread `RadiationService`，可选内存被 `ThermalMemoryBudget` 一次性准入。Campfire/Generator 沿用 Phase G source ID、lifecycle generation、启停和总功率，并从 profile 的 `RADIATION` port share 得到单一辐射原点；source 只索引在原点 section。玩家采样只查固定半径 section bucket，经 range/flux cutoff 后稳定选择 top-K source，再向 feet/torso/head 发出有 candidate/ray/DDA hard cap 的 loaded-only quarter-block DDA。active receiver witness 保存经过 section 的独立 revision；block/raw replacement、section identity、chunk load/unload 会使 revision 失配并触发 bounded retrace。未加载/未解析空间保守返回 unresolved，预算耗尽返回 `RADIATION_BUDGET_LIMITED` 和较低 confidence。采样不调用 `ThermalSourceTimeline`，不扣 source accumulator，也不重复注入 radiation energy；现有 radiation port 仍在 source ledger 中只结算一次 declared ambient loss。玩家 wrapper 现把结果按有效投影面积、吸收率、更新秒数和有效热容换成身体温升；材料 radiation 仍未实现。

Phase K 当前接通 player、crop 与 town gameplay authority，并保留 registered-machine observation 接口。`MinecraftThermalInput.samplePlayerEnvironment`、`sampleMachineEnvironment`、`sampleCropEnvironment` 和 town group lookup 复用同一个私有 published-air lookup：它只查 `pages` 中已有的 Page，并将规则 coverage 或 mixed Brick 精确 microcell component 解析为 arena slot；`DimensionThermalRuntime.tryReadPublishedCell` 在 logical writer 空闲时核对当前 dimension/geometry/topology envelope 后读取 `QueryPublication`。读取完成后再次核对 Page publication，阻止 mutation 与 query 的竞争窗口返回旧 geometry。超过 caller age 上限、无 Page、无空气、stale geometry 和 publication miss 都显式降级；Air Mesh 分支不会 admission、加载 chunk 或读取世界。player wrapper 可追加已启用的 Phase J bounded radiation，machine/crop/town wrapper 不自动执行 radiation、source、heat-network 或 machine-capacity 逻辑。

`TemperatureUpdate` 先计算一次旧环境值作为首次 Page 初温和 publication miss 兜底，再由 `gameplayPlayerEnvironment` 选择新 publication；旧周边方块线程调度暂时注释。工程中目前没有读取 legacy 环境温度的普通机器，因此没有虚构 gameplay hook；普通机器仍保持 `NONE`，Campfire/Generator 属于 `POWER_SOURCE`。

`WorldTemperature.checkPlantStatus` 在真正需要温度时调用 `gameplayCropEnvironment`；预计算温度的随机 tick、生长事件和树苗事件都进入同一边界，天气提前返回的分支不会虚增调用。publication 命中时 new air 直接驱动作物阈值，miss 时原 block temperature 回退。`CropShadowSnapshot` 仍分开记录 legacy block/new air 差异；同 tick 不增加缓存对象或 retained crop state，调用量继续显式计数。

town 不增加第二次建筑扫描。现有 `BuildingBlockScanner` 访问空气时把内部体素按 world-aligned `4×4×4` base Brick 压成 `TownThermalProjection`：每组只有一个确定的真实空气代表点与整数权重，不保留体素坐标列表。住宅和狩猎扫描成功后调用 `gameplayTownEnvironment`，稳定 refresh 每组只读一次已有 publication；完整覆盖时 new air 加权平均直接成为建筑温度，部分或全部 miss 时整体使用 legacy 全体素平均，并以 `QUERY_PARTIAL_REGION` 和 voxel coverage confidence 报告。miss 不创建 Page/Brick/Cell/Interest 或 mesh lease。矿井基地主路径不计算 gameplay 温度，停用的 `MineBlockScanner` 未重新接活。HUD 位于 player consumer 下游并读取已同步的 `PlayerTemperatureData`，不是需要独立接线的第五种 query。

Phase L 首批观测由 `MinecraftThermalInput.shadowRuntimeSnapshot` 按需组合，不建立并行 metrics runtime。它包含 player/machine/crop/town 的累计 shadow error、published-air lookup hit/miss 与具体 fallback flags、publication age、Page/mixed Brick/source/radiation 数量、main-thread seal 与 worker frame 纳秒累计、coordinator mailbox、最后一次 topology/request/solve report，以及 `DimensionThermalRuntime.Diagnostics`。diagnostics 在 logical writer 空闲时读取 arena、sweep 与 publication footprint；writer 正在修改状态时立即返回 `writerBusy=true`，并以 `-1` 表示不可安全读取的 arena/sweep 瞬时计数，不等待 worker。这里的纳秒累计是 production shadow capture 的原始观测量，不替代 JFR/JMH、retained-size 或完整 workload gate。

Phase L synthetic query diagnostics 由 `thermalPhaseLQueryDiagnostics` 统一生成 `build/reports/thermal-phase-l/` 下的 JMH JSON/text、100-receiver JFR、JOL retained-size 和环境/限制 manifest。`ThermalShadowQueryFixtures` 直接组合 production `ThermalPage`、`ThermalCellArena`、`DimensionThermalRuntime` 与 `QueryPublication`，分别测一个共享 Page 和每个 receiver 分散到不同 Page 的 `1/10/50/100` 顺序查询批次。诊断首轮发现 `Map<Long, ThermalPage>` 的 section-key 装箱在分散布局中约分配 `24 B/query`；production `pages` 索引改为 fastutil `Long2ObjectOpenHashMap` 后，最终 JMH 的两种布局都约为 `0.01 B/query` 测量噪声。最终 100-query 批次均值约 `4.9 us`，p95 为 `5.0 us`，p99 为 `12.69 us`（共享）/`13.30 us`（分散）。JOL isolated graph 为共享 100 receivers `11,264 B`，分散 100 Pages/Cells `313,472 B`。这些数字不包含真实 `ServerPlayer`、`ServerLevel`、radiation receiver cache、topology rebuild、worker solve、TPS、chunk lifecycle 或生产模组集合，因此只能证明 hot query 和 isolated retained slope，不能批准 Phase L 或切换 gameplay authority。

一次外部附着的 Java 17 Forge GameTestServer JFR 进一步确认这些 dormant 路径能在真实 Forge JVM 生命周期中通过 `19/19` required GameTests。该 `55 s` 录制包含模组加载、世界启动和关服：`5,300` 个 execution samples 中 `97` 个包含 thermal package（`1.83%` sample share，主要来自 Phase 0 mutation probe、resolver census、几何解析和 topology rebuild），只有一个执行栈经过 Phase L `samplePublishedAir`；allocation sampling 没有命中该 query，thermal allocation weight 则主要来自 GameTest 触发的几何与探针 fixture。录制中的 `93` 个 GC pause event 总计约 `851.5 ms`，最大 `25.456 ms`；`13` 个 contended monitor-enter event 均不含 thermal frame。这是集成诊断，不是稳态玩家负载，不能用其中的周期 tick、CPU 或采样分配权重宣称多人 TPS、精确 allocation rate 或 Phase L acceptance。

当前与温度核心模型直接相关的自动化测试为：

| Test | Locked behavior |
|---|---|
| `BlockTemperatureModelTest` | 方块气候系数、热区上限/倍率、绝对零度下限 |
| `ClimateEventModelTest` | 气候事件生成和轨道聚合模型 |
| `SphericalHeatFieldModelTest` | 球形热区覆盖的区块边界与几何 |
| `GeneratorHeatFieldModelTest` | 能量塔等级到半径/温度映射 |
| `GeneratorFuelModelTest` | 燃料 process ticks 与批量推进 |
| `SurroundingTemperatureSimulatorCacheTest` | 空碰撞热源的缓存/采样规则 |
| 白幕 V1 定向测试（`34` 条 JUnit） | 旧存档 Codec、四方向玩法/连续视觉场、含末端边界、`5s` 相位平滑、精确 cache 失效、snapshot、dayTime freeze/jump/correction、Compatibility 长时钟、候选排序/双网格/专用 sampler、室内/前沿外 tick/frame ownership、墙段距离、固定 caps 和 Vanilla compatibility 映射 |
| `thermal.phase0.reference.*`（`29` 条 JUnit） | 秒/tick 单位、`H/C/P/G`、source 精确积分、解析交换、LOD/geometry 能量账本（含空气 cell 全部消失/创建）、workload 分类、legacy/shadow routing，以及 benchmark evidence provenance 合同 |
| `thermal.phase0.mutation.*`（`16` 条 JUnit） | Vanilla/Forge/Create/Frosted Heart 常见 writer census、capture route、同 tick 归一化、重复/断裂检测、mapped/unmapped 分流、显式 resync adapter 和延期项不参与 gate |
| Phase A geometry/profile fixtures（`45` 条 JUnit） | conservative `4x4` face raster、bounded air components、无 false opening property、`4^3` Brick compiler、27-bit dependency mask、snapshot sentinel/audit、`int` signature registry、真实 `VoxelShape` adapter、generic state-static resolver、deterministic explicit/contextual dispatcher 和 conservative fluid fallback |
| `GeometrySummaryCacheTest` / `thermal.mesh.*` | 固定 `73` summaries、`int coverageRef[64]`、粗 coverage 即时失效、同 tick 同 Brick 合并、ring overflow sticky resync/新旧 ACK、dense cell span、完整 coverage 分区校验、signed geometry ledger，以及 `16↔16`、`16↔4`、`8↔4` 三轴 canonical face ownership |
| Phase C `ThermalCellArenaTest` / `ImplicitAirAdjacencyTest` / published coverage | primitive regular/mixed `H/C` state、compiled component volume/centroid/face port、dense Page span 双持 commit/rollback、pure-LOD split/merge `sum(H)`、O(1) caller-owned coverage result、regular/mixed 与 mixed/mixed aperture pair、stale frontier 和 closed-system conservation |
| `thermal.solver.*` / `thermal.source.*` | `dtSeconds`/`-expm1` pair 与 boundary kernel、buoyancy、forward/reverse sweep、sealed epoch/no backlog、bounded `TIME_DEGRADED`、source `integral(P dt)`、same-tick impulse/rebind、retained replay/checksum、queue-full retry、source + sweep 共享同一个 arena，以及 stale source/sweep generation 在 mutation 前被拒绝 |
| `TopologyGuardTest` / `FarFieldReferenceHarnessTest` | `MATERIAL/OPEN_AMBIENT/OPEN_CONTINUATION/UNRESOLVED` containment、未批准/超域 profile 不生成 transport、approved impedance 直接生成 arena-bound boundary、独立 fit/holdout、open/half-open/cavern/tunnel、calm/windy 与 `1/10/100 kW` synthetic reference matrix |
| PR7 runtime/query foundation + Phase K/L envelope（`22` 条 JUnit） | server/dimension memory admission、critical reserve、resize peak ownership、preallocated seqlock double buffer、ABA/revision/generation fallback、single writer/latest-only epoch、explicit non-source ACK、whole-set sleep/wake、hard work caps、fixed ready queue、sticky re-offer、recovery quota、fairness、unload replacement generation、current topology advance 后拒绝旧 publication，以及 writer-owned arena 的非阻塞诊断边界 |
| PR8 Minecraft input/topology | fixed common-event envelope、真实 section Mixin、显式 interest/admission、dependency-mask invalidation、bounded full-Page signature cut、regular/mixed/no-air rebuild、sparse `H` migration、replacement sweep + non-source ACK、post-sweep Page release、source span retention gate，以及 latest-only shadow dispatch |
| Phase G physical sources（`2` 条 Forge GameTest） | Campfire `1 kW` 与 Generator `10 kW * TLevel`、固定 port shares、exact face-component binding、blocked/degraded sinks、cold Page cap、dirty-only producer、unload/revival、topology-cut preapply 和不双注能 recovery |
| Phase H `MinecraftMaterialBoundaryTest`（`3` 条 JUnit） | 单格 stateless wall、两格厚墙拒绝 shortcut、capacitive residual heat、surface key 重建迁移、Page unload、exposed-only natural rock surface/deep pole 与 geothermal boundary |
| Phase I `PhaseTransitionRuntimeTest` + `MinecraftMaterialBoundaryTest`（`7` 条 JUnit） | Brick/profile 聚合、精确 microcell interface、潜热守恒、弱输入阈值、多 contact 合并、单 request、request/ACK overflow sticky retry、generation-safe ACK、重建迁移和 gamerule policy |
| Phase J `RadiationServiceTest`（`5` 条 JUnit）+ Minecraft radiation GameTest | inverse-square flux、deterministic top-K、candidate/ray/source/memory caps、signed section packing、revision retrace、witness cache hit、真实石墙阻挡/拆除恢复，以及重复观察不写 source ledger |
| Phase K player + crop + town gameplay query and registered-machine observation | runtime envelope advance 拒绝旧 publication、mixed Brick point-component 命中/solid 拒绝、publication age fallback、passive Page miss 不 admission、真实 `PlantStatus` 使用 published air、town 完整 weighted projection 接管/partial 整体回退，以及 10,000 crop miss/4,096 town group miss 不增长 Page/cell/arena energy |
| `FrostedHeartPhase0aGameTests`（`7` 条 Forge GameTest） | 低层 section mutation、流体与门类、递归写入、活塞、generation/publication、raw palette 检测、raw block/biome 与 whole-section replacement adapter、真实 ticket load/unload/reload，以及真实 Create assemble/move/disassemble 世界写入；旧 dynamic exclusion fixture 只保留为注释 |
| Phase A Forge GameTest（`5` 条） | air/solid/slab/stairs/Door/Trapdoor/fence/pane/snow/waterlogged fixture、bounded/missing/unloaded snapshot、远端 no-load、piston dynamic exclusion，以及启用 registry 的 full-dispatch/two-generation reload census |

完整 thermal JUnit 当前为 `238/238`，Forge GameTest 为 `19/19` required；同轮完整仓库 JUnit 执行 `812` 条，其中 `811` 条通过，唯一失败是 `TeamTownActualSaveCodecProbeTest.actualSaveSurvivesTheFullSyncCodec` 缺少外部存档 fixture，与 thermal 改动无关。thermal GameTest 路径包括 Phase 0a `7` 条、Phase 0b `1` 条、Phase A `5` 条、PR8 input `2` 条、Phase G physical source `2` 条和 Phase J radiation `1` 条。Phase K 的 player/machine/crop/town query 复用现有 Minecraft input GameTest，并覆盖 mixed Brick、solid microcell、未 admission Page、publication age、批量 machine miss、真实 plant-status observation、town weighted-group compression、10,000 crop miss、4,096 town group miss 与只读 runtime state；Phase L 同一场景继续锁定 consumer 计数、footprint、fallback、timing、mailbox 和 solve report 聚合，`DimensionThermalRuntimeTest` 锁定 logical writer 活跃时不读取可变 arena/sweep 状态。`TownThermalProjectionTest` 另锁定负坐标 Brick 分组和与遍历顺序无关的代表点。Phase A census 枚举 `84,147` 个启用 BlockState：`82,197` 个走 generic static、`1` 个走 explicit、`12` 个走 contextual，合计 `82,210` resolved；`1,925` 个未注册 dynamic state 为 `NOT_REGISTERED`，另有 `12` 个 moving-piston state unresolved。共有 `262` 个完整 signatures、`259` 个唯一 geometry patterns、`2` 个 contextual outputs，最大观测 local-region count 为 `4`。同输入旧/新 registry 同时保留时 signature ID 确定，JOL 记录约 `79,936 B` overlap；production datapack listener 尚未接线，`Rmax` 和 packed width 仍未冻结。真实 Create bearing 测试冻结的语义是：assemble 捕获 `stone -> air`；contraption 移动期间按空气处理且不产生热几何 delta；disassemble 在目标位置捕获 `air -> stone`。因此当前方案没有 Create movement adapter、Mixin 或 AABB exclusion。真实 chunk lifecycle 测试通过 region ticket 驱动 load -> unload -> reload，并验证旧 section identity、generation 和 publication 被拒绝。

Phase B 的 `ThermalPage` 与一个 `16^3` section 对齐，保留固定 `int coverageRef[64]`、4/8/16 coverage width、`64 + 8 + 1` primitive geometry summaries、`mixedBrickMask`/`dirtyBrickMask`、dense arena spans，以及分离的 live/published `long` geometry revision。一次 active mutation 先递增 live revision、令旧 publication 失效并 O(1) 标记 base Brick；同 tick 同 Brick 的 voxel mask 只向 bounded primitive ring 发送一条 delta。ring 无容量时，Page-owned `FULL_GEOMETRY_RESYNC_REQUIRED` 不依赖 ring entry 存活；ACK 必须匹配 section key、lifecycle generation、最新 required revision 和原 sticky reason。粗 support mutation 会阻止 publication，直到完整且世界对齐的 4/8/16 coverage partition 被安装。

`FacePatchIterator` 由世界负轴侧唯一拥有 patch，三轴统一使用 `axis + packed signed world coordinate` key，并对当前 overlap 给出精确整数面积和 `halfWidthA + halfWidthB` 中心距离。`GeometryMigrationLedger` 是 pure-LOD/overlap 公式的 production owner；`ThermalMigrationReference` 只从 Phase 0 test/reference 方向调用它。ledger 显式累计 signed `GEOMETRY_INGRESS_J`、`GEOMETRY_EGRESS_J` 和 residual；旧或新空气 cell 数为零也有定义。

Phase C/H/I 的 `ThermalCellArena` 使用 primitive SoA 保存规则 4/8/16 AirCell、compiled mixed-Brick component、sparse material surface/deep pole 与 Brick-local latent reservoir 的 Page owner、lifecycle generation、world support 和物理状态；空气与 material pole 温度由 `Tref + H/C` 派生，phase reservoir 只保存潜热账户，不是第二个温度权威。mixed component 的容量来自 `effectiveVolumetricCapacity * componentVolume`，中心来自 `CompiledBrick` centroid，并复用其 face-port table。Page LOD split/merge 会先建立完整的新 dense span，旧、新 span 在 Page 安装新 coverage 前同时有效；安装成功后 commit 才释放旧 span，失败可 rollback。当前 correctness layout 中 support 使用 arena `int` slot，这不是最终 packing 或 size-class 承诺。

`ThermalPage.tryQueryPublishedCoverage` 使用 caller-owned `MutableCoverageQuery` 做固定 `baseIndex -> coverageRef/width` 查询；live/published revision 不一致时清空结果并要求 Page-wide fallback。`ImplicitAirAdjacency.compileOwnedPairs` 只遍历 Page support 的 `+X/+Y/+Z`，规则面使用精确 overlap，mixed 面直接相交 `CompiledBrick` aperture mask，并按 cell pair 聚合为 `ThermalSweep.PairOperation`；不保存 generic edge，也不存在待接的 `CoverageCellResolver`。未 admission、stale publication、foreign coverage 或 mixed mask 不一致会拒绝本次编译并要求 runtime 重建/fallback。

Phase D 把原 PR4/PR5 合并为一条具体执行路径。`ThermalSourceTimeline` 按 monotonic source watermark 和 authoritative game tick 消费 register/power/enabled/rebind/impulse/cold-route/unload command，独占 `ThermalSourceRegistry` 与 `NodePowerAccumulatorArena` mutation，并把完整 `integral(P dt)` 直接写入同一个 `ThermalCellArena`。`ThermalSweep` 在构造时绑定该 arena 并捕获 operation endpoint generation，执行前一次性验证全部 target；`ThermalStepExecutor` 明确拒绝 source timeline 与 sweep 指向不同 arena。`TIME_DEGRADED` 只跳过显式 transport/phase tick，source energy 仍推进到 epoch target。

旧的通用 `SourceEnergyApplier`、`IntervalOperator`、空 phase callback、未使用 `SourceMutation` event result 和恒零 phase execution count 已删除。`NodeEnergyConsumer` 只保留给 accumulator 的独立测试/诊断遍历；combined timeline 不通过 callback 写 `H`。`SourceResyncReplayer.ReplayTarget` 仍是 recovery segment 到 mesh/internal/loss destination 的窄边界。

PR7 的 `DimensionThermalRuntime` 现直接拥有 `LatestSolveEpochScheduler`、`ThermalStepExecutor`、`ThermalCellArena`、`ThermalSourceTimeline` 与 `ThermalSweep`，以 explicit `InputWatermarks` ACK 接收 non-source stream cut；worker 启动时冻结 geometry/topology identity，执行期间到达的更新 ACK 保持 applied watermark 单调但不能把旧 solve 冒充新 revision。`QueryPublication` 只保存 query temperature/medium/flags projection，使用 admitted 预分配双缓冲与单调 odd/even seqlock；reader 检查 lifecycle + live geometry revision，一次 retry 后 fallback。`ThermalMemoryBudget` 同时执行 server-global 与 dimension `CRITICAL/OPTIONAL` admission，resize 必须在旧 backing 仍计费时先获得完整新 reservation。

`ThermalRuntimeCoordinator` 使用固定 primitive ready-dimension queue 和 `IDLE -> QUEUED -> RUNNING` mailbox；queue 满留下 sticky re-offer，normal request 不占 recovery reserve，并以 FIFO、oldest-age promotion 和 recovery quota 轮转维度。sleep 只允许整个 aggregate solve set 在无 source power/pending energy、topology resolved、无 pending frame 且 pair/boundary residual 稳定后进入。Minecraft geometry、topology、Campfire/Generator source、player/radiation、crop、town query 与 recipe-compiled hot-side phase 已进入上述实机测试路径；其他材料 profile 仍 dormant，registered-machine 仍只有无真实调用者的 observation 接口。FarField boundary、surface compositor 和 gameplay calibration 尚未完成，因此不能把当前切换解释为全温度系统迁移完成。

Phase E 的 `TopologyGuard` 只消费 logical geometry owner 已准备的 loaded-only 局部证据，不读取 World，也不扫描完整房间或洞穴。`UNRESOLVED` 和没有 outdoor proof、没有 approved profile、或当前功率/温差超出校准域的 opening 都不会生成 transport，而是保持 `UNRESOLVED/OPEN_CONTINUATION`；只有 `APPROVED_STATIC_IMPEDANCE` 才能直接生成现有 `ThermalSweep.BoundaryOperation`。`FarFieldReferenceHarness` 使用与 production boundary kernel 分离的多 cell RK4 finite-volume reference，严格以 fit cases 选择 `Ginf`，再以 holdout cases 检查温度轨迹、threshold crossing、signed boundary energy 和 phase received power。synthetic matrix 覆盖 open、half-open、cavern、tunnel exit、calm/windy 和 `1/10/100 kW`，但 Phase 0b 尚未冻结真实 gameplay tolerances，因此 production registry 当前保持空，不得把 synthetic test tolerance 当作 profile approval。

Phase 0a 现按冻结的常见路径范围完成：Vanilla/Forge 常规 setter、流体、门、活塞、递归 mutation、Create assemble/disassemble，以及 Frosted Heart 已知 direct bypass。`DebugCommand restore_backup` 已执行 owner rebind + full resnapshot；FastNoise raw block/biome 通知只在 section 已有 loaded owner 时置 distinct sticky resync，普通 unmapped worldgen 产生零 thermal work。resync ACK 携带开始重建时的 section identity、lifecycle generation、required revision 和 reason；旧 R1 token 不能清除重建期间产生的 R2 requirement。fingerprint 只用于 GameTest/人工 debug，不做 production 周期扫描。原 21-runtime 穷举清单与断言保留为非执行注释，不是 gate；未知第三方 bypass 在玩家报告可复现后补专用 adapter。`/resetchunks` 同样是延期兼容项，不是当前 gate；以后若支持，应丢弃刷新区块的整份旧 thermal Page 并按 interest 懒重建，而不是保留旧状态。

尚无直接自动化覆盖：真实玩家加入后首次 profile/Page 构建、完整五部位长期推进、跨 section/chunk 移动、多人主线程耗时、recipe `/reload` 后 profile cut、实际 `ServerLevel.tickChunk` Mixin 等价性、`ChunkHeatData` 负值聚合、热网多 provider/consumer tick 和网络发送频率。现有 JMH/JFR 不能替代这次存档内玩家测试；FarField、surface compositor 和材料/换热校准仍未完成，所以只有玩家空气 consumer 暂时切换，其他 gameplay authority 不变。
