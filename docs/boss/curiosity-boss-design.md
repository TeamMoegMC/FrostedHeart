# 「雪原深处的好奇心」世界遭遇型 Boss 设计文档

> 状态：设计定稿草案，等待评审后进入实现。
> 关联系统：温度（`content/climate`）、世界生成（`content/world`）、注册表（`bootstrap/common`）。
> 命名 ID：`frostedheart:curiosity_entity`（沿用既有注册，见 `FHEntityTypes.CURIOSITY`）。
> 本文描述「雪原深处的好奇心」（Nanite 匍匐集群）Boss 的玩法流程与技术方案，替换
> `src/main/java/com/teammoeg/frostedheart/content/world/entities/` 下的简陋原型。

## 1. 背景与定位

### 1.1 世界观依据（`docs/design/`，只读）

`docs/design/lore.md` 与 `docs/design/world-design.md` 给出了纳米机器人的完整设定，本 Boss 与
其严格对齐（这些文件标注「AGENTS SHOULD NEVER MODIFY」，只读不改）：

- **「智雾」**：旧联邦军方（the Empire，与 `content/world/structures/package-info.java` 的称谓
  一致）研发的可自我复制纳米机器人，失控后覆盖全球，酿成大寒潮。
- **胞体**：纳米机器在宏观层面聚集形成的集群。本 Boss 即世界设计文档点名的
  「**匍匐纳米机器人集群**」，属胞体的一个门类（另有浮空集群、水栖集群等）。胞体外观
  似「半透明的纱」，常模仿生物形态。
- 胞体**无法被物理攻击伤害，只能通过提高温度驱散**；敌对共生体同样惧怕剧烈的热量波动。
  这正是「用火燃烧就会消散」的设定来源：火 = 局部高温注入。
- 纳米机器「**无法凭空产热或制冷**」（不违反物理法则），但明确拥有「**温度流动的控制**」
  能力。因此场地骤冷的设定诠释是：集群把场地内的热量**搬运**进冻土深处（热泵式排热），
  而非凭空制冷——这同时解释了为什么热量能反制它（热水/加热背包抬高体温、能量塔/加热器
  向环境供入热量，与排热竞争，见 §4.2），以及为什么火能烧散它（瞬时热量注入超过其搬运
  能力，高温驱散胞体）。
- 美术基调（`docs/design/technicals.md`）：BlockBench 低多边形、16×16 贴图、复古未来风；
  本 Boss 的隆起 mound 与露出核心均呈「半透明纱质」胞体质感。
- 区域为寒潮后的北美西北部雪原（游戏主舞台），雪原偶遇在叙事上自洽。

### 1.2 定位

本 Boss 是「一片在雪原冻土下休眠、因好奇而苏醒的纳米机器人集群」：它既不是有血有肉的怪物，
也不是机械哨兵，而是环境本身的一部分——因此它的威胁全部通过本模组的**温度系统**体现，
击杀方式也不是常规武器，而是**火**。

定位：**世界遭遇型 Boss**（野外偶遇，非任务/结构绑定）。玩家在雪原探索时发现一片异常平整的
区域——那是集群匍匐的痕迹。逗留过久即触发战斗；打不过可以跑，逃出场地即脱战。战斗全程无
强制锁场，符合「好奇心」而非「猎杀者」的叙事。

设计目标（优先级从高到低）：

1. **温度系统是主威胁**：降温由场地冷场驱动，玩家用热水（`ITempAdjustFood`/`CupTempAdjustProxy`）、
   加热背包（`HeatingDeviceContext`）和衣物（`ClothData`/`BodyHeatingCapability`）反制——完全复用
   现有体温管线，不新增独立温度机制。
2. **可逃可战**：任何阶段玩家都可以离开场地脱战；坚持/穿迷宫/烧核心是可选的胜利路径。
3. **低侵入性方块操作**：所有场地方块修改可完整恢复，不破坏玩家建筑，不在无雪地形生效。
4. **可调参**：所有数值进入 `FHConfig`，便于整合包平衡。

