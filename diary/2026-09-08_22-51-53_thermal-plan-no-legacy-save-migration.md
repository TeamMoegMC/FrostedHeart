# 热模型计划取消旧存档兼容

- Time: `2026-09-08 22:51:53 +08:00`
- Author: `Codex; OpenAI GPT-6; primary engineering planning agent`
- Status: `completed`
- Scope: `Brick 修复与方块级热模型计划；无 Java 改动`

## Completed

- 按用户明确的“尚未投入生产，不考虑旧存档”更新[工程计划](../plans/2026-09-08_21-58-58_thermal-unknown-block-ventilation-75.md)，取消旧格式读取、版本迁移、双格式适配及相应验收。
- 本条修正[前一条工程方案记录](2026-09-08_22-43-57_thermal-brick-block-model-engineering-plan.md)中的旧存档转换决定，保留历史记录原文。

## Decisions

- 只维护方块级新格式的保存与加载，保留有限预算、衰减时间、源支持及重启恢复。
- 运行中拆建方块／门开关的热量迁移，以及休眠期间布局变化后的按块恢复仍是必要行为。

## Validation

- 检查计划相关条目、相对链接和文档空白；没有运行 GameTest、JUnit 或性能测试。
- 文档影响仅为计划与本条记录；生产行为未改变，无需修改 living docs。

## Remaining

- Java 实施与真实 Forge GameTest／生产路径验收仍待进行。
