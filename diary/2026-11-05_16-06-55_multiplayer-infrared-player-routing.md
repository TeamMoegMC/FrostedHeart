# 多人红外视野的玩家请求与回包路径核对

- Time: `2026-11-05 16:06:55 +08:00`
- Author: `OpenAI Codex / GPT-6; primary investigation agent`
- Status: `completed`（源码路径检查；未完成真实多客户端联机验收）
- Scope: 红外客户端开关、C2S/S2C、温度查询、共享编码缓冲、能量塔温场

## Completed

- `FHKeyHandler.onClientKey()` 仅按 LDLib 加载状态和本地按键调用 `InfraredViewRenderer.toggleInfraredView()`；开关和请求编号属于各客户端 JVM 的静态状态，没有队长、房主或 OP 判断。
- `FHRequestInfraredViewDataSyncPacket.handle()` 通过 `Context.getSender()` 获取请求者，在服务器主线程生成其快照，再使用 `sendPlayer(player, ...)` 回给同一玩家。`CBaseNetwork.sendPlayer()` 使用 `PacketDistributor.PLAYER.with(() -> p)`，没有把接收者替换成队长或世界所有者。
- `InfraredCapture.capture()` 按请求者所在 `ServerLevel` 和坐标确定窗口；`MinecraftThermalInput.active()` 按维度世界对象查找热运行时；解析温场也是每个 `ServerLevel` 的空间索引，不按请求者团队筛选。
- `GeneratorData.publishGameplayHeat()` 中的 `owner` 用于温场来源键及替换/删除，不作为红外读取者身份限制。
- 全局共享 `InfraredCapture` 是服务器主线程顺序复用的工作区。响应的 presence/field 数组按需复制，编码分片通过 `ByteBufUtil.getBytes()` 独立保存；没有发现后一个玩家请求覆盖前一个响应的直接路径。
- 前一轮修复移除了客户端温度覆盖与开关的耦合，适用于普通队员的客户端；只更新服务端不会更新其他玩家客户端中的旧开关逻辑。

## Decisions

- 当前代码未发现“仅队长/房主可打开红外”的身份限制，不能据此证明玩家报告的联机故障已经全部消失。
- 无覆盖判断可能造成旧版的显示差异，但没有对应联机现场证据，本次不把它断言为那条历史反馈的原因。
- Documentation impact: 未修改实现或网络合同，无需修改现行系统文档；本次验证边界记录于此。

## Validation

- 只读追踪 `FHKeyHandler` → `InfraredViewRenderer` → `FHRequestInfraredViewDataSyncPacket` → `MinecraftThermalInput.gameplayInfraredSnapshot` / `InfraredCapture` → `CBaseNetwork.sendPlayer` → `FHResponseInfraredViewDataSyncPacket`。
- 核对 `MinecraftGameplayFields`、`GeneratorData.publishGameplayHeat()` 与 `InfraredBrickCodec.Builder.finishParts()` / `flushPart()`。
- 现有 `ThermalInfraredGameTests` 的 observer 为 `FakePlayerFactory.getMinecraft(level)`；其快照/codec检查不能充当队长与普通成员的真实多客户端联网显示验证。本次没有运行新测试、构建或替换游戏 JAR。

## Remaining

- 尚未用修复版的多个真实客户端连接同一专用服务器，验证相同位置下队长/普通队员、不同开启顺序、独立关闭、重新连接和实际热色同步。历史反馈是否另有原因仍须现场复现。
