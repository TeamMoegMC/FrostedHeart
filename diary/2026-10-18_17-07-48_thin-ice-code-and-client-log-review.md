# 薄冰代码检查与最近客户端日志复核

- Time: `2026-10-18 17:07:48 +08:00`
- Author: `OpenAI Codex；代码检查与验证`
- Status: `completed`
- Scope: `薄冰代码、2026-10-18 16:39–16:55 runClient 日志、普通踩踏目标与专项回归`

## Completed

- 检查踩踏概率/冷却、冲击、六面任意邻水、相变温度读取、SavedData 生命周期、模型与生成器的一致性。
- 修正 `ThinIceBlock.stepOn` 的单层叠冰误判：只接受与实体 `getOnPos()` 实际支撑位置一致的接触，避免 vanilla legacy 采样把普通踩踏传给下方完整薄冰。
- 在 `ThinIceGameTests.ordinaryContactDoesNotPassThroughSingleLayerIce` 使用真实移动/碰撞路径覆盖上下叠冰，并检查直接踩完整薄冰仍可开裂。
- 复核 [客户端日志](../run/logs/latest.log) 和 [debug 日志](../run/logs/debug.log)。会话 16:39:01 启动，16:55:18 开始正常退出，16:55:23 所有维度保存完毕，16:55:28 结束工作线程；没有薄冰相关报错或新崩溃记录。
- 文档影响：更新 [薄冰说明](../docs/climate/thin-ice.md)，向 [讨论](../discussion/2026-10-17_23-27-52_ice-caves-and-thin-ice.md) 追加代码检查和日志观察，明确不等同于人工试玩通过。

## Decisions

- 只修正新踩踏逻辑的实际接触目标，不改变确认的概率、冷却、冲击或相变参数。
- 最新客户端 18 条 ERROR 的内容与前一轮 `2026-10-18-1.log.gz` 一致：15 条配方解析错误，以及 Embeddium 兼容提示、剧情初始化缺失、FTB Teams 玩家状态错误。薄冰落地 Mixin 应用和事件订阅成功，未将这些已有错误算作薄冰新增回归。
- 旧版 `LayeredThinIceBlock.skipRendering` 经 `IceBlock -> HalfTransparentBlock` 也会隐藏同方块的较矮邻接侧面；本次检查确认它早于碎裂改动，作为单独遗留问题记录。

## Validation

- Java 17：`runGameTestServer '-PgameTestNamespaces=frostedheart_thin_ice' '-Dnet.minecraftforge.gradle.check.certs=false' --offline --no-daemon --console=plain` 编译与运行通过；17:07:43 报告 8/8 必需测试通过，BUILD SUCCESSFUL，见 [测试日志](../build/thin-ice-review-gametest.log)。
- 27 个裂纹叠层 JSON 均为 translucent、颜色 `66ffffff`；两类 blockstate 引用的模型均存在。
- `git diff --check`、`git diff --cached --check` 通过。此前 110 项回归结果沿用其原记录，本轮没有将其描述为对最新补丁的复跑。

## Remaining

- 客户端唯一资源加载在 16:40:08，早于 16:51 的裂纹调浅；这次日志不能验证调浅后的画面，也没有覆盖本轮踩踏补丁的客户端运行。
- 16:41:22 有一次 4429 ms / 88 ticks 落后，紧接保存区块记录。日志不足以归因，大片冰面的性能仍未实测。
- 不同厚度分层薄冰的既有侧面剔除问题待单独处理；音效、透明排序与多人表现仍需实际试玩。
