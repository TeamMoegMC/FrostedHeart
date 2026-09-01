/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */
package com.teammoeg.frostedheart.content.climate.render;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.client.shader.management.ShaderManager;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.bootstrap.client.FHShaders;
import com.teammoeg.frostedheart.content.climate.network.FHRequestInfraredViewDataSyncPacket;
import com.teammoeg.frostedheart.content.climate.network.InfraredBrickCodec;
import com.teammoeg.frostedheart.mixin.oculus.IrisRenderingPipelineAccess;

import io.netty.buffer.Unpooled;
import net.irisshaders.iris.Iris;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;

import javax.annotation.Nullable;
import java.nio.ShortBuffer;
import java.util.Arrays;

@OnlyIn(Dist.CLIENT)
public final class InfraredViewRenderer {
    private static final int EXPANDING_TICKS = 20;
    private static final int SCAN_RADIUS_BLOCKS = 64;
    private static final int PAGE_RADIUS = 4;
    private static final int PAGE_WIDTH = 9;
    private static final int BLOCKS_PER_PAGE_AXIS = 16;
    private static final int TEXTURE_SIZE =
            PAGE_WIDTH * BLOCKS_PER_PAGE_AXIS;
    private static final int TEXTURE_TEXELS =
            TEXTURE_SIZE * TEXTURE_SIZE * TEXTURE_SIZE;
    private static final int PAGE_TEXELS = 16 * 16 * 16;
    private static final int REFRESH_TICKS = 40;
    private static final int FULL_RETRY_MIN_TICKS = 41;
    private static final int FULL_RETRY_SPREAD_TICKS = 19;
    private static final float RADIUS_DUR =
            (float) SCAN_RADIUS_BLOCKS / EXPANDING_TICKS;

    @Nullable
    private static ShortBuffer temperatureMirror;
    @Nullable
    private static ShortBuffer pageUpload;
    private static final long[] knownPresence =
            new long[FHRequestInfraredViewDataSyncPacket.PRESENCE_WORDS];
    private static final long[] dirtyUploadPages =
            new long[FHRequestInfraredViewDataSyncPacket.PRESENCE_WORDS];
    private static final short[] decodedBrick =
            new short[InfraredBrickCodec.BLOCKS_PER_BRICK];
    private static final InfraredBrickCodec.Decoder brickDecoder =
            new InfraredBrickCodec.Decoder();

    @Nullable
    private static PoseStack cameraPose;
    @Nullable
    private static RenderTarget overlayTarget;
    private static boolean open;
    private static boolean deltaBaselineValid;
    private static boolean requestCenterValid;
    private static int requestId;
    private static long lastRequestTick = Long.MIN_VALUE;
    private static int infraredEpoch;
    private static int requestedChunkX;
    private static int requestedChunkZ;
    private static int requestedSectionY;
    private static int textureCenterChunkX;
    private static int textureCenterChunkZ;
    private static int textureCenterSectionY;
    private static int temperatureTexture;
    private static float radius;

    private InfraredViewRenderer() {
    }

    public static void setCameraPose(@Nullable PoseStack pose) {
        cameraPose = pose;
    }

    public static void toggleInfraredView() {
        open = !open;
        if (!open) {
            invalidateRequests();
        }
    }

