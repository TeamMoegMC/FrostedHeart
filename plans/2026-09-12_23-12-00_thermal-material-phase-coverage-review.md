# 材料与相变热状态覆盖复查

- Time: `2026-09-12 23:12:00 +08:00`
- Author: `Codex; OpenAI GPT-6; primary engineering agent`
- Status: `completed`
- Investigation status: `completed; repair plan prepared, code not implemented`
- Scope: `方块分类、材料/相变状态、换热、拓扑迁移、休眠恢复、温度消费者及红外语义；未修改生产/测试代码`
- Outcome: `确认多处模型缺口与状态口径不一致。不能把当前系统称为完整的逐块材料温度/相变模型，也不能只改红外mask或shader修复。`
- Related: [现行红外计划](2026-09-08_21-58-58_thermal-unknown-block-ventilation-75.md)、[材料与热源文档](../docs/climate/heat-production-and-network.md)、[runtime文档](../docs/climate/thermal-runtime-architecture-and-optimization.md)。

执行方案已单独整理为[2026-09-12_23-53-20_thermal-material-enthalpy-lifecycle-repair.md](2026-09-12_23-53-20_thermal-material-enthalpy-lifecycle-repair.md)；本报告保留原调查结论。

## 范围与证据

本报告核对当前生产源码的可达分支。下列“已确认”指代码行为明确，不代表已逐一在用户存档复现。具体BlockState受加载的StateTransitionData和tags影响；不会仅凭蓝色截图认定缺值或确定材料类型。

核心锚点：

- [MinecraftThermalProfiles](../src/main/java/com/teammoeg/frostedheart/content/climate/thermal/profile/minecraft/MinecraftThermalProfiles.java)、[StateStaticThermalResolver](../src/main/java/com/teammoeg/frostedheart/content/climate/thermal/profile/minecraft/StateStaticThermalResolver.java)、[MinecraftStateThermalTable](../src/main/java/com/teammoeg/frostedheart/content/climate/thermal/profile/minecraft/MinecraftStateThermalTable.java)。
- [BrickTopologyCompiler](../src/main/java/com/teammoeg/frostedheart/content/climate/thermal/topology/BrickTopologyCompiler.java)、[TopologyView](../src/main/java/com/teammoeg/frostedheart/content/climate/thermal/topology/TopologyView.java)、[BrickMigrationKernel](../src/main/java/com/teammoeg/frostedheart/content/climate/thermal/topology/BrickMigrationKernel.java)。
- [ThermalCellArena](../src/main/java/com/teammoeg/frostedheart/content/climate/thermal/mesh/ThermalCellArena.java)、[MaterialBoundaryRegistry](../src/main/java/com/teammoeg/frostedheart/content/climate/thermal/mesh/MaterialBoundaryRegistry.java)、[PhaseTransitionRuntime](../src/main/java/com/teammoeg/frostedheart/content/climate/thermal/solver/PhaseTransitionRuntime.java)。
- [DormantChunkThermalState](../src/main/java/com/teammoeg/frostedheart/content/climate/thermal/persistence/minecraft/DormantChunkThermalState.java)、[MinecraftPhaseController](../src/main/java/com/teammoeg/frostedheart/content/climate/thermal/runtime/minecraft/input/MinecraftPhaseController.java)、[MinecraftThermalInput](../src/main/java/com/teammoeg/frostedheart/content/climate/thermal/runtime/minecraft/MinecraftThermalInput.java)。
- [StateTransitionData](../src/main/java/com/teammoeg/frostedheart/content/climate/data/StateTransitionData.java)、[ServerLevelMixin_TemperatureUpdate](../src/main/java/com/teammoeg/frostedheart/mixin/minecraft/temperature/ServerLevelMixin_TemperatureUpdate.java)、[SoilThermometer](../src/main/java/com/teammoeg/frostedheart/content/utility/SoilThermometer.java)。

## 当前状态实际含义

| 类别 | 实际节点与温度 | 相同问题的来源 |
|---|---|---|
| 普通无材料Air | 连通的多块可合并；完整Air Brick为一个节点 | 原环境查询的基础，不是墙温 |
| 暴露V0普通材料 | 暴露块各有有限热容节点，T由H/C得到 | C与暴露面相关；未暴露无节点 |
| 通风且有材料的mixed | 单块一个transport温度，暴露时使用材料容量 | 同一值同时充当Air和表面温度 |
| 升温相变材料 | 同Brick同phase profile共用能量池；温度API返回固定转换阈值 | 无独立显热温度，且红外surface mask排除 |
| 无相变的流体/含水状态 | 通风率0，普通材料分类被跳过 | 可能没有任何有限材料节点 |
| 植物等明确排除类别 | classify返回null；依几何仍可能有Air transport | 没有植物自身温度 |
| source/静态辐射/机器热网 | 功率、接收通量或heat unit状态 | 不等于机器方块表面温度 |

