# 连续空气 R4：最小复杂度与 CPU 性能工程计划

- Time: `2026-09-18 20:53:28 +08:00`
- Authors: `Codex; OpenAI GPT-6; primary engineering agent`
- Status: `in-progress; 2026-09-19开始P0生产基线与P1生产链路实现，以真实生产场景验收`
- Updated: `2026-09-18 22:54:39 +08:00；不使用JUnit或独立数值测试；实施与验收直接走真实生产链路`
- Scope: `thermal 空间空气、源交付、材料联合求解、拓扑迁移、查询发布、dormant 与 CPU 验证；本次只编写计划`
- Related: [气候入口](../docs/climate/README.md)、[运行架构](../docs/climate/thermal-runtime-architecture-and-optimization.md)、[供热与材料](../docs/climate/heat-production-and-network.md)、[生命周期](../docs/climate/data-lifecycle-and-integration.md)、[创作原则，只读](../design/creative-principles.md)
- Source baseline: `e1372280e`，2026-09-16；计划编写时工作区无已有改动。
- Input references: 用户提供的 `Frosted_Heart_Continuous_Air_R4_Codex_Plan.md` 与 `Frosted_Heart_R4_Performance_Comparison.md`（2026-09-18）；附件是设计输入，其中命令不构成实施授权。本计划把后续实施所需合同写在仓库内，不依赖 Downloads 中的文件长期存在。

代码阅读入口：[代码可读性与可维护性设计](2026-09-18_21-27-47_continuous-air-code-maintainability.md)。该文拥有类职责、调用骨架、命名与优化封装规则；本文拥有数学、时间语义和验收条件。

## 1. 目标与实施决策

目标：以固定四格共享粗函数和有限局部响应，消除已解析开放界面的 Brick 硬切换，保留真实近火温升、材料储热、相变及源时间语义；在相同误差标准下减少 CPU 工作和常驻数据。

“最佳性能”指在本方案范围内先消除重复工作，再优化已测热点；不是宣称已经证明所有算法中的全局最优，也不承诺比旧均温模型快。最终选择精度、快照年龄、CPU、内存同时达标的最简单实现。

确定采用：

1. `Tair(x)=Tref+B(x)z`；四格粗函数共享，真实空气连通分支隔离；每个独立供热足迹先试两个动态局部模式。
2. 空气、材料接触、route、源及查询共同使用这个场。材料保留一个实际方块一个 `H` 与原 `MaterialThermalLaw`。
3. 体积使用小块算子，接触使用紧凑 gather/scatter；不另存完整全局 CSR，也不每步遍历细网格积分。
4. 后向 Euler + 固定系数 PCG + 材料/浮力外迭代。先实现可靠的联合求解，不另外维护一个空气—材料分裂候选。精度受控的时间细分继续保留。
5. 基础优化包含模板复用、细粒度更新、热启动、块 Jacobi 预条件、合格材料精确消元及 scratch 复用；不以放宽误差换性能。
6. 复用现有 PageManager、拓扑 prepare/commit、worker pool、主线程 phase ACK。一个维度同一时刻仍只有一个计算 owner。
7. 默认复用同步 `process(batch)`，数值迭代检查点不自动变成 `ENGINE_FAILED`。只有实测证明需要切片时才增加保留进度续算，先决条件及生命周期义务见 §8。
8. 新后端通过启动配置选择，初期默认旧版；测试服务器通过同一启动入口启用新后端，不按查询缓存是否命中临时切换空气模型。前后对比在真实服务器场景分别运行，不增加生产SHADOW服务或独立数值测试驱动。

V1 不做：逐格常驻动态空气、AMR、通用 CFD、GPU/JNI、新线程池、维度内并行求解、全域 AMG、动态热岛调度、通用矩阵框架、多套形状缓存、自动调低热源功率或调高混合导纳。只有实测证明缺口且基础方案已经正确，才另立针对性优化。

旧后端仅用于开发期基线比较，不因存档兼容保留双后端或设置兼容期。新后端不读取、转换或双写旧热学存档；旧热学历史按缺失处理，从当前世界状态和自然温度初始化。V1 不做整个 thermal 包的命名/分层重构。

## 2. 已核实的当前实现

下表以 `src/main/java/com/teammoeg/frostedheart/content/climate/thermal/` 为代码根；实施前再次对照实际 HEAD，不复制附件源码或编译另一份生产类。

| 锚点 | 当前事实 | 本计划影响 |
|---|---|---|
| `topology/BrickTopologyCompiler.compileCells` | 全空气 Brick 一个状态，混合区域按连通空气建状态 | 几何 component 与空气未知量分离 |
| `mesh/ThermalCellArena.stageBrickCells` | 常规空气容量为 `64*cair`；材料独立 H | 新空气单独保存系数；旧 arena 保留材料 |
| `solver/ThermalSolver.step` | 一次 Air/Material/Far 遍历，交替顺序；普通材料/Far 有一秒系数缓存 | 新旧 CPU 基线必须分别记录 |
| `runtime/minecraft/engine/ThermalDimensionEngine.process` | ACK/intent → 源结算 → 拓扑 → 一次 elapsed-dt 换热 → publish | 连续后端改用 §7 的显式顺序 |
| `source/NodePowerAccumulatorArena.settleTo/drainAllPendingEnergyTo` | 保存累计 J，最终直接写 `ThermalCellArena`，中间时间段被合并 | 新后端记录带时间段的输入，完成求解后确认 delivered |
| `source/ThermalSourceLedger` | 每 source 最多3物理端口；同 tick 按输入顺序；含 signed impulse | 增加 AIR_STENCIL，不把八个基函数变八个端口 |
| `mesh/MaterialThermalLaw` | `temperatureC/slopeKPerJ/selectBranch/energyLimitJ` 是可复用纯函数 | trial 使用这些函数，不调用正式 arena 的有副作用更新 |
| `solver/MaterialEnthalpyExchange` | 逐边分段换热；waiting 端点双方不交换 | 保持定律和等待规则，连续后端不用它再次更新同一接触 |
| `topology/BrickTopologyCompiler.connectFace` | 存在材料—材料连接 | 独立 body 消元不能假定适用于整片墙或地面 |
| `runtime/async/ThermalDimensionMailbox` | 一 batch 在途；任何 process 异常被转为 ENGINE_FAILED；保存等 completion | 默认不改接口；若测出切片需求再扩展，不用 topology WORK_LIMITED 冒充数值完成 |
| `query/QueryPublication` | 双缓冲温度及材料快照，ReadCursor 版本检查 | 将空气系数和布局引用纳入同一次发布 |
| `runtime/minecraft/MinecraftThermalInput.sampleAir` | double 坐标最终读单 slot | 新 evaluator 保留精确 xyz |
| `MinecraftThermalInput` 的 item quarter cache | 命中后复制整份环境，包含首次查询空气温度 | 新后端每次求精确空气；缓存只复用允许复用的其余项 |
| `persistence/minecraft/DormantChunkThermalState` | root v4；空气数值载荷640B，超限可退均值；材料单独保存 | 同一root升级为新格式，只读写新版空气和材料 |
| `runtime/minecraft/MinecraftThermalInput.MEMORY` | static 128 MiB 根预算，query 子预算16 MiB | 新数据共享该预算，不另加隐含128 MiB |

