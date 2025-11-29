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

package com.teammoeg.frostedheart.content.utility;

import com.teammoeg.frostedheart.bootstrap.common.ToolCompat;
import com.teammoeg.frostedheart.item.FHBaseItem;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ToolAction;
import se.mickelus.tetra.properties.IToolProvider;

public class FHLeveledTool extends FHBaseItem {
    protected int level;

    public FHLeveledTool(int lvl, Properties properties) {
        super(properties);
        this.level = lvl;
    }

    public int getLevel() {
        return level;
    }

	@Override
	public boolean canPerformAction(ItemStack stack, ToolAction toolAction) {
		return super.canPerformAction(stack, toolAction);
	}
    public static int getLevel(ItemStack item) {
        if (item.getItem() instanceof FHLeveledTool)
            return ((FHLeveledTool) item.getItem()).getLevel();

        return ((IToolProvider) item.getItem()).getToolLevel(item, ToolCompat.coreSpade);
    }

}
