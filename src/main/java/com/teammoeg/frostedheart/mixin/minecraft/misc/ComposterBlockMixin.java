/*
 * Copyright (c) 2024 TeamMoeg
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

package com.teammoeg.frostedheart.mixin.minecraft.misc;

import com.teammoeg.frostedheart.bootstrap.common.FHItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.ComposterBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ComposterBlock.class)
public class ComposterBlockMixin {

    // This redirects the ItemStack creation in the extractProduce method
    @Redirect(
            method = "extractProduce",
            at = @At(
                    value = "NEW",
                    target = "net/minecraft/world/item/ItemStack",
                    ordinal = 0
            )
    )
    private static ItemStack redirectBoneMealItemStack(ItemLike pItem) {
        return new ItemStack(FHItems.BIOMASS.asItem());
    }

    // This modifies the constructor of OutputContainer to use our biomass item
    @Redirect(
            method = "getContainer",
            at = @At(
                    value = "NEW",
                    target = "net/minecraft/world/item/ItemStack",
                    ordinal = 0
            )
    )
    private ItemStack redirectOutputContainerStack(ItemLike pItem) {
        return new ItemStack(FHItems.BIOMASS.asItem());
    }
}