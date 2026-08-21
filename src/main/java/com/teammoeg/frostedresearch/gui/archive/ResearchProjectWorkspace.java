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

import com.mojang.datafixers.util.Pair;
import com.teammoeg.chorda.client.CInputHelper.Cursor;
import com.teammoeg.chorda.client.RenderingHint;
import com.teammoeg.chorda.client.cui.base.MouseButton;
import com.teammoeg.chorda.client.cui.base.TooltipBuilder;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.frostedresearch.FHResearch;
import com.teammoeg.frostedresearch.FRNetwork;
import com.teammoeg.frostedresearch.api.ClientResearchDataAPI;
import com.teammoeg.frostedresearch.data.ResearchData;
import com.teammoeg.frostedresearch.gui.TechIcons;
import com.teammoeg.frostedresearch.gui.archive.clue.ClueDestination;
import com.teammoeg.frostedresearch.gui.archive.clue.ResearchClueView;
import com.teammoeg.frostedresearch.gui.archive.clue.ResearchClueViewFactory;
import com.teammoeg.frostedresearch.network.FHEffectTriggerPacket;
import com.teammoeg.frostedresearch.network.FHResearchControlPacket;
import com.teammoeg.frostedresearch.research.Research;
import com.teammoeg.frostedresearch.research.clues.Clue;
import com.teammoeg.frostedresearch.research.clues.MinigameClue;
import com.teammoeg.frostedresearch.research.effects.Effect;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Project detail, theory, and experiment workspace shown beside or over the graph. */
final class ResearchProjectWorkspace extends UIElement {
    private static final int HEADER_HEIGHT = 35;
    private static final int TAB_HEIGHT = 20;
    private static final int FOOTER_HEIGHT = 27;
    private static final int CLUE_ROW_HEIGHT = 43;

    private final ResearchOpenContext openContext;
    private final ResearchWorkspaceState state;
    private final ResearchNavigationController navigation;
    private final Runnable navigationChanged;
    private Map<String, Research> researchById = Map.of();
    private int scroll;
    @Nullable
    private String cachedPresentationResearchId;
    private int cachedPresentationWidth = -1;
    private boolean presentationDirty = true;
    private List<ResearchClueView> cachedClueViews = List.of();
    private List<ResearchClueView> cachedDetailViews = List.of();
    private List<ResearchClueView> cachedTheoryViews = List.of();
    private List<List<FormattedCharSequence>> cachedDescriptionLines = List.of();
    private List<FormattedCharSequence> cachedExperimentLines = List.of();
    private int cachedDetailCluesOffset;
    private int cachedDetailContentHeight;
    private int cachedTheoryContentHeight;
    private int cachedExperimentContentHeight;

    ResearchProjectWorkspace(
            UIElement parent,
            ResearchOpenContext openContext,
            ResearchWorkspaceState state,
            ResearchNavigationController navigation,
            Runnable navigationChanged) {
        super(parent);
        this.openContext = Objects.requireNonNull(openContext, "openContext");
        this.state = Objects.requireNonNull(state, "state");
        this.navigation = Objects.requireNonNull(navigation, "navigation");
        this.navigationChanged = Objects.requireNonNull(navigationChanged, "navigationChanged");
    }

    void setDefinitions(List<Research> definitions) {
        Map<String, Research> byId = new HashMap<>();
        definitions.forEach(research -> byId.put(research.getId(), research));
        researchById = Map.copyOf(byId);
        scroll = 0;
        invalidatePresentation();
        retainSelectedClue();
    }

    void onProgressChanged(@Nullable String researchId) {
        if (researchId == null || researchId.equals(state.selectedResearchId())) {
            invalidatePresentation();
            retainSelectedClue();
        }
    }

    void onActiveResearchChanged(@Nullable String researchId) {
        invalidatePresentation();
        retainSelectedClue();
    }

    void onClueProgressChanged(String researchId, String clueNonce) {
        if (researchId.equals(state.selectedResearchId())) {
            invalidatePresentation();
            retainSelectedClue();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int x, int y, int width, int height, RenderingHint hint) {
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 600);
        try {
            renderWorkspace(graphics, x, y, width, height);
        } finally {
            graphics.pose().popPose();
        }
    }