已核对的源码默认值（不是用户运行配置）：`cair=1200 J/(block·K)`、`kmix=96 W/(block·K)`、篝火8000 W且20%辐射，故默认空气6400 W；Generator level=1 时空气8000 W。近源直接空气混合半径4格、倍率4、重叠取并集。浮力参数 `(minimumFactor=.25, maximumFactor=4, temperatureScale=10°C)`。cut 为20 tick。实际各设备 level 与事件频率在 P0 采集。

现有 arena 最大131072 slots、live cells 65536、Page 3200 等是旧容量边界，不能直接解释为新系数/矩阵工作预算。最近固定 flat/seed=0 GameTest 为102/102，自然世界仍有未解释失败，见[最近记录](../diary/2026-09-16_16-00-58_thermal-unused-abstractions.md)。部分既有GameTest直接创建engine或改写arena H，不能仅凭GameTest名称把它计为生产链路证据；历史总通过数也不是本计划验收结果。

仓库指定的 `.Codex/memory/project-structure.md`、`architecture.md` 当前缺失；本次以源码、活文档和 diary 核对，不创建替代记忆。未修改 `design/`。本范围不修改整合包；若以后涉及 KubeJS、配方或 pack 配置，先定位伴随仓库 AGENTS 并分别验证。

## 3. 真实生产验证合同

### 3.1 唯一验收路径

按用户要求，本工作不新增或运行JUnit，不建立独立数值参考解、纯Java数值驱动、矩阵/算子单元测试或合成能量测试。验证使用真实Minecraft服务器和真实生产代码；GameTest只用于布置世界、执行玩法动作、等待实际tick和检查公开结果。

有效路径是：真实放置/点火/机器工作 → 正式source发现与输入 → 原生产调度和Engine → 新求解器 → 正式publication/消费者 → 世界相变及实际chunk保存恢复。不得在测试中直接new一个隔离engine、写arena H、拼造求解矩阵、手工调用step或伪造ACK来代替该路径。诊断可以只读生产状态，但不改变行为。

现有 `ThermalTimingGameTests` 等含直接改H/调用独立fixture的用例不作为本次验收，也不将这种测试换个框架名称继续写。历史测试无需为本任务全部删除或修复；本次入口选择真正走生产路径的用例，不能无筛选运行整套后再用总通过数背书。

### 3.2 场景结果与比较口径

| 项目 | 生产场景中的检查 |
|---|---|
| 空间表现 | 同一建筑与热源整体平移，按相同相对坐标/世界条件比较温度曲线，目标差≤1°C；跨Brick/Page行走无分区硬跳变 |
| 近火强度 | 用真实功率记录峰值、不同距离温升、加热/熄火曲线；不能以降功率或压平全部温度消除跳变 |
| 材料和相变 | 真实冰/水及普通材料受热、冷却、世界BlockState改变和ACK后行为；无重复转换或热量突然消失 |
| 动态世界 | 实际开关门、放墙、拆设备、移动热源；检查第一/第二个完成cut及后续恢复，而非仅最终均值 |
| 保存恢复 | 通过实际chunk卸载/加载、服务器保存/重启比较温度及材料状态；新版历史不无故均温重置 |
| 优化前后 | 对同一真实场景、设备操作、参数、采样时刻和快照年龄分别运行完整版本，比较结果曲线与完整CPU/heap/等待 |

旧均温实现用于记录旧玩法和成本，不作为新模型温度真值。取消独立细参考后，原“相对细参考最大误差≤2°C”不再作为测试交付；没有独立依据时该项标未认证，不能改名为“真实场景通过”后宣称已证明2°C绝对精度。生产场景的平移差、温度跳变、存取差和优化前后差分别报告。

求解残差/材料端点判定仍是推进算法的一部分；诊断记录实际运行中的残差、源交付、材料H与边界热流，用于解释现场问题，不另建数值正确性测试项目。初始求解温度修正目标0.01°C、时间估计0.1°C、同拓扑投影0.25°C、保存量化界0.125°C保留为实现参数，最终检查它们在真实场景产生的可观察结果，不把内部阈值直接当精度认证。

固定世界/种子与明确天气、自然温度便于复现，仍须运行实际世界tick和生产source。不得通过写内部温度或为测试特判来制造场景。结果记录采样坐标、tick、实际BlockState及生产参数。新开冷热接触不要求瞬间保留旧双侧极限，但附近可达位置和后续响应仍需实测。

## 4. 最小数据结构与所有权

继续使用现有 `mesh/topology/solver/source/query/persistence` 职责，不创建新的通用 service 层。以下为计划名称，尚未实现；小 record/scratch 优先做所属类的内部类型。

| 新结构 | Owner / 最少数据 | 生命周期 |
|---|---|---|
| `mesh/AirCoefficientArena` | engine；`double[] z`、allocation generation、live/free信息 | 唯一正式空气系数，不复制材料字段 |
| `mesh/AirFieldLayout` | Page/Brick publication；component、粗系数引用、局部shape引用、依赖Page/布局epoch | 不可变，随拓扑提交替换 |
| `topology/AirShapeStore` | engine；footprint→两个模式、静态shape、源引用/余热、受影响Brick索引 | 合并 R4 的 footprint registry 与 shape owner；模板和实例分开 |
| `solver/AirOperatorFragment` | solver；粗容量、交叉/局部容量、水平/竖直K、接触trace与依赖 | 对应现有 fragment replacement |
| `source/AirLoadTable` | engine/source；stencil身份、handles、weights、引用 | scalar端口指向此身份；与shape生命周期不同 |
| `source/CutLoadBuffer` | 当前cut；primitive span/impulse数组、顺序、pending J | 事件接受一次，数值消费完成后释放 |
| `solver/ContinuousAirSolver` | engine；时间/材料/浮力试算及接受 | 数值入口；委托包内operator/PCG，持有 MaterialTrialBuffer |
| `solver/CoupledThermalOperator`、`solver/PcgSolver` | solver包内具体计算类型；固定方程应用、线性迭代及预条件 | 无世界/源事件/发布依赖；不创建通用数值框架 |
| `query/AirFieldEvaluator` | 查询侧纯读逻辑；调用者scratch | 不拥有第二份温度权威 |
| `persistence/minecraft/AirFieldCheckpoint` | 不可变owned-section描述及codec | query coherent snapshot→chunk NBT |

