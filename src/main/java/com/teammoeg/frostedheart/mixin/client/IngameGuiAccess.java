package com.teammoeg.frostedheart.mixin.client;

import net.minecraft.client.gui.IngameGui;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(IngameGui.class)
public interface IngameGuiAccess {
    @Accessor("remainingHighlightTicks")
    int getRemainingHighlightTicks();

    @Accessor("highlightingItemStack")
    ItemStack getHighlightingItemStack();
}
