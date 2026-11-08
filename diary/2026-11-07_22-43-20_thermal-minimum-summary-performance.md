# 按需粗档摘要性能补测：未证明整体提速

- Time: `2026-11-07 22:43:20 +08:00`
- Author: `OpenAI Codex / GPT-6; primary measurement agent`
- Status: `completed`（选定负载性能测量）
- Scope: 热模拟按需摘要的真实负载对照、复用编译产物入口与结果解释；没有追加生产内核改动

## Completed

- 补齐上一轮遗漏的性能前后对照；[上一条实现记录](2026-11-07_21-45-38_thermal-minimum-summary-on-demand.md)中的“没有性能百分比”保留为历史事实。
- 给`scripts/run-thermal-baseline.ps1`及其init增加已有编译组件选择和原生线程周期开关。共用同一资源/编译测试，生产3741类仅本次8类族的26个class文件不同，未复制历史源码或重编旧版。
- 完成既有普通世界`normal-load`旧、新、新、旧四个独立JVM；[实测摘要](../docs/climate/validation/thermal-minimum-summary-performance-2026-11-07.json)保留每轮原值、中心值、配置和原始日志/JFR路径。

## Decisions

- 初次平坦试跑改变周边空气规模，旧版降粗并违反服务断言；原记录保留但不计提速。恢复该负载原普通世界预设，不提高预算或放宽断言。
- 仅测该代表性负载，不重跑29项矩阵。预先约定四轮后收口，不因结果不理想追加筛选性重复。
- 四轮没有证明稳定整体提速。分配中心值少2.14%、登记峰少1.04%；细档推进活动耗时少5.54%，全部区域时间片活动耗时却多5.10%、worker周期多8.85%，server P99中心值亦未改善。两次/版本的重复波动大，不能将这些中心值直接解释为因果效果。
- Documentation impact: 在[运行合同](../docs/climate/thermal-runtime-control.md)及[基线入口](../docs/climate/thermal-validation-baseline.md)补充测量和不确定性；新增JSON、完成[计划](../plans/2026-11-07_22-19-00_thermal-minimum-summary-performance.md)。模型、保存、配置默认值与design未变化。

## Validation

- 四次2400tick、120游戏秒、256篝火/256工作机器/100温度消费者/12000查询；配置、server.properties、气候输入字节一致。均1/1通过、热保存/worker错误0、关闭root0；服务年龄/几何/消费者违规0，所有采样无降粗，接受推进全部grid1。
- Java17.0.12，4GiB堆，5个可见处理器/4worker；两边JFR profile与Windows QueryThreadCycleTime均开启。ThreadMXBean CPU秒不用于结论；活动耗时包含执行中抢占/GC。
- 细档接受任务4299/4304/4299/4299；全窗活动耗时旧4.149/1.838s、新3.392/2.264s，周期旧41.161/18.528十亿、新37.621/27.350十亿，原值未隐去。400tick后诊断窗周期中心值多0.48%，仍没有稳定收益。
- 新旧热主线程P99中心值1.664→1.641ms；完整server P9942.152→57.338ms，仅为有明显波动的描述统计。
- 运行时生产类路径、堆与处理器设置核对正确；未运行旧源码编译。入口PowerShell解析、JSON与文档链接/空白、3741类产物差异收尾核对通过；48条采样状态的服务/降粗/工作限额违规均0。

## Remaining

- 本批不承诺明显提速。若继续做性能优化，需要先稳定测量环境，并优先依据构建/材料换热的实际成本选择改动；不把局部耗时中心值下降作为整体优化证明。
- 此次只有一个真实Forge负载与两次/版本；极端相变控制延迟、完整整合包、100联网玩家和强制粗档仍不在范围。没有新增温度曲线资格，原同帧数值验证范围保持。
