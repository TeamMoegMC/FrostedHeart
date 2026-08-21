/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.chorda.client.cui.base;

import com.mojang.blaze3d.vertex.PoseStack;
import com.teammoeg.chorda.client.CInputHelper.Cursor;
import com.teammoeg.chorda.client.RenderingHint;
import net.minecraft.client.gui.GuiGraphics;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * 可复用的二维平移缩放视口，适用于地图、节点图和大型虚拟画布。
 * <p>
 * Reusable two-dimensional pan/zoom viewport for maps, node graphs, and large
 * virtual canvases. Virtual content is rendered through a clipped callback while
 * ordinary CUI children remain available for toolbar controls.
 */
public class PanZoomViewport extends UILayer {
    public static final double DEFAULT_MIN_ZOOM = 0.05D;
    public static final double DEFAULT_MAX_ZOOM = 16.0D;
    public static final double DEFAULT_ZOOM_STEP = 1.12D;

    private Camera camera = Camera.DEFAULT;
    private double minZoom;
    private double maxZoom;
    private double zoomStep = DEFAULT_ZOOM_STEP;
    private boolean panning;
    @Nullable
    private MouseButton panButton;

    public PanZoomViewport(UIElement parent) {
        this(parent, DEFAULT_MIN_ZOOM, DEFAULT_MAX_ZOOM);
    }

    public PanZoomViewport(UIElement parent, double minZoom, double maxZoom) {
        super(parent);
        validateZoomRange(minZoom, maxZoom);
        this.minZoom = minZoom;
        this.maxZoom = maxZoom;
        camera = camera.clamped(minZoom, maxZoom);
        // Virtual world content is clipped explicitly; avoid UILayer's stencil pass.
        setScissorEnabled(false);
    }

    @Override
    public void addUIElements() {
    }

    @Override
    public void alignWidgets() {
    }

    /**
     * 绘制视口外观、裁剪后的虚拟内容和覆盖层。
     * <p>
     * Draws viewport chrome, clipped virtual content, and overlays in that order.
     */
    @Override
    public final void drawBackground(
            GuiGraphics graphics, int x, int y, int width, int height, RenderingHint hint) {
        drawViewportBackground(graphics, x, y, width, height, hint);
        graphics.enableScissor(x, y, x + width, y + height);
        try {
            drawViewportContent(graphics, x, y, width, height, camera, hint);
        } finally {
            graphics.disableScissor();
        }
        drawViewportOverlay(graphics, x, y, width, height, camera, hint);
    }

    /** Draws non-world viewport chrome before clipping is enabled. */
    protected void drawViewportBackground(
            GuiGraphics graphics, int x, int y, int width, int height, RenderingHint hint) {
    }

    /**
     * 在矩形裁剪启用时绘制虚拟世界内容。
     * <p>
     * Draws virtual world content while rectangular clipping is active.
     */
    protected void drawViewportContent(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            int height,
            Camera camera,
            RenderingHint hint) {
    }

    /** Draws labels and non-widget overlays after virtual content. */
    protected void drawViewportOverlay(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            int height,
            Camera camera,
            RenderingHint hint) {
    }

    public final Camera getCamera() {
        return camera;
    }

    public final double getMinZoom() {
        return minZoom;
    }

    public final double getMaxZoom() {
        return maxZoom;
    }

    public final double getZoomStep() {
        return zoomStep;
    }

    public final boolean isPanning() {
        return panning;
    }

    /** Sets and sanitizes the camera without creating per-frame helper objects. */
    public final void setCamera(Camera camera) {
        applyCamera(camera, CameraChange.PROGRAMMATIC);
    }

    public final void setZoomRange(double minZoom, double maxZoom) {
        validateZoomRange(minZoom, maxZoom);
        this.minZoom = minZoom;
        this.maxZoom = maxZoom;
        applyCamera(camera, CameraChange.PROGRAMMATIC);
    }

    public final void setZoomStep(double zoomStep) {
        if (!Double.isFinite(zoomStep) || zoomStep <= 1.0D) {
            throw new IllegalArgumentException("zoomStep must be finite and greater than 1");
        }
        this.zoomStep = zoomStep;
    }

