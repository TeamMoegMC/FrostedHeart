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

import com.teammoeg.frostedheart.FHTileTypes;
import com.teammoeg.frostedheart.base.block.FHBaseBlock;
import com.teammoeg.frostedheart.climate.chunkheatdata.ChunkHeatData;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

/**
 * A house in the town.
 */
public class HouseBlock extends FHBaseBlock {

    public HouseBlock( Properties blockProps) {
        super(blockProps);
    }

    @Override
    public TileEntity createTileEntity(@Nonnull BlockState state, @Nonnull IBlockReader world) {
        return FHTileTypes.HOUSE.get().create();
    }

    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }

    //test
    public ActionResultType onBlockActivated(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn, BlockRayTraceResult hit) {
        if (!worldIn.isRemote && handIn == Hand.MAIN_HAND) {
            HouseTileEntity houseTileEntity = (HouseTileEntity) worldIn.getTileEntity(pos);
            player.sendStatusMessage(new StringTextComponent("isRoomValid:" + (houseTileEntity.isWorkValid() ? "true" : "false")), false);
            player.sendStatusMessage(new StringTextComponent("volume" + (houseTileEntity.volume)), false);
            player.sendStatusMessage(new StringTextComponent("area" + (houseTileEntity.area)), false);
            player.sendStatusMessage(new StringTextComponent("score" + houseTileEntity.getRating()), false);
            //player.sendStatusMessage(new StringTextComponent("deco score" + houseTileEntity.score_deco), false);
            //player.sendStatusMessage(new StringTextComponent("space score" + houseTileEntity.score_space), false);
            //player.sendStatusMessage(new StringTextComponent("avg temperature" + houseTileEntity.temperature), false);
            //player.sendStatusMessage(new StringTextComponent("real temperature" + ChunkHeatData.getTemperature(worldIn, pos)), false);
        }
        return ActionResultType.SUCCESS;
    }
}
