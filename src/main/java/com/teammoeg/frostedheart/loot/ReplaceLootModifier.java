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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nonnull;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.frostedheart.util.io.CodecUtil;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import net.minecraftforge.registries.ForgeRegistries;

public class ReplaceLootModifier extends LootModifier {
	public static final Codec<ReplaceLootModifier> CODEC=RecordCodecBuilder.create(t->codecStart(t).and(
		Codec.list(CodecUtil.pairCodec("from",CodecUtil.INGREDIENT_CODEC,"to", ForgeRegistries.ITEMS.getCodec())).fieldOf("changes").forGetter(o->o.remap)
		).apply(t,ReplaceLootModifier::new));
	


    List<Pair<Ingredient,Item>> remap = new ArrayList<>();

    private ReplaceLootModifier(LootItemCondition[] conditionsIn, Collection<Pair<Ingredient,Item>> pairsin) {
        super(conditionsIn);
        this.remap.addAll(pairsin);
    }

    @Nonnull
    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        generatedLoot.replaceAll(this::doReplace);
        return generatedLoot;
    }

    private ItemStack doReplace(ItemStack orig) {
        for (Pair<Ingredient,Item> rp : remap) {
            if (rp.getFirst().test(orig)) {
                return new ItemStack(rp.getSecond(), orig.getCount());
            }
        }
        return orig;
    }

	@Override
	public Codec<? extends IGlobalLootModifier> codec() {
		// TODO Auto-generated method stub
		return CODEC;
	}
}