    /** Centers the camera without changing zoom. */
    public final void centerOn(double worldX, double worldY) {
        applyCamera(new Camera(worldX, worldY, camera.zoom()), CameraChange.CENTER);
    }

    /** Centers the camera on bounds at an explicitly requested zoom. */
    public final void centerOn(WorldBounds bounds, double zoom) {
        Objects.requireNonNull(bounds, "bounds");
        applyCamera(new Camera(bounds.centerX(), bounds.centerY(), zoom), CameraChange.CENTER);
    }

    /**
     * Fits world bounds inside this viewport using an unscaled pixel padding.
     * Returns false before the viewport has a usable size.
     */
    public final boolean fitToBounds(WorldBounds bounds, int padding) {
        Objects.requireNonNull(bounds, "bounds");
        if (getWidth() <= 0 || getHeight() <= 0) {
            return false;
        }
        int safePadding = Math.max(0, padding);
        double availableWidth = Math.max(1.0D, getWidth() - safePadding * 2.0D);
        double availableHeight = Math.max(1.0D, getHeight() - safePadding * 2.0D);
        double horizontalZoom = bounds.width() == 0.0D
                ? maxZoom : availableWidth / bounds.width();
        double verticalZoom = bounds.height() == 0.0D
                ? maxZoom : availableHeight / bounds.height();
        applyCamera(new Camera(
                bounds.centerX(), bounds.centerY(), Math.min(horizontalZoom, verticalZoom)),
                CameraChange.FIT);
        return true;
    }

    /** Zooms around a point expressed in viewport-local screen coordinates. */
    public final void zoomAt(double localScreenX, double localScreenY, double requestedZoom) {
        double mouseWorldX = screenToWorldX(localScreenX);
        double mouseWorldY = screenToWorldY(localScreenY);
        double newZoom = clampZoom(requestedZoom);
        applyCamera(new Camera(
                mouseWorldX - (localScreenX - getWidth() / 2.0D) / newZoom,
                mouseWorldY - (localScreenY - getHeight() / 2.0D) / newZoom,
                newZoom), CameraChange.ZOOM);
    }

    /** Applies and consumes a non-zero wheel delta using the configured exponential zoom step. */
    public final boolean zoomByWheel(double scroll, double localScreenX, double localScreenY) {
        if (scroll == 0.0D) {
            return false;
        }
        zoomAt(localScreenX, localScreenY, camera.zoom() * Math.pow(zoomStep, scroll));
        return true;
    }

    /** Converts a world X coordinate to a viewport-local screen coordinate. */
    public final double worldToLocalX(double worldX) {
        return getWidth() / 2.0D + (worldX - camera.x()) * camera.zoom();
    }

    /** Converts a world Y coordinate to a viewport-local screen coordinate. */
    public final double worldToLocalY(double worldY) {
        return getHeight() / 2.0D + (worldY - camera.y()) * camera.zoom();
    }

    /** Converts a local screen X coordinate to world space. */
    public final double screenToWorldX(double localScreenX) {
        return camera.x() + (localScreenX - getWidth() / 2.0D) / camera.zoom();
    }

    /** Converts a local screen Y coordinate to world space. */
    public final double screenToWorldY(double localScreenY) {
        return camera.y() + (localScreenY - getHeight() / 2.0D) / camera.zoom();
    }

    public final double visibleWorldMinX() {
        return screenToWorldX(0.0D);
    }

    public final double visibleWorldMinY() {
        return screenToWorldY(0.0D);
    }

    public final double visibleWorldMaxX() {
        return screenToWorldX(getWidth());
    }

    public final double visibleWorldMaxY() {
        return screenToWorldY(getHeight());
    }

    /**
     * Applies the current world-to-screen transform to an already pushed pose.
     * The caller owns the matching {@link PoseStack#pushPose()} and pop operation.
     */
    protected final void applyWorldTransform(
            PoseStack pose, int viewportX, int viewportY, int viewportWidth, int viewportHeight) {
        pose.translate(viewportX + viewportWidth / 2.0D, viewportY + viewportHeight / 2.0D, 0.0D);
        pose.scale((float) camera.zoom(), (float) camera.zoom(), 1.0F);
        pose.translate(-camera.x(), -camera.y(), 0.0D);
    }

