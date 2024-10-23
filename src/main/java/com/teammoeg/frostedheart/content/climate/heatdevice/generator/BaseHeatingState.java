package com.teammoeg.frostedheart.content.climate.heatdevice.generator;

import com.teammoeg.frostedheart.base.multiblock.components.OwnerState;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;

public class BaseHeatingState extends OwnerState {
    private float temperatureLevel;
    private float rangeLevel;
    private float lastTLevel;
    private float lastRLevel;
    private boolean initialized;

	public BaseHeatingState() {
		super();
	}

	@Override
	public void writeSaveNBT(CompoundTag nbt) {
		super.writeSaveNBT(nbt);
        //nbt.putBoolean("isWorking", isWorking);
        //nbt.putBoolean("isOverdrive", isOverdrive);
        nbt.putFloat("temperatureLevel", temperatureLevel);
        nbt.putFloat("rangeLevel", rangeLevel);
	}

	@Override
	public void readSaveNBT(CompoundTag nbt) {
		super.readSaveNBT(nbt);
       // isWorking = nbt.getBoolean("isWorking");
        //isOverdrive = nbt.getBoolean("isOverdrive");
        temperatureLevel = nbt.getFloat("temperatureLevel");
        rangeLevel = nbt.getFloat("rangeLevel");
	}
	public boolean shouldUpdate() {
		int rLevel=this.getActualRange();
		int tLevel=this.getActualTemp();

		boolean shouldUpdate= !initialized||lastRLevel!=rLevel||lastTLevel!=tLevel;
		lastRLevel=rLevel;
		lastTLevel=tLevel;
		initialized=true;
		return shouldUpdate;
	}
    public float getTemperatureLevel() {
		return temperatureLevel;
	}

	public void setTemperatureLevel(float temperatureLevel) {
		this.temperatureLevel = temperatureLevel;
	}

	public float getRangeLevel() {
		return rangeLevel;
	}

	public void setRangeLevel(float rangeLevel) {
		this.rangeLevel = rangeLevel;
	}


	public boolean isInitialized() {
		return initialized;
	}

	public void setInitialized(boolean initialized) {
		this.initialized = initialized;
	}

	/**
	 * Get the actual range of the heating device.
	 * The range is calculated by the formula:
	 * 12 + 4 * (rangeLevel - 1) if rangeLevel>1
	 *
	 * The Base range at level 1 is 12 blocks.
	 * For each additional level, the range increases by 4 blocks.
	 * @return in blocks
	 */
	public int getActualRange() {
    	float rlevel=getRangeLevel();
    	if(rlevel<=1)
    		return (int) (12*rlevel);
    	return (int) (12+(rlevel-1)*4);
    }

	/**
	 * Get the actual temperature modification of the heating device.
	 * The temperature modification is calculated by the formula:
	 * 10 * temperatureLevel
	 *
	 * The Base temperature modification at level 1 is 10.
	 * For each additional level, the temperature modification increases by 10.
	 * @return in degrees
	 */
    public int getActualTemp() {
        return (int) (getTemperatureLevel() * 10);
    }


	/**
	 * Get the vertical range towards the ground.
	 *
	 * The lower bound is calculated by the formula:
	 * ceil(rangeLevel)
	 *
	 * @return in blocks
	 */
    public int getLowerBound() {
        return Mth.ceil(getRangeLevel());
    }

	/**
	 * Get the vertical range towards the sky.
	 *
	 * The upper bound is calculated by the formula:
	 * ceil(rangeLevel) * 4
	 * 
	 * @return in blocks
	 */
    public int getUpperBound() {
        return Mth.ceil(getRangeLevel() * 4);
    }
}
