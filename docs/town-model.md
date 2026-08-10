# 城镇临界自给数值模型

> 状态：模型设计草案；模拟器尚未开始实现。2026-08-10 已先修复冷住宅跳过日结算和居民食物恒为 `1` 单位的问题，本文基线已同步更新。
>
> 目标：把 FH/TWR 当前代码和数据中的城镇数值关系整理为一套可调用、可审计、可模拟的 Java 数学模型。本文是后续实现时的上下文基准。

## 1. 设计原则

### 1.1 代码是模型的起点

模型首先回答“当前代码实际算了什么”，再讨论应该怎样调数值。不能先发明一套热学或生产系统，再把当前代码勉强映射进去。

当前最重要的事实是：

- 城镇建筑使用离散内部体素的平均方块温度。
- 能量塔和加热器产生恒定值的球形热场；多个热场重叠时只取最大值。
- 当前供暖成本不取决于热场体积、表面积、环境温差或建筑保温。
- 采矿基地当前没有温度工作条件。
- 狩猎基地和住宅有温度条件，但还保留着已经过时的直接热网消费者端点。
- 城镇生产与居民结算每日一次；建筑温度不是全天平均值。

因此首版不引入热容、热惯性、传热系数、热源爬升、`U/L/C` 或“每立方米每升高一度消耗多少燃料”等当前代码不存在的量。

### 1.2 参数必须分层

所有数值分为三类：

1. **模型参数 `TownModelParameters`**：决定数学公式形状或生产率，可在模拟中覆盖。默认值来自当前 Java、`FHConfig`、FH 数据包或 TWR KubeJS。
2. **场景参数 `TownScenario`**：描述某一座玩家城镇，例如人口、岗位、建筑体素、塔和加热器位置、初始库存、每日加工能力。这些不进入 `FHConfig`。
3. **派生统计量**：由前两类计算，例如每 SWE 日产煤、每居民需要的猎人 SWE、塔的每日焦煤消耗。派生量不能与底层参数同时成为相互独立的配置，否则会产生矛盾。

不过，最核心的派生量必须成为命令行工具的一等输出和一等扫描目标。数值策划可以直接要求“把每采矿 SWE 日产煤调到 2.0”，工具再唯一地换算为需要修改的底层参数。

### 1.3 每个符号都必须有定义

本文约定：

- `i`：居民索引。
- `j`：热源索引。
- `v`：一个整数坐标的建筑内部空气体素。
- `h`：模拟中的游戏小时索引。
- `d`：模拟中的游戏日索引。
- `floor(x)`：向下取整。
- \(\operatorname{clamp}(x,a,b)=\min(\max(x,a),b)\)。
- 指示函数 \(\mathbf 1[condition]\)：条件成立时为 1，否则为 0。

任何后续实现新增数学符号时，都必须在首次出现前给出名称、单位、取值范围和数据来源。

## 2. 首版范围与有意省略

首版包含：

- 当前随机气候事件产生的逐小时气候温度。
- 能量塔、T2 热网和加热器产生的球形热场。
- 住宅、采矿基地、狩猎基地的结构、容量、温度和居民公式。
- TWR `fossil_deposits` 采矿配方；煤是唯一燃料原料。
- 煤到焦煤的抽象加工能力上限，不模拟具体焦炉。
- 当前狩猎掉落表中的原版生肉。
- 生肉到熟肉的抽象加工能力上限，不模拟具体风扇或烟熏设备。
- 显式岗位分配；保留居民属性、熟练度、健康和精神反馈。
- 确定性理论计算、固定种子模拟和蒙特卡洛统计。

首版有意省略：

- 开局脚本暴风雪。气候样本视为无限长随机气候序列中的一段。
- 地下城镇、地热梯度和高海拔降温。
- 热惯性、围护传热、换气和热源爬升。
- 泥炭、生物质、木炭、木炭坑和其他燃料线。
- 非肉类食物和复杂烹饪链。
- 具体焦炉、Create 风扇、物品管道、槽位、流体罐和副产物堵塞。
- 人口老化、难民生成和自动岗位分配。
- 热网管道路径与距离损耗。
- 自动布局优化。

矿井资源在模拟中视为无限：一个矿井区块耗尽后，玩家立即把矿井迁移到相同矿层的新区块，不产生额外时间或劳动成本。模型仍记录累计开采的矿物单位、完整耗尽的区块数和进入过的区块数，用于判断城镇对外围矿区的占用速度。

HUNT 地形资源首版也不作为瓶颈。当前默认总量约为数百万次掉落，在参考人口规模下远大于食物产能；`audit` 仍记录其当前配置，但第一阶段模拟不做耗尽与恢复。

## 3. 当前代码的真实时间顺序

### 3.1 游戏时钟

- `20 game ticks = 1 real second`
- `1000 game ticks = 1 game hour`
- `24000 game ticks = 1 game day`
- 城镇早晨结算发生在 `dayTime % 24000 == 1000` 附近，而不是一天结束时。

### 3.2 能量塔、建筑扫描与城镇结算不是同一种频率

| 系统 | 当前频率 | 当前行为 |
|---|---:|---|
| 能量塔城镇接管 | 约每 20 tick | `TeamTownData.tickSecond` 调用 `GeneratorData.townTick`，一次批量处理 20 tick 燃料 |
| 方块实体普通 tick | 每 tick | 住宅/狩猎基地只更新旧热网温度修正，并把自己登记进全局扫描队列 |
| 建筑结构与温度扫描 | 默认整个维度每 tick 执行 1 个已登记任务 | `SchedulerQueue` 轮询建筑；每栋建筑的实际扫描间隔约为“登记建筑数 / 每 tick 任务数” |
| 气候缓存 | 每秒检查，数据按小时生成 | 气候温度在一个游戏小时内保持同一小时值 |
| 城镇生产与居民结算 | 每游戏日一次 | 早晨读取建筑最近一次扫描保存的温度，随后生产、吃饭和更新居民 |

住宅和狩猎基地不会对一天内的温度积分，也不会在早晨计算 24 小时平均温度。扫描器每次扫描时对当前内部空气体素求一次空间平均，并覆盖建筑保存的温度；早晨结算读到的是最近一次扫描的快照。

因此模拟器的时间分辨率分开处理：

- 气候和风险报告：每游戏小时更新。
- 建筑实际日结算：只计算早晨时点的体素温度。
- T1 燃料：在控制状态不变时直接按每日解析公式累计。
- T2 热网：只有网络燃料与缓冲状态需要保留当前每 20 tick 的取整语义；不需要每 20 tick 重算建筑体素。

## 4. 气候、地点与方块温度

### 4.1 场景输入与默认地点

建筑内部体素直接保存三维整数坐标 `(x, y, z)`；`y` 已经是高度，场景中不再重复保存一个“建筑高度”参数。

地点相关的两个可调模型参数是：

- `dimensionTemperatureCelsius`：维度温度修正，单位 °C；默认主世界当前值 `-10`。
- `biomeTemperatureCelsius`：群系温度修正，单位 °C；默认 `minecraft:snowy_plains` 当前 FH 数据值 `0`。

首版参考城镇满足：

- 所有内部体素都在海平面 `y=63` 以上，因此气候影响系数恒为 `0.5`。
- 不计算海拔温度修正，固定为 `0°C`。
- 不考虑不同建筑跨越不同群系。

完整高度函数仍必须实现和测试，为以后导入真实城镇保留接口：

\[
\alpha(y)=
\begin{cases}
0, & y\le 0\\
0.5\dfrac{y}{63}, & 0<y\le63\\
0.5, & y>63
\end{cases}
\]

其中：

- `y`：体素的世界纵坐标，单位方块。
- \(\alpha(y)\)：气候温度作用到方块温度的无量纲比例。

