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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL30;

import javax.annotation.Nullable;
import java.nio.FloatBuffer;

/**
 * The textures of a sky state on the GPU, as half float RGB textures with linear filtering. Each view has a sky and an
 * ice texture, each halo map one texture, and the halo profile and the row table one each. Content that only moves to
 * another slot keeps its texture and is not uploaded again. Render thread only.
 */
@OnlyIn(Dist.CLIENT)
final class SkyTextures {
    private static final int CHANNELS = SkyAtmosphere.CHANNEL_COUNT;
    private static final int VIEW_SLOTS = SkyViewSet.VIEW_COUNT;
    private static final int HALO_SLOTS = SkyState.HALO_COUNT;
    /**
     * The row table texture holds the sun's radiance at the low and the high depth, then the diffraction weights at the
     * low and the high depth, one column each.
     */
    private static final int ROW_TABLE_COLUMNS = 4;

    @Nullable
    private FloatBuffer uploadBuffer;
    private final int[] viewSkyTextures = new int[VIEW_SLOTS];
    private final int[] viewIceTextures = new int[VIEW_SLOTS];
    private final int[] haloTextures = new int[HALO_SLOTS];
    private int haloProfileTexture;
    private int rowTableTexture;
    /**
     * What the textures of each slot hold, so that upload skips content already on the GPU.
     */
    private final Object[] uploadedViews = new Object[VIEW_SLOTS];
    private final Object[] uploadedHalos = new Object[HALO_SLOTS];
    @Nullable
    private IceHaloProfile uploadedHaloProfile;
    @Nullable
    private SkyTables uploadedLowDepthTables;
    @Nullable
    private SkyTables uploadedHighDepthTables;
    private final Object[] wantedSlots = new Object[VIEW_SLOTS];
    private final int[] slotSources = new int[VIEW_SLOTS];
    private final boolean[] slotTaken = new boolean[VIEW_SLOTS];
    private final boolean[] slotNeedsUpload = new boolean[VIEW_SLOTS];
    private final int[] reorderedTextures = new int[VIEW_SLOTS];
    private final float[] rowTableValues = new float[ROW_TABLE_COLUMNS * SkyTables.VIEW_HEIGHT * CHANNELS];

    int viewSkyTexture(int slot) {
        return viewSkyTextures[slot];
    }

    int viewIceTexture(int slot) {
        return viewIceTextures[slot];
    }

    int haloTexture(int slot) {
        return haloTextures[slot];
    }

    int haloProfileTexture() {
        return haloProfileTexture;
    }

    int rowTableTexture() {
        return rowTableTexture;
    }

    /**
     * Uploads what the textures do not hold yet of the state, so that each slot's textures hold that slot's content.
     */
    void upload(SkyState next) {
        if (uploadedHaloProfile != next.haloProfile()) {
            haloProfileTexture = uploadTable(haloProfileTexture, next.haloProfile().values(),
                    IceHaloProfile.ANGLE_BINS, 2, GL12.GL_CLAMP_TO_EDGE);
            uploadedHaloProfile = next.haloProfile();
        }
        if (uploadedLowDepthTables != next.lowDepthTables() || uploadedHighDepthTables != next.highDepthTables()) {
            fillRowTable(next.lowDepthTables(), next.highDepthTables());
            rowTableTexture = uploadTable(rowTableTexture, rowTableValues, ROW_TABLE_COLUMNS, SkyTables.VIEW_HEIGHT,
                    GL12.GL_CLAMP_TO_EDGE);
            uploadedLowDepthTables = next.lowDepthTables();
            uploadedHighDepthTables = next.highDepthTables();
        }

        for (int slot = 0; slot < HALO_SLOTS; slot++) {
            wantedSlots[slot] = next.halo(slot);
        }
        reassignSlots(uploadedHalos, HALO_SLOTS, haloTextures, null);
        for (int slot = 0; slot < HALO_SLOTS; slot++) {
            if (slotNeedsUpload[slot]) {
                haloTextures[slot] = uploadTable(haloTextures[slot], next.halo(slot).values(), IceHaloMap.AZIMUTH_BINS,
                        IceHaloMap.ELEVATION_BINS, GL14.GL_MIRRORED_REPEAT);
            }
        }

        for (int slot = 0; slot < VIEW_SLOTS; slot++) {
            wantedSlots[slot] = next.views().view(slot);
        }
        reassignSlots(uploadedViews, VIEW_SLOTS, viewSkyTextures, viewIceTextures);
        for (int slot = 0; slot < VIEW_SLOTS; slot++) {
            if (slotNeedsUpload[slot]) {
                SkyView view = next.views().view(slot);
                viewSkyTextures[slot] = uploadTable(viewSkyTextures[slot], view.sky(), SkyTables.VIEW_WIDTH,
                        SkyTables.VIEW_HEIGHT, GL11.GL_REPEAT);
                viewIceTextures[slot] = uploadTable(viewIceTextures[slot], view.ice(), SkyTables.VIEW_WIDTH,
                        SkyTables.VIEW_HEIGHT, GL11.GL_REPEAT);
            }
        }
    }

