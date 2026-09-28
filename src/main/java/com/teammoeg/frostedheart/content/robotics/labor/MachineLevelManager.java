package com.teammoeg.frostedheart.content.robotics.labor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.chorda.dataholders.SpecialData;
import com.teammoeg.chorda.dataholders.SpecialDataHolder;
import com.teammoeg.chorda.util.struct.WeakReferenceSlot;

import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import net.minecraft.core.GlobalPos;
import net.minecraft.util.Mth;

/**
 * 机器等级管理器。
 *
 * <p>劳工点数按 {@link LaborType 劳工类型}分别计算：每种劳工类型有一个独立的池，
 * 池的总量由提供者汇总派生，被各机器在该类型上的实际等级占用。一台机器可以同时使用
 * 多种劳工类型，每种劳工类型的期望等级与实际等级互相独立。</p>
 *
 * 核心不变式（对每个劳工类型分别成立）：
 *   allocatedSum(laborType) = Σ cost(laborType, actualLevel)   （所有机器，包含未加载）
 *   allocatedSum(laborType) <= totalPool(laborType)
 *
 * 性能优化：
 *   deficientSet 按劳工类型维护所有 actualLevel &lt; desiredLevel 的机器，
 *   补足流程只遍历该集合，避免全量扫描；
 *   MachineData 缓存各劳工类型实际等级之和，每 tick 的等级查询是 O(1)。
 */
public class MachineLevelManager implements SpecialData{

	
	public static final Codec<MachineLevelManager> CODEC = RecordCodecBuilder.create(t -> t.group(
		Codec.list(MachineData.CODEC).fieldOf("machines").forGetter(o->new ArrayList<>(o.machines.values())),
		Codec.list(ProviderData.CODEC).fieldOf("generators").forGetter(o->new ArrayList<>(o.providers.values()))
		).apply(t, MachineLevelManager::new));

	/** 单一劳工类型的池状态。 */
	private static final class Pool {
		/** 该劳工类型的提供者合计（含额外提供者）。 */
		private int providerSum;
		/** 该劳工类型的额外提供者点数（不来自方块），已计入 providerSum。 */
		private int extraProvider;
		/** 该劳工类型的总点数上限。 */
		private int totalPool;
		/** 该劳工类型已分配点数（Σ 所有机器在该类型上的实际消耗）。 */
		private int allocatedSum;
		/** 该劳工类型下 actual < desired 的机器。 */
		private final Set<GlobalPos> deficientSet = new HashSet<>();
	}

    /** 所有机器数据（包含未加载机器） */
    private final Map<GlobalPos, MachineData> machines = new HashMap<>();

    /** 劳工类型 → 池状态 */
    private final Map<LaborType, Pool> pools = new LinkedHashMap<>();

    /** 点数提供者表（有序，便于 UI 展示与调试） */
    private final Map<GlobalPos, ProviderData> providers = new LinkedHashMap<>();

	@SuppressWarnings("rawtypes")
	public MachineLevelManager(SpecialDataHolder teamData) {
		super();
		replaceProviders(null);
	}

	public MachineLevelManager(List<MachineData> initialMachines,List<ProviderData> generators) {

	    if (initialMachines != null) {
	        for (MachineData raw : initialMachines) {

	            GlobalPos id = raw.getPos();
	            if (id == null) {
	                throw new IllegalArgumentException("machine pos must not be null");
	            }
	            if (machines.containsKey(id)) {
	                throw new IllegalArgumentException("duplicate machine pos: " + id);
	            }
	            machines.put(id, raw);

	            for (LaborType laborType : raw.getLaborTypes()) {
	                pool(laborType).allocatedSum += raw.getActualCost(laborType);
	            }
	        }
	    }
	    // 初始总池等于已分配量，随后由 replaceProviders 按提供者重算
	    for (Pool pool : pools.values()) {
	        pool.totalPool = pool.allocatedSum;
	    }
	    rebuildDeficientSet();
	    this.replaceProviders(generators);
	}

    // ============================================================
    // 池的内部访问
    // ============================================================

    /** 取得（必要时创建）某个劳工类型的池。 */
    private Pool pool(LaborType laborType) {
        return pools.computeIfAbsent(laborType, k -> new Pool());
    }

    /** 只读取某个劳工类型的池，不存在时返回 null，不产生副作用。 */
    private Pool poolOrNull(LaborType laborType) {
        return pools.get(laborType);
    }

