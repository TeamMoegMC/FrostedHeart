# 连续空气：代码可读性与可维护性设计

- Time: `2026-09-18 21:27:47 +08:00`
- Authors: `Codex; OpenAI GPT-6; primary engineering agent`
- Status: `ready; 与工程计划共同用于后续实现，当前未修改生产代码`
- Updated: `2026-09-18 22:54:39 +08:00；验证仅使用真实生产路径，不使用JUnit或独立数值测试`
- Scope: `连续空气代码职责、调用顺序、数据访问、命名、数值内核和变更入口`
- Related: [工程计划](2026-09-18_20-53-28_continuous-air-r4-cpu-engineering.md)、[已完成的温度代码整理](2026-09-16_00-15-34_thermal-readability-maintainability-refactor.md)、[当前运行架构](../docs/climate/thermal-runtime-architecture-and-optimization.md)

## 1. 阅读入口与设计目标

维护者先读本文件理解代码，再按需查工程计划的公式、时序和验收。物理模型、存档格式与精度要求由工程计划拥有，本文件不重复定义另一套。新增类型/方法均为拟定名称，不代表仓库已经实现。

代码应让维护者快速回答：温度存在哪里、谁能修改、一次更新按什么顺序发生、某个公式在哪里、改参数会重算什么。遵循最近温度代码整理的原则：保留已有有意义类型，删除无用转发，不为缩短文件而建立接口或通用Context。

默认保持现有同步 `process(batch)` 和单维度owner；先测实际cut延迟。保存试算状态是联合求解的必要数据，不等于必须引入跨任务续算状态机。只有测量证明调度公平性/尾延迟需要切片时，才按工程计划§8扩展。

## 2. 四种职责，固定依赖方向

```text
ThermalDimensionEngine                         更新顺序、正式提交
  ├─ ThermalSourceLedger + CutLoadBuffer       源事件与待交付输入
  ├─ ContinuousAirSolver                     时间步、材料段、浮力试算
  │    ├─ CoupledThermalOperator              当前方程的矩阵应用
  │    └─ PcgSolver                          线性迭代
  ├─ TopologyUpdatePlanner / TopologyCommitter 准备与安装几何变更
  └─ QueryPublication                        一致快照发布

查询 → QueryPublication.ReadCursor → AirFieldEvaluator
保存 → 已发布快照 → AirFieldCheckpoint → 现有chunk保存入口
```

`Engine`、planner和publication沿用现有类。`CoupledThermalOperator`与`PcgSolver`是solver包内两个具体计算类型，其独立理由分别是“热学离散算子”和“职责独立的线性迭代”；不增加对应factory、service、strategy或泛型矩阵接口，也不为它们另写独立数值测试。

`ContinuousAirSolver`是跨包调用的数值入口。低层operator/PCG不依赖Engine、源注册表、PageManager、QueryPublication、NBT或Minecraft世界；只接收已经解析的数值数据。Engine不读取PCG的r/p向量，也不替它计算迭代公式。

### 职责表

| 类型 | 负责的工作 | 输入/输出边界 |
|---|---|---|
| `ThermalDimensionEngine` | 一次cut顺序、源回执与空气/材料共同提交 | batch → 完成回执；不包含矩阵循环 |
| `AirCoefficientArena` | 正式空气系数与分配身份 | 外部通过生命周期/提交方法写，solver读取初值 |
| `ThermalCellArena` | 正式材料H/branch及现有phase身份 | 复用纯law，增加必要的批量数值提交入口 |
| `AirFieldLayout` | 位置对应的粗/局部函数、索引、支撑依赖 | 编译后不可变；编译、查询使用相同基函数定义 |
| `AirShapeStore` | footprint/template实例及引用、余热模式寿命 | shape制备放所属编译方法/内部helper，不放求解热循环 |
| `AirOperatorFragment` | 已编译的局部容量/导热/接触数据 | 不负责源、发布或推进全维度 |
| `CoupledThermalOperator` | 给定固定dt/浮力/材料段的 `apply(input, output)` | 输入与输出为稠密工作向量；所有系数在一次PCG内固定 |
| `PcgSolver` | 固定SPD方程的迭代、残差与收敛统计 | 不识别source、Brick或相变；使用具体operator和块预条件 |
| `ContinuousAirSolver` | 源时间分段、材料/浮力外迭代、trial接受/重试 | 读正式状态，输出一份完整trial与实际交付回执 |
| `ThermalSourceLedger` / `AirLoadTable` | 物理源身份/功率账与空间负载身份 | ledger不解温度，load不拥有第二份功率时间线 |
| `QueryPublication` / `AirFieldEvaluator` | 一致读取与指定位置求温度 | 只读发布数据，不能从查询反向修改驻留或创建mode |
| `AirFieldCheckpoint` | 新版空气记录的编码/解码 | 接收已准备数据，不读世界、不等worker、不推进温度 |

