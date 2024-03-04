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

import java.util.function.Function;

public enum TownResourceType {
    /**
     * Storage space
     */
    STORAGE(null),
    /**
     * Residents
     */
    RESIDENT(t -> 1000D),
    /**
     * Work force
     */
    WORK(null),
    /**
     * Generator power
     */
    HEAT(null),
    WOOD(t -> 250D + 100 * t.get(STORAGE)),
    IRON(t -> 250D + 100 * t.get(STORAGE)),
    STONE(t -> 250D + 100 * t.get(STORAGE)),
    TOOL(t -> 250D + 100 * t.get(STORAGE)),
    RAW_FOOD(t -> 250D + 100 * t.get(STORAGE)),
    PREP_FOOD(t -> 250D + 100 * t.get(STORAGE));
    Function<Town, Double> maxStorage;

    public static TownResourceType from(String t) {
        return TownResourceType.valueOf(t.toUpperCase());
    }

    /**
     * Create a new type
     *
     * @param maxStorage provider for max storage calculations. Must be null for services
     */
    TownResourceType(Function<Town, Double> maxStorage) {
        this.maxStorage = maxStorage;
    }

    public int getIntMaxStorage(Town town) {
        if (maxStorage == null) return 0;
        return (int) (maxStorage.apply(town) * 1000);
    }

    public String getKey() {
        return this.name().toLowerCase();
    }

    public double getMaxStorage(Town town) {
        if (maxStorage == null) return 0;
        return maxStorage.apply(town);
    }
}
