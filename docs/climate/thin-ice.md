# 薄冰开裂与回冻

- Status: `Partial`（此前 110 项回归通过；最新踩踏目标修正后的 8 项薄冰专项通过；客户端日志检查与裂纹资源校验通过，调浅观感、音效试听与性能实测待完成）
- Last verified: `2026-10-18`
- Scope: `frostedheart:thin_ice`、`frostedheart:layered_thin_ice` 的踩踏、冲击、温度裂纹与堆叠
- Primary code anchors: `ThinIceBlock`、`LayeredThinIceBlock`、`ThinIceEvents`、`ThinIcePhaseData`、`MinecraftPhaseController.temperature`、`EntityMixin_ThinIceLanding`、`FHBlockStateGen.thinIce`、`ThinIceGameTests`

## 阶段与踩踏

完整薄冰为 `crack=0..3`：0 完好，1/2/3 为三级裂纹；再推进一次破碎。分层薄冰的总承受次数为 `ceil(L/2)`，L 为厚度 1..8：1–2 层承受 1 次、3–4 层 2 次、5–6 层 3 次、7–8 层 4 次。裂纹级数记录已经承受的推进次数，不是厚度。

服务端 `stepOn` 与 `LivingTickEvent` 收集当 tick 的接触（后者补齐 1.20.1 静止服务端玩家可能不逐 tick 调用 move/stepOn 的情况），`ThinIceEvents.endTick` 对每块被踩冰仅判定一次。同踩取最剧烈动作，不叠加人数。活体触发，包括创造模式玩家；旁观玩家与非活体不触发。创造玩家实际站在冰面上时使用相同规则，悬空飞行不算踩踏。潜行优先；否则水平位移平方小于 `1e-6` 视为站立，移动时按 sprint 标记区分跑步。

| 动作 | 每 tick 成功概率 | 成功后的冷却 |
| --- | --- | --- |
| 潜行站立/移动 | 1/96 | 96 ticks |
| 正常站立 | 1/24 | 24 ticks |
| 走路 | 1/12 | 12 ticks |
| 跑步 | 1/6 | 6 ticks |

单位为 game tick；20 TPS 下 24 ticks 为 1.2 秒。失败不进入冷却。scheduled block tick 仅为冷却令牌，到期不自动推进裂纹。离开后停止普通踩踏推进，保留裂纹。站立首次成功平均约 24 次抽样，成功之间再加约 24 ticks 冷却；整块破碎时间随机，不保证固定撤退时间。

分层碰撞与外观一致，每层 2/16 格。低层通过 `entityInside` 补充触发：实体落地且脚底距冰面小于 0.05 格，避免 vanilla `stepOn` 指向下面的方块。`ThinIceBlock.stepOn` 仅接受与实体 `getOnPos()` 实际支撑位置一致的接触，防止单层薄冰上方的普通踩踏同时损伤下方完整薄冰。

## 冲击、堆叠与破碎结果

`LivingJumpEvent` 起跳按高度 1 格冲击，落地用 `fallOn` 的实际坠落距离 h（格）；单层低表面通过 `EntityMixin_ThinIceLanding` 更正 `Entity.checkFallDamage` 中 `Block.fallOn` 的目标，补齐 legacy 采样落到下面方块的情况。此补丁在摔落伤害判断之前执行，创造模式免伤仍可产生冲击；它只更正单层采样错位，并且只调用一次 `fallOn`，不把同一次冲击传给下方的冰。h<1 没有冲击；否则推进 `floor(h/2)` 级，小数部分再作为概率推进一次。最多计算 4 级，足以破坏任何厚度。冲击绕过已有冷却；有效冲击后，只要冰仍存在，就重新开始 24 ticks 冷却，包括概率未造成裂纹的冲击。后续冲击仍能绕过。普通摔落伤害继续交给父类。

两种方块机械破碎均无掉落，并检查六面相邻的实际 `minecraft:water` 方块（含水源、流水，含上下，不含含水方块）：有水则原位变水源，否则变空气。检查不主动加载邻区块。不传播裂纹。破碎粒子和声音使用 level event 2001；阶段声音仅在成功改变裂纹时播放，1/2 级音量 0.15，3 级 0.9，音调均为 1.0，音源 `FHSoundEvents.ICE_CRACKING`。一次冲击跨级仅播放结果阶段的声音。

分层可继续堆叠至 8 层。`LayeredThinIceBlock.inheritedCrack` 以
`newCrack=ceil(oldCrack * newTotalStages / oldTotalStages)` 继承损伤比例；完好仍为完好，同一承受次数档位不改变损伤。已有冷却保持到期时间。

## 温度裂纹的热量预算

用户确认以相变热量预算代替固定耗时；每级热裂与每级修复均使用对应完整相变预算的 1/4。预算读取实际编译 `MaterialThermalLaw.Transition` 的能量端点差，不使用对已声明目标材料可能不生效的配方默认 `latent_heat_j`。

