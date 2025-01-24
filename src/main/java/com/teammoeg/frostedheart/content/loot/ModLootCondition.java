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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nonnull;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.teammoeg.chorda.io.SerializeUtil;

import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import net.minecraftforge.registries.RegistryObject;

public class ModLootCondition implements LootItemCondition {
    public static class Serializer implements net.minecraft.world.level.storage.loot.Serializer<ModLootCondition> {

        @Nonnull
        @Override
        public ModLootCondition deserialize(JsonObject jsonObject, JsonDeserializationContext context) {
            if (jsonObject.has("mod")) {
                return new ModLootCondition(jsonObject.get("mod").getAsString());
            }
            return new ModLootCondition(SerializeUtil.parseJsonElmList(jsonObject.get("mods"), JsonElement::getAsString));
        }

        @Override
        public void serialize(JsonObject jsonObject, ModLootCondition cond, JsonSerializationContext serializationContext) {
            if (cond.mods.size() == 1)
                jsonObject.addProperty("mod", cond.mods.iterator().next());
            else
                jsonObject.add("mods", SerializeUtil.toJsonList(cond.mods, JsonPrimitive::new));
        }
    }
    public static RegistryObject<LootItemConditionType> TYPE;

    private Set<String> mods = new HashSet<>();

    public ModLootCondition(Collection<String> mods) {
        mods.addAll(mods);

    }

    public ModLootCondition(String mod) {
        mods.add(mod);

    }

    @Override
    public LootItemConditionType getType() {
        return TYPE.get();
    }

    @Override
    public boolean test(LootContext t) {
        return mods.contains(t.getQueriedLootTableId().getNamespace());
    }
}