    /** Converts world X to an absolute render coordinate without allocating a point. */
    protected final int worldToScreenX(double worldX, int viewportX, int viewportWidth) {
        return (int) Math.round(viewportX + viewportWidth / 2.0D
                + (worldX - camera.x()) * camera.zoom());
    }

    /** Converts world Y to an absolute render coordinate without allocating a point. */
    protected final int worldToScreenY(double worldY, int viewportY, int viewportHeight) {
        return (int) Math.round(viewportY + viewportHeight / 2.0D
                + (worldY - camera.y()) * camera.zoom());
    }

    /** Tests a world-space rectangle against the current viewport. */
    public final boolean isWorldRectVisible(double x1, double y1, double x2, double y2) {
        double minX = Math.min(worldToLocalX(x1), worldToLocalX(x2));
        double maxX = Math.max(worldToLocalX(x1), worldToLocalX(x2));
        double minY = Math.min(worldToLocalY(y1), worldToLocalY(y2));
        double maxY = Math.max(worldToLocalY(y1), worldToLocalY(y2));
        return maxX > 0.0D && maxY > 0.0D && minX < getWidth() && minY < getHeight();
    }

    /**
     * Allows a map implementation to clamp the camera to its world bounds.
     * The returned camera is sanitized again before being stored.
     */
    protected Camera constrainCamera(Camera requested) {
        return requested;
    }

    /** Called only when the effective camera value changes. */
    protected void onCameraChanged(Camera previous, Camera current, CameraChange change) {
    }

    /** Allows virtual markers to reserve left-click without becoming CUI children. */
    protected boolean isPanStartBlocked(MouseButton button) {
        return false;
    }

    /** Configures which mouse buttons start panning. */
    protected boolean canPanWith(MouseButton button) {
        return button == MouseButton.LEFT || button == MouseButton.MIDDLE;
    }

    @Override
    public boolean onMousePressed(MouseButton button) {
        if (!isMouseOver()) {
            return false;
        }
        if (super.onMousePressed(button)) {
            return true;
        }
        if (!canPanWith(button) || isPanStartBlocked(button)) {
            return false;
        }
        panning = true;
        panButton = button;
        return true;
    }

    @Override
    public boolean onMouseDragged(MouseButton button, double dragX, double dragY) {
        if (!panning || button != panButton) {
            return super.onMouseDragged(button, dragX, dragY);
        }
        applyCamera(new Camera(
                camera.x() - dragX / camera.zoom(),
                camera.y() - dragY / camera.zoom(),
                camera.zoom()), CameraChange.PAN);
        return true;
    }

    @Override
    public void onMouseReleased(MouseButton button) {
        super.onMouseReleased(button);
        if (button == panButton) {
            panning = false;
            panButton = null;
        }
    }

    @Override
    public boolean onMouseScrolled(double scroll) {
        if (super.onMouseScrolled(scroll)) {
            return true;
        }
        return isMouseOver() && zoomByWheel(scroll, getMouseX(), getMouseY());
    }

    @Override
    public Cursor getCursor() {
        if (panning) {
            return Cursor.MOVE;
        }
        Cursor childCursor = super.getCursor();
        if (childCursor != null) {
            return childCursor;
        }
        return isMouseOver() && canPanWith(MouseButton.LEFT)
                && !isPanStartBlocked(MouseButton.LEFT) ? Cursor.MOVE : null;
    }

    private void applyCamera(Camera requested, CameraChange change) {
        Camera sanitized = Objects.requireNonNull(requested, "camera").clamped(minZoom, maxZoom);
        Camera constrained = Objects.requireNonNull(constrainCamera(sanitized), "constrained camera")
                .clamped(minZoom, maxZoom);
        Camera previous = camera;
        if (!previous.equals(constrained)) {
            camera = constrained;
            onCameraChanged(previous, constrained, change);
        }
    }

