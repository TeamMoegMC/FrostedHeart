package com.teammoeg.frostedheart.content.town.buildings.warehouse;

import com.teammoeg.frostedheart.content.town.resource.ItemStackResourceKey;
import groovyjarjarantlr4.v4.runtime.misc.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public record SimpleItemKey(Item item, @Nullable CompoundTag tag)
{
    public SimpleItemKey {
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
}
