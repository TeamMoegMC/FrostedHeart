# 红外带宽设计与现有测量边界

- Time: `2026-11-07 15:52:56 +08:00`
- Author: `OpenAI Codex / GPT-6; primary source inspection agent`
- Status: `completed`（当前实现检查；未实施带宽优化或宣称最优）
- Scope: `InfraredCapture`、`InfraredBrickCodec`、红外请求/回复/ACK、已有采样统计

## Completed

- 当前已有 0.25°C 显示量化、材料 Brick epoch 增量、INVALID/UNIFORM/INDEXED/RAW 编码选择、full 中省略 INVALID，以及单个未完成请求。它们减少部分数据或重复请求，但不足以证明实际链路带宽最优。
- 解析场命中的当前/历史 Page 使 `refresh=true`、`replace=true`、`changed=-1L`，重新发送该 Page 的 Brick，即使最终量化热色与客户端已有值一致。恒定能量塔场仍存在可确认的重复发送路径。
- 客户端跨 Section 中心，或服务端 `knownCenter != center` 时使用 full；没有复用移动前后窗口重叠部分的增量合同。full 仍会省略无效记录，不能据 729 Page 窗口直接认定每次都发送整个温度数组。
- 请求总是携带 12 个 long 的材料 presence（96 字节），场 footprint 非空时再携带 96 字节；分包携带重复头和 Page mask。ACK 复用通用响应头及请求 baseline，有进一步收紧编码的空间。
- 按当前编码字段手工计数：当相关 VarInt 均为一字节、无解析场 footprint 时，请求正文 119 字节，ACK 正文 28 字节；20 TPS、每 40 tick 一轮约 73.5 字节/秒。此数不含 Forge/Minecraft 封装、连接/传输开销或压缩，不能当作实际网络实测。
- 单个 Brick 的 INVALID/UNIFORM/RAW 正文分别为 3/5/131 字节；INDEXED 在既有 SimpleBitStorage 格式下按字典大小择优。这是局部编码取舍，不是全协议或网络层最优证明。
- `ThermalInfraredGameTests.measureSurfaceRequests()` 只在非 null snapshot 时累计 S2C packet 编码字节，没有统计 C2S、ACK 或 Minecraft 压缩后的链路字节；固定 tick 下的 100 次顺序 baseline 采样也不是实际多人连接带宽测量。

## Decisions

- 本轮回答性能确定性问题，保留生产实现；没有把此前请求正确性与慢回复回归通过当作带宽资格。
- Documentation impact: 当前系统文档没有声称红外带宽已达到最优；本次未改变行为或合同，无需修改现行说明。
- 判断优化价值时先关注重复发送的温度正文与移动 full，再评估小控制头；节省链路字节还需要结合压缩、服务端 CPU、缓存内存和首包时延。

## Validation

- 只读核对发送筛选、编码分支、请求/回复字段与现有采样函数，手工复算上述包体字节数。
- 本次未运行新性能测试、编译、打包或修改实例；不是实际网络流量或跨国线路测量。

## Remaining

- 尚无当前实现的完整上下行、Minecraft 压缩后字节和延迟分布对照；恒定场重复、移动窗口复用、精简 metadata 的收益与实现代价尚未完成测量或优化。
