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

package com.teammoeg.frostedheart.content.town.hunting;

import com.teammoeg.chorda.block.CEntityBlock;
import com.teammoeg.chorda.dataholders.team.CTeamDataManager;
import com.teammoeg.chorda.math.CMath;
import com.teammoeg.chorda.text.Components;
import com.teammoeg.chorda.util.CUtils;
import com.teammoeg.frostedheart.bootstrap.common.FHBlockEntityTypes;
import com.teammoeg.frostedheart.content.town.AbstractTownWorkerBlock;
import com.teammoeg.frostedheart.util.client.FHClientUtils;
import com.teammoeg.frostedresearch.mixinutil.IOwnerTile;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

import java.util.function.Supplier;

import javax.annotation.Nullable;

public class HuntingBaseBlock extends AbstractTownWorkerBlock implements CEntityBlock<HuntingBaseBlockEntity> {
    public HuntingBaseBlock(Properties blockProps) {
        super(blockProps);
    }


    @Override
    public void animateTick(BlockState stateIn, Level worldIn, BlockPos pos, RandomSource rand) {
        super.animateTick(stateIn, worldIn, pos, rand);
        if (stateIn.getValue(AbstractTownWorkerBlock.LIT)) {
            FHClientUtils.spawnSteamParticles(worldIn, pos);
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level worldIn, BlockPos pos, Player player, InteractionHand handIn, BlockHitResult hit) {
        if (!worldIn.isClientSide && handIn == InteractionHand.MAIN_HAND) {
            HuntingBaseBlockEntity te = (HuntingBaseBlockEntity) worldIn.getBlockEntity(pos);
            if (te == null) {
                return InteractionResult.FAIL;
            }
            te.refresh_safe();
            player.displayClientMessage(Components.str(te.isWorkValid() ? "Valid working environment" : "Invalid working environment"), false);
            player.displayClientMessage(Components.str("status:"+te.getStatus()), false);
            player.displayClientMessage(Components.str("Raw temperature: " +
                    CMath.round(te.getTemperature(), 2)), false);
            player.displayClientMessage(Components.str("Temperature modifier: " +
                    CMath.round(te.getTemperatureModifier(), 2)), false);
            player.displayClientMessage(Components.str("Effective temperature: " +
                    CMath.round(te.getEffectiveTemperature(), 2)), false);
            player.displayClientMessage(Components.str("BedNum: " + te.getBedNum()), false);
            if(te.isWorkValid())
            	player.displayClientMessage(Components.str("MaxResident: " + te.getState().maxResidents), false);
            player.displayClientMessage(Components.str("TanningRackNum: " + te.getTanningRackNum()), false);
            player.displayClientMessage(Components.str("chestNum: " + te.getChestNum()), false);
            player.displayClientMessage(Components.str("Volume: " + (te.getVolume())), false);
            player.displayClientMessage(Components.str("Area: " + (te.getArea())), false);
            player.displayClientMessage(Components.str("Rating: " +
                    CMath.round(te.getRating(), 2)), false);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }


	@Override
	public Supplier<BlockEntityType<HuntingBaseBlockEntity>> getBlock() {
		return FHBlockEntityTypes.HUNTING_BASE;
	}

}
