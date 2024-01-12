/*
 * Copyright (c) 2022-2024 TeamMoeg
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

package com.teammoeg.frostedheart.climate;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.world.server.ServerWorld;

/**
 * An anti command and sleep clock source
 */
public class WorldClockSource {
    long secs;
    long lastdaytime;

    public WorldClockSource() {
    }

    public void deserialize(CompoundNBT cnbt) {
        secs = cnbt.getLong("secs");
        lastdaytime = cnbt.getLong("last");
    }

    public long getDate() {
        return (secs / 50) / 24;
    }

    public int getHourInDay() {
        return (int) ((secs / 50) % 24);
    }

    public long getHours() {
        return (secs / 50);
    }

    public long getMonth() {
        return (secs / 50) / 24 / 30;
    }

    public long getTimeSecs() {
        return secs;
    }

    public CompoundNBT serialize() {
        return serialize(new CompoundNBT());
    }

    public CompoundNBT serialize(CompoundNBT cnbt) {
        cnbt.putLong("secs", secs);
        cnbt.putLong("last", lastdaytime);
        return cnbt;
    }

    public void setDate(long date) {
        secs = (secs % 1200) + date * 1200;
    }

    @Override
    public String toString() {
        return "WorldClockSource [secs=" + secs + "]";
    }

    public void update(long newTime) {
        long dt = newTime - lastdaytime;
        if (dt < 0) {// if time run backwards, it's command done the trick
            long nextday = lastdaytime + 24000L;
            nextday = nextday - nextday % 24000L;
            dt = newTime % 24000L + nextday - lastdaytime;//assumpt it's next day and continue
        }
        secs += dt / 20;
        lastdaytime = newTime - newTime % 20;
    }

    public void update(ServerWorld w) {
        update(w.getDayTime());
    }
}
