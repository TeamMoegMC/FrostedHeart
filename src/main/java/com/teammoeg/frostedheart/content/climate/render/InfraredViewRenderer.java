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


import com.lowdragmc.lowdraglib.client.shader.management.Shader;
import com.lowdragmc.lowdraglib.client.shader.management.ShaderProgram;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.bootstrap.client.FHShaders;
import com.teammoeg.frostedheart.content.climate.network.FHRequestInfraredViewDataSyncPacket;
import com.teammoeg.frostedheart.content.climate.network.InfraredBrickCodec;
import com.teammoeg.frostedheart.content.climate.render.infrared.InfraredSurfaceTarget;


import io.netty.buffer.Unpooled;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.core.SectionPos;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.MinecraftThermalInput.InfraredSnapshot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryStack;

import javax.annotation.Nullable;
import java.nio.ShortBuffer;
import java.nio.IntBuffer;
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
    private static InfraredSurfaceTarget surfaceTarget;
    @Nullable private static ShaderProgram infraredProgram;
    @Nullable private static Shader infraredShader;
    private static boolean surfacePrepared, terrainVisited, terrainDepthCaptured;
    private static int frameTexture, frameOriginX, frameOriginY, frameOriginZ;
    private static boolean open;
    private static boolean deltaBaselineValid;
    private static boolean receivingResponse;
    private static boolean requestCenterValid;
    private static int requestId;
    private static long lastRequestTick = Long.MIN_VALUE;
    private static int infraredEpoch;
    private static long storedEpoch;
    private static long generation;
    private static boolean materialReadable;
    private static long[] knownFieldPages = new long[0];
    private static final Matrix4f inverseViewProjection = new Matrix4f();
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
        if (radius > 0 && GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING)
                != Minecraft.getInstance().getMainRenderTarget().frameBufferId) return;
        cameraPose = pose;
        surfacePrepared = terrainVisited = false;
        terrainDepthCaptured = false;
        frameTexture = 0;
    }

    /** Called by our terrain renderer after the original pass has selected its target. */
    public static boolean visitTerrainPass() {
        if (radius <= 0 || cameraPose == null || GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING)
                != Minecraft.getInstance().getMainRenderTarget().frameBufferId) return false;
        terrainVisited = true;
        return true;
    }

    public static boolean prepareSurfaceCapture() {
        if (surfacePrepared) return true;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return false;
        if (temperatureTexture == 0 && !receivingResponse) initializeEmptyTemperatureTexture(minecraft);
        RenderTarget main = minecraft.getMainRenderTarget();
        if (surfaceTarget == null) surfaceTarget = new InfraredSurfaceTarget();
        if (!surfaceTarget.ensure(main.width, main.height, main.getColorTextureId(), main.getDepthTextureId())) return false;
        surfaceTarget.clear();
        frameTexture = temperatureTexture;
        frameOriginX = (textureCenterChunkX - PAGE_RADIUS) * 16;
        frameOriginY = (textureCenterSectionY - PAGE_RADIUS) * 16;
        frameOriginZ = (textureCenterChunkZ - PAGE_RADIUS) * 16;
        surfacePrepared = true;
        return true;
    }

    public static int frameTemperatureTexture() { return frameTexture; }
    public static int frameOriginX() { return frameOriginX; }
    public static int frameOriginY() { return frameOriginY; }
    public static int frameOriginZ() { return frameOriginZ; }
    public static int surfaceCaptureFramebuffer() { return surfaceTarget.captureFramebuffer(); }

    public static void captureTerrainDepth() {
        surfaceTarget.captureTerrainDepth();
        terrainDepthCaptured = true;
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
        // All parts are sent together over TCP; let an accepted response finish.
        boolean retryFull = awaitingFull && !receivingResponse
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
        receivingResponse = false;
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
                        generation, SectionPos.asLong(textureCenterChunkX, textureCenterSectionY, textureCenterChunkZ),
                        infraredEpoch, knownPresence.clone(), materialReadable, knownFieldPages, storedEpoch));
    }

    /** Only LAST commits the complete display baseline, including field-only responses. */
    public static void updateData(int responseRequestId, InfraredSnapshot data, boolean firstPart, boolean lastPart) {
        RenderSystem.assertOnRenderThread();
        if (!open || responseRequestId != requestId) return;
        boolean sameCenter = data.centerChunkX() == textureCenterChunkX
                && data.centerChunkZ() == textureCenterChunkZ && data.centerSectionY() == textureCenterSectionY;
        if (firstPart) {
            if (!data.full() && (!deltaBaselineValid || !sameCenter || data.generation() != generation)) {
                deltaBaselineValid = false;
                requestCenterValid = false;
                return;
            }
            receivingResponse = true;
            deltaBaselineValid = false;
            requestedChunkX = data.centerChunkX(); requestedChunkZ = data.centerChunkZ();
            requestedSectionY = data.centerSectionY(); requestCenterValid = true;
            ensureTemperatureMirror();
            Arrays.fill(dirtyUploadPages, 0L);
            if (data.full()) clearMirror();
        } else if (!receivingResponse) return;
        for (byte[] records : data.brickRecords()) {
            FriendlyByteBuf input = new FriendlyByteBuf(Unpooled.wrappedBuffer(records));
            try {
                int brick;
                while ((brick = brickDecoder.readRecord(input, decodedBrick)) >= 0)
                    writeBrick(brick, decodedBrick);
            } finally { input.release(); }
        }
        if (!lastPart) return;
        receivingResponse = false;
        if (data.presence().length != 0)
            System.arraycopy(data.presence(), 0, knownPresence, 0, knownPresence.length);
        textureCenterChunkX = data.centerChunkX(); textureCenterChunkZ = data.centerChunkZ();
        textureCenterSectionY = data.centerSectionY();
        generation = data.generation(); infraredEpoch = data.infraredEpoch();
        storedEpoch = data.storedEpoch();
        if (data.full()) uploadFullTemperatureTexture(); else uploadDirtyPages();
        materialReadable = data.readable();
        deltaBaselineValid = true;
        Arrays.fill(dirtyUploadPages, 0L);
        knownFieldPages = data.fieldPages();
    }

    public static void invalidateDisplay() {
        invalidateRequests();
        deltaBaselineValid = false;
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
        int dirtyPages = 0;
        for (long word : dirtyUploadPages) {
            dirtyPages += Long.bitCount(word);
        }
        if (dirtyPages == 0) {
            return;
        }
        if (temperatureTexture == 0 || dirtyPages == PAGE_WIDTH * PAGE_WIDTH * PAGE_WIDTH) {
            uploadFullTemperatureTexture();
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
        GlStateManager._pixelStore(GL12.GL_UNPACK_IMAGE_HEIGHT, 0);
        GlStateManager._pixelStore(GL12.GL_UNPACK_SKIP_IMAGES, 0);
        GlStateManager._pixelStore(GL11.GL_UNPACK_ALIGNMENT, 4);
    }

    public static void renderInfraredView() {
        Minecraft minecraft = Minecraft.getInstance();
        if (cameraPose == null) return;
        if (radius <= 0) {
            if (surfaceTarget != null) { surfaceTarget.close(); surfaceTarget = null; }
            cameraPose = null;
            return;
        }
        if (minecraft.cameraEntity == null || minecraft.player == null || !terrainVisited) return;
        RenderTarget main = minecraft.getMainRenderTarget();
        int previousFramebuffer = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        if (previousFramebuffer != main.frameBufferId) return;
        // Even an empty terrain pass prepares an INVALID image, so no previous-frame silhouette survives.
        if (!prepareSurfaceCapture()) return;
        ShaderProgram program = getInfraredProgram();
        inverseViewProjection.set(RenderSystem.getProjectionMatrix()).mul(cameraPose.last().pose()).invert();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            int previousProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
            IntBuffer viewport = stack.mallocInt(4);
            GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
            boolean scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
            boolean depthWrites = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
            boolean depthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
            boolean blending = GL11.glIsEnabled(GL11.GL_BLEND);
            int srcRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB), dstRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
            int srcAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA), dstAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
            int equationRgb = GL11.glGetInteger(GL20.GL_BLEND_EQUATION_RGB), equationAlpha = GL11.glGetInteger(GL20.GL_BLEND_EQUATION_ALPHA);
            int activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
            RenderSystem.activeTexture(GL13.GL_TEXTURE0);
            int oldDepthBinding = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            RenderSystem.activeTexture(GL13.GL_TEXTURE1);
            int oldSurfaceBinding = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            RenderSystem.activeTexture(GL13.GL_TEXTURE2);
            int oldTerrainDepthBinding = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            RenderSystem.activeTexture(GL13.GL_TEXTURE3);
            int oldEnvironmentBinding = GL11.glGetInteger(GL12.GL_TEXTURE_BINDING_3D);
            try {
                // Color only: sampling the main depth cannot create a framebuffer feedback loop.
                GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, surfaceTarget.blendFramebuffer());
                RenderSystem.viewport(0, 0, main.width, main.height);
                RenderSystem.disableScissor();
                RenderSystem.depthMask(false);
                RenderSystem.disableDepthTest();
                RenderSystem.enableBlend();
                RenderSystem.blendFuncSeparate(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ZERO, GL11.GL_ONE);
                GL20.glBlendEquationSeparate(GL14.GL_FUNC_ADD, GL14.GL_FUNC_ADD);
                RenderSystem.activeTexture(GL13.GL_TEXTURE0);
                RenderSystem.bindTexture(main.getDepthTextureId());
                RenderSystem.activeTexture(GL13.GL_TEXTURE1);
                RenderSystem.bindTexture(surfaceTarget.temperatureTexture());
                RenderSystem.activeTexture(GL13.GL_TEXTURE2);
                RenderSystem.bindTexture(surfaceTarget.terrainDepthTexture());
                RenderSystem.activeTexture(GL13.GL_TEXTURE3);
                GL11.glBindTexture(GL12.GL_TEXTURE_3D, frameTexture);
                program.use(uniforms -> {
                    uniforms.glUniform1I("depthTexture", 0);
                    uniforms.glUniform1I("surfaceTemperature", 1);
                    uniforms.glUniform1I("terrainDepthTexture", 2);
                    uniforms.glUniform1I("hasTerrainDepth", terrainDepthCaptured ? 1 : 0);
                    uniforms.glUniform1I("environmentTemperature", 3);
                    uniforms.glUniform1I("hasEnvironmentTemperature", frameTexture != 0 ? 1 : 0);
                    var camera = minecraft.gameRenderer.getMainCamera().getPosition();
                    uniforms.glUniform3F("cameraToTemperatureOrigin", (float)(camera.x - frameOriginX),
                            (float)(camera.y - frameOriginY), (float)(camera.z - frameOriginZ));
                    uniforms.glUniform1F("radius", Mth.clamp(radius + minecraft.getFrameTime() * RADIUS_DUR * (open ? 1 : -1), 0, SCAN_RADIUS_BLOCKS));
                    uniforms.glUniformMatrix4F("u_InverseViewProjectionMatrix", inverseViewProjection);
                });
                BufferBuilder buffer = Tesselator.getInstance().getBuilder();
                buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
                buffer.vertex(-1, 1, 0).endVertex();
                buffer.vertex(-1, -1, 0).endVertex();
                buffer.vertex(1, -1, 0).endVertex();
                buffer.vertex(1, 1, 0).endVertex();
                BufferUploader.draw(buffer.end());
            } finally {
                // ShaderProgram binds raw GL. Restore it raw too, leaving vanilla/Oculus caches unchanged.
                GL20.glUseProgram(previousProgram);
                RenderSystem.activeTexture(GL13.GL_TEXTURE0);
                RenderSystem.bindTexture(oldDepthBinding);
                RenderSystem.activeTexture(GL13.GL_TEXTURE1);
                RenderSystem.bindTexture(oldSurfaceBinding);
                RenderSystem.activeTexture(GL13.GL_TEXTURE2);
                RenderSystem.bindTexture(oldTerrainDepthBinding);
                RenderSystem.activeTexture(GL13.GL_TEXTURE3);
                GL11.glBindTexture(GL12.GL_TEXTURE_3D, oldEnvironmentBinding);
                RenderSystem.activeTexture(activeTexture);
                RenderSystem.blendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha);
                GL20.glBlendEquationSeparate(equationRgb, equationAlpha);
                if (!blending) RenderSystem.disableBlend();
                if (depthTest) RenderSystem.enableDepthTest(); else RenderSystem.disableDepthTest();
                RenderSystem.depthMask(depthWrites);
                RenderSystem.viewport(viewport.get(0), viewport.get(1), viewport.get(2), viewport.get(3));
                if (scissor) GlStateManager._enableScissorTest();
                GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, previousFramebuffer);
                cameraPose = null;
                surfacePrepared = terrainVisited = false;
                terrainDepthCaptured = false;
            }
        }
    }

    private static ShaderProgram getInfraredProgram() {
        Shader shader = FHShaders.getInfraredView();
        if (infraredProgram == null || infraredShader != shader) {
            if (infraredProgram != null) infraredProgram.delete();
            infraredProgram = new ShaderProgram().attach(FHShaders.IMAGE_V).attach(shader);
            infraredShader = shader;
        }
        return infraredProgram;
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
        storedEpoch = 0;
        generation = 0;
        materialReadable = false;
        knownFieldPages = new long[0];
        surfacePrepared = terrainVisited = false;
        terrainDepthCaptured = false;
        frameTexture = 0;
        Arrays.fill(knownPresence, 0L);
        Arrays.fill(dirtyUploadPages, 0L);
        cameraPose = null;
        int textureToDelete = temperatureTexture;
        temperatureTexture = 0;
        InfraredSurfaceTarget surfaceToDelete = surfaceTarget;
        surfaceTarget = null;
        ShaderProgram programToDelete = infraredProgram;
        infraredProgram = null;
        infraredShader = null;
        Runnable release = () -> {
            if (textureToDelete != 0) {
                GL11.glDeleteTextures(textureToDelete);
            }
            if (surfaceToDelete != null) surfaceToDelete.close();
            if (programToDelete != null) programToDelete.delete();
        };
        if (RenderSystem.isOnRenderThread()) {
            release.run();
        } else {
            RenderSystem.recordRenderCall(release::run);
        }
    }

    private static void invalidateRequests() {
        receivingResponse = false;
        requestId = nextRequestId(requestId);
        requestCenterValid = false;
        lastRequestTick = Long.MIN_VALUE;
    }

    private static int nextRequestId(int current) {
        return current == Integer.MAX_VALUE ? 0 : current + 1;
    }

}
