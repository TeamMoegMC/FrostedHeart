/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge.client.ui;

import com.teammoeg.chorda.client.RenderingHint;
import com.teammoeg.chorda.client.CInputHelper.Cursor;
import com.teammoeg.chorda.client.cui.base.*;
import com.teammoeg.frostedresearch.knowledge.client.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.*;
import java.util.function.Consumer;

/**
 * Independent knowledge canvas using Chorda's pan/zoom camera and clipped rendering.
 */
public final class KnowledgeGraphViewport extends PanZoomViewport {
    private KnowledgeGraphModel model;
    private final Consumer<KnowledgeGraphModel.Node> open;

    private record NodeText(List<FormattedCharSequence> lines, Component kind) {
    }

    private final Map<String, NodeText> titles = new HashMap<>();
    private final Set<String> matches = new HashSet<>();
    private String query = "", pressed;
    private boolean moved, fitted;
    private double drag;
    private double hitX = Double.NaN, hitY = Double.NaN;
    private KnowledgeGraphModel.Node hovered;

    public KnowledgeGraphViewport(UIElement parent, Consumer<KnowledgeGraphModel.Node> open) {
        super(parent, 0.1, 2.4);
        this.open = open;
    }

    public void model(KnowledgeGraphModel model) {
        this.model = model;
        titles.clear();
        hitX = Double.NaN;
        hovered = null;
        find(query);
        if (!fitted && !model.nodes.isEmpty() && getWidth() > 0) fit();
    }

    public void find(String query) {
        this.query = query.toLowerCase(Locale.ROOT);
        matches.clear();
        if (model != null && !query.isBlank()) for (var node : model.nodes)
            if (node.title().getString().toLowerCase(Locale.ROOT).contains(this.query)) matches.add(node.id());
    }

    public void fit() {
        if (model == null || model.nodes.isEmpty()) return;
        fitted = fitToBounds(new WorldBounds(-44, -44, model.width + 20, model.height + 20), 18);
    }

    public void zoom(double multiplier) {
        zoomAt(getWidth() / 2d, getHeight() / 2d, getCamera().zoom() * multiplier);
    }

    private KnowledgeGraphModel.Node underMouse() {
        if (model == null || !isMouseOver()) return null;
        if (getMouseX() < 0 || getMouseY() < 0 || getMouseX() > getWidth() || getMouseY() > getHeight()) return null;
        double x = screenToWorldX(getMouseX()), y = screenToWorldY(getMouseY());
        if (x == hitX && y == hitY) return hovered;
        hitX = x;
        hitY = y;
        hovered = null;
        for (var node : model.nodes)
            if (x >= node.x() && x <= node.x() + node.width() && y >= node.y() && y <= node.y() + node.height()) {
                hovered = node;
                break;
            }
        return hovered;
    }

    @Override
    public boolean onMousePressed(MouseButton button) {
        if (!isMouseOver()) return false;
        var node = underMouse();
        pressed = button == MouseButton.LEFT && node != null ? node.id() : null;
        moved = false;
        drag = 0;
        return super.onMousePressed(button);
    }

    @Override
    public boolean onMouseDragged(MouseButton button, double x, double y) {
        drag += Math.abs(x) + Math.abs(y);
        moved |= drag > 3;
        return super.onMouseDragged(button, x, y);
    }

    @Override
    public void onMouseReleased(MouseButton button) {
        super.onMouseReleased(button);
        var node = underMouse();
        if (button == MouseButton.LEFT && !moved && pressed != null && node != null && pressed.equals(node.id()))
            open.accept(node);
        pressed = null;
    }

    @Override
    public void getTooltip(TooltipBuilder tooltip) {
        var node = underMouse();
        if (node != null && !isPanning()) tooltip.accept(node.title());
    }

    @Override
    public Cursor getCursor() {
        return !isPanning() && underMouse() != null ? Cursor.HAND : super.getCursor();
    }

    @Override
    protected void drawViewportBackground(GuiGraphics g, int x, int y, int w, int h, RenderingHint hint) {
        g.fill(x, y, x + w, y + h, 0xFF4B4F47);
        // Fixed sparse guide marks are cheap and do not create a texture/cache for the whole graph.
        for (int gx = x + 12; gx < x + w; gx += 32)
            for (int gy = y + 12; gy < y + h; gy += 32) g.fill(gx, gy, gx + 1, gy + 1, 0xFF646659);
    }

