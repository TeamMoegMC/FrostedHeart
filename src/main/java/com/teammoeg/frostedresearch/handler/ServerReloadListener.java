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

package com.teammoeg.frostedresearch.handler;

import com.teammoeg.frostedresearch.ResearchHooks;

import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.item.crafting.RecipeManager;

public class ServerReloadListener implements ResourceManagerReloadListener {
	private final RecipeManager recipes;

	public ServerReloadListener(RecipeManager recipes) {
		this.recipes = recipes;
	}

	@Override
	public void onResourceManagerReload(ResourceManager pResourceManager) {
		ResearchHooks.ServerReload(pResourceManager, recipes);
	}

}
