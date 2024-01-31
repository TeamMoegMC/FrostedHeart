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

import java.util.List;

import javax.annotation.Nonnull;

import com.google.gson.JsonObject;

import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootContext;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.conditions.ILootCondition;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.loot.GlobalLootModifierSerializer;
import net.minecraftforge.common.loot.LootModifier;

public class AddLootModifier extends LootModifier {


    public static class Serializer extends GlobalLootModifierSerializer<AddLootModifier> {
        @Override
        public AddLootModifier read(ResourceLocation location, JsonObject object, ILootCondition[] conditions) {
            return new AddLootModifier(conditions, new ResourceLocation(object.get("loot_table").getAsString()));
        }

        @Override
        public JsonObject write(AddLootModifier instance) {
            JsonObject object = new JsonObject();
            object.addProperty("loot_table", instance.lt.toString());
            return object;
        }
    }
    ResourceLocation lt;

    boolean isAdding;

    private AddLootModifier(ILootCondition[] conditionsIn, ResourceLocation lt) {
        super(conditionsIn);
        this.lt = lt;
    }


    @Nonnull
    @Override
    protected List<ItemStack> doApply(List<ItemStack> generatedLoot, LootContext context) {
        LootTable loot = context.getLootTable(lt);
        //if(context.addLootTable(loot)) {
        if (!isAdding) {
            try {
                isAdding = true;
                List<ItemStack> nl = loot.generate(context);
                generatedLoot.addAll(nl);
            } finally {
                isAdding = false;
            }
        }
        //}
        return generatedLoot;
    }
}