    // ============================================================
    // 查询
    // ============================================================

    /** 已知劳工类型（被机器或提供者使用过），按首次出现顺序。 */
    public Set<LaborType> getLaborTypes() { return Collections.unmodifiableSet(new LinkedHashSet<>(pools.keySet())); }

    public int getTotalPool(LaborType laborType)   { Pool p = poolOrNull(laborType); return p == null ? 0 : p.totalPool; }
    public int getAllocatedSum(LaborType laborType){ Pool p = poolOrNull(laborType); return p == null ? 0 : p.allocatedSum; }
    public int getAvailable(LaborType laborType)   { Pool p = poolOrNull(laborType); return p == null ? 0 : p.totalPool - p.allocatedSum; }

    /** 所有劳工类型的总点数上限之和。 */
    public int getTotalPool() {
        int sum = 0;
        for (Pool p : pools.values()) sum += p.totalPool;
        return sum;
    }

    /** 所有劳工类型已分配点数之和。 */
    public int getAllocatedSum() {
        int sum = 0;
        for (Pool p : pools.values()) sum += p.allocatedSum;
        return sum;
    }

    /** 所有劳工类型的可用点数之和。 */
    public int getAvailable() {
        int sum = 0;
        for (Pool p : pools.values()) sum += p.totalPool - p.allocatedSum;
        return sum;
    }

    /** 缺少数值的机器条目数（按劳工类型分别计数）。 */
    public int getDeficientCount() {
        int sum = 0;
        for (Pool p : pools.values()) sum += p.deficientSet.size();
        return sum;
    }

    public int getDeficientCount(LaborType laborType) {
        Pool p = poolOrNull(laborType);
        return p == null ? 0 : p.deficientSet.size();
    }

    public MachineData getMachine(GlobalPos machineId) {
        return machines.get(machineId);
    }

    /** 该机器各劳工类型实际等级之和；机器不存在时为 0。 */
    public int getActualLevel(GlobalPos machineId) {
        MachineData d = machines.get(machineId);
        return d == null ? 0 : d.getTotalActualLevel();
    }

    /** 该机器在给定劳工类型上的实际等级；机器不存在或类型未使用时为 0。 */
    public int getActualLevel(GlobalPos machineId, LaborType laborType) {
        MachineData d = machines.get(machineId);
        return d == null ? 0 : d.getActualLevel(laborType);
    }

    /** 给定劳工类型下所有缺少数值的机器。 */
    public Set<GlobalPos> getDeficientMachineIds(LaborType laborType) {
        Pool p = poolOrNull(laborType);
        return p == null ? Collections.emptySet() : Collections.unmodifiableSet(new LinkedHashSet<>(p.deficientSet));
    }

    /** 所有劳工类型下缺少数值的机器（并集）。 */
    public Set<GlobalPos> getDeficientMachineIds() {
        Set<GlobalPos> all = new LinkedHashSet<>();
        for (Pool p : pools.values()) all.addAll(p.deficientSet);
        return Collections.unmodifiableSet(all);
    }

    /** 注册机器（幂等）。 */
    public MachineData registerMachine(WeakReferenceSlot<Machine> machine) {
    	GlobalPos machineId=machine.getOrThrow().getMachineLocation();
    	MachineData machineData=getMachine(machineId);
    	if(machineData==null) {
    		machines.put(machineId,machineData= new MachineData(machine,machineId));
    	}else if(!Objects.equals(machineData.getType(), machine.getOrThrow().getType())) {
    		releaseMachine(machineId);
    		machines.put(machineId,machineData= new MachineData(machine,machineId));
    	}else {
    		machineData.setInstance(machine);
    	}
        return machineData;
    }
    /**
     * 释放一台机器，把它在每种劳工类型上占用的点数归还对应的池。
     *
     * <p>归还的点数会立刻通过 {@link #distributeSurplus(LaborType, int)} 随机补给
     * 同种劳工类型下缺少数值的机器（如果有）。若无人缺乏，则剩余点数留在池中。</p>
     *
     * <p>该方法幂等：释放不存在的机器返回 false，不抛异常。</p>
     *
     * @param machineId 机器 ID
     * @return true 表示机器存在并已被释放；false 表示机器从未注册过
     */
    public boolean releaseMachine(GlobalPos machineId) {
        MachineData data = machines.remove(machineId);
        if (data == null) {
            return false;
        }

        // 1. 逐劳工类型归还占用的点数，并立刻补给该类型下缺乏的机器
        for (LaborType laborType : data.getLaborTypes()) {
            Pool pool = pool(laborType);
            pool.deficientSet.remove(machineId);

            int releasedCost = data.getActualCost(laborType);
            data.setActualLevel(laborType, 0);
            data.setDesiredLevel(laborType, 0);

            if (releasedCost > 0) {
                pool.allocatedSum -= releasedCost;
                distributeSurplus(laborType, releasedCost);
            }
        }

        // 2. 解绑实例（若已加载），并把等级清零，避免残留状态被误读
        Machine instance = data.getInstance();
        if (instance != null) {
            data.setInstance(null);
            // 主动把等级推给即将失去绑定的机器，避免其继续按旧等级运行
            instance.applyLevel(0);
        }

        return true;
    }