### 4.2 随机气候片段

模拟不添加 `WorldClimate.addInitTempEvent` 的开局暴风雪。每个种子先生成并丢弃固定 `365` 游戏日作为 burn-in，然后取其后的连续片段进行实验。这样保留当前三条气候轨道和事件生成逻辑，又避免前 15 日特殊分支和开局剧情天气污染长期平衡。

当前长期气候默认值如下，全部进入 `ClimateParameters`，模拟时可以覆盖：

| 参数 | 默认值 | 定义 |
|---|---:|---|
| `coldEventProbability` | `0.8` | 第 15 日以后新事件为寒冷事件的概率 |
| `warmEventProbability` | `0.2` | 新事件为暖事件的概率 |
| `coldBottomCelsius` | `[-10,-20,-30,-40]` | 四档寒冷事件基础谷值 |
| `coldBottomWeights` | `[4,3,2,1]` | 四档谷值的相对权重 |
| `eventDurationDays` | `[2,7)` | 冷/暖事件持续天数范围 |
| `eventPaddingHours` | `[8,24)` | 事件开始到首个峰值的小时范围 |
| `calmDurationDays` | `[2,7)` | 普通事件后的平静期范围 |
| `coldPreludePeakCelsius` | `-5` | 寒冷事件正式降温前的基础峰值 |
| `warmPeakCelsius` | `8` | 暖事件基础峰值 |
| `temperatureNoiseStdDev` | `1` | 寒潮谷值和前导峰值使用的高斯噪声标准差 |
| `warmPeakNoiseScale` | `2` | 暖峰使用 `8 - 2·|Gaussian|` 中的系数 |
| `climateTrackCount` | `3` | 同时运行的气候事件轨道数 |

每小时从三条轨道中取最大正温度贡献和最小负温度贡献，再相加为该小时气候温度。插值继续使用当前 `InterpolationClimateEvent` 的三次 Hermite 公式，不重新设计曲线。

### 4.3 自然温度、热场与方块温度

定义：

- \(T_{climate,h}\)：第 `h` 个游戏小时的气候温度，单位 °C。
- \(T_{dimension}\)：场景的维度温度修正，默认 `-10°C`。
- \(T_{biome}\)：场景的群系温度修正，默认 `0°C`。
- \(T_{natural,v,h}\)：体素 `v` 在第 `h` 小时、不考虑人工热源时的方块温度。

首版不计海拔修正，因此：

\[
T_{natural,v,h}=T_{dimension}+T_{biome}+\alpha(y_v)T_{climate,h}
\]

对热源 `j`：

- \(c_j=(x_j,y_j,z_j)\)：球心整数坐标。
- \(r_j\)：球形热场半径，单位方块。
- \(q_j\)：球内热场值，单位 °C；球内恒定，球外为零。

体素 `v` 处的有效热场只取最大值：

\[
T_{heat,v,h}=\max_j\left(q_j\mathbf 1[\|v-c_j\|^2\le r_j^2]\right)
\]

最后复用当前 `WorldTemperature.blockWithHeat`：

\[
T_{block,v,h}=
\begin{cases}
T_{natural,v,h}, & T_{natural,v,h}>T_{heat,v,h}\\
\min(T_{natural,v,h}+2T_{heat,v,h},T_{heat,v,h}), & T_{natural,v,h}\le T_{heat,v,h}
\end{cases}
\]

结果最低为 `-273°C`。

建筑 `b` 的内部体素集合记为 \(V_b\)。建筑当次扫描温度为：

\[
T_{building,b,h}=\frac{1}{|V_b|}\sum_{v\in V_b}T_{block,v,h}
\]

这里的 \(|V_b|\) 就是扫描器记录的内部体积。它是离散空气方块数量，不是连续几何球体积。

## 5. 能量塔、燃料过程 tick 与 T2 热网

### 5.1 热场等级

取消热源爬升后，塔或加热器在有供能时立即达到目标等级。

定义：

- \(l_T\)：温度等级，无量纲。
- \(l_R\)：范围等级，无量纲。
- `generatorBaseRadiusBlocks`：塔在范围等级 1 时的半径，当前 `16`。
- `generatorAdditionalRadiusPerLevelBlocks`：塔在等级 1 以上每级增加的半径，当前 `8`。
- `radiatorBaseRadiusBlocks`：加热器在范围等级 1 时的半径，当前 `8`。
- `radiatorAdditionalRadiusPerLevelBlocks`：加热器在等级 1 以上每级增加的半径，当前 `8`。
- `temperaturePerHeatLevelCelsius`：每温度等级对应的热场值，当前 `10°C`。

\[
R_{tower}(l_R)=
\begin{cases}
\lfloor16l_R\rfloor, & l_R\le1\\
\lfloor16+8(l_R-1)\rfloor, & l_R>1
\end{cases}
\]

\[
R_{radiator}(l_R)=
\begin{cases}
\lfloor8l_R\rfloor, & l_R\le1\\
\lfloor8+8(l_R-1)\rfloor, & l_R>1
\end{cases}
\]

\[
Q_{heat}(l_T)=\lfloor10l_T\rfloor
\]

当前稳态结果：

| 热源模式 | \(l_T\) | \(l_R\) | 半径 | 球内热场值 |
|---|---:|---:|---:|---:|
| T1 普通 | 1 | 1 | 16 | 10°C |
| T1 超载 | 2 | 1 | 16 | 20°C |
| T2 普通，蒸汽等级 1 | 2 | 2 | 24 | 20°C |
| T2 超载，蒸汽等级 1 | 3 | 2 | 24 | 30°C |
| 获得温度等级 2 的加热器 | 2 | 2 | 16 | 20°C |

### 5.2 燃料时长与 `E_generator`

`GeneratorData` 不使用统一 FV，而是把每件燃料转化为一个“剩余燃料过程 tick”计数器。

定义：

- \(D_{recipe,f}\)：燃料 `f` 在 FH generator recipe 中的基础时长，单位燃料过程 tick。当前煤为 `1600`，焦煤为 `3200`。
- \(m_{base}\)：代码中硬编码的基础燃料时长倍率，当前 `0.7`。它没有进一步物理意义；数值上意味着无研究时只获得配方时长的 70%。后续命名为 `baseFuelDurationMultiplier` 并暴露。
- \(e_{generator}\)：`ResearchVariant.GENERATOR_EFFICIENCY` 的累计研究加成，无量纲。TWR 当前取值为 `0`、完成一级后 `0.1`、完成两级后 `0.2`。
- \(D_{effective,f}\)：一件燃料实际加入的燃料过程 tick。

\[
D_{effective,f}=\left\lfloor D_{recipe,f}(m_{base}+e_{generator})\right\rfloor
\]

普通运行每个游戏 tick 消耗 `baseFuelProcessTicksPerGameTick=1` 个燃料过程 tick；超载额外消耗 `overdriveExtraFuelProcessTicksPerGameTick=1` 个。因此在没有 T2 网络附加消耗时：

\[
N_{fuel/day}=\frac{24000(c_{base}+c_{overdrive})}{D_{effective,f}}
\]

其中：

- \(N_{fuel/day}\)：塔每天消耗的燃料物品期望值。
- \(c_{base}=1\)：普通运行每游戏 tick 的过程 tick 消耗。
- 普通模式 \(c_{overdrive}=0\)，超载模式 \(c_{overdrive}=1\)。

无研究、普通 T1 的理论值：

- 煤有效时长 `floor(1600×0.7)=1120`，消耗 `21.4286 coal/day`。
- 焦煤有效时长 `floor(3200×0.7)=2240`，消耗 `10.7143 coke/day`。

完成两级燃烧效率研究后，倍率为 `0.9`：

