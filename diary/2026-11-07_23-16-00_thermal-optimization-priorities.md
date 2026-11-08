# 依据现有录制重排热模拟优化优先级

- Time: `2026-11-07 23:16:00 +08:00`
- Author: `OpenAI Codex / GPT-6; primary investigation agent`
- Status: `completed`（调查；候选未实施）
- Scope: 既有四轮性能JFR、传热图/共享网格构建与化简优先级；生产代码无变化

## Completed

- 直接重用上一批四个原始录制，不运行新GameTest。排除启动和关闭，分析原2400tick测量窗内Java执行样本及分配weight。
- 四轮`PairwiseGraphPreparation`与`LongRadixSortCursor`分配采样权重约0.277–0.308GB，共享网格编译约0.154–0.206GB；当前新版两轮RegionBuild占热线程Java执行样本37.4%/40.8%。这些是热点定位证据，不是可全部节省的精确成本。
- 确认普通网格/传热图和部分工作空间已有复用；优先候选是减少重复边的排序与临时坐标复制，避免泛泛新增缓存。
- 将逐项原型、边顺序/贡献映射约束、重复度成本和撤回条件写入[候选计划](../plans/2026-11-07_23-16-00_thermal-optimization-hotspots.md)。

## Decisions

- 首选在已有传热图准备内部验证“先合并端点键，再排序唯一边”，保持最终边序和贡献累加顺序；低重复度时哈希/映射可能更贵，原型先测再决定保留。
- 保存包装下的大额分配包含原版区块序列化，未将其误报为热系统可全部删除的分配。
- 按需摘要尚未证明整体收益且增加控制边界，列为建议撤回项，本调查未实际撤回。方块中心和四点接触合并的数值模型变化继续留待独立原型。
- Documentation impact: 行为、模型、配置、持久化合同未改变，living docs不需要更新；新增工作计划和本日记。

## Validation

- 四个原始JFR经JDK RecordingFile流式读取，汇总保留于ignored `build/thermal-optimization-inspection/`；与原性能报告的测量tick窗口一致。源码核对现有排序、去重、复用及临时数组。
- 初次完整JFR JSON导出递归展开元数据形成过大临时文件，已停止并删除这些临时输出；改为256MiB堆的流式提取。原始录制和生产代码未改。

## Remaining

- 候选尚未实施，没有新提速百分比、净减行数或温差资格。后续按计划做一个有限原型，收益和复杂度不达标即撤回。
