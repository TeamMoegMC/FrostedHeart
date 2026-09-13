# 红外顶点格式与材料计划继续复查

- Time: `2026-09-11 00:41:05 +08:00`
- Author: `Codex; OpenAI GPT-6; primary engineering agent`
- Status: `completed`
- Scope: `解释并复查Embeddium/Oculus顶点布局，补充现有材料闭环计划；未实施生产代码`

## Completed

- 原位补充[现有计划](../plans/2026-09-08_21-58-58_thermal-unknown-block-ventilation-75.md#material-temperature-closure)的顶点字段表、16-bit owner合同、writer/stride/quad回写/排序/region生命周期和成本对照。
- 核实普通Compact格式20 B，Oculus XHFP格式40 B；不再一律假定20->24。紧凑候选22/42，对齐候选24/44，最终依据实际GPU结果选择一个实现。
- 核实mid-block的乘64、8-bit回绕及ignoreMidBlock路径；不能用其无条件替代显式owner，也不新增异常模型旁路来节省一个字段。
- 明确墓碑数组仅主线程可变，admission/NBT各生成一次独占压实载荷，补充在途内存预算和验证。
- 更新执行基线为最新完整55项Forge测试，保留hNatural一次计算及其他已完成热路径复用，不继续沿用过时的测试类排除假设。

## Decisions

- 热表面温度不写入顶点，顶点只存稳定空间归属/热类别；温度和相机窗口变化不重编mesh。
- 不挪用光照、shaderpack block ID等现有字段，不复制/重编依赖或renderer源码，不实现额外顶点旁表。
- 当前oculus.properties的shaderPack为空，不能将其当成实际扩展格式的客户端通过证据。
- Documentation impact: 本轮只改计划和日记，游戏行为没有变化；living docs不写入预期功能。

## Validation

- 只读javap检查当前Oculus的XHFPModelVertexType、XHFPTerrainVertex、BlockContextHolder、ExtendedDataHelper；核实40字节步长、40/80/120回写与mid-block算式。
- 对照当前材料发布/迁移与最新自然温度、玩家系数代码，核对计划尚未实施及最新回归基线。
- 检查Markdown链接、格式、步长增量计算和mid-block正常/失真例子。未编译或运行游戏，本轮没有生产代码改动。

## Remaining

- 按计划实施A–G；在真实启用shaderpack下验证schema、图像及22/24、42/44候选GPU时间，完成材料生命周期和100观察者回归。