- 煤有效时长 `1440`，消耗 `16.6667 coal/day`。
- 焦煤有效时长 `2880`，消耗 `8.3333 coke/day`。

### 5.3 T2 网络参数的意义

当前代码中的网络公式需要拆成有明确意义的参数，不能继续依赖几个无名常数：

| 新参数名 | 当前值 | 代码中的意义 |
|---|---:|---|
| `heatBufferPerSteamLevel` | `25` | 每 1.0 蒸汽等级对应的目标网络热缓冲 |
| `networkHeatPerFuelBatch` | `25` | 一批网络燃料换算所生成的热量；当前恰好与上一参数共用常数 25，但逻辑意义不同 |
| `networkFuelProcessTicksPerBatch` | `8` | 生成上一批热量所需的燃料过程 tick |
| `generatorHeatResearchBonus` | `0` 或 `0.2` | `ResearchVariant.GENERATOR_HEAT` 累计研究加成 |
| `generatorEndpointMaxOutputPerTick` | `2000` | 塔热端点每 tick 最大输出 |
| `generatorEndpointCapacity` | `8000` | 当前 provider 端点内部容量 |
| `radiatorHeatIntakePerTick` | `4` | 加热器端点每 tick 最大输入，也是激活时尝试消耗量 |
| `radiatorEndpointCapacity` | `16` | 加热器端点热缓冲容量 |
| `radiatorPriority` | `100` | 热网消费者优先级；高值先获得热 |

公式中的变量定义：

- \(s\)：T2 当前蒸汽等级，代码中是 `0..1` 的浮点值。
- \(P_{target}\)：塔希望维持的热缓冲目标，单位 heat。
- \(P_{before}\)：本次补充前，塔认为仍剩余的热缓冲，单位 heat。
- \(e_{heat}\)：`generatorHeatResearchBonus`，TWR 当前为 `0` 或 `0.2`。
- \(C_{network}\)：本次补充需要额外消耗的燃料过程 tick。

\[
P_{target}=s\cdot heatBufferPerSteamLevel
\]

\[
C_{network}=\left\lfloor
\frac{\max(0,P_{target}-P_{before})}{1+e_{heat}}
\cdot
\frac{networkFuelProcessTicksPerBatch}{networkHeatPerFuelBatch}
\right\rfloor
\]

研究加成的效果是：同样的缓冲缺口需要更少燃料过程 tick。

### 5.4 当前 T2 实现必须先验证的问题

设计意图上，\(P_{before}\) 应当是热网消费者取走热量后，塔端点真实剩余的热缓冲。但当前代码存在两条状态同步路径：

- `GeneratorState.tickData` 把端点剩余热量写入 `GeneratorData.lastPower`。
- `GeneratorData.townTick` 随后又执行 `lastPower = power`，可能用上次生成值覆盖端点消费后的余量。

如果确实如此，城镇每秒接管期间的网络补充燃料可能低估，甚至在第一次填充后接近免费。首版文档不把这一点猜成既定公式。

实现顺序必须是：先写一个只包含 `GeneratorData`、provider 端点、热网和若干加热器的 Java 夹具，记录每 tick 的 `endpoint.heat`、`power`、`lastPower`、加热器取得热量和燃料过程 tick；确认实际时序后，再确定 T2 模拟器使用“忠实复现当前行为”还是修正后的目标行为。T2 蒙特卡洛在该验证完成前不进入平衡结论。

## 6. SWE：标准工人当量

### 6.1 精确定义

SWE 是 Standard Worker Equivalent，即“标准工人当量”。它不是人数，也不是居民固定属性，而是某位居民在某一职业、某一天提供的相对生产力。

一名该职业的**标准工人**定义为：

- 健康 `50`
- 精神 `50`
- 力量 `50`
- 智力 `50`
- 该职业熟练度 `0`

在当前默认线性参数下，这名居民恰好贡献 `1 SWE`。同一个居民可以在采矿职业贡献一个 SWE 值，在狩猎职业贡献另一个 SWE 值，因为两种职业的属性权重和熟练度奖励不同。

对居民 `i` 定义：

- \(H_i\)：健康，范围 `0..100`。
- \(M_i\)：精神，范围 `0..100`。
- \(S_i\)：力量，范围 `0..100`。
- \(I_i\)：智力，范围 `0..100`。
- \(P_i\)：当前职业熟练度，范围 `0..P_max`。这里的 \(P_i\) 只表示熟练度，不表示人口。
- \(P_{max}\)：该职业满熟练度的数值，当前采矿和狩猎都为 `100`。
- \(w_H,w_M,w_S,w_I\)：该职业的四项属性权重。
- \(W=w_H+w_M+w_S+w_I\)：正权重之和。
- \(A_i\)：按职业权重计算的综合属性，范围 `0..100`。

当至少一个权重为正时：

\[
A_i=\frac{w_HH_i+w_MM_i+w_SS_i+w_II_i}{W}
\]

如果所有权重都不为正，则和当前 `TownMathFunctions.weightedAttributeAverage` 一样，退化为四项属性的算术平均。

再定义：

- \(p_0\)：综合属性为 0、熟练度为 0 时的生产力。
- \(p_{100}\)：综合属性为 100、熟练度为 0 时的生产力。
- \(b_P\)：满熟练度提供的加法生产力奖励。
- \(p_{min},p_{max}\)：最终生产力上下限。

居民 `i` 的职业 SWE：

\[
SWE_i=\operatorname{clamp}\left(
p_0+(p_{100}-p_0)\frac{A_i}{100}
+b_P\frac{\operatorname{clamp}(P_i,0,P_{max})}{P_{max}},
p_{min},p_{max}
\right)
\]

建筑当天总 SWE 是所有具备工作资格且已分配到该建筑的居民 SWE 之和。

### 6.2 工作资格

首版只创建成年居民并显式定岗。当前工作资格为：

- 有住宅。
- 健康严格大于 `10`。
- 精神严格大于 `5`。
- 不是婴儿；首版不存在婴儿。

工作资格阈值也要进入居民参数，不能继续作为无名硬编码。

## 7. 采矿与煤炭

### 7.1 采矿 SWE 参数

当前默认采矿参数：

| 参数 | 当前值 | 意义 |
|---|---:|---|
| `healthWeight` | `30` | 健康在综合属性中的权重 |
| `mentalWeight` | `10` | 精神权重 |
| `strengthWeight` | `45` | 力量权重 |
| `intelligenceWeight` | `15` | 智力权重 |
| `productivityAtAttributeZero` | `0.5` | 综合属性 0 时生产力 |
| `productivityAtAttributeHundred` | `1.5` | 综合属性 100 时生产力 |
| `maximumProficiency` | `100` | 满熟练度值 |
| `bonusAtMaximumProficiency` | `0.5` | 满熟练度加法奖励 |
| `minimumResidentProductivity` | `0.5` | 单人最小采矿 SWE |
| `maximumResidentProductivity` | `2.0` | 单人最大采矿 SWE |
| `baseOutputPerStandardWorkerDay` | `3.5` | 每采矿 SWE 每日产生的全部矿物物品单位 |

这些值目前已经位于 `FHConfig.SERVER.TOWN.MINING`，模拟场景可以覆盖，但默认必须从配置快照读取，不能在模拟器里再写一份常量。

### 7.2 TWR 矿层权重

TWR `kubejs/server_scripts/src/recipes_types/frostedheart/biome_mine.js` 中 `the_winter_rescue:fossil_deposits` 当前权重为：

| 产物 | 权重 | 产物占比 |
|---|---:|---:|
| 煤 | 8 | `1/3` |
| 骨块 | 4 | `1/6` |
| 生物质 | 2 | `1/12` |
| 石头 | 10 | `5/12` |

定义：

