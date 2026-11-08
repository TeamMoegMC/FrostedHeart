# 热存档局部捕获与Brick查询索引

作者：Codex。日期：2026-10-12。状态：本轮实施与验收完成。

用户要求按证据推进，性能优先、最小复杂度。先保留存档格式与物理精度，只处理确定的冗余工作。

两个生产文件变化：`QueryPublication.ReadCursor.captureAir`按Page组件选原区域，`AirFieldCheckpoint.Capture`使用原系数地址收集恢复值，去掉保存一个Page时合并完整区域的布局/系数。`AirFieldCheckpoint`构造64个Brick头和保序next数组，休眠/恢复采样只查对应Brick；两个数组计入backing、不写盘。无新增后台线程、周期全场复制、缓存系统、热参数或保存时间契约。

增强已有真实温差GameTest：同一真实sampleTick的已完成快照与发布场逐点比较，两个阶段各16480点最大7.105e-15C；保留现有1/16C省略阈值和一轮滞后1C目标。没有JUnit或人工热状态。

验收：候选6个JVM13项正常退出；另1个旧代码JVM实际保存用于新JVM读取兼容链。FUNCTIONS生产5、SHARED_GRID读者/phase2、温差1、旧格式读取1、原生初始/构建/返回3、4000tick真实容量1。粉雪7017供热tick和0.475328/0.576372C恢复差异保持之前记录。两次原代码容量控制通过。

失败完整保留：错误namespace导致无必测；第一次候选没观察到pending卸载，第二次没观察到维护延期。未降低断言或人为减慢worker。第三次完整容量验收通过，普通窗口0热等待、22tick恢复、Root66.795MiB→0。

统计修正：两次覆盖失败停服阶段各新增59个checkpoint，剔除出工作负载对比。平均分配基线172889–173893B、候选156250–157071B；P99基线828456–828648B、候选513736–513752B，最终对照减少9.62%/37.99%。精确单位与值以JSON为准。末次基线快照平均0.672ms与候选0.713ms、整体server P9925.350/31.251ms不支持稳定速度收益，默认未推广。没有把首轮102ms峰值当成被确定消除的CPU成本。

源码与配置有精确SHA256备份；所有测试进程结束，common配置、server.properties和未改facade均恢复/保持初始哈希。既有脏工作保留，没有提交/发布/CT2激活。

文档影响：更新`docs/climate/continuous-air-runtime.md`中checkpoint真实实现、索引内存计量与验收限制。计划及报告记录停止边界：先不引入新编码/float或构建缓存，后续需独立编码/恢复CPU证据。

报告：`build/checkpoint-local-final-report-20261012.json`；历史：`build/checkpoint-local-runs-20261012.json`；独立源码差异：`build/checkpoint-local-final-source-diff-20261012.patch`。