    private void renderWorkspace(GuiGraphics graphics, int x, int y, int width, int height) {
        Research research = selectedResearch();
        ResearchArchiveLayer archive = (ResearchArchiveLayer) getParent();
        int archiveX = x - getX();
        int archiveY = y - getY();
        graphics.fill(archiveX, archiveY,
                archiveX + archive.getWidth(), archiveY + archive.getHeight(),
                0x990C0D0C);
        TechIcons.DIALOG.draw(graphics, x, y, width, height);

        if (research == null || !ResearchArchiveLayer.isDefinitionVisible(research)) {
            graphics.drawString(getFont(), Component.translatable("gui.frostedresearch.archive.select_project"),
                    x + 12, y + 13, ResearchArchiveLayer.COLOR_MUTED_INK, false);
            drawCloseButton(graphics, x, y, width);
            return;
        }
        ensurePresentation(research);
        scroll = Math.min(scroll, maxScroll(research));

        research.getIcon().draw(graphics, x + 9, y + 9, 18, 18);
        String title = getFont().plainSubstrByWidth(research.getName().getString(), width - 94);
        graphics.drawString(getFont(), title, x + 33, y + 9, ResearchArchiveLayer.COLOR_INK, false);
        String category = getFont().plainSubstrByWidth(research.getCategory().getName().getString(), width - 94);
        graphics.drawString(getFont(), category, x + 33, y + 20,
                ResearchArchiveLayer.COLOR_MUTED_INK, false);
        drawBookmarkButton(graphics, x, y, width, research);
        drawCloseButton(graphics, x, y, width);

        drawTabs(graphics, x, y + HEADER_HEIGHT, width);
        int contentTop = y + HEADER_HEIGHT + TAB_HEIGHT;
        int contentBottom = y + height - FOOTER_HEIGHT;
        graphics.enableScissor(x + 5, contentTop, x + width - 5, contentBottom);
        switch (state.projectTab()) {
            case DETAIL -> drawDetail(graphics, research, x + 10, contentTop + 8 - scroll, width - 20);
            case THEORY -> drawClues(graphics, research, x + 7, contentTop + 5 - scroll, width - 14);
            case EXPERIMENT -> drawExperimentEmpty(graphics, x + 11, contentTop + 10, width - 22);
        }
        graphics.disableScissor();
        drawFooter(graphics, research, x, y + height - FOOTER_HEIGHT, width);
    }

    private void drawTabs(GuiGraphics graphics, int x, int y, int width) {
        ResearchWorkspaceState.ProjectTab[] tabs = ResearchWorkspaceState.ProjectTab.values();
        int tabWidth = width / tabs.length;
        for (int i = 0; i < tabs.length; i++) {
            int tabX = x + i * tabWidth;
            boolean selected = tabs[i] == state.projectTab();
            graphics.fill(tabX, y, i == tabs.length - 1 ? x + width : tabX + tabWidth,
                    y + TAB_HEIGHT,
                    selected ? ResearchArchiveLayer.COLOR_TEAL : ResearchArchiveLayer.COLOR_PAPER_DARK);
            Component title = tabTitle(tabs[i]);
            int titleX = tabX + Math.max(3, (tabWidth - getFont().width(title)) / 2);
            graphics.drawString(getFont(), title, titleX, y + 6,
                    selected ? 0xFFF8F1DE : ResearchArchiveLayer.COLOR_INK, false);
        }
    }

