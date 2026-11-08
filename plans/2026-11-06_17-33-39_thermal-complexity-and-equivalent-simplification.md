# 热模拟复杂度调查与近似等效简化候选

- Time: `2026-11-06 17:33:39 +08:00`
- Authors: `OpenAI Codex / GPT-6; primary investigation agent`
- Status: `completed`（本次源码调查完成；下列实现候选尚未实施）
- Scope: 当前工作区 `src/main/java/com/teammoeg/frostedheart/content/climate/thermal/` 的代码量、实际计算链路、历史表示残留、近似等效简化机会
- Related: [当前运行合同](../docs/climate/thermal-runtime-control.md)、[余温生命周期](../docs/climate/thermal-residual-lifecycle.md)、[后续工作入口](2026-10-29_23-38-00_thermal-next-steps.md)、[历史清理记录](../diary/2026-10-29_23-38-00_thermal-development-history.md)

## 结论与证据范围

有简化空间，但目前的大头已经不是多套旧运行时原封不动共存。旧 FUNCTIONS/scalar 运行入口、PCG 和 two-level 求解路径已被删除；现有大部分类型服务当前共享网格、区域任务、粗细切换或世界生命周期。

当前复杂度主要来自：多层空间表示、细档与最低模型之间的转换、可暂停的异步构建/计算、动态世界几何、输入与保存的跨时刻交接。仅继续删除无调用方法只能取得小幅收缩；想明显减少概念和实现量，应减少长期维护的表示以及表示之间的转换。

本次核对当前工作区，包括既有未提交修改；检查时 HEAD 为 `61eddf706`。使用文件统计、全仓库 Java 符号检索、关键调用链和实际算法阅读，结合最近的余温与河道修正记录。没有修改生产实现、配置或测试，没有运行新数值对照，不能据此宣称任何候选已满足 0.1°C 或其他误差界，也不提供未经实现的减行百分比或 CPU 提速比例。

## 代码量

口径为 `thermal/` 下所有生产 `.java` 文件的物理行，包含空行和注释。

| 部分 | 文件数 | 行数 | 主要职责 |
|---|---:|---:|---|
| runtime | 27 | 10,654 | Minecraft 接入、输入、调度、预算、保存协调 |
| solver | 24 | 8,158 | 区域组织、构建、细档/最低模型、换热 |
| topology | 18 | 6,823 | 世界几何捕获、连通、通路、变化安装 |
| source | 15 | 4,835 | 源身份、端口、功率窗口、待处理能量 |
| mesh | 19 | 4,095 | 空间布局、材料状态与热力学定律 |
| persistence | 8 | 2,505 | 空气/材料/未消费输入保存及冷却 |
| radiation | 4 | 2,501 | 辐射源发现、遮挡与接收者缓存 |
| query | 4 | 2,161 | 区域发布、读者、材料/空气查询 |
| profile | 7 | 1,110 | 方块签名和热参数编译 |
| field | 4 | 544 | 能量塔等玩法解析场 |
| consumer | 1 | 93 | 城镇温度投影 |
| 合计 | 131 | 43,479 | |

空行 2,985 行；文本移除注释后的非空行约 37,652 行，这不是 Java 语句数或复杂度指标。`runtime + topology + mesh` 共 21,572 行，占约 49.6%；`solver` 目录也包含区域调度，不能把其 8,158 行全部称为数值公式。

独立 GameTest 的 `thermal/` 有 93 文件、16,057 行，不计入上述生产统计。`player/unused/` 的旧粒子相关代码为 4 文件、1,303 行，处于统计范围之外；最近既有计划明确将其排除在热系统清理范围，不能把它当成本轮 4.35 万行的主要来源。

最大的五个生产文件是 `MinecraftThermalInput` 2,623 行、`RegionalAirRuntime` 2,602 行、`MinecraftPageManager` 1,733 行、`ThermalDimensionEngine` 1,477 行、`ContinuousAirSolver` 1,314 行。拆分文件本身不会减少状态和协议。

## 当前算法已经简化过什么

