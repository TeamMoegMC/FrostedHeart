# 红外带宽优化、重叠窗口与完整协议测量

- Time: `2026-11-07 21:50:55 +08:00`
- Author: `OpenAI Codex / GPT-6; primary engineering agent`
- Status: `completed`（实现、20项Forge回归、对照计量与生产打包；实际客户端/WAN范围见Remaining）
- Scope: 红外请求/响应/ACK、编码去重、观察基准、共享显示缓存、窗口平移与文档

## Completed

- 实施[计划](../plans/2026-11-07_16-05-09_infrared-bandwidth-optimization.md)。`knownDisplayRequestId`标识最后在LAST提交的数据；ACK不推进它。服务器只对匹配基准省略既有记录，避免取消/未收到的回复污染判变依据。
- `InfraredBrickCodec.Builder.finishPage()`生成Section局部的标准编码，排除隐含INVALID，精确比较最终0.25°C显示记录。整页相同直接回退writerIndex；部分不同只保留变化或显式删除的Brick。没有概率哈希或第二份物理温度权威。
- `InfraredDisplayCache`共享不可变编码世界Page；最多256观察基准、8MiB计量负载及估算metadata。旧版本按真实观察引用保留并计量，miss/淘汰重发实际值。离线、换维度、实际Level卸载及停服释放相关引用。
- `InfraredWindow`重映射presence/旧场footprint，并在FIRST原地平移现有direct mirror；进入区域初始化INVALID，LAST提交移动后的GPU图。任一轴无重叠、generation改变或基准失效仍走full。移动没有新增完整CPU温度镜像，仍需完整本地纹理上传。
- 掩码在空、稀疏Page索引差分、12-long完整表示中择小；ACK仅requestId与flags（小编号2B）；presence/场footprint/休眠版本只随LAST传输，保留FIRST/LAST与慢响应事务完成。
- 更新旧capture微剖析日志名称，明确其不统计C2S/ACK/压缩；新增独立bandwidth测量入口，计入两方向正文、实际Forge包、Minecraft压缩与长度帧。

## Decisions

- 温度精度、64格扫描和材料/解析场合成规则保持原合同。固定场即使重算，最终编码相同也只回复ACK；材料温度变更、场变更/删除与区域退出仍必须更新。
- 共享Page缓存属于派生显示加速，8MiB是计量上限而非JVM heap硬界。没有为每个玩家增加6MiB温度镜像，也不以缓存命中决定真实热活动。
- 本轮沿用当前工作区生产版本0.7.9，未改版本号、配套仓库、整合包配置或运行实例JAR。两端需使用本轮匹配协议。
- Documentation impact: 同步[网络与生命周期](../docs/climate/data-lifecycle-and-integration.md)、[世界温度](../docs/climate/world-climate-and-temperature.md)与[验证JSON](../docs/climate/validation/infrared-bandwidth-2026-11-07.json)。计划已标completed并写入结果。

## Validation

- 优化前：真实Forge固定场/移动两项断言按预期失败；同一工作区基准已接入提交token的测量管线，但尚无去重、平移或紧凑编码。日志：`build/infrared-bandwidth-before-20261107.log`。
- 最终：Java17、独立输出/项目缓存，`assemble runGameTestServer`，5个红外namespace的20项全部通过，`BUILD SUCCESSFUL in 2m 53s`。日志：`build/infrared-bandwidth-delivery-20261107.log`。沿用当前热验证对未完成deposit夹具的排除，没有引入JUnit。
- 覆盖恒定/量化内变化、改变/删除、独立基准、100观察者共享、缓存丢失/淘汰/释放、新进入热区和远跳、材料变化及保留解析场、三轴正负/混合/极限窗口每个texel、空/稀疏/完整mask、全窗口分包LAST、慢回复与旧区域删除。
- 固定温场24轮：完整双向压缩帧250961→3456B，减少98.62%；移动一Section：5659→237B，减少95.81%。压缩阈值256B，含Forge包装、Minecraft packet ID、CompressionEncoder与Varint21长度帧，不含TCP/IP或重传。
- 同一进程同一状态交替64次：未缓存/缓存查询P50为4930.5/4332.1µs，P95为9590.0/9186.9µs；ThreadMXBean查询分配为22504/17600B每次。它们是此场景的查询耗时/分配，不是纯CPU或总体服务器性能证明。更早不同运行的计时波动保留在各日志，没有合并为统一CPU提速率。
- 100独立FakePlayer基准共享64个Page，缓存计量622384B；100次稳定轮询没有温度数据包，只有ACK。它不是100完整网络玩家测试。
- 生产包：`build/infrared-refresh-20261105/libs/frostedheart-1.20.1-0.7.9.jar`，14610548B，Manifest为`1.20.1-0.7.9`。窗口/缓存生产类存在，`javap`确认平移和编码Page判变调用；GameTest类不进入生产JAR。本次修改文件`git diff --check`通过。

## Remaining

- 本地实例未自动替换或重启。真实客户端GL画面、跨国WAN/实际TCP重传，以及100完整玩家游戏承载未实测；当前结果不能宣称全局带宽最优。
- 解析场仍按查询重算，移动仍需本地纹理完整上传。此轮节省网络正文与控制字节，不将它扩大描述为省去所有计算或GPU工作。
