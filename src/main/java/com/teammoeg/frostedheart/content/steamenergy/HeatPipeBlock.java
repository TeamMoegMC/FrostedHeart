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

import com.teammoeg.chorda.block.CEntityBlock;
import com.teammoeg.frostedheart.content.steamenergy.pipe.CPipeBlock;
import com.teammoeg.frostedheart.bootstrap.common.FHBlockEntityTypes;
import com.teammoeg.frostedheart.content.steamenergy.capabilities.HeatCapabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

import java.util.function.Supplier;

public class HeatPipeBlock extends CPipeBlock<HeatPipeBlock> implements CEntityBlock<HeatPipeTileEntity> {

    public HeatPipeBlock(Properties blockProps) {
        super(HeatPipeBlock.class, blockProps);
        this.lightOpacity = 0;
    }


    @Override
    public boolean canConnectTo(LevelAccessor world, BlockPos neighbourPos, BlockState neighbour, Direction direction) {
        return HeatCapabilities.canConnectAt(world, neighbourPos, direction.getOpposite());
    }


    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
    }


    @Override
    public Supplier<BlockEntityType<HeatPipeTileEntity>> getBlock() {
        return FHBlockEntityTypes.HEATPIPE;
    }


}