- [ContinuousAirSolver](../src/main/java/com/teammoeg/frostedheart/content/climate/thermal/solver/ContinuousAirSolver.java) 的 `PairwiseCursor` 使用对称的成对换热推进；20 个阶段中含复制、材料内部换热、源输入、接触更新、浮力系数和发布前状态写回，并不是 20 轮全局收敛迭代。
- [CoupledThermalOperator](../src/main/java/com/teammoeg/frostedheart/content/climate/thermal/solver/CoupledThermalOperator.java) 的 `ProjectionPreparation.run` 已直接计算 `temperature = energy / capacity`；没有可通过放宽停止误差大幅减少的 PCG 迭代。
- `ContinuousAirSolver.TRANSPORT_TOLERANCE_C` 在当前生产路径没有用于控制传热迭代，仅见声明和 GameTest 日志引用。配置 `continuousAirCorrectionToleranceC` 仍参与直接投影的结果检查，不能将其解释为正常传热的全场误差保证。
- [ThermalSolver](../src/main/java/com/teammoeg/frostedheart/content/climate/thermal/solver/ThermalSolver.java) 现在承担几何/材料边与引用管理。旧名字仍在，不代表旧标量求解器仍在执行。

## A：优先评估，目标是保持正常温度计算

### A1. 细档不必每次发布都预先生成完整粗档状态

已核实的链路：

1. [RegionWork.prepareFrame](../src/main/java/com/teammoeg/frostedheart/content/climate/thermal/solver/RegionWork.java) 每次细档结果准备完成后，无条件调用 `prepareMinimumProjection(field)`，运行投影后再 `withMinimumSummary(...)`。
2. [MinimumThermalModel.Projection](../src/main/java/com/teammoeg/frostedheart/content/climate/thermal/solver/MinimumThermalModel.java) 遍历空气节点与材料，汇总能量、热容、相变分支和材料锚点。
3. [RegionalQuerySnapshot](../src/main/java/com/teammoeg/frostedheart/content/climate/thermal/query/RegionalQuerySnapshot.java) 除细档状态外保存 `minimumSummary`、`minimumSummaryAnchor`；`withMaterialControl` 同步修正汇总状态与锚点。
4. 粗档的传热图已是按需生成：`RegionJob` 的 `MINIMUM_LAYOUT` 调用 `prepareMinimumTransport()`。因此当前是预先维护粗档状态摘要，并不是同时运行两套传热或每次都构造完整粗图。

候选：保留区域几何和自然背景，降档时从最后一份已提交细档快照生成最低状态。这样可移除正常发布路径上的反复投影及细档材料变更对粗档摘要的同步维护。

关键依赖：`componentPerturbationC`、`vertexPerturbationC`、`activeAtSlot` 当前借用 `minimumSummary.layout().background()`。必须先把背景作为独立的几何数据保留，否则简单删除 summary 会改变余温扩张和退出。还需保留低资源状态下转换所需的有界工作空间与持续服务能力。

温度预期：在相同输入与细档模型下，细档算术可以保持不变；系统级一致性仍需检查背景、降档时机、内存准入和温度年龄。没有实测转换代价前，不能宣称这是无条件收益。节省目标是摘要生成和同步职责，不是删除整个最低模型。

### A2. 旧存档表示在读入边界统一

[AirFieldCheckpoint](../src/main/java/com/teammoeg/frostedheart/content/climate/thermal/persistence/minecraft/AirFieldCheckpoint.java) 仍同时携带 `coarseC`、`unitCorners/nodalC`、旧 `LocalAirShape/modes/amplitudes`。当前 `Capture.finish()` 创建空 shapes/amplitudes，旧局部函数来自已有存档的解码和后续合并。

[DormantChunkThermalState.SectionEntry](../src/main/java/com/teammoeg/frostedheart/content/climate/thermal/persistence/minecraft/DormantChunkThermalState.java) 同时维护 `spatialAir` 和旧 `counts/residuals/blocks` 表示。分支不只在 decode，还散落于保留、采样、合并、编码和内存盘点。

