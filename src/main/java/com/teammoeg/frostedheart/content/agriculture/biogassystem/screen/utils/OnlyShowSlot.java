package com.teammoeg.frostedheart.content.agriculture.biogassystem.screen.utils;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

public class OnlyShowSlot extends SlotItemHandler {

    public OnlyShowSlot(IItemHandler itemHandler, int index, int xPosition, int yPosition) {
        super(itemHandler, index, xPosition, yPosition);
    }

    @Override
    public boolean mayPlace(ItemStack pStack) {
        return false;
    }

    @Override
    public boolean mayPickup(Player pPlayer) {
        return false;
    }
}
