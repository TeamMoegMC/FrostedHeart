# 劳动力派遣机构

状态：已实现，专用服务器 GameTest 已通过；客户端交互待验证。最后核对：2026-11-13。
范围：城镇基础工人与 Labor 池。代码锚点：`LaborDispatchBuilding`、`TeamTownData.tick`、`MachineLevelManager.setExtraProviderValue`。

## 系统边界与数据流

劳动力派遣把城镇居民岗位转换为队伍的机器劳工供给。居民仍由城镇负责住房、营养、健康和每日岗位分配；机器端只得到按类型计数的点数，不直接持有居民，也不为每台机器再分配一名居民实体。

数据流为：`TownStaffingPlan` / `TownAssignmentModel` 分配居民 → `LaborDispatchBuilding.availableWorkers` 统计 → `TeamTownData.tick` 汇总 → `FHSpecialDataTypes.LABOUR_DATA` 的 `MachineLevelManager` → 各机器的实际劳工等级。城镇岗位队列决定谁进入派遣机构，机器期望等级决定已形成的点数如何使用；这是两个不同的分配阶段。城镇通用岗位保障与剩余人力规则见 [town-model.md](town-model.md)。

`LaborTypes.WORKER` 的注册名是 `worker`。点数表示当前可占用的劳动力容量，不是每日产出的库存，也不是可累积的人天。派遣建筑的 `work` 返回 `false`，供给由城镇 tick 刷新，不通过每日生产函数发放。

## 建筑和居民资格

除下述房间面积和体积外，建筑必须已初始化、结构有效、占用空间不重叠，并取得 `connectivityWorkable`。道路资格见 [building-data.md](building-data.md#道路连接与能量塔2026-11-13)：连塔集群优先，没有连塔集群时使用最大集群回退；不是无塔就一定无法派遣。扫描通过 `setRoadEntrances(room.doors)` 提供门口数据。

默认居民资格由 `ITownResidentWorkBuilding.canResidentWork` 调用 `ResidentDailyModel.canWork` 判断：年龄阶段值至少 `1`、健康严格大于 `10`、精神严格大于 `5`，且已有住房。这些是服务器居民规则的默认值；年龄值是阶段编码，不是周岁。派遣建筑没有额外的教育或熟练度门槛，适配分数恒为 `1`，不会把健康较高或熟练度较高的居民折算成更多点数。

统计对象是建筑 `residentsID` 对应的现存合格居民，不是城镇总人口或空余工位。正常岗位调度负责容量约束；`availableWorkers` 本身不再按工位数截断一次。右键界面展示的是这栋建筑的合格供给和工位，不是全队机器分配后的剩余 worker 池。

## 队伍池与机器分配

对任一劳工类型，`总供给 = 方块提供者合计 + extraProvider`；`剩余 = 总供给 - 已分配消耗`。派遣机构只写 `worker` 类型的 `extraProvider`，不会替换方块提供者或其他类型。多个有效机构先相加，再调用一次 `setExtraProviderValue`；相同数值直接返回，不触发池重分配。

`MachineType.LaborCost` 定义每种类型的最大等级和每级成本，累计消耗为 `min(level, maxLevel) × costPerLevel`。`MachineData` 分开保存期望等级和实际等级；每种类型独立分配，总实际等级是各类型实际等级之和。

- 供给下降：`reducePool` 优先对占用较大的机器逐级降级，直到占用不超过总池。
- 供给上升：`increasePool` 按各机器的期望消耗比例分配预算，再映射到可承担的整数等级；取整后可能剩余点数，不保证所有点数立即用尽。
- 提高期望等级但点数不足：`setDesiredLevel` 返回 `false`，期望仍被保存，实际等级维持原值并进入缺乏集合。降低期望或释放机器时，释放点数可由 `distributeSurplus` 随机补给缺乏机器。
- 机器卸载不释放持久记录中的占用；真正拆除并调用 `releaseMachine` 才归还点数。派遣居民失去资格可能导致机器实际等级下降，但不删除机器的期望。

例如：两栋派遣机构分别有 3、2 名合格在岗居民，额外供给为 5。若还有方块提供者提供 2 点，全队 worker 总池为 7；这不意味着每日新增 7 点，也不意味着每台机器都达到 7 级。

## 刷新、存档与扩展约束

城镇 tick 先推进连通性，再统计派遣供给。健康、精神、住房、建筑有效性或人员变化在下一次城镇 tick 统计时体现；道路变化仍受连通快照更新周期影响，不能承诺方块改变当 tick 就撤销供给。

`LaborDispatchBuilding.CODEC` 保存位置、初始化/结构状态、占用体积、居民 UUID、面积、体积和最大工位；建筑类型同时注册到持久化与管理视图 Codec。道路资格由运行时重建。`MachineLevelManager.CODEC` 保存 `machines`、`generators`，不保存派遣 `extraProvider`；读档后的城镇 tick 按当前建筑和居民重新计算，不能把旧 worker 数当作额外永久奖励。

当前整个队伍的额外 `worker` 通道归城镇统一管理：其他系统若向同一通道写值，会被后续城镇统计覆盖。新增非方块供给来源时应先明确汇总归属，不能各自独立覆盖这个单值。后续类型扩展可复用独立类型池，但当前派遣机构只生产基础 `worker`，未实现职业专用池、逐机器绑定居民或通勤到达后才供给的规则。

建筑方块为 `frostedheart:labor_dispatch`，存档类型为 `laborDispatch`。使用既有城镇人员分配机制；有效封闭房间须有门，地板面积至少 9 格、内部体积至少 18 格，每 4 格地板提供一个工位（向下取整）。工作资格沿用 `ITownResidentWorkBuilding.canResidentWork`。

每名符合资格的在岗居民提供 1 点 `LaborTypes.WORKER`。服务器城镇 tick 汇总所有有效派遣机构，替换该队伍的额外 worker 提供量，不随每日结算累加；无有效机构时归零。此额外 worker 来源由城镇统一管理，方块提供者不受影响。建筑失效、移除、居民资格变化后在下一次城镇 tick 更新。读档后亦重新计算。

右键界面显示可用基础工人和工位；市长印章可用于城镇人员分配。建筑暂用货运站核心贴图，无新增合成配方。

## 验证

2026-11-13：`LaborDispatchGameTests.supplyPersistenceAndWithdrawal` 在真实 Forge GameTest 服务器通过（1/1）。覆盖核心方块实体注册、合格居民计数、建筑 Codec 和 `ITownBuilding.BASE_CODEC` 往返、居民健康失效、建筑失效、城镇 tick 清除旧供给、真实拆除核心注销建筑。命令：`bash gradlew --offline runGameTestServer -PgameTestNamespaces=frostedheart_labor_dispatch -PgameTestDirectory=build/labor-dispatch-gametest --console=plain`。

本用例直接构造有效建筑状态，未覆盖完整房间扫描、每日自动分配、非零工人池到机器等级的完整链路、客户端界面或独立进程重启读档。

生成资源：`FHBlocks.LABOR_DISPATCH` 显式使用 `FHBlockStateGen.simpleCubeAll("transport_station")`，使 datagen 与运行时一致复用货运站贴图。2026-11-13 完整离线 `runData` 验证成功。