一个类名对应一项实际职责，不要求表中每个scratch都变成顶层文件。预条件因子和PCG向量由数值模块内部拥有；材料trial、源receipt、阶段统计优先使用所属类的内部类型。数值算法有独立职责且内联已妨碍阅读/修改时才提取包内类，不按行数拆文件，也不为方便写单元测试而拆类。

## 3. 主流程应能从上往下读懂

连续后端入口采用下列顺序。示例是调用骨架，方法名供实现定位，省略具体参数及原有work-limited处理；不要求为每行新增一个转发方法。

```java
// ThermalDimensionEngine: one completed numerical interval, then endpoint changes.
private ThermalCompletion processContinuousBatch(ThermalInputBatch batch) {
    validateBatch(batch);
    prepareCutStorage(batch);          // capacity and immutable inputs

    sources.acceptAndRecord(batch.sourceEvents(), batch.targetTick(), cutLoads);
    continuousSolver.solveCut(cutInputs, cutLoads, trialState);
    commitNumericalState(trialState); // air + bodies + delivery receipt

    applyEndpointInputs(batch);      // ACK, phase intent, environment
    prepareAndCommitTopology(batch); // existing prepare/commit/release ownership
    publishCompletedCut(batch);
    return collectCompletion(batch);
}
```

`commitNumericalState`有实际职责：在任何正式写入前核对全部待提交身份，写air/body，确认源receipt一次，再推进数值时钟。不能在一个材料循环中写一半后再做可能失败的试算。

`prepareAndCommitTopology`应在同一文件紧邻主流程展示既有planner/committer/bindings/release顺序；若提取只增加转发层，则直接把调用留在入口。其内部可能保留已推进的旧合法布局，具体时序按工程计划§7，不隐藏为“事务自动回滚”。

开发期新旧后端选择只放在Engine入口及组装位置。不会在每条接触、材料温度读取、PCG和查询乘加中散布 `if (continuous)`。旧存档兼容已取消，不新增旧字段别名和v4读写adapter。

### 求解器内部再按一层时间顺序展开

```text
选下一源时间区间
  → 读取本段已接受起点
  → 选择材料段/屏障，计算浮力
  → 构造当前方程与预条件
  → PCG求解
  → 检查材料端点、浮力自洽、时间误差
  → 接受子步，或恢复本段起点并缩短时间步
最后输出完整cut trial与源receipt
```

外迭代、PCG和相变端点定位各有命名清楚的循环。不能用一个大while加大量布尔字段同时表示三种进度；也不为每个阶段建立实现同一接口的对象。

`solveCut`只写trial/scratch，不调用正式arena写H，不发phase请求。solver自身接受一个子步是更新本cut的trial起点，不等于向世界发布或提交正式状态。

## 4. 数据与可变状态的边界

| 数据 | 唯一写入者 | 使用期限 |
|---|---|---|
| 正式air系数、body H/branch | Engine控制的数值提交；几何变化走现有拓扑提交 | 直到下一次相应提交 |
| 静态layout/shape/fragment | 编译/prepare阶段 | 提交后只读，所有使用者解除引用再释放 |
| PCG向量、材料trial、候选浮力 | solver内部 | 当前计算；容量可复用，内容不能作为发布快照 |
| CutLoadBuffer | ledger记录阶段 | 求解期间只读，receipt确认后才能清空 |
| 查询缓冲和checkpoint片段 | publication准备阶段 | 按现有一致性协议发布，IO不得持会被覆盖的scratch |

采用primitive数组表达高频数据，例如 `coefficientValuesC[]`、`materialEnthalpiesJ[]`、`materialBranches[]`。不为每个系数、接触或PCG迭代创建对象。调用边界可用少量明确类型携带参数，不把所有owner和数组塞进一个万能Context。

借用数组的方法用Javadoc写明：owner、索引域、单位、是否可写、何时失效。编译结果可转移数组所有权，不默认clone；跨线程发布按既有snapshot生命周期隔离。禁止公开“任何调用者都能改”的裸数组字段。

