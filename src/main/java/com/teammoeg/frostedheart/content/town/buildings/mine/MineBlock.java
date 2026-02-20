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

package com.teammoeg.frostedheart.content.town.buildings.mine;

import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.teammoeg.chorda.block.CEntityBlock;
import com.teammoeg.chorda.dataholders.team.CTeamDataManager;
import com.teammoeg.chorda.dataholders.team.TeamDataHolder;
import com.teammoeg.chorda.lang.Components;
import com.teammoeg.frostedheart.bootstrap.common.FHBlockEntityTypes;
import com.teammoeg.frostedheart.content.climate.gamedata.chunkheat.ChunkHeatData;
import com.teammoeg.frostedheart.content.town.Town;
import com.teammoeg.frostedheart.content.town.block.AbstractTownBuildingBlock;
import com.teammoeg.frostedheart.content.town.TeamTown;

import blusunrize.immersiveengineering.common.util.Utils;
import com.teammoeg.frostedheart.content.town.block.AbstractTownBuildingBlockEntity;
import com.teammoeg.frostedheart.content.town.block.TownBlockEntity;
import com.teammoeg.frostedheart.content.town.provider.TeamTownProvider;
import com.teammoeg.frostedresearch.mixinutil.IOwnerTile;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class MineBlock extends AbstractTownBuildingBlock implements CEntityBlock<MineBlockEntity> {

    public MineBlock(Properties blockProps){
        super(blockProps);
    }



    //test
    public InteractionResult use(BlockState state, Level worldIn, BlockPos pos, Player player, InteractionHand handIn, BlockHitResult hit) {
        if (!worldIn.isClientSide && handIn == InteractionHand.MAIN_HAND) {
            MineBlockEntity te = (MineBlockEntity) worldIn.getBlockEntity(pos);
            if (te == null) {
                return InteractionResult.FAIL;
            }
            te.getBuilding().ifPresent(building -> {
                te.refresh_safe(building);
                player.displayClientMessage(Components.str(building.isBuildingWorkable() ? "Workable" : "Unworkable"), false);
                player.displayClientMessage(Components.str(building.isStructureValid ? "Valid structure" : "Invalid structure"), false);
                player.displayClientMessage(Components.str("Biome: " + building.biomePath), false);
                player.displayClientMessage(Components.str("Rating: " + String.format("%.2f", building.rating)), false);
            });
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity entity, ItemStack stack) {
        super.setPlacedBy(world, pos, state, entity, stack);
        if (Utils.getExistingTileEntity(world, pos) instanceof TownBlockEntity<?> townBlockEntity) {
            // register the house to the town
            if (entity instanceof ServerPlayer placer) {
                TeamDataHolder teamDataHolder = CTeamDataManager.get(placer);
                TeamTown.from(placer).addTownBlock(pos, townBlockEntity);
                if(townBlockEntity instanceof AbstractTownBuildingBlockEntity<?> abstractTownBuildingBlockEntity){

                    if(teamDataHolder != null){
                        abstractTownBuildingBlockEntity.townProvider = new TeamTownProvider(teamDataHolder.getId());
                    }
                }

                if(teamDataHolder != null){
                    IOwnerTile.trySetOwner((BlockEntity) townBlockEntity, teamDataHolder.getId());
                }
            }
        }
    }



	@Override
	public Supplier<BlockEntityType<MineBlockEntity>> getBlock() {
		return FHBlockEntityTypes.MINE;
	}
}
