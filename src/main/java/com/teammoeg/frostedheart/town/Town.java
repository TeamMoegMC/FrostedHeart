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

package com.teammoeg.frostedheart.town;

import java.util.Optional;

/**
 * Interface for accessing town data
 */
public interface Town {
    /**
     * Adds resource.
     *
     * @param name     the resouce type
     * @param val      procuded
     * @param simulate simulate process, not actually add.
     * @return the value that has been add
     */
    double add(TownResourceType name, double val, boolean simulate);

    /**
     * Adds a service,
     * Service is kind or resource that only valid in this tick, and removed when tick ends. It would still count as a resource for get or cost
     *
     * @param name the resouce type
     * @param val  procuded
     * @return the value that has been add
     */
    double addService(TownResourceType name, double val);

    /**
     * Cost a resource
     *
     * @param name     the resouce type
     * @param val      to be cost
     * @param simulate simulate process, not actually cost.
     * @return the value that has been actually cost.
     */
    double cost(TownResourceType name, double val, boolean simulate);

    /**
     * Cost a resource as a service, that means does not actually cost the resource, but would cost temporary at this tick.
     *
     * @param name     the resouce type
     * @param val      to be cost
     * @param simulate simulate process, not actually cost.
     * @return the value that has been actually cost.
     */
    double costAsService(TownResourceType name, double val, boolean simulate);

    /**
     * Cost a service
     *
     * @param name     the resouce type
     * @param val      to be cost
     * @param simulate simulate process, not actually cost.
     * @return the value that has been actually cost.
     */
    double costService(TownResourceType name, double val, boolean simulate);

    /**
     * Gets resource.
     *
     * @param name the resouce type
     * @return resource amount
     */
    double get(TownResourceType name);

    /**
     * Gets the team town data, may be null if not a player team.
     *
     * @param name the resouce type
     * @return resource amount
     */
    Optional<TeamTownData> getTownData();

}
