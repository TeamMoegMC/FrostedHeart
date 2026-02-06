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

package com.teammoeg.frostedresearch.blocks;

import com.teammoeg.chorda.creativeTab.CreativeTabItemHelper;
import com.teammoeg.chorda.creativeTab.ICreativeModeTabItem;

import com.teammoeg.frostedresearch.FRContents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class FHReusablePen extends Item implements ICreativeModeTabItem,IPen {
    int lvl;

    public FHReusablePen(Properties properties, int lvl) {
        super(properties);
        this.lvl = lvl;
    }

    @Override
    public boolean canUse(Player e, ItemStack stack, int val) {
        return stack.getDamageValue() < stack.getMaxDamage() - val;
    }

    @Override
    public void doDamage(Player e, ItemStack stack, int val) {
        stack.hurtAndBreak(val, e, ex -> {
        });
    }

    @Override
    public int getLevel(ItemStack is, Player player) {
        return lvl;
    }

	@Override
	public void fillItemCategory(CreativeTabItemHelper helper) {
        if(helper.isType(FRContents.Tabs.BLOCK_TAB_TYPE))
            helper.accept(this);
		
	}

}
