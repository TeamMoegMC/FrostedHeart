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

package com.teammoeg.frostedheart.content.loot;

import javax.annotation.Nonnull;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;

import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class TreasureLootCondition implements LootItemCondition {
    public static class Serializer implements net.minecraft.world.level.storage.loot.Serializer<TreasureLootCondition> {

        @Nonnull
        @Override
        public TreasureLootCondition deserialize(JsonObject jsonObject, JsonDeserializationContext context) {
            return new TreasureLootCondition();
        }

        @Override
        public void serialize(JsonObject jsonObject, TreasureLootCondition matchTagCondition, JsonSerializationContext serializationContext) {
        }
    }

    public static RegistryObject<LootItemConditionType> TYPE;

    public TreasureLootCondition() {
    }

    @Override
    public LootItemConditionType getType() {
        return TYPE.get();
    }

    @Override
    public boolean test(LootContext t) {
        return t.getResolver().getLootTable(t.getQueriedLootTableId()).getParamSet() == LootContextParamSets.CHEST;
    }
}
