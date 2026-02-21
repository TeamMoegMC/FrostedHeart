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
import java.util.Set;

import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.town.block.OccupiedArea;
import com.teammoeg.frostedheart.content.town.block.blockscanner.BlockScanner;
import com.teammoeg.frostedheart.content.town.block.blockscanner.FloorBlockScanner;
import com.teammoeg.frostedheart.content.town.block.blockscanner.HeightCheckingInfo;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.Tags;
import se.mickelus.tetra.blocks.rack.RackBlock;

//矿场基地需要有铁轨连通到矿场，因此不做任何的密封性要求，有个顶就行。
public class MineBaseBlockScanner extends FloorBlockScanner {
    public MineBaseBlockScanner(Level world, BlockPos startPos) {
        super(world, startPos);
    }
    private final HashSet<BlockPos> rails = new HashSet<>();
    @Getter
    private int area = 0;
    @Getter
    private int volume;
    @Getter
    private int chest = 0;
    @Getter
    private int rack = 0;
    @Getter
    private double temperature = 0;
    private int counter_for_temperature = 0;//used to calculate average temperature.
    @Getter
    private final Set<BlockPos> linkedMines = new HashSet<>();
    @Getter
    private final OccupiedArea occupiedArea = new OccupiedArea();

    @Override
    public boolean isValidFloor(BlockPos pos){
        //BlockState state = world.getBlockState(pos);
        if(scanningBlocksNew.contains(pos)) return false;
        //if(state.is(BlockTags.RAILS)){
        //    rails.add(pos);
        //    return false;
        //}
        if(!isFloorBlock(pos)){
            return false;
        }
        HeightCheckingInfo floorInformation = countBlocksAbove(world,pos,(pos1)->{
            if(isHouseBlock(pos1)) return true;
            BlockState state1 = world.getBlockState(pos1);
            if(state1.is(BlockTags.RAILS)){
                rails.add(pos1);
                return true;
            }
            if(state1.is(Tags.Blocks.CHESTS)){
                chest++;
                return false;
            }
            if(state1.getBlock().equals(RackBlock.instance)){
                rack++;
                return false;
            }
            if(state1.isAir()){
                temperature += WorldTemperature.block(world, pos1);
                counter_for_temperature++;
                return false;
            }
            return false;
        });
        if(floorInformation.result()){
            if(floorInformation.height() >= 2){
                volume += floorInformation.height();
                return true;
            }
            return false;
        }
        this.isValid = false;
        return false;
    }

    public boolean scan(){
        this.scan(256, (blockPos) -> {
            area++;
            occupiedArea.add(toColumnPos(blockPos));
            }, BlockScanner.PREDICATE_FALSE);
        temperature /= counter_for_temperature;
        if(!this.rails.isEmpty()){
            RailScanner railScanner = new RailScanner();
            railScanner.scan(512, CONSUMER_NULL, PREDICATE_FALSE);
        }
        return this.area > 4 && this.volume > 8 && this.isValid;
    }

    class RailScanner extends FloorBlockScanner{
        public RailScanner(){
            super(MineBaseBlockScanner.this.world, MineBaseBlockScanner.this.rails.iterator().next());
            this.scanningBlocks = MineBaseBlockScanner.this.rails;
        }

        @Override
        public boolean isValidFloor(BlockPos pos){
            if(world.getBlockState(pos).is(BlockTags.RAILS)){
                return world.getBlockState(pos.above()).isAir();
            } else if(world.getBlockState(pos).getBlock().equals(FHBlocks.MINE.get())){
                linkedMines.add(pos);
            }
            return false;
        }
    }
}
