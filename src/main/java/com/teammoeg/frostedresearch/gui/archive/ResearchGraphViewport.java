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
import com.teammoeg.frostedresearch.FHResearch;
import com.teammoeg.frostedresearch.gui.archive.graph.ResearchGraphEdge;
import com.teammoeg.frostedresearch.gui.archive.graph.ResearchGraphLayout;
import com.teammoeg.frostedresearch.gui.archive.graph.ResearchGraphLayoutEngine;
import com.teammoeg.frostedresearch.gui.archive.graph.ResearchGraphProjection;
import com.teammoeg.frostedresearch.gui.archive.graph.ResearchGraphSnapshot;
import com.teammoeg.frostedresearch.research.Research;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Clipped, zoomable full research dependency graph. */
final class ResearchGraphViewport extends UIElement {
    private static final double NODE_WIDTH = ResearchGraphLayoutEngine.NODE_WIDTH;
    private static final double NODE_HEIGHT = ResearchGraphLayoutEngine.NODE_HEIGHT;
    private static final int TOOL_SIZE = 20;

    private final ResearchWorkspaceState state;
    private final ResearchNavigationController navigation;
    private final Runnable navigationChanged;
    private final ResearchGraphLayoutEngine layoutEngine = new ResearchGraphLayoutEngine();
    private final Set<String> fittedResearchTypes = new HashSet<>();
    private Map<String, Research> researchById = Map.of();
    private ResearchGraphSnapshot snapshot = ResearchGraphSnapshot.of(List.of(), 0L);
    private ResearchGraphLayout layout = layoutEngine.layout(snapshot);
    private ResearchGraphProjection projection = ResearchGraphProjection.forResearchType(snapshot, "*");
    private boolean dragging;

    ResearchGraphViewport(
            UIElement parent,
            ResearchWorkspaceState state,
            ResearchNavigationController navigation,
            Runnable navigationChanged) {
        super(parent);
        this.state = Objects.requireNonNull(state, "state");
        this.navigation = Objects.requireNonNull(navigation, "navigation");
        this.navigationChanged = Objects.requireNonNull(navigationChanged, "navigationChanged");
    }

    void setDefinitions(List<Research> definitions, long revision) {
        Map<String, Research> byId = new HashMap<>();
        definitions.forEach(research -> byId.put(research.getId(), research));
        researchById = Map.copyOf(byId);
        snapshot = ResearchGraphSnapshot.fromResearches(definitions, revision);
        layout = layoutEngine.layout(snapshot);
        projection = ResearchGraphProjection.forResearchType(snapshot, state.researchTypeFilter());
        fittedResearchTypes.clear();
        fitToVisible();
    }

    void onFilterChanged() {
        projection = ResearchGraphProjection.forResearchType(snapshot, state.researchTypeFilter());
        if (fittedResearchTypes.add(state.researchTypeFilter())) {
            fitToVisible();
        }
    }

    void onProgressChanged(@Nullable String researchId) {
        // Node status is derived from live synchronized research data during rendering.
    }

    @Override
    public void render(GuiGraphics graphics, int x, int y, int width, int height, RenderingHint hint) {
        graphics.fill(x, y, x + width, y + height, ResearchArchiveLayer.COLOR_GRAPH);
        graphics.enableScissor(x, y, x + width, y + height);
        ResearchWorkspaceState.Camera camera = state.camera(state.researchTypeFilter());
        drawGrid(graphics, x, y, width, height, camera);
        for (ResearchGraphEdge edge : projection.edges()) {
            drawEdge(graphics, edge, x, y, width, height, camera);
        }
        for (String id : projection.visibleNodeIds()) {
            drawNode(graphics, id, x, y, width, height, camera);
        }
        graphics.disableScissor();

        drawTool(graphics, x + width - TOOL_SIZE - 5, y + 5,
                Component.translatable("gui.frostedresearch.archive.fit"), isOverFitTool());
        drawTool(graphics, x + width - TOOL_SIZE * 2 - 8, y + 5,
                Component.translatable("gui.frostedresearch.archive.focus"), isOverFocusTool());
        Component zoom = Component.literal(Math.round(camera.zoom() * 100.0D) + "%");
        graphics.drawString(getFont(), zoom, x + 6, y + height - 13, 0xFFB8BDB4, false);
    }

