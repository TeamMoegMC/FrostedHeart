package com.teammoeg.frostedheart.item.snowsack;

import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class SnowSackMenuProvider implements MenuProvider {
    private final ItemStack snowSack;

    public SnowSackMenuProvider(ItemStack snowSack) {
        this.snowSack = snowSack;
    }

    @Nonnull
    @Override
    public Component getDisplayName() {
        return snowSack.getDisplayName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, @NotNull Inventory playerInventory, @NotNull Player player) {
        return new SnowSackMenu(windowId, playerInventory, snowSack);
    }
}