# Citizen 最近四个提交的代码审查与语义不变性能改进点

- Time: `2026-08-21 14:30:00 +0800`
- Author: `Kimi; code review`
- Status: `completed`
- Scope: `content/town/citizen/**`（提交 d590cd572 渲染管线、92b5c9dfc 居民模拟+字体、eb6a63f57 居民年龄、77e40b975 服务端性能）

## Completed

- 逐文件审查四个提交，未发现确认的正确性缺陷。以下疑点均经反证排除：
  - Flywheel `InstancingEngine.addListener` 疑似累积泄漏——反编译确认为 `WeakHashSet`，set 语义去重且弱引用不泄漏；
  - 77e40b975 粗格 AOI 索引按 `CitizenPresence.behaviorScheduled`（= 全部有效状态）建索引，是 `presentationEligible` 的严格超集，无漏选；
  - 索引陈旧窗口 ≤ AOI_REFRESH(20t)，移动位移有界（解卡 nudge ≤1 格、坠落仅 Y、BehaviorSystem 不写 px/pz、传送走 notifyImmediate→invalidateVisibilityIndex），16 格 halo 安全；
  - eb6a63f57 spawn 包 age 字节读写对称；`prepareSleepingState` 改为只清 valid-bed 位、保留年龄位，方向正确；
  - FakeCitizenManager 与 ClientCitizenCache 两处准星选取均已按 modelScale 缩放，无年龄尺寸不一致。
- 确认 77e40b975 的 trackedUnion 过滤与原行为等价且更省：未 tracked 居民原本也不会被发送（发送循环按 tracked 过滤），只是白做脏检测。
- 记录一处代码异味：`CitizenRenderOwnership.resolve` 的 `sleeping` 参数未被方法体使用（死参数，API 有误导性）。

## Decisions

- 性能改进建议（均已核实为语义不变，未实施，按收益排序）：
  1. `CitizenRenderCoordinator.applyBatch/applySpawn`：`ClientCitizenCache.applyBatch` 内部已按 id 解析对象，协调器又逐条 `get` 一次仅为 notify——让 applyBatch 返回/回调已更新对象，省第二轮哈希（包处理热路径）。
  2. `CitizenRenderCoordinator.batchOwnerFor`：每居民每 tick（Flywheel）+ 每帧（CPU 后端）做 `BATCH_OWNERS.get` + 无条件 `put/remove`；owner 未变时跳过 map 写入。
  3. `ClientCitizen.now()`：`renderPos()`/`visualYaw()` 每居民每次走 `Minecraft.getInstance()→level→getGameTime+getFrameTime` 虚调用链；在 render/tick 入口解析一次经重载传入。
  4. `SyncEngine.flushDeltas`：组包循环对每个 due id 每玩家做 `findById`+`indexOf` 双哈希，writeback 再一轮，共 P+1 轮；flush 开始一次性解析为可复用暂存，组包与回写共用，降到 1 轮。
  5. `flushDeltas` 每玩家 `new Long2ObjectOpenHashMap` + 每 chunk `new ArrayList` + `computeIfAbsent` lambda；Entry 为不可变 record 可跨玩家共享，map/list 可做 scratch 复用，显著降服务端 GC。
  6. 小项：`FakeCitizenManager` ACTIVE 用 `int2ObjectEntrySet().fastIterator()` 避免装箱；`CitizenDeltaPacketBatcher` 拆组时 `new ArrayList<>(subList)` 冗余拷贝可省（低频）。

## Validation

- 静态审查：`git show` 逐文件 diff + 工作区当前源码交叉核对；`javap` 反编译 flywheel 0.6.11-13 确认 `InstancingEngine.listeners` 为 `WeakHashSet`、`ReloadRenderersEvent` 走 Forge 总线（订阅注册正确）。

## Remaining

- 上述 6 项性能改进未修改代码，待确认后实施（建议 1-3 先做，改动小、热路径收益直接）。
- 建议在 `SpatialGrid.rebuildVisibility` 注释或文档中固化"halo 假设：任意单条路径 20t 内 XZ 位移 < 16 格"这一不变量，防止未来新增长位移路径悄悄破坏。
- 92b5c9dfc 中的字体缓存重构（KGlyphProvider/UnihexGlyphStore 等）属 scenario 字体系统，未在本次 citizen 审查范围内深入。
