# 货运站（Transport Station）设计与实施计划

> 文档状态：当前；货运站生产和首个仓库接口运力消费者均已实现，本文描述现行行为。
>
> 最近验证：2026-08-22。
>
> 范围：货运站建筑、城镇运力生产、持久化与后续实施计划。
>
> 代码锚点：`TransportStationBuilding`、`TransportStationDailyModel`、`TownTransportCapacityModel`、
> `TownModelParameters.TransportStationParameters`、`TownTransportState`、
> `TeamTownData#buildingsWork`、`VirtualResourceType.TRANSPORT_CAPACITY`、`WarehouseInterfaceBlockEntity`、
> `WarehouseInterfaceMenu`、`TownTransportSnapshot`、`TownVirtualResourcesPanel`。
>
> 固定命名：Java 类前缀 `TransportStation`；注册名 `transport_station`；英文显示名
> `Transport Station`；中文显示名 `货运站`。

## 1. 结论

货运站应当作为一个标准的城镇居民工作建筑接入现有系统，并分两个阶段实施：

1. **建筑基础阶段**：完成方块、方块实体、建筑数据、结构扫描、岗位分配、持久化、界面和资源文件，
   但不生产运力。此阶段先验证建筑生命周期，不提前锁定尚未确定的数值规则。
2. **城镇生产阶段**：由驻站居民在每日城镇结算时生产
   `VirtualResourceType.TRANSPORT_CAPACITY`，补齐产出公式、配置、熟练度和日报。

当前设计只覆盖城镇建筑与城镇虚拟资源。KHJ 物流方案已暂时搁置，不纳入本建筑的职责、接口、实施计划或
验收范围。

## 2. 现有代码依据

本设计主要沿用以下现有实现：

- `HuntingBase*`：居民工作建筑、封闭空间扫描、Menu/Screen、生产日报的完整参考。
- `MineBase*`：基于有效面积计算岗位容量的参考。
- `AbstractTownBuildingBlockEntity`：注册、客户端城镇快照、定时刷新、拆除注销和缺失建筑恢复。
- `TownStaffingPlan` + `TownAssignmentModel`：当前实际岗位分配入口。旧的
  `getResidentPriority()` 已废弃，不应为货运站重新实现一套优先级。
- `ITownBuilding.CODEC`：建筑多态持久化入口，新建筑必须登记稳定的类型键。
- `TownBuildingsPanel`：镇长印章中的建筑列表和详情。
- `VirtualResourceType.TRANSPORT_CAPACITY`：已预留的城镇运力资源。

## 3. 命名与标识

### 3.1 固定名称

| 用途 | 值 |
|---|---|
| Java 类前缀 | `TransportStation` |
| 方块、方块实体、菜单注册名 | `frostedheart:transport_station` |
| 英文显示名 | `Transport Station` |
| 中文显示名 | `货运站` |
| 包路径 | `com.teammoeg.frostedheart.content.town.buildings.logistics` |
| 建筑 Codec 类型键 | `transportStation` |

`transport_station` 是 Forge/Minecraft 注册 ID；`transportStation` 是
`ITownBuilding.CODEC` 写入存档的多态类型键。后者沿用现有 `huntingBase`、`mineBase` 约定，首次发布后不得改名。

### 3.2 类清单

| 类 | 基类/接口 | 职责 |
|---|---|---|
| `TransportStationBlock` | `AbstractTownBuildingBlock` + `CEntityBlock<TransportStationBlockEntity>` | 放置、交互、创建方块实体 |
| `TransportStationBlockEntity` | `AbstractTownBuildingBlockEntity<TransportStationBuilding>` + `MenuProvider` | 解析所属城镇、扫描结构、打开菜单 |
| `TransportStationBuilding` | `AbstractTownResidentWorkBuilding` | 可持久化的城镇建筑状态、岗位评分和每日生产 |
| `TransportStationBlockScanner` | `BuildingBlockScanner` | 封闭空间扫描；为后续货运设施统计预留扩展点 |
| `TransportStationMenu` | `CBlockEntityMenu<TransportStationBlockEntity>` | 向界面提供建筑、城镇和居民快照 |
| `TransportStationScreen` | `StandardTownBuildingScreen<TransportStationMenu>` | 概览、员工和生产页 |