    public static void clientTick() {
        if (open && radius < SCAN_RADIUS_BLOCKS) {
            radius = Mth.clamp(
                    radius + RADIUS_DUR, 0.0F, SCAN_RADIUS_BLOCKS);
        } else if (!open && radius > 0.0F) {
            radius = Mth.clamp(
                    radius - RADIUS_DUR, 0.0F, SCAN_RADIUS_BLOCKS);
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (!open || minecraft.player == null || minecraft.level == null) {
            return;
        }
        int centerChunkX = Mth.floor(minecraft.player.getX()) >> 4;
        int centerChunkZ = Mth.floor(minecraft.player.getZ()) >> 4;
        int centerSectionY = Mth.floor(minecraft.player.getEyeY()) >> 4;
        boolean forceFull = !requestCenterValid
                || centerChunkX != requestedChunkX
                || centerChunkZ != requestedChunkZ
                || centerSectionY != requestedSectionY;
        long gameTick = minecraft.level.getGameTime();
        boolean awaitingFull = requestCenterValid && !deltaBaselineValid;
        boolean retryFull = awaitingFull
                && gameTick - lastRequestTick
                >= FULL_RETRY_MIN_TICKS + Math.floorMod(
                        minecraft.player.getId(), FULL_RETRY_SPREAD_TICKS);
        boolean periodic = !awaitingFull && Math.floorMod(
                gameTick + minecraft.player.getId(), REFRESH_TICKS) == 0L;
        if ((forceFull || retryFull || periodic)
                && lastRequestTick != gameTick) {
            sendRequest(
                    centerChunkX, centerChunkZ, centerSectionY,
                    forceFull, gameTick);
        }
    }

    private static void sendRequest(
            int centerChunkX,
            int centerChunkZ,
            int centerSectionY,
            boolean forceFull,
            long gameTick
    ) {
        requestId = nextRequestId(requestId);
        lastRequestTick = gameTick;
        requestCenterValid = true;
        requestedChunkX = centerChunkX;
        requestedChunkZ = centerChunkZ;
        requestedSectionY = centerSectionY;
        if (forceFull) {
            // Keep retries full until a matching response installs this origin.
            deltaBaselineValid = false;
        }
        FHNetwork.INSTANCE.sendToServer(
                new FHRequestInfraredViewDataSyncPacket(
                        requestId,
                        forceFull || !deltaBaselineValid,
                        infraredEpoch,
                        knownPresence));
    }

    public static void updateData(
            int responseRequestId,
            int centerChunkX,
            int centerChunkZ,
            int centerSectionY,
            int responseInfraredEpoch,
            boolean full,
            long[] presence,
            byte[] brickRecords
    ) {
        RenderSystem.assertOnRenderThread();
        if (!open || responseRequestId != requestId
                || !acceptResponseCenter(
                        full,
                        centerChunkX,
                        centerChunkZ,
                        centerSectionY)) {
            return;
        }
        boolean createdMirror = ensureTemperatureMirror();
        Arrays.fill(dirtyUploadPages, 0L);
        if (full) {
            if (!createdMirror) {
                clearMirror();
            }
        } else if (presence.length != 0) {
            for (int localPageIndex = 0;
                    localPageIndex < PAGE_WIDTH * PAGE_WIDTH * PAGE_WIDTH;
                    localPageIndex++) {
                boolean wasPresent = presenceBit(
                        knownPresence, localPageIndex);
                boolean isPresent = presenceBit(
                        presence, localPageIndex);
                if (wasPresent != isPresent) {
                    clearPage(localPageIndex);
                }
            }
        }

        FriendlyByteBuf input = new FriendlyByteBuf(
                Unpooled.wrappedBuffer(brickRecords));
        try {
            int localBrickIndex;
            while ((localBrickIndex = brickDecoder.readBrick(
                    input, decodedBrick)) >= 0) {
                writeBrick(localBrickIndex, decodedBrick);
            }
        } finally {
            input.release();
        }
        if (presence.length != 0) {
            System.arraycopy(
                    presence, 0, knownPresence, 0, knownPresence.length);
        }
        textureCenterChunkX = centerChunkX;
        textureCenterChunkZ = centerChunkZ;
        textureCenterSectionY = centerSectionY;
        infraredEpoch = responseInfraredEpoch;
        deltaBaselineValid = true;
        if (full) {
            uploadFullTemperatureTexture();
        } else {
            uploadDirtyPages();
        }
        Arrays.fill(dirtyUploadPages, 0L);
    }

    private static boolean acceptResponseCenter(
            boolean full,
            int centerChunkX,
            int centerChunkZ,
            int centerSectionY
    ) {
        if (!full && (!deltaBaselineValid
                || centerChunkX != textureCenterChunkX
                || centerChunkZ != textureCenterChunkZ
                || centerSectionY != textureCenterSectionY)) {
            deltaBaselineValid = false;
            requestCenterValid = false;
            return false;
        }
        requestedChunkX = centerChunkX;
        requestedChunkZ = centerChunkZ;
        requestedSectionY = centerSectionY;
        requestCenterValid = true;
        return true;
    }

    private static void clearMirror() {
        ShortBuffer mirror = temperatureMirror;
        if (mirror == null) {
            return;
        }
        mirror.clear();
        while (mirror.hasRemaining()) {
            mirror.put(InfraredBrickCodec.INVALID_TEMPERATURE);
        }
        mirror.clear();
    }

    private static boolean ensureTemperatureMirror() {
        if (temperatureMirror != null) {
            return false;
        }
        temperatureMirror = BufferUtils.createShortBuffer(TEXTURE_TEXELS);
        clearMirror();
        return true;
    }

    private static void clearPage(int localPageIndex) {
        int pageX = localPageIndex % PAGE_WIDTH;
        int pageZ = localPageIndex / PAGE_WIDTH % PAGE_WIDTH;
        int pageY = localPageIndex / (PAGE_WIDTH * PAGE_WIDTH);
        int baseX = pageX * BLOCKS_PER_PAGE_AXIS;
        int baseY = pageY * BLOCKS_PER_PAGE_AXIS;
        int baseZ = pageZ * BLOCKS_PER_PAGE_AXIS;
        for (int z = 0; z < BLOCKS_PER_PAGE_AXIS; z++) {
            for (int y = 0; y < BLOCKS_PER_PAGE_AXIS; y++) {
                int offset = ((baseZ + z) * TEXTURE_SIZE + baseY + y)
                        * TEXTURE_SIZE + baseX;
                for (int x = 0; x < BLOCKS_PER_PAGE_AXIS; x++) {
                    temperatureMirror.put(
                            offset + x,
                            InfraredBrickCodec.INVALID_TEMPERATURE);
                }
            }
        }
        markDirtyPage(localPageIndex);
    }

    private static void writeBrick(
            int localBrickIndex,
            short[] values
    ) {
        int localPageIndex = localBrickIndex >>> 6;
        int brickIndex = localBrickIndex & 63;
        int pageX = localPageIndex % PAGE_WIDTH;
        int pageZ = localPageIndex / PAGE_WIDTH % PAGE_WIDTH;
        int pageY = localPageIndex / (PAGE_WIDTH * PAGE_WIDTH);
        int baseX = pageX * BLOCKS_PER_PAGE_AXIS + (brickIndex & 3) * 4;
        int baseZ = pageZ * BLOCKS_PER_PAGE_AXIS
                + (brickIndex >>> 2 & 3) * 4;
        int baseY = pageY * BLOCKS_PER_PAGE_AXIS
                + (brickIndex >>> 4) * 4;
        for (int block = 0; block < InfraredBrickCodec.BLOCKS_PER_BRICK;
                block++) {
            int textureX = baseX + (block & 3);
            int textureZ = baseZ + (block >>> 2 & 3);
            int textureY = baseY + (block >>> 4);
            temperatureMirror.put(
                    (textureZ * TEXTURE_SIZE + textureY) * TEXTURE_SIZE
                            + textureX,
                    values[block]);
        }
        markDirtyPage(localPageIndex);
    }

    private static void markDirtyPage(int localPageIndex) {
        dirtyUploadPages[localPageIndex >>> 6] |=
                1L << (localPageIndex & 63);
    }

    private static boolean presenceBit(long[] presence, int localPageIndex) {
        return (presence[localPageIndex >>> 6]
                & 1L << (localPageIndex & 63)) != 0L;
    }

    private static int getOrCreateTemperatureTexture() {
        if (temperatureTexture != 0) {
            return temperatureTexture;
        }
        uploadFullTemperatureTexture();
        return temperatureTexture;
    }

    private static void uploadFullTemperatureTexture() {
        ensureTemperatureMirror();
        boolean allocate = temperatureTexture == 0;
        if (allocate) {
            temperatureTexture = GL11.glGenTextures();
        }
        int previousTexture = GL11.glGetInteger(
                GL12.GL_TEXTURE_BINDING_3D);
        try {
            resetTextureUploadState();
            GL11.glBindTexture(GL12.GL_TEXTURE_3D, temperatureTexture);
            if (allocate) {
                GL11.glTexParameteri(
                        GL12.GL_TEXTURE_3D,
                        GL11.GL_TEXTURE_MIN_FILTER,
                        GL11.GL_NEAREST);
                GL11.glTexParameteri(
                        GL12.GL_TEXTURE_3D,
                        GL11.GL_TEXTURE_MAG_FILTER,
                        GL11.GL_NEAREST);
                GL11.glTexParameteri(
                        GL12.GL_TEXTURE_3D,
                        GL11.GL_TEXTURE_WRAP_S,
                        GL12.GL_CLAMP_TO_EDGE);
                GL11.glTexParameteri(
                        GL12.GL_TEXTURE_3D,
                        GL11.GL_TEXTURE_WRAP_T,
                        GL12.GL_CLAMP_TO_EDGE);
                GL11.glTexParameteri(
                        GL12.GL_TEXTURE_3D,
                        GL12.GL_TEXTURE_WRAP_R,
                        GL12.GL_CLAMP_TO_EDGE);
                GL12.glTexImage3D(
                        GL12.GL_TEXTURE_3D,
                        0,
                        GL30.GL_R16I,
                        TEXTURE_SIZE, TEXTURE_SIZE, TEXTURE_SIZE,
                        0,
                         GL30.GL_RED_INTEGER,
                         GL11.GL_SHORT,
                         0L);
            }
            temperatureMirror.clear();
            GL12.glTexSubImage3D(
                    GL12.GL_TEXTURE_3D,
                    0,
                    0, 0, 0,
                    TEXTURE_SIZE, TEXTURE_SIZE, TEXTURE_SIZE,
                    GL30.GL_RED_INTEGER,
                    GL11.GL_SHORT,
                    temperatureMirror);
        } finally {
            GL11.glBindTexture(GL12.GL_TEXTURE_3D, previousTexture);
        }
    }

    private static void uploadDirtyPages() {
        boolean dirty = false;
        for (long word : dirtyUploadPages) {
            dirty |= word != 0L;
        }
        if (!dirty) {
            return;
        }
        if (temperatureTexture == 0) {
            getOrCreateTemperatureTexture();
            return;
        }

        int previousTexture = GL11.glGetInteger(
                GL12.GL_TEXTURE_BINDING_3D);
        try {
            resetTextureUploadState();
            GL11.glBindTexture(GL12.GL_TEXTURE_3D, temperatureTexture);
            for (int wordIndex = 0;
                    wordIndex < dirtyUploadPages.length;
                    wordIndex++) {
                long word = dirtyUploadPages[wordIndex];
                while (word != 0L) {
                    int bit = Long.numberOfTrailingZeros(word);
                    int localPageIndex = (wordIndex << 6) + bit;
                    if (localPageIndex
                            < PAGE_WIDTH * PAGE_WIDTH * PAGE_WIDTH) {
                        uploadPage(localPageIndex);
                    }
                    word &= word - 1L;
                }
            }
        } finally {
            GL11.glBindTexture(GL12.GL_TEXTURE_3D, previousTexture);
        }
    }

    private static void uploadPage(int localPageIndex) {
        int pageX = localPageIndex % PAGE_WIDTH;
        int pageZ = localPageIndex / PAGE_WIDTH % PAGE_WIDTH;
        int pageY = localPageIndex / (PAGE_WIDTH * PAGE_WIDTH);
        int baseX = pageX * BLOCKS_PER_PAGE_AXIS;
        int baseY = pageY * BLOCKS_PER_PAGE_AXIS;
        int baseZ = pageZ * BLOCKS_PER_PAGE_AXIS;
        ShortBuffer upload = pageUpload;
        if (upload == null) {
            upload = BufferUtils.createShortBuffer(PAGE_TEXELS);
            pageUpload = upload;
        }
        upload.clear();
        for (int z = 0; z < BLOCKS_PER_PAGE_AXIS; z++) {
            for (int y = 0; y < BLOCKS_PER_PAGE_AXIS; y++) {
                int offset = ((baseZ + z) * TEXTURE_SIZE + baseY + y)
                        * TEXTURE_SIZE + baseX;
                for (int x = 0; x < BLOCKS_PER_PAGE_AXIS; x++) {
                    upload.put(temperatureMirror.get(offset + x));
                }
            }
        }
        upload.flip();
        GL12.glTexSubImage3D(
                GL12.GL_TEXTURE_3D,
                0,
                baseX, baseY, baseZ,
                BLOCKS_PER_PAGE_AXIS,
                BLOCKS_PER_PAGE_AXIS,
                BLOCKS_PER_PAGE_AXIS,
                GL30.GL_RED_INTEGER,
                GL11.GL_SHORT,
                upload);
    }

    private static void resetTextureUploadState() {
        GlStateManager._pixelStore(GL11.GL_UNPACK_ROW_LENGTH, 0);
        GlStateManager._pixelStore(GL11.GL_UNPACK_SKIP_ROWS, 0);
        GlStateManager._pixelStore(GL11.GL_UNPACK_SKIP_PIXELS, 0);
        GlStateManager._pixelStore(GL11.GL_UNPACK_ALIGNMENT, 4);
    }

    public static void renderInfraredView() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.cameraEntity == null || minecraft.player == null
                || radius <= 0.0F || cameraPose == null) {
            return;
        }
        if (temperatureTexture == 0) {
            initializeEmptyTemperatureTexture(minecraft);
        }
        RenderTarget mainTarget = minecraft.getMainRenderTarget();
        RenderTarget overlay = getOrCreateOverlayTarget(
                mainTarget.width, mainTarget.height);
        float partialTicks = minecraft.getFrameTime();
        Vec3 cameraPos = minecraft.gameRenderer.getMainCamera().getPosition();

