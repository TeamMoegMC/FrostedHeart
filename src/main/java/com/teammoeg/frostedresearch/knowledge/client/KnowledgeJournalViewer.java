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

package com.teammoeg.frostedresearch.knowledge.client;

import javax.annotation.Nullable;

import com.teammoeg.chorda.client.cui.base.HidesSidebarButtons;
import com.teammoeg.chorda.client.cui.base.PrimaryLayer;
import com.teammoeg.chorda.client.cui.screenadapter.CUIScreenWrapper;
import com.teammoeg.frostedresearch.knowledge.model.KnowledgeKey;

import net.minecraft.client.Minecraft;

/**
 * 不在绘图台旁也能翻的札记：只读，能翻页、搜索、阅读，不能整理、联想或用工具。从研究所的布告板点开一个想法时用它，
 * 打开就停在那一条上；关上回到原来的界面。
 * <p>
 * The journal away from a drawing desk: read-only, for browsing, searching and reading, without organising, linking or
 * tools. The research institute's board opens it at an idea; closing it returns to the screen it came from.
 */
public final class KnowledgeJournalViewer extends PrimaryLayer implements HidesSidebarButtons {
    private final KnowledgeLayer journal;
    @Nullable
    private final KnowledgeKey entry;
    private int lastWidth = -1, lastHeight = -1;

    private KnowledgeJournalViewer(@Nullable KnowledgeKey entry) {
        this.entry = entry;
        journal = new KnowledgeLayer(this, null, () -> closeGui(true), () -> {
        });
    }

    /** Opens the journal read-only, at {@code entry} when one is given. */
    public static void open(@Nullable KnowledgeKey entry) {
        CUIScreenWrapper.open(new KnowledgeJournalViewer(entry));
    }

    @Override
    public boolean onInit() {
        resizeToWindow();
        journal.refreshSnapshot();
        if (entry != null) journal.showEntry(entry);
        return super.onInit();
    }

    /** The same size the drawing desk gives its journal. */
    private void resizeToWindow() {
        int guiW = Minecraft.getInstance().getWindow().getGuiScaledWidth(), guiH = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        if (guiW == lastWidth && guiH == lastHeight) return;
        lastWidth = guiW;
        lastHeight = guiH;
        int width = Math.min(660, Math.max(296, guiW - 24)), height = Math.min(420, Math.max(210, guiH - 24));
        setSize(width, height);
        journal.setPos(0, 0);
        journal.resize(width, height);
    }

    @Override
    public void addChildUIElements() {
        add(journal);
    }

    @Override
    public void alignWidgets() {
    }

    @Override
    public void tick() {
        super.tick();
        resizeToWindow();
        // Picks up a new snapshot from the server; does nothing while it is unchanged.
        journal.refreshSnapshot();
    }

    @Override
    public void back() {
        if (!journal.goBack()) closeGui(true);
    }
}
