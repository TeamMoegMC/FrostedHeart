/*
 * Copyright (c) 2024 TeamMoeg
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

import com.cannolicatfish.rankine.init.RankineTags;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.Tags.IOptionalNamedTag;

import java.util.HashMap;
import java.util.Map;

public class BreedUtil {
    static Map<EntityType<?>, IOptionalNamedTag<Item>> tag = new HashMap<>();

    static {
        tag.put(EntityType.PIG, RankineTags.Items.BREEDABLES_PIG);
        tag.put(EntityType.COW, RankineTags.Items.BREEDABLES_COW);
        tag.put(EntityType.MOOSHROOM, RankineTags.Items.BREEDABLES_COW);
        tag.put(EntityType.SHEEP, RankineTags.Items.BREEDABLES_SHEEP);
        tag.put(EntityType.LLAMA, RankineTags.Items.BREEDABLES_LLAMA);
        tag.put(EntityType.CHICKEN, RankineTags.Items.BREEDABLES_CHICKEN);

        tag.put(EntityType.FOX, RankineTags.Items.BREEDABLES_FOX);
        tag.put(EntityType.RABBIT, RankineTags.Items.BREEDABLES_RABBIT);
        tag.put(EntityType.CAT, RankineTags.Items.BREEDABLES_CAT);
        tag.put(EntityType.HORSE, RankineTags.Items.BREEDABLES_HORSE);
        tag.put(EntityType.DONKEY, RankineTags.Items.BREEDABLES_HORSE);
    }

    public static boolean isBreedingItem(EntityType<?> type, ItemStack itemStack) {
        IOptionalNamedTag<Item> t = tag.get(type);
        if (t != null)
            return itemStack.getItem().isIn(t);
        return false;
    }
}
