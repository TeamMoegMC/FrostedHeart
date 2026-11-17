# 修正矿床与冻结 Feature 的排序循环

- Time: `2026-10-24 10:55:47 +08:00`
- Author: `OpenAI Codex / GPT-6，故障定位与数据修正`
- Status: `partial`（修改完成，按用户要求由用户测试）
- Scope: TheWinterRescue 矿床 biome modifier/导出器；FH 的说明与已有测试阶段断言

## Completed

- 用户日志指出 FeatureSorter 在 snowy_shrubland / frozen_peaks 间出现排序循环。
- 对照实际群系 JSON、KubeJS 标签、FHBiomeModifiers.Instance 和 Minecraft FeatureSorter 源码：snowy_shrubland 已在主表声明 freeze，且在 not_improved_winter 中，ADD 时追加 deposit；frozen_peaks 则在 ADD 后的 MODIFY 中追加 freeze。因此两者同处 step 10 时形成 freeze → deposit → freeze。
- 将共享 deposit biome modifier 从 top_layer_modification 改为 vegetal_decoration（step 9），使矿床在各群系中都先于 step 10 冻结。Python 导出来源同步，避免下次生成覆盖修复。
- 现有夹具生成位置、阶段断言和维护说明同步到 step 9。矿床几何、频率、材料和缓存算法未在本次调整。

## Decisions

- 修正数据阶段关系，不增加 Mixin 或新排序管理器。
- 保留既有冻结规则和共享矿床入口覆盖。

## Validation

- 依据日志与源码完成定位和编辑。
- 用户明确指示“你就改就行了，测试先交给我”；本次没有启动构建、GameTest 或运行测试。
- 之前保存的性能和 600 秒流体记录针对调整前的阶段，不能作为本次修正的运行验收。

## Remaining

- 用户重启游戏后测试实际多群系创建世界。
- Documentation impact: 更新 docs/world-generation.md 和整合包 deposits README，写明阶段与验证范围。
