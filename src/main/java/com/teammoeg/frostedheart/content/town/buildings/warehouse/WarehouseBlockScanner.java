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
    private final Set<BlockPos> emitterCandidates = new LinkedHashSet<>();

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
        if (block instanceof WarehouseLevelEmitterBlock) {
            emitterCandidates.add(pos.immutable());
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
        return getWallDevicePositions(interfaceCandidates, WarehouseInterfaceBlock.class, WarehouseInterfaceBlock.FACING);
    }

    /**
     * Returns only level emitters whose back faces the scanned room and whose front
     * does not, following the same wall rule as interfaces.
     */
    public Set<BlockPos> getWallEmitterPositions() {
        return getWallDevicePositions(emitterCandidates, WarehouseLevelEmitterBlock.class, WarehouseLevelEmitterBlock.FACING);
    }

    private Set<BlockPos> getWallDevicePositions(Set<BlockPos> candidates, Class<?> deviceClass, net.minecraft.world.level.block.state.properties.DirectionProperty facingProperty) {
        Set<BlockPos> result = new LinkedHashSet<>();
        for (BlockPos pos : candidates) {
            BlockState state = world.getBlockState(pos);
            if (!deviceClass.isInstance(state.getBlock()) || !state.hasProperty(facingProperty)) {
                continue;
            }
            Direction facing = state.getValue(facingProperty);
            boolean backIsInside = occupiedVolume.getOccupiedBlocks().get(pos.relative(facing.getOpposite()));
            boolean frontIsInside = occupiedVolume.getOccupiedBlocks().get(pos.relative(facing));
            if (backIsInside && !frontIsInside) {
                result.add(pos.immutable());
            }
        }
        return result;
    }
}
