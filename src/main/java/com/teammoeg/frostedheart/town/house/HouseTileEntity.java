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

package com.teammoeg.frostedheart.town.house;

import com.teammoeg.frostedheart.FHTileTypes;
import com.teammoeg.frostedheart.town.ITownBlockTE;
import com.teammoeg.frostedheart.town.TownWorkerType;
import com.teammoeg.frostedheart.town.resident.Resident;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;

import java.util.List;

/**
 * A house in the town.
 */
public class HouseTileEntity extends TileEntity implements ITownBlockTE {

    public static final double COMFORTABLE_TEMP = 25;
    public static final int OPTIMAL_VOLUME = 100;
    public static final int BEST_DECORATION = 100;

    public int size; // how many resident can live here
    public List<Resident> residents;
    public double temperature;
    public int volume;
    public int decoration;

    public HouseTileEntity() {
        super(FHTileTypes.HOUSE.get());
    }

    /**
     * Get the rating based on house status
     * TODO: this is now a very crude numerical experiment.
     * @return a rating in range of zero to one
     */
    public double getRating() {
        double tempDiff = Math.abs(temperature - COMFORTABLE_TEMP);
        double tempRating = 1 - Math.min(1.0, tempDiff / COMFORTABLE_TEMP);
        double decoRating = (double) decoration / BEST_DECORATION;
        double volumeRating = (double) volume / OPTIMAL_VOLUME;
        return (decoRating + volumeRating + tempRating) / 3;
    }

    /**
     * Check room insulation
     * Check minimum volume
     * Check within generator range (or just check steam connection instead?)
     * @param housePos the core block
     * @return whether this room is well-defined
     */
    private boolean isRoomValid(BlockPos housePos) {
        // TODO
        return false;
    }

    /**
     * Check if work environment is valid
     */
    @Override
    public boolean isWorkValid() {
        BlockPos pos = this.getPos();
        boolean roomValid = isRoomValid(pos);
        boolean tempConstraint = temperature >= 0 && temperature <= 50;
        return roomValid && tempConstraint;
    }

    @Override
    public TownWorkerType getWorker() {
        return TownWorkerType.HOUSE;
    }

    @Override
    public CompoundNBT getWorkData() {
        CompoundNBT data = new CompoundNBT();
        // TODO: Serialize resident
        return data;
    }

    @Override
    public int getPriority() {
        return 0;
    }
}
