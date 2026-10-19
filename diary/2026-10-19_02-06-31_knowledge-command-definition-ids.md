# 知识指令：定义 ID 不再需要引号

- Time: `2026-10-19 02:06:31 -0500`
- Author: `Claude; Anthropic Opus 5.5; 修复`
- Status: `completed`
- Scope: `knowledge/KnowledgeCommands`、知识文档

## Completed

- 问题：项目所有者测试时输入 `/knowledge forget result knowledge_example:pressure_control`，得到「参数后应有空格分隔，但发现了紧邻的数据」。
- 原因：`grant`、`forget`、`status`、`dream` 和 `discuss invite` 用 `StringArgumentType.string()` 读取身份。不加引号的字符串在 `:` 处停止，而所有定义 ID 都带命名空间，所以必须加双引号才能用。
- 修复：类型参数改为字面子命令 `observation`、`idea`、`result`。
  - 观察用 `UuidArgument` 读取。
  - 想法与成果用 `ResourceLocationArgument` 读取，并补全已加载的定义，做法与 `research complete` 相同。
  - 指令写法不变。
- 删去 `key(context)` 与 `INVALID_INPUT` 分支：解析阶段已经保证身份合法。原先的分支也漏了一种情况：`new ResourceLocation` 抛出的 `ResourceLocationException` 不是 `IllegalArgumentException`，大写或非法字符会变成未捕获异常。
- 指令树抽成包内的 `KnowledgeCommands.tree()`，事件只负责注册，测试可以直接解析。

## Decisions

- 示例里的 `pressure_control` 是项目，不是成果，所以修复后这条指令仍然不会生效。项目没有知识身份；要重新开始一个已完成的项目，忘记它的一项成果即可。检查路线以 `pressure_plate_design` 为例，因为它不是任何联络的输入，忘记它不会影响其他想法。
- 补全只列出知识定义里的想法与成果；旧研究目录中的成果仍可以手动输入。
- Documentation impact：更新了 [状态与 API](../docs/knowledge/state-and-api.md) 的行政操作说明和 [检查路线](../docs/knowledge/playthrough.md) 的 `pressure_control`。

## Validation

- 新增 `KnowledgeCommandsTest.definitionIdsNeedNoQuotesAndObservationsTakeRecordUuids`：
  - `forget result` 和 `grant idea` 后面跟不加引号的命名空间 ID 时，能解析出完整的资源位置；
  - `status observation` 能解析出记录 UUID；
  - `forget project` 不是可用的写法。
- `./gradlew test --tests 'com.teammoeg.frostedresearch.*'`：BUILD SUCCESSFUL，135 个测试，0 失败。
- 没有在游戏内执行过，需要重启开发客户端。

## Remaining

- 游戏内确认补全列表，以及 `forget result` 之后研究页能再次开始 `pressure_control`。
