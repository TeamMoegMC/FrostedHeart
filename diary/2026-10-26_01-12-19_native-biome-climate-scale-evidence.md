# 原版群系碎片与热液参数尺度实测

- Time: `2026-10-26 01:12:19 +08:00`
- Author: `OpenAI Codex / GPT-6，独立实验与解释`
- Status: `completed`（本轮测试与结论；生产地形未改）
- Scope: 伴随包 Scripts/worldgen/biome_scale；FH 仅追加调查计划与本记录，并给旧草案增加状态说明

## Completed

- 建立可复跑的 Java 原生 climate/nearest-neighbour 实验及 Python 连通性分析、原生 quart 复查和图表；生产 Java/数据包/用户存档未改。
- 8 种子、完整实际参数表，276,824,064 次主/补充点选择；另查 11 处小分量/投影、12 处主体箱体候选。
- 原版默认和大型群系都有真实小群系反例；经 ProtoChunk 原生 palette + BiomeManager 验证，61/75/82 个方块位置的三个封闭区域分别约 6×5×5、6×5×6、6×6×6。
- 记录 cached sampler 气候一致检查；原生 R-tree lastResult 对同 fitness 的查询顺序差异不能误报为缓存故障。
- 仅扩大两条热液温度范围的局部效果最明确：覆盖率 1.315%→2.185%，较多主体气候空间；全局频率变化会移动大量其他群系。微基准未证明稳定速度优势。

## Decisions

- 修正先前过早推荐独立群系来源：优先现有气候/密度路径，不把原版小碎片存在当成必须新增路径的理由。
- 不把粗分辨率的小分量计数当成真实碎片数；实际复查有小分量重新连接到大区。
- 不将含 160×64×40 气候箱体等同于真实洞腔、主体数量或可通行空间。最后候选整个在 Y>=-56；允许触底的较早记录只作诊断。

## Validation

- `run_probe.py` 主实验 64 体积、补充 24 体积、原生 quart 11+12 处、原版 block-biomes 三例全部成功；Java 17，无 Mixin bootstrap。
- 主/补充 342,144 次原生采样一致检查，实际 palette 路径 139,776 次 cached climate 一致检查。
- Python py_compile、相关 diff --check 与新增源码尾随空格检查通过。原生 API 的初次 private 类型引用和对同 fitness 的错误 palette 比较在调查中修正；最终结果使用成功重跑输出。
- 已实际打开检查三份输出 PNG；图中明确区分群系采样和地形方块。
- 证据入口：[报告](../../TheWinterRescue/Scripts/worldgen/biome_scale/results/2026-10-26-REPORT.md)。

## Remaining

- 用户尚未批准正式改生产规则。候选参数盒与其他群系的重叠、作者工具和完整表统一、主体布点、真实整包地形和 FULL 区块性能仍需下一轮。
- Documentation impact: living docs 无运行变化；更新旧 draft 的推荐状态，新增已完成调查计划，报告保存准确的范围和限制。
