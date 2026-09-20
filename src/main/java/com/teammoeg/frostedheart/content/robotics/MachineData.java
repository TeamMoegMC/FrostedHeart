package com.teammoeg.frostedheart.content.robotics;

import java.lang.ref.WeakReference;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.chorda.util.struct.WeakReferenceSlot;

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
    private final GlobalPos pos;
    private final MachineType type;
    
    private int desiredLevel;
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
    public MachineType getType() {
		return type;
	}

	public GlobalPos getPos()          { return pos; }
    public int  getDesiredLevel()         { return desiredLevel; }
    public void setDesiredLevel(int v)    { this.desiredLevel = Mth.clamp(v, 0, type.maxLevel()); }
    public int  getActualLevel()          { return actualLevel; }
    public void setActualLevel(int v)     { this.actualLevel = v; }
    public boolean isLoaded()             { return instance!=null&&instance.isPresent(); }
    public boolean isDeficient()          { return actualLevel<desiredLevel; }
    public Machine getInstance()          { return instance==null?null:instance.orElse(null); }
    public void setInstance(WeakReferenceSlot<Machine> m)    { this.instance = m; }

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