# 成年北极熊反击与撤退

- 作者：Codex
- 日期：2026-11-14 UTC
- 状态：实现完成，编译验证通过，游戏内验证待进行

新增伤害来源优先、15 秒无法接触及低于 25% 血量撤退；撤退持续 10 秒，保留幼熊及原白天敌对行为。行为说明位于 `docs/polar-bear-combat.md`。仓库没有 diary/README.md，参照最近日记格式。

验证：运行 `./gradlew compileJava --offline`；结果 BUILD SUCCESSFUL；修正 tick 注入点后无新增北极熊注入警告，仍有 21 条已有编译警告；git diff --check 通过。尚未进行游戏内验证。