- \(SWE_{mine,d}\)：第 `d` 日全部矿工的采矿 SWE 总和。
- \(q_{mine}\)：`baseOutputPerStandardWorkerDay`，默认 `3.5 item/SWE/day`。
- \(f_{coal}\)：全部矿物产出中煤的权重占比，默认 `1/3`。
- \(Q_{ore,d}\)：第 `d` 日全部矿物物品单位。
- \(Q_{coal,d}\)：第 `d` 日煤产量。

\[
Q_{ore,d}=q_{mine}\cdot SWE_{mine,d}
\]

\[
Q_{coal,d}=Q_{ore,d}\cdot f_{coal}
\]

因此当前标准值：

\[
Y_{coal}=3.5\times\frac13=1.1666667\ coal/SWE/day
\]

其中 \(Y_{coal}\) 称为“采矿 SWE 煤产率”，是最核心的平衡统计量之一。

### 7.3 无限迁矿与区块统计

定义：

- \(R_{chunk}\)：一个矿井区块可开采的总矿物单位，当前默认 `1000`。
- \(Q_{ore,total}\)：模拟至今累计开采的全部矿物单位。

\[
N_{chunks,exhausted}=\left\lfloor\frac{Q_{ore,total}}{R_{chunk}}\right\rfloor
\]

\[
N_{chunks,entered}=
\begin{cases}
0,&Q_{ore,total}=0\\
\left\lceil\dfrac{Q_{ore,total}}{R_{chunk}}\right\rceil,&Q_{ore,total}>0
\end{cases}
\]

区块耗尽后立刻迁移，所以不会降低当日产量，也不存在“矿井寿命导致城镇永久停摆”。

### 7.4 煤到焦煤

不模拟 IE 焦炉。`TownScenario` 只定义：

- `coalToCokeCapacityPerDay`：每天最多能把多少个煤转化为焦煤，单位 `coal/day`；`null` 表示不设上限。

每日实际加工量：

\[
Q_{coked,d}=\min(Q_{rawCoalAvailable,d},K_{coke,d})
\]

其中：

- \(Q_{rawCoalAvailable,d}\)：加工前可用原煤库存。
- \(K_{coke,d}\)：场景给出的当日加工能力。
- \(Q_{coked,d}\)：当日加工的煤数，也是新增焦煤数；首版固定 `1 coal → 1 coke`。

不读取、模拟或审计具体焦炉时间、杂酚油和机器槽位。

## 8. 狩猎、肉与食物

### 8.1 狩猎 SWE 参数

当前默认狩猎参数：

| 参数 | 当前值 | 意义 |
|---|---:|---|
| `healthWeight` | `25` | 健康权重 |
| `mentalWeight` | `20` | 精神权重 |
| `strengthWeight` | `25` | 力量权重 |
| `intelligenceWeight` | `30` | 智力权重 |
| `productivityAtAttributeZero` | `0.5` | 综合属性 0 时生产力 |
| `productivityAtAttributeHundred` | `1.5` | 综合属性 100 时生产力 |
| `maximumProficiency` | `100` | 满熟练度值 |
| `bonusAtMaximumProficiency` | `1.0` | 满熟练度加法奖励 |
| `minimumResidentProductivity` | `0.5` | 单人最小狩猎 SWE |
| `maximumResidentProductivity` | `2.5` | 单人最大狩猎 SWE |
| `expectedLootRollsPerStandardWorkerDay` | `7/6` | 每狩猎 SWE 每日的长期期望掉落次数 |
| `passiveExpectedLootRollsPerBaseDay` | `0` | 无人工作时的每日期望掉落次数 |

这些值目前已位于 `FHConfig.SERVER.TOWN.HUNTING`。

定义：

- \(SWE_{hunt,d}\)：第 `d` 日全部猎人的狩猎 SWE 总和。
- \(r_{hunt}\)：每狩猎 SWE 每日期望掉落次数，默认 `7/6 roll/SWE/day`。
- \(C_d\)：狩猎基地从前一日保留的小数掉落 carry，范围 `[0,1)`。
- \(R_d\)：第 `d` 日真正执行的整数掉落次数。

\[
R_d=\left\lfloor r_{hunt}SWE_{hunt,d}+C_d\right\rfloor
\]

\[
C_{d+1}=r_{hunt}SWE_{hunt,d}+C_d-R_d
\]

### 8.2 当前肉产率

当前 FH 狩猎掉落表每次只选择一个条目。按物品权重和数量区间计算：

\[
E[meat/roll]=\frac{19.5}{19}=1.0263158
\]

这里的 `meat` 只包括牛肉、猪肉、鸡肉和羊肉；皮革、骨头、羽毛和兔皮不是食物。

因此标准狩猎 SWE 的理论肉产率：

\[
Y_{meat}=\frac76\times\frac{19.5}{19}=1.1973684\ meat/SWE/day
\]

蒙特卡洛必须按真实掉落表抽样；`audit` 同时从掉落表解析期望值和方差，用于检查样本均值是否收敛到该理论值。

### 8.3 生肉到熟肉

不模拟 Create 风扇或其他烟熏机器。`TownScenario` 只定义：

- `rawMeatProcessingCapacityPerDay`：每天最多能把多少个生肉转化为对应熟肉，单位 `meat/day`；`null` 表示不设上限。

首版固定 `1 raw meat → 1 cooked meat`。具体肉类保持不变，以便查找各自的营养值。

居民食物资源量不再默认恒为 `1`。对没有显式 `ItemResourceAmountRecipe` 覆盖的可食用物品，定义：

- \(H_x\)：食物 `x` 的原版饥饿值，即 `FoodProperties.getNutrition()`。
- \(m_x\)：食物 `x` 的原版饱和度系数，即 `FoodProperties.getSaturationModifier()`。
- \(S_x=2H_xm_x\)：食物的名义饱和度恢复量。
- \(F_x=H_x+S_x=H_x(1+2m_x)\)：一件食物转换出的居民食物资源单位。

显式 item-resource amount 配方优先于此公式。没有正数原版食物值的居民食物 Tag 物品暂时保持历史回退值 `1`，避免零换算量导致资源扣除除零；这些非食物条目应由后续 `audit` 单独报告。

当前狩猎肉类的换算如下：

| 肉类 | 生肉 \(H,m\) | 生肉 \(F_x\) | 熟肉 \(H,m\) | 熟肉 \(F_x\) |
|---|---:|---:|---:|---:|
| 牛肉、猪肉 | `3, 0.3` | `4.8` | `8, 0.8` | `20.8` |
| 鸡肉 | `2, 0.3` | `3.2` | `6, 0.6` | `13.2` |
| 羊肉 | `2, 0.3` | `3.2` | `6, 0.8` | `15.6` |

因此烟熏同时提高食物资源量和营养质量，而不仅是营养质量。

### 8.4 每居民食物需求

定义：

- \(n_d\)：第 `d` 日存活居民数量。
- \(c_{food}\)：每居民每日所需食物资源单位，当前 `6.5 food units/resident/day`。
- \(Q_{food,required,d}=n_dc_{food}\)：当日理论食物需求。

狩猎仍然每 SWE 期望产出 `1.1973684` 件肉，但不同肉类不能再按“每件等于一个食物单位”聚合。若所有肉都保持生肉状态，则每次掉落的期望食物资源量为：

\[
E[F_{raw}/roll]
=\frac{4\times2\times4.8+3\times2\times4.8+2\times2\times3.2+1\times1.5\times3.2}{19}
=4.4631579
\]

\[
Y_{food,raw}=\frac76\times4.4631579=5.2070175\ food\ units/SWE/day
\]

若加工能力足以把当天全部肉做熟，则：

\[
E[F_{cooked}/roll]
=\frac{4\times2\times20.8+3\times2\times20.8+2\times2\times13.2+1\times1.5\times15.6}{19}
=19.3368421
\]

