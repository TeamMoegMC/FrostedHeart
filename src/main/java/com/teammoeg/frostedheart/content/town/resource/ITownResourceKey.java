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

package com.teammoeg.frostedheart.content.town.resource;

/**
 * A class that contains ITownResourceType and level of the resource.
 * You can change certain resources with this town resource key in TownResourceManager.
 */
public interface ITownResourceKey {
    ITownResourceType getType();
    int getLevel();

    /**
     * Create a new town resource key of given type and level.
     * Can accept interface ITownResourceType.
     */
    static ITownResourceKey of(ITownResourceType type, int level){
        return type.generateKey(level);
    }
}
