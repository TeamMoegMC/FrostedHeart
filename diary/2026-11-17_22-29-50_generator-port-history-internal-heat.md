# 能量塔Port记录与旧三项容量的历史核对

- Time: `2026-11-17 22:29:50 +08:00`
- Author: `OpenAI Codex`
- Status: `completed; history and current source inspected; comments/documentation only`
- Scope: `MinecraftPhysicalSourceProfile.Port`、`EmissionPort`、`ThermalSourceLedger.maxPortsPerSource`、`SpatialSourceTimeline.CHANNELS`
- Follows: [四向排热实现](2026-11-17_22-21-49_generator-four-horizontal-outlets.md#generator-four-horizontal-outlets)

## Findings And Correction

- 用户要求解释旧三项上限的来源，并纠正将源代码的 `Port` 记录口语化为实际出口的表述。本次未改变供热参数和运行逻辑。
- `0b3929568`（2026-08-29）已在 `ThermalDimensionEngine` 中给 `ThermalSourceLedger` 传入 `maxPortsPerSource = 3`。同一版本的 `GENERATOR` 恰有三条记录：`Port.airFace(0, SourceChannel.CONVECTION, 0.7D, ...)`、`Port.internalHeat(1, SourceChannel.CONTACT, 0.1D)`、`Port.declaredLoss(2, SourceChannel.RADIATION, 0.2D)`。
- `0e3589e1a`（2026-08-30）简化了类型表示，仍保留三项；辐射项表示为 `Port.radiationLoss(2, 0.2D)`。
- `c306b4c73`（2026-08-31）删除 `Port.internalHeat(1, 0.1D)`，把Air份额从0.7改为0.8，辐射0.2保留；源账本的三项容量随后继续沿用。
- `61eddf706`（2026-10-31）加入 `SpatialSourceTimeline.CHANNELS = 4`，用于公共输入汇总及按Port记录汇总；这是公共通道加三条Port记录的存储安排。
- 前条日记“原来的3口上限满足当时最多2口的机器profile”和前轮解释只描述了本次修改前的profile，遗漏了最初确实存在三条记录的历史。旧容量并不是凭空给当前两个记录预留一项，也不能将这个数字解释为三个空间排热方向。

## Current Representation

- `EmissionPort` 的字段为 `portId`、`powerShare`、`SourceBinding`。该数组既表示实际供热目标，也表示已声明的能量损耗；不是每一项都有空间出口。
- `WorkerPhysicalSourceBindings.initialPorts` 将每条 `MinecraftPhysicalSourceProfile.Port` 转成一条 `EmissionPort`；`RADIATION_LOSS` 转为 `SourceBinding.declaredLoss`，所以当前实现中它占一条记录。
- `ThermalSourceLedger` 以 `sourceSlot * maxPortsPerSource` 取得每源数组段，`validatePorts` 按数组长度检查容量，各项份额合计为1。旧3是定长记录容量，不是热辐射或传热的物理限制。
- `radiativePowerShare` 在profile中独立求和，`radiativePowerW` 由总功率与该份额计算。实际辐射查询不需要Air目标；辐射占Port记录来自统一功率分账的表示选择。
- `PhysicalSourceSpatialIndex.refreshTargets` 只处理正份额 `AIR_FACE`，其去重目标Brick计数不包含 `RADIATION_LOSS`。不要将这个目标存储与账本的Port记录容量混为一谈。

## Documentation And Validation

- 用 `git show`、`git log -S`、`git blame` 核对历史，没有复制或编译历史工程。
- 更新热源文档，直接使用 `PortKind.AIR_FACE`、`PortKind.RADIATION_LOSS` 和 `EmissionPort` 解释数组计数；修正本次新增容量常量的Javadoc措辞。
- 本轮只改注释和文档，执行 `git diff --check`；未重跑行为测试。四向供热的14项通过记录属于前条实现验证。
