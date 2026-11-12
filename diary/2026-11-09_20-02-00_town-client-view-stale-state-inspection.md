# 城镇局部界面过期状态：同类问题检查

- Time: `2026-11-09 20:02:00 +08:00`
- Author: `Codex; OpenAI GPT-6; primary inspection agent`
- Status: `completed` for source/history inspection; the two additional defects remain unfixed and not reproduced in a live client
- Scope: 镇长印章各页面、城镇建筑界面、经营数据与物流的变化通知和客户端应用链路

## Completed

- 按用户“还有没有类似问题”的要求，检查“服务端事实已变化、客户端漏更新或本地草稿不收敛”这一类功能问题。沿用用户无需 build 的要求，未执行 Gradle、编译、打包或新游戏进程；未修改生产源码。
- 确认两类额外界面缺陷的确定性代码路径：调度回复内容相等时不回滚本地草稿；多人连续改名时本地 pending 等待已被覆盖的中间值。代码成立不等于已经完成竞态的实机复现。
- 政策提交与日结生效均有 `TownPolicyStateUpdatePacket`；日结历史和镇龄通过 `TownHistoryUpdatePacket` 更新，客户端 `TownHistoryModel.upsert` 有条数限制；资源脏键与运输/P2P绑定、滤镜摘要均有 `TownResourceUpdatePacket` 路径。普通建筑/居民变化有监听、Codec、应用与界面读取路径。未在这些已核对入口发现另一项与建筑注册相同的确定性漏发；不据此宣称城镇全系统无其他 bug。
- 仓库 `getVirtualItemMap` 的客户端失效嫌疑已排除：该 supplier 仅在 `ServerPlayer` 菜单中使用，客户端仓库列表另由 `WarehouseUpdatePacket` 和 `viewDirty` 管理。运输绑定修改也通过 `replaceAndMark` 或明确的 `markTransportStateChanged` 进入同步。
- Documentation impact: 修正 [town-model.md](../docs/town/town-model.md) 对“收到权威计划即可回滚”的过强表述，并标注两类当前已知限制及其验证范围。`design/`、整合包和既有生产修改保持原状。

## Decisions

- 第一类，岗位/住宅保障草稿：`TownStaffingPanel.syncDraft` 的 `serverPlan.equals(observedPlan)` 与 `TownHousingPanel.sync` 的 `plan.equals(observed)` 会忽略内容未变的权威回复。两种编辑 handler 本来均会在操作未改变服务端计划时向发起者返回计划；两个 `withTarget/withGuarantee` 则按当时容量限制目标。
- 具体顺序：客户端原容量 4、保障 2，开始编辑为 4；请求处理前服务端扫描后容量降至 2；服务端将请求限制为 2，与现有计划相同，返回保障 2；客户端仍认为该回复等于已观察计划而跳过清理草稿，页面持续显示保障 4。服务端实际采用 2。该序列不要求服务端计算错误。正常新增计划的上一轮修复不能覆盖这条回复处理路径。
- 第二类，城镇和居民名称：`TownNameEditor.syncAuthoritativeName` 仅在当前权威名字等于 `pendingName` 时解除等待，`TownResidentsPanel.syncNameEditors` 对 `pendingNameKey` 同样处理。两名玩家连续将同一对象改为 A/B，若客户端下一帧前已应用两次更新，或居民增量只发送合并后的 B，发起 A 的编辑器永远观察不到 A，继续显示 A；当前客户端数据和服务端实际名字均可已为 B。切页重建或改选居民能清除对应局部状态。
- Git blame：岗位 equality 条件来自 `d50c21cd3c`（2026-08-14），住宅来自 `0e66cb70d3`（2026-08-17）；两种名称 pending 判断来自 `20a2c95145`（2026-08-13）。这两类是既有局部状态问题，不将它们归因于 2026-11-08 的减少全量同步提交。

## Validation

- 阅读实际发送、接收和显示代码：`TownStaffingEditRequestPacket`、`TownHousingEditRequestPacket`、两种 PlanUpdatePacket、`TownStaffingPlan.withTarget`、`TownHousingPlan.withGuarantee`、两种调度 Panel；名称修改包、`Resident` setter 通知、`TownNameUpdatePacket`、两种名称编辑器。
- 核对 `TownSync.flushChanges`、`TeamTownData.publishDayEffects/applyHistoryUpdate`、`TownTransportService` 中的运输变化标记、`WarehouseMenu` 服务端 supplier 和客户端缓存、住宅/狩猎/矿业/货运建筑 setter 与界面 supplier。
- 使用 `git blame` / `git log` 核对引入时间；没有 checkout 或编译旧版本。
- 本轮未运行任何新增 GameTest 或图形客户端场景。上一轮 39 项和 844 帧验证不覆盖这里的容量变化与并发改名反例，不能沿用为这两类问题的通过证据。

## Remaining

- 优先修复两种调度 Panel 对内容相等权威回复的草稿回滚，并用实际客户端编辑过程中容量变化的场景验收。
- 修复两种名称编辑器对 pending 的解除条件，验证多人连续改名和居民更新合并。
- 本轮是有明确范围的同步/局部状态检查，未覆盖城镇全部业务规则和运行负载。
