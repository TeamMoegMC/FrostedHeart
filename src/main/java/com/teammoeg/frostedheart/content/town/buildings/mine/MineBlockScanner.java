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

package com.teammoeg.frostedheart.content.town.buildings.mine;

import java.util.HashSet;

import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.town.block.OccupiedVolume;
import com.teammoeg.frostedheart.content.town.block.blockscanner.ConfinedSpaceScanner;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.Tags;

public class MineBlockScanner extends ConfinedSpaceScanner {
    private final int startX;
    private final int startY;
    private final int startZ;
    @Getter
    private int validStone = 0;
    @Getter
    private int light = 0;
    @Getter
    private double temperature = 0;
    private int volume = 0;//used to calculate temperature
    @Getter
    private final OccupiedVolume occupiedVolume = new OccupiedVolume();
    public MineBlockScanner(Level world, BlockPos startPos, int maxScanBlocks) {
        super(world, startPos, maxScanBlocks);
        this.startX = startPos.getX();
        this.startY = startPos.getY();
        this.startZ = startPos.getZ();
    }


    public static boolean isStoneOrOre(Level world, BlockPos pos){
        BlockState state = world.getBlockState(pos);
        return state.is(Tags.Blocks.ORES) || state.is(Tags.Blocks.STONE);
    }

    @Override
    protected boolean isValidAir(BlockPos pos){
        return Math.abs(pos.getZ()-startZ) < 6 && Math.abs(pos.getX()-startX) < 6 && Math.abs(pos.getY()-startY) < 5 && world.getBlockState(pos).isAir();
    }

    @Override
    protected HashSet<BlockPos> nextScanningBlocks(BlockPos pos) {
        HashSet<BlockPos> nextScanningBlocks = new HashSet<>();
        nextScanningBlocks.addAll(getBlocksAdjacent(pos, (pos1)->{
            if(scannedBlocks.contains(pos1.asLong()) || scanningBlocks.contains(pos1.asLong())) return false;
            return isValidAir(pos1);
        }));
        return nextScanningBlocks;
    }

    @Override
    protected void processAirBlock(BlockPos pos) {
        this.volume++;
        this.temperature += WorldTemperature.block(world, pos);
    }

    @Override
    protected void processNonAirBlock(BlockPos pos) {
        if(isStoneOrOre(world, pos)){
            validStone++;
            occupiedVolume.add(pos);
        }
        light += world.getBlockState(pos).getLightEmission(world, pos);
    }

    public boolean scan(){
        super.scan();

        if(validStone <= 0){
            this.isValid = false;
            return false;
        }
        light = light * 7 / validStone;//单个光源亮度约为14-15，乘7后约为100.
        temperature = temperature / volume;
        return this.isValid && validStone > 5;
    }
}