    private double clampZoom(double zoom) {
        double safeZoom = Double.isFinite(zoom) ? zoom : 1.0D;
        return Math.max(minZoom, Math.min(maxZoom, safeZoom));
    }

    private static void validateZoomRange(double minZoom, double maxZoom) {
        if (!Double.isFinite(minZoom) || !Double.isFinite(maxZoom)
                || minZoom <= 0.0D || maxZoom < minZoom) {
            throw new IllegalArgumentException("zoom range must be finite, positive, and ordered");
        }
    }

    /** Allocation-free clipping for a horizontal segment; max bounds are exclusive. */
    public static boolean clipHorizontalSegment(
            int x1,
            int x2,
            int y,
            int minX,
            int minY,
            int maxX,
            int maxY,
            ScreenSegment output) {
        Objects.requireNonNull(output, "output");
        if (y < minY || y >= maxY || maxX <= minX || maxY <= minY) {
            return false;
        }
        int start = Math.max(Math.min(x1, x2), minX);
        int end = Math.min(Math.max(x1, x2), maxX - 1);
        if (start > end) {
            return false;
        }
        output.set(start, y, end, y);
        return true;
    }

    /** Allocation-free clipping for a vertical segment; max bounds are exclusive. */
    public static boolean clipVerticalSegment(
            int x,
            int y1,
            int y2,
            int minX,
            int minY,
            int maxX,
            int maxY,
            ScreenSegment output) {
        Objects.requireNonNull(output, "output");
        if (x < minX || x >= maxX || maxX <= minX || maxY <= minY) {
            return false;
        }
        int start = Math.max(Math.min(y1, y2), minY);
        int end = Math.min(Math.max(y1, y2), maxY - 1);
        if (start > end) {
            return false;
        }
        output.set(x, start, x, end);
        return true;
    }

    public enum CameraChange {
        PROGRAMMATIC,
        PAN,
        ZOOM,
        CENTER,
        FIT
    }

    /** Immutable world-space camera. */
    public record Camera(double x, double y, double zoom) {
        public static final Camera DEFAULT = new Camera(0.0D, 0.0D, 1.0D);

        public Camera clamped(double minZoom, double maxZoom) {
            validateZoomRange(minZoom, maxZoom);
            double safeX = Double.isFinite(x) ? x : 0.0D;
            double safeY = Double.isFinite(y) ? y : 0.0D;
            double safeZoom = Double.isFinite(zoom) ? zoom : 1.0D;
            double clampedZoom = Math.max(minZoom, Math.min(maxZoom, safeZoom));
            return Double.doubleToLongBits(x) == Double.doubleToLongBits(safeX)
                    && Double.doubleToLongBits(y) == Double.doubleToLongBits(safeY)
                    && Double.doubleToLongBits(zoom) == Double.doubleToLongBits(clampedZoom)
                    ? this : new Camera(safeX, safeY, clampedZoom);
        }
    }

    /** Immutable axis-aligned world bounds. */
    public record WorldBounds(double minX, double minY, double maxX, double maxY) {
        public WorldBounds {
            if (!Double.isFinite(minX) || !Double.isFinite(minY)
                    || !Double.isFinite(maxX) || !Double.isFinite(maxY)
                    || maxX < minX || maxY < minY
                    || !Double.isFinite(maxX - minX) || !Double.isFinite(maxY - minY)) {
                throw new IllegalArgumentException("world bounds must be finite and ordered");
            }
        }

        public double width() {
            return maxX - minX;
        }

        public double height() {
            return maxY - minY;
        }

        public double centerX() {
            return minX + width() / 2.0D;
        }

        public double centerY() {
            return minY + height() / 2.0D;
        }
    }

    /** Mutable scratch result for allocation-free segment clipping. */
    public static final class ScreenSegment {
        private int x1;
        private int y1;
        private int x2;
        private int y2;

        public int x1() {
            return x1;
        }

        public int y1() {
            return y1;
        }

        public int x2() {
            return x2;
        }

        public int y2() {
            return y2;
        }

        private void set(int x1, int y1, int x2, int y2) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
        }
    }
}
