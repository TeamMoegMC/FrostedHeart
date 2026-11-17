# 超平坦世界的温度，与不依赖能量塔的城镇更新

- Time: `2026-11-10 16:23:06 -0600`
- Authors: `Claude (Anthropic Opus 5.5), 设计起草；项目所有者做出两项决定`
- Status: `completed`（2026-11-10 所有者确认：没有能量塔时照常结算；结果见「结果」与日记）
- Scope: 世界温度的竖直规则（`content/climate/WorldTemperature`、`town/block/blockscanner/BlockScanner`）；城镇执行世界（`TeamTownData.executionLevel` 及其调用方）
- Related: [世界气候与温度](../docs/climate/world-climate-and-temperature.md)（海拔分段 `A(y)`、`alpha_air`）、[建筑数据](../docs/town/building-data.md)、[持久化与运行时](../docs/town/persistence-and-runtime.md)、[城镇界面重新设计](2026-11-09_21-45-44_town-ui-redesign.md)

## 起因

第三个测试世界 `town-third`（超平坦，默认配置）暴露了两件事：

1. **超平坦世界的屋子 80°C。** `WorldTemperature.altitude` 只看绝对 y：y ≤ −55 时每低一格加 20°C。超平坦的地面在 y = −61，
   屋里的空气在 −60 到 −58，分别是 100、80、60°C，平均正好 80°C。这个深度下空气的气候系数为 0，世界的寒冷完全进不来。
   - 之前的测试世界 `institute-fresh` 也是超平坦。11-09 测试时把它的住宅最高温度从 40°C 改成了 200°C 来绕开，
     这是错误做法，那个世界里关于室温的结果都不算数，见「附带处理」。
2. **没有能量塔的城镇，什么变化都不会发给客户端。** 先放了 8 个建筑核心、还没建塔：服务端登记了 8 座，客户端只有建镇时
   快照里的 1 座，其余建筑一直显示「建筑数据还没到」。

## 已核实的现状

### 温度

- 竖直规则只有一处实现：`WorldTemperature` 的 `altitude`、`naturalAirUnclamped`（空气气候系数 `alpha_air`，用硬编码
  `SEA_LEVEL = 63`、`STONE_INTERFACE_LEVEL = 0`）和 `naturalBlock`（方块气候系数，读配置 `climateSeaLevel`、
  `climateStoneInterfaceLevel`）。`BlockScanner.sampleNaturalTemperature` 抄了一份方块公式。其余调用方（热模拟运行时、
  红外、冰雪、动物、研究观测等）都经过 `WorldTemperature` 的公开方法。
- 世界温度只在服务端计算，客户端没有调用。超平坦可以按维度精确判断：`ServerLevel.getChunkSource().getGenerator()
  instanceof FlatLevelSource`。
- 热模拟运行时按需重新读取自然温度（自然刷新队列、休眠区段按需计算），没找到持久化的「自然温度版本」。规则改了以后，
  已有超平坦世界里的房间应当按冷却速度逐渐降到新的自然温度，不需要迁移；在 `town-third` 里确认。
- **GameTest 世界是普通地形，不是超平坦**（初稿写错了，第一次跑测试后更正）。`run-gametest/server.properties` 的
  `level-type=minecraft:normal`，主世界生成器是 `minecraft:noise`。测试建筑放在 y ≈ −60，处在普通地形的深层地热段里，
  这次改动不改变它们的温度环境。`src/gametest/resources` 里的 `flat.json` 只影响用超平坦预设新建的世界，例如
  bridge 的 `newworld`。GameTest 服务端没有超平坦维度，所以超平坦规则拆成纯函数来测。

### 能量塔

- `TeamTownData.executionLevel(owner)` 要求队伍有 `GENERATOR_DATA`，且它的维度等于城镇的建筑维度，否则返回 null。
  这是有意的设计：`docs/town/building-data.md` 写着「generator 缺失、维度不匹配……时，执行连接不可用」，
  `/town tick` 的报错是 "The town is waiting for its energy tower in its building dimension"。
- 依赖它的有：自动结算、`/town tick`、`/town spawn_refugees`、每秒更新 `tickSecond`、每刻的视图准备与增量同步
  `runtime.data.tick`、运行时修复（居民模拟、运输绑定）、`CitizenSimScheduler.isTownInLevel`、P2P 终端。