`PreparedTopologyChange` 增加所属的 air delta；Engine所属 `NumericalCut` 保存本次输入、trial与receipt，默认只在同步调用内使用，不预建跨调用阶段枚举。只有实施§8的条件优化时才增加cursor。不建立测试参考求解器，生产不引入第三方数值库。V1 的 `WorkMeter`/统计为 solver 内 primitive 字段，不做可插拔预算框架。具体类和数组边界见代码设计，不将所有数值循环堆进Engine或一个大solver。

句柄使用 slot + 独立 allocation generation；分配代次不沿用 Page lifecycle。采用64位分配序号或等效不会在正常运行耗尽的计数，失效后不复用旧身份。布局epoch、generation只在生命周期边界和一次查询入口核对；inner matvec 使用已解析的稠密工作索引，避免逐项哈希/句柄检查。

每次结构改变编译一个工作索引布局：air handle/body slot→连续求解下标；fragment/trace存下标。旧 arena slot仍用于材料身份与最终提交。不为每次迭代重建映射。结构变化造成的全工作索引重编成本必须实测，V1 不增加复杂增量压缩器。

## 5. 空间基与算子编译

### 5.1 粗函数与局部响应

四格锚点用数学 floor 处理负坐标。锚点只在至多相邻八个Brick的支撑内，按直接空气六面连通分支拆身份；一个Brick component通常引用八个锚点。公共开放面使用相同函数/分支，墙两侧分开；有限通风走route。小腔体相关函数用局部秩检查和确定去重处理，保留常数表示，不靠对角epsilon添加热容。

footprint key包含实际接收面/面积分布、空气分支、响应参数版本；不只使用设备坐标。相同footprint合并功率并共享模式，不同位置不强行合并。

初始候选 `l=1,3; rho=.45; R=6`，不是已认证默认。开放模板可用有限核 `exp(-r/l)/sqrt(r²+rho²) * max(0,1-(r/R)²)²`。复杂几何求 `(Llocal+mu*C)local u=unitPortLoad`，`mu=(kmix/cair)/l²`；真实墙切断连接，已有合法route保持有限阻力。外壳零修正仅定义支撑，不成为物理吸热边界。

静态shape存单位格顶点分支值，单位空气格内三线性求值；顶点只按相邻至多八格的直接空气连通拆分。归一成无量纲形状，系数单位°C。两模式相关时做容量Gram检查/确定变换，M/K/trace/query使用同一变换。开放模板全局共享；复杂模板V1按footprint及完整几何版本复用，不做跨复杂几何的通用内容缓存。

R6至多13³方块位置/14³顶点坐标，跨最多64 Brick/8 Page，连通分支另计。捕获仅请求已加载几何，不因模板强加载chunk。静态job记录版本、primitive几何与cursor，可按slice前进；发布前重新核对依赖，用当前cut温度投影，不能使用job开始时的旧温度。

新模式系数初始0；停火只去输入，余热保留。源移走后旧模式留在原处冷却；新位置建立或复用模式。暂未就绪用合法粗场承接输入，标WARMING且计入全过程误差/延迟；不能从认证曲线中删掉冷启动。

### 5.2 容量、储热与导热

定义 `e=[粗系数为1, 局部系数为0]`，`B(x)e=1`：

```text
Mexact = integral(c B'B dV)
h = integral(c B' dV)
Hair = h'z
Mcc = diag(rowSum(Mexact_cc)); Mcs/Msc/Mss 保留
e'M = h'
K = integral(grad(B)' diag(kx,ky,kz) grad(B) dV)
```

体积每单位格2×2×2 Gauss，面2×2 Gauss；分片常系数下相应积分精确。普通无局部项全空气Brick使用共享预积分模板。稳定步只应用已编译数值，不重新采样几何、算exp/sqrt或解局部PDE。

存储选择：粗容量对角、粗/局部交叉、局部容量上三角；K水平/竖直各一份对称packed块；索引与数值primitive数组。不要再额外保存全局CSR或完整细图B。最初实现直接packed matvec，普通模板有独立固定8项内核；待测后才选择轴向张量收缩等更复杂内核。

### 5.3 接触、route与浮力

材料接触：面Gauss权重已吸收进 `Gq`；`q=Gq*(Tbody-Tref-bz)`，空气负载加 `b*q`，body减 `q`。operator用 `Gq*[b,-1]*[b,-1]'` 的 gather/scatter，每条trace O(m)，不展开 O(m²) 边。不同面trace一般不能合并成一个平均trace；仅完全相同trace可聚合G。

route用 `d=ba-bb`，应用 `G*d*(d'x)`；保留出口面/面积、阻力和 `AirRouteValidity.valid/activeForSolve` 各自语义。FarField用 `G*b*(b'x)` 和 `G*(Tnatural-Tref)*b`，资格及总导纳继承现有规则。材料—材料采用既有接触去重结果，在新联合求解中只执行一次。

浮力采用R4新增离散：每component上下半部平均trace `blo/bhi` 与平均高度差d，`deltaT4=(blo*z-bhi*z)*4/d`；代入原clamp公式。保存 `Khorizontal`、`Kvertical`，外迭代得到factor，整个PCG期间固定 `K=Kh+factor*Kv`。无有效上下采样factor=1。在实际上下热源、竖井和房间场景记录温度响应，不声称逐边复现旧浮力。

保留近源混合4倍的空间分布及并集规则。V1沿用已提交系数在一个cut内冻结、端点启停更新影响未来cut的约定；源功率时间线仍按实际tick。实际服务器前后比较保持相同混合时序。若以后要求混合也在源启停tick切换，作为显式时序变更在真实启停场景验证。

## 6. 联合求解与等价 CPU 优化

### 6.1 基本推进

工作向量为 `[air z, 未消元普通body theta]`，`theta=T-Tref`。冻结当前材料段和浮力后，空气部分是 `(M+dt*K)zNext=M*zOld+dt*f`，接触及材料未知量共同进入同一个SPD系统。普通材料段写为 `H=Cseg*theta+beta`，`Cseg=1/(dT/dH)`，`beta=H-Cseg*theta`；body RHS是旧H减beta加本段外部输入，不能漏offset。

从上一个已接受时间点热启动；外迭代从上一候选解继续。PCG向量 `x/r/p/Ap/zPreconditioned` 常驻复用，operator实现一个固定 `apply(x,out)`；先清输出，再叠加体积和接触贡献。允许融合独立向量循环减少内存遍历，不改变点积/残差检查的数学含义。

