package com.teammoeg.frostedheart.content.steamenergy;

import net.minecraft.nbt.CompoundNBT;

/**
 * Class SteamNetworkConsumer.
 * <p>
 * Integrated power cache manager for power devices
 */
public class SteamNetworkConsumer extends SteamNetworkHolder {

    /**
     * The max power.<br>
     */
    public final float maxPower;

    /**
     * The max intake.<br>
     */
    public final float maxIntake;
    private float power;

    /**
     * Instantiates a new SteamNetworkConsumer.<br>
     *
     * @param maxPower  the max power to store<br>
     * @param maxIntake the max intake from network to cache<br>
     */
    public SteamNetworkConsumer(float maxPower, float maxIntake) {
        super();
        this.maxPower = maxPower;
        this.maxIntake = maxIntake;
    }

    /**
     * Save.
     *
     * @param nbt the nbt<br>
     */
    public void save(CompoundNBT nbt) {
        nbt.putFloat("power", power);
    }

    /**
     * Load.
     *
     * @param nbt the nbt<br>
     */
    public void load(CompoundNBT nbt) {
        power = nbt.getFloat("power");
    }

    /**
     * Tick and absorb power.
     *
     * @return atually drained
     */
    @Override
    public boolean tick() {
        if (isValid()) {
            super.tick();
            float actual = super.drainHeat(Math.min(24, maxPower - power));
            if (actual > 0) {
                power += actual;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean tryDrainHeat(float val) {
        if (power >= val) {
            power -= val;
            return true;
        }
        return false;
    }

    @Override
    public float drainHeat(float val) {
        float drained = Math.min(power, val);
        power -= drained;
        return drained;
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
     * set power stored.
     *
     * @param power value to set power to.
     */
    public void setPower(float power) {
        this.power = power;
    }

    /**
     * Get max power.
     *
     * @return max power<br>
     */
    public float getMaxPower() {
        return maxPower;
    }

    @Override
    public String toString() {
        return "SteamNetworkConsumer [maxPower=" + maxPower + ", maxIntake=" + maxIntake + ", power=" + power + ", sen="
                + sen + ", dist=" + dist + ", counter=" + counter + "]";
    }
}