## F01：相变资格替代了显热材料状态（核心缺口）

`MinecraftThermalProfiles.prepare`检测到有效升温转换后用PHASE_RESERVOIR替代普通profile。`MaterialBoundaryRegistry.Profile.phaseReservoir`的surfaceCapacity为0；`ThermalCellArena.temperatureC`对phase无条件返回transitionTemperatureC。`BrickTopologyCompiler`只为普通材料/有材料热容mixed设置surfaceNodeMask。

后果：缺少“低温冰升温到熔点”的材料温度过程；固定阈值不能当当前温度。直接把phase加入红外mask会把未到相变条件的物体也涂成阈值温度。空相变池不会凭空给冷空气放热，因为applyContact把释放量限制在未预留能量内；不能把这个有限池错误描述成无限恒温热源。

修复方向：定义共同的材料显热/焓温关系，相变为可选能量区间和状态；查询区分实际温度、阈值与转换进度。先定状态含义，再改红外资格。

## F02：配置升温相变会同时改变几何通风语义（分类耦合）

`prepare`先求`StateStaticThermalResolver.ventilation(state)`，但进入相变分支后直接`ventilation=0`。因此仅添加有效升温配方，就可能把原75/100的部分形状或非阻挡状态变成整块不通风；是否进入该分支由配方决定。

这不同于已明确选择的“有流体整块阻挡”近似。相变能力不应自动替换所有几何语义。未来把phase改成有显热节点时，必须同时拆开这一耦合，不能只加一个温度字段。

## F03：相变能量非逐块，转换位置有编号偏序（粗粒度近似）

`compileCells`按profile合并`phaseMasks`，不按连通性分组；`PhaseKey`只由转换温度和单位转换能量构成。不同配方只要这些参数相同，也可能共用profile。接触传来的能量进入同一池，`PhaseTransitionRuntime.reserveOrRetry`选择`Long.numberOfTrailingZeros(candidateMask)`。

后果：同Brick中A一侧受热，累计能量可以用于转换编号更小的B，B未必是受热位置，也未必与A连通。这是池化模型行为，不能称为每个相变方块有独立温度或独立相变进度。

修复方向：明确能量的空间归属；若保留聚合，必须给出适用条件和转换分配规则，而不是把成员mask当成局部热状态。

## F04：相变池在成员更换后整池继承（状态生命周期问题）

`BrickMigrationKernel.migratePhase`在同Page生命周期、同Brick、同profile时复制完整H与请求状态，没有按旧/新candidate mask交集分配能量。删除部分旧块、增加新块或同一次cut内把整批块换到另一位置，均不能通过该函数表达“旧物质带走多少能量、新物质带入多少能量”。

后果：新相变成员可能继承旧成员的蓄能；缩小成员集合也不会在这一步按剩余质量调整H。ACK的身份检查能拒绝失效请求，但不等于修复了池内能量归属。

修复方向：区分真实物质保留、替换、迁移与候选变化；先确定H所有权，再调整迁移和ACK。不能靠请求被拒绝或clamp掩盖状态错配。

## F05：转换后没有连续的温度/能量状态（相变闭合缺口）

`MinecraftPhaseController.applyRecipe`改变BlockState；非phase迁移环节遇到旧或新槽为phase就跳过普通H迁移。新目标按自己的profile初始化，可能进入另一阈值池、普通材料自然初值，或没有节点。

后果：没有统一路径保证“冰融化后的水从熔点状态继续升温”，也没有通用的转换前后显热/剩余能量映射。不能通过给phase的temperatureC返回值加标记解决。

修复方向：定义转换边界与目标状态初值、潜热消费和剩余能量交接，配套实际前后状态测试。

## F06：含水状态可能丢失整个固体本体的热状态（覆盖缺口）

无有效升温转换时，普通分类要求`state.getFluidState().isEmpty()`；含水楼梯/栅栏等状态不满足，profile保持0。同时resolver把非空fluidState通风率设为0。这样的状态既不建transport也不建普通材料节点。

