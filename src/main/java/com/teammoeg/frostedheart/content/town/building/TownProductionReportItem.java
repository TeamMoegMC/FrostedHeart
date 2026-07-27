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
 */

package com.teammoeg.frostedheart.content.town.building;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

/**
 * One item row in a persisted daily production report.
 *
 * @param item item type produced by the building
 * @param produced amount produced before warehouse handling
 * @param stored amount accepted by town storage
 */
public record TownProductionReportItem(Item item, double produced, double stored) {
    public static final Codec<TownProductionReportItem> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(TownProductionReportItem::item),
            Codec.DOUBLE.optionalFieldOf("produced", 0.0).forGetter(TownProductionReportItem::produced),
            Codec.DOUBLE.optionalFieldOf("stored", 0.0).forGetter(TownProductionReportItem::stored)
    ).apply(instance, TownProductionReportItem::new));

    public TownProductionReportItem {
        produced = sanitize(produced);
        stored = Math.min(produced, sanitize(stored));
    }

    public double lost() {
        return Math.max(0.0, produced - stored);
    }

    private static double sanitize(double value) {
        return Double.isFinite(value) ? Math.max(0.0, value) : 0.0;
    }
}
