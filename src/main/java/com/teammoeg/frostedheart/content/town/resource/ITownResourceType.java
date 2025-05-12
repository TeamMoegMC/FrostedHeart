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
 * The interface of town resource type.
 * Town resource type should be a enum.
 * Town resource type have a max level as integer.
 * Max level can be 0 or any positive integer.
 */
public interface ITownResourceType extends IGettable {
    int getMaxLevel();

    /**
     * 生成这个TownResourceType的小写字符串。
     * 并非ItemResourceKey.
     * @return 该ItemResourceType名字的小写字符串。
     */
    String getKey();

    /**
     * Generate town resource attribute of this resource type with given level.
     * @param level The level of the resource. Shouldn't be negative or more than max level.
     * @return TownResourceAttribute of this type and given level.
     */
    ITownResourceAttribute generateAttribute(int level);

    static ITownResourceType from(String key){
        for(ITownResourceType type:ItemResourceType.values()){
            if(type.getKey().equals(key)) return type;
        }
        for(ITownResourceType type:VirtualResourceType.values()){
            if(type.getKey().equals(key)) return type;
        }
        return null;
    }

    /**
     * Check if the given level is valid for this resource type.
     */
    default boolean isLevelValid(int level){
        return level>=0&&level<=getMaxLevel();
    }
}
