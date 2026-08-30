# 暖石类热库饰品实施计划

- Time: `2026-08-24 19:34:16 +08:00`
- Updated: `2026-08-30 16:00:42 +08:00`
- Authors: `Codex; OpenAI; coding agent`
- Status: `completed`
- Scope: `Frosted Heart 玩家五部位体温、可穿戴热库、Curios 自定义槽位、新物品与配套整合包进度`
- Related: [`docs/climate/README.md`](../docs/climate/README.md), [`docs/climate/player-temperature.md`](../docs/climate/player-temperature.md), [`docs/climate/data-lifecycle-and-integration.md`](../docs/climate/data-lifecycle-and-integration.md), [`暖石任务 AI 上下文与公用交接`](../discussion/2026-08-26_23-21-11_warm-stone-ai-context-handoff.md), [`2026-08-22_16-47-30_probe-compiled-sparse-conservative-thermal-graph.md`](2026-08-22_16-47-30_probe-compiled-sparse-conservative-thermal-graph.md), `TemperatureUpdate`, `PlayerTemperatureData`, `CuriosCompat`, `SlotTypeMessage.Builder`

> 本计划已完成；当前行为以源码、生成数据、配套整合包内容和 `docs/climate/` living docs 为准。本文保留实施决策、阶段结果和验收记录。

## 1. 目标

加入“暖石类物品”这一可扩展的可穿戴热库类别。物品不修改玩家属性或换热时间常数，而是保存自己的温度和热量状态，并在佩戴时与玩家核心体温进行双向、守恒的热交换。物品与玩家温度接近后，会随玩家一起升降温，从而以附加热质量的方式变相提高玩家热容。

首版只加入两件物品，但公共接口、标签、测试夹具和数据格式不得把数量写死为两件：

| Registry ID | 中文名 | Curios 槽位 | 相对玩家热容 `r = C_item / C_player` | 表面到玩家传热率 `g_sp` | `20 degC` 温差下玩家增温率 | 首版定位 |
|---|---|---|---:|---:|---:|---|
| `frostedheart:warm_stone` | 暖石 | `warm_stone` | `0.10` | `1.2e-4 /s` | `0.0024 degC/s` | 较小热库，接触传热较快、持续时间较短 |
| `frostedheart:hot_water_bag` | 热水袋 | `warm_stone` | `0.25` | `8e-5 /s` | `0.0016 degC/s` | 较大热库，接触传热较缓、持续时间较长 |

两件物品均不可堆叠。默认只能在同一个容量为 `1` 的专用饰品槽内二选一佩戴；后续新增同类物品时复用同一槽位和热库合同。

## 2. 已确认决策

1. 暖石类首版数量为两种：暖石和热水袋，不排除后续增加其他实现。
2. 暖石类物品不提供 `BODY_HEAT_CAPACITY` 属性，也不把玩家所有体温变化统一除以倍率。
3. 每个物品分别持久化内部温度与表面温度，并拥有相对热容和换热参数；实际效果来自内部到表面、表面到玩家或环境之间的热量转移。
4. 玩家归一化热容定义为 `C_player = 1`，暖石为 `0.10`，热水袋为 `0.25`。这只是玩家体温玩法中的相对热容量，不是 SI 比热容或焦耳。
5. 两件物品使用同一个 `warm_stone` Curios 槽。已核对当前 Curios `5.9.1+1.20.1` API，`SlotTypeMessage.Builder` 支持自定义 identifier、icon、priority 和 `size(1)`，因此无需占用 `charm` 或 `back`。
6. 热物品会加热玩家并降温；冷物品会冷却玩家并升温。暖石不是无条件提供正向温度加成。
7. 穿戴或卸下物品不重算任何一方温度；穿戴后的变化只能来自后续换热。
8. 掉落在世界中的暖石类物品直接暴露于环境，环境换热速率必须远高于普通物品栏状态；把两种物品丢在篝火等已注册物理热源附近，是首版通用充热方式。
9. 蒸汽充气机 charger recipe 暂不实现，也不作为首版完成条件。将来加入时复用同一热状态合同，cost 再按 `capacityRatio` 成比例定义。
10. 首版采用物品内部、物品表面、玩家核心三个热节点。各相邻节点的瞬时传热与温差线性相关；表面先冷却、内部再补热所产生的整体非线性曲线来自节点状态演化，不使用任意的 `deltaT^2` 或其他非线性接触公式。
11. `g_sp` 表示每秒、每 `1 degC` 表面与玩家温差直接造成的玩家核心温度改变量，单位为 `degC_player / (s * degC_difference) = 1/s`。暖石固定为 `1.2e-4 /s`，热水袋固定为 `8e-5 /s`。
12. 上述 `g_sp` 是最终核心体温口径，不再乘躯干的 `0.5` 权重。`20 degC` 温差时两件物品分别产生 `0.0024 degC/s`（`0.144 degC/min`）和 `0.0016 degC/s`（`0.096 degC/min`）的瞬时玩家侧变化，与当前手炉约 `0.002 degC/s` 的目标部位原始增温量处于同一量级；这只是平衡锚点，不表示两套机制相同。
13. 两件物品的表面热容占比统一固定为 `a = 0.20`。因此暖石的内部/表面相对热容为 `0.08/0.02`，热水袋为 `0.20/0.05`；首版不把表面占比作为平衡旋钮。
14. 内部到表面的权威 profile 参数是单位温差传热率 `k_cs`，单位固定为 `1/s`。暖石固定为 `6.1613e-5 /s`，热水袋固定为 `9.2420e-4 /s`；它们分别对应隔绝玩家和环境时内外温差 `180 s` 与 `30 s` 的半衰期。半衰期只用于解释、配置辅助和测试，不作为第二个独立存储参数。
15. 库存与掉落环境导热率不再是独立 profile 参数，而是从每件物品已冻结的 `g_sp` 派生：`k_inventory = 0.5 * g_sp`，`k_dropped = 16 * k_inventory = 8 * g_sp`。倍率作用于归一化导热率，不直接乘最终表面温度变化率。
16. 掉落物辐射不使用独立的物品吸收率或换热系数；它复用玩家现有的等效辐射温度桥接：`deltaT_radiant = radiantFluxWPerM2 * 0.8 / 6`，再令 `T_effective_env = T_air + deltaT_radiant`。玩家身体能量换算仍保持独立，不用于物品。
17. `creative`、`spectator` 或其他 `invulnerable` 玩家保持当前体温管线的管理语义：整轮跳过佩戴热库交换，既不改变玩家，也不推进物品；`INSULATION` 只阻断环境换热，仍允许佩戴热库与玩家交换。
18. version 1 热状态的技术有效范围固定为 `[-1000, 1000] degC`。非有限、越界或未知 schema 不能进入积分器；仅在服务端当前环境温度有限时，将环境温度夹到该范围并同时重建内部和表面，否则保持未初始化并跳过本轮。
19. 首版热库合同使用 item 实现的窄接口、不可变 profile 和 ItemStack NBT，不注册 Forge capability/provider，也不增加第二份状态权威。
20. 数值推进复用现有 `ThermalExchangeKernel` 的守恒 pair/fixed-boundary 原语，以薄的三节点分步编排实现；不再实现第二套通用积分器或热图框架。
21. Curios `5.9.1+1.20.1` 的 `ICurioStacksHandler.isVisible()` 只控制界面可见性，`getRenders()` 只控制饰品外观；两者都不锁定 handler 使用。暖石玩法只以 `warm_stone` handler 存在、slot `0` 有效且 Stack 实现热库合同为启用条件。

## 3. 非目标

- 不新增玩家热容属性、全局装备倍率或叠加上限。
- 不改变衣物保温公式、现有 `BodyHeatingCapability`、食物温度数值或玩家/作物/城镇既有环境采样语义；只为掉落暖石增加一个复用当前热力运行时的窄 receiver 查询。
- 不把 `HeatStorageCapability`、`HeatEndpoint.heat`、蒸汽量或发电机 power 直接解释为暖石热量。
- 不让主手、副手、普通背包库存或 `charm/back/hands` 槽中的暖石产生玩家换热效果。
- 不在首版模拟液体质量、相变潜热、泄漏、沸腾、冻裂或热水袋实际流体内容。
- 不在本计划内实现世界守恒热图；暖石热库是局限于 ItemStack 与玩家之间的玩法模型。
- 不在首版接入蒸汽充气机，不新增 charger recipe 或 recipe cost。

## 4. 已验证的当前边界

1. 玩家部位温度和核心温度使用相对 `37 degC` 的偏移，物品温度需要另行规定基准，不能直接混用。
2. `TemperatureUpdate` 每个温度更新周期先计算环境、衣物、设备、运动和恒温，再合并头、躯干和腿，并处理腿脚、躯干双手交换。
3. `FoodTemperatureHandler` 会在独立事件中直接设置所有部位体温；暖石不应侵入该路径，而应在下一次玩家温度更新中自然响应。
4. `PlayerTemperatureData` 已持久化五部位体温，但 ItemStack 还没有通用热库状态。
5. `CuriosCompat.sendIMCS` 当前注册 `back` 和 `charm` 预设槽；可以在同一入口注册一个新的 `warm_stone` 槽。
6. 当前生成数据已有 `data/curios/tags/items/{back,charm,hands}.json`，专用槽需要对应新增 `data/curios/tags/items/warm_stone.json`。
7. 配套仓库 `TheWinterRescue` 当前承担便携供暖物品的 KubeJS 配方、研究、任务和 Create 风格 tooltip；暖石充热和进度入口也应在那里接入。
8. `MinecraftThermalInput.gameplayPassiveEnvironment` 会读取既有 Air Mesh publication 并组合 analytic fields，miss 时使用调用方给出的 natural temperature；该路径不建立 Page interest、不加载区块。
9. Campfire、Generator、Radiator 与 Fountain 已通过 `MinecraftPhysicalSourceManager` 接入环境 source；其直接辐射由 `RadiationService` 以只读 receiver 查询提供。目前公开的辐射 receiver 是玩家的 feet/torso/head 三点采样，尚无适合掉落物的一点式入口。
10. 仅调用 `WorldTemperature.air` 或只提高环境松弛系数，不能保证物品在篝火旁吸热：该接口的自然空气回退不代表 receiver 处的直接物理热源辐射。因此首版必须同时接通已有 publication 与物理 source receiver，不能恢复旧 `SurroundingTemperatureSimulator` 随机方块采样来伪造附近热源。

## 5. 热状态合同

### 5.1 表示与持久化

暖石类物品统一使用绝对摄氏温度，避免与玩家的 `37 degC` 偏移混淆：

```text
T_player_abs = PlayerTemperatureData.coreBodyTemp + 37
T_core_abs   = ItemStack thermal state core_temperature_c
T_surface_abs = ItemStack thermal state surface_temperature_c
C_player     = 1.0 normalized player heat capacity
C_item_total = capacity_ratio * C_player
C_item_core + C_item_surface = C_item_total
```

由 item 实现的 `WearableThermalReservoir` 窄接口暴露以下只读 profile 和状态操作；首版不注册 Forge capability/provider，避免为两个固定 Item 类型引入额外生命周期、序列化和查询层：

```text
capacityRatio(stack)
surfaceCapacityFraction(stack)
coreSurfaceTransferRatePerSecond(stack)
playerTransferRatePerSecond(stack)
coreTemperatureC(stack)
surfaceTemperatureC(stack)
setTemperaturesC(stack, coreValue, surfaceValue)
```

ItemStack NBT 使用带版本的独立 compound，至少保存：

