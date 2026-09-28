package com.teammoeg.frostedheart.content.robotics.labor;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.teammoeg.chorda.io.registry.TypeRegistry;

/**
 * 机器类型。
 *
 * <p>一个机器类型声明该机器使用哪些 {@link LaborType 劳工类型}，并为每种劳工类型指定两个数值：
 * <b>最大等级</b>与<b>每级所需劳工数量</b>。等级 {@code level} 的累计消耗为
 * {@code min(level, maxLevel) * costPerLevel}。同一台机器可以同时使用多种劳工，
 * 每种劳工的等级互相独立。</p>
 */
public interface MachineType {

	/**
	 * 一种劳工类型的等级配置：最大等级 + 每级所需点数。
	 */
	public static record LaborCost(int maxLevel, int costPerLevel) {

		public LaborCost {
			if (maxLevel < 0) {
				throw new IllegalArgumentException("maxLevel must be >= 0, got " + maxLevel);
			}
			if (costPerLevel <= 0) {
				throw new IllegalArgumentException("costPerLevel must be > 0, got " + costPerLevel);
			}
		}

		/** 等级 level 的累计消耗：低于 0 按 0 级算，高于 maxLevel 按 maxLevel 算。 */
		public int cost(int level) {
			int clamped = level < 0 ? 0 : Math.min(level, maxLevel);
			return clamped * costPerLevel;
		}

		/** 消耗不超过 value 的最大合法等级。 */
		public int maxLevelForValue(int value) {
			if (value <= 0) return 0;
			return Math.min(maxLevel, value / costPerLevel);
		}
	}

	/**
	 * 默认实现：持有若干劳工类型各自的 {@link LaborCost}，构造后不可变。
	 *
	 * <p>表的迭代顺序即 {@link #getLaborTypes()} 的顺序，第一个劳工类型是这台机器的主劳工类型。
	 * 直接传 Map 时必须是有序的（例如 {@link LinkedHashMap}），用无序 Map 会让主类型不确定；
	 * {@link #builder()} 按添加顺序记录，不受此影响。</p>
	 */
	public static record BaseMachineType(Map<LaborType, LaborCost> costs) implements MachineType {

		public BaseMachineType {
			if (costs == null || costs.isEmpty()) {
				throw new IllegalArgumentException("costs must not be empty");
			}
			costs = Collections.unmodifiableMap(new LinkedHashMap<>(costs));
		}

		/** 一种劳工类型：单独指定最大等级与每级所需数量。 */
		public BaseMachineType(LaborType laborType, int maxLevel, int costPerLevel) {
			this(Map.of(laborType, new LaborCost(maxLevel, costPerLevel)));
		}

		/** 多种劳工类型共用同一组「最大等级 + 每级所需数量」。 */
		public BaseMachineType(Collection<LaborType> laborTypes, int maxLevel, int costPerLevel) {
			this(toCosts(laborTypes, new LaborCost(maxLevel, costPerLevel)));
		}

		/**
		 * 流式构建器。适合要逐个劳工类型配置、或要让部分类型共用同一组数值的场景。
		 *
		 * <pre>{@code
		 * BaseMachineType type = BaseMachineType.builder()
		 *     .labor(LaborTypes.GENERAL, 10, 5)                  // 最大 10 级，每级 5 点
		 *     .labor(List.of(SKILLED, ELECTRICITY), 5, 8)        // 两种类型共用：最大 5 级，每级 8 点
		 *     .build();
		 * }</pre>
		 *
		 * <p>{@link Builder#build()} 之后的机器类型仍然不可变：继续修改 builder 不会影响已构建的对象。</p>
		 */
		public static Builder builder() {
			return new Builder();
		}

		/** {@link BaseMachineType#builder()} 的构建器。 */
		public static final class Builder {

			private final Map<LaborType, LaborCost> costs = new LinkedHashMap<>();

			/** 为一种劳工类型设置最大等级与每级所需数量。 */
			public Builder labor(LaborType laborType, int maxLevel, int costPerLevel) {
				return labor(laborType, new LaborCost(maxLevel, costPerLevel));
			}

			/** 为多种劳工类型设置同一组最大等级与每级所需数量。 */
			public Builder labor(Collection<LaborType> laborTypes, int maxLevel, int costPerLevel) {
				return labor(laborTypes, new LaborCost(maxLevel, costPerLevel));
			}

			/** 为一种劳工类型设置等级配置。 */
			public Builder labor(LaborType laborType, LaborCost cost) {
				if (laborType == null) {
					throw new IllegalArgumentException("laborType must not be null");
				}
				if (cost == null) {
					throw new IllegalArgumentException("cost must not be null");
				}
				costs.put(laborType, cost);
				return this;
			}

			/** 为多种劳工类型设置同一份等级配置。 */
			public Builder labor(Collection<LaborType> laborTypes, LaborCost cost) {
				if (laborTypes == null || laborTypes.isEmpty()) {
					throw new IllegalArgumentException("laborTypes must not be empty");
				}
				for (LaborType laborType : laborTypes) {
					labor(laborType, cost);
				}
				return this;
			}

			/** 构建不可变的机器类型；至少要配置一种劳工类型。 */
			public BaseMachineType build() {
				return new BaseMachineType(costs);
			}
		}

		private static Map<LaborType, LaborCost> toCosts(Collection<LaborType> laborTypes, LaborCost cost) {
			if (laborTypes == null || laborTypes.isEmpty()) {
				throw new IllegalArgumentException("laborTypes must not be empty");
			}
			Map<LaborType, LaborCost> costs = new LinkedHashMap<>();
			for (LaborType laborType : laborTypes) {
				costs.put(laborType, cost);
			}
			return costs;
		}

		@Override
		public int getCost(LaborType laborType, int level) {
			LaborCost cost = costs.get(laborType);
			return cost == null ? 0 : cost.cost(level);
		}

		@Override
		public int maxLevelForValue(LaborType laborType, int value) {
			LaborCost cost = costs.get(laborType);
			return cost == null ? 0 : cost.maxLevelForValue(value);
		}

		@Override
		public int maxLevel(LaborType laborType) {
			LaborCost cost = costs.get(laborType);
			return cost == null ? 0 : cost.maxLevel();
		}

		@Override
		public List<LaborType> getLaborTypes() {
			return List.copyOf(costs.keySet());
		}
	}

	public static TypeRegistry<MachineType> registry = new TypeRegistry<>();

	/** 该机器使用的所有劳工类型，声明顺序即主类型优先。 */
	List<LaborType> getLaborTypes();

	/** 该机器在给定劳工类型下的等级消耗；机器不使用该劳工类型时返回 0。 */
	int getCost(LaborType laborType, int level);

	/** 该机器在给定劳工类型下，消耗不超过 value 的最大合法等级。 */
	int maxLevelForValue(LaborType laborType, int value);

	/** 该机器在给定劳工类型下的最高等级。 */
	int maxLevel(LaborType laborType);

	/** 主劳工类型：声明顺序中的第一个。无劳工类型参数的快捷方法都作用于它。 */
	default LaborType getPrimaryLaborType() {
		return getLaborTypes().get(0);
	}

	public static <A extends MachineType> A register(A cls, String type) {
		registry.register(cls, type);
		return cls;
	}
}
