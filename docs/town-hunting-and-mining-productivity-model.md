# 城镇狩猎与采矿生产力数学模型

本文描述当前代码中的实际结算逻辑，供数值策划、程序校验和配置调整使用。除特别说明外，所有“每天”均指一个 Minecraft 日。

对应主要实现：

- 居民生产力：[TownMathFunctions.java](../src/main/java/com/teammoeg/frostedheart/content/town/TownMathFunctions.java)
- 居民熟练度：[Resident.java](../src/main/java/com/teammoeg/frostedheart/content/town/resident/Resident.java)
- 狩猎：[HuntingBaseBuilding.java](../src/main/java/com/teammoeg/frostedheart/content/town/buildings/hunting/HuntingBaseBuilding.java)
- 采矿：[MineBaseBuilding.java](../src/main/java/com/teammoeg/frostedheart/content/town/buildings/mine/MineBaseBuilding.java)
- 服务端配置：[FHConfig.java](../src/main/java/com/teammoeg/frostedheart/infrastructure/config/FHConfig.java)

## 1. Minecraft 标准单位

| 名称 | 单位和含义 |
|---|---|
| 游戏刻 | 1 tick；正常服务器为每秒 20 tick |
| Minecraft 日 | 24,000 tick，即正常速度下约 20 分钟 |
| 城镇生产周期 | 每个 Minecraft 日调用一次 `tickMorning` |
| 方块 | 长度单位，1 block |
| 室内面积 | 扫描到的地板方块数，可理解为 block² |
| 室内容积 | 扫描到的室内方块数，可理解为 block³ |
| 区块 | 水平方向 16×16 blocks |
| 物品单位 | 城镇仓库中的 1 个物品；仓库内部允许保存小数物品量 |
| 标准工人 | 四项属性均为 50、对应职业熟练度为 0 的居民 |
| 相对生产力 \(S_i\) | 居民 \(i\) 相当于多少个标准工人；无量纲 |
| 狩猎抽取 | 对 `town/hunting` 战利品表执行一次随机抽取 |
| HUNT 单位 | 1 次实际狩猎抽取消耗 1 单位 |
| ORE 单位 | 1 单位可开采量对应 1 个采矿物品单位 |

`tickMorning` 大约发生在每天开始后的第 1,000 tick，并按队伍错开少量 tick。当前只有队伍存在在线成员，且处理的世界维度与队伍发电机维度匹配时，才会触发该队伍的每日城镇结算。

## 2. 两套系统共用的居民生产力

### 2.1 输入

对居民 \(i\)，使用四项范围为 0～100 的属性：

$$
x_{i,h}=\text{健康},\quad
x_{i,m}=\text{精神},\quad
x_{i,s}=\text{力量},\quad
x_{i,n}=\text{智力}
$$

狩猎和采矿各自拥有独立的四项权重：

$$
w_h,\ w_m,\ w_s,\ w_n
$$

每项权重只能配置为非负值。代码计算前还会把属性限制在 0～100。

### 2.2 加权属性均值

只统计权重大于 0 的属性：

$$
A_i=
\frac{
w_hx_{i,h}+w_mx_{i,m}+w_sx_{i,s}+w_nx_{i,n}
}{
w_h+w_m+w_s+w_n
}
$$

如果四项权重全部为 0，代码退化为四项属性的普通算术平均，而不是令属性效果消失：

$$
A_i=\frac{x_{i,h}+x_{i,m}+x_{i,s}+x_{i,n}}{4}
$$

### 2.3 属性生产力

设：

- \(P_0\)：加权属性为 0、熟练度为 0 时的生产力；
- \(P_{100}\)：加权属性为 100、熟练度为 0 时的生产力。

属性部分在两个端点间线性插值：

$$
S_{i,\mathrm{attr}}
=
P_0+(P_{100}-P_0)\frac{A_i}{100}
$$

### 2.4 熟练度生产力

居民对狩猎基地和采矿基地分别保存职业熟练度。居民数据本身将熟练度限制在 0～100；首次读取一个尚未记录的职业时，会按旧规则生成 0～50 的随机初始熟练度，且分布偏向较低值。

设：

- \(p_i\)：居民当前职业熟练度；
- \(p_{\max}\)：配置中达到完整熟练度加成所需的熟练度；
- \(B_{\max}\)：满熟练度提供的额外相对生产力。

则：

$$
S_{i,\mathrm{prof}}
=
B_{\max}
\frac{\operatorname{clamp}(p_i,0,p_{\max})}{p_{\max}}
$$

