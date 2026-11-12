# M3-B 整合复核与回退点

- Time: `2026-11-12 15:40:06 +08:00`
- Author: `Codex / OpenAI GPT-6 Astra；主协调与复核`
- Status: `partial`（实现复核完成，客户端整合继续）
- Scope: FH 旧研究承接、TWR 正式土制冶炼炉数据与对应文档。

## Completed

- 复核两工作者的源码、正式项目/成果/旧 nonce、稳定配方 ID 及验证日志；31项 FH 桥接文件与13项 TWR 内容按各自仓库选择性提交，保护无关改动及 TWR 原已暂存的 config/.gitignore。
- 旧已完成接管已有新 run 时，KnowledgeLayer 状态文案优先显示旧完成，避免列表仍写进行中。更新知识 README 入口、正式路线状态、总计划与协调记录。

## Decisions

- M3-C 用现有客户端验证设施和已准备的隔离 Prism 实例检验真实加载；Astra 并行设计 M4，6 Luna 只读准备 M6。未授权推送或发布。

## Validation

- [FH工作者验证](2026-11-12_15-29-57_research-m3b-legacy-bridge-handoff.md)：完整编译与27项必需 GameTest通过，主协调核对 build/m3b-validation-2.log。
- [TWR工作者验证](2026-11-12_15-10-46_research-m3b-twr-earthen-smelter.md)：JSON/引用/文案、Node语法和116条配方生成差异核对通过。
- 主协调 git diff --check 通过。上述27项发生在纯显示顺序修正之前，显示修正的编译与画面验证由 M3-C 接续，不重复声称已验证。

## Remaining

实际完整包 RecipeManager、界面/模型、真实操作、生存材料、多人与独立进程重启仍待集成验收。提交号由[协调记录](../plans/2026-11-12_13-00-00_research-development-coordination.md)登记。
