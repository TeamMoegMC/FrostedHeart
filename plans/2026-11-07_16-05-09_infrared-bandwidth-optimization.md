# 红外带宽测量与增量优化

- Time: `2026-11-07 16:05:09 +08:00`
- Authors: `OpenAI Codex / GPT-6; primary engineering agent`
- Status: `completed`
- Scope: 红外请求、ACK、解析场显示去重、移动窗口重叠与验证
- Related: [带宽检查](../diary/2026-11-07_15-52-56_infrared-bandwidth-design-inspection.md)、[网络合同](../docs/climate/data-lifecycle-and-integration.md)

## Goal

减少未改变热色和移动时重复发送的数据，保持 0.25°C 显示合同、真实材料/解析场来源、删除通知和慢响应事务完成。用完整 C2S/S2C 与 Minecraft 压缩帧计量收益，并同时记录查询 CPU 和缓存代价，不宣称全局带宽最优。

## Verified Current State

- 当前/历史解析场 Page 每轮重发其 Brick，固定场也会重复；跨 Section 中心使整个 9³ 窗口 full。
- 请求固定携带 96B presence，ACK 使用通用回复头；既有 measureSurfaceRequests 遗漏 C2S 与 ACK，未经过 Minecraft 压缩帧。
- 已有单个未完成请求、LAST 提交、材料 epoch/休眠删除，以及统一最终显示纹理，必须保留。

## Decisions

- 先在当前工作区建立真实 Forge 的固定场、场变化/删除、移动、独立观察者等对照，不复制旧源码。
- 解析场使用有界的、按观察者最近一次实际发送显示保存的编码 Page 缓存。客户端携带最后提交 requestId；只有它与缓存及窗口身份匹配才逐字节比较，不能依赖概率哈希。缓存只加速显示，淘汰后回到完整字段发送。
- 候选缓存约束为全服合计最多 8MiB 计量负载、最多256观察者；按实际编码 Page 保存，不为每玩家增加完整温度镜像。容量和回退必须验证。
- 移动重叠时重定位已有 CPU mirror，补进入区域及已变化部分；LAST 后 GPU 按移动后的镜像提交。远距离无重叠、重置和失效仍使用 full。
- 最后收紧空/稀疏掩码、ACK和分包元数据；不以放宽温度精度换字节数。

## Steps

1. 完整包/压缩帧测量和优化前回归。
2. 编码 Page 精确判变及提交编号缓存，验证定值、量化内变化、增减温、删除和独立 baseline。
3. 窗口掩码/mirror 平移与进入区增量，验证方向、边界、远跳和中途失效。
4. 精简控制字段，回归慢回复、ACK、分包和退出删除。
5. 当前源码生产打包，同步 living docs，追加 diary 并记录实际测量边界。

## Validation

- 游戏状态由真实 Forge 世界与生产查询生成。统计 packet 正文、Forge/Minecraft 包和压缩帧；不将它称为 TCP/IP 抓包。
- 同时记录原始值重建一致性、查询耗时及缓存计量。采样和长期真实多人/客户端画面的资格分别说明。

## Outcome

已完成有界共享编码 Page 的精确判变、提交编号基准、窗口 overlap/mirror 平移、空/稀疏掩码、短ACK与LAST元数据。离线、换维度、Level卸载与停服释放缓存；miss/淘汰回到实际数据重发。

优化前两个针对重复数据的断言失败；最终20项Forge回归通过，生产assemble成功。固定温场24轮双向Minecraft压缩帧由250961B降至3456B（98.62%），移动一Section由5659B降至237B（95.81%）。同进程交替64次对照的查询中位耗时减少12.14%、分配减少21.79%；100个独立FakePlayer基准共享64个编码页，缓存计量622384B。

测量、场景与资格限制见[验证JSON](../docs/climate/validation/infrared-bandwidth-2026-11-07.json)；当前合同已写入[网络文档](../docs/climate/data-lifecycle-and-integration.md)与[世界温度](../docs/climate/world-climate-and-temperature.md)。详细交付见[开发日记](../diary/2026-11-07_21-50-55_infrared-bandwidth-optimization-delivery.md)。没有宣称全局最优、实际跨国链路或100完整玩家承载资格。
