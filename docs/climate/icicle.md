# 冰锥

- Status: `Partial`（2026-10-20 新增融化滴水及冰屑调整后 130 项回归通过；客户端观感、整合包新洞穴和完整游戏性能待试玩）
- Last verified: `2026-10-20`
- Scope: `frostedheart:icicle`、同名 feature、TWR `cave/icicle`、随机生长、材料相变与坠落
- Primary code anchors: `IcicleBlock`、`IcicleEvents`、`IcicleFalling`、`IcicleFeature`、`FallingBlockEntityMixin_Icicle`、`MinecraftPhaseController.meltIcicle`、`MaterialChanges.GAMEPLAY_TRANSITION`、`IcicleGameTests`、`IcicleThermalGameTests`

源码和数据为最终权威。设计演变见[讨论](../../discussion/2026-10-17_23-27-52_ice-caves-and-thin-ice.md)；本文解释向下悬挂的冰锥，房檐单格小锥见[冰溜子](eave-icicles.md)。数值是源码默认，尚无玩家平衡反馈。

## 方块与连接

`frostedheart:icicle` 使用 `cracked=false/true`、`shape=0..3` 和仅用于相变收据的 `melt_cycle=false/true`。外观分别为尖端 `icicle_tip`、倒数第二段 `icicle_tail`、中段 `icicle_middle_1/2`、多段链根部 `icicle_top`；中段两种外观随机选用。五套既有交叉模型使用 cutout 渲染。连接变化自动更新形态，一格长的根部使用尖端外观。

根部需要任意方块的完整底面，下段需要上方另一段。挖掘完成后移除该段，上段保留，下段下一游戏刻坠落；移除顶部支撑则全部坠落。左键起手与普通投射物没有额外断裂回调。爆炸直接破坏沿用普通方块规则；活塞推动时破坏。静止冰锥不增加刺伤。

末端 `shape=0` 的选中/碰撞形状为局部坐标 `(5,8,5)..(11,16,11)`（每格 16 单位）：高度半格、宽深各 6/16 格，贴合上半格的尖端贴图，避免下方透明部分仍可选中。其余段保持原有形状。

自然链上限为八格；手动接长不设八格限制。断裂与末端查找按连续链真实长度处理，上限自然受维度高度约束，不截断长链。未知未加载支撑不作为空气。

## 生长与生成

自然生长仅限 `the_winter_rescue:ice_caves`，不判断区块年龄，不检查供水、湿度或每日世界湿度。手动放置可在其他群系。

材料温度 T（°C）读取 `MinecraftPhaseController.temperature`：优先活动/休眠材料，确实没有材料记录时沿用 FH 环境回退；未发布/不匹配状态等待。T>0 或非有限值不生长，其他条件满足时：

```text
m_cold(T) = min(2, 1 - 0.1*T), T≤0
p_extension = 0.05*m_cold(T)
p_root = 0.005*m_cold(T)
```

温差按连续值加算，0/-5/≤-10°C 的延长概率为每次随机刻 5%/7.5%/10%；根部为 0.5%/0.75%/1%。两者分别先抽最大可能概率 10%/1%，通过空间/群系等便宜条件后读取材料温度、补抽实际比例；热裂的 1/8 抽样不再乘到生长上。

延长读取现尖端材料，要求下方空气、连续链少于八格且没有已预警段。长根读取基底冰块材料：`minecraft:ice` tag 中除 `ThinIceBlock`（含分层薄冰）以外的冰、完整底面、下面空气。`BlockStateBaseMixin_RandomTick` 与共享随机位置抽样补齐浮冰/蓝冰等非 vanilla 随机刻基底。新根水平方向 ±2 格、竖直方向 ±1 格内不得已有根部；邻区块未知则推迟。这些根部密度值为实施时选择的可调初值。

默认 20 TPS、randomTickSpeed=3、持续处于随机刻范围且空间足够时，延长一格平均约 22.8/15.2/11.4 分钟。不是每游戏 tick 5%；八格从已有一格延长需七次成功，0°C 理想均值约 2.65 小时。

新地形使用注册 feature `frostedheart:icicle`（空 config）。TWR 的 `kubejs/data/the_winter_rescue/worldgen/placed_feature/cave/icicle.json` 保留原放置器、改为该 feature；扫描位置若为实心洞顶，则在下方空气放置。长度为 `clamp(round(2+N(0,1)*3.75),1,8)`，遇障碍缩短、检查邻近根部并设置连接形态。初始地形装饰不取决于实时材料温度。旧普通冰柱不追溯替换，旧洞穴靠运行时生长补充。

