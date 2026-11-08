# 红外慢响应不再被周期请求编号淘汰

- Time: `2026-11-06 22:19:45 +08:00`
- Author: `OpenAI Codex / GPT-6; primary engineering agent`
- Status: `completed`（请求状态、回复协议、Forge 回归和生产打包；未进行实际跨国链路测试）
- Scope: 红外客户端轮询与事务完成、C2S/S2C 回复、`InfraredRequestState`、网络回归及当前文档

## Completed

- 用户询问海外连接国内服务器时网络卡顿是否会导致没有红外热色。源码确认存在相关时序缺陷：首个 full 未开始接收前，41..59 tick 后重试会递增 requestId；稳定轮询也每 40 tick 更换 requestId。响应若持续比该窗口更晚到达，会被 `updateData()` 的编号检查丢弃，导致首次热色饥饿或后续热色停更。
- 该条件取决于请求到完整消息可接收之间的耗时，包含往返、排队、服务与传输，不能仅凭跨国连接或几百毫秒 ping 认定已经触发。没有将源码时序反例当作玩家现场的实测原因。
- 新增无客户端类型依赖的 `InfraredRequestState`，只保存当前编号和 pending 状态。客户端同一时刻仅允许一个未完成请求；经过时间、周期轮询和移动不会替换它。移动在响应完成后合并为下一次当前窗口请求，分片保持至 LAST。
- `InfraredCapture` 返回 null 时，C2S 处理器通过现有 S2C 类型发送 ACK。响应旗标使用原空闲 bit 8；ACK 只结束等待，不提交或清空显示。其余 wire 字段回显请求携带的已提交 baseline，不制造新温度/覆盖。
- 关闭、世界 reset、配方失效仍取消 pending；旧编号无法结束新请求。意外不匹配的 delta 会通过 `invalidateDisplay()` 释放旧 pending 并请求新 full，避免等待无法完成。
- 保留原 40 tick 错峰轮询；原 41..59 tick 仅用于已收到回复、但仍未取得可用 full 基准后的下一次尝试，不再作为淘汰慢响应的超时。
- 生产包仍使用工作区当前 `0.7.9` 版本；本任务未修改版本或替换运行实例。

## Decisions

- 使用单个未完成事务和明确回复完成，避免慢连接中不断排入重复 full、丢弃慢包，或在多个不同 delta baseline 间交叉提交。
- 此修复需要客户端和服务端都实现回复完成合同。只有服务端更新或只有客户端更新不构成本次完整修复；没有新增连接权限或角色条件。
- Documentation impact: 更新 [data-lifecycle-and-integration.md](../docs/climate/data-lifecycle-and-integration.md) 与 [world-climate-and-temperature.md](../docs/climate/world-climate-and-temperature.md)，明确 pending、ACK、移动合并、重试含义和配套端要求。
- 配套整合包仓库与配置没有变更。

## Validation

- Java 17，当前工作区的独立输出/项目缓存，运行 `assemble runGameTestServer`，namespace 为 `frostedheart_infrared_network`；`All 4 required tests passed`，`BUILD SUCCESSFUL in 2m 41s`。日志：`build/infrared-latency-validation-20261106.log`。
- `InfraredRequestGameTests` 验证 90 游戏 tick 延迟的首个 full 在 40/60/80 tick 轮询尝试后仍被接受、ACK 编解码保持 baseline 并释放后续请求、取消后的旧回复不结束新请求，以及三个分片保持原编号至 LAST。这些是生产请求状态与真实 packet 编解码的 Forge 回归，没有实际延迟注入 TCP 或客户端 GL 画面。
- 生产 JAR：`build/infrared-refresh-20261105/libs/frostedheart-1.20.1-0.7.9.jar`，14,595,398 字节；Manifest 为 `1.20.1-0.7.9`。新增状态类存在，GameTest 未进入生产包。`javap` 核对客户端 pending/tryBegin/complete 及服务端 ACK 调用存在。
- 沿用已有独立构建设置与 deposit 未完成夹具排除；未改变项目构建约束、增加 JUnit 或重复热模拟负载测试。本次修改文件 `git diff --check` 通过。

## Remaining

- 修复包尚未替换实际客户端或服务端。真实海外网络、带宽受限或丢包下的端到端首包时延和客户端连续画面尚未实测；不能据此断言任何跨国链路都能达到固定更新时间。
