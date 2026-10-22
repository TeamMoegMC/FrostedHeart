# 温泉调试定位

- Status: `Partial`（源码与 Minecraft 1.20.1 API 已核对；编译及 3 项定向 GameTest 已通过；整合包长距离搜索与交互体验待实际观察）
- Last verified: `2026-10-22`
- Scope: 管理员在指定维度内定位真实存在的温泉流体，不判断自然生成成功率或温泉回暖效果
- Code anchors: [`HotSpringLocateCommand`](../../src/main/java/com/teammoeg/frostedheart/infrastructure/command/HotSpringLocateCommand.java) 的 `command`、`Search.tick`、`findInSection`；[`FHFluids.HOT_SPRING`](../../src/main/java/com/teammoeg/frostedheart/bootstrap/common/FHFluids.java)
- Validation anchors: [`HotSpringLocateGameTests`](../../src/gametest/java/com/teammoeg/frostedheart/infrastructure/command/HotSpringLocateGameTests.java)，namespace `frostedheart_hot_spring_locate`，batch `ice_cave`

## 命令

权限为 `CommandSourceStack.hasPermission(2)`，与仓库管理员调试命令一致。推荐使用 `/fh` 前缀；`/frostedheart` 和 `/twr` 使用同一命令树，不注册独立 `/locate`，保留原版结构/群系定位。

| 命令 | 行为 |
|---|---|
| `/fh locate hot_spring` | 默认半径 `16` 区块，只搜索已加载区块 |
| `/fh locate hot_spring <radius_chunks>` | 显式指定半径，只搜索已加载区块 |
| `/fh locate hot_spring generate` | 默认半径 `16` 区块，显式加载/生成目标地形后搜索 |
| `/fh locate hot_spring generate <radius_chunks>` | 显式指定加载/生成搜索半径 |
| `/fh locate hot_spring status` | 查看当前维度、中心、模式、扫描/跳过/超时数量 |
| `/fh locate hot_spring cancel` | 停止服务器当前定位任务，释放本命令区块票据 |

半径为含两端的整数 `0..32`，单位是区块，以执行时所在区块为中心形成方形目标范围，目标数为 `(2r+1)²`；默认 `r=16` 共 `1,089` 个，最大 `r=32` 共 `4,225` 个。`r=0` 只检查执行者所在区块。目标按从中心向外的方形环枚举，命中后立即停止，结果不承诺是欧氏距离最近的一座温泉。

起点固定在执行时位置，搜索覆盖该维度完整建筑高度。玩家可先进入冰洞再运行；也可用 `/execute in <dimension> positioned <x> <y> <z> run fh locate hot_spring ...` 指定维度与中心。命令不按冰洞群系筛选，也不限制自然生成的 Y 范围，因此旧世界自然泉、手放源流体、流动温泉和高处温泉均可检出；普通水不会命中。

结果给出实际温泉流体格的 `x,y,z` 和维度。点击绿色坐标只填入 `/execute in <dimension> run tp @s <x+0.5> <y+1> <z+0.5>` 建议，需要玩家确认执行；该位置在命中流体上一格，不代表经过可通行路径或落脚点检查。定位本身不传送玩家。

## 扫描与加载边界

普通模式仅调用 `ServerChunkCache.getChunkNow`，不会为定位读取磁盘旧区块或生成新地形。即使某区块曾经生成但当前未加载，也计为跳过。已加载性按实际扫描当时判断；任务不固定只读模式区块，过程中卸载的区块可被跳过，未扫描不代表不存在温泉。

`generate` 是明确会改变存档地形的调试模式：每次仅为一个目标调用自有 `fh_hot_spring_locate` 区块票据，`addRegionTicket(..., radius=0, ...)` 使原版区块管线推进到 `FULL`，随后在后续 tick 使用 `getChunkNow` 检查完成情况。不会调用同步等待的 `getChunk` 或 `getChunkFuture`。原版生成所需的邻区依赖也可能被加载或推进，尤其在未探索区域；目标半径并不是所有受生成依赖影响区块的严格外界。取消不撤销已经产生的地形。

全服只保留一个定位任务，防止多个管理员任务叠加生成。每 tick 最多处理 `16` 个目标区块、检查 `64` 个 section，`LevelChunkSection.maybeHas` 跳过调色板内不含温泉的 section；只有命中候选 section 才逐格找实际坐标。扫描存在的流体状态，源与流动形式均通过 `isSame(FHFluids.HOT_SPRING.get())` 匹配；不建立世界级温泉索引，也不依赖新版本生成记录。

单个生成目标等待最多 `600` tick（20 TPS 时约 30 秒），超时计数后继续下一个；全任务最多 `12,000` tick（20 TPS 时约 10 分钟）。每 `200` tick 返回进度。取消、完成、玩家退出或切换维度、服务器停止均释放自有票据。世界边界外的目标跳过；结束信息区分实际扫描、未加载/边界跳过与超时，不能据未找到推出整片世界没有自然温泉。

## 验证边界

定向 GameTest 覆盖普通水排除、真实源/流动温泉识别、负区块边缘及高/低 Y 坐标换算、有限环范围和不重复、只读任务分 tick 且不加载缺失区块、显式生成首 tick 让出、权限和原版命令根保留。2026-10-22 随本轮 28 项测试一起通过，日志 `build/ice-caves-feedback-tests.log`；这不等同于完整远距离生成任务和点击传送交互已经实测。

仍需实际整合包确认：新地形长任务的生成耗时与取消体验、完整生成后命中自然泉、玩家点击坐标建议，以及读旧自然泉/手放泉的端到端操作。定位命令不改变温泉生成参数；生成机制与温泉稀少问题的调查见[冰洞当前实现](ice-caves.md)。
