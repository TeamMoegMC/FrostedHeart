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
import com.teammoeg.frostedheart.mixin.client.LightEngineAccessor_LightPillars;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.lighting.LayerLightEventListener;
import net.minecraft.world.level.lighting.LayerLightSectionStorage;
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
import java.nio.ByteBuffer;
import java.util.BitSet;
import java.util.Locale;

/**
 * Keeps a DustLightVolume around the camera for the client level while glints of diamond dust lit by lamps can be
 * drawn, and its light in a 3D texture for diamond_dust.glsl. Each client tick centers the volume on the block corner
 * nearest the camera and reads up to READS_PER_TICK of its marked blocks from the block light the client sees, one
 * section's light array at a time. LightPillarWorldChanges passes the sections whose block light the client computed or
 * received from the server. The texture is uploaded whole when a light changed. Client thread only.
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class DustLights {
    /**
     * A whole volume is read in this many ticks.
     */
    static final int READS_PER_TICK = DustLightVolume.BLOCKS / 8;
    private static final int TICKS_PER_SECOND = 20;

    private static final DustLightVolume volume = new DustLightVolume();
    private static final LevelLights source = new LevelLights();
    private static final ByteBuffer uploadBuffer = BufferUtils.createByteBuffer(DustLightVolume.BLOCKS);

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

    private DustLights() {
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
            volume.forget();
            trackedLevel = level;
        }
        Vec3 eye = minecraft.gameRenderer.getMainCamera().getPosition();
        volume.follow(Mth.floor(eye.x + 0.5), Mth.floor(eye.y + 0.5), Mth.floor(eye.z + 0.5));
        source.begin(level);
        currentReads += volume.readMarked(source, READS_PER_TICK);
        source.end();
        currentNanos += System.nanoTime() - start;
        currentBytes += LightPillarSources.allocatedBytes() - allocatedBefore;
    }

    private static boolean isWanted(ClientLevel level) {
        return CalculatedSkyRenderer.isAllowed(level) && Math.round(SkySetting.DUST_GLITTER_COUNT.get()) > 0
                && SkySetting.DUST_LAMP_GLITTER.get() > 0.0;
    }

    private static void stopTracking() {
        if (trackedLevel != null) {
            trackedLevel = null;
            volume.forget();
        }
    }

    /**
     * Marks a section whose block light the client computed in the tracked level. The new values are readable now.
     */
    static void onLightUpdated(ClientLevel level, SectionPos section) {
        if (level == trackedLevel && volume.markSection(section.x(), section.y(), section.z())) {
            currentMarks++;
        }
    }

    /**
     * Marks the sections of a column whose block light came from the server in the tracked level, those whose bit is
     * set in either mask, counted from the lowest light section. The new values are readable before the next tick.
     */
    static void onLightReceived(ClientLevel level, int chunkX, int chunkZ, BitSet dataMask, BitSet emptyMask,
            int lowestLightSection, int lightSectionCount) {
        if (level != trackedLevel) {
            return;
        }
        for (int index = 0; index < lightSectionCount; index++) {
            if ((dataMask.get(index) || emptyMask.get(index))
                    && volume.markSection(chunkX, lowestLightSection + index, chunkZ)) {
                currentMarks++;
            }
        }
    }

    /**
     * Whether blocks in the volume wait to be read, so lamp glints there are not lit yet.
     */
    static boolean isReading() {
        return trackedLevel != null && volume.markedCount() > 0;
    }

    /**
     * Whether the volume holds a block of the client's level, which it does for the eye's block from the tick after
     * any jump or level change.
     */
    static boolean covers(int x, int y, int z) {
        return trackedLevel != null && trackedLevel == Minecraft.getInstance().level && volume.contains(x, y, z);
    }

    static int originX() {
        return volume.originX();
    }

    static int originY() {
        return volume.originY();
    }

    static int originZ() {
        return volume.originZ();
    }

    /**
     * The 3D texture of the light, a single channel of SIZE texels on each side indexed by world position modulo SIZE,
     * filtered between texels and repeating, made on first use and uploaded again when a light changed. The 3D texture
     * bound before stays bound.
     */
    static int texture() {
        boolean allocate = texture == 0;
        if (!volume.takeChanged() && !allocate) {
            return texture;
        }
        uploadBuffer.clear();
        uploadBuffer.put(volume.values());
        uploadBuffer.flip();
        int target = allocate ? GL11.glGenTextures() : texture;
        int previousTexture = GL11.glGetInteger(GL12.GL_TEXTURE_BINDING_3D);
        try {
            GlStateManager._pixelStore(GL11.GL_UNPACK_ROW_LENGTH, 0);
            GlStateManager._pixelStore(GL11.GL_UNPACK_SKIP_ROWS, 0);
            GlStateManager._pixelStore(GL11.GL_UNPACK_SKIP_PIXELS, 0);
            GlStateManager._pixelStore(GL12.GL_UNPACK_IMAGE_HEIGHT, 0);
            GlStateManager._pixelStore(GL12.GL_UNPACK_SKIP_IMAGES, 0);
            GlStateManager._pixelStore(GL11.GL_UNPACK_ALIGNMENT, 4);
            GL11.glBindTexture(GL12.GL_TEXTURE_3D, target);
            if (allocate) {
                GL11.glTexParameteri(GL12.GL_TEXTURE_3D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
                GL11.glTexParameteri(GL12.GL_TEXTURE_3D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
                GL11.glTexParameteri(GL12.GL_TEXTURE_3D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
                GL11.glTexParameteri(GL12.GL_TEXTURE_3D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
                GL11.glTexParameteri(GL12.GL_TEXTURE_3D, GL12.GL_TEXTURE_WRAP_R, GL11.GL_REPEAT);
                GL11.glTexParameteri(GL12.GL_TEXTURE_3D, GL12.GL_TEXTURE_MAX_LEVEL, 0);
                GL12.glTexImage3D(GL12.GL_TEXTURE_3D, 0, GL30.GL_R8, DustLightVolume.SIZE, DustLightVolume.SIZE,
                        DustLightVolume.SIZE, 0, GL11.GL_RED, GL11.GL_UNSIGNED_BYTE, uploadBuffer);
            } else {
                GL12.glTexSubImage3D(GL12.GL_TEXTURE_3D, 0, 0, 0, 0, DustLightVolume.SIZE, DustLightVolume.SIZE,
                        DustLightVolume.SIZE, GL11.GL_RED, GL11.GL_UNSIGNED_BYTE, uploadBuffer);
            }
        } finally {
            GL11.glBindTexture(GL12.GL_TEXTURE_3D, previousTexture);
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
     * A line about the blocks read and uploaded in the last second and the ticks' time, for the client command.
     */
    public static String describe() {
        if (trackedLevel == null) {
            return "Dust lights are not tracked while no glints can be drawn or dustLampGlitter is 0";
        }
        String allocated = LightPillarSources.countsAllocations()
                ? String.format(Locale.ROOT, "%d bytes allocated", lastBytes)
                : "allocations not counted by this JVM";
        return String.format(Locale.ROOT, "Dust lights in the last second: %d blocks read, %d sections marked by light, "
                        + "%d uploads, %.3f ms per tick, %s; %d waiting, volume from %d %d %d", lastReads, lastMarks,
                lastUploads, lastNanos * 1.0e-6 / TICKS_PER_SECOND, allocated, volume.markedCount(),
                volume.originX(), volume.originY(), volume.originZ());
    }

    /**
     * Reads the block light the client sees, keeping the light array of the last section read. A section without one
     * has no block light. The storage and the array are taken again each tick, since the light engine replaces them.
     */
    private static final class LevelLights implements DustLightVolume.LightSource {
        @Nullable
        private LayerLightSectionStorage<?> storage;
        @Nullable
        private DataLayer section;
        private long sectionKey;
        private boolean hasSection;

        private void begin(ClientLevel level) {
            LayerLightEventListener listener = level.getLightEngine().getLayerListener(LightLayer.BLOCK);
            storage = listener instanceof LightEngineAccessor_LightPillars accessor
                    ? accessor.frostedheart$getStorage() : null;
            hasSection = false;
        }

        private void end() {
            storage = null;
            section = null;
            hasSection = false;
        }

        @Override
        public int blockLight(int x, int y, int z) {
            LayerLightSectionStorage<?> current = storage;
            if (current == null) {
                return 0;
            }
            long key = SectionPos.asLong(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(y),
                    SectionPos.blockToSectionCoord(z));
            if (!hasSection || key != sectionKey) {
                section = current.getDataLayerData(key);
                sectionKey = key;
                hasSection = true;
            }
            DataLayer light = section;
            return light != null ? light.get(SectionPos.sectionRelative(x), SectionPos.sectionRelative(y),
                    SectionPos.sectionRelative(z)) : 0;
        }
    }
}
