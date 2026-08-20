/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.buildings.logistics;

import com.teammoeg.frostedheart.bootstrap.common.FHBlockEntityTypes;
import com.teammoeg.frostedheart.content.town.TownMathFunctions;
import com.teammoeg.frostedheart.content.town.block.AbstractTownBuildingBlockEntity;
import com.teammoeg.frostedheart.content.town.block.OccupiedVolume;
import com.teammoeg.frostedheart.content.town.block.blockscanner.AbstractBlockScanner;
import com.teammoeg.frostedheart.content.town.block.blockscanner.BuildingBlockScanner;
import com.teammoeg.frostedheart.content.town.block.blockscanner.FloorBlockScanner;
import com.teammoeg.frostedheart.content.town.building.AbstractTownBuilding;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Scans and exposes the logical building owned by a transport-station core. */
public class TransportStationBlockEntity
        extends AbstractTownBuildingBlockEntity<TransportStationBuilding>
        implements MenuProvider {
    public TransportStationBlockEntity(BlockPos pos, BlockState state) {
        super(FHBlockEntityTypes.TRANSPORT_STATION.get(), pos, state);
    }

    @Override
    public boolean scanStructure(TransportStationBuilding building) {
        Level level = Objects.requireNonNull(this.level);
        List<BlockPos> doors = AbstractBlockScanner.getBlocksAdjacent(
                getBlockPos(), pos -> level.getBlockState(pos).is(BlockTags.DOORS));
        if (doors.isEmpty()) return false;

        Set<Long> attemptedStarts = new HashSet<>();
        for (BlockPos door : doors) {
            BlockPos floorBelowDoor = AbstractBlockScanner.getBlockBelow(
                    level, pos -> !level.getBlockState(pos).is(BlockTags.DOORS), door);
            if (floorBelowDoor == null) continue;

            for (Direction direction : AbstractBlockScanner.PLANE_DIRECTIONS) {
                BlockPos start = floorBelowDoor.relative(direction);
                if (!FloorBlockScanner.isValidFloorOrLadder(level, start)) {
                    if (!FloorBlockScanner.isValidFloorOrLadder(level, start.below())
                            || FloorBlockScanner.isBuildingBlock(level, start.above(2))) {
                        continue;
                    }
                    start = start.below();
                }
                if (!attemptedStarts.add(start.asLong())) continue;

                BuildingBlockScanner scanner = new BuildingBlockScanner(level, start);
                if (scanner.scan()) {
                    applyScan(building, scanner);
                    return true;
                }
            }
        }
        return false;
    }

    private static void applyScan(
            TransportStationBuilding building,
            BuildingBlockScanner scanner
    ) {
        applyScanResult(
                building,
                scanner.getArea(),
                scanner.getVolume(),
                scanner.getOccupiedVolume());
    }

    static void applyScanResult(
            TransportStationBuilding building,
            int area,
            int volume,
            OccupiedVolume occupiedVolume
    ) {
        building.setArea(area);
        building.setVolume(volume);
        building.setOccupiedVolume(occupiedVolume);

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
