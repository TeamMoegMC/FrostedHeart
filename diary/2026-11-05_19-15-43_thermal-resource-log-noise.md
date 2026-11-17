# 温度资源调节日志改为按共享预算汇总

- Time: `2026-11-05 19:15:43 +08:00`
- Author: `Codex; OpenAI GPT-6; primary engineering agent`
- Status: `completed` for logging changes and two focused existing Forge cases
- Scope: `RegionalQualityControl`、`RegionalAirRuntime`、`MinecraftThermalInput`的日志输出及当前运行时说明

## Completed

- 候选内存拒绝、候选降粗、区域降粗请求、策略预选/安装和区域交接共五类逐次日志，统一由已有`ThermalDiagnostics.TRACE`控制。默认不输出；设置`FH_CONTINUOUS_AIR_REGION_DIAGNOSTICS=true`仍能获得原始逐次明细。
- 服务器共享预算每60秒单调时钟检查一次，仅在新增准入拒绝时输出一条`THERMAL_RESOURCE_PRESSURE` INFO，包含新增/累计拒绝次数、当前登记量、累计峰值和软目标。所有维度共用汇总时钟与基线，没有按regionId创建限频表或新增队列、生产类型、配置项。
- `closeAll`在工作归还后更新拒绝基线，避免同JVM下一次进入世界重报上次会话。没有新拒绝时不打印，也不把没有新拒绝解释成恢复细档。
- 保留`RegionWorkPolicy.installed`的实际状态更新、现有区域进度描述和预算计数；异常日志、预算准入、粗细决策、重试和保存流程未修改。
- Documentation impact: 更新[温度区域运行时说明](../docs/climate/thermal-runtime-control.md)的核验日期、源码锚点和日志合同；未编辑`design/`、整合包配置或其他系统。共享工作区原有改动保留。

## Decisions

- 预算拒绝是已处理的正常资源调节事件，默认用累计汇总表达；详细原因仍由现有诊断开关显式开启，避免把逐次异常文本写进普通游玩日志。
- 汇总读取既有廉价计数，不扫描区域、对象或堆；这些字节数是模组登记量，不表示JVM/整机内存。60秒是日志检查间隔，与游戏tick、CPU额度和恢复退避无关。
- 本次是可逆的日志修改，没有新增镜像实现的测试；使用现有真实压力与粗细恢复场景验证。

## Validation

- Java17 `assemble compileGameTestJava -I build/thermal-log-noise.init.gradle --offline`通过，49s；[编译日志](../build/thermal-log-noise-compile-20261105.log)。当前源码编译到独立生成目录，不复制历史源码、不引入路径hash。沿用既有热系统编译范围，排除其他会话未完成的deposit GameTest夹具，没有编辑其源码。
- 默认诊断关闭，`frostedheart_geometry_capacity`现有病例1/1通过，2m29s；[日志](../build/thermal-log-noise-default-20261105.log)。5GiB堆、8个可见处理器、realtime20、128MiB软目标、1000ms/s活动额度。1024真实热源、1600tick观察期，准入拒绝49085次；五类逐次日志合计0，默认汇总1条。对照温度推进80次，最大年龄43tick、空读0、失败0，三次实际原版保存完成。此观察期只覆盖一次汇总输出，不声称实测了多个60秒周期。
- 开启区域诊断，`frostedheart_regional_grid_switch`现有病例1/1通过，1m40s；[日志](../build/thermal-log-noise-diagnostics-20261105.log)。实际降粗和恢复完成2次、Checkpoint检查2次，failure=null；策略明细89条、交接88条、降粗请求1条。无新增准入拒绝，预算汇总0条，证明诊断可打开、无拒绝不打印汇总。该轮明确包含详细日志成本，不用作性能对照。
- 两次进程退出码0，未记录热worker失败或热运行时启动失败。本批`git diff --check`通过；没有新增JUnit或运行百人/长期内存矩阵。
- 安装包已生成：[frostedheart-1.20.1-0.7.7.jar](../build/thermal-log-noise-20261105/libs/frostedheart-1.20.1-0.7.7.jar)，14930714 bytes，三个修改的运行时类在包内，GameTest类0。产物来自当前共享工作区，包含其他原有修改；未自动安装、提交或发布。

## Remaining

- 截图对应整合包的退出原因没有复现；本批解决日志噪声，不构成原生OOM、内存泄漏或长期重试问题已修复的证据。
- 同JVM重新进入世界的汇总基线重置经源码复查，未启动客户端作重新进入世界验收。
