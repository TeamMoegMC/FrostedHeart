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
import com.teammoeg.chorda.client.icon.CIconBatch;
import com.teammoeg.chorda.client.icon.FlatIcon;
import com.teammoeg.frostedresearch.FHResearch;
import com.teammoeg.frostedresearch.gui.archive.graph.ResearchGraphEdge;
import com.teammoeg.frostedresearch.gui.archive.graph.ResearchGraphLayout;
import com.teammoeg.frostedresearch.gui.archive.graph.ResearchGraphLayoutEngine;
import com.teammoeg.frostedresearch.gui.archive.graph.ResearchGraphProjection;
import com.teammoeg.frostedresearch.gui.archive.graph.ResearchGraphSnapshot;
import com.teammoeg.frostedresearch.gui.archive.graph.ResearchTypeIdNormalizer;
import com.teammoeg.frostedresearch.research.Research;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.profiling.ProfilerFiller;
import org.joml.Matrix4f;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/** Clipped, zoomable full research dependency graph. */
final class ResearchGraphViewport extends PanZoomViewport {
    private static final double NODE_WIDTH = ResearchGraphLayoutEngine.NODE_WIDTH;
    private static final double NODE_HEIGHT = ResearchGraphLayoutEngine.NODE_HEIGHT;
    private static final int TOOL_SIZE = 20;

    private final ResearchWorkspaceState state;
    private final ResearchArchiveViewCache viewCache;
    private final ResearchNavigationController navigation;
    private final Runnable navigationChanged;
    private final boolean ownsViewCache;
    private final ResearchGraphLayoutEngine layoutEngine = new ResearchGraphLayoutEngine();
    private final Set<String> fittedResearchTypes = new HashSet<>();
    private final Button fitButton;
    private final Button focusButton;
    private ResearchGraphSnapshot snapshot = ResearchGraphSnapshot.of(List.of(), 0L);
    private ResearchGraphLayout layout = layoutEngine.layout(snapshot);
    private ResearchGraphProjection projection = ResearchGraphProjection.forResearchType(snapshot, "*");
    private String cachedSearchQuery = "";
    private int projectionBuildCount;
    private int renderPlanBuildCount;
    private int lastIconFlushCount;
    private final ScreenSegment clippedSegment = new ScreenSegment();
    private final CIconBatch iconBatch = new CIconBatch();
    private List<NodeRenderSource> nodeSources = List.of();
    private List<EdgeRenderSource> edgeSources = List.of();
    private final List<NodeRenderData> renderedNodeBuffer = new ArrayList<>();
    private final List<RectRenderData> renderedEdgeBuffer = new ArrayList<>();
    private final List<RectRenderData> renderedGridBuffer = new ArrayList<>();
    private int renderedNodeCount;
    private int renderedEdgeRectCount;
    private int renderedGridRectCount;
    private boolean renderPlanDirty = true;
    private boolean styleDirty = true;
    private int plannedX;
    private int plannedY;
    private int plannedWidth = -1;
    private int plannedHeight = -1;
    private Camera plannedCamera;
    private float plannedTextScale = 1.0F;
    private long plannedPresentationRevision = -1L;
    private long styledStateRevision = -1L;
    @Nullable
    private String styledSelectedResearchId;
    private boolean nodeHitCacheValid;
    private double nodeHitMouseX;
    private double nodeHitMouseY;
    @Nullable
    private Research nodeHitResult;

    ResearchGraphViewport(
            UIElement parent,
            ResearchWorkspaceState state,
            ResearchNavigationController navigation,
            Runnable navigationChanged) {
        this(parent, state, new ResearchArchiveViewCache(), navigation, navigationChanged, true);
    }

    ResearchGraphViewport(
            UIElement parent,
            ResearchWorkspaceState state,
            ResearchArchiveViewCache viewCache,
            ResearchNavigationController navigation,
            Runnable navigationChanged) {
        this(parent, state, viewCache, navigation, navigationChanged, false);
    }

