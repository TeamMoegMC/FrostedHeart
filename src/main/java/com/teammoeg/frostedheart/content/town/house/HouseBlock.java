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

package com.teammoeg.frostedheart.content.town.house;

import com.teammoeg.frostedheart.FHTileTypes;
import com.teammoeg.frostedheart.base.block.FHEntityBlock;
import com.teammoeg.frostedheart.content.town.AbstractTownWorkerBlock;
import com.teammoeg.frostedheart.util.MathUtils;
import com.teammoeg.frostedheart.util.TranslateUtils;
import com.teammoeg.frostedheart.util.client.ClientUtils;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Random;
import java.util.function.Supplier;

import net.minecraft.world.level.block.state.BlockBehaviour.Properties;

/**
 * A house in the town.
 */
public class HouseBlock extends AbstractTownWorkerBlock implements FHEntityBlock<HouseTileEntity>{
    public HouseBlock(Properties blockProps) {
        super(blockProps);
    }

    @Override
    public void animateTick(BlockState stateIn, Level worldIn, BlockPos pos, RandomSource rand) {
        super.animateTick(stateIn, worldIn, pos, rand);
        if (stateIn.getValue(AbstractTownWorkerBlock.LIT)) {
            ClientUtils.spawnSteamParticles(worldIn, pos);
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level worldIn, BlockPos pos, Player player, InteractionHand handIn, BlockHitResult hit) {
        if (!worldIn.isClientSide && handIn == InteractionHand.MAIN_HAND) {
            HouseTileEntity te = (HouseTileEntity) worldIn.getBlockEntity(pos);
            if (te == null) {
                return InteractionResult.FAIL;
            }
            te.refresh();
            player.displayClientMessage(TranslateUtils.str(te.isWorkValid() ? "Valid working environment" : "Invalid working environment"), false);
            player.displayClientMessage(TranslateUtils.str(te.isTemperatureValid() ? "Valid temperature" : "Invalid temperature"), false);
            player.displayClientMessage(TranslateUtils.str(te.isStructureValid() ? "Valid structure" : "Invalid structure"), false);
            player.displayClientMessage(TranslateUtils.str("Raw temperature: " +
                    MathUtils.round(te.getTemperature(), 2)), false);
            player.displayClientMessage(TranslateUtils.str("Temperature modifier: " +
                    MathUtils.round(te.getTemperatureModifier(), 2)), false);
            player.displayClientMessage(TranslateUtils.str("Effective temperature: " +
                    MathUtils.round(te.getEffectiveTemperature(), 2)), false);
            player.displayClientMessage(TranslateUtils.str("Volume: " + (te.getVolume())), false);
            player.displayClientMessage(TranslateUtils.str("Area: " + (te.getArea())), false);
            player.displayClientMessage(TranslateUtils.str("Rating: " +
                    MathUtils.round(te.getRating(), 2)), false);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

	@Override
	public Supplier<BlockEntityType<HouseTileEntity>> getBlock() {
		return FHTileTypes.HOUSE;
	}
}
