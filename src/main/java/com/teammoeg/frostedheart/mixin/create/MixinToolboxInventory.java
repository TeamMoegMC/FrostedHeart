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

package com.teammoeg.frostedheart.mixin.create;

import com.simibubi.create.content.curiosities.toolbox.ToolboxInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.items.ItemStackHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ToolboxInventory.class)
public class MixinToolboxInventory extends ItemStackHandler {
    ResourceLocation forbid = new ResourceLocation("immersiveengineering:forbidden_in_crates");

    @Inject(at = @At("HEAD"), method = "isItemValid", cancellable = true, remap = false)
    public void FH$AvoidForbid(int slot, ItemStack stack, CallbackInfoReturnable<Boolean> cbi) {
        if (stack.getItem().getTags().contains(forbid))
            cbi.setReturnValue(false);
    }
}