        RenderSystem.depthMask(false);
        RenderSystem.disableDepthTest();
        overlay.clear(Minecraft.ON_OSX);
        ShaderManager.getInstance().renderFullImageInFramebuffer(
                overlay,
                FHShaders.getInfraredView(),
                uniforms -> {
                    uniforms.glUniform1F(
                            "radius",
                            Mth.clamp(
                                    radius + partialTicks * RADIUS_DUR
                                            * (open ? 1.0F : -1.0F),
                                    0.0F,
                                    SCAN_RADIUS_BLOCKS));

                    RenderSystem.activeTexture(GL13.GL_TEXTURE0);
                    RenderSystem.bindTexture(mainTarget.getColorTextureId());
                    uniforms.glUniform1I("mainTexture", 0);

                    RenderSystem.activeTexture(GL13.GL_TEXTURE1);
                    RenderSystem.bindTexture(mainTarget.getDepthTextureId());
                    uniforms.glUniform1I("depthTexture", 1);

                    RenderSystem.activeTexture(GL13.GL_TEXTURE2);
                    if (LDLib.isOculusLoaded()
                            && Iris.getPipelineManager().getPipeline()
                            .orElse(null) instanceof IrisRenderingPipelineAccess access) {
                        RenderSystem.bindTexture(
                                access.getRenderTargets()
                                        .getDepthTextureNoHand().getTextureId());
                    } else {
                        RenderSystem.bindTexture(mainTarget.getDepthTextureId());
                    }
                    uniforms.glUniform1I("noHandDepthTexture", 2);

                    RenderSystem.activeTexture(GL13.GL_TEXTURE3);
                    if (LDLib.isOculusLoaded()
                            && Iris.getPipelineManager().getPipeline()
                            .orElse(null) instanceof IrisRenderingPipelineAccess access) {
                        RenderSystem.bindTexture(
                                access.getRenderTargets()
                                        .getDepthTextureNoTranslucents().getTextureId());
                    } else {
                        RenderSystem.bindTexture(mainTarget.getDepthTextureId());
                    }
                    uniforms.glUniform1I("noTranslucentDepthTexture", 3);

                    RenderSystem.activeTexture(GL13.GL_TEXTURE4);
                    GL11.glBindTexture(
                            GL12.GL_TEXTURE_3D,
                            temperatureTexture);
                    uniforms.glUniform1I("temperatureTexture", 4);
                    uniforms.glUniform3F(
                            "temperatureCameraOffset",
                            (float) (cameraPos.x
                                    - (textureCenterChunkX - PAGE_RADIUS)
                                    * 16.0D),
                            (float) (cameraPos.y
                                    - (textureCenterSectionY - PAGE_RADIUS)
                                    * 16.0D),
                            (float) (cameraPos.z
                                    - (textureCenterChunkZ - PAGE_RADIUS)
                                    * 16.0D));

                    uniforms.glUniformMatrix4F(
                            "u_InverseProjectionMatrix",
                            RenderSystem.getProjectionMatrix().invert(new Matrix4f()));
                    uniforms.glUniformMatrix4F(
                            "u_InverseViewMatrix",
                            cameraPose.last().pose().invert(new Matrix4f()));
                },
                null);