    private void drawGrid(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            int height,
            ResearchWorkspaceState.Camera camera) {
        double step = ResearchGraphLayoutEngine.LANE_SPACING * camera.zoom();
        while (step < 18.0D) {
            step *= 2.0D;
        }
        double originX = x + width / 2.0D - camera.x() * camera.zoom();
        double originY = y + height / 2.0D - camera.y() * camera.zoom();
        int firstX = (int) Math.floor((x - originX) / step);
        int lastX = (int) Math.ceil((x + width - originX) / step);
        for (int i = firstX; i <= lastX; i++) {
            int lineX = (int) Math.round(originX + i * step);
            graphics.fill(lineX, y, lineX + 1, y + height, ResearchArchiveLayer.COLOR_GRAPH_GRID);
        }
        int firstY = (int) Math.floor((y - originY) / step);
        int lastY = (int) Math.ceil((y + height - originY) / step);
        for (int i = firstY; i <= lastY; i++) {
            int lineY = (int) Math.round(originY + i * step);
            graphics.fill(x, lineY, x + width, lineY + 1, ResearchArchiveLayer.COLOR_GRAPH_GRID);
        }
    }

    private void drawEdge(
            GuiGraphics graphics,
            ResearchGraphEdge edge,
            int x,
            int y,
            int width,
            int height,
            ResearchWorkspaceState.Camera camera) {
        ResearchGraphLayout.NodePosition parent = layout.positions().get(edge.parentId());
        ResearchGraphLayout.NodePosition child = layout.positions().get(edge.childId());
        if (parent == null || child == null) {
            return;
        }
        int x1 = worldToScreenX(parent.x() + NODE_WIDTH, x, width, camera);
        int y1 = worldToScreenY(parent.y() + NODE_HEIGHT / 2.0D, y, height, camera);
        int x2 = worldToScreenX(child.x(), x, width, camera);
        int y2 = worldToScreenY(child.y() + NODE_HEIGHT / 2.0D, y, height, camera);
        int midX = (x1 + x2) / 2;
        int color = projection.contextNodeIds().contains(edge.parentId())
                ? 0xFF626861 : 0xFF8D947F;
        horizontalLine(graphics, x1, midX, y1, color);
        verticalLine(graphics, midX, y1, y2, color);
        horizontalLine(graphics, midX, x2, y2, color);
    }

    private void drawNode(
            GuiGraphics graphics,
            String id,
            int x,
            int y,
            int width,
            int height,
            ResearchWorkspaceState.Camera camera) {
        ResearchGraphLayout.NodePosition position = layout.positions().get(id);
        Research research = researchById.get(id);
        if (position == null || research == null || research.isHidden()) {
            return;
        }
        int nodeX = worldToScreenX(position.x(), x, width, camera);
        int nodeY = worldToScreenY(position.y(), y, height, camera);
        int nodeWidth = scaledNodeLength(NODE_WIDTH, camera.zoom());
        int nodeHeight = scaledNodeLength(NODE_HEIGHT, camera.zoom());
        if (nodeX > x + width || nodeY > y + height || nodeX + nodeWidth < x || nodeY + nodeHeight < y) {
            return;
        }

        boolean selected = id.equals(state.selectedResearchId());
        boolean context = projection.contextNodeIds().contains(id);
        boolean searchMatch = matchesSearch(research);
        boolean dimmed = !state.searchQuery().isEmpty() && !searchMatch;
        int border = selected ? ResearchArchiveLayer.COLOR_RED
                : research.isCompleted() ? ResearchArchiveLayer.COLOR_TEAL
                : research.isInProgress() ? ResearchArchiveLayer.COLOR_GOLD
                : context ? 0xFF76796F : 0xFFAFA58E;
        int paper = context ? 0xFFD2C9B5 : ResearchArchiveLayer.COLOR_PAPER;
        if (dimmed) {
            border = 0xFF4F524D;
            paper = 0xFF77766D;
        }
        graphics.fill(nodeX, nodeY, nodeX + nodeWidth, nodeY + nodeHeight, border);
        int inset = camera.zoom() >= 0.45D ? 2 : 1;
        if (nodeWidth > inset * 2 && nodeHeight > inset * 2) {
            graphics.fill(nodeX + inset, nodeY + inset,
                    nodeX + nodeWidth - inset, nodeY + nodeHeight - inset, paper);
        }
        if (nodeWidth >= 7 && nodeHeight >= 5) {
            int stripeWidth = Math.max(1, Math.min(4, scaledNodeLength(4.0D, camera.zoom())));
            graphics.fill(nodeX + inset, nodeY + inset,
                    nodeX + inset + stripeWidth, nodeY + nodeHeight - inset, border);
        }

        int iconSize = scaledNodeIconLength(camera.zoom());
        int iconX = nodeX + Math.max(2, scaledNodeLength(10.0D, camera.zoom()));
        int iconY = nodeY + (nodeHeight - iconSize) / 2;
        research.getIcon().draw(graphics, iconX, iconY, iconSize, iconSize);

        String title = research.getName().getString();
        float textScale = nodeTextScale(camera.zoom());
        int textX = iconX + iconSize + Math.max(1, scaledNodeLength(4.0D, camera.zoom()));
        int textWidth = Math.max(2, nodeX + nodeWidth - inset - textX);
        int logicalTextWidth = Math.max(1, (int) Math.floor(textWidth / textScale));
        String visibleTitle = getFont().plainSubstrByWidth(title, logicalTextWidth);
        if (visibleTitle.isEmpty() && !title.isEmpty()) {
            visibleTitle = title.substring(0, title.offsetByCodePoints(0, 1));
        }
        int renderedTextHeight = Math.max(1, (int) Math.ceil(getFont().lineHeight * textScale));
        int textY = nodeY + (nodeHeight - renderedTextHeight) / 2;
        graphics.pose().pushPose();
        graphics.pose().translate(textX, textY, 0.0F);
        graphics.pose().scale(textScale, textScale, 1.0F);
        graphics.drawString(getFont(), visibleTitle, 0, 0,
                dimmed ? 0xFF43443F : ResearchArchiveLayer.COLOR_INK, false);
        graphics.pose().popPose();
    }

