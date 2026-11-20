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

package com.teammoeg.frostedheart.content.climate.render.sky;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL15C;
import org.lwjgl.opengl.GL33C;
import org.lwjgl.opengl.GLCapabilities;

import java.util.Locale;

/**
 * Measures the CPU and GPU time of a draw between begin and end, as means over the last draws. The GPU time comes
 * from timestamp queries, read some frames later so the CPU never waits for them. Timestamps do not clash with the
 * time elapsed query of Minecraft's frame timer. Without timer queries, only the CPU time is measured.
 * Render thread only.
 */
@OnlyIn(Dist.CLIENT)
final class SkyDrawTimer {
    private static final int QUERY_PAIRS = 4;
    private static final int AVERAGED_DRAWS = 120;

    private final int[] startQueries = new int[QUERY_PAIRS];
    private final int[] endQueries = new int[QUERY_PAIRS];
    private final boolean[] pending = new boolean[QUERY_PAIRS];
    private final long[] gpuNanos = new long[AVERAGED_DRAWS];
    private final long[] cpuNanos = new long[AVERAGED_DRAWS];
    private int gpuCount;
    private int gpuNext;
    private long gpuSum;
    private int cpuCount;
    private int cpuNext;
    private long cpuSum;
    private int nextPair;
    private boolean checked;
    private boolean supported;
    private boolean measuringGpu;
    private long beginNanos;

    void begin() {
        beginNanos = System.nanoTime();
        if (!checked) {
            GLCapabilities capabilities = GL.getCapabilities();
            supported = capabilities.OpenGL33 || capabilities.GL_ARB_timer_query;
            if (supported) {
                GL15C.glGenQueries(startQueries);
                GL15C.glGenQueries(endQueries);
            }
            checked = true;
        }
        if (!supported) {
            return;
        }
        collect();
        measuringGpu = !pending[nextPair];
        if (measuringGpu) {
            GL33C.glQueryCounter(startQueries[nextPair], GL33C.GL_TIMESTAMP);
        }
    }

    void end() {
        if (measuringGpu) {
            GL33C.glQueryCounter(endQueries[nextPair], GL33C.GL_TIMESTAMP);
            pending[nextPair] = true;
            nextPair = (nextPair + 1) % QUERY_PAIRS;
            measuringGpu = false;
        }
        long nanos = System.nanoTime() - beginNanos;
        cpuSum += nanos - cpuNanos[cpuNext];
        cpuNanos[cpuNext] = nanos;
        cpuNext = (cpuNext + 1) % AVERAGED_DRAWS;
        cpuCount = Math.min(cpuCount + 1, AVERAGED_DRAWS);
    }

    private void collect() {
        for (int pair = 0; pair < QUERY_PAIRS; pair++) {
            if (pending[pair] && GL15C.glGetQueryObjecti(endQueries[pair], GL15C.GL_QUERY_RESULT_AVAILABLE) != 0) {
                long nanos = GL33C.glGetQueryObjecti64(endQueries[pair], GL15C.GL_QUERY_RESULT)
                        - GL33C.glGetQueryObjecti64(startQueries[pair], GL15C.GL_QUERY_RESULT);
                pending[pair] = false;
                gpuSum += nanos - gpuNanos[gpuNext];
                gpuNanos[gpuNext] = nanos;
                gpuNext = (gpuNext + 1) % AVERAGED_DRAWS;
                gpuCount = Math.min(gpuCount + 1, AVERAGED_DRAWS);
            }
        }
    }

    /**
     * The means for the client command.
     */
    String describe() {
        if (cpuCount == 0) {
            return "no draw measured yet";
        }
        String gpu = !supported ? "GPU time not measured without timer queries"
                : gpuCount == 0 ? "GPU time not read yet"
                : String.format(Locale.ROOT, "GPU %.3f ms over the last %d draws", gpuSum / 1.0e6 / gpuCount,
                gpuCount);
        return String.format(Locale.ROOT, "%s, CPU %.3f ms over the last %d draws", gpu, cpuSum / 1.0e6 / cpuCount,
                cpuCount);
    }
}
