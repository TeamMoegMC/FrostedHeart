# 完成水体相变提交、继续降温及旧记录释放

- Time: `2026-11-06 22:05:37 +08:00`
- Author: `OpenAI Codex / GPT-6; primary implementation and production-validation agent`
- Status: `completed`（源码与选定真实Forge验证；用户明确取消打包）
- Scope: `MinecraftPhaseController`、`StateTransitionData.WorldConditions`说明、旧保存/在线细粗档回归、相变与生命周期文档

## Completed

- 完成[计划](../plans/2026-11-06_21-28-38_completed-water-phase-commit.md)。根因是把无H记录自然冻结的 `fluid_boundary_tag` 启动条件，重复作为已完成潜热的物理提交条件。
- 自然环境回退仍要求液体边界；在线实际完成请求和完成H的保存/休眠记录兑现目标状态，随后按目标材料继续换热。休眠入口补充完成度检查，半程潜热不能变块。其他明确世界约束保留，原H与v6格式继续使用，没有额外温度状态、记录倒计时或重复潜热支付。
- 用经过v6 NBT读回的内水完成记录，真实Forge观察到17tick变薄冰，原始H精确保持1493000J；200tick后约-8.87494°C，后续薄冰→冰的真实潜热继续推进，采样于22000tick首次看到记录已释放，25217tick尾部确认实际冰和空附件。自然内水/岸水正反例与半程H拒绝均通过。
- 在线细档/实际粗档分别通过有限已计账控制→开关暂停→恢复→主线程ACK→材料重新发布。细/粗分别89/83tick，H均为目标1493000J，四个水平邻居仍为水。
- 用户随后要求“不用你build了”。停止后续编译/打包，让已启动的河道验证使用既有编译结果结束；禁用排队的打包初始化，测试摘要写出后停止其wrapper。未生成新生产JAR，上一份JAR不包含本次源码修复。

## Decisions

- 让“热量过程已经完成”兑现为实际物态，避免通过清空H、跳过潜热、另建冷却副本或强制温度来隐藏等待。
- 本次行为变化：参与热模拟并真正耗尽冻结潜热的内水可以结冰；无H记录的普通自然冻结仍受岸边条件约束。明确禁用、群系禁止、高度和解析场保底等规则继续生效。
- 中间在线夹具的同层火源受水影响，清理断言失败；调整为干燥高一格的真实基座并明确检查双源LIT后，细/粗两项通过。保留原失败记录，未放宽相变或H断言。
- Documentation impact: 更新[余温/提交合同](../docs/climate/thermal-residual-lifecycle.md)、[材料规则](../docs/climate/heat-production-and-network.md)和[接入说明](../docs/climate/data-lifecycle-and-integration.md)；新增[验证摘要](../docs/climate/validation/thermal-completed-phase-2026-11-06.json)。旧调查仅作为修正前证据。无配套仓库或`design/`变更。

## Validation

- 修正前真实Forge：完成水记录的及时提交断言按预期失败，热保存/worker错误0、关闭root0。日志 `build/river-thermal-completed-phase-before.log`。
- 当前工作区Java17编译与测试夹具编译均在用户取消build之前完成；最终只使用既有class运行测试。
- 最终分组：在线2/2、材料/红外/跨Section图11/11、Minecraft接入6/6、旧记录及余温5/5、真实20TPS河道1/1，共25项通过；每组错误0、关闭root0。范围及各日志见验证摘要，不把分组计数冒充单次全套运行。
- 180秒河道仍保持正常耗煤与近处供暖，128m标记处三组水/岸红外均INVALID，无远处材料热记录。该缺失值不是实测-20°C。
- 生产实现改动集中于一个控制器的条件用途与完成度，另一个生产文件只校正WorldConditions注释。未修改工具依赖约束、复制历史源码重编或新增路径hash门槛。

## Remaining

- 本次源码修复与授权的选定验证完成；打包按用户要求取消。
- 实际客户端画面、独立JVM磁盘重启及其他明确世界禁令的玩法设计不在本次资格内。
- 上轮单独记录的无源粗档退出断言仍未闭合，本次未将它宣称为已解决。
