# 复查后收拢相变职责，停止扩展边缘规则

- Time: `2026-09-14 18:58:02 +08:00`
- Author: `Codex; OpenAI GPT-6; primary engineering agent`
- Status: `completed`
- Scope: `保留已验证的交接修复；按用户最新要求收拢MinecraftThermalInput职责`

## Completed

- `MinecraftThermalInput`随机相变入口仅转发给原有`MinecraftPhaseController.tryAtRandomTick`；相变归属、规则和提交集中在该控制器。
- 保存状态投影归`DormantChunkThermalState.projectMaterial`，解析场下限查询归`MinecraftGameplayFields.guaranteedFloor`。调用者复用少量工作样本，不增加逐方块缓存/数组或调度器。
- 入口类由本次整理前1986行降至1905行；没有新拆分文件或适配类，也不将此次局部整理称为整体架构重构。
- 保留复查确认的修复：嵌套替换不继承外层相变H，作用域记录后续替换以覆盖返回同一目标的情况；外层迟到回调和过期提交不覆写新物体。岩浆所有液位的比焓参考一致，减量在目标law参考下计算。
- 按用户收紧范围的要求，撤回未验证通过的单向配方升温专项处理及其专项测试，停止继续修改自然岩浆初温。撤回后恢复已验证行为，不保留旁路或兼容开关。

## Decisions

- 核实当前Minecraft `LevelReader.getNoiseBiome`使用`getChunk(..., BIOMES, false)`，缺失时直接查询生成器；没有增加不必要的邻区块等待限制。
- 此次先收回新增职责，后续规则需求应进入相应组件，不继续堆进门面类。
- Documentation impact: 同步runtime、生命周期、热源文档及休眠plan；原日记保留历史，未改design或伴随整合包。

## Validation

- Java17 `runGameTestServer --offline --no-daemon --console=plain -Dnet.minecraftforge.gradle.check.certs=false`：84/84通过，[完整日志](../build/thermal-input-responsibility-cleanup.log)。保留原82项与新增的嵌套替换/岩浆液位两项回归。
- `git diff --check`通过；已检查没有遗留`applyUnmodeledGameplayHeating`或门面中的`phaseFloor`实现。

## Remaining

- 自然生成岩浆首次进入材料模型仍使用自然环境初温；单边物理law与另一方向特殊配方的完整衔接尚未处理。按用户要求，本轮停止扩展这些规则。
- 入口类仍较大，剩余整体整理与真实多人负载测试不属于这次局部收拢的完成声明。
