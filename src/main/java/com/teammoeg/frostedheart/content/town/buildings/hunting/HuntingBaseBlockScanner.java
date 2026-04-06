/*
 * Copyright (c) 2026 TeamMoeg
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

package com.teammoeg.frostedheart.content.town.buildings.hunting;

import com.teammoeg.chorda.util.CRegistryHelper;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.town.block.blockscanner.BuildingBlockScanner;
import lombok.Getter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.CommandBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.Objects;

@Getter
public class HuntingBaseBlockScanner extends BuildingBlockScanner {
    public int tanningRackNum = 0;
    public double temperature;


    public HuntingBaseBlockScanner(Level world, BlockPos startPos) {
        super(world, startPos);
    }

    @Override
    protected void processBuildingAirBlock(BlockPos pos) {
        temperature += WorldTemperature.block(world, pos);
    }


    @Override
    protected void processBuildingNonAirBlock(BlockPos pos){
        BlockState blockState = world.getBlockState(pos);
        if(Objects.requireNonNull(CRegistryHelper.getRegistryName(blockState.getBlock())).getPath().equals("tanning_rack") || blockState.getBlock() instanceof CommandBlock) tanningRackNum++;
    }

    @Override
    public boolean scan() {
        super.scan();
        if(this.isValid){
            this.temperature /= this.volume;
            return true;
        }else{
            this.temperature = 0;
            return false;
        }
    }

}
