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
import com.teammoeg.chorda.client.icon.CIcons.CIcon;
import com.teammoeg.chorda.client.icon.FlatIcon;
import com.teammoeg.frostedresearch.FHResearch;
import com.teammoeg.frostedresearch.gui.archive.graph.ResearchGraphEdge;
import com.teammoeg.frostedresearch.gui.archive.graph.ResearchGraphLayout;
import com.teammoeg.frostedresearch.gui.archive.graph.ResearchGraphLayoutEngine;
import com.teammoeg.frostedresearch.gui.archive.graph.ResearchGraphProjection;
import com.teammoeg.frostedresearch.gui.archive.graph.ResearchGraphSnapshot;
import com.teammoeg.frostedresearch.gui.archive.graph.ResearchTypeIdNormalizer;
import com.teammoeg.frostedresearch.research.Research;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
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
    private static final int ICON_SIZE = 16;
    private static final int MIN_ICON_SIZE = 8;
    private static final int MIN_HIT_SIZE = 12;
    private static final Style TITLE_STYLE = Style.EMPTY;
    private static final int TITLE_LINE_HEIGHT = 9;
    private static final int TITLE_TEXT_HEIGHT = 8;
    private static final int MIN_ELLIPSIS_WIDTH = 48;
    private static final long ZOOM_DURATION_NANOS = 150_000_000L;

    private final ResearchWorkspaceState state;
    private final ResearchArchiveViewCache viewCache;
    private final ResearchNavigationController navigation;
    private final Runnable navigationChanged;
    private final ResearchGraphLayoutEngine layoutEngine = new ResearchGraphLayoutEngine();
    private final Set<String> fittedResearchTypes = new HashSet<>();
    private final Button fitButton;
    private final Button focusButton;
    private int renderedTitleCount;
    private int titlePlanBuildCount;
    private int renderedIconSize = ICON_SIZE;
    private double cardExpansion;
    @Nullable
    private ZoomMotion zoomMotion;
    private boolean applyingZoomMotion;
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
    private int renderedNodeWidth;
    private int renderedNodeHeight;
    private int viewportScreenX;
    private int viewportScreenY;
    private int renderOriginX;
    private int renderOriginY;
    private double renderZoom;
    private boolean renderPlanDirty = true;
    private boolean styleDirty = true;
    @Nullable
    private RenderPlanKey renderPlanKey;
    private String renderPlanZoomLabel = "100%";
    @Nullable
    private NodeStyleKey nodeStyleKey;
    private final NodeHitCache nodeHitCache = new NodeHitCache();

    ResearchGraphViewport(
            UIElement parent,
            ResearchWorkspaceState state,
            ResearchArchiveViewCache viewCache,
            ResearchNavigationController navigation,
            Runnable navigationChanged) {
        super(parent, ResearchWorkspaceState.MIN_ZOOM, ResearchWorkspaceState.MAX_ZOOM);
        this.state = Objects.requireNonNull(state, "state");
        this.viewCache = Objects.requireNonNull(viewCache, "viewCache");
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

    @Override
    public void updateRenderInfo(int x, int y, double mouseX, double mouseY, float partialTicks) {
        // The screen adapter's centering offset is supplied here, not by the CUI root's getScreenX/Y.
        viewportScreenX = x + getX();
        viewportScreenY = y + getY();
        super.updateRenderInfo(x, y, mouseX, mouseY, partialTicks);
    }

    void setDefinitions(List<Research> definitions, long revision) {
        zoomMotion = null;
        snapshot = ResearchGraphSnapshot.fromResearches(definitions, revision, FHResearch.editor);
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
        zoomMotion = null;
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
        zoomMotion = null;
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
            if (position != null && view != null && (FHResearch.editor || !view.research().isHidden())) {
                nodes.add(new NodeRenderSource(
                        id,
                        view,
                        view.research().getIcon(),
                        position,
                        projection.contextNodeIds().contains(id),
                        new NodeTitleLayout()));
            }
        }
        nodeSources = List.copyOf(nodes);

        List<EdgeRenderSource> edges = new ArrayList<>(projection.edges().size());
        for (ResearchGraphEdge edge : projection.edges()) {
            ResearchGraphLayout.NodePosition parent = layout.positions().get(edge.parentId());
            ResearchGraphLayout.NodePosition child = layout.positions().get(edge.childId());
            if (parent != null && child != null) {
                edges.add(new EdgeRenderSource(
                        edge.parentId(),
                        edge.childId(),
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
        advanceZoomMotion();
        ensureRenderPlan(x, y, width, height, getCamera());
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
                    drawNodeBackground(shapes, matrix, renderedNodeBuffer.get(i));
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
                    iconBatch.draw(node.source.icon,
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
            drawNodeTitles(graphics);
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
        graphics.drawString(getFont(), renderPlanZoomLabel,
                x + 6, y + height - 13, 0xFFB8BDB4, false);
    }

    private void ensureRenderPlan(int x, int y, int width, int height, Camera camera) {
        long presentationRevision = viewCache.presentationRevision();
        double guiScale = Minecraft.getInstance().getWindow().getGuiScale();
        if (!renderPlanDirty && renderPlanKey != null
                && renderPlanKey.matches(x, y, width, height, camera, presentationRevision, guiScale)) {
            return;
        }
        renderPlanKey = new RenderPlanKey(x, y, width, height, camera, presentationRevision, guiScale);
        renderZoom = camera.zoom();
        // Snap the shared translation once. Panning must move all projects by the same pixels.
        renderOriginX = (int) Math.round(x + width / 2.0D - camera.x() * renderZoom);
        renderOriginY = (int) Math.round(y + height / 2.0D - camera.y() * renderZoom);
        int scaledWidth = scaledNodeLength(NODE_WIDTH, camera.zoom());
        int scaledHeight = scaledNodeLength(NODE_HEIGHT, camera.zoom());
        renderedIconSize = Math.max(MIN_ICON_SIZE, Math.min(ICON_SIZE,
                (int) Math.round(ResearchGraphLayoutEngine.LANE_SPACING * camera.zoom() - 4)));
        cardExpansion = smoothStep((camera.zoom() - .24D) / .46D);
        int tileSize = renderedIconSize + 2;
        // At a distance, use spare column space for short names and retain a one-pixel edge corridor.
        int compactWidth = Math.max(tileSize,
                (int) Math.floor(ResearchGraphLayoutEngine.RANK_SPACING * camera.zoom()) - 1);
        renderedNodeWidth = (int) Math.round(compactWidth + (scaledWidth - compactWidth) * cardExpansion);
        renderedNodeHeight = Math.max(tileSize,
                (int) Math.round(tileSize + (scaledHeight - tileSize) * cardExpansion));
        renderPlanZoomLabel = Math.round(camera.zoom() * 100.0D) + "%";
        rebuildGridPlan(x, y, width, height, camera);
        rebuildEdgePlan(x, y, width, height);
        rebuildNodePlan(x, y, width, height);
        invalidateNodeHitCache();
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
        int firstX = (int) Math.floor((x - renderOriginX) / step);
        int lastX = (int) Math.ceil((x + width - renderOriginX) / step);
        for (int i = firstX; i <= lastX; i++) {
            int lineX = renderOriginX + (int) Math.round(i * step);
            if (lineX >= x && lineX < x + width) {
                addGridRect(lineX, y, lineX + 1, y + height, ResearchArchiveLayer.COLOR_GRAPH_GRID);
            }
        }
        int firstY = (int) Math.floor((y - renderOriginY) / step);
        int lastY = (int) Math.ceil((y + height - renderOriginY) / step);
        for (int i = firstY; i <= lastY; i++) {
            int lineY = renderOriginY + (int) Math.round(i * step);
            if (lineY >= y && lineY < y + height) {
                addGridRect(x, lineY, x + width, lineY + 1, ResearchArchiveLayer.COLOR_GRAPH_GRID);
            }
        }
    }

    private void rebuildEdgePlan(int x, int y, int width, int height) {
        renderedEdgeRectCount = 0;
        for (EdgeRenderSource edge : edgeSources) {
            int x1 = projectX(edge.parent.x() + NODE_WIDTH / 2.0D)
                    - renderedNodeWidth / 2 + renderedNodeWidth;
            int y1 = projectY(edge.parent.y() + NODE_HEIGHT / 2.0D);
            int x2 = projectX(edge.child.x() + NODE_WIDTH / 2.0D)
                    - renderedNodeWidth / 2;
            int y2 = projectY(edge.child.y() + NODE_HEIGHT / 2.0D);
            int midX = x1 + Math.floorDiv(x2 - x1, 2);
            appendHorizontalPlan(x1, midX, y1, x, y, x + width, y + height, edge);
            appendVerticalPlan(midX, y1, y2, x, y, x + width, y + height, edge);
            appendHorizontalPlan(midX, x2, y2, x, y, x + width, y + height, edge);
        }
    }

    private int projectX(double worldX) {
        return renderOriginX + (int) Math.round(worldX * renderZoom);
    }

    private int projectY(double worldY) {
        return renderOriginY + (int) Math.round(worldY * renderZoom);
    }

    private void rebuildNodePlan(int x, int y, int width, int height) {
        int nodeWidth = renderedNodeWidth;
        int nodeHeight = renderedNodeHeight;
        int iconFrameSize = Math.min(nodeHeight, renderedIconSize + 2 + (int) Math.round(6 * cardExpansion));
        // Share one border so the nameplate visibly belongs to its icon at every zoom.
        int titleFrameOffset = iconFrameSize - 1;
        int textInset = 1 + (int) Math.round(3 * cardExpansion);
        int titleFrameWidth = nodeWidth - titleFrameOffset;
        int textWidth = Math.max(1, titleFrameWidth - textInset * 2);
        boolean twoLinesFit = TITLE_LINE_HEIGHT + TITLE_TEXT_HEIGHT <= nodeHeight - 2;
        long presentationRevision = viewCache.presentationRevision();
        renderedNodeCount = 0;
        renderedTitleCount = 0;
        for (NodeRenderSource source : nodeSources) {
            ResearchGraphLayout.NodePosition position = source.position;
            int nodeX = projectX(position.x() + NODE_WIDTH / 2.0D) - nodeWidth / 2;
            int nodeY = projectY(position.y() + NODE_HEIGHT / 2.0D) - nodeHeight / 2;
            if (nodeX >= x + width || nodeY >= y + height
                    || nodeX + nodeWidth <= x || nodeY + nodeHeight <= y) continue;
            NodeRenderData node;
            if (renderedNodeCount < renderedNodeBuffer.size()) node = renderedNodeBuffer.get(renderedNodeCount);
            else {
                node = new NodeRenderData();
                renderedNodeBuffer.add(node);
            }
            node.source = source;
            node.x = nodeX;
            node.y = nodeY;
            node.width = nodeWidth;
            node.height = nodeHeight;
            node.iconFrameSize = iconFrameSize;
            node.iconFrameY = nodeY + (nodeHeight - iconFrameSize) / 2;
            node.iconX = nodeX + (iconFrameSize - renderedIconSize) / 2;
            node.iconY = nodeY + (nodeHeight - renderedIconSize) / 2;
            node.iconSize = renderedIconSize;
            NodeTitleLayout title = source.titleLayout;
            updateTitleLayout(source, textWidth, twoLinesFit, presentationRevision);
            node.titleLine1 = title.line1;
            node.titleLine2 = title.line2;
            node.titleGlyphs1 = title.glyphs1;
            node.titleGlyphs2 = title.glyphs2;
            int lines = node.titleLine2.isEmpty() ? 1 : 2;
            int textHeight = TITLE_TEXT_HEIGHT + TITLE_LINE_HEIGHT * (lines - 1);
            node.titleX = nodeX + titleFrameOffset;
            node.titleWidth = titleFrameWidth;
            node.titleHeight = Math.min(nodeHeight, textHeight + textInset * 2);
            node.titleY = nodeY + (nodeHeight - node.titleHeight) / 2;
            // Keep text at its native size and on whole GUI pixels, independently of camera zoom.
            node.fontX = node.titleX + textInset;
            node.fontY = node.titleY + (node.titleHeight - textHeight) / 2;
            if (!node.titleLine1.isBlank()) renderedTitleCount++;
            renderedNodeCount++;
        }
    }

    /** One latest layout per source keeps panning independent of text measurement and allocation. */
    private void updateTitleLayout(NodeRenderSource source, int width, boolean twoLinesFit, long revision) {
        NodeTitleLayout layout = source.titleLayout;
        if (layout.width == width && layout.twoLinesFit == twoLinesFit && layout.revision == revision) return;
        String title = source.view.title();
        String first = getFont().getSplitter().plainHeadByWidth(title, width, TITLE_STYLE);
        String second = "";
        if (first.length() < title.length()) {
            if (twoLinesFit) {
                // Prefer English word boundaries; CJK and long single words use glyph boundaries.
                int space = first.lastIndexOf(' ');
                if (space > 0) first = first.substring(0, space);
                second = clipTitle(title.substring(first.length()).stripLeading(), width);
            } else first = ellipsizeTitle(first, width);
        }
        layout.width = width;
        layout.twoLinesFit = twoLinesFit;
        layout.revision = revision;
        layout.line1 = first;
        layout.line2 = second;
        layout.glyphs1 = FormattedCharSequence.forward(first, TITLE_STYLE);
        layout.glyphs2 = second.isEmpty() ? FormattedCharSequence.EMPTY
                : FormattedCharSequence.forward(second, TITLE_STYLE);
        titlePlanBuildCount++;
    }

    private void updateNodeStyles() {
        String selectedResearchId = state.selectedResearchId();
        Research hovered = nodeAtMouse();
        String hoveredResearchId = hovered == null ? null : hovered.getId();
        long stateRevision = viewCache.stateRevision();
        if (!styleDirty && nodeStyleKey != null
                && nodeStyleKey.matches(selectedResearchId, hoveredResearchId, cachedSearchQuery, stateRevision)) {
            return;
        }
        for (int i = 0; i < renderedNodeCount; i++) {
            NodeRenderData node = renderedNodeBuffer.get(i);
            ResearchArchiveViewCache.View view = node.source.view;
            boolean isSelected = node.source.id.equals(selectedResearchId);
            boolean isHovered = node.source.id.equals(hoveredResearchId);
            boolean matches = !cachedSearchQuery.isEmpty()
                    && view.lowercaseSearchText().contains(cachedSearchQuery);
            boolean dimmed = !cachedSearchQuery.isEmpty() && !matches && !isSelected && !isHovered;
            int border = isSelected ? ResearchArchiveLayer.COLOR_RED
                    : isHovered ? ResearchArchiveLayer.COLOR_GOLD
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
        for (int i = 0; i < renderedEdgeRectCount; i++) {
            RectRenderData rect = renderedEdgeBuffer.get(i);
            EdgeRenderSource edge = rect.edgeSource;
            boolean focused = edge.connects(selectedResearchId) || edge.connects(hoveredResearchId);
            rect.color = focused ? 0xFFD0B274 : edge.color;
        }
        nodeStyleKey = new NodeStyleKey(selectedResearchId, hoveredResearchId, cachedSearchQuery, stateRevision);
        styleDirty = false;
    }

    private void drawNodeBackground(
            ShapeTesslator shapes,
            Matrix4f matrix,
            NodeRenderData node) {
        int iconPaper = node.dimmed ? 0xFF6B6D64 : node.source.context ? 0xFFBDB69F : 0xFFD6C8A6;
        drawNodeFrame(shapes, matrix, node.x, node.iconFrameY, node.iconFrameSize, node.iconFrameSize,
                node.border, iconPaper);
        drawNodeFrame(shapes, matrix, node.titleX, node.titleY, node.titleWidth, node.titleHeight,
                node.border, node.paper);
    }

    /** Two small cut-corner frames keep the icon mount distinct from its paper nameplate. */
    private static void drawNodeFrame(ShapeTesslator shapes, Matrix4f matrix,
            int x, int y, int width, int height, int border, int paper) {
        shapes.fillRect(matrix, x + 1, y, x + width - 1, y + height, border);
        shapes.fillRect(matrix, x, y + 1, x + width, y + height - 1, border);
        shapes.fillRect(matrix, x + 1, y + 1, x + width - 1, y + height - 1, paper);
    }

    private String clipTitle(String title, int width) {
        String prefix = getFont().getSplitter().plainHeadByWidth(title, width, TITLE_STYLE);
        return prefix.length() == title.length() ? prefix : ellipsizeTitle(prefix, width);
    }

    private String ellipsizeTitle(String prefix, int width) {
        // In a tiny name slot, keep useful letters instead of spending the slot on an ellipsis.
        if (width < MIN_ELLIPSIS_WIDTH) return prefix;
        int ellipsisWidth = getFont().width(FormattedCharSequence.forward("…", TITLE_STYLE));
        return getFont().getSplitter().plainHeadByWidth(prefix, width - ellipsisWidth, TITLE_STYLE) + "…";
    }

    private void drawNodeTitles(GuiGraphics graphics) {
        // Minecraft owns glyph baking/atlases; cached sequences use the same font as measurement.
        Matrix4f matrix = graphics.pose().last().pose();
        Font font = getFont();
        for (int i = 0; i < renderedNodeCount; i++) {
            NodeRenderData node = renderedNodeBuffer.get(i);
            int color = node.dimmed ? 0xFF43443F : ResearchArchiveLayer.COLOR_INK;
            font.drawInBatch(node.titleGlyphs1, node.fontX, node.fontY, color, false, matrix,
                    graphics.bufferSource(), Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
            if (!node.titleLine2.isEmpty()) {
                font.drawInBatch(node.titleGlyphs2, node.fontX, node.fontY + TITLE_LINE_HEIGHT, color, false, matrix,
                        graphics.bufferSource(), Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
            }
        }
        graphics.flush();
    }

    private void drawRect(ShapeTesslator shapes, Matrix4f matrix, RectRenderData rect) {
        shapes.fillRect(matrix, rect.x1, rect.y1, rect.x2, rect.y2, rect.color);
    }

    @Override
    public boolean onMousePressed(MouseButton button) {
        if (isMouseOver()) zoomMotion = null;
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
    public boolean onMouseScrolled(double scroll) {
        if (!isMouseOver() || scroll == 0) return false;
        Camera current = getCamera();
        double target = zoomMotion == null ? current.zoom() : zoomMotion.targetZoom;
        double requested = Math.max(getMinZoom(), Math.min(getMaxZoom(), target * Math.pow(getZoomStep(), scroll)));
        if (requested == current.zoom()) {
            zoomMotion = null;
            return true;
        }
        zoomMotion = new ZoomMotion(current.zoom(), requested, screenToWorldX(getMouseX()), screenToWorldY(getMouseY()),
                getMouseX(), getMouseY(), System.nanoTime());
        fittedResearchTypes.add(state.researchTypeFilter());
        return true;
    }

    private void advanceZoomMotion() {
        ZoomMotion motion = zoomMotion;
        if (motion == null) return;
        double progress = Math.min(1.0D, (System.nanoTime() - motion.startedNanos) / (double) ZOOM_DURATION_NANOS);
        double amount = smoothStep(progress);
        double zoom = progress == 1.0D ? motion.targetZoom
                : Math.exp(Math.log(motion.startZoom) + Math.log(motion.targetZoom / motion.startZoom) * amount);
        if (progress == 1.0D) zoomMotion = null;
        applyingZoomMotion = true;
        try {
            setCamera(new Camera(motion.worldX - (motion.localX - getWidth() / 2.0D) / zoom,
                    motion.worldY - (motion.localY - getHeight() / 2.0D) / zoom, zoom));
        } finally {
            applyingZoomMotion = false;
        }
    }

    private static double smoothStep(double value) {
        double t = Math.max(0.0D, Math.min(1.0D, value));
        return t * t * (3.0D - 2.0D * t);
    }

    @Override
    protected void onCameraChanged(Camera previous, Camera current, CameraChange change) {
        if (!applyingZoomMotion) zoomMotion = null;
        state.setCamera(state.researchTypeFilter(), current);
        invalidateRenderPlan();
        invalidateNodeHitCache();
        if (change != CameraChange.PROGRAMMATIC) {
            fittedResearchTypes.add(state.researchTypeFilter());
        }
    }

    @Override
    public void getTooltip(TooltipBuilder tooltip) {
        if (toolAtMouse()) {
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
        fitToBounds(new WorldBounds(minX, minY, maxX, maxY), 24);
        state.setCamera(state.researchTypeFilter(), getCamera());
        fittedResearchTypes.add(state.researchTypeFilter());
    }

    private void focusSelected() {
        String selectedResearchId = state.selectedResearchId();
        ResearchGraphLayout.NodePosition position = selectedResearchId == null
                ? null : layout.positions().get(selectedResearchId);
        if (position == null) {
            fitToVisible();
            return;
        }
        centerOn(new WorldBounds(position.x(), position.y(),
                position.x() + NODE_WIDTH, position.y() + NODE_HEIGHT), Math.max(1.0D, getCamera().zoom()));
    }

    @Nullable
    private Research nodeAtMouse() {
        if (!isMouseOver() || isPanning() || toolAtMouse()) {
            return null;
        }
        int x = viewportScreenX;
        int y = viewportScreenY;
        ensureRenderPlan(x, y, getWidth(), getHeight(), getCamera());
        double mouseX = x + getMouseX();
        double mouseY = y + getMouseY();
        if (nodeHitCache.matches(mouseX, mouseY)) {
            return nodeHitCache.result();
        }
        Research hit = null;
        double nearestDistance = Double.POSITIVE_INFINITY;
        for (int i = 0; i < renderedNodeCount; i++) {
            NodeRenderData node = renderedNodeBuffer.get(i);
            int padX = Math.max(0, (MIN_HIT_SIZE - node.width) / 2);
            int padY = Math.max(0, (MIN_HIT_SIZE - node.height) / 2);
            if (mouseX >= node.x - padX && mouseX < node.x + node.width + padX
                    && mouseY >= node.y - padY && mouseY < node.y + node.height + padY) {
                double dx = mouseX - (node.x + node.width / 2.0D);
                double dy = mouseY - (node.y + node.height / 2.0D);
                double distance = dx * dx + dy * dy;
                if (distance < nearestDistance) {
                    hit = node.source.view.research();
                    nearestDistance = distance;
                }
            }
        }
        nodeHitCache.update(mouseX, mouseY, hit);
        return hit;
    }

    private boolean toolAtMouse() {
        return fitButton.isMouseOver() || focusButton.isMouseOver();
    }

    private void invalidateNodeHitCache() {
        nodeHitCache.invalidate();
    }

    private void appendHorizontalPlan(
            int x1,
            int x2,
            int y,
            int minX,
            int minY,
            int maxX,
            int maxY,
            EdgeRenderSource edge) {
        if (clipHorizontalSegment(x1, x2, y, minX, minY, maxX, maxY, clippedSegment)) {
            addEdgeRect(
                    clippedSegment.x1(), clippedSegment.y1(),
                    clippedSegment.x2() + 1, clippedSegment.y2() + 1, edge);
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
            EdgeRenderSource edge) {
        if (clipVerticalSegment(x, y1, y2, minX, minY, maxX, maxY, clippedSegment)) {
            addEdgeRect(
                    clippedSegment.x1(), clippedSegment.y1(),
                    clippedSegment.x2() + 1, clippedSegment.y2() + 1, edge);
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

    private void addEdgeRect(int x1, int y1, int x2, int y2, EdgeRenderSource edge) {
        RectRenderData rect;
        if (renderedEdgeRectCount < renderedEdgeBuffer.size()) {
            rect = renderedEdgeBuffer.get(renderedEdgeRectCount);
        } else {
            rect = new RectRenderData();
            renderedEdgeBuffer.add(rect);
        }
        rect.set(x1, y1, x2, y2, edge.color);
        rect.edgeSource = edge;
        renderedEdgeRectCount++;
    }

    private void invalidateRenderPlan() {
        renderPlanDirty = true;
        styleDirty = true;
    }

    static int scaledNodeLength(double worldLength, double zoom) {
        return Math.max(1, (int) Math.round(worldLength * zoom));
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

    int titlePlanBuildCountForTest() {
        return titlePlanBuildCount;
    }

    private record ZoomMotion(double startZoom, double targetZoom, double worldX, double worldY,
                              double localX, double localY, long startedNanos) {}

    private record RenderPlanKey(
            int x,
            int y,
            int width,
            int height,
            Camera camera,
            long presentationRevision,
            double guiScale) {

        private boolean matches(
                int currentX,
                int currentY,
                int currentWidth,
                int currentHeight,
                Camera currentCamera,
                long currentPresentationRevision,
                double currentGuiScale) {
            return x == currentX
                    && y == currentY
                    && width == currentWidth
                    && height == currentHeight
                    && Objects.equals(camera, currentCamera)
                    && presentationRevision == currentPresentationRevision
                    && guiScale == currentGuiScale;
        }
    }

    private record NodeStyleKey(
            @Nullable String selectedResearchId,
            @Nullable String hoveredResearchId,
            String searchQuery,
            long stateRevision) {

        private boolean matches(
                @Nullable String currentSelectedResearchId,
                @Nullable String currentHoveredResearchId,
                String currentSearchQuery,
                long currentStateRevision) {
            return Objects.equals(selectedResearchId, currentSelectedResearchId)
                    && Objects.equals(hoveredResearchId, currentHoveredResearchId)
                    && searchQuery.equals(currentSearchQuery)
                    && stateRevision == currentStateRevision;
        }
    }

    private record NodeRenderSource(
            String id,
            ResearchArchiveViewCache.View view,
            CIcon icon,
            ResearchGraphLayout.NodePosition position,
            boolean context,
            NodeTitleLayout titleLayout) {
    }

    /** Bounded by projected nodes; replaced in place when typography inputs change. */
    private static final class NodeTitleLayout {
        private int width = -1;
        private boolean twoLinesFit;
        private long revision = -1;
        private String line1;
        private String line2;
        private FormattedCharSequence glyphs1;
        private FormattedCharSequence glyphs2;
    }

    private record EdgeRenderSource(
            String parentId,
            String childId,
            ResearchGraphLayout.NodePosition parent,
            ResearchGraphLayout.NodePosition child,
            int color) {

        private boolean connects(@Nullable String researchId) {
            return parentId.equals(researchId) || childId.equals(researchId);
        }
    }

    private static final class RectRenderData {
        private int x1;
        private int y1;
        private int x2;
        private int y2;
        private int color;
        private EdgeRenderSource edgeSource;

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
        private int iconFrameSize;
        private int iconFrameY;
        private int titleX;
        private int titleY;
        private int titleWidth;
        private int titleHeight;
        private String titleLine1;
        private String titleLine2;
        private FormattedCharSequence titleGlyphs1;
        private FormattedCharSequence titleGlyphs2;
        private int fontX;
        private int fontY;
        private int border;
        private int paper;
        private boolean dimmed;

        private void setStyle(int border, int paper, boolean dimmed) {
            this.border = border;
            this.paper = paper;
            this.dimmed = dimmed;
        }

    }

    private static final class NodeHitCache {
        private boolean valid;
        private double mouseX;
        private double mouseY;
        @Nullable
        private Research result;

        private boolean matches(double currentMouseX, double currentMouseY) {
            return valid && mouseX == currentMouseX && mouseY == currentMouseY;
        }

        @Nullable
        private Research result() {
            return result;
        }

        private void update(double currentMouseX, double currentMouseY, @Nullable Research currentResult) {
            valid = true;
            mouseX = currentMouseX;
            mouseY = currentMouseY;
            result = currentResult;
        }

        private void invalidate() {
            valid = false;
            result = null;
        }
    }
}
