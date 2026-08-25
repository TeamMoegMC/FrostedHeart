# Climate And Temperature Documentation

这里归档 Frosted Heart 当前已实现的气候、世界温度、局部热区、玩家体温与热网行为。源码、服务端配置、数据包配方和存档数据仍是最终权威。

| Document | Scope | Status |
|---|---|---|
| [world-climate-and-temperature.md](world-climate-and-temperature.md) | 逻辑气候时钟、事件轨道、世界/空气/方块温度公式、局部热区 | Current |
| [weather-rendering.md](weather-rendering.md) | 暴风雪与白幕的空间状态、每玩家天气同步、降水/雾/粒子/声音渲染及优化边界 | Current |
| [player-temperature.md](player-temperature.md) | 周围环境采样、玩家分部位体温、衣物、设备、食物、效果与同步 | Current |
| [heat-production-and-network.md](heat-production-and-network.md) | 能量塔、热网、散热器、蒸汽喷泉及现有 heat/power 语义 | Current |
| [data-lifecycle-and-integration.md](data-lifecycle-and-integration.md) | 数据入口、能力、持久化、网络、配置、命令、消费者与验证 | Current |

Primary anchors: `WorldClimate`, `WhiteCurtainInfo`, `ServerLevelMixin_WeatherCycle`, `LevelRendererMixin`, `FogModification`, `WorldClockSource`, `WorldTemperature`, `BlockTemperatureModel`, `MinecraftThermalInput`, `MinecraftPhysicalSourceManager`, `SurroundingTemperatureSimulator`, `TemperatureUpdate`, `PlayerTemperatureData`, `GeneratorData`, `HeatEndpoint`, `HeatNetwork`, `FHConfig.SERVER.CLIMATE`, `FHConfig.SERVER.SIMULATION`.

## 系统地图

```text
dimension + biome + altitude + WorldClimate
                    |
                    v
            natural temperature
                    |
       sparse mesh publication (when present)
                    |
       analytic control fields (Boss/admin)
                    |
                    +--> blocks, crops, transitions, town
                    |
                    +--> receiver-local weather/surface/radiation/body
                                      |
                                      v
                           five body-part temperatures -> HUD

Campfire / Generator / fountain --> physical source manager --> mesh + radiation
GeneratorData --> HeatEndpoint --> HeatNetwork --> device buffers
```

这是几套相邻但不统一的数值模型：

| 名称 | 当前语义 | 单位/基准 |
|---|---|---|
| 气候温度 `C` | 长期事件对背景温度的修正 | `degC` 增量 |
| 世界空气/方块温度 | 维度、群系、海拔、气候和热区合成后的查询值 | 绝对 `degC` |
| `AnalyticField.temperatureC` | Boss/脚本/管理员的非守恒控制温度或增量 | `degC`；不复制到区块 |
| physical source `P` | Campfire、Generator、喷泉注入 mesh/radiation 的显式功率 | `W`，由设备 profile 映射 |
| `BlockTempData.temperature` | 玩家周围粒子采样中的方块热/冷贡献 | 温度增量 |
| 玩家部位体温 | 相对 `37degC` 的偏移，`0` 表示正常体温 | `degC` 偏移 |
| 玩家环境温度/体感温度 | `PlayerTemperatureData` 对外保存和显示的值 | 绝对 `degC` |
| `HeatEndpoint.heat` / `GeneratorData.power` | 热网端点中的可存取标量缓冲 | 任意 heat unit，不是 SI 功率 |
| `HeatEndpoint.tempLevel` | 供热设备传递给消费者的等级 | 无量纲 |

## 当前物理边界

当前新热学 runtime 已对 admitted Page 内的空气、物理 source 与受热物态转换执行能量记账。旧 `ChunkHeatData` 已删除；热网库存和大部分材料仍不是统一 SI 模型。`PlantTempData.heat_capacity` 和 `StateTransitionData.heatCapacity` 原始语义仍是随机状态变化的等待因子，不是热容；热侧 profile 只把后者作为相对能量倍率。

`SurroundingTemperatureSimulator` 会让热源对向下运动的采样轨迹权重较低、冷源对向上运动的轨迹权重较低，以近似“热上升、冷下沉”；它不保存流体速度、密度或热量，也不在方块间推进对流状态。因此后续研究对流、热容或功率时，应以这些现有玩法输出作为兼容边界，而不是把现有同名字段直接解释为物理量。

当前玩家路径由 `MinecraftThermalInput.gameplayPlayerEnvironment` 的 Page publication 与有界直接辐射查询驱动原有体温和 HUD 下游；publication miss 使用 natural backend，再叠加 analytic field。作物和住宅/狩猎建筑使用同一 compositor，miss 或 town partial coverage 时回退 natural composition，不读取旧热区。普通机器没有现存温度消费者。admitted Page 中具有保守材料接触面的 `StateTransitionData` 热侧转换由 phase reservoir 接管；Page 外、无材料 mask、动态形状及冻结/凝结方向继续使用随机转换。

## 阅读顺序

先读世界气候与温度公式；准备优化暴风雪或白幕画面时读天气渲染文档，再读玩家体温。需要研究供热设备或功率时读热网文档；准备修改配置、持久化、数据包或性能路径时再读数据与生命周期文档。

城镇建筑如何抽样和使用方块温度见 [town/town-model.md](../town/town-model.md)。Curiosity 的冷场由 `AnalyticField` 的 `ADD_DELTA` 模式实现，不进入物理能量账本；物理热源先形成局部 mesh 温度，再叠加冷场增量，因此仍能反制。
