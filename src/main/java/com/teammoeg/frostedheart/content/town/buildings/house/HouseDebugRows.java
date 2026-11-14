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

package com.teammoeg.frostedheart.content.town.buildings.house;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.teammoeg.frostedheart.content.town.buildings.hunting.HuntingBaseBuilding;
import com.teammoeg.frostedheart.content.town.buildings.mine.MineBaseBuilding;
import com.teammoeg.frostedheart.content.town.resident.Resident;
import com.teammoeg.frostedheart.content.town.resident.ResidentNutritionExplanation;
import com.teammoeg.frostedheart.content.town.tabs.TownInfoPanel.Row;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;

import net.minecraft.network.chat.Component;

/**
 * 住宅调试页的两张记录：今天的饭（每样东西各一行，带图标和原始数量）和每位居民的原始数值与明天的预测。玩家看的餐食页和居民页
 * 只出整数和等级词。
 * <p>
 * The house debug sheets: today's meal, one line per item with its icon and raw amount, and each resident's raw figures
 * with tomorrow's forecast. The players' meal and residents tabs show whole numbers and grade words only.
 */
final class HouseDebugRows {
    private static final int TITLE = 0xFFFFAA00, TEXT = 0xFFFFFFFF, MUTED = 0xFFAAAAAA, HEADING = 0xFFFFFF55,
            GOOD = 0xFF55FF55, BAD = 0xFFFF5555;

    private HouseDebugRows() {
    }

    static List<Row> meal(HouseMenu menu) {
        List<Row> rows = new ArrayList<>();
        rows.add(Row.colored(Component.translatable("gui.frostedheart.house.daily_meal"), TITLE));
        HouseBuilding house = menu.getHouse().orElse(null);
        if (house == null) {
            rows.add(Row.colored(Component.translatable("gui.frostedheart.house.unavailable"), MUTED));
            return rows;
        }
        HouseBuilding.DailyReport report = house.getDailyReport();
        HouseBuilding.DailyMeal meal = report.meal();
        if (!meal.hasData()) {
            rows.add(Row.colored(Component.translatable("gui.frostedheart.house.no_meal_report"), MUTED));
            return rows;
        }
        rows.add(Row.colored(Component.translatable("gui.frostedheart.house.meal_settlement_day", meal.settlementDay()), MUTED));
        rows.add(Row.text(Component.translatable("gui.frostedheart.house.meal_food_units",
                decimal(report.foodConsumed()), decimal(report.foodRequired()))));
        if (meal.entries().isEmpty()) {
            rows.add(Row.colored(Component.translatable("gui.frostedheart.house.no_meal_served"), MUTED));
            return rows;
        }
        rows.add(Row.empty());
        for (HouseBuilding.MealEntry entry : meal.entries()) {
            var stack = entry.item().toItemStack();
            rows.add(new Row(stack.getHoverName().copy().append(Component.literal("  ×" + decimal(entry.amount()))), TEXT, stack));
        }
        return rows;
    }

    static List<Row> residents(HouseMenu menu) {
        List<Row> rows = new ArrayList<>();
        List<Resident> residents = menu.getResidents();
        if (residents.isEmpty()) {
            rows.add(Row.colored(Component.translatable("gui.frostedheart.house.no_residents"), MUTED));
            return rows;
        }
        HouseBuilding house = menu.getHouse().orElse(null);
        boolean forecast = house != null && house.getDailyReport().hasData();
        double maxProficiency = FHConfig.SERVER.TOWN.RESIDENT_PROGRESSION.maximumWorkProficiency.get();
        for (Resident resident : residents) {
            if (!rows.isEmpty()) rows.add(Row.empty());
            rows.add(Row.colored(Component.literal(resident.toString()), TITLE));
            rows.add(stat("gui.frostedheart.house.health", resident.getHealth(), 100));
            rows.add(stat("gui.frostedheart.house.mental", resident.getMental(), 100));
            rows.add(stat("gui.frostedheart.house.strength", resident.getStrength(), 100));
            rows.add(stat("gui.frostedheart.house.intelligence", resident.getIntelligence(), 100));
            rows.add(stat("gui.frostedheart.town.nutrition_carbohydrate", resident.getNutrition().carbohydrate(), 100));
            rows.add(stat("gui.frostedheart.town.nutrition_protein", resident.getNutrition().protein(), 100));
            rows.add(stat("gui.frostedheart.town.nutrition_vegetable", resident.getNutrition().vegetable(), 100));
            rows.add(stat("gui.frostedheart.town.nutrition_fat", resident.getNutrition().fat(), 100));
            ResidentNutritionExplanation.lines(resident).forEach(line -> rows.add(Row.colored(line, MUTED)));
            rows.add(Row.colored(Component.translatable("gui.frostedheart.house.proficiency"), HEADING));
            rows.add(stat("gui.frostedheart.house.mining", resident.getWorkProficiency(MineBaseBuilding.class), maxProficiency));
            rows.add(stat("gui.frostedheart.house.hunting", resident.getWorkProficiency(HuntingBaseBuilding.class), maxProficiency));
            if (!forecast) {
                rows.add(Row.colored(Component.translatable("gui.frostedheart.house.no_forecast"), MUTED));
                continue;
            }
            HouseDailyModel.ResidentEffects effects = house.calculateResidentEffects(resident);
            rows.add(Row.colored(Component.translatable("gui.frostedheart.house.next_day_forecast"), HEADING));
            rows.add(delta("gui.frostedheart.house.health", effects.healthDelta()));
            rows.add(delta("gui.frostedheart.house.mental", effects.mentalDelta()));
        }
        return rows;
    }

    private static Row stat(String key, double value, double maximum) {
        return Row.text(Component.translatable(key).append(Component.literal(": " + decimal(value) + " / " + Math.round(maximum))));
    }

    private static Row delta(String key, double delta) {
        int color = delta > 0.0001 ? GOOD : delta < -0.0001 ? BAD : MUTED;
        return Row.colored(Component.translatable(key).append(Component.literal(": " + String.format(Locale.ROOT, "%+.2f", delta))), color);
    }

    /** Up to four decimals, trailing zeros dropped: the raw value a debug sheet is for. */
    private static String decimal(double value) {
        String result = String.format(Locale.ROOT, "%.4f", value);
        while (result.endsWith("0")) result = result.substring(0, result.length() - 1);
        if (result.endsWith(".")) result = result.substring(0, result.length() - 1);
        return result;
    }
}
