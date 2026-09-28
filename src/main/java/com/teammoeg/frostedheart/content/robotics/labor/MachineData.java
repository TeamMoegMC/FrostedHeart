package com.teammoeg.frostedheart.content.robotics.labor;

import java.util.List;
import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.chorda.util.struct.WeakReferenceSlot;

import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntMaps;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import lombok.Getter;
import net.minecraft.core.GlobalPos;
import net.minecraft.util.Mth;

/**
 * 每台机器的持久化数据。即使机器不在加载范围内，这份数据也会保留在管理器中。
 *
 * <p>一台机器可以同时使用多种劳工类型，每种劳工类型各有一份期望等级与实际等级。
 * {@link #getTotalActualLevel()} 是各劳工类型实际等级之和，随等级变更增量维护，
 * 查询为 O(1)。</p>
 *
 * <p>等级表用 fastutil 的 {@link Reference2IntOpenHashMap}，按引用（{@code ==}）匹配劳工类型键，
 * 未命中的等级按 0 处理；因此键必须是 {@link LaborType} 注册表里的类型对象。</p>
 */
public class MachineData {
	public static final Codec<MachineData> CODEC = RecordCodecBuilder.create(t -> t.group(
		GlobalPos.CODEC.fieldOf("pos").forGetter(o -> o.pos),
		MachineType.registry.typeCodec().fieldOf("type").forGetter(o -> o.type),
		Codec.unboundedMap(LaborType.codec(), Codec.INT).fieldOf("desired").forGetter(o -> o.desiredLevels),
		Codec.unboundedMap(LaborType.codec(), Codec.INT).fieldOf("actual").forGetter(o -> o.actualLevels)
		).apply(t, MachineData::new));

	@Getter
	private final GlobalPos pos;
	@Getter
	private final MachineType type;

	/** 该机器使用的劳工类型，来自 {@link MachineType#getLaborTypes()}。 */
	private final List<LaborType> laborTypes;

	/** 劳工类型 → 期望等级，未命中的类型按 0 处理。 */
	private final Reference2IntOpenHashMap<LaborType> desiredLevels = new Reference2IntOpenHashMap<>();
	/** 劳工类型 → 实际等级，未命中的类型按 0 处理。 */
	private final Reference2IntOpenHashMap<LaborType> actualLevels = new Reference2IntOpenHashMap<>();

	/** 各劳工类型实际等级之和（缓存）。 */
	private int totalActualLevel;

	private transient WeakReferenceSlot<Machine> instance;

	public MachineData(GlobalPos pos, MachineType type, Map<LaborType, Integer> desiredLevels, Map<LaborType, Integer> actualLevels) {
		if (pos == null) {
			throw new IllegalArgumentException("machine pos must not be null");
		}
		if (type == null) {
			throw new IllegalArgumentException("machine type must not be null");
		}
		this.pos = pos;
		this.type = type;
		this.laborTypes = List.copyOf(type.getLaborTypes());
		// 只保留该机器类型声明的劳工类型，并把等级钳制到合法范围
		for (LaborType laborType : laborTypes) {
			this.desiredLevels.put(laborType, clampLevel(laborType, desiredLevels == null ? 0 : desiredLevels.getOrDefault(laborType, 0)));
			this.actualLevels.put(laborType, clampLevel(laborType, actualLevels == null ? 0 : actualLevels.getOrDefault(laborType, 0)));
		}
		recomputeTotalActualLevel();
	}

	public MachineData(MachineType type, GlobalPos pos) {
		this(pos, type, Map.of(), Map.of());
	}

	public MachineData(WeakReferenceSlot<Machine> machine, GlobalPos pos) {
		this(pos, machine.getOrThrow().getType(), Map.of(), Map.of());
		this.instance = machine;
	}

	/** 该机器使用的劳工类型。 */
	public List<LaborType> getLaborTypes() {
		return laborTypes;
	}

	/** 该机器是否使用给定劳工类型。 */
	public boolean usesLabor(LaborType laborType) {
		return laborTypes.contains(laborType);
	}

