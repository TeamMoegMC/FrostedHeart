# 基于现有 Java 实现的城镇闭环数值模型

## 1. 目标与边界

建立一个与 FH/TWR 当前代码和数据直接耦合的 Java 数值模型。游戏逻辑与命令行模拟器调用相同的纯函数；不再维护独立的 Python 抽象模型。

首版只包含：

- 当前气候事件与逐小时气温。
- 能量塔、T2 热网和加热器产生的球形热场。
- 住宅、采矿基地、狩猎基地的现有扫描、容量、温度和居民公式。
- `fossil_deposits` 矿井：煤、骨块、生物质、石头仍按真实权重进入仓库，但只有煤可作燃料原料。
- 煤通过 IE 焦炉转化为焦煤。
- 狩猎产生原版生肉，通过 Create 风扇烟熏为熟肉。
- 居民显式分配为矿工、猎人或不工作；保留健康、精神、属性、熟练度反馈。
- 焦炉、风扇按真实配方和吞吐运行，但假设杂酚油、灰烬和机器输出得到及时清运。
- 实际住宅和狩猎基地的旧热网端点本轮不删除；模拟器按选定的目标拓扑忽略它们，并在 `audit` 中明确报告这一代码差异。
- 不模拟热惯性、建筑传热、热源爬升、泥炭、生物质、木炭坑、其他食物、人口老化、难民、自动岗位分配、管道路径损耗和布局优化。

关键认识：当前代码中供热燃料成本与被加热体积、球面面积、环境温差无关。建筑几何只影响球体覆盖和室内体素温度，不影响塔的燃料消耗。因此首版不得引入 `U/L/C`、热容或“每立方米每度燃料”等不存在于玩法代码中的参数。

## 2. 数学模型：输入、状态和公式

### 2.1 单位与时间顺序

- 距离、面积、体积：方块、方块²、方块³。
- 温度：摄氏度。
- 时间：游戏 tick、游戏小时、游戏日；`20 tick/s`、`24000 tick/day`。
- 燃料不再使用虚构的 FV，直接使用代码中的“燃料过程 tick”。
- 生产量：真实物品个数；采矿允许小数库存，机器只能提取整数物品。
- SWE：由当前居民生产力公式计算出的标准工人当量。

每个游戏小时更新气候并记录建筑温度；塔和热网按当前 tick 公式累计燃料消耗。城镇生产、吃饭、健康与熟练度仍在每日早晨结算。建筑是否工作以早晨扫描温度为准，逐小时温度只用于暴露寒潮过程，不改变当前日结算规则。

### 2.2 气候和方块温度

场景输入：

- 随机种子、模拟天数、是否包含开局暴风雪。
- 每个建筑内部体素的坐标、高度、维度温度、群系温度和海拔修正。
- 当前 `InterpolationClimateEvent` 使用的事件概率、寒潮分级、持续时间和插值参数。

对体素 \(v\) 和小时 \(h\)：

\[
\alpha(y)=
\begin{cases}
0 & y\le0\\
y/126 & 0<y\le63\\
0.5 & y>63
\end{cases}
\]

\[
N_{v,h}=T_{dimension}+T_{biome,v}+T_{altitude,v}+\alpha(y_v)T_{climate,h}
\]

所有有效球形热场只取最大值：

\[
H_{v,h}=\max_j\left(H_j\cdot \mathbf{1}\left[\|v-c_j\|^2\le r_j^2\right]\right)
\]

复用 `WorldTemperature.blockWithHeat`：

\[
T_{v,h}=
\begin{cases}
N_{v,h} & N_{v,h}>H_{v,h}\\
\min(N_{v,h}+2H_{v,h},H_{v,h}) & N_{v,h}\le H_{v,h}
\end{cases}
\]

结果下限为 `-273°C`。建筑温度是扫描器得到的全部内部空气体素的算术平均：

\[
T_{building,h}=\frac{1}{|V_b|}\sum_{v\in V_b}T_{v,h}
\]

建筑输入保存离散体素集合；JSON 场景可用长方体快捷定义，但载入后必须展开为整数体素，不能用连续体积近似。

