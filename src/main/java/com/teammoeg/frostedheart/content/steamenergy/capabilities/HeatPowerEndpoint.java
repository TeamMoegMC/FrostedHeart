package com.teammoeg.frostedheart.content.steamenergy.capabilities;

import net.minecraft.nbt.CompoundNBT;

public abstract class HeatPowerEndpoint extends HeatEndpoint {
	/**
     * The max power.<br>
     */
    public final float maxPower;
    protected float power;
	public HeatPowerEndpoint(float maxPower) {
		super();
		this.maxPower = maxPower;
	}

    /**
     * Get max power.
     *
     * @return max power<br>
     */
    public float getMaxPower() {
        return maxPower;
    }

    /**
     * Get power stored.
     *
     * @return power<br>
     */
    public float getPower() {
        return power;
    }
    /**
     * Load.
     *
     * @param nbt the nbt<br>
     */
    public void load(CompoundNBT nbt,boolean isPacket) {
        power = nbt.getFloat("net_power");
    }

    /**
     * Save.
     *
     * @param nbt the nbt<br>
     */
    public void save(CompoundNBT nbt,boolean isPacket) {
        nbt.putFloat("net_power", power);
    }

    /**
     * set power stored.
     *
     * @param power value to set power to.
     */
    public void setPower(float power) {
        this.power = power;
    }
    
    @Override
    public boolean canSendHeat() {
    	return power<maxPower;
    }
    @Override
    public boolean canProvideHeat() {
    	return power>0;
    }

}
