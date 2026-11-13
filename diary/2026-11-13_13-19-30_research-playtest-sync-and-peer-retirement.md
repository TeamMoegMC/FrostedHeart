# Playtest 同步与 Peer 实例退役

- Time: `2026-11-13 13:19:30 +08:00`
- Author: `Codex / OpenAI；部署`
- Status: `completed`
- Scope: 两个研究专用 Prism 实例，不涉及 Development 或 runClient。

## Completed

- 用户指定后续更新暂时同步至 `D:/games/minecraft/prismlauncher/instances/The-Winter-Rescue-Research-Playtest-20261112`。
- 将 f8f15e4d9 的生产 JAR（14,908,433 字节）同步至 Playtest，包括整页研究详情、默认札记入口和鼓风口模型修复。
- 双客户端验收已结束，Research Peer 无需常驻；确认未运行后将 `The-Winter-Rescue-Research-Peer-20261112` 移入 Windows 回收站。自动审批拒绝直接递归删除，改用可恢复的回收站操作成功。
- 旧 Playtest JAR 备份及部署清单位于 `build/prism-backups/2026-11-13-research-playtest/`。

## Validation

- 安装包与构建产物 SHA256 一致，mods 中只有一份 FrostedHeart。
- 前后核对 311 份配置、options.txt/instance.cfg 及存档目录名称/时间均不变；没有修改存档内容。
- Peer 原目录已不存在；Playtest 保留，未启动游戏。
- TWR 本轮没有新内容需要同步，也没有修改其源码或原有暂存 config/.gitignore。

## Remaining

- 本次 UI 与模型的游戏内视觉验收仍待进行。部署不等于运行验收；living docs 无游戏行为新增，本次仅记录部署目标及结果。