热循环可在一次方法入口取得经过核对的数组/范围，随后直接遍历；不需要每项调用多层getter、哈希查句柄或验证generation。边界身份检查与内部数学循环分开。

### 状态表达

- `geometryRevision`、`allocationGeneration`、`batchSequence`、`layoutEpoch`保留各自含义，不统称version。
- material branch继续使用已有 `SENSIBLE/HEATING/COOLING`；waiting依照现有phase协议。不要另造镜像phase状态机。
- 可由已有数据低成本得到的状态不新增布尔镜像，例如不同时存 `ready`、`valid`、`hasResult`表示同一个结果。
- 空几何/无接触允许使用共享EMPTY数组/结果；未知几何、尚未计算和数值失败是不同含义，不统一返回0°C。
- 循环正常收敛/需减步用所属结果或明确分支；异常不承担正常时间细分控制流。诊断保留具体batch、位置、材料段和残差，不吞掉原因。

## 5. 数值代码保持可读，同时保留连续内存访问

### 统一术语和命名

| 术语 | 含义 | 代码名称示例 |
|---|---|---|
| cut | 一次batch覆盖的模拟时间区间 | `targetTick/elapsedSeconds` |
| shape / basis | 空间形状/基函数，自己不存热量 | `shapeValues/basisWeights` |
| mode | 一个局部形状及其动态系数 | `localModeIndex/coefficientC` |
| trace | 接触位置的基函数权重列表 | `contactOffsets/contactUnknownIndices/contactWeights` |
| stencil | 一个物理源的空间输入权重 | `sourceLoadId/loadWeights` |
| body | 一个材料方块的热状态 | `materialSlot/materialEnthalpyJ` |
| trial | 尚未正式提交的候选状态 | `trialEnthalpyJ/trialBranch` |
| Schur消元 | 精确消去合格材料未知量 | `condensedMaterials/recoverMaterialStates` |

物理变量带单位：`powerW`、`energyJ`、`temperatureC`、`capacityJPerK`、`conductanceWPerK`、`elapsedSeconds`。跨方法字段使用意义明确的名称。局部数学推导中的 `r/p/Ap` 可以保留标准PCG符号，方法注释给出对应关系；不把正式空气状态长期命名为 `a/b/tmp`。

区分 `airSlot`、`materialSlot`、`unknownIndex`、`pageSlot`、`brickIndex`。source id与stencil id也分开命名。索引转换只发生在编译/边界，不在一条表达式里反复解包位置、查slot、读温度、累计能量。

### 接触算子示例

将空气权重和材料的 `-1` 一起编入接触列表。工作向量中的各项单位均为°C：空气项为基函数系数，普通材料项为 `T-Tref`。以下是后向Euler矩阵应用的一部分；输出单位J。它计算线性算子，不直接修改材料H。

```java
private void addContactContribution(
        int contactIndex,
        double elapsedSeconds,
        double[] inputC,
        double[] outputJ
) {
    int firstWeight = contactOffsets[contactIndex];
    int endWeight = contactOffsets[contactIndex + 1];

    double temperatureDifferenceC = 0.0;
    for (int entry = firstWeight; entry < endWeight; entry++) {
        int unknownIndex = contactUnknownIndices[entry];
        temperatureDifferenceC += contactWeights[entry] * inputC[unknownIndex];
    }

    double transferredEnergyJ = elapsedSeconds
            * contactConductancesWPerK[contactIndex]
            * temperatureDifferenceC;
    for (int entry = firstWeight; entry < endWeight; entry++) {
        int unknownIndex = contactUnknownIndices[entry];
        outputJ[unknownIndex] += contactWeights[entry] * transferredEnergyJ;
    }
}
```

这里局部变量解释了索引、物理意义和单位，仍然只有两段数组循环。代码旁应说明它实现 `dt*G*d*d'`，以及为什么材料权重为-1；无需注释“循环遍历数组”。凝聚后的负rank-one项放独立的 `addCondensedMaterialContribution`，不要塞进该方法的多种mode分支。

`CoupledThermalOperator.apply`按容量、体积导热、普通接触、材料连接、凝聚修正、边界贡献分组调用。每组只清晰地承担一个数学项；输出向量在入口清零一次，各组只累加。不得某个helper偷偷清空前面贡献。

### 形状求值只有一个定义

整数格定位、局部分支选择、三线性权重由 `AirFieldLayout`所属纯方法提供。编译source/contact和查询evaluator复用同一方法或其已编译结果，不各自写一套不同插值公式。这一共享只针对数学定义；publication版本检查仍归查询层。

