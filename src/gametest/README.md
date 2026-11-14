# Forge GameTest 验证

这里保存Frosted Heart的独立测试代码、数值参照、真实服务端用例和测试资源。普通生产JAR不包含GameTest源集。当前源码与[热运行合同](../../docs/climate/thermal-runtime-control.md)是行为依据；逐轮结果及边界由[生产退役回执](../../docs/climate/validation/thermal-cell-enthalpy-validation-2026-11-14.json#/records/thermal-production-runtime-retirement-2026-11-14)记录。

## 从哪里开始

- `thermal-runtime-namespaces.txt`：当前固定温场、源输入、几何、查询、保存、故障恢复、材料和独立图构建对照的统一命名空间列表。
- `TiledThermalFieldGameTests`：单元值/能量、当前位置查询和v7存储；包含明确忽略旧v6数据的检查。
- `ContinuousAirWorkerRecoveryGameTests`：六个实际worker故障点的原生存盘读回，以及替代场仍供暖时的旧场回收。
- `ThermalIndependentProgressGameTests`：无关基地、未来控制、首次捕获与原生重入的真实边界。
- `ContinuousAirConcurrentSaveGameTests`、`ThermalCommittedSaveGameTests`：Working不可见、Claim/pending归属及磁盘读回。
- `ThermalMaterialBehaviorGameTests`：当前固定引擎中的材料焓、相变、状态身份与保存。
- `PairwiseGraphSortGameTests`：仅供独立数值参照的图构建与TreeMap对照。相关Q1算子、材料边界和快照类型位于GameTest，不构成生产后端。

旧区域调度器专属的控制与性能对照入口已退役；历史测量继续保留在docs/climate/validation及diary中。它们不能作为当前生产资格。

## 最小运行方式

在仓库根目录使用Java17：

```powershell
$env:JAVA_HOME = 'C:/Program Files/Java/jdk-17'
& .\gradlew.bat --no-daemon -PgameTestNamespaces=frostedheart_tiled_thermal runGameTestServer
```

统一命名空间可从本目录的列表读取，并作为单个参数传入：

```powershell
$thermalNamespaces = (Get-Content src/gametest/thermal-runtime-namespaces.txt -Raw).Trim()
& .\gradlew.bat --no-daemon "-PgameTestNamespaces=$thermalNamespaces" runGameTestServer
```

任务默认使用独立的`run-gametest`目录并重建测试世界。`catalog/`是研究数据原料，`resources/`是结构、Mixin和其他测试资源；运行目录、日志与剖析文件保留在生成目录中。

`ProductionObservationBridge`只在测试启动时注册当前固定worker与原生保存的观察器。`PrivateSaveObservationMixin`只用于功能GameTest，观察真实私有推进到原提交之前的间隔；正式JAR和网络负载测试不加载它。

`Scripts/thermal-network-load.init.gradle`及`ThermalNetworkLoadServer`提供100个实际FML3连接的独立负载入口。旧Q1工厂替换Mixin和后端选择参数已删除。当前性能优化暂停，短功能通过不等于百人长期性能通过。
