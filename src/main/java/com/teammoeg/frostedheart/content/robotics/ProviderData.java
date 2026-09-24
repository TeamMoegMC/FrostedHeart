package com.teammoeg.frostedheart.content.robotics;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import lombok.Getter;
import net.minecraft.core.GlobalPos;

public class ProviderData {
	public static final Codec<ProviderData> CODEC = RecordCodecBuilder.create(t -> t.group(
		GlobalPos.CODEC.fieldOf("pos").forGetter(o->o.pos),
		Codec.INT.fieldOf("value").forGetter(o->o.value)
		).apply(t, ProviderData::new));
    @Getter
    private final GlobalPos pos;

    /** 当前提供的点数，>= 0 */
    @Getter
    private int value;

    public ProviderData(GlobalPos pos, int value) {
        if (pos == null) {
            throw new IllegalArgumentException("providerId must not be null");
        }
        if (value < 0) {
            throw new IllegalArgumentException("value must be >= 0, got " + value);
        }
        this.pos = pos;
        this.value = value;
    }

    /** 内部使用，外部请通过管理器的 updateProvider 修改。 */
    void setValueInternal(int v) { this.value = v; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProviderData)) return false;
        return pos.equals(((ProviderData) o).pos);
    }

    @Override
    public int hashCode() { return pos.hashCode(); }

    @Override
    public String toString() {
        return "PointProvider[" + pos + "=" + value + "]";
    }
}