    /**
     * 批量释放。方便存档清理或玩家一次拆掉整片基地。
     *
     * @return 实际被释放的机器数量
     */
    public int releaseMachines(Collection<GlobalPos> machineIds) {
        if (machineIds == null || machineIds.isEmpty()) return 0;
        int count = 0;
        for (GlobalPos id : machineIds) {
            if (releaseMachine(id)) count++;
        }
        return count;
    }

    // ============================================================
    // 池变化
    // ============================================================

    /** 手动设置某个劳工类型的总点数上限。 */
    public void setTotalPool(LaborType laborType, int newTotal) {
        if (newTotal < 0) throw new IllegalArgumentException("totalPool must be >= 0");
        Pool pool = pool(laborType);
        if (newTotal == pool.totalPool) return;

        int oldTotal = pool.totalPool;
        pool.totalPool = newTotal;

        if (newTotal < oldTotal) {
            reducePool(laborType, newTotal);
        } else {
            increasePool(laborType, newTotal);
        }

        rebuildDeficientSet();
    }

    /**
     * 某个劳工类型的池缩小：按该类型下各机器的点数占用从大到小依次降低实际等级。
     *
     * <p>每台机器一次只降一级（降到前一个合法等级），释放的点数立即计入可用池；
     * 一轮走完如果已分配点数仍超过 {@code newTotal}，就按新的占用量重新排序再来一轮，
     * 直到 {@code allocatedSum <= newTotal}，或者所有使用该类型的机器都已经降到 0 级为止。</p>
     *
     * <p>与按比例缩放不同，这里不做等级的除法映射：每台机器都只下降到相邻的下一级，
     * 因此不会越过任何合法等级；剩余未分配的点数保留在池中。</p>
     */
    private void reducePool(LaborType laborType, int newTotal) {
        Pool pool = pool(laborType);
        if (pool.allocatedSum <= 0 || pool.allocatedSum <= newTotal) return;

        List<MachineData> ordered = new ArrayList<>(machines.values());
        ordered.sort(Comparator.comparingInt((MachineData d) -> d.getActualCost(laborType)).reversed());

        boolean lowered = true;
        while (pool.allocatedSum > newTotal && lowered) {
            lowered = false;

            for (MachineData data : ordered) {
                if (pool.allocatedSum <= newTotal) break;

                int level = data.getActualLevel(laborType);
                if (level <= 0) continue; // 已到 0 级或未使用该劳工类型，无法再降

                int freed = data.getType().getCost(laborType, level) - data.getType().getCost(laborType, level - 1);
                if (freed <= 0) continue;

                data.setActualLevel(laborType, level - 1);
                pool.allocatedSum -= freed;
                lowered = true;
            }

            if (pool.allocatedSum > newTotal) {
                // 占用已经变化，重新排序后再来一轮，保证仍然从当前占用最大的机器开始降
                ordered.sort(Comparator.comparingInt((MachineData d) -> d.getActualCost(laborType)).reversed());
            }
        }
    }

