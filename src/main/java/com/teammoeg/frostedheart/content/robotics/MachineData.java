package com.teammoeg.frostedheart.content.robotics;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.chorda.util.struct.WeakReferenceSlot;

import lombok.Getter;
import net.minecraft.core.GlobalPos;
import net.minecraft.util.Mth;

/**
 * 每台机器的持久化数据。即使机器不在加载范围内，这份数据也会保留在管理器中。
 */
public class MachineData {
	public static final Codec<MachineData> CODEC = RecordCodecBuilder.create(t -> t.group(
		GlobalPos.CODEC.fieldOf("pos").forGetter(o->o.pos),
		MachineType.registry.typeCodec().fieldOf("type").forGetter(o->o.type),
		Codec.INT.fieldOf("desired").forGetter(o->o.desiredLevel),
		Codec.INT.fieldOf("actual").forGetter(o->o.actualLevel)
		).apply(t, MachineData::new));
    @Getter
    private final GlobalPos pos;
    @Getter
    private final MachineType type;

    @Getter
    private int desiredLevel;
    @Getter
    private int actualLevel;

    private transient WeakReferenceSlot<Machine> instance;
    
    public MachineData(GlobalPos pos, MachineType type, int desiredLevel, int actualLevel) {
		super();
		this.pos = pos;
		this.type = type;
		this.desiredLevel = desiredLevel;
		this.actualLevel = actualLevel;
	}

	public MachineData(MachineType type,GlobalPos pos) {
        this.type=type;
        this.pos = pos;
    }
    
    public MachineData(WeakReferenceSlot<Machine> machine,GlobalPos pos) {
        this.type=machine.getOrThrow().getType();
        this.pos = pos;
        this.instance=machine;
    }
    public boolean isLoaded()             { return instance!=null&&instance.isPresent(); }
    public boolean isDeficient()          { return actualLevel<desiredLevel; }
    public Machine getInstance()          { return instance==null?null:instance.orElse(null); }

    // INTERNAL METHODS DO NOT USE
    void setDesiredLevel(int v)    { this.desiredLevel = Mth.clamp(v, 0, type.maxLevel()); }
    void setActualLevel(int v)     { this.actualLevel = v; }
    void setInstance(WeakReferenceSlot<Machine> m)    { this.instance = m; }
    // INTERNAL METHODS END
    @Override
    public String toString() {
        return String.format(
            "MachineData[%s desired=%d actual=%d]",
            pos, desiredLevel, actualLevel);
    }

    public int getActualCost() {
    	return getType().getCost(getActualLevel());
    }

    public int getDesiredCost() {
    	return getType().getCost(getDesiredLevel());
    }
}