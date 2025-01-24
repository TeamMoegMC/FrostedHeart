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

import org.joml.Vector3f;

/**
 * A 2D Implementation of <a href="https://en.wikipedia.org/wiki/Metaballs">Metaballs</a>, primarily using the techniques outlined in <a href="http://jamie-wong.com/2014/08/19/metaballs-and-marching-squares/">this blog</a>
 */
public class Metaballs2D implements INoise2D {
    private final Vector3f[] balls; // x, y, weight

    public Metaballs2D(int size, Random random) {
        balls = new Vector3f[3 + random.nextInt(5)];
        for (int i = 0; i < balls.length; i++) {
            float ballSize = (0.1f + random.nextFloat() * 0.2f) * size;
            balls[i] = new Vector3f((random.nextFloat() - random.nextFloat()) * size * 0.5f, (random.nextFloat() - random.nextFloat()) * size * 0.5f, ballSize);
        }
    }

    @Override
    public float noise(float x, float z) {
        float f = 0;
        for (Vector3f ball : balls) {
            f += ball.z * Math.abs(ball.z) / ((x - ball.x) * (x - ball.x) + (z - ball.y) * (z - ball.y));
        }
        return f > 1 ? 1 : 0;
    }
}