    private void drawDetail(GuiGraphics graphics, Research research, int x, int y, int width) {
        ResearchData data = research.getData();
        Component status = research.isCompleted()
                ? Component.translatable("gui.frostedresearch.archive.completed")
                : research.isInProgress()
                ? Component.translatable("gui.frostedresearch.research.in_progress")
                : research.isUnlocked()
                ? Component.translatable("gui.frostedresearch.research.can_research")
                : Component.translatable("gui.frostedresearch.archive.locked");
        int statusColor = research.isCompleted() ? ResearchArchiveLayer.COLOR_TEAL
                : research.isInProgress() ? ResearchArchiveLayer.COLOR_GOLD
                : research.isUnlocked() ? ResearchArchiveLayer.COLOR_RED
                : ResearchArchiveLayer.COLOR_MUTED_INK;
        graphics.drawString(getFont(), status, x, y, statusColor, false);
        y += 14;

        float progress = Math.max(0.0F, Math.min(1.0F, research.getProgressFraction()));
        graphics.fill(x, y, x + width, y + 7, ResearchArchiveLayer.COLOR_PANEL_DARK);
        graphics.fill(x, y, x + Math.round(width * progress), y + 7, ResearchArchiveLayer.COLOR_TEAL);
        Component points = Component.translatable("gui.frostedresearch.archive.points",
                data.getTotalCommitted(research), research.getRequiredPoints());
        graphics.drawString(getFont(), points, x, y + 10, ResearchArchiveLayer.COLOR_MUTED_INK, false);
        y += 25;

        graphics.drawString(getFont(), Component.translatable("gui.frostedresearch.archive.summary"),
                x, y, ResearchArchiveLayer.COLOR_RED, false);
        y += 13;
        for (List<FormattedCharSequence> paragraph : cachedDescriptionLines) {
            for (FormattedCharSequence line : paragraph) {
                graphics.drawString(getFont(), line, x, y, ResearchArchiveLayer.COLOR_INK, false);
                y += 11;
            }
            y += 5;
        }

        if (!research.getRequiredItems().isEmpty()) {
            y += 3;
            y = drawMaterials(graphics, research, x, y, width);
        }

        List<ResearchClueView> detailViews = cachedDetailViews;
        if (!detailViews.isEmpty()) {
            y += 3;
            graphics.drawString(getFont(), Component.translatable("gui.frostedresearch.research.clues"),
                    x, y, ResearchArchiveLayer.COLOR_RED, false);
            y += 14;
            for (ResearchClueView view : detailViews) {
                drawClueRow(graphics, view, x, y, width);
                y += CLUE_ROW_HEIGHT;
            }
        }

        if (!research.getEffects().isEmpty()) {
            y += 3;
            graphics.drawString(getFont(), Component.translatable("gui.frostedresearch.research.effects"),
                    x, y, ResearchArchiveLayer.COLOR_RED, false);
            y += 14;
            for (Effect effect : research.getEffects()) {
                if (effect.isHidden() || (research.isHideEffects() && !research.isCompleted() && !FHResearch.editor)) {
                    continue;
                }
                effect.getIcon().draw(graphics, x, y, 16, 16);
                String name = getFont().plainSubstrByWidth(effect.getName(research).getString(), width - 22);
                graphics.drawString(getFont(), name, x + 21, y + 4,
                        data.isEffectGranted(effect) ? ResearchArchiveLayer.COLOR_MUTED_INK
                                : ResearchArchiveLayer.COLOR_INK, false);
                y += 20;
            }
        }
    }

    private void drawClues(GuiGraphics graphics, Research research, int x, int y, int width) {
        List<ResearchClueView> views = cachedTheoryViews;
        for (int index = 0; index < views.size(); index++) {
            ResearchClueView view = views.get(index);
            int rowY = y + index * CLUE_ROW_HEIGHT;
            drawClueRow(graphics, view, x, rowY, width);
        }
        if (views.isEmpty()) {
            graphics.drawString(getFont(), Component.translatable("gui.frostedresearch.archive.no_tasks"),
                    x + 4, y + 5, ResearchArchiveLayer.COLOR_MUTED_INK, false);
        }
    }

    private void drawExperimentEmpty(GuiGraphics graphics, int x, int y, int width) {
        for (FormattedCharSequence line : cachedExperimentLines) {
            graphics.drawString(getFont(), line, x, y, ResearchArchiveLayer.COLOR_MUTED_INK, false);
            y += 11;
        }
    }

