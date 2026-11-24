# 机械风箱研究

- Time: 2026-11-23 01:28:04 +08:00
- Author: Codex（实现与验证）
- Status: completed
- Scope: FH 手动送风行为与概率配置，TWR 机械风箱分阶段研究。

## Completed

- 手动风箱实际给块炼炉送风后触发想法判定。队伍可联络两条观察，并以青铜小齿轮图纸为可选辅助知识。
- 复用零件排样与封闭房间方块检测。实验 100 游戏刻，完成后开放正式配方。旧完成效果承接图纸。
- 更新[知识文档](../docs/knowledge/mechanical-bellows.md)、概率定义和旧目录处置表。

## Validation

- Java 编译及[Forge GameTest](../build/bellows-gametest.log)通过，含新增真实手动风箱触发用例。
- TWR 研究数据、双语键、旧效果映射与配方脚本语法通过静态校验。

## Remaining

- 正式整合包客户端生存流程待试玩。旧研究已扣除活塞的补偿未包含在本次迁移。
