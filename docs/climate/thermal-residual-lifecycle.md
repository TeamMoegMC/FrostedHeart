# 余温退出、保存与红外清理

- Status: `Current for the implemented lifecycle and listed validation; full modpack/client capacity remains outside this qualification`
- Last verified: `2026-11-06`
- Scope: 普通空气/材料余温保留、Page/Brick 退出、已加载休眠维护、v6 恢复、红外删除及自然冻结启动/已完成相变提交
- Code anchors: `ThermalRetentionPolicy`, `WorkerPageStore.advanceMaintenance/requestFace/collectMaterialFrontier`, `RegionalQuerySnapshot.materialPerturbationAtSlotC`, `MaterialThermalLaw.initialBranch`, `MinecraftPhaseController.tryAtRandomTick/applyDormantTransition/apply/canTransition`, `AirFieldCheckpoint.retain`, `MaterialSectionState.Retirement`, `DormantChunkThermalState.commitRetirement`, `DormantThermalMaintenance`, `MinecraftPageManager.retire/processShrinks`, `MinecraftThermalInput.captureDormantPage/dormantMaintenanceMask`, `InfraredCapture`

## 阈值的含义

所有数值为相对相应背景的摄氏温差。`ThermalRetentionPolicy.ENTER_C = 1.0` 用于热活动进入及向外扩张，`EXIT_C = 0.5` 用于保留已经形成的热活动和普通保存记录；等于 0.5°C 时保留，小于时允许退出。正负温差对称。

一个尚未计算的区域不会自己检测到升温。`WorkerPageStore.requestFace` 读取已有活动区域的边界扰动：尚未请求的相邻 Brick 达到 1°C 时申请纳入；已请求的相邻 Brick 使用 0.5°C 保持。真实 source seed 和路由/邻接需要也会保留所需区域。点火不必先积累到 1°C。

材料接触扩张另外由 `WorkerPageStore.collectMaterialFrontier` 逐个检查已有表面材料的已提交温度，而不是由整个 Brick 的活跃标记无条件请求邻接材料。`RegionalQuerySnapshot.materialPerturbationAtSlotC` 使用：

```text
H0 = law.enthalpyAtTemperature(N)
b0 = law.initialBranch(N)
T0 = law.temperatureC(H0, b0)
material expansion residual = abs(Tmaterial - T0)
```

`N` 是当前 Page 捕获的自然空气温度，`T0` 是同种材料按该自然值首次初始化后实际求解得到的温度，`Tmaterial` 来自已有区域发布；温度单位 °C、差值单位 K，`H0` 单位 J，`b0` 为显热/升温/降温分支。材料接触也使用进入 1K、保持 0.5K 的既有滞回，针对目标 Brick 的请求状态选择门槛。没有额外材料温度副本或全维度扫描。

这项区分对自然相变尤其重要。源码数据下，`N=-23.7°C` 的新水体进入冻结分支后可读为 -5°C，新泥土可读为 -10°C；即使尚未收到热源输入，也可能存在这些平台。它们仍保留自己的 H、分支与潜热责任；平台本身不再授权直接接力纳入下一批自然材料。空气的实际扰动和发生温度偏移的材料仍通过各自边界规则扩张。材料每个方块各有一个 H，捕获单元为 4×4×4 的 Brick，因此同一 Brick 中数格水可同时进入模拟和显色。

因此，0.5°C 删除还需要满足所有权条件。仍被真实热源、相邻传热、输入恢复或相变需要的部分不能仅因当前温差小而停止计算。纳入计算本身不创造热量，也不自动赋予继续扩张的资格；范围的稳定性由供热、散热、背景和门槛共同决定，门槛不是固定最大半径。

“退休”是退出计算和释放记录的完整交接，没有额外的退休计时阶段。Page 可以只缩小已冷的 Brick；同页其他热点继续存在。材料潜热进度与未消费 J 有独立责任，不能用普通温差过滤清空。

## 捕获和保存

`AirFieldCheckpoint.Capture.add` 的普通空气过滤与旧 residual 保存分支现统一到 0.5°C。v6 的数据表示和原始采样时间继续保持，旧 residual 的十六分之一度量化单位并未重新解释。

`MaterialSectionState.capture` 的生产保存入口过滤接近自然温度且没有潜热/相变请求的材料。被实际采样但结果为空的 Brick 仍携带 `sampledBricks`，合并时删除旧记录，避免空结果反而保留上一份余温。

