# 热液图内部灰区的大陆性与 depth 原因

- Time: `2026-10-26 01:20:16 +08:00`
- Author: `OpenAI Codex / GPT-6，查询定位`
- Status: `completed`
- Scope: 上轮群系地图的内部非热液区；生产地形/数据没有变化

## Completed

- 在独立 BiomeScaleEvidence 增加坐标查询模式，输出六轴实际气候、选中群系和候选 fitness。
- 分类用户种子 Y=-24 粗图内部灰区：主要为 darkfang_caves，另有 deep_frozen_ocean；不是图上的空气判定。
- (-1216,-24,-768) 温度 0.7031、depth 0.8325 满足热液层，但大陆性 0.027 落入 darkfang 而超出热液上界 -0.19；darkfang fitness=0，热液=4,708,900。
- (-1840,-24,-1136) 实际 depth=0.3049，距离对应热液最低 depth=0.5 较远，deep_frozen_ocean 更近。
- 扩大温度前后两种原因都不变；最大 darkfang 内区的粗采样点数同为 2,372。

## Decisions

- 上轮温度扩大仅证明覆盖改善，不能解释为自动消除内区。需要统一大陆性/depth 与相邻群系分区，而不是凭温度单轴或增加 BiomeSource 解决。
- 群系图灰区与真实地形空腔分开解释，避免以错误层次回答用户。

## Validation

- Java 17 原生查询 2 方案×3 坐标成功，无 Mixin；结果距离不是平局。
- 记录在 [后续定位](../../TheWinterRescue/Scripts/worldgen/biome_scale/results/2026-10-26-holes/README.md)。
- Documentation impact: 总报告追加定位与边界；living docs 无运行变化，不改。

## Remaining

- 相邻大陆性/深度分区的具体新设计尚未实施；实际地形空气若为用户所指，不能使用本次群系图证据替代其生成来源定位。
