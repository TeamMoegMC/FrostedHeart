/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.chorda.client.cui.base;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PanZoomViewportTest {
    private static final double EPSILON = 0.0000001D;

    @Test
    void cameraSanitizesIllegalValuesAndEnforcesZoomRange() {
        TestViewport viewport = viewport(0.1D, 2.0D, 200, 100);

        viewport.setCamera(new PanZoomViewport.Camera(Double.NaN, Double.POSITIVE_INFINITY, -4.0D));
        assertCamera(0.0D, 0.0D, 0.1D, viewport.getCamera());

        viewport.setCamera(new PanZoomViewport.Camera(4.0D, -8.0D, Double.POSITIVE_INFINITY));
        assertCamera(4.0D, -8.0D, 1.0D, viewport.getCamera());

        viewport.setCamera(new PanZoomViewport.Camera(4.0D, -8.0D, 8.0D));
        assertEquals(2.0D, viewport.getCamera().zoom());
        assertThrows(IllegalArgumentException.class, () -> viewport.setZoomRange(0.0D, 2.0D));
        assertThrows(IllegalArgumentException.class, () -> viewport.setZoomRange(2.0D, 1.0D));
        assertThrows(IllegalArgumentException.class, () -> viewport.setZoomStep(1.0D));
    }

    @Test
    void zoomKeepsTheMouseWorldPositionFixed() {
        TestViewport viewport = viewport(0.1D, 4.0D, 200, 100);
        viewport.setCamera(new PanZoomViewport.Camera(10.0D, 20.0D, 1.0D));
        double localX = 160.0D;
        double localY = 25.0D;
        double worldX = viewport.screenToWorldX(localX);
        double worldY = viewport.screenToWorldY(localY);

        viewport.zoomAt(localX, localY, 2.0D);

        assertEquals(worldX, viewport.screenToWorldX(localX), EPSILON);
        assertEquals(worldY, viewport.screenToWorldY(localY), EPSILON);
        assertCamera(40.0D, 7.5D, 2.0D, viewport.getCamera());
    }

    @Test
    void leftAndMiddleMouseButtonsPanTheCamera() {
        TestViewport viewport = viewport(0.1D, 4.0D, 200, 100);
        viewport.setCamera(new PanZoomViewport.Camera(0.0D, 0.0D, 2.0D));
        setMouseInside(viewport);

        assertTrue(viewport.onMousePressed(MouseButton.LEFT));
        assertTrue(viewport.isPanning());
        assertTrue(viewport.onMouseDragged(MouseButton.LEFT, 20.0D, -10.0D));
        assertCamera(-10.0D, 5.0D, 2.0D, viewport.getCamera());
        viewport.onMouseReleased(MouseButton.LEFT);
        assertFalse(viewport.isPanning());

        assertTrue(viewport.onMousePressed(MouseButton.MIDDLE));
        assertTrue(viewport.onMouseDragged(MouseButton.MIDDLE, -4.0D, 8.0D));
        assertCamera(-8.0D, 1.0D, 2.0D, viewport.getCamera());
        viewport.onMouseReleased(MouseButton.MIDDLE);
        assertFalse(viewport.isPanning());
    }

    @Test
    void fitToBoundsCentersContentUsingAvailableViewportSpace() {
        TestViewport viewport = viewport(0.1D, 2.0D, 200, 100);
        PanZoomViewport.WorldBounds bounds = new PanZoomViewport.WorldBounds(0.0D, 0.0D, 400.0D, 100.0D);

        assertTrue(viewport.fitToBounds(bounds, 10));
        assertCamera(200.0D, 50.0D, 0.45D, viewport.getCamera());

        TestViewport unsized = viewport(0.1D, 2.0D, 0, 0);
        assertFalse(unsized.fitToBounds(bounds, 10));
    }

    @Test
    void worldBoundsCenterRemainsFiniteForLargeSameSignCoordinates() {
        PanZoomViewport.WorldBounds bounds = new PanZoomViewport.WorldBounds(
                1.0E308D, 1.1E308D, 1.5E308D, 1.3E308D);

        assertEquals(1.25E308D, bounds.centerX(), 1.0E292D);
        assertEquals(1.2E308D, bounds.centerY(), 1.0E292D);
    }

    @Test
    void worldAndScreenCoordinatesRoundTripAndVisibilityUsesViewportBounds() {
        TestViewport viewport = viewport(0.1D, 4.0D, 200, 100);
        viewport.setCamera(new PanZoomViewport.Camera(40.0D, -20.0D, 2.0D));

        assertEquals(120.0D, viewport.worldToLocalX(50.0D), EPSILON);
        assertEquals(40.0D, viewport.worldToLocalY(-25.0D), EPSILON);
        assertEquals(50.0D, viewport.screenToWorldX(120.0D), EPSILON);
        assertEquals(-25.0D, viewport.screenToWorldY(40.0D), EPSILON);
        assertTrue(viewport.isWorldRectVisible(89.0D, -1.0D, 91.0D, 1.0D));
        assertFalse(viewport.isWorldRectVisible(90.0D, -1.0D, 95.0D, 1.0D));
        assertFalse(viewport.isWorldRectVisible(-20.0D, 20.0D, 0.0D, 30.0D));
    }

    @Test
    void horizontalAndVerticalClippingReuseTheProvidedScreenSegment() {
        PanZoomViewport.ScreenSegment segment = new PanZoomViewport.ScreenSegment();

        assertFalse(PanZoomViewport.clipHorizontalSegment(-8, -1, 5, 0, 0, 10, 10, segment));
        assertFalse(PanZoomViewport.clipHorizontalSegment(-8, 12, 10, 0, 0, 10, 10, segment));
        assertTrue(PanZoomViewport.clipHorizontalSegment(-3, 14, 4, 0, 0, 10, 10, segment));
        assertSegment(0, 4, 9, 4, segment);
        assertTrue(PanZoomViewport.clipHorizontalSegment(8, 2, 9, 0, 0, 10, 10, segment));
        assertSegment(2, 9, 8, 9, segment);

        assertFalse(PanZoomViewport.clipVerticalSegment(10, -8, 12, 0, 0, 10, 10, segment));
        assertFalse(PanZoomViewport.clipVerticalSegment(2, -8, -1, 0, 0, 10, 10, segment));
        assertTrue(PanZoomViewport.clipVerticalSegment(3, -4, 13, 0, 0, 10, 10, segment));
        assertSegment(3, 0, 3, 9, segment);
        assertTrue(PanZoomViewport.clipVerticalSegment(0, 8, 2, 0, 0, 10, 10, segment));
        assertSegment(0, 2, 0, 8, segment);
    }

    private static TestViewport viewport(double minZoom, double maxZoom, int width, int height) {
        TestViewport viewport = new TestViewport(minZoom, maxZoom);
        viewport.setSize(width, height);
        return viewport;
    }

    private static void setMouseInside(TestViewport viewport) {
        viewport.updateRenderInfo(0, 0, 10.0D, 10.0D, 0.0F);
        viewport.updateMouseOver();
    }

    private static void assertCamera(double x, double y, double zoom, PanZoomViewport.Camera camera) {
        assertEquals(x, camera.x(), EPSILON);
        assertEquals(y, camera.y(), EPSILON);
        assertEquals(zoom, camera.zoom(), EPSILON);
    }

    private static void assertSegment(
            int x1, int y1, int x2, int y2, PanZoomViewport.ScreenSegment segment) {
        assertEquals(x1, segment.x1());
        assertEquals(y1, segment.y1());
        assertEquals(x2, segment.x2());
        assertEquals(y2, segment.y2());
    }

    private static final class TestViewport extends PanZoomViewport {
        private TestViewport(double minZoom, double maxZoom) {
            super(null, minZoom, maxZoom);
        }
    }
}
