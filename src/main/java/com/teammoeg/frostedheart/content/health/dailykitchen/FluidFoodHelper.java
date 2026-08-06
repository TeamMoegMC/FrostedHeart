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

package com.teammoeg.frostedheart.content.health.dailykitchen;

import com.teammoeg.caupona.data.recipes.BowlContainingRecipe;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fluids.capability.ItemFluidContainer;

/**
 * 流体食物解析工具：把"食用中的物品"解析为每日厨房应记录/匹配的食物 Item。
 * <p>
 * 背景：caupona（汤锅模组）的汤既可以做成碗装食物（StewItem，直接食用），也可以装进
 * 保温杯等流体容器（{@link ItemFluidContainer}，饮用）。保温杯本身不是食物（尽管
 * DrinkContainerItem#isEdible 恒为 true），直接记录会污染 foodsEaten 候选池；因此需要
 * 把容器内的流体通过 caupona 的 {@link BowlContainingRecipe} 映射回对应的汤碗 Item。
 * 非食物流体（如水）解析结果为 null，不参与记录与匹配。
 * <p>
 * Helper for resolving fluid food: maps the item being eaten to the food Item that the
 * daily kitchen should record / match.
 * <p>
 * Background: caupona soups can be served as bowl food (StewItem, eaten directly) or put
 * into fluid containers such as thermos ({@link ItemFluidContainer}, drunk). The container
 * itself is not food (though DrinkContainerItem#isEdible always returns true), so recording
 * it directly would pollute the foodsEaten candidate pool; hence the contained fluid is
 * mapped back to the corresponding soup bowl Item via caupona's {@link BowlContainingRecipe}.
 * Non-food fluids (e.g. water) resolve to null and take no part in recording or matching.
 */
public final class FluidFoodHelper {
    private FluidFoodHelper() {
    }

    /**
     * 解析"食用中的物品"对应的食物 Item。
     * <p>
     * Resolves the food Item corresponding to the item being eaten.
     *
     * <ul>
     *   <li>普通可食用物品：返回物品本身 / normal edible items: return the item itself</li>
     *   <li>流体容器（保温杯等）：返回容器内 caupona 汤对应的汤碗 Item，非食物流体返回 null
     *       / fluid containers (thermos etc.): return the soup bowl Item of the contained
     *       caupona soup, or null for non-food fluids</li>
     * </ul>
     *
     * @param stack 食用中的物品 / the item being eaten
     * @return 应记录/匹配的食物 Item；空物品或非食物流体返回 null / the food Item to record
     *         or match; null for empty stacks or non-food fluids
     */
    public static Item resolveFoodItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        if (stack.getItem() instanceof ItemFluidContainer) {
            return resolveFluidFood(stack);
        }
        return stack.getItem();
    }

    /**
     * 把流体容器内的流体解析为 caupona 汤碗 Item。
     * <p>
     * Resolves the fluid contained in a fluid container to a caupona soup bowl Item.
     *
     * @param stack 装流体的容器物品 / the fluid container item stack
     * @return 对应汤碗 Item；容器为空、装非食物流体或 caupona 配方未加载时返回 null / the
     *         matching soup bowl Item, or null when the container is empty, holds a non-food
     *         fluid, or the caupona recipe has not been loaded
     */
    private static Item resolveFluidFood(ItemStack stack) {
        IFluidHandlerItem handler = stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).orElse(null);
        if (handler == null) return null;
        FluidStack fluid = handler.getFluidInTank(0);
        if (fluid.isEmpty()) return null;
        BowlContainingRecipe recipe = BowlContainingRecipe.recipes.get(fluid.getFluid());
        if (recipe == null) return null;
        ItemStack bowl = recipe.handle(fluid.getFluid());
        return bowl.isEmpty() ? null : bowl.getItem();
    }
}
