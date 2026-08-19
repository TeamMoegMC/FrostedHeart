# Citizen 模拟系统存储与内存深度优化

- Time: `2026-08-18 22:10:00 +08:00`
- Author: `Antigravity (Google DeepMind); Core Developer`
- Status: `completed`
- Scope: `src/main/java/com/teammoeg/frostedheart/content/town/citizen/`

## Completed

- 移除了 `CitizenSim` 中未使用的 `ty` 目标高度数组与 `BehaviorSystem.enterSleep` / `TownSimData` 中的死写入；
- 移除了 `CitizenSim` 中冗余的 `tickPhase` 数组，在 `BehaviorSystem.tick` 中直接通过 `(sim.id[i] % SLICE) != slice` 计算分帧，实现 0 内存开销与 0 Cache Miss；
- 剔除了 `CitizenSim.save` 中瞬态网络状态 `sdir` 的 NBT 落盘，并在 `CitizenSim.load` 中保留对旧存档 `sdir`/`syaw` 的平滑迁移回退；
- 在 `SpatialGrid` 中引入 `IntArrayList` 对象池（`listPool`），消除每 5 tick 服务端重建网格时产生的数千个小列表与 Lambda 闭包 GC 抖动；
- 在 `ClientCitizenRenderer` 渲染主循环中引入视锥前置朝向点积粗剔除（`dot < -1.5m` 直接跳过背向摄像机单位），显著减少客户端每帧 `AABB` 堆对象分配；
- 在 `FlowField.build` 中改用 `IntArrayFIFOQueue` 替代 `ArrayDeque<Integer>`，消除单次流场构建时多达 4,096 次原始类型装箱拆箱；
- 更新了 `CitizenSimPersistenceTest` 单元测试以验证数据结构精简与向后兼容性。

## Decisions

- **移动系统高度与寻路解耦**：居民移动继续采用 2.5D 高度贴合（`conformHeight`），目标点 `(tx, tz)` 足够描述平面导航，移除 `ty` 不影响未来任何攀爬状态机的扩展。
- **分帧相位算术化**：居民 ID 为自增唯一整数，`tickPhase` 恒等于 `id % 20`，无需使用 `byte[]` 数组持久化存储。
- **空间网格池化**：复用已有 `IntArrayList` 实例，在热路径下达到稳态 0 对象分配。

## Validation

- `./gradlew test --tests com.teammoeg.frostedheart.content.town.citizen.*` 全部通过；
- `./gradlew compileJava compileTestJava` 编译通过无错误。

## Remaining

- None
