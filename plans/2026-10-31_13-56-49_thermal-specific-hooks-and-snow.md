# 热源专用接入与积雪调用链修正

- Time: `2026-10-31 13:56:49 +08:00`
- Authors: `Codex; OpenAI GPT-6; implementation and validation`
- Status: `superseded; selected changes retained, remaining public paths completed by the later removal plan`
- Scope: `篝火点火接入、公共材料回调、额外积雪与对应重点回归`
- Related: `docs/climate/thermal-runtime-control.md`; supersedes the campfire-entry decision in `2026-10-30_23-51-15_thermal-mixin-integration-review.md`

## Goal

篝火专用行为只接入CampfireBlock，不在公共Chunk/Block写入路径判断篝火。真正共有的几何、材料与保存职责保留公共接入。不得为了减少Mixin数量丢失点火、燃料、保存或积雪玩法。

## Verified Current State

- 11个共享网格接入已集中到mixin.minecraft.thermal；篝火目标本来就是CampfireBlock/CampfireBlockEntity，但新点火识别被放到了LevelChunk最终写入回调。
- 当前Forge47.3.0的LevelChunk.setBlockState先写Section，再调用onRemove/onPlace，随后创建或更新BE；captureBlockSnapshots期间不调用onPlace，确认放置由Forge恢复生命周期通知。
- PhysicalSourceSpatialIndex.resyncBlock只需要BlockState，不要求BE已创建。补发现的空BE过滤目前会拒绝onPlace时的已知新源请求。
- ServerLevelMixin_TemperatureUpdate在tickChunk中段执行ClimateBlockUpdates后取消原方法；额外积雪单独挂原方法RETURN，当前链路不可达。
- 额外积雪方法中两处不可变BlockState.setValue返回值被丢弃，SNOWY地表属性没有实际写入。

## Steps

1. CampfireBlockMixin_TimeLimit仅在自身onPlace后识别真实新点火，交给专用onCampfireIgnited；核对当前已加载Chunk与实际状态，保留启动拒绝重试、容量拒绝补查。
2. 公共Chunk回调重命名为onMaterialStateWritten，仅维护在线/休眠材料。删除公共回调中的篝火分支，不留备用入口。
3. 空BE过滤只用于加载时的未知源发现准入；明确补查沿原队列无条件记录并丢弃旧位置快照。这样onPlace中BE尚未加入时也能在后续tick发现，不新增队列、事件、标志或轮询。
4. 将额外积雪实现移到ClimateBlockUpdates并在现有气候更新末尾直接调用；删除旧独立Mixin、配置登记及对Mixin的业务依赖。保留原配置、概率、温度/天气/光照条件与五层上限，修正SNOWY状态实际赋值。
5. 整批复查生命周期、捕获放置、来源拒绝、材料保存及积雪实际调用；完成后统一编译和重点GameTest，不运行JUnit、全性能矩阵或历史工程复制构建。

## Validation

- 复用源发现/首次点火/熄灭重燃/容量恢复和实际燃料回归。
- 将容量恢复夹具的新源放到没有BE的另一个已加载Chunk，覆盖onPlace时拒绝不能丢补查。
- 增加捕获放置确认/撤销检查，确保公共材料回调不启动篝火。
- 增加真实ServerLevel.tickChunk积雪检查：开启、关闭、无雨、过暖、层数上限及地表状态；其他已有接入重点回归同批运行。
- 静态确认旧积雪类/注册/业务导入删除，公共材料方法没有篝火分支，无新增公共基类注入。

## Documentation Impact

更新当前运行合同的篝火专用入口、补发现准入、积雪归属和验证范围；原计划标注本项被新用户约束取代。完成后追加日记，旧日记不改写。

## Outcome

已完成的选定修改：显式点火从CampfireBlock自身onPlace通知，公共Chunk回调仅维护材料；已知源拒绝复用补查并允许BE稍后创建，加载时仍过滤空Chunk；额外积雪在现有气候更新末尾执行，旧独立Mixin/登记/业务入口退出，SNOWY实际赋值修复。没有新增生产类型、队列或标志，生产Java净减44行，独立Mixin减少1个。

2026-10-31 18:13复查确认，本计划的“专用行为退出公共写入路径”目标尚未全部完成。公共Section写入仍比较由campfireFlags生成的源标志，SOURCE_MUTATION→drainMutations→resyncBlock仍维护篝火点火状态、熄灭和移除；完整Section重同步还会resyncSection。此前把公共Chunk入口移除扩大表述为完整生命周期退出，范围判断不准确。必须覆盖上述所有实际调用者，并保留点火、熄灭、水熄、破坏、替换、捕获放置回滚及Section重同步的玩法后，才能再次标记完整完成；不能把已有12项通过等同于该架构要求通过。

后续源码调查和三条残留路径的具体移除方法见[公共专用链路移除方案](2026-10-31_18-28-24_thermal-public-specialization-removal.md)，其中明确随机资格的额外调色板检查/随机序列代价，以及裸Section改写和真正共有数据通知的兼容边界。该后续方案现已完成并通过最终同轮15项重点回归，本计划转为superseded，原12项记录仍只证明当时编译。

assemble/compileGameTestJava首次通过，生产源码此后未改；最终重点12项通过，源6采用r2、接入4采用r4、燃料2采用r1，各进程热错误0、关闭root0。首轮新增夹具错误假设普通地形中的固定高度是Air/地表，随后又遗漏即时雨强与−13°C真实门槛；三次失败及仅修测试的边界均保留，未修改生产门槛或为通过测试添加条件。

记录：[验收JSON](../docs/climate/validation/thermal-specific-hooks-2026-10-31.json)、[当前运行合同](../docs/climate/thermal-runtime-control.md)、[完成日记](../diary/2026-10-31_14-25-54_thermal-specific-hooks-and-snow.md)。正式JAR包含11个集中接入、0个旧积雪类、0个GameTest类，onPlace已重映射；本批未重复独立生产JAR重启或性能矩阵。改动未提交或推送。
