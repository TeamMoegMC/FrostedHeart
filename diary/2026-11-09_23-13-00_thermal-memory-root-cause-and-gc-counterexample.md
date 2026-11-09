# 热内存根因调查与Full GC对照反证

- Time: `2026-11-09 23:13:00 +08:00`
- Author: `OpenAI Codex / GPT-6; primary investigation and implementation agent`
- Status: `partial`（新增缺陷修复和验证完成；原服OOM完整归因、实时性能未完成）
- Scope: 热输入就绪检查、协调轮执行时间、等待边界计账、真实专用服分配/GC/堆对照。没有修改采集脚本或地形生成算法。

## Completed

- 从已有live堆追到326个已就绪粗档任务、最早边界及119个合计58.9MiB的候选额度，未把READY快照单独当作调度饿死证明。
- JFR确认`SpatialSourceIngress.pendingFor → checkpointTarget`反复分配临时目标。改为复用worker已有解析对象，仅返回归属判断；保留保存所需的不可变身份路径。
- 稳定3ms前置耗时控制在旧实现120轮不推进。把选中工作的2ms起点移到前置扫描/选择之后，整轮仍由原共享额度计费；修复后3轮推进。
- 等待`BoundaryCandidate`只拥有边界额度；在选中后、实际`FrozenSources`创建之前登记源快照准备空间。58.9MiB是候选总额度，含真实边界，不能全数宣称物理堆节省。
- 继续确认旧回调引用泄漏已解除；最终堆没有遗留已完成`RebuildPreparation`。

## Decisions

- 区分真实强引用泄漏、累计临时分配、准入额度与推进性能，不把128MiB作为JVM堆硬限制。
- 不把消除某一分配路径或10项语义测试通过当作Full GC/OOM完整修复。
- 剩余热点包括原版光照、序列化、流体和本模组群系列fitness数组。这里只确认分配来源，没有发现它们异常保留历史对象的证据；没有顺带修改地形生成。
- Documentation impact: 更新[运行合同](../docs/climate/thermal-runtime-control.md)、[内存证据](../docs/climate/thermal-server-memory-evidence.md)及气候README，新增[结构化回执](../docs/climate/validation/thermal-memory-root-cause-2026-11-09.json)。design未改。

## Validation

- 必要增量编译及`build/thermal-root-cause-unified-final-20261109.log`：10/10通过。30,000次实际端口检查0B；21个完成准备对象回收；Page顺序、输入一次结算和暂停全部worker后的真实磁盘保存通过，后者14.5685ms。
- 初次控制使用`parkNanos`，不能保证3ms实际耗时，未作为最终控制依据。确定性控制改用实际计时区间。第一次统一9/10的失败是误用`independentCommits`断言，核实该计数的条件后改为`commitRevision`和tick，整组重跑。
- 两轮均Java21、4GiB、128MiB目标、1000ms/s、4热worker、3072营火、三名FakePlayer真实区块跟踪、9600tick、相同JFR设置。最终访问21983个不同区块位置，0OOM、0热worker错误、0保存错误。
- 匹配区间热worker分配速率208→85MiB/s，约降59%。负载期间自然Full GC 4→5，关闭各1次，诊断各1次；Full GC未消除。最终live堆约1.11GiB、474个受热区域仍停5920tick，输入9680tick。因此不记为实时性能或原服OOM修复通过。
- 早先启动的两补丁版本于tick0停止，以纳入确认后的截止时间修复，未计通过。原日志及失败回执保留在ignored build目录。没有制作发布包。

## Remaining

- 原服Full GC后仍保留约6GiB的实际对象归属尚未定位；现有本地复刻没有重现该存活量。
- 原服长时间GC停顿及本地受热区域的推进滞后不能宣布修复。本轮产出是已证实缺陷的最小改动、统一语义回归，以及不支持“已全部修复”的实际反证。
