# 统一收缩当前计划到方块表面红外

- Time: `2026-09-11 00:49:19 +08:00`
- Author: `Codex; OpenAI GPT-6; primary engineering agent`
- Status: `completed`
- Scope: `统一当前计划和旧红外入口；未改生产代码`

## Completed

- 按用户“目前只做方块表面热辐射，其他以后再做”的要求，重写[当前计划](../plans/2026-09-08_21-58-58_thermal-unknown-block-ventilation-75.md#block-surface-infrared)的活动部分，取消原完整A–G作为当前执行路径。
- 当前仅保留已有材料节点读取、显示增量/小背景、原后处理地形取样；明确用方块侧取值，不再拿前方Air温度代表墙面。
- 顶点扩展、专用热表面几何pass、实体/衣物、透明材质、自定义BE、导热重构、材料存档、温度计/作物/玩家辐射等统一列为后续。旧顶点方案是未选定候选，不是以后必做的任务。
- [旧红外计划](../plans/2026-08-29_18-38-09_infrared-mesh-temperature-rendering.md)的跳转与范围说明同步更新；保留material-temperature-closure旧锚点，避免历史链接失效。

## Decisions

- 复用屏幕颜色/深度、现有温度纹理和网络。当前不改变Embeddium/Oculus顶点格式，不新增世界几何绘制。
- 为避免实体/粒子被误着色，当前方案在普通solid/cutout地形完成后、实体等绘制前执行原后处理。透明层、自定义BE及真实shaderpack特殊时序单列后续，明确当前支持边界。
- 本阶段只显示已有热模拟状态，不宣称修复材料传热和余热恢复。其他系统缺口不再成为红外实现的隐含依赖。
- Documentation impact: 本轮仅改计划/日记，游戏行为未变；当前living docs保持实现真相，实施后只同步相关红外读取/网络/渲染说明。

## Validation

- 对照当前AFTER_LEVEL调用、renderLevel HEAD相机捕获、shader采样和材料publication核对现状。
- 重写时逐字比较“先前方案与实施记录”以下内容，确认已有实施历史完整保留。
- 检查本轮Markdown链接、活动范围与旧入口一致性、git diff --check；无生产代码变更，未编译或运行游戏测试。

## Remaining

- 实施当前方块表面红外并完成真实Forge和客户端地形阶段验证；后续清单不纳入本阶段完成门槛。
