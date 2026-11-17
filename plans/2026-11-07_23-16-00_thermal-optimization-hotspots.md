# 热模拟有效优化候选：优先简化构建

- Time: `2026-11-07 23:16:00 +08:00`
- Authors: `OpenAI Codex / GPT-6; primary investigation agent`
- Status: `completed`（按需摘要已撤回；第一候选实测不合格，原型已移除）
- Scope: 撤回按需摘要；重复边构建原型的固定输入比较与淘汰；物理模型、默认配置及其余候选未改变
- Related: [性能补测](../docs/climate/thermal-validation-baseline.md)、[复杂度调查](2026-11-06_17-33-39_thermal-complexity-and-equivalent-simplification.md)

## Evidence

复用性能补测的四个独立进程录制，只分析原2400tick测量窗，不运行新测试。读取`jdk.ExecutionSample`和`jdk.ObjectAllocationSample`，直接流式读取类名/方法名/weight，避免完整JSON的递归元数据展开。分配weight是采样估计，不是精确字节账本或可全部删除的额度；CPU栈采样也不是精确CPU秒。

| 运行顺序 | 热worker Java执行样本 | RegionBuild路径样本 | 边整理/排序分配采样权重MB | 共享网格编译分配采样权重MB |
|---|---:|---:|---:|---:|
| 旧1 | 1026 | 391 | 284.80 | 154.33 |
| 新1 | 962 | 360 | 293.60 | 201.64 |
| 新2 | 490 | 200 | 276.83 | 161.03 |
| 旧2 | 417 | 160 | 307.66 | 206.02 |

当前新实现两轮`RegionBuild.advance`分别占37.42%/40.82%的热worker Java执行样本；`PairwiseCursor.run`路径分别38/962、21/490，约4%。两个路径占比不能代表全部构建/全部物理推进，也不能当作可实现提速比例。记录尚包含额外拓扑准备、维护、发布和协调。

保存包装`MinecraftThermalInput.runMaintenanceSave`下有约1.36–1.56GB分配采样权重；栈可见原版`PalettedContainer.pack`/`ChunkSerializer.write`，不能把整个保存包装的分配算作热独占热点。单Chunk源捕获局部化仍可单独调查，不据包装函数名承诺节省这部分全部分配。

本地精简汇总在ignored `build/thermal-optimization-inspection/*.hotspots.json`；原JFR仍归属`build/thermal-baselines/minimum-perf-normal-{before,after}-{1,2}/normal-load.jfr`。

## First Candidate: Graph Construction

代码：[CoupledThermalOperator.PairwiseGraphPreparation](../src/main/java/com/teammoeg/frostedheart/content/climate/thermal/solver/CoupledThermalOperator.java)、[LongRadixSortCursor](../src/main/java/com/teammoeg/frostedheart/content/climate/thermal/runtime/LongRadixSortCursor.java)。

当前为每个空气单元枚举最多12次轴向边贡献，先计数再填完整出现表，排序全部出现记录后去重，并建立原贡献地址到唯一边的映射。相邻空气单元会重复贡献同一物理边；现有排序已缩减有效bit轮数，不能再次声称刚发现了完整64bit排序浪费。

候选是在已有`PairwiseGraphPreparation`中合并重复端点键，只给唯一边排序，保留原地址映射；争取删去完整重复键表、部分排序缓冲及packed/unpacked地址编码分支。它是待验证算法，不承诺固定收益或净减行数。

实现前先取得同一真实布局的出现次数E、唯一边数U与所需数组大小。哈希表、重映射和唯一键排序也有成本；重复度低时可能更贵，不能只拿减少排序项数当作收益。原型不得扩展为新缓存管理器或新后端。

保留条件：最终边端点顺序、每个原贡献地址的映射、导热贡献累加顺序与旧结果一致，保持原传热公式/源输入/时间步；固定输入测出超过重复波动的构建收益或明确分配下降，并确认真实双基地整体代价和服务未退步。原型不合格则撤回，不长期并存两种算法。

## Second Candidate: Unit-grid Temporary Data

代码：[SharedAirGridCompiler.Preparation](../src/main/java/com/teammoeg/frostedheart/content/climate/thermal/topology/SharedAirGridCompiler.java)。

当前按每个空气方块分配`xs/ys/zs`三份坐标数组、并查集/身份数组和坐标索引，构建局部角点数组后再产生全局布局。先评估从已有captured组件/airBlocks遍历坐标，移除三份临时坐标缓存或多余复制；不改变顶点合并和编号次序。去掉三份int坐标数组的载荷是12B/空气方块/次构建，不等于能省掉整条路径的采样分配。

已有`withRetainedUnitGrid`、旧传热图复用和工作数组复用继续核对；不能把“增加缓存”重新包装为首次复用。重复解码坐标可能增加运算，必须和少写/少分配的实际收益一起测。

## Subsequent Structural Simplification

旧温场兼容表示可在解码边界统一，减少`AirFieldCheckpoint`与`DormantChunkThermalState`采样、合并、退休、编码中的兼容分支；它主要服务概念/代码收缩，没有当前常态CPU收益证明。

四点面接触合并及方块中心主模型属于数值模型变化。保留为后续单独原型，沿已有0.1°C正常误差目标和近火/门洞/隔墙/相变效应检查；通过后替换旧路径，不长期增加第三套后端。

## Scope And Decision Rules

- 下一次只实施第一候选的有限原型；先固定真实几何测热点，再跑既有代表性负载和必要的数值/生命周期检查，不重跑全矩阵来掩盖热点收益不明。
- 性能和复杂度分别记录：实际去掉的遍历、数组、状态、分支，以及整体任务耗时、分配、服务延迟和温差。不以源码少几行替代性能证据，也不以缓存层变多替代系统化简。
- 上一轮按需摘要已按本轮授权撤回，生产8文件净减33行；保留7个无关hunk，包括材料自然背景修正及TRACE调整，原数值/性能记录保留。
- 没有收益且未实质减少状态/流程的候选应撤回。测量有噪声时报告不确定性，不挑有利轮次。

## Outcome

第一候选在现有两个生产文件中实现过净减16行的原型；最终端点、排序及贡献映射通过独立参考，固定生产网格夹具的候选/旧编译组件各3项通过。密集8192单元的98304次贡献对应27200条唯一边：候选每图分配2.185→1.834MB，但中位构建耗时2.202→2.633ms；256个不连通单元分配87.4→136.7kB、耗时0.0483→0.7219ms。每版仅一次进程，耗时有波动；没有整体收益依据，低重复度也明显恶化，故停止该方向并移除全部原型及专用基准源码，保留本地产物和补丁供复核。

最终生产变化仅为摘要回退，净减33行、新增类型0；当前源码编译通过。真实粗细转换1项通过、双基地1项通过，热错误0/root0；后者12000查询服务违规0、无降粗。双基地使用保留编译组件，thermal类与最新R4编译相同；同期10个玩家体温类不同，不据此验证该玩家改动。

完整证据见[撤回与原型否决记录](../docs/climate/validation/thermal-graph-prototype-rejection-2026-11-08.json)和[新日记](../diary/2026-11-08_03-10-00_thermal-rollback-and-rejected-graph-prototype.md)。更新当前帧准备合同；第二候选及模型替换尚未实施。没有新增整体提速或温度曲线资格。