\[
Y_{food,cooked}=\frac76\times19.3368421=22.5596491\ food\ units/SWE/day
\]

所以每居民的狩猎劳动需求存在两个清晰边界：

\[
SWE_{raw|required\ per\ resident}=\frac{6.5}{5.2070175}=1.2483154
\]

\[
SWE_{cooked|required\ per\ resident}=\frac{6.5}{22.5596491}=0.2881250
\]

这说明肉加工吞吐不只是营养恢复参数，而是食物闭环的强杠杆。标准 `1 SWE` 猎人若只供应生肉，仍略微养不活一名居民；全部做熟时则理论上可供应约 `3.47` 名居民。蒙特卡洛需要按实际肉类分别记账，并在 `rawMeatProcessingCapacityPerDay` 限制下得到两条边界之间的真实产率。

### 8.5 居民食物等级与消耗顺序

住宅按 `RESIDENT_FOOD_LEVEL` 从 level 4 向 level 0 消耗。五级的首轮语义如下：

| 等级 | 设计语义 | 代表食物 |
|---:|---|---|
| 4 | 完整军粮或稀有强化食物 | 军用口粮、金胡萝卜、金苹果 |
| 3 | 复合餐食、高密度保存食物或特别高能熟食 | 汤、粥、兔肉煲、压缩饼干、熟鲸肉 |
| 2 | 普通安全熟食、主食和加工零食 | 熟肉、熟鱼、面包、烤马铃薯、巧克力 |
| 1 | 可直接吃但未经充分加工的基础食物 | 生肉、生鱼、根茎、浆果、含锯末的应急餐 |
| 0 | 危险食物、不可直接食用原料或城镇尚未正确建模的食物 | 生鸡肉、河豚、蜘蛛眼、面团、锯末、骨头、蛋糕物品 |

精确成员由 `town_resource_resident_food_level_0.json` 到 `level_4.json` 定义。FH 与原版的内置成员必须互斥；外部数据包仍可追加，但 `audit` 应报告跨级重复。蛋糕放在 level 0 是因为蛋糕物品本身没有 `FoodProperties`，城镇也未建模放置后的七片食用过程，暂时不应优先消耗。

等级是第一排序键，因此 level 4 始终先于 level 3，依次直到 level 0。对同一等级内的食物 `x`，定义：

- \(N_x\)：一件物品提供的 FH 营养标量。其值为所有匹配 `NutritionRecipe` 的 `getNutritionValue()/4` 之和，与住宅结算累计营养时使用的口径相同。
- \(F_x\)：一件物品换算出的居民食物资源单位；显式 `ItemResourceAmountRecipe` 优先，否则使用 \(H_x(1+2m_x)\)。
- \(q_x=N_x/F_x\)：每一个居民食物资源单位提供的营养质量。

同等级内按 \(q_x\) 从高到低消耗。这不是新增的综合评分，而是直接最大化 9.6 节中住宅实际得到的 \(N_{sum}/F_{consumed}\)。仅按单件总营养 \(N_x\) 排序会偏爱食物单位同样很高的大份食物，不能保证居民在满足同样食物需求时得到更高营养，所以不采用。无营养配方、非有限数值或 \(F_x\le 1/8192\) 时令 \(q_x=0\)。质量相同则按 `物品注册名|NBT` 升序作为稳定平局规则，消除原先 `HashMap` 迭代顺序造成的随机性。

## 9. 住宅、空间、舒适与居民状态

### 9.1 住宅空间评分

定义：

- \(A_b\)：住宅 `b` 的有效地板方块数。
- \(V_b\)：住宅内部空气体素数。
- \(h_b=V_b/A_b\)：平均内部高度。
- \(S_{space,b}\)：空间评分。

当前公式拆成三步：

\[
h_b=\frac{V_b}{A_b}
\]

\[
X_b=A_b\left(a_0+a_{log}\ln(h_b-h_0)\right)
\]

\[
S_{space,b}=1-\exp\left(-k_{space}X_b^{p_{space}}\right)
\]

当前系数：

| 参数 | 当前值 | 数学意义 |
|---|---:|---|
| `spaceBaseScore` \(a_0\) | `1.55` | 每单位地板面积的基础空间分 |
| `spaceHeightLogWeight` \(a_{log}\) | `0.6` | 额外层高对空间分的对数权重 |
| `spaceHeightOffset` \(h_0\) | `1.6` | 层高进入对数前扣除的偏移 |
| `spaceSaturationRate` \(k_{space}\) | `0.024` | 空间评分趋近 1 的速度 |
| `spaceScoreExponent` \(p_{space}\) | `1.11` | 放大大房屋空间分的指数 |

数学含义：

- 面积是近似线性基础，房屋越大，评分越容易饱和到 1。
- 层高只有对数收益，继续加高的边际收益快速下降。
- 当平均层高接近 `1.6` 时公式非常敏感；若不大于 `1.6`，对数无定义。当前扫描器通常要求至少两格可通行空气，但模型必须显式校验输入，不能让 `NaN` 静默传播。
- 最后的 `1-exp(-x)` 使评分有上限，但它与人口容量相乘后仍会奖励大面积建筑。

### 9.2 住宅容量

定义：

- \(B_b\)：住宅床位数。
- \(a_{resident}\)：每个居民所需的有效地板面积，当前 `4 block²/resident`。
- \(N_{capacity,b}\)：住宅容量。

\[
N_{space,b}=\left\lfloor\frac{S_{space,b}A_b}{a_{resident}}\right\rfloor
\]

\[
N_{capacity,b}=\min(N_{space,b},B_b)
\]

住宅还要求最小地板面积 `4`、最小内部体积 `8`。这些当前硬编码值需要提取为参数。

### 9.3 装饰评分

对住宅中第 `k` 种装饰，数量记为 \(n_k\)。当前装饰分：

\[
X_{decor}=\sum_k\left[a_{decor}\ln(n_k+n_0)+b_{decor}\right]
\]

\[
S_{decor}=\min\left(1,\frac{X_{decor}}{d_0+A_b/d_A}\right)
\]

当前系数：

| 参数 | 当前值 | 数学意义 |
|---|---:|---|
| `decorationCountOffset` \(n_0\) | `0.32` | 保证对数输入为正并调整第一件装饰价值 |
| `decorationLogWeight` \(a_{decor}\) | `1.75` | 同类装饰数量的对数收益 |
| `decorationTypeBaseScore` \(b_{decor}\) | `0.9` | 每种装饰类型的基础奖励 |
| `decorationBaseRequirement` \(d_0\) | `6` | 小房屋达到满装饰分的基础门槛 |
| `decorationAreaDivisor` \(d_A\) | `16` | 面积每增加 16 格，提高 1 点装饰需求 |

数学含义：同类装饰快速边际递减，而增加装饰种类会重复获得基础奖励，因此该公式强烈鼓励多样性；大房屋需要更多装饰，但需求只随面积缓慢增长。

### 9.4 温度评分

定义：

- \(T_b\)：住宅早晨最近一次扫描的有效温度。
- \(T_{comfort}\)：舒适温度，当前 `24°C`。
- \(d_T=|T_b-T_{comfort}|\)：偏离舒适温度的绝对值。
- \(S_T\)：温度评分。

\[
S_T=s_{floor}+\frac{1}{1+\exp(k_T(d_T-d_{mid}))}
\]

当前参数：

| 参数 | 当前值 | 数学意义 |
|---|---:|---|
| `comfortableTemperatureCelsius` \(T_{comfort}\) | `24` | 评分最高的中心温度 |
| `temperatureRatingFloor` \(s_{floor}\) | `0.017` | 极端温度下仍保留的评分底值 |
| `temperatureRatingSteepness` \(k_T\) | `0.4` | 舒适区边缘下降的陡峭程度 |
| `temperatureRatingMidpointDeltaCelsius` \(d_{mid}\) | `10` | 偏离舒适温度 10°C 时进入曲线中点 |

