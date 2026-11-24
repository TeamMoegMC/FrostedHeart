# 公开探矿工具配方

- Time: `2026-11-24 16:36:22 +08:00`
- Author: Codex（OpenAI，实现与验证）
- Status: completed
- Scope: TWR 探矿工具配方权限及 FH 流程文档

## Completed

- 移除八项进阶工具的新成果配方权限和旧研究配方效果。九把探矿工具备齐材料即可合成。
- 更新中英文任务提示、[探矿流程](../docs/knowledge/prospecting.md)和[旧研究处置表](../docs/research/legacy-content-disposition.md)。

## Decisions

- 保留配方 ID、材料和排列。退役的两项旧研究定义保留 ID 和旧档记录。
- 「探矿镐的使用方法」保留探测权限成果与「岩石的性质」旧档映射。

## Validation

- 执行合成配方脚本，核对九把工具的配方与修改前一致，检查新旧研究定义已无对应配方权限。
- `validateResearchCatalog` 校验 80 份旧研究定义通过。

## Remaining

- 本轮验证范围为源码数据与配方脚本。客户端实机验证待后续开展。
