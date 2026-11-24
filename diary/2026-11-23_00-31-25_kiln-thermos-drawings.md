# 土制冶炼炉与保温杯图纸

- Time: 2026-11-23 00:31:25 +08:00
- Author: Codex（OpenAI，实现与验证）
- Status: completed
- Scope: TWR 正式项目、双语任务说明，FH 小炉旧档承接

## Completed

- 土炉先用填满的拼图规划炉壁，定稿后临时开放三种配比的随身合成。推进阶段后试烧并观察三件样品。
- 保温杯改用用完零件的拼图。更新两个项目的研究详情、任务书及[土炉](../docs/knowledge/earthen-smelter.md)、[小炉](../docs/knowledge/trial-kilns-and-samples.md)、[保温杯](../docs/knowledge/thermos.md)文档。

## Decisions

- 旧单阶段土炉研究承接到试烧阶段，补发本次研究的试样配方，保留已获观察与完好样提交记录。

## Validation

- Forge GameTest 共 7 项通过，包括两份正式图纸的服务端定稿、三张配方的权限切换、试烧观察、旧档迁移与保温杯实验台产出。输出在 build/puzzle-distribution-gradle.log。
- 检查两个仓库的相关差异及 TWR 中英文文案引用。

## Remaining

- 本次验证使用专用服务端，尚未在整合包客户端走完这两条新流程。