        ShaderManager.getInstance().renderFullImageInFramebuffer(
                mainTarget,
                FHShaders.IMAGE_F,
                uniforms -> {
                    RenderSystem.activeTexture(GL13.GL_TEXTURE0);
                    RenderSystem.bindTexture(overlay.getColorTextureId());
                    uniforms.glUniform1I("DiffuseSampler", 0);
                },
                null);

        RenderSystem.activeTexture(GL13.GL_TEXTURE0);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        cameraPose = null;
    }

    private static void initializeEmptyTemperatureTexture(
            Minecraft minecraft
    ) {
        if (!ensureTemperatureMirror()) {
            clearMirror();
        }
        textureCenterChunkX = Mth.floor(minecraft.player.getX()) >> 4;
        textureCenterChunkZ = Mth.floor(minecraft.player.getZ()) >> 4;
        textureCenterSectionY = Mth.floor(minecraft.player.getEyeY()) >> 4;
        uploadFullTemperatureTexture();
    }

    public static void reset() {
        invalidateRequests();
        open = false;
        radius = 0.0F;
        deltaBaselineValid = false;
        infraredEpoch = 0;
        Arrays.fill(knownPresence, 0L);
        Arrays.fill(dirtyUploadPages, 0L);
        cameraPose = null;
        int textureToDelete = temperatureTexture;
        temperatureTexture = 0;
        RenderTarget overlayToDelete = overlayTarget;
        overlayTarget = null;
        Runnable release = () -> {
            if (textureToDelete != 0) {
                GL11.glDeleteTextures(textureToDelete);
            }
            if (overlayToDelete != null) {
                overlayToDelete.destroyBuffers();
            }
        };
        if (RenderSystem.isOnRenderThread()) {
            release.run();
        } else {
            RenderSystem.recordRenderCall(release::run);
        }
    }

    private static void invalidateRequests() {
        requestId = nextRequestId(requestId);
        requestCenterValid = false;
        lastRequestTick = Long.MIN_VALUE;
    }

    private static int nextRequestId(int current) {
        return current == Integer.MAX_VALUE ? 0 : current + 1;
    }

    private static RenderTarget getOrCreateOverlayTarget(int width, int height) {
        if (overlayTarget == null) {
            overlayTarget = new TextureTarget(
                    width, height, false, Minecraft.ON_OSX);
            overlayTarget.setClearColor(0, 0, 0, 0);
        } else if (overlayTarget.width != width
                || overlayTarget.height != height) {
            overlayTarget.resize(width, height, Minecraft.ON_OSX);
        }
        return overlayTarget;
    }
}