- 热裂预算 Qh：完整薄冰 9°C 加热到水的端点能量差 /4；当前数据为 95000 J。
- 修复预算 Qc：水源 -5°C 冷却到完整薄冰的端点能量差 /4；当前数据为 90800 J。
- 相变温度 T：`MinecraftPhaseController.temperature(level, pos)`，读取薄冰自身位置。优先读取 `MinecraftThermalInput.sampleMaterial` 的活动材料或按现有相变规则投影的休眠材料温度；只有该位置确实没有材料热状态时，才使用与原材料随机相变共用的 `environmentTemperature`（原位 `naturalBlock + gameplayPassiveEnvironment`）回退。材料归属尚未发布或存档状态不匹配时暂停，保留进度等待后续随机刻，不改用空气温度。`WorldTemperature.block` 仍是环境接口，不能代替材料读取。
- 温暖上限：0<T<3°C 最多热裂 1 级，3≤T<6°C 最多 2 级，6≤T<9°C 最多 3 级；上限不超过该厚度的破碎所需次数，因此薄层可在 9°C 以下热裂破碎。已有更高机械损伤不会因上限较低而被修复。
- T≥9°C 停止此裂纹预算推进，原有材料相变负责融化。完整薄冰原有目标为水，分层薄冰原有目标为空气；本次不改原有融化配方。
- 修复仅在 T<-5°C、六面邻水且冰面没有可触发踩裂的活体时推进，一次预算恢复一级。-5°C 正好相等时没有净冷却功率。已有 -20°C 完整薄冰变普通冰的相变仍存在。

`ThinIcePhaseData` 保存的是裂纹玩法的相变工作进度 W（J），与实际材料本体 H 分开；不复制材料节点或修改材料能量守恒/融化 ACK。采用现有休眠相变的热交换速率作为裂纹预算速率：`G=ln(2)*C/tHalf`（W/K），C 取完整薄冰热容量（当前 5400 J/K），tHalf 取 `FHConfig.COMMON.THERMAL_RUNTIME.dormantTemperatureHalfLifeSeconds`（默认 1800 秒）。每秒温暖累积 `G*T` J，冷却扣除 `G*(-5-T)` J；T 统一使用上述相变温度，分别达到 Qh 或 -Qc 时推进/恢复一级并扣除相应预算。反方向工作先抵消现有部分进度。预算比例不表示实际耗时严格为 1/4，也不等同于活动热网的瞬时功率。

热裂、修复与材料相变使用统一的温度读取路径和相变热量预算。裂纹部分进度仍保存在薄冰专属 SavedData，没有新增 thermal worker 的多分支材料相态，也没有把 W 当作材料本体 H 的真实热流。

## 生命周期与性能边界

random tick 发现符合热裂/修复条件的薄冰后激活对应位置；每 20 ticks 仅更新活跃集合。达到上限或暂不满足条件时退出集合，未完成的工作进度保留，后续随机刻可重新激活。未加载或非 entity-ticking 的区块退出活跃集合，卸载期间不累计工作，重新激活后继续。无需为每块冰创建方块实体，也不全量扫描世界薄冰。冷修复时每秒作局部活体查询。

存档位于各维度 SavedData `frostedheart_thin_ice`，NBT `progress` 列表记录 `position` 与 `work_j`；方块更换时清除该位置进度。裂纹保存在 BlockState，冷却由 vanilla scheduled ticks 保存。`randomTickSpeed=0` 停止热裂与修复，普通踩踏与冲击继续生效。

旧世界相同 ID 使用新规则，新增 crack 默认 0、既有层数保留。普通挖掘、精准采集和掉落继续使用既有 loot 与 `IceBlockMixin_Melt.playerDestroy`。保留随机冰面，顶面使用原版 `destroy_stage` 纹理叠加裂纹。`FHBlockStateGen.thinIce` 为裂纹叠层使用 `translucent` 和顶面 `ForgeFaceData.color=0x66FFFFFF`（40% 不透明度），让裂纹透出冰面颜色、降低黑色对比度，阶段仍由纹理密度区分；没有新冰锥或通用碎裂框架。

## 验证与反馈

自动测试入口为 `ThinIceGameTests`，测试 namespace `frostedheart_thin_ice`，已列入 `build.gradle` 的默认测试 namespace 列表，沿用 `phase0a_empty.snbt` 空结构。温度统一与创造交互修正通过 110 项回归，最后细化落地目标后 7 项薄冰专项全部通过，包含本体/环境温度反差、陈旧 checkpoint 和上下叠冰落地；详细记录见[本轮开发日记](../../diary/2026-10-18_16-16-28_thin-ice-material-temperature-and-creative.md)，此前原型验证保留于[原型日记](../../diary/2026-10-18_14-30-27_thin-ice-mechanics-and-phase-work.md)。

已收到用户观感反馈：原裂纹颜色过深，现已调为 40% 不透明度；调整后的客户端效果待复核，资源检查见[调浅日记](../../diary/2026-10-18_16-51-46_thin-ice-lighter-cracks.md)。系统性试玩仍需覆盖潜行/走跑/起跳的节奏、顶面裂纹透明排序、细微/明显音效的辨识度、流体更新及多人表现；热裂/修复的实际等待时间与大量冰面下的性能也尚未实测。设计确认与后续真实反馈追加在[讨论](../../discussion/2026-10-17_23-27-52_ice-caves-and-thin-ice.md)。

最后代码检查修正了普通踩踏沿 legacy 位置误传给单层冰下方完整薄冰的问题，新增真实移动/碰撞路径回归，最新 8/8 薄冰专项通过。2026-10-18 16:39–16:55 的客户端正常保存退出，没有薄冰加载或执行异常；现有 ERROR 与前一轮客户端日志一致。该会话唯一资源加载在 16:40，早于 16:51 调浅调整，因此不作为调浅效果验证。日志存在一次保存之后的 4.4 秒 tick 落后，不能据此归因于薄冰或确认性能。详见[代码与日志检查日记](../../diary/2026-10-18_17-07-48_thin-ice-code-and-client-log-review.md)。
