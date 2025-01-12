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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import net.minecraftforge.registries.ForgeRegistries;

public class DechantLootModifier extends LootModifier {
	public static final Codec<DechantLootModifier> CODEC= RecordCodecBuilder.create(t->codecStart(t).and(
		Codec.list(ForgeRegistries.ENCHANTMENTS.getCodec()).fieldOf("removed").forGetter(o->o.removed)
		).apply(t, DechantLootModifier::new));


    List<Enchantment> removed = new ArrayList<>();

    private DechantLootModifier(LootItemCondition[] conditionsIn, Collection<Enchantment> pairsin) {
        super(conditionsIn);
        removed.addAll(pairsin);
    }

    @Nonnull
    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        generatedLoot.replaceAll(e -> doRemove(e, context));
        return generatedLoot;
    }

    private ItemStack doRemove(ItemStack orig, LootContext context) {
        Map<Enchantment, Integer> enchs = EnchantmentHelper.getEnchantments(orig);
        int origsize = enchs.size();
        enchs.keySet().removeIf(removed::contains);
        if (origsize == enchs.size())
            return orig;
        if (orig.getItem() == Items.ENCHANTED_BOOK) {
            if (enchs.isEmpty())
                return EnchantmentHelper.enchantItem(context.getRandom(), new ItemStack(Items.BOOK), 1, false);
            orig = new ItemStack(Items.ENCHANTED_BOOK);
            EnchantmentHelper.setEnchantments(enchs, orig);
            return orig;
        }
        EnchantmentHelper.setEnchantments(enchs, orig);
        return orig;
    }

	@Override
	public Codec<? extends IGlobalLootModifier> codec() {
		return CODEC;
	}
}
