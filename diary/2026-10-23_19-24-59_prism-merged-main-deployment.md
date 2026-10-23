# Prism 更新至合并后的主分支

- Time: `2026-10-23 19:24:59 +08:00`
- Author: `OpenAI Codex；主代理`
- Status: `completed`
- Scope: The-Winter-Rescue-Development 实例

## Completed

- FH master `ebe4c38d55af0f6258bb53d284b821502c25b664` 构建并替换实例中的 frostedheart-1.20.1-0.7.7.jar（13897274 字节）。
- 实例切换到1.20，从本地TWR仓库快进到 `274beba7720852437de19d371b52be27fd722e28`，无需远端push。
- 旧JAR备份：`D:/games/minecraft/Project TWR/FrostedHeart/build/prism-backups/2026-10-23_19-24-59`。
- 两份实例配置原样保留，未操作存档；未同步来源仓库未提交改动。
- 文档影响：仅部署已实现内容，无需更改系统文档。

## Decisions

- 使用jar/reobfJar生成生产JAR，复用上一轮已完成的合并回归测试。

## Validation

- Gradle jar reobfJar离线构建成功，1m8s；日志build/prism-main-build.log。
- JAR ZIP完整性及关键feature class存在检查通过，已删除的温泉定位class不在包内。
- 安装文件与构建产物逐字节一致；实例HEAD与TWR 1.20一致；配置内容与更新前一致。
- 部署记录build/prism-main-deployment-result.json。

## Remaining

- 未启动客户端，本条仅确认构建和安装完成；上一轮54/54 GameTest及配套生成采样通过。
