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
import com.teammoeg.chorda.client.TesselateHelper;
import com.teammoeg.chorda.client.TesselateHelper.ShapeTesslator;
import com.teammoeg.chorda.client.cui.base.MouseButton;
import com.teammoeg.chorda.client.cui.base.PanZoomViewport;
import com.teammoeg.chorda.client.cui.base.PanZoomViewport.Camera;
import com.teammoeg.chorda.client.cui.base.PanZoomViewport.CameraChange;
import com.teammoeg.chorda.client.cui.base.PanZoomViewport.ScreenSegment;
import com.teammoeg.chorda.client.cui.base.PanZoomViewport.WorldBounds;
import com.teammoeg.chorda.client.cui.base.TooltipBuilder;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.chorda.client.cui.widgets.Button;
import com.teammoeg.chorda.client.icon.FlatIcon;
import com.teammoeg.frostedresearch.FHResearch;
import com.teammoeg.frostedresearch.gui.archive.graph.ResearchGraphEdge;
import com.teammoeg.frostedresearch.gui.archive.graph.ResearchGraphLayout;
import com.teammoeg.frostedresearch.gui.archive.graph.ResearchGraphLayoutEngine;
import com.teammoeg.frostedresearch.gui.archive.graph.ResearchGraphProjection;
import com.teammoeg.frostedresearch.gui.archive.graph.ResearchGraphSnapshot;
import com.teammoeg.frostedresearch.research.Research;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Clipped, zoomable full research dependency graph. */
final class ResearchGraphViewport extends PanZoomViewport {
    private static final double NODE_WIDTH = ResearchGraphLayoutEngine.NODE_WIDTH;
    private static final double NODE_HEIGHT = ResearchGraphLayoutEngine.NODE_HEIGHT;
    private static final int TOOL_SIZE = 20;

    private final ResearchWorkspaceState state;
    private final ResearchNavigationController navigation;
    private final Runnable navigationChanged;
    private final ResearchGraphLayoutEngine layoutEngine = new ResearchGraphLayoutEngine();
    private final Set<String> fittedResearchTypes = new HashSet<>();
    private final Button fitButton;
    private final Button focusButton;
    private Map<String, Research> researchById = Map.of();
    private ResearchGraphSnapshot snapshot = ResearchGraphSnapshot.of(List.of(), 0L);
    private ResearchGraphLayout layout = layoutEngine.layout(snapshot);
    private ResearchGraphProjection projection = ResearchGraphProjection.forResearchType(snapshot, "*");
    private Set<String> searchMatches = Set.of();
    private String cachedSearchQuery = "";
    private int projectionBuildCount;
    private final ScreenSegment clippedSegment = new ScreenSegment();

    ResearchGraphViewport(
            UIElement parent,
            ResearchWorkspaceState state,
            ResearchNavigationController navigation,
            Runnable navigationChanged) {
        super(parent, ResearchWorkspaceState.MIN_ZOOM, ResearchWorkspaceState.MAX_ZOOM);
        this.state = Objects.requireNonNull(state, "state");
        this.navigation = Objects.requireNonNull(navigation, "navigation");
        this.navigationChanged = Objects.requireNonNull(navigationChanged, "navigationChanged");
        this.fitButton = new Button(
                this,
                Component.translatable("gui.frostedresearch.archive.fit"),
                FlatIcon.BOX.toCIcon()) {
            @Override
            public void onClicked(MouseButton button) {
                if (button == MouseButton.LEFT) {
                    fitToVisible();
                }
            }
        };
        this.focusButton = new Button(
                this,
                Component.translatable("gui.frostedresearch.archive.focus"),
                FlatIcon.SIGHT.toCIcon()) {
            @Override
            public void onClicked(MouseButton button) {
                if (button == MouseButton.LEFT) {
                    focusSelected();
                }
            }
        };
        setCamera(state.camera(state.researchTypeFilter()));
    }

    @Override
    public void addUIElements() {
        add(fitButton);
        add(focusButton);
    }

    @Override
    public void alignWidgets() {
        fitButton.setPosAndSize(getWidth() - TOOL_SIZE - 5, 5, TOOL_SIZE, TOOL_SIZE);
        focusButton.setPosAndSize(getWidth() - TOOL_SIZE * 2 - 8, 5, TOOL_SIZE, TOOL_SIZE);
    }