冷退役捕获可以跳过在当前区域背景下已不活跃的 Air，避免其自然背景差异再次被保存为额外热历史。普通卸载通过已有 `RegionalCheckpoint` 保存有效历史。`MinecraftPageManager` 的后台完整退役与 shrink 都等待捕获成功；只捕获 shrink 实际退出的范围。卸载捕获可以使用回调持有的真实 Chunk，避免只通过已失效的加载索引定位它。

普通 SAVE 和实际 UNLOAD 继续遵守[运行时合同](thermal-runtime-control.md)：普通保存是镜像，实际卸载才导出输入所有权；取消卸载取消临时交接。`pending`、`coldPending` 和在途输入阻止后台清理。过滤幸存记录不重写 H、branch 或原 sampleTick。

## 已加载休眠维护

`DormantThermalMaintenance` 在服务器 END tick 执行，独立于是否存在物理 runtime。每个已加载、持有保存状态的 Chunk 最多一个候选；一个进行中的 Section 切片共享临时读取位置和材料样本。空表直接返回，不加载区块。

检查周期为 100 游戏 tick。每次服务器 tick 最多检查 64 个候选、512 个材料记录，约每 32 个材料记录检查一次 200000ns 软预算。Air 过滤和最终数组发布也计入实际时间；该软预算不是硬时限。积压时继续分片，不能据此保证任何规模下都在五秒内完成。

维护排除 live/captured Brick、封存和输入恢复责任。一个 Section 的候选固定原快照、自然值、profile epoch 和采样时刻；提交前再次核对。材料一次筛选后只压缩一次，全部退出返回 null；Air 通过节点极值与旧局部形状的保守幅值界判定。原数组可继续被已经持有它的读者使用。

在线潜热进度继续使用现有活动判定。自然地形的冻结过程也可能保持相关区域，这不等于篝火仍在供热。应检查 H 随时间的变化来判断是否继续散热，不能仅由相变平台上恒定的温度得出卡住的结论。当前真实地面复测已观察到熄火后的 LIVE 泥土 H 继续下降；构图连接性改动未获足够生产证据，已从本批撤回。

2026-11-06 的河道生产复现进一步区分了保留与扩张：旧邻接规则在高岸河道中把自然平台当作不断纳入邻接材料的理由，十秒篝火熄灭后也可在百米外生成同样的平台记录；修正只约束上述材料扩张资格，潜热的保留和能量交换继续由现有材料所有者处理。具体场景、正常20TPS对照、长时间行为验证和限制见[河道验证摘要](validation/thermal-river-expansion-2026-11-06.json)。

已存材料若完成相变，通过现有 `MinecraftPhaseController.applyDormantTransition` 执行实际世界业务；变化后放弃旧维护视图并重新读取。不能只丢掉材料记录而不完成世界转换。

### 自然冻结启动与已完成相变提交

`world_conditions.fluid_boundary_tag`用于没有材料H记录的自然转换启动。`MinecraftPhaseController.tryAtRandomTick` 的环境回退调用 `canTransition(..., requireFluidBoundary=true)`；水的四个水平邻居全都属于 `minecraft:water` 时，不会仅凭低环境温度直接变成薄冰。靠岸的无记录水仍可执行原自然冻结。

已有材料的相变能量完成后，在线 `apply` 与保存/休眠 `applyDormantTransition` 不再重查这项启动边界。在线仍通过原 `matchesMaterialRequest` 核对当前H已完成、方块、Page/profile、分支和请求序号；休眠入口明确要求 `edge.complete(energyJ)`，半程潜热不能提交。二者保留随机刻开关、最低高度、群系禁止和解析场保底等明确世界限制。转换后用原 `THERMAL_TRANSITION` 交接同一H，目标材料按自己的容量/分支继续降温；没有重复支付潜热或增加第二份温度。v6格式未改变。

2026-11-06早先的[同类机制检查](validation/thermal-similar-mechanisms-2026-11-06.json)记录了修正前行为：自然约-23.7°C、无物理源、完成H为1493000J的内水，在12000tick后仍为水/-5°C；人为造岸后才转换。该记录描述修正前构建。

后续 `ThermalBlockedPhaseProductionGameTests.completedInteriorWaterCommitsPreservesEnergyAndRetires` 使用经过v6 NBT读回的同类完成记录：17tick后提交为 `frostedheart:thin_ice`，原始H仍为1493000J；提交后200tick读到约-8.87494°C。后续真实薄冰→冰潜热继续推进，在每2000tick的采样中首次于22000tick观察到记录已释放，25217tick尾部确认实际 `minecraft:ice` 和空附件。这是观察窗，不是任意场景固定清理时限。

