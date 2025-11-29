package com.teammoeg.frostedheart.content.town.warehouse;

import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;

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

    public static List<VirtualItemStack> toClientVisualList(List<VirtualItemStack> list,Map<ItemStack, Double> itemMap){
        for (Map.Entry<ItemStack, Double> entry : itemMap.entrySet()) {
            ItemStack stack = entry.getKey();
            Double amount = entry.getValue();
            list.add(new VirtualItemStack(stack, amount.longValue()));
        }
        return list;
    }
}