## 6. 性能优化的封装边界

| 优化 | 应集中在哪里 | 普通业务代码看到什么 |
|---|---|---|
| packed对称矩阵、固定8项模板 | operator内部命名明确的内核 | 同一个 `apply` |
| 合格材料精确消元 | solver的资格选择、operator凝聚项与恢复步骤 | 相同材料H/branch结果 |
| 块Jacobi及热启动 | solver/PCG内部 | 相同误差验收和试算结果 |
| 缓存复用 | 编译结果owner的版本/dirty逻辑 | 已准备的有效数据 |
| 查询无分配 | evaluator + caller-owned scratch | 温度、时间和质量 |

先完成一个清晰的生产packed路径，用真实服务器场景建立结果和成本基线。专用内核替换后重跑同一生产场景，比较公开温度曲线、实际材料相变、存档和完整耗时；不保留测试侧参考内核，也不为“将来也许用”保留多个生产算法开关。

手工展开循环、改变布局或融合计算，需要真实服务器profile和同场景生产前后结果。不得以“JIT可能更快”为由把可读代码改成多层位运算或复制四份同一公式。确定的热循环避免stream、装箱、每项lambda及临时集合；编译和准备阶段仍可使用项目已有标准容器，不把低频代码全部手工数组化。

dirty更新集中在实际owner入口：功率变化归源、几何变化归planner、材料段变化归solver、发布版本归publication。缓存表及失效条件以工程计划§6为唯一详细定义；调用者不自行复制版本号再判一次。

## 7. 常见改动应该落在哪里

| 将来的需求 | 首个修改位置 | 验证重点 |
|---|---|---|
| 改篝火额定功率/份额 | 原profile/config；ledger无需新分支 | 端口总功率、时间输入 |
| 增加一种材料相变 | 原MaterialThermalLaw/数据入口 | trial分段、等待、ACK |
| 改局部shape候选 | shape制备与参数定义 | M/K/trace/query共同形状、完整精度 |
| 优化矩阵乘法 | CoupledThermalOperator内部 | 真实场景温度/相变前后结果及服务器CPU |
| 更换预条件参数 | solver/PCG内部 | 相同残差和温度误差、setup总成本 |
| 修复查询坐标/分支 | layout求值或evaluator | 公共面、负坐标、跨Page |
| 改块后热量不对 | planner投影与迁移外部能量 | surviving体积、隔离区域H、材料身份 |
| 读取新版保存温度错误 | checkpoint codec/恢复投影 | 保存误差、时间、owned H |
| 主线程保存卡顿 | 先profile数值cut与发布/等待 | 不把编码、求解和排队混为一个耗时 |

若普通功率改动必须同时改Engine、PCG和checkpoint，说明边界被打穿；应修正具体依赖。不要为让表看起来整齐而提前添加抽象层。

## 8. 实现评审的具体标准

- 维护者从Engine入口能找到两次正式提交及发布顺序，从solver入口能找到时间步/外迭代/PCG三个层次。
- 某个温度结果能沿 `位置→layout权重→已发布系数` 找到来源；没有查询专属温度副本。
- 每份可变数组只有明确owner；一个失败trial不会留下正式H、branch或phase请求变化。
- 同一方程项只有一个生产实现位置，没有测试侧第二实现；单位和索引域可从名称/注释判断。
- 每个新增类有独立职责；纯转发包装、一次性factory、空接口和万能Context不进入实现。
- 代码注释解释物理假设、单位、数组布局或生命周期原因。当前行为不写成“未来可能”，也不把开发对话放进源码。
- 验证只用真实生产场景：实际放置/点火/机器工作，经正式source、Engine、publication、查询和世界相变/保存读取结果。GameTest只做世界操作与结果检查；不直接创建测试engine、改H、手动step或伪造ACK。
- 不新增或运行JUnit，不写独立数值参考、纯算子测试、合成矩阵测试或只检查helper调用次数的测试。复用旧GameTest前先确认它实际走了生产路径。
- 新旧存档兼容继续排除。调度切片是否实施由P2结果决定；没有证据时不创建MORE_WORK、队列轮换或跨调用PCG状态机。

## Outcome

已确定供后续实施使用的代码职责、调用骨架、状态所有权、命名和优化位置；尚未创建上述生产类。按工程计划P0–P6直接实现生产闭环，并以真实服务器行为与性能验收。该文件不是已经完成可维护性验收的声明。
