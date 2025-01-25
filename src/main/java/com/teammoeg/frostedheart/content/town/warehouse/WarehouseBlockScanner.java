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

package com.teammoeg.frostedheart.content.town.warehouse;

import com.teammoeg.frostedheart.content.town.OccupiedArea;
import com.teammoeg.frostedheart.content.town.blockscanner.FloorBlockScanner;
import net.minecraft.world.level.block.NetherVines;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.AbstractMap;

//仓库的结构不考虑密封，体积仅统计地板上方的空气。
public class WarehouseBlockScanner extends FloorBlockScanner {
    private int volume = 0;
    private int area = 0;
    private static final int MAX_SCANNING_TIMES = 512;
    private final OccupiedArea occupiedArea = new OccupiedArea();

    public WarehouseBlockScanner(Level world, BlockPos startPos) {
        super(world, startPos);
    }

    public int getVolume(){return this.volume;}
    public int getArea(){return this.area;}
    public OccupiedArea getOccupiedArea(){return this.occupiedArea;}

    @Override
    public boolean isValidFloor(BlockPos pos){
        return isFloorBlock(pos) && isAirOrLadder(world, pos.above()) && isAirOrLadder(world, pos.above(2));
    }

    public boolean scan(){
        return scan(MAX_SCANNING_TIMES, (pos1)->{
            this.area++;
            AbstractMap.SimpleEntry<Integer, Boolean> floorInformation = countBlocksAbove(pos1, (pos2)->!NetherVines.isValidGrowthState(world.getBlockState(pos2)));
            if(!floorInformation.getValue()) this.isValid=false;
            this.volume += floorInformation.getKey();
            occupiedArea.add(toColumnPos(pos1));
        },(useless)->false);
    }
}
