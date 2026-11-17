# 温度数值目标更正为旧版生存与供暖体验

- Time: `2026-11-05 15:30:47 +08:00`
- Author: `Codex; OpenAI GPT-6; primary engineering agent`
- Status: `completed` for target correction and source-level balance investigation; calibration outstanding
- Scope: 温度玩法数值对照与气候文档；没有修改生产参数

## Completed

- 用户明确允许新机制，要求旧危险环境仍然危险、供暖和衣物收益大致保持。更正此前把目标解释成恢复旧公式/显示的建议。
- 更新[数值对照](../docs/climate/thermal-legacy-gameplay-parity.md)，保留已核对事实，改用失温、伤害/死亡时间、恢复、防护与资源代价作为校准标准。
- 根据已核对源码计算普通难度、食物充足、干燥无风无装备、空气 −80°C、各部位初始 33.5°C 的第一步：旧核心继续降约 0.00060955°C；当前失热约 139.9844 W，基础加最强御寒 153.125 W，净增约 13.1406 W，开始回温。该条件不是现场死亡测试，不代表有限食物下永久安全。

## Decisions

- 保留新的身体、传播、蓄热和相变机制；公式差异本身不直接作为错误，不默认回退旧系统。
- 先检查冷环境持续失热能否被御寒完全抵消，再校准时间尺度。单调身体时间倍率不能改变固定状态的净功率方向。
- 此前审计记录属于当时的理解；用本条补充更正，不重写日记历史。

## Validation

- 复核当前 `PlayerThermalModel`、`PlayerThermoregulation`、身体异常阈值及旧 `TemperatureUpdate`。
- PowerShell 按源码公式计算上述固定状态的温变/功率；没有运行 JUnit、Forge 压力矩阵或历史工程编译。
- 本轮只修改相关文档；生产数值和整合包没有改变。

## Remaining

- 需要按实际配置、有限食物和真实天气测量代表场景，再统一校准和重点验收；目前没有旧新版完整死亡/恢复时间对照。
