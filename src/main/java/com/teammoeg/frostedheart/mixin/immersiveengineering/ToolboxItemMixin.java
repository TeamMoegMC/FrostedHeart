package com.teammoeg.frostedheart.mixin.immersiveengineering;

import blusunrize.immersiveengineering.api.IETags;
import blusunrize.immersiveengineering.common.items.ToolboxItem;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 防止容器套娃
 */
@Mixin(ToolboxItem.class)
public class ToolboxItemMixin {

    @Inject(at = @At("HEAD"), method = "overrideOtherStackedOnMe", cancellable = true)
    public void overrideOtherStackedOnMe(ItemStack pStack, ItemStack pOther, Slot pSlot, ClickAction pAction, Player pPlayer, SlotAccess pAccess, CallbackInfoReturnable<Boolean> cir) {
        if (pOther.is(IETags.forbiddenInCrates)) {
            cir.setReturnValue(false);
        }
    }
}
