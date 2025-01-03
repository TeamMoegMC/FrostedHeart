package com.teammoeg.frostedheart.content.town.resource;

import net.minecraft.world.item.ItemStack;

public record ResourceActionResult (boolean isSuccess, double amount, double lowestLevel, double averageLevel){
    public static final ResourceActionResult NOT_SUCCESS = new ResourceActionResult(false, 0, 0, 0);

    public ResourceActionResult(boolean isSuccess, double amount, ITownResourceKey key){
        this(isSuccess, amount, key.getLevel(), key.getLevel());
    }

    public ResourceActionResult(boolean isSuccess, double amount, ItemStack itemStack){
        this(isSuccess, amount, ItemResourceKey.fromItemStack(itemStack));
    }
}