	/**
	 * 各劳工类型实际等级之和。等级变更时增量维护，可直接用于每 tick 的等级查询。
	 */
	public int getTotalActualLevel() {
		return totalActualLevel;
	}

	/** 各劳工类型期望等级之和。 */
	public int getTotalDesiredLevel() {
		int sum = 0;
		for (LaborType laborType : laborTypes) {
			sum += getDesiredLevel(laborType);
		}
		return sum;
	}

	/** 无类型查询返回合计值：各劳工类型实际等级之和。 */
	public int getActualLevel() {
		return totalActualLevel;
	}

	/** 无类型查询返回合计值：各劳工类型期望等级之和。 */
	public int getDesiredLevel() {
		return getTotalDesiredLevel();
	}

	public int getActualLevel(LaborType laborType) {
		return actualLevels.getInt(laborType);
	}

	public int getDesiredLevel(LaborType laborType) {
		return desiredLevels.getInt(laborType);
	}

	/** 给定劳工类型的实际点数消耗。 */
	public int getActualCost(LaborType laborType) {
		return type.getCost(laborType, getActualLevel(laborType));
	}

	/** 给定劳工类型的期望点数消耗。 */
	public int getDesiredCost(LaborType laborType) {
		return type.getCost(laborType, getDesiredLevel(laborType));
	}

	/** 无类型查询返回合计值：各劳工类型实际点数之和。 */
	public int getActualCost() {
		int sum = 0;
		for (LaborType laborType : laborTypes) {
			sum += getActualCost(laborType);
		}
		return sum;
	}

	/** 无类型查询返回合计值：各劳工类型期望点数之和。 */
	public int getDesiredCost() {
		int sum = 0;
		for (LaborType laborType : laborTypes) {
			sum += getDesiredCost(laborType);
		}
		return sum;
	}

	/** 给定劳工类型是否缺乏（actual < desired）。 */
	public boolean isDeficient(LaborType laborType) {
		return getActualLevel(laborType) < getDesiredLevel(laborType);
	}

	/** 任一劳工类型缺乏即为缺乏。 */
	public boolean isDeficient() {
		for (LaborType laborType : laborTypes) {
			if (isDeficient(laborType)) return true;
		}
		return false;
	}

	public boolean isLoaded() {
		return instance != null && instance.isPresent();
	}

	public Machine getInstance() {
		return instance == null ? null : instance.orElse(null);
	}

	/** 只读的劳工类型 → 期望等级视图。 */
	public Reference2IntMap<LaborType> getDesiredLevels() {
		return Reference2IntMaps.unmodifiable(desiredLevels);
	}

	/** 只读的劳工类型 → 实际等级视图。 */
	public Reference2IntMap<LaborType> getActualLevels() {
		return Reference2IntMaps.unmodifiable(actualLevels);
	}

	// INTERNAL METHODS DO NOT USE
	void setDesiredLevel(LaborType laborType, int v) {
		if (!usesLabor(laborType)) return;
		desiredLevels.put(laborType, clampLevel(laborType, v));
	}

	void setActualLevel(LaborType laborType, int v) {
		if (!usesLabor(laborType)) return;
		int clamped = clampLevel(laborType, v);
		int old = actualLevels.put(laborType, clamped);
		totalActualLevel += clamped - old;
	}

	void setInstance(WeakReferenceSlot<Machine> m) {
		this.instance = m;
	}

	/** 按当前等级表重算合计实际等级。 */
	void recomputeTotalActualLevel() {
		int sum = 0;
		for (LaborType laborType : laborTypes) {
			sum += actualLevels.getInt(laborType);
		}
		totalActualLevel = sum;
	}
	// INTERNAL METHODS END

	private int clampLevel(LaborType laborType, int level) {
		return Mth.clamp(level, 0, type.maxLevel(laborType));
	}

	@Override
	public String toString() {
		return String.format("MachineData[%s desired=%s actual=%s total=%d]", pos, desiredLevels, actualLevels, totalActualLevel);
	}
}
