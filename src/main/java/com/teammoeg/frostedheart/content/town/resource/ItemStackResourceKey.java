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

package com.teammoeg.frostedheart.content.town.resource;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

/**
 * Wrapper for ItemStack, added special hashCode and equals method, for saving ItemStack in HashMap.
 * The count of ItemStack will be changed to 1 when creating this wrapper. Because TownResourceHolder used other things to save the amount of items.
 */
@Getter
public class ItemStackResourceKey implements ITownResourceKey {
    public ItemStack itemStack;

    public static final Codec<ItemStackResourceKey> CODEC = RecordCodecBuilder.create(t -> t.group(
                    ItemStack.CODEC
                            .xmap(
                                    // 反序列化时强制 count=1
                                    stack -> stack.copyWithCount(1),
                                    // 序列化时也保持 count=1
                                    stack -> stack.copyWithCount(1)
                            ).fieldOf("itemStack").forGetter(o -> o.itemStack)
            ).apply(t, ItemStackResourceKey::new)
    );

    public ItemStackResourceKey(ItemStack itemStack) {
        this.itemStack = itemStack.copyWithCount(1);
    }

    public ItemStackResourceKey(Item item) {
        this.itemStack = new ItemStack(item, 1);
    }

    public boolean equals(Object o) {
        ItemStack itemStack2;
        if (o instanceof ItemStackResourceKey) {
            itemStack2 = ((ItemStackResourceKey) o).getItemStack();
        } else return false;
        return ItemStack.isSameItemSameTags(itemStack, itemStack2);
    }

    public int hashCode() {
        int itemHash = itemStack.getItem().hashCode();
        int tagHash = itemStack.getTag() == null ? 0 : itemStack.getTag().hashCode();
        return Objects.hash(itemHash, tagHash);
    }

    @Override
    public String toString() {
        return "{Item Resource: " +
                itemStack +
                '}';
    }
}