## 2. 现有原型与复用决定

| 项 | 决定 |
|----|------|
| 注册 ID `curiosity_entity` | **保留**。`FHEntityTypes.CURIOSITY`、生成蛋 `curiosity_spawn_egg`、属性与放置规则注册点均沿用，只改实现。 |
| `CuriosityEntity` 本体 | **重写**为多阶段状态机（见 §3），删除旧「220 tick 无敌 + 火焰击杀」逻辑。 |
| `CuriosityEntityModel` / `CuriosityEntityRenderer` | **重写**。模型改为「雪丘/凸起地面」形态，对应地下追踪者的地表表现。 |
| 属性/生成注册点（`FHCommonEventsMod`） | 保留，属性按新设计调整，`canSpawn` 加入地形与群系条件。 |
| Boss 条（`ServerBossEvent`） | 保留概念，仅在场内玩家可见；Boss 音乐播放 `the_fall_of_arcana`（OGA 开源，已注册 `FHSoundEvents.TFOA`，开关进配置，见 §8/§12）。 |

## 3. 战斗流程（状态机）

单实体多阶段：一个 `CuriosityEntity` 贯穿全程，地面「隆起」等纯表现由短寿命辅助实体承担
（`CuriosityMoundEntity`，见 §8）。

```
                 ┌──────────── 玩家逗留 ≥5s ────────────┐
                 ▼                                        │
 DORMANT ──▶ RISING ──▶ HUNT(60s) ──▶ MAZE ──▶ EXPOSED ──┼─(核心被火烧)─▶ DISPERSED(胜利)
  (匍匐)     (3s 演出)   (地下追踪)   (迷宫)   (核心露出)  │
                 ▲           ▲                            │
                 │           └─ 60s 未烧掉核心 → BURROW ──┘(冷场升级，返回 HUNT 新一轮)
                 └─ 玩家离场 ≥10s → RESET(清理场地，回 DORMANT)
```

| 状态 | 触发 | 行为 | 结束条件 |
|------|------|------|----------|
| DORMANT | 生成/重置 | 无冷场；实体潜伏在场地中心地下 1 格，不可见（无渲染）；检测玩家 | 玩家进入半径 12 且累计停留 5s（离开清零） |
| RISING | 逗留触发 | 3s 演出：地面粒子雪雾渐浓、低频音效、冷场 tier1 放置、Boss 条出现 | 3s 结束 |
| HUNT | RISING 结束 | 地下追踪（§6.2）：朝目标玩家移动，地表隆起 + 雪雾 + 沿途撒细雪；冷场 tier1 | 坚持 60s → MAZE；玩家离场 → RESET |
| MAZE | HUNT 计时到 | 雪墙迷宫 5s 升起（§5.3）；冷场升 tier2；升起完成时核心在深处「露出」 | 核心被烧 → DISPERSED；60s 超时 → BURROW；离场 → RESET |
| EXPOSED | 迷宫升起完成 | 核心（实体自身）静止于迷宫深处随机格；仅受火伤害（§6.4） | 被火烧死 → DISPERSED；60s 超时 → BURROW |
| BURROW | EXPOSED 超时 | 演出 2s：核心钻回地下，冷场再降一档，round+1，回到 HUNT | — |
| DISPERSED | 核心死亡 | 雪雾爆发、音效、清理场地与冷场、掉落奖励、Boss 条移除 | — |
| RESET | 任意战斗状态中玩家离场 ≥10s；或战斗范围内所有玩家死亡（§6.5） | 清理全部场地方块与冷场，回 DORMANT | — |

**逃脱永远是安全阀**：冷场半径固定（24），玩家跑出半径 40 且 10 秒不回来即脱战；Boss 本体
不会离开场地中心。多玩家时目标取场地内最近玩家（§8）。

## 4. 温度联动设计（核心）

### 4.1 冷场实现

复用既有 `ChunkHeatData` 热区系统，放置**负值**球形热区作为「冷场」：