这里的加成是加法，不再与属性部分相乘。

### 2.5 最终相对生产力

$$
\boxed{
S_i=
\operatorname{clamp}
\left(
S_{i,\mathrm{attr}}+S_{i,\mathrm{prof}},
S_{\min},
S_{\max}
\right)
}
$$

狩猎与采矿当前使用相同默认参数，但两边配置独立：

| 参数 | 配置键 | 默认值 |
|---|---|---:|
| 四项属性权重 | `healthWeight`、`mentalWeight`、`strengthWeight`、`intelligenceWeight` | 各 1 |
| \(P_0\) | `productivityAtAttributeZero` | 0.5 |
| \(P_{100}\) | `productivityAtAttributeHundred` | 1.5 |
| \(p_{\max}\) | `maximumProficiency` | 100 |
| \(B_{\max}\) | `bonusAtMaximumProficiency` | 1.0 |
| \(S_{\min}\) | `minimumResidentProductivity` | 0.5 |
| \(S_{\max}\) | `maximumResidentProductivity` | 2.5 |

默认情况下可直接写成：

$$
\boxed{
S_i=
\operatorname{clamp}
\left(
0.5+\frac{A_i}{100}+\frac{p_i}{100},
0.5,
2.5
\right)
}
$$

典型值如下：

| 加权属性 \(A_i\) | 熟练度 \(p_i\) | \(S_i\) |
|---:|---:|---:|
| 0 | 0 | 0.5 |
| 50 | 0 | 1.0 |
| 50 | 50 | 1.5 |
| 100 | 0 | 1.5 |
| 100 | 100 | 2.5 |

当前代码没有在成功工作后自动调用熟练度增长。因此该模型会读取熟练度，但熟练度本身不会仅因每日狩猎或采矿而自动上升。

## 3. 建筑空间、岗位与居民分配

狩猎基地和采矿基地都用相同的空间评分：

$$
h=\frac{V}{F}
$$

$$
q_{\mathrm{space}}
=
1-\exp
\left[
-0.024
\left(
F\left(1.55+0.6\ln(h-1.6)\right)
\right)^{1.11}
\right]
$$

其中：

- \(F\)：地板面积；
- \(V\)：室内容积；
- \(h\)：平均室内高度；
- \(q_{\mathrm{space}}\)：空间评分。

有效地板面积与岗位上限为：

$$
F_{\mathrm{effective}}=Fq_{\mathrm{space}}
$$

$$
M=
\max
\left(
M_{\min},
\left\lfloor
\frac{F_{\mathrm{effective}}}{F_{\mathrm{perWorker}}}
\right\rfloor
\right)
$$

当前两个基地的默认值都是：

- 每个岗位需要 4 个有效地板方块；
- 合法结构至少提供 1 个岗位。

每天结算前，城镇会先给有住房且尚无工作的居民分配工作。建筑优先级决定哪个建筑先挑人，然后建筑从可用居民中选择 \(S_i\) 最高者。

采矿基地的默认分配优先级：

$$
Q_{\mathrm{mine}}
=
0.4-n+\frac{n}{M}
$$

狩猎基地还会使用建筑评分：

$$
Q_{\mathrm{hunt}}
=
0.5-n+\frac{n}{M}+R_{\mathrm{building}}
$$

默认建筑评分由空间和温度按 3:2 加权：

$$
q_{\mathrm{temp}}
=
0.017+
\frac{1}{
1+\exp\left[0.4\left(\lvert24-T_{\mathrm{effective}}\rvert-10\right)\right]
}
$$

$$
R_{\mathrm{building}}
=
\frac{3q_{\mathrm{space}}+2q_{\mathrm{temp}}}{5}
$$

其中 \(n\) 是已经分配的居民数。建筑满员时优先级为负无穷，不再参与分配。

这些评分只会间接改变“哪些居民进入建筑”；进入建筑后，产量仍只使用各居民的 \(S_i\)。

## 4. 狩猎生产模型

### 4.1 工作前置条件

狩猎基地必须同时满足：

1. 已初始化；
2. 结构有效；
3. 占用空间未与其他建筑重叠；
4. 地板面积至少为 4 block²；
5. 室内容积至少为 8 block³；
6. 有效室温不低于 0 ℃。

有效室温为：

$$
T_{\mathrm{effective}}
=
T_{\mathrm{scan}}+T_{\mathrm{heat}}
$$

默认热网成功供热时，每 tick 消耗 1 热量，并令：