### 2.3 塔、加热器、热网与燃料

取消爬升后，热源立即达到当前代码的稳态等级：

| 模式 | 温度等级 | 范围等级 | 当前半径 | 热场值 |
|---|---:|---:|---:|---:|
| T1 普通 | 1 | 1 | 16 | 10°C |
| T1 超载 | 2 | 1 | 16 | 20°C |
| T2 普通，蒸汽等级 1 | 2 | 2 | 24 | 20°C |
| T2 超载，蒸汽等级 1 | 3 | 2 | 24 | 30°C |
| T2 加热器，获得等级 2 热 | 2 | 2 | 16 | 20°C |

一般公式：

\[
R_{tower}(l)=
\begin{cases}
\lfloor16l\rfloor & l\le1\\
\lfloor16+8(l-1)\rfloor & l>1
\end{cases}
\]

\[
R_{radiator}(l)=
\begin{cases}
\lfloor8l\rfloor & l\le1\\
\lfloor8+8(l-1)\rfloor & l>1
\end{cases},
\qquad H(l)=\lfloor10l\rfloor
\]

燃料配方直接读取 FH 数据：

- 煤：`1600` 配方 tick。
- 焦煤：`3200` 配方 tick。
- 实际燃烧时长：

\[
D_f=\left\lfloor D_{recipe}(0.7+E_{generator})\right\rfloor
\]

普通运行每游戏 tick 消耗一个燃料过程 tick，超载额外消耗一个。T2 网络附加消耗继续复用 `GeneratorData.tickFuelProcess` 的取整方式：

\[
P_{max}=25\cdot steamLevel
\]

\[
C_{network}=
\left\lfloor
\frac{\max(0,P_{max}-P_{previous})}{1+E_{heat}}
\cdot\frac{8}{25}
\right\rfloor
\]

模拟器按当前城镇调用方式每秒以 `ticks=20` 调用一次该模型，保留现有批处理取整，而不是换成连续功率近似。热网首版只有 T2 塔提供者和加热器消费者；每台加热器需要并消耗 `4 heat/tick`，不足时该 tick 关闭。

### 2.4 采矿、焦化和煤库存

每个矿工 \(i\) 的采矿 SWE：

\[
A_i=\frac{30H_i+10M_i+45S_i+15I_i}{100}
\]

\[
SWE^{mine}_i=
clamp\left(
0.5+\frac{A_i}{100}+0.5\frac{P_i}{100},
0.5,2.0
\right)
\]

每日请求总产量：

\[
Q_{mine}=3.5\sum_i SWE^{mine}_i
\]

TWR `fossil_deposits` 权重为：

- 煤 `8/24=1/3`
- 骨块 `4/24=1/6`
- 生物质 `2/24=1/12`
- 石头 `10/24=5/12`

因此标准矿工的默认煤产量为：

\[
3.5/3=1.1666667\ coal/day
\]

产量继续按 `MineBaseBuilding` 的方法分配到有效矿井区块，并受每区块 `1000` 总矿物单位限制。矿物恢复保持当前实际行为：配置值为零，且区块追踪矿物目前不会恢复。仓库溢出时物品损失，但矿物储量仍被扣除。

焦炉状态逐 tick 保存：

- `1 coal → 1 coal_coke + 500 mB creosote`
- `1800 tick/次`
- 单炉理论上限 `24000/1800=13.3333 coke/day`
- 默认塔只烧焦煤；`audit` 另行计算直接烧煤的对照值。
- 假设杂酚油和输出持续清走，但输出所需的最低清运速率必须进入报告。

### 2.5 狩猎、烟熏和食物

猎人 SWE：

\[
A_i=\frac{25H_i+20M_i+25S_i+30I_i}{100}
\]

\[
SWE^{hunt}_i=
clamp\left(
0.5+\frac{A_i}{100}+1.0\frac{P_i}{100},
0.5,2.5
\right)
\]

每日期望掉落次数：

\[
R_{expected}=\frac{7}{6}\sum_i SWE^{hunt}_i+carry
\]