```java
// 激活：服务器端
ChunkHeatData.addTempAdjust(level, new SphereHeatArea(arenaCenter, R, -coldTier));
// 清理（RESET / DISPERSED / onRemovedFromWorld）
ChunkHeatData.removeTempAdjust(level, arenaCenter);
```

- 冷场自动流入 `WorldTemperature.air/block` → `SurroundingTemperatureSimulator` → 玩家体温，
  **不需要改动任何温度管线**。
- 设定诠释（§1.1）：冷场 = 集群把场地热量搬运进冻土深处（温度流动的控制），实现上即一个
  负值热区，无新机制。
- 生成器热场已有同款先例：`HeatingLogic.addSphereTempAdjust(...)`（`climate/block/generator`）。
- 红外视野（InfraredView）会自动把冷场渲染出来，等于免费获得「冷域可视化」。

### 4.2 反制关系：环境侧与身体侧（关键依据）

冷场把 `WorldTemperature.air`（环境温度）拉低，但玩家受到的伤害由**体感温度**决定：

```text
体感温度 ≈ 体温 + 导热系数 × (环境温度 − 体温)   // PlayerTemperatureData.updateWhenInsulated
伤害来源 = 低体温症效果按体感温度阈值结算        // HypothermiaEffect
```

反制因此分两条路径，机制完全不同：

- **环境侧（世界热场）**：只有能量塔/加热器这类**放置式设备**才会生成正值热区
  （`HeatingLogic.addSphereTempAdjust`）。`ChunkHeatData.getAdditionTemperatureAtBlock` 对重叠
  热区**取最大值**（`tmp > ret`），正值热区与冷场重叠时环境温度被抬回正值——热源从环境侧
  抵消冷场。
- **身体侧（玩家随身装备）**：热水（`ITempAdjustFood`）、加热背包等加热设备
  （`HeatingDeviceContext`）**提高体温**；衣物（`ClothData`）**降低导热系数**。它们**不产生
  任何世界热场**，而是在环境温度不变的情况下抬高体感温度，从而压低/消除低体温症伤害。

玩家的随身装备与 Boss 冷场**不共享同一套数值**：冷场只作用于环境侧，装备只作用于身体侧，
两者在体感温度处汇合——这正是「喝口热水，或是开启加热背包」能撑住、而跑出冷场半径即可
脱险的设计依据。冷场半径外环境不受影响，天然形成「场地内骤冷」的边界。

结论：**不需要新温度机制**，只需一个负值 `SphereHeatArea`；两类反制手段均复用现有系统。

### 4.3 降温阶梯

| 阶段 | 冷场值（温度增量） | 效果参照（雪原背景温约 -10~-30°C 时场地空气温度） |
|------|--------------------|----------------------------------------------------|
| RISING / HUNT | -15 | 约 -25~-45°C：明显变冷，无装备快速失温（低体温症 `HypothermiaEffect`） |
| MAZE（第 1 轮） | -30 | 约 -40~-60°C：无加热难以久留 |
| 每轮 BURROW | 追加 -15 | 逐轮逼近低温期底部（`COLD_PERIOD_BOTTOM_T10 = -90`） |
| 上限 | -75 | 约 ≤ -100°C：极限环境，必须重加热装备 |

数值全部进配置（§12）。冷场在 RESET/DISPERSED 时移除，温度即刻恢复。

### 4.4 可选扩展（Stretch Goal）

- 战斗期间将场地所在区块标记为局部暴风雪（`WorldClimate`），让降雪视觉与冷场同步；
  `WorldClimate` 是全局气候数据，需先评估改动成本，故列为弹性目标，v1 不依赖。

## 5. 场地与方块

### 5.1 场地定义

- 场地中心 `arenaCenter` = 实体生成点；战斗半径 `R = 24`（圆形判定用 XZ 距离）。
- **DORMANT 的「平整异常区域」不修改地形**：`canSpawn` 要求生成点 3×3 范围内高度差 ≤1 且
  地表为雪/雪层/草方块，天然满足「异常平整」的观感；玩家通过视觉（均匀雪面 + 潜伏期少量
  缓慢粒子）发现它。

