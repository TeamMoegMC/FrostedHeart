# 原版碎冰踩踏与冷却调度核对

- Time: `2026-10-18 11:54:07 +08:00`
- Author: `OpenAI Codex；主协作代理，源码调查`
- Status: `completed`
- Scope: `Java 26.4 Snapshot 4 的 BrittleIceBlock 检测方式；不包含 FH 开发`

## Completed

- 下载 Mojang 官方快照 client JAR 到 `build/vanilla-inspection/26.4-snapshot-4/`，只用 JDK 21 `javap -c -p` 检查，未启动或编译快照。
- 查明实体脚下回调驱动碎冰开裂；24 ticks scheduled tick 仅充当冷却，空 tick 本身不推进裂纹。
- 查明阶段推进无概率/姿态倍率、破碎传播到六面同类邻块，区别于 FH 设计。
- 文档影响：仅追加[讨论](../discussion/2026-10-17_23-27-52_ice-caves-and-thin-ice.md)中的原版实现核对，不改变当前 FH 行为说明。

## Decisions

- 回答依据官方 JAR 字节码，区分实际调度代码与只描述玩法的快照说明。

## Validation

- 检查 `BrittleIceBlock`、`CrackingIceBlock`、`IceBlock`、`BlockBehaviour` 和 `Entity`；可复查的 javap 文本位于上述 build 目录。
- 未修改 FH 源码、数据或模型，未运行构建、GameTest 或试玩。

## Remaining

- 将姿态/温度概率映射和落地冲击方案讨论清楚后，再实现 FH 版本。