执行次数为 `floor(R_expected)`，小数保留到次日。每次执行消耗一个 HUNT 地形资源并按当前 FH 狩猎掉落表随机一次。当前掉落表的期望肉量为：

\[
E[meat/roll]=\frac{19.5}{19}=1.0263158
\]

所以标准猎人：

\[
E[meat/SWE/day]=\frac{7}{6}\frac{19.5}{19}=1.1973684
\]

所有原版生肉通过 Create 风扇烟熏为对应熟肉：

- `1 raw meat → 1 cooked meat`
- `fanProcessingTime=150`
- 当前 Create 批量公式按每 16 个物品一个处理档计算。
- 生肉和熟肉在城镇中都等于一个食物资源单位；烟熏只提高营养，不增加饱腹资源量。

### 2.6 住宅和居民结算

住宅容量继续使用当前扫描结果：

\[
space=1-\exp\left[-0.024\left(area(1.55+0.6\ln(volume/area-1.6))\right)^{1.11}\right]
\]

\[
capacity=\min\left(
\left\lfloor\frac{space\cdot area}{4}\right\rfloor,
bedCount
\right)
\]

每居民每日食物需求为 `6.5`：

\[
F=\min(consumedFood/(6.5P),1)
\]

\[
Q=clamp\left(
\frac{nutrition/consumedFood}{7000},0,1
\right),\qquad
N=0.5+0.5Q
\]

温度评分：

\[
R_T=0.017+\frac{1}{1+\exp(0.4(|24-T|-10))}
\]

健康和精神变化直接调用现有 `HouseDailyModel`：

\[
\Delta health=-8(1-F)+2FN R_T(1-health/100)
\]

\[
\Delta mental=-5(1-F)+1.5FN R_C(1-mental/100)
\]

首版保留当前结算漏洞以呈现真实后果：住宅早晨温度低于 `0°C` 或高于 `50°C` 时整栋住宅跳过食物、健康和精神结算，居民仍被视为有房。该现象单独统计为“冷住宅静止居民日”。

场景显式保存居民到住宅、矿井或狩猎基地的分配。只模拟成年人；工作资格保持当前条件：有住宅、健康 `>10`、精神 `>5`。每日生产顺序作为场景输入保存，以复现具体城镇的建筑迭代顺序；参考场景固定使用 `HOUSE → MINE → HUNT`，并额外运行一次 `HUNT → HOUSE`，量化当前同优先级 `HashMap` 顺序对缺粮结果的影响。

## 3. 参数来源与 FHConfig 接口

建立只包含数值的 Java records：

- `TownModelParameters`：气候、供热、住宅、采矿、狩猎、机器参数。
- `TownScenario`：种子、时间、体素几何、热源位置、居民、岗位、机器数量、矿井区块和初始库存。
- `TownState`：逐日居民、库存、矿物储量、HUNT 储量、燃料过程、机器进度和小数 carry。
- `TownSimulationResult`：逐小时温度、逐日资源账本、单次运行总结。
- `TownNumericalModel.simulate(parameters, scenario)`：无 `Level`、NBT、方块实体或注册表依赖的入口。

现有游戏类逐项改为调用同一批纯函数：方块温度组合、球体覆盖、塔燃料结算、居民生产力、矿井权重分配、狩猎次数、住宅日结算。重构必须保持现有玩法结果；旧建筑热网端点除外，它们继续留在游戏中但不进入模拟器。

参数分三类处理：

1. 继续从数据和配方读取，不复制进 `FHConfig`：

    - 煤/焦煤燃烧时长。
    - TWR 矿井物品权重。
    - 狩猎掉落权重和数量。
    - IE 焦炉配方。
    - Create 风扇时间和原版烟熏配方。
    - 食物资源标签和营养数据。

2. 已存在于 `FHConfig`，直接复用：

    - 采矿日产量、狩猎次数、HUNT/矿物储量。
    - 居民属性权重、熟练度增益和生产力上下限。
    - 每居民食物需求、营养参考值、健康/精神损失和恢复。
    - 狩猎建筑面积、体积、温度和工位要求。

