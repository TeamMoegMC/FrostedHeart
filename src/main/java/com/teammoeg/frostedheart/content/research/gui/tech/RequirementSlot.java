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

package com.teammoeg.frostedheart.content.research.gui.tech;

import blusunrize.immersiveengineering.api.crafting.IngredientWithSize;

import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.client.ui.CGuiHelper;
import com.teammoeg.frostedheart.compat.jei.JEICompat;
import com.teammoeg.frostedheart.content.research.gui.TechIcons;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftblibrary.ui.WidgetType;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public class RequirementSlot extends Widget {
    ItemStack[] i;
    int cnt;

    public RequirementSlot(Panel panel, IngredientWithSize iws) {
        super(panel);
        this.i = iws.getMatchingStacks();
        this.cnt = iws.getCount();
        this.setSize(16, 16);
    }

    @Override
    public void addMouseOverText(TooltipList list) {
        ItemStack cur = i[(int) ((System.currentTimeMillis() / 1000) % i.length)];
        //list.add(cur.getDisplayName());
        cur.getTooltipLines(ClientUtils.getPlayer(), TooltipFlag.Default.NORMAL).forEach(list::add);
    }

    @Override
    public void draw(GuiGraphics matrixStack, Theme theme, int x, int y, int w, int h) {
        ItemStack cur = i[(int) ((System.currentTimeMillis() / 1000) % i.length)];
        dev.ftb.mods.ftblibrary.ui.GuiHelper.setupDrawing();
        TechIcons.SLOT.draw(matrixStack, x - 4, y - 4, 24, 24);
        CGuiHelper.drawItem(matrixStack, cur, x, y, 0, w / 16F, h / 16F, true, cnt != 0 ? String.valueOf(cnt) : null);
    }

    @Override
    public boolean mousePressed(MouseButton button) {
        if (isMouseOver()) {
            if (getWidgetType() != WidgetType.DISABLED) {
                //TODO edit ingredient
                JEICompat.showJEIFor(i[(int) ((System.currentTimeMillis() / 1000) % i.length)]);
            }

            return true;
        }

        return false;
    }


}
