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

package com.teammoeg.frostedheart.content.town.ui;

import javax.annotation.Nullable;

import java.util.Comparator;
import java.util.Map;

import com.teammoeg.frostedheart.content.town.building.AbstractTownBuilding;
import com.teammoeg.frostedheart.content.town.building.ITownBuilding;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/**
 * 玩家视图里建筑的可读名字：「类型 + 编号」，代替裸坐标。编号按同类建筑的位置排序得出。
 * <p>
 * Readable building names for the player view, "type + number", used instead of raw coordinates. Numbers follow
 * the position order among buildings of the same type.
 */
public final class TownBuildingLabels {
    private TownBuildingLabels() {
    }

    public static Component typeName(ITownBuilding building) {
        return Component.translatable("gui.frostedheart.town_manager.building." + building.getClass().getSimpleName());
    }

    /** The one-character document mark of a building type, e.g. 研 for the institute. */
    public static Component docMark(ITownBuilding building) {
        return Component.translatable(TownFormat.KEY + "doc_mark." + building.getClass().getSimpleName());
    }

    /** The building's kept number; before the town has given one, its place among its type in position order. */
    public static int number(Map<BlockPos, ? extends ITownBuilding> buildings, BlockPos pos, ITownBuilding building) {
        if (building instanceof AbstractTownBuilding kept && kept.getNumber() > 0) return kept.getNumber();
        Class<?> type = building.getClass();
        return 1 + (int) buildings.entrySet().stream()
                .filter(entry -> entry.getValue() != null && entry.getValue().getClass() == type)
                .map(Map.Entry::getKey)
                .sorted(Comparator.comparingLong(BlockPos::asLong))
                .takeWhile(other -> !other.equals(pos))
                .count();
    }

    /** The players' name for the building, or its type and number. */
    public static Component name(ITownBuilding building, int number) {
        if (building instanceof AbstractTownBuilding named && named.getCustomName() != null) return Component.literal(named.getCustomName());
        return Component.translatable(TownFormat.KEY + "building.numbered", typeName(building), number);
    }

    /** The type and number, whatever the players named it; what a blank name falls back to. */
    public static Component defaultName(Map<BlockPos, ? extends ITownBuilding> buildings, BlockPos pos, ITownBuilding building) {
        return Component.translatable(TownFormat.KEY + "building.numbered", typeName(building), number(buildings, pos, building));
    }

    public static Component name(Map<BlockPos, ? extends ITownBuilding> buildings, BlockPos pos, ITownBuilding building) {
        return name(building, number(buildings, pos, building));
    }

    /** Within this many blocks of the tower a building counts as central. */
    private static final int CENTRAL = 16;

    /**
     * 建筑在镇上的方位：以能量塔为镇中心，16 格以内算镇中心，再往外分八个方向。城镇还没有能量塔时不标方位。
     * Where a building stands in town: the energy tower is the centre; within 16 blocks counts as central, beyond that
     * eight directions. A town without a tower gives no direction.
     */
    @Nullable
    public static String direction(@Nullable BlockPos tower, BlockPos pos) {
        if (tower == null) return null;
        double dx = pos.getX() - tower.getX(), dz = pos.getZ() - tower.getZ();
        if (dx * dx + dz * dz < CENTRAL * CENTRAL) return "center";
        // Minecraft's +z is south; split the circle into eight 45° sectors starting at east.
        double angle = Math.toDegrees(Math.atan2(dz, dx));
        int sector = (int) Math.floorMod(Math.round(angle / 45.0), 8);
        return new String[]{"east", "southeast", "south", "southwest", "west", "northwest", "north", "northeast"}[sector];
    }

    /**
     * 列表里的第二行：建筑在镇上的方位；玩家给它起了名字时，再写上它是什么建筑。
     * The second line in lists: where the building stands, and what it is once players have given it a name.
     */
    public static Component subtitle(ITownBuilding building, @Nullable BlockPos tower, BlockPos pos) {
        Component direction = directionName(tower, pos);
        if (!(building instanceof AbstractTownBuilding named && named.getCustomName() != null)) return direction;
        if (direction.getString().isEmpty()) return typeName(building);
        return Component.translatable(TownFormat.KEY + "type_where", typeName(building), direction);
    }

    /** The direction in words, or empty when the town has no tower. */
    public static Component directionName(@Nullable BlockPos tower, BlockPos pos) {
        String direction = direction(tower, pos);
        return direction == null ? Component.empty() : Component.translatable(TownFormat.KEY + "dir." + direction);
    }
}
