/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.buildings.logistics;

import com.teammoeg.chorda.menu.CBlockEntityMenu;
import com.teammoeg.frostedheart.bootstrap.common.FHMenuTypes;
import com.teammoeg.frostedheart.content.town.ITownWithBuildings;
import com.teammoeg.frostedheart.content.town.ITownWithResidents;
import com.teammoeg.frostedheart.content.town.TeamTown;
import com.teammoeg.frostedheart.content.town.resident.Resident;
import net.minecraft.world.entity.player.Inventory;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Menu bridge for the synchronized client-side transport-station snapshot. */
public class TransportStationMenu extends CBlockEntityMenu<TransportStationBlockEntity> {
    public TransportStationMenu(
            int id,
            Inventory inventory,
            TransportStationBlockEntity blockEntity
    ) {
        super(FHMenuTypes.TRANSPORT_STATION.get(), blockEntity, id, inventory.player, 0);
        addPlayerInventory(inventory, 8, 140, 197);
    }

    public Optional<TransportStationBuilding> getBuilding() {
        return blockEntity.getBuilding();
    }

    public Optional<TeamTown> getTown() {
        ITownWithBuildings town = blockEntity.getTown();
        return town instanceof TeamTown teamTown ? Optional.of(teamTown) : Optional.empty();
    }

    public List<Resident> getResidents() {
        ITownWithBuildings town = blockEntity.getTown();
        if (!(town instanceof ITownWithResidents residentTown)) return List.of();
        return residentTown.getAllResidents().stream()
                .filter(resident -> blockEntity.getBlockPos().equals(resident.getWorkPos()))
                .sorted(Comparator.comparing(Resident::toString, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(Resident::getUUID))
                .toList();
    }
}
