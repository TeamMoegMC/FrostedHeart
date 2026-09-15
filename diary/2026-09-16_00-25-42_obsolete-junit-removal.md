# 删除无法直接使用的失配 JUnit

- Time: `2026-09-16 00:25:42 +08:00`
- Author: `Codex; OpenAI GPT-6; primary engineering agent`
- Status: `completed`
- Scope: `用户明确要求不恢复 JUnit，删除无法直接使用的测试；同步温度重构计划和验证入口文档`

## Completed

- 编译确认旧温度几何类、签名解析 API、红外协议与旧城镇构造器失配；删除 31 个相关测试/夹具文件。
- 温度共 20 份：`ThermalTestFixtures`、`ThermalRuntimeTestFixtures`；`ComponentBrickCompilerTest`、`ConservativeAirGeometryTest`、`VoxelShapeUnitBoxAdapterTest`、`ThermalPageTest`、`ThermalCellArenaTest`、`ThermalSignatureTableTest`、`StateStaticThermalResolverTest`、`QueryPublicationTest`、`ThermalDimensionMailboxTest`、`ThermalDimensionEngineTest`、`ThermalTopologyPipelineTest`、`DimensionInputAccumulatorTest`、`ThermalSolverTest`、`ThermalSourceLedgerTest`、`SourceSolverIntegrationTest`、`NodePowerAccumulatorArenaTest`、`MinecraftPhysicalSourceProfileTest`、`InfraredPacketCodecTest`。
- 城镇共 11 份：`TransportStationForecastTest`、`ClientTownDataSourceTest`、`TeamTownActualSaveCodecProbeTest`、`TeamTownDataS2CPacketTest`、`TownResourceUpdatePacketTest`、`TeamTownP2PBindingTest`、`TeamTownTransportReservationTest`、`TeamTownTransportSettlementTest`、`TeamTownWarehouseTopologyTest`、`TownBuildingRemovalTest`、`TownTransportStateTest`。
- 修改[重构计划](../plans/2026-09-16_00-15-34_thermal-readability-maintainability-refactor.md)，取消恢复旧 JUnit 和对应前置要求。原调查 diary 的恢复建议被本次用户决定取代。

## Decisions

- 不迁移失配 JUnit、不恢复旧生产接口、不增加兼容类型或测试排除配置。其余测试源码保留，GameTest 未修改。
- 生产温度架构和城镇行为未改；实际重构仍待实施。后续温度验证以现有 GameTest 和实际运行场景为主。
- Documentation impact: 更新温度架构文档中过时的 16/16 当前验证说明，删除 P2P 文档中的失效测试入口，给货运站历史测试清单标注已删除状态；没有改写旧 diary。

## Validation

- Java 17：`gradlew.bat compileTestJava compileGameTestJava --offline --no-daemon --console=plain -Dnet.minecraftforge.gradle.check.certs=false`，BUILD SUCCESSFUL。
- 搜索确认测试源码不再引用 `ThermalTestFixtures`、`ThermalRuntimeTestFixtures`、`ConservativeAirGeometry`、`ComponentBrickCompiler`、`VoxelShapeUnitBoxAdapter`。
- 未运行剩余 JUnit，不将编译成功表述为测试全部通过。最近 GameTest 的 102/102 是前次热行为修复结果，本次无需重复执行。
- 未产生临时源码、脚本、测试世界或重定向日志文件；使用原 Gradle 编译输出。

## Remaining

- 当前源码中发现的失配编译入口已清理；未调查其余 JUnit 的运行时结果。
- 温度可读性/可维护性重构按更新后的计划实施；本轮已完成检查和准备。
