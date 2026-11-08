# 建筑数据计划第二次工程复查与复杂度裁剪

- Time: `2026-10-23 00:45:28 +08:00`
- Author: `Codex; OpenAI GPT-6; source inspection and architecture review`
- Status: `completed`（本轮计划复查与修订完成；实现及运行验收尚未开始）
- Scope: [统一建筑数据计划 Revision 2](../plans/2026-10-23_00-18-00_town-building-data-system.md)

## Completed

- 按用户要求重新对照真实调用链复查，并在原计划中修正文句和规则，未创建第二份实现计划。
- 核对 `TownSettlement.tickResidentsMorning`、`TownSimData`、住房/就业统计，确认上一稿把 pending 坐标留在 housePos/workPos 会被当成有效分配；改为 Resident 中一个可选 PendingAssignments 字段，旧实际字段保持语义。同时确认原 group 15 字段，新增一个可达 16，分别新增两个会越过常用构建上限。
- 区分 pending 与 archive，关闭删除后冲突记录/坏容器自动重新成为建筑的路径；删除同时清理原文、计划及恢复意图。元数据迁移和修复单独 dirty，服务端安装 listener 后报告初始待保存变化。
- 按关键经营事实、可重建快照、展示日报区分恢复粒度，避免一份坏日报使整栋住宅停止服务。
- 补齐建筑维度的模拟周期扫描、运输设备、全量同步、经营/观察入口接线清单，区分存储归属与当前可执行世界。
- 确认 `CachedLevel.getCachedLevel` 会调用 level.getChunk 取周边 AABB；计划改用非加载扫描适配器，补缺区块 INCOMPLETE、世界高度边界、零采样和非有限温度处理。
- 补充操作边界：生产/回调中核心增删先经既有 runtime 生命周期入口，提交后再通知；旧对象、旧菜单和晚到任务核对当前 holder/data/store/实例。

## Decisions

- 裁掉通用递归字段所有权树，顶层键从各类 MapCodec 派生，嵌套格式改变必须升版本；明确兼容范围，避免两套 schema 漂移。
- 每栋只保存一份懒生成编码缓存；未解析数据不再生成等价副本，不长期保留 RoomData 的方块索引/世界缓存。
- 扫描使用一份按位置的 LinkedHashMap 轮询与既有预算，保持主线程执行；首版不做全局方块事件反向索引、多级队列或额外线程。
- 明确实际刷新间隔受 B/q 限制；Codec 次数减少不能单独证明总性能最佳，必须同时检查捕获复制、内存峰值、完整 tick 和更新延迟。

## Validation

- 静态检查依据包括 `TeamDataHolder` 缓存、`Resident.CODEC`、`CitizenSimScheduler`、`TownSync`、`TownWarehouseDeviceAccess`、`BlockScanner`、`CachedLevel` 及相关管理 UI。
- 在统一计划中补充 pending 实际无家结算、删除后再次重开、分级损坏、元数据单独保存、非加载扫描、生产中生命周期事件、跨维度服务门控等真实验收场景。
- 对本轮文档执行本地链接、围栏、Unicode、空白及规则一致性检查；生产源码未修改，没有编译或运行 JUnit/GameTest。
- 文档影响：当前行为未变，docs 无需更新；旧日记保持历史，追加本条。design、整合包和热系统工作均未修改。

## Remaining

- 设计无法代替运行结果。实施完成后统一真实游戏测试与独立进程恢复，并比较相同工作负载下的主线程、内存和更新延迟，才可给出性能结论。
