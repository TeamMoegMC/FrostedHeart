# 热源补发现：省去重复位置复制与逐tick列表

- Time: `2026-10-30 17:46:04 +08:00`
- Author: `Codex; OpenAI GPT-6; implementation and verification`
- Status: `completed`
- Scope: `热源补发现入队、游标推进、区块原表判空及3项源回归`

## Completed

- `enqueueSourceDiscovery`直接判断原版`blockEntities`与`pendingBlockEntities`是否为空，省去原来为了判空而调用`getBlockEntitiesPos`创建的完整HashSet。已有Chunk附件只增加无状态判空方法，既有Mixin沿`ChunkAccess`父类读取原表。
- `drainPendingSources`无待办时直接返回；通过原游标表的迭代器移除完成项，删除每tick的`ArrayList`副本。原队列、8游标、32位置/游标、总256位置和每tick1新快照规则保持。
- 原待实例化篝火用例迁入`ContinuousAirSourceInputProductionGameTests`，检查空表、live BE、移除和仅packed NBT四种状态，以及不实例化BE的分段发现。删除只供旧用例调用的`discoverChunk(LevelChunk)`整块同步入口。
- 生产代码净增加15行，无新增顶层类型、队列、缓存或状态标志。更新[运行合同](../docs/climate/thermal-runtime-control.md)的源发现与验证范围说明。

## Decisions

- 跨tick游标继续持有独立位置快照；原表会因放置、移除及NBT提升而变化。此次只删重复复制，不增加篝火位置镜像或新的缓存失效规则。
- 收益依据是省去每次入队的O(BE位置数)复制，以及删除逐tick列表分配；没有整体CPU前后对照，不报告虚构的提速百分比。

## Validation

- `assemble compileGameTestJava`最终通过，日志`build/thermal-discovery-compile-20261030-r2.log`。生产JAR含426个thermal类、0个GameTest类；受保护旧粒子4类保留。
- 首次Forge检查在加载世界前失败：我错误地在LevelChunk Mixin上Shadow了定义于ChunkAccess的字段。已改为通过正确父类继承访问；失败日志和JSON保留于`build/thermal-discovery-source-check-20261030-r1.*`。
- 修正后只重跑源回归组，3项全部通过，退出0、热错误0、关闭rootBytes=0。新放置发布sampleTick600、airC12.912017981724182；生命周期回归提交10次、跨发热变化检查4次、failure=null。结果`build/thermal-discovery-source-check-20261030-r2.json`，对应日志同名`.log`。
- 本轮未运行JUnit或其他系统测试；旧生产JAR专用服记录保留自身版本资格，没有替换为本次产物的实服验证。

## Remaining

- 启动定位和新游标首次创建仍有完整BE位置集合的合并/复制成本；不受256次逐位置额度覆盖。
- 未测整体CPU/帧时间提速比例；本批不改变温度算法、时间步长、输入量或粗细切档策略。
