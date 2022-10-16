/*
 * Copyright (c) 2022 TeamMoeg
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

import com.teammoeg.frostedheart.FHConfig;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.INameable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerInventory.class)
public abstract class PlayerInventoryMixin implements IInventory, INameable {

    public PlayerInventoryMixin() {
    }

    private PlayerInventory getThis() {
        return (PlayerInventory) (Object) this;
    }

    @Inject(at = @At("HEAD"), method = "dropAllItems", cancellable = true)
    public void fh$dropAllItems(CallbackInfo cbi) {
        if (FHConfig.SERVER.keepEquipments.get()) {
            PlayerInventory othis = getThis();
            for (int i = 9; i < othis.mainInventory.size(); i++) {
                ItemStack itemstack = othis.mainInventory.get(i);
                if (!itemstack.isEmpty()) {
                    othis.player.dropItem(itemstack, true, false);
                    othis.mainInventory.set(i, ItemStack.EMPTY);
                }
            }
            for (int i = 0; i < 9; i++) {
                ItemStack itemstack = othis.mainInventory.get(i);
                if (!itemstack.isEmpty() && !itemstack.isDamageable() && itemstack.getToolTypes().isEmpty()) {
                    othis.player.dropItem(itemstack, true, false);
                    othis.mainInventory.set(i, ItemStack.EMPTY);
                }
            }
            cbi.cancel();

        }
    }

}