- 拆掉能量塔时，`GeneratorData.unregister` 只把 `actualPos` 清空，数据和维度都还在，所以「曾经有塔、后来拆了」的城镇
  今天其实照常更新。真正停住的是两种情况：**从来没建过塔**，以及**塔的数据在别的维度**。这只是碰巧没出问题：
  它依赖塔的数据残留。
- 用到塔的各处本来就能处理「没有塔」：`tickSecond` 只在 `actualPos` 不为空时驱动塔；结算的日报在没有塔时退回第一座
  建筑的位置；难民只在塔存在且开着时来；镇长印章的能量塔卡片有「镇上还没有能量塔」。
- 有两个 GameTest 断言的正是旧设计：`TownBuildingGameTests.dimensionMismatchSurvivesPeriodicRegistryAndCoreQueries`
  （塔在下界时暂停模拟与手动结算）和 `TeamTownPersistenceGameTests.generatorRemovalPausesSimulationAndReinstallRestoresIt`
  （删掉塔的数据就暂停模拟）。

## 决定

所有者 2026-11-10 决定：

- 超平坦世界单独计算，**没有地热**，因为基岩下面什么也没有。
- 城镇的更新与能量塔是否存在**无关**；玩家可能中途拆掉能量塔。

## 设计

### 一、超平坦世界没有地下

规则：**在超平坦维度里，海平面（y = 63）以下一律按海平面算**。

| 项 | 普通世界（不变） | 超平坦维度 |
|---|---|---|
| 海拔项 `A(y)`，y ≤ 63 | 分段：0 / 每格 +0.1 / 每格 +20 | 0（没有地热） |
| 海拔项 `A(y)`，y > 63 | 每格 −0.1，240 以上每格 −2 | 同普通世界 |
| 空气气候系数 `alpha_air` | 0（y ≤ 0）到 1（y > 63） | 1（没有岩层隔开气候） |
| 方块气候系数 | 0 到 `blockMaximumClimateAffection` | `blockMaximumClimateAffection` |

- 判断：`WorldTemperature.hasNoUnderground(LevelReader)`。`ServerLevel` 看它的生成器是不是 `FlatLevelSource`，
  `WorldGenLevel` 取其所属的 `ServerLevel`，其他读取器按普通世界算。只按维度判断，超平坦存档里的下界、末地照常。
- 高处降温仍以 y = 63 为准，不改成离超平坦地面多高：海拔降温在这个模型里是绝对高度的概念，经典超平坦里也几乎没人
  建到 63 以上。这样超平坦的地面就等于海平面。
- 实现集中在 `WorldTemperature`：`altitude`、`naturalAirUnclamped`、`naturalBlock` 各加超平坦分支，新增
  `blockClimateAffection(LevelReader, int y)`。`BlockScanner.sampleNaturalTemperature` 改用这个方法，不再自己抄公式。
- 普通世界的分段（包括 y = −54/−55 处的不连续）原样不动。

效果：`town-third` 的屋子在没有供暖时约为「维度 −10 + 群系 + 当时气候」，也就是零下，和普通世界地表的屋子一样。
有了能量塔供暖才会暖和。

### 二、城镇的更新不依赖能量塔

规则：**城镇在它的建筑维度里运行**。还没有建筑时，才用塔的维度。塔在不在、塔在哪个维度，都不影响城镇的更新。

- `executionLevel(owner)` 去掉对 `GENERATOR_DATA` 的要求：holder 仍须是当前注册的实例，
  返回 `resolveTownDimension(owner)` 对应的世界。既没有建筑也没有塔时仍为 null，没有东西可更新。
- 于是这些都不再等塔：增量同步（新建筑、居民、资源、塔的出现和消失）、每秒更新、运行时修复、居民模拟、P2P、
  自动结算和 `/town tick`。
