# 大小冰晶方块与冰色晶簇资源

- Time: `2026-10-22 19:35:38 +08:00`
- Author: `OpenAI Codex；冰晶方块与资源子代理`
- Status: `partial`（实现与静态资源核对完成；编译、GameTest 和客户端验收由主代理统一执行）
- Scope: `FHBlocks`、大小冰晶资源、`IceCaveContentGameTests`；不包含世界生成

## Completed

- 保留 `frostedheart:ice_crystal`、亮度 `1` 与 `AmethystClusterBlock(7, 3)`；新增 `frostedheart:large_ice_crystal`、亮度 `3` 与 `AmethystClusterBlock(14, 1)`。同原版紫水晶簇支持六向附着、水浸与失去支撑消失，不添加方块实体或 tick。
- 以原生 JSON 晶柱和 45 度斜切尖顶表现三柱小簇、五柱大簇，最高分别 `7/16` 与 `14/16`，旋转后的几何全部位于单格内。复用已有 `ice_1`、`packed_ice_1` 的冰白蓝色材质，未新增或修改 PNG。
- 补齐新方块的手写六向 blockstate、block/item model、英文/倒置英文/中文与精准采集限定掉落表，以及 Registrate 的资源存在约定、lang、loot 和大小两者的镐采掘标签。
- 扩充内容 GameTest：从真实注册 BlockItem 右键六面放置，验证方向、注册光照值、水浸状态、支撑要求及失去支撑后的移除；通过加载掉落表验证普通工具无掉落、精准采集保留对应尺寸。未修改世界生成测试。
- 文档影响：主代理统一更新 [当前冰洞实现](../docs/worldgen/ice-caves.md)，本子任务不并行编辑该文档。

## Decisions

- 参考紫水晶簇的大小分组与附着玩法；渲染采用静态原生几何和已有冰色纹理。无运行期染色、材质转换、动态模型或独立纹理维护。
- 模型使用 `minecraft:solid`，晶面为不透明冰白蓝；避免新增透明晶面与邻近覆冰的排序交错。该材质取舍需要实际画面验收。
- 两种冰晶均需精准采集，保持既有小冰晶采集规则；未把晶簇加入 `minecraft:ice`，避免被当作完整冰体。

## Validation

- Python 仅解析 JSON 与计算几何：大小模型分别 6/10 个 element，旋转后最高 `7/16` 与 `14/16`，全轴边界均在 `0..16`；六向模型引用、item parent、loot 身份、四处语言键和两张已有纹理存在检查通过。无位图生成/编辑。
- 本子任务按分工未运行 Gradle、打包或客户端；GameTest 已编写，尚未将其记为通过。

## Remaining

- 主代理统一编译并运行内容测试命名空间 `frostedheart_ice_caves`（复数），与世界生成的 `frostedheart_ice_cave`（单数）不同。
- 客户端检查斜切尖顶、大小辨识、六向朝向、冰色晶面及物品显示；静态几何核对不替代视觉验收。
