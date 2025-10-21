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

package com.teammoeg.frostedheart.content.loot;

import java.util.function.BiPredicate;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.teammoeg.chorda.util.CUtils;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;

import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.world.level.Level;

public class TemperatureLootCondition implements LootItemCondition {
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
    public static class Serializer implements net.minecraft.world.level.storage.loot.Serializer<TemperatureLootCondition> {

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
    public static RegistryObject<LootItemConditionType> TYPE;

    private float temp;

    private Comp comparator;

    public TemperatureLootCondition(float temp, Comp comparator) {
        this.temp = temp;
        this.comparator = comparator;
    }

    @Override
    public LootItemConditionType getType() {
        return TYPE.get();
    }

    @Override
    public boolean test(LootContext t) {
        if (t.hasParam(LootContextParams.ORIGIN)) {
            Vec3 v = t.getParamOrNull(LootContextParams.ORIGIN);
            BlockPos bp = CUtils.vec2Pos(v);
            Level w = t.getLevel();
            return comparator.test(WorldTemperature.block(w, bp), temp);
        }
        return false;
    }
}
