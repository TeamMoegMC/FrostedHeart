package com.teammoeg.frostedheart.content.robotics;
import java.util.*;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.chorda.dataholders.SpecialData;
import com.teammoeg.chorda.dataholders.SpecialDataHolder;
import com.teammoeg.chorda.util.struct.WeakReferenceSlot;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;

/**
 * 机器等级管理器。
 *
 * 核心不变式：
 *   allocatedSum = Σ cost(actualLevel)   （所有机器，包含未加载）
 *   allocatedSum <= totalPool
 *
 * 性能优化：
 *   deficientSet 维护所有 actualLevel < desiredLevel 的机器，
 *   补足流程只遍历该集合，避免全量扫描。
 */
public class MachineLevelManager implements SpecialData{

	
	public static final Codec<MachineLevelManager> CODEC = RecordCodecBuilder.create(t -> t.group(
		Codec.list(MachineData.CODEC).fieldOf("machines").forGetter(o->new ArrayList<>(o.machines.values())),
		Codec.list(ProviderData.CODEC).fieldOf("generators").forGetter(o->new ArrayList<>(o.providers.values()))
		).apply(t, MachineLevelManager::new));

    /** 总数值池上限 */
    @Getter
    private int totalPool;

    /** 所有机器实际等级消耗之和（缓存） */
    @Getter
    private transient int allocatedSum;

    /** 所有机器数据（包含未加载机器） */
    private final Map<GlobalPos, MachineData> machines = new HashMap<>();

    /** 缺少数值的机器 ID 集合（actual < desired） */
    private final Set<GlobalPos> deficientSet = new HashSet<>();
    
    /** 点数提供者表（有序，便于 UI 展示与调试） */
    private final Map<GlobalPos, ProviderData> providers = new HashMap<>();

    /** 提供者贡献的总点数（缓存） */
    private int providerSum = 0;
    @Getter
    private int extraProvider=0;
    
	@SuppressWarnings("rawtypes")
	public MachineLevelManager(SpecialDataHolder teamData) {
		super();
		replaceProviders(null);
	}
	
	public MachineLevelManager(List<MachineData> initialMachines,List<ProviderData> generators) {

	    this.totalPool = 0;
	    this.allocatedSum = 0;

	    if (initialMachines == null || initialMachines.isEmpty()) {
	        return;
	    }

	    int sum = 0;
	    for (MachineData raw : initialMachines) {

	        GlobalPos id = raw.getPos();
	        if (id == null) {
	            throw new IllegalArgumentException("machineId must not be null/empty");
	        }
	        if (machines.containsKey(id)) {
	            throw new IllegalArgumentException("duplicate machineId: " + id);
	        }
	        machines.put(id, raw);

	        sum += raw.getType().getCost(raw.getActualLevel());
	        
	    }
	    rebuildDeficientSet();
	    this.totalPool=sum;
	    this.allocatedSum = sum;
	    this.replaceProviders(generators);
	}

    // ============================================================
    // 查询
    // ============================================================
    public int getAvailable()      { return totalPool - allocatedSum; }
    public int getDeficientCount() { return deficientSet.size(); }

    public MachineData getMachine(GlobalPos machineId) {
        return machines.get(machineId);
    }

    public int getActualLevel(GlobalPos machineId) {
        MachineData d = machines.get(machineId);
        return d == null ? 0 : d.getActualLevel();
    }

    public Set<GlobalPos> getDeficientMachineIds() {
        return Collections.unmodifiableSet(deficientSet);
    }

