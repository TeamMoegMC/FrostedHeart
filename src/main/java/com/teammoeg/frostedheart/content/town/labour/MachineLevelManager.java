package com.teammoeg.frostedheart.content.town.labour;
import java.util.*;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.GlobalPos;
import net.minecraft.util.Mth;

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
public class MachineLevelManager {


	public static final Codec<MachineLevelManager> CODEC = RecordCodecBuilder.create(t -> t.group(
		Codec.list(MachineData.CODEC).fieldOf("machines").forGetter(o->new ArrayList<>(o.machines.values())),
		Codec.INT.fieldOf("total").forGetter(o->o.totalPool),
		Codec.INT.fieldOf("allocated").forGetter(o->o.allocatedSum)
		).apply(t, MachineLevelManager::new));

    /** 总数值池上限 */
    private int totalPool;

    /** 所有机器实际等级消耗之和（缓存） */
    private int allocatedSum;

    /** 所有机器数据（包含未加载机器） */
    private final Map<GlobalPos, MachineData> machines = new HashMap<>();

    /** 缺少数值的机器 ID 集合（actual < desired） */
    private final Set<GlobalPos> deficientSet = new HashSet<>();
	public MachineLevelManager() {
		super();
	}
    public MachineLevelManager(List<MachineData> machines,int totalPool, int allocatedSum) {
		super();
		for(MachineData machine:machines) {
			this.machines.put(machine.getPos(), machine);
		}
		this.totalPool = totalPool;
		this.allocatedSum = allocatedSum;
		rebuildDeficientSet();
	}

    // ============================================================
    // 查询
    // ============================================================

    public int getTotalPool()      { return totalPool; }
    public int getAllocatedSum()   { return allocatedSum; }
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
    public MachineData registerMachine(GlobalPos machineId,Machine machine) {
    	MachineData machineData=getMachine(machineId);
    	if(machine==null) {
    		machines.put(machineId,machineData= new MachineData(machine,machineId));
    	}else if(!Objects.equals(machineData.getType(), machine.getType())) {
    		releaseMachine(machineId);

    		machines.put(machineId,machineData= new MachineData(machine,machineId));
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
        int releasedCost = data.getType().getCost().cost(data.getActualLevel());
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
    public void onMachineLoaded(Machine machine) {
        MachineData data = registerMachine(machine.getMachineLocation(),machine);
        data.setInstance(machine);
        machine.applyLevel(data.getActualLevel());

        if (data.isDeficient()) {
            deficientSet.add(data.getPos());
        }
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
        	LevelCostTable costTable=data.getInstance().getType().getCost();
            int currentCost = costTable.cost(data.getActualLevel());
            // target = floor(currentCost * newTotal / oldAllocated)
            long targetValue = currentCost * (long) newTotal / oldAllocated;
            int newLevel = costTable.maxLevelForValue((int) targetValue);
            data.setActualLevel(newLevel);
            newAllocated += costTable.cost(newLevel);
        }
        allocatedSum = newAllocated;
    }

    /** 总池扩大：按期望消耗比例重新分配实际等级。 */
    private void increasePool(int newTotal) {
        long totalDesiredCost = 0L;
        for (MachineData d : machines.values()) {
        	LevelCostTable costTable=d.getInstance().getType().getCost();
            totalDesiredCost += costTable.cost(d.getDesiredLevel());
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

        	LevelCostTable costTable=d.getInstance().getType().getCost();
            int desiredCost = costTable.cost(d.getDesiredLevel());
            // target = floor(newTotal * desiredCost / totalDesiredCost)
            long targetValue = (long) newTotal * desiredCost / totalDesiredCost;
            if (targetValue > desiredCost) targetValue = desiredCost; // 不超过期望
            int newLevel = costTable.maxLevelForValue((int) targetValue);
            d.setActualLevel(newLevel);
            newAllocated += costTable.cost(newLevel);
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

    	LevelCostTable costTable=data.getInstance().getType().getCost();
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

    	LevelCostTable costTable=data.getInstance().getType().getCost();
        if (newDesired < oldActual) {
            int oldCost = costTable.cost(oldActual);
            int newCost = costTable.cost(newDesired);
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

    	LevelCostTable costTable=data.getInstance().getType().getCost();
        int currentCost = costTable.cost(data.getActualLevel());
        int newDesiredCost = costTable.cost(newDesired);
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

    	LevelCostTable costTable=data.getInstance().getType().getCost();
        while (current < target) {
            int stepCost = costTable.cost(current + 1) - costTable.cost(current);
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

}