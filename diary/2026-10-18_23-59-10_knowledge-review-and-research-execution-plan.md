# 知识系统审阅与研究项目执行计划

- Time: `2026-10-18 23:59:10 -0500`
- Author: `Claude; Anthropic Opus 5.5; 代码审阅与计划起草`
- Status: `completed`
- Scope: `discussion/、plans/；合并 P0-1 与 P1 两个修复分支（源码改动由各自会话完成，见其 diary）`

## Completed

- 审阅知识系统在 `a8d89e5f2` 的开发状态，结论写在
  [知识系统审阅](../discussion/2026-10-18_23-01-04_knowledge-system-development-review.md)。
  - P0-1：单条记录解码失败会导致整个队伍文件被跳过，并在下次保存时删除。
  - P1-1：全量快照的体量与计算随档案增长。
  - P1-2：服务端权限投影每次查询都整体重建。
  - P2：七项体验问题与潜在隐患。
- P0-1 与 P1 交给两个独立会话修复；按项目所有者要求，把两边的改动提交到各自分支并合入：
  - [P0-1](2026-10-18_23-54-59_knowledge-state-unreadable-entries.md)：`05f3aa8b4`；
  - [P1](2026-10-19_00-00-13_knowledge-sync-coalescing-projection-cache.md)：`1bdf58fba`；
  - 合并提交 `0c448bb3f`。`docs/knowledge/state-and-api.md` 两边改的是不同小节，自动合并无冲突。
- 在审阅文档末尾追加「后续」，链接上述修复。
- 起草并确认 [知识研究项目执行计划](../plans/2026-10-18_23-42-16_knowledge-research-project-execution.md)：
  - 把 V2 计划与 `design/knowledge_research.md` 对齐；
  - 给出计算/证据/实验的最小方案；
  - 25 项决定由项目所有者确认，全部采用推荐方案，计划状态为 `ready`。
- 按决定 A1 把 [V2 计划](../plans/2026-08-25_16-17-39_generative-research-knowledge-laboratory-v2.md) 标为 `superseded`。只改了状态行，并在 Outcome 末尾补记原因，原文其余部分保持不变。

## Decisions

- P2 类问题暂缓，由项目所有者决定。P2-7 并入研究所计划；P2-1 在增加观察来源时一并处理。
- 研究项目执行先于知识来源接入。设计文档把研究玩法留到「知识系统基础完成后」展开，现在正好到这一阶段。
- V2 中「纸牌局产生想法」「讨论作为研究劳动」「工作页缺口编译」「审阅后发布」与设计冲突，不采用；实验台、研究所、Prototype 章节保留作为参考。
- 两项修复用合并提交保留各自分支的历史，两个会话分支的提交都成为合并结果的祖先。
- Documentation impact：本条记录只新增 discussion、plan 与 diary，并更新 V2 计划的状态。行为改动的文档更新已随两项修复提交。

## Validation

- 审阅与计划中的事实逐项对照源码核实（文件与行号见两份文档）。
- 合并前逐个核对两个会话的 diff，与各自 diary 的描述一致。
- 合并结果上 `./gradlew test --tests 'com.teammoeg.frostedresearch.*'`：BUILD SUCCESSFUL，35 个类、118 个测试，0 失败（原有 108 个，加 P0-1 的 6 个和 P1 的 4 个）。
- 合并结果上未重跑 GameTest；两个会话各自在分支上跑过，结果见其 diary。
- 相对链接全部可解析；`git diff --check` 通过。
- 审阅中的体量与耗时是按代码推算的量级，未实测。

## Remaining

- 研究计划 R1 的前置已满足，可以开工。
- P1-1 第 2 步（获得历史不复制元素）与第 4 步（观察等价签名）未做。
- Chorda `CTeamDataManager` 加载失败即跳过、保存时删除文件的风险仍在，属于 Chorda。
- 两项修复都还没在真实客户端和多人专用服务器上验证。
- P2 各项未处理。
