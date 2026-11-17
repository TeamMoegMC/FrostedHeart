# 河道自然相变接力扩张的真实复现与修正

- Time: `2026-11-06 16:19:12 +08:00`
- Author: `OpenAI Codex / GPT-6; primary implementation and production-validation agent`
- Status: `completed`（选定河道、材料、余温与红外生产路径；客户端画面和完整整合包不在本次资格内）
- Scope: `MaterialThermalLaw.initialBranch`、`ThermalCellArena.stageMaterialLaw`、`RegionalQuerySnapshot.materialPerturbationAtSlotC`、`WorkerPageStore.collectMaterialFrontier`、河道生产 GameTest 与地面潜热夹具

## Completed

- 按[计划](../plans/2026-11-06_09-56-00_river-thermal-expansion-production.md)在实际 Forge 世界搭建三条160m长、21m宽、5m水道的 `minecraft:frozen_river`：真实成型/团队注册/耗煤T1塔、点燃10秒后熄灭的真实篝火、无源对照。自然背景约-23.7°C，河岸高水面1格。
- 平岸首轮180秒没有百米传播；高岸正常20TPS完整复现：沿河128m标记附近（距塔约127m）水-5°C、泥土-10°C，短时篝火熄灭后也接力出现；水H仍为自然初始化值1732779.9949645996J。源输入并未逐格把这些远处水加热到该温度。
- 根因在材料准入：活跃Brick的表面接触原来无条件请求相邻材料，新材料又因自然相变平台保持活跃，形成接力。现在按已有发布中的具体表面材料温度，相对同种材料自然初始化后的温度计算扰动，并使用原1K进入/0.5K保持滞回。`initialBranch`统一旧初始化分支公式；H、潜热、源功率、空气传播及保存格式未重新定义。
- 正常20TPS相同3600tick场景修复前按预期失败、修复后通过；修复后12000tick/10游戏分钟lockstep行为场景也通过。实际红外编码在远处从水-20、岸-40（单位0.25°C）变为INVALID；它表示缺失材料记录，不能描述为实测水温-20°C。
- 修正旧地面潜热测试的隐式地形前提：在实际房间地面明确放置9块泥土，先断言已发布真实相变材料，再保留熄火后H继续下降的原要求。

## Decisions

- 把潜热保留责任与招募下一批材料的资格分开，保留现有材料进度。没有按河流ID特判、改变塔功率或设置固定物理传播半径。
- 保留平岸不复现、高岸旧代码失败、早期长场景不完整以及旧地面夹具失败的记录，避免把源码公式、未完成运行或修正后的夹具冒充原始世界实测。
- Documentation impact: 更新[余温生命周期](../docs/climate/thermal-residual-lifecycle.md)、[热源与材料](../docs/climate/heat-production-and-network.md)及气候入口；新增[可提交验证摘要](../docs/climate/validation/thermal-river-expansion-2026-11-06.json)。`design/`只读，无配套整合包仓库修改。

## Validation

- Java17当前工作区统一编译成功；随后地面夹具单独编译成功。日志分别为 `build/river-thermal-after-compile.log`、`build/river-thermal-ground-fixture-compile.log`。
- `raised-baseline20`：1项按预期失败；`after-realtime20`：1/1通过。两个180秒窗口均消耗4煤，结束时仍1个实际源；局部塔温度保底断言通过。128m远处水/岸修正后无材料记录，实际红外编码为INVALID。
- `after-long`：12000tick、lockstep实际Forge行为1/1通过，远处仍无材料记录；此时钟不用于墙钟性能资格。
- `material-regressions`：9/9通过，覆盖实际材料能量/转换/ACK/保存/替换/冻结和跨Section接触图。`retention-infrared-final`：4/4通过，覆盖两个红外删除、露天冷却和熄火后实际LIVE泥土释放6.678961353143677J。各个已完成正反场景热保存/worker错误0、关闭root0。
- 同180秒场景结束登记量（包括当时在途工作）由116665218B变为6414784B；区域155变为2。这不是峰值堆或CPU性能测量。完整配置、风噪声差别及范围见验证摘要。
- `assemble`成功：`build/river-thermal-20261106/libs/frostedheart-1.20.1-0.7.9.jar`，14593843B。当前3734个main class缺失0，GameTest class交集0，Manifest为1.20.1-0.7.9；`javap`确认新增初始化/材料扰动方法。未替换用户运行实例的文件。
- 构建在当前源码上增量进行，没有复制历史源码再编译、路径敏感hash或修改依赖工具版本约束；复用现有动态工具解析缓存。

## Remaining

- 本次授权的河道扩张修正和上述生产验证已完成。
- 真实客户端画面、完整整合包分发、已有存档所有相变记录的消退时长及更大并发不在已验证范围；已有潜热记录继续按原生命周期处理。
