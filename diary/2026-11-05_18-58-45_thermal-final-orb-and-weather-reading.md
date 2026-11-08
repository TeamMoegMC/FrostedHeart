# 服务端直发温度球读数，预报改显独立天气基准

- Time: `2026-11-05 18:58:45 +08:00`
- Author: `Codex; OpenAI GPT-6; primary engineering agent`
- Status: `completed` for implementation and focused validation; client visual inspection not performed
- Scope: 玩家显示观测、身体/气候同步包、预报栏温度、客户端气候状态、相关Forge用例与气候文档

## Completed

- 修正[上一阶段](2026-11-05_18-39-37_thermal-orb-radiation-reading.md)的同步设计：删掉原始float辐射，服务端在观测发布时算好最终球读数，客户端只保存和绘制。`FHBodyDataSyncPacket` v2最终载荷7字节：版本1、空气short2、核心short2、最终球温short2。
- 用户明确选择保留现有客户端功能、额外保留2字节空气。呼气仍按真实空气低于−10°C判断，饮品提示仍与既有服务端空气/37°C杯子倍率一致；没有把综合显示值冒充物理空气。
- 原预报栏当前数字错误复用玩家局部空气，现改为 `FHClimatePacket` 的独立 `WorldClimate.getTemp(ChunkPos)` 气候基准；常规天气和局部白幕生效，房间加热和辐射不进入这个数字。`ClientClimateData.clear` 同时清除该值。
- 气候包增加一个0.1°C的short，沿用已有小时/跳时及登录/重生/切维度同步。没有新增轮询、任务、队列、框架、Mixin或采样射线。预报原解锁、配置与Boss栏显示规则保留。
- `setEnvTemp` 命令与普通观测共用服务端最终显示计算；死亡、重置和读盘清掉显示观测。空气变化但球值不变时仍同步，确保其他空气消费者及时更新。
- Documentation impact: 更新[玩家温度](../docs/climate/player-temperature.md)、[生命周期](../docs/climate/data-lifecycle-and-integration.md)、[世界气候](../docs/climate/world-climate-and-temperature.md)及[旧玩法对照](../docs/climate/thermal-legacy-gameplay-parity.md)。没有编辑城镇、研究、世界生成、整合包配置、旧粒子或`design/`。

## Decisions

- 区分三种用途：温度球的局部供暖参考、呼气/饮品的局部真实空气、天气系统的气候基准。前两种结果在同一身体包保持一致，天气值沿用独立气候包，不再混读。
- 天气基准不是包含维度、生物群系和海拔后的室外空气实测；显示的是上次气候同步时玩家区块的天气值。默认20TPS、时间正常推进时气候小时为1000tick/50秒。
- 比最初5字节身体包增加2字节，比上一阶段9字节删除2字节；默认一秒周期、100人每次发送的新增身体包内容为200bytes/s（未计网络封装），属于计算值而非百人网络实测。
- 杯子按空气与37°C切倍率是明确保留的旧玩法规则，本轮没有擅自重定饮品收益。用户前一轮确认的是保留功能；这不构成对旧规则合理性的验收。

## Validation

- 统一修改并闭合复查后运行 Java17 `assemble compileGameTestJava -I build/thermal-orb-radiation.init.gradle`，通过，22s；日志 `build/thermal-orb-radiation-compile-20261105-r2.log`。Gradle正常复用部分构建缓存，没有复制旧工程或加入路径hash门禁。
- 一次统一Forge运行 `frostedheart_player_release` **13/13通过**，日志 `build/thermal-orb-radiation-20261105-r2.log`，摘要 `build/thermal-orb-radiation-20261105-r2.json`。
- 7字节编码/解码、服务端24.733333°C量化为客户端24.7°C、客户端无需辐射值、归零与强制同步、空气改变而球温不变、命令刷新、死亡/重置/重载，以及独立气候包完整round-trip和清零通过；原9项身体、衣物、真实篝火、伤害、食物方向性、装备与存档回归通过。
- 天气用例使用实际服务器气候能力与人为设置的对照局部观测，证明包值不从玩家热观测读取；没有把它说成实际客户端天气画面验收。
- 退出码0，热运行时错误0，最后关闭 `rootBytes=0`。本批 `git diff --check` 通过。没有JUnit、长时间数值矩阵或百人压力测试。
- 当前工作区产物已生成，没有自动安装、提交或发布。

## Remaining

- 尚未启动客户端作画面核验；已验证的是编译、协议、客户端数据应用及服务端重点玩法。
- [生存与资源数值差距](../docs/climate/thermal-gameplay-balance.md)仍待定标，显示与同步修正不表示整个数值平衡通过。
