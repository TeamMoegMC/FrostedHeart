package com.teammoeg.frostedheart.content.robotics.labor;

import com.mojang.serialization.Codec;
import com.teammoeg.chorda.io.registry.TypeRegistry;

/**
 * 劳工类型（结构参考 {@link MachineType}）。
 *
 * <p>劳工点数按劳工类型分别计算：每种劳工类型有一个独立的池，机器可以同时使用多种劳工类型。
 * 劳工类型本身只表示「身份」，它的最大等级与每级所需数量由使用它的机器类型定义
 * （{@link MachineType#getCost(LaborType, int)}）。</p>
 *
 * <p>类型注册在 {@link #registry} 中，按注册 ID 序列化；同一个 ID 在存档与运行时对应同一个值，
 * 因此可以直接作为 Map 的键，也可以用 equals 比较。</p>
 */
public interface LaborType {

	/** 默认实现：只携带一个注册 ID。 */
	public static record BaseLaborType(String id) implements LaborType {

		@Override
		public String getId() {
			return id;
		}

		@Override
		public String toString() {
			return id;
		}
	}

	public static TypeRegistry<LaborType> registry = new TypeRegistry<>();

	/** 该劳工类型的注册 ID。 */
	String getId();

	/** 劳工类型的编解码器：按注册名读写。 */
	public static Codec<LaborType> codec() {
		return registry.typeCodec();
	}

	public static <A extends LaborType> A register(A cls, String type) {
		registry.register(cls, type);
		return cls;
	}

	/** 用默认实现 {@link BaseLaborType} 注册一个劳工类型，注册 ID 同时作为它的 id。 */
	public static LaborType register(String type) {
		return register(new BaseLaborType(type), type);
	}
}
