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

package com.teammoeg.frostedheart.content.town.buildings.warehouse;

import java.util.function.Supplier;

import com.teammoeg.chorda.block.CEntityBlock;
import com.teammoeg.chorda.text.Components;
import com.teammoeg.frostedheart.bootstrap.common.FHBlockEntityTypes;
import com.teammoeg.frostedheart.content.town.block.AbstractTownBuildingBlock;
import com.teammoeg.frostedheart.util.CConstants;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

public class WarehouseBlock extends AbstractTownBuildingBlock implements CEntityBlock<WarehouseBlockEntity> {
    public WarehouseBlock(Properties blockProps) {
        super(blockProps);
    }


    //test
    public InteractionResult use(BlockState state, Level worldIn, BlockPos pos, Player player, InteractionHand handIn, BlockHitResult hit) {
        if (!worldIn.isClientSide && handIn == InteractionHand.MAIN_HAND) {
            WarehouseBlockEntity te = (WarehouseBlockEntity) worldIn.getBlockEntity(pos);
            if (te == null) {
                return InteractionResult.FAIL;
            }
            te.getBuilding().ifPresentOrElse(building -> {
                te.refresh_safe(building);
                // The board opens whatever the room's state and says what is wrong; a broken room keeps its stock
                // closed, which WarehouseMenu.canUseStock enforces on the server.
                NetworkHooks.openScreen((ServerPlayer) player, te, pos);
            }, () -> player.displayClientMessage(Components.str(CConstants.NO_CORRESPONDING_TOWN_BUILDING_INSTANCE_FOUND), false));
            return InteractionResult.SUCCESS;
        }
        // Claim the click on the client too, so a held item (the Mayor's Seal, say) is not used as well.
        return worldIn.isClientSide && handIn == InteractionHand.MAIN_HAND ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }

	@Override
	public Supplier<BlockEntityType<WarehouseBlockEntity>> getBlock() {
		return FHBlockEntityTypes.WAREHOUSE;
	}

}
