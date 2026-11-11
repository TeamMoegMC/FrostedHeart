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

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;

/**
 * 建筑编号：每种建筑从 1 号起，新建筑拿当时最小的空号，拿到后不再变。旧存档里还没有编号的建筑按位置顺序补上。
 * 只在服务端运行；编号随建筑一起存盘和同步。
 * <p>
 * Building numbers: each type counts from 1, a new building takes the smallest free number of its type and keeps it.
 * Buildings from older saves that have none are numbered in position order. Server side only; numbers are saved and
 * synced with the building.
 */
public final class TownBuildingNumbers {
    private TownBuildingNumbers() {
    }

    /** Gives every unnumbered building the smallest free number of its type; returns how many were numbered. */
    public static int assignMissing(Map<BlockPos, ? extends AbstractTownBuilding> buildings) {
        Map<Class<?>, Set<Integer>> taken = new HashMap<>();
        for (AbstractTownBuilding building : buildings.values())
            if (building != null && building.getNumber() > 0)
                taken.computeIfAbsent(building.getClass(), type -> new HashSet<>()).add(building.getNumber());
        List<Map.Entry<BlockPos, ? extends AbstractTownBuilding>> missing = buildings.entrySet().stream()
                .filter(entry -> entry.getValue() != null && entry.getValue().getNumber() <= 0)
                .sorted(Comparator.comparingLong(entry -> entry.getKey().asLong()))
                .<Map.Entry<BlockPos, ? extends AbstractTownBuilding>>map(entry -> entry)
                .toList();
        for (Map.Entry<BlockPos, ? extends AbstractTownBuilding> entry : missing) {
            Set<Integer> used = taken.computeIfAbsent(entry.getValue().getClass(), type -> new HashSet<>());
            int number = 1;
            while (used.contains(number)) number++;
            used.add(number);
            entry.getValue().setNumber(number);
        }
        return missing.size();
    }
}
