# Climate And Temperature Documentation

这里归档 Frosted Heart 当前已实现的气候、世界温度、局部热区、玩家体温与热网行为。源码、服务端配置、数据包配方和存档数据仍是最终权威。

| Document | Scope | Status |
|---|---|---|
| [world-climate-and-temperature.md](world-climate-and-temperature.md) | 逻辑气候时钟、事件轨道、世界/空气/方块温度公式、局部热区 | Current |
| [thermal-runtime-architecture-and-optimization.md](thermal-runtime-architecture-and-optimization.md) | 新 thermal runtime 的所有权、Page/arena/topology/source/solver/publication 生命周期、复杂度与优化边界 | Current |
| [weather-rendering.md](weather-rendering.md) | 暴风雪与白幕的空间状态、天气同步、降水/雾/粒子/声音渲染 | Current |
| [player-temperature.md](player-temperature.md) | 玩家环境查询、分部位体温、衣物、效果与同步 | Current |
| [heat-production-and-network.md](heat-production-and-network.md) | 物理 source、worker 能量、材料/phase 与独立热网 | Current |
| [data-lifecycle-and-integration.md](data-lifecycle-and-integration.md) | 配方、能力、持久化、服务端生命周期、网络与消费者 | Current |

Primary anchors: `WorldClimate`, `WorldTemperature`, `TemperatureUpdate`, `PlayerTemperatureData`, `MinecraftThermalInput`, `MinecraftPageManager`, `PhysicalSourceSpatialIndex`, `ThermalDimensionEngine`, `ThermalSolver`, `ThermalSourceLedger`, `GeneratorData`, `HeatEndpoint`, `HeatNetwork`, `FHConfig.SERVER.CLIMATE`, `FHConfig.SERVER.SIMULATION`.

## 系统地图

```text
dimension + biome + altitude + WorldClimate
                    |
                    v
            natural temperature
                    |
       20-tick thermal Page cut
                    |
       Page-local Air/material solver
                    |
       analytic fields + direct radiation
                    |
              players / crops / town / phase

Campfire / Generator / Radiator / Fountain -> source ledger -> Page cells + radiation
GeneratorData -> HeatEndpoint -> HeatNetwork -> device buffers
```

气候、世界温度、analytic field、physical source、热网 heat unit 与玩家体温
是不同模型。它们的单位和转换不能因为字段名称相似而混用；物理 source
只能通过 `ThermalSourceLedger` 进入 Page cell 能量。

## 阅读顺序

先读 [world-climate-and-temperature.md](world-climate-and-temperature.md) 的
公式，再读 [thermal-runtime-architecture-and-optimization.md](thermal-runtime-architecture-and-optimization.md)
的线程/生命周期和复杂度。修改玩家体温读 [player-temperature.md](player-temperature.md)，
修改机器或热网读 [heat-production-and-network.md](heat-production-and-network.md)，
修改配方、配置、持久化、网络或命令读 [data-lifecycle-and-integration.md](data-lifecycle-and-integration.md)。

城镇建筑的 weighted representative 查询见 [../town/town-model.md](../town/town-model.md)。
