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

package com.teammoeg.chorda.recipe;

import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * 数据配方的抽象基类，用于存储纯数据而非实际的合成配方。
 * 该类继承自 {@link CustomRecipe}，但默认禁用了所有合成功能（匹配、组装、尺寸检查均返回 false/空值），
 * 仅作为数据容器通过 Minecraft 的配方系统进行加载和管理。
 * <p>
 * Abstract base class for data-driven recipes that store pure data rather than actual crafting recipes.
 * Extends {@link CustomRecipe} but disables all crafting functionality by default (matches, assemble,
 * and dimension checks all return false/empty). Serves purely as a data container managed through
 * Minecraft's recipe system.
 *
 * @see DataContainerRecipe
 * @see CodecRecipeSerializer
 */
public abstract class DataRecipe extends CustomRecipe {

	/**
	 * 使用指定的资源位置构造数据配方。
	 * <p>
	 * Constructs a data recipe with the specified resource location.
	 *
	 * @param pId 配方的资源位置标识符 / The resource location identifier for this recipe
	 */
	public DataRecipe(ResourceLocation pId) {
		super(pId, CraftingBookCategory.MISC);
	}

	/**
	 * 始终返回 false，因为数据配方不参与实际合成匹配。
	 * <p>
	 * Always returns false since data recipes do not participate in actual crafting matching.
	 *
	 * @param pContainer 合成容器 / The crafting container
	 * @param pLevel 当前世界 / The current level
	 * @return 始终返回 false / Always returns false
	 */
	@Override
	public boolean matches(CraftingContainer pContainer, Level pLevel) {
		return false;
	}

	/**
	 * 始终返回空物品堆，因为数据配方不产生合成结果。
	 * <p>
	 * Always returns an empty ItemStack since data recipes do not produce crafting results.
	 *
	 * @param pContainer 合成容器 / The crafting container
	 * @param pRegistryAccess 注册表访问器 / The registry access
	 * @return 始终返回 {@link ItemStack#EMPTY} / Always returns {@link ItemStack#EMPTY}
	 */
	@Override
	public ItemStack assemble(CraftingContainer pContainer, RegistryAccess pRegistryAccess) {
		return ItemStack.EMPTY;
	}

	/**
	 * 始终返回 false，因为数据配方不适用于任何合成网格尺寸。
	 * <p>
	 * Always returns false since data recipes are not applicable to any crafting grid dimensions.
	 *
	 * @param pWidth 合成网格宽度 / The crafting grid width
	 * @param pHeight 合成网格高度 / The crafting grid height
	 * @return 始终返回 false / Always returns false
	 */
	@Override
	public boolean canCraftInDimensions(int pWidth, int pHeight) {
		return false;
	}

	/**
	 * 获取此数据配方的配方类型。
	 * <p>
	 * Gets the recipe type for this data recipe.
	 *
	 * @return 配方类型 / The recipe type
	 */
	public abstract RecipeType<?> getType();
}
