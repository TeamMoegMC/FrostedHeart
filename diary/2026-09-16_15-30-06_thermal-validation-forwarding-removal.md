# 删除相变请求校验转发并明确保留的职责债务

- Time: `2026-09-16 15:30:06 +08:00`
- Author: `Codex; OpenAI GPT-6; primary engineering agent`
- Status: `completed`
- Scope: `回应删除优先的重构审查，精简请求校验链`

## Completed

- Input 每次 level tick 调用 `phase.tick(queryPublication)`；PhaseController 在处理请求时直接校验已解析 Page 和材料发布。
- 删除 `MinecraftPageManager.matchesMaterialRequest` 纯转发以及 `MinecraftThermalInput.matchesMaterialRequest` 公共入口，必要逻辑归控制器的私有方法。
- 没有增加接口、上下文字段、重启通知或对象；复用控制器已有主线程 MaterialSample，并减少一次重复 Page 查询。样本在世界写入前读完，不在控制器保存 publication。
- 为 SectionOwner 补充具体归属说明：管理器唯一创建/失效、拥有 dirty 队列/Page 句柄与 scratch 交换；保持 static 嵌套位置。
- 当前架构文档和[计划](../plans/2026-09-16_00-15-34_thermal-readability-maintainability-refactor.md)明确检查点仍跨 Input、PageManager、SectionOwner，属于未解决的职责重叠；不再以“紧密关联”证明现有分工合理。

## Decisions

- 前次把删除校验转发与新增依赖替换机制绑定的判断不成立，调用时传当前引用即可。
- 删除不取消校验：Page 生命周期、几何变更、slot generation、发布版本、requestSequence、branch、目标状态和潜热完成条件保持。
- SectionOwner 的嵌套归属与其检查点投影职责是两个判断：前者有实际所有权依据，后者仍有重叠债务。
- Documentation impact: 同步实际校验入口、删除决策及检查点职责债务，没有改写前序 diary。

## Validation

- `gradlew.bat runGameTestServer --offline --no-daemon --console=plain -Dnet.minecraftforge.gradle.check.certs=false`，BUILD SUCCESSFUL，102/102 GameTest 通过。
- 请求校验搜索仅剩控制器私有实现与调用，旧转发无引用；没有新增持久引用或分配。
- 清理本次测试世界、验证日志和游戏日志；无临时源码或额外依赖。

## Remaining

- 检查点编排跨三处的职责重叠仍存在；本次未处理，也不声明为合理的最终架构。