后果不只是水没有模型：固体骨架也可能从有温度变为无温度。具体实例取决于加载的配方；“非空fluidState且无有效升温转换”这一触发条件已由代码确认。

修复方向：区分固体本体与含水/流体属性，至少不能因含水自动删除本体状态。水体本身仍需定义显热/相变模型。

## F07：可见植物与部分非阻挡方块没有自身温度（明确排除）

`classify`先排除LeavesBlock及LEAVES/CROPS/SAPLINGS/FLOWERS/REPLACEABLE；未命中其他类别时只有blocksMotion才进GENERIC_SOLID。因此这些可见terrain表面可能只有空气transport或没有有限节点。

后果：红外可能读不到其本体温度，即使它可见、可被加热。当前蓝色/场修正不能补成植物真实温度。这是现有分类范围，不应误称所有方块材料已经接通。

修复方向：列清材料覆盖表，为植物/非阻挡材料决定必要的低成本状态；不因本次复查擅自加全部植物模拟。

## F08：mixed一份温度同时充当空气和材料（状态口径混用）

有通风率的材料块进入transport；有暴露面时容量替换为`surfaceCapacity*exposure`，没有暴露时退到Air参考容量。它仍满足isAirCell，并用于resolveAirPoint/source端口；红外通过surfaceNodeMask把同一值作为表面温度。两个通风块的连接走Air mixing分支，未使用各自的材料面导热系数。

后果：楼梯内气流温度和实体骨架不能分开；通风材质对之间的导热不能解释为材料导热率。该合并可作为简化模型，但必须说明适用范围，不能把它和V0普通材料的独立热状态混称同一种精度。

## F09：暴露面既决定热容，又决定节点是否存在，并受驻留影响（几何/状态耦合）

`compileCells`对exposure=0的V0材料不建节点，容量是每面容量乘exposure。跨Brick暴露读取`TopologyView.signatureAtWorld`，非resident邻Brick返回UNRESOLVED；`ThermalSignatureTable.ventilation`对无效ID返回0。

后果：实际暴露但邻区未参与模拟的面可能不计入暴露；封住最后一面可让节点消失；再次暴露可能重新初始化。材料缺值及有效C受模拟边界影响。在线保H而改C时，温度按`Tnew=Tref+(Told-Tref)*Cold/Cnew`改变，不能把这种跳变解释成实际已交换的热量。

若模型刻意只存“暴露表层”，C随面积变化本身是可选近似，但新增/撤去表层的能量必须有明确解释。现在不能把该C称为整块固定热容。修复需分开几何已知与模拟驻留，并决定材料体积/有效层状态的寿命。

## F10：相邻实心材料不直接导热（已知模型缺口）

`BrickTopologyCompiler.face`遇到`va==0 && vb==0`立即返回，包括普通材料之间以及普通材料与相变材料之间。连接主要是transport-transport、transport-material、transport-phase。`WorkerPageStore.faceResidualC`也只沿transport边界请求扩展；热材料会参与HotMask保留自身Brick，但不是固体导热扩张前沿。

后果：热石直接接触冷石/冰，不能期待按实体接触面导热；隔墙/地板的热传播仍主要依赖空气路径。这不是红外没取到已经算好的固体导热结果，而是该传播没有实现。

## F11：热学签名相同的物体替换缺少独立热状态失效（状态生命周期问题）

`MinecraftStateThermalTable.mutationFlags`按热学signature/source/radiation/occlusion变化决定标脏，不比较物体身份。普通迁移又只按materialProfileId相同来继承H。

后果：直接把热块替换成同热学签名的另一块时，原温度可能保留；同一cut内拆除再放回也不能仅凭最终签名识别新物质。这与同一块门打开等应保留温度的状态变化不同，当前合同未区分两者。

修复方向：区分“同一物体状态改变”和“物体替换”，保留现有几何签名缓存，但不要让签名相等代替物质生命周期判断。

## F12：材料持久化未闭合，mixed记录与恢复口径也不一致（恢复缺口）

`DormantChunkThermalState.capture`只遍历transportNodeCount，普通材料/phase不存档。但transport包括有材料热容mixed，它们会进入所谓Air checkpoint。当前`BrickMigrationKernel`恢复过滤为`isAirCell && !isSurfaceCell`，于是这些mixed记录不会恢复回自身的材料槽；同位置的dormant查询还可能先返回存下来的值。

