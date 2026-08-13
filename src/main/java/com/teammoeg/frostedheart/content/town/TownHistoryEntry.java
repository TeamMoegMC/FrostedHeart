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

package com.teammoeg.frostedheart.content.town;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.frostedheart.content.town.observation.TownSignalEvent;

import java.util.List;

/**
 * 城镇每日快照。每日城镇结算（tickMorning）后记录一条，
 * 用于镇长印章 GUI 中的数据统计折线图。
 * <p>
 * Daily snapshot of a town. Recorded after the daily town settlement
 * (tickMorning), used by the Mayor's Seal GUI to draw statistic charts.
 *
 * @param day 记录时的世界天数 / the world day when recorded
 * @param population 居民数量 / resident count
 * @param avgHealth 居民平均生命 / average resident health (0-100)
 * @param avgMental 居民平均精神 / average resident mental (0-100)
 * @param buildings 城镇建筑数量 / town building count
 */
public record TownHistoryEntry(
        long day,
        int population,
        double avgHealth,
        double avgMental,
        int buildings,
        double p10Health,
        double minHealth,
        double p10Mental,
        double minMental,
        int unableToWorkCount,
        int exitRiskCount,
        boolean towerWorking,
        int climateLevel,
        List<TownSignalEvent> events
) {

    public static final Codec<TownHistoryEntry> CODEC = RecordCodecBuilder.create(t -> t.group(
            Codec.LONG.fieldOf("day").forGetter(TownHistoryEntry::day),
            Codec.INT.fieldOf("population").forGetter(TownHistoryEntry::population),
            Codec.DOUBLE.fieldOf("avgHealth").forGetter(TownHistoryEntry::avgHealth),
            Codec.DOUBLE.fieldOf("avgMental").forGetter(TownHistoryEntry::avgMental),
            Codec.INT.fieldOf("buildings").forGetter(TownHistoryEntry::buildings),
            Codec.DOUBLE.optionalFieldOf("p10Health", 0.0).forGetter(TownHistoryEntry::p10Health),
            Codec.DOUBLE.optionalFieldOf("minHealth", 0.0).forGetter(TownHistoryEntry::minHealth),
            Codec.DOUBLE.optionalFieldOf("p10Mental", 0.0).forGetter(TownHistoryEntry::p10Mental),
            Codec.DOUBLE.optionalFieldOf("minMental", 0.0).forGetter(TownHistoryEntry::minMental),
            Codec.INT.optionalFieldOf("unableToWorkCount", 0).forGetter(TownHistoryEntry::unableToWorkCount),
            Codec.INT.optionalFieldOf("exitRiskCount", 0).forGetter(TownHistoryEntry::exitRiskCount),
            Codec.BOOL.optionalFieldOf("towerWorking", false).forGetter(TownHistoryEntry::towerWorking),
            Codec.INT.optionalFieldOf("climateLevel", 0).forGetter(TownHistoryEntry::climateLevel),
            TownSignalEvent.CODEC.listOf().optionalFieldOf("events", List.of()).forGetter(TownHistoryEntry::events)
    ).apply(t, TownHistoryEntry::new));

    public TownHistoryEntry {
        events = List.copyOf(events);
    }

    /** Source-compatible constructor for callers that only have legacy averages. */
    public TownHistoryEntry(
            long day,
            int population,
            double avgHealth,
            double avgMental,
            int buildings
    ) {
        this(day, population, avgHealth, avgMental, buildings,
                avgHealth, avgHealth, avgMental, avgMental,
                0, 0, false, 0, List.of());
    }
}