    private int drawMaterials(GuiGraphics graphics, Research research, int x, int y, int width) {
        if (research.getRequiredItems().isEmpty()) {
            return y;
        }
        graphics.drawString(getFont(), Component.translatable("gui.frostedresearch.archive.research_materials"),
                x, y, ResearchArchiveLayer.COLOR_RED, false);
        int columns = Math.max(1, width / 21);
        int index = 0;
        for (Pair<Ingredient, Integer> required : research.getRequiredItems()) {
            ItemStack[] items = required.getFirst().getItems();
            if (items.length == 0) {
                index++;
                continue;
            }
            int itemX = x + index % columns * 21;
            int itemY = y + 14 + index / columns * 20;
            graphics.renderItem(items[0], itemX, itemY);
            String count = String.valueOf(required.getSecond());
            graphics.drawString(getFont(), count, itemX + 13 - getFont().width(count), itemY + 10,
                    0xFFFFFFFF, true);
            index++;
        }
        return y + materialsHeight(research, width);
    }

    private void drawClueRow(GuiGraphics graphics, ResearchClueView view, int x, int y, int width) {
        boolean selected = view.nonce().equals(state.selectedClueNonce());
        int background = selected ? 0xFFDBCEAD : 0xFFD7CCB2;
        graphics.fill(x, y, x + width, y + CLUE_ROW_HEIGHT - 3, background);
        graphics.fill(x, y, x + 3, y + CLUE_ROW_HEIGHT - 3,
                view.completed() ? ResearchArchiveLayer.COLOR_TEAL
                        : view.required() ? ResearchArchiveLayer.COLOR_RED
                        : ResearchArchiveLayer.COLOR_GOLD);
        graphics.drawString(getFont(), view.completed() ? "[x]" : "[ ]", x + 7, y + 6,
                view.completed() ? ResearchArchiveLayer.COLOR_TEAL : ResearchArchiveLayer.COLOR_INK, false);
        String title = getFont().plainSubstrByWidth(view.title().getString(), width - 41);
        graphics.drawString(getFont(), title, x + 27, y + 6,
                view.completed() ? ResearchArchiveLayer.COLOR_MUTED_INK : ResearchArchiveLayer.COLOR_INK, false);
        if (view.description() != null) {
            String description = getFont().plainSubstrByWidth(view.description().getString(), width - 34);
            graphics.drawString(getFont(), description, x + 27, y + 19,
                    ResearchArchiveLayer.COLOR_MUTED_INK, false);
        }
        Component destination = destinationLabel(view.destination());
        if (destination != null) {
            String action = getFont().plainSubstrByWidth(destination.getString(), width - 34);
            graphics.drawString(getFont(), action, x + 27, y + 30,
                    ResearchArchiveLayer.COLOR_RED, false);
        } else if (view.contribution() > 0.0F) {
            int percent = Math.round(view.contribution() * 100.0F);
            graphics.drawString(getFont(), "+" + percent + "%", x + 27, y + 30,
                    ResearchArchiveLayer.COLOR_TEAL, false);
        }
    }

    private void drawFooter(GuiGraphics graphics, Research research, int x, int y, int width) {
        graphics.fill(x, y, x + width, y + FOOTER_HEIGHT, ResearchArchiveLayer.COLOR_PANEL_DARK);
        Action action = actionFor(research);
        if (action == null) {
            Component hint = Component.translatable("gui.frostedresearch.archive.back_hint");
            graphics.drawString(getFont(), hint, x + 8, y + 10,
                    ResearchArchiveLayer.COLOR_MUTED_INK, false);
            return;
        }
        int color = action.operator == FHResearchControlPacket.Operator.PAUSE
                ? ResearchArchiveLayer.COLOR_RED : ResearchArchiveLayer.COLOR_TEAL;
        graphics.fill(x + 7, y + 4, x + width - 7, y + FOOTER_HEIGHT - 4, color);
        String actionTitle = getFont().plainSubstrByWidth(action.title.getString(), Math.max(8, width - 22));
        int titleX = x + Math.max(9, (width - getFont().width(actionTitle)) / 2);
        graphics.drawString(getFont(), actionTitle, titleX, y + 10, 0xFFF8F1DE, false);
    }