住宅可工作温度范围当前为 `[0,50]°C`。范围之外住宅仍被视为不可工作，因此不会分配新居民；但已有居民的每日食物和状态结算仍会执行。极端温度通过 \(S_T\) 压低健康恢复和综合舒适度，不再暂停整栋住宅。

### 9.5 综合舒适度

定义：

- \(w_T,w_S,w_D\)：温度、空间、装饰的相对权重，当前分别为 `0.4,0.3,0.3`。
- \(C_b\)：住宅综合舒适度。

\[
C_b=\frac{w_TS_T+w_SS_{space,b}+w_DS_{decor,b}}{w_T+w_S+w_D}
\]

当三个权重之和为零时，当前纯函数退化为三项评分的算术平均。

### 9.6 食物满足度与营养质量

对住宅 `b` 的一次日结算定义：

- \(n_b\)：住宅内居民数。
- \(c_{food}\)：每居民每日食物需求，当前 `6.5`。
- \(F_{required}=n_bc_{food}\)：住宅理论食物需求。
- \(F_{consumed}\)：实际从城镇库存扣除的食物单位。
- \(S_F\)：食物满足度。

\[
S_F=
\begin{cases}
1,&F_{required}=0\\
\operatorname{clamp}(F_{consumed}/F_{required},0,1),&F_{required}>0
\end{cases}
\]

再定义：

- \(N_{sum}\)：被消费食物的营养值总和；每件食物的值来自 FH `NutritionRecipe.getNutritionValue()/4`。
- \(n_{reference}\)：每食物单位达到满质量所需营养，当前 `7000`。
- \(Q_N\)：营养质量，范围 `0..1`。
- \(m_{nutrition,min}\)：零营养质量仍保留的恢复倍率，当前 `0.5`。
- \(M_N\)：最终营养恢复倍率。

\[
Q_N=
\begin{cases}
0,&F_{consumed}=0\\
\operatorname{clamp}\left(\dfrac{N_{sum}/F_{consumed}}{n_{reference}},0,1\right),&F_{consumed}>0
\end{cases}
\]

\[
M_N=m_{nutrition,min}+(1-m_{nutrition,min})Q_N
\]

熟肉相对生肉既会通过 \(F_x=H_x(1+2m_x)\) 提高食物单位产量，也可能通过营养配方提高 \(Q_N\) 和恢复速度。食物能量与营养质量仍是两条独立的轴。

### 9.7 健康和精神变化

对居民 `i`：

- \(health_i\)：结算前健康，范围 `0..100`。
- \(mental_i\)：结算前精神，范围 `0..100`。
- \(L_H\)：完全缺粮时每日健康损失，当前 `8`。
- \(L_M\)：完全缺粮时每日精神损失，当前 `5`。
- \(R_H\)：健康为 0、其他条件完美时的最大每日恢复，当前 `2`。
- \(R_M\)：精神为 0、其他条件完美时的最大每日恢复，当前 `1.5`。

\[
\Delta health_i=-L_H(1-S_F)+R_HS_FM_NS_T\left(1-\frac{health_i}{100}\right)
\]

\[
\Delta mental_i=-L_M(1-S_F)+R_MS_FM_NC_b\left(1-\frac{mental_i}{100}\right)
\]

结果限制在 `[0,100]`。

数学含义：

- 缺粮惩罚与缺口线性相关，恢复也被食物满足度线性缩放。
- 高健康/高精神居民因 `(1-current/100)` 接近零，很难继续恢复。
- 温度只直接影响健康恢复；精神恢复使用包含温度、空间和装饰的综合舒适度。
- 低营养不会加重饥饿损失，只会降低恢复。

### 9.8 早晨死亡、无家可归与冷住宅结算

当前每日顺序是先处理居民死亡，再重新分配住宅和工作，最后执行住宅结算：

- 早晨开始时没有住宅：先损失 `10` 健康。
- 健康 `<=5`：居民死亡并移除。
- 精神 `<=5`：居民离开并从城镇移除。
- 健康 `<=10` 或精神 `<=5`：不能工作。

这些阈值和无家可归伤害都需要提取为居民参数。

每日调度现在区分两个判定：

- `isBuildingWorkable()`：住宅结构有效、已初始化、未重叠、面积至少 `4`、体积至少 `8`，且温度在 `[0,50]°C`；用于分房和 UI 工作状态。
- `shouldRunDailySettlement()`：只要求上述结构条件，不检查温度；用于已有居民的食物、健康和精神日结算。

因此冷住宅不再“免费静止”。已有居民仍会消耗食物并运行连续状态公式，但冷住宅不会接收新居民。当前修复有意只剥离温度门槛；无效、重叠或过小住宅仍不参与日结算，模拟 `current` 语义时必须保持这一边界并检查是否还存在关联残留问题。

## 10. 参数目录与配置归属

### 10.1 已在 `FHConfig` 中的参数

实现时直接读取，不复制默认值：

- 住宅：每居民食物需求、营养参考、最低营养恢复倍率、缺粮健康/精神损失、最大健康/精神恢复、三项舒适权重。
- 采矿：每 SWE 总产量、工位面积、连接半径、属性生产力端点、熟练度奖励、生产力上下限和四项权重。
- 狩猎：每 SWE 掉落次数、小数 carry、工位/面积/体积/温度要求、属性生产力端点、熟练度奖励、生产力上下限和四项权重。
- 居民熟练度增长。
- 每矿井区块矿物单位和 HUNT 资源参数。

### 10.2 当前硬编码、需要提取的模型参数

下列值先进入纯 Java 参数 records；游戏侧开始调用纯函数时，再以保持当前默认值的方式加入 `FHConfig`：

- 塔和加热器的基础半径、额外等级半径、每级热场温度。
- 基础燃料时长倍率 `0.7`。
- 普通/超载每 tick 燃料过程消耗。
- T2 热缓冲、热量换算、燃料换算、provider 输出/容量。
- 加热器消耗、容量和优先级。
- 住宅最低/最高可结算温度、舒适温度、最小面积/体积、每居民有效面积。
- 空间评分的 `1.55, 0.6, 1.6, 0.024, 1.11`。
- 温度评分的 `0.017, 0.4, 10`。
- 装饰评分的 `0.32, 1.75, 0.9, 6, 16`。
- 无家可归健康损失、死亡/离开阈值、工作健康/精神阈值。
- 长期气候事件概率、寒潮档位、持续时间、平静期、峰值和噪声参数。

所有这些参数在模拟中都可覆盖。是否最终全部保留为面向服主的 `FHConfig` 项，可以在敏感度结果出来后再收窄；但纯函数中不能继续存在无法追踪的魔法数字。

### 10.3 只属于 `TownScenario` 的参数

- 模拟天数、运行次数和随机种子。
- 人口及每位居民的属性、熟练度、住宅和岗位。
- 建筑内部体素、地板面积、床位和装饰数量。
- 塔、加热器的位置、等级和控制时间表。
- 维度温度与群系温度；默认主世界 `-10°C` 和雪原 `0°C`。
- 初始原煤、焦煤、生肉、熟肉和其他库存。
- `coalToCokeCapacityPerDay`。
- `rawMeatProcessingCapacityPerDay`。
- 可选仓库容量；默认不设上限。
- 每日建筑结算顺序。参考场景固定顺序，另做顺序对照实验。

### 10.4 数值策划直接操作的核心轴

以下值虽然由底层参数推导，但必须可以被 `sweep` 直接指定：

