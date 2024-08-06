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

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;

public class AddLootModifier extends LootModifier {

	public static final Codec<AddLootModifier> CODEC = 
		RecordCodecBuilder.create(inst -> codecStart(inst)
	.and(ResourceLocation.CODEC.fieldOf("loot_table").forGetter(lm->lm.lt)).apply(inst, AddLootModifier::new));
    ResourceLocation lt;

    boolean isAdding;

    private AddLootModifier(LootItemCondition[] conditionsIn, ResourceLocation lt) {
        super(conditionsIn);
        this.lt = lt;
    }


    @Nonnull
    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        LootTable loot = context.getResolver().getLootTable(lt);
        //if(context.addLootTable(loot)) {
        if (!isAdding) {
            try {
                isAdding = true;
               loot.getRandomItemsRaw(new LootContext.Builder(context).withQueriedLootTableId(lt).create(null),generatedLoot::add);
            } finally {
                isAdding = false;
            }
        }
        //}
        return generatedLoot;
    }


	@Override
	public Codec<? extends IGlobalLootModifier> codec() {
		// TODO Auto-generated method stub
		return CODEC;
	}
}
