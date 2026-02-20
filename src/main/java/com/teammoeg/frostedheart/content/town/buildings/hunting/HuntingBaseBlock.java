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

package com.teammoeg.frostedheart.content.town.buildings.hunting;

import com.teammoeg.chorda.block.CEntityBlock;
import com.teammoeg.chorda.lang.Components;
import com.teammoeg.chorda.math.CMath;
import com.teammoeg.frostedheart.bootstrap.common.FHBlockEntityTypes;
import com.teammoeg.frostedheart.content.town.block.AbstractTownBuildingBlock;
import com.teammoeg.frostedheart.util.client.FHClientUtils;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

import java.util.function.Supplier;

public class HuntingBaseBlock extends AbstractTownBuildingBlock implements CEntityBlock<HuntingBaseBlockEntity> {
    public HuntingBaseBlock(Properties blockProps) {
        super(blockProps);
    }


    @Override
    public void animateTick(BlockState stateIn, Level worldIn, BlockPos pos, RandomSource rand) {
        super.animateTick(stateIn, worldIn, pos, rand);
        if (stateIn.getValue(AbstractTownBuildingBlock.LIT)) {
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
            te.getBuilding().ifPresent(building -> {
                te.refresh_safe(building);
                player.displayClientMessage(Components.str(building.isBuildingWorkable() ? "Valid working environment" : "Invalid working environment"), false);
                player.displayClientMessage(Components.str("Raw temperature: " +
                        CMath.round(building.temperature, 2)), false);
                player.displayClientMessage(Components.str("Temperature modifier: " +
                        CMath.round(te.getTemperatureModifier(), 2)), false);
                player.displayClientMessage(Components.str("Effective temperature: " +
                        CMath.round(building.getEffectiveTemperature(), 2)), false);
                if(building.isBuildingWorkable())
                    player.displayClientMessage(Components.str("MaxResident: " + building.maxResidents), false);
                player.displayClientMessage(Components.str("TanningRackNum: " + building.tanningRackNum), false);
                player.displayClientMessage(Components.str("Volume: " + building.volume), false);
                player.displayClientMessage(Components.str("Area: " + building.area), false);
                player.displayClientMessage(Components.str("Rating: " +
                        CMath.round(building.rating, 2)), false);
            });
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }


	@Override
	public Supplier<BlockEntityType<HuntingBaseBlockEntity>> getBlock() {
		return FHBlockEntityTypes.HUNTING_BASE;
	}

}