    @Override
    protected void drawViewportContent(GuiGraphics g, int x, int y, int w, int h, Camera camera, RenderingHint hint) {
        if (model == null || model.nodes.isEmpty()) {
            g.drawString(getFont(), KnowledgePresentation.t("graph.empty"), x + 18, y + 22, 0xFFE2D8BE, false);
            return;
        }
        if (!fitted) fit();
        var hover = underMouse();
        g.pose().pushPose();
        applyWorldTransform(g.pose(), x, y, w, h);
        for (var edge : model.edges) {
            var a = model.byId.get(edge.from());
            var b = model.byId.get(edge.to());
            int sx = a.kind() == KnowledgeGraphModel.Kind.LINK ? a.centerX() : a.x() + a.width(), sy = a.centerY();
            int tx = b.kind() == KnowledgeGraphModel.Kind.LINK ? b.centerX() : b.x(), ty = b.centerY();
            boolean backward = tx < sx;
            int routeY = Math.min(a.y(), b.y()) - 22;
            if (!isWorldRectVisible(Math.min(sx, tx) - 12, Math.min(Math.min(sy, ty), routeY), Math.max(sx, tx) + 12, Math.max(sy, ty) + 2))
                continue;
            int color = edge.planned() ? 0xFF969380 : backward ? 0xFF83AAA0 : 0xFFC4AC76;
            if (backward) {
                line(g, sx, sy, sx + 10, sy, color, edge.planned());
                line(g, sx + 10, sy, sx + 10, routeY, color, edge.planned());
                line(g, sx + 10, routeY, tx - 10, routeY, color, edge.planned());
                line(g, tx - 10, routeY, tx - 10, ty, color, edge.planned());
                line(g, tx - 10, ty, tx, ty, color, edge.planned());
            } else {
                int middle = (sx + tx) / 2;
                line(g, sx, sy, middle, sy, color, edge.planned());
                line(g, middle, sy, middle, ty, color, edge.planned());
                line(g, middle, ty, tx, ty, color, edge.planned());
            }
            if (b.kind() != KnowledgeGraphModel.Kind.LINK) {
                g.fill(tx - 3, ty - 3, tx - 2, ty + 4, color);
                g.fill(tx - 2, ty - 2, tx - 1, ty + 3, color);
            }
        }
        for (var node : model.nodes) {
            if (!isWorldRectVisible(node.x(), node.y(), node.x() + node.width(), node.y() + node.height())) continue;
            int nx = node.x(), ny = node.y();
            boolean hovered = hover != null && hover.id().equals(node.id());
            if (node.kind() == KnowledgeGraphModel.Kind.LINK) {
                g.fill(nx + 4, ny, nx + 8, ny + 12, 0xFF83AAA0);
                g.fill(nx, ny + 4, nx + 12, ny + 8, 0xFF83AAA0);
                continue;
            }
            int accent = switch (node.kind()) {
                case OBSERVATION -> 0xFF766C4E;
                case IDEA -> 0xFF537E73;
                case RESULT -> 0xFFAA8048;
                default -> 0xFF8A674D;
            };
            g.fill(nx, ny, nx + node.width(), ny + node.height(), node.active() ? KnowledgeUiTheme.PAPER : KnowledgeUiTheme.PAPER_DARK);
            g.fill(nx, ny, nx + 3, ny + node.height(), accent);
            if (hovered || matches.contains(node.id())) {
                g.fill(nx, ny, nx + node.width(), ny + 2, 0xFFF3E4B9);
                g.fill(nx, ny + node.height() - 2, nx + node.width(), ny + node.height(), 0xFFF3E4B9);
            }
            if (camera.zoom() >= 0.45) {
                if (camera.zoom() >= 0.6) g.renderItem(node.icon(), nx + 9, ny + 10);
                var text = titles.computeIfAbsent(node.id(), id -> new NodeText(getFont().split(node.title(), node.width() - 42),
                        node.kind() == KnowledgeGraphModel.Kind.PROJECT ? KnowledgePresentation.t(node.completed() ? "graph.project_completed" : "graph.project_available") : KnowledgePresentation.kind(node.kind().name())));
                var wrapped = text.lines();
                int ly = ny + 7;
                for (int i = 0; i < Math.min(2, wrapped.size()); i++, ly += 11)
                    g.drawString(getFont(), wrapped.get(i), nx + 33, ly, KnowledgeUiTheme.INK, false);
                g.drawString(getFont(), text.kind(), nx + 33, ny + 34, accent, false);
            }
        }
        g.pose().popPose();
    }

    private static void line(GuiGraphics g, int x1, int y1, int x2, int y2, int color, boolean dashed) {
        int left = Math.min(x1, x2), top = Math.min(y1, y2), right = Math.max(x1, x2), bottom = Math.max(y1, y2);
        if (!dashed) {
            g.fill(left, top, right + 1, bottom + 1, color);
            return;
        }
        if (top == bottom)
            for (int x = left; x <= right; x += 7) g.fill(x, top, Math.min(right + 1, x + 4), top + 1, color);
        else for (int y = top; y <= bottom; y += 7) g.fill(left, y, left + 1, Math.min(bottom + 1, y + 4), color);
    }
}
