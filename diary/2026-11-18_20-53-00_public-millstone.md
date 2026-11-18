# 开放石磨工作台配方

- Time: `2026-11-18 20:53:00 +08:00`
- Author: `Codex / OpenAI；实现与验证`
- Status: `completed`
- Scope: TWR 旧研究目录、石器时代任务及 Research Playtest 数据。FH 更新现行文档。

## Completed

- TWR 移除 `millstone`（固体研磨）旧研究定义，解除 `stone_age:millstone` 的配方权限。继续使用原工作台配方。
- 石磨任务 `7887EB905BAA7BEF` 从工作台任务 `03A553EBD453466A` 接出，移除研究解锁提示，保留任务 ID、物品目标与黑麦奖励。
- FH 更新[旧研究处置](../docs/research/legacy-content-disposition.md)和[普通耐火砖材料路线](../docs/knowledge/refractory-brick.md)。

## Decisions

- 石磨随工作台开放。旧定义没有下游研究父项引用。旧队伍数据保留在 `TeamResearchData`，配方权限由当前目录重新算得。

## Validation

- TWR 检查了剩余80份 JSON，确认没有研究效果封锁石磨，也没有父项指向退役项目。核对任务变更范围。
- FH 使用 Java 17 执行 `validateResearchCatalog`，80份定义通过。Java 编译输出由 Gradle 复用。
- Research Playtest 关闭时同步 `t1.snbt`，核对该文件字节一致及实例旧目录剩余80份。旧定义移至备份，与原任务文件一起保存在 `build/prism-backups/2026-11-18_20-52-59-public-millstone/`。

## Remaining

- 客户端尚未实测新队伍的石磨合成与任务显示。
