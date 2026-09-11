/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedresearch.mixin.create;

import com.simibubi.create.content.kinetics.crafter.MechanicalCrafterBlockEntity;
import com.simibubi.create.content.kinetics.crafter.RecipeGridHandler;
import com.simibubi.create.content.kinetics.crafter.RecipeGridHandler.GroupedItems;
import com.teammoeg.frostedresearch.ResearchHooks;
import com.teammoeg.frostedresearch.mixinutil.IOwnerTile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MechanicalCrafterBlockEntity.class)
public abstract class MechanicalCrafterBlockEntityMixin {
    @Redirect(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/simibubi/create/content/kinetics/crafter/RecipeGridHandler;tryToApplyRecipe(Lnet/minecraft/world/level/Level;Lcom/simibubi/create/content/kinetics/crafter/RecipeGridHandler$GroupedItems;)Lnet/minecraft/world/item/ItemStack;",
                    remap = false),
            remap = false)
    private ItemStack fh$withResearchOwner(Level level, GroupedItems items) {
        MechanicalCrafterBlockEntity self = (MechanicalCrafterBlockEntity) (Object) this;
        return ResearchHooks.withRecipeOwner(
                IOwnerTile.getOwner(self), () -> RecipeGridHandler.tryToApplyRecipe(level, items));
    }
}
