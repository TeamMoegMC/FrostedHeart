/*
 * Copyright (c) 2024 TeamMoeg
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

package com.teammoeg.frostedheart.content.town;

import com.teammoeg.frostedheart.content.town.resident.Resident;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface ITownWithResidents extends Town{
    /**
     * get all residents in the town.
     * @return collection of all residents
     */
    Collection<Resident> getAllResidents();

    /**
     * get a resident by uuid
     * @param id the uuid of the resident
     * @return the resident that catches the uuid. might be null if the resident doesn't exist in the town.
     */
    Optional<Resident> getResident(UUID id);

}
