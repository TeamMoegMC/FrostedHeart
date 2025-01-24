/*
 * Copyright (c) 2021-2024 TeamMoeg
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

package com.teammoeg.chorda.math.noise;

import java.util.Random;

import org.joml.Vector4f;

public class Metaballs3D implements INoise3D {
    private final Vector4f[] balls; // x, y, z, weight

    public Metaballs3D(int size, Random random) {
        balls = new Vector4f[5 + random.nextInt(7)];
        for (int i = 0; i < balls.length; i++) {
            float ballSize = (0.1f + random.nextFloat() * 0.2f) * size;
            balls[i] = new Vector4f((random.nextFloat() - random.nextFloat()) * size * 0.5f, (random.nextFloat() - random.nextFloat()) * size * 0.5f, (random.nextFloat() - random.nextFloat()) * size * 0.5f, ballSize);
        }
    }

    @Override
    public float noise(float x, float y, float z) {
        float f = 0;
        for (Vector4f ball : balls) {
            f += ball.w * Math.abs(ball.w) / ((x - ball.x) * (x - ball.x) + (y - ball.y) * (y - ball.y) + (z - ball.z) * (z - ball.z));
            if (f > 1) {
                return 1; // Shortcut out of the loop if possible
            }
        }
        return f > 1 ? 1 : 0;
    }
}