后果：保存/卸载/重启/配方重载后不能承诺材料余热和相变进度延续；mixed可能在dormant查询与重新admit之间发生温度跳变。此前禁止Air均温恢复材料是正确的局部纠错，却没有因此完成材料持久化。

## F13：消费者仍主要读环境；拓扑间隙还会取整个Brick最暖transport（查询缺口）

`SoilThermometer`两条使用路径都调用`WorldTemperature.block`，经`gameplayPassiveEnvironment`进入sampleAir；确定无Air的实心位置回退自然/场，不读取普通material slot。crop/town等同样使用环境接口，这不能作为材料热状态已接通的证据。

此外`resolveLastPublication`在暂时缺少current cut时遍历该Brick全部transport取最大值，没有按查询点所属旧component或实际邻接区分。两个隔离气腔可能在间隙期被最暖代表值混用；mixed温度也在transport范围内。

修复方向：提供明确的材料/环境查询语义，按消费者物理需求接入；间隙fallback明确空间身份与有效性，不把“最暖”当通用点温度。

## F14：相变升温、降温和玩法场转换并非一个守恒闭环（既有双轨规则）

`StateTransitionData.heatingTransition`只生成SOLID升温和LIQUID蒸发阶段；GAS无升温阶段。冷凝/冻结等仍在`ServerLevelMixin_TemperatureUpdate`按环境阈值改变世界，没有通过phase池释放潜热。明确analytic bound达到阈值时还可绕过物理池走玩法转换。

这是源码里明示的非守恒玩法规则，并非本轮新发现一个隐藏漏扣能量bug；但它与“统一类现实材料相变”目标不一致。以后实现显热/潜热时必须确定两条路径的职责，避免一侧守恒、另一侧重置或重复变化。

## F15：热源/显示/读数仍有不同语义，不能互相当作温度证据（范围与显示问题）

`WorkerPhysicalSourceBindings`把功率绑定到AIR_FACE或declared/degraded loss；机器有功率不保证本体有对应温度。静态熔岩/火焰辐射按配置功率/参考发射温度形成接收通量，不是统一材料状态。BE/透明材质等热成像仍不在普通terrain范围。

当前红外已恢复“材料基础+解析场显示修正”，不是纯材料实测；没有材料且没有场时蓝色占位，又与实际≤-20°C同色。色标还在20°C以上饱和。因而图片不能独自证明热区的材料覆盖、真实温差或热源本体状态；此处应保持来源语义与诊断能力，不能用补背景或扩大色偏掩盖缺状态。

## 文档纠正

本轮发现`heat-production-and-network.md`仍写“材料由休眠Air mean恢复”“动态形状导致整Brick未解析”以及candidate microcells，均与当前源码不符。已改正为当前恢复过滤、whole-block候选和75通风/塔体V0规则。该纠正是文档同步，不代表修复了F01–F15。

## 修复优先顺序（供下一轮定案）

1. 先定义统一的材料状态、显热/相变关系与方块生命周期：F01/F04/F05/F11。禁止直接把固定相变阈值放进红外温度纹理冒充完成。
2. 再决定相变空间聚合与能量分配，以及几何/热容量/驻留的边界：F02/F03/F08/F09/F10。不能先批量增加节点而不明确其状态含义。
3. 补覆盖与持久化合同：F06/F07/F12，并核对相变双轨F14。含水固体、普通固体和相变状态不能相互抹去。
4. 最后统一材料消费者和显示验收：F13/F15。按同一位置对照原始状态、发布值、场合成值与像素地址。

这是依赖顺序，不是自动授权一次性实现所有系统。稀疏Page/Brick、原source ledger、异步worker和既有单纹理后处理可保留；目前没有证据要求改Embeddium顶点或另建渲染器。

## 验证边界与下一步用例

本轮为源码复查，未改生产/测试代码，未重复运行之前58项GameTest或1,440组GPU平面测试。它们验证了已有合同中的一部分，不证明本报告中的材料物理闭环。

后续针对性验证应覆盖：同一冷材料启用/停用相变配方后显热状态连续；部分形状加配方不改变几何；同Brick隔离相变块不同热输入；成员删除/替换/迁移后的H归属；转换目标初态；干/含水状态；封闭/再暴露、邻Brick驻留变化；热块替换同类冷块；存档前后材料/phase/mixed；材料探针与环境探针的分离。用户截图中每个蓝块的实际来源仍需运行期采样，不能把上述代码路径逐个套到图片上。
