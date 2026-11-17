# 按需粗档摘要性能补测

- Time: `2026-11-07 22:19:00 +08:00`
- Authors: `OpenAI Codex / GPT-6; primary measurement agent`
- Status: `completed`（性能补测完成；未证明稳定整体提速）
- Scope: 同一真实双基地负载的改动前后运行代价；不修改热模型
- Related: [实现记录](2026-11-07_16-00-11_thermal-minimum-summary-on-demand.md)、[统一入口](../docs/climate/thermal-validation-baseline.md)

## Verified Current State

- 上一轮完成数值/生命周期验证，但没有提供性能前后对照。
- `minimum-demand-r3/compiled` 与 `unified-replay-r6/compiled` 都已存在；完整产物间另有红外/城镇等同期变化，不能直接归因。
- 复用当前产物和同一GameTest夹具，只将本次8个生产类族替换为已编译旧类族组成对照。所有资源、其余生产类、配置和观察工具一致，不复制历史源码或重新编译旧版。

## Steps

1. 给现有入口增加已有编译输出选择及原生线程周期观察开关；每次运行保留独立世界/日志/JFR。
2. 固定世界预设、气候记录、4GiB堆、5个可见处理器与原配置，顺序运行旧、新、新、旧四次`normal-load`。
3. 比较同一2400tick窗口的工作线程分配、原生线程周期、JFR任务活动耗时及主线程/服务器分位延迟；核对任务数和服务有效性。活动耗时不冒充精确CPU时间。
4. 发布每次原值与重复间波动，不用不同环境的旧日志计算收益；更新文档与新日记。

## Outcome

- 平坦世界的首次旧版试跑`minimum-perf-before-1`完成但服务断言失败，降粗及服务违规记录保留，不用于提速率。
- 主对照恢复负载原来的普通世界预设，完成旧、新、新、旧四次独立JVM，均通过、热错误0/root0、查询12000、无降粗。生产内核、预算、负载与断言未追加修改。
- 入口支持已有编译输出和原生线程周期开关；共用测试/资源，3741个生产类仅8个类族/26个class文件有差异，无关同期变动一致，未重编旧源码。
- [实测摘要](../docs/climate/validation/thermal-minimum-summary-performance-2026-11-07.json)：worker分配中心值少2.14%、登记峰少1.04%、细档推进活动耗时少5.54%，全部区域时间片耗时多5.10%、线程周期多8.85%，server P99未改善。重复波动大，不具备稳定整体提速资格。
- 更新[当前运行说明](../docs/climate/thermal-runtime-control.md)和[基线入口及数据解释](../docs/climate/thermal-validation-baseline.md)，新增性能日记，保留旧数值验收与历史“未测性能”记录。