    void setDefinitions(List<Research> definitions, long revision) {
        Map<String, Research> byId = new HashMap<>();
        definitions.forEach(research -> byId.put(research.getId(), research));
        researchById = Map.copyOf(byId);
        snapshot = ResearchGraphSnapshot.fromResearches(definitions, revision);
        layout = layoutEngine.layout(snapshot);
        rebuildProjection();
        rebuildSearchMatches();
        fittedResearchTypes.clear();
        fitToVisible();
    }

    void onResearchTypeChanged() {
        rebuildProjection();
        setCamera(state.camera(state.researchTypeFilter()));
        if (!fittedResearchTypes.contains(state.researchTypeFilter())) {
            fitToVisible();
        }
    }

    void onSearchChanged() {
        rebuildSearchMatches();
    }

    void resizeViewport(int x, int y, int width, int height) {
        setPosAndSize(x, y, width, height);
        alignWidgets();
        if (!fittedResearchTypes.contains(state.researchTypeFilter())) {
            fitToVisible();
        }
    }

    private void rebuildProjection() {
        projection = ResearchGraphProjection.forResearchType(snapshot, state.researchTypeFilter());
        projectionBuildCount++;
    }

    private void rebuildSearchMatches() {
        cachedSearchQuery = state.searchQuery().toLowerCase(Locale.ROOT);
        if (cachedSearchQuery.isEmpty()) {
            searchMatches = Set.of();
            return;
        }
        Set<String> matches = new HashSet<>();
        for (Research research : researchById.values()) {
            if (research.getId().toLowerCase(Locale.ROOT).contains(cachedSearchQuery)
                    || research.getName().getString().toLowerCase(Locale.ROOT).contains(cachedSearchQuery)) {
                matches.add(research.getId());
            }
        }
        searchMatches = Set.copyOf(matches);
    }

    void onProgressChanged(@Nullable String researchId) {
        // Node status is derived from live synchronized research data during rendering.
    }

    @Override
    protected void drawViewportContent(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            int height,
            Camera camera,
            RenderingHint hint) {
        List<NodeRenderData> renderedNodes = collectRenderedNodes(x, y, width, height, camera);
        Matrix4f matrix = graphics.pose().last().pose();

        try (ShapeTesslator shapes = TesselateHelper.getShapeTesslator()) {
            shapes.fillRect(matrix, x, y, x + width, y + height, ResearchArchiveLayer.COLOR_GRAPH);
            drawGrid(shapes, matrix, x, y, width, height, camera);
            for (ResearchGraphEdge edge : projection.edges()) {
                drawEdge(shapes, matrix, edge, x, y, width, height, camera);
            }
            for (NodeRenderData node : renderedNodes) {
                drawNodeBackground(shapes, matrix, node, camera.zoom());
            }
        }
        for (NodeRenderData node : renderedNodes) {
            drawNodeContent(graphics, node, camera.zoom());
        }
    }

    @Override
    protected void drawViewportOverlay(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            int height,
            Camera camera,
            RenderingHint hint) {
        Component zoom = Component.literal(Math.round(camera.zoom() * 100.0D) + "%");
        graphics.drawString(getFont(), zoom, x + 6, y + height - 13, 0xFFB8BDB4, false);
    }

