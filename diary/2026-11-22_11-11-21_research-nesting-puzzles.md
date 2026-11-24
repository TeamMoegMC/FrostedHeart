# 方块排样研究任务

- Time: 2026-11-22 11:11:21 +08:00
- Author: Codex / OpenAI，研究玩法实现与验证
- Status: completed
- Scope: Frosted Research 绘图台、阶段任务、双语文本与 Research Playtest

## Completed

- 支持布置全部零件与铺满槽面两种目标，随机生成有解盘面，支持旋转、右键整件回盘、撤销、换题、草稿续玩与定稿。
- 更新[作者指南](../docs/knowledge/authoring-guide.md#方块排样)及阶段文档。隐藏试玩项目为 `frostedresearch:nesting_debug`。

## Decisions

- 两种模式各有一个小游戏 ID，由内容作者选择，暂用同一默认等级。复用折线已有的图纸保存和定稿路径，兼容已有折线存档。
- Research Playtest 使用本次生产 JAR，原 JAR 保存在 build/nesting-client-backup。此次仅增加试玩项目和可配置任务。

## Validation

- Forge 定向测试 4/4。两个模式各抽查 1024 盘，包括有解、留空与剩件；另验证实机绘图台服务端存读、回盘、定稿及折线回归。
- 正式整合包隔离实例新世界两种模式均通过实际客户端界面操作。确认 R 键旋转，右键点已放零件的非首格即可整件回盘，撤销恢复原位置和朝向，定稿后队伍获得对应任务进度。截图与结果在 build/nesting-client/，临时客户端验证模组已移除。

## Remaining

- 本轮使用夹具开启试玩研究。多人同桌与完整生存流程尚未实玩。