    private void drawBookmarkButton(GuiGraphics graphics, int x, int y, int width, Research research) {
        boolean bookmarked = state.bookmarkedResearchIds().contains(research.getId());
        graphics.fill(x + width - 49, y + 8, x + width - 30, y + 27,
                bookmarked ? ResearchArchiveLayer.COLOR_RED : ResearchArchiveLayer.COLOR_PAPER_DARK);
        graphics.drawString(getFont(), "*", x + width - 43, y + 14,
                bookmarked ? 0xFFF8F1DE : ResearchArchiveLayer.COLOR_INK, false);
    }

    private void drawCloseButton(GuiGraphics graphics, int x, int y, int width) {
        graphics.fill(x + width - 26, y + 8, x + width - 7, y + 27,
                ResearchArchiveLayer.COLOR_PAPER_DARK);
        graphics.drawString(getFont(), "x", x + width - 19, y + 14,
                ResearchArchiveLayer.COLOR_INK, false);
    }

    @Override
    public boolean onMousePressed(MouseButton button) {
        if (!isMouseOver() || button != MouseButton.LEFT) {
            return false;
        }
        Research research = selectedResearch();
        int localX = (int) getMouseX();
        int localY = (int) getMouseY();
        if (localY >= 8 && localY <= 27 && localX >= getWidth() - 26) {
            state.setProjectWorkspaceOpen(false);
            navigationChanged.run();
            return true;
        }
        if (research != null && localY >= 8 && localY <= 27
                && localX >= getWidth() - 49 && localX < getWidth() - 30) {
            boolean bookmarked = state.bookmarkedResearchIds().contains(research.getId());
            state.setBookmarked(research.getId(), !bookmarked);
            return true;
        }
        if (localY >= HEADER_HEIGHT && localY < HEADER_HEIGHT + TAB_HEIGHT) {
            int index = Math.min(ResearchWorkspaceState.ProjectTab.values().length - 1,
                    localX * ResearchWorkspaceState.ProjectTab.values().length / Math.max(1, getWidth()));
            state.setProjectTab(ResearchWorkspaceState.ProjectTab.values()[index]);
            state.selectClue(null);
            scroll = 0;
            return true;
        }
        if (research != null && localY >= getHeight() - FOOTER_HEIGHT) {
            triggerAction(research);
            return true;
        }
        if (research != null && state.projectTab() != ResearchWorkspaceState.ProjectTab.EXPERIMENT) {
            ResearchClueView view = clueAtMouse(research);
            if (view != null) {
                state.selectClue(view.nonce());
                followDestination(view.destination());
            }
            return true;
        }
        return true;
    }

    @Override
    public boolean onMouseScrolled(double amount) {
        if (!isMouseOver()) {
            return false;
        }
        Research research = selectedResearch();
        int next = Math.max(0, scroll - (int) Math.signum(amount) * 28);
        scroll = research == null ? 0 : Math.min(next, maxScroll(research));
        return true;
    }

    @Override
    public void getTooltip(TooltipBuilder tooltip) {
        Research research = selectedResearch();
        if (research == null) {
            return;
        }
        ResearchClueView view = clueAtMouse(research);
        if (view != null) {
            tooltip.accept(view.title());
            if (view.hint() != null) {
                tooltip.accept(view.hint());
            }
        }
    }

    @Override
    public Cursor getCursor() {
        return isMouseOver() ? Cursor.HAND : null;
    }

    private void triggerAction(Research research) {
        Action action = actionFor(research);
        if (action == null) {
            return;
        }
        if (action.claimEffects) {
            FRNetwork.INSTANCE.sendToServer(new FHEffectTriggerPacket(research));
        } else {
            FRNetwork.INSTANCE.sendToServer(new FHResearchControlPacket(action.operator, research));
        }
    }

    @Nullable
    private Action actionFor(Research research) {
        ResearchData data = research.getData();
        if (research.hasUnclaimedReward()) {
            return new Action(Component.translatable("gui.frostedresearch.research.claim_rewards"), null, true);
        }
        if (!data.canResearch() && research.isUnlocked()) {
            return new Action(Component.translatable(
                    "gui.frostedresearch.research.commit_material_and_start"),
                    FHResearchControlPacket.Operator.COMMIT_ITEM, false);
        }
        if (research.isInProgress()) {
            return new Action(Component.translatable("gui.frostedresearch.research.stop"),
                    FHResearchControlPacket.Operator.PAUSE, false);
        }
        if (!data.isCompleted() && !research.isInCompletable()) {
            return new Action(Component.translatable("gui.frostedresearch.research.start"),
                    FHResearchControlPacket.Operator.START, false);
        }
        return null;
    }

