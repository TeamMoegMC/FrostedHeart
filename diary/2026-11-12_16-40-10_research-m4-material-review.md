# 冶铁原型材料链复核更正

- Time: 2026-11-12 16:40:10 +08:00
- Author: Codex / OpenAI GPT-6 Astra；主协调复核
- Status: partial（源码路线核实，真实生存待验）
- Scope: docs/knowledge/iron-bloomery.md、TWR iron_materials 触发标签；不修改配方。

## Completed

复核 TWR change.js 的实际图案、按产物移除逻辑、shapeless.js 组块、树桩台阶配方与当前 Charcoal Pit 7748097 JAR 的陶窑烧砖配方。修正工作者补核文档中的数量计算：风箱4防腐木+2台阶，合计5木板、625mB；收集器4单砖；8单砖按正式替代配方需10沙+8黏土球，原生较省沙的坯料配方已被移除。

## Decisions

保留既有配方；砂砖可用原生陶窑烧成，普通桶可用青铜板，源码不存在必须先完成冶铁炉的材料环。此前工作者日记中7木板/875mB及原生坯料数量以本条更正为准。

按用户“铁制品可触发”的范围，给现有 iron_materials 标签补入正式配方已有引用的 forge:plates/iron、rods/iron、dusts/iron，不构建配方递归推断。

## Validation

读取原始 JSON/JAR 与脚本图案逐格计数、核对材料标签；修改后的触发标签 JSON 解析通过。没有运行游戏；材料链中的注液交互和世界获取仍需实玩。

## Remaining

完成 M4 机制及正式包验证，再更新路线文档可靠性状态。
