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

package com.teammoeg.frostedheart.content.town.buildings.institute;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.teammoeg.chorda.menu.CBlockEntityMenu;
import com.teammoeg.frostedheart.bootstrap.common.FHMenuTypes;
import com.teammoeg.frostedheart.content.town.ITownWithBuildings;
import com.teammoeg.frostedheart.content.town.ITownWithResidents;
import com.teammoeg.frostedheart.content.town.TeamTown;
import com.teammoeg.frostedheart.content.town.resident.Resident;

import net.minecraft.world.entity.player.Inventory;

/** Menu bridge for the synchronized client-side research institute snapshot. */
public class ResearchInstituteMenu extends CBlockEntityMenu<ResearchInstituteBlockEntity> {
    public ResearchInstituteMenu(int id, Inventory inventory, ResearchInstituteBlockEntity blockEntity) {
        // An information-only building: the notice-board screen shows no player inventory.
        super(FHMenuTypes.RESEARCH_INSTITUTE.get(), blockEntity, id, inventory.player, 0);
    }

    public Optional<ResearchInstituteBuilding> getBuilding() {
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

    public double getTemperatureModifier() {
        return blockEntity.getTemperatureModifier();
    }

    public boolean isHeated() {
        return blockEntity.getIsActive();
    }
}