首期的 `TransportStationBlockScanner` 可以只包装通用扫描器，但必须保持无额外状态；如果实现时确定长期不会有
货运站专属结构元素，也可以直接使用 `BuildingBlockScanner`，避免保留空抽象。

## 4. 建筑状态设计

### 4.1 首期持久化字段

`TransportStationBuilding.CODEC` 应包含：

| 字段 | 默认值 | 说明 |
|---|---:|---|
| `pos` | `BlockPos.ZERO` | 核心方块位置 |
| `initialized` | `false` | 是否至少完成一次刷新 |
| `occupiedAreaOverlapped` | `false` | 是否与其他城镇建筑内部体积重叠 |
| `isStructureValid` | `false` | 封闭结构扫描是否成功 |
| `occupiedVolume` | `OccupiedVolume.EMPTY` | 用于重叠检测的室内体积 |
| `residentsID` | 空列表 | 当前日岗位名册；解码后转为集合 |
| `area` | `0` | 地板面积 |
| `volume` | `0` | 室内容积 |
| `maxResidents` | `0` | 扫描后计算的岗位上限 |

所有新增 setter 都要先比较旧值，仅在净变化时赋值并调用 `fireChange()`。扫描器会周期刷新，缺少值守卫会让
建筑每次扫描都进入增量同步脏集合。`AbstractTownResidentWorkBuilding#setMaxResidents` 目前本身没有值守卫，
实现本建筑时应一并补上这个共享守卫，并增加回归测试。

`residentsID` 编码时建议按 UUID 排序后输出，以获得稳定的存档和测试结果；解码时仍转为 `HashSet`。

### 4.2 第二阶段字段（已实现）

单站 `dailyReport` 使用带默认值的 Codec 字段，记录：

- 是否已有结算数据；
- 当日有效工人数和总生产力；
- 计划产出与实际加入城镇资源池的运力；
- 停产原因。

新增 Codec 字段必须使用 `optionalFieldOf`，保证第一阶段存档可直接升级。

城镇汇总日报不归属于任意一座 `TransportStationBuilding`。它由城镇级
`TownTransportState` 维护，记录当日城镇总运力、实时仓库接口预约和晨间占用快照。接口登记、比例限速和后续汇总提示见
[`docs/town/implementation-reference.md`](town/implementation-reference.md) 和
[`docs/town/town-model.md`](town/town-model.md)。已完成工作的实施记录保留在
[`plans/2026-08-20_16-53-08_transport-capacity-consumers.md`](../plans/2026-08-20_16-53-08_transport-capacity-consumers.md)。

### 4.3 拆除语义

- 居民岗位清理由 `AbstractTownResidentWorkBuilding#onRemoved` 和
  `TeamTown.removeTownBlock` 的统一路径负责，不在方块类中重复清理。
- 运力按城镇结算日重建。当天拆除货运站不追回已经建立的当日运力，下一次晨间结算不再计入该站产出。
- 方块实体拆除必须继续走 `AbstractTownBuildingBlockEntity#onRemoved`；不能直接修改建筑 Map。

## 5. 结构扫描与岗位

### 5.1 扫描流程

`TransportStationBlockEntity#scanStructure` 建议沿用 `HuntingBaseBlockEntity` 的多门搜索流程：

1. 搜索核心方块相邻的门；
2. 找到门下承重方块；
3. 从门两侧可能的室内地板位置尝试扫描；
4. 使用 `TransportStationBlockScanner` 得到 `area`、`volume`、`occupiedVolume`；
5. 计算岗位上限并一次性回写 Building；
6. 所有入口均失败时返回 `false`。

首期不要添加蒸汽粒子或主动切换 `LIT`。基类虽然自带 `LIT`、`FACING`，但在“工作中”的实时定义明确前，
随意点亮会向玩家表达不存在的运行状态。

