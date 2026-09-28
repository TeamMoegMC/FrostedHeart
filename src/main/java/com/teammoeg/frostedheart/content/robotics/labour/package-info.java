/**
 * 机器等级与劳工分配系统。
 *
 * <h2>一、系统概述</h2>
 * <p>
 * 本包实现了一套「劳工池 → 机器等级」的分配与同步机制，核心目标如下：
 * </p>
 * <ul>
 *   <li>每个玩家拥有一个总劳工池，劳工数由若干「劳工提供者」汇总而来。</li>
 *   <li>每台机器可配置一个「期望等级」，该等级对应固定的劳工消耗。</li>
 *   <li>系统在总劳工限制下，自动计算每台机器的「实际生效等级」。</li>
 *   <li>总劳工变化时，按比例重新分配所有机器的实际等级，整数取整并映射到合法离散等级。</li>
 *   <li>机器离开加载范围时，其数据（期望等级、实际等级、占用劳工）仍被持久化，重新加载后恢复。</li>
 *   <li>当实际等级低于期望等级时，机器进入「缺乏状态」；释放出的劳工数会随机补给缺乏劳工的机器。</li>
 *   <li>提升期望等级时，若可用劳工不足，则直接拒绝操作。</li>
 * </ul>
 *
 * <h2>二、核心概念</h2>
 * <dl>
 *   <dt>劳工提供者（{@link LaborProvider}）</dt>
 *   <dd>劳工数的来源，例如发电机、劳工宿舍、增益效果等。每个提供者有一个非负整数值，
 *       所有提供者的值之和构成玩家的总劳工池。</dd>
 *
 *   <dt>总劳工池（totalLabor）</dt>
 *   <dd>由所有劳工提供者汇总得到的可用劳工上限。该值不直接由外部设置，而是随提供者的
 *       增删改自动重算。</dd>
 *
 *   <dt>等级消耗表（{@link LevelCostTable}）</dt>
 *   <dd>定义每个离散等级所需的劳工消耗，必须严格单调递增，且 0 级消耗为 0。
 *       提供「给定劳工数求最大合法等级」的二分查找。</dd>
 *
 *   <dt>机器数据（{@link MachineData}）</dt>
 *   <dd>每台机器的持久化数据，包含期望等级、实际等级、是否加载、是否缺乏等字段。
 *       即使机器不在加载范围内，该数据仍保留在管理器中。</dd>
 *
 *   <dt>机器实例（{@link Machine}）</dt>
 *   <dd>用于注册的接口，可以监听系统的实际等级变化并用于注册。</dd>
 *
 *   <dt>期望等级 / 实际等级</dt>
 *   <dd>期望等级由玩家配置，代表目标；实际等级由系统在总劳工约束下自动分配。
 *       正常情况下 {@code actualLevel <= desiredLevel}。</dd>
 *
 *   <dt>缺乏状态（deficient）</dt>
 *   <dd>当 {@code actualLevel < desiredLevel} 时，机器处于缺乏状态。
 *       所有缺乏机器的 ID 被维护在 {@code deficientSet} 中，用于增量补足。</dd>
 * </dl>
 *
 * <h2>三、主要类</h2>
 * <ul>
 *   <li>{@link LevelCostTable} —— 等级消耗表与等级映射工具。</li>
 *   <li>{@link ProviderData} —— 劳工提供者数据类。</li>
 *   <li>{@link MachineData} —— 机器持久化数据。</li>
 *   <li>{@link Machine} —— 机器实例接口。</li>
 *   <li>{@link MachineLevelManager} —— 核心管理器，持有提供者、机器、总劳工池与缺乏集合，
 *       负责所有分配、重算、补足与释放逻辑。</li>
 * </ul>
 *
 * <h2>四、核心不变式</h2>
 * <ol>
 *   <li>{@code totalPool == Σ provider.value}，总劳工池始终由劳工提供者汇总派生。</li>
 *   <li>{@code allocatedLabor == Σ cost(machine.actualLevel)}，包含所有机器（含未加载）。</li>
 *   <li>{@code allocatedLabor <= totalPool}，任何时候已分配劳工不得超过总劳工池。</li>
 *   <li>{@code actualLevel <= desiredLevel}，实际等级不会超过期望等级。</li>
 *   <li>{@code deficientSet} 恰好包含所有 {@code actualLevel < desiredLevel} 的机器 ID。</li>
 * </ol>
 *
 * <h2>五、主要流程</h2>
 * <h3>5.1 劳工提供者变更</h3>
 * <p>
 * 调用 {@code addProvider} / {@code updateProvider} / {@code removeProvider} 时，
 * 管理器自动重算 {@code providerSum} 并更新 {@code totalPool}：
 * </p>
 * <ul>
 *   <li>总劳工减少 → {@code reducePool}：按当前实际消耗从大到小依次把机器降低一级，
 *       每降一级释放的劳工立即归还池中；若一轮之后仍超出上限，则按新的占用重排再来一轮，
 *       直到已分配劳工不超过总劳工池，或所有机器都已降到 0 级。</li>
 *   <li>总劳工增加 → {@code increasePool}：按期望消耗比例重新分配每台机器的劳工数，
 *       并映射到合法等级。</li>
 * </ul>
 *
 * <h3>5.2 设置期望等级</h3>
 * <ul>
 *   <li>降低期望：若新期望低于当前实际等级，则立即降级并释放差额劳工；
 *       释放的劳工通过 {@code distributeSurplus} 随机补给缺乏劳工的机器。</li>
 *   <li>提升期望：更新期望，并计算所需额外劳工，若可用劳工不足则返回 {@code false}；
 *       足够则并将该机器直接提升至新期望。</li>
 * </ul>
 *
 * <h3>5.3 补足缺乏劳工的机器</h3>
 * <p>
 * {@code distributeSurplus} 从 {@code deficientSet} 中随机抽取机器，在预算内逐级提升，
 * 直到劳工用完或所有机器都满足期望。剩余劳工保留在池中。
 * </p>
 *
 * <h3>5.4 机器加载 / 卸载 / 释放</h3>
 * <ul>
 *   <li>{@code onMachineLoaded}：绑定实例，推送当前实际等级，并将缺乏状态同步到集合。</li>
 *   <li>{@code onMachineUnloaded}：解除实例绑定，但保留数据与占用劳工，缺乏状态不变。</li>
 *   <li>{@code releaseMachine}：彻底移除机器，归还其占用劳工，并立即尝试补给缺乏劳工的机器。</li>
 * </ul>
 *
 * <h2>六、设计要点</h2>
 * <ul>
 *   <li><b>整数运算</b>：按比例分配（{@code increasePool}）使用 {@code long} 中间量做乘除，
 *       最后截断为 {@code int}，等价于向下取整，避免浮点误差。</li>
 *   <li><b>离散等级映射</b>：{@code increasePool} 通过 {@link LevelCostTable#maxLevelForValue(int)}
 *       二分查找把比例结果落到合法等级；{@code reducePool} 每次只降一级（{@code level - 1}），
 *       不会越过相邻等级。</li>
 *   <li><b>增量优化</b>：维护 {@code deficientSet}，补足流程只遍历缺乏劳工的机器，而非全量机器。
 *       总劳工变化时，未加载机器可延迟重算（当前实现为立即全量重算，可在此基础上扩展脏标记）。</li>
 *   <li><b>随机分配</b>：补足时对候选集合做 {@code Collections.shuffle}，不考虑优先级，
 *       符合「随机分配」的需求。</li>
 *   <li><b>存档恢复</b>：提供接收 {@code Collection<MachineData>} 与
 *       {@code Collection<LaborProvider>} 的构造器，构造完成后自动重算
 *       {@code allocatedLabor}、{@code laborSum}、{@code deficientSet}，并校验不变式。</li>
 *   <li><b>防御性编程</b>：反序列化数据会被规范化（钳制等级范围、强制
 *       {@code actual <= desired}）；若机器占用超过劳工提供者总和，抛出异常快速失败。</li>
 * </ul>
 *
 * <h2>七、线程模型</h2>
 * <p>
 * 本包当前设计为<b>单线程</b>使用。所有公开方法均未做同步，调用方需保证在同一个逻辑线程
 * （如游戏主线程）中访问 {@link MachineLevelManager}。若需多线程访问，应在外部加锁，
 * 或将内部集合替换为并发容器并处理 {@code Random} 争用。
 * </p>
 *
 * <h2>八、典型用法</h2>
 * <pre>{@code
 * LevelCostTable table = new LevelCostTable(new int[]{0, 5, 12, 21, 32});
 * MachineLevelManager mgr = new MachineLevelManager(table);
 *
 * // 添加劳工提供者
 * mgr.addProvider(GlobalPos.of(Level.OVERWORLD,BlockPos.ZERO), 30);
 * mgr.addProvider(GlobalPos.of(Level.OVERWORLD,BlockPos.ZERO), 20);   // totalLabor = 50
 *
 * // 注册机器并设置期望等级
 * mgr.registerMachine(WeakReferenceSlot.of(...));
 * mgr.setDesiredLevel(GlobalPos.of(Level.OVERWORLD,BlockPos.ZERO), 3);
 *
 * // 释放机器，归还劳工并补给其他缺乏劳工的机器
 * mgr.releaseMachine(GlobalPos.of(Level.OVERWORLD,BlockPos.ZERO));
 * }</pre>
 *
 */

package com.teammoeg.frostedheart.content.robotics.labour;

