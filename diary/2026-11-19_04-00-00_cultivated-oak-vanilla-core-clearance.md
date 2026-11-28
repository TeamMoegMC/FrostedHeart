# 栽培橡树改用原版核心空间判定

- Time: `2026-11-19 04:00:00 UTC`
- Author: `OpenAI Codex`
- Status: 实现完成，专用 Forge GameTest 5/5 通过。
- Scope: 独立栽培橡树的生成空间检查；未修改原版树苗或数据包参数。

## 改动

`CultivatedOakBlockEntity.tryGenerate` 移除整块树冠包围盒中的障碍、流体和额外高度检查，只保留 `hasChunksAt` 区块加载条件。核心空间、世界高度与可替换性直接由 `Feature.TREE.place` 的原版逻辑和配置 `minimum_size` 判定。默认橡树底部是中心柱，其上为 3×3 核心；外围树冠遇到障碍不再禁止生长。生成失败仍恢复成熟树苗与 NBT，保留目标高度，不重复扣肥。

## 验证

`bash FrostedHeart/gradlew -p FrostedHeart --offline runGameTestServer -PgameTestNamespaces=frostedheart_cultivated_oak -PgameTestDirectory=build/cultivated-oak-gametest`：通过，4m11s，`All 5 required tests passed`。日志：`build/cultivated-oak-core-gametest.log`。

新增 `vanillaCoreClearance`：同时放置核心 x=1 和树冠外围 x=2 的石块，核心受阻时保留树苗及进度/目标；只移除核心石块后生成六格橡木树干，外围石块原样保留。既有四项生长、肥料、存档和重试测试一并通过。`git diff --check` 通过。本轮未重新运行渲染客户端或打包，不将上次客户端验收视作本次变更的客户端实测。

## 文档影响

更新 `docs/climate/cultivated-oak.md` 的空间规则、核对日期与验证状态；保留已有历史日记不变。
