# 已有真实录制：保存峰值与常态捕获耗时

- Time: `2026-10-13 18:42:03 +08:00`
- Author: `Codex; OpenAI; primary agent`
- Status: `analysis completed`
- Scope: `最终真实客户端JFR的保存/快照毫秒口径；生产源码无改动`

## Completed

- 按实际场景gameTick16098–28098读取已有快照事件，成功活跃温场捕获371次mean0.538/P99=2.094/max17.318ms；峰值在开始后51tick预热阶段。
- 排除首400tick后357次mean0.456/P99=1.136/max1.696ms；已存休眠数据与空初始化不混入此统计。整份JFR含停服捕获，未将其523次总数冒充场景内次数。
- 两次自然保存调用49.410/28.249ms、完整tick51.720/30.990ms；最终强制flush调用151.256ms、完整tick172.836ms分别保留。
- 更新[结果报告](../build/online-entry-final-closure-20261013.md)和[当前文档](../docs/climate/continuous-air-entry-recovery.md)，补充最大值与通常捕获成本的区别。

## Decisions

- 本机场景的常态捕获成本可接受；自然自动保存的最慢完整tick略超50ms预算。强制flush是低频完成屏障，需独立评估，不能用它代替普通在线性能。
- 没有同场景无热模组控制或真实百人数据，不断言纯原版正常范围或扩服耗时。

## Validation

- 只读取已有真实JFR，无新Forge运行、JUnit、模拟温度或生产改动。[分布摘要](../build/online-entry-final-checkpoint-latency-20261013.json)和[逐次事件](../build/online-entry-final-checkpoint-latency-20261013.tsv)保留真实数据。
- JFR峰值17.318ms与内部计时17.335ms范围略有差异；事件wall不作为独占CPU毫秒。

## Remaining

- 沿用既有总计划：正式保存配置接入、多维度与更多真实客户端，重点观察普通在线完整tick和同时保存/卸载的峰值。
