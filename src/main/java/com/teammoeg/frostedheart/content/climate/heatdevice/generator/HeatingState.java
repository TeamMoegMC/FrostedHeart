package com.teammoeg.frostedheart.content.climate.heatdevice.generator;

import com.teammoeg.frostedheart.base.multiblock.components.OwnerState;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;

public class HeatingState extends OwnerState {
    private boolean active;
    private float tempLevel;
    private float rangeLevel;
    private float lastTempMod;
    private float lastRadius;
    private boolean initialized;

    public HeatingState() {
        super();
    }

    @Override
    public void writeSaveNBT(CompoundTag nbt) {
        super.writeSaveNBT(nbt);
        //nbt.putBoolean("isWorking", isWorking);
        //nbt.putBoolean("isOverdrive", isOverdrive);
        nbt.putFloat("tempLevel", tempLevel);
        nbt.putFloat("rangeLevel", rangeLevel);
        nbt.putBoolean("active", active);
    }

    @Override
    public void readSaveNBT(CompoundTag nbt) {
        super.readSaveNBT(nbt);
        // isWorking = nbt.getBoolean("isWorking");
        //isOverdrive = nbt.getBoolean("isOverdrive");
        tempLevel = nbt.getFloat("tempLevel");
        rangeLevel = nbt.getFloat("rangeLevel");
        active = nbt.getBoolean("active");
    }

    /**
     * Determines whether the heat radius or temperature modification has changed.
     */
    public boolean shouldUpdateAdjust() {
        int radius = this.getRadius();
        int tempMod = this.getTempMod();

        boolean shouldUpdate = !initialized || lastRadius != radius || lastTempMod != tempMod;
        lastRadius = radius;
        lastTempMod = tempMod;
        initialized = true;
        return shouldUpdate;
    }

    public float getTempLevel() {
        return tempLevel;
    }

    public void setTempLevel(float tempLevel) {
        this.tempLevel = tempLevel;
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

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * Get the actual range of the heating device.
     * The range is calculated by the formula:
     * 12 + 4 * (rangeLevel - 1) if rangeLevel>1
     * <p>
     * The Base range at level 1 is 12 blocks.
     * For each additional level, the range increases by 4 blocks.
     *
     * @return in blocks
     */
    public int getRadius() {
        float rlevel = getRangeLevel();
        if (rlevel <= 1)
            return (int) (12 * rlevel);
        return (int) (12 + (rlevel - 1) * 4);
    }

    /**
     * Get the actual temperature modification of the heating device.
     * The temperature modification is calculated by the formula:
     * 10 * temperatureLevel
     * <p>
     * The Base temperature modification at level 1 is 10.
     * For each additional level, the temperature modification increases by 10.
     *
     * @return in degrees
     */
    public int getTempMod() {
        return (int) (getTempLevel() * 10);
    }


    /**
     * Get the vertical range towards the ground.
     * <p>
     * The lower bound is calculated by the formula:
     * ceil(rangeLevel)
     *
     * @return in blocks
     */
    public int getDownwardRange() {
        return Mth.ceil(getRangeLevel());
    }

    /**
     * Get the vertical range towards the sky.
     * <p>
     * The upper bound is calculated by the formula:
     * ceil(rangeLevel) * 4
     *
     * @return in blocks
     */
    public int getUpwardRange() {
        return Mth.ceil(getRangeLevel() * 4);
    }
}