候选：保留旧数据读取能力，解码时转换成统一内部温场；正常查询、合并、退休与保存只处理这一种内部表示。先完成表示边界的收敛，再判断旧形状类型及常规路径上的兼容分支能删除多少。

温度预期：当前格式可保持原值；旧格式转换要验证每个连通分支、节点/块内采样、旧量化单位和采样时刻。不能直接丢弃旧余温，也不能将当前最低模型仍使用的四格粗表示误删为历史垃圾。迁移会增加一次性解码工作，净减行数需要实际实现后计算。

### A3. 确实无仓库内调用的小入口

全仓库 `src/**/*.java` 检索只找到定义的两个包装方法：

- `DimensionInputAccumulator.emitSourceImpulse`。
- `MinecraftThermalProfiles.lavaBlockRadiationEnabled`。

这些属于可清理的小尾项。该证据是仓库内符号检索，不是对外部模组反射调用的证明。`MinecraftStateThermalTable.hasRadiationMode` 有 `BlockRadiationIndex` 正式调用，不能连带删除；`IMPULSE`、待处理 J 的零时长表示、取消归还和恢复路径也不能由这个无人调用的包装入口推导为无用。

其它低引用项包括 Forge 事件订阅、测试观测和正式诊断；没有按“引用少”判为死代码。本次没有发现足以通过删无调用类就消除上万行的证据。

## B：允许小温差时，最值得做数值对照的候选

### B1. 将普通材料表面和天空边界的四个换热点合并为面平均接触

`LocalMaterialBoundary.surfacePoints` 的细档返回 4；`ContinuousAirSolver.ContactPreparation.materialBody` 每面创建四个接触，`boundary` 对天空面片也创建四个接触。细网格的 `unitAirRow` 将这些位置绑定到相应角节点。它们影响节点间热分配和算子构造。

候选是每面使用一个平均空气温度和总导热系数，而非简单删除三个点。对同一个瞬时材料温度 `Tm`，四个等权接触的总通量满足：

```text
sum((G/4) * (Ti - Tm)) = G * (mean(Ti) - Tm)
```

其中 `G` 为整面导热系数，单位 W/K；`Ti` 是四个空气节点温度，`Tm` 是材料温度，单位 °C。这个等式只说明瞬时总通量，不能证明有限时间步的温度轨迹相同：现实现顺序更新材料、各空气节点和相变状态；平均接触还会改变面内温差的消散。

优先测墙面温度较平滑的普通房间，再测贴墙火源、薄墙两侧温差、屋顶散热和相变材料。相比全量重写网格，这个试验范围更小。不要仅将 `surfacePoints` 改成 1：现有单点地址选择并不自动成为四节点面平均。

### B2. 用每空气方块一个状态替代八角节点表示，并让粗细模型共用换热结构

[SharedAirGridCompiler](../src/main/java/com/teammoeg/frostedheart/content/climate/thermal/topology/SharedAirGridCompiler.java) 为每个空气块创建八个角身份，合并实际相邻面的共享顶点，再建立局部/全局系数索引。后续有 `AirOperatorFragment` 体积系数、`CoupledThermalOperator` 去重边、接触权重、查询插值和重建能量投影。

最低模型则由 `MinimumThermalState` 固定上限的 64 个空气节点、64 个材料池和最多 32 个显式相变体组成，并有 `MinimumThermalModel`、`MinimumRegionStep`、材料锚点和恢复过程。正常情况下只推进选定的一个模型；复杂度来自两种表示及转换。

候选目标是以方块中心的温度/能量和面传热边为主表示，保留现有成对换热规律、源 W/J、材料热容/潜热及真实连通性；查询需要的平滑在局部完成。粗档使用同类热节点/边并做有约束的聚合，避免长期维护两套数值和状态协议。

这有最大的结构收缩潜力，但属于离散模型变更。相同热容和总输入不保证相同局部温度，尤其是热源附近、窄缝、接触面和高低温分层；插值必须尊重墙体和分离的空气分支。粗档如何保持工作量上限也要单独论证，不能为了统一模型退回无界的细节点计算。