$$
T_{\mathrm{heat}}
=
\max(10L_{\mathrm{heat}},24)\ \mathrm{^\circ C}
$$

热量不足时 \(T_{\mathrm{heat}}=0\)。当前扫描到的晾皮架数量和建筑评分均不直接乘入狩猎产量；建筑评分只影响居民分配顺序。

### 4.2 每日请求抽取数

设狩猎基地中的有效居民集合为 \(I\)，总劳动力为：

$$
S_{\mathrm{total}}=\sum_{i\in I}S_i
$$

设：

- \(R_{\mathrm{worker}}\)：每标准工人每天的期望抽取数，对应 `expectedLootRollsPerStandardWorkerDay`，默认 \(7/6\)；
- \(R_{\mathrm{passive}}\)：每基地每天的被动期望抽取数，对应 `passiveExpectedLootRollsPerBaseDay`，默认 0。

则当天新增的期望抽取预算为：

$$
R_{\mathrm{expected}}
=
R_{\mathrm{passive}}
+R_{\mathrm{worker}}S_{\mathrm{total}}
$$

启用默认的小数累计时，每座基地保存一个 \(0\le c<1\) 的 `lootRollCarry`：

$$
B=c_{\mathrm{old}}+R_{\mathrm{expected}}
$$

$$
N_{\mathrm{requested}}=\lfloor B\rfloor
$$

$$
c_{\mathrm{new}}=B-\lfloor B\rfloor
$$

例如一个标准猎人每天产生 \(7/6\) 抽；资源充足时，连续 6 天总计执行 7 抽，而不是每天向下取整为 1 抽。

如果关闭小数累计，则：

$$
N_{\mathrm{requested}}=\lfloor R_{\mathrm{expected}}\rfloor,\qquad c_{\mathrm{new}}=0
$$

### 4.3 HUNT 地形资源限制

1 次实际抽取消耗 1 HUNT 单位。设当前可用 HUNT 资源为 \(H\)，则：

$$
\boxed{
N_{\mathrm{actual}}
=
\min
\left(
N_{\mathrm{requested}},
\lfloor H\rfloor
\right)
}
$$

只有 \(N_{\mathrm{actual}}\) 次会执行战利品表并扣除 HUNT 资源。

小数余额在检查 HUNT 储量前已经结算。因 HUNT 不足而未执行的整数抽取不会形成欠账，也不会在以后补发。

HUNT 是城镇级全局资源，不按区块分别储存。当前默认参数为：

- 储量密度：0.1 抽取单位/block²；
- 恢复密度：0.005 抽取单位/block²/天；
- 资源模型最大半径：3,200 blocks；
- 代码以 3 近似圆周率。

因此理论总量为：

$$
H_{\max}=3\times0.1\times3200^2=3{,}072{,}000
$$

恢复发生在当天所有建筑完成工作后。恢复规模会随当前累计开采量对应的“耗竭半径”增长，并对小数恢复量进行随机取整。

### 4.4 单次抽取的物品分布

每次抽取固定从一个池中选择一个条目。Loot Luck 固定为 0，战利品组成不受居民属性或熟练度影响。

| 物品 | 权重 | 选中概率 | 选中后的数量 | 每抽期望数量 |
|---|---:|---:|---:|---:|
| 牛肉 | 4 | \(4/19\) | 1～3，均值 2 | \(8/19\) |
| 猪排 | 3 | \(3/19\) | 1～3，均值 2 | \(6/19\) |
| 鸡肉 | 2 | \(2/19\) | 1～3，均值 2 | \(4/19\) |
| 皮革 | 3 | \(3/19\) | 0～2，均值 1 | \(3/19\) |
| 骨头 | 3 | \(3/19\) | 0～2，均值 1 | \(3/19\) |
| 羽毛 | 2 | \(2/19\) | 0～2，均值 1 | \(2/19\) |
| 兔子皮 | 1 | \(1/19\) | 1 | \(1/19\) |
| 羊肉 | 1 | \(1/19\) | 1～2，均值 1.5 | \(3/38\) |

所以每抽的期望物品总数为：

$$
\mu_{\mathrm{loot}}
=
\frac{
8+6+4+3+3+2+1+1.5
}{19}
=1.5
$$

资源充足且仓库容量足够时：

$$
\boxed{
\mathbb{E}[\text{狩猎物品/天}]
=
1.5
\left(
R_{\mathrm{passive}}
+R_{\mathrm{worker}}\sum_iS_i
\right)
}
$$

默认标准猎人的长期期望产出为：

