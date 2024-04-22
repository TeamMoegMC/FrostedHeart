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

package com.teammoeg.frostedheart.content.research.gui.drawdesk;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.teammoeg.frostedheart.content.research.ResearchListeners;
import com.teammoeg.frostedheart.content.research.blocks.DrawingDeskTileEntity;
import com.teammoeg.frostedheart.content.research.gui.TechButton;
import com.teammoeg.frostedheart.content.research.gui.TechIcons;
import com.teammoeg.frostedheart.content.research.gui.drawdesk.game.CardStat;
import com.teammoeg.frostedheart.content.research.gui.drawdesk.game.CardType;
import com.teammoeg.frostedheart.content.research.gui.drawdesk.game.ClientResearchGame;
import com.teammoeg.frostedheart.content.research.inspire.EnergyCore;
import com.teammoeg.frostedheart.util.TranslateUtils;
import com.teammoeg.frostedheart.util.client.ClientUtils;

import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.TextField;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import net.minecraft.util.text.StringTextComponent;

class MainGamePanel extends Panel {
    ClientResearchGame rg;
    DrawDeskPanel ot;
    TechButton reset;
    TextField status;
    int lstatus = 0;
    CardButton[][] cbs = new CardButton[9][9];

    boolean enabled = true;

    public MainGamePanel(DrawDeskPanel panel, DrawDeskScreen p) {
        super(panel);
        ot = panel;
        rg = new ClientResearchGame(p.getTile().getGame(), p.getTile().getPos());
        for (int i = 0; i < 9; i++)
            for (int j = 0; j < 9; j++) {
                CardButton cb = new CardButton(this, rg, i, j);
                cb.setPosAndSize(17 * i, 17 * j + 3, 17, 17);
                cbs[i][j] = cb;
            }
        reset = new TechButton(this, DrawDeskIcons.RESET) {

            @Override
            public void addMouseOverText(TooltipList list) {
                super.addMouseOverText(list);
                list.add(TranslateUtils.translateGui("draw_desk.reset"));
            }

            @Override
            public void onClicked(MouseButton arg0) {
                rg.init();
                refreshWidgets();
            }
        };
        reset.setPosAndSize(157, 136, 27, 16);
        status = new TextField(this).addFlags(Theme.CENTERED).addFlags(Theme.CENTERED_V).setMaxWidth(108).setColor(TechIcons.text);

        status.setPosAndSize(22, 54, 108, 50);
    }

    @Override
    public void addWidgets() {
        rg.attach();
        for (int i = 0; i < 9; i++)
            for (int j = 0; j < 9; j++) {
                add(cbs[i][j]);
            }
        int cntcs = 0;

        Panel states = new Panel(this) {

            @Override
            public void addWidgets() {
                int cntad = 0;
                int cntall = 0;
                for (CardStat cs : rg.getStats().values())
                    if (cs.type == CardType.ADDING)
                        cntall++;
                if (cntall <= 6) {
                    for (CardStat cs : rg.getStats().values()) {
                        if (cs.type == CardType.ADDING) {
                            OrderWidget ow = new OrderWidget(this, rg, cs.pack());
                            ow.setPosAndSize(0, 28 * cntad, 16, 28);
                            add(ow);
                            cntad++;
                        }
                    }
                } else {
                    int cntig = 0;
                    for (CardStat cs : rg.getStats().values()) {
                        if (cs.type == CardType.ADDING) {
                            if (cntig < cntall - 6 && cs.num == 0) {
                                cntig++;
                                continue;
                            }
                            if (cntad >= 5 && cs.card != 8) {
                                OrderWidget ow2 = new OrderWidget(this, rg, 0);
                                ow2.setPosAndSize(0, 28 * cntad, 16, 28);
                                add(ow2);
                                break;
                            }
                            OrderWidget ow = new OrderWidget(this, rg, cs.pack());
                            ow.setPosAndSize(0, 28 * cntad, 16, 28);
                            add(ow);
                            cntad++;

                        }
                    }
                }
            }

            @Override
            public void alignWidgets() {
            }
        };
        states.setPosAndSize(188, 0, 16, 164);
        add(states);
        for (CardStat cs : rg.getStats().values()) {
            if (cs.type != CardType.ADDING) {
                CardStatPanel csp = new CardStatPanel(this, rg, cs.pack());
                // System.out.println(cs.toString());
                csp.setPosAndSize(154 + (cntcs % 2) * 15, 1 + (cntcs / 2) * 28, 16, 28);
                add(csp);
                cntcs++;
            }
        }

        TechButton help = new TechButton(this, TechIcons.Question) {

            @Override
            public void addMouseOverText(TooltipList list) {
                super.addMouseOverText(list);
                list.add(TranslateUtils.translateGui("draw_desk.help"));
            }

            @Override
            public void onClicked(MouseButton arg0) {
                ot.openHelp();
            }
        };
        help.setPosAndSize(157, 116, 27, 16);
        add(help);

        add(reset);
    }

    @Override
    public void alignWidgets() {
    }

    @Override
    public void draw(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {

        super.draw(matrixStack, theme, x, y, w, h);
        if (lstatus != 0) {
            DrawDeskIcons.DIALOG_FRAME.draw(matrixStack, x + 7, y + 54, 137, 52);
            status.draw(matrixStack, theme, status.getX(), status.getY(), status.width, status.height);
        }
        if (ResearchListeners.fetchGameLevel() == -1) {
            if (lstatus != 4) {
                status.setText(TranslateUtils.translateGui("minigame.no_clue"));
                status.setPosAndSize(22, 54, 108, 50);
                lstatus = 4;
            }
            return;
        }


        if ((reset.isMouseOver() || rg.getLevel() == -1) && EnergyCore.getEnergy(ClientUtils.getPlayer())<=0) {

            if (lstatus != 1) {
                status.setText(TranslateUtils.translateGui("minigame.tired_to_research"));
                status.setPosAndSize(22, 54, 108, 50);
                lstatus = 1;
            }
            return;
        }
        DrawingDeskTileEntity tile = ot.dd.getTile();
        if (!tile.isInkSatisfied(reset.isMouseOver() ? 5 : 1)) {
            if (lstatus != 2) {
                status.setText(TranslateUtils.translateGui("minigame.no_ink"));
                status.setPosAndSize(22, 54, 108, 50);
                lstatus = 2;
            }
            return;
        } else if (reset.isMouseOver() && !tile.isPaperSatisfied()) {
            if (lstatus != 3) {
                status.setText(TranslateUtils.translateGui("minigame.no_paper"));
                status.setPosAndSize(22, 54, 108, 50);
                lstatus = 3;
            }
            return;
        } else if (rg.getLevel() == -1) {
            if (lstatus != 5) {
                status.setText(TranslateUtils.translateGui("minigame.can_start"));
                status.setPosAndSize(22, 54, 108, 50);
                lstatus = 5;
            }
            return;
        }
        if (lstatus != 0) {
            status.setText(StringTextComponent.EMPTY);
            lstatus = 0;
        }

    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void onClosed() {
        rg.deinit();
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}