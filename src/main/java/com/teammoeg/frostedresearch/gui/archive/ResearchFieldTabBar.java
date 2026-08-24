/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedresearch.gui.archive;

import com.teammoeg.chorda.client.CInputHelper.Cursor;
import com.teammoeg.chorda.client.RenderingHint;
import com.teammoeg.chorda.client.cui.base.MouseButton;
import com.teammoeg.chorda.client.cui.base.TooltipBuilder;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.chorda.client.icon.CIcons.CIcon;
import com.teammoeg.frostedresearch.gui.TechIcons;
import com.teammoeg.frostedresearch.gui.archive.graph.ResearchTypeIdNormalizer;
import com.teammoeg.frostedresearch.research.ResearchCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.Objects;

/** Top-level research-field tabs following the original technology-tree layout. */
final class ResearchFieldTabBar extends UIElement {
    private static final ResearchCategory[] CATEGORIES = ResearchCategory.values();
    private static final int TAB_COUNT = CATEGORIES.length + 1;
    private static final int MAX_TAB_WIDTH = 40;
    private static final int ICON_SIZE = 16;
    private static final String[] TYPE_IDS = createTypeIds();
    private static final CIcon[] ICONS = createIcons();
    private static final Component TYPES_TITLE =
            Component.translatable("gui.frostedresearch.archive.types");
    private static final Component ALL_TYPES_TITLE =
            Component.translatable("gui.frostedresearch.archive.all_types");

    private final ResearchArchiveLayer archive;
    private final ResearchWorkspaceState state;

    ResearchFieldTabBar(ResearchArchiveLayer parent, ResearchWorkspaceState state) {
        super(parent);
        this.archive = parent;
        this.state = Objects.requireNonNull(state, "state");
    }

    @Override
    public void render(GuiGraphics graphics, int x, int y, int width, int height, RenderingHint hint) {
        graphics.fill(x, y, x + width, y + height, ResearchArchiveLayer.COLOR_PAPER_DARK);
        graphics.fill(x, y + height - 1, x + width, y + height, ResearchArchiveLayer.COLOR_PANEL_DARK);

        int tabWidth = tabWidth();
        for (int index = 0; index < TAB_COUNT; index++) {
            int tabX = x + index * tabWidth;
            boolean selected = TYPE_IDS[index].equals(state.researchTypeFilter());
            if (selected) {
                graphics.fill(tabX, y, tabX + tabWidth, y + height - 1, ResearchArchiveLayer.COLOR_PAPER);
                int highlightWidth = Math.min(30, tabWidth);
                TechIcons.TAB_HL.draw(graphics,
                        tabX + (tabWidth - highlightWidth) / 2, y, highlightWidth, 7);
            }
            int iconX = tabX + Math.max(1, (tabWidth - ICON_SIZE) / 2);
            int iconY = y + (selected ? 5 : 8);
            ICONS[index].draw(graphics, iconX, iconY, ICON_SIZE, ICON_SIZE);
        }

        int labelX = x + TAB_COUNT * tabWidth + 7;
        if (labelX + getFont().width(TYPES_TITLE) <= x + width - 4) {
            graphics.drawString(getFont(), TYPES_TITLE, labelX, y + 10,
                    ResearchArchiveLayer.COLOR_MUTED_INK, false);
        }
    }

    @Override
    public boolean onMousePressed(MouseButton button) {
        int index = tabAtMouse();
        if (button != MouseButton.LEFT || index < 0) {
            return false;
        }
        archive.setResearchTypeFilter(typeId(index));
        return true;
    }

    @Override
    public void getTooltip(TooltipBuilder tooltip) {
        int index = tabAtMouse();
        if (index < 0) {
            return;
        }
        if (index == 0) {
            tooltip.accept(ALL_TYPES_TITLE);
            return;
        }
        ResearchCategory category = CATEGORIES[index - 1];
        tooltip.accept(category.getName());
        tooltip.accept(category.getDesc().copy().withStyle(ChatFormatting.GRAY));
    }

    @Override
    public Cursor getCursor() {
        return tabAtMouse() >= 0 ? Cursor.HAND : null;
    }

    private int tabAtMouse() {
        if (!isMouseOver()) {
            return -1;
        }
        int index = (int) getMouseX() / tabWidth();
        return index >= 0 && index < TAB_COUNT ? index : -1;
    }

    private int tabWidth() {
        return Math.max(1, Math.min(MAX_TAB_WIDTH, getWidth() / TAB_COUNT));
    }

    private static String typeId(int index) {
        return TYPE_IDS[index];
    }

    private static String[] createTypeIds() {
        String[] typeIds = new String[TAB_COUNT];
        typeIds[0] = ResearchTypeIdNormalizer.ALL_TYPES;
        for (int index = 1; index < TAB_COUNT; index++) {
            typeIds[index] = ResearchTypeIdNormalizer.normalize(CATEGORIES[index - 1]);
        }
        return typeIds;
    }

    private static CIcon[] createIcons() {
        CIcon[] icons = new CIcon[TAB_COUNT];
        icons[0] = TechIcons.INF;
        for (int index = 1; index < TAB_COUNT; index++) {
            icons[index] = CIcons.getIcon(CATEGORIES[index - 1].getIcon());
        }
        return icons;
    }
}
