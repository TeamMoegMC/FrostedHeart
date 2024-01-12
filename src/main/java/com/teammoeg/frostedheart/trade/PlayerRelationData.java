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

package com.teammoeg.frostedheart.trade;

import net.minecraft.nbt.CompoundNBT;

public class PlayerRelationData {
    public static final PlayerRelationData EMPTY = new PlayerRelationData();
    public int totalbenefit;
    int sawmurder;

    long lastUpdated;

    public void deserialize(CompoundNBT data) {

        sawmurder = data.getInt("murder");
        totalbenefit = data.getInt("benefit");

        lastUpdated = data.getLong("last");
    }

    public CompoundNBT serialize(CompoundNBT data) {

        data.putInt("murder", sawmurder);
        data.putInt("benefit", totalbenefit);

        data.putLong("last", lastUpdated);
        return data;
    }

    public void update(long day) {
        long delta = day - lastUpdated;
        if (delta > 0) {
            totalbenefit = (int) (totalbenefit * Math.pow(0.75f, delta));
        }
        lastUpdated = day;
    }
}