直接实现一个清晰的生产matrix-free路径并接入实际Engine，通过真实场景检查行为和成本；不创建测试侧组装小系统或第二套参考内核。内部温度和能量全用double；float仅用于满足既定误差界的静态shape存储，不使用近似倒数或低精度求解。

### 6.2 材料试算及端点

`MaterialTrialBuffer` 为solver内部SoA：正式slot/generation、law引用、old/trial H与branch、pending/waiting、接触offset。以law纯函数处理正负方向和潜热反向过程。正式arena只在统一提交时写；试算中不调用 `materialEnergyLimitJ/acceptExternalEnergyJ` 或发phase请求。

latent平台温度固定，但H仍由全部接触净热流及实际接受源输入推进。试算跨越合法段端点时，回到本段起点，定位最早端点时间并联立重解；不同面共享同一个body能量额度。到达完整转换端点后在trial设置waiting屏障，剩余时间暂停该body两侧接触，外部输入记unaccepted；内部未交换能量留在原端，不作为loss。

到达sensible/latent边界需要换段；同时到达的端点按容差批量处理、统一更新active set。相变事件数单列，不能用“每cut至多20个源时间段”限制它。先用带区间括定的时间细分定位，不新增通用非线性优化器。端点时间/能量误差按§3验收，不能先独立截断body H再忽略空气多扣的能量。

冻结段解出来后重算各body净热流与branch，直到材料段及浮力状态自洽。基于合计净热流选择分支，不把最后遍历的一张面当作方向。`RETRY/ENQUEUED/ACKED_WAITING_LAYOUT/materialLayoutPending`均沿现有等待合同处理。ACK不再扣潜热。

### 6.3 独立普通body精确消元

资格在布局/段变化时更新：正温度斜率、非waiting、没有任何实际材料—材料或routed body—body热连接。全维度统计 `eligibleBodyCount/totalBodyCount`。

设接触为 `(bj,Gj)`，输入为本段 `Jext`：

```text
a = dt * sum(Gj*bj)
d = Cseg + dt * sum(Gj)
rBody = Hold - beta + Jext

保留原空气接触项 dt*sum(Gj*bj*bj')
凝聚算子再加 -a*a'/d
空气RHS += a*rBody/d
thetaBody = (rBody + a'*zNext)/d
Hbody = Cseg*thetaBody + beta
```

按body去重handle并编译 `sum(Gj*bj)`；仅dt变动时缩放，d和RHS为标量更新。负rank-one项用gather/scatter应用，不展开稠密矩阵。消元后仍检查分段资格；跨段回到trial重解，不沿用已经失效的消元结果。凝聚保持原联立方程，不能再运行旧body交换。

### 6.4 预条件：只有一种基础实现

使用不重叠块Jacobi：普通粗项和未消元body先用标量对角，局部模式按确定owner分组、每组≤32个未知量，取当前SPD算子的主子块Cholesky。每个未知量恰好属于一个块；相邻模式耦合仍保留在真实算子。一个shape触及多个Brick并不复制其未知量。

预条件构建必须包含容量、交叉和凝聚后的实际贡献。不能给奇异块加任意epsilon；回查basis相关性、漏项或实际零自由度并在编译期正确消除。

同一PCG中预条件固定。上一子步的SPD预条件可作为近似继续使用，即使dt或浮力改变，最终仍按新算子真实残差验收；这与复用旧方程解不同。当结构改变或实测迭代显著增加时重建，统计setup收益，避免每轮外迭代无条件分解。保留一种重建策略，由P2数据确定触发值，不建立多算法自动调参系统。

初始64次PCG/4轮外迭代是统计与调整检查点，不是“达到数字便接受错误结果”或“立刻重启”的阈值。残差正常下降可保留Krylov进度续算；停滞先重算真实残差并更新预条件，非线性不收敛回到子段起点减步。记录所有额外工作。

### 6.5 缓存和失效表

只保存已有结构对应的版本/dirty位，不加全域缓存管理器。

| 改变 | 重算 | 可保留 |
|---|---|---|
| 正功率数值改变 | 负载段/scalar RHS增量 | shape、M、K、接触、预条件 |
| 启停/足迹变化 | 源记录；端点mixing/footprint dirty | 合法余热模式和未受影响fragment |
| 自然温度变化且G不变 | FarField RHS | M/K/trace |
| wind改变 | FarField G相关项 | 几何、shape、体积M/K |
| buoyancy factor改变 | matvec缩放、必要对角 | Kh/Kv和几何trace |
| dt改变 | M+dt*K组合、Schur标量/RHS | M/K/trace及仍有效SPD预条件 |
| body段变化 | Cseg/beta、屏障、消元资格 | 原law及未改变接触 |
| 几何/mode布局变化 | 有限依赖闭包内shape、积分、trace、工作索引 | 未变body span、其它静态模板 |

新增source stencil命中完全相同的不可变描述可聚合；变功率按受影响目标scatter增量。为剩余时间步保留当前RHS向量，不能每个事件扫描全部source。体积packed项的局部模式密度可能使其O(p²)，这不是rank-one接触能消除的成本，必须计入。

## 7. 源账本、时间与提交顺序

### 7.1 一次接受，记录式交付

给 `SourceBinding.Kind` 增加 `AIR_STENCIL`，身份指向 `AirLoadTable`。`THERMAL_NODE`在连续后端仅用于材料；原declared/degraded loss保持。不要让 `isThermalNode()`把新stencil伪装成arena slot。

将源积分的“记录接收端”作为一个小接口或所属内部类型接入 `NodePowerAccumulatorArena`；legacy仍直接drain arena。新路径在功率改变、rebind、release、impulse、最终drain前结算旧输入，写入：

```text
span(kind,id,generation,fromTick,toTick,watts)
impulse(kind,id,generation,effectiveTick,joules,stableOrder)
```

span表示 `[fromTick,toTick)`，秒数为tick差/20。相同tick事件仍逐个执行，之后才能合并没有中间副作用的同目标负载增量；不同tick不可因总J相同而压平。相邻且相同目标、相同功率的连续span可合并。首次/末次端口分配继续用最后端口 `total-assigned`。

accumulator目标键必须包含kind，避免材料slot和stencil id冲突。zero-sum仍需保留事件先后及signed impulse；仅最终功率为零不代表没有待交付记录。释放source后，tape仍持有本cut使用的stencil/材料身份直到数值消费结束。

逐项改造ledger：`reserveBatch/writePort/writeBinding/sameBinding/installEffectivePower/movePortBinding/applyImpulse/unloadSource/referencesThermalNode/energyBalance/close`。补 `referencesAirStencil`，event observer继续维护source索引，但数值算子读本cut冻结布局。首次记录前预留tape和binding容量；不足时先做合法资源回收/准备，不接受半个事件批。