    private void drawTool(GuiGraphics graphics, int x, int y, Component label, boolean hovered) {
        graphics.fill(x, y, x + TOOL_SIZE, y + TOOL_SIZE,
                hovered ? ResearchArchiveLayer.COLOR_RED : 0xDD4A504B);
        String text = getFont().plainSubstrByWidth(label.getString(), TOOL_SIZE - 4);
        graphics.drawString(getFont(), text,
                x + (TOOL_SIZE - getFont().width(text)) / 2,
                y + 6, 0xFFF8F1DE, false);
    }

    @Override
    public boolean onMousePressed(MouseButton button) {
        if (!isMouseOver()) {
            return false;
        }
        if (button == MouseButton.LEFT && isOverFitTool()) {
            fitToVisible();
            return true;
        }
        if (button == MouseButton.LEFT && isOverFocusTool()) {
            focusSelected();
            return true;
        }
        Research research = nodeAtMouse();
        if (button == MouseButton.LEFT && research != null && ResearchArchiveLayer.canReveal(research)) {
            navigation.openResearch(research.getId());
            navigationChanged.run();
            return true;
        }
        if (button == MouseButton.MIDDLE || (button == MouseButton.LEFT && research == null)) {
            dragging = true;
            return true;
        }
        return research != null;
    }

    @Override
    public boolean onMouseDragged(MouseButton button, double dragX, double dragY) {
        if (!dragging || (button != MouseButton.LEFT && button != MouseButton.MIDDLE)) {
            return false;
        }
        ResearchWorkspaceState.Camera camera = state.camera(state.researchTypeFilter());
        state.setCamera(state.researchTypeFilter(), new ResearchWorkspaceState.Camera(
                camera.x() - dragX / camera.zoom(),
                camera.y() - dragY / camera.zoom(),
                camera.zoom()));
        fittedResearchTypes.add(state.researchTypeFilter());
        return true;
    }

    @Override
    public void onMouseReleased(MouseButton button) {
        dragging = false;
    }

    @Override
    public boolean onMouseScrolled(double scroll) {
        if (!isMouseOver() || scroll == 0.0D) {
            return false;
        }
        ResearchWorkspaceState.Camera camera = state.camera(state.researchTypeFilter());
        double oldZoom = camera.zoom();
        double newZoom = Math.max(ResearchWorkspaceState.MIN_ZOOM,
                Math.min(ResearchWorkspaceState.MAX_ZOOM, oldZoom * Math.pow(1.12D, scroll)));
        double mouseWorldX = camera.x() + (getMouseX() - getWidth() / 2.0D) / oldZoom;
        double mouseWorldY = camera.y() + (getMouseY() - getHeight() / 2.0D) / oldZoom;
        state.setCamera(state.researchTypeFilter(), new ResearchWorkspaceState.Camera(
                mouseWorldX - (getMouseX() - getWidth() / 2.0D) / newZoom,
                mouseWorldY - (getMouseY() - getHeight() / 2.0D) / newZoom,
                newZoom));
        fittedResearchTypes.add(state.researchTypeFilter());
        return true;
    }

    @Override
    public void getTooltip(TooltipBuilder tooltip) {
        Research research = nodeAtMouse();
        if (research != null && ResearchArchiveLayer.canReveal(research)) {
            tooltip.accept(research.getName());
            if (FHResearch.editor) {
                tooltip.accept(Component.literal(research.getId()));
            }
            tooltip.accept(research.getCategory().getName());
        }
    }

    @Override
    public Cursor getCursor() {
        if (dragging) {
            return Cursor.MOVE;
        }
        return nodeAtMouse() != null || isOverFitTool() || isOverFocusTool() ? Cursor.HAND : null;
    }

