# 余温回收交付与最终验收

- Time: `2026-11-06 09:50:02 +08:00`
- Author: `Codex; OpenAI GPT-6.1 Sol; primary closure and production-validation agent; implementation continued from GPT-6 Astra`
- Status: `completed`（选定实现和生产验证范围；未宣称完整整合包或多人显示资格）
- Scope: [余温架构](../plans/2026-11-05_16-44-09_thermal-residual-retirement-architecture.md)的实现、真实Forge回归、跨JVM磁盘验证、生产JAR与配套默认配置

## Completed

- 完成[前一条进行中记录](2026-11-05_23-24-00_thermal-residual-retirement-implementation.md)的剩余验收，并将计划置为completed。当前行为由[余温生命周期](../docs/climate/thermal-residual-lifecycle.md)维护；原始失败和构建范围见[验收JSON](../docs/climate/validation/thermal-residual-retirement-2026-11-06.json)。
- `ThermalRetentionPolicy`统一1°C进入、0.5°C退出/保留；温度保留相关0.0625°C与旧残差门槛统一，原存档量化单位不重解释。潜热和未消费J独立保留。
- Air/Material筛选保持原H/branch/sampleTick，部分删除一次发布；完整Page捕获成功后才释放，shrink只处理退出mask。有限主线程维护处理已加载休眠数据，卸载后无维护任务。
- 补齐稀疏Brick在Section中间的露天边界，以及屋顶变化时各层失效，避免计算范围的位置对齐截断散热通路。
- 收尾补齐输入恢复后的空附件注销：必须等实际输入/恢复责任结束，才能清掉已空的Chunk附件；使用真实20J恢复路径验证，未增加物理热源。
- 配套仓库`D:/TheWinterRescue/config/frostedheart-common.toml`仅新增显式30游戏秒半衰期；仓库/父目录未发现AGENTS.md，已检查相关配置与注册键，TOML解析、独立diff检查通过。
- 生产包为`build/thermal-retirement-isolated-20261105/libs/frostedheart-1.20.1-0.7.8.jar`，14593397B。3734个当前main class全部存在，GameTest class交集0，关键保留策略/维护/BlockDropsEvent存在。没有路径敏感hash要求。

## Decisions

- 撤回尝试过的全局热容缩小与连接性重构，原空气1200J/(block·K)、普通材料stone5400/wood2700J/K及现有潜热活动规则保持。休眠默认半衰期改为30游戏秒；旧文件显式配置仍优先。
- 不能用平台温度不变判定泥土不散热。原热容地面场景已实际观察到熄火后LIVE泥土焓下降；独立休眠相变完成冻结、再删除普通余温约150秒。
- 不能按210秒倒计时清空仍有真实余热的房间。通风房间检查实际降温与完整卸载/冷却/重载/再点火链路，露天场景检查纯在线退出。3223tick的房间结果包含2000tick卸载等待，不冒充全程在线冷却时间。
- 多个超出小模板范围、带全局源数量/容量假设的夹具合并到同一个世界会互相干扰。组合失败保留，源、保存、空附件分到独立世界后原断言全部通过；没有为通过测试放宽生产门槛。
- 最后打包的ForgeGradle动态工具metadata请求曾阻塞。使用既有成功重映射参数和缓存中可用的最高工具版本完成当前产物重混淆；未固定唯一工具版本、复制历史源码或绕开生产类检查。
- Documentation impact: 更新`docs/climate/README.md`、`thermal-runtime-control.md`、`world-climate-and-temperature.md`、`heat-production-and-network.md`、`data-lifecycle-and-integration.md`，新增当前余温说明与验收JSON；`design/`始终只读。

## Validation

- Java17统一编译与打包成功。最后空附件守卫变更后`compileJava compileGameTestJava`通过，日志`build/thermal-retirement-closeout-classes.log`；当前生产包经`build/thermal-retirement-package-final.log`重混淆。
- 普通记录、两个独立红外baseline、部分删除、无人查询和泥土冻结等核心场景通过。原代码热容下，露天四相对高度温升均约1.835°C、屋顶变化有效、熄火在线退出；真实地面焓继续下降。
- 存储5项、原版早期卸载/初始几何/取消3项均通过。完整房间实际卸载重载及重新点火1项通过，最终pages0、维护chunks0。历史房间错误时限断言分别保留在`final-core`和`final-lifecycle`失败日志中。
- 最后一次代码修改后，独立`closeout-pending`1/1、`closeout-sources`7/7、`closeout-save`1/1均通过，热保存/worker错误0、关闭root0。此前`closeout-final`组合的3次失败由独立世界复测闭合。
- 三个独立GameTest JVM使用同一磁盘世界：`restart-write`1/1保存H/branch/tick；`restart-read`1/1读回原值并实际冷却删除；`restart-empty`1/1确认下一次启动无旧热数据复活。各次root0。
- 128真实篝火、2400tick压力窗通过；夹具分配100对象但实际活跃消费者10→30，共1601次查询。年龄/几何/消费者过期违规0，热主线程P95/P99为0.338/1.126ms，完整server tick P95/P99为9.562/56.820ms，首个观测tick最大861.201ms，热登记峰66253016B约63.18MiB，关闭root0。该测量先于最终空附件分支增加，非空路径相同；不作为提速比例、准确CPU或百人20TPS资格。
- 最终JAR以`forgeserver`而非userdev启动独立服务器，版本0.7.8；真实篝火LifeTime从20000降到18831，随后熄灭、`save-all flush`、正常stop退出0。实际磁盘读取v6、2个Air component、1条待消费输入，热保存/worker错误0。日志`build/thermal-retirement-production-server.log`与校验JSON保留；精简依赖的可选配方/研究catalogue诊断不冒充完整整合包通过。
- 两个Git仓库分别检查diff；新说明/计划/日记相对链接和JSON/TOML解析通过。未替换用户正在游玩的实例文件。

## Remaining

- 本次授权的实现与选定真实测试已完成。
- 完整整合包多人客户端红外渲染、更大并发、所有建筑/材料的平衡校准仍是后续验证范围；现有结果不提供这些资格。