## 热裂和预警

热裂读取冰锥位置的局部环境：`gameplayPassiveEnvironment(naturalAir)`，包含可用局部供热，去掉玩家测温噪声；不使用材料本体温度。

完整冰锥每次随机刻先通过概率 1/8，才读取热裂环境温度 T（°C）。T≥5°C 时进行第二次抽样：

```text
p_fracture = min(1, (1/8) * (1+(T-5)/5) * (1+0.1*min(n,5)) * r)
r = 2（上方不是冰锥），否则 1
```

n 是下方连续冰锥段数，只数到五段。温度增加 5°C 使门槛概率翻倍；温度更高继续线性增加并封顶。材料相变抽样仍独立运行，不将热裂首层抽样用于限制必要的材料相变。

进入预警时 `cracked=true`，播放 `FHSoundEvents.ICE_CRACKING`、音量 0.9、音调 1.0，与薄冰最后一级相同；客户端 animateTick 持续生成 `FALLING_DUST` 冰屑。2026-10-20 起冰屑从本段底部的外圈（x/z 各取 0.06 或 0.94）落下，绕开下方冰锥本体，仍使用普通粒子的地形碰撞；不把预警表现迁到整根末端。均匀抽取 80..640 game ticks（20 TPS 下 4–32 秒），到期本段变空气、下段坠落；降温不撤销。热裂/振动预警不再延长链，末端仍可先融化。

只有爆炸和实际铃铛位置的 `SoundEvents.BELL_BLOCK` 触发振动，三维半径分别 32/12 格，暂不做遮挡/衰减。遍历范围内已加载 chunk 的 section，palette 不含冰锥则跳过，候选按球形距离筛选；不加载范围外区块，也不维护世界声学网。每事件先保存候选的原预警状态：完整段 5% 进入预警，原已预警段 90% 立即破碎。一次事件不会对刚预警的段再次补抽 90%，后续敲钟可以继续排险。冰自身裂纹音效不触发此机制。

## 材料相变与预算结算

相变数据在 `data/frostedheart/recipes/state_transition/frostedheart/icicle.json`，所有外观/预警状态共用材料 law：热容 1800 J/K、初始焓参考 506666.6666666667 J、加热平台 2°C、目标空气。`heating.latent_heat_j=126666.6666666667 J` 是配方默认预算，约为当前完整薄冰 heating 端点差的 1/3。使用通用材料编译规则，不再在编译器中按冰锥类型推导预算；以后调整完整薄冰预算时，应同步冰锥配方数值。GameTest 核对当前两份数据的比例。

任意段完成材料潜热区间后，`IcicleBlock.meltTip` 只删除其下方当前连续链的最末端一格；不挖掉受热中段，不生成水源。如果受热段自身仍存在，它付出这一潜热区间，恢复到下一轮吸热起点，继续保留预警状态/剩余倒计时；多个受热段可各自付费缩短。被删除末端原来积累的部分潜热随其删除，不交换给受热段，也不持续转移链上的热量。

相变结果的唯一专用处理方法为 `MinecraftPhaseController.meltIcicle`，由活动请求提交、休眠提交和缺失材料记录的回退路径调用。源段仍存在时翻转冰锥自身不可见的 `melt_cycle` 状态，经原有 `setMaterialBlock` 标记为 `MaterialChanges.GAMEPLAY_TRANSITION`。既有材料迁移把已完成的 2°C 平台复位到当前温度的显热起点，潜热差额进入 `externalMaterialEnergyJ` 出项；活动请求仍用原 `APPLIED` ACK。源段自身被删除时沿用普通材料删除流程。

材料记录、休眠投影与保存均使用温度系统通用规则；没有冰锥专用的材料记录插入或休眠投影分支。存在材料记录且未触发框架强制相变时，达到 2°C 只是平台开始，仍需完成潜热预算。确实缺失记录时，通用流程按环境平衡判定，满足条件可在该次随机刻直接缩短末端，不为冰锥补建潜热起点；解析保证温度场同样沿用通用强制相变流程。因此缺失记录/休眠/保证温度场下的融化时机可能与旧专用实现不同。

冰锥仅改变相变的世界结果；温度编译器、材料存档类、休眠类、求解器和 ACK/存档协议均没有为此增加特判。薄冰和其他材料规则保持通用行为。

## 坠落、伤害与灭火

