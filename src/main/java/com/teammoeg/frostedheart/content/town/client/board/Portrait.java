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

package com.teammoeg.frostedheart.content.town.client.board;

import java.util.UUID;

import net.minecraft.client.gui.GuiGraphics;

/**
 * 照片里的人像：按居民编号生成的像素半身像。肤色、发色、帽子、围巾和外套随人而定；婴儿裹着兜帽，老人头发花白，
 * 有的留胡子；生病的人脸色发白、额头缠着绷带。同一个人每次画出来都一样。
 * <p>
 * The portrait in a photo: a pixel bust generated from the resident's id. Skin, hair, hat, scarf and coat vary from
 * person to person; infants are wrapped in a hood, elders have grey hair and some a beard; the sick look pale with a
 * bandage on the forehead. The same person always looks the same.
 */
public final class Portrait {
    private static final int GRID_W = 12, GRID_H = 14;
    private static final int[] SKIN = {0xFFF2D0B4, 0xFFE8B896, 0xFFD6A27E, 0xFFB57D57, 0xFF8A5A3B};
    private static final int[] HAIR = {0xFF2B1D14, 0xFF4A3020, 0xFF6E4426, 0xFFA05A2C, 0xFFD2A963};
    private static final int[] GREY = {0xFFB9B6AE, 0xFFD8D5CE, 0xFF9A968E};
    private static final int[] COAT = {0xFF55688A, 0xFF7A4E3A, 0xFF4F6A4C, 0xFF84566A, 0xFF5E5E66, 0xFF8A7346};
    private static final int[] SCARF = {0xFFB23A3A, 0xFF3A68B2, 0xFFC99A2E, 0xFF5E8C3A, 0xFFE8E4D8};
    private static final int[] HAT = {0xFF9E3B3B, 0xFF3B5B9E, 0xFF5E7A3B, 0xFF8C6A3B, 0xFF6B4E7A};
    private static final int EYE = 0xFF2A2420, BLUSH = 0xFFE89A8A, BANDAGE = 0xFFF4F1E8, BANDAGE_RED = 0xFFC0392B;

    private Portrait() {
    }

    /**
     * @param age  年龄组：0 婴儿、1 孩子、2 大人、3 老人 / age group: 0 infant, 1 child, 2 adult, 3 elder
     * @param sick 是否正在休养 / whether the resident is resting ill
     */
    public static void draw(GuiGraphics g, int x, int y, int w, int h, UUID id, int age, boolean sick) {
        // Whole-pixel cells keep the art crisp. The top row is always empty, so the cells may grow until only that row
        // would spill over the frame, and it is clipped there.
        int size = Math.max(1, Math.min(w / GRID_W, h / (GRID_H - 1)));
        int ox = x + (w - GRID_W * size) / 2, oy = y + h - GRID_H * size;
        Pixels p = new Pixels(g, ox, oy, size, y);
        long seed = id.getMostSignificantBits() * 31 + id.getLeastSignificantBits();
        int skin = pick(SKIN, seed, 1);
        if (sick) skin = blend(skin, 0xFFE6EBE0, 0.45f);
        int shade = blend(skin, 0xFF000000, 0.18f);
        int hair = age >= 3 ? pick(GREY, seed, 2) : pick(HAIR, seed, 2);
        int coat = pick(COAT, seed, 3), scarf = pick(SCARF, seed, 4), hat = pick(HAT, seed, 5);
        int style = (int) Math.floorMod(seed >>> 20, 10);

        if (age <= 0) {
            // An infant swaddled in a hooded bundle.
            p.rect(2, 4, 8, 10, coat);
            p.rect(3, 9, 6, 5, blend(coat, 0xFFFFFFFF, 0.25f));
            p.rect(3, 5, 6, 5, hat);
            p.rect(4, 6, 4, 4, skin);
            p.dot(4, 7, EYE);
            p.dot(7, 7, EYE);
            p.dot(4, 8, BLUSH);
            p.dot(7, 8, BLUSH);
            return;
        }
        boolean child = age == 1;
        int faceTop = child ? 5 : 4, faceBottom = 9;
        // Coat and shoulders.
        p.rect(1, 11, 10, 3, coat);
        p.rect(5, 11, 2, 3, blend(coat, 0xFF000000, 0.2f));
        // Neck and face.
        p.rect(5, 10, 2, 1, shade);
        p.rect(3, faceTop, 6, faceBottom - faceTop + 1, skin);
        p.dot(4, 7, EYE);
        p.dot(7, 7, EYE);
        if (child || style % 3 == 0) {
            p.dot(3, 8, BLUSH);
            p.dot(8, 8, BLUSH);
        }
        p.rect(5, 9, 2, 1, shade);
        // Hair or a knitted hat.
        boolean hatOn = style < 4;
        if (hatOn) {
            p.rect(2, faceTop - 2, 8, 3, hat);
            p.rect(2, faceTop, 8, 1, blend(hat, 0xFFFFFFFF, 0.3f));
            p.rect(5, faceTop - 3, 2, 1, 0xFFF4F1E8);
        } else {
            p.rect(3, faceTop - 1, 6, 2, hair);
            p.rect(2, faceTop, 1, style >= 7 ? 7 : 3, hair);
            p.rect(9, faceTop, 1, style >= 7 ? 7 : 3, hair);
            p.dot(3, faceTop + 1, hair);
        }
        if (age >= 3 && style % 2 == 1) {
            // A grey beard over the chin.
            p.rect(3, 9, 6, 2, hair);
            p.rect(4, 11, 4, 1, hair);
        }
        // A scarf against the cold, for most grown-ups.
        if (!child && style != 5) {
            p.rect(3, 10, 6, 1, scarf);
            p.rect(7, 11, 1, 2, scarf);
        }
        if (sick) {
            p.rect(3, faceTop + 1, 6, 1, BANDAGE);
            p.dot(4, faceTop + 1, BANDAGE_RED);
        }
    }

    private static int pick(int[] palette, long seed, int salt) {
        long mixed = seed ^ (salt * 0x9E3779B97F4A7C15L);
        mixed ^= mixed >>> 31;
        mixed *= 0xBF58476D1CE4E5B9L;
        mixed ^= mixed >>> 29;
        return palette[(int) Math.floorMod(mixed, palette.length)];
    }

    private static int blend(int a, int b, float t) {
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        return 0xFF000000 | (Math.round(ar + (br - ar) * t) << 16) | (Math.round(ag + (bg - ag) * t) << 8) | Math.round(ab + (bb - ab) * t);
    }

    /** Fills cells of the portrait grid, clipped at the top of the frame. */
    private record Pixels(GuiGraphics g, int ox, int oy, int size, int top) {
        void rect(int col, int row, int w, int h, int color) {
            if (row < 0) {
                h += row;
                row = 0;
            }
            if (w <= 0 || h <= 0) return;
            int y0 = Math.max(top, oy + row * size), y1 = oy + (row + h) * size;
            if (y1 <= y0) return;
            g.fill(ox + col * size, y0, ox + (col + w) * size, y1, color);
        }

        void dot(int col, int row, int color) {
            rect(col, row, 1, 1, color);
        }
    }
}
