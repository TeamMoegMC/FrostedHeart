# Climate And Temperature Documentation

这里归档 Frosted Heart 当前已实现的气候、世界温度、局部热区、玩家体温与热网行为。源码、服务端配置、数据包配方和存档数据仍是最终权威。

| Document | Scope | Status |
|---|---|---|
| [world-climate-and-temperature.md](world-climate-and-temperature.md) | 逻辑气候时钟、事件轨道、世界/空气/方块温度公式、局部热区 | Current |
| [weather-rendering.md](weather-rendering.md) | 暴风雪与白幕的空间状态、每玩家天气同步、降水/雾/粒子/声音渲染及优化边界 | Current |
| [player-temperature.md](player-temperature.md) | 周围环境采样、玩家分部位体温、衣物、设备、食物、效果与同步 | Current |
| [heat-production-and-network.md](heat-production-and-network.md) | 能量塔、热网、散热器、蒸汽喷泉及现有 heat/power 语义 | Current |
| [data-lifecycle-and-integration.md](data-lifecycle-and-integration.md) | 数据入口、能力、持久化、网络、配置、命令、消费者与验证 | Current |

Primary anchors: `WorldClimate`, `WhiteCurtainInfo`, `ServerLevelMixin_WeatherCycle`, `LevelRendererMixin`, `FogModification`, `WorldClockSource`, `WorldTemperature`, `BlockTemperatureModel`, `ChunkHeatData`, `SurroundingTemperatureSimulator`, `TemperatureUpdate`, `PlayerTemperatureData`, `GeneratorData`, `HeatEndpoint`, `HeatNetwork`, `FHConfig.SERVER.CLIMATE`, `FHConfig.SERVER.SIMULATION`.

## 系统地图

```text
WorldClimate hourly climate C and weather type
             |
dimension D + biome B + altitude A + local heat-field H
             |
             +--> WorldTemperature.block --> crops, fluids, block transitions, town buildings
             |
             +--> WorldTemperature.air
                      + BlockTempData particle sample
                      + day/night and Vanilla weather overrides
                               |
                               v
                    player environment offset
                               |
              clothes + wind + medium + equipment
                               |
                               v
                  five body-part temperatures
                               |
                    status effects, HUD, food/water costs

GeneratorData / radiator / fountain --> ChunkHeatData heat areas --> H
GeneratorData --> HeatEndpoint --> HeatNetwork --> radiator/fountain buffers
```

这是几套相邻但不统一的数值模型：

| 名称 | 当前语义 | 单位/基准 |
|---|---|---|
| 气候温度 `C` | 长期事件对背景温度的修正 | `degC` 增量 |
| 世界空气/方块温度 | 维度、群系、海拔、气候和热区合成后的查询值 | 绝对 `degC` |
| `ChunkHeatData` 热区值 `H` | 恒定空间场的温度控制值；重叠时取最大正值 | 代码按 `degC` 使用，但不是能量 |
| `BlockTempData.temperature` | 玩家周围粒子采样中的方块热/冷贡献 | 温度增量 |
| 玩家部位体温 | 相对 `37degC` 的偏移，`0` 表示正常体温 | `degC` 偏移 |
| 玩家环境温度/体感温度 | `PlayerTemperatureData` 对外保存和显示的值 | 绝对 `degC` |
| `HeatEndpoint.heat` / `GeneratorData.power` | 热网端点中的可存取标量缓冲 | 任意 heat unit，不是 SI 功率 |
| `HeatEndpoint.tempLevel` | 供热设备传递给消费者的等级 | 无量纲 |

## 当前物理边界

当前新热学 runtime 已对 admitted Page 内的空气、物理 source 与受热物态转换执行能量记账，但旧热网、热区和大部分材料仍不是统一 SI 模型。`PlantTempData.heat_capacity` 和 `StateTransitionData.heatCapacity` 原始语义仍是随机状态变化的等待因子，不是热容；热侧 profile 只把后者作为相对能量倍率。

`SurroundingTemperatureSimulator` 会让热源对向下运动的采样轨迹权重较低、冷源对向上运动的轨迹权重较低，以近似“热上升、冷下沉”；它不保存流体速度、密度或热量，也不在方块间推进对流状态。因此后续研究对流、热容或功率时，应以这些现有玩法输出作为兼容边界，而不是把现有同名字段直接解释为物理量。

当前玩家实机测试路径已暂时停止调度这套旧周边方块采样，改由 `MinecraftThermalInput.gameplayPlayerEnvironment` 的 Page publication 与有界直接辐射查询驱动原有体温和 HUD 下游；旧空气公式只在首个 publication 未完成或 query 明确 miss 时兜底。作物和住宅/狩猎建筑也会在已有 publication 完整命中时使用新空气温度，miss 或 town partial coverage 时整体回退原方块温度。普通机器没有现存温度消费者。admitted Page 中具有保守材料接触面的 `StateTransitionData` 热侧转换由 phase reservoir 接管；Page 外、无材料 mask、动态形状及冻结/凝结方向继续使用旧随机转换。

## 阅读顺序

先读世界气候与温度公式；准备优化暴风雪或白幕画面时读天气渲染文档，再读玩家体温。需要研究供热设备或功率时读热网文档；准备修改配置、持久化、数据包或性能路径时再读数据与生命周期文档。

城镇建筑如何抽样和使用方块温度见 [town/town-model.md](../town/town-model.md)。Curiosity 的相关材料状态为 Transitional，涉及负热区的叙述必须以本目录记录的实际 `ChunkHeatData` 聚合规则为准。
