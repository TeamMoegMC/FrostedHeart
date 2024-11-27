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

package com.teammoeg.frostedheart.foundation.loot;

import org.jetbrains.annotations.NotNull;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;


public class ApplyDamageLootModifier extends LootModifier {
	public static final Codec<ApplyDamageLootModifier> CODEC=RecordCodecBuilder.<ApplyDamageLootModifier>create(t->codecStart(t).and(
		t.group(Codec.INT.fieldOf("max").forGetter(o->o.max),
		Codec.INT.fieldOf("min").forGetter(o->o.min))
		).apply(t,ApplyDamageLootModifier::new));

    int min;
    int max;

    private ApplyDamageLootModifier(LootItemCondition[] conditionsIn, int max, int min) {
        super(conditionsIn);
        this.min=min;
        this.max=max;
    }

 

	@Override
	public Codec<? extends IGlobalLootModifier> codec() {
		return CODEC;
	}

	@Override
	protected @NotNull ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
		generatedLoot.forEach(e -> {
            if (e.getDamageValue() == 0 && e.isDamageableItem()) {
                e.setDamageValue((int) (e.getMaxDamage() * context.getRandom().nextIntBetweenInclusive(min, max)));
            }
        });
        return generatedLoot;
	}
}
