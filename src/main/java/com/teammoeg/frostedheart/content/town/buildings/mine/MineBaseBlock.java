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

import com.teammoeg.chorda.block.CEntityBlock;
import com.teammoeg.chorda.lang.Components;
import com.teammoeg.frostedheart.bootstrap.common.FHBlockEntityTypes;
import com.teammoeg.frostedheart.content.town.block.AbstractTownBuildingBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class MineBaseBlock extends AbstractTownBuildingBlock implements CEntityBlock<MineBaseBlockEntity> {

    public MineBaseBlock(Properties blockProps) {
        super(blockProps);
    }


    //test
    public InteractionResult use(BlockState state, Level worldIn, BlockPos pos, Player player, InteractionHand handIn, BlockHitResult hit) {
        if (!worldIn.isClientSide && handIn == InteractionHand.MAIN_HAND) {
            MineBaseBlockEntity te = (MineBaseBlockEntity) worldIn.getBlockEntity(pos);
            if (te == null) {
                return InteractionResult.FAIL;
            }
            te.getBuilding().ifPresent(building -> {
                te.refresh_safe(building);
                player.displayClientMessage(Components.str(building.isBuildingWorkable() ? "Valid working environment" : "Invalid working environment"), false);
                player.displayClientMessage(Components.str("Structure: "+(building.isStructureValid? "Valid" : "Invalid")), false);
                player.displayClientMessage(Components.str("Area: " + (building.area)), false);
                player.displayClientMessage(Components.str("Volume: " + (building.volume)), false);
                player.displayClientMessage(Components.str("Temperature: " + String.format("%.2f", building.temperature) + "°C"), false);
                player.displayClientMessage(Components.str("Temperature valid: " + (building.isTemperatureValid() ? "Yes" : "No")), false);
                if(building.isBuildingWorkable())
                    player.displayClientMessage(Components.str("Max residents: " + (building.maxResidents)), false);
                player.displayClientMessage(Components.str("Linked mines: " + (building.linkedMines.size())), false);
                player.displayClientMessage(Components.str("Rating: " + String.format("%.2f", building.rating)), false);
            });
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }


	@Override
	public Supplier<BlockEntityType<MineBaseBlockEntity>> getBlock() {
		return FHBlockEntityTypes.MINE_BASE;
	}
 
}