### 5.2 空间规则与配置

货运站应拥有独立配置，不能复用狩猎或采矿配置。建议在
`FHConfig.SERVER.TOWN` 下增加 `TransportStation` 配置段，并让默认值来自
`TownModelParameters.Defaults`：

| 参数 | 建议首版默认值 | 用途 |
|---|---:|---|
| `minimumFloorAreaBlocks` | `4` | 小于该面积时不可工作 |
| `minimumInteriorVolumeBlocks` | `8` | 小于该体积时不可工作 |
| `floorBlocksPerWorkerSlot` | `4.0` | 每个岗位需要的有效地板面积 |
| `minimumWorkerSlots` | `1` | 合法结构至少提供的岗位数 |
| `physicalActivity` | `1.0` | 完成工作日后记录的体力活动 |
| `learningActivity` | `0.25` | 完成工作日后记录的学习活动 |

建议岗位公式与现有基地一致：

```text
spaceRating = TownMathFunctions.calculateSpaceRating(volume, area, buildingScoring...)
effectiveFloorArea = spaceRating * area
maxResidents = max(minimumWorkerSlots, floor(effectiveFloorArea / floorBlocksPerWorkerSlot))
```

`isBuildingWorkable()` 在基类条件之外检查最小面积和最小体积。上述默认值是当前货运站的首版配置基线，
并由 `TownModelParameters.Defaults` 与 `FHConfig` 共享。

### 5.3 岗位分配

实现 `ITownResidentWorkBuilding` 后，货运站会被 `TownStaffingPlan#normalize` 自动加入岗位队列。无需实现已废弃的
`getResidentPriority()`；玩家队列顺序和保障人数决定建筑间优先关系，`getResidentScore(Resident)` 只负责在同一
货运站候选居民中排序。

当前 `getResidentScore` 使用独立、可测试的生产力公式，并将运输熟练度键固定为
`TransportStationBuilding.class.getSimpleName()`，与现有 `Resident` 熟练度存储方式一致。

## 6. 每日工作与运力生产

### 6.1 第一阶段（已完成）

建筑基础阶段已完成；生产前的结构检查、岗位名册和 `isBuildingWorkable()` 仍由建筑自身负责。

### 6.2 第二阶段（已完成）

生产逻辑应拆到 Forge 无关的 `TransportStationDailyModel`，Building 只负责收集输入和执行资源 Action。H03
冻结的首版参数为：

| 参数 | 首版值 |
|---|---:|
| 标准工日产出 | `64` 运力/标准工人日 |
| 健康/精神/力量/智力权重 | `35 / 15 / 30 / 20` |
| 属性为 0/100 时的生产力 | `0.5 / 1.5` |
| 最大运输熟练度 | `100` |
| 满熟练度加成 | `0.8` |
| 最低/最高最终生产力 | `0.5 / 2.3` |
| 无工人被动产出 | `0` |

沿用共享熟练度每日成长曲线。权重按总和归一化，当前恰好合计 `100`。生产公式为：

```text
weightedAttribute = health * 0.35 + mental * 0.15 + strength * 0.30 + intelligence * 0.20
residentProductivity = clamp(0.5 + weightedAttribute / 100 + 0.8 * proficiency / 100, 0.5, 2.3)
totalProductivity = sum(所有当日驻站且合格居民的 residentProductivity)
producedCapacity = totalProductivity * 64
```

标准工人（四项属性均为 `50`、运输熟练度为 `0`）每日生产 `64` 运力；满属性且满熟练度时每日生产
`147.2` 运力。参数必须进入 `TownModelParameters.Defaults` 与服务端配置，不能散落在 Building 或 Screen 中。
货运站实际增加运力超过 `TeamTownResourceHolder.DELTA` 时，为当日参与生产的居民记录默认活动向量
`(physical=1.0, learning=0.25)` 并增加运输熟练度；不可工作、无有效员工、零产出或实际增加量为零时两者都不发生。

