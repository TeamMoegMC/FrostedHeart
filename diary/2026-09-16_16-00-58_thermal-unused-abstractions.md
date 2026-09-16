# 清理四处无用包装和无状态实例

- Time: `2026-09-16 16:00:58 +08:00`
- Author: `Codex; OpenAI GPT-6; primary engineering agent`
- Status: `completed; 原随机测试环境仍有未解释失败`
- Scope: `无调用入口、构造包装、解析场写转发及无状态提交器实例`

## Completed

- 删除 WorkerPageStore.resolveAirFaceSlot。唯一实际调用者使用 resolveAirFaceTarget 并复用非空 MutableAirTarget，因此同时删除该死入口留下的两个可空输出判断。
- 删除 MaterialBoundaryRegistry.Profile.body，MinecraftThermalProfiles 直接构造 Profile，构造器原数值校验保持。
- 删除 MinecraftThermalInput.upsertGameplayAnalyticField/removeGameplayAnalyticField；HeatAdjustCommand、CuriosityEntity 和相关 GameTest 直接调用已有 MinecraftGameplayFields.upsert/remove。未新建入口类，主线程检查、键、排序与世界生命周期保持。
- TopologyCommitter 的 commit/restorePagePublications/releaseOldSpans 改为 static，构造器 private，Engine 不再保存字段或创建实例。
- 更新气候架构、世界温度、Boss 冷场文档和计划；清理旧调用与无用 import。

## Decisions

- 保留真正有责任的提交操作集合、材料 registry 和解析场拥有者；只删除本次确认的重复或无状态部分，不继续扩展新一轮架构审查。
- 没有新增数组、锁、周期任务或运行时校验；每个 Engine 少一个提交器对象及对应引用，不宣称有可量化的帧率收益。
- 检查点职责重叠保持为已知问题，本次不处理。
- Documentation impact: 活文档和示例使用真实入口，历史 diary 未改写。

## Validation

- Java 17 编译生产与 GameTest 源码通过；全仓库 src 搜索无四项旧入口/实例引用，git diff --check 通过。
- 原 run-gametest/server.properties 为 normal 世界、空 seed。第一轮完整运行 101/102，通过之外一项 placedReservoirsReceiveTheDroppedItemRadiationBoundary 的正辐射断言失败。
- 给上述断言增加 source/middle/receiver/raining 失败诊断，未改变条件或超时。第二轮该项通过，但 realChunkTicksFreezeOnlyTheScheduledSurfaceSample 和 phaseRequestSurvivesSameBrickTopologyChurn 失败（后者 H 已到目标、request=1、randomTickSpeed=3）。这两轮没有被记为成功；环境/调度因素可能相关，但本轮没有查明全部原因。
- 为排除随机自然地形/群系变化，临时将开发 GameTest 环境设为 seed=0、minecraft:flat、plains，使用既有完整命令运行：102/102 全部通过，BUILD SUCCESSFUL。没有改生产算法、配方、断言阈值或注入测试专用生产行为。
- 测试后逐字还原原 server.properties；没有提交测试世界配置或引入新测试框架。固定环境结果证明该环境的功能回归通过，不证明原自然世界的失败已解决。
- 清理本次验证日志、配置备份与测试世界，保留必要失败诊断。

## Remaining

- 随机自然世界下上述三项用例的环境前提/时间依赖仍需单独调查；本次没有把它们归因于某个已证明的生产缺陷。
