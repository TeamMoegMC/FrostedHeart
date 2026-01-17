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

import com.teammoeg.chorda.client.cui.Button;
import com.teammoeg.chorda.client.cui.MouseButton;
import com.teammoeg.chorda.client.cui.TextField;
import com.teammoeg.chorda.client.cui.UILayer;
import com.teammoeg.frostedresearch.Lang;
import com.teammoeg.frostedresearch.gui.TechIcons;

import net.minecraft.client.gui.GuiGraphics;

class HelpPanel extends UILayer {
    DrawDeskPanel ot;

    public HelpPanel(DrawDeskPanel panel) {
        super(panel);
        ot = panel;
    }

    @Override
    public void addUIElements() {
        float scale = Float.parseFloat(Lang.translateGui("minigame.scale").getString());
        TextField t1 = new TextField(this).setColor(TechIcons.text).setScale(scale)
                .setMaxWidth(112).setMaxLines(2).setText(Lang.translateGui("minigame.t1"));
        t1.setPos(8, 70);
        TextField t2 = new TextField(this).setColor(TechIcons.text).setScale(scale)
                .setMaxWidth(114).setMaxLines(2).setText(Lang.translateGui("minigame.t2"));
        t2.setPos(124, 70);
        TextField t3 = new TextField(this).setColor(TechIcons.text).setScale(scale)
                .setMaxWidth(90).setMaxLines(3).setText(Lang.translateGui("minigame.t3"));
        t3.setPos(5, 137);
        TextField t4 = new TextField(this).setColor(TechIcons.text).setScale(scale)
                .setMaxWidth(68).setMaxLines(3).setText(Lang.translateGui("minigame.t4"));
        t4.setPos(95, 137);
        TextField t5 = new TextField(this).setColor(TechIcons.text).setScale(scale)
                .setMaxWidth(74).setMaxLines(3).setText(Lang.translateGui("minigame.t5"));
        t5.setPos(165, 137);
        add(t1);
        add(t2);
        add(t3);
        add(t4);
        add(t5);
        Button closePanel = new Button(this) {
            @Override
            public void drawBackground(GuiGraphics matrixStack, int x, int y, int w, int h) {
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
    public void render(GuiGraphics guiGraphics, int x, int y, int w, int h) {
        if (ot.showHelp) {

            super.render(guiGraphics, x, y, w, h);
            guiGraphics.drawString(getFont(), Lang.translateGui("minigame.match"), x + 8, y + 90, TechIcons.text, false);
            guiGraphics.drawString(getFont(), Lang.translateGui("minigame.display"), x + 8, y + 2, TechIcons.text, false);
        }
    }

    @Override
    public void drawBackground(GuiGraphics matrixStack, int x, int y, int w, int h) {
        DrawDeskIcons.HELP.draw(matrixStack, x, y, w, h);
    }

    @Override
    public boolean isEnabled() {
        return ot.showHelp;
    }

}