# 转镜子规则与界面
- Time: 2026-11-24 16:32:15 +08:00
- Author: Codex / OpenAI
- Status: completed
- Scope: 绘图台转镜子界面和双语规则

## Completed
左页给出热辐射目标、起点终点及鼠标操作。移除批注按钮、答案标记和对应文案。已更新作者指南。

## Decisions
各研究共用同一转镜子界面。

## Validation
双语 JSON 解析通过，Java 17 jar reobfJar 构建成功。

## Remaining
客户端画面待重启换包后验收。

发布复查在独立目录 build/foldline-release-review-1124 运行，2 项 Forge GameTest 通过。中英文各 21 个转镜子界面键一致。