    /** 注册机器（幂等）。加载范围内的机器实例由 onMachineLoaded 绑定。 */
    public MachineData registerMachine(WeakReferenceSlot<Machine> machine) {
    	GlobalPos machineId=machine.getOrThrow().getMachineLocation();
    	MachineData machineData=getMachine(machineId);
    	if(machineData==null) {
    		machines.put(machineId,machineData= new MachineData(machine,machineId));
    	}else if(!Objects.equals(machineData.getType(), machine.orElse(null).getType())) {
    		releaseMachine(machineId);

    		machines.put(machineId,machineData= new MachineData(machine,machineId));
    	}else {
    		machineData.setInstance(machine);
    	}
        return machineData;
    }
    /**
     * 释放一台机器，把它占用的点数归还数值池。
     *
     * <p>归还的点数会立刻通过 {@link #distributeSurplus(int)} 随机补给
     * 当前所有缺少数值的机器（如果有）。若无人缺乏，则剩余点数留在池中。</p>
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

        // 1. 从缺乏集合移除
        deficientSet.remove(machineId);

        // 2. 归还该机器占用的点数
        int releasedCost = data.getType().getCost(data.getActualLevel());
        allocatedSum -= releasedCost;

        // 3. 解绑实例（若已加载），并把等级清零，避免残留状态被误读
        Machine instance = data.getInstance();
        if (instance != null) {
            data.setInstance(null);
            // 主动把等级推给即将失去绑定的机器，避免其继续按旧等级运行
            instance.applyLevel(0);
        }

        data.setActualLevel(0);
        data.setDesiredLevel(0);

        // 4. 归还的点数立即补给缺乏机器
        if (releasedCost > 0) {
            distributeSurplus(releasedCost);
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
    // 总池变化
    // ============================================================

    public void setTotalPool(int newTotal) {
        if (newTotal < 0) throw new IllegalArgumentException("totalPool must be >= 0");
        if (newTotal == totalPool) return;

        int oldTotal = totalPool;
        totalPool = newTotal;

        if (newTotal < oldTotal) {
            reducePool(newTotal);
        } else {
            increasePool(newTotal);
        }

        rebuildDeficientSet();
    }

    /** 总池缩小：按比例缩减每台机器的数值，再映射到合法等级。 */
    private void reducePool(int newTotal) {
        if (allocatedSum <= 0) return;

        final long oldAllocated = allocatedSum;
        int newAllocated = 0;

        for (MachineData data : machines.values()) {
        	MachineType costTable=data.getType();
            int currentCost = costTable.getCost(data.getActualLevel());
            // target = floor(currentCost * newTotal / oldAllocated)
            long targetValue = currentCost * (long) newTotal / oldAllocated;
            int newLevel = costTable.maxLevelForValue((int) targetValue);
            data.setActualLevel(newLevel);
            newAllocated += costTable.getCost(newLevel);
        }
        allocatedSum = newAllocated;
    }

    /** 总池扩大：按期望消耗比例重新分配实际等级。 */
    private void increasePool(int newTotal) {
        long totalDesiredCost = 0L;
        for (MachineData d : machines.values()) {
        	MachineType costTable=d.getType();
            totalDesiredCost += costTable.getCost(d.getDesiredLevel());
        }

        if (totalDesiredCost == 0) {
            for (MachineData d : machines.values()) {
                d.setActualLevel(0);
            }
            allocatedSum = 0;
            return;
        }

        int newAllocated = 0;
        for (MachineData d : machines.values()) {

        	MachineType costTable=d.getType();
            int desiredCost = costTable.getCost(d.getDesiredLevel());
            // target = floor(newTotal * desiredCost / totalDesiredCost)
            long targetValue = (long) newTotal * desiredCost / totalDesiredCost;
            if (targetValue > desiredCost) targetValue = desiredCost; // 不超过期望
            int newLevel = costTable.maxLevelForValue((int) targetValue);
            d.setActualLevel(newLevel);
            newAllocated += costTable.getCost(newLevel);
        }
        allocatedSum = newAllocated;
    }

    // ============================================================
    // 期望等级变更
    // ============================================================

    /**
     * 设置某台机器的期望等级。
     *
     * @return true 表示操作被接受；false 表示数值不足，提升被拒绝。
     */
    public boolean setDesiredLevel(GlobalPos machineId, int newDesired) {
        MachineData data = machines.get(machineId);
        if (data == null) {
            throw new IllegalArgumentException("Unknown machine: " + machineId);
        }

    	MachineType costTable=data.getType();
        newDesired = Mth.clamp(newDesired, 0, costTable.maxLevel());
        int oldDesired = data.getDesiredLevel();
        if (newDesired == oldDesired) return true;

        if (newDesired < oldDesired) {
            lowerDesired(data, newDesired);
            return true;
        } else {
            return raiseDesired(data, newDesired);
        }
    }

    /** 降低期望：立即降级，释放的数值随机补给缺少数值的机器。 */
    private void lowerDesired(MachineData data, int newDesired) {
        int oldActual = data.getActualLevel();
        data.setDesiredLevel(newDesired);

    	MachineType costTable=data.getType();
        if (newDesired < oldActual) {
            int oldCost = costTable.getCost(oldActual);
            int newCost = costTable.getCost(newDesired);
            int freed = oldCost - newCost;

            data.setActualLevel(newDesired);
            allocatedSum -= freed;

            if (freed > 0) {
                distributeSurplus(freed);
            }
        }
        updateDeficientStatus(data);
    }

    /** 提升期望：数值不足则拒绝；足够则允许，并把该机器直接拉到新期望。 */
    private boolean raiseDesired(MachineData data, int newDesired) {

    	MachineType costTable=data.getType();
        int currentCost = costTable.getCost(data.getActualLevel());
        int newDesiredCost = costTable.getCost(newDesired);
        int requiredExtra = Math.max(0, newDesiredCost - currentCost);

        if (totalPool - allocatedSum < requiredExtra) {
            return false; // 数值不足，拒绝
        }

        data.setDesiredLevel(newDesired);
        if (data.getActualLevel() < newDesired) {
            data.setActualLevel(newDesired);
            allocatedSum += requiredExtra;
        }
        updateDeficientStatus(data);
        return true;
    }

