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
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import se.mickelus.tetra.module.schematic.OutcomeDefinition;

import java.util.Optional;

/** Revalidates S04's optional workbench choices before Tetra consumes ingredients. */
@Pseudo
@Mixin(targets = "se.mickelus.tetra.module.schematic.ConfigSchematic", remap = false)
public abstract class ConfigSchematicMixin_StrataDrill {
    @Shadow
    public abstract String getKey();

    @Shadow
    private Optional<OutcomeDefinition> getOutcomeFromMaterial(ItemStack materialStack, int slot) {
        throw new AssertionError();
    }

    @Inject(method = "isMaterialsValid", at = @At("HEAD"), cancellable = true)
    private void fh$checkCurrentStrataDrillPart(ItemStack input, String slot, ItemStack[] materials,
                                               CallbackInfoReturnable<Boolean> callback) {
        if (!(input.getItem() instanceof TetraStrataDrillItem drill)) return;

        String key = getKey();
        // Tetra checks the schematic requirement when listing choices, not on a saved selection.
        if (!drill.allowsPartSwap(input, slot, key)) {
            callback.setReturnValue(false);
            return;
        }

        if ("stratadrill/cutting_edge".equals(key) && (!"stratadrill/head".equals(slot)
                || materials.length == 0
                || getOutcomeFromMaterial(materials[0], 0)
                        .filter(outcome -> outcome.improvements.entrySet().stream()
                                .anyMatch(entry -> drill.supportsCutter(input, entry.getKey(), entry.getValue())))
                        .isEmpty())) {
            callback.setReturnValue(false);
        }
    }
}