    private void followDestination(ClueDestination destination) {
        switch (destination) {
            case ITEM_EXAMINE -> {
                navigation.goToDrawingDesk(ResearchWorkspaceState.DrawDeskFocusTarget.ITEM_EXAMINE);
                navigationChanged.run();
            }
            case THEORY_GAME -> {
                navigation.goToDrawingDesk(ResearchWorkspaceState.DrawDeskFocusTarget.THEORY_GAME);
                navigationChanged.run();
            }
            case WORLD -> navigation.returnToWorld();
            default -> {
                // Informational and blocked destinations remain in the current workspace.
            }
        }
    }

    @Nullable
    private ResearchClueView clueAtMouse(Research research) {
        ensurePresentation(research);
        int localY = (int) getMouseY();
        ResearchWorkspaceState.ProjectTab tab = state.projectTab();
        if (tab == ResearchWorkspaceState.ProjectTab.EXPERIMENT) {
            return null;
        }
        int top = tab == ResearchWorkspaceState.ProjectTab.DETAIL
                ? HEADER_HEIGHT + TAB_HEIGHT + 8 + detailCluesOffset(research, getWidth() - 20)
                : HEADER_HEIGHT + TAB_HEIGHT + 5;
        int relativeY = localY - top + scroll;
        if (localY < HEADER_HEIGHT + TAB_HEIGHT
                || localY >= getHeight() - FOOTER_HEIGHT
                || relativeY < 0) {
            return null;
        }
        int index = relativeY / CLUE_ROW_HEIGHT;
        List<ResearchClueView> filtered = tab == ResearchWorkspaceState.ProjectTab.DETAIL
                ? cachedDetailViews : cachedTheoryViews;
        return index < filtered.size() ? filtered.get(index) : null;
    }

    @Nullable
    private String currentTheoryClueNonce(@Nullable Research current) {
        if (current == null) {
            return null;
        }
        ResearchData data = current.getData();
        for (Clue clue : current.getClues()) {
            if (clue instanceof MinigameClue && !data.isClueTriggered(clue)) {
                return clue.getNonce();
            }
        }
        return null;
    }

    private void retainSelectedClue() {
        Research research = selectedResearch();
        if (research == null) {
            state.retainClues(List.of());
            return;
        }
        List<String> nonces = new ArrayList<>();
        research.getClues().forEach(clue -> nonces.add(clue.getNonce()));
        state.retainClues(nonces);
    }

    static List<ResearchClueView> viewsForTab(
            List<ResearchClueView> views, ResearchWorkspaceState.ProjectTab tab) {
        if (tab == ResearchWorkspaceState.ProjectTab.EXPERIMENT) {
            return List.of();
        }
        return views.stream()
                .filter(view -> !view.systemClue()
                        && (tab == ResearchWorkspaceState.ProjectTab.DETAIL || view.tab() == tab))
                .toList();
    }

    private int detailCluesOffset(Research research, int width) {
        ensurePresentation(research);
        return cachedDetailCluesOffset;
    }

    private static int materialsHeight(Research research, int width) {
        int columns = Math.max(1, width / 21);
        int rows = Math.max(1, (research.getRequiredItems().size() + columns - 1) / columns);
        return 17 + rows * 20;
    }

    private int maxScroll(Research research) {
        int viewportHeight = Math.max(0, getHeight() - HEADER_HEIGHT - TAB_HEIGHT - FOOTER_HEIGHT);
        return Math.max(0, contentHeight(research) - viewportHeight);
    }

    private int contentHeight(Research research) {
        ensurePresentation(research);
        return switch (state.projectTab()) {
            case DETAIL -> cachedDetailContentHeight;
            case THEORY -> cachedTheoryContentHeight;
            case EXPERIMENT -> cachedExperimentContentHeight;
        };
    }

