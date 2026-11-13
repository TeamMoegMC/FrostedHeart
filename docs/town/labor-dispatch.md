# 劳动力派遣机构

状态：已实现，专用服务器 GameTest 已通过；客户端交互待验证。最后核对：2026-11-13。
范围：城镇基础工人与 Labor 池。代码锚点：`LaborDispatchBuilding`、`TeamTownData.tick`、`MachineLevelManager.setExtraProviderValue`。

建筑方块为 `frostedheart:labor_dispatch`，存档类型为 `laborDispatch`。使用既有城镇人员分配机制；有效封闭房间须有门，地板面积至少 9 格、内部体积至少 18 格，每 4 格地板提供一个工位（向下取整）。工作资格沿用 `ITownResidentWorkBuilding.canResidentWork`。

每名符合资格的在岗居民提供 1 点 `LaborTypes.WORKER`。服务器城镇 tick 汇总所有有效派遣机构，替换该队伍的额外 worker 提供量，不随每日结算累加；无有效机构时归零。此额外 worker 来源由城镇统一管理，方块提供者不受影响。建筑失效、移除、居民资格变化后在下一次城镇 tick 更新。读档后亦重新计算。

右键界面显示可用基础工人和工位；市长印章可用于城镇人员分配。建筑暂用货运站核心贴图，无新增合成配方。

## 验证

2026-11-13：`LaborDispatchGameTests.supplyPersistenceAndWithdrawal` 在真实 Forge GameTest 服务器通过（1/1）。覆盖核心方块实体注册、合格居民计数、建筑 Codec 和 `ITownBuilding.BASE_CODEC` 往返、居民健康失效、建筑失效、城镇 tick 清除旧供给、真实拆除核心注销建筑。命令：`bash gradlew --offline runGameTestServer -PgameTestNamespaces=frostedheart_labor_dispatch -PgameTestDirectory=build/labor-dispatch-gametest --console=plain`。

本用例直接构造有效建筑状态，未覆盖完整房间扫描、每日自动分配、非零工人池到机器等级的完整链路、客户端界面或独立进程重启读档。

生成资源：`FHBlocks.LABOR_DISPATCH` 显式使用 `FHBlockStateGen.simpleCubeAll("transport_station")`，使 datagen 与运行时一致复用货运站贴图。2026-11-13 完整离线 `runData` 验证成功。
