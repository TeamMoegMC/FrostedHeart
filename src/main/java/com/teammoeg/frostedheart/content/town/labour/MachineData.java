package com.teammoeg.frostedheart.content.town.labour;

import java.lang.ref.WeakReference;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.GlobalPos;

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

    private transient WeakReference<Machine> instance;
    
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
    
    public MachineData(Machine machine,GlobalPos pos) {
        this.type=machine.getType();
        this.pos = pos;
        this.instance=new WeakReference<>(machine);
    }
    public MachineType getType() {
		return type;
	}

	public GlobalPos getPos()          { return pos; }
    public int  getDesiredLevel()         { return desiredLevel; }
    public void setDesiredLevel(int v)    { this.desiredLevel = v; }
    public int  getActualLevel()          { return actualLevel; }
    public void setActualLevel(int v)     { this.actualLevel = v; }
    public boolean isLoaded()             { return instance!=null&&instance.get()!=null&&instance.get().isLoaded(); }
    public boolean isDeficient()          { return actualLevel<desiredLevel; }
    public Machine getInstance()          { return instance.get(); }
    public void setInstance(Machine m)    { this.instance = new WeakReference<>(m); }

    @Override
    public String toString() {
        return String.format(
            "MachineData[%s desired=%d actual=%d]",
            pos, desiredLevel, actualLevel);
    }
}