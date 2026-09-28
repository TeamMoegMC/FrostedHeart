/**
 * 机器等级与劳工分配系统（多劳工类型）。
 *
 * <h2>一、系统概述</h2>
 * <p>
 * 本包实现了一套「多劳工类型的提供者池 → 机器分类型等级」的分配与同步机制：
 * </p>
 * <ul>
 *   <li>劳工点数按 {@link LaborType 劳工类型}分别计算，每种类型有独立的池。</li>
 *   <li>每个池的总量由若干「劳工提供者」汇总而来，一个提供者可以同时提供多种劳工类型。</li>
 *   <li>一台机器可以同时使用多种劳工类型，每种劳工类型各有一个「期望等级」和一个「实际等级」。</li>
 *   <li>系统在每种类型各自的池限制下，独立计算每台机器在该类型上的「实际生效等级」。</li>
 *   <li>某个类型的池变化时，只重算该类型上的全部等级，其它劳工类型不受影响。</li>
 *   <li>机器离开加载范围时，其数据（各类型的期望等级、实际等级）仍被持久化，重新加载后恢复。</li>
 *   <li>某类型下实际等级低于期望等级即为「缺乏」；释放出的该类型点数会随机补给同类型下缺乏的机器。</li>
 *   <li>提升某类型的期望等级时，若该类型可用点数不足，则直接拒绝操作。</li>
 * </ul>
 *
 * <h2>二、核心概念</h2>
 * <dl>
 *   <dt>劳工类型（{@link LaborType}）</dt>
 *   <dd>注册在 {@link LaborType#registry} 中的类型对象（例如 {@code general}），表示一种劳工点数。
 *       所有池、等级、缺乏集合都按劳工类型分别维护，类型按注册名序列化。</dd>
 *
 *   <dt>劳工提供者（{@link ProviderData}）</dt>
 *   <dd>劳工点数的来源，例如发电机、劳工宿舍、增益效果等。每个提供者持有一张
 *       「劳工类型 → 非负整数点数」的映射；同一类型下所有提供者的值之和构成该类型的池。</dd>
 *
 *   <dt>池（pool）</dt>
 *   <dd>每种劳工类型一个池，保存该类型的总点数上限（totalPool）、已分配点数（allocatedSum）
 *       与该类型下缺乏的机器集合（deficientSet）。总点数不直接由外部设置，而是随提供者的
 *       增删改自动重算。</dd>
 *
 *   <dt>劳工等级配置（{@link MachineType.LaborCost}）</dt>
 *   <dd>机器为它使用的每种劳工类型各指定两个数值：<b>最大等级</b>与<b>每级所需劳工数量</b>。
 *       等级 {@code level} 的累计消耗为 {@code min(level, maxLevel) * costPerLevel}。
 *       同一个机器类型可以为它使用的每种劳工类型分别指定，也可以让多种类型共用同一组数值。</dd>
 *
 *   <dt>机器类型（{@link MachineType}）</dt>
 *   <dd>声明一台机器使用哪些劳工类型（{@link MachineType#getLaborTypes()}，顺序即主类型优先），
 *       以及每种劳工类型的等级消耗（{@link MachineType#getCost(LaborType, int)}）。</dd>
 *
 *   <dt>机器数据（{@link MachineData}）</dt>
 *   <dd>每台机器的持久化数据，包含每种劳工类型的期望等级与实际等级。即使机器不在加载范围内，
 *       该数据仍保留在管理器中。</dd>
 *
 *   <dt>机器实例（{@link Machine}）</dt>
 *   <dd>用于注册的接口，可以监听系统的实际等级变化并用于注册。</dd>
 *
 *   <dt>期望等级 / 实际等级</dt>
 *   <dd>按劳工类型分别维护。期望等级由玩家配置，代表目标；实际等级由系统在该类型的池约束下
 *       自动分配。正常情况每个类型都满足 {@code actualLevel <= desiredLevel}。</dd>
 *
 *   <dt>总实际等级</dt>
 *   <dd>{@link MachineData#getTotalActualLevel()} 为该机器各劳工类型实际等级之和，
 *       随等级变更增量维护，查询为 O(1)。无类型参数的重载
 *       （{@link MachineData#getActualLevel()}、{@link MachineData#getActualCost()} 等）
 *       都返回这种合计值。</dd>
 *
 *   <dt>缺乏状态（deficient）</dt>
 *   <dd>某劳工类型下 {@code actualLevel < desiredLevel} 即为缺乏。每个池各有一个 deficientSet
 *       维护该类型下缺乏的机器 ID，用于增量补足。</dd>
 * </dl>
 *
 * <h2>三、主要类</h2>
 * <ul>
 *   <li>{@link LaborType} —— 劳工类型注册表与类型对象。</li>
 *   <li>{@link MachineType} —— 机器类型接口，声明劳工类型与各自的「最大等级 + 每级所需」。</li>
 *   <li>{@link ProviderData} —— 劳工提供者数据类。</li>
 *   <li>{@link MachineData} —— 机器持久化数据（分类型的等级 + 合计缓存）。</li>
 *   <li>{@link Machine} —— 机器实例接口。</li>
 *   <li>{@link MachineLevelManager} —— 核心管理器，持有各类型的池、提供者与机器，
 *       负责所有分配、重算、补足与释放逻辑。</li>
 * </ul>
 *
 * <h2>四、核心不变式</h2>
 * <p>以下不变式对每个劳工类型 t 分别成立：</p>
 * <ol>
 *   <li>{@code totalPool(t) == Σ provider.value(t)}，池总量始终由该类型的提供者汇总派生。</li>
 *   <li>{@code allocatedSum(t) == Σ cost(t, machine.actualLevel(t))}，包含所有机器（含未加载）。</li>
 *   <li>{@code allocatedSum(t) <= totalPool(t)}。</li>
 *   <li>{@code actualLevel(t) <= desiredLevel(t)}。</li>
 *   <li>{@code deficientSet(t)} 恰好包含所有 {@code actualLevel(t) < desiredLevel(t)} 的机器 ID。</li>
 *   <li>{@code machine.getTotalActualLevel() == Σ actualLevel(t)}（缓存与实际值一致）。</li>
 * </ol>
 *
 * <h2>五、主要流程</h2>
 * <h3>5.1 劳工提供者变更</h3>
 * <p>
 * 调用 {@code updateProvider} / {@code removeProvider} / {@code replaceProviders} 时，
 * 管理器自动重算受影响劳工类型的 {@code providerSum} 并更新该类型的 {@code totalPool}：
 * </p>
 * <ul>
 *   <li>该类型总点数减少 → {@code reducePool}：按该类型下各机器的点数占用从大到小依次把机器降低一级，
 *       每降一级释放的点数立即归还池中；若一轮之后仍超出上限，则按新的占用重排再来一轮，
 *       直到已分配点数不超过总点数，或所有使用该类型的机器都已降到 0 级。</li>
 *   <li>该类型总点数增加 → {@code increasePool}：按该类型下的期望消耗比例重新分配每台机器的等级，
 *       并映射到合法等级。</li>
 * </ul>
 *
 * <h3>5.2 设置期望等级</h3>
 * <ul>
 *   <li>降低期望：若新期望低于该类型当前实际等级，则立即降级并释放差额点数；
 *       释放的点数通过 {@code distributeSurplus} 随机补给同类型下缺乏的机器。</li>
 *   <li>提升期望：更新期望，并计算所需额外点数，若该类型可用点数不足则返回 {@code false}；
 *       足够则把该机器在该类型上直接提升至新期望。</li>
 *   <li>无劳工类型参数的重载作用于该机器的主劳工类型（{@link MachineType#getPrimaryLaborType()}）。</li>
 * </ul>
 *
 * <h3>5.3 补足缺乏劳工的机器</h3>
 * <p>
 * {@code distributeSurplus} 从该劳工类型的 deficientSet 中随机抽取机器，在预算内逐级提升，
 * 直到点数用完或该类型下所有机器都满足期望。剩余点数保留在池中。
 * </p>
 *
 * <h3>5.4 机器加载 / 卸载 / 释放</h3>
 * <ul>
 *   <li>{@code registerMachine}：绑定实例，机器数据不存在时创建，存在且类型相同则只替换引用。</li>
 *   <li>{@code releaseMachine}：移除机器，逐劳工类型归还占用的点数并立即尝试补足同类型下缺乏的机器；
 *       机器实例的等级被推为 0。{@code LogisticState} 在拆除时调用它。</li>
 *   <li>机器不在加载范围内时数据与占用点数保持不变，缺乏状态也不变。</li>
 * </ul>
 *
 * <h2>六、设计要点</h2>
 * <ul>
 *   <li><b>按类型独立</b>：池、已分配点数、缺乏集合、等级重算全部以劳工类型为粒度，
 *       某个类型的提供者变化不会触碰其它类型的等级。</li>
 *   <li><b>合计查询 O(1)</b>：{@link MachineData} 在等级变更时增量维护各类型实际等级之和，
 *       每 tick 查询「总实际等级」不需要遍历劳工类型。</li>
 *   <li><b>整数运算</b>：按比例分配（{@code increasePool}）使用 {@code long} 中间量做乘除，
 *       最后截断为 {@code int}，等价于向下取整，避免浮点误差。</li>
 *   <li><b>离散等级映射</b>：{@code increasePool} 通过
 *       {@link MachineType#maxLevelForValue(LaborType, int)} 把比例结果落到合法等级；
 *       {@code reducePool} 每次只降一级（{@code level - 1}），不会越过相邻等级。</li>
 *   <li><b>增量优化</b>：每个池维护自己的 deficientSet，补足流程只遍历缺乏的机器，而非全量机器。</li>
 *   <li><b>随机分配</b>：补足时对候选集合做 {@code Collections.shuffle}，不考虑优先级，
 *       符合「随机分配」的需求。</li>
 *   <li><b>存档恢复</b>：提供接收 {@code Collection<MachineData>} 与
 *       {@code Collection<ProviderData>} 的构造器，构造完成后自动重算各类型的
 *       {@code allocatedSum}、{@code providerSum}、deficientSet。</li>
 *   <li><b>数据规范化</b>：读档时只保留机器类型声明的劳工类型，并把等级钳制到合法范围。</li>
 * </ul>
 *
 * <h2>七、线程模型</h2>
 * <p>
 * 本包当前设计为<b>单线程</b>使用。所有公开方法均未做同步，调用方需保证在同一个逻辑线程
 * （如游戏主线程）中访问 {@link MachineLevelManager}。若需多线程访问，应在外部加锁，
 * 或将内部集合替换为并发容器。
 * </p>
 *
 * <h2>八、典型用法</h2>
 * <pre>{@code
 * // 机器类型：用 builder 逐种劳工类型配置（添加顺序即主类型优先）
 * LaborType general = LaborTypes.GENERAL;
 * LaborType skilled = LaborType.register("skilled");
 * MachineType type = MachineType.register(BaseMachineType.builder()
 *     .labor(general, 5, 8)                      // 最大 5 级，每级 8 点
 *     .labor(skilled, 2, 8)                      // 最大 2 级，每级 8 点
 *     .build(), "logistic");
 *
 * // 多种劳工类型共用同一组数值：
 * // BaseMachineType.builder().labor(List.of(general, skilled), 5, 8).build()
 * // 单一类型也可以直接用构造器：new BaseMachineType(general, 5, 8)
 *
 * MachineLevelManager mgr = new MachineLevelManager(teamData);
 *
 * // 提供者：同一个方块可以提供多种劳工
 * mgr.updateProvider(pos, general, 30);
 * mgr.updateProvider(pos, skilled, 10);
 *
 * // 设置某台机器在某劳工类型上的期望等级
 * mgr.setDesiredLevel(machinePos, general, 3);
 *
 * // 查询合计实际等级（O(1)）
 * int total = mgr.getMachine(machinePos).getTotalActualLevel();
 * }</pre>
 *
 */

package com.teammoeg.frostedheart.content.robotics.labor;