3. 将目前关键硬编码补入 `FHConfig`，默认值保持现状：

    - `Town.Heating.generatorBaseRadiusBlocks = 16`
    - `generatorAdditionalRadiusPerLevelBlocks = 8`
    - `radiatorBaseRadiusBlocks = 8`
    - `radiatorAdditionalRadiusPerLevelBlocks = 8`
    - `temperaturePerHeatLevelCelsius = 10`
    - `baseFuelProcessTicksPerGameTick = 1`
    - `overdriveExtraFuelProcessTicksPerGameTick = 1`
    - `steamHeatCapacityPerLevel = 25`
    - `networkFuelProcessTicksPerHeatCapacity = 8`
    - `radiatorHeatConsumptionPerTick = 4`
    - `Housing.minimumWorkingTemperatureCelsius = 0`
    - `Housing.maximumWorkingTemperatureCelsius = 50`
    - `Housing.comfortableTemperatureCelsius = 24`
    - `Housing.minimumFloorAreaBlocks = 4`
    - `Housing.minimumInteriorVolumeBlocks = 8`
    - `Housing.effectiveFloorBlocksPerResident = 4`
    - `Climate.regularColdEventChance = 0.8`
    - `Climate.coldTierBottoms = [-10,-20,-30,-40]`
    - `Climate.coldTierWeights = [4,3,2,1]`
    - `Climate.eventDurationDaysMin/MaxExclusive = 2/7`
    - `Climate.calmDurationDaysMin/MaxExclusive = 2/7`
    - `Climate.paddingHoursMin/MaxExclusive = 8/24`
    - `Climate.coldPreludePeakCelsius = -5`
    - `Climate.warmPeakCelsius = 8`
    - `Climate.eventTemperatureNoiseStdDev = 1`

空间评分中的曲线系数和居民乘数不会再复制成新的上层参数；它们保留在公式中并由敏感度报告观察。诸如“每个居民需要几个猎人”“每床需要多少球体体积”“每度时燃料”等全部是派生结果，禁止进入配置。

## 4. Java 命令行工具与统计量

增加 `TownSimulationMain` 和 Gradle `runTownSimulation` 入口，使用 JSON 场景，输出 JSON 总结与 CSV 时间序列。现有 Python 模拟入口标记为废弃，不再作为数值依据。

命令：

- `audit --pack-root <TWR .minecraft>`：读取 FH 配方/掉落/营养资源、TWR `biome_mine.js`、IE jar 中的焦炉 JSON、Create `defaultconfigs/create-server.toml` 和原版烟熏资源；输出每个值的来源并在漂移时失败。
- `simulate --scenario <json> --runs N --seed-base S`：`N=1` 输出单条完整过程；`N>1` 运行蒙特卡洛。
- `sweep --scenario <json> --parameter <name> --values <list> --runs N`：只做一维参数或整数岗位分配扫描，不实现通用优化器。

每个统计量必须按以下定义输出：

| 统计量 | 定义 |
|---|---|
| 煤产率 | `开采煤总数 / 采矿SWE日` |
| 肉产率 | `获得肉总数 / 狩猎SWE日` |
| 燃料自给率 | `开采原煤 / 焦炉与塔消耗的原煤` |
| 食物自给率 | `新增可食用物品单位 / 居民理论需求单位` |
| 劳动力余量 | `可工作居民总SWE - 维持当期燃料与食物所需SWE` |
| 食物储备日 | `可食用库存 / (存活居民数 × 6.5)` |
| 供热续航小时 | 停止生产后，按当前控制策略运行至首次塔缺燃料的时间 |
| 矿井寿命 | `剩余矿物总单位 / 最近30日平均开采总单位` |
| 建筑热场覆盖率 | `被任一球体覆盖的建筑内部体素 / 建筑内部体素总数` |
| 热场有效利用率 | `球体并集内属于目标建筑内部的体素 / 球体并集体素` |
| 热场重叠率 | `(各球体体素数之和 - 球体并集体素数) / 各球体体素数之和` |
| 工作小时率 | 温度满足建筑阈值的小时数占比，仅作观察 |
| 工作早晨率 | 每日实际结算时通过工作条件的天数占比 |
| 塔缺燃小时 | 控制策略要求开启但因无燃料未工作的小时数 |
| 加热器断供 tick | 要求开启但无法从热网取出 `4 heat` 的 tick 数 |
| 缺粮居民日 | 每日所有居民的 `1-F` 之和 |
| 冷住宅静止居民日 | 因住宅温度非法而完全跳过结算的居民数之和 |
| 失能工人日 | 因健康或精神阈值无法工作的居民数之和 |
| 仓库溢出 | 未能进入仓库的实际物品单位 |
| 机器利用率 | `忙碌tick / (机器台数 × 总tick)` |
| 生存成功 | 120 日内死亡数为零 |
| 严格阶段自给 | 无死亡，且结束时食物库存与原煤当量库存均不低于开始值 |

