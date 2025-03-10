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

package com.teammoeg.frostedresearch.gui.drawdesk;

import com.teammoeg.chorda.util.Lang;
import com.teammoeg.frostedresearch.gui.RTextField;
import com.teammoeg.frostedresearch.gui.TechIcons;

import dev.ftb.mods.ftblibrary.ui.Button;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import net.minecraft.client.gui.GuiGraphics;

class HelpPanel extends Panel {
    DrawDeskPanel ot;

    public HelpPanel(DrawDeskPanel panel) {
        super(panel);
        ot = panel;
    }

    @Override
    public void addWidgets() {
        float scale = Float.parseFloat(Lang.translateGui("minigame.scale").getString());
        RTextField t1 = new RTextField(this).setColor(TechIcons.text).addFlags(0).setScale(scale)
                .setMaxWidth(112).setMaxLine(2).setText(Lang.translateGui("minigame.t1"));
        t1.setPos(8, 70);
        RTextField t2 = new RTextField(this).setColor(TechIcons.text).addFlags(0).setScale(scale)
                .setMaxWidth(114).setMaxLine(2).setText(Lang.translateGui("minigame.t2"));
        t2.setPos(124, 70);
        RTextField t3 = new RTextField(this).setColor(TechIcons.text).addFlags(0).setScale(scale)
                .setMaxWidth(90).setMaxLine(3).setText(Lang.translateGui("minigame.t3"));
        t3.setPos(5, 137);
        RTextField t4 = new RTextField(this).setColor(TechIcons.text).addFlags(0).setScale(scale)
                .setMaxWidth(68).setMaxLine(3).setText(Lang.translateGui("minigame.t4"));
        t4.setPos(95, 137);
        RTextField t5 = new RTextField(this).setColor(TechIcons.text).addFlags(0).setScale(scale)
                .setMaxWidth(74).setMaxLine(3).setText(Lang.translateGui("minigame.t5"));
        t5.setPos(165, 137);
        add(t1);
        add(t2);
        add(t3);
        add(t4);
        add(t5);
        Button closePanel = new Button(this) {
            @Override
            public void drawBackground(GuiGraphics matrixStack, Theme theme, int x, int y, int w, int h) {
            }

            @Override
            public void onClicked(MouseButton mouseButton) {
                ot.closeHelp();
            }
        };
        closePanel.setPosAndSize(226, 7, 9, 8);
        add(closePanel);
    }

    @Override
    public void alignWidgets() {
    }

    @Override
    public void draw(GuiGraphics arg0, Theme arg1, int arg2, int arg3, int arg4, int arg5) {
        if (ot.showHelp) {

            super.draw(arg0, arg1, arg2, arg3, arg4, arg5);
            arg1.drawString(arg0, Lang.translateGui("minigame.match"), arg2 + 8, arg3 + 90, TechIcons.text, 0);
            arg1.drawString(arg0, Lang.translateGui("minigame.display"), arg2 + 8, arg3 + 2, TechIcons.text, 0);
        }
    }

    @Override
    public void drawBackground(GuiGraphics matrixStack, Theme theme, int x, int y, int w, int h) {
        DrawDeskIcons.HELP.draw(matrixStack, x, y, w, h);
    }

    @Override
    public boolean isEnabled() {
        return ot.showHelp;
    }

}