### 5.2 HUNT 阶段的细雪斑块

- 追踪者移动时，每 30 tick（1.5s）在脚下放置一块 3×3 的 `Blocks.POWDER_SNOW`（1 层），玩家
  踩入会下陷并持续失温——即「不要走进它制造的细雪之中」。
- 仅允许替换 `AIR`/`Blocks.SNOW`（雪层）；所有放置位置记录到 `placedBlocks` 环形缓冲，上限
  40 块，超出时移除最旧（滚动窗口，同时限制存储与清理成本）。
- RESET/DISPERSED 时按记录还原原始方块状态。

### 5.3 MAZE 阶段的雪墙迷宫

- 尺寸：11×11 格 × 每格 3×3，墙厚 1、高 3（占地 43×43 = 11×4−1，含外墙，覆盖场地中心）；墙壁方块
  `Blocks.SNOW_BLOCK`。
- 生成算法：确定性递归回溯（种子 = world seed + arenaCenter + round），双端可复现，无需同步
  布局数据（§8）。
- 入口 = 升起时刻玩家所在格；核心（EXPOSED 位置）= 距入口最远的格之一（随机取）。
- **升起动画**：按与入口的 BFS 距离分波次放置方块（每 tick 一波 + 雪雾粒子），约 100 tick
  （5s）完成——「雪墙筑成的迷宫缓缓上升」。
- 方块安全规则：仅替换空气/雪层；若目标格已有非雪方块（玩家建筑、树木），该墙段跳过并在
  邻格补墙（降级策略），保证不破坏建筑。
- 清理：按 `placedBlocks` 快照逐块还原（放置前记录原始状态；还原时校验当前方块仍是我们放
  置的，防止吞掉玩家新放的方块）。

## 6. 各阶段详细行为

### 6.1 DORMANT / RISING

- 实体 `noPhysics = true`，位于地表下 1 格，不参与常规 AI；Boss 条隐藏。
- 检测：每 tick 对 `level.players()` 计算 XZ 距离 ≤12 者累计停留计时；中断即清零。
- RISING：3s 内粒子密度递增（`ParticleTypes.SNOWFLAKE` + 自定义雪雾）、播放低频环境音、
  放置冷场 tier1、显示 Boss 条。RISING 结束进入 HUNT。

### 6.2 HUNT（地下追踪，60s）

- 移动：追踪者朝目标玩家水平移动，速度 0.30（玩家疾跑约 0.28 加速度后更快，可持续逃脱；
  实际值调参），每轮 +0.03，上限 0.45；不攀爬、不跳跃、穿墙（noClip 移动）。
- 地表表现：每 10 tick 在追踪者上方地表生成一只短寿命 `CuriosityMoundEntity`（存活 10 tick，
  渲染为隆起的雪丘模型 + 向外扩散的雪雾粒子）——「地表随着它移动时升起，形成大量雪雾，
  遮挡视线」。
- 目标切换：每 20 tick 重选场内最近玩家；玩家死亡/离场按 RESET 规则处理。
- 计时 60s 结束 → MAZE。计时期间若追踪者与玩家距离 <2 格，不直接造成伤害，而是以其位置
  为中心立刻撒一圈细雪（避免无警告秒杀，威胁全部来自温度）。

### 6.3 MAZE

- 进入 MAZE：冷场升 tier2，按 §5.3 生成并升起迷宫；升起期间实体潜回中心地下。
- **核心位置不给任何提示**（已确认：无粒子、无指南针，玩家自行搜索）。
- 升起完成后实体传送至核心格、进入 EXPOSED。

### 6.4 EXPOSED（匍匐集群——脆弱的核心）

- 核心 HP 20（配置）；**仅 `DamageTypeTags.IS_FIRE` 伤害有效**，所有火系伤害源均生效
  （已确认：打火石、火焰附加剑、火矢、岩浆），其余伤害源 `hurt()` 直接返回 false 并播放
  「无效」音效 + 少量粒子。
