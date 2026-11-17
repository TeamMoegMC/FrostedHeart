# 撤销热液矿床的 Mixin 与群系来源接入

- Time: `2026-10-24 02:45:22 +08:00`
- Author: `OpenAI Codex / GPT-6，实施与核对`
- Status: `partial`
- Scope: `content/world/hydrothermal` 草稿和此前增加的世界生成接线

## Completed

- 根据用户明确要求，删除本轮新增的 HydrothermalSamplerMixin、HydrothermalGeneratorMixin、HydrothermalBiomeSource 及入口注册。
- 撤销 NoiseBasedChunkGeneratorMixin 中本轮增加的群系包装和雕刻结束后写入，恢复原有列缓存处理。
- 移除 HydrothermalRegions 的 sampler 注入访问接口；其余未接入的地质体草稿留待后续选择。
- 将原计划标为 superseded，追加新的边界：无新增 Mixin、实际群系继续采用现有气候表、无新增供热。

## Decisions

- 原有 configured_feature 已调用 Minecraft 的 Java Feature 算法。新增 Java feature 是扩展一种算法，不是新的生成阶段或独立生成体系。
- 本次不继续引入共享生成架构，先解释原有 JSON 能力与实际缺口。

## Validation

- 源码引用和 Mixin 配置检查通过：没有新增热液接线或入口注册，原生成 Mixin 的逐行内容与原版工作基线一致。
- 确认运行 normal.json 仍为 minecraft:multi_noise，三条原热液记录保留；相关文件 git diff --check 通过。
- `gradlew.bat compileJava --offline --console=plain` 通过，41 秒；现有 Mixin/JEI 警告仍在。此结果只验证撤销后的编译，不是新版矿床验收。

## Remaining

- 地质体算法仍为未注册草稿；未交付新的矿床生成行为。
- 按修订后的范围确定现有 feature 的组合与必要新增能力，并完成统一验证。
- Living docs 不更新为新矿床已实现，因为没有启用新行为；原计划已记录撤销结果。