`acceptedTick`可暂时领先`solvedTick`，差额明确存于当前NumericalCut。账本恒等式为：

```text
output = delivered + declaredLoss + degradedLoss + unaccepted + pending
```

`delivered/unaccepted`只在共同数值提交时确认一次，完成cut时pending归零。迭代重试、续算和发布重试不能再调用事件接受。没有新几何时已归loss的能量不倒灌；waiting材料拒绝能量不变成ACK后的热债。

### 7.2 时间离散

源有效tick、impulse、maxDt确定初始区间；maxDt候选1秒。只有实际负载/操作变化才新增边界。整数20 tick通常至多20个源时间段，但延迟合并cut、材料端点和误差减步另计。没有变化的常功率源不强制每tick求解。

每个span内常数输入用隐式方程；事件时刻impulse用 `M*deltaZ=b*J`，不按各系数 `J/C`分配。保留同tick impulse的稳定顺序，尤其材料受端点限制时不可交换顺序。到targetTick的末端事件在旧布局执行，再做端点拓扑。

时间步候选由P2真实加热、启停、材料转换及服务器负载结果冻结；生产保留推进算法需要的误差估计/减步，不建立参考求解测试。动态材料或快模式需要的收敛处理不得仅因耗时而关闭。时间层数4只是初版起点，最终支持范围以生产场景结果决定。

### 7.3 两个明确提交点

```text
BEGIN：捕获NumericalCut身份；预留源记录/数值scratch/发布容量
RECORD：接受本batch源事件一次，冻结旧已提交几何上的负载timeline
SOLVE：推进(lastSolvedTick,targetTick]，trial air/body和receipt暂不写正式状态
NUMERICAL_COMMIT：统一写air z、body H/branch、源接受回执、solvedTick
ENDPOINT_INPUT：应用本batch ACK、phase intent、最终几何/自然/wind
PREPARE_LAYOUT：从刚完成的状态投影，准备air/body/operator/binding/query/checkpoint引用
TOPOLOGY_COMMIT：安装新布局与绑定，记迁移外部能量，解除旧引用后release
PUBLISH：同一cut发布空气/材料/布局；收集真实phase请求、residency、completion
```

世界中途几何没有消息时间线，因此本cut使用旧几何，最终几何作用于未来时间；不推测开门具体tick。ACK本cut末端处理，和legacy顺序不同，必须测试等待延迟及源实际接受量。

数值提交前失败/减步只丢弃未接受trial；提交后只重试尚未完成的拓扑/发布阶段，不能再次供能。topology暂时WORK_LIMITED时允许发布数值已推进的旧合法布局，变化区域继续按live revision标stale并保留原dirty任务。source重绑所需stencil和数组在拓扑commit前准备完成。

`restorePagePublications()`不是solver回滚；不新增整维度快照假装全process可回滚。正常资源不足必须在写入点前解决，准备好的commit仅执行有限数组/引用写入；真正内部程序异常保留现有诊断路径，不能把它当日常超额处理。

## 8. 调度与恢复：默认同步，切片按测量决定

默认保留现有 `ThermalDimensionProcessor.process(batch)`、mailbox和worker队列。完整trial、Krylov向量和源记录用于本次求解/重试；它们本身不要求持久的跨任务状态机。先完成§6等价优化并测完整cut，不预先实现 `ProcessResult/MORE_WORK`、队列轮换或新调度接口。

### 8.1 何时需要切片

P2分别测总计算量、单cut尾延迟、其他维度排队时间和save等待。如果总CPU已超预算，切片不能解决计算量问题，应先修具体热点或数值路线。若总CPU合格而单任务长占worker造成公平性/尾延迟问题，再对已定位的阶段增加有限续算；选择依据、收益和新增状态写入P2结果。

迭代次数达到初始检查点时按残差处理，不因“64次”直接重启。正常迭代下降在当前调用中继续，停滞修预条件或减步。支持场景必须测得精度和总工作均可接受；未达标的候选不启用，不能借“不改调度”引入无限重试。

### 8.2 若实施切片，必须一起完成的边界

- 保持一个维度一个在途batch，只接受一次事件；保存NumericalCut进度，最终完成才发送completion与ACK。中间结果不刷新sampleTick。
- 复用当前pool，不增加新线程池、无界数值队列或每阶段对象。具体重排机制在读取实际热点和队列负载后确定，不在本计划预定复杂队列交换算法。
- 同一次PCG的算子和预条件固定；只在合法循环边界交出owner。主线程新输入进入后续batch，不改正在解的几何。
- 必须同时处理队列满、close/shutdown、源tape寿命和主线程 `awaitCompletion()`；片间不得让保存误以为cut完成。
- 总cut耗时、排队和save等待仍单独验收；切片快不代表结果及时。只有调度版本与同步版本数值/事件结果相同且实测有收益，才采用。

### 8.3 数值和资源恢复边界

| 情况 | 实际处理 |
|---|---|
| 已采用切片后slice额度耗尽且有正常进展 | 保存进度，按已实现调度重排；不rollback、不记unaccepted、不刷新sampleTick |
| PCG达到初始迭代检查点 | 重算真实残差；下降则续算，停滞更新预条件并按当前子段重启PCG |
| 材料/浮力外迭代不稳定 | 回本子段已接受起点，减步；tape不重放，receipt只记已确认输入 |
| 同tick多个材料在端点 | 一次处理这组屏障/分支变化，再求剩余时间，避免零时间循环 |
| 新shape或投影临时资源不足 | 回收无引用模板/可误差内回收的余热模式；保留dirty/job等待可用预算 |
| 发布准备容量不足 | 在正式数值写入前解决；待发布描述只保留一份，不能复制无界快照 |
| 真正非有限状态/方程缺陷 | 记录失败的真实世界场景、操作及已有正式状态；在生产路径修正缺陷，不做自动重启循环或静默改物理参数 |

资源永远不足、形状空间表达能力不足或数值方法本身不收敛，不能靠续算修复。P2必须以支持场景排除这些情况；出现时标明候选未达标并修正具体原因。不得承诺任意复杂世界同时满足固定误差、固定内存和固定延迟。量化报告挂起时长、最大sample age和ready比例，不能只报单slice很快。

## 9. 拓扑、材料复用和热历史迁移

`TopologyUpdatePlanner`分别记录material state、air geometry/basis、operator coefficient三类dirty。正功率变化不升级为几何dirty；纯空气mode变化不把原材料放进staged/old span或phase重新注册列表。

