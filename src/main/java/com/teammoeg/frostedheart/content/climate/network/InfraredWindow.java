/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.network;

import java.nio.ShortBuffer;
import java.util.Arrays;
import net.minecraft.core.SectionPos;

/** Reuses the absolute overlap of the existing 9x9x9 display window. */
public final class InfraredWindow {
    public static final int WIDTH = 9, SIDE = WIDTH * 16, PAGES = WIDTH * WIDTH * WIDTH;
    private InfraredWindow() { }

    public static boolean overlaps(long previous, long next) {
        return Math.abs(SectionPos.x(next) - SectionPos.x(previous)) < WIDTH
                && Math.abs(SectionPos.y(next) - SectionPos.y(previous)) < WIDTH
                && Math.abs(SectionPos.z(next) - SectionPos.z(previous)) < WIDTH;
    }

    public static int previousPage(int page, long previous, long next) {
        int x = page % WIDTH + SectionPos.x(next) - SectionPos.x(previous);
        int z = page / WIDTH % WIDTH + SectionPos.z(next) - SectionPos.z(previous);
        int y = page / (WIDTH * WIDTH) + SectionPos.y(next) - SectionPos.y(previous);
        return x < 0 || x >= WIDTH || y < 0 || y >= WIDTH || z < 0 || z >= WIDTH
                ? -1 : x + WIDTH * (z + WIDTH * y);
    }

    public static void remap(long[] source, long[] target, long previous, long next) {
        Arrays.fill(target, 0);
        for (int page = 0; page < PAGES; page++) {
            int old = previousPage(page, previous, next);
            if (old >= 0 && (old >>> 6) < source.length && (source[old >>> 6] & 1L << (old & 63)) != 0)
                target[page >>> 6] |= 1L << (page & 63);
        }
    }

    public static void shift(ShortBuffer mirror, long previous, long next) {
        int dx = (SectionPos.x(next) - SectionPos.x(previous)) * 16;
        int dy = (SectionPos.y(next) - SectionPos.y(previous)) * 16;
        int dz = (SectionPos.z(next) - SectionPos.z(previous)) * 16;
        int x0 = Math.max(0, -dx), x1 = Math.min(SIDE, SIDE - dx);
        int y0 = Math.max(0, -dy), y1 = Math.min(SIDE, SIDE - dy);
        int z0 = Math.max(0, -dz), z1 = Math.min(SIDE, SIDE - dz);
        int offset = dx + SIDE * (dy + SIDE * dz);
        if (offset == 0) return;
        if (x0 < x1 && y0 < y1 && z0 < z1) {
            ShortBuffer source = mirror.duplicate(), target = mirror.duplicate();
            int direction = offset > 0 ? 1 : -1;
            for (int z = direction > 0 ? z0 : z1 - 1; z >= z0 && z < z1; z += direction)
                for (int y = direction > 0 ? y0 : y1 - 1; y >= y0 && y < y1; y += direction) {
                    int destination = (z * SIDE + y) * SIDE + x0;
                    source.clear().position(destination + offset).limit(destination + offset + x1 - x0);
                    target.clear().position(destination);
                    target.put(source);
                }
        }
        // Clear entering rows only after every overlapping source row has been copied.
        for (int z = 0; z < SIDE; z++) for (int y = 0; y < SIDE; y++) {
            int row = (z * SIDE + y) * SIDE;
            if (z < z0 || z >= z1 || y < y0 || y >= y1 || x0 >= x1) {
                for (int x = 0; x < SIDE; x++) mirror.put(row + x, InfraredBrickCodec.INVALID_TEMPERATURE);
            } else {
                for (int x = 0; x < x0; x++) mirror.put(row + x, InfraredBrickCodec.INVALID_TEMPERATURE);
                for (int x = x1; x < SIDE; x++) mirror.put(row + x, InfraredBrickCodec.INVALID_TEMPERATURE);
            }
        }
    }
}