    /** 某个劳工类型的池扩大：按该类型下的期望消耗比例重新分配实际等级。 */
    private void increasePool(LaborType laborType, int newTotal) {
        Pool pool = pool(laborType);

        long totalDesiredCost = 0L;
        for (MachineData d : machines.values()) {
            totalDesiredCost += d.getType().getCost(laborType, d.getDesiredLevel(laborType));
        }

        if (totalDesiredCost == 0) {
            for (MachineData d : machines.values()) {
                d.setActualLevel(laborType, 0);
            }
            pool.allocatedSum = 0;
            return;
        }

        int newAllocated = 0;
        for (MachineData d : machines.values()) {

            int desiredCost = d.getType().getCost(laborType, d.getDesiredLevel(laborType));
            // target = floor(newTotal * desiredCost / totalDesiredCost)
            long targetValue = (long) newTotal * desiredCost / totalDesiredCost;
            if (targetValue > desiredCost) targetValue = desiredCost; // 不超过期望
            int newLevel = d.getType().maxLevelForValue(laborType, (int) targetValue);
            d.setActualLevel(laborType, newLevel);
            newAllocated += d.getType().getCost(laborType, newLevel);
        }
        pool.allocatedSum = newAllocated;
    }

    // ============================================================
    // 期望等级变更
    // ============================================================
    /**
     * 设置某台机器主劳工类型的期望等级；多劳工类型的机器请使用
     * {@link #setDesiredLevel(GlobalPos, LaborType, int)}。
     *
     * @return true 表示数值充足；false 表示数值不足。
     */
    public boolean setDesiredLevel(WeakReferenceSlot<Machine> machine, int newDesired) {
    	if(machine.isPresent())
    		return setDesiredLevel(machine.getOrThrow().getMachineLocation(),newDesired);
    	return false;
    }
    /**
     * 设置某台机器主劳工类型的期望等级。
     *
     * @return true 表示数值充足；false 表示数值不足。
     */
    public boolean setDesiredLevel(Machine machine, int newDesired) {
    	return setDesiredLevel(machine.getMachineLocation(),newDesired);
    }
    /**
     * 设置某台机器主劳工类型的期望等级。
     *
     * @return true 表示数值充足；false 表示数值不足。
     */
    public boolean setDesiredLevel(GlobalPos machineId, int newDesired) {
        MachineData data = machines.get(machineId);
        if (data == null) {
            throw new IllegalArgumentException("Unknown machine: " + machineId);
        }
        return setDesiredLevel(machineId, data.getType().getPrimaryLaborType(), newDesired);
    }

    /**
     * 设置某台机器在指定劳工类型上的期望等级。
     *
     * @return true 表示数值充足；false 表示数值不足。
     */
    public boolean setDesiredLevel(WeakReferenceSlot<Machine> machine, LaborType laborType, int newDesired) {
    	if(machine.isPresent())
    		return setDesiredLevel(machine.getOrThrow().getMachineLocation(),laborType,newDesired);
    	return false;
    }

    /**
     * 设置某台机器在指定劳工类型上的期望等级。
     *
     * @return true 表示数值充足；false 表示数值不足。
     */
    public boolean setDesiredLevel(Machine machine, LaborType laborType, int newDesired) {
    	return setDesiredLevel(machine.getMachineLocation(),laborType,newDesired);
    }

    /**
     * 设置某台机器在指定劳工类型上的期望等级。
     *
     * @return true 表示数值充足；false 表示数值不足。
     */
    public boolean setDesiredLevel(GlobalPos machineId, LaborType laborType, int newDesired) {
        MachineData data = machines.get(machineId);
        if (data == null) {
            throw new IllegalArgumentException("Unknown machine: " + machineId);
        }
        if (!data.usesLabor(laborType)) {
            throw new IllegalArgumentException("Machine " + machineId + " does not use labor type " + laborType);
        }

    	MachineType costTable = data.getType();
        newDesired = Mth.clamp(newDesired, 0, costTable.maxLevel(laborType));
        int oldDesired = data.getDesiredLevel(laborType);
        if (newDesired == oldDesired) return true;

        if (newDesired < oldDesired) {
            lowerDesired(data, laborType, newDesired);
            return true;
        } else {
            return raiseDesired(data, laborType, newDesired);
        }
    }

    /** 降低期望：立即降级，释放的数值随机补给同类型下缺少数值的机器。 */
    private void lowerDesired(MachineData data, LaborType laborType, int newDesired) {
        int oldActual = data.getActualLevel(laborType);
        data.setDesiredLevel(laborType, newDesired);

    	MachineType costTable = data.getType();
        if (newDesired < oldActual) {
            int oldCost = costTable.getCost(laborType, oldActual);
            int newCost = costTable.getCost(laborType, newDesired);
            int freed = oldCost - newCost;

            data.setActualLevel(laborType, newDesired);
            pool(laborType).allocatedSum -= freed;

            if (freed > 0) {
                distributeSurplus(laborType, freed);
            }
        }
        updateDeficientStatus(data, laborType);
    }

