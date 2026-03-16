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

package com.teammoeg.chorda.math.noise;

import java.util.Random;

import org.joml.Vector3f;

/**
 * 二维元球(Metaballs)噪声实现，生成基于影响场的有机形状。
 * <p>
 * 2D Metaballs noise implementation, generating organic shapes based on influence fields.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Metaballs">Metaballs - Wikipedia</a>
 */
public class Metaballs2D implements INoise2D {
    private final Vector3f[] balls; // x, y, weight

    /**
     * 构造一个指定大小的二维元球噪声，随机生成3-7个元球。
     * <p>
     * Constructs a 2D metaballs noise of the specified size, randomly generating 3-7 metaballs.
     *
     * @param size 元球分布区域大小 / the size of the metaball distribution area
     * @param random 随机数生成器 / the random number generator
     */
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