`TRANSPORT_CAPACITY` 是不消耗仓库容量、不会在运输中扣减、每个城镇结算日重新建立的 service。晨间必须在
任何货运站工作前将其归零，再由所有可工作货运站分别通过以下 Action 加回当日产出：

```java
town.getActionExecutorHandler().execute(
    new TownResourceActions.VirtualResourceAttributeAction(
        VirtualResourceType.TRANSPORT_CAPACITY.generateAttribute(0),
        producedCapacity,
        ResourceActionType.ADD,
        ResourceActionMode.ATTEMPT
    )
);
```

不要直接调用 `TeamTownResourceHolder` 的 unsafe 方法。`VirtualResourceType.TRANSPORT_CAPACITY` 已设为
`isService=true`，并由 `TeamTownData#buildingsWork` 在逐建筑生产前调用 `resetAllServices()`；所有货运站完成
生产后，城镇再写入汇总日报。运力不会跨日囤积，也不存在运输任务扣减。

## 7. 方块、菜单与界面

### 7.1 方块交互

`TransportStationBlock#use` 沿用 `HuntingBaseBlock`：只在服务端处理主手，确认方块实体与建筑映射存在后调用
`refresh_safe`，再用 `NetworkHooks.openScreen` 打开菜单；映射缺失时显示现有错误提示。

`TransportStationBlockEntity` 实现：

- `createBuilding()`；
- `getBuilding(AbstractTownBuilding)` 的类型过滤；
- `scanStructure(TransportStationBuilding)`；
- `createMenu(...)`；
- `getDisplayName()`，翻译键为 `container.frostedheart.transport_station`。

### 7.2 Menu

`TransportStationMenu` 沿用 `HuntingBaseMenu` 的无机器槽布局：

- 菜单类型为 `FHMenuTypes.TRANSPORT_STATION`；
- 提供玩家背包槽；
- `getBuilding()` 从方块实体解析；
- `getTown()` 仅在数据源为 `TeamTown` 时返回；
- `getResidents()` 按 `workPos == blockPos` 过滤，并稳定排序。

### 7.3 Screen

当前提供三个页签：

- **概览**：可工作状态、失败原因、员工数/上限、面积、体积；使用 `TownInfoPanel` 和现有
  `MineBaseScreen` 行构造工具。
- **员工**：使用 `TownWorkforcePanel`，显示真实个人生产力、预计每日运力贡献和熟练度成长。
- **生产**：显示下次结算预测、单站最近结算的计划/实际运力，以及城镇日报中的总运力和已占用运力。

预测由 `TransportStationBuilding#getForecast(TeamTown)` 计算，和实际结算共用人员筛选与
`TransportStationDailyModel` 输入。城镇汇总值读取 `TownTransportState.DailyReport`；该报告的占用量现在来自晨间
结算时的实时预约，但当天调速不会反写历史日报。所有面板通过 Supplier 每帧读取客户端城镇快照；收到增量包时不重建
Screen，因此无需新增重复数据通道。

居民在创建和旧存档解码时由 `Resident#initializeMissingWorkProficiencies` 补齐
`TransportStationBuilding` 熟练度键。因此预测与实际结算读取同一已持久化熟练度，不会在客户端界面首次打开时
随机生成不同值。

### 7.4 镇长印章

货运站已出现在 `TownBuildingsPanel` 和 `TownStaffingPlan` 管理界面。建筑详情显示面积、体积、员工数、单站
上次计划/实际产出、当日城镇总运力、已占用运力和停产原因。

`TownVirtualResourcesTab` 另提供不依赖具体方块位置的城镇级入口。其“运力”详情从
`resources[TRANSPORT_CAPACITY]` 和 `TownTransportState.DailyReport` 显示总运力、已占用、剩余、缺口与有效
传输比例。全量 `TeamTownDataS2CPacket` 和增量 `TownResourceUpdatePacket` 都携带权威
`TownTransportSnapshot`，因此实时占用不会从省略派生值的持久化 Codec 错误重算为 `0`。设备详情位于汇总和晨间报告
之后、默认收起；展开后按稳定端点顺序显示接口坐标、绑定仓库核心、设置/有效速率、距离因子、占用运力和状态。
“仓库容量”详情同时显示总容量、`TeamTownResourceHolder.occupiedCapacity`、剩余、缺口和使用率。

