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

import com.teammoeg.frostedheart.content.town.ITownWithResidents;
import com.teammoeg.frostedheart.content.town.resident.Resident;

import java.util.Collection;
import java.util.UUID;

/**
 * 提供了一系列操纵居民的方法。
 * 与ITownResidentWorkBuilding不同的是：这个接口不要求建筑是参与工作的建筑，也就是说城镇房屋也包括在内，
 */
public interface ITownResidentBuilding {
    /**
     * Add resident to building
     *
     * @param resident resident to add
     * @return {@code true} if this set did not already contain the specified element
     */
    boolean addResident(Resident resident);

    /**
     * Remove resident from building
     *
     * @param resident the resident to remove
     * @return {@code true} if this set contained the specified element
     */
    boolean removeResident(Resident resident);

    int getMaxResidents();

    /**
     * get all IDs of residents in this building
     *
     * @return UUID Collection of all residents in building.
     */
    Collection<UUID> getResidentsID();

    /** Installs the final roster once; resident position mirrors are committed by TownAssignments. */
    void replaceResidents(Collection<UUID> residents);

    Collection<Resident> getResidents(ITownWithResidents townOfBuilding);
}
