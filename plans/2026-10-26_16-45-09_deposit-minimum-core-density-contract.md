# 矿床最小核心、完整归属与密度接线的实际原型

- Time: `2026-10-26 16:45:09 +08:00`
- Authors: `OpenAI Codex / GPT-6，设计边界与原生代码验证`
- Status: `draft`（已完成固定锚点密度原型，严格完整系统尚未完成）
- Scope: 最小尺寸约束、所有特殊群系的主体覆盖、DensityFunction注册/接线、真实含水层
- Related: [区域方案](2026-10-24_21-22-46_hydrothermal-shared-region-density.md)、[原型报告](../../TheWinterRescue/Scripts/worldgen/deposits/terrain_proof/results/2026-10-26-final/REPORT.md)

## Verified current state

气候分区改善不足以保证每片热液群系都能容纳大主体。新独立源码 DepositCoreDensity 实现有余量的连通核心，二维有界扰动只向外扩大。实际经原生NoiseChunk插值、fillFromNoise和setBlockState生成。

27组解析尺寸、10,044项检查，10个受控原生生成场景通过尺寸和空气连通检查；2个实际包NoiseRouter/含水层场景中央空气高度仅11/30格，未达到40格目标。所有场景固定锚点、固定plains来源；没有验证热液归属或动态覆盖，不是生产集成。

## Required contract

出现频率、单体几何尺寸、实际群系归属、最终可用空间是四个合同，必须分别验证。对每个成功布局保留尺寸下限，与“原气候独立产生的每片群系都拥有主体”不同。若保留无尺寸约束的原热液碎片，就不能满足后者。

严格方案需使候选接受/拒绝同步作用于特殊群系与地形：布局先给出核心与包络，两入口读取同一结果；群系表不得继续独立产生相同特殊群系的小碎片。普通来源仍可委托现有multi_noise；区域来源包装或配套气候编码仍是两种接线方式。前者已有无新增Mixin种子API验证，后者维护更多编码规则；两者的完整系统均未验收。

## Remaining implementation

1. 决定并审核严格归属接线，而不是以气候覆盖率改善代替。
2. 将固定锚点原型改为有界、缓存、可复算的候选布局；所有矿床读取各自最小尺寸配置，真正不同形态另用简单计算函数。
3. 使用独立基础地形与世界高度约束做接受检查，避免递归getBiome/getBaseHeight。
4. 解决区域含水层路由与中央空气空间，允许自然岸边，不新增虚空热量、补水或人工恒干路线。
5. 正式Forge类型注册、Codec分派、缓存适配、真实群系归属与完整生成阶段验收。
6. 在同等地貌/装饰/资源条件下对照FULL区块性能；当前受控原型不证明最佳性能。

## Documentation impact

原型与结果位于伴随包Scripts/worldgen/deposits/terrain_proof，生产source/data/JAR/save均未改。living docs继续描述当前feature；不将原型登记为已实现系统。