    /** 提升期望：数值不足则拒绝；足够则允许，并把该机器直接拉到新期望。 */
    private boolean raiseDesired(MachineData data, LaborType laborType, int newDesired) {

    	MachineType costTable = data.getType();
    	Pool pool = pool(laborType);
        int currentCost = costTable.getCost(laborType, data.getActualLevel(laborType));
        int newDesiredCost = costTable.getCost(laborType, newDesired);
        int requiredExtra = Math.max(0, newDesiredCost - currentCost);
        data.setDesiredLevel(laborType, newDesired);
        if (pool.totalPool - pool.allocatedSum < requiredExtra) {
            // 数值不足：期望已经生效，实际等级维持原样，该机器进入缺乏状态
            updateDeficientStatus(data, laborType);
            return false;
        }

        
        if (data.getActualLevel(laborType) < newDesired) {
            data.setActualLevel(laborType, newDesired);
            pool.allocatedSum += requiredExtra;
        }
        updateDeficientStatus(data, laborType);
        return true;
    }

    // ============================================================
    // 缺少数值分配
    // ============================================================

    /**
     * 把 surplus 点数随机分配给指定劳工类型下 deficientSet 中的机器，直到用完或全部满足。
     * 剩余未分配的点数保留在池中（不影响 allocatedSum）。
     */
    private void distributeSurplus(LaborType laborType, int surplus) {
        Pool pool = pool(laborType);
        if (surplus <= 0 || pool.deficientSet.isEmpty()) return;

        List<GlobalPos> candidates = new ArrayList<>(pool.deficientSet);
        Collections.shuffle(candidates);

        int remaining = surplus;
        for (GlobalPos id : candidates) {
            if (remaining <= 0) break;

            MachineData data = machines.get(id);
            if (data == null || !data.isDeficient(laborType)) continue;

            remaining = upgradeTowardDesired(data, laborType, remaining);
            updateDeficientStatus(data, laborType);
        }
    }

    /**
     * 在预算内把机器在指定劳工类型上的等级从 actualLevel 逐级升向 desiredLevel。
     * 若某级消耗超过剩余预算则停止，不拆分等级。
     *
     * @return 剩余未使用的数值
     */
    private int upgradeTowardDesired(MachineData data, LaborType laborType, int budget) {
        int current = data.getActualLevel(laborType);
        int target  = data.getDesiredLevel(laborType);

    	MachineType costTable = data.getType();
    	Pool pool = pool(laborType);
        while (current < target) {
            int stepCost = costTable.getCost(laborType, current + 1) - costTable.getCost(laborType, current);
            if (budget < stepCost) break;
            budget -= stepCost;
            current++;
            pool.allocatedSum += stepCost;
        }
        data.setActualLevel(laborType, current);
        return budget;
    }

    // ============================================================
    // 缺乏状态维护
    // ============================================================

    private void updateDeficientStatus(MachineData data, LaborType laborType) {
        Pool pool = pool(laborType);
        if (data.isDeficient(laborType)) {
            pool.deficientSet.add(data.getPos());
        } else {
            pool.deficientSet.remove(data.getPos());
        }
    }

    private void rebuildDeficientSet() {
        for (Pool pool : pools.values()) {
            pool.deficientSet.clear();
        }
        for (MachineData d : machines.values()) {
            for (LaborType laborType : d.getLaborTypes()) {
                if (d.isDeficient(laborType)) pool(laborType).deficientSet.add(d.getPos());
            }
        }
    }

    /**
     * 修改提供者在某个劳工类型上提供的点数。新值可以与旧值相同（幂等，不触发重算）。
     *
     * @return true 表示值发生变化并已应用；false 表示新值与旧值相同
     */
    public boolean updateProvider(GlobalPos pos, LaborType laborType, int newValue) {
        if (newValue < 0) {
            throw new IllegalArgumentException("value must be >= 0, got " + newValue);
        }
        ProviderData p = providers.get(pos);
        int delta;
        if (p == null) {
        	delta = newValue;
        	p = new ProviderData(pos, laborType, newValue);
            providers.put(pos, p);
        }else {
        	delta = newValue - p.getValue(laborType);
	        if (delta == 0) return false;
	        p.setValueInternal(laborType, newValue);
        }
        pool(laborType).providerSum += delta;
        applyProviderSumChange(laborType);
        rebuildDeficientSet();
        return true;
    }