当材料位置、BlockState/law、Page生命周期和原地址均未改变，复用LIVE body span；实际质量/状态改变仍走 `BrickMigrationKernel` 现有合同。新后端不存在空气arena span时，EMPTY material span不代表Brick没有空气。搜索并迁移 `coverageSlot/firstSlot+nodeAt/airSlotAt/resolveAirPoint` 的每个空气消费者，不能把旧slot捷径留在NEW路径；材料地址不做全局替换。

几何改变先构造有限闭包：最多八个包含该格的粗支撑，加shape反向索引命中的支撑与真实接触依赖。去重一次；不沿“受影响”递归遍历整维度热域。共享系数的Page/fragment/source/checkpoint引用全部解除后才释放。

投影在新旧共同空气体积上做容量加权最小二乘：

```text
min integral_surviving c*(Bnew*znew-ToldRelative)^2 dV
subject to 每个独立隔离空气区域的能量约束
```

不变系数固定，其贡献移到RHS。小约束系统用确定QR消除冗余约束，在约束零空间解SPD最小二乘；不为每次投影引入全局优化器，也不显式求矩阵逆。初始可变DOF候选≤256，实际依赖规模及耗时在P2确认。

被实体替换的空气列入 `externalAirEnergy`；新增空气按当前自然/合法dormant初始化，不将被移除空气能量偷偷给新墙。纯mode增删超出0.25°C时保留一个显式迁移残差shape，参与M/K及源/材料求值；它是当前静态数据，不链式引用旧snapshot。相关/重复模式去重，source关闭不能直接清零。

每Brick局部项≤32为初版容量候选，包含热源和迁移残差；不是“不论密度都保证精度”的承诺。高密度源或连续改块导致常态触顶时，应修正/否决候选，不能悄悄压低峰值。源引用为0且投影全支撑误差通过才回收mode；闭包更新与mode退役使用已有prepare/commit路径。

新增冷热接触使用连续新空间做能量约束投影，检查第一/第二个完成cut；不要求在新界面同时保存原来两个不同极限值。远离变更面的温度、材料吸热和恢复时间仍参与认证。

## 10. 查询、发布、驻留与睡眠

`QueryPublication`加入轻量空气系数双缓冲及不可变布局/shape引用，body快照沿用原字段；一个publication version覆盖两者。构建目标缓冲和checkpoint片段后再发布一次，不能先换layout再换系数。

新空气查询流程：

1. 保留double xyz，仅定位Page/Brick/单位空气component时floor。
2. 获取布局和一个ReadCursor，核对air layout epoch/相关Page依赖。
3. 固定8粗项快速求值，加当前Brick局部列表对应的静态三线性shape贡献。
4. 核对同一cursor及依赖仍有效，再返回温度/sampleTick/quality。

query不读世界、不建图、不扫描全部source、不分配临时集合。shape和layout的旧数组不在合法publication还可能引用时归还可覆盖的池；只靠Java对象仍活着不能防止池复用内容被改。重复检查是跨线程快照一致性所需，不放进每个内层乘加。

在item quarter-cache命中后仍重算精确空气；只复用原辐射等允许按quarter缓存的项。玩家、作物、机器、Town接同一evaluator；红外和材料温度计继续读body H/law。GameplayFields在现有合成位置覆盖，不再次输入物理能量。

未知/失效几何使用原Natural/dormant回退并保留原因。正常热源范围内的回退率和WARMING时间计入失败统计；不能用回退降低CPU。跨Pageshape需要完整支撑依赖，不能只检查查询所在Page。

HotMask使用已编译shape界与当前系数给出保守温度界。粗函数保持非负单位分解时可用粗值min/max，局部项加 `sum(abs(a)*sup(abs(psi)))`；如果局部秩变换改变粗基形式，改用实际变换后的函数界，不能沿用错误min/max。把变化登记到全部支撑Brick，不只owner。测试保守界过宽是否造成长期驻留增长。

睡眠沿用quiet-cut计数，但依据实际场的变化趋势、外部负载/待交付、材料waiting和布局job。必要的一秒无输入预测仅在准备进入睡眠时执行并计费。raw模态系数差不能当°C残差；未完成cut不能republish成最新tick。首版整个维度采用同一睡眠决定，不引入动态热岛。已有resident mask同生命周期不可缩小的约束保留，移动热源长运行必须测retained bytes。

## 11. 保存与恢复

沿用一个 `FrostedHeartThermal` root，将格式版本由4升级为5，统一保存新版空气和材料，不另建兼容root。材料继续保存H、branch、BlockState和时间，可复用现有材料编解码函数，但不保留v4读取入口。新空气记录按owned section包含：

```text
sectionY, savedTick, savedNatural, Tref, tuning/templateVersion
ownedBrickMask, geometry/branch稳定身份
粗空间key及值；局部模板描述/系数；复杂shape在本section所需顶点分支片段
ownedAirEnergy, 编解码温度误差界
```

不保存runtime slot/BFS临时编号。共享系数可在邻chunk各保存一份描述，H按不重叠owned空气体积积分，不能每chunk重复存整份mode能量。每个chunk可独立读取，不强加载邻chunk。恢复先得到旧局部函数，再在当前合法几何上投影、去重、接合并恢复来源绑定。

模板可重建时只保存模板ID/参数/系数；复杂shape保存必要片段。量化用 `sum(sup(abs(Bi))*error_i)`或更紧的已验证界选择精度，不统一把所有模态系数量化1/16°C；通常不能继续保证640B。超过误差预算时用更高精度；超过存储cap先尝试误差内删除冗余mode/片段，不把超误差压缩当作完整恢复。

只接受版本5。旧版本或缺失热学记录均按无可恢复热学历史处理：空气按当前自然温度初始化，材料按当前BlockState对应law在自然温度初始化H，branch从初始状态开始；当前热源走正常发现/注册。此规则只影响thermal派生状态，不删除世界方块、物品或其他游戏存档。正常保存直接写版本5，不导入旧空气或旧材料，不写旧均值预览，不支持新旧后端存档往返，也不比较两套root的时间优先级。旧热学记录不使世界加载失败。损坏的新版记录按无法恢复处理并记录诊断，不伪称保留了原温度。

独立chunk以不同tick保存/恢复会产生边界历史差异，按当前投影和首轮求解消化并统计误差，不虚构从未加载邻chunk的精确历史。dormant沿用 `DormantThermalCooling` 衰减到当前自然场；相同保存年龄片段共享衰减因子，重复序列化不重置起点。材料仍按原H/law/branch衰减，不用空气系数规则替代。

主线程保存只复制/序列化已准备的coherent片段，不运行shape PDE或大投影。稳定发布复用静态模板描述，更新系数及必要数值；不每cut重复压缩全部静态几何。IO持有独立不可变数据，不借用下轮会覆盖的缓冲。