- **塔只在城镇自己的维度里才算数**。塔在别的维度时，对这个城镇等于没有塔：
  - `prepareView` 的 `followTower` 只抄同一维度的塔位置（方位跟着它显示或消失）；
  - `tickSecond` 只在塔与城镇同维度时调用 `GeneratorData.townTick`；
  - 结算日报的位置与 `TownOperationalStatusProvider` 的塔状态只认同一维度的塔；
  - 难民（自动和 `/town spawn_refugees`）要求同一维度里有开着的塔，否则照旧不来，命令改为说明这一点。
- 没有塔时结算照常进行：屋子只有自然温度，没有难民。居民会因为冷而生病，最后可能离开。这是拆塔的自然后果，
  不暂停时间，否则拆塔就成了冻结城镇的办法（所有者已确认）。
- 客户端不用改：建筑、居民、资源和塔位置的增量包本来就有，只是以前在没有塔时发不出去。

## 步骤

1. 温度：改 `WorldTemperature` 与 `BlockScanner`。
2. 温度测试：
   - `naturalWorldQueriesRetainTheirTemperatureResults` 照旧在普通地形的测试主世界里断言普通规则不变，并先确认它不是超平坦；
   - 新增超平坦规则测试：用真实的 `FlatLevelSource` 确认判断，再通过 `altitude(int, boolean)`、
     `airClimateAffection`、`blockClimateAffection(int, boolean)` 确认 y = −64 到 63 没有地热、带满气候项，63 以上与普通世界相同。
3. 城镇：改 `executionLevel`，按上面四处加「同一维度」的塔判断，改两条命令的报错。
4. 城镇测试：
   - 两个「暂停」测试改写为新设计：塔在下界、或塔的数据被删掉时，城镇照常结算、模拟保持接管；在错误的世界里直接结算
     仍然跳过；
   - `TownViewSyncGameTests` 新增：没有塔的城镇里放第二座建筑，客户端探针收到建筑增量包；塔被拆掉时收到塔位置清空。
5. 跑 GameTest：九个城镇套件一组，默认开发集一组，分开跑（`save-off` 是全服的，同一次启动里会互相干扰）。
   默认开发集里的失败用开关恢复旧的塔判断做对照，确认与这次改动无关。
6. 游戏内验证，只用单人客户端、只在 `town-third`（所有者 2026-11-10 决定）：屋子从 80°C 降到零下；没有塔时新放的
   建筑一两秒内出现在镇长印章上；`/town tick` 可用；建塔后方位立即出现，拆塔后立即消失、能量塔卡片显示没有塔；
   没有塔时结算一天，屋子冷、居民受冻。
7. 文档：`docs/climate/world-climate-and-temperature.md` 的 `A(y)` 与 `alpha_air` 小节写上超平坦规则；
   `docs/town/building-data.md`、`docs/town/persistence-and-runtime.md` 的执行世界说明；
   `docs/town/town-model.md` 的结算频率表与方位说明；写日记。

## 附带处理

- 把 `run/saves/institute-fresh` 和测试客户端那份副本的住宅最高温度改回默认的 40°C。11-09 改成 200°C 是为了绕开这个
  问题，之后在那里得到的室温结果不算数。
- 不在这次改动里处理：审稿人提的界面问题；普通世界 y = −54/−55 处的不连续。

## 风险

- 以前因为没有塔、或塔在别的维度而停住的城镇，会恢复更新，并从下一个结算窗口开始结算。
- 没有塔、但有建筑的城镇也会每刻准备视图、检查增量，开销与今天有塔的城镇相同。

## 已确认

- 没有能量塔时结算照常进行：屋子变冷、没有难民（所有者 2026-11-10）。

## 结果

- 两处改动都已实现，见[日记](../diary/2026-11-10_16-23-06_flat-world-temperature-and-towerless-town.md)。
- GameTest：城镇九个套件 62 个全部通过；默认开发集 91 个里 4 个在改动前就失败（用临时开关恢复旧的塔判断对照确认），
  另有 2 个计时测试时过时不过。
- 游戏内（只在 `town-third`）：没有塔时新放的建筑立即出现；屋子从 80°C 变成 −10°C；建塔出现「镇东」，拆塔消失；
  没有塔时结算推进到第 1 日。
- 文档：`docs/climate/world-climate-and-temperature.md`、`docs/town/building-data.md`、
  `docs/town/persistence-and-runtime.md`、`docs/town/town-model.md`。

