package com.teammoeg.frostedheart.content.steamenergy.capabilities;

import net.minecraft.nbt.CompoundNBT;

// TODO: Auto-generated Javadoc
/**
 * The Class HeatPowerEndpoint.
 * Basic class for heat power endpoint with power action
 */
public abstract class HeatPowerEndpoint extends HeatEndpoint {
    public final float maxPower;
    
    protected float power;
	
	public HeatPowerEndpoint(float maxPower) {
		super();
		this.maxPower = maxPower;
	}

    public float getMaxPower() {
        return maxPower;
    }

    public float getPower() {
        return power;
    }
    
    public void load(CompoundNBT nbt,boolean isPacket) {
        power = nbt.getFloat("net_power");
    }

    public void save(CompoundNBT nbt,boolean isPacket) {
        nbt.putFloat("net_power", power);
    }

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