    // ============================================================
    // 缺少数值分配
    // ============================================================

    /**
     * 把 surplus 数值随机分配给 deficientSet 中的机器，直到用完或全部满足。
     * 剩余未分配的数值保留在池中（不影响 allocatedSum）。
     */
    private void distributeSurplus(int surplus) {
        if (surplus <= 0 || deficientSet.isEmpty()) return;

        List<GlobalPos> candidates = new ArrayList<>(deficientSet);
        Collections.shuffle(candidates);

        int remaining = surplus;
        for (GlobalPos id : candidates) {
            if (remaining <= 0) break;

            MachineData data = machines.get(id);
            if (data == null || !data.isDeficient()) continue;

            remaining = upgradeTowardDesired(data, remaining);
            updateDeficientStatus(data);
        }
    }

    /**
     * 在预算内把机器从 actualLevel 逐级升向 desiredLevel。
     * 若某级消耗超过剩余预算则停止，不拆分等级。
     *
     * @return 剩余未使用的数值
     */
    private int upgradeTowardDesired(MachineData data, int budget) {
        int current = data.getActualLevel();
        int target  = data.getDesiredLevel();

    	MachineType costTable=data.getType();
        while (current < target) {
            int stepCost = costTable.getCost(current + 1) - costTable.getCost(current);
            if (budget < stepCost) break;
            budget -= stepCost;
            current++;
            allocatedSum += stepCost;
        }
        data.setActualLevel(current);
        return budget;
    }

    // ============================================================
    // 缺乏状态维护
    // ============================================================

    private void updateDeficientStatus(MachineData data) {
        if (data.isDeficient()) {
            deficientSet.add(data.getPos());
        } else {
            deficientSet.remove(data.getPos());
        }
    }

    private void rebuildDeficientSet() {
        deficientSet.clear();
        for (MachineData d : machines.values()) {
            if (d.isDeficient()) deficientSet.add(d.getPos());
        }
    }

    /**
     * 修改提供者的数值。新值可以与旧值相同（幂等，不触发重算）。
     *
     * @return true 表示值发生变化并已应用；false 表示新值与旧值相同
     */
    public boolean updateProvider(GlobalPos pos, int newValue) {
        if (newValue < 0) {
            throw new IllegalArgumentException("value must be >= 0, got " + newValue);
        }
        ProviderData p = providers.get(pos);
        int delta;
        if (p == null) {
        	delta=newValue;
        	p = new ProviderData(pos, newValue);
            providers.put(pos, p);
        }else {
        	delta = newValue - p.getValue();
	        if (delta == 0) return false;
	        p.setValueInternal(newValue);
        }
        providerSum += delta;
        applyProviderSumChange();
        return true;
    }

    /**
     * 删除一个提供者。
     *
     * @return true 表示存在并已删除；false 表示不存在
     */
    public boolean removeProvider(GlobalPos pos) {
        ProviderData p = providers.remove(pos);
        if (p == null) return false;

        providerSum -= p.getValue();
        applyProviderSumChange();
        return true;
    }

    public int getProviderSum() {
        return providerSum;
    }

    public ProviderData getProvider(GlobalPos pos) {
        return providers.get(pos);
    }

    public Collection<ProviderData> getProviders() {
        return Collections.unmodifiableCollection(providers.values());
    }

    /**
     * 批量替换整个提供者集合（用于读档 / 网络同步）。
     * 会按差量重算，并触发一次 reduce / increase。
     */
    public void replaceProviders(Collection<ProviderData> newProviders) {
        int newSum = extraProvider;
        providers.clear();
        if (newProviders != null) {
            for (ProviderData p : newProviders) {
                if (p == null) continue;
                newSum += p.getValue();
                providers.put(p.getPos(), p);
            }
        }
        providerSum = newSum;
        
        applyProviderSumChange();
    }
    public void setExtraProviderValue(int value) {
    	providerSum-=extraProvider;
    	extraProvider=value;
    	providerSum+=extraProvider;

        applyProviderSumChange();
    }
    /**
     * 内部：提供者总和变化后，重新驱动总池。
     * 若新总和 == 旧总和，不做任何事。
     */
    private void applyProviderSumChange() {
        if (providerSum == totalPool) return;

        int oldTotal = totalPool;
        totalPool = providerSum;

        if (providerSum < oldTotal) {
            reducePool(providerSum);
        } else {
            increasePool(providerSum);
        }
        rebuildDeficientSet();
    }
}