## 8. 注册、资源与本地化

### 8.1 Java 注册

| 文件 | 新增内容 |
|---|---|
| `FHBlocks` | `TRANSPORT_STATION`，注册名 `transport_station`，`.lang("Transport Station")`，加入 `TOWN_BLOCKS` |
| `FHBlockEntityTypes` | `TRANSPORT_STATION`，同名注册并绑定方块 |
| `FHMenuTypes` | `TRANSPORT_STATION`，绑定 `TransportStationBlockEntity` 与 `TransportStationMenu` |
| `FHScreens` | 将菜单注册到 `TransportStationScreen` |
| `ITownBuilding.CODEC` | `.type("transportStation", TransportStationBuilding.class, TransportStationBuilding.CODEC)` |

本期不新增网络包。若后续确需专用包，必须同时在 `FHNetwork` 注册并限制正确方向。

### 8.2 客户端与数据资源

至少需要：

- `src/main/resources/assets/frostedheart/textures/block/transport_station.png`；
- 数据生成后的 blockstate、block model、item model；
- 方块 loot table；
- `TOWN_BLOCKS` tag 条目；
- 如需要创造模式获取，确认现有 Registrate item 注册会将其放入预期页签。

执行 `runData` 后只提交与货运站相关的生成文件，避免把无关数据生成差异带入本任务。

### 8.3 翻译键

中英文语言文件都应显式补齐，不能只依赖 Registrate 生成方块英文名：

```text
block.frostedheart.transport_station
container.frostedheart.transport_station
gui.frostedheart.town_manager.building.TransportStationBuilding
gui.frostedheart.transport_station.overview
gui.frostedheart.transport_station.workers
gui.frostedheart.transport_station.transport_proficiency
```

production、forecast、produced capacity、stop reason 等键已加入。英文值使用本需求指定的
`Transport Station`；中文核心名称统一使用“货运站”。

## 9. 兼容性与风险

1. **Codec 键是存档协议**：`transportStation` 发布后不可随意改名；新增字段必须有默认值。
2. **注册 ID 必须全链路一致**：Block、BlockEntityType、MenuType、模型、掉落和翻译均使用
   `transport_station`。
3. **周期扫描不能空转同步**：所有扫描回写字段和 `maxResidents` 必须有净变化守卫。
4. **岗位系统不要走旧接口**：实现 `getResidentScore`，但不实现已废弃的 `getResidentPriority`。
5. **结构失效不应保留可用岗位**：当日规划以 `isBuildingWorkable` 为准；界面仍可显示扫描得到的物理上限。
6. **资源素材是完成条件**：只有 Java 注册而没有纹理、模型和 loot table 不算可交付建筑。
7. **运力是城镇 service，不是建筑库存**：先统一归零、再执行全部货运站生产、最后生成城镇汇总日报；不能让
   单座建筑按迭代顺序写入城镇最终总量。
8. **状态所有权必须分层**：单站产出日报属于 `TransportStationBuilding`；城镇总运力、占用汇总和接口预约属于
   `TownTransportState`。实际接口搬运由 `WarehouseInterfaceBlockEntity` 读取预约后限速，不写回单站状态。

## 10. 实施计划

### 里程碑 A：建筑基础

- [x] 新增 `TransportStationBlock`、`TransportStationBlockEntity`、`TransportStationBuilding`；扫描直接复用通用 Scanner。
- [x] 建立 Codec 并登记 `transportStation` 分派键。
- [x] 实现多门结构扫描、面积/体积校验和岗位容量计算。
- [x] 新增独立配置及 `TownModelParameters.Defaults` 默认值。
- [x] 补 `setMaxResidents` 和新增字段的值守卫。
- [x] 完成 Block、BlockEntityType、MenuType、Screen 注册。
- [x] 完成概览/员工页和镇长印章详情。
- [x] 补齐纹理、数据生成资源、中英文翻译。