| 核心轴 | 当前默认 | 工具如何映射到底层参数 |
|---|---:|---|
| `coalPerMiningSweDay` | `1.1666667` | 固定煤权重占比时，反解 `baseOutputPerStandardWorkerDay` |
| `meatPerHuntingSweDay` | `1.1973684` | 固定掉落表时，反解 `expectedLootRollsPerStandardWorkerDay` |
| `foodPerResidentDay` | `6.5` | 直接映射现有住宅食物需求配置 |
| `towerCoalPerActiveDay` | `21.4286` | 固定燃料 recipe 时，反解基础燃料消耗倍率或燃料时长倍率 |
| `towerCokePerActiveDay` | `10.7143` | 同上，但以焦煤 recipe 为基准 |

这些是“设计别名”，不是额外保存的独立配置。一次扫描必须说明它实际修改了哪个底层参数，防止多个入口相互矛盾。

## 11. Java 模型接口

后续实现建立不依赖 `Level`、NBT、方块实体或 Forge 注册表的纯 Java API：

- `TownModelParameters`：聚合气候、供热、居民、住宅、采矿和狩猎参数。
- `TownScenario`：描述一场实验。
- `TownState`：描述某个时间点的居民、库存、燃料过程、加工 carry 和累计开采量。
- `TownDayResult`：一天的输入、生产、消费和状态变化账本。
- `TownSimulationResult`：完整时间序列与汇总统计。
- `TownNumericalModel.simulate(parameters, scenario)`：统一模拟入口。

纯函数按领域拆分：

- `ClimateModel`
- `BlockTemperatureModel`
- `SphereHeatFieldModel`
- `GeneratorFuelModel`
- `HeatNetworkModel`
- `ResidentProductivityModel`
- `MiningDailyModel`
- `HuntingDailyModel`
- `HouseGeometryModel`
- 复用并扩展现有 `HouseDailyModel`

游戏代码逐步改为调用这些函数，而不是让模拟器复制一份公式。每次提取先写相同输入的前后对照测试，确保默认参数下行为不漂移。

## 12. 数据审计 `audit`

### 12.1 读取范围

`audit` 读取：

- FH `FHConfig` 中城镇默认参数。
- FH 煤和焦煤 generator recipes。
- FH 狩猎掉落表。
- FH 食物资源标签和营养 recipes。
- FH 群系温度数据，特别是 `minecraft:snowy_plains`。
- TWR `biome_mine.js` 中 `fossil_deposits` 权重。
- TWR `generator_efficiency_1/2.json` 和 `generator_heat_1.json` 研究加成。
- 对仍为 Java 常量的参数，读取统一的参数快照构造器并记录对应源码符号。

首版 `audit` 不读取 IE 焦炉和 Create 风扇配方，因为具体机器已被场景中的每日加工能力替代。

### 12.2 输出

`audit` 默认在终端输出适合人工阅读的表格，同时在指定报告目录生成两个 JSON：

1. `source-snapshot.json`
   - 每个输入参数的规范名称、数值、单位、来源类型和来源文件。
   - 数据文件的内容哈希，用于判断 TWR/FH 更新后场景是否基于旧快照。
   - 不包含模拟结果。
2. `audit-report.json`
   - 由快照推导出的核心系数和代数预测。
   - 数据漂移、重复定义、未暴露硬编码和已知代码漏洞。
   - 理论值与固定基准模拟的差值。

默认输出目录为 `build/reports/town-model/audit/<timestamp>/`，不污染源码目录。命令允许 `--output` 指定其他目录。

### 12.3 必须给出的理论基线

当前默认快照必须推导出：

- `1 mining SWE = 1.1666667 coal/day`
- `1 hunting SWE = 1.1973684 meat/day`
- `1 hunting SWE = 5.2070175 raw food units/day`
- `1 hunting SWE = 22.5596491 cooked food units/day`，前提是加工吞吐足够
- `1 resident = 6.5 food units/day`
- T1、无研究、普通运行、直接烧煤：`21.4286 coal/day`
- T1、无研究、普通运行、烧焦煤：`10.7143 coke/day`，即至少 `10.7143 raw coal/day`
- 维持 T1 直接烧煤所需采矿劳动：`18.3673 mining SWE`
- 维持 T1 焦煤路线所需采矿劳动：`9.1837 mining SWE`
- 仅供应生肉时维持一个居民所需劳动：`1.2483154 hunting SWE/resident`
- 全部肉做熟时维持一个居民所需劳动：`0.2881250 hunting SWE/resident`

这些理论预测必须在进行复杂场景模拟之前显示。蒙特卡洛不能用随机结果掩盖明显的代数不可行性。

## 13. 场景文件

所有可复现实验放在：

```text
Scripts/town_scenarios/
├── baseline/       # 当前默认参数与 1/8/24/48 人基准
├── experiments/    # 数值策划主动创建的参数实验
└── regression/     # 固定种子、固定预期结果的回归场景
```

模拟结果不写回该目录，而进入 `build/reports/town-model/simulations/<run-id>/`。

场景 JSON 至少包含：

```json
{
  "metadata": {
    "name": "8-resident-t1-baseline",
    "description": "当前参数下的八人 T1 城镇",
    "sourceSnapshot": "optional-audit-hash"
  },
  "simulation": {
    "days": 120,
    "seed": 1,
    "climateBurnInDays": 365
  },
  "location": {
    "dimensionTemperatureCelsius": -10.0,
    "biomeTemperatureCelsius": 0.0,
    "ignoreAltitudeTemperature": true
  },
  "residents": [],
  "buildings": [],
  "heatSources": [],
  "initialInventory": {},
  "processing": {
    "coalToCokeCapacityPerDay": null,
    "rawMeatProcessingCapacityPerDay": null
  },
  "control": {},
  "parameterOverrides": {}
}
```

`parameterOverrides` 中的每个键必须是 `TownModelParameters` 的规范路径。运行结果要同时写出覆盖前值、覆盖后值和来源，避免实验 JSON 中的数字失去语义。

## 14. 命令行工具

增加 Java `TownSimulationMain` 和 Gradle 入口 `runTownSimulation`：

- `audit --pack-root <TWR .minecraft> --output <dir>`
- `simulate --scenario <json> --runs <N> --seed-base <S>`
- `sweep --scenario <json> --parameter <name> --values <list> --runs <N>`

输出：

- JSON：完整参数、来源、运行汇总和失败事件。
- CSV：逐小时气候/建筑温度、逐日库存/生产/居民状态。
- 终端摘要：核心代数系数、成功概率、P5/P50/P95 和最先发生的瓶颈。

不生成 HTML，不依赖 Python、Pandas、SciPy 或绘图库。

## 15. 统计量及其精确定义

### 15.1 核心闭环系数

| 名称 | 定义 |
|---|---|
| 采矿 SWE 煤产率 | `累计产煤 / 累计采矿SWE日` |
| 狩猎 SWE 肉产率 | `累计获得的生肉物品 / 累计狩猎SWE日` |
| 居民食物需求 | `理论食物需求 / 存活居民日` |
| 塔每日煤耗 | `塔消耗煤 / 塔以煤为燃料的激活日` |
| 塔每日焦煤耗 | `塔消耗焦煤 / 塔以焦煤为燃料的激活日` |
| 每居民所需狩猎 SWE | `居民食物需求 / 狩猎SWE肉产率` |
| 塔所需采矿 SWE | `塔每日原煤当量需求 / 采矿SWE煤产率` |

这里的“SWE 日”是某位居民一天贡献的 SWE 与天数的乘积。例如某猎人当天贡献 `1.4 SWE`，则计为 `1.4 hunting-SWE-day`。

### 15.2 资源和加工

