# 统一自然休眠冷却与既有随机相变入口

- Time: `2026-09-14 18:16:17 +08:00`
- Author: `Codex; OpenAI GPT-6; primary engineering agent`
- Status: `completed`
- Scope: `空气/材料休眠、H/潜热、存储/恢复/编辑、随机相变、红外同步；受控功能与成本验证`

## Completed

- 实施[统一休眠计划](../plans/2026-09-14_16-22-45_thermal-unified-dormant-natural-cooling.md)。`DormantThermalCooling`共用显热半衰期，平台按热流推进并停于世界转换端点；活动arena与休眠共用law的branch/能量边界判断。
- v4保存Air自然基准、材料H/branch/state/law及实际时间。同龄用scalar，部分编辑才分配逐记录ticks；保留COW隔离和局部merge。读取/保存不推进权威H，部分Air capture保留未采样范围并避免重复保存量化漂移。
- 主线程immutable `DormantMaterialCut`携带时间/自然/速率供worker恢复；live迁移不重复冷却。普通编辑先按保存law结算，物质量交接的点自然温度与休眠Section自然温度分开，避免混用。
- 复用原随机tick和地表冻结；`GAMEPLAY / DEFERRED / CHANGED`避免跳过潜热及误停非相变回调。成功转换通过原APPLYING/Chunk钩子交接H/tick，没有Page或伪ACK需求。
- 编译补齐原生雪层到Air、源岩浆到玄武岩、流水冻结和配方到Air边；不能编译为物理边的非互逆配方保留正式玩法。tracked雪层作为整个当前物体处理，不新增逐层物质量模型。重载时只复算随机资格变化涉及的已加载Section。
- 红外保留渲染流程，协议新增storedSampleTick与LAST基准；双时刻量化比较使时间冷却产生增量。复用scratch和同龄显热因子，不增加观察者缓存或额外窗口数组。
- 删除sourceSustained/save-time衰减及其七Section刷新链，删除旧接管查询和整套无调用PhaseCandidates发布数组/构建scratch。无新Mixin、调度器、候选位图或旧版本兼容reader。
- 生产Java净增85行；新增数值工具68行。测试和文档不包含在该数字中，行数不是性能证明。

## Decisions

- 保持现有随机更新机会，不主动加载Chunk或启动Page，不回放历史天气/多个物态阶段。source拆除后，正常参与原更新的位置仍可转换；停服务器不计现实时间。
- 休眠显热统一半衰期，不承诺不同材质的离线物理散热速度；潜热/目标law能量交接仍保留。
- 源码复查发现部分原生/单向转换原本没有物理law，补齐原生和消失阶段，并显式保留非互逆配方玩法，避免按“有材料H就必须有物理相变边”误禁用已有功能。
- Documentation impact: 同步四份climate living文档、配置注释及主/子plan。design和伴随整合包未改动。

## Validation

- Java17 `runGameTestServer --offline --no-daemon --console=plain -Dnet.minecraftforge.gradle.check.certs=false` BUILD SUCCESSFUL，82/82通过：[日志](../build/dormant-final-validation.log)。包含原74项回归和8项新增休眠测试。
- 数值覆盖同半衰期、保存无漂移、同温潜热零热流、反向退出平台与独立细步积分、长期步长有界；生命周期覆盖COW/异龄/NBT、无Page物理转换、显式场与delta区别、原生雪层/岩浆、流水目标、红外时间增量和温度计同源。
- 1/8 Section × 1/64条记录，每组8192次直接入口调用：四组测量均0 bytes/attempt。p50为649.61/596.88/94.92/102.34 ns，p95为743.75/670.31/98.83/117.19 ns。顺序/JIT会影响结果，不能据此声称更多Section更快。首次COW+异龄写入224/1536 bytes，单列报告。
- 4096条记录、1024次真实setBlockState夹具：p50 981.25 ns，p95 1331.25 ns，24.1484375 bytes/call；包括真实Minecraft调用路径，不宣称这条完整路径始终零分配或相对历史运行必然加速。
- 修复测试夹具：恢复性能测试模板外方块/存储；岩浆测试遵守Y>-55；拓扑churn先通过真实PhaseIntent建立待ACK请求并验证requestSequence，继续原600tick及每21tick几何变化。早期失败是尚在9°C潜热平台，不能等同于已生成相变请求。
- `git diff --check`通过。未恢复普通JUnit源集中此前删除的几何/旧协议兼容类型；本轮未声称整个普通JUnit源集通过。未重复运行无关GPU像素测试，shader/Mixin渲染代码未变。

## Remaining

- 完整真人/机器人多玩家MSPT、网络总量、长期retained heap未测；1/8个区域的受控入口测量不是完整联机压测。继续由主材料计划跟踪该验收，不宣称全局最佳性能。
- v3及更旧热记录不会读回；世界方块仍保留。异龄时间数据最坏增加8 bytes/材料记录（4096条约32 KiB），还需计对象头和COW峰值。
