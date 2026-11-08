# 热液矿床：有尺寸下限的共同区域与基础地形

- Time: `2026-10-24 21:22:46 +08:00`
- Authors: `OpenAI Codex / GPT-6，设计草案与源码核对`
- Status: `draft`；供用户审核，未修改生产 Java、世界预设或噪声路由。
- Scope: 热液矿床的区域、实际群系归属、基础洞腔与后续细节；先验证一种矿床，不扩成通用地质模拟。
- Related: [真实客户端审核](../../TheWinterRescue/Scripts/worldgen/deposits/results/2026-10-24-visual-review/REPORT.md)、[当前实现](../docs/world-generation.md)。

> 2026-10-26 调查更新：用户要求优先验证修改原气候路径。原生对照支持先调整参数规则，暂不新增区域 BiomeSource；本文的 A 推荐不是当前已选实现。详见 [后续调查](2026-10-26_01-12-19_biome-climate-scale-native-investigation.md) 与 [实测报告](../../TheWinterRescue/Scripts/worldgen/biome_scale/results/2026-10-26-REPORT.md)。以下区域架构仍为需要严格空间约束时的备选草案，生产未迁移。

## 核心目标

每一处新规则产生的热液群系，都包含一处足够大的连通基础地貌。群系与洞腔读取同一份位置、范围和高度信息。候选位置不适合容纳最小核心时，同时放弃该处地貌和热液群系；不留下有群系名称却没有主体的小碎片。

“大”描述实际生成后的有效空间，不以配置里的包络长度代替。第一版候选目标为：主体水平跨度至少 160 格，主洞厅最宽处至少 64 格，中央净高至少 40 格。这不是要求每个断面都宽 64、高 40，更不是生成一块长方体。具体下限供用户审核，实测必须包含插值和方块采样。

允许小 feature 在岸边生成浅水、沉淀和植物；不再以永久平整、完全干燥、无植物的三格道路为目标。继续复用现有方块和正常流体/热系统。

## 玩家看到的地貌

一个连续的大洞厅区，主裂隙穿过两三处不等大的空间；池盆沿裂隙和低洼处分布，岸边有沉淀和露头。地面与洞顶有起伏，外围过渡到原有洞穴。水与矿化形成探索线索，而不是一条固定水位旁的平整步道。

此版以可探索的大洞厅与池群为方向，不把所有区域都做成深水淹没洞。含水层是否意外淹没核心，是原型必须检查的项目。

## 共同区域怎么产生

1. 根据世界种子与位置确定稀疏候选。候选出现密度与内部尺寸分开控制；改变出现频率不能把已生成矿床变小。
2. 候选直接生成一次不可变布局，给出最小连通核心、外围扩展、上下高度和包围范围。
3. 在独立的原始地形输入上检查地下空间/高度条件。禁止在密度函数内部调用当前 `getBiome/getNoiseBiome` 或依赖本函数的 `getBaseHeight`，避免循环依赖。
4. 通过候选的同一份布局同时提供群系归属与洞腔数据。拒绝候选在两边都返回“普通区域”。

网格可以继续作为低成本索引，但它是内部技术细节：大小从最大包络、偏移和跨块余量推导，不再把 512 格加四个补查点解释成群系范围。用户主要调出现密度和地貌尺寸，不同时维护网格、概率、补查次数等多套间接频率。

## 怎样保住最小尺寸，又有随机性

先构造一个已知连通、有尺寸下限的核心，边缘噪声负责向外扩展、改变分叉和轮廓。核心与额外洞腔取并集，使外围扰动不能抹掉核心。

在密度符号上，可以表达为核心洞腔与额外洞腔的 `min`，再与原始地形组合。floor/roof 等水平信息尽量按 X/Z 计算和缓存，Y 只参与简单的垂直截面组合；不在每个三维采样点重新寻找气候范围或重新规划整个矿床。

必须为密度插值、方块采样和轮廓扰动预留计算余量。目标是验证实际连通空间满足下限，不是让配置长度恰好等于 160 就宣布保证成功。含水层可能产生水或屏障，后续 carver/结构/装饰也会改变局部；这些必须进入完整生成验收。基础地貌核心保证与“最终每格永不变化”分开描述。

## 群系接入的两条路

共同区域与洞腔设计不依赖这里的选择。用户正在审核两种接入方式的差异，当前没有替用户切换。

### A. 一个通用区域群系来源包装（推荐）

使用正常 `BiomeSource` Codec 注册一个通用类型，配置中包含原来的 `minecraft:multi_noise` 作为普通区域来源，以及区域布局配置。

概念逻辑只有两层：当前 quart 位置属于矿床区域就返回该区域的群系；其余位置委托普通来源。所有同类矿床共用它，不为每种矿床增加包装类。ChunkGenerator 仍是 `minecraft:noise`。

