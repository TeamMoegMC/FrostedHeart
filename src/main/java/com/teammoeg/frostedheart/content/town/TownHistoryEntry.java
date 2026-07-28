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
public record TownHistoryEntry(long day, int population, double avgHealth, double avgMental, int buildings) {

    public static final Codec<TownHistoryEntry> CODEC = RecordCodecBuilder.create(t -> t.group(
            Codec.LONG.fieldOf("day").forGetter(TownHistoryEntry::day),
            Codec.INT.fieldOf("population").forGetter(TownHistoryEntry::population),
            Codec.DOUBLE.fieldOf("avgHealth").forGetter(TownHistoryEntry::avgHealth),
            Codec.DOUBLE.fieldOf("avgMental").forGetter(TownHistoryEntry::avgMental),
            Codec.INT.fieldOf("buildings").forGetter(TownHistoryEntry::buildings)
    ).apply(t, TownHistoryEntry::new));
}
