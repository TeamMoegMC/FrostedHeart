# 能量核心再利用

- Status: Current
- Last verified: 2026-11-23
- Scope: 《冬季救援》一级能量塔研究、配方和旧档接续。
- Code anchors: `KnowledgeBehaviorBridge.obtained`、`LegacyResearchBridge.claimItemRewards`、`ResearchHooks.canFormMultiblock`。

玩家首次取得任意 `#forge:ingots/copper` 时，队伍获得「能量核心再利用」的想法。已经持有铜锭但未收录想法的队伍可以再取得一枚。T1 任务「长久之计」指引玩家打开绘图台开展研究。

先用 1 级转镜子校核核心的导热路线。第一次定稿后推进阶段，再用填满格子的拼图补齐核心缺口。两关各需一次定稿。第二关完成后，在项目详情完成研究并领取一枚能量核心。两道题模拟修补过程，开题和定稿均无实物提交。

成果开放 `the_winter_rescue:research/generator_core_t1`，用一枚能量核心、四块红砖和四枚铜锭制作塔芯。搭建成果开放 `frostedheart:multiblocks/generator` 的工程师锤成形权限。「一级能量塔」备料任务要求一枚塔芯、八个一级增幅器和二十一块能量塔砖。

旧 `generator_T1` 已完成的队伍承接配方、搭建权限和完成信用。原核心领取状态仍由旧效果 `4994b13a11204080` 记录，旧完成未领和新项目完成的队伍都从同一队伍记录领取。旧研究有投入的未完成队伍可通过已有桥接获得研究想法，旧效果分别映射，原投入留在旧档。新完成信用供 `generator_T2`、`generator_efficiency_1`、`generator_regain` 和 `wool_armor` 判断父项。

[整合包项目](../../../TheWinterRescue/kubejs/data/the_winter_rescue/frostedresearch/knowledge/projects/generator_t1.json) · [旧研究处置](../research/legacy-content-disposition.md)