- 不新增 Mixin，不修改原版 `MultiNoiseBiomeSource` 实现。
- 原热液三条独立气候记录从普通来源中分离，避免在共同区域之外继续产生热液碎片；其他原始规则保留。原本属于旧热液但不在新区域的位置由普通地下群系接管，这是目标变化的一部分。
- 普通区域继续使用原来的气候坐标，不需要气候压缩或标记编码。
- 与区域密度函数共用配置、种子输入、布局算法和缓存语义。不能一个入口用 `level.getSeed()`，另一个入口用不相干的常量噪声。
- 种子可通过原版 `RandomState` 访问器种子化的输入密度/NoiseHolder 提供。注册函数须正确实现 `mapAll` 和 `visitNoise`；不得为注入种子新建 Mixin。具体两入口一致性是首个原型的验证门槛。

已确认的性能接点：FH 当前只在 resolver 是 `MultiNoiseBiomeSource` 时建立 `ColumnCachingBiomeResolver`。包装后直接逐格委托会绕开已有优化，因此必须在普通 Java helper `FastNoiseEngine.populateBiomes` 中接回区域 resolver：普通部分复用 delegate 的列缓存，矿床范围覆盖相应 Y。这个适配不需要增加或修改 Mixin 文件，但本身需要测试。

兼容性检查包括当前安装的 World Preview、`/locate biome`、普通区域群系、`possibleBiomes` 和存档 Codec。不能因为是正常注册就承诺所有模组都天然兼容。

### B. 保留原版群系来源，编译气候标记

保留 `minecraft:multi_noise` 入口，通过 NoiseRouter 的气候输入与对应群系参数表表达区域标记。严格尺寸的区域函数仍需要普通 Java 密度类型注册；保留原版群系来源不等于零 Java。

原版最近邻不是布尔开关，不能随意把一个气候值设为特殊数就认为完成。可行方向是把普通气候坐标和参数表统一映射到一个受限范围，再把矿床放在分离的 depth 区间。映射必须同时处理六轴距离和 offset；区域内的 depth 标记必须唯一选中目标，区域外必须不能自然选中矿床。

- 对普通坐标做同一线性尺度变换，可在实数运算中保持普通规则之间的距离顺序，但 Minecraft 的整数气候量化会带来边界差异，不能承诺逐位不变。
- 技术协议应由数据生成器生成，普通作者仍编辑原始气候范围，不手改编码数字。
- 其他五个气候坐标保持 Y 不变，仅 depth 携带区域的垂直归属，才能继续利用现有列缓存。`BiomeColumnSolver.solveColumn` 已支持非单调的 depthPerY。
- 必须检查直接消费 NoiseRouter 气候输入的其他代码/模组，不能只看群系选择结果。
- 生成隔离的 noise_settings / 测试预设，避免将编码输入搭配旧预设的未编码参数表。

这条路避免注册新的群系来源，但增加了数据编译、量化边界和外部消费者的维护成本。它是有真实代价的备选，不能称为只改几条 JSON 的捷径。

### 对比

| 项目 | A：通用包装 | B：气候编码 |
|---|---|---|
| 表达方式 | 区域直接决定群系，外部委托原选择器 | 用气候输入和规则表间接表达区域 |
| 新 Java 职责 | 区域密度函数、通用来源包装、现有 helper 的列缓存适配 | 区域密度函数、气候编码输入 |
| 数据工具 | 输出区域与来源配置，保留原始气候规则 | 还要编译气候坐标/参数表并管理标记 |
| 原普通区域 | 可继续原坐标、原选择逻辑 | 需要检查量化及直接气候消费者 |
| 现有优化 | 需显式接回 delegate 的列缓存 | 保持五轴二维与 depth 单独变化才能沿用 |
| 兼容性 | 检查假定来源必须是 MultiNoise 的调用方 | 检查依赖原始气候数值的调用方 |
| 新 Mixin / 方块 | 不需要 | 不需要 |
| 性能结论 | 必须实测 | 必须实测 |

选择 A 的理由是规则更直接、调试路径更短，不是未经测量就声称它更快。若用户继续坚持不新增 BiomeSource，应明确接受 B 的额外编译与兼容成本。

## 后续 feature 做什么

基础洞腔在噪声阶段形成；水池、沉淀、矿石、植物等使用正常后续 feature，允许自然改变岸边。旧 `DepositWriter` 不再在所有装饰之后重挖一遍大厅，也不再把所有干燥洞壁包装成统一围岩壳。

