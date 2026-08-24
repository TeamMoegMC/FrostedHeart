/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.render.weather;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpatialWeatherRendererTest {
    private static final String WALL_TEXTURE =
            "/assets/frostedheart/textures/environment/white_curtain.png";

    @Test
    void fastSnowGridDoesNotRephaseInsideOneTwoBlockCell() {
        assertEquals(
                SpatialWeatherRenderer.snowGridStart(0.01, 2, 16),
                SpatialWeatherRenderer.snowGridStart(1.99, 2, 16));
    }

    @Test
    void fastSnowGridMovesByOneCellAtTheOuterBoundaryOnly() {
        int before = SpatialWeatherRenderer.snowGridStart(1.99, 2, 16);
        int after = SpatialWeatherRenderer.snowGridStart(2.0, 2, 16);
        assertEquals(2, after - before);
    }

    @Test
    void fastSnowGridUsesFloorCellsAtNegativeCoordinates() {
        assertEquals(
                SpatialWeatherRenderer.snowGridStart(-0.01, 2, 16),
                SpatialWeatherRenderer.snowGridStart(-1.99, 2, 16));
        assertEquals(-2,
                SpatialWeatherRenderer.snowGridStart(-2.01, 2, 16)
                        - SpatialWeatherRenderer.snowGridStart(-1.99, 2, 16));
    }

    @Test
    void wallTextureProvidesAContinuousDenseCurtain() throws IOException {
        BufferedImage image;
        try (InputStream stream = SpatialWeatherRendererTest.class.getResourceAsStream(WALL_TEXTURE)) {
            assertNotNull(stream, "white curtain texture must be packaged as a client resource");
            image = ImageIO.read(stream);
        }
        assertNotNull(image);
        assertEquals(64, image.getWidth());
        assertEquals(256, image.getHeight());

        long alphaSum = 0L;
        int visiblePixels = 0;
        int pixels = image.getWidth() * image.getHeight();
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int alpha = image.getRGB(x, y) >>> 24;
                alphaSum += alpha;
                if (alpha > 0) {
                    visiblePixels++;
                }
            }
        }

        double coverage = visiblePixels / (double) pixels;
        double meanTextureAlpha = alphaSum / (255.0 * pixels);
        assertTrue(coverage >= 0.98, "a wall texture cannot be a sparse flake atlas");
        assertTrue(meanTextureAlpha >= 0.45, "the texture must carry a visible snow veil");

        double remainingTransparency = 1.0;
        for (int slice = 0; slice < WeatherQualityProfile.FAST.wallSlices(); slice++) {
            float sliceFade = 1.0F - slice / (float) (WeatherQualityProfile.FAST.wallSlices() + 1);
            double layerOpacity = meanTextureAlpha
                    * SpatialWeatherRenderer.wallLayerAlpha(sliceFade, 1.0F);
            remainingTransparency *= 1.0 - layerOpacity;
        }
        assertTrue(1.0 - remainingTransparency >= 0.55,
                "Fast must still read as a curtain instead of isolated transparent flakes");

        assertTrue(meanOppositeEdgeDelta(image) <= 16.0,
                "opposite texture edges must remain visually continuous when repeated");
    }

    private static double meanOppositeEdgeDelta(BufferedImage image) {
        long difference = 0L;
        long channels = 0L;
        for (int y = 0; y < image.getHeight(); y++) {
            difference += rgbaDifference(image.getRGB(0, y), image.getRGB(image.getWidth() - 1, y));
            channels += 4L;
        }
        for (int x = 0; x < image.getWidth(); x++) {
            difference += rgbaDifference(image.getRGB(x, 0), image.getRGB(x, image.getHeight() - 1));
            channels += 4L;
        }
        return difference / (double) channels;
    }

    private static int rgbaDifference(int first, int second) {
        return Math.abs((first >>> 24) - (second >>> 24))
                + Math.abs(((first >>> 16) & 0xff) - ((second >>> 16) & 0xff))
                + Math.abs(((first >>> 8) & 0xff) - ((second >>> 8) & 0xff))
                + Math.abs((first & 0xff) - (second & 0xff));
    }
}
