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

package com.teammoeg.frostedheart.content.town.buildings.house;

import com.teammoeg.frostedheart.bootstrap.reference.FHTags;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.chorda.util.CRegistryHelper;
import com.teammoeg.frostedheart.content.town.block.blockscanner.*;

import lombok.Getter;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.BedPart;

import java.util.*;

import static blusunrize.immersiveengineering.api.utils.SafeChunkUtils.getBlockState;

//严格来讲这不是一个正常的BlockScanner，而是一个用于将FloorBlockScanner和ConfinedSpaceScanner结合起来的类
@Getter
public class HouseBlockScanner extends BuildingBlockScanner {
    public final Map<String/*block.getName()*/, Integer> decorations = new HashMap<>();
    public final List<BlockPos> beds = new ArrayList<>();
    public double temperature = 0;//average temperature

    public HouseBlockScanner(Level world, BlockPos startPos) {
        super(world, startPos);
    }

    //扫描装饰方块，并统计装饰数量
    //统计床的数量
    @Override
    protected void processBuildingNonAirBlock(BlockPos pos) {
        BlockState blockState = getBlockState(world, pos);
        Block block = blockState.getBlock();
        if(block instanceof BedBlock){
            if(blockState.getValue(BedBlock.PART) == BedPart.HEAD){
                beds.add(pos);
            }
            return;
        }
        if (blockState.is(FHTags.Blocks.TOWN_DECORATIONS.tag) || Objects.requireNonNull(CRegistryHelper.getRegistryName(block)).getNamespace().equals("cfm")) {
            String name = block.toString();
            decorations.merge(name, 1, Integer::sum);
        }
    }

    @Override
    protected void processBuildingAirBlock(BlockPos pos) {
        temperature += WorldTemperature.block(world, pos);
    }


    public boolean scan() {
        super.scan();
        if(this.isValid){
            this.temperature /= this.volume;
            return true;
        }else{
            this.temperature = 0;
            this.decorations.clear();
            this.beds.clear();
            return false;
        }
    }
    /*
    * 此方法尚存在缺陷，下面（横截面示意图）这种情况
    块块块块
    块块空空
    空空空空
    空空块块
    块块块块
    * 如果是玩家的话，不能从左边走到右边或者从右边走到左边（会顶头），但是blockScanner可以直接通过
    * 考虑到这种情况应该出现得很少，并且影响不大，暂且不去修它
    * */
}
