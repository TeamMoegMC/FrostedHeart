# 城镇任务书同步至 Prism

- Time: `2026-10-23 22:46:20 +08:00`
- Author: `Codex; OpenAI GPT-6; local deployment`
- Status: `completed`
- Scope: `The-Winter-Rescue-Development/.minecraft 的城镇章节与中英文任务文本`

## Completed

- 按用户要求迁移城镇任务书供启动查看，目标为 `D:/games/minecraft/prismlauncher/instances/The-Winter-Rescue-Development/.minecraft`。
- 复制 `config/ftbquests/quests/chapters/building_town_expansion.snbt`；向 `kubejs/assets/twr_quests/lang/zh_cn.json` 和 `en_us.json` 各合入 122 个城镇文本键。
- 旧文件备份到 `build/prism-backups/2026-10-23_22-46-20/town-quests/`，操作记录为 `build/prism-town-quest-sync-result.json`。

## Decisions

- 源码 HEAD 为 `2d0fefd3`，目标 HEAD 为 `274beba7`，均为 `1.20`；城镇任务书是源码未提交改动，直接迁移工作区内容，未提交或更新分支。
- 初次检查发现语言文件的已提交基线包含独立暖石任务差异，尚未写入文件便停止整文件复制。改为逐键合入城镇改动，保留目标其他语言内容。
- 本次为任务书预览，不构建或替换 FH JAR，不同步其他配方。
- 文档影响：无运行时机制变化，仅记录部署结果。

## Validation

- 写入前未发现 Java 游戏进程，目标三份任务文件没有本地修改。
- 章节与源文件逐字节一致；语言 JSON 可解析，变更键与源文案一致，其他键保留。
- 目标章节通过安装版 FTB Library 的 `SNBT.tryRead` 解析；目标 `git diff --check` 通过。
- `frostedheart-client.toml` 和 `frostedheart-common.toml` 与同步前逐字节一致，存档未改动。

## Remaining

- 用户启动实例检查任务图和分页；本次未启动游戏，文件同步不代表实机验收。
