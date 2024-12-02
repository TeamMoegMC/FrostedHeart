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

import com.teammoeg.frostedheart.content.town.resource.TownResourceKey;
import com.teammoeg.frostedheart.content.town.resource.TownResourceManager;
import com.teammoeg.frostedheart.content.town.resource.TownResourceType;

import java.util.Optional;

/**
 * Interface for accessing town data.
 * <p>
 * This is an abstract town, it may be a (player) team town, or a npc town.
 */
public interface Town {

    /**
     * Gets the resource manager.
     * Resource manager is used to change resources in the town.
     * Maybe there will be an interface super all resources managers, including town resource manager? I don't know.
     * @return resource manager
     */
    TownResourceManager getResourceManager();

    /**
     * Gets the team town data, may be null if not a player team.
     *
     * @return resource amount
     */
    Optional<TeamTownData> getTownData();

}
