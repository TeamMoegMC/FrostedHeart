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

package com.teammoeg.frostedheart.mixin.stone_age;

import java.util.List;

import javax.annotation.Nonnull;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.google.common.collect.ImmutableList;
import com.teammoeg.frostedheart.content.research.ResearchListeners;
import com.yanny.age.stone.blocks.FlintWorkbenchTileEntity;
import com.yanny.age.stone.recipes.FlintWorkbenchRecipe;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.NonNullList;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.items.wrapper.RecipeWrapper;

@Mixin(FlintWorkbenchTileEntity.class)
public class FlintWorkbenchTileEntityMixin extends BlockEntity {
    @Shadow(remap = false)
    private NonNullList<ItemStack> stacks;
    @Shadow(remap = false)
    private RecipeWrapper inventoryWrapper;

    Player pe;

    public FlintWorkbenchTileEntityMixin(BlockEntityType<?> tileEntityTypeIn) {
        super(tileEntityTypeIn);
    }

    @Inject(at = @At("HEAD"), method = "blockActivated", remap = false)
    public void fh$blockActivated(@Nonnull Player player, @Nonnull BlockHitResult hit,
                                  CallbackInfoReturnable<InteractionResult> cbi) {
        pe = player;
    }

    /**
     * @author khjxiaogu
     * @reason Make research can limit flint workbench
     */
    @Overwrite(remap = false)
    private List<FlintWorkbenchRecipe> findMatchingRecipes() {
        assert this.level != null;

        if (stacks.stream().allMatch(ItemStack::isEmpty))
            return ImmutableList.of();
        List<FlintWorkbenchRecipe> ret = this.level.getRecipeManager().getRecipesFor(FlintWorkbenchRecipe.flint_workbench,
                inventoryWrapper, this.level);
        ret.removeIf(r -> !ResearchListeners.canUseRecipe(pe, r));

        return ret;
    }

    /**
     * @author khjxiaogu
     * @reason Make research can limit flint workbench
     */
    @Overwrite(remap = false)
    private List<FlintWorkbenchRecipe> findMatchingRecipes(@Nonnull ItemStack heldItemMainhand) {
        assert this.level != null;

        return findMatchingRecipes().stream()
                .filter(flintWorkbenchRecipe -> flintWorkbenchRecipe.testTool(heldItemMainhand)).findFirst().map(ImmutableList::of).orElseGet(ImmutableList::of);
    }
}