`CompletedPhaseOnlineGameTests` 的实际细档/粗档也通过已计账的有限相变控制、开关暂停/恢复及世界ACK；转换后H与目标H相同。明确禁用相变或禁止转换的群系等约束仍会暂停提交，不因本修正被绕过。配置、时钟、正反例和范围见[完成相变提交验证](validation/thermal-completed-phase-2026-11-06.json)。

Chunk 实际卸载、Level 卸载、服务器停止会解除维护引用。已卸载数据不保留定时任务；重新加载时登记维护，实际恢复构建先过滤已耗尽记录。配置重载会使在途维护视图失效，下一次重新按当前 profile 判断。

## 红外与温度查询

普通查询保持只读，按原 sampleTick 投影。清理提交前可继续看到衰减中的历史记录；提交后材料 revision 或 Chunk materialRevision 通知现有红外 full/delta 路径发送 INVALID，清除客户端已有纹理。删除即使没有跨过 0.25°C 显示量化档也需要发送。

这与无覆盖时的开关行为独立：红外视野仍可保持开启。材料读数缺失时温度计使用当前不可用合同，红外显示蓝色占位；该颜色不是实测 -20°C。仍有解析场时按既有顺序重新合成，不因材料删除抹掉能量塔等场的显示。详见[世界温度与红外](world-climate-and-temperature.md)。

## 散热时间

`FHConfig.COMMON.THERMAL_RUNTIME.dormantTemperatureHalfLifeSeconds` 的源码默认改为 30 游戏秒。在线 `airHeatCapacityJPerBlockK` 仍为 1200 J/(block·K)，普通材料类别仍采用 stone 5400、wood 2700 J/K；显式配方热容/潜热仍由配方决定。本批试过的全局热容缩小已撤回。原有配置文件中的显式数值仍优先，修改需要重启。测试配置与实际整合包配置必须分别核对。

在自然浴固定、普通显热成立时，温差按 `deltaT(t) = deltaT0 * 2^(-t / halfLifeSeconds)` 衰减，t 的单位是游戏秒（20 tick）。2/10/20°C 温差到 0.5°C 门槛约需 60/130/160 秒；相变平台不适用此时间表。停服的现实时间不计入。

在线区域继续使用实际容量、材料接触和环境边界换热，因此上述时间表不是任意在线房间的退休时限。封闭石屋在熄火约200秒后仍观察到超过0.5°C的真实余热，不能按倒计时清空。通风房间复测在熄火60秒内温升从约4.84°C降至3.16°C；随后实际卸载、等待100游戏秒、重载，余温与Page结束并可重新点燃，完整链路3223tick。此数值包含卸载等待，不是全程在线冷却时长，也不证明所有建筑的平衡已校准。

## 验证范围

本批统一编译、打包成功。`ThermalRetirementProductionGameTests` / `ThermalRetirementRoomGameTests` / `ThermalRetirementReloadGameTests` 覆盖最后材料删除与两个红外baseline、部分回收/原时间戳、无人查询的衰减回收、自然地面释放潜热、露天在线退出，以及通风房间冷却/实际卸载重载/余温释放/重新点燃，均已有真实Forge通过记录。`ThermalRetirementRestartGameTests` 使用同一磁盘世界的三个JVM，写入、读回H/branch/tick并冷却删除、确认不复活三轮全部通过。最后的`ThermalRetirementPendingGameTests`以实际20J恢复输入验证空附件要等输入交接结束后注销。

5项存储、3项原版早期卸载/初始化/取消、7项热源输入及独立保存检查均通过。部分历史组合因共享世界中的热源/容量夹具干扰而失败，已按独立世界重新验证；旧房间固定时间清空预期也已修正。所有原始失败、构建范围及复测结果见[验收摘要](validation/thermal-residual-retirement-2026-11-06.json)，不能将它们称为一次全量首过。

128个真实篝火、2400tick负载窗运行通过，温度年龄/几何逾期/消费者过期违规均为0。该夹具虽分配100个消费者对象，实际只推进到10→30个消费者，共1601次查询。热主线程P99为1.126ms，完整服务器tick P99为56.820ms，最大861.201ms发生在第一个观测tick；登记峰值约63.18MiB，关闭root0。这些数字包含冷构建、施工与JFR，是特定窗口的观察，不是提速比例、准确CPU占用或百人20TPS资格。

最终生产JAR另在独立`forgeserver`进程完成启动、真实篝火燃烧与熄灭、保存和正常停服。实际磁盘区块包含v6、2个空气component与1条待消费输入，热保存/worker错误0。该服务器使用精简依赖集，有可选配方/研究catalogue诊断，不作为完整整合包验收。真实多个客户端的红外渲染仍需单独验证。
