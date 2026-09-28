package com.teammoeg.frostedheart.content.robotics.labor;

import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntMaps;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import lombok.Getter;
import net.minecraft.core.GlobalPos;

/**
 * 劳工提供者数据：一个提供者（例如发电机、劳工宿舍）可以同时提供多种劳工类型的点数。
 *
 * <p>劳工点数按类型分别计算，每种类型各自汇总成一个独立的池。</p>
 *
 * <p>点数表用 fastutil 的 {@link Reference2IntOpenHashMap}，按引用（{@code ==}）匹配劳工类型键，
 * 未命中的类型按 0 处理；因此键必须是 {@link LaborType} 注册表里的类型对象。</p>
 */
public class ProviderData {
	public static final Codec<ProviderData> CODEC = RecordCodecBuilder.create(t -> t.group(
		GlobalPos.CODEC.fieldOf("pos").forGetter(o -> o.pos),
		Codec.unboundedMap(LaborType.codec(), Codec.INT).fieldOf("values").forGetter(o -> o.values)
		).apply(t, ProviderData::new));

	@Getter
	private final GlobalPos pos;

	/** 劳工类型 → 当前提供的点数，每个值都 > 0；0 值不会保留，未命中的类型按 0 处理。 */
	private final Reference2IntOpenHashMap<LaborType> values = new Reference2IntOpenHashMap<>();

	public ProviderData(GlobalPos pos, Map<LaborType, Integer> values) {
		if (pos == null) {
			throw new IllegalArgumentException("provider pos must not be null");
		}
		this.pos = pos;
		if (values != null) {
			for (Map.Entry<LaborType, Integer> e : values.entrySet()) {
				if (e.getKey() == null || e.getValue() == null) continue;
				setValueInternal(e.getKey(), e.getValue());
			}
		}
	}

	public ProviderData(GlobalPos pos, LaborType laborType, int value) {
		this(pos, Map.of(laborType, value));
	}

	/** 给定劳工类型提供的点数；未提供该类型时为 0。 */
	public int getValue(LaborType laborType) {
		return values.getInt(laborType);
	}

	/** 只读的劳工类型 → 点数视图。 */
	public Reference2IntMap<LaborType> getValues() {
		return Reference2IntMaps.unmodifiable(values);
	}

	/** 内部使用，外部请通过管理器的 updateProvider 修改。 */
	void setValueInternal(LaborType laborType, int v) {
		if (v < 0) {
			throw new IllegalArgumentException("value must be >= 0, got " + v);
		}
		if (v == 0) {
			values.removeInt(laborType);
		} else {
			values.put(laborType, v);
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof ProviderData)) return false;
		return pos.equals(((ProviderData) o).pos);
	}

	@Override
	public int hashCode() {
		return pos.hashCode();
	}

	@Override
	public String toString() {
		return "LaborProvider[" + pos + "=" + values + "]";
	}
}