    /**
     * 删除一个提供者（连同它提供的所有劳工类型）。
     *
     * @return true 表示存在并已删除；false 表示不存在
     */
    public boolean removeProvider(GlobalPos pos) {
        ProviderData p = providers.remove(pos);
        if (p == null) return false;

        for (LaborType laborType : new ArrayList<>(p.getValues().keySet())) {
            Pool pool = pool(laborType);
            pool.providerSum -= p.getValue(laborType);
            applyProviderSumChange(laborType);
        }
        rebuildDeficientSet();
        return true;
    }

    /** 某个劳工类型的提供者合计（含额外提供者）。 */
    public int getProviderSum(LaborType laborType) {
        Pool pool = poolOrNull(laborType);
        return pool == null ? 0 : pool.providerSum;
    }

    /** 所有劳工类型的提供者合计之和。 */
    public int getProviderSum() {
        int sum = 0;
        for (Pool pool : pools.values()) sum += pool.providerSum;
        return sum;
    }

    /** 某个劳工类型的额外提供者点数（不来自方块）。 */
    public int getExtraProvider(LaborType laborType) {
        Pool pool = poolOrNull(laborType);
        return pool == null ? 0 : pool.extraProvider;
    }

    public ProviderData getProvider(GlobalPos pos) {
        return providers.get(pos);
    }

    public Collection<ProviderData> getProviders() {
        return Collections.unmodifiableCollection(providers.values());
    }

    /**
     * 批量替换整个提供者集合（用于读档 / 网络同步）。
     * 会按差量重算每个受影响的劳工类型，并触发对应的 reduce / increase。
     */
    public void replaceProviders(Collection<ProviderData> newProviders) {
        // 旧集合里出现过的类型也要重算（可能已经变成 0）
        Set<LaborType> affected = new LinkedHashSet<>(pools.keySet());
        providers.clear();

        if (newProviders != null) {
            for (ProviderData p : newProviders) {
                if (p == null) continue;
                providers.put(p.getPos(), p);
            }
        }

        // 按去重后的提供者集合汇总，重复 pos 只按最后一份计算
        Map<LaborType, Integer> sums = new LinkedHashMap<>();
        for (ProviderData p : providers.values()) {
            for (Reference2IntMap.Entry<LaborType> e : p.getValues().reference2IntEntrySet()) {
                sums.merge(e.getKey(), e.getIntValue(), Integer::sum);
            }
        }
        affected.addAll(sums.keySet());

        for (LaborType laborType : affected) {
            Pool pool = pool(laborType);
            pool.providerSum = sums.getOrDefault(laborType, 0) + pool.extraProvider;
            applyProviderSumChange(laborType);
        }
        rebuildDeficientSet();
    }

    /** 设置某个劳工类型的额外提供者点数（不来自方块）。 */
    public void setExtraProviderValue(LaborType laborType, int value) {
        if (value < 0) {
            throw new IllegalArgumentException("value must be >= 0, got " + value);
        }
        Pool pool = poolOrNull(laborType);
        if (pool == null) {
            if (value == 0) return; // 既没有池也没有额外点数，不需要建立条目
            pool = pool(laborType);
        }
        if (pool.extraProvider == value) return;

        pool.providerSum += value - pool.extraProvider;
        pool.extraProvider = value;

        applyProviderSumChange(laborType);
        rebuildDeficientSet();
    }

    /**
     * 内部：某个劳工类型的提供者总和变化后，重新驱动该类型的池。
     * 若新总和 == 旧总和，不做任何事。缺乏集合由调用方统一重建。
     */
    private void applyProviderSumChange(LaborType laborType) {
        Pool pool = pool(laborType);
        if (pool.providerSum == pool.totalPool) return;

        int oldTotal = pool.totalPool;
        pool.totalPool = pool.providerSum;

        if (pool.providerSum < oldTotal) {
            reducePool(laborType, pool.providerSum);
        } else {
            increasePool(laborType, pool.providerSum);
        }
    }
}
