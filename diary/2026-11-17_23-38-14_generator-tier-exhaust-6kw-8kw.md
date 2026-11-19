# T1/T2 四向排热固定为每口6kW与8kW

- Time: `2026-11-17 23:38:14 +08:00`
- Author: `OpenAI Codex`
- Status: `completed; compilation and 14 focused Forge GameTests passed`
- Scope: `GeneratorLogic.tickFuel`、`MinecraftPhysicalSourceProfile.GENERATOR/GENERATOR_T2`、`MinecraftThermalInput.onGeneratorTick`、`PhysicalSourceSpatialIndex.nearestEnabledGenerator`
- Follows: [四向出口实现](2026-11-17_22-21-49_generator-four-horizontal-outlets.md)、[Port记录的历史核对](2026-11-17_22-29-50_generator-port-history-internal-heat.md)

## Completed And Decisions

- 按用户最终要求，T1的四个排热口各固定6,000W，对流合计24,000W；T2各固定8,000W，对流合计32,000W。两种塔的辐射功率均固定2,000W，总功率分别为26,000W和34,000W。
- 总功率以 `4 * airPowerPerOutletW + 2000` 计算；每条 `AIR_FACE` 的 `powerShare = airPowerPerOutletW / totalPowerW`，`RADIATION_LOSS` 的份额为 `2000 / totalPowerW`。辐射功率按绝对W数定义，不再按旧20%比例随对流一起放大。
- 出口位置仍为底座中心相对 `(2,2,0)`、`(-2,2,0)`、`(0,2,-2)`、`(0,2,2)`，单位为方块。每塔仍是一个物理源。
- T1沿用 `GENERATOR` / profile ID2；T2使用 `GENERATOR_T2` / profile ID5。两个不可变定义在启动时通过同一 `generator` 工厂建立，复用同一几何和绑定流程。
- `GeneratorLogic.tickFuel` 按实际多方块类型选择profile，并在温度等级为正时传入功率倍率1，否则为0；实际启停继续由active状态控制。物理功率不会随正温度等级倍增，等级仍用于原解析保底。
- `byId` 增加T2映射，`nearestEnabledGenerator` 同时识别两个profile ID，保证T2继续被现有能量塔查询发现。
- 复用现有五条 `EmissionPort` 容量及四目标绑定；没有新增每tick的profile对象或独立求解器。中心额外5°C的解析场仍使用原公式。

## Validation

- 最终执行：`./gradlew.bat --offline runGameTestServer '-PgameTestNamespaces=frostedheart_generator_heat,frostedheart_source_admission' -PgameTestDirectory=build/generator-center-boost-gametest`。
- 生产代码及GameTest编译通过，14/14项通过，`BUILD SUCCESSFUL in 2m 43s`。
- 真实T1、T2分别在温度等级10和1时读取已提交到生产者的 `offeredPowerW`；验证总功率26,000/34,000W、四条Air记录各6,000/8,000W、辐射2,000W及两个profile均能被能量塔查询发现。
- 四侧未合成解析场的物理Air均实际升温；保留跨Chunk目标、堵住东侧而其余三侧可用、重开后恢复、配方重载恢复、源启停、拆除与燃料不重复消耗检查。
- 现有中心5°C、人物/作物/城镇/物品/相变、红外、团队权威、卸载及source admission预算/取消释放检查继续通过。
- 相变拓扑反复变化的受控输入场景显式传入T1 profile；其测试输入倍率独立于真实机器的固定倍率，不代表真实塔按温度等级放大功率。
- `git diff --check` 通过。最终日志保留在忽略的 `build/generator-tiers-fixed-exhaust.log`。

## Documentation Impact

- `docs/climate/heat-production-and-network.md` 更新每级塔的绝对功率、分配公式、profile ID及正常机器倍率。
- `docs/climate/world-climate-and-temperature.md` 更新T1/T2出口功率与固定辐射。
- `src/gametest/README.md` 补充不同温度等级下的功率检查。
- 此前日记中的10,000W乘等级及80/20分配属于历史阶段；当前以本条和源码为准。

## Remaining

- 本轮未做客户端画面或整服性能对照；验证范围为上述真实Forge服务端场景。
