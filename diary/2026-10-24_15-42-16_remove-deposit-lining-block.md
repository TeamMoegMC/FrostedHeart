# 删除矿床专用围岩方块

- Time: `2026-10-24 15:42:16 +08:00`
- Author: `OpenAI Codex / GPT-6，代码与数据维护`
- Status: `partial`（源码与数据已修改；用户负责测试，未重新构建 JAR）
- Scope: FH 方块注册、资源、GameTest 引用；TheWinterRescue 热液 Profile 和导出的 configured feature

## Completed

- 按用户明确要求删除 `frostedheart:deposit_lining`：移除 `FHBlocks.DEPOSIT_LINING` 注册，以及物品、模型、blockstate、翻译、掉落和采掘标签条目。
- 删除仅为该方块添加的 `features_cannot_replace`、`lava_pool_stone_cannot_replace` 标签资源。
- 热液 `materials.lining` 改用已有 `minecraft:tuff`；通过导出器写入生产 configured feature，四份 GameTest configured-feature 夹具同步。
- 清理已有 GameTest 对已删除方块的导入、断言、保护用例和报告字段。
- Documentation impact: 更新 `docs/world-generation.md` 与整合包 `Scripts/worldgen/deposits/README.md`，说明材料、普通替换规则及历史验收的适用范围。

## Decisions

- 保留现有材料配置和几何封闭层，使用原版凝灰岩承载；不新增替代方块或物品。
- 不把凝灰岩加入全局保护标签。它仍遵循正常的标签与替换规则。
- 历史计划、开发日记及性能/流体原始记录保留原貌；其旧材料结果不作为当前版本通过验收的证据。

## Validation

- 执行 `python -X utf8 Scripts/worldgen/deposits/export_deposits.py --write`，仅更新生产 `worldgen/configured_feature/deposits/main.json`。
- 静态检索 FH `src` 与整合包当前配置/工具来源，已无被删除方块的注册 ID、字段或资源路径引用；人工核对四份 GameTest 夹具使用 `minecraft:tuff`。
- 遵循用户“测试先交给我”的要求，没有启动构建、GameTest 或游戏测试。

## Remaining

- 当前构建产物和游戏目录中的旧 JAR 尚未重新打包替换；源码移除需要重新构建 FH、同步整合包数据后生效。
- 普通凝灰岩围岩的运行效果由用户验收，历史专用围岩的流体结果不能直接继承。
- 先前查明的热液主体覆盖率和旧装饰削减问题不属于本次方块删除，仍待后续修正。
