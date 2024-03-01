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

package com.teammoeg.frostedheart.town.house;

import blusunrize.immersiveengineering.common.util.Utils;
import com.teammoeg.frostedheart.FHTileTypes;
import com.teammoeg.frostedheart.base.block.FHBaseBlock;
import com.teammoeg.frostedheart.base.block.FHBlockInterfaces;
import com.teammoeg.frostedheart.climate.chunkheatdata.ChunkHeatData;
import com.teammoeg.frostedheart.content.generator.GeneratorData;
import com.teammoeg.frostedheart.team.SpecialDataManager;
import com.teammoeg.frostedheart.team.SpecialDataTypes;
import com.teammoeg.frostedheart.town.TeamTownData;
import com.teammoeg.frostedheart.util.client.ClientUtils;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Random;

/**
 * A house in the town.
 */
public class HouseBlock extends FHBaseBlock {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public HouseBlock(Properties blockProps) {
        super(blockProps);
        this.setDefaultState(this.stateContainer.getBaseState().with(LIT, Boolean.FALSE));
    }

    @Override
    public TileEntity createTileEntity(@Nonnull BlockState state, @Nonnull IBlockReader world) {
        return FHTileTypes.HOUSE.get().create();
    }

    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }

    @Override
    public void animateTick(BlockState stateIn, World worldIn, BlockPos pos, Random rand) {
        super.animateTick(stateIn, worldIn, pos, rand);
        if (stateIn.get(LIT)) {
            ClientUtils.spawnSteamParticles(worldIn, pos);
        }
    }

    //test
    public ActionResultType onBlockActivated(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn, BlockRayTraceResult hit) {
        if (!worldIn.isRemote && handIn == Hand.MAIN_HAND) {
            HouseTileEntity te = (HouseTileEntity) worldIn.getTileEntity(pos);
            if (te == null) {
                return ActionResultType.FAIL;
            }
            player.sendStatusMessage(new StringTextComponent(te.isWorkValid() ? "Valid working environment" : "Invalid working environment"), false);
            player.sendStatusMessage(new StringTextComponent(te.isTemperatureValid() ? "Valid temperature" : "Invalid temperature"), false);
            player.sendStatusMessage(new StringTextComponent(te.isStructureValid() ? "Valid structure" : "Invalid structure"), false);
            player.sendStatusMessage(new StringTextComponent("Temperature: " + te.temperature), false);
            player.sendStatusMessage(new StringTextComponent("Volume: " + (te.volume)), false);
            player.sendStatusMessage(new StringTextComponent("Area: " + (te.area)), false);
            player.sendStatusMessage(new StringTextComponent("Score: " + te.rating), false);
            return ActionResultType.SUCCESS;
        }
        return ActionResultType.PASS;
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, BlockState state, @Nullable LivingEntity entity, ItemStack stack) {
        super.onBlockPlacedBy(world, pos, state, entity, stack);
        HouseTileEntity te = (HouseTileEntity) Utils.getExistingTileEntity(world, pos);
        if (te != null) {
            // register the house to the town
            if (entity instanceof PlayerEntity) {
                TeamTownData townData = SpecialDataManager.get((PlayerEntity) entity).getData(SpecialDataTypes.TOWN_DATA);
                GeneratorData generatorData = SpecialDataManager.get((PlayerEntity) entity).getData(SpecialDataTypes.GENERATOR_DATA);
                BlockPos generatorPos = generatorData.actualPos;
                // check if the house is in generator range
                float range = generatorData.RLevel;
                if (generatorPos.distanceSq(pos) <= range * range) {
                    townData.registerTownBlock(pos, te);
                }
            }
        }
    }
}