    private void ensurePresentation(Research research) {
        int width = Math.max(1, getWidth());
        if (!presentationDirty
                && research.getId().equals(cachedPresentationResearchId)
                && cachedPresentationWidth == width) {
            return;
        }

        Research current = ClientResearchDataAPI.getData().get().getCurrentResearch().get();
        cachedClueViews = ResearchClueViewFactory.create(
                research,
                research.getData(),
                new ResearchClueViewFactory.Context(
                        openContext.mode(),
                        current == null ? null : current.getId(),
                        currentTheoryClueNonce(current)));
        cachedDetailViews = viewsForTab(cachedClueViews, ResearchWorkspaceState.ProjectTab.DETAIL);
        cachedTheoryViews = viewsForTab(cachedClueViews, ResearchWorkspaceState.ProjectTab.THEORY);

        int detailWidth = Math.max(1, width - 20);
        List<List<FormattedCharSequence>> descriptionLines = new ArrayList<>(research.getDesc().size());
        int detailOffset = 14 + 25 + 13;
        for (Component paragraph : research.getDesc()) {
            List<FormattedCharSequence> lines = List.copyOf(getFont().split(paragraph, detailWidth));
            descriptionLines.add(lines);
            detailOffset += lines.size() * 11 + 5;
        }
        cachedDescriptionLines = List.copyOf(descriptionLines);
        if (!research.getRequiredItems().isEmpty()) {
            detailOffset += 3 + materialsHeight(research, detailWidth);
        }
        if (!cachedDetailViews.isEmpty()) {
            detailOffset += 3 + 14;
        }
        cachedDetailCluesOffset = detailOffset;

        int detailHeight = 8 + detailOffset + cachedDetailViews.size() * CLUE_ROW_HEIGHT;
        if (!research.getEffects().isEmpty()) {
            detailHeight += 3 + 14;
            for (Effect effect : research.getEffects()) {
                if (!effect.isHidden()
                        && (!research.isHideEffects() || research.isCompleted() || FHResearch.editor)) {
                    detailHeight += 20;
                }
            }
        }
        cachedDetailContentHeight = detailHeight;
        cachedTheoryContentHeight = 5 + Math.max(11, cachedTheoryViews.size() * CLUE_ROW_HEIGHT);
        cachedExperimentLines = List.copyOf(getFont().split(
                Component.translatable("gui.frostedresearch.archive.experiment_empty"),
                Math.max(1, width - 22)));
        cachedExperimentContentHeight = 10 + cachedExperimentLines.size() * 11;
        cachedPresentationResearchId = research.getId();
        cachedPresentationWidth = width;
        presentationDirty = false;
    }

    private void invalidatePresentation() {
        presentationDirty = true;
    }

    @Nullable
    private Research selectedResearch() {
        return researchById.get(state.selectedResearchId());
    }

    private Component tabTitle(ResearchWorkspaceState.ProjectTab tab) {
        return Component.translatable("gui.frostedresearch.archive.tab."
                + tab.name().toLowerCase(java.util.Locale.ROOT));
    }

    @Nullable
    private Component destinationLabel(ClueDestination destination) {
        return switch (destination) {
            case NONE -> null;
            case ITEM_EXAMINE -> Component.translatable("gui.frostedresearch.archive.action.examine");
            case THEORY_GAME -> Component.translatable("gui.frostedresearch.archive.action.theory");
            case WORLD -> Component.translatable("gui.frostedresearch.archive.action.world");
            case DETAILS -> Component.translatable("gui.frostedresearch.archive.action.details");
            case DRAWING_DESK_REQUIRED -> Component.translatable("gui.frostedresearch.archive.action.desk_required");
            case START_RESEARCH_REQUIRED -> Component.translatable("gui.frostedresearch.archive.action.start_required");
            case PREVIOUS_THEORY_REQUIRED -> Component.translatable("gui.frostedresearch.archive.action.previous_theory");
        };
    }

    private record Action(
            Component title,
            @Nullable FHResearchControlPacket.Operator operator,
            boolean claimEffects) {
    }
}