```text
frostedheart:thermal_reservoir
  version: 1
  initialized: true
  core_temperature_c: <finite float>
  surface_temperature_c: <finite float>
```

规则如下：

- NBT 是物品热状态的唯一持久化权威；capacity ratio 来自物品 profile，不重复写入 NBT。
- 缺失状态的物品在首次有服务端环境上下文时初始化为所在位置的当前绝对环境温度，不能默认为 `0 degC` 或玩家体温。
- 缺失状态首次初始化时，内部与表面都设为同一个当前绝对环境温度，不能凭空产生初始内外温差。
- 配方或未来加热设备可以显式产出带初温的物品；只提供一个初温时同时设置内部与表面，显式值优先于首次环境初始化。
- 有效温度必须是 `[-1000, 1000] degC` 内的有限值。非有限温度、未知 schema 或越界数据不能进入积分器；若服务端当前环境有限，则夹到同一范围后同时重建内部与表面，否则保持未初始化并跳过本轮。
- 异常警告按少量固定原因全局限频，不能按玩家、实体或 ItemStack 建立无界 warning key；修复动作只能发生在服务端。
- 两件物品 `stacksTo(1)`，防止一个 ItemStack 的单份温度被多件物品共享。
- cadence、上次更新时间、receiver cache key 等运行时元数据不写入热状态 NBT；NBT 只保存可跨生命周期延续的两个温度和 schema 版本。

物品 tooltip 至少显示直接决定玩家接触换热的表面温度和“热容：玩家的 10%/25%”；内部温度放在高级 tooltip 或调试观测中，供识别“表面已冷、内部仍热”的状态。未初始化时显示“温度：未初始化”，不在客户端凭世界温度自行写 NBT。

### 5.2 玩家与物品的守恒换热

首版把物品拆为内部和表面两个有限热容节点，并把玩家核心作为第三个节点。每次玩家温度更新完成正常环境/设备/生理计算后、同步客户端前，推进一次三节点交换：

```text
r = item.capacityRatio
a = item.surfaceCapacityFraction, 0 < a < 1
r_core = r * (1 - a)
r_surface = r * a

q_core_surface = k_core_surface * (T_core - T_surface)
q_surface_player = g_sp * (T_surface - T_player)

dT_core/dt = -q_core_surface / r_core
dT_surface/dt = (q_core_surface - q_surface_player) / r_surface
dT_player/dt = q_surface_player
```

`k_core_surface` 是相对于归一化玩家热容的单位温差内外传热率；在 `C_player=1` 的归一化模型中单位为 `1/s`。`g_sp` 同样以 `1/s` 表示，数值上既是表面到玩家的归一化传热率，也是用户指定的玩家核心温度响应率。玩家侧瞬时速率必须严格满足：

```text
dT_player/dt = g_sp * (T_surface - T_player)

warm_stone:    g_sp = 1.2e-4 /s
hot_water_bag: g_sp = 8e-5 /s
```

首版内部/表面参数冻结为：

| 物品 | `a` | `r_core` | `r_surface` | `k_core_surface` | 隔绝外界时内外温差半衰期 |
|---|---:|---:|---:|---:|---:|
| 暖石 | `0.20` | `0.08` | `0.02` | `6.1613e-5 /s` | `180 s` |
| 热水袋 | `0.20` | `0.20` | `0.05` | `9.2420e-4 /s` | `30 s` |

半衰期描述的是只开启内部与表面交换、关闭玩家和环境交换时，`abs(T_core - T_surface)` 减半所需的游戏时间。它与权威单位温差传热率的关系为：

```text
deltaT = T_core - T_surface
d(deltaT)/dt = -k_core_surface * (1 / r_core + 1 / r_surface) * deltaT

t_half = ln(2) / (k_core_surface * (1 / r_core + 1 / r_surface))
       = r * a * (1 - a) * ln(2) / k_core_surface

k_core_surface = r * a * (1 - a) * ln(2) / t_half
```

实现应提供一个纯函数辅助入口，建议命名为：

```text
coreSurfaceTransferRatePerSecondFromHalfLife(
    capacityRatio,
    surfaceCapacityFraction,
    halfLifeSeconds
) -> transferRatePerSecond
```

profile 中只保存该方法算出的 `transferRatePerSecond`，不得同时保存半衰期以免两个值失配。方法的 Javadoc/代码注释必须给出上述正向公式，并说明半衰期仅在隔绝玩家和环境的两节点条件下成立；测试辅助可另提供反向换算 `coreSurfaceHalfLifeSeconds(...)`，用于 round trip 和可读断言。

任意时间步都必须在浮点容差内满足局部守恒：

```text
r_core * deltaT_core
+ r_surface * deltaT_surface
+ 1 * deltaT_player
= 0
```

原计划中两件物品共用的 `4 game seconds` 换热时间常数取消，不得继续作为实现默认。`surfaceCapacityFraction`、`k_core_surface` 以及对应的 `180 s/30 s` 半衰期均已冻结，不再由 Phase 0 调校：暖石表现为表面较快降温后由内部缓慢补热；热水袋因内部流体对流更快均温，但橡胶外壳使其固定 `g_sp` 仍低于暖石。

纯模型复用现有 `ThermalExchangeKernel.exchangePairInto` 与 `exchangeFixedBoundaryInto` 的守恒、有界解析原语。`ThreeNodeWearableHeatExchange` 与 `ReservoirEnvironmentExchange` 只负责固定拓扑的对称分步编排、最大子步长和单位换算，并复用调用方持有的 result/scratch；不得复制指数核、引入第二套通用 solver，或在稳定 tick 路径持续分配临时集合/对象。公式使用实际经过的游戏 tick，改变 `temperatureUpdateIntervalTicks` 后，同等游戏时间的结果只能有测试声明范围内的分步误差。

### 5.3 五部位接入

暖石只与核心热库交换：

1. 正常体温管线完成当轮环境换热、主动设备、运动、恒温和部位内部交换。
2. 读取更新后的核心绝对体温，与 `warm_stone` 槽中的有效物品执行一次交换。
3. 将玩家侧温差等量应用到 `HEAD`、`TORSO`、`LEGS`，重新计算 `coreBodyTemp`；`HANDS`、`FEET` 留给下一轮既有端部交换追随。
4. 物品侧温度写回同一个 ItemStack。
5. immediate feel temperature 不改写；暖石改变身体温度，不伪造环境或体感温度。

应新增一个原子式 `PlayerTemperatureData` API 完成核心温度调整，禁止事件处理器分别写三个部位后忘记刷新 `coreBodyTemp`；`prevCoreBodyTemp` 保持当轮常规更新已经捕获的上一轮值，不能因暖石交换再次前移。`INSULATION` 管理效果只阻断环境换热，不阻断玩家与已佩戴暖石之间的接触换热。`creative`、`spectator` 或其他 `invulnerable` 玩家则整轮跳过这次交换，物品也不单边推进，保持当前管理分支不改变体温的语义。

食物、手炉、暖手宝、蒸汽瓶、加热背心、运动和恒温仍按当前行为先作用玩家；暖石随后被动吸收或释放热量。因此本计划不会降低这些来源的燃料/资源消耗，也不会直接重写它们的单次温度增量。

## 6. 非穿戴状态与环境充热

物品内部与表面温度必须在脱离玩家后仍保留，否则它不能作为带内部传导的热库。首版生命周期定为：

- **专用槽中**：执行内部到表面、表面到玩家的双向换热，不再让表面额外直连环境；这样物品提供的是附加热质量，而不是新的散热面。
- **玩家普通库存中**：不与玩家换热；服务端以 `20 game ticks` 的 source-default cadence 让表面向玩家所在环境温度缓慢松弛，同时继续内部与表面的传导。只 tick 实际玩家库存中的热库 Stack，不扫描玩家或全库存；库存内物品不接收直接辐射，视为被背包和衣物遮蔽。
- **掉落实体中**：作为完全暴露的环境 receiver，服务端以 `20 game ticks` 的 source-default cadence 错峰采样所在位置的既有 Air Mesh/analytic air 与物理热源辐射，并以远高于库存状态的导热率加热或冷却表面；热量再由表面向内部传导。把暖石或热水袋丢到点燃的篝火、工作的发电机/散热器/喷泉等已注册热源附近，应能沿同一通用路径加热；熄灭热源或移开物品后则按新环境自然冷却。
- **箱子或未 tick 的容器中**：首版冻结内部与表面两个温度。Forge 不会普遍 tick 任意容器内 ItemStack；该限制必须写入文档，不能假装已经模拟离线冷却或内外均温。
- **服务器离线期间**：不按现实墙钟追赶温度变化，只按游戏 tick 推进。

### 6.1 环境换热模型

环境只与物品表面交换，物品内部继续通过 `k_core_surface` 向表面传热。普通库存和掉落实体使用相同方程，但采用不同的归一化环境导热率：

```text
r = item.capacityRatio
a = item.surfaceCapacityFraction
r_core = r * (1 - a)
r_surface = r * a
deltaT_radiant = radiantFluxWPerM2
                 * PLAYER_RADIATION_ABSORPTIVITY
                 / PLAYER_RADIATION_TRANSFER_W_PER_M2_K
T_effective_env = T_air + deltaT_radiant

k_mode = normalized environmental conductance for inventory or dropped state
q_core_surface = k_core_surface * (T_core - T_surface)
q_surface_env = k_mode * (T_surface - T_effective_env)

dT_core/dt = -q_core_surface / r_core
dT_surface/dt = (q_core_surface - q_surface_env) / r_surface
```

`k_mode` 表示相对于归一化玩家热容的表面环境导热率，不能由 `capacityRatio` 反推。首版把它绑定到同一物品已经冻结的表面到玩家传热率 `g_sp`：

```text
k_inventory = inventoryEnvironmentMultiplier * g_sp
            = 0.5 * g_sp
k_dropped = droppedEnvironmentMultiplier * k_inventory
          = 16 * k_inventory
          = 8 * g_sp
```

因此暖石使用 `k_inventory=6.0e-5 /s`、`k_dropped=9.6e-4 /s`，热水袋使用 `k_inventory=4.0e-5 /s`、`k_dropped=6.4e-4 /s`。若暂时隔绝内部传导，只看表面到环境这条边，对应的库存/掉落表面温差半衰期分别为：暖石 `231.0 s/14.4 s`，热水袋 `866.4 s/54.2 s`。这些是单边表面响应说明，不是完整双节点物品的总热量半衰期；完整曲线仍由 `r`、`a`、`k_core_surface` 与 `k_mode` 共同决定。

库存状态不接收直接辐射，令 `radiantFluxWPerM2=0`，其目标严格等于空气温度。掉落状态复用玩家当前的等效辐射温度换算，权威常量为 `PLAYER_RADIATION_ABSORPTIVITY=0.8` 与 `PLAYER_RADIATION_TRANSFER_W_PER_M2_K=6.0 W/(m2*K)`：

```text
deltaT_radiant = radiantFluxWPerM2 * 0.8 / 6.0
T_effective_env = T_air + deltaT_radiant
```

实现时应把这段换算提取为 climate/radiation 共享的纯函数，由玩家体感和物品环境共同调用；物品代码不得依赖 HUD 或玩家事件处理器。玩家的 `radiantBodyTemperatureDelta` 使用投影面积、吸收率、更新时长和有效身体热容计算真实玩家侧增温，语义不同，不能拿来推进物品。辐射 receiver 是只读观察，不改变或重复扣除 source ledger。Campfire `8,000 W` profile 的辐射份额、距离平方衰减和遮挡共同决定实际 `radiantFluxWPerM2`；不得把固定 `60 degC` 写成所有热源旁的目标温度。

