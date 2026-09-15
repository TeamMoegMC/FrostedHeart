# 明确类现实表面热像的最小物理模型

- Time: `2026-09-11 20:36:47 +08:00`
- Author: `Codex; OpenAI GPT-6; primary engineering agent`
- Status: `completed`
- Scope: `解释并补充当前红外架构的物理含义；未实施生产代码`

## Completed

- 在[当前计划](../plans/2026-09-08_21-58-58_thermal-unknown-block-ventilation-75.md#block-surface-infrared)补充黑体表面近似、服务端温度到客户端后处理的架构图和对应验收。
- 明确epsilon=tau=1时标定后的表观温度等于表面温度，可直接使用现有温度纹理，不需要运行期T⁴/反演、光谱积分或新的辐亮度缓存。
- 明确最近不透明表面遮挡、距离不按1/r²降低表观温度、视角不乘cos、观察不改变热状态。
- 完整圆形覆盖、材料优先和自然热平衡背景估计保留；实体/透明材质等仍后续。

## Decisions

- 黑体近似是当前物理假设，不是新增材料参数系统。自然背景只是未模拟表面的热平衡估计，不是局部热Air测量。
- 原0.43融合属于增强显示，不能宣传为定量相机原始输出；纯热像、分材质发射率和反射以后单独确定。
- 不扩大到Embeddium顶点改造、新几何pass或完整材料热学重构。
- Documentation impact: 本轮只修改计划/日记，当前代码仍使用原Air/解析场显示路径，living docs不改成预期状态。

## Validation

- 复核当前InfraredCapture仍使用transportAt、shader仍使用旧温度纹理与色标；没有把设计当成已实现。
- 核对黑体公式的量纲、温度标定抵消关系、计划链接与Markdown格式。
- 未编译或运行游戏；物理行为及实际图像仍按计划进行实现后验收。

## Remaining

- 实施当前有限方块表面阶段，验证材料输入、完整扫描覆盖、最近表面遮挡及距离/视角不伪造温差。
