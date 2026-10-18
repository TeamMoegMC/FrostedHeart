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

/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge.definition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Optional;

/**
 * 实物检索可复制的附加数据。数量不属于物品附加 NBT，永远不由此复制。
 */
public record ItemRetrievalRule(Optional<ResourceLocation> item, Optional<ResourceLocation> tag, List<String> nbtKeys) {
    public static final Codec<ItemRetrievalRule> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceLocation.CODEC.optionalFieldOf("item").forGetter(ItemRetrievalRule::item),
            ResourceLocation.CODEC.optionalFieldOf("tag").forGetter(ItemRetrievalRule::tag),
            Codec.STRING.listOf().optionalFieldOf("nbt_keys", List.of()).forGetter(ItemRetrievalRule::nbtKeys)
    ).apply(i, ItemRetrievalRule::new));

    public ItemRetrievalRule {
        nbtKeys = List.copyOf(nbtKeys);
    }

    public boolean matches(ItemStack stack) {
        return !stack.isEmpty() && item.map(id -> id.equals(ForgeRegistries.ITEMS.getKey(stack.getItem()))).orElse(true)
                && tag.map(id -> stack.is(TagKey.create(Registries.ITEM, id))).orElse(true);
    }

    public CompoundTag copyAllowedData(ItemStack stack) {
        CompoundTag result = new CompoundTag();
        if (matches(stack) && stack.hasTag()) for (String key : nbtKeys) {
            var value = stack.getTag().get(key);
            if (value != null) result.put(key, value.copy());
        }
        return result;
    }
}
