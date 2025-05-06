package com.teammoeg.frostedheart.content.agriculture.biogassystem.screen.utils;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

public class OnlyExtractSlot extends SlotItemHandler {

    public OnlyExtractSlot(IItemHandler itemHandler, int index, int xPosition, int yPosition) {
        super(itemHandler, index, xPosition, yPosition);
    }

    @Override
    public boolean mayPlace(ItemStack pStack) {
        return false;
    }
}