完成标准：方块可获取、放置、扫描、保存、重载和拆除；合法结构可进入岗位队列并分配居民；无资源产出；
客户端界面使用同步快照且无缺失资源。

### 里程碑 B：城镇运力生产

- [x] 确定属性权重、熟练度曲线、标准工日产出和每日重建语义。
- [x] 新增 `TransportStationDailyModel` 及纯逻辑测试。
- [x] 将 `TRANSPORT_CAPACITY` 接入晨间 service 重建，并通过 Action 系统生产。
- [x] 增加运输熟练度成长、单站日报、城镇汇总日报、持久化与增量同步。
- [x] 增加预测、生产页和镇长印章生产详情。
- [x] 将关键数值加入城镇模拟/审计参数来源。

完成标准：同一输入产生确定的日结结果；无工人或建筑不可工作时不产出；运力不会跨日累积；资源增量同步、
存档、城镇汇总日报和界面一致。

## 11. 验证计划

### 自动化测试

- `TransportStationBuildingCodecTest`：默认字段、具体 Codec 往返、多态 Codec 往返、第一阶段存档兼容。
- `TransportStationBuildingChangeTest`：重复设置相同扫描结果不会重复触发 `fireChange()`。
- `TransportStationStaffingTest`：自动加入岗位计划、目标人数受 `maxResidents` 限制、不可工作时容量为 0。
- 扩展 `TownBuildingRemovalTest`：拆除后居民保留但 `workPos` 和建筑名册被清理。
- `TransportStationDailyModelTest`：标准/满熟练度产出、属性权重、零配置和异常输入。
- `TeamTownTransportSettlementTest`：单/多工人、多货运站、晨间归零、Action 实际修改量、活动、熟练度、停产原因、
  城镇汇总和重复空结算同步守卫。
- `TransportStationForecastTest`：预测与结算共用合格工人及公式，空名册和不可工作原因。
- `TransportStationBuildingCodecTest`、`TownTransportStateTest`、`TownResourceUpdatePacketTest`：单站/城镇日报的
  旧存档兼容、持久化和增量包往返。

### 命令验证

```powershell
.\gradlew.bat test --tests "*TransportStation*" --offline --no-daemon --console=plain
.\gradlew.bat compileJava --offline --no-daemon --console=plain
.\gradlew.bat runData --no-daemon --console=plain
git diff --check
```

`runData` 可能需要非离线依赖环境，应在执行前记录工作区状态，执行后审查生成文件范围。

### 游戏内验收

1. 放置后建筑立即注册，重登和服务器重启后仍存在。
2. 多个相邻门、非法开放空间、面积/体积不足时结果正确。
3. 两栋内部空间重叠时均不可工作，拆除其中一栋后另一栋恢复。
4. 岗位队列可调整货运站顺序和保障人数，晨间分配稳定。
5. 打开自身界面和镇长印章时，中英文名称、数值和失败原因正确。
6. 拆除核心后立即注销，旧菜单不能恢复已拆建筑。
7. 第二阶段验证每日先归零后产出、无工人停产、多站汇总、城镇日报、存档与资源同步。

## 12. 已冻结的 H03 决策

- 每标准工人日生产 `64` 运力。
- 完成工作日记录 `(physical=1.0, learning=0.25)`，无实际运力产出时不记录活动。
- 健康、精神、力量、智力权重依次为 `35 / 15 / 30 / 20`。
- 属性生产力范围为 `0.5～1.5`，满熟练度加成为 `0.8`，最终生产力范围为 `0.5～2.3`。
- 运力不被运输消耗；每次晨间城镇结算先归零，再由货运站重新建立，不跨日囤积。
- 城镇汇总日报记录晨间总运力和已占用运力；仓库接口登记、限速、镇长印章实时明细、有限方块状态与每日短缺 Tip
  已由消费者计划 T03-T11 实现。
