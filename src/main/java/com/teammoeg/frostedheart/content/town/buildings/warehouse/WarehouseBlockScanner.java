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

import com.teammoeg.frostedheart.content.decoration.WarehouseStorageRackBlock;
import com.teammoeg.frostedheart.content.town.block.blockscanner.BuildingBlockScanner;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static blusunrize.immersiveengineering.api.utils.SafeChunkUtils.getBlockState;

@Getter
public class WarehouseBlockScanner extends BuildingBlockScanner {
    public final Map<String, Integer> decorations = new HashMap<>();
    private final Set<BlockPos> interfaceCandidates = new LinkedHashSet<>();

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
        if (block instanceof WarehouseInterfaceBlock) {
            interfaceCandidates.add(pos.immutable());
        }
    }

    public boolean scan(){
        return super.scan();
    }

    /**
     * Returns only interfaces whose back faces the scanned room and whose front
     * does not. This excludes interface blocks used as furniture inside a room.
     */
    public Set<BlockPos> getWallInterfacePositions() {
        Set<BlockPos> result = new LinkedHashSet<>();
        for (BlockPos pos : interfaceCandidates) {
            BlockState state = world.getBlockState(pos);
            if (!(state.getBlock() instanceof WarehouseInterfaceBlock) || !state.hasProperty(WarehouseInterfaceBlock.FACING)) {
                continue;
            }
            Direction facing = state.getValue(WarehouseInterfaceBlock.FACING);
            boolean backIsInside = occupiedVolume.getOccupiedBlocks().get(pos.relative(facing.getOpposite()));
            boolean frontIsInside = occupiedVolume.getOccupiedBlocks().get(pos.relative(facing));
            if (backIsInside && !frontIsInside) {
                result.add(pos.immutable());
            }
        }
        return result;
    }
}