    private ResearchGraphViewport(
            UIElement parent,
            ResearchWorkspaceState state,
            ResearchArchiveViewCache viewCache,
            ResearchNavigationController navigation,
            Runnable navigationChanged,
            boolean ownsViewCache) {
        super(parent, ResearchWorkspaceState.MIN_ZOOM, ResearchWorkspaceState.MAX_ZOOM);
        this.state = Objects.requireNonNull(state, "state");
        this.viewCache = Objects.requireNonNull(viewCache, "viewCache");
        this.navigation = Objects.requireNonNull(navigation, "navigation");
        this.navigationChanged = Objects.requireNonNull(navigationChanged, "navigationChanged");
        this.ownsViewCache = ownsViewCache;
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
        if (ownsViewCache) {
            viewCache.setDefinitions(definitions);
        }
        snapshot = ResearchGraphSnapshot.fromResearches(definitions, revision);
        layout = layoutEngine.layout(snapshot);
        rebuildProjection();
        rebuildSearchMatches();
        Set<String> validResearchTypes = new HashSet<>();
        validResearchTypes.add(ResearchTypeIdNormalizer.ALL_TYPES);
        for (Research research : definitions) {
            validResearchTypes.add(ResearchTypeIdNormalizer.normalize(research.getCategory()));
        }
        fittedResearchTypes.retainAll(validResearchTypes);
        restoreOrFitCurrentCamera();
    }

    void onResearchTypeChanged() {
        rebuildProjection();
        restoreOrFitCurrentCamera();
    }

    void onSearchChanged() {
        rebuildSearchMatches();
    }

    void onPresentationChanged() {
        rebuildSearchMatches();
        invalidateRenderPlan();
    }

    void resizeViewport(int x, int y, int width, int height) {
        setPosAndSize(x, y, width, height);
        alignWidgets();
        invalidateRenderPlan();
        invalidateNodeHitCache();
        if (!fittedResearchTypes.contains(state.researchTypeFilter())) {
            fitToVisible();
        }
    }

    private void restoreOrFitCurrentCamera() {
        String researchType = state.researchTypeFilter();
        if (state.hasCamera(researchType)) {
            setCamera(state.camera(researchType));
            fittedResearchTypes.add(researchType);
        } else if (!fittedResearchTypes.contains(researchType)) {
            fitToVisible();
        }
    }

    private void rebuildProjection() {
        projection = ResearchGraphProjection.forResearchType(snapshot, state.researchTypeFilter());
        rebuildRenderSources();
        projectionBuildCount++;
        invalidateRenderPlan();
        invalidateNodeHitCache();
    }

    private void rebuildRenderSources() {
        List<NodeRenderSource> nodes = new ArrayList<>(projection.visibleNodeIds().size());
        for (String id : projection.visibleNodeIds()) {
            ResearchGraphLayout.NodePosition position = layout.positions().get(id);
            ResearchArchiveViewCache.View view = viewCache.view(id);
            if (position != null && view != null && !view.research().isHidden()) {
                nodes.add(new NodeRenderSource(
                        id, view, position, projection.contextNodeIds().contains(id)));
            }
        }
        nodeSources = List.copyOf(nodes);

        List<EdgeRenderSource> edges = new ArrayList<>(projection.edges().size());
        for (ResearchGraphEdge edge : projection.edges()) {
            ResearchGraphLayout.NodePosition parent = layout.positions().get(edge.parentId());
            ResearchGraphLayout.NodePosition child = layout.positions().get(edge.childId());
            if (parent != null && child != null) {
                edges.add(new EdgeRenderSource(
                        parent,
                        child,
                        projection.contextNodeIds().contains(edge.parentId())
                                ? 0xFF626861 : 0xFF8D947F));
            }
        }
        edgeSources = List.copyOf(edges);
    }

    private void rebuildSearchMatches() {
        cachedSearchQuery = state.searchQuery().toLowerCase(Locale.ROOT);
        styleDirty = true;
    }

