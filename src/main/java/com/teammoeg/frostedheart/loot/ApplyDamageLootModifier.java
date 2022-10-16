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

import com.google.gson.JsonObject;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootContext;
import net.minecraft.loot.RandomValueRange;
import net.minecraft.loot.conditions.ILootCondition;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.loot.GlobalLootModifierSerializer;
import net.minecraftforge.common.loot.LootModifier;

import javax.annotation.Nonnull;
import java.util.List;

public class ApplyDamageLootModifier extends LootModifier {
    RandomValueRange dmg;

    private ApplyDamageLootModifier(ILootCondition[] conditionsIn, RandomValueRange rv) {
        super(conditionsIn);
        dmg = rv;
    }

    @Nonnull
    @Override
    protected List<ItemStack> doApply(List<ItemStack> generatedLoot, LootContext context) {
        generatedLoot.forEach(e -> {
            if (e.getDamage() == 0 && e.isDamageable()) {
                e.setDamage((int) (e.getMaxDamage() * dmg.generateFloat(context.getRandom())));
            }
        });
        return generatedLoot;
    }

    public static class Serializer extends GlobalLootModifierSerializer<ApplyDamageLootModifier> {
        @Override
        public ApplyDamageLootModifier read(ResourceLocation location, JsonObject object, ILootCondition[] conditions) {
            return new ApplyDamageLootModifier(conditions, RandomValueRange.of(object.get("min").getAsFloat(), object.get("max").getAsFloat()));
        }

        @Override
        public JsonObject write(ApplyDamageLootModifier instance) {
            JsonObject object = new JsonObject();
            object.addProperty("min", instance.dmg.getMin());
            object.addProperty("max", instance.dmg.getMax());
            return object;
        }
    }
}
