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

package com.teammoeg.frostedheart.content.town.buildings.logistics;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.teammoeg.frostedheart.bootstrap.common.FHBlockEntityTypes;
import com.teammoeg.frostedheart.content.town.TownMathFunctions;
import com.teammoeg.frostedheart.content.town.block.AbstractTownBuildingBlockEntity;
import com.teammoeg.frostedheart.content.town.block.blockscanner.BlockScanner;
import com.teammoeg.frostedheart.content.town.block.blockscanner.BlockScanner.RoomData;
import com.teammoeg.frostedheart.content.town.building.AbstractTownBuilding;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;

/** Scans and exposes the logical building owned by a transport-station core. */
public class TransportStationBlockEntity
        extends AbstractTownBuildingBlockEntity<TransportStationBuilding>
        implements MenuProvider {
    public TransportStationBlockEntity(BlockPos pos, BlockState state) {
        super(FHBlockEntityTypes.TRANSPORT_STATION.get(), pos, state);
    }

    @Override
    public StructureScanResult scanStructure(TransportStationBuilding building) {
        RoomData room = BlockScanner.scanRoomDataFromBlock(level, worldPosition);
        if (room == null) {
            return StructureScanResult.DATA_UNAVAILABLE;
        }
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
            TransportStationBuilding building,
            int area,
            int volume
    ) {
        FHConfig.Server.Town.BuildingScoring scoring = FHConfig.SERVER.TOWN.BUILDING_SCORING;
        FHConfig.Server.Town.TransportStation config = FHConfig.SERVER.TOWN.TRANSPORT_STATION;
        double spaceRating = TownMathFunctions.calculateSpaceRating(
                volume,
                area,
                scoring.spaceAreaCoefficient.get(),
                scoring.spaceHeightLogCoefficient.get(),
                scoring.spaceHeightLogOffset.get(),
                scoring.spaceResponseScale.get(),
                scoring.spaceResponseExponent.get());
        double effectiveFloorArea = Math.max(0.0, spaceRating * area);
        int calculatedSlots = (int) Math.floor(
                effectiveFloorArea / config.floorBlocksPerWorkerSlot.get());
        building.setArea(area);
        building.setVolume(volume);
        building.setMaxResidents(Math.max(config.minimumWorkerSlots.get(), calculatedSlots));
    }

    @Override
    public @Nullable TransportStationBuilding getBuilding(AbstractTownBuilding building) {
        return building instanceof TransportStationBuilding transportStation
                ? transportStation
                : null;
    }

    @Override
    public @NotNull TransportStationBuilding createBuilding() {
        return new TransportStationBuilding(getBlockPos());
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(
            int id,
            @NotNull Inventory inventory,
            @NotNull Player player
    ) {
        return new TransportStationMenu(id, inventory, this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.frostedheart.transport_station");
    }
}
