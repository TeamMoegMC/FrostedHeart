/*
 * Copyright (c) 2026 TeamMoeg
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

package com.teammoeg.frostedheart.mixin.tetra;

import com.teammoeg.frostedheart.content.stratadrill.TetraStrataDrillItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keeps S04 part swaps consistent with the current stack and Tetra's crafted result. */
@Pseudo
@Mixin(targets = "se.mickelus.tetra.module.schematic.ConfigSchematic", remap = false)
public abstract class ConfigSchematicMixin_StrataDrill {
    @Shadow
    public abstract String getKey();

    @Inject(method = "isMaterialsValid", at = @At("HEAD"), cancellable = true)
    private void fh$checkCurrentStrataDrillPart(ItemStack input, String slot, ItemStack[] materials,
                                               CallbackInfoReturnable<Boolean> callback) {
        // Tetra checks the schematic requirement when listing choices, not when crafting a saved selection.
        if (input.getItem() instanceof TetraStrataDrillItem drill && !drill.allowsPartSwap(input, slot, getKey())) {
            callback.setReturnValue(false);
        }
    }

    @Inject(method = "applyUpgrade", at = @At("RETURN"))
    private void fh$pruneInactiveCutters(ItemStack input, ItemStack[] materials, boolean consumeMaterials,
                                         String slot, Player player, CallbackInfoReturnable<ItemStack> callback) {
        if (consumeMaterials && callback.getReturnValue() != input
                && input.getItem() instanceof TetraStrataDrillItem drill) {
            drill.pruneInactiveCuttersAfterCraft(callback.getReturnValue(), slot, getKey());
        }
    }
}
