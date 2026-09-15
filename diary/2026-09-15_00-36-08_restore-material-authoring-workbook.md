# 恢复误删的材料相变人工编辑源表

- Time: `2026-09-15 00:36:08 +08:00`
- Author: `Codex; OpenAI GPT-6; primary engineering agent`
- Status: `partial`
- Scope: `state_transition.xlsx人工编辑入口`

## Completed

- 用户指出原表用于人工维护并配合JSON生成器，不能视为旧运行逻辑删除。已从Git恢复原始`src/datagen/resources/data/frostedheart/data/state_transition.xlsx`，保留全部原始内容。
- Documentation impact: 数据生命周期文档注明源表恢复与生成器尚未接回的实际状态。原删除记录作为历史保留。

## Decisions

- 人工编辑工作流应保留。运行时数据格式改变不等于废弃源表，后续应适配表列和生成器。

## Validation

- 确认原表已恢复，大小12105 bytes。生产代码和生成配方未变，未重新运行游戏测试。

## Remaining

- 当前FHRecipeProvider仍读取material_transitions.json；原表尚未适配新模型并接回生成流程。不能将文件恢复宣称为编辑工作流已恢复。
