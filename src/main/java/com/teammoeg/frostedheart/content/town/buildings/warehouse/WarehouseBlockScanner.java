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

import com.teammoeg.chorda.util.CRegistryHelper;
import com.teammoeg.frostedheart.bootstrap.reference.FHTags;
import com.teammoeg.frostedheart.content.decoration.WarehouseStorageRackBlock;
import com.teammoeg.frostedheart.content.town.block.blockscanner.BuildingBlockScanner;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static blusunrize.immersiveengineering.api.utils.SafeChunkUtils.getBlockState;

@Getter
public class WarehouseBlockScanner extends BuildingBlockScanner {
    public final Map<String, Integer> decorations = new HashMap<>();
    public WarehouseBlockScanner(Level world, BlockPos startPos) {
        super(world, startPos);
    }

    @Override
    protected void processBuildingNonAirBlock(BlockPos pos) {
        BlockState blockState = getBlockState(world, pos);
        Block block = blockState.getBlock();

        if (block instanceof WarehouseStorageRackBlock) {
            String name = block.toString();
            decorations.merge(name, 1, Integer::sum);
        }
    }

    public boolean scan(){
        return super.scan();
    }
}
