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

    public static List<VirtualItemStack> toClientVisualList(Map<ItemStack, Double> itemMap) {
        if (itemMap == null || itemMap.isEmpty()) {
            return List.of();
        }
        return itemMap.entrySet().stream()
                .map(entry -> {
                    ItemStack stack = entry.getKey();
                    Double amount = entry.getValue();
                    return new VirtualItemStack(stack, amount.longValue());
                })
                .toList();
    }
}
