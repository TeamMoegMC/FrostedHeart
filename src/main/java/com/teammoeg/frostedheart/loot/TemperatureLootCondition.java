/*
 * Copyright (c) 2022-2024 TeamMoeg
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

package com.teammoeg.frostedheart.loot;

import java.util.function.BiPredicate;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.teammoeg.frostedheart.content.climate.heatdevice.chunkheatdata.ChunkHeatData;

import net.minecraft.loot.ILootSerializer;
import net.minecraft.loot.LootConditionType;
import net.minecraft.loot.LootContext;
import net.minecraft.loot.LootParameters;
import net.minecraft.loot.conditions.ILootCondition;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

public class TemperatureLootCondition implements ILootCondition {
    private enum Comp {
        lt((a, b) -> a < b),
        le((a, b) -> a <= b),
        eq((a, b) -> a == b),
        ne((a, b) -> a != b),
        ge((a, b) -> a >= b),
        gt((a, b) -> a > b);
        final BiPredicate<Float, Float> comp;

        Comp(BiPredicate<Float, Float> comp) {
            this.comp = comp;
        }

        private boolean test(float f1, float f2) {
            return comp.test(f1, f2);
        }
    }
    public static class Serializer implements ILootSerializer<TemperatureLootCondition> {

        @Override
        public TemperatureLootCondition deserialize(JsonObject jo, JsonDeserializationContext jdc) {

            return new TemperatureLootCondition(jo.get("temp").getAsFloat(), Comp.valueOf(jo.get("compare").getAsString()));
        }

        @Override
        public void serialize(JsonObject jo, TemperatureLootCondition ot,
                              JsonSerializationContext p_230424_3_) {
            jo.addProperty("temp", ot.temp);
            jo.addProperty("compare", ot.comparator.name());
        }
    }
    public static LootConditionType TYPE;

    private float temp;

    private Comp comparator;

    public TemperatureLootCondition(float temp, Comp comparator) {
        this.temp = temp;
        this.comparator = comparator;
    }

    @Override
    public LootConditionType getConditionType() {
        return TYPE;
    }

    @Override
    public boolean test(LootContext t) {
        if (t.has(LootParameters.ORIGIN)) {
            Vector3d v = t.get(LootParameters.ORIGIN);
            BlockPos bp = new BlockPos(v.x, v.y, v.z);
            World w = t.getWorld();
            return comparator.test(ChunkHeatData.getTemperature(w, bp), temp);
        }
        return false;
    }
}
