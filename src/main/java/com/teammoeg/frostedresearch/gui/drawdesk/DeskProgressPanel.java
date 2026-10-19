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

package com.teammoeg.frostedresearch.gui.drawdesk;

import com.teammoeg.chorda.client.RenderingHint;
import com.teammoeg.chorda.client.cui.base.TooltipBuilder;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.chorda.client.cui.base.UILayer;
import com.teammoeg.chorda.client.cui.theme.UIColors;
import com.teammoeg.chorda.client.cui.widgets.TextField;
import com.teammoeg.chorda.client.icon.CIcons.CIcon;
import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.chorda.client.ui.CGuiHelper;
import com.teammoeg.frostedresearch.Lang;
import com.teammoeg.frostedresearch.blocks.DrawingDeskTileEntity;
import com.teammoeg.frostedresearch.gui.DrawDeskTheme;
import com.teammoeg.frostedresearch.gui.TechIcons;
import com.teammoeg.frostedresearch.gui.tech.ResearchProgressPanel;
import com.teammoeg.frostedresearch.knowledge.client.KnowledgeClientState;
import com.teammoeg.frostedresearch.knowledge.client.KnowledgeJournalData;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;

import java.util.Objects;

/**
 * 绘图台左上角的研究栏：桌子有知识研究的演算目标时，显示纸牌局计入的项目与进度，否则显示旧研究的进度。
 * The desk's top-left research corner. While the desk calculates for a knowledge research it shows the project the
 * games count toward and their progress; otherwise the legacy research progress.
 */
public class DeskProgressPanel extends UILayer {
    private static final CIcon PAPER = CIcons.getIcon(Items.PAPER);
    /** Height above the desk buttons that share this corner. */
    private static final int BODY = 48;

    private record Target(Component project, Component requirement, int level, int done, int count) {
    }

    private final DrawDeskScreen dd;
    private Target target;
    private int signature;

    public DeskProgressPanel(UIElement parent, DrawDeskScreen dd) {
        super(parent);
        this.dd = dd;
        signature = signature();
        target = find();
    }

    private int signature() {
        DrawingDeskTileEntity tile = dd.getTile();
        return Objects.hash(tile.calculationRun(), tile.calculationRequirement(), KnowledgeClientState.serial());
    }

    /**
     * The calculation target as the client snapshot shows it; null without a target or before the snapshot has the run.
     */
    private Target find() {
        DrawingDeskTileEntity tile = dd.getTile();
        if (!tile.hasCalculation()) return null;
        String run = tile.calculationRun().toString();
        for (var open : new KnowledgeJournalData(KnowledgeClientState.snapshot()).runs)
            if (open.id().equals(run))
                for (var requirement : open.requirements())
                    if (requirement.id().equals(tile.calculationRequirement()))
                        return new Target(open.title(), requirement.title().getString().isBlank()
                                ? Component.translatable("gui.frostedresearch.journal.research.calculation") : requirement.title(),
                                requirement.level(), Math.min(requirement.done(), requirement.count()), requirement.count());
        return null;
    }

    @Override
    public void addUIElements() {
        if (target == null) {
            ResearchProgressPanel legacy = new ResearchProgressPanel(this);
            legacy.setPosAndSize(0, 0, getWidth(), getHeight());
            add(legacy);
            return;
        }
        TextField title = new TextField(this);
        title.setMaxWidth(71).setMaxLines(2).setPos(40, 15);
        title.setText(target.project());
        add(title);
    }

    @Override
    public void alignWidgets() {
    }

    @Override
    public void tick() {
        int now = signature();
        if (now != signature) {
            signature = now;
            target = find();
            refresh();
        }
        super.tick();
    }

    @Override
    public void render(GuiGraphics g, int x, int y, int w, int h, RenderingHint hint) {
        super.render(g, x, y, w, h, hint);
        if (target == null) return;
        int ink = UIColors.UI_TEXT.getColorARGB(this, x, y, hint);
        g.drawString(getFont(), Lang.translateGui("draw_desk.calculation_for"), x + 3, y, ink, false);
        TechIcons.SHADOW.draw(g, x + 1, y + 38, 36, 9);
        PAPER.draw(g, x + 3, y + 12, 32, 32);
        CGuiHelper.resetGuiDrawing();
        DrawDeskTheme.INSTANCE.drawProgressBar(g, x + 40, y + 32, 70, 8, target.done() / (float) target.count());
        g.drawString(getFont(), Lang.translateGui("draw_desk.calculation_progress", target.done(), target.count(), target.level()),
                x + 40, y + 40, ink, false);
        TechIcons.HLINE_LR.draw(g, x + 1, y + BODY, w - 1, 3);
    }

    @Override
    public void getTooltip(TooltipBuilder list) {
        if (target == null || !isMouseOver() || getMouseY() >= BODY) {
            super.getTooltip(list);
            return;
        }
        list.accept(target.project());
        list.accept(target.requirement());
        list.accept(Component.translatable("gui.frostedresearch.journal.research.calculation.explain", target.level()));
    }

    @Override
    public void drawBackground(GuiGraphics g, int x, int y, int w, int h, RenderingHint hint) {
    }
}
