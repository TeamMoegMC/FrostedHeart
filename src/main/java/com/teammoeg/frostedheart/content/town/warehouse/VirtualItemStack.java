package com.teammoeg.frostedheart.content.town.warehouse;

import net.minecraft.world.item.ItemStack;

public class VirtualItemStack {
    private final ItemStack stack;
    private long amount;

    public VirtualItemStack(ItemStack stack, long amount) {
        this.stack = stack;
        this.amount = amount;
    }

    public ItemStack getStack() { return stack; }
    public long getAmount() { return amount; }
    public void setAmount(long amount) { this.amount = amount; }
}
