# 余温回收统一实现与真实 Forge 验证

- Time: `2026-11-05 23:24:00 +08:00`
- Author: `Codex; OpenAI GPT-6 Astra and GPT-6.1 Sol; primary implementation and validation agent across model handoff`
- Status: `partial`（代码已统一编译；最后负载和跨进程验证进行中）
- Scope: 普通温差退出、Page/Chunk 生命周期、有限休眠维护、露天散热边界、红外删除与配套默认配置

## Completed

- 按[架构](../plans/2026-11-05_16-44-09_thermal-residual-retirement-architecture.md)实现 `ThermalRetentionPolicy`、Air/Material 批量筛选、`DormantThermalMaintenance`。温度保留相关0.0625°C与旧残差0.25°C门槛统一到0.5°C，等号保留；存档量化单位不重解释。
- 完整Page退出等待可用捕获，shrink仅处理实际退出mask；空捕获明确删除旧保存内容。已加载休眠状态按服务器主线程预算维护，旧读者继续使用原数组，幸存记录保留H/branch/sampleTick。
- 露天散热接到实际稀疏上边界，而非仅Section最上层；高度采样与屋顶变化失效同步覆盖各层。
- 相变通过现有世界转换完成，普通查询保持只读；材料版本与现有INVALID编码清除两个独立红外baseline。卸载/Level结束/停服解除维护引用。
- 源码休眠温差半衰期默认30游戏秒。配套仓库`D:/TheWinterRescue`未发现AGENTS.md，其`config/frostedheart-common.toml`此前没有Thermal Runtime段，本批仅增加显式30秒配置，其他配置保留；TOML解析与该仓库diff检查通过。

## Decisions

- 本批探索过将空气热容1200降到200、普通材料热容取消6倍系数，以及用连接性摘要改变潜热驻留。实际证据不足以支持这些广泛变更，全部撤回，原热容与潜热活动规则保持。
- 最初把平台温度不变解释成初始化/连接故障不充分。实际NBT显示泥土有冻结潜热；最终原热容地面场景直接观察到熄火后LIVE材料焓下降，不能由-10°C平台断言不散热。
- 封闭/通风房间在固定210秒观察窗口仍有超过0.5°C的余热，不能要求按倒计时清空。保留这些失败记录，最终场景分别检查露天在线退出、房间真实降温，以及卸载后冷却、重载无旧热、重新点火。
- Documentation impact: 新增[余温生命周期](../docs/climate/thermal-residual-lifecycle.md)，更新气候入口、世界红外、材料及运行合同；现行说明区分在线空间传热与休眠自然浴，未宣称全部房间统一1–3分钟退出。

## Validation

- 当前工作区使用Java17和独立build目录、独立Gradle项目缓存统一`assemble compileGameTestJava`通过。生产构建：`build/thermal-retirement-final-compile.log`；随后仅测试夹具编译，无生产行为修改。
- 第一轮生产构建成功后，GameTest使用的class目录消失，启动未进入场景即缺少`BlockDropsEvent`；生产JAR中该类完整。隔离Gradle项目缓存后运行正常。另一次工具metadata联网等待以本地已有工具和有限网络等待处理；没有复制历史源码、固定工具单一版本或增加路径敏感hash。
- 当前生产代码的`final-core`真实Forge7项中6项通过，旧房间固定时间退出断言失败；日志保留于`build/thermal-retirement-final-core.log`。
- `final-lifecycle`真实Forge11项中10项通过：5项存储、3项原版早期卸载/初始化/取消、露天四高度与地面潜热进度。旧房间重载即清空断言失败，仍恢复出真实余热；日志保留于`build/thermal-retirement-final-lifecycle.log`。
- 只修正房间生命周期夹具后，`final-reload`1/1通过：在线60秒温升4.84°C降至3.16°C，实际卸载`coldPendingRecords=0`，卸载期间等待100游戏秒，重载后旧状态清除且重新点燃成功。完整链路熄火后3223tick，含离开等待；不是全程在线冷却时长。最终pages0、维护chunks0、热关闭root0。
- 露天四种Section相对高度的实际温升均约1.835°C，加盖/拆除屋顶影响正确，熄火后在线退出。独立休眠泥土完成冻结和普通余温释放约150秒。所有已完成的通过/失败Forge组关闭root0，热保存/worker错误0；没有把跨构建次数合并成全量首过。
- 最后负载与三次JVM磁盘验证的结果完成后追加于本条记录。

## Remaining

- 完成128热源/100温度消费者负载记录，以及同一世界写入→新JVM读回并删除→第三JVM确认不复活。
- 生成可提交验证摘要，完成生产JAR核对和最终文档/计划收尾。真实多客户端渲染、整合包百人承载和所有房间的平衡校准不在已验证结论内。