### 6.2 掉落物环境采样

在 `MinecraftThermalInput` 增加目的明确的 `sampleItemEnvironment`/`gameplayItemEnvironment` 窄入口：

1. 以 `ItemEntity` 中心为单一 receiver point，先被动读取已有 Air Mesh publication；没有可用 publication 时组合 analytic fields 与稳定的 `WorldTemperature.naturalAir`，不使用每次带随机扰动的 `WorldTemperature.air` 作为持久状态积分目标。
2. 复用 `RadiationService` 的 source index、遮挡 DDA、距离衰减和 receiver witness cache，新增一点式 item receiver；不能调用玩家 feet/torso/head 三点接口后取巧平均，也不能为每个物品复制一份 source 列表。
3. 查询只读取已加载世界和已有 runtime 状态，不因掉落物建立 Page interest、不主动加载区块、不扫描附近方块，也不恢复 `SurroundingTemperatureSimulator`。
4. 入口必须是热库 Item 自身的精确 `ItemEntity` tick hook（实施时按 Forge `1.20.1` 实际签名复核），只处理当前实体中的 Stack；禁止通过 level tick 枚举全部实体或对世界做全局掉落物扫描。
5. 每个 `ItemEntity` 按稳定实体身份散列到 `20 game ticks` cadence bucket，温度公式使用自上次有效更新以来的实际 loaded game tick。实体生成、拾取、合并、跨维度与重新掉落时重置 transient 模式计时但不重置温度，不追算容器、卸载或离线期间；模式计时不得进入 ItemStack 热状态 NBT。
6. 同位置、同 tick 的掉落物在 `MinecraftThermalInput` 所属的 per-level runtime 内复用局部不可变 sample。缓存必须有固定容量、tick generation 和 level 关闭清理，不使用静态全局 map、`WeakHashMap` 或按实体永久增长的表。
7. item receiver 的每 tick 工作额度和 witness/cache 容量与现有 player receiver 分区；现有玩家容量与三点采样命中率不得因物品功能下调。准确 item hard cap 在 `T13` 依据现有容量和 workload 测试冻结并写入 source-default 常量。
8. 辐射查询当轮受限时，优先复用仍在声明时效内的同局部 sample，否则以已取得的 air/analytic/natural 结果和 `radiantFluxWPerM2=0` 推进本段并前移计时基准；不能冻结后再用未来环境追算整段时间，也不能退回逐实体方块扫描。

首版所需的通用充热入口就是上述掉落物环境交换，不要求专用“篝火配方”。热水袋的制作/灌装配方可以按实际输入决定是否带初温；没有明确热输入时按首次环境初始化。蒸汽充气机 charger recipe 延后，不属于 Phase 3 或完成标准；将来实现时必须推进同一组内部/表面温度，或在明确瞬时均温语义下同时设置二者，其 cost 再按 `0.10:0.25` 即 `2:5` 的容量比例设计。

冷却玩法同样自然发生。若后续需要主动制冷入口，复用同一温度 NBT 和热库接口，不新增第二套“冷量”字段。

## 7. Curios 专用槽

在 `CuriosCompat.sendIMCS` 中注册：

```text
identifier: warm_stone
size: 1
icon: frostedheart 自有空槽纹理
```

同时补齐：

- `FHTags.Items.CURIOS_WARM_STONE`；
- `data/curios/tags/items/warm_stone.json`，首版只含 `warm_stone` 与 `hot_water_bag`；
- `curios.identifier.warm_stone` 的 `zh_cn/en_us` 翻译；
- 独立空槽图标，轮廓能表达可放置暖石/热水袋；
- `ICurio#canEquip` 或等价校验，防止标签或外部配置误把非热库物品放入后进入换热路径。

不提供 `charm` 或 `back` fallback。若专用槽在整合包中没有出现，应修复注册或槽配置，不能静默改变物品占槽策略。

默认槽容量为 `1`，因此两件首发物品不能同时生效。扫描实现不得按具体 item ID 写 `if/else`；它应读取 `warm_stone` 槽中实现热库合同的 ItemStack，以便后续增加第三种物品。

## 8. 实现结构

建议职责拆分如下，具体包名服从实现时源码布局：

```text
content/climate/player/thermalitem/
  WearableThermalReservoir          热库合同
  WearableThermalState              NBT 读取、校验、初始化
  ThreeNodeWearableHeatExchange     内部/表面/玩家纯 Java 守恒换热模型
  ReservoirEnvironmentExchange      内部/表面/环境纯 Java 守恒换热模型
  WearableThermalExchangeHandler    Curios 查找和玩家核心接入
  DroppedReservoirExchangeHandler   ItemEntity cadence、局部采样复用与状态推进
  WarmStoneItem                     通用物品实现/profile

climate/radiation/
  RadiantEquivalentTemperature      玩家与物品共用的 q*0.8/6 纯函数
```

关键边界：

- `ThreeNodeWearableHeatExchange` 不依赖 `Player`、`Level`、ItemStack 或 Curios，便于属性测试；它的玩家侧输出直接采用核心温度口径，不经过部位权重二次缩放。它只编排现有 `ThermalExchangeKernel` 原语，不拥有另一套指数或通用图 solver。
- `ReservoirEnvironmentExchange` 同样是无 Minecraft 依赖的纯函数；它接收 `RadiantEquivalentTemperature` 计算的有限目标并复用同一 kernel，世界采样、receiver 查询和状态持久化不能塞进公式类。
- `WearableThermalState` 同时管理内部与表面状态，不读取玩家体温。
- `WearableThermalExchangeHandler` 每个温度更新周期最多查询一次 identifier 为 `warm_stone` 的 handler 和 slot `0`，不在五部位循环内重复扫描 Curios；handler 存在且 slot `0` 有效才允许玩法查询，`isVisible()` 与 `getRenders()` 都不能关闭热交换。
- `DroppedReservoirExchangeHandler` 由热库 Item 的精确实体 tick hook 调用，只处理传入的 `ItemEntity`；局部 sample cache 属于 per-level `MinecraftThermalInput` runtime，不属于 handler 静态字段。item receiver 的缓存/工作额度须与 player receiver 分区，避免大量掉落物持续挤掉玩家已有 witness cache。
- ItemStack 温度由服务端权威写入。先用现有 Curios/容器同步路径测量 NBT 写入和 packet 次数；热状态每个 cadence 最多写回一次，只有量化后的 tooltip 值改变才允许请求客户端同步。若现有路径仍陈旧，增加针对相关 Stack 的限频同步；禁止每 tick、全背包或全玩家广播。
- 首版容量和换热 profile 用构造参数/不可变 record 提供，后续如需数据驱动再设计 reload 与客户端同步，不提前增加半成品 JSON 系统。

## 9. 执行任务清单

### 9.1 复杂度与模型标注

复杂度衡量改动的推理范围、跨系统耦合和验证成本，不按代码行数判断：

| Level | 含义 | 典型工作 |
|---|---|---|
| `S` | 单一既有模式，边界明确，机械验证充分 | lang、tag、模型 JSON、清单收尾 |
| `M` | 单子系统内的新行为，需要单元测试或数据生成 | 纯换热模型、NBT 状态、物品注册、Curios 槽 |
| `L` | 跨生命周期或跨模块接入，需要集成测试和回归判断 | 玩家体温接入、掉落物 cadence、配套仓库进度 |
| `XL` | 修改共享热力运行时或有界查询合同，错误会影响其他 consumer | `RadiationService` receiver 泛化、`MinecraftThermalInput` item query、GameTest |

