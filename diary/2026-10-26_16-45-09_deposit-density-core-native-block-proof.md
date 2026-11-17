# 有尺寸下限的矿床核心密度与原生方块验证

- Time: `2026-10-26 16:45:09 +08:00`
- Author: `OpenAI Codex / GPT-6，独立原型与限制核对`
- Status: `partial`（几何与接线完成，真实含水层和完整系统未验收）
- Scope: 伴随包Scripts/worldgen/deposits/terrain_proof；FH仅计划/日记，生产不变

## Completed

- 新增完整可编译DepositCoreDensity：Codec、mapAll、fillArray、上下界，有尺寸余量的胶囊核心，二维缓存噪声只向外扰动。
- 通过原生min + interpolated接入actual NoiseRouter，实际调用NoiseChunk/Aquifer/NoiseBasedChunkGenerator写方块；独立工具没有Mixin bootstrap。
- 27组尺寸解析检查10,044项；3种子×3方向及另一尺寸共10个受控场景，空气全部与中心连通，160/64/40目标实测177~179/73~75.21/49，224/96/56实测244/108.26/67。
- 另外2个真实包路由+含水层场景揭示中央空气高度11/30，不能宣称达到40。保留全部失败条件数据与实际块剖面，不以连入其他洞穴的大包围盒当成本矿床尺寸。
- 最终12场景共1,884个NOISE区块，固定锚点、plains来源，不冒充热液归属与所有布局覆盖。

## Decisions

- 更正此前统计方案的能力边界：调气候改善不能保证每个原热液碎片都有大主体；严格方案需特殊群系和地形共同遵循已接受布局。
- 受控干燥背景仅隔离几何；真实含水层必须另行解决，负密度不等于空气。
- 固定锚点只是清楚展示接线，不是自然热液造型或最终位置算法；普通注册API已编译，自定义Codec直接往返通过，但未运行Forge注册分派。

## Validation

- `run_terrain_proof.py --output .../results/2026-10-26-final` 成功，Java17；首次结果2026-10-26使用不同roughness输入，最终数据另存不混用。
- 实际渲染并打开检查actual-noise-stage-sections.png。
- [最终报告](../../TheWinterRescue/Scripts/worldgen/deposits/terrain_proof/results/2026-10-26-final/REPORT.md)。

## Remaining

- 正式选址、每处特殊群系归属、含水层、完整生成、材质/资源与性能验收未完成，不能保证所有矿床都正常。
- Documentation impact: 新建draft合同计划与独立报告/README；living docs无生产行为变化。
