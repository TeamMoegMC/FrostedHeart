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

import com.mojang.blaze3d.platform.GlStateManager;
import com.teammoeg.frostedheart.FHMain;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL30;

import javax.annotation.Nullable;
import java.nio.FloatBuffer;
import java.util.Locale;

/**
 * Keeps a DustColumnWindow around the camera for the client level while diamond dust glints can be drawn, and its
 * heights in a texture for diamond_dust.glsl. Each client tick centers the window on the camera's column and reads up to
 * READS_PER_TICK of its marked columns from the MOTION_BLOCKING heightmap. LightPillarWorldChanges passes block changes
 * that may change a column's height and chunks that arrive. The texture is uploaded whole when a height changed. Client
 * thread only.
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class DustColumns {
    /**
     * A whole window is read in this many ticks.
     */
    static final int READS_PER_TICK = DustColumnWindow.COLUMNS / 4;
    private static final int TICKS_PER_SECOND = 20;

    private static final DustColumnWindow window = new DustColumnWindow();
    private static final LevelColumns source = new LevelColumns();
    private static final FloatBuffer uploadBuffer = BufferUtils.createFloatBuffer(DustColumnWindow.COLUMNS);

    @Nullable
    private static ClientLevel trackedLevel;
    private static int texture;
    private static int ticksThisSecond;
    private static int currentReads;
    private static int currentMarks;
    private static int currentUploads;
    private static long currentNanos;
    private static long currentBytes;
    private static int lastReads;
    private static int lastMarks;
    private static int lastUploads;
    private static long lastNanos;
    private static long lastBytes;

    private DustColumns() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        rollStatistics();
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || !isWanted(level)) {
            stopTracking();
            return;
        }
        long allocatedBefore = LightPillarSources.allocatedBytes();
        long start = System.nanoTime();
        if (level != trackedLevel) {
            window.forget();
            trackedLevel = level;
            source.level = level;
        }
        Vec3 eye = minecraft.gameRenderer.getMainCamera().getPosition();
        window.follow(Mth.floor(eye.x), Mth.floor(eye.z));
        currentReads += window.readMarked(source, READS_PER_TICK);
        currentNanos += System.nanoTime() - start;
        currentBytes += LightPillarSources.allocatedBytes() - allocatedBefore;
    }

    private static boolean isWanted(ClientLevel level) {
        return CalculatedSkyRenderer.isAllowed(level) && Math.round(SkySetting.DUST_GLITTER_COUNT.get()) > 0
                && SkySetting.DUST_GLITTER_OPEN_AIR.isOn();
    }

    private static void stopTracking() {
        if (trackedLevel != null) {
            trackedLevel = null;
            source.level = null;
            window.forget();
        }
    }

    /**
     * Marks the column of a block change in the tracked level when the change may move its height.
     */
    static void onBlockChanged(ClientLevel level, BlockPos pos, BlockState oldState, BlockState newState) {
        if (level != trackedLevel
                || DustColumnWindow.stopsCrystals(oldState) == DustColumnWindow.stopsCrystals(newState)) {
            return;
        }
        window.markColumn(pos.getX(), pos.getZ());
        currentMarks++;
    }

    /**
     * Marks the columns of a chunk that arrived in the tracked level, a new one or one sent again.
     */
    static void onChunkLoaded(ClientLevel level, int chunkX, int chunkZ) {
        if (level == trackedLevel) {
            window.markChunk(chunkX, chunkZ);
        }
    }

    /**
     * Whether columns in the window wait to be read, so glints there do not show yet.
     */
    static boolean isReading() {
        return trackedLevel != null && window.markedCount() > 0;
    }

    /**
     * Whether the window holds a column of the client's level, which it does for the eye's column from the tick after
     * any jump or level change.
     */
    static boolean covers(int x, int z) {
        return trackedLevel != null && trackedLevel == Minecraft.getInstance().level && window.contains(x, z);
    }

    static int originX() {
        return window.originX();
    }

    static int originZ() {
        return window.originZ();
    }

    /**
     * The texture of the heights, a single float channel of SIZE by SIZE texels indexed by world column modulo SIZE,
     * made on first use and uploaded again when a height changed. The texture bound before stays bound.
     */
    static int texture() {
        boolean allocate = texture == 0;
        if (!window.takeChanged() && !allocate) {
            return texture;
        }
        uploadBuffer.clear();
        uploadBuffer.put(window.heights());
        uploadBuffer.flip();
        int target = allocate ? GL11.glGenTextures() : texture;
        int previousTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        try {
            GlStateManager._pixelStore(GL11.GL_UNPACK_ROW_LENGTH, 0);
            GlStateManager._pixelStore(GL11.GL_UNPACK_SKIP_ROWS, 0);
            GlStateManager._pixelStore(GL11.GL_UNPACK_SKIP_PIXELS, 0);
            GlStateManager._pixelStore(GL11.GL_UNPACK_ALIGNMENT, 4);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, target);
            if (allocate) {
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
                GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_R32F, DustColumnWindow.SIZE, DustColumnWindow.SIZE,
                        0, GL11.GL_RED, GL11.GL_FLOAT, uploadBuffer);
            } else {
                GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, DustColumnWindow.SIZE, DustColumnWindow.SIZE,
                        GL11.GL_RED, GL11.GL_FLOAT, uploadBuffer);
            }
        } finally {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, previousTexture);
        }
        texture = target;
        currentUploads++;
        return texture;
    }

    private static void rollStatistics() {
        ticksThisSecond++;
        if (ticksThisSecond < TICKS_PER_SECOND) {
            return;
        }
        lastReads = currentReads;
        lastMarks = currentMarks;
        lastUploads = currentUploads;
        lastNanos = currentNanos;
        lastBytes = currentBytes;
        currentReads = 0;
        currentMarks = 0;
        currentUploads = 0;
        currentNanos = 0L;
        currentBytes = 0L;
        ticksThisSecond = 0;
    }

    /**
     * A line about the columns read and uploaded in the last second and the ticks' time, for the client command.
     */
    public static String describe() {
        if (trackedLevel == null) {
            return "Dust columns are not tracked while no glints can be drawn or dustGlitterOpenAir is off";
        }
        String allocated = LightPillarSources.countsAllocations()
                ? String.format(Locale.ROOT, "%d bytes allocated", lastBytes)
                : "allocations not counted by this JVM";
        return String.format(Locale.ROOT, "Dust columns in the last second: %d read, %d marked by blocks, %d uploads, "
                        + "%.3f ms per tick, %s; %d waiting, window from %d %d", lastReads, lastMarks, lastUploads,
                lastNanos * 1.0e-6 / TICKS_PER_SECOND, allocated, window.markedCount(), window.originX(),
                window.originZ());
    }

    /**
     * Reads the first open height of a column from the MOTION_BLOCKING heightmap of the tracked level. A column of a
     * chunk not loaded reads as open from the bottom of the world.
     */
    private static final class LevelColumns implements DustColumnWindow.ColumnSource {
        @Nullable
        private ClientLevel level;

        @Override
        public int firstOpenY(int x, int z) {
            ClientLevel current = level;
            return current != null ? current.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z)
                    : (int) DustColumnWindow.CLOSED;
        }
    }
}
