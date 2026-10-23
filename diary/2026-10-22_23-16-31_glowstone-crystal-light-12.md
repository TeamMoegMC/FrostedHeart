# 荧石晶体亮度调整为12

- Time: `2026-10-22 23:16:31 +08:00`
- Author: `OpenAI Codex; 主代理`
- Status: `completed`
- Scope: `FHBlocks.GLOWSTONE_CRYSTAL` 注册亮度

## Completed

- 按用户要求将荧石晶体亮度从7改为12。
- 文档影响：同步 `docs/worldgen/ice-caves.md` 当前亮度。

## Decisions

- 只调整注册常量，不改变生成或大小冰晶亮度。

## Validation

- 核对源码差异及 `git diff --check`；常量修改未新增测试或重新打包。

## Remaining

- 此次仅修改源码，尚未重新构建或更新 Prism JAR。
