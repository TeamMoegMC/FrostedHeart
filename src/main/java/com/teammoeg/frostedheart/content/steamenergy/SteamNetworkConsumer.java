/*
 * Copyright (c) 2024 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

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

    @Override
    public float drainHeat(float val) {
        float drained = Math.min(power, val);
        power -= drained;
        return drained;
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
    public void load(CompoundNBT nbt) {
        power = nbt.getFloat("power");
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
     * set power stored.
     *
     * @param power value to set power to.
     */
    public void setPower(float power) {
        this.power = power;
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
    public String toString() {
        return "SteamNetworkConsumer [maxPower=" + maxPower + ", maxIntake=" + maxIntake + ", power=" + power + ", sen="
                + sen + ", dist=" + dist + ", counter=" + counter + "]";
    }

    @Override
    public boolean tryDrainHeat(float val) {
        if (power >= val) {
            power -= val;
            return true;
        }
        return false;
    }
}