水体先遵循正常含水层和有限初始放置。统一固定水位、主洞厅空气比例和池群深度若需要约束，使用共同区域验证/调整原版含水层路由，而不是另建持续补水或恒温系统。所有大空间是否实际变成水洞、是否形成含水层屏障，首个原型就要测清楚。

热量只由既有热系统与实际材料处理；不新增热量注入、新方块、BE 或矿床 tick。矿石总量、岩浆块/气泡柱密度与视觉配比单独验收，不通过删除原有内容来宣称优化成功。

## 数据与代码范围

- 基础噪声继续接当前 `twr_final_density` 组合入口；区域输出作为新增地貌分支。
- 先复用当前配置/不可变布局中的合适部分，新增区域密度职责；A 额外增加一个所有矿床共用的来源包装。
- 不引入新 ChunkGenerator、地区存档数据库、后台世界管理器或通用 SDF 节点系统。
- 原始群系数据必须从当前运行中的完整 1,374 条记录保留。已核对 `Scripts/worldgen/normal.json` 只有 455 条，不能直接用它覆盖运行预设。
- 首轮建立独立新世界与测试预设；生产 normal/噪声数据在用户审核及原型验收前保持当前状态。
- 普通包络/间距限制、缓存容量与种子接线是实现参数；作者主要配置最小有效尺寸、出现密度、边缘变化、池群/矿化材料。

## 先做哪个原型

1. 只接一个热液区域、群系归属和无装饰的基础洞腔，证明两个入口对同一位置/种子给出同一个区域。
2. 生成中心、边缘、拒绝位置与不同种子；检查实际核心尺寸和连通空间，以及实际群系覆盖。拒绝位置不能残留热液群系碎片。
3. 接入原版含水层和现有小 feature，重新检查空间、水体、浮空残留与自然岸边。允许涉水和沉淀覆盖，不以纯空气道路作为失败条件。
4. 对同种子完整区块做当前方案/新方案对照，检查普通区块成本与活跃区域冷/热成本、资源总量、缓存上界；实际客户端截图继续作为必要验收。

首轮不先承诺减少多少毫秒或多少行代码。当前方案已有的 600 秒检查和客户端数据是基线，不能当成新噪声区域方案已验证的证据。

## A 的具体代码接入：不新增或改动 Mixin

以下是方法级接线设计。生产类名和注册 ID 仍为拟定；独立的种子/API 验证已经运行，完整生产集成尚未实现。

### 注册与类的职责

| 位置/拟定类 | 具体职责 |
|---|---|
| `FHWorldgenTypes` | 通过 `DeferredRegister` 注册一个通用 BiomeSource Codec 和区域密度函数 Codec，并在 FHMain 绑定 mod event bus |
| `RegionContextDensity` | 委托原 depth 数值；通过 `mapAll/visitNoise` 获得已种子化的区域噪声，提供布局访问与有限缓存 |
| `RegionLayout` | 纯布局/几何规则；从相同配置、种子化输入和候选坐标生成不可变区域，含群系包络、核心与边缘 |
| `RegionFieldDensity` | 从区域布局投影 floor、roof、扩展洞腔等地形字段；二维结果使用原版缓存节点 |
| `RegionalBiomeSource` | 实现 `codec`、`collectPossibleBiomes`、`getNoiseBiome`；区域内返回注册的矿床群系，外部委托 base |
| 既有 `FastNoiseEngine.populateBiomes` | 对区域来源建立适配 resolver，普通部分保留原来的列缓存 |

注册声明的形式是：

```java
DeferredRegister<Codec<? extends BiomeSource>> SOURCES =
        DeferredRegister.create(Registries.BIOME_SOURCE, FHMain.MODID);
DeferredRegister<Codec<? extends DensityFunction>> DENSITIES =
        DeferredRegister.create(Registries.DENSITY_FUNCTION_TYPE, FHMain.MODID);
```

这些注册器在正常 mod event bus 阶段注册。不能把类型注册推迟到世界启动后，也不需要给原版注册表或构造器写注入。

### 关键种子接线

`BiomeSource.getNoiseBiome` 接收 quart 坐标和 `Climate.Sampler`，没有 ServerLevel 或世界种子参数。不能直接在这里假设可调用 `level.getSeed()`。本方案通过原版已存在的密度函数种子化路径解决：

1. `noise_router.depth` 指向 `RegionContextDensity`。
2. 该函数包含原 depth 输入、区域配置和一个专用 `NoiseHolder`。
3. `RandomState` 的正常 `mapAll` 调用会经 `visitNoise` 为 NoiseHolder 填入当前世界种子的原生噪声实例。
4. `RandomState` 建立 `Climate.Sampler` 时保留这个自定义上下文；它只展开原版 Holder/Marker 包装。
5. 群系来源通过 `sampler.depth()` 取得区域上下文；地形投影经同一个种子化流程读取区域规则。