- **打火石/火焰弹右键可直接点燃核心**（`mobInteract` 实现：原版 1.20.1 的
  FlintAndSteelItem/FireChargeItem 没有 interactLivingEntity，右键实体本无效果）。
- 点燃后进入燃烧过程（实体着火，约 3s 内燃尽死亡）——「用火燃烧就会消散」。
- 核心静止不反击，但迷宫内冷场仍生效，玩家要在失温前找到并烧掉它。
- 60s 内未击杀 → BURROW。

### 6.5 DISPERSED / BURROW / RESET

- DISPERSED：雪雾爆发（一次性大量粒子）、消散音效、按 §5 清理全部场地方块、移除冷场、
  移除 Boss 条、发放掉落（§11）。实体本身 `discard()`。
- BURROW：2s 钻地演出 → round+1 → 冷场值追加一档 → 回到 HUNT（新一轮计时）。
- RESET：玩家离场 ≥10s（或场地内无玩家 60s），或**战斗范围内所有玩家均已死亡**——多人协作
  时只要还有存活玩家在场地内战斗就继续（已确认）：清理方块与冷场、round 归零、回 DORMANT。

## 7. 数值初案（平衡表）

| 参数 | 初值 | 说明 |
|------|------|------|
| 战斗半径 R | 24 | 冷场与细雪活动范围 |
| 逗留触发 | 半径 12，累计 5s | DORMANT 检测 |
| HUNT 时长 | 60s（1200 tick） | 「坚持一分钟后」 |
| MAZE/EXPOSED 时长 | 60s | 超时 BURROW |
| 迷宫升起时长 | 5s（100 tick） | |
| 逃脱判定 | 半径 40，离场 10s | RESET |
| 冷场 tier1 / tier2 / 每轮追加 / 上限 | -15 / -30 / -15 / -75 | §4.3 |
| 追踪者速度 | 0.24（略低于疾跑 ≈0.28 b/t），每轮 +0.03，上限 0.45 | 疾跑可逃脱 |
| 细雪斑块 | 3×3，每 1.5s 一块，上限 40 块 | 滚动窗口 |
| 隆起 mound | 每 0.5s 一只，寿命 0.5s | |
| 核心 HP | 20，仅火伤害，燃尽 3s | |
| 矿霜掉落 | 24 个随机矿霜球（`CONDENSED_BALLS` 池）+ 经验 50 | §11 |

## 8. 网络与同步

- 实体状态（阶段、round、计时器、核心位置、冷场值）经 `EntityDataAccessor` 同步；全部状态
  变化发生在服务器，客户端只渲染。
- 迷宫布局**不同步**：双端以 `(worldSeed, arenaCenter, round)` 确定性生成，仅需同步 3 个值。
- `CuriosityMoundEntity`：普通实体同步（位置 + 存活 tick）；无 AI、无碰撞、无存档
  （`noSave()`），仅客户端渲染。
- Boss 条：`ServerBossEvent`，对 `startSeenByPlayer/stopSeenByPlayer` 的场地内玩家增删。
- Boss 音乐（已确认）：战斗期间客户端循环播放 `the_fall_of_arcana`（`FHSoundEvents.TFOA`，
  OGA 开源，sounds.json 已有 stream 条目）；若 `ServerBossEvent` 的 Boss 音乐为硬编码原版
  曲目，则自实现 Boss 音乐 `SoundInstance`（参照 `MinecartBossMusicSoundInstance`），开关
  由 `bossMusic` 配置控制，状态随实体数据同步。
- 冷场数据走既有 `ChunkHeatData` 同步通道（`FHNotifyChunkHeatUpdatePacket`），零新增网络代码。

## 9. 持久化与异常恢复

- NBT 持久化：`state`、`round`、`arenaCenter`、`placedBlocks` 快照列表、各计时器。
- 维度切换/实体卸载：`onRemovedFromWorld` 中移除冷场并让 mound 自然消失；重载后按 NBT 恢复
  状态并**重新放置冷场**；已升起的迷宫沿用记录方块，不重建。
