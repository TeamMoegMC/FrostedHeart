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

	public int getActualRange() {
    	float rlevel=getRangeLevel();
    	if(rlevel<=1)
    		return (int) (12*rlevel);
    	return (int) (12+(rlevel-1)*4);
    }

    public int getActualTemp() {
        return (int) (getTemperatureLevel() * 10);
    }



    public int getLowerBound() {
        return Mth.ceil(getRangeLevel());
    }

    public int getUpperBound() {
        return Mth.ceil(getRangeLevel() * 4);
    }
}