融化滴水由 `IceDripping` 在玩家附近低频处理；任意连续段材料达到 2°C 即可使当前链末端滴水，不要求潜热进度或等待整格消失。只显示一个末端出口，不转移链上热量。平均 40 game ticks 一滴、最远向下 16 格、命中玩家后 100 ticks 潮湿和头部防护规则见[冰溜子与滴水](eave-icicles.md#融化滴水共用规则)。

坠落复用原版 `FallingBlockEntity` 的实体类型、数据包同步、渲染与 NBT。断开时保存原链形态再移除方块，每段一个实体；末端一段保留伤害，其余段无伤害。冰锥专用 tick 不执行落地放置/物品掉落，落地、接触水、超时或越过世界底部后碎掉。

伤害按原版悬挂滴水石的落差/链长公式计算原始值后 /3：

```text
damage = min(floor(max(0,ceil(fallDistance-1))*min(1+2*length,40)),40)/3
```

fallDistance、length 单位格；伤害为生命点，上限 40/3≈13.33 点。使用 `fallingStalactite` 伤害来源，护甲和免伤等继续走原版处理，最终扣血不保证恒为原版的 1/3。创造/旁观不受伤。

实际扫过/碰撞位置中的普通/灵魂篝火执行 `CampfireBlock.dowse` 并置 `LIT=false`，保留方块、燃料时间与烹饪内容；普通/灵魂火焰移除。FH 实际热源随燃烧状态更新，已经存在的周围余热自然消散。无范围灭火，不扩展到其他热源或燃烧实体。

## 存档与验证边界

`cracked`/`shape`/`melt_cycle` 随普通 BlockState 保存。倒计时沿用 vanilla scheduled block ticks 的剩余延时存档，不新增每段方块实体或逐 tick 写存档；支持检查会取代已有长倒计时，替换该位置时清理旧计时。预警状态缺少计时器时按允许的重载后备方案重建 80..320 ticks（4–16 秒）。材料 H 使用现有 thermal chunk checkpoint，坠落实体使用 vanilla NBT；没有冰锥独立 SavedData。

不再提供 `MaterialSectionState.withMaterial`，也不保留针对该专用插入 API 的 `IcicleMaterialSeedGameTests`。材料进度通过现有材料生命周期建立并保存，`melt_cycle` 仅触发普通材料更新，不定义新温度存档字段或专用保存窗口。

自动用例在 `IcicleGameTests` 和 `IcicleThermalGameTests`，namespace `frostedheart_icicle`。2026-10-18 的旧接入曾通过完整 124 项回归和最终 14 项冰锥专项，并生成发行 JAR；详见[当日日记](../../diary/2026-10-18_21-27-48_icicle-growth-fracture-and-melting.md)。这些是旧版本验证结果。

2026-10-19 使用材料熔化 2°C / 环境热裂 5°C，并收敛相变接入后，完整温度/薄冰/冰锥回归 124/124 通过，其中冰锥 13 项。覆盖普通 worker 的潜热消费/重复 ACK/下一次吸热、休眠源段保留预警与逐预算缩短、末端部分潜热不转交、缺失记录的冷/暖回退和单段融化。详见[收敛日记](../../diary/2026-10-19_14-20-30_icicle-phase-integration-simplified.md)；本轮未重新打包 JAR。

16 根×5 格代表场景先预警全部根部，再强制振动判定成功：产生恰好 64 个下段坠落实体，60 步手动物理推进后全部碎掉。三次观测中，单次振动为约 6.3–14.2 ms，物理推进合计约 10.5–23.4 ms。环境为 Java 17、4 个有效 CPU、1.5 GiB 测试 JVM，没有客户端；这些耗时不包含完整服务端 tick、网络接收/渲染和完整 TWR 洞穴负载，不能直接换算为实际 TPS。

仍需客户端确认根部/尖端外观、粒子与声音辨识度、真实卸载/重载体验，以及完整 TWR 冰洞的新地形分布、实际游戏平衡与大片坠落时的表现。

2026-10-20 新增[房檐冰溜子与共用滴水](eave-icicles.md)并把预警冰屑移至外圈后，完整 130/130 required tests 通过（原有 124 项及新增 6 项）。本轮 `thermal/` 代码无改动；滴水只读取已有材料温度。未打包新 JAR，客户端外圈冰屑与滴水观感待试玩；完成记录见[日记](../../diary/2026-10-20_23-09-05_eave-icicles-and-dripping.md)。
