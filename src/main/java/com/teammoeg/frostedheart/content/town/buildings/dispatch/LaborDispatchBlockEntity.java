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

package com.teammoeg.frostedheart.content.town.buildings.dispatch;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.teammoeg.frostedheart.bootstrap.common.FHBlockEntityTypes;
import com.teammoeg.frostedheart.content.town.block.AbstractTownBuildingBlockEntity;
import com.teammoeg.frostedheart.content.town.block.blockscanner.BlockScanner;
import com.teammoeg.frostedheart.content.town.block.blockscanner.BlockScanner.RoomData;
import com.teammoeg.frostedheart.content.town.building.AbstractTownBuilding;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;

/** Scans and exposes the logical building owned by a labor-dispatch core. */
public class LaborDispatchBlockEntity
        extends AbstractTownBuildingBlockEntity<LaborDispatchBuilding>
        implements MenuProvider {
    public LaborDispatchBlockEntity(BlockPos pos, BlockState state) {
        super(FHBlockEntityTypes.LABOR_DISPATCH.get(), pos, state);
    }

    @Override
    public StructureScanResult scanStructure(LaborDispatchBuilding building) {
        RoomData room = BlockScanner.scanRoomDataFromBlock(level, worldPosition);
        if (room == null) {
            return StructureScanResult.DATA_UNAVAILABLE;
        }
        building.setRoadEntrances(room.doors);
        if (room.doors.isEmpty()) {
            room = room.withValidity(false);
        }
        if (!room.isValid()) {
            building.setOccupiedVolume(room.occupiedCells);
            return StructureScanResult.INVALID;
        }
        applyScanResult(building, room.area, room.volume);
        building.setOccupiedVolume(room.occupiedCells);
        return StructureScanResult.VALID;
    }

    static void applyScanResult(
            LaborDispatchBuilding building,
            int area,
            int volume
    ) {
        building.setArea(area);
        building.setVolume(volume);
        building.setMaxResidents(Math.max(0, area / 4));
    }

    @Override
    public @Nullable LaborDispatchBuilding getBuilding(AbstractTownBuilding building) {
        return building instanceof LaborDispatchBuilding transportStation
                ? transportStation
                : null;
    }

    @Override
    public @NotNull LaborDispatchBuilding createBuilding() {
        return new LaborDispatchBuilding(getBlockPos());
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(
            int id,
            @NotNull Inventory inventory,
            @NotNull Player player
    ) {
        return new LaborDispatchMenu(id, inventory, this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.frostedheart.labor_dispatch");
    }
}