    /**
     * Moves the textures of the slots so that content in wantedSlots that a texture already holds keeps that texture,
     * and sets slotNeedsUpload for the slots whose content no texture holds. Those slots get the textures of content
     * that is no longer wanted. Afterwards the uploaded array holds the wanted content.
     */
    private void reassignSlots(Object[] uploaded, int slotCount, int[] textures, @Nullable int[] secondTextures) {
        for (int slot = 0; slot < slotCount; slot++) {
            slotSources[slot] = -1;
            slotTaken[slot] = false;
        }
        for (int slot = 0; slot < slotCount; slot++) {
            for (int source = 0; source < slotCount; source++) {
                if (uploaded[source] != null && uploaded[source] == wantedSlots[slot]) {
                    slotSources[slot] = source;
                    slotTaken[source] = true;
                    break;
                }
            }
        }
        for (int slot = 0; slot < slotCount; slot++) {
            slotNeedsUpload[slot] = slotSources[slot] < 0;
            if (!slotNeedsUpload[slot]) {
                continue;
            }
            for (int source = 0; source < slotCount; source++) {
                if (!slotTaken[source]) {
                    slotSources[slot] = source;
                    slotTaken[source] = true;
                    break;
                }
            }
        }
        reorder(textures, slotCount);
        if (secondTextures != null) {
            reorder(secondTextures, slotCount);
        }
        for (int slot = 0; slot < slotCount; slot++) {
            uploaded[slot] = wantedSlots[slot];
            wantedSlots[slot] = null;
        }
    }

    private void reorder(int[] textures, int slotCount) {
        for (int slot = 0; slot < slotCount; slot++) {
            reorderedTextures[slot] = textures[slotSources[slot]];
        }
        System.arraycopy(reorderedTextures, 0, textures, 0, slotCount);
    }

    private void fillRowTable(SkyTables lowDepthTables, SkyTables highDepthTables) {
        for (int row = 0; row < SkyTables.VIEW_HEIGHT; row++) {
            for (int channel = 0; channel < CHANNELS; channel++) {
                int source = row * CHANNELS + channel;
                int texel = row * ROW_TABLE_COLUMNS * CHANNELS + channel;
                rowTableValues[texel] = lowDepthTables.sunRadiance()[source];
                rowTableValues[texel + CHANNELS] = highDepthTables.sunRadiance()[source];
                rowTableValues[texel + 2 * CHANNELS] = lowDepthTables.diffractionWeights()[source];
                rowTableValues[texel + 3 * CHANNELS] = highDepthTables.diffractionWeights()[source];
            }
        }
    }

    /**
     * Uploads an interleaved RGB table as a half float texture with linear filtering, clamped in T.
     * A texture is allocated on its first upload and keeps its size afterwards.
     */
    private int uploadTable(int texture, float[] values, int width, int height, int wrapS) {
        if (uploadBuffer == null || uploadBuffer.capacity() < values.length) {
            uploadBuffer = BufferUtils.createFloatBuffer(values.length);
        }
        FloatBuffer buffer = uploadBuffer;
        buffer.clear();
        buffer.put(values);
        buffer.flip();
        boolean allocate = texture == 0;
        int target = allocate ? GL11.glGenTextures() : texture;
        int previousTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        try {
            GlStateManager._pixelStore(GL11.GL_UNPACK_ROW_LENGTH, 0);
            GlStateManager._pixelStore(GL11.GL_UNPACK_SKIP_ROWS, 0);
            GlStateManager._pixelStore(GL11.GL_UNPACK_SKIP_PIXELS, 0);
            GlStateManager._pixelStore(GL11.GL_UNPACK_ALIGNMENT, 4);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, target);
            if (allocate) {
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, wrapS);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
                GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_RGB16F, width, height, 0, GL11.GL_RGB, GL11.GL_FLOAT,
                        buffer);
            } else {
                GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, width, height, GL11.GL_RGB, GL11.GL_FLOAT, buffer);
            }
        } finally {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, previousTexture);
        }
        return target;
    }
}