推荐模型基于 2026-08-26 的 [OpenAI Models 指南](https://developers.openai.com/api/docs/models)：官方将 `GPT-5.6 Sol` 定位为复杂推理与编码旗舰，`GPT-5.6 Terra` 定位为能力与成本平衡，`GPT-5.6 Luna` 定位为成本敏感的高吞吐工作。这里的模型是单项任务的最低建议，不是源码依赖；执行时若可用模型发生变化，应重新核对官方说明。

| 推荐 | 用途 | 默认 reasoning effort |
|---|---|---|
| `gpt-5.6-sol` | 共享 runtime API、辐射 receiver、玩家温度接入、跨系统最终审查 | `high`；`XL` 任务用 `xhigh` |
| `gpt-5.6-terra` | 大多数实现、纯模型、NBT、Curios、测试与 living docs | `high`；机械性实现可用 `medium` |
| `gpt-5.6-luna` | 已冻结设计下的资源补齐、静态扫描、验证记录与 diary | `medium` |

不要把 `T13-T16` 分给彼此隔离的执行上下文同时修改：四项共同触及 `RadiationService`、`MinecraftThermalInput` 与 `ItemEntity` receiver 生命周期，应由同一上下文串行完成或在每项开始前重新读取前项完整 diff 与测试结果。新主对话、分支对话、子 agent 的具体开启点、文件所有权和交接模板统一见 [`暖石任务 AI 上下文与公用交接`](../discussion/2026-08-26_23-21-11_warm-stone-ai-context-handoff.md)。其余并行机会由依赖列决定，不以并行为目标。

### 9.2 Phase 0：合同、基线与纯模型

| Done | ID | Task | Depends | Complexity | Recommended model | 验收产物 |
|---|---|---|---|---|---|---|
| [x] | `T00` | 重新核对实现前基线 | - | `M` | `gpt-5.6-terra high` | 记录 `TemperatureUpdate`/`PlayerTemperatureData` 顺序、`ThermalExchangeKernel` 可复用原语、Curios `5.9.1` 的 handler visibility 与 render bit 语义、Forge `1.20.1` 精确 ItemEntity tick hook、现有辐射回归测试、Curios/容器 Stack 同步行为、thermal runtime 测试数和两个仓库 Git 状态；发现假设过期时先修计划 |
| [x] | `T01` | 冻结热库 profile 合同 | `T00` | `M` | `gpt-5.6-terra high` | 不可变 profile 只以 `capacityRatio`、`surfaceCapacityFraction`、`coreSurfaceTransferRatePerSecond` 和 `playerTransferRatePerSecond` 为权威字段；冻结两件物品现有参数、环境派生倍率、共享 `q*0.8/6` 换算和 `[-1000,1000] degC` 状态范围；合同是 item 窄接口，不注册 Forge capability/provider |
| [x] | `T02` | 实现 `ThreeNodeWearableHeatExchange` | `T01` | `L` | `gpt-5.6-sol high` | 无 Minecraft 依赖的内部/表面/玩家固定拓扑编排，复用 `ThermalExchangeKernel` pair 原语和 caller-owned result/scratch；玩家侧严格按 `g_sp*(T_surface-T_player)`；实现半衰期正反 helper，覆盖 `180s/30s` round trip、守恒、有界性、时间步拆分和稳定路径零集合分配，不复制通用 solver |
| [x] | `T03` | 实现 `ReservoirEnvironmentExchange` | `T01` | `L` | `gpt-5.6-terra high` | 建立共享纯函数 `RadiantEquivalentTemperature` 和无 Minecraft 依赖的内部/表面/环境固定拓扑编排，复用同一 kernel；严格派生两种 `k_mode`，覆盖 `q=0`、`q=100 W/m2`、内外温差、容量差异、有限边界及非法输入，不修改玩家事件/HUD |
| [x] | `T04` | 建立曲线夹具并验证全部冻结参数 | `T02`, `T03` | `L` | `gpt-5.6-sol high` | 用合成 `q=0/100 W/m2` 输出玩家/内部/表面、库存和掉落纯模型曲线，不依赖尚未实现的 Campfire receiver；将 `TemperatureComputation` 的玩家等效辐射公式委托给共享 helper，并保持现有 `TemperatureComputationRadiationTest` 的 `100 -> 13.333333 degC` 回归；只验证不调参 |

**Gate A：** `T02-T04` 的纯模型测试全部通过，且默认参数已有可复现曲线后，才能把公式接入 ItemStack、玩家或世界 tick。

`T00` outcome（`2026-08-28`）：

- Frosted Heart 基线为 `master@8b8ee276178ac0d96c7b1a72a5ad656931af6d71`；实现前已有改动均为未跟踪用户文件。TheWinterRescue 基线为 `1.20@2a7cbd2a1412434e84ede3859d952fc273e60df9`，已有 `kubejs/server_scripts/src/recipes/shaped/new.js` 修改和未跟踪 `.workbuddy/`；未找到 companion `AGENTS.md`，两边也未发现既有 `warm_stone`、`hot_water_bag` 或 thermal-reservoir ID。
- `TemperatureUpdate` 在服务端 START、默认每 `20 ticks` 采样环境；普通分支完成设备/运动/恒温、核心合并和端部交换后调用 `PlayerTemperatureData.update`，`INSULATION` 与 invulnerable 当前共用 `updateWhenInsulated` 分支，最后在 interval 外每 tick 发送 `FHBodyDataSyncPacket`。`PlayerTemperatureData.update` 先前移 `prevCoreBodyTemp`，再从五部位重算 `coreBodyTemp`。
- `ThermalExchangeKernel.exchangePairInto` 与 `exchangeFixedBoundaryInto` 已提供 caller-owned result、解析指数趋近、有界性和 numeric-degraded no-op，可直接供 T02/T03 固定拓扑编排复用。现有 thermal JUnit 为 `33` suites、`187/187` tests；当前 gameTest 源含 `11` 个 thermal `@GameTest` 方法。
- Forge `1.20.1-47.3.0` 的精确掉落入口是 `Item#onEntityItemUpdate(ItemStack, ItemEntity)`；处理后返回 `false` 才继续原版 `ItemEntity.tick`。Curios `5.9.1` 的 `SlotTypeMessage` 仍支持 identifier/icon/priority/size，但已标为 deprecated；项目现有注册仍使用该兼容入口。
- Curios 每个 living tick 用 `ItemStack.matches` 对比上一份 Stack copy，NBT 变化会触发 `SPacketSyncStack`；普通库存继续依赖原版 container slot diff。实际 cadence packet 数仍由 Gate B 在生产写回路径接通后计数。API 明确 `isVisible()` 不锁定 handler 使用，故已修正本计划，不让 visibility 或 render bit 控制玩法。

`T01` outcome（`2026-08-28`）：

- 新增 `WearableThermalProfile` record，仅保存 `r/a/k_cs/g_sp` 四个权威分量；`WARM_STONE_DEFAULT`、`HOT_WATER_BAG_DEFAULT` 锁定全部物品参数，并由 `g_sp` 严格派生 `k_inventory` 与 `k_dropped`。
- `WearableThermalProfileTest` 锁定 record component 集、两件物品的权威/派生值和 source profile 输入域。定向 profile、`ThermalExchangeKernelTest` 与 `TemperatureComputationRadiationTest` 共 `10/10` tests 通过；T02/T03 已解锁，Gate A 仍开放。

`T02-T04` outcome（`2026-08-28`）：

- `ThreeNodeWearableHeatExchange` 以 caller-owned `MutableResult/Scratch` 和最大 `1 s` 子步编排 `core-surface half -> surface-player full -> core-surface half`，所有有限节点交换均委托 `ThermalExchangeKernel.exchangePairInto`。半衰期正反 helper、玩家初始导数、守恒、有界、平衡、拆分误差和非法输入原子 no-op 均由 `ThreeNodeWearableHeatExchangeTest` 冻结。
- `ReservoirEnvironmentExchange` 以 `environment half -> core-surface full -> environment half` 推进内部/表面/固定环境拓扑，库存和掉落入口严格消费 profile 派生的 `0.5*g_sp` 与 `8*g_sp`。`RadiantEquivalentTemperature` 成为 `q*0.8/6` 的共享纯函数；`TemperatureComputation.radiantFeelingTemperatureDelta` 已委托它，玩家身体能量增温公式保持独立。
- `WearableThermalCurveFixtureTest` 以 `0/60/300/900/1800 s` 五个检查点冻结完整 CSV。穿戴夹具从 `core=60, surface=player=37 degC` 开始；库存夹具从 `core=surface=60 degC` 向 `air=0 degC, q=0` 冷却；掉落夹具从 `core=surface=0 degC` 向 `air=0 degC, q=100 W/m2` 的 `13.333333 degC` 等效环境升温。
- `1800 s` 检查点为：穿戴暖石 `46.491909/41.020417/38.000239 degC`、热水袋 `48.368763/47.738715/38.789312 degC`（内部/表面/玩家）；库存暖石 `33.460260/18.005584 degC`、热水袋 `45.637727/44.100345 degC`；掉落暖石 `9.652867/13.108203 degC`、热水袋 `12.710289/12.943663 degC`（内部/表面）。这些是冻结参数的验证结果，不是调参输入。
- T00-T04 直接组合测试 `27/27` 通过；扩展 climate thermal 回归为 `39` suites、`209/209` tests。Gate A 已关闭，但尚未增加 ItemStack、玩家或世界 tick 集成。

`T05-T08` outcome（`2026-08-28`）：

- `WearableThermalState` 是唯一的 version-1 ItemStack 温度权威：`frostedheart:thermal_reservoir` 只保存 `version`、`initialized`、`core_temperature_c`、`surface_temperature_c`。两个节点为绝对 `degC`，只接受有限 `[-1000,1000]`；读取不写 NBT，缺失/未知/畸形/越界状态只在有限服务端环境可用时用夹紧后的环境值修复。告警按固定失败原因全局限频，不建立逐 Stack 状态；profile 与 transient cadence/cache 不进 NBT。
- `WearableThermalReservoir` 提供 immutable profile 与 ItemStack 状态的窄合同，不引入 Forge capability/provider。`WarmStoneItem` 以一个通用实现绑定 `WARM_STONE_DEFAULT` 与 `HOT_WATER_BAG_DEFAULT`，两件物品均 `stacksTo(1)`，且只可装备 `warm_stone` 的 slot `0`。
- `FHItems` 以现行 Registrate 风格注册稳定 ID `warm_stone` 和 `hot_water_bag`；注册产物没有初始化 NBT。`CuriosCompat` 通过既有 IMC 注册 `warm_stone`、priority `190`、`size(1)` 和 `frostedheart:slot/empty_warm_stone_slot`，并提供只读该槽 slot `0` 的精确热库查询。该查询不扫描其他 Curios，且不会让 `isVisible()` 或 `getRenders()` 决定玩法。
- `build.gradle` 为测试 source set 补充现有 Curios API 的 `testCompileOnly` 依赖。JDK 17 下 T05-T08 定向测试为 `11/11`；T00-T08 直接组合为 `10` suites、`38/38` tests。T09 尚未添加 item tag、文本、model、texture、槽图标资源或 tooltip；T11/T12 尚未接入玩家、库存或实体 runtime，Gate B 的写回/packet 计数也尚未开始。

`T09` outcome（`2026-08-28`）：

- `FHTags.Items.CURIOS_WARM_STONE` 和 `data/curios/tags/items/warm_stone.json` 只包含 `frostedheart:warm_stone`、`frostedheart:hot_water_bag`。两个 Registrate 物品同时拥有英文 datagen 名称和中英文合并语言条目；`curios.identifier.warm_stone` 已翻译。
- 两件物品均有 `minecraft:item/generated` 模型和仓库内 `16x16` 透明 PNG；`CuriosCompat` 已引用的 `frostedheart:slot/empty_warm_stone_slot` 也有对应 `16x16` 透明 PNG。
- `WarmStoneItem.appendHoverText` 仅读 `WearableThermalState.read`。普通提示显示表面温度（或未初始化状态）和 profile 的 `10%/25%` 热容；高级提示追加内部温度。它不创建 tag、不初始化、不写 NBT。
- JDK 17 下 `WarmStoneItemTest` 与 `WearableThermalStateTest` 为 `10/10`。前者新增 tooltip 无写入、普通/高级显示测试。T11/T12 未接入，Gate B 仍开放。

### 9.3 Phase 1：状态、物品与 Curios

| Done | ID | Task | Depends | Complexity | Recommended model | 验收产物 |
|---|---|---|---|---|---|---|
| [x] | `T05` | 实现 `WearableThermalState` NBT schema | `T01` | `M` | `gpt-5.6-terra high` | `WearableThermalState` + `WearableThermalStateTest`；version 1 双温度、有限环境延迟初始化/修复、固定原因限频告警，profile/transient 不进 NBT |
| [x] | `T06` | 实现热库物品合同并绑定两个 profile | `T01`, `T05` | `M` | `gpt-5.6-terra high` | `WearableThermalReservoir`、`WarmStoneItem` + `WarmStoneItemTest`；无 Forge capability，两个不可变 profile 和 `stacksTo(1)` |
| [x] | `T07` | 在 `FHItems` 注册两件物品 | `T06` | `S` | `gpt-5.6-luna medium` | `FHItems.warm_stone`、`FHItems.hot_water_bag` 以现行 Registrate 注册，正常 Stack 保持未初始化 |
| [x] | `T08` | 注册 `warm_stone` Curios 槽和精确查询 helper | `T00` | `M` | `gpt-5.6-terra high` | `CuriosCompat` 注册 identifier/icon/priority/`size(1)`，精确 slot-0 helper + `CuriosCompatWarmStoneTest` 覆盖 hidden/render-off 不参与玩法 |
| [x] | `T09` | 补齐 tag、lang、模型、纹理、空槽图标和 tooltip | `T07`, `T08` | `M` | `gpt-5.6-terra medium` | `FHTags.Items.CURIOS_WARM_STONE`、datagen tag、`zh_cn/en_us`、两个 item model/纹理、槽图标；tooltip 显示表面温度与 `10%/25%`，高级观测可区分内部温度，客户端不写 NBT |
| [x] | `T10` | 增加玩家核心温度原子调整 API | `T00`, `T02` | `M` | `gpt-5.6-terra high` | `PlayerTemperatureData.applyCoreBodyTemperatureDelta` 原子调整 `HEAD/TORSO/LEGS` 并复用唯一核心重算入口；`prevCoreBodyTemp`、`HANDS/FEET` 不变，非法/溢出输入完整 no-op |
| [x] | `T11` | 接入佩戴状态的内部/表面/玩家双向换热 | `T02`, `T05`, `T06`, `T08`, `T10` | `L` | `gpt-5.6-sol high` | `WearableThermalExchangeHandler` 在正常/`INSULATION` 体温处理后、同步前精确查询槽并推进三节点；管理模式查询前跳过；初始化与有效推进均至多一次 Stack 写回，状态码暴露本轮写入次数 |
| [x] | `T12` | 实现普通库存慢速环境交换 | `T03`, `T05`, `T06` | `M` | `gpt-5.6-terra high` | `WarmStoneItem.inventoryTick` 精确接入服务端 `PlayerInventory.items`；固定 `20 tick/1 s`，稳定 passive air 只作用表面，排除 Curios/容器/实体/客户端；identity set 同 tick 去重，初始化或有效推进均至多一次写回 |

**Gate B：** 两件物品、NBT、专用槽、tooltip、玩家换热和库存松弛均可在不修改 thermal runtime 的情况下独立验证；空槽玩家基线必须保持不变。测试或调试计数必须证明每个相关 Stack 每 cadence 最多一次 NBT 写回、没有每 tick slot/full-inventory/full-player 广播，并记录现有同步路径的实际 packet 数；tooltip 陈旧才允许进入限频定向同步方案。

`T10` outcome（`2026-08-28`）：

- `PlayerTemperatureData.applyCoreBodyTemperatureDelta(float)` 先计算 `HEAD/TORSO/LEGS` 的三个候选值，全部有限时才一次性提交并立即重算 `coreBodyTemp`；正负与零 delta 均受测试覆盖。非有限 delta 或会令任一核心部位溢出的有限 delta 返回 `false`，且五部位、核心值和 `prevCoreBodyTemp` 全部不变。
- 既有 `update()` 与新 API 共用私有 `recalculateCoreBodyTemp()`，核心权重仍由 `BodyPart.CoreParts` 与 `affectsCore` 唯一决定。正常 update 仍只在原位置前移一次 `prevCoreBodyTemp`；新 API 不前移历史值，也不写 `HANDS/FEET`。
- JDK 17 主线复验 `PlayerTemperatureDataCoreTemperatureAdjustmentTest`、`TemperatureComputationRadiationTest` 与 `ThreeNodeWearableHeatExchangeTest` 共 `3` suites、`15/15` tests 通过。T10 没有接入玩家 tick 或热库 Stack，Gate B 仍开放。

`T11` outcome（`2026-08-28`）：

- `WearableThermalExchangeHandler` 复用一个 caller-owned `ThreeNodeWearableHeatExchange.MutableResult` 和 `Scratch`，读取 `WearableThermalReservoir` 的 version-1 双节点状态，并在有效输入下把玩家绝对核心温度作为第三节点推进。其 `Status.stackWriteCount()` 明确区分零写入和一次写入；缺失/无效状态只初始化并结束本 cadence，不在同一轮产生第二次写回。
- `TemperatureUpdate` 在正常 `update` 或 `INSULATION` 的 `updateWhenInsulated` 后、既有 body packet 前调用 handler。精确 Curios 查询每 cadence 至多一次；创造、旁观、实体无敌或 ability 无敌在查询前整轮跳过。玩家侧只调用 `applyCoreBodyTemperatureDelta`，手、脚和 `prevCoreBodyTemp` 保持原 ownership。
- JDK 17 下 T11 handler 定向测试 `7/7`，T00-T11 直接组合 `12` suites、`52/52` tests，扩展 player/thermal/Curios 回归 `47` suites、`241/241` tests 全部通过。普通库存和同步 packet 实测仍归 T12/Gate B，因此 Gate B 保持开放。

`T12` outcome（`2026-08-28`）：

- `WarmStoneItem.inventoryTick` 只把服务端 `ServerPlayer` 的 `PlayerInventory.items` 精确 slot/object 交给 `InventoryThermalExchangeHandler`。固定 source-default cadence 为 `20 game ticks`、elapsed 为 `1.0 s`；Curios 已装备 Stack、盔甲、副手、外部容器、客户端、非玩家和 `ItemEntity` 均被排除，不扫描玩家或完整库存。
- 库存环境以 `WorldTemperature.naturalAir` 为稳定、无高斯扰动的基线，再由 `MinecraftThermalInput.gameplayPassiveEnvironment` 被动读取已有 Air Mesh 或组合 analytic fields；不创建 Page interest，不接收直接辐射。有效状态只调用 `ReservoirEnvironmentExchange.advanceInventoryInto`，严格使用 `k_inventory=0.5*g_sp`，环境只连接表面。
- handler-owned ItemStack identity set 每服务器 tick 清空，防止同一对象同 tick 重复推进；cadence 与去重状态不写 NBT。缺失/无效状态只初始化并结束本 cadence；有效结果仅在持久化 float 温度实际变化时写回一次。
- JDK 17 下 T03/T05/T06/T11/T12 定向组合为 `5` suites、`33/33` tests；扩展 player/thermal/Curios 回归为 `48` suites、`252/252` tests。未新增同步包；真实 Curios/container packet 计数和 tooltip 新鲜度尚未实测，因此 Gate B 保持开放。

Gate B observability outcome（`2026-08-29`）：

- 新增默认关闭的客户端 `WarmStoneGateBPacketCounter` 与 `/fh_gate_b start|status|reset|stop`。它分别聚合全部与暖石相关的 Curios `SPacketSyncStack`、原版 `ClientboundContainerSetSlotPacket`、原版 `ClientboundContainerSetContentPacket`，并记录整包内热库 Stack 数和探针错误。
- `SPacketSyncStackMixin` 仅在 Curios 实现类存在时加载；`ClientPacketListenerMixin` 在两个原版容器包实际处理完成后观测。相关事件以 `FH_GATE_B_PACKET` 输出 slot、item、初始化状态及双节点温度，停止时以 `FH_GATE_B_SUMMARY` 输出机器可检索汇总。观测器不写 NBT、不新增或发送 packet、不改变服务端 cadence。
- `WarmStoneGateBPacketCounterTest` 为 `3/3`；JDK 17 扩展 player/thermal/Curios 回归为 `49` suites、`255/255` tests。`runClient` 完成客户端加载且没有 `InvalidInjection`/`InjectionError`；Curios 目标类在实际连接收到同步时才首次变换，因此实测汇总的 `probe_errors` 必须为 `0`。
- 截至观测器完成时 Gate B 仍开放：工具已经就位，但静置、佩戴、普通库存、槽位移动的实际 packet 数和 tooltip 新鲜度尚未记录。最终实测结论见下方 Gate B measurement outcome。

Gate B measurement outcome（`2026-08-29`）：

- 开发客户端连接集成服务端后完成三段真实收包测量，时长分别为 `162.126 s`、`135.113 s`、`28.762 s`。三段 `probe_errors` 均为 `0`，专用 Curios `SPacketSyncStack` 均为 `0`；本次佩戴、库存和槽位移动实际走原版 container slot/content 同步。
- 原版单槽包分别为 `147`（其中热库 `146`）、`84`（热库 `84`）、`20`（热库 `20`），折合全部单槽包 `0.907/s`、`0.622/s`、`0.695/s`，不存在 `20/s` 的每 tick 广播。整内容包分别为 `3`、`0`、`1`，只随容器界面/槽位生命周期出现，没有持续 full-inventory 广播。
- T11/T12 的状态码和定向测试已证明初始化或有效推进对每个相关 Stack 每 cadence 至多一次 NBT 写回；实现没有新增全玩家、整库存或专用热库同步。玩家实测确认 tooltip 随既有同步更新且未见异常，因此不进入量化限频定向同步方案。
- **Gate B 已关闭。** `T05-T12` 接受，空槽玩家基线不变；`C2-world-runtime` 可以从 `T13` 开始。

### 9.4 Phase 2：掉落物与世界热源

| Done | ID | Task | Depends | Complexity | Recommended model | 验收产物 |
|---|---|---|---|---|---|---|
| [x] | `T13` | 将 `RadiationService` 泛化出一点式 item receiver | `T00`, `T04` | `XL` | `gpt-5.6-sol xhigh` | 从玩家三点 API 提取共同内部单点循环，复用 source index/top-K/DDA/witness 和 caller-owned scratch；玩家 API 数值、现有 `128` receiver 容量和测试不变；为 item 冻结独立 hard cap/cache 预算，不用物品下调玩家份额 |
| [x] | `T14` | 增加 `MinecraftThermalInput.sampleItemEnvironment` 与 gameplay wrapper | `T03`, `T13` | `XL` | `gpt-5.6-sol xhigh` | publication -> analytic -> `naturalAir` fallback 后组合一点式辐射；per-level runtime 拥有固定容量、tick-generation 局部 sample cache 并随 level 关闭清理；query 不 admission Page、不加载区块、不扫描方块，无静态全局缓存 |
| [x] | `T15` | 实现 `DroppedReservoirExchangeHandler` | `T05`, `T06`, `T14` | `L` | `gpt-5.6-sol high` | 通过 T00 确认的热库 Item 精确实体 tick hook 只处理传入实体，禁止 level/entity 全局枚举；稳定实体身份错峰到 `20 tick` bucket，使用 loaded elapsed；同局部 sample 可复用，拾取/跨维度/重新掉落重置 transient 计时但不重置温度 |
| [x] | `T16` | 处理查询受限与实体生命周期边界 | `T15` | `L` | `gpt-5.6-sol high` | transient mode timer 不进 Stack NBT或无界全局 map；有时效 sample 优先，过期按 air-only 推进并前移时钟；不以未来环境追算；不可堆叠；卸载/重载无墙钟追赶，实体删除后缓存/计时状态在有界时限内消失 |
| [x] | `T17` | 增加一点式 receiver 单测 | `T13`, `T14` | `L` | `gpt-5.6-terra high` | 覆盖距离、遮挡、cache hit/retrace、item hard cap、publication hit/miss、fallback、per-level cache 容量/关闭清理，以及玩家三点数值、容量和预算不回归 |
| [x] | `T18` | 增加 Campfire 掉落物 Forge GameTest | `T15`, `T16` | `XL` | `gpt-5.6-sol xhigh` | 两种物品在点燃 Campfire 旁升温，熄灭/移远后冷却，石墙改变直接辐射；查询前后 Page/cell/admission 与 chunk load 计数不增长 |
| [x] | `T19` | 增加大量掉落物工作量回归 | `T16`, `T17` | `L` | `gpt-5.6-sol high` | 固定数量实体下用计数器验证只访问热库实体、无 level 全局枚举、cadence 分布、sample 复用、item hard cap、玩家 receiver 可用性、实体移除后状态有界回收和稳定路径分配上限；墙钟只作诊断不作唯一断言 |

`T13-T16` outcome（`2026-08-29`）：

- `RadiationService.samplePlayer` 与新 `sampleItem` 共用同一候选发现、top-K、DDA、revision witness 与 caller-owned scratch 循环。玩家生产预算保持 `128 receivers / 64 visits / top 8 / 24 rays`；item 独立冻结为 `64 / 32 / 4 / 4`，两类 witness cache 分离且共同计入 optional-memory reservation。
- `MinecraftThermalInput.sampleItemEnvironment` 与 `gameplayItemEnvironment` 按已有 publication、analytic composition、稳定 `WorldTemperature.naturalAir` fallback、一点式辐射形成完整 sample。每 level 固定 `64` 个 quarter-block 同 tick cache，tick generation 改变时整代回收并在 `close()` 清空；满额位置继续 air-only，不 admission Page、不加载 chunk、不扫描邻域或实体。
- `WarmStoneItem.onEntityItemUpdate` 只把当前精确 Stack/`ItemEntity` 交给 `DroppedReservoirExchangeHandler`。UUID bucket 将 `20 tick` cadence 错峰；实体至少加载 `20+bucket` ticks 后首次推进，随后每 `20 loaded ticks` 固定推进 `1.0 s`。`k_dropped=8*g_sp` 与共享 `q*0.8/6` 环境桥接保持冻结值。
- cadence 只使用未持久化的 `ItemEntity.tickCount`；handler 没有实体 map。拾取、重新掉落、跨维度和 unload/reload 重置模式计时而保留 Stack 双温度，离线墙钟不追赶；过期 observation 以已有空气与零辐射推进，不冻结到未来环境。Stack NBT 仍只有 version、initialized 与两个温度。
- JDK 17 接手基线为 `49` suites、`255/255`；T13 定向 `13/13`、T14 定向 `14/14`、T15 定向 `28/28`、T16 定向 `30/30`，最终扩展 player/thermal/Curios 回归为 `50` suites、`266/266`。Gate C 仍开放，等待 T17-T19。

`T17-T19` outcome（`2026-08-29`）：

- T17 将 `RadiationServiceTest` 扩展为 `10` 条，覆盖 item 一点式反平方距离、遮挡 revision hit/retrace、独立 work/receiver hard cap、实际 `128 player / 64 item` cache 与 close；`MinecraftThermalInputTest` 覆盖 publication hit、natural fallback 和 sample cache capacity/generation/close。只增加 package-private、无分配的 cache/Page/admission 只读观测入口，没有改变生产数值。
- T18 在现有 thermal harness 增加一个独立 batch Forge GameTest。真实暖石、热水袋和石墙控制 `ItemEntity` 走 `WarmStoneItem.onEntityItemUpdate`：点燃 Campfire 时两种物品升温、墙后直接辐射为零；熄火并移到 source `32` blocks 外后两者冷却。即时查询和冷却阶段 Page count、chunk watermark、arena high-water/live cell 与 loaded-chunk 数不增长。
- T19 用 `400` 个 synthetic identity 验证全部 `20` buckets 且每个 loaded cadence window 每实体恰好一次；`4,096` 个 sample claims 产生 `64` store、`960` hit、`3,072` overflow，下一 tick generation 清零。`512` 次 item receiver churn 后 item/player live cache 固定为 `64/128`，player retrace 为 `0`。
- 稳态分配计数为 cadence `0 B / 40,000 checks`、sample cache `0 B / 100,000 hits`、receiver cache `304 B / 100,000 hits`；`512` churn claims 分配 `1,415,464 B`，低于声明上限 `2,572,288 B`。墙钟未用于通过条件。
- JDK 17 T17 定向为 `2` suites、`18/18`；T18 `compileGameTestJava` 通过且 `13/13 required` GameTest 通过；T19 定向为 `3` suites、`7/7`。最终扩展 player/thermal/Curios JUnit 为 `53` suites、`277/277`，零 failure/error/skip；forbidden-path 与 `git diff --check` 均通过。

**Gate C 已关闭：** `T13-T19` 已通过定向 JUnit、Forge GameTest 和 workload 计数；`WorldTemperature.air`、邻域扫描、主动 Page admission、chunk load 和 level 实体枚举均未进入掉落物热状态路径。可以从 T20 开始配套仓库任务，但本轮停在 T20 前。

### 9.5 Phase 3：配套仓库、玩家资源与进度

| Done | ID | Task | Depends | Complexity | Recommended model | 验收产物 |
|---|---|---|---|---|---|---|
| [x] | `T20` | 重新核对 `TheWinterRescue` 约束与关联 ID | `T07`, `T09`, `T18` | `M` | `gpt-5.6-terra high` | 未找到 companion `AGENTS.md`；已核对两仓库的暖石/热水袋/charger/Curios ID、配方入口和 Git 状态，见本节 outcome 与交接记录 9.16 |
| [x] | `T21` | 加入制作/获取与可选热水灌装入口 | `T20` | `L` | `gpt-5.6-terra high` | `TheWinterRescue/kubejs/server_scripts/src/recipes/warm_stone.js` 提供两件物品的制作入口；只有现有 Hot Water 杯把热水袋的两个节点初始化为 `60 degC`，没有新增篝火或 charger 配方 |
| [x] | `T22` | 接入研究、任务与 Create tooltip | `T20`, `T21` | `M` | `gpt-5.6-terra high` | `warm_stone` 研究、`Carry The Warmth` 任务和中英 Create 风格 tooltip 已准确覆盖专用槽、双向交换、物理热源旁充热与未 tick 容器暂停；不宣称 charger 可用 |
| [x] | `T23` | 补充开发测试堆栈与手工验证工具 | `T05`, `T07`, `T18` | `M` | `gpt-5.6-terra medium` | OP-only `/fh_warm_stone_test` 生成四种独立 version-1 Stack 并按可选 cadence 输出玩家/内部/表面温度序列；没有正式创造栏调试变体 |

### 9.6 Phase 4：验证、文档与收尾

| Done | ID | Task | Depends | Complexity | Recommended model | 验收产物 |
|---|---|---|---|---|---|---|
| [x] | `T24` | 运行定向与全量自动化 | `T11-T19`, `T21-T23` | `L` | `gpt-5.6-terra high` | 依次完成 kernel/模型/NBT/Curios/同步测试、`TemperatureComputationRadiationTest`、`RadiationServiceTest`、`MinecraftThermalInputTest`、相关 GameTest、全量 `test` 和 `build`；失败必须归因并修复，不能只重跑 |
| [x] | `T25` | 执行第 11 节实机矩阵与冻结参数验收 | `T24` | `L` | `gpt-5.6-sol high` | 保存全部温度与篝火/遮挡曲线；验证而不改写冻结参数；记录无敌/render-off 行为、库存/掉落 cadence、每 cadence NBT 写回和客户端 packet 计数、关键体温阈值及大量实体 workload 计数 |
| [x] | `T26` | 更新 Frosted Heart living docs | `T24`, `T25` | `M` | `gpt-5.6-terra high` | 更新第 13 节列出的三个 climate 文档及索引，写明公式、单位、默认值、代码锚点、生命周期和查询边界 |
| [x] | `T27` | 分别验证两个仓库与资源差异 | `T24`, `T26` | `M` | `gpt-5.6-terra high` | 两仓库分别执行适用构建/脚本/启动验证、`git diff --check` 和 registry/NBT 扫描；报告各自 Git 状态，不混写结果 |
| [x] | `T28` | 完成计划 Outcome 与开发 diary | `T27` | `S` | `gpt-5.6-luna medium` | 本计划标记 `completed` 并记录实际路径、最终参数、测试结果和剩余限制；新增 timestamp diary，文档影响明确 |

**Gate D：closed.** 第 14 节完成标准已满足，`T24-T28` 均有可追溯结果，计划状态已改为 `completed`。

`T20` outcome (`2026-08-29 19:02:05 +08:00`):

- TheWinterRescue 根目录及其工作树内未找到 `AGENTS.md`；其 `kubejs/CONTRIBUTING.md` 仍要求新增配方遵循 `server_scripts/src/functions.js` 和 `recipes/remove.js` 的既有入口。`frostedheart:charger` 的 schema 和既有 activated-carbon 配方存在，但没有暖石或热水袋 charger 配方；它不属于本阶段。
- FrostedHeart 已保留完整的 `frostedheart:warm_stone`、`frostedheart:hot_water_bag`、`frostedheart:thermal_reservoir` 和容量为 `1` 的 `warm_stone` Curios 槽；现有代码/测试/文档继续证明掉落物靠通用 ItemEntity 环境 receiver 从已注册物理热源取得热量。`git diff --check` 通过。
- TheWinterRescue 没有旧 `warm_stone`、`hot_water_bag`、thermal-reservoir 或同名 Curios ID；`caupona:nail_soup` 是已有的“Hot Water”流体，现有篝火/烟熏把水容器转为它，Create mixing 也能生产它。它是唯一可用于显式热水灌装的已证实热输入，不增加专用篝火配方。其工作树基线为分支 `1.20`，已有暂存的 `kubejs/server_scripts/src/recipes/shaped/new.js` 修改和未跟踪 `.workbuddy/`；两者均保留不改。

`T21` outcome (`2026-08-29 19:07:05 +08:00`):

- `TheWinterRescue/kubejs/server_scripts/src/recipes/warm_stone.js` 新增 `the_winter_rescue:minecraft/crafting_shaped/warm_stone` 和 `.../hot_water_bag`。前者消耗 `minecraft:smooth_stone` 与 `frostedheart:straw_lining`，后者消耗 `frostedheart:leather_water_bag` 与同一衬里；两者都不写热库 NBT。
- 可选 `.../crafting_shapeless/fill_hot_water_bag` 只匹配装有 `250 mB caupona:nail_soup` 的 `frostedheart:wooden_cup_drink`，写入 version-1 compound 的两个 `60 degC` 节点并返还空木杯。没有 `caupona:nail_soup` 时，不能得到初始化的高温热水袋。
- `node --check` 与配方合同静态断言通过；两仓库的 `git diff --check` 均通过。没有修改 `new.js` 的用户暂存变更、T13-T19 receiver 合同、任何 campfire recipe 或 charger recipe。

`T22` outcome (`2026-08-29 19:20:14 +08:00`):

- TheWinterRescue 新增 `config/fhresearches/warm_stone.json`，它在 `hand_warmer` 后注册 `frostedresearch:living` 研究并解锁两件物品的 recipe effect。中英研究文本准确说明专用单槽、冷热双向交换、已注册物理热源旁充热及未 tick 容器暂停。
- `config/ftbquests/quests/chapters/t0.snbt` 在既有篝火任务 `20E34B234337C28A` 后加入可选 `Carry The Warmth`（`6A97729E570341EF`），要求取得两件物品；中英任务文本复用同一实现边界。
- `kubejs/assets/twr_tooltips/lang/{en_us,zh_cn}.json` 增加两件物品的 Create 风格 tooltip。它们清晰标明 `warm_stone` 专用槽、双向交换、掉在点燃篝火等已注册物理热源旁充热和未 tick 容器暂停；热水袋另准确说明 Hot Water 灌装得到双节点 `60 degC`。
- T22 的 JSON parse、KubeJS syntax、研究 parent/effect、语言 key、quest SNBT 平衡/引用和禁用 charger 文本专项检查均通过。JDK 17 强制回归为 `53` suites、`277/277` tests，零 failure/error/skip。完整 TheWinterRescue research catalogue 预检仍因已存在的 `coke_oven`、`mechanical_bellows`、`storage_drawers`、`tetra` 缺少 parent `workbench` 而失败；`warm_stone` 不在错误项中，隔离目录预检因环境不允许创建临时测试目录而未运行。
- `config/.gitignore` 现显式白名单 `!/fhresearches/*.json`，使新增 `warm_stone.json` 不再被根级 `*` 隐藏；该目录今后新增的研究定义也会进入 Git 状态。

`T23` outcome (`2026-08-29 19:40:03 +08:00`):

- 新增 `WarmStoneTestCommand`，仅 OP 可执行的 `/fh_warm_stone_test give <warm_stone|hot_water_bag> <cold|environment|hot|core_hot_surface_cold>` 每次创建独立 Stack。预设为 `-20/-20 degC`、`WorldTemperature.naturalAir` 同值双节点、`60/60 degC` 和 `60/0 degC`；不新增 item、tag、recipe 或创造栏变体。
- `/fh_warm_stone_test observe start [interval_ticks]|status|stop` 默认每 `20` ticks 输出 `FH_WARM_STONE_OBSERVE`，包括游戏 tick、玩家核心绝对温度、专用 Curios 槽中热库的内部/表面温度或 empty/uninitialized 状态。观察默认关闭，仅读取，登录退出和服务器停止时清理，绝不写额外 NBT 或同步。
- `WarmStoneTestCommandTest` 覆盖四个预设、version-1 NBT 写入、命令树和独立节点输出。JDK 17 定向组合为 `2` suites、`10/10` tests，零 failure/error/skip；`git diff --check` 通过。第一次命令树测试暴露 Registrate 方法引用在构建期过早初始化，已改用延迟 Supplier，未改变实际 give 行为。

`T24` outcome (`2026-08-29 20:48:22 +08:00`):

- JDK `17.0.2` 强制定向回归覆盖 kernel、profile、三节点/环境模型、曲线、NBT、物品、Curios、佩戴/库存/掉落、Gate B 同步观测、玩家核心调整、T23 命令、辐射和 Minecraft item environment/workload，共 `21` suites、`101/101` tests，零 failure/error/skip。`WarmStoneTestCommandTest` 与全部既有热库合同均在该集合内通过。
- Forge GameTest 重新编译并运行 `13/13 required`，其中 thermal `12` 条、Frosted Research `1` 条；暖石/热水袋的点燃 Campfire 升温、移远冷却和石墙遮挡批次通过。
- 第一次全量 `test` 在 `868` 条中定位到唯一失败：`TeamTownActualSaveCodecProbeTest` 读取硬编码私人 macOS 存档路径。该测试已改为仓库自包含的 `NbtOps` 持久化 fixture，保持“存档 payload -> 完整同步 packet”覆盖且不改生产行为；定向 `1/1` 后，强制全量为 `201` suites、`868/868`，零 failure/error/skip。完整 `build` 随后成功。
- TheWinterRescue 的 KubeJS 语法、`7` 个 JSON、研究 parent/effects、任务定义、双语 key、热水 NBT、无 charger 路径及 worktree/index `git diff --check` 均通过。完整 `validateResearchCatalog` 仍只报告既有 `coke_oven`、`mechanical_bellows`、`storage_drawers`、`tetra` 缺少 `workbench` parent，未报告 `warm_stone`；T24 不修这些目录外问题。
- 本阶段没有修改暖石运行行为、T13-T19 receiver 合同或 living docs；总体温度 Tooltip/config 仍等待用户明确恢复。T24 完成，停在 T25 前。

`T25` outcome (`2026-08-30 15:37:40 +08:00`):

- 用户在开发集成世界 `260829warmstone` 完成空槽、hot/cold 暖石与热水袋、`60/0 degC` 双节点反转、严寒附加热质量、暖手宝、普通库存/掉落、Campfire/移远/石墙、箱子、Curios 单槽与 render-off、creative/spectator、跨维度/重连/完整客户端重启和多掉落实体矩阵；人工报告未发现卡顿、Tooltip 陈旧、槽位、渲染、持久化或冷热效果异常。
- 隔绝环境的实测表面到玩家传热率为：hot 暖石 `1.19892e-4 /s`、hot 热水袋 `7.9992e-5 /s`、cold 热水袋 `7.9990e-5 /s`、cold 暖石 `1.20052e-4 /s`，相对冻结 `1.2e-4/8e-5 /s` 的整段误差均小于 `0.1%`；日志三位小数下最大归一化热量残差小于 `0.001`。暖石初始更快，hot/cold 累计效果分别约在 `136 s/204 s` 被热水袋反超。
- `core=60, surface=0 degC` 的热水袋令玩家先降温，在约 `55 s` 达到最低点、约 `57 s` 表面越过玩家后转为升温；该段反推 `g_sp=7.9878e-5 /s`，误差 `-0.152%`，最大归一化热量残差 `0.00075`。
- `-20 degC` control field 下 180 秒玩家变化为空槽 `-0.178 degC`、`37/37 degC` 暖石 `-0.091 degC`、热水袋 `-0.073 degC`。库存/掉落实测同段末附近，库存暖石约为 `57.44/33.49 degC`，掉落暖石约为 `50.46/-7.62 degC`（内部/表面），确认掉落环境响应显著更快。暖手宝段仍受寒冷环境主导，玩家净降 `0.082 degC`，但暖石表面升 `1.116 degC`；该段只作为设备顺序定性证据，不据此调参。
- Campfire 暴露时两件 cold reservoir 均升温；移远后暖石开始冷却，仍低于当地环境的热水袋继续以显著更慢斜率升温；相近约 80-90 秒时墙后暖石表面约 `-12.09 degC`，无遮挡约 `-10.38 degC`。箱子 180 秒双节点保持、Curios 单槽互斥/图标/render-off、creative/spectator 精确暂停、survival 恢复均通过。
- 完整客户端重启前最后保存值为玩家/内部/表面 `37.183/48.185/46.922 degC`，重启后首次为 `37.165/48.130/46.988 degC`，符合进入世界后数个 cadence 的连续交换，没有重置或未初始化。重启前日志正常轮换到 `2026-08-30-1.log.gz`。
- 多实体 smoke 段持续约 `345.936 s`，Gate B 观察到 `31` 个不同热库槽位；9 次 `/forge tps` 均为 `20.000 TPS`，总平均 tick 耗时 `11.935..19.080 ms`、均值 `16.404 ms`。约 `364.032 s` Gate B 汇总为 thermal slot `1817`、content `0`、Curios `0`、`probe_errors=0`（约 `4.99` thermal slot packet/s），无 tick 追赶或相关错误。
- 没有修改冻结参数、生产代码、T13-T19 receiver 合同或 living docs。T25 完成并接受，停在 T26 前；总体温度 Tooltip/config follow-up 仍等待用户明确恢复。

`T26-T28` outcome (`2026-08-30 16:00:42 +08:00`):

- T26 将最终行为汇总到 `docs/climate/player-temperature.md`、`world-climate-and-temperature.md`、`data-lifecycle-and-integration.md` 与 climate 索引：明确内部/表面/玩家三节点公式、隔绝双节点半衰期正反换算、`degC`/`1/s` 单位、冻结 profile、NBT/Curios/同步、库存/掉落/容器生命周期、item receiver 查询边界及 T25 验收证据。
- T27 在 JDK `17.0.2` 下以一次 `test build runGameTestServer --rerun-tasks` 完成 FrostedHeart 最终验证：JUnit `201` suites、`868/868`，零 failure/error/skip；完整 build 成功；Forge GameTest `13/13 required`。ID/tag/model/version-1 NBT、无热库专用同步、无 charger 路径和两种 diff check 均通过。GameTest 服务端启动时有一次 `280 ticks behind` 加载警告；T25 正常世界的 9 次稳态样本仍全部为 `20.000 TPS`。
- TheWinterRescue 独立通过 `node --check`、`7` 个 JSON、研究/任务/双语 key、Hot Water NBT、唯一 KubeJS 入口、无 charger claim 及 worktree/index diff check。完整 `validateResearchCatalog` 仍只报告既有 `coke_oven`、`mechanical_bellows`、`storage_drawers`、`tetra` 缺少 `workbench` parent，没有 `warm_stone` 错误；其递归工作树仍没有 `AGENTS.md`。
- 最终生产路径为 FrostedHeart 的 `content/climate/player/thermalitem/`、共享 radiation/Minecraft thermal input、Curios/item/command/mixin/resource 接入，以及 TheWinterRescue 的 `recipes/warm_stone.js`、`fhresearches/warm_stone.json`、T0 quest 和中英文本。冻结参数保持暖石/热水袋 `r=0.10/0.25`、`a=0.20`、`k_cs=6.1613e-5/9.2420e-4 /s`、`g_sp=1.2e-4/8e-5 /s`；未因 T25 调参。
- Gate D 关闭，T00-T28 全部完成。保留限制为未 tick 容器暂停、不追算离线墙钟、只有掉落物从通用已注册物理热源 receiver 充热、没有篝火/charger recipe 或热库专用同步；总体温度 Tooltip/client config follow-up 仍等待用户明确恢复。

### 9.7 推荐执行顺序

关键路径为：

```text
T00 -> T01 -> T02/T03 -> T04
     -> T05/T06/T08/T10 -> T11/T12
     -> T13 -> T14 -> T15/T16 -> T17/T18/T19
     -> T20/T21/T22/T23 -> T24 -> T25 -> T26/T27 -> T28
```

`T07/T09` 可在 `T06/T08` 完成后与纯模型测试并行准备；`T10-T12` 不依赖世界 item receiver；`T20` 之前不得改配套仓库脚本。每完成一项就勾选对应行并附实现路径或验证命令，不等到结尾一次性全部标记。

## 10. 自动化验证

### 10.1 数学与状态测试

- 任意有限冷热初温下，物品内部、物品表面和玩家三节点交换的归一化总热量残差在声明的浮点容差内。
- 三节点相同温度时不发生变化；每条边的热量总从高温流向低温；积分不产生超出初始节点温度包络的数值过冲。
- 当表面与玩家温差为 `20 degC` 时，暖石玩家侧初始变化严格为 `0.0024 degC/s`，热水袋严格为 `0.0016 degC/s`；负温差时数值等大反向。
- 半衰期辅助方法按 `k_cs = r*a*(1-a)*ln(2)/t_half` 换算；暖石 `180 s` 得到 `6.1613e-5 /s`，热水袋 `30 s` 得到 `9.2420e-4 /s`，正反换算 round trip 在声明浮点容差内。
- 隔绝环境并运行到平衡时，先按内部/表面热容求物品初始能量加权平均温度；暖石令玩家移动该平均温差的 `0.10/1.10`，热水袋令玩家移动 `0.25/1.25`。
- 相同内部与表面初温下，热水袋能交换的总热量大于暖石；暖石接触初始速率更高，但不得据此改变总热容。
- “内部热、表面冷”时玩家首先按表面温差响应，随后内部补热改变表面和玩家曲线，证明实现没有退化回单一物品温度。
- 把同一游戏时间拆成不同 update interval 后，最终温差只允许落在积分器声明并由测试冻结的数值误差内。
- 两个热交换编排器复用 `ThermalExchangeKernel`，默认 cadence 的稳定路径不创建临时 collection；测试不得出现独立复制的指数推进公式或第二个通用 solver。
- 相同物品、内外初温、空气和 elapsed time 下，严格使用 `k_inventory=0.5*g_sp`、`k_dropped=16*k_inventory=8*g_sp`；暖石两值为 `6.0e-5/9.6e-4 /s`，热水袋为 `4.0e-5/6.4e-4 /s`。
- 环境只直接改变表面；内部温度通过已冻结的 `k_core_surface` 追随。隔绝内部传导时，暖石库存/掉落表面半衰期为 `231.0 s/14.4 s`，热水袋为 `866.4 s/54.2 s`；完整双节点曲线不误标为这些单边半衰期。
- 辐射为 `0` 时环境目标严格等于空气温度；`q=100 W/m2` 时共享 helper 和玩家 `TemperatureComputation` 都返回 `13.333333 degC` 温差；非法输入按声明规则回退而不产生 `NaN`。
- NBT round trip 同时保留内部与表面温度；`-1000/1000 degC` 边界有效，越界、旧版、`NaN`、无穷只在有限环境可用时同时重建双温度，非有限环境下保持未初始化且不进入积分器；限频器 key 数固定有界。
- 物品内部与表面从热变冷、从冷变热以及反复穿脱时都连续，不因装备事件重置或强制均温。

### 10.2 集成与资源测试

- 空 `warm_stone` 槽保持当前玩家温度行为。
- 槽容量为 `1`，两件物品只能二选一；其他 Curios 槽不能触发换热；hidden handler 和关闭饰品渲染仍然换热，只有准确 handler/slot 的存在性控制玩法。
- 食物、手炉、暖手宝、蒸汽瓶、加热背心先按当前逻辑生效，物品随后只做守恒交换。
- `INSULATION` 效果下环境换热停止，但已佩戴物品仍可与玩家换热。
- `creative`、`spectator` 和其他 `invulnerable` 玩家整轮跳过佩戴交换，玩家与物品温度均不改变。
- 死亡、重连、跨维度和服务器重启保留物品内部与表面温度，没有玩家侧残留 modifier。
- 两个物品 ID、专用 Curios tag、槽位翻译、空槽图标、模型、纹理和 tooltip 全部存在。
- item receiver 在 Air Mesh 命中/miss、analytic field、点燃/熄灭 Campfire、实体移入/移出辐射范围和石墙遮挡变化下返回符合当前 thermal runtime 的结果。
- item receiver 查询不增加 Page/cell/admission 数，不加载区块，不扫描邻近方块或枚举 level 全部实体；大量掉落物按 cadence/per-level bounded cache/item hard cap 分摊工作，且不降低既有玩家 receiver 容量。
- 玩家槽与库存路径每个相关 Stack 每 cadence 最多一次 NBT 写回；同步计数证明没有每 tick slot、full-inventory 或全玩家广播，实体删除/level 关闭后 transient cache 与计时状态回到固定上限内。
- 数据生成后执行 `git diff --check`，不接受无关 generated-resource churn。

### 10.3 构建命令

按实施时仓库现状执行并记录：

```text
gradlew.bat test --tests <新增热库模型、NBT 和接入测试>
gradlew.bat test
gradlew.bat build
```

配套仓库使用其当时已有的 KubeJS/资源校验流程；若没有自动校验器，至少执行脚本加载、注册/NBT key 扫描和一次干净客户端启动。两个仓库的结果必须分别报告。

## 11. 实机验证矩阵

| Scenario | Expected result |
|---|---|
| 内部/表面均为 `60 degC` 的暖石配正常体温玩家 | 玩家初始增温率符合 `g_sp`，表面先降温、内部继续补热，三节点总热量守恒 |
| 内部/表面均为 `60 degC` 的热水袋配正常体温玩家 | 初始接触速率低于暖石，但同等条件下总缓冲更强，不瞬间跳到平衡温度 |
| 内部为 `60 degC`、表面接近玩家温度 | 玩家起初几乎不升温，待内部热量传到表面后再升温，形成自然的非线性时间曲线 |
| 冷暖石配温暖玩家 | 暖石吸热并使玩家短时降温，证明交换是双向的 |
| 暖石先与玩家平衡，再进入严寒 | 玩家和暖石共同降温，玩家曲线比空槽更平缓 |
| 暖石先与玩家平衡，再由玩家靠近热源 | 玩家先从现有环境/辐射路径升温，暖石随后被动吸热，曲线同样更平缓 |
| 手炉/热饮/加热背心工作时 | 热源先加热玩家，暖石随后吸收部分热量且自身升温 |
| 穿脱、换成另一件、丢出再拾取 | 两个 ItemStack 各自温度连续，无复制、清零或串状态 |
| 同初温物品留在普通库存/掉落在无热源环境 | 表面先趋近当地空气温度、内部随后追随；掉落实体的表面环境导热率严格为库存的 `16` 倍 |
| 两种物品丢在点燃篝火旁 | 均通过通用 item environment receiver 先加热表面、再加热内部；热水袋储热更多，无专用篝火 recipe |
| 熄灭篝火或把掉落物移远 | 有效环境温度随 source 状态/距离变化，物品停止受热并自然冷却 |
| 石墙隔开篝火与掉落物 | 直接辐射遵循既有遮挡结果，空气温度仍取已有 publication 或 natural/analytic fallback |
| 箱子或未 tick 容器 | 保持最后温度，符合首版暂停限制 |
| Curios GUI | 只有一个专用暖石槽，图标和名称正确，不能同时佩戴两件 |
| 关闭暖石的饰品渲染 | 只隐藏外观，玩家与暖石仍正常交换 |
| `creative`/`spectator`/其他无敌状态佩戴暖石 | 玩家与物品都不推进；退出管理状态后从原温度继续 |
| 重连/重启/跨维度 | 温度状态持久且客户端 tooltip 最终一致 |
| 长时间佩戴、放在库存和大量掉落实体 | NBT 写回、同步 packet、receiver 和 sample cache 均受 cadence/hard cap 限制，无逐 tick 全量广播、全实体扫描或持续增长状态 |

至少保存空槽、暖石和热水袋在相同环境下的玩家/内部/表面温度时间序列，记录总热量残差、表面峰值、达到平衡的时间和关键体温阈值。

## 12. 风险与兼容性

- **高初温伤害**：热水袋热容为 `0.25`，高初温或过强的局部热源可能把玩家推过状态阈值。必须用已冻结的表面到玩家传热率、内部传热率、辐射等效温度参数和明确配方初温调节速率与总量控制曲线，不能偷偷降低热容或改写 `a`、`k_core_surface`、`g_sp`。
- **表面小热容的积分刚性**：若表面热容占比过小而内部或环境导热率过高，表面温度会形成很短的时间尺度。T02/T03 必须复用 `ThermalExchangeKernel` 的有界原语、对称分步并覆盖大 elapsed time，不能依赖默认一秒 cadence 恰好不爆炸，也不能维护第二套数值核。
- **当前玩家模型不守恒**：玩家与环境、设备及部位内部交换仍是旧玩法公式；本计划只保证“物品内部、物品表面与玩家核心”这一局部交换守恒。
- **容器不 tick**：箱子会暂停温度演化，这是首版明确限制。若未来修复，应设计可确定的惰性时间推进与环境来源，不能按现实墙钟或未知区块温度猜测。
- **掉落物查询规模与状态泄漏**：世界中可能同时存在大量 `ItemEntity`。必须由热库 Item 自身的实体 hook 精确触发，错峰采样、按局部位置复用 per-level bounded sample、限制每 tick item receiver 工作并用实际 elapsed tick 推进；不能枚举 level 全部实体、逐实体扫描附近方块、主动 admission Page，或用静态/Weak map 留下无界实体状态。
- **辐射到物温的桥接**：现有物理 source 给出 `W/m2`，暖石热容是相对玩家的玩法量。首版复用玩家 `q*0.8/6` 的等效辐射温度换算，不引入独立物品 absorptivity/transfer 参数；该共享常量若改变，玩家体感与掉落物目标会一起改变，必须同时回归两条路径。不能把辐射功率直接当摄氏度相加，也不能把玩家身体能量增温公式误用为物品环境目标。
- **自然空气抖动**：`WorldTemperature.air` 含查询时随机扰动，不适合作为长期 ItemStack 积分目标。item receiver miss 时使用 `WorldTemperature.naturalAir` 再组合 analytic fields，保证同位置、同 tick 的共享 sample 稳定。
- **频繁 NBT 同步**：每轮写浮点温度可能造成 Curios 槽同步流量。实现应保留服务端精度，每个相关 Stack 每 cadence 最多写回一次；客户端只在量化后的 tooltip 值变化时定向同步。Gate B/T25 必须记录 packet 计数，不能只凭代码目测认为现有 Curios 同步足够便宜。
- **自定义槽冲突**：identifier 固定为 `warm_stone`。实施时检查整合包和已装模组没有同名不同语义槽；冲突时应统一槽定义，而不是注册第二个近义槽。
- **首次初始化**：没有世界上下文的创造、命令或 recipe 输出可以暂时未初始化；首次服务端使用前必须完成初始化，客户端不得擅自选择温度。
- **未来扩展**：新增第三件物品只应增加 profile、注册、tag、资源和配方；不应修改三节点算法或玩家更新分支。
- **未来热图**：`capacityRatio` 可在未来映射为 `C_item = ratio * C_playerBaseline`，但当前不能宣称它等于实际 `J/K`。

## 13. 文档影响

实施时更新：

- `docs/climate/player-temperature.md`：物品内部/表面温度基准、相对热容、`k_core_surface`/`g_sp` 的 `1/s` 单位与默认值、半衰期正反换算公式、交换顺序、三节点公式、库存/掉落/容器生命周期、通用掉落充热和两件物品；
- `docs/climate/world-climate-and-temperature.md`：item receiver 的 Air Mesh/analytic/natural fallback 顺序，以及物理 source 辐射如何进入掉落物有效环境温度；
- `docs/climate/data-lifecycle-and-integration.md`：ItemStack NBT schema、Curios 槽注册、初始化、同步、掉落物 cadence/cache/budget、配置/常量和 reload 边界；
- `docs/climate/README.md`：准确区分世界材料热容、玩家本体模型和暖石局部归一化热库；
- 配套仓库任务/研究说明：充热方式、当前温度、冷热双向效果和专用槽位。

不新建独立系统文档；该功能属于玩家体温系统。

## 14. 完成标准

只有满足以下条件才能把计划标为 `completed`：

1. 暖石与热水袋均独立持久化内部与表面温度，热容比分别严格为 `0.10` 和 `0.25`。
2. 内部/表面/玩家换热方向、平衡点、时间步和归一化能量守恒测试通过；两件物品的表面热容占比均严格为 `0.20`，暖石/热水袋的内外传热率严格为 `6.1613e-5 /s`、`9.2420e-4 /s`，对应半衰期严格为 `180 s/30 s`；玩家侧传热率分别严格为 `1.2e-4 /s` 和 `8e-5 /s`，且直接采用最终核心温度口径。两个固定拓扑模型复用 `ThermalExchangeKernel`，没有第二套通用 solver 或稳定 tick 临时集合分配。
3. 专用 `warm_stone` Curios 槽稳定显示且容量为 `1`，两件物品不能同时佩戴。
4. 空槽不改变现有体温行为；现有供热、食物、保温和管理效果符合本文边界；`INSULATION` 仍交换，`creative/spectator/invulnerable` 双方都跳过，render toggle 不影响玩法。
5. tooltip、双语文本、模型、纹理、空槽图标、tag、制作/获取入口、研究和任务完整，且没有对尚未实现的蒸汽充气机充热作出说明。
6. ItemStack 内部与表面温度在穿脱、掉落、死亡、重连、跨维度和服务器重启后符合生命周期合同；只接受 `[-1000,1000] degC` 有限状态；普通库存严格使用 `k_inventory=0.5*g_sp`，掉落实体严格使用 `k_dropped=16*k_inventory=8*g_sp`；没有 Forge capability 或第二份状态权威。
7. 两种物品丢在点燃篝火等已注册物理热源附近时，均能通过通用 item environment receiver 加热；熄灭、移远和遮挡变化能沿现有 thermal runtime 反映。查询不建立 Page interest、不加载区块、不枚举 level 全部实体；per-level cache、item receiver 工作和 transient 状态有明确硬上限，且现有玩家 receiver 容量不下降。
8. 玩家槽、库存和掉落物稳定路径满足 cadence 约束；每个相关 Stack 每 cadence 至多一次 NBT 写回，没有逐 tick slot/full-inventory/full-player 广播，Gate B/T25 留有 packet 与 workload 计数证据。
9. Frosted Heart 与 TheWinterRescue 分别完成构建或脚本/启动验证，并分别报告 Git 状态。
10. living docs、计划 Outcome 和开发 diary 已同步最终参数、限制与验证结果。

## 15. Outcome

已完成。`T00-T28` 全部完成并接受，Gate A、Gate B、Gate C、Gate D 均已关闭。两件物品的三节点热库、专用 Curios 槽、生命周期、通用掉落环境 receiver、制作、真实 Hot Water 灌装、研究、任务、Create 风格 tooltip、OP 测试工具、自动化、实机矩阵、living docs 和两仓库最终验证均已完成。最终 JUnit 为 `201` suites、`868/868`，Forge GameTest 为 `13/13 required`，完整 build 成功；TheWinterRescue 暖石专项静态验证通过。剩余限制与延期项仅为本文 `T26-T28 outcome` 所列内容。
