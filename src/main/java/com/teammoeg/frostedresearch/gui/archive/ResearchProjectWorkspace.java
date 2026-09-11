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
    private static final ResearchWorkspaceState.ProjectTab[] PROJECT_TABS =
            ResearchWorkspaceState.ProjectTab.values();
    private static final Component SELECT_PROJECT =
            Component.translatable("gui.frostedresearch.archive.select_project");
    private static final Component COMPLETED =
            Component.translatable("gui.frostedresearch.archive.completed");
    private static final Component IN_PROGRESS =
            Component.translatable("gui.frostedresearch.research.in_progress");
    private static final Component CAN_RESEARCH =
            Component.translatable("gui.frostedresearch.research.can_research");
    private static final Component LOCKED =
            Component.translatable("gui.frostedresearch.archive.locked");
    private static final Component SUMMARY =
            Component.translatable("gui.frostedresearch.archive.summary");
    private static final Component CLUES =
            Component.translatable("gui.frostedresearch.research.clues");
    private static final Component EFFECTS =
            Component.translatable("gui.frostedresearch.research.effects");
    private static final Component RESEARCH_MATERIALS =
            Component.translatable("gui.frostedresearch.archive.research_materials");
    private static final Component NO_TASKS =
            Component.translatable("gui.frostedresearch.archive.no_tasks");
    private static final Component BACK_HINT =
            Component.translatable("gui.frostedresearch.archive.back_hint");
    private static final Component EXPERIMENT_EMPTY =
            Component.translatable("gui.frostedresearch.archive.experiment_empty");
    private static final Component CLAIM_REWARDS =
            Component.translatable("gui.frostedresearch.research.claim_rewards");
    private static final Component COMMIT_AND_START =
            Component.translatable("gui.frostedresearch.research.commit_material_and_start");
    private static final Component STOP =
            Component.translatable("gui.frostedresearch.research.stop");
    private static final Component START =
            Component.translatable("gui.frostedresearch.research.start");
    private static final Component[] TAB_TITLES = createTabTitles();
    private static final Component[] DESTINATION_LABELS = createDestinationLabels();

    private final ResearchOpenContext openContext;
    private final ResearchWorkspaceState state;
    private final ResearchArchiveViewCache viewCache;
    private final ResearchNavigationController navigation;
    private final Runnable navigationChanged;
    private Map<String, Research> researchById = Map.of();
    private int scroll;
    @Nullable
    private WorkspaceRenderSnapshot cachedRenderSnapshot;
    private boolean renderSnapshotDirty = true;

    ResearchProjectWorkspace(
            UIElement parent,
            ResearchOpenContext openContext,
            ResearchWorkspaceState state,
            ResearchArchiveViewCache viewCache,
            ResearchNavigationController navigation,
            Runnable navigationChanged) {
        super(parent);
        this.openContext = Objects.requireNonNull(openContext, "openContext");
        this.state = Objects.requireNonNull(state, "state");
        this.viewCache = Objects.requireNonNull(viewCache, "viewCache");
        this.navigation = Objects.requireNonNull(navigation, "navigation");
        this.navigationChanged = Objects.requireNonNull(navigationChanged, "navigationChanged");
    }

    void setDefinitions(List<Research> definitions) {
        Map<String, Research> byId = new HashMap<>();
        definitions.forEach(research -> byId.put(research.getId(), research));
        researchById = Map.copyOf(byId);
        scroll = 0;
        invalidateRenderSnapshot();
        retainSelectedClue();
    }

    void onProgressChanged(@Nullable String researchId) {
        if (researchId == null || researchId.equals(state.selectedResearchId())) {
            invalidateRenderSnapshot();
            retainSelectedClue();
        }
    }

    void onActiveResearchChanged(@Nullable String researchId) {
        invalidateRenderSnapshot();
        retainSelectedClue();
    }

    void onClueProgressChanged(String researchId, String clueNonce) {
        if (researchId.equals(state.selectedResearchId())) {
            invalidateRenderSnapshot();
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

        if (research == null) {
            graphics.drawString(getFont(), SELECT_PROJECT,
                    x + 12, y + 13, ResearchArchiveLayer.COLOR_MUTED_INK, false);
            drawCloseButton(graphics, x, y, width);
            return;
        }
        WorkspaceRenderSnapshot renderSnapshot = renderSnapshotFor(research);
        scroll = Math.min(scroll, maxScroll(renderSnapshot));

        research.getIcon().draw(graphics, x + 9, y + 9, 18, 18);
        graphics.drawString(getFont(), renderSnapshot.header.title, x + 33, y + 9,
                ResearchArchiveLayer.COLOR_INK, false);
        graphics.drawString(getFont(), renderSnapshot.header.category, x + 33, y + 20,
                ResearchArchiveLayer.COLOR_MUTED_INK, false);
        drawBookmarkButton(graphics, x, y, width, research);
        drawCloseButton(graphics, x, y, width);

        drawTabs(graphics, renderSnapshot.tabs, x, y + HEADER_HEIGHT, width);
        int contentTop = y + HEADER_HEIGHT + TAB_HEIGHT;
        int contentBottom = y + height - FOOTER_HEIGHT;
        graphics.enableScissor(x + 5, contentTop, x + width - 5, contentBottom);
        switch (state.projectTab()) {
            case DETAIL -> drawDetail(
                    graphics, renderSnapshot.progress, renderSnapshot.detail,
                    x + 10, contentTop + 8 - scroll, width - 20);
            case THEORY -> drawClues(
                    graphics, renderSnapshot.theory, x + 7, contentTop + 5 - scroll, width - 14);
            case EXPERIMENT -> drawExperimentEmpty(
                    graphics, renderSnapshot.experiment, x + 11, contentTop + 10);
        }
        graphics.disableScissor();
        drawFooter(graphics, renderSnapshot.footer, x, y + height - FOOTER_HEIGHT, width);
    }

    private void drawTabs(GuiGraphics graphics, List<TabRenderData> tabs, int x, int y, int width) {
        int tabWidth = width / PROJECT_TABS.length;
        for (int i = 0; i < PROJECT_TABS.length; i++) {
            int tabX = x + i * tabWidth;
            boolean selected = PROJECT_TABS[i] == state.projectTab();
            graphics.fill(tabX, y, i == PROJECT_TABS.length - 1 ? x + width : tabX + tabWidth,
                    y + TAB_HEIGHT,
                    selected ? ResearchArchiveLayer.COLOR_TEAL : ResearchArchiveLayer.COLOR_PAPER_DARK);
            TabRenderData tab = tabs.get(i);
            graphics.drawString(getFont(), tab.title, tabX + tab.xOffset, y + 6,
                    selected ? 0xFFF8F1DE : ResearchArchiveLayer.COLOR_INK, false);
        }
    }

    private void drawDetail(
            GuiGraphics graphics,
            ProgressRenderData progress,
            DetailTabRenderData detail,
            int x,
            int y,
            int width) {
        graphics.drawString(getFont(), progress.status, x, y, progress.statusColor, false);
        y += 14;

        graphics.fill(x, y, x + width, y + 7, ResearchArchiveLayer.COLOR_PANEL_DARK);
        graphics.fill(x, y, x + Math.round(width * progress.fraction), y + 7,
                ResearchArchiveLayer.COLOR_TEAL);
        graphics.drawString(getFont(), progress.points, x, y + 10,
                ResearchArchiveLayer.COLOR_MUTED_INK, false);
        y += 25;

        graphics.drawString(getFont(), SUMMARY, x, y, ResearchArchiveLayer.COLOR_RED, false);
        y += 13;
        for (List<FormattedCharSequence> paragraph : detail.descriptionLines) {
            for (FormattedCharSequence line : paragraph) {
                graphics.drawString(getFont(), line, x, y, ResearchArchiveLayer.COLOR_INK, false);
                y += 11;
            }
            y += 5;
        }

        if (detail.materialsHeight > 0) {
            y += 3;
            y = drawMaterials(graphics, detail, x, y);
        }

        if (!detail.clueRows.isEmpty()) {
            y += 3;
            graphics.drawString(getFont(), CLUES, x, y, ResearchArchiveLayer.COLOR_RED, false);
            y += 14;
            for (ClueRowRenderData row : detail.clueRows) {
                drawClueRow(graphics, row, x, y, width);
                y += CLUE_ROW_HEIGHT;
            }
        }

        if (!detail.effects.isEmpty()) {
            y += 3;
            graphics.drawString(getFont(), EFFECTS, x, y, ResearchArchiveLayer.COLOR_RED, false);
            y += 14;
            for (EffectRenderData effect : detail.effects) {
                effect.effect.getIcon().draw(graphics, x, y, 16, 16);
                graphics.drawString(getFont(), effect.title, x + 21, y + 4, effect.color, false);
                y += 20;
            }
        }
    }

    private void drawClues(
            GuiGraphics graphics, TheoryTabRenderData theory, int x, int y, int width) {
        for (int index = 0; index < theory.clueRows.size(); index++) {
            ClueRowRenderData row = theory.clueRows.get(index);
            int rowY = y + index * CLUE_ROW_HEIGHT;
            drawClueRow(graphics, row, x, rowY, width);
        }
        if (theory.clueRows.isEmpty()) {
            graphics.drawString(getFont(), NO_TASKS, x + 4, y + 5,
                    ResearchArchiveLayer.COLOR_MUTED_INK, false);
        }
    }

    private void drawExperimentEmpty(
            GuiGraphics graphics, ExperimentTabRenderData experiment, int x, int y) {
        for (FormattedCharSequence line : experiment.lines) {
            graphics.drawString(getFont(), line, x, y, ResearchArchiveLayer.COLOR_MUTED_INK, false);
            y += 11;
        }
    }

    private int drawMaterials(GuiGraphics graphics, DetailTabRenderData detail, int x, int y) {
        graphics.drawString(getFont(), RESEARCH_MATERIALS, x, y, ResearchArchiveLayer.COLOR_RED, false);
        for (MaterialRenderData material : detail.materials) {
            int itemX = x + material.xOffset;
            int itemY = y + material.yOffset;
            graphics.renderItem(material.stack, itemX, itemY);
            graphics.drawString(getFont(), material.count, x + material.countXOffset, itemY + 10,
                    0xFFFFFFFF, true);
        }
        return y + detail.materialsHeight;
    }

    private void drawClueRow(GuiGraphics graphics, ClueRowRenderData row, int x, int y, int width) {
        ResearchClueView view = row.view;
        boolean selected = view.nonce().equals(state.selectedClueNonce());
        int background = selected ? 0xFFDBCEAD : 0xFFD7CCB2;
        graphics.fill(x, y, x + width, y + CLUE_ROW_HEIGHT - 3, background);
        graphics.fill(x, y, x + 3, y + CLUE_ROW_HEIGHT - 3,
                view.completed() ? ResearchArchiveLayer.COLOR_TEAL
                        : view.required() ? ResearchArchiveLayer.COLOR_RED
                        : ResearchArchiveLayer.COLOR_GOLD);
        graphics.drawString(getFont(), view.completed() ? "[x]" : "[ ]", x + 7, y + 6,
                view.completed() ? ResearchArchiveLayer.COLOR_TEAL : ResearchArchiveLayer.COLOR_INK, false);
        graphics.drawString(getFont(), row.title, x + 27, y + 6,
                view.completed() ? ResearchArchiveLayer.COLOR_MUTED_INK : ResearchArchiveLayer.COLOR_INK, false);
        if (row.description != null) {
            graphics.drawString(getFont(), row.description, x + 27, y + 19,
                    ResearchArchiveLayer.COLOR_MUTED_INK, false);
        }
        if (row.action != null) {
            graphics.drawString(getFont(), row.action, x + 27, y + 30, row.actionColor, false);
        }
    }

    private void drawFooter(
            GuiGraphics graphics, FooterRenderData footer, int x, int y, int width) {
        graphics.fill(x, y, x + width, y + FOOTER_HEIGHT, ResearchArchiveLayer.COLOR_PANEL_DARK);
        Action action = footer.action;
        if (action == null) {
            graphics.drawString(getFont(), BACK_HINT, x + 8, y + 10,
                    ResearchArchiveLayer.COLOR_MUTED_INK, false);
            return;
        }
        int color = action.operator == FHResearchControlPacket.Operator.PAUSE
                ? ResearchArchiveLayer.COLOR_RED : ResearchArchiveLayer.COLOR_TEAL;
        graphics.fill(x + 7, y + 4, x + width - 7, y + FOOTER_HEIGHT - 4, color);
        int titleX = x + Math.max(9, (width - footer.titleWidth) / 2);
        graphics.drawString(getFont(), footer.title, titleX, y + 10, 0xFFF8F1DE, false);
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
            int index = Math.min(PROJECT_TABS.length - 1,
                    localX * PROJECT_TABS.length / Math.max(1, getWidth()));
            state.setProjectTab(PROJECT_TABS[index]);
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
        scroll = research == null ? 0 : Math.min(next, maxScroll(renderSnapshotFor(research)));
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
        Action action = renderSnapshotFor(research).footer.action;
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
    private Action actionFor(
            Research research,
            ResearchData data,
            boolean completed,
            boolean active,
            boolean unlocked) {
        if (hasUnclaimedReward(research, data, completed)) {
            return new Action(CLAIM_REWARDS, null, true);
        }
        if (!data.canResearch() && unlocked) {
            return new Action(COMMIT_AND_START,
                    FHResearchControlPacket.Operator.COMMIT_ITEM, false);
        }
        if (active) {
            return new Action(STOP,
                    FHResearchControlPacket.Operator.PAUSE, false);
        }
        if (!completed && !research.isInCompletable()) {
            return new Action(START,
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
        WorkspaceRenderSnapshot renderSnapshot = renderSnapshotFor(research);
        int localY = (int) getMouseY();
        ResearchWorkspaceState.ProjectTab tab = state.projectTab();
        if (tab == ResearchWorkspaceState.ProjectTab.EXPERIMENT) {
            return null;
        }
        int top = tab == ResearchWorkspaceState.ProjectTab.DETAIL
                ? HEADER_HEIGHT + TAB_HEIGHT + 8 + renderSnapshot.detail.cluesOffset
                : HEADER_HEIGHT + TAB_HEIGHT + 5;
        int relativeY = localY - top + scroll;
        if (localY < HEADER_HEIGHT + TAB_HEIGHT
                || localY >= getHeight() - FOOTER_HEIGHT
                || relativeY < 0) {
            return null;
        }
        int index = relativeY / CLUE_ROW_HEIGHT;
        List<ClueRowRenderData> rows = tab == ResearchWorkspaceState.ProjectTab.DETAIL
                ? renderSnapshot.detail.clueRows : renderSnapshot.theory.clueRows;
        return index < rows.size() ? rows.get(index).view : null;
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

    private static int materialsHeight(int itemCount, int width) {
        int columns = Math.max(1, width / 21);
        int rows = Math.max(1, (itemCount + columns - 1) / columns);
        return 17 + rows * 20;
    }

    private int maxScroll(WorkspaceRenderSnapshot renderSnapshot) {
        int viewportHeight = Math.max(0, getHeight() - HEADER_HEIGHT - TAB_HEIGHT - FOOTER_HEIGHT);
        return Math.max(0, contentHeight(renderSnapshot) - viewportHeight);
    }

    private int contentHeight(WorkspaceRenderSnapshot renderSnapshot) {
        return switch (state.projectTab()) {
            case DETAIL -> renderSnapshot.detail.contentHeight;
            case THEORY -> renderSnapshot.theory.contentHeight;
            case EXPERIMENT -> renderSnapshot.experiment.contentHeight;
        };
    }

    private WorkspaceRenderSnapshot renderSnapshotFor(Research research) {
        int width = Math.max(1, getWidth());
        long presentationRevision = viewCache.presentationRevision();
        long stateRevision = viewCache.stateRevision();
        if (!renderSnapshotDirty && cachedRenderSnapshot != null
                && cachedRenderSnapshot.cacheKey.matches(
                        research.getId(), width, presentationRevision, stateRevision, FHResearch.editor)) {
            return cachedRenderSnapshot;
        }

        ResearchArchiveViewCache.View cachedView = viewCache.view(research.getId());
        ResearchData data = research.getData();
        ProjectState projectState = snapshotProjectState(research, data, cachedView);
        List<ResearchClueView> clueViews = buildClueViews(research, data);

        cachedRenderSnapshot = new WorkspaceRenderSnapshot(
                new WorkspaceRenderKey(
                        research.getId(), width, presentationRevision, stateRevision, FHResearch.editor),
                buildHeaderRenderData(research, cachedView, width),
                buildProgressRenderData(research, data, projectState),
                buildTabs(width),
                buildDetailTabRenderData(research, data, projectState, clueViews, width),
                buildTheoryTabRenderData(clueViews, width),
                buildExperimentTabRenderData(width),
                buildFooterRenderData(research, data, projectState, width));
        renderSnapshotDirty = false;
        return cachedRenderSnapshot;
    }

    private ProjectState snapshotProjectState(
            Research research,
            ResearchData data,
            @Nullable ResearchArchiveViewCache.View cachedView) {
        if (cachedView != null) {
            return new ProjectState(
                    cachedView.completed(), cachedView.active(), cachedView.unlocked(), cachedView.progress());
        }
        float progress = data.getProgress(research);
        float clampedProgress = Float.isFinite(progress)
                ? Math.max(0.0F, Math.min(1.0F, progress))
                : 0.0F;
        return new ProjectState(
                data.isCompleted(), research.isInProgress(), research.isUnlocked(), clampedProgress);
    }

    private HeaderRenderData buildHeaderRenderData(
            Research research,
            @Nullable ResearchArchiveViewCache.View cachedView,
            int width) {
        int headerTextWidth = Math.max(1, width - 94);
        String title = getFont().plainSubstrByWidth(
                cachedView == null ? research.getName().getString() : cachedView.title(),
                headerTextWidth);
        String category = getFont().plainSubstrByWidth(
                cachedView == null ? research.getCategory().getName().getString() : cachedView.categoryTitle(),
                headerTextWidth);
        return new HeaderRenderData(title, category);
    }

    private ProgressRenderData buildProgressRenderData(
            Research research, ResearchData data, ProjectState projectState) {
        Component status = projectState.completed
                ? COMPLETED
                : projectState.active ? IN_PROGRESS : projectState.unlocked ? CAN_RESEARCH : LOCKED;
        int statusColor = projectState.completed ? ResearchArchiveLayer.COLOR_TEAL
                : projectState.active ? ResearchArchiveLayer.COLOR_GOLD
                : projectState.unlocked ? ResearchArchiveLayer.COLOR_RED
                : ResearchArchiveLayer.COLOR_MUTED_INK;
        long committedPoints = data.getTotalCommitted(research);
        Component points = Component.translatable(
                "gui.frostedresearch.archive.points", committedPoints, research.getRequiredPoints());
        return new ProgressRenderData(status, statusColor, projectState.progress, points);
    }

    private List<ResearchClueView> buildClueViews(Research research, ResearchData data) {
        Research current = activeResearch();
        return ResearchClueViewFactory.create(
                research,
                data,
                new ResearchClueViewFactory.Context(
                        openContext.mode(),
                        current == null ? null : current.getId(),
                        currentTheoryClueNonce(current)));
    }

    private DetailTabRenderData buildDetailTabRenderData(
            Research research,
            ResearchData data,
            ProjectState projectState,
            List<ResearchClueView> clueViews,
            int width) {
        int detailWidth = Math.max(1, width - 20);
        List<Component> description = research.getDesc();
        List<List<FormattedCharSequence>> descriptionLines = new ArrayList<>(description.size());
        int cluesOffset = 14 + 25 + 13;
        for (Component paragraph : description) {
            List<FormattedCharSequence> lines = List.copyOf(getFont().split(paragraph, detailWidth));
            descriptionLines.add(lines);
            cluesOffset += lines.size() * 11 + 5;
        }
        List<Pair<Ingredient, Integer>> requiredItems = research.getRequiredItems();
        int materialHeight = 0;
        List<MaterialRenderData> materials = List.of();
        if (!requiredItems.isEmpty()) {
            materialHeight = materialsHeight(requiredItems.size(), detailWidth);
            materials = buildMaterials(requiredItems, detailWidth);
            cluesOffset += 3 + materialHeight;
        }
        List<ClueRowRenderData> clueRows = buildClueRows(
                viewsForTab(clueViews, ResearchWorkspaceState.ProjectTab.DETAIL), detailWidth);
        if (!clueRows.isEmpty()) {
            cluesOffset += 3 + 14;
        }

        List<EffectRenderData> effects = buildEffects(
                research, data, projectState.completed, detailWidth);
        int contentHeight = 8 + cluesOffset + clueRows.size() * CLUE_ROW_HEIGHT;
        if (!effects.isEmpty()) {
            contentHeight += 3 + 14 + effects.size() * 20;
        }
        return new DetailTabRenderData(
                List.copyOf(descriptionLines),
                materials,
                materialHeight,
                clueRows,
                effects,
                cluesOffset,
                contentHeight);
    }

    private TheoryTabRenderData buildTheoryTabRenderData(
            List<ResearchClueView> clueViews, int width) {
        List<ClueRowRenderData> clueRows = buildClueRows(
                viewsForTab(clueViews, ResearchWorkspaceState.ProjectTab.THEORY),
                Math.max(1, width - 14));
        int contentHeight = 5 + Math.max(11, clueRows.size() * CLUE_ROW_HEIGHT);
        return new TheoryTabRenderData(clueRows, contentHeight);
    }

    private ExperimentTabRenderData buildExperimentTabRenderData(int width) {
        List<FormattedCharSequence> lines = List.copyOf(
                getFont().split(EXPERIMENT_EMPTY, Math.max(1, width - 22)));
        return new ExperimentTabRenderData(lines, 10 + lines.size() * 11);
    }

    private FooterRenderData buildFooterRenderData(
            Research research, ResearchData data, ProjectState projectState, int width) {
        Action action = actionFor(
                research, data, projectState.completed, projectState.active, projectState.unlocked);
        String actionTitle = action == null ? null : getFont().plainSubstrByWidth(
                action.title.getString(), Math.max(8, width - 22));
        return new FooterRenderData(
                action, actionTitle, actionTitle == null ? 0 : getFont().width(actionTitle));
    }

    private void invalidateRenderSnapshot() {
        renderSnapshotDirty = true;
    }

    @Nullable
    private Research selectedResearch() {
        String selectedResearchId = state.selectedResearchId();
        return selectedResearchId == null ? null : researchById.get(selectedResearchId);
    }

    @Nullable
    private Research activeResearch() {
        String activeResearchId = viewCache.activeResearchId();
        if (activeResearchId == null) {
            return null;
        }
        Research current = researchById.get(activeResearchId);
        return current == null ? FHResearch.getResearch(activeResearchId) : current;
    }

    private List<MaterialRenderData> buildMaterials(
            List<Pair<Ingredient, Integer>> requiredItems, int width) {
        int columns = Math.max(1, width / 21);
        List<MaterialRenderData> materials = new ArrayList<>(requiredItems.size());
        for (int index = 0; index < requiredItems.size(); index++) {
            Pair<Ingredient, Integer> required = requiredItems.get(index);
            ItemStack[] items = required.getFirst().getItems();
            if (items.length == 0) {
                continue;
            }
            int xOffset = index % columns * 21;
            int yOffset = 14 + index / columns * 20;
            String count = String.valueOf(required.getSecond());
            materials.add(new MaterialRenderData(
                    items[0], count, xOffset, yOffset, xOffset + 13 - getFont().width(count)));
        }
        return List.copyOf(materials);
    }

    private List<ClueRowRenderData> buildClueRows(List<ResearchClueView> views, int width) {
        List<ClueRowRenderData> rows = new ArrayList<>(views.size());
        int titleWidth = Math.max(1, width - 41);
        int bodyWidth = Math.max(1, width - 34);
        for (ResearchClueView view : views) {
            String title = getFont().plainSubstrByWidth(view.title().getString(), titleWidth);
            String description = view.description() == null ? null
                    : getFont().plainSubstrByWidth(view.description().getString(), bodyWidth);
            Component destination = destinationLabel(view.destination());
            String action = null;
            int actionColor = ResearchArchiveLayer.COLOR_RED;
            if (destination != null) {
                action = getFont().plainSubstrByWidth(destination.getString(), bodyWidth);
            } else if (view.contribution() > 0.0F) {
                action = "+" + Math.round(view.contribution() * 100.0F) + "%";
                actionColor = ResearchArchiveLayer.COLOR_TEAL;
            }
            rows.add(new ClueRowRenderData(view, title, description, action, actionColor));
        }
        return List.copyOf(rows);
    }

    private List<EffectRenderData> buildEffects(
            Research research, ResearchData data, boolean completed, int width) {
        List<EffectRenderData> effects = new ArrayList<>(research.getEffects().size());
        for (Effect effect : research.getEffects()) {
            if (effect.isHidden() || (research.isHideEffects() && !completed && !FHResearch.editor)) {
                continue;
            }
            String title = getFont().plainSubstrByWidth(effect.getName(research).getString(), width - 22);
            int color = data.isEffectGranted(effect)
                    ? ResearchArchiveLayer.COLOR_MUTED_INK : ResearchArchiveLayer.COLOR_INK;
            effects.add(new EffectRenderData(effect, title, color));
        }
        return List.copyOf(effects);
    }

    private List<TabRenderData> buildTabs(int width) {
        int tabWidth = width / PROJECT_TABS.length;
        List<TabRenderData> tabs = new ArrayList<>(PROJECT_TABS.length);
        for (Component title : TAB_TITLES) {
            tabs.add(new TabRenderData(title, Math.max(3, (tabWidth - getFont().width(title)) / 2)));
        }
        return List.copyOf(tabs);
    }

    private static boolean hasUnclaimedReward(Research research, ResearchData data, boolean completed) {
        if (!completed) {
            return false;
        }
        for (Effect effect : research.getEffects()) {
            if (!data.isEffectGranted(effect)) {
                return true;
            }
        }
        return false;
    }

    private static Component[] createTabTitles() {
        Component[] titles = new Component[PROJECT_TABS.length];
        for (int i = 0; i < PROJECT_TABS.length; i++) {
            titles[i] = Component.translatable("gui.frostedresearch.archive.tab."
                    + PROJECT_TABS[i].name().toLowerCase(java.util.Locale.ROOT));
        }
        return titles;
    }

    private static Component[] createDestinationLabels() {
        Component[] labels = new Component[ClueDestination.values().length];
        labels[ClueDestination.ITEM_EXAMINE.ordinal()] =
                Component.translatable("gui.frostedresearch.archive.action.examine");
        labels[ClueDestination.THEORY_GAME.ordinal()] =
                Component.translatable("gui.frostedresearch.archive.action.theory");
        labels[ClueDestination.WORLD.ordinal()] =
                Component.translatable("gui.frostedresearch.archive.action.world");
        labels[ClueDestination.DETAILS.ordinal()] =
                Component.translatable("gui.frostedresearch.archive.action.details");
        labels[ClueDestination.DRAWING_DESK_REQUIRED.ordinal()] =
                Component.translatable("gui.frostedresearch.archive.action.desk_required");
        labels[ClueDestination.START_RESEARCH_REQUIRED.ordinal()] =
                Component.translatable("gui.frostedresearch.archive.action.start_required");
        labels[ClueDestination.PREVIOUS_THEORY_REQUIRED.ordinal()] =
                Component.translatable("gui.frostedresearch.archive.action.previous_theory");
        return labels;
    }

    @Nullable
    private static Component destinationLabel(ClueDestination destination) {
        return DESTINATION_LABELS[destination.ordinal()];
    }

    private record Action(
            Component title,
            @Nullable FHResearchControlPacket.Operator operator,
            boolean claimEffects) {
    }

    private record MaterialRenderData(
            ItemStack stack,
            String count,
            int xOffset,
            int yOffset,
            int countXOffset) {
    }

    private record ClueRowRenderData(
            ResearchClueView view,
            String title,
            @Nullable String description,
            @Nullable String action,
            int actionColor) {
    }

    private record EffectRenderData(Effect effect, String title, int color) {
    }

    private record TabRenderData(Component title, int xOffset) {
    }

    private record WorkspaceRenderKey(
            String researchId,
            int width,
            long presentationRevision,
            long stateRevision,
            boolean editor) {

        private boolean matches(
                String currentResearchId,
                int currentWidth,
                long currentPresentationRevision,
                long currentStateRevision,
                boolean currentEditor) {
            return researchId.equals(currentResearchId)
                    && width == currentWidth
                    && presentationRevision == currentPresentationRevision
                    && stateRevision == currentStateRevision
                    && editor == currentEditor;
        }
    }

    private record ProjectState(
            boolean completed,
            boolean active,
            boolean unlocked,
            float progress) {
    }

    private record HeaderRenderData(String title, String category) {
    }

    private record ProgressRenderData(
            Component status,
            int statusColor,
            float fraction,
            Component points) {
    }

    private record DetailTabRenderData(
            List<List<FormattedCharSequence>> descriptionLines,
            List<MaterialRenderData> materials,
            int materialsHeight,
            List<ClueRowRenderData> clueRows,
            List<EffectRenderData> effects,
            int cluesOffset,
            int contentHeight) {
    }

    private record TheoryTabRenderData(
            List<ClueRowRenderData> clueRows,
            int contentHeight) {
    }

    private record ExperimentTabRenderData(
            List<FormattedCharSequence> lines,
            int contentHeight) {
    }

    private record FooterRenderData(
            @Nullable Action action,
            @Nullable String title,
            int titleWidth) {
    }

    /** Immutable display-ready snapshot, grouped by the dialog regions that consume it. */
    private record WorkspaceRenderSnapshot(
            WorkspaceRenderKey cacheKey,
            HeaderRenderData header,
            ProgressRenderData progress,
            List<TabRenderData> tabs,
            DetailTabRenderData detail,
            TheoryTabRenderData theory,
            ExperimentTabRenderData experiment,
            FooterRenderData footer) {
    }
}
