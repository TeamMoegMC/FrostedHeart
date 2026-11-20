# 探矿镐现场获得想法

- Time: 2026-11-20 21:09:31 +08:00
- Author: Codex（OpenAI，实现与验证）
- Status: completed
- Scope: 探矿镐右键探测入口

## Completed

- 服务端先检查探测权限。缺少权限且队伍档案和来件中均无想法时，直接授予探矿想法并提示。普通镐和 Tetra 共用入口。
- 更新[探矿研究流程](../docs/knowledge/prospecting.md)。

## Validation

- Java 17 下 Gradle compileJava 通过。

## Remaining

- 待客户端实机验证。