    private void drawGrid(
            ShapeTesslator shapes,
            Matrix4f matrix,
            int x,
            int y,
            int width,
            int height,
            Camera camera) {
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
            if (lineX >= x && lineX < x + width) {
                shapes.fillRect(matrix, lineX, y, lineX + 1, y + height,
                        ResearchArchiveLayer.COLOR_GRAPH_GRID);
            }
        }
        int firstY = (int) Math.floor((y - originY) / step);
        int lastY = (int) Math.ceil((y + height - originY) / step);
        for (int i = firstY; i <= lastY; i++) {
            int lineY = (int) Math.round(originY + i * step);
            if (lineY >= y && lineY < y + height) {
                shapes.fillRect(matrix, x, lineY, x + width, lineY + 1,
                        ResearchArchiveLayer.COLOR_GRAPH_GRID);
            }
        }
    }

    private void drawEdge(
            ShapeTesslator shapes,
            Matrix4f matrix,
            ResearchGraphEdge edge,
            int x,
            int y,
            int width,
            int height,
            Camera camera) {
        ResearchGraphLayout.NodePosition parent = layout.positions().get(edge.parentId());
        ResearchGraphLayout.NodePosition child = layout.positions().get(edge.childId());
        if (parent == null || child == null) {
            return;
        }
        int x1 = worldToScreenX(parent.x() + NODE_WIDTH, x, width);
        int y1 = worldToScreenY(parent.y() + NODE_HEIGHT / 2.0D, y, height);
        int x2 = worldToScreenX(child.x(), x, width);
        int y2 = worldToScreenY(child.y() + NODE_HEIGHT / 2.0D, y, height);
        int midX = (x1 + x2) / 2;
        int color = projection.contextNodeIds().contains(edge.parentId())
                ? 0xFF626861 : 0xFF8D947F;
        appendHorizontalLine(shapes, matrix, x1, midX, y1, x, y, x + width, y + height, color);
        appendVerticalLine(shapes, matrix, midX, y1, y2, x, y, x + width, y + height, color);
        appendHorizontalLine(shapes, matrix, midX, x2, y2, x, y, x + width, y + height, color);
    }

    private List<NodeRenderData> collectRenderedNodes(
            int x,
            int y,
            int width,
            int height,
            Camera camera) {
        int nodeWidth = scaledNodeLength(NODE_WIDTH, camera.zoom());
        int nodeHeight = scaledNodeLength(NODE_HEIGHT, camera.zoom());
        List<NodeRenderData> rendered = new ArrayList<>();
        for (String id : projection.visibleNodeIds()) {
            ResearchGraphLayout.NodePosition position = layout.positions().get(id);
            Research research = researchById.get(id);
            if (position == null || research == null || research.isHidden()) {
                continue;
            }
            int nodeX = worldToScreenX(position.x(), x, width);
            int nodeY = worldToScreenY(position.y(), y, height);
            if (nodeX >= x + width || nodeY >= y + height
                    || nodeX + nodeWidth <= x || nodeY + nodeHeight <= y) {
                continue;
            }
            boolean selected = id.equals(state.selectedResearchId());
            boolean context = projection.contextNodeIds().contains(id);
            boolean dimmed = !cachedSearchQuery.isEmpty() && !searchMatches.contains(id);
            int border = selected ? ResearchArchiveLayer.COLOR_RED
                    : research.isCompleted() ? ResearchArchiveLayer.COLOR_TEAL
                    : research.isInProgress() ? ResearchArchiveLayer.COLOR_GOLD
                    : context ? 0xFF76796F : 0xFFAFA58E;
            int paper = context ? 0xFFD2C9B5 : ResearchArchiveLayer.COLOR_PAPER;
            if (dimmed) {
                border = 0xFF4F524D;
                paper = 0xFF77766D;
            }
            rendered.add(new NodeRenderData(
                    research, nodeX, nodeY, nodeWidth, nodeHeight, border, paper, dimmed));
        }
        return rendered;
    }

    private void drawNodeBackground(
            ShapeTesslator shapes,
            Matrix4f matrix,
            NodeRenderData node,
            double zoom) {
        shapes.fillRect(matrix, node.x(), node.y(), node.x() + node.width(), node.y() + node.height(),
                node.border());
        int inset = zoom >= 0.45D ? 2 : 1;
        if (node.width() > inset * 2 && node.height() > inset * 2) {
            shapes.fillRect(matrix, node.x() + inset, node.y() + inset,
                    node.x() + node.width() - inset, node.y() + node.height() - inset, node.paper());
        }
        if (node.width() >= 7 && node.height() >= 5) {
            int stripeWidth = Math.max(1, Math.min(4, scaledNodeLength(4.0D, zoom)));
            shapes.fillRect(matrix, node.x() + inset, node.y() + inset,
                    node.x() + inset + stripeWidth, node.y() + node.height() - inset, node.border());
        }
    }

    private void drawNodeContent(GuiGraphics graphics, NodeRenderData node, double zoom) {
        int inset = zoom >= 0.45D ? 2 : 1;
        int iconSize = scaledNodeIconLength(zoom);
        int iconX = node.x() + Math.max(2, scaledNodeLength(10.0D, zoom));
        int iconY = node.y() + (node.height() - iconSize) / 2;
        node.research().getIcon().draw(graphics, iconX, iconY, iconSize, iconSize);

        String title = node.research().getName().getString();
        float textScale = nodeTextScale(zoom);
        int textX = iconX + iconSize + Math.max(1, scaledNodeLength(4.0D, zoom));
        int textWidth = Math.max(2, node.x() + node.width() - inset - textX);
        int logicalTextWidth = Math.max(1, (int) Math.floor(textWidth / textScale));
        String visibleTitle = getFont().plainSubstrByWidth(title, logicalTextWidth);
        if (visibleTitle.isEmpty() && !title.isEmpty()) {
            visibleTitle = title.substring(0, title.offsetByCodePoints(0, 1));
        }
        int renderedTextHeight = Math.max(1, (int) Math.ceil(getFont().lineHeight * textScale));
        int textY = node.y() + (node.height() - renderedTextHeight) / 2;
        graphics.pose().pushPose();
        graphics.pose().translate(textX, textY, 0.0F);
        graphics.pose().scale(textScale, textScale, 1.0F);
        graphics.drawString(getFont(), visibleTitle, 0, 0,
                node.dimmed() ? 0xFF43443F : ResearchArchiveLayer.COLOR_INK, false);
        graphics.pose().popPose();
    }

    @Override
    public boolean onMousePressed(MouseButton button) {
        if (super.onMousePressed(button)) {
            return true;
        }
        Research research = nodeAtMouse();
        if (button == MouseButton.LEFT && research != null && ResearchArchiveLayer.canReveal(research)) {
            navigation.openResearch(research.getId());
            navigationChanged.run();
            return true;
        }
        return research != null;
    }

    @Override
    protected boolean isPanStartBlocked(MouseButton button) {
        return button == MouseButton.LEFT && nodeAtMouse() != null;
    }

    @Override
    protected void onCameraChanged(Camera previous, Camera current, CameraChange change) {
        state.setCamera(state.researchTypeFilter(), current);
        if (change != CameraChange.PROGRAMMATIC) {
            fittedResearchTypes.add(state.researchTypeFilter());
        }
    }

    @Override
    public void getTooltip(TooltipBuilder tooltip) {
        if (fitButton.isMouseOver() || focusButton.isMouseOver()) {
            super.getTooltip(tooltip);
            return;
        }
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
        if (isPanning()) {
            return Cursor.MOVE;
        }
        return nodeAtMouse() != null ? Cursor.HAND : super.getCursor();
    }

    private void fitToVisible() {
        if (projection.visibleNodeIds().isEmpty() || getWidth() <= 0 || getHeight() <= 0) {
            setCamera(Camera.DEFAULT);
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
        centerOn(new WorldBounds(minX, minY, maxX, maxY), ResearchWorkspaceState.MIN_ZOOM);
        fittedResearchTypes.add(state.researchTypeFilter());
    }

    private void focusSelected() {
        ResearchGraphLayout.NodePosition position = layout.positions().get(state.selectedResearchId());
        if (position == null) {
            fitToVisible();
            return;
        }
        centerOn(position.x() + NODE_WIDTH / 2.0D, position.y() + NODE_HEIGHT / 2.0D);
    }

    @Nullable
    private Research nodeAtMouse() {
        if (!isMouseOver() || fitButton.isMouseOver() || focusButton.isMouseOver()) {
            return null;
        }
        double worldX = screenToWorldX(getMouseX());
        double worldY = screenToWorldY(getMouseY());
        for (String id : projection.visibleNodeIds()) {
            ResearchGraphLayout.NodePosition position = layout.positions().get(id);
            if (position != null && worldX >= position.x() && worldX <= position.x() + NODE_WIDTH
                    && worldY >= position.y() && worldY <= position.y() + NODE_HEIGHT) {
                return researchById.get(id);
            }
        }
        return null;
    }

    private void appendHorizontalLine(
            ShapeTesslator shapes,
            Matrix4f matrix,
            int x1,
            int x2,
            int y,
            int minX,
            int minY,
            int maxX,
            int maxY,
            int color) {
        if (clipHorizontalSegment(x1, x2, y, minX, minY, maxX, maxY, clippedSegment)) {
            shapes.fillRect(matrix, clippedSegment.x1(), clippedSegment.y1(),
                    clippedSegment.x2() + 1, clippedSegment.y2() + 1, color);
        }
    }

    private void appendVerticalLine(
            ShapeTesslator shapes,
            Matrix4f matrix,
            int x,
            int y1,
            int y2,
            int minX,
            int minY,
            int maxX,
            int maxY,
            int color) {
        if (clipVerticalSegment(x, y1, y2, minX, minY, maxX, maxY, clippedSegment)) {
            shapes.fillRect(matrix, clippedSegment.x1(), clippedSegment.y1(),
                    clippedSegment.x2() + 1, clippedSegment.y2() + 1, color);
        }
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

    int projectionBuildCountForTest() {
        return projectionBuildCount;
    }

    private record NodeRenderData(
            Research research,
            int x,
            int y,
            int width,
            int height,
            int border,
            int paper,
            boolean dimmed) {
    }
}
