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

package com.teammoeg.frostedheart.content.town.buildings.warehouse;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.frostedheart.content.town.resource.ItemStackResourceKey;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

public record SimpleItemKey(Item item, @Nullable CompoundTag tag)
{
    public static final Codec<SimpleItemKey> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(SimpleItemKey::item),
            CompoundTag.CODEC.optionalFieldOf("tag").forGetter(key -> Optional.ofNullable(key.tag()))
    ).apply(instance, (item, tag) -> new SimpleItemKey(item, tag.orElse(null))));

    public SimpleItemKey {
        Objects.requireNonNull(item);
        tag = tag != null ? tag.copy() : null;
    }

    public static SimpleItemKey from(ItemStack stack) {
        return new SimpleItemKey(stack.getItem(), stack.getTag());
    }

    public static SimpleItemKey from(ItemStackResourceKey stackResourceKey) {
        return new SimpleItemKey(stackResourceKey.getItem(), stackResourceKey.toItemStack().getTag());
    }

    public static SimpleItemKey from(VirtualItemStack vStack) {
        return from(vStack.getDisplayStack());
    }

    public ItemStack toStack(int count) {
        ItemStack s = new ItemStack(item, count);
        s.setTag(tag != null ? tag.copy() : null);
        return s;
    }

    public void writeTo(FriendlyByteBuf buf) {
        buf.writeId(BuiltInRegistries.ITEM, item);
        buf.writeNbt(tag);
    }

    public static SimpleItemKey fromBuffer(FriendlyByteBuf buf) {
        return new SimpleItemKey(
            buf.readById(BuiltInRegistries.ITEM),
            buf.readNbt()
        );
    }

    public boolean matches(ItemStack stack) {
        return stack.getItem() == item && Objects.equals(stack.getTag(), tag);
    }

}
