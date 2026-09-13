# 材料与相变热状态全面复查

- Time: `2026-09-12 23:15:00 +08:00`
- Author: `Codex; OpenAI GPT-6; primary engineering agent`
- Status: `completed`
- Scope: `材料分类、相变/换热、状态迁移和恢复、温度消费者与红外的源码复查；未修改生产/测试代码`

## Completed

- 输出[完整调查报告](../plans/2026-09-12_23-12-00_thermal-material-phase-coverage-review.md)，列出15类有代码依据的行为、后果、性质和修复方向。
- 确认相变替代显热状态、配方强制V0、同Brick能量池和最低bit转换、按profile整池迁移、转换目标无连续温度状态。
- 确认非相变含水固体可能无材料profile、植物明确排除、mixed共用Air/材料状态、暴露面与驻留决定节点和热容、无V0/V0导热。
- 确认同签名替换缺少物质身份语义、材料/phase持久化缺失与mixed capture/restore口径差异、温度计仍读环境、拓扑间隙取最暖transport。
- 将相变玩法双轨、source功率/表面温度分离及蓝色缺值歧义作为明确的语义/范围限制记录，不把每项都称作独立运行错误。
- Documentation impact: 修正`docs/climate/heat-production-and-network.md`中过期的材料Air恢复、dynamic-shape未解析和microcells描述，补充当前模型边界；现行红外plan增加报告入口。既有日记未改。

## Decisions

- 不通过取消phase红外过滤把固定转换阈值当当前材料温度。
- 修复依赖先材料状态/显热相变及生命周期，再空间聚合、几何与能量迁移，再覆盖/持久化，最后消费者和显示。此顺序不等于本轮已授权实施全部后续系统。
- 明确源码可达行为与用户场景实测的区别；不根据截图把每个蓝块认定为无节点或某个profile。

## Validation

- 核对Profiles/StateTable、BrickTopologyCompiler/TopologyView/BrickMigrationKernel、Arena/PhaseStore/PhaseTransitionRuntime、PhaseController和legacy转换、Dormant capture/restore、source绑定、SoilThermometer和环境查询、红外合成与shader。
- 确认空phase池不能无限放热；HotMask仍保留热材料自身Brick，不能错误描述成材料完全不影响驻留。
- 本轮未重复编译、GameTest或GPU测试；这些是模型/接线复查，不以先前58/1,440用例通过冒充物理闭环证据。
- 文档链接与diff空白检查通过；没有改design或伴生整合包。

## Remaining

- 将核心材料状态与相变生命周期修复定案并实施相应用例；完整后续范围见报告。
- 用户存档中的逐点材料值、缺值和场命中仍需运行期证据。
