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

package com.teammoeg.frostedheart.content.town.building;

import com.teammoeg.frostedheart.content.town.ITownWithBuildings;
import com.teammoeg.frostedheart.content.town.ITownWithResidents;
import com.teammoeg.frostedheart.content.town.resident.Resident;
import lombok.Getter;
import net.minecraft.core.BlockPos;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.Collections;
import java.util.Collection;

public abstract class AbstractTownResidentWorkBuilding extends AbstractTownBuilding implements ITownResidentWorkBuilding{
    protected Set<UUID> residentsID = new HashSet<>();
    @Getter
    private int maxResidents;

    public void setMaxResidents(int maxResidents) {
        if (this.maxResidents == maxResidents) {
            return;
        }
        this.maxResidents = maxResidents;
        fireChange();
    }

    protected AbstractTownResidentWorkBuilding(BlockPos pos) {
        super(pos);
    }

    @Override
    public void onRemoved(ITownWithBuildings buildingTown) {
        if(buildingTown instanceof ITownWithResidents residentTown){
            for(UUID uuid : this.residentsID){
                residentTown.getResident(uuid).ifPresent(resident -> {
                    resident.setWorkPos(null);
                });
            }
        }
        clearResidents();
    }

    public boolean addResident(Resident resident){
        resident.setWorkPos(this.getPos());
        boolean added = this.residentsID.add(resident.getUUID());
        if (added) {
            fireChange();
        }
        return added;
    }

    public boolean removeResident(Resident resident){
        resident.setWorkPos(null);
        boolean removed = this.residentsID.remove(resident.getUUID());
        if (removed) {
            fireChange();
        }
        return removed;
    }

    /** Clears the authoritative daily roster without mutating resident fields. */
    public void clearResidents() {
        if (this.residentsID.isEmpty()) {
            return;
        }
        this.residentsID.clear();
        fireChange();
    }

    @Override
    public void replaceResidents(Collection<UUID> residents) {
        Set<UUID> next = new HashSet<>(residents);
        if (residentsID.equals(next)) {
            return;
        }
        residentsID = next;
        fireChange();
    }

    @Override
    public Set<UUID> getResidentsID(){
        return Collections.unmodifiableSet(residentsID);
    }

}
