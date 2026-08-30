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
import com.teammoeg.frostedheart.content.climate.network.FHResponseInfraredViewDataSyncPacket;
import com.teammoeg.frostedheart.mixin.oculus.IrisRenderingPipelineAccess;

import net.irisshaders.iris.Iris;
import net.minecraft.client.Minecraft;
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
    private static final int BRICKS_PER_PAGE_AXIS = 4;
    private static final int TEXTURE_SIZE =
            PAGE_WIDTH * BRICKS_PER_PAGE_AXIS;
    private static final int TEXTURE_TEXELS =
            TEXTURE_SIZE * TEXTURE_SIZE * TEXTURE_SIZE;
    private static final int REFRESH_TICKS = 40;
    private static final float RADIUS_DUR =
            (float) SCAN_RADIUS_BLOCKS / EXPANDING_TICKS;

    private static final short[] temperatureMirror =
            new short[TEXTURE_TEXELS];
    private static final ShortBuffer temperatureUpload =
            BufferUtils.createShortBuffer(TEXTURE_TEXELS);
    private static final long[] knownPresence =
            new long[FHRequestInfraredViewDataSyncPacket.PRESENCE_WORDS];

    @Nullable
    private static PoseStack cameraPose;
    @Nullable
    private static RenderTarget overlayTarget;
    private static boolean open;
    private static boolean snapshotAvailable;
    private static boolean requestCenterValid;
    private static int requestId;
    private static long lastRequestTick = Long.MIN_VALUE;
    private static long temperatureChangeId;
    private static int requestedChunkX;
    private static int requestedChunkZ;
    private static int requestedSectionY;
    private static int textureCenterChunkX;
    private static int textureCenterChunkZ;
    private static int textureCenterSectionY;
    private static int temperatureTexture;
    private static float radius;

    static {
        Arrays.fill(temperatureMirror, Short.MIN_VALUE);
    }

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
        boolean periodic = Math.floorMod(
                gameTick + minecraft.player.getId(), REFRESH_TICKS) == 0L;
        if ((forceFull || periodic) && lastRequestTick != gameTick) {
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
        FHNetwork.INSTANCE.sendToServer(
                new FHRequestInfraredViewDataSyncPacket(
                        requestId,
                        forceFull || !snapshotAvailable,
                        temperatureChangeId,
                        knownPresence));
    }

    public static void updateData(
            int responseRequestId,
            int centerChunkX,
            int centerChunkZ,
            int centerSectionY,
            long responseTemperatureChangeId,
            boolean full,
            short[] pageRecords
    ) {
        RenderSystem.assertOnRenderThread();
        if (!open || responseRequestId != requestId) {
            return;
        }
        boolean upload = full || pageRecords.length != 0;
        if (full) {
            Arrays.fill(temperatureMirror, Short.MIN_VALUE);
            Arrays.fill(knownPresence, 0L);
        }
        int pageCount =
                pageRecords.length / FHResponseInfraredViewDataSyncPacket.RECORD_SHORTS;
        for (int page = 0; page < pageCount; page++) {
            int recordOffset =
                    page * FHResponseInfraredViewDataSyncPacket.RECORD_SHORTS;
            int localPageIndex =
                    Short.toUnsignedInt(pageRecords[recordOffset]);
            if (localPageIndex >= PAGE_WIDTH * PAGE_WIDTH * PAGE_WIDTH) {
                continue;
            }
            knownPresence[localPageIndex >>> 6] |=
                    1L << (localPageIndex & 63);
            writePage(localPageIndex, pageRecords, recordOffset + 1);
        }
        textureCenterChunkX = centerChunkX;
        textureCenterChunkZ = centerChunkZ;
        textureCenterSectionY = centerSectionY;
        temperatureChangeId = responseTemperatureChangeId;
        snapshotAvailable = true;
        if (upload) {
            uploadTemperatureTexture();
        }
    }

    private static void writePage(
            int localPageIndex,
            short[] records,
            int recordOffset
    ) {
        int pageX = localPageIndex % PAGE_WIDTH;
        int pageZ = localPageIndex / PAGE_WIDTH % PAGE_WIDTH;
        int pageY = localPageIndex / (PAGE_WIDTH * PAGE_WIDTH);
        for (int brick = 0; brick < 64; brick++) {
            int brickX = brick & 3;
            int brickZ = brick >>> 2 & 3;
            int brickY = brick >>> 4;
            int textureX = pageX * BRICKS_PER_PAGE_AXIS + brickX;
            int textureZ = pageZ * BRICKS_PER_PAGE_AXIS + brickZ;
            int textureY = pageY * BRICKS_PER_PAGE_AXIS + brickY;
            temperatureMirror[
                    (textureZ * TEXTURE_SIZE + textureY) * TEXTURE_SIZE
                            + textureX] = records[recordOffset + brick];
        }
    }

    private static void uploadTemperatureTexture() {
        if (temperatureTexture == 0) {
            getOrCreateTemperatureTexture();
        } else {
            uploadTemperaturePixels(false);
        }
    }

    private static int getOrCreateTemperatureTexture() {
        if (temperatureTexture != 0) {
            return temperatureTexture;
        }
        temperatureTexture = GL11.glGenTextures();
        uploadTemperaturePixels(true);
        return temperatureTexture;
    }

    private static void uploadTemperaturePixels(boolean allocate) {
        temperatureUpload.clear();
        temperatureUpload.put(temperatureMirror);
        temperatureUpload.flip();
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
            GL12.glTexSubImage3D(
                    GL12.GL_TEXTURE_3D,
                    0,
                    0, 0, 0,
                    TEXTURE_SIZE, TEXTURE_SIZE, TEXTURE_SIZE,
                    GL30.GL_RED_INTEGER,
                    GL11.GL_SHORT,
                    temperatureUpload);
        } finally {
            GL11.glBindTexture(GL12.GL_TEXTURE_3D, previousTexture);
        }
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
                            getOrCreateTemperatureTexture());
                    uniforms.glUniform1I("temperatureTexture", 4);
                    uniforms.glUniform3F(
                            "temperatureOrigin",
                            (textureCenterChunkX - PAGE_RADIUS) * 16.0F,
                            (textureCenterSectionY - PAGE_RADIUS) * 16.0F,
                            (textureCenterChunkZ - PAGE_RADIUS) * 16.0F);

                    uniforms.glUniformMatrix4F(
                            "u_InverseProjectionMatrix",
                            RenderSystem.getProjectionMatrix().invert(new Matrix4f()));
                    uniforms.glUniformMatrix4F(
                            "u_InverseViewMatrix",
                            cameraPose.last().pose().invert(new Matrix4f()));
                    uniforms.glUniform3F(
                            "u_CameraPosition",
                            (float) cameraPos.x,
                            (float) cameraPos.y,
                            (float) cameraPos.z);
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

    public static void reset() {
        invalidateRequests();
        open = false;
        radius = 0.0F;
        snapshotAvailable = false;
        temperatureChangeId = 0L;
        Arrays.fill(knownPresence, 0L);
        Arrays.fill(temperatureMirror, Short.MIN_VALUE);
        cameraPose = null;
        Runnable release = () -> {
            if (temperatureTexture != 0) {
                GL11.glDeleteTextures(temperatureTexture);
                temperatureTexture = 0;
            }
            if (overlayTarget != null) {
                overlayTarget.destroyBuffers();
                overlayTarget = null;
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