- 方块还原安全（防吞玩家方块）：还原前校验「当前方块 == 我们放置的方块」（指纹校验），
  不匹配则跳过；chunk 卸载中的方块不处理。
- 服务器崩溃中断：`placedBlocks` 快照随实体 NBT 保存，重启后仍可还原。

## 10. 生成与整合

- 生成途径：`FHBiomeModifiers.Instance.modify()` 中追加
  `spawns.addSpawn(MobCategory.CREATURE, new SpawnerData(FHEntityTypes.CURIOSITY.get(), weight, 1, 1))`，
  weight 取 `FHConfig` 的 `spawnWeight`；**测试期默认 10（方便在雪原寻找测试），发布前调低**。
- `canSpawn` 增强（已确认）：生成点群系 ID 必须在 `FHConfig.SERVER.CURIOSITY.spawnBiomes`
  列表内（**默认仅 `minecraft:snowy_plains`**；TWR 包的自定义雪原群系 ID 由包侧配置添加）、
  生成点 3×3 高度差 ≤1、地表为雪/雪层、距玩家 >32。
- 生成蛋 `curiosity_spawn_egg` 保留，供测试与整合包任务使用。
- 整合包协作：群系列表与权重均为配置项，包侧直接改配置即可；按 AGENTS 规则在两个仓库
  分别验证。

## 11. 掉落与奖励（已确认，见 §16-1）

DISPERSED 时掉落**大量「矿霜」凝缩矿石球**：从 `FHTags.Items.CONDENSED_BALLS` 标签池中随机
抽取，总计 `oreFrostDropCount`（初值 24）个 + 经验 50。池内含已注册的 9 种矿霜球：
`condensed_ball_{iron,copper,gold,zinc,silver,tin,pyrite,nickel,lead}_ore`。

- 不新增任何物品/材质，全部复用现有矿霜球。
- 「纳米机器人原始粉末」暂不引入（暂无材质），保留为未来选项，与胞体收集设定不冲突。

## 12. 配置项

`FHConfig.SERVER` 新增 `CURIOSITY` 节：

```text
arenaRadius=24, lingerRadius=12, lingerSeconds=5, escapeRadius=40, escapeSeconds=10,
huntDurationTicks=1200, mazeDurationTicks=1200, mazeRaiseTicks=100, risingTicks=60, burrowTicks=40,
coldTier1=-15, coldTier2=-30, coldPerRound=-15, coldCap=-75,
trackerSpeed=0.24, trackerSpeedPerRound=0.03, trackerSpeedCap=0.45,
powderSnowEnabled=true, powderSnowIntervalTicks=30, powderSnowMaxPatches=40,
moundIntervalTicks=10, moundLifetimeTicks=10, coreHealth=20, coreBurnTicks=60, mazeCells=11,
spawnWeight=10（测试期默认，发布前调低）, spawnBiomes=["minecraft:snowy_plains"],
oreFrostDropCount=24, oreFrostDropXp=50, bossMusic=true
```

注：`spawnBiomes` 为群系 ID 字符串列表（`List<String>`），包侧可直接增补自定义雪原群系。

## 13. 文件与模块清单

新增：

- `content/world/entities/CuriosityPhase.java` — 阶段枚举与转换表
- `content/world/entities/CuriosityArena.java` — 场地管理：细雪/雪墙放置与快照恢复、冷场挂载
- `content/world/entities/CuriosityMaze.java` — 确定性迷宫生成器
- `content/world/entities/CuriosityMoundEntity.java` — 地表隆起表现实体
- `content/world/entities/CuriosityMoundRenderer.java`（客户端）
- 音效注册 + `assets/frostedheart/sounds.json` 新条目（环境嗡鸣/隆起/消散/无效打击）
  （Boss 音乐复用已注册的 `the_fall_of_arcana`，无需新资产）

重写：

