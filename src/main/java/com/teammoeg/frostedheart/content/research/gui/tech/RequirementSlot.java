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

package com.teammoeg.frostedheart.content.research.gui.tech;

import net.minecraft.client.gui.GuiGraphics;
import com.teammoeg.frostedheart.compat.jei.JEICompat;
import com.teammoeg.frostedheart.content.research.gui.TechIcons;
import com.teammoeg.frostedheart.util.client.ClientUtils;

import blusunrize.immersiveengineering.api.crafting.IngredientWithSize;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.ui.GuiHelper;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftblibrary.ui.WidgetType;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.ItemStack;

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
        GuiHelper.setupDrawing();
        TechIcons.SLOT.draw(matrixStack, x - 4, y - 4, 24, 24);
        matrixStack.pushPose();
        matrixStack.translate(0, 0, 100);
        GuiHelper.drawItem(matrixStack, cur, x, y, w / 16F, h / 16F, true, null);
        if (cnt > 1) {
            matrixStack.pushPose();
            matrixStack.translate(0, 0, 100);
            int dx = 5;
            if (cnt >= 10)
                dx = 0;
            theme.drawString(matrixStack, String.valueOf(cnt), dx + x + 8, y + 9, Color4I.WHITE, Theme.SHADOW);
            matrixStack.popPose();
        }
        matrixStack.popPose();
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
