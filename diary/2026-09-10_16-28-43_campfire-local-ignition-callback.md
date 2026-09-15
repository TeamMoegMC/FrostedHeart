# 篝火启动回调限定到篝火自身

- Time: `2026-09-10 16:28:43 +08:00`
- Author: `Codex; OpenAI GPT-6; primary engineering agent`
- Status: `completed`
- Scope: `CampfireBlockMixin_TimeLimit / LevelChunkMixin_DormantThermal`

## Completed

- 删除全局LevelChunk.setBlockState篝火注入及专用imports；在既有篝火mixin覆盖onPlace、保留父类调用，只处理点燃变化。
- 核对此前事件/tick/请求/辐射路径，未发现另一处同类高频全局挂载。更新现有计划和climate下runtime、生命周期、热源文档。

## Decisions

- 无新增生产状态、缓存或轮询；沿用加载/重载恢复与原发现队列，实体创建前的回调不立即读取BlockEntity。

## Validation

- Java17 runGameTestServer：完整53项真实Forge GameTest全部通过，BUILD SUCCESSFUL；无JUnit、无测试类排除。
- 新增真实打火石操作及Forge取消事件测试；原新放置/加载/重载恢复测试同时通过。日志run-gametest/campfire-local-callback-verification.log；git diff --check通过。

## Remaining

- 未进行客户端FPS基准，本轮收益依据删除全局回调路径确认。