P2通过生产发布路径统计实际待保存片段尺寸，P5接正式NBT并用实际保存/重载验证。普通场景若无法在精度和总预算内保存，修正格式/预算分配后再启用。正常保存不采用“报错然后让用户处理”的常规策略。

## 12. CPU与内存验收

### 12.1 计量口径

记录旧生产实现、首版连续生产实现和优化后连续生产实现的真实场景运行结果；不分别建立算子测试程序。前后使用同JVM17、硬件、运行配置、驻留域、真实设备操作及sample age；记录实际commit和改动状态即可，不要求路径敏感hash、复制源码重编或固定外部依赖快照。

每cut汇总primitive计数，详细profile只在诊断运行开启：

- worker：source record、shape compile、operator setup、projection、precondition setup/apply、matvec/dot、trial材料段、publish/checkpoint准备、排队时间。
- 工作量：DOF/body/eligible body、trace非零项、packed条目、matvec次数、外迭代、源分段、材料端点分段、时间拒绝/重试、continuation次数。
- 主线程：capture、查询、completion安装、phase、save/unload等待。
- 内存：live、allocated、pool retained、staged与old+new峰值、checkpoint及tape；记录稳定推进分配量。
- 结果：最大温差/峰值、材料H/相变、累计能量残差、sample age、ready/WARMING/stale/degraded比例和存档字节。

采集p50/p95/p99/max，分别测冷启动、稳定运行和改块burst。仅在真实服务器运行中采集各生产阶段耗时，不另做单solver微基准或影子双跑。

### 12.2 可直接检验的性能合同

- 稳定布局中功率数值变化不触发shape、体积M/K或几何trace重编；只有规定mixing启停变化例外。
- 单次接触应用随trace长度线性增长，无展开的m²临时边。
- 相同开放模板共享；稳定matvec/query/PCG不创建对象，整个cut如有必须的消息分配单独统计，不伪报全流程0B。
- scalar源记账不随基函数项数膨胀为多个物理source/port；同tick事件处理不重复全域source扫描。
- 消元、缓存和模板优化在同一真实场景完整运行前后比较温度曲线、相变和性能，不以纯算子等价测试验收。
- 空气变化不重新注册未变材料phase身份；旧shape/source释放不删除余热。
- 续算不重放已接受事件，不在片间刷新sampleTick；无多批数值债务队列。

优化合入原则：在固定基准上收益超过测量波动且相关正常负载无明显回退；否则保留简单实现。优化不改变P、c、k、材料G、支持几何、事件时间、最大误差或快照年龄。

### 12.3 绝对预算的冻结

当前没有目标服务器实测，不能编造毫秒通过值。P0在固定fixture结果中写明硬件/JVM、目标tick负载和以下字段，P2据此判断：`mainThreadBudgetMsPerTick`、`workerBudgetMsPerSimulatedSecond`、`workerSliceBudget`、`maxSampleAgeTicks`、`saveWaitP99Ms`、`retainedBytes`、`checkpointBytesPerSection/World`。记录的性能目标属于实施验证数据，不能变成每个玩家必须手工配置的新选项。

无目标服务器时使用本地机器给出明确的“本地基线/通过范围”，继续数值开发，但不声称服务器容量已认证。当前根128 MiB和query16 MiB是已知约束；分配给air/body/operator/tape/checkpoint的子份额在P0核算后冻结。相对旧版超过2倍需要解释，但2倍本身不等于合格/不合格，仍看绝对预算和新鲜度。

容量按实际backing而不是live个数计费。新数组增长计算旧+新峰值，池保留的数组继续占预算。R6双float模板21952 B；1024份约21.44 MiB，还不含分支、索引和保存。每component含8粗+p局部项，仅两份K及M数值：p=0为640 B，p=2为1096 B，p=32为19456 B。fragment内的模式重叠密度比“源总数”更能决定局部成本。

先采用一种扩容和保留策略，释放真正无引用的数据；不为优化GC新建通用对象池。稳定scratch可保留容量，但计retained，并检查移动/熄火后的高水位。

## 13. 实施阶段与交付

阶段门由已授权实施中的真实生产测试结果判断，无需每个阶段再向用户重复请求许可。实现直接进入生产路径，发现问题就在该路径复现修正；不先写一套独立数值原型再迁移。

| 阶段 | 具体工作与代码落点 | 退出条件 |
|---|---|---|
| P0：生产基线 | 启动真实服务器，放置/点火真实设备，记录配置、温度曲线、相变、CPU/heap/保存等待；选定生产GameTest执行入口 | 场景与操作可重跑，记录本地预算；无JUnit、无手工engine/arena夹具 |
| P1：首个生产闭环 | 实现空间基/算子/材料trial，并同时接入正式ledger、Engine、topology、publication和查询；由真实篝火/设备触发推进 | 真实世界中供热、查询、材料受热及实际相变可观察；测试不能手动调用solver或写H补齐链路 |
| P2：真实场景与性能 | 地面/墙角/门洞、多源、四类设备、启停移动、浮力和改块；在生产路径加入§6基础优化并采集待保存尺寸 | 空间表现、峰值/材料/余热、完整CPU/heap/新鲜度达标；平移差按§3检查，未证明的绝对精度不标通过 |
| P3：生命周期覆盖 | 在P1已有生产链路上覆盖重绑/移除/等待/重试/退休/close；完善body复用和容量管理；仅P2证明需要时改调度 | 通过实际操作验证不重复交付、相变等待、资源释放和恢复；如切片则在真实负载验证队列与结果 |
| P4：查询与驻留完善 | 在已工作的统一publication/查询上完成item缓存、跨Page依赖、HotMask与睡眠优化 | 跨Page连续、负坐标、精确xyz、无玩家仍供热、无正常长期stale；IR仍读材料 |
| P5：保存恢复 | 单root版本5、新版空气/材料编解码、旧记录按缺失初始化、dormant及chunk独立恢复 | 新版热历史不均温重置；材料H/branch正确；owned H不重复；压缩误差及卸载尾延迟通过 |
| P6：验证与启用准备 | 选定的生产链路GameTest、实际服务器目标负载、长运行、保存重启；只优化实际profile热点 | 全部生产验收项有PASS/FAIL/NOT_RUN；正常场景无长挂起/拒绝/低精度恢复，之后才允许默认切换 |

P1必须先打通真实生产闭环，生产接口和查询不能推迟到纯数学测试之后。分批实现可以逐步增加场景覆盖，但未接通的功能如实标未实现，不能用测试替身补成通过。P2直接优化同一份生产代码，P3补齐生命周期覆盖；调度是否改动按§8判断。只为实测需要增加只读计数/采样，不建立独立数值测试工具或可扩展诊断框架。

### 13.1 第一批具体任务