$$
1.5\times\frac{7}{6}=1.75\ \text{物品/天}
$$

### 4.5 最终入库

实际抽出的同类物品先合并，再使用 `MAXIMIZE` 模式逐类加入城镇仓库：

$$
I_{j,\mathrm{stored}}
=
\min
\left(
I_{j,\mathrm{generated}},
C_{\mathrm{remaining\ before}\ j}
\right)
$$

仓库不足时，能放下的部分入库，其余丢失。无论仓库是否装下战利品，实际执行的狩猎都已经消耗对应 HUNT 资源。

因此狩猎的完整关系是：

$$
\text{居民和建筑状态}
\rightarrow
\sum_iS_i
\rightarrow
R_{\mathrm{expected}}
\rightarrow
N_{\mathrm{requested}}
\rightarrow
N_{\mathrm{actual}}
\rightarrow
\text{随机战利品}
\rightarrow
\text{仓库实际入库}
$$

## 5. 采矿生产模型

### 5.1 工作前置条件

采矿基地本身要进入每日工作调用，必须：

1. 已初始化；
2. 结构有效；
3. 占用空间未与其他建筑重叠。

要进一步产生正产量，还必须至少链接一个当前可工作的矿场，并且至少一个相关区块仍有 ORE 储量。

采矿基地当前没有最低面积、最低体积或最低室温的额外工作门槛；面积和体积只通过岗位数间接限制工人数。

每天结算时，系统会重新链接矿场。一个结构有效的矿场会被遍历过程中第一个位于连接半径内的结构有效采矿基地认领，并从未分配集合中移除；这里不按距离选择最近基地，遍历顺序也不应作为稳定的策划规则。

默认连接半径为 1,024 blocks，判断使用三维方块坐标距离：

$$
\lVert\mathbf{x}_{\mathrm{mine}}-\mathbf{x}_{\mathrm{base}}\rVert
\le 1024
$$

### 5.2 每日请求产量

设采矿基地内居民总劳动力为：

$$
S_{\mathrm{total}}=\sum_iS_i
$$

每标准工人每天基础产量为：

$$
O_{\mathrm{worker}}=3.5\ \text{物品单位/天}
$$

对应配置键为 `baseOutputPerStandardWorkerDay`。

基地每天请求的总产量为：

$$
\boxed{
O_{\mathrm{requested}}
=
3.5\sum_iS_i
}
$$

采矿不执行随机战利品抽取，也不把结果取整。后续分配和仓库均保留小数物品量。

### 5.3 矿场权重与区块分配

每个矿场根据所在生物群系取得一组物品权重。设矿场 \(m\) 对物品 \(j\) 的权重为 \(w_{m,j}\)。

同一区块 \(c\) 中所有链接矿场的权重会相加：

$$
w_{c,j}=\sum_{m\in c}w_{m,j}
$$

区块总权重：

$$
W_c=\sum_jw_{c,j}
$$

所有有效链接矿场的总权重：

$$
W_{\mathrm{all}}=\sum_cW_c
$$

基地分配给区块 \(c\) 的请求量：

$$
O_{c,\mathrm{desired}}
=
O_{\mathrm{requested}}
\frac{W_c}{W_{\mathrm{all}}}
$$

当前内置生物群系配方为：

| 生物群系 | 物品权重 |
|---|---|
| `minecraft:plains` | 石头 1 |
| `minecraft:forest` | 圆石 1、闪长岩 1 |
| 其他无专用配方的生物群系 | 圆石 1、煤炭 1 |

需要注意：当前同一组权重同时控制“区块获得多少总产量”和“区块内部的物品组成”。例如权重总和为 2 的森林矿场，会得到权重总和为 1 的平原矿场两倍的基地请求量。把某个配方的所有权重同时扩大，不仅保持了物品比例，还会提高它相对其他矿场获得的总产量份额。

### 5.4 ORE 区块储量限制

ORE 按 Minecraft 区块分别记录。默认每个活跃采矿区块拥有：

$$
O_{\mathrm{reserve}}=1000\ \text{ORE 单位/区块}
$$

这里不会再乘以区块的 256 block² 面积。

设区块 \(c\) 已累计开采 \(E_c\)，当天真正从该区块取得：

$$
\boxed{
O_{c,\mathrm{actual}}
=
\min
\left(
O_{c,\mathrm{desired}},
\max(0,1000-E_c)
\right)
}
$$

并更新：

$$
E_c\leftarrow E_c+O_{c,\mathrm{actual}}
$$