    private void fitToVisible() {
        if (projection.visibleNodeIds().isEmpty() || getWidth() <= 0 || getHeight() <= 0) {
            state.setCamera(state.researchTypeFilter(), ResearchWorkspaceState.Camera.DEFAULT);
            return;
        }
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        for (String id : projection.visibleNodeIds()) {
            ResearchGraphLayout.NodePosition position = layout.positions().get(id);
            if (position == null) {
                continue;
            }
            minX = Math.min(minX, position.x());
            minY = Math.min(minY, position.y());
            maxX = Math.max(maxX, position.x() + NODE_WIDTH);
            maxY = Math.max(maxY, position.y() + NODE_HEIGHT);
        }
        if (!Double.isFinite(minX)) {
            return;
        }
        state.setCamera(state.researchTypeFilter(), new ResearchWorkspaceState.Camera(
                (minX + maxX) / 2.0D, (minY + maxY) / 2.0D,
                ResearchWorkspaceState.MIN_ZOOM));
        fittedResearchTypes.add(state.researchTypeFilter());
    }

    private void focusSelected() {
        ResearchGraphLayout.NodePosition position = layout.positions().get(state.selectedResearchId());
        if (position == null) {
            fitToVisible();
            return;
        }
        ResearchWorkspaceState.Camera camera = state.camera(state.researchTypeFilter());
        state.setCamera(state.researchTypeFilter(), new ResearchWorkspaceState.Camera(
                position.x() + NODE_WIDTH / 2.0D,
                position.y() + NODE_HEIGHT / 2.0D,
                camera.zoom()));
    }

    @Nullable
    private Research nodeAtMouse() {
        if (!isMouseOver() || isOverFitTool() || isOverFocusTool()) {
            return null;
        }
        ResearchWorkspaceState.Camera camera = state.camera(state.researchTypeFilter());
        double worldX = camera.x() + (getMouseX() - getWidth() / 2.0D) / camera.zoom();
        double worldY = camera.y() + (getMouseY() - getHeight() / 2.0D) / camera.zoom();
        for (String id : projection.visibleNodeIds()) {
            ResearchGraphLayout.NodePosition position = layout.positions().get(id);
            if (position != null && worldX >= position.x() && worldX <= position.x() + NODE_WIDTH
                    && worldY >= position.y() && worldY <= position.y() + NODE_HEIGHT) {
                return researchById.get(id);
            }
        }
        return null;
    }

    private boolean matchesSearch(Research research) {
        String query = state.searchQuery().toLowerCase(Locale.ROOT);
        return query.isEmpty()
                || research.getId().toLowerCase(Locale.ROOT).contains(query)
                || research.getName().getString().toLowerCase(Locale.ROOT).contains(query);
    }

    private boolean isOverFitTool() {
        return getMouseX() >= getWidth() - TOOL_SIZE - 5 && getMouseX() <= getWidth() - 5
                && getMouseY() >= 5 && getMouseY() <= 5 + TOOL_SIZE;
    }

    private boolean isOverFocusTool() {
        return getMouseX() >= getWidth() - TOOL_SIZE * 2 - 8
                && getMouseX() <= getWidth() - TOOL_SIZE - 8
                && getMouseY() >= 5 && getMouseY() <= 5 + TOOL_SIZE;
    }

    private int worldToScreenX(
            double worldX, int viewportX, int viewportWidth, ResearchWorkspaceState.Camera camera) {
        return (int) Math.round(viewportX + viewportWidth / 2.0D
                + (worldX - camera.x()) * camera.zoom());
    }

    private int worldToScreenY(
            double worldY, int viewportY, int viewportHeight, ResearchWorkspaceState.Camera camera) {
        return (int) Math.round(viewportY + viewportHeight / 2.0D
                + (worldY - camera.y()) * camera.zoom());
    }

    private static void horizontalLine(GuiGraphics graphics, int x1, int x2, int y, int color) {
        graphics.fill(Math.min(x1, x2), y, Math.max(x1, x2) + 1, y + 1, color);
    }

    private static void verticalLine(GuiGraphics graphics, int x, int y1, int y2, int color) {
        graphics.fill(x, Math.min(y1, y2), x + 1, Math.max(y1, y2) + 1, color);
    }

    static int scaledNodeLength(double worldLength, double zoom) {
        return Math.max(1, (int) Math.round(worldLength * zoom));
    }

    static int scaledNodeIconLength(double zoom) {
        return Math.max(4, Math.min(16, scaledNodeLength(16.0D, zoom)));
    }

    static float nodeTextScale(double zoom) {
        return (float) Math.max(0.25D, Math.min(1.0D, zoom));
    }
}
