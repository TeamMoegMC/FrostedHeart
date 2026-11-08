# 恢复热液群系特征并修正主体选址

- Time: `2026-10-24 16:07:14 +08:00`
- Author: `OpenAI Codex / GPT-6，生成回归修正`
- Status: `partial`（源码与数据已修改；按用户要求由用户运行测试，未构建新 JAR）
- Scope: TheWinterRescue 热液 biome/placed features/生成工具；FH `DepositFeature` 和已有 GameTest 夹具

## Completed

- 恢复 `hydrothermal_deposits.json` 原有 feature ID 列表和各自阶段，包含闪长岩、热液湖、三角洲、原有岩浆/泉水、矿石、方解石及藤蔓。静态 JSON 对照与 `fixtures/legacy.json` 的 feature 数组一致。
- 方解石恢复 30 次、藤蔓恢复 75 次、晶洞恢复原 1.4 次期望。删除先前降为 3 次的 `deposits/background_calcite` 包装及其测试夹具。
- 修正 `hydrothermal_delta` 的放置流水线：40 次固定候选 → 区块内 X/Z → 起始高度 → 向下最多 32 格寻找坚实地面 → 实际落点群系过滤。删除先全高度找层、随后又随机重置 Y 的旧组合。
- 热液湖、三角洲、岩浆、方解石、藤蔓的空气条件同时接受 `minecraft:air` 和 `minecraft:cave_air`。
- 主体的全局单元概率改为 1.0。`DepositFeature.plan` 保留原候选优先，并最多尝试允许中心范围的四角；同一单元仍最多一个主体，保留坐标种子、按区块写入与配置缓存。粗筛范围覆盖所有备选中心，避免外围区块漏写。
- 同一单元的候选共用参考 Y、Profile 概率抽样和几何种子；成功即停止，全部拒绝结果也缓存。空 Profile 配置直接返回。
- 导出生产配置并刷新既有 GameTest 资源。候选/缓存测试单独使用 0.75 概率保留正反样本；FULL 生成仍使用当前生产配置。普通群系夹具保留生产 Profile，走真正的不匹配查询路径。
- 气候证据工具同步备选顺序，新增 `biome_probes`，历史结果文件未改写。

## Decisions

- 群系的背景细节独立于大型主体；不能再次因主体未命中而把整片热液群系留成稀少装饰的普通洞穴。
- 单元间包络与现有缓存结构沿用。冷路径最多 5 次原生群系查询、25 次基础高度探针；不遍历群系连通分量、不加载远处区块、不增加逐方块气候查询。
- 不新增方块、物品、Mixin、BiomeSource、ChunkGenerator 或供热机制。恢复的岩浆/岩浆块条目来自原始群系内容。
- 恢复装饰会恢复相应生成成本。先前削减装饰版本的速度与矿量结果不代表本次改动，不能继续据此宣称相同视觉内容下提升 13%。

## Validation

- 阅读本机映射后的 Minecraft 1.20.1 `CountOnEveryLayerPlacement`、`DeltaFeature`、`LakeFeature`、`BiomeFilter`、`VegetationPatchFeature` 源码；通过 `javap` 核对 `EnvironmentScanPlacement` 的起点、搜索和终点条件。
- 运行导出器 `--write` 和 `prepare_acceptance.py`，后者写出 20 个历史依赖和 36 个当前依赖。
- 静态核对 JSON、原 feature 数组、来源与导出参数、各实际 placed feature 与对应夹具；核对 ordinary 夹具保留真实 Profile。
- 相关已跟踪路径 `git diff --check` 无错误；源码和生产配置未引用已删除的专用围岩或降频方解石入口。
- 没有启动构建、GameTest、气候探针、性能测量或游戏测试；用户负责测试。

## Remaining

- 用户重新构建 FH、同步整合包数据，在新生成区域验收；当前游戏 JAR 与已保存区块不会因源码修改而更新。
- 五个候选点只能改善主体覆盖率，不能保证每片三维热液群系都有大型主体。实际外观、矿量、流体及耗时尚待验证。
- 使用普通凝灰岩围岩时，后续生成仍按正常规则替换它；旧区块与新选址区域之间可能有接缝。本次没有修改用户存档或自动回填。
- Documentation impact: 更新 `docs/world-generation.md` 和整合包 deposits README，记录恢复内容、成本上限、验证范围及已生成区块的限制。
