# 暖石 Gate B 包观测器

- Time: `2026-08-29 16:23:04 +08:00`
- Author: `Codex; OpenAI GPT-5; primary engineering agent`
- Status: `completed`
- Scope: `暖石现有 Curios/container 同步的默认关闭客户端观测与 Gate B 实测准备`

## Completed

- 增加 `WarmStoneGateBPacketCounter` 和客户端命令 `/fh_gate_b start|status|reset|stop`，分别统计全部与暖石相关的 Curios Stack、原版单槽和原版整内容包。
- 在实际事件日志中记录 item、slot、初始化状态、内部温度和表面温度；汇总额外记录整内容包中的热库 Stack 数与 `probe_errors`。
- 通过可选 `SPacketSyncStackMixin` 观测 Curios 解码包，并复用 `ClientPacketListenerMixin` 观测原版包；Curios 缺失时插件跳过对应 Mixin。

## Decisions

- 观测器默认关闭且完全位于客户端，只读已经收到的 `ItemStack`。它不写 NBT、不初始化状态、不新增网络通道，也不改变现有同步频率。
- 同时保留全部包与暖石相关包的计数，避免把同时发生的其他容器流量误归因给热库。Curios 反射探针一旦不匹配即累计错误并在本次测量首次告警，后续包继续重试；实测只有在 `probe_errors=0` 时有效。
- 本工作只提供 Gate B 证据工具；没有实际存档的计数与 tooltip 新鲜度记录前，Gate B 仍开放。

## Validation

- JDK 17 `compileJava compileTestJava` 通过。
- `WarmStoneGateBPacketCounterTest`：`3/3` 通过。
- player/thermal/Curios 扩展回归：`49` suites、`255/255` tests 通过。
- `runClient` 完成开发客户端装载，客户端命令成功自动订阅，日志无 `InvalidInjection`、`InjectionError` 或新增探针异常；退出阶段仅见现有纹理管理 shutdown warning。
- `git diff --check` 在记录前用于发现并修正本次触及 Mixin 的混合行尾。

## Remaining

- 在真实服务端/多人路径执行静置、佩戴换热、普通库存换热和槽位移动矩阵，保存 `/fh_gate_b stop` 汇总与 tooltip 观察。确认 `probe_errors=0` 后才能判定是否关闭 Gate B，或是否确有必要设计定向同步。
