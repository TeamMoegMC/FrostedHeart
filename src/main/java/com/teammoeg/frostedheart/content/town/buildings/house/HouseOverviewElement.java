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

import com.teammoeg.frostedheart.content.town.tabs.BuildingInfoElement;
import com.teammoeg.frostedheart.content.town.TownMathFunctions;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class HouseOverviewElement extends BuildingInfoElement {

    HouseOverviewElement(
            com.teammoeg.chorda.client.cui.base.UIElement parent,
            int x,
            int y,
            HouseMenu menu
    ) {
        super(parent, x, y, 160, 130, () -> collectLines(menu));
    }

    private static List<Component> collectLines(HouseMenu menu) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("gui.frostedheart.house.overview")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        lines.add(BuildingInfoElement.separator());

        HouseBuilding house = menu.getHouse().orElse(null);
        if (house == null) {
            lines.add(Component.translatable("gui.frostedheart.house.unavailable")
                    .withStyle(ChatFormatting.RED));
            return lines;
        }

        lines.add(localizedStatus("gui.frostedheart.house.workable", house.isBuildingWorkable()));
        if (!house.isBuildingWorkable()) {
            appendWorkabilityFailures(lines, house);
        }
        lines.add(value(
                "gui.frostedheart.house.residents",
                menu.getResidents().size() + " / " + house.getMaxResidents()));
        lines.add(BuildingInfoElement.separator());

        HouseBuilding.DailyReport report = house.getDailyReport();
        if (!report.hasData()) {
            lines.add(Component.translatable("gui.frostedheart.house.no_daily_report")
                    .withStyle(ChatFormatting.GRAY));
            return lines;
        }

        lines.add(Component.translatable("gui.frostedheart.house.last_settlement")
                .withStyle(ChatFormatting.YELLOW));
        lines.add(value(
                "gui.frostedheart.house.food_consumption",
                decimal(report.foodConsumed()) + " / " + decimal(report.foodRequired())));
        lines.add(percent("gui.frostedheart.house.food_satisfaction", report.foodSatisfaction()));
        lines.add(percent("gui.frostedheart.house.nutrition_quality", report.nutritionQuality()));
        lines.add(temperatureValue(report.effectiveTemperature()));
        lines.add(percent("gui.frostedheart.house.temperature_rating", report.temperatureRating()));
        lines.add(percent("gui.frostedheart.house.space_rating", report.spaceRating()));
        lines.add(percent("gui.frostedheart.house.decoration_rating", report.decorationRating()));
        lines.add(percent("gui.frostedheart.house.comfort_rating", report.comfortRating()));
        return lines;
    }

    private static void appendWorkabilityFailures(
            List<Component> lines,
            HouseBuilding house
    ) {
        if (!house.isInitialized()) {
            lines.add(failure("gui.frostedheart.house.failure.not_initialized"));
        }
        if (house.isOccupiedAreaOverlapped()) {
            lines.add(failure("gui.frostedheart.house.failure.area_overlapped"));
        }
        if (!house.isStructureValid()) {
            lines.add(failure("gui.frostedheart.house.failure.invalid_structure"));
        }
        if (house.getArea() < 4) {
            lines.add(failure(
                    "gui.frostedheart.house.failure.area_too_small",
                    house.getArea(),
                    4));
        }
        if (house.getVolume() < 8) {
            lines.add(failure(
                    "gui.frostedheart.house.failure.volume_too_small",
                    house.getVolume(),
                    8));
        }
        if (!house.isTemperatureValid()) {
            String key = house.getEffectiveTemperature() < TownMathFunctions.MIN_TEMP_HOUSE
                    ? "gui.frostedheart.house.failure.temperature_too_low"
                    : "gui.frostedheart.house.failure.temperature_too_high";
            lines.add(failure(
                    key,
                    decimal(house.getEffectiveTemperature()),
                    TownMathFunctions.MIN_TEMP_HOUSE,
                    TownMathFunctions.MAX_TEMP_HOUSE));
        }
    }

    private static Component failure(String key, Object... arguments) {
        return Component.translatable(key, arguments).withStyle(ChatFormatting.RED);
    }

    private static Component value(String key, Object value) {
        return Component.translatable(key)
                .withStyle(ChatFormatting.WHITE)
                .append(Component.literal(": " + value).withStyle(ChatFormatting.AQUA));
    }

    private static Component percent(String key, double value) {
        return value(key, decimal(value * 100.0) + "%");
    }

    private static Component temperatureValue(double temperature) {
        String statusKey;
        ChatFormatting color;
        double temperatureRating = TownMathFunctions.calculateTemperatureRating(temperature);
        if (temperatureRating >= 0.5) {
            statusKey = "gui.frostedheart.house.temperature_comfortable";
            color = ChatFormatting.GREEN;
        } else if (temperature < TownMathFunctions.COMFORTABLE_TEMP_HOUSE) {
            statusKey = "gui.frostedheart.house.temperature_too_low";
            color = ChatFormatting.RED;
        } else {
            statusKey = "gui.frostedheart.house.temperature_too_high";
            color = ChatFormatting.RED;
        }
        return Component.translatable("gui.frostedheart.house.effective_temperature")
                .withStyle(ChatFormatting.WHITE)
                .append(Component.literal(": " + decimal(temperature) + " °C (")
                        .withStyle(ChatFormatting.AQUA))
                .append(Component.translatable(statusKey).withStyle(color))
                .append(Component.literal(")").withStyle(ChatFormatting.AQUA));
    }

    private static Component localizedStatus(String key, boolean value) {
        return Component.translatable(key)
                .withStyle(ChatFormatting.WHITE)
                .append(Component.literal(": "))
                .append(Component.translatable(value
                                ? "gui.frostedheart.house.yes"
                                : "gui.frostedheart.house.no")
                        .withStyle(value ? ChatFormatting.GREEN : ChatFormatting.RED));
    }

    private static String decimal(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }
}
