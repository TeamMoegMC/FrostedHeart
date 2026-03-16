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

package com.teammoeg.frostedheart.content.town.resource;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import groovyjarjarantlr4.v4.runtime.misc.Nullable;
import lombok.Getter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Objects;

/**
 * Wrapper for ItemStack, added special hashCode and equals method, for saving ItemStack in HashMap.
 * The count of ItemStack will be changed to 1 when creating this wrapper. Because TownResourceHolder used other things to save the amount of items.
 */
@Getter
public class ItemStackResourceKey implements ITownResourceKey {
    private final Item item;
    @Nullable
    private final CompoundTag tag;
    private final int hashCode;

    public static final Codec<ItemStackResourceKey> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BuiltInRegistries.ITEM.byNameCodec()
                    .fieldOf("item")
                    .forGetter(ItemStackResourceKey::getItem),
            CompoundTag.CODEC
                    .optionalFieldOf("tag", null)
                    .forGetter(ItemStackResourceKey::getCompoundTag)
    ).apply(instance, ItemStackResourceKey::new));

    //三种构造函数
    public ItemStackResourceKey(Item item, @Nullable CompoundTag tag) {
        this.item = item;
        this.tag = tag != null ? tag.copy() : null;
        this.hashCode = computeHash();
    }

    public ItemStackResourceKey(ItemStack stack) {
        this.item = stack.getItem();
        this.tag = stack.getTag() != null ? stack.getTag().copy() : null;
        this.hashCode = computeHash();
    }

    public ItemStackResourceKey(Item item) {
        this.item = item;
        this.tag = null;
        this.hashCode = computeHash();
    }

    public CompoundTag getCompoundTag() {
        return tag;
    }

    public Item getItem() {
        return item;
    }


    public ItemStack toItemStack() {
        ItemStack stack = new ItemStack(item, 1);
        if (tag != null) {
            stack.setTag(tag.copy());
        }
        return stack;
    }

    public boolean matches(ItemStack stack) {
        if (stack.getItem() != this.item) return false;
        return Objects.equals(stack.getTag(), this.tag);
    }



    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ItemStackResourceKey other)) return false;
        return this.item == other.item
                && Objects.equals(this.tag, other.tag);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    private int computeHash() {
        int h = item.hashCode();
        if (tag != null) {
            h = 31 * h + tag.hashCode();
        }
        return h;
    }

    public boolean isEmpty() {
        return item == Items.AIR;
    }


    @Override
    public String toString() {
        return "{Item Resource: " + item + (tag != null ? " " + tag : "") + '}';
    }
}
