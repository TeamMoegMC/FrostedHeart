# 探矿工具解锁合并

- Time: `2026-11-21 23:42:30 +08:00`
- Author: Codex（OpenAI，实现与验证）
- Status: completed
- Scope: FH 旧研究承接机制及 TWR 探矿研究

## Completed

- 将「地质学初步」和「勘矿升级」的八项制作配方并入现有「探矿镐的使用方法」，更新研究说明和 T1 任务提示。
- 更新[探矿流程](../docs/knowledge/prospecting.md)、旧目录处置表及旧研究承接契约。

## Decisions

- 沿用原 600 tick 探矿镐实验和铜探矿镐公开配方。八项进阶配方随研究完成一并开放，按原配方使用制作材料。
- 旧效果按 nonce 分项承接，旧三项全部完成时记为整个项目完成。曾完成新版实验的队伍从原有研究记录补齐配方成果。

## Validation

- Java 17 下编译通过，Forge `frostedresearch_m3b` 的 7 项必测通过。新增场景检查旧单项完成、分项效果、多旧项完成和已完成新版实验的补领。
- 执行配方脚本并检查八个配方注册及产物、原效果映射、铜探矿镐入口和中英文提示。

## Remaining

- Research Playtest 实例待同步和客户端实机验收。
