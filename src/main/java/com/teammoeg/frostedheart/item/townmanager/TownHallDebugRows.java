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

package com.teammoeg.frostedheart.item.townmanager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Nullable;

import com.teammoeg.frostedheart.content.town.TeamTown;
import com.teammoeg.frostedheart.content.town.TeamTownData;
import com.teammoeg.frostedheart.content.town.TownCareLaw;
import com.teammoeg.frostedheart.content.town.TownPolicyState;
import com.teammoeg.frostedheart.content.town.building.AbstractTownBuilding;
import com.teammoeg.frostedheart.content.town.building.ITownResidentWorkBuilding;
import com.teammoeg.frostedheart.content.town.building.ITownTemperatureBuilding;
import com.teammoeg.frostedheart.content.town.buildings.house.HouseBuilding;
import com.teammoeg.frostedheart.content.town.resident.Resident;
import com.teammoeg.frostedheart.content.town.resident.ResidentNutrition;
import com.teammoeg.frostedheart.content.town.tabs.TownInfoPanel.Row;
import com.teammoeg.frostedheart.content.town.ui.TownBuildingLabels;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/**
 * 镇公所调试页上的原始数据：居民、建筑和法令各一张表，保留原始小数，方便排查和复制。玩家看到的各页只出整数和等级词。
 * <p>
 * The town hall's debug sheets: residents, buildings and laws as plain rows with their raw figures, for checking and
 * copying. The players' pages show whole numbers and grade words only.
 */
final class TownHallDebugRows {
    private static final int TITLE = 0xFFFFAA00, GOOD = 0xFF55FF55, BAD = 0xFFFF5555, MUTED = 0xFFAAAAAA;

    private TownHallDebugRows() {
    }

    private static Component d(String key, Object... args) {
        return Component.translatable("gui.frostedheart.board.town.debug." + key, args);
    }

    private static String raw(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static String at(@Nullable BlockPos pos) {
        return pos == null ? "-" : pos.toShortString();
    }

    static List<Row> residents(@Nullable TeamTown town) {
        List<Row> rows = new ArrayList<>();
        if (town == null) {
            rows.add(Row.colored(d("none"), MUTED));
            return rows;
        }
        List<Resident> residents = new ArrayList<>(town.getAllResidents());
        residents.sort(Comparator.comparing(Resident::toString));
        rows.add(Row.colored(d("residents.count", residents.size()), TITLE));
        for (Resident resident : residents) {
            rows.add(Row.empty());
            rows.add(Row.text(Component.literal(resident.toString())));
            rows.add(Row.colored(d("resident.stats", raw(resident.getHealth()), raw(resident.getMental()),
                    raw(resident.getStrength()), raw(resident.getIntelligence()), resident.getEducationLevel()), MUTED));
            ResidentNutrition nutrition = resident.getNutrition();
            rows.add(Row.colored(d("resident.nutrition", raw(nutrition.carbohydrate()), raw(nutrition.protein()),
                    raw(nutrition.vegetable()), raw(nutrition.fat())), MUTED));
            rows.add(Row.colored(d("resident.places", at(resident.getHousePos()), at(resident.getWorkPos()),
                    resident.getAge(), resident.getAgeDays()), MUTED));
        }
        return rows;
    }

    static List<Row> buildings(@Nullable TeamTown town) {
        List<Row> rows = new ArrayList<>();
        if (town == null) {
            rows.add(Row.colored(d("none"), MUTED));
            return rows;
        }
        Map<BlockPos, AbstractTownBuilding> buildings = town.getTownBuildings();
        rows.add(Row.colored(d("buildings.count", buildings.size(), at(town.getTowerPos())), TITLE));
        List<BlockPos> order = new ArrayList<>(buildings.keySet());
        order.sort(Comparator.comparingLong(BlockPos::asLong));
        for (BlockPos pos : order) {
            AbstractTownBuilding building = buildings.get(pos);
            if (building == null) continue;
            rows.add(Row.empty());
            rows.add(Row.colored(d("building.name", TownBuildingLabels.name(buildings, pos, building),
                    TownBuildingLabels.typeName(building), pos.toShortString()), building.isBuildingWorkable() ? GOOD : BAD));
            String direction = TownBuildingLabels.direction(town.getTowerPos(), pos);
            rows.add(Row.colored(d("building.state", flag(building.isInitialized()), flag(building.isStructureValid()),
                    flag(!building.isOccupiedAreaOverlapped()), building.getNumber(), direction == null ? "-" : direction), MUTED));
            String temperature = building instanceof ITownTemperatureBuilding warm ? raw(warm.getEffectiveTemperature()) : "-";
            String people;
            if (building instanceof HouseBuilding house) {
                long living = town.getAllResidents().stream().filter(r -> pos.equals(r.getHousePos())).count();
                people = living + " / " + house.getMaxResidents();
            } else if (building instanceof ITownResidentWorkBuilding work) {
                long working = town.getAllResidents().stream().filter(r -> pos.equals(r.getWorkPos())).count();
                people = working + " / " + work.getMaxResidents();
            } else people = "-";
            rows.add(Row.colored(d("building.room", temperature, people), MUTED));
        }
        return rows;
    }

    static List<Row> laws(@Nullable TeamTownData data) {
        List<Row> rows = new ArrayList<>();
        if (data == null) {
            rows.add(Row.colored(d("none"), MUTED));
            return rows;
        }
        TownPolicyState policy = data.getPolicyState();
        rows.add(Row.colored(d("laws.state", data.getTownDay(), policy.changedAtTownDay(), policy.remainingCooldown(data.getTownDay())), TITLE));
        rows.add(Row.text(d("laws.active", law(policy.careLaw()))));
        rows.add(Row.text(d("laws.pending", policy.hasPendingChanges() ? law(policy.displayedCareLaw()) : Component.literal("-"))));
        rows.add(Row.empty());
        rows.add(Row.colored(d("laws.selections", policy.selections().toString()), MUTED));
        rows.add(Row.colored(d("laws.pending_raw", policy.pending().toString()), MUTED));
        return rows;
    }

    private static Component law(TownCareLaw law) {
        return Component.translatable("gui.frostedheart.town_manager.policy." + law.id());
    }

    private static Component flag(boolean value) {
        return d(value ? "yes" : "no");
    }

    /** All the rows as text, one line each, for the copy button. */
    static String text(List<Row> rows) {
        StringBuilder out = new StringBuilder();
        for (Row row : rows) out.append(row.text().getString()).append('\n');
        return out.toString();
    }
}