    void onProgressChanged(@Nullable String researchId) {
        styleDirty = true;
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
        ensureRenderPlan(x, y, width, height, camera);
        updateNodeStyles();
        Matrix4f matrix = graphics.pose().last().pose();
        ProfilerFiller profiler = net.minecraft.client.Minecraft.getInstance().getProfiler();

        profiler.push("frostedresearch_graph_shapes");
        try {
            try (ShapeTesslator shapes = TesselateHelper.getShapeTesslator()) {
                shapes.fillRect(matrix, x, y, x + width, y + height, ResearchArchiveLayer.COLOR_GRAPH);
                for (int i = 0; i < renderedGridRectCount; i++) {
                    drawRect(shapes, matrix, renderedGridBuffer.get(i));
                }
                for (int i = 0; i < renderedEdgeRectCount; i++) {
                    drawRect(shapes, matrix, renderedEdgeBuffer.get(i));
                }
                for (int i = 0; i < renderedNodeCount; i++) {
                    drawNodeBackground(shapes, matrix, renderedNodeBuffer.get(i), camera.zoom());
                }
            }
        } finally {
            profiler.pop();
        }

        profiler.push("frostedresearch_graph_item_icons");
        try {
            iconBatch.begin(graphics, false, CIconBatch.Ordering.LAYER_THEN_LIGHTING);
            try {
                for (int i = 0; i < renderedNodeCount; i++) {
                    NodeRenderData node = renderedNodeBuffer.get(i);
                    iconBatch.draw(node.source.view.research().getIcon(),
                            node.iconX, node.iconY, node.iconSize, node.iconSize);
                }
            } finally {
                lastIconFlushCount = iconBatch.end();
            }
        } finally {
            profiler.pop();
        }
        profiler.push("frostedresearch_graph_labels");
        try {
            drawNodeLabels(graphics);
        } finally {
            profiler.pop();
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
        String zoom = Math.round(camera.zoom() * 100.0D) + "%";
        graphics.drawString(getFont(), zoom, x + 6, y + height - 13, 0xFFB8BDB4, false);
    }

    private void ensureRenderPlan(int x, int y, int width, int height, Camera camera) {
        if (!renderPlanDirty
                && plannedX == x
                && plannedY == y
                && plannedWidth == width
                && plannedHeight == height
                && Objects.equals(plannedCamera, camera)
                && plannedPresentationRevision == viewCache.presentationRevision()) {
            return;
        }
        plannedX = x;
        plannedY = y;
        plannedWidth = width;
        plannedHeight = height;
        plannedCamera = camera;
        plannedPresentationRevision = viewCache.presentationRevision();
        plannedTextScale = nodeTextScale(camera.zoom());
        rebuildGridPlan(x, y, width, height, camera);
        rebuildEdgePlan(x, y, width, height);
        rebuildNodePlan(x, y, width, height, camera.zoom());
        renderPlanDirty = false;
        styleDirty = true;
        renderPlanBuildCount++;
    }

    private void rebuildGridPlan(int x, int y, int width, int height, Camera camera) {
        renderedGridRectCount = 0;
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
                addGridRect(lineX, y, lineX + 1, y + height, ResearchArchiveLayer.COLOR_GRAPH_GRID);
            }
        }
        int firstY = (int) Math.floor((y - originY) / step);
        int lastY = (int) Math.ceil((y + height - originY) / step);
        for (int i = firstY; i <= lastY; i++) {
            int lineY = (int) Math.round(originY + i * step);
            if (lineY >= y && lineY < y + height) {
                addGridRect(x, lineY, x + width, lineY + 1, ResearchArchiveLayer.COLOR_GRAPH_GRID);
            }
        }
    }

    private void rebuildEdgePlan(int x, int y, int width, int height) {
        renderedEdgeRectCount = 0;
        for (EdgeRenderSource edge : edgeSources) {
            int x1 = worldToScreenX(edge.parent.x() + NODE_WIDTH, x, width);
            int y1 = worldToScreenY(edge.parent.y() + NODE_HEIGHT / 2.0D, y, height);
            int x2 = worldToScreenX(edge.child.x(), x, width);
            int y2 = worldToScreenY(edge.child.y() + NODE_HEIGHT / 2.0D, y, height);
            int midX = (x1 + x2) / 2;
            appendHorizontalPlan(x1, midX, y1, x, y, x + width, y + height, edge.color);
            appendVerticalPlan(midX, y1, y2, x, y, x + width, y + height, edge.color);
            appendHorizontalPlan(midX, x2, y2, x, y, x + width, y + height, edge.color);
        }
    }

    private void rebuildNodePlan(int x, int y, int width, int height, double zoom) {
        int nodeWidth = scaledNodeLength(NODE_WIDTH, zoom);
        int nodeHeight = scaledNodeLength(NODE_HEIGHT, zoom);
        renderedNodeCount = 0;
        for (NodeRenderSource source : nodeSources) {
            ResearchGraphLayout.NodePosition position = source.position;
            int nodeX = worldToScreenX(position.x(), x, width);
            int nodeY = worldToScreenY(position.y(), y, height);
            if (nodeX >= x + width || nodeY >= y + height
                    || nodeX + nodeWidth <= x || nodeY + nodeHeight <= y) {
                continue;
            }
            NodeRenderData renderedNode;
            if (renderedNodeCount < renderedNodeBuffer.size()) {
                renderedNode = renderedNodeBuffer.get(renderedNodeCount);
            } else {
                renderedNode = new NodeRenderData();
                renderedNodeBuffer.add(renderedNode);
            }
            int inset = zoom >= 0.45D ? 2 : 1;
            int iconSize = scaledNodeIconLength(zoom);
            int iconX = nodeX + Math.max(2, scaledNodeLength(10.0D, zoom));
            int iconY = nodeY + (nodeHeight - iconSize) / 2;
            int textX = iconX + iconSize + Math.max(1, scaledNodeLength(4.0D, zoom));
            int textWidth = Math.max(2, nodeX + nodeWidth - inset - textX);
            int logicalTextWidth = Math.max(1, (int) Math.floor(textWidth / plannedTextScale));
            String title = source.view.title();
            String visibleTitle = getFont().plainSubstrByWidth(title, logicalTextWidth);
            if (visibleTitle.isEmpty() && !title.isEmpty()) {
                visibleTitle = title.substring(0, title.offsetByCodePoints(0, 1));
            }
            int renderedTextHeight = Math.max(1, (int) Math.ceil(getFont().lineHeight * plannedTextScale));
            int textY = nodeY + (nodeHeight - renderedTextHeight) / 2;
            renderedNode.setGeometry(
                    source,
                    nodeX,
                    nodeY,
                    nodeWidth,
                    nodeHeight,
                    iconX,
                    iconY,
                    iconSize,
                    textX,
                    textY,
                    visibleTitle);
            renderedNodeCount++;
        }
    }

    private void updateNodeStyles() {
        String selectedResearchId = state.selectedResearchId();
        if (!styleDirty
                && Objects.equals(styledSelectedResearchId, selectedResearchId)
                && styledStateRevision == viewCache.stateRevision()) {
            return;
        }
        for (int i = 0; i < renderedNodeCount; i++) {
            NodeRenderData node = renderedNodeBuffer.get(i);
            ResearchArchiveViewCache.View view = node.source.view;
            boolean selected = node.source.id.equals(selectedResearchId);
            boolean dimmed = !cachedSearchQuery.isEmpty()
                    && !view.lowercaseSearchText().contains(cachedSearchQuery);
            int border = selected ? ResearchArchiveLayer.COLOR_RED
                    : view.completed() ? ResearchArchiveLayer.COLOR_TEAL
                    : view.active() ? ResearchArchiveLayer.COLOR_GOLD
                    : node.source.context ? 0xFF76796F : 0xFFAFA58E;
            int paper = node.source.context ? 0xFFD2C9B5 : ResearchArchiveLayer.COLOR_PAPER;
            if (dimmed) {
                border = 0xFF4F524D;
                paper = 0xFF77766D;
            }
            node.setStyle(border, paper, dimmed);
        }
        styledSelectedResearchId = selectedResearchId;
        styledStateRevision = viewCache.stateRevision();
        styleDirty = false;
    }

    private void drawNodeBackground(
            ShapeTesslator shapes,
            Matrix4f matrix,
            NodeRenderData node,
            double zoom) {
        shapes.fillRect(matrix, node.x, node.y, node.x + node.width, node.y + node.height, node.border);
        int inset = zoom >= 0.45D ? 2 : 1;
        if (node.width > inset * 2 && node.height > inset * 2) {
            shapes.fillRect(matrix, node.x + inset, node.y + inset,
                    node.x + node.width - inset, node.y + node.height - inset, node.paper);
        }
        if (node.width >= 7 && node.height >= 5) {
            int stripeWidth = Math.max(1, Math.min(4, scaledNodeLength(4.0D, zoom)));
            shapes.fillRect(matrix, node.x + inset, node.y + inset,
                    node.x + inset + stripeWidth, node.y + node.height - inset, node.border);
        }
    }

    private void drawNodeLabels(GuiGraphics graphics) {
        if (renderedNodeCount == 0) {
            return;
        }
        graphics.pose().pushPose();
        try {
            graphics.pose().scale(plannedTextScale, plannedTextScale, 1.0F);
            for (int i = 0; i < renderedNodeCount; i++) {
                NodeRenderData node = renderedNodeBuffer.get(i);
                int logicalX = Math.round(node.textX / plannedTextScale);
                int logicalY = Math.round(node.textY / plannedTextScale);
                graphics.drawString(
                        getFont(),
                        node.visibleTitle,
                        logicalX,
                        logicalY,
                        node.dimmed ? 0xFF43443F : ResearchArchiveLayer.COLOR_INK,
                        false);
            }
        } finally {
            graphics.pose().popPose();
        }
    }

    private void drawRect(ShapeTesslator shapes, Matrix4f matrix, RectRenderData rect) {
        shapes.fillRect(matrix, rect.x1, rect.y1, rect.x2, rect.y2, rect.color);
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
        invalidateRenderPlan();
        invalidateNodeHitCache();
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
            ResearchArchiveViewCache.View view = viewCache.view(research.getId());
            tooltip.accept(view == null ? research.getName() : view.name());
            if (FHResearch.editor) {
                tooltip.accept(Component.literal(research.getId()));
            }
            tooltip.accept(view == null ? research.getCategory().getName() : view.categoryName());
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
        if (nodeSources.isEmpty() || getWidth() <= 0 || getHeight() <= 0) {
            setCamera(Camera.DEFAULT);
            return;
        }
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        for (NodeRenderSource source : nodeSources) {
            ResearchGraphLayout.NodePosition position = source.position;
            minX = Math.min(minX, position.x());
            minY = Math.min(minY, position.y());
            maxX = Math.max(maxX, position.x() + NODE_WIDTH);
            maxY = Math.max(maxY, position.y() + NODE_HEIGHT);
        }
        if (!Double.isFinite(minX)) {
            return;
        }
        centerOn(new WorldBounds(minX, minY, maxX, maxY), ResearchWorkspaceState.MIN_ZOOM);
        state.setCamera(state.researchTypeFilter(), getCamera());
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
        double mouseX = getMouseX();
        double mouseY = getMouseY();
        if (nodeHitCacheValid && mouseX == nodeHitMouseX && mouseY == nodeHitMouseY) {
            return nodeHitResult;
        }
        double worldX = screenToWorldX(mouseX);
        double worldY = screenToWorldY(mouseY);
        Research hit = null;
        for (NodeRenderSource source : nodeSources) {
            ResearchGraphLayout.NodePosition position = source.position;
            if (worldX >= position.x() && worldX <= position.x() + NODE_WIDTH
                    && worldY >= position.y() && worldY <= position.y() + NODE_HEIGHT) {
                hit = source.view.research();
                break;
            }
        }
        nodeHitCacheValid = true;
        nodeHitMouseX = mouseX;
        nodeHitMouseY = mouseY;
        nodeHitResult = hit;
        return hit;
    }

    private void invalidateNodeHitCache() {
        nodeHitCacheValid = false;
        nodeHitResult = null;
    }

    private void appendHorizontalPlan(
            int x1,
            int x2,
            int y,
            int minX,
            int minY,
            int maxX,
            int maxY,
            int color) {
        if (clipHorizontalSegment(x1, x2, y, minX, minY, maxX, maxY, clippedSegment)) {
            addEdgeRect(
                    clippedSegment.x1(), clippedSegment.y1(),
                    clippedSegment.x2() + 1, clippedSegment.y2() + 1, color);
        }
    }

    private void appendVerticalPlan(
            int x,
            int y1,
            int y2,
            int minX,
            int minY,
            int maxX,
            int maxY,
            int color) {
        if (clipVerticalSegment(x, y1, y2, minX, minY, maxX, maxY, clippedSegment)) {
            addEdgeRect(
                    clippedSegment.x1(), clippedSegment.y1(),
                    clippedSegment.x2() + 1, clippedSegment.y2() + 1, color);
        }
    }

    private void addGridRect(int x1, int y1, int x2, int y2, int color) {
        RectRenderData rect;
        if (renderedGridRectCount < renderedGridBuffer.size()) {
            rect = renderedGridBuffer.get(renderedGridRectCount);
        } else {
            rect = new RectRenderData();
            renderedGridBuffer.add(rect);
        }
        rect.set(x1, y1, x2, y2, color);
        renderedGridRectCount++;
    }

    private void addEdgeRect(int x1, int y1, int x2, int y2, int color) {
        RectRenderData rect;
        if (renderedEdgeRectCount < renderedEdgeBuffer.size()) {
            rect = renderedEdgeBuffer.get(renderedEdgeRectCount);
        } else {
            rect = new RectRenderData();
            renderedEdgeBuffer.add(rect);
        }
        rect.set(x1, y1, x2, y2, color);
        renderedEdgeRectCount++;
    }

    private void invalidateRenderPlan() {
        renderPlanDirty = true;
        styleDirty = true;
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

    int renderPlanBuildCountForTest() {
        return renderPlanBuildCount;
    }

    int lastIconFlushCountForTest() {
        return lastIconFlushCount;
    }

    private record NodeRenderSource(
            String id,
            ResearchArchiveViewCache.View view,
            ResearchGraphLayout.NodePosition position,
            boolean context) {
    }

    private record EdgeRenderSource(
            ResearchGraphLayout.NodePosition parent,
            ResearchGraphLayout.NodePosition child,
            int color) {
    }

    private static final class RectRenderData {
        private int x1;
        private int y1;
        private int x2;
        private int y2;
        private int color;

        private void set(int x1, int y1, int x2, int y2, int color) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
            this.color = color;
        }
    }

    private static final class NodeRenderData {
        private NodeRenderSource source;
        private int x;
        private int y;
        private int width;
        private int height;
        private int iconX;
        private int iconY;
        private int iconSize;
        private int textX;
        private int textY;
        private String visibleTitle;
        private int border;
        private int paper;
        private boolean dimmed;

        private void setGeometry(
                NodeRenderSource source,
                int x,
                int y,
                int width,
                int height,
                int iconX,
                int iconY,
                int iconSize,
                int textX,
                int textY,
                String visibleTitle) {
            this.source = source;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.iconX = iconX;
            this.iconY = iconY;
            this.iconSize = iconSize;
            this.textX = textX;
            this.textY = textY;
            this.visibleTitle = visibleTitle;
        }

        private void setStyle(int border, int paper, boolean dimmed) {
            this.border = border;
            this.paper = paper;
            this.dimmed = dimmed;
        }

    }
}
