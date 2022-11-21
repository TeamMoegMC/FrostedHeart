/*
 * Copyright (c) 2022 TeamMoeg
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nonnull;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.loot.LootContext;
import net.minecraft.loot.conditions.ILootCondition;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.loot.GlobalLootModifierSerializer;
import net.minecraftforge.common.loot.LootModifier;

public class RemoveLootModifier extends LootModifier {
    List<Ingredient> removed = new ArrayList<>();

    private RemoveLootModifier(ILootCondition[] conditionsIn, Collection<Ingredient> pairsin) {
        super(conditionsIn);
        this.removed.addAll(pairsin);
    }

    @Nonnull
    @Override
    protected List<ItemStack> doApply(List<ItemStack> generatedLoot, LootContext context) {
        generatedLoot.removeIf(this::shouldRemove);
        return generatedLoot;
    }

    private boolean shouldRemove(ItemStack orig) {
    	if(removed.isEmpty())return true;
        for (Ingredient rp : removed) {
            if (rp.test(orig)) {
                return true;
            }
        }
        return false;
    }

    public static class Serializer extends GlobalLootModifierSerializer<RemoveLootModifier> {
        @Override
        public RemoveLootModifier read(ResourceLocation location, JsonObject object, ILootCondition[] conditions) {
            JsonArray ja = object.get("removed").getAsJsonArray();
            List<Ingredient> changes = new ArrayList<>();
            for (JsonElement je : ja) {
                changes.add(Ingredient.deserialize(je));
            }
            return new RemoveLootModifier(conditions, changes);
        }

        @Override
        public JsonObject write(RemoveLootModifier instance) {
            JsonObject object = new JsonObject();
            JsonArray removed = new JsonArray();
            instance.removed.stream().map(Ingredient::serialize).forEach(removed::add);
            object.add("removed", removed);
            return object;
        }
    }
}
