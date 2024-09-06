/*
 * Copyright (c) 2021-2024 TeamMoeg
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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.teammoeg.frostedheart.FHTileTypes;
import com.teammoeg.frostedheart.base.block.FluidPipeBlock;
import com.teammoeg.frostedheart.content.steamenergy.capabilities.HeatCapabilities;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;

import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import org.jetbrains.annotations.NotNull;

public class HeatPipeBlock extends FluidPipeBlock<HeatPipeBlock>  {

    public HeatPipeBlock(Properties blockProps) {
        super(HeatPipeBlock.class,  blockProps);
        this.lightOpacity = 0;
    }


    @Override
	public boolean canConnectTo(LevelAccessor world, BlockPos neighbourPos, BlockState neighbour, Direction direction) {
		return HeatCapabilities.canConnectAt(world, neighbourPos, direction.getOpposite());
	}


    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return FHTileTypes.HEATPIPE.get().create(pos, state);
    }


    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
    }


    @Override
    public void tick(BlockState blockstate, ServerLevel world, BlockPos pos, RandomSource random) {
        super.tick(blockstate, world, pos, random);
        BlockEntity be = world.getBlockEntity(pos);
        if(be instanceof HeatPipeTileEntity heatPipeTileEntity) {
            heatPipeTileEntity.tick();
        }
    }
}
