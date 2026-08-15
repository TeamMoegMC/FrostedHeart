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
 */

package com.teammoeg.frostedheart.content.town.buildings.hunting;

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

public class HuntingBaseMenu extends CBlockEntityMenu<HuntingBaseBlockEntity> {
    public HuntingBaseMenu(int id, Inventory inventory, HuntingBaseBlockEntity blockEntity) {
        super(FHMenuTypes.HUNTING_BASE.get(), blockEntity, id, inventory.player, 0);
        addPlayerInventory(inventory, 8, 140, 197);
    }

    public Optional<HuntingBaseBuilding> getBuilding() {
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
