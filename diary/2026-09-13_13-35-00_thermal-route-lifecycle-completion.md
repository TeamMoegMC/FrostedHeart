# 热行为交接：通路失效、材料身份与玩法转换

- Time: `2026-09-13 13:35:00 +08:00`
- Author: `Codex; OpenAI GPT-6; primary engineering agent`
- Status: `completed`
- Scope: `继续材料热模型实施，完成本轮通路和生命周期修复；不做独立可读性重构`

## Completed

- 间接连接移入`ThermalFragment.RoutedContacts`，携带所属通路的共享有效性标记，仍由同一个solver和原有交换kernel更新H。删除旧的“间接路线混入AirPairs、用buoyancy开关区分”的执行表示；普通直接Air连接继续使用浮力。
- 通路失效立即停止对应间接Q；新路线仅在匹配的拓扑提交后生效。未提交的通路工作和脏fragment保留；预算拒绝时重绑失效端口并重试halo捕获。稳定cut的route visits为0。
- 删除`TopologyPlan.exposureDeltaAt`及按暴露变化重建材料容量的遗留分支。
- `SECTION_REPLACED`即使签名相同也重建材料身份；raw container替换使用相同语义。清理对应存档材料，阻止旧checkpoint投影/相变请求恢复被删除的物体，重置标记保留到新publication可见。
- 等待布局的材料使用arena位图暂停旧节点换热、source接收和新phase请求。H继续保存在原arena供一致checkpoint投影使用，公开世界查询仍检查Page几何身份。没有增加兼容节点或温度副本。
- `MinecraftPhaseController.materialChangeCause`统一活跃和休眠状态的变化分类；不可由物理phase law表达的玩法转化显式标注`GAMEPLAY_TRANSITION`并按旧材料T初始化目标law。外部H差额只在拓扑提交时记入`externalMaterialEnergyJ`，物理ACK不重复扣L。
- 保留已有含水/流体有效热容接续，合并重复公式到`MaterialThermalLaw.afterMassChange`。用户指出无必要后，撤回本轮短暂加入的雪层数量/双层台阶体积缩放，不实现物质搬运模型。
- 材料存档回调由服务端线程执行；其他线程的回调交给Minecraft现有executor，没有新建调度器。
- 同步必要测试调用，源码不再引用旧PHASE_RESERVOIR、CAPACITIVE_SURFACE、PhaseContacts或旧相变池访问器。未加入旧存档读取或新旧实现切换开关。
- 本轮生产Java净增加165行；与上一轮清理合并，相对清理起点累计净减少827行。此计数不含测试、文档和更早的其他工作区改动。

## Decisions

- 解析场是正式玩法机制，保留既有组合规则与区域效果。材料模型接续其状态，不把解析场降级为无效显示或要求其全部受有限功率模拟约束。
- 预算间隔明确采用局部暂停：失效间接连接Q=0，其他有效连接继续；恢复后从当前H继续，不补算历史传热。这是暂态近似，未宣称在重建期间精确模拟新几何。
- 用户要求不使用Python修改源码，本轮全部源码变更使用显式代码补丁。
- Documentation impact: 更新[runtime架构](../docs/climate/thermal-runtime-architecture-and-optimization.md)、[材料/热源](../docs/climate/heat-production-and-network.md)、[生命周期与消费者](../docs/climate/data-lifecycle-and-integration.md)和[主plan](../plans/2026-09-12_23-53-20_thermal-material-enthalpy-lifecycle-repair.md)。明确温度计材料测量与作物HUD环境查询的区别。

## Validation

- Java 17；最终`gradlew.bat runGameTestServer --offline --no-daemon --console=plain -Dnet.minecraftforge.gradle.check.certs=false`通过，包含生产与GameTest编译，68/68必需测试通过。日志：`run-gametest/thermal-behavior-final.log`。
- 新/扩展验证覆盖：六向且跨Brick/Page的两层导热和第三层隔离；内部Q守恒与反向传热；长通路跨cut完成及稳定零展开；预算不足时封墙停用旧边；A-Air-A、同签名整段替换、形状保H、玩法转化及休眠存档接续；冻结平台checkpoint与ACK守恒；source饱和的实际入账/未接收/声明损耗/退化损耗闭合且不重放。
- 256格通路夹具使用4096个动态节点，在初始cut后继续1个cut完成；每cut route visits不超过4096，稳定后为0。这是指定夹具的工作量观测，不是大型存档CPU时限或全局最优证明。
- `git diff --check`通过。shader未修改，复用此前GPU栅格验证结果，不重复无关检查。

## Remaining

- 主计划保持in-progress：尚未完成真实大型存档负载、完整CPU/heap对比及重建暂停的实机体验验收，不宣称已经实测最佳性能。
- 独立可读性整理仍等待用户另行指示；没有为该项提前扩展本轮改动。
