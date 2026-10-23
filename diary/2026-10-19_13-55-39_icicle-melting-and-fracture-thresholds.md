# 冰锥熔化与热裂温度调整

- Time: `2026-10-19 13:55:39 +08:00`
- Author: `Codex; implementation agent`
- Status: `completed`
- Scope: `IcicleBlock.HEAT_THRESHOLD_C`、冰锥相变配方、冰锥 GameTest 与气候文档

## Completed

- 按用户要求，将材料熔化平台从 5°C 改为 2°C，将环境热裂门槛从 9°C 改为 5°C。
- 同步阈值边界/基准/倍增测试断言，以及真实 worker 潜热预算用例的初始平台温度。
- 文档影响：更新[冰锥说明](../docs/climate/icicle.md)的公式与相变平台，以及[热源和材料说明](../docs/climate/heat-production-and-network.md)；明确先前回归结果对应旧阈值。

## Decisions

- 继续以材料温度驱动熔化、环境温度驱动热裂；第二层热裂概率从新门槛每升高 5°C 翻倍，原有潜热预算和预警倒计时规则沿用。

## Validation

- 相变 JSON 解析成功，确认 `heating.temperature_c=2.0`。
- 核对热裂边界为 4.99°C 无概率、5°C 基准 1/8、10°C 为 1/4（均为第二层概率）。
- `git diff --check` 通过。遵照用户工作流，本次未打包，也未重跑 GameTest。

## Remaining

- 在下次游戏测试时确认新阈值的实际体验。
