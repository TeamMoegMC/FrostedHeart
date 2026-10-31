# Climate And Temperature Documentation

这里归档 Frosted Heart 当前已实现的气候、世界温度、局部热区、玩家体温与热网行为。源码、服务端配置、数据包配方和存档数据仍是最终权威。

| Document | Scope | Status |
|---|---|---|
| [thermal-runtime-control.md](thermal-runtime-control.md) | 当前SHARED_GRID输入、任务、准入、粗细切换、查询、Minecraft接入归属和v6保存合同 | Current; validation scope and deferred work recorded in the linked document |
| [thermal-storage-optimization.md](thermal-storage-optimization.md) | 局部容量摘要、Debug盘点、热活动维护、无损存储及同配置CPU/分配/服务代价 | Current; control refactor, focused lifecycle checks and per-build performance/resource limits recorded |
| [thermal-regional-server-validation.md](thermal-regional-server-validation.md) | 当前区域架构的100温度消费者分散负载、资源代价、粗档全程构建与部署配置 | Current for stated multicore and single-worker measurements; full networked server scope stated separately |
| [world-climate-and-temperature.md](world-climate-and-temperature.md) | 逻辑气候时钟、事件轨道、世界/空气/方块温度公式、局部热区 | Current |
| [thermal-runtime-architecture-and-optimization.md](thermal-runtime-architecture-and-optimization.md) | 运行时组成及按日期保留的架构/性能记录 | Transitional; current control contract linked above |
| [continuous-air-runtime.md](continuous-air-runtime.md) | 区域空气场、材料成对换热、精确位置查询、空间存档及带日期的历史试验 | Partial; current control linked separately |
| [continuous-air-overload.md](continuous-air-overload.md) | 当前过载协议入口及按日期保留的粗细/容量试验 | Transitional; current control contract linked above |
| [continuous-air-architecture-comparison.md](continuous-air-architecture-comparison.md) | 两层求解与共享单格网格的真实Forge架构对照、收益和容量失败 | Current for recorded trials |
| [continuous-air-large-base-optimization.md](continuous-air-large-base-optimization.md) | 两层128源基地、10分钟生命周期与100个玩家环境查询对象的生产结果 | Current for recorded trials |
| [continuous-air-rebuild-compression.md](continuous-air-rebuild-compression.md) | 重建分阶段计时、组件表面缓存与解析积分、实际256源结果和剩余发布缺口 | Current for recorded trials |
| [continuous-air-local-preparation.md](continuous-air-local-preparation.md) | 函数筛选缓存、实际halo位置失效、构建精度与分块因子试验 | Current for recorded trials |
| [continuous-air-regional-preparation.md](continuous-air-regional-preparation.md) | 持久区域、单一推进循环、普通/构建期间输入服务、局部构建与一次供能确认 | Current for recorded regional trials; CPU/full U1 and default gates pending |
| [continuous-air-entry-recovery.md](continuous-air-entry-recovery.md) | 新热源空间准备、空发现队列过滤、实际客户端进入恢复、温差与计时 | Current for selected implementation and real-client entry acceptance; general multiplayer/default acceptance partial |
| [continuous-air-persistence.md](continuous-air-persistence.md) | 普通保存配置、区块温度NBT、原版卸载队列、真实读盘/CT2与保存边界 | Current for implementation and recorded normal-config validation; broader capacity partial |
| [thin-ice.md](thin-ice.md) | 薄冰踩踏、冲击、相变温度、热裂与回冻修复 | Partial |
| [icicle.md](icicle.md) | 冰洞冰锥、生长、相变缩短、断裂坠落与振动 | Partial |
| [eave-icicles.md](eave-icicles.md) | 房檐冰溜子、供水、轻微坠落伤害，以及冰锥/冰溜子融化滴水 | Partial |
| [hot-spring.md](hot-spring.md) | 温泉液体、接触换热、湿身与采集限制 | Partial |
| [ice-cave-blocks.md](ice-cave-blocks.md) | 冰晶、荧石晶体、软锰矿与装饰热液岩 | Partial |
| [weather-rendering.md](weather-rendering.md) | 暴风雪与白幕的空间状态、天气同步、降水/雾/粒子/声音渲染 | Current |
| [player-temperature.md](player-temperature.md) | 玩家环境查询、分部位体温、衣物、效果与同步 | Current |
| [heat-production-and-network.md](heat-production-and-network.md) | 物理 source、worker 能量、材料/phase 与独立热网 | Transitional |
| [data-lifecycle-and-integration.md](data-lifecycle-and-integration.md) | 配方、能力、持久化、服务端生命周期、网络与消费者 | Transitional |

理解当前热系统先读[运行时控制说明](thermal-runtime-control.md)，公式与单位读[世界气候与温度](world-climate-and-temperature.md)。玩家体温、机器/热网和持久化分别从表中对应文档进入；城镇温度消费者见[城镇模型](../town/town-model.md)。按日期保留的试验只证明其对应版本，不能作为当前资格。

修改前核对对应源码、配置和当前文档；行为或合同变化时同步维护拥有该规则的文档，并按[仓库约定](../../AGENTS.md)新增开发日记。具体实现步骤放在[plans](../../plans/README.md)，本README只提供入口。
