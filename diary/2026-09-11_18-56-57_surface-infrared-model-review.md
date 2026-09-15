# 方块表面红外的数据含义与架构复查

- Time: `2026-09-11 18:56:57 +08:00`
- Author: `Codex; OpenAI GPT-6; primary engineering agent`
- Status: `completed`
- Scope: `复查红外模型并修正当前计划；未改生产代码`

## Completed

- 修订[当前计划](../plans/2026-09-08_21-58-58_thermal-unknown-block-ventilation-75.md#block-surface-infrared)，新增模型问题表及五层职责划分，区分表层状态、未知数据、模型限制和后续功能。
- 删除自然背景表及其刷新状态：无有效材料节点保留原画面，不以自然Air背景假装材料读数。
- 将禁止Air-only恢复材料列为必要的局部输入纠错；不扩大到新存档格式、固定热容或固体导热。
- 细化有效混合材料节点的判定、温度dirty与拓扑清除、NO_DATA语义、runtime代次与READABLE控制，以及full尾包提交规则。
- 撤回固定后移1/2048的保证，按实际depth bits/type定义数值取样候选和必须进行的客户端误差验证。
- 明确旧色标饱和和0.43叠色只提供定性覆盖，不能拿最终RGB证明温度相等。

## Decisions

- 范围仍为普通方块表面后处理；不新增实体/透明材质热模型、Embeddium顶点修改或热表面几何pass。
- 未变化的有效快照与已失效快照必须区分；沿原40-tick poll通知可用性变化，保留原分配供恢复使用，不新增逐块TTL或服务器逐玩家缓存。
- 现有材料的表层平均、暴露热容和缺少固体间导热不能由红外伪装修复。材料真实余热保存仍后续。
- Documentation impact: 当前行为未变，living docs不写入计划目标；实施时需同步红外数据合同、生命周期和局部恢复过滤。

## Validation

- 核对当前QueryPublication、InfraredCapture、两个packet、CPU/GPU提交、shader和BrickMigrationKernel；确认publication允许年龄为40ticks，旧shader有-20..20色标及INVALID冷蓝回退。
- 只读核对项目配置的Forge47.3.0源包中的阶段注册与LevelRenderer/RenderTarget补丁，确认AFTER_CUTOUT_BLOCKS挂点存在及depth格式需要实际识别；这不等于客户端时序已验证。
- 检查计划链接、当前条款一致性、diff空白和标准透视深度量化的数值演算。数值演算不替代真实GPU浮点误差和图像验证。
- 本轮无生产代码修改，未编译或运行游戏；此前55项记录只作为现有回归基线。

## Remaining

- 实施当前有限范围，先验证实际地形FBO/depth与数值取样，再完成材料输入、协议状态及真实场景验收；不得提前报告表面模型已实现或性能最优。
