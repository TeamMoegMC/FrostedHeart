# 北极熊撤退中短暂反击

- 作者：Codex
- 日期：2026-11-14 UTC
- 状态：实现完成，编译通过

逃跑时遭到新的实体攻击，暂停撤退并反击 2 秒，随后继续剩余撤退时间；连续受伤不延长当前反击。同步更新 docs/polar-bear-combat.md。

验证：./gradlew compileJava --offline 成功（21 条已有警告）；git diff --check 通过。尚未游戏内验证。
