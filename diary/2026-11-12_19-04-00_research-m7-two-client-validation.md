# M7 两个真实客户端的同队原型与重连验证

- Time: `2026-11-12 19:04:00 +08:00`
- Author: `Codex / OpenAI；客户端部署与验证助手`
- Status: `completed`（本条限定 M7；M5 单客户端入口与控温炉仍待执行）
- Scope: 固定 M5 生产包上的 M4 同队研究；独立 Host / Peer Prism 测试实例，未操作用户 Development 实例和旧存档。

## Completed

- 两个 Minecraft JVM，通过真实 `127.0.0.1:65254` TCP 连接；Host 保留集成服务器，Peer 真实登录、退出、重连、再退出。采用离线测试名字、集成 published 状态和回环 listener，不开启局域网广播，不涉及 Prism 外网。
- 使用真正 FTB Teams provider 和正常队伍命令，在任何研究开始前组队；服务端两个玩家的 `CTeamDataManager` holder 一致。Peer FTB selfTeam ID 与 Host 服务端 FTB 队伍一致。
- Host 正常绘图台研究入口和风道按钮；Peer 收到同一 run，正常生存背包 2×2 合成格由 C2S 放入三项材料、取出原型并正常放置，EntityPlaceEvent 的玩家为 Peer。生存模式只用于真实背包界面，世界、材料、起始知识和无敌状态均为夹具，不宣称生存进度。
- Peer 退出后，Host 真实右键风箱，正常服务器 tick 延迟 **10 tick** 变成 Charcoal Pit 正常砂质鼓风口模型；Host 进行服务器 **60 tick** 新鲜现场观察，消除该 run 的 proof 并完成。观察通过真实 ObservationActionPacket，不声称本段存在按住 N 的记录预览。
- 首轮成果事件数 **1**，两个正式配方有权限、临时原型无权限；Peer 重连后收到 completed 阶段和两项正式配方权限。
- 为第二轮显式使用遗忘成果夹具；正常再开始、再解风道。Host 与 Peer 重复发送 expectedStage=airflow 的推进，Peer 追加旧首轮 COMPLETE；第二轮停在尚未满足的 trial，首轮奖励总数仍为 1。正常放弃第二轮后，两端临时/正式权限均与遗忘状态一致。

## Decisions

- 本轮原 helper 错把 FTB 入队参数写成显示队名 `M7Study`。邀请实际送达（18:49:50），错误 join 在 18:49:58 返回找不到玩家/队伍；未假设已经入队。
- 精确定位 Peer JVM `19240` 的窗口，通过定向 Windows 消息打开真实聊天并输入 `/ftbteams party join M7Host`。18:54:12 两端真实日志确认加入，后续正常运行至通过。助手源码现已修为邀请者玩家名；**本轮运行 JAR 仍是原 helper，原结果中的错误命令字段保留，没有改写原始结果**。这一次实际聊天纠正和关闭 Host 遮挡的任务书界面为本轮人工式辅助操作。
- 客户端 `CClientTeamDataManager` holder 是 `UUID.randomUUID()` 创建的本地容器，reset 也重新随机；不可与服务端持久 holder ID 比较。原结果分别保留两个字段，只将服务端共同 holder、客户端真实 FTB 队伍以及实际研究/权限投影作为相应证据。没有为此改生产实现。

## Validation

- 固定生产 `build/m5-artifacts/frostedheart-1.20.1-0.7.10.jar`：**14896430 bytes**；本轮 helper **80800 bytes**；两端使用同一固定 M5 KubeJS、fhresearches 与 FTB 任务书快照。M5 固定后未再次复制工作区变化。
- 原始会话：`The-Winter-Rescue-Research-Playtest-20261112/.minecraft/build/m7-session/session-1794480236020`。
- 已归档 [host-result.json](../build/m7-client/session-1794480236020/host-result.json)、[peer-result.json](../build/m7-client/session-1794480236020/peer-result.json)、两端 latest.log、各阶段状态与 12 张截图至 `build/m7-client/session-1794480236020/`。两端均 `status=passed`，首轮 `a4ce8b20-37da-4f29-993f-c1fd8327c8d6`，第二轮 `60591f69-1bb4-45c1-ade9-84b6abc8e081`。
- 已打开核看 Peer `02-peer-real-crafting-grid.png`（可见 2×2 实物合成格及原型输出）、Host `04-host-blow-normal-tuyere.png`（正常模型、Peer 已退出）。截图是游戏渲染结果，未修图。
- 仅助手源 `src/m3c/.../M7ClientValidation.java` 的 join 参数改动及本条新增日记；未改生产源码/数据。没有 Git 暂存、提交、推送；没有额外 Gradle。实现行为未变，living docs 无需更新。

## Remaining

- M5 单客户端完整入口、试验、生产、分支、JEI 和实际教程加载另行验证；本条通过不代替它们，也不覆盖完整采矿/加工生存链和物理人手输入体验。
