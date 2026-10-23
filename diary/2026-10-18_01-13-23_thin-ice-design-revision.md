# 完整薄冰概率开裂与回冻设计核对

- Time: `2026-10-18 01:13:23 +08:00`
- Author: `OpenAI Codex；主协作代理，设计核对`
- Status: `completed`
- Scope: `完整薄冰设计讨论与现有温度/相变接口核对；不包含实现`

## Completed

- 将用户本轮的姿态概率开裂、六面邻水破碎结果、低温邻水恢复和高温影响追加到[讨论](../discussion/2026-10-17_23-27-52_ice-caves-and-thin-ice.md)。补充确认离开后停止踩踏推进、保留裂纹。
- 核实当前水结冰阈值为 -5°C、水源冷却目标为完整薄冰；指出已有相变条件与六面邻水修复不同。
- 核实随机刻和被动气温查询入口，为温暖环境自裂提出避免全量轮询的建议，未宣称实测性能。
- 文档影响：[薄冰草稿说明](../docs/climate/thin-ice.md) 标明旧行为不符合本轮设计，并移除已撤回生成器方法的代码锚点。

## Decisions

- 本轮确认限于完整薄冰，分层薄冰不自动继承全部修订。
- 设计未谈完前不继续实现、模型或构建；具体概率、落地冲击曲线及回冻过程仍待讨论。

## Validation

- 只读核对 `ThinIceBlock`、`LayeredThinIceBlock`、`WorldTemperature`、`StateTransitionData`、`MinecraftPhaseController`、`ServerLevelMixin_TemperatureUpdate` 和现有相变配方。
- 未修改代码或数据，未运行编译、GameTest、性能测试或客户端试玩。

## Remaining

- 明确修复裂纹与破碎后水源回冻的范围、修复速度以及与现有 -20°C 普通冰相变的关系。
- 定阶段概率、温度倍率、落地冲击曲线和抽样单位，再实现并试玩。
