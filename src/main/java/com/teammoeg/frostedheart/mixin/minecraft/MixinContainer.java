/*
 * Copyright (c) 2022-2024 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.teammoeg.frostedheart.mixin.minecraft;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
/**
* fix a dupe bug made by careless bugjang developers
* Hope this would work
*/
@Mixin(AbstractContainerMenu.class)
public class MixinContainer {
    @Shadow
    private List<Slot> inventorySlots;

    @Inject(at = @At("HEAD"), method = "doClick", cancellable = true, require = 1)
    private void fh$slotSwapPending(int p_241440_1_, int p_241440_2_, ClickType p_241440_3_, Player p_241440_4_, CallbackInfoReturnable<ItemStack> cbi) {
        if (p_241440_3_ == ClickType.SWAP) {
            Slot slot2 = null;
            for (Slot slot : this.inventorySlots) {
                if (slot.getSlotIndex() == p_241440_2_ && slot.container instanceof Inventory) {
                    slot2 = slot;
                    break;
                }
            }
            if (slot2 == null) return;
            if (!slot2.mayPickup(p_241440_4_))
                cbi.setReturnValue(ItemStack.EMPTY);
        }
    }
}
