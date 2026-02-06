/*
 * Copyright (c) 2026 TeamMoeg
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

package com.teammoeg.frostedheart.util;

import java.util.HashMap;
import java.util.Map;

import com.teammoeg.frostedheart.bootstrap.reference.FHTags;

import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
/**
 * To add custom tag in animal food in mixin
 * Add new type if you want to customize feeding item for an entity type
 * */
public class BreedingHelper {
	static Map<EntityType<?>, TagKey<Item>> tag = new HashMap<>();
	static void register(EntityType type,FHTags.Items item) {
		tag.put(type, item.tag);
	}
	static {
		register(EntityType.COW, FHTags.Items.COW_FEED);
		register(EntityType.CHICKEN, FHTags.Items.CHICKEN_FEED);
		register(EntityType.PIG, FHTags.Items.PIG_FEED);
		register(EntityType.CAT, FHTags.Items.CAT_FEED);
		register(EntityType.OCELOT, FHTags.Items.CAT_FEED);
		register(EntityType.RABBIT, FHTags.Items.RABBIT_FEED);
	}

	public static boolean isBreedingItem(EntityType<?> type, ItemStack itemStack) {
		TagKey<Item> t = tag.get(type);
		if (t != null)
			return itemStack.is(t);
		return false;
	}
}
