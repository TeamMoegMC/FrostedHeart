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

package com.teammoeg.frostedheart.content.town.block;

import javax.annotation.Nullable;

import com.teammoeg.chorda.block.CBlock;
import com.teammoeg.chorda.dataholders.team.CTeamDataManager;
import com.teammoeg.chorda.dataholders.team.TeamDataHolder;
import com.teammoeg.frostedheart.content.climate.gamedata.chunkheat.ChunkHeatData;
import com.teammoeg.frostedheart.content.town.TeamTown;
import com.teammoeg.frostedheart.content.town.Town;
import com.teammoeg.frostedheart.content.town.provider.TeamTownProvider;
import com.teammoeg.frostedresearch.mixinutil.IOwnerTile;

import blusunrize.immersiveengineering.common.util.Utils;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.UUID;

public abstract class AbstractTownBuildingBlock extends CBlock {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final Properties TOWN_BUILDING_CORE_BLOCK_BASE_PROPERTY = Block.Properties
            .of()
            .sound(SoundType.WOOD)
            .requiresCorrectToolForDrops()
            .strength(2, 6)
            .noOcclusion();

    public AbstractTownBuildingBlock(Properties blockProps) {
        super(blockProps);
        this.registerDefaultState(this.stateDefinition.any().setValue(LIT, Boolean.FALSE).setValue(BlockStateProperties.FACING, Direction.SOUTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LIT,BlockStateProperties.FACING);
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity entity, ItemStack stack) {
        super.setPlacedBy(world, pos, state, entity, stack);
        if (Utils.getExistingTileEntity(world, pos) instanceof TownBlockEntity<?> townBlockEntity) {
            // register the house to the town
            if (entity instanceof ServerPlayer placer) {
                TeamDataHolder teamDataHolder = CTeamDataManager.get(placer);
                if (Town.DEBUG_MODE ||ChunkHeatData.hasActiveAdjust(world, pos)) {
                    TeamTown.from(placer).addTownBlock(pos, townBlockEntity);
                    if(townBlockEntity instanceof AbstractTownBuildingBlockEntity<?> abstractTownBuildingBlockEntity){

                        if(teamDataHolder != null){
                            abstractTownBuildingBlockEntity.townProvider = new TeamTownProvider(teamDataHolder.getId());
                        }
                    }
                }
                if(teamDataHolder != null){
                    IOwnerTile.trySetOwner((BlockEntity) townBlockEntity, teamDataHolder.getId());
                }
            }
        }
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(BlockStateProperties.FACING, context.getClickedFace().getOpposite());
    }
}

