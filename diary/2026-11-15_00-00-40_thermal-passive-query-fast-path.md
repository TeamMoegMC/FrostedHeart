# 无 Page 区块的被动温度查询不再挂 SectionOwner

- 作者：Claude（Anthropic），受 yuesha-yc 委托
- 时间：`2026-11-15 00:00:40 -06:00`
- 状态：实现完成；测量 gametest 前后对照完成；热运行时回归矩阵 158/158 通过；百人负载在本机三次都没有跑满（见下文）
- 相关：[统一验证数据](../docs/climate/validation/thermal-cell-enthalpy-validation-2026-11-14.json)、[运行时架构 · Mutation Capture](../docs/climate/thermal-runtime-architecture-and-optimization.md#mutation-capture)

## 起因

统一验证数据里 `mixing-direct-100-default-r10`（100 客户端、默认 250 ms/s 额度、8000 tick）的主线程分阶段总计：

- `thermal_passive_query` 19,600,081 次，约 2,450 次/tick、0.52 ms/tick，平均 0.21 µs；
- `thermal_world_mutation` 3,888,761 次，约 486 次/tick、0.19 ms/tick；
- 主线程热模拟平均 1.543 ms/tick，等于各阶段之和；目标 p95 0.5 ms、p99 1 ms。

被动查询主要来自 `ClimateBlockUpdates` 接管的区块随机刻：雪、冰、水的相变尝试（`MinecraftPhaseController.environmentTemperature`）、植物温度检查，以及下雪判断里的 `WorldTemperature.air`。这是按代码路径推断的，没有逐调用者计数。

`MinecraftThermalInput.sampleAir` 每次都先调用 `pages.loadedSectionOrAttach`。所以任何被查询过的已加载 Section 都会挂上 `SectionOwner`。之后这个 Section 里每次 `setBlockState` 都要走 `onSectionSetBlockState` 分类，并在下一 tick 的 `drainMutations` 里排空。在没有 Page 的 Chunk 里这些记录没有消费者：

- `collectCenter` 和 `recordMaterialCheckpointChange` 在没有 page 时直接返回；
- 天空柱只为 `pagesByChunk` 里的 Chunk 标记；
- halo、热源、辐射遮挡和 `BlockRadiationIndex` 的 Section 各自显式挂 owner。

## 改动

- `MinecraftThermalInput.sampleAir`：所在 Chunk 没有 Page（新增 `MinecraftPageManager.hasPageInChunk`，与 `drainMutations` 标记天空柱用的是同一判断），且所在 Section 没有场 Tile（新增 `QueryPublication.hasFixedSection`，即 `readAir` 原有的无锁预检）时，直接返回 `sampleDormant(chunkForThermalCapture(...))`，不挂 owner。
- 返回值与原路径相同。原路径在这种情况下 `tryReadAir` 必然失败；page 为 null，跳过 CAPTURED；`airPublication(null)` 为 null，最后同样走到 `sampleDormant`，所用区块也相同。
- 没有使用 `hasTrackedChunk`，因为它里面的 `hasPendingRetirementInChunk` 是对当前 cut 退休列表的线性扫描。退休窗口内场 Tile 仍在的情况由 `hasFixedSection` 兜住，走原路径。
- 有 Page 的 Chunk 内保持原路径，仍然挂 owner。玩家、物品、城镇查询同样经过 `sampleAir`，规则一致。
- 新增 `ThermalPassiveQueryFastPathGameTests`（命名空间 `frostedheart_passive_query_fast_path`），并加入 `src/gametest/thermal-runtime-namespaces.txt`。

## 验证

测量 gametest 在同一台机器（macOS，默认堆）上运行，世界种子随机；改动前把三个生产文件临时 stash 后用同一测试运行。72 个远处 Chunk（1728 个 Section），营火 Page 在中心 Chunk。

| 指标 | 改动前 r2 | 改动后 r2 |
|---|---|---|
| 显式查询后远处 Section 的 owner 数 | 1726 / 1728 | 0 |
| 400 tick 随机刻后远处 owner 增量 | 2 → 8 | 0 → 0 |
| 返回值与输入自然温度不一致的次数 | 0 | 0 |
| 每次查询，按区块连续（随机刻的实际顺序） | 20.25 ns | 20.07 ns |
| 每次查询，跨 72 个区块随机跳 | 21.67 ns | 38.68 ns |
| 4096 次方块改动触发的热钩子 | 4093 次，2.17 ms | 0 |
| 改动后 3 tick 的 `thermal_input_tick` | 437 µs/tick | 11.8 µs/tick |
| 有 Page 的 Chunk 内查询仍挂 owner | 是 | 是 |

- 跨区块随机跳时多出的约 17 ns 来自 `getChunkNow`：原路径复用已挂 owner 里存的 chunk；`ServerChunkCache` 的 4 项缓存在连续访问同一区块时命中，所以按区块连续的顺序下两者持平。
- r1 两次的自然随机刻窗口差异来自随机世界和 JIT 时机，不可比，未采用。
- 热运行时回归矩阵：`thermal-runtime-namespaces.txt` 全部 30 个命名空间，`-PthermalTestHeap=4g`，一次运行 8 分 27 秒，158/158 通过（`All 158 required tests passed`）。比 11-14 最终矩阵的 157 项多出的一项是本次新增的测试。矩阵里本测试的耗时数字（60 / 33 ns）受同服其它测试状态影响，不与上表比较。

### 百人网络负载（本机，含本次改动）

照 `Scripts/thermal-network-load.init.gradle` 和 `Scripts/thermal-network-clients.cjs` 在本机搭建。`server.properties` 按验证数据里记录的值填写（端口 25585、离线、视距与模拟距离 6、种子 71108222452），另加 `max-players=128`；worker 额度 250 ms/s，`-PthermalLoadSeconds=400 -PthermalLoadRecoverySeconds=30 -PthermalLoadRecoveryReturnHome=true`。三次都没有跑满 8000 tick：

| 运行 | 堆 | 世界 | 结束 |
|---|---|---|---|
| `build/thermal-network-load` | 4 GiB | 全新 | 4400 tick 后 OOM，看门狗关服；另一个会话的 gametest 与启动阶段重叠 |
| `build/thermal-network-load-r2` | 8 GiB | 全新 | 4600 tick 后看门狗关服，堆已满 |
| `build/thermal-network-load-r3` | 8 GiB | r2 已生成的世界 | 第 0–7 轮 16–20 TPS；4800 tick 进入从未生成过的第 8 轮后跌到 3.6–7.8 TPS、区块卸载停止；6000 tick 时看门狗关服，堆未满 |

原因是夹具本身：侦察玩家每 600 tick 传送到 `origin + (8192 + cycle × 256, …)`，每一轮都是新地形。在本机上，世界生成和存盘跟不上，`ProtoChunk` 和待写区块不断累积。第一次的堆快照里有 20,445 个 `ProtoChunk`、10,992 个 `IOWorker$PendingStore`、5,919 个 `LevelChunk`；热模拟对象合计远小于 50 MiB。热模拟卸载检查点只请求了 211 次，结束时没有积压。

r3 前 4800 tick（地形已生成，平均 18.2 TPS）与验证数据中改动前的 `mixing-direct-100-default-r10`（Windows，20 TPS）每 tick 对比：

| 每 tick | r3 0–4800 | r10 |
|---|---|---|
| `thermal_world_mutation` 次数 | 3.0 | 486.1 |
| `thermal_world_mutation` 耗时 | 3.8 µs | 188.4 µs |
| `thermal_passive_query` 次数 / 耗时 | 1,847 / 321 µs | 2,450 / 523 µs |
| `thermal_input_tick` | 130 µs | 439 µs |
| `player_thermal_update`（不受本次改动影响） | 194 µs | 296 µs |
| 热模拟主线程 p50 / p95 / p99 / mean | 0.52 / 1.81 / 2.98 / 0.73 ms | 1.30 / 2.73 / 4.97 / 1.54 ms |

- 钩子次数是计数，与机器无关；两边查询量同一量级，负载可比。
- 耗时跨机器：以不受影响的 `player_thermal_update` 估计本机约快 35%。折算后均值约低 28%、p50 约低 39%，p95 持平，p99 约低 9%。
- r3 这段的区块加载、卸载每 tick 只有 r10 的约 40%，所以 `thermal_input_tick` 的下降不能全部归到本次改动。

## 文档影响

`docs/climate/thermal-runtime-architecture-and-optimization.md` 的 Mutation Capture 段：

- 原文说懒启动会挂载所有已加载 Section。代码实际只给 Page、halo 和启动触发点挂，已更正。
- 写明 owner 的来源和本次规则。
- 记录一个已有缺口：同一 Page Chunk 内，Page 上方 Section 的改动只有在该 Section 恰好已有 owner 时才会标记天空柱，没有任何代码为此挂 owner。

## 遗留

1. 上述天空柱缺口是已有问题，本次没有修。改动前它也只靠偶然挂上的 owner（包括被动查询挂的）覆盖；本次只在没有 Page 的 Chunk 里不再挂，有 Page 的 Chunk 内行为不变。
2. 百人负载在本机还没有完整跑满 8000 tick，原因见上。要完整跑一次，需要先把第 8–13 轮的侦察地形生成好，或者在 r3 的世界上再跑一到两次。
3. r3 这段的样本年龄 p99 只有 3 tick（r10 同额度下是 232）。本次改动只影响主线程，这个差异来自机器或负载，本次没有分析。
