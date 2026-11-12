# 岗位与住宅空队列：真实客户端复现、同步修复与回归来源

- Time: `2026-11-09 19:51:00 +08:00`
- Author: `Codex; OpenAI GPT-6; primary engineering agent`
- Status: `completed`
- Scope: 城镇岗位/住宅计划同步、镇长印章字段说明、真实 Forge 与渲染客户端验证、Git 回归归因

## Completed

- 使用当前源码的真实 Minecraft 图形客户端与集成服务器，从空城镇完整基线开始建造并扫描住宅、仓库、狩猎站，招募 3 名居民并执行正常日结算。修复前画面精确复现“可工作 3 · 目标 0/0 · 待分配 0”，岗位与住宅列表均空白。服务端各有 1 个计划条目，客户端两种计划均为 0 个条目；两端均确认 3 人有住房和工作，住宅实际消耗食物。该阶段 `passed=false` 是预期缺陷复现，不作为验收通过。
- `TeamTownData.normalizeStaffingPlan` / `normalizeHousingPlan` 在服务器权威计划变化时分别标记现有 `DataSyncCache`；`TownSync.flushChanges` 先发建筑，再发变化的权威计划。复用已有两种计划包，温度、日报和普通名册更新不重发未变计划。同步异常沿用脏标记恢复。客户端不用新建筑的当前名册人数推断玩家保障人数。
- 中文汇总“目标”改为“保障”，英文改为 `Guaranteed`。0 保障仍允许第二轮分配、入住和供餐；`0/0` 本身正常，空建筑列表才是本次同步错误。
- 用户后续要求无需 build、重点解释为何以前没有；后续转为只读 Git 历史核对和记录。已运行的渲染客户端自然完成，未为历史比较 checkout、复制或编译旧工程。
- Documentation impact: 更新 [保存与运行边界](../docs/town/persistence-and-runtime.md)、[实现说明](../docs/town/implementation-reference.md)、[岗位指标解释](../docs/town/town-model.md#224-玩家界面)和测试入口，保存 [受控证据](../docs/town/validation/staffing-housing-client-2026-11-09.json)。`diary/README.md` 在任务开始前已被删除，本轮从 Git HEAD 读取格式，不恢复用户删除内容。未改动 `design/` 或伴生整合包。

## Decisions

- 回归来源由两个提交的实际差异确认：`763d8475bc`（2026-10-26，`Modernize town persistence and building lifecycle`）删除 `getStaffingPlan` / `getHousingPlan` 中的自动 normalize，移到显式 `prepareAssignments`；当时建筑变化仍调用 `TownSync.invalidate`，打开建筑/印章仍 `requestFull`，完整快照会带回计划，掩盖缺失的计划增量。
- `fdacc2bc724`（2026-11-08 11:27:53 +08:00，`修复城镇和温度模拟系统问题`）令 `TownBuildingStore.install/installMissing` 使用 `metadataChanged(false)`，普通删除也不再要求全量；同时 `TownManagerItem.use` 与建筑菜单打开入口改为 `TownSync.ensureCurrent`。原计划增量只在玩家编辑时发出，建筑注册引发的计划改变未接入 flush，因此从该提交起失去两条全量兜底路径。这是近期优化暴露的同步回归。
- 旧 `TownViewSyncGameTests` 检查建筑/资源增量到达与没有额外全量；`TownViewClientValidation` 渲染住宅、狩猎站、矿场建筑界面，不打开镇长印章的岗位/住宅页，也不比较对应队列，故此前通过不足以覆盖此回归。
- 从登录时已有建筑的完整视图开始，或重新连接后，计划通常可见；同一连接期间新增、拆除、替换建筑会暴露陈旧计划。单纯关闭重开印章已改为复用基线，不能可靠补齐。

## Validation

- 修复前图形场景：[原始结果](../build/town-scheduling-before/result.json)、[岗位截图](../build/town-scheduling-before/01-zero-guarantee-staffing.png)、[住宅截图](../build/town-scheduling-before/02-zero-guarantee-housing.png)。两端居民岗位/住房均 3，服务端队列各 1、客户端各 0，额外全量安装 0。
- Forge 服务端：`gradlew.bat --offline runGameTestServer -I Scripts/town-scheduling-validation.init.gradle -PgameTestDirectory=build/town-scheduling-server-run "-PgameTestNamespaces=frostedheart_town_scheduling,frostedheart_town_view,frostedheart_buildings,frostedheart_persistence,frostedheart_warehouse_network" -PthermalTestHeap=4G -PthermalTestProcessors=8 --console=plain`；[最终日志](../build/town-scheduling-server-r3.log) **39/39 required 通过**，退出 0。包含新增 3 项和原有 36 项；普通温度更新对应计划包数为 0。
- 图形客户端：Java 17，`gradlew.bat --offline --no-daemon -Dnet.minecraftforge.gradle.check.certs=false "-Dorg.gradle.jvmargs=-Xmx1G -XX:MaxMetaspaceSize=512m -Dfile.encoding=UTF-8" runTownSchedulingClient -I Scripts/town-scheduling-validation.init.gradle -PtownSchedulingOutput=build/town-scheduling-after-r4 --console=plain`；[最终结果](../build/town-scheduling-after-r4/result.json) `passed=true`，844 渲染帧，额外完整视图安装 0，[日志](../build/town-scheduling-after-r4.log)退出 0。两种滑条均通过实际 Screen 鼠标事件提交，服务端和客户端目标变为 2；次日仍 3 人入住、3 人工作并实际用餐。加建、拆除和狩猎核心原位换住宅后，两队列与权威状态一致。
- 测试只将建筑环境输入控制为 20 ℃，结构使用真实墙体、门、床和扫描器；第二服务端玩家使用 EmbeddedChannel。没有宣称人工操作、两台远程图形客户端或低温/规模性能验收。
- 中间启动问题分别为共享旧测试 class 残留、城镇专用测试源不含默认热测试观察 mixin、自动点击未计入 CUI 包装器居中与 hover 更新、ForgeGradle 新 daemon 的外部连通检查等待；均修复在测试运行入口/夹具，失败日志保留，未通过修改生产 UI 输入或吞异常来验收。

## Remaining

- 当前工作区修复未提交或部署。用户要求重点解释回归来源，后续不再构建。
- 远程网络延迟、多人图形客户端与非受控温度不在本次调度验证范围。