先测一个可替换的小范围原型，达到温差与复杂度目标再替换旧路径；不能把长期并存的第三套后端当作最终精简成果。此处不承诺减半或固定减行量。

## 不宜混同为等效简化的改变

- 直接让整个房间恒温，会损失近火/远火差异、垂直分层、门洞与隔墙效果。适合另一个玩法模型，不能视为当前系统的小误差清理。
- 当前最低档是过载服务模型，不能直接推广为正常模型并宣称误差很小。已有[特定版本验证](../docs/climate/thermal-regional-server-validation.md)记录降档最大跳变约 1.475°C；该数既不是当前通用误差界，也不是所有场景都能接受的证明。
- 将普通固体全部当作恒温环境，会改变升温延迟和熄火后余温；合并材料还要区分相变材料和普通显热材料。最近河道修正保持了自然初始化/潜热和真实扰动的区别，后续改写必须继续覆盖这一行为。
- 动态墙体/区块变化、输入成功提交后才消费、保存包含未消费 J、相变的实际世界转换、读者持有一致快照，均有实际消费者和故障复现依据。实现可以合并职责，但这些效果需要继续满足。
- 多核、时间切片、资源准入和粗档服务是当前多人负载能力的一部分。删掉机制会缩小服务范围；缩小逐对象预算与共享引用记账可能有价值，但目前尚无明确的等价替换证明。保留可观测性，不以删除诊断制造行数收益。

## 性能机会与精简机会分开评价

`TopologyUpdatePlanner.continuationBoundary` 将 seed 扩成相邻 3×3×3 Section，再沿已捕获/候选/通路依赖 Section 做保守连通闭包。缩小变化影响范围可能比削减换热公式更有性能收益，但通常需要更精确的依赖维护，未必减少代码量。

单 Chunk 保存的 producer 捕获局部化也已列在既有后续计划中。这类工作要比较实际扫描范围与耗时，不能把减少工作量自动描述为简化架构。

## 建议实施顺序与验收尺度

1. 先做 A1 的背景/摘要解耦和 A2 的解码边界收敛；A3 随同清理。目标是减少状态表示，不改变细档换热公式。
2. 用 B1 验证允许小数值差异的真实收益。如果减少接触后温差不合格，保留四点；不用更宽的断言掩盖局部损失。
3. 只有确认主要复杂度确实来自节点/接触/转换，且小原型表现合适时，进入 B2 的单一空间表示替换。

正常近似先沿用[已有后续计划](2026-10-29_23-38-00_thermal-next-steps.md)的 0.1°C 最大绝对温差目标作为试验标准；本次用户没有指定新阈值，不擅自将其改成 0.5°C 或 1°C。是否接受更大误差属于后续玩法取舍。

对照以同一世界 tick、同一源时间线、配置、气候、几何事件为基础，同时记录空气温度、材料 H/温度、玩家辐射或体感读数。空气误差小不代表相变完成时间和体温结果一定一致。最少覆盖：

- 单火近远/高低测点；封闭、通风和开门房间；隔墙与贴墙火源。
- 多源叠加、墙材/厚度变化、升温及熄火后的完整降温曲线。
- 水/土/冰的自然初始化、受热相变与退役，特别是高岸河道的接力扩张回归。
- 活跃与休眠交接、保存重启、部分记录删除、粗细切换及撤压恢复。

同时记录实际删除的类型/状态/转换、热任务耗时、构建代价、分配和温度年龄。保留当前实现的输出基线后再修改并统一构建验证；无需复制历史源码重编或引入路径敏感 hash。复用现有 Forge GameTest 场景，只补足缺少的差异采样，现有“能升温/能冷却”的定性断言不能替代逐时刻温差对照。

## Documentation Impact 与 Outcome

本次新增调查记录与开发日记；没有行为或合同变化，现行系统文档不需要改写。候选均明确为未实施。已得到可执行的优先级和验证范围；是否满足温差目标、实际净减行数及性能收益，留给对应实现与对照阶段。
