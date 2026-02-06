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

package com.teammoeg.frostedheart.content.town.mine;

import java.util.function.Supplier;

import com.teammoeg.chorda.block.CEntityBlock;
import com.teammoeg.chorda.text.Components;
import com.teammoeg.frostedheart.bootstrap.common.FHBlockEntityTypes;
import com.teammoeg.frostedheart.content.town.AbstractTownWorkerBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class MineBaseBlock extends AbstractTownWorkerBlock implements CEntityBlock<MineBaseBlockEntity> {

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
            te.refresh_safe();
            player.displayClientMessage(Components.str(te.isWorkValid() ? "Valid working environment" : "Invalid working environment"), false);
            player.displayClientMessage(Components.str("status: "+te.getStatus()), false);
            player.displayClientMessage(Components.str("Area: " + (te.getArea())), false);
            player.displayClientMessage(Components.str("Volume: " + (te.getVolume())), false);
            player.displayClientMessage(Components.str("Chest: " + (te.getChest())), false);
            player.displayClientMessage(Components.str("Rack: " + (te.getRack())), false);
            player.displayClientMessage(Components.str("Linked mines: " + (te.linkedMines)), false);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }


	@Override
	public Supplier<BlockEntityType<MineBaseBlockEntity>> getBlock() {
		return FHBlockEntityTypes.MINE_BASE;
	}
 
}
