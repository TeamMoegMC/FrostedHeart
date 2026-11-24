# 传动机械旧研究替换补齐试玩部署

- Time: `2026-11-24 18:20:01 +08:00`
- Author: Codex（OpenAI，部署与验证）
- Status: completed
- Scope: Research Playtest 实例、传动机械观察规则和旧档承接。

## Completed

- FH 提交 `547adfc86`，TWR 提交 `2fdf1de8`。更新旧研究处置表的四项映射。
- 实例退出后同步生产 JAR、项目定义及中英文的传动任务提示。部署前备份在 [Prism 备份](../build/prism-backups/2026-11-24_18-19-26_transmission-observations)。

## Decisions

- 实例中的共享语言文件按传动任务键更新。六条配方、五份其它知识定义和任务书提示已匹配，因此沿用实例文件。

## Validation

- 生产 JAR 编译成功，部署文件完成字节校验。原旧定义与 TWR 同版，四份设计引用的六条配方 ID 均可从实例脚本解析。
- [现有存档回放](../build/transmission-save-replay-gametest.log)通过。在独立 Forge 测试进程中读取试玩存档，验证进行中的研究在初始化时补记两项材料观察，拼图仍单独判定。

## Remaining

- 试玩客户端重开后的界面待实看。
