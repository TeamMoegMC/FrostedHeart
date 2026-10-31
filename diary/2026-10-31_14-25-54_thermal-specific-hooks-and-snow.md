# 篝火专用接入与积雪调用链修正

- Time: `2026-10-31 14:25:54 +08:00`
- Author: `Codex; OpenAI GPT-6; implementation and scoped validation`
- Status: `completed`
- Scope: `篝火点火、公共材料通知、源补查、额外积雪与重点Forge验收`

## Completed

- 按用户的正确性底线撤回“为合并入口而在公共Chunk判断新点火”。CampfireBlockMixin_TimeLimit只作用CampfireBlock，自身onPlace调用onCampfireIgnited；公共onMaterialStateWritten仅维护在线/休眠材料，旧onBlockStateWritten退出。
- 点火通知核对实际已加载Chunk和状态；不依赖BE已经创建。空BE过滤移至加载时的未知源发现，已知源拒绝仍复用原补查队列并清除旧快照，无新队列或等待标志。
- 额外积雪从ClimateBlockUpdates末尾直接执行；删除不可达的ServerLevelMixin_PlaceExtraSnow、登记和旧业务实现。保留配置、概率、−13°C门槛、冷群系、雨强、光照与五层上限；两处丢弃setValue返回值的SNOWY写入已修正。
- 生产Java净减44行，删除1个独立Mixin，无新生产类型。正式JAR有11个集中热接入、0个旧积雪类、0个GameTest类；Campfire.onPlace已重映射为m_6807_。

## Decisions

- 专用玩法留在专用生命周期，真正共有的Section几何、材料和保存保留公共接入。Mixin数量不是优先于作用范围和玩法完整性的指标。
- Forge捕获放置期间不由公共材料回调启动源，确认onPlace后才登记；源容量拒绝不因BE稍后创建而丢失。
- 恢复原先被提前返回绕过的积雪会增加随机采样，不能声称后续随机序列与缺陷版本完全一致。

## Validation

- 首次统一assemble/compileGameTestJava通过；之后生产源码未改，只重编测试夹具，未运行JUnit或大矩阵。
- 最终源6项采用r2、接入4项采用r4、燃料2项采用r1，共12项有通过证据；各进程热错误0，关闭rootBytes=0。点火/熄灭重燃、空Chunk源拒绝恢复、packed BE、捕获放置回滚与确认、实际燃料读盘、维护保存和真实tickChunk积雪均覆盖。
- r1共12项，10过/2失败：新夹具误把普通地形中固定高度当成Air/地表。r2修正后10项9过，积雪仍失败；夹具只改天气计划，没有设置当下雨强，而且−10°C高于真实−13°C门槛。r3修雨强后4项3过，温度前提仍错；r4按源码门槛两侧设置并先检查地表/雨强/温度/光照，4项全过。未为通过测试改变生产规则。
- 完整证据：[本批验收](../docs/climate/validation/thermal-specific-hooks-2026-10-31.json)。生成日志和运行世界在build/，未加入版本控制。
- Documentation impact: 更新[运行合同](../docs/climate/thermal-runtime-control.md)的专用入口、材料职责、BE前补查与积雪规则；[本计划](../plans/2026-10-31_13-56-49_thermal-specific-hooks-and-snow.md)完成，原方案入口合并决策明确被取代，后续工作入口同步更新。旧日记未改写。

## Remaining

- 本批未重复独立生产JAR服务器重启，既有正式包结果保留旧编译资格；没有新性能百分比或百人联网结论。
- 当前随机循环接管、岩浆邻接通知、雪直接调用保护和全局原版禁冻保留；不以删除玩法换取Mixin数量减少。
- 改动尚未提交或推送；本轮未修改城镇、世界生成算法、design或受保护旧粒子实现。
