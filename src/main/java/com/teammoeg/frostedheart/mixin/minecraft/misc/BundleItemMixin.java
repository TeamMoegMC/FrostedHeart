package com.teammoeg.frostedheart.mixin.minecraft.misc;

import blusunrize.immersiveengineering.api.IETags;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 防止容器套娃
 */
@Mixin(BundleItem.class)
public class BundleItemMixin {

    @Inject(at = @At("HEAD"), method = "overrideStackedOnOther", cancellable = true)
    public void overrideStackedOnOther(ItemStack pStack, Slot pSlot, ClickAction pAction, Player pPlayer, CallbackInfoReturnable<Boolean> cir) {
        if (pSlot.getItem().is(IETags.forbiddenInCrates)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(at = @At("HEAD"), method = "overrideOtherStackedOnMe", cancellable = true)
    public void overrideOtherStackedOnMe(ItemStack pStack, ItemStack pOther, Slot pSlot, ClickAction pAction, Player pPlayer, SlotAccess pAccess, CallbackInfoReturnable<Boolean> cir) {
        if (pOther.is(IETags.forbiddenInCrates)) {
            cir.setReturnValue(false);
        }
    }
}
