# 暖石 Gate B 实测关闭

- Time: `2026-08-29 16:49:48 +08:00`
- Author: `Codex; OpenAI GPT-5; primary engineering agent, with user manual gameplay verification`
- Status: `completed`
- Scope: `暖石佩戴、普通库存和槽位移动的实际客户端同步计数与 Tooltip 新鲜度验收`

## Completed

- 在开发客户端连接集成服务端的真实收包路径完成三段 `/fh_gate_b` 测量，覆盖佩戴、普通库存、容器界面和槽位移动。
- 三段汇总均为 `probe_errors=0`。专用 Curios `SPacketSyncStack` 均为 `0`；热库状态实际通过原版 container slot/content 包到达本地客户端。
- 用户同时观察 Tooltip，确认表面温度显示未见陈旧或其他异常。

## Decisions

- Gate B 关闭。既有差量同步已满足首版需求，不增加热库专用、量化或限频同步。
- Curios 专用包计数为零表示本次场景没有发出该包，不是探针失败；原版容器事件与无错误汇总共同记录了实际使用的同步路径。
- 默认关闭的观测命令继续保留，供未来可复现的远端客户端显示问题诊断。

## Validation

- Run 1：`162.126 s`，slot `147` / thermal slot `146`，content `3` / thermal content `3`，content 内累计 thermal Stack `5`，约 `0.907` slot packet/s。
- Run 2：`135.113 s`，slot `84` / thermal slot `84`，content `0`，约 `0.622` slot packet/s。
- Run 3：`28.762 s`，slot `20` / thermal slot `20`，content `1` / thermal content `1`，content 内 thermal Stack `2`，约 `0.695` slot packet/s。
- 三段 Curios/thermal Curios packet 均为 `0/0`，`probe_errors=0`；没有 `20/s` 的每 tick slot 流，也没有持续 full-inventory 流。
- T11/T12 既有状态码与定向测试覆盖每个相关 Stack 每 cadence 零或一次 NBT 写回；本次仅更新验收文档，未修改代码。

## Remaining

- Gate B 无剩余项；后续从 T13 的 world runtime 工作继续。
