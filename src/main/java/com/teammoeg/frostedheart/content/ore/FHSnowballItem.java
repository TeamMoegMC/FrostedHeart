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

package com.teammoeg.frostedheart.content.ore;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.util.creativeTab.CreativeTabItemHelper;
import com.teammoeg.frostedheart.util.creativeTab.ICreativeModeTabItem;
import net.minecraft.world.item.SnowballItem;

public class FHSnowballItem extends SnowballItem implements ICreativeModeTabItem {
    public FHSnowballItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public void fillItemCategory(CreativeTabItemHelper helper) {
        if(helper.isType(FHMain.itemGroup))
            helper.accept(this);
    }
}