`RegionContextDensity.compute(context)` 仍直接返回原 depth 输入值，`minValue/maxValue` 也委托原输入。它不把矿床编码成特殊气候数值，其他五个气候输入不变。`compute` 本身不逐 Y 规划矿床；布局由单独方法查询、缓存。

必须正确转发：

```java
public DensityFunction mapAll(DensityFunction.Visitor visitor) {
    return visitor.apply(new RegionContextDensity(
            input.mapAll(visitor),
            visitor.visitNoise(seedNoise),
            settings));
}
```

这是自定义 DensityFunction 的正常接口，没有读取 RandomState 私有字段或通过 Mixin 注入 seed。两个 Java 包装对象是否相同不作为正确性条件；相同配置/位置/已种子化输入必须得到相同布局。

### 群系查询

方法中的坐标先从 quart 转为 block，不能把两种单位混用。概念代码为：

```java
var context = RegionContextDensity.from(sampler.depth());
var region = context.layoutAt(QuartPos.toBlock(qx), QuartPos.toBlock(qz));
if (region != null && region.containsBiome(
        QuartPos.toBlock(qx), QuartPos.toBlock(qy), QuartPos.toBlock(qz))) {
    return regionalBiomes.get(region.kind());
}
return base.getNoiseBiome(qx, qy, qz, sampler);
```

`collectPossibleBiomes` 返回 base 的可能群系与区域群系的并集；区域类型与 Holder 的映射由同一份配置来源生成。旧热液三条独立气候记录从 base 分离，其他原始记录保留。

### 保留现有列缓存

当前 Mixin 在 resolver 是 MultiNoise 时建立列缓存，之后都会调用普通 Java helper `FastNoiseEngine.populateBiomes`。区域包装不满足这个 instanceof，因此可在 helper 开头接入：

```java
if (resolver instanceof RegionalBiomeSource regional) {
    resolver = regional.resolverForChunk(chunk, sampler);
}
```

该 resolver 对 16 个 quart X/Z 列复用区域查询结果，普通部分使用原有 `ColumnCachingBiomeResolver(base, sampler, chunk)`，按 Y 覆盖区域群系。区域外不重新构造气候数据或逐格重跑一套选择器。

功能本身在原版标准生成器上可运行；这个 helper 适配用于 FH 的已有快速填充路径。既有 Mixin 文件、注解、注入点和目标方法均无需新增或修改。其他 mod 的 blender/wrapper 及 World Preview 路径仍要在集成时验证。

### 数据接线

新世界预设仍用 `minecraft:noise`，群系来源改为拟新增的 `frostedheart:regional`，其中 base 是原有普通 multi_noise 配置。噪声设置的关键引用形如：

```json
{
  "depth": "the_winter_rescue:regions/context",
  "final_density": "the_winter_rescue:overworld/noise_router/regional_final_density"
}
```

这是拟定的 noise_router 片段，不是当前已经存在的资源。区域配置由一个作者输入生成 context、地形投影和群系映射，避免手工维持多份尺寸。上下文只对配套的区域来源和噪声设置启用；数据生成器负责输出配套预设。

原版阶段顺序仍为 BIOMES → NOISE → SURFACE/CARVERS → FEATURES：群系阶段先查询区域，噪声阶段按相同区域形成洞腔，最后由普通 feature 装饰。没有后期重写群系 palette 的步骤。

### 已完成的无 Mixin 核心验证

独立工具：`TheWinterRescue/Scripts/worldgen/deposits/proof/RegionalSeedBridgeProof.java`，运行入口 `run_seed_bridge_proof.py`。

- 直接用映射后的 Minecraft 1.20.1 类和实际包 NoiseRouter，没有 ModLauncher 或 Mixin bootstrap。
- 8 个种子（包括用户种子）、665,640 个 quart 位置，区域群系判断与地形信号逐点一致。
- 区域外委托真实的原版 MultiNoiseBiomeSource（两个测试参数条目），结果与直接调用 base 一致。
- 所有检查位置的六个 Climate.TargetPoint 坐标与未加上下文时完全相同。
- `sampler.depth()` 保留自定义上下文；群系与地形路径拿到同一个原生 seeded NormalNoise 实例。
- 自定义对象的直接 Codec 往返和原版 base 来源的 Codec 分派通过。
- Forge DeferredRegister 声明已编译；独立工具没有运行 Forge mod 注册生命周期或自定义 type ID 的全包加载。

这个验证支持“注册扩展加原版种子接口可以不靠新 Mixin 接入”的核心方案。它不等于已经完成大洞腔、含水层、列缓存适配、所有 1,374 条规则、World Preview 或完整客户端的验收。