- `content/world/entities/CuriosityEntity.java`
- `content/world/entities/CuriosityEntityModel.java`、`CuriosityEntityRenderer.java`

修改：

- `bootstrap/common/FHEntityTypes.java`（保持 ID；sized/category 调整；注册 Mound 类型）
- `events/FHCommonEventsMod.java`（属性表、canSpawn 规则）
- `events/FHClientEventsMod.java`（渲染与 layer 注册）
- `content/world/FHBiomeModifiers.java`（生成权重）
- `infrastructure/config/FHConfig.java`（CURIOSITY 节）
- `assets/frostedheart/lang/*.json`（名称、Boss 条文本）

删除：旧原型残留逻辑（Invul tick、占位目标 goal 等）。

## 14. 实施里程碑

| 里程碑 | 内容 | 验收 |
|--------|------|------|
| M1 | 状态机骨架 + DORMANT/RISING + 生成与配置 + NBT 持久化 | 雪原生成、逗留触发、Boss 条出现 |
| M2 | 冷场挂载 + HUNT 追踪 + 雪雾粒子 + 隆起 mound | 激活后温度骤降，热水/加热背包可反制 |
| M3 | 细雪斑块 + 逃脱/RESET + 滚动清理 | 细雪生效、离场脱战、方块完整还原 |
| M4 | 迷宫生成与升起 + EXPOSED 核心 + 火焰击杀 + 掉落 | 完整一轮战斗闭环 |
| M5 | 音效/音乐开关、调参、多玩家联测、文档与 diary | 平衡表复核、跨仓库验证 |

## 15. 风险与对策

| 风险 | 对策 |
|------|------|
| 方块操作破坏玩家建筑 | 只替换空气/雪层；快照 + 指纹校验还原；非雪方块跳过 |
| 崩溃/断电后场地残留 | 快照随实体 NBT 保存，重启后恢复；还原前校验 |
| 粒子/实体性能 | 粒子数量上限；mound 寿命 0.5s 且 noSave；迷宫一次性生成 |
| 多玩家并发 | 目标 = 场内最近玩家；Boss 条按玩家增删；全员离场才 RESET |
| 冷场误伤周边生态（作物/动物冻死） | 半径 24 固定、仅战斗期间存在——有意为之的机制，写入文档与提示文本 |
| 生成过频污染探索体验 | weight 与群系列表均配置化；测试期 weight=10 方便测试，发布前调低；单只生成 + 平整雪面条件 |
| 与 KubeJS/整合包生成配置冲突 | AGENTS 双仓库验证；模组默认低权重 |

## 16. 已确认决策（用户答复，2026-08-15）

1. **掉落**：不掉粉末（暂无材质）。掉落大量已注册「矿霜」凝缩矿石球——从
   `FHTags.Items.CONDENSED_BALLS` 标签池随机抽取（§11）。
2. **Boss 音乐**：使用 `the_fall_of_arcana.ogg`（OGA 下载的开源音轨，已注册
   `FHSoundEvents.TFOA`）。
3. **生成频率与群系**：频率进 `FHConfig`（`spawnWeight`），测试期默认调高（10）方便在
   世界中寻找；可生成群系做成配置 ID 列表（`spawnBiomes`），默认仅雪原
   `minecraft:snowy_plains`，TWR 的自定义雪原群系由包侧配置添加。
4. **迷宫提示**：不给任何核心位置提示，玩家自行搜索。
5. **死亡重置**：战斗范围内**所有**玩家死亡才立即回 DORMANT；还有存活玩家则战斗继续。
6. **点火方式**：所有火系伤害源均有效（打火石、火焰附加剑、火矢、岩浆，即
   `DamageTypeTags.IS_FIRE`）；另支持打火石/火焰弹右键直接点燃核心。
   **掉落保护**：矿霜球掉落物设为火焰免疫，岩浆/火矢击杀不会烧毁战利品。

> 注：本文档为设计稿，实现时如遇上述源码事实变化（如 `ChunkHeatData` 叠加规则调整），以代码
> 为准并同步修订本文。