| 名称 | 定义 |
|---|---|
| 燃料自给率 | `累计产煤 / 累计用于直接燃烧和转化焦煤的原煤需求` |
| 食物自给率 | `累计新增可食用肉物品 / 累计居民理论食物需求` |
| 原煤库存变化率 | 最近 30 日原煤库存的日均净变化 |
| 焦煤库存变化率 | 最近 30 日焦煤库存的日均净变化 |
| 熟肉库存变化率 | 最近 30 日熟肉库存的日均净变化 |
| 食物储备日 | `当前可食用物品数 / (当前居民数 × 每居民每日需求)` |
| 供热续航小时 | 冻结所有新生产后，按当前控制运行到塔首次缺燃料的时间 |
| 煤加工利用率 | `累计煤转焦煤量 / 累计煤转焦煤能力`；能力无上限时不报告 |
| 肉加工利用率 | `累计烟熏肉量 / 累计烟熏能力`；能力无上限时不报告 |
| 累计矿物单位 | 采矿基地生产的全部矿物物品单位，包括非煤副产物 |
| 完整耗尽区块数 | `floor(累计矿物单位 / 每区块矿物单位)` |
| 进入矿井区块数 | 累计矿物大于零时 `ceil(累计矿物单位 / 每区块矿物单位)` |

### 15.3 热场和建筑

| 名称 | 定义 |
|---|---|
| 建筑体素覆盖率 | `位于至少一个有效球形热场内的建筑内部体素 / 建筑内部体素总数` |
| 热场有效利用率 | `球形热场并集内属于目标建筑内部的体素 / 球形热场并集体素数` |
| 热场重叠率 | `(各球体体素数之和 - 球体并集体素数) / 各球体体素数之和` |
| 最低室温裕量 | `模拟期最低建筑温度 - 该建筑最低工作温度` |
| 小时工作温度满足率 | 逐小时温度满足阈值的小时数占比；只作风险观察，不参与当前生产结算 |
| 早晨可工作率 | 每日结算时通过实际工作温度条件的天数占比 |
| 塔缺燃小时 | 控制要求开启、但塔因燃料不足未提供热场的游戏小时数 |
| 加热器断供时间 | 控制要求开启、但加热器无法获得所需热量的 tick 或秒数 |

### 15.4 居民压力和失败

| 名称 | 定义 |
|---|---|
| 缺粮居民日 | 每日对所有居民累加 `1 - 食物满足度` |
| 冷住宅静止居民日 | 因住宅温度非法而整日跳过食物、健康和精神结算的居民数量累计 |
| 失能工人日 | 因健康或精神不满足工作阈值而无法工作的居民数量累计 |
| 死亡/离开数 | 因健康 `<=5` 或精神 `<=5` 被移除的居民数 |
| 生存成功 | 规定模拟期内死亡和离开总数为零 |
| 阶段自给成功 | 无死亡/离开，且模拟末期原煤当量和食物库存均不低于初始值 |
| 恢复时间 | 一次库存或温度软失稳结束后，恢复到场景目标储备所需天数 |

### 15.5 蒙特卡洛统计

对布尔事件，报告：

- `事件发生次数 / 总运行数`
- 95% Wilson 二项比例区间

对连续统计量，报告：

- 均值
- 中位数 P50
- P5
- P95

每个结果保存完整种子，任何异常样本都必须可以用 `--runs 1 --seed <seed>` 重放。

理论与蒙特卡洛必须互相校验：

- 采矿在无库存上限、无限迁矿时是确定性的，样本值必须与理论值在浮点容差内完全一致。
- 狩猎样本均值与理论值的误差必须不超过由掉落表方差计算的 `3 × 标准误`；不能写死一个随样本量失真的百分比容差。
- T1 燃料消耗必须与解析公式一致。
- T2 只有在网络时序夹具通过后，才建立对应的理论预测。

## 16. 分阶段实现与验收

不能一次实现完整模拟器。每一阶段都必须形成一个可运行、可审计的小闭环，再进入下一阶段。

### 阶段 0：参数快照与纯代数审计

- 建立参数 records 和 `audit`。
- 读取 FH/TWR 数据并生成两个 JSON 报告。
- 输出 SWE、煤产率、肉件数产率、生肉/全熟食物单位产率、居民食物需求和 T1 每日燃料消耗。
- 不运行多日模拟。

验收：本文件第 12.3 节的理论基线全部精确出现，并且每个值能追溯到源码或数据文件。

### 阶段 1：单日确定性生产

- 实现 SWE、采矿、无限迁矿、抽象煤加工和 T1 燃料。
- 使用一个标准矿工和一个固定属性矿工验证公式。
- 暂不加入气候、住宅和随机狩猎。

验收：逐项资源守恒；修改 `3.5`、属性权重或煤占比后，理论与程序结果同步变化。

### 阶段 2：狩猎、食物和住宅日结算

- 加入真实掉落表随机、carry、抽象肉加工、食物和营养。
- 加入住宅容量、舒适度、健康和精神。
- 先用恒定合法温度，不加入气候。

验收：确定性住宅公式精确；狩猎长期均值满足 `3 × 标准误`；冷住宅仍消费食物并执行状态结算；生肉、熟肉和部分加工三种理论食物产率与模拟一致。

### 阶段 3：多日库存与岗位反馈

- 加入显式岗位、工作资格、熟练度和库存变化。
- 加入每日建筑结算顺序的对照实验。
- 建立 1、8、24、48 人基准场景。

验收：资源账本逐日闭合；理论上不可行的配置不会因结算顺序或初始库存被误判为长期自给。

### 阶段 4：气候和球形热场

- 加入 365 日 burn-in 后的随机气候片段。
- 加入离散球体、重叠取最大值和建筑内部体素平均。
- 每小时记录风险，但只在早晨温度上执行城镇日结算。

验收：固定种子完全复现；球体边界、海平面以上 `alpha=0.5`、默认雪原温度和建筑平均温度通过测试。

### 阶段 5：T2 网络专项

- 先实现并运行网络状态夹具，确认 `lastPower/power/endpoint heat` 的真实时序。
- 明确当前行为与目标行为是否不同。
- 仅对网络状态使用每 20 tick 步进；建筑温度仍按小时和早晨计算。

验收：T2 理论燃料预测与逐步模拟一致；若发现免费网络热等漏洞，单独列出，不能悄悄修正后仍称为“当前模型”。

### 阶段 6：蒙特卡洛和数值建议

- 对 8、24、48 人场景运行 120 日、每档 1000 个种子。
- 扫描煤/SWE、肉/SWE、肉加工吞吐、食物/居民日、塔燃料/激活日五个核心轴。
- 再扫描热场半径、温升和住宅阈值。
- 输出达到给定生存概率和阶段自给概率所需的最小调整。

验收：所有蒙特卡洛结果都附理论基线、置信区间、固定种子和实际覆盖参数；最终才提出写入 `FHConfig` 或数据包的数值建议。

## 17. 当前已经确认的关键结论

在任何完整模拟之前，当前默认值已经给出一条决定性燃料约束和一条食物加工约束：

1. T1 使用焦煤且无研究时，需要约 `9.1837 mining SWE` 才能仅维持塔的连续普通运行；8 名标准居民即使全部采矿仍不足。
2. 每名居民若只吃生肉，需要约 `1.2483 hunting SWE`；若肉全部做熟，只需要约 `0.2881 hunting SWE`。因此 `rawMeatProcessingCapacityPerDay` 会强烈决定食物闭环位置。

所以当前城镇闭环最明确的不闭合仍在采煤劳动与塔耗；食物侧不能再从“肉件数”直接断言不可行，而必须把肉类构成、原版食物值和每日加工吞吐一起纳入。后续模拟的第一职责是验证这些代数预测在库存、健康、气候和随机掉落加入后如何表现；第二职责才是寻找合理的新参数范围。
