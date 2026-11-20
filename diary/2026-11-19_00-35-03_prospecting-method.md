# 探矿镐使用研究

- Time: `2026-11-19 00:35:03 +0800`
- Author: Codex（OpenAI，实现与验证）
- Status: completed
- Scope: FrostedHeart 的探矿入口、知识系统与实验台，以及 TheWinterRescue 的研究定义和任务书。

## Completed

- 铜探矿镐公开制作。新研究授予队伍右键探测权限，旧完成或旧配方效果承接使用权限，部分进度承接想法。旧进阶地质工具以有效的研究完成为前置。
- 实验台按 Forge 工具动作接受普通镐和 Tetra 探矿工具，600 tick 后返还投入的工具。更新了[玩家流程](../docs/knowledge/prospecting.md)、[知识定义](../docs/knowledge/definitions.md)和[旧目录处置表](../docs/research/legacy-content-disposition.md)。

## Decisions

- 挖矿触发检查实际成功破坏的 `#forge:ores` 方块和挖矿者的 `create:copper`。将 1% 基础与每项 1 个百分点配置在该想法，保留联络独立入口。
- 使用 `#forge:stone` 与 `#forge:ores` 匹配房间方块。实验正常结算原样返还工具，手动停止或切换沿用实验台取消交付的规则。

## Validation

- Forge 47.3.0 GameTest 首轮 18 项通过。覆盖正式定义、挖矿事件、观察、概率、实验台、权限与旧档兼容。补充一轮使用整合包真实 Tetra 模块和替换定义验证联络、计时以及原物返还。
- Java 17 的 `jar reobfJar` 通过。TWR 80 份旧目录定义预检通过，正式 JSON 和中英文键核对通过。两个仓库的 `git diff --check` 通过。

## Remaining

- 未部署 Prism，未进行客户端界面或完整生存验证。