蒙特卡洛对每个事件输出 `事件次数/N` 和 95% Wilson 区间；连续量输出均值、中位数、P5、P95。所有结果保存种子，任意失败样本都能单独重放。

`audit` 必须首先输出以下当前默认推导值：

- `1 SWE矿工 = 1.1666667 煤/日`
- `1 SWE猎人 = 1.1973684 肉/日`
- `1居民 = 6.5 食物单位/日`
- T1、无研究、常开：直接烧煤 `21.4286 coal/day`
- T1、无研究、常开：烧焦煤需 `10.7143 raw coal/day`
- 维持 T1 所需矿工：直接烧煤 `18.367 SWE`，烧焦煤 `9.184 SWE`
- 维持一名居民食物所需猎人：`5.4286 SWE`
- 生肉和熟肉均为一个食物单位；熟肉只改变营养恢复

这些结果应作为“当前闭环不可行”的基础证据，而不是由任意场景参数掩盖。

## 5. 测试、验收与文档

自动测试包括：

- `WorldTemperature` 分段公式、绝对零度、球体边界和重叠取最大值。
- 建筑体素平均温度、面积、体积、住宅容量。
- T1/T2/超载稳态半径和温度；明确验证没有爬升和热惯性。
- 煤与焦煤时长、研究效率、燃料取整和 T2 网络附加消耗。
- 加热器 `4 heat/tick`、断供关闭和热网无距离损耗。
- 标准矿工/猎人 SWE、TWR `8/4/2/10` 分配、矿物耗尽和仓库溢出。
- 狩猎掉落期望、固定种子复现、HUNT 消耗和小数 carry。
- 焦炉 `1:1/1800/500mB`、风扇批处理和生熟肉营养差异。
- 住宅缺粮公式、健康/精神反馈、工作资格和“冷住宅免费静止”回归测试。
- 不同每日建筑顺序的结果差异。
- 游戏侧调用与纯函数在相同输入上的逐项一致性。
- `audit` 对 FH/TWR 数据漂移的失败测试，以及 CLI JSON/CSV 稳定性。

验收运行固定为 8、24、48 人，120 日，每档 1000 个种子；场景显式列出矿工/猎人数量、住宅几何、热源位置、焦炉/风扇台数、矿井区块数和初始库存。首轮不要求调到某个成功概率，而是必须可靠回答：

1. 当前参数为何无法闭环。
2. 燃料、食物、热场覆盖、机器吞吐、矿井寿命分别在哪个环节成为瓶颈。
3. 把某个 `FHConfig` 参数调到什么范围后，8/24/48 人分别达到指定生存率或阶段自给率。
4. 哪些概率变化来自气候，哪些只是狩猎掉落、取整、建筑顺序或库存容量。
5. 当前旧建筑热网端点如果继续存在，会使实际游戏与目标模型产生什么明确差异。

最后重写 `docs/TWR城镇数值模型设计.md`：只记录上述源码公式、TWR 数据来源、配置映射、审计结果和扫描结果；移除 RC 热学、虚构燃料值和未经代码支持的流程。完成实现与验证后按仓库规范新增 diary 条目。
