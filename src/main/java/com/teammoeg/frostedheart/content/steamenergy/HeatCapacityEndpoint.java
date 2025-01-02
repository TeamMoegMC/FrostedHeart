package com.teammoeg.frostedheart.content.steamenergy;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.minecraft.nbt.CompoundTag;

/**
 * Defines a HeatEndpoint with a capacity to store, provide, and receive heat.
 */
@Getter
@ToString(callSuper = true)
public abstract class HeatCapacityEndpoint extends HeatEndpoint {
    /** The max capacity to store heat. */
    protected final float capacity;
    /**
     * Detach priority.
     * Consumer priority, if power is low, endpoint with lower priority would detach first
     */
    protected final int priority;
    /** Current heat stored. */
    @Setter
    protected float heat;

	public HeatCapacityEndpoint(int priority, float capacity) {
		this.capacity = capacity;
		this.priority = priority;
	}

    public void load(CompoundTag nbt,boolean isPacket) {
        heat = nbt.getFloat("net_power");
    }

    public void save(CompoundTag nbt,boolean isPacket) {
        nbt.putFloat("net_power", heat);
    }

    @Override
    public boolean canReceiveHeat() {
    	return heat < capacity;
    }
    
    @Override
    public boolean canProvideHeat() {
    	return heat > 0;
    }

}
