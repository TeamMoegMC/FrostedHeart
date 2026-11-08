# 玩家取暖装备、叠加需求与有限热库回暖

- Time: `2026-11-08 03:20:42 +08:00`
- Author: `Codex; OpenAI GPT-6; primary implementation and validation agent`
- Status: `completed` for the stated equipment fixes and Forge regression scope
- Scope: 主动取暖装备、五部位身体供热接入、暖石/热水袋、装备提示、相关测试及文档

## Completed

- 按用户选定的体验，让普通取暖装备在寒冷时有效回暖、普通环境趋于舒适体温，真正高温环境仍会过热。
- 煤手炉、暖贴、蒸汽瓶、氧气蜡烛、蘑菇培养袋和加热背心共用现有五部位积分中的37°C额外供热需求。保留冷态最大功率与普通燃料/放热/生长时间，不将身体温度直接重置为37°C。
- 背心在其他主动热源之后补热，并计入代谢/调节、Wet、辐射和着火。饰品背心先于原版手持槽被发现时也不会重复购买已由手炉提供的热量。
- 暖石/热水袋按偏冷部位能量需求分配收到的热量，未使用的能量留在物品；冷物品继续吸热。原有NBT float格式、版本和键保持不变。
- 提示界面说明最大回暖功率和按需传热；修正护甲防风提示误读取保温属性的问题。
- Documentation impact: 更新[玩家温度](../docs/climate/player-temperature.md)、[数值报告](../docs/climate/thermal-gameplay-balance.md)、[旧玩法对照](../docs/climate/thermal-legacy-gameplay-parity.md)、[测试入口](../src/gametest/README.md)和[计划Outcome](../plans/2026-11-07_23-38-41_player-equipment-heating-balance.md)，新增[机器验证记录](../docs/climate/validation/equipment-heating-2026-11-08.json)。

## Decisions

- 只限制装备额外传入身体的热量，环境、辐射、着火、熔岩及原有冷热阈值继续按既有身体模型工作；复用一次指数响应，不增加第二套人体求解器或存档状态。
- 保留一次性燃烧/固定放热装置的消耗时钟，受控背心只为剩余需求付费；有限热库保留没有传出的余热。
- 范围纠正：首轮守恒断言忽略既有float写回舍入，测得约3.18J累计差后曾临时改成double存储。用户明确反对未经确认的精度修改，已完全撤回`WearableThermalState.java`改动并停止r2 JVM；该次运行不作为最终资格。最终测试按每个温度半个float ULP乘热容检查存储误差，另严格检查double计算阶段的能量守恒，不通过改变精度满足错误断言。
- 配套仓库定位为`D:/TheWinterRescue`，根目录及递归均未找到AGENTS.md；只读核对相关标识、暖石槽和60°C热水袋配方。该仓库没有本轮修改。

## Validation

- Java17生产构建通过。首次完整GameTest编译受已有矿床测试四处错误阻止；本轮ignored init仅排除`content/world/features/deposit/**`测试源码，未改该系统或生产构建依赖版本。
- 最终`assemble runGameTestServer`运行日志为`build/equipment-heating-20261107/run-r3.log`：30/30通过，退出码0，热运行时错误0，最终`rootBytes=0`。r1/r2原始日志保留。
- 新增8个Forge入口，覆盖72条1200秒固定环境装备回放、寒冷回暖收益、主副手、厚衣物与叠加、0.25/1/5/60秒周期、资源耗尽/部分秒存档、背心需求、有限热库守恒、真实玩家/Curios/渲染开关及创造/旁观暂停。
- 72条装备回放最高部位37.179314°C。−50°C裸体20分钟躯干：无装备35.30937°C，副手手炉/暖贴36.99877°C，背心36.75429°C。
- 29条无装备身体回放的最终核心/躯干温度及御寒耗食与2026-11-06记录完全一致。有限食物8条中裸体−50/−80°C死亡时间仍为23:12/15:18；真实篝火四场景共1744行，最低生命20。
- 生产JAR位于`build/equipment-heating-20261107/compiled/libs/frostedheart-1.20.1-0.7.9.jar`，14,611,807 bytes，无GameTest类，中英文新提示已打包。它来自当前共享工作区，包含原有未提交工作，未安装、提交或发布。
- 相关差异空白检查、语言JSON解析、文档链接及原精度源码无差异检查通过。

## Remaining

- 本轮已确认的装备修复完成。未进行实际客户端画面、用户原存档、全难度长期资源平衡或完整整合包联机验收；固定环境装备回放保持食物/水可用，与有限食物玩家回放的范围分开。