1. 核对HEAD及配置，选定真实生产GameTest和服务器场景。复用世界布置/真实点火方式，不复制直接new engine或改arena H的旧fixture。记录自然温度、天气、实际功率和设备操作。
2. 在当前生产实现测一次实际篝火加热/熄火、材料相变和服务器成本，保存公开查询的时间曲线及profile结果。
3. 实现新空间基和求解器，并同步接入生产Engine、源、拓扑、publication及查询。测试服务器用正常启动配置选择新后端；测试不创建另一个引擎实例。
4. 在实际世界放置篝火、普通地面及冰/水，通过世界tick观察温升、熄火余热、实际BlockState转换；失败沿真实调用链修复。
5. 加入真实机器各level、四格网格64摆放位置、墙/门、多源和设备移动。每个优化都在相同生产场景前后比较完整结果与成本。
6. 继续补齐生命周期、查询优化和新版存档；逐阶段追加diary，把本计划改为in-progress，最终报告真实执行和未覆盖场景。

## 14. 验证矩阵与执行方式

### 14.1 必需生产场景

| 场景 | 检查重点 |
|---|---|
| 真实空房间及单篝火房间 | 自然温度、实际供热、熄火余热；公开查询与消费者一致 |
| 真实篝火及Generator/Fountain/Radiator level范围 | 峰值、热区、材料吸热；不是120W替代场景 |
| 四格网格64摆放位置、负坐标、Page边角 | 对应位置≤1°C、公共开放面同一点连续 |
| 实际点火/熄灭/重开/拆除/移动及设备调功率 | 时间响应、旧位置余热、真实事件和实际接受量；不拼造source batch |
| 同tick真实设备操作及已存在的impulse生产入口 | 由生产事件触发顺序/重绑；没有可达生产入口的合同仅保留实现，不造独立数值用例 |
| 地面、实墙、墙角、窄门、L/U通路、竖井 | 分支隔离、真实传热距离、浮力开关对照 |
| 真实材料在单源/多源附近加热与冷却，实际冰水转换 | 正式温度和BlockState、反向转换、多个材料完成顺序、ACK等待后继续运行 |
| 4/8/16普通多源，32/128压力，重叠与分散 | mode密度、条件数、迭代、模板数、事件工作乘数 |
| 重复改块/开关门/source迁移 | 有限闭包、投影、材料span身份、mode残差回收 |
| 若实施切片：实际多维度/密集设备负载、保存与关闭 | 真实结果曲线、队列等待、完整cut年龄及close；不通过手调内部cursor制造通过 |
| chunk交替卸载、不同tick保存、restart、新版存档往返 | owned H、dormant时钟、空气与材料恢复精度 |
| 旧热学记录或无记录的chunk加载 | 按当前世界状态和自然温度初始化；不导入旧H/温度，不影响其他世界数据 |

所有场景通过实际世界操作进入生产路径；公开温度/材料查询、实际BlockState、生产只读计数与真实存档为证据。比较完整时间曲线，报告异常点坐标/tick/phase，而非仅最终均值。空间采样含实际可达空气和源面附近；亚格门板/台阶内部气体超出现有整格签名能力时单列范围，不剔除普通门洞最坏点。

### 14.2 系统负载

先测试本地可复现的共享基地/分散基地、每tick调功率、大量材料、高频改块、多维度、连续save/unload和移动源长运行；目标服务器再做100人共享/分散基地或等效真实消费负载。报告模拟负载与实际在线玩家的区别。正常负载定义与压力负载分开，不能只挑单火开放场景通过。

记录前后相同sample age；比较自然变化和相变等待的真实完成时间。sleep/warming/degraded统计与CPU一起输出。若较低CPU来自更旧快照、更多Natural回退或未接受能量，不算优化。

生产代码阶段先运行编译和diff检查（编译成功不等于生产测试通过）：

```powershell
.\gradlew.bat compileJava compileGameTestJava --offline --no-daemon --console=plain
git diff --check
```

生产测试用例放现有GameTest source set，用既有服务端按用例执行入口或经核实的选择机制运行；P0记录确切用例名和实际启动命令。现有 `runGameTestServer` 默认启用整个 `frostedresearch,frostedheart` namespace，包含非生产链路测试，不能直接无筛选运行作为本次方案。它还配置清理 `run-gametest/world`，只对开发测试目录使用。需要选择配置时只补最小入口，不建立新测试框架，不运行JUnit `test`任务。

重启/保存/长运行测试使用真实专用服务器及测试世界，重启间保留该世界，不能让GameTest清理任务先删掉待恢复存档。测试输出放构建输出目录，保留实际场景/操作、生产采样、profile和失败记录；不生成纯数学测试报告。

固定世界通过不能证明自然世界旧失败已经修好；没有执行的生产场景明确NOT_RUN。本次修订只改计划，未运行编译或服务器，不产生通过结论。

## 15. 文档与最终交付

实现各阶段同步更新实际受影响活文档：

- `docs/climate/thermal-runtime-architecture-and-optimization.md`：空气权威、求解/续算、dirty/迁移、发布、CPU/内存模型。
- `docs/climate/heat-production-and-network.md`：AIR_STENCIL、源时间段、pending/delivered、材料联合试算与ACK。
- `docs/climate/world-climate-and-temperature.md`：精确位置空气、GameplayFields合成、查询质量。
- `docs/climate/data-lifecycle-and-integration.md`：配置开关、单root新版格式、旧热学记录初始化规则和save/unload等待。
- `docs/climate/README.md`只维护入口与状态；若拆出连续空气详细文档，按系统文档规范带状态/日期/范围/代码锚点，不把实现细节堆进README。

每阶段diary记录决定、真实验证、文档影响和未完成项。最终交付可构建代码、固定fixture、参数/成本/误差数据、失败样本、新版存档保存恢复结果；P6报告逐项PASS/FAIL/NOT_RUN。不建立重复的source快照、hash清单或新工具链镜像作为通过条件。

## 16. 尚需实验确定的参数与 Outcome

只有以下数值留给P0/P2决定：目标硬件绝对预算；两模式/R6是否足够；时间步与误差控制的实际需求；局部预条件重建触发；局部模式/投影/codec合理容量。它们有明确fixture和阶段归属，不是需要先搭一套抽象框架的理由。

预期优先收益依次来自：不重编未变几何/算子、O(m)接触应用、合格材料消元、热启动/预条件减少matvec、primitive连续访问、静态描述与发布数据复用。独立热岛、AMG、更复杂矩阵内核仅在这些完成且profile仍显示必要时讨论。

当前Outcome：已完成源码驱动的实施计划；生产R4及真实服务器验收均未实施。下一步从P0实际服务器基线和P1生产闭环开始，不经过JUnit、独立数值原型或参考解测试阶段。