虽然存在 `oreRecoveryPerChunkDay` 配置项，但当前区块追踪实现不会应用 ORE 恢复；默认值也是 0。因此当前矿脉储量耗尽后不会自动恢复。

### 5.5 区块内部的物品分配

对区块 \(c\) 中的物品 \(j\)：

$$
I_{c,j,\mathrm{generated}}
=
O_{c,\mathrm{actual}}
\frac{w_{c,j}}{W_c}
$$

并且：

$$
\sum_jI_{c,j,\mathrm{generated}}
=
O_{c,\mathrm{actual}}
$$

因此采矿生成量在进入仓库前是确定值，不是概率期望。

例：一个标准矿工 \(S=1\)，连接两个不同区块中的矿场：

- 平原矿场：石头权重 1；
- 森林矿场：圆石权重 1、闪长岩权重 1。

则总请求量为 3.5，两个区块的总权重分别为 1 和 2：

$$
O_{\mathrm{plains}}=3.5\times\frac13=\frac76
$$

$$
O_{\mathrm{forest}}=3.5\times\frac23=\frac73
$$

资源充足时最终生成：

- 石头 \(7/6\)；
- 圆石 \(7/6\)；
- 闪长岩 \(7/6\)；
- 合计 3.5 个物品单位。

### 5.6 最终入库

每个区块的每种物品使用 `ATTEMPT` 模式分别入库：

$$
I_{c,j,\mathrm{stored}}
=
\begin{cases}
I_{c,j,\mathrm{generated}},
&C_{\mathrm{remaining\ before}\ (c,j)}
\ge I_{c,j,\mathrm{generated}}\\
0,
&C_{\mathrm{remaining\ before}\ (c,j)}
<I_{c,j,\mathrm{generated}}
\end{cases}
$$

即仓库无法完整容纳某一批物品时，该批完全不入库，不会像狩猎一样尽量加入一部分。区块 ORE 已在入库前扣除，因此入库失败仍会消耗矿藏。

不同区块和不同物品通过哈希映射遍历，容量不足时最终保留哪一批物品不应被视为稳定的策划顺序。

采矿的完整关系是：

$$
\text{居民、基地和矿场状态}
\rightarrow
\sum_iS_i
\rightarrow
O_{\mathrm{requested}}
\rightarrow
\text{按矿场权重分配到区块}
\rightarrow
\text{区块 ORE 截断}
\rightarrow
\text{按物品权重确定生成量}
\rightarrow
\text{仓库实际入库}
$$

## 6. 默认数值的快速换算

假设建筑可工作、地形资源充足、仓库容量充足：

| 居民状态 | \(S_i\) | 狩猎长期期望物品/天 | 采矿物品/天 |
|---|---:|---:|---:|
| 属性 0，熟练度 0 | 0.5 | 0.875 | 1.75 |
| 属性 50，熟练度 0 | 1.0 | 1.75 | 3.5 |
| 属性 50，熟练度 50 | 1.5 | 2.625 | 5.25 |
| 属性 100，熟练度 0 | 1.5 | 2.625 | 5.25 |
| 属性 100，熟练度 100 | 2.5 | 4.375 | 8.75 |

多人基地先把所有居民的 \(S_i\) 相加，再使用上表对应的单位倍率：

$$
\mathbb{E}[\text{狩猎物品/天}]
=1.75\sum_iS_i
$$

$$
\text{采矿请求物品/天}
=3.5\sum_iS_i
$$

狩猎的单日实际结果仍会受到整数抽取、随机战利品、HUNT 储量和仓库容量影响；采矿则受到矿场权重、每区块 ORE 剩余量和仓库容量影响。

## 7. 当前模型中需要特别注意的实现事实

- `canResidentWork` 当前没有参与每日生产循环或新工作分配的实际筛选；生产使用建筑内保存的居民集合。
- 狩猎的晾皮架数量目前不影响产量、战利品组成或岗位数。
- 狩猎建筑评分只影响居民分配优先级，不直接乘入产量。
- 狩猎整数抽取受资源限制而未执行时不会积累欠账，只有小数部分跨天保存。
- 采矿配方权重同时影响矿场获得的总产量份额和内部物品比例。
- 采矿每区块储量当前不会恢复。
- 狩猎先确认可用 HUNT，抽取并尝试入库后再扣除 HUNT；采矿先扣除 ORE 再尝试入库。两者都不会因为仓库损失而返还地形资源。
- 当前没有自动熟练度成长，因此“达到满熟练度需要多少天”尚未进入生产模型。
