package com.teammoeg.frostedheart.client.renderer;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.client.shader.management.ShaderManager;
import com.lowdragmc.lowdraglib.client.shader.management.ShaderUBO;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.teammoeg.frostedheart.FHConfig;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.client.shaders.FHShaders;
import com.teammoeg.frostedheart.content.climate.heatdevice.chunkheatdata.FHRequestInfraredViewDataSyncPacket;
import com.teammoeg.frostedheart.mixin.oculus.IrisRenderingPipelineAccess;
import lombok.Getter;
import lombok.Setter;
import net.irisshaders.iris.Iris;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;

import javax.annotation.Nullable;


@OnlyIn(Dist.CLIENT)
public class InfraredViewRenderer {
    private static final int EXPANDING_TICKS = 20; // 4s
    private static final int MAXIMUM_CHUNK_RADIUS = 4;
    private static final int MAXIMUM_ADJUSTS_SUPPORT = 512; // should be equal to the infrared_view.fsh
    /**
     * encoding structure as infrared_view.fsh:
     * <pre>
     * struct HeatArea {
     *     vec4 position; // [3 float for position, 1 float for mode]
     *     vec4 data; // [1 float for value, 1 float for radius, 2 float for pillar (upper, lower)]
     * };
     * </pre>
     */
    private static final int HEAT_AREA_STRUCTURE_SIZE = 8;
    private static final float RADIUS_DUR = MAXIMUM_CHUNK_RADIUS * 16f / EXPANDING_TICKS;
    // runtime data
    @Getter
    @Setter
    @Nullable
    private static PoseStack cameraPose;
    @Getter
    private static boolean isOpen = false;
    @Nullable
    private static ChunkPos lastChunkPos;
    private static int adjustNum = 0;
    @Nullable
    private static RenderTarget overlayTarget;
    @Nullable
    private static ShaderUBO adjustUBO;
    @Getter
    @Setter
    private static float radius = 0.0f;

    /**
     * Toggle the infrared view
     */
    public static void toggleInfraredView() {
        isOpen = !isOpen;
    }

    /**
     * Tick the infrared view for radius update
     */
    public static void clientTick() {
        if (isOpen && radius < MAXIMUM_CHUNK_RADIUS * 16) {
            radius = Mth.clamp(radius + RADIUS_DUR, 0, MAXIMUM_CHUNK_RADIUS * 16);
        } else if (!isOpen && radius > 0) {
            radius = Mth.clamp(radius - RADIUS_DUR, 0, MAXIMUM_CHUNK_RADIUS * 16);
        }
    }

    /**
     * Render the infrared view
     */
    public static void renderInfraredView() {
        var mc = Minecraft.getInstance();
        if (mc.cameraEntity == null || mc.player == null || radius <= 0 || cameraPose == null) return;
        var mainTarget = mc.getMainRenderTarget();
        var overlayTarget = getOrCreateOverlayTarget(mainTarget.width, mainTarget.height);
        var partialTicks = mc.getFrameTime();
        var cameraPos = mc.cameraEntity.getEyePosition(partialTicks);
        var onChunkPos = new ChunkPos(mc.cameraEntity.getOnPos());

        if (!onChunkPos.equals(lastChunkPos)) {
            lastChunkPos = onChunkPos;
            FHNetwork.sendToServer(new FHRequestInfraredViewDataSyncPacket(onChunkPos, MAXIMUM_CHUNK_RADIUS));
        }

        RenderSystem.depthMask(false);
        RenderSystem.disableDepthTest();
        overlayTarget.clear(Minecraft.ON_OSX);
        ShaderManager.getInstance().renderFullImageInFramebuffer(overlayTarget, FHShaders.getInfraredView(), uniformCache -> {
            uniformCache.glUniform1F("radius",
                    Mth.clamp(radius + partialTicks * RADIUS_DUR * (isOpen ? 1 : -1), 0, MAXIMUM_CHUNK_RADIUS * 16));
            uniformCache.glUniform1I("adjust_num", adjustNum);

            RenderSystem.activeTexture(GL13.GL_TEXTURE0);
            RenderSystem.bindTexture(mainTarget.getColorTextureId());
            uniformCache.glUniform1I("mainTexture", 0);

            RenderSystem.activeTexture(GL13.GL_TEXTURE1);
            RenderSystem.bindTexture(mainTarget.getDepthTextureId());
            uniformCache.glUniform1I("depthTexture", 1);

            RenderSystem.activeTexture(GL13.GL_TEXTURE2);
            if (LDLib.isOculusLoaded() && Iris.getPipelineManager().getPipeline().orElse(null) instanceof IrisRenderingPipelineAccess access) {
                RenderSystem.bindTexture(access.getRenderTargets().getDepthTextureNoHand().getTextureId());
            } else {
                RenderSystem.bindTexture(mainTarget.getDepthTextureId());
            }
            uniformCache.glUniform1I("noHandDepthTexture", 2);

            RenderSystem.activeTexture(GL13.GL_TEXTURE3);
            if (LDLib.isOculusLoaded() && Iris.getPipelineManager().getPipeline().orElse(null) instanceof IrisRenderingPipelineAccess access) {
                RenderSystem.bindTexture(access.getRenderTargets().getDepthTextureNoTranslucents().getTextureId());
            } else {
                RenderSystem.bindTexture(mainTarget.getDepthTextureId());
            }
            uniformCache.glUniform1I("noTranslucentDepthTexture", 3);

            uniformCache.glUniformMatrix4F("u_InverseProjectionMatrix", RenderSystem.getProjectionMatrix().invert(new Matrix4f()));
            uniformCache.glUniformMatrix4F("u_InverseViewMatrix", cameraPose.last().pose().invert(new Matrix4f()));
            uniformCache.glUniform3F("u_CameraPosition", (float) cameraPos.x, (float) cameraPos.y, (float) cameraPos.z);
        }, program -> {
            program.linkProgram();
            getOrCreateAdjustUBO().bindToShader(program.programId, "Adjusts");
        });

        ShaderManager.getInstance().renderFullImageInFramebuffer(mainTarget, FHShaders.IMAGE_F, uniformCache -> {
            RenderSystem.activeTexture(GL13.GL_TEXTURE0);
            RenderSystem.bindTexture(overlayTarget.getColorTextureId());
            uniformCache.glUniform1I("DiffuseSampler", 0);
        }, null);

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);

        // release the pose of current frame
        setCameraPose(null);
    }

    public static void updateData(ChunkPos chunkPos, float[] data) {
        // upload heat area data to ubo
        lastChunkPos = chunkPos;
        adjustNum = data.length / HEAT_AREA_STRUCTURE_SIZE;
        getOrCreateAdjustUBO().bufferSubData(0, data);
    }

    public static void updateData(ChunkPos chunkPos, int[] data) {
        // upload heat area data to ubo
        // Cause java is using the same bytes for float and int, we can use the same method to update data
        lastChunkPos = chunkPos;
        adjustNum = data.length / HEAT_AREA_STRUCTURE_SIZE;
        getOrCreateAdjustUBO().bufferSubData(0, data);
    }

    public static void notifyChunkDataUpdate() {
        lastChunkPos = null;
    }

    /**
     * NOTE!!!
     * @author KilaBash
     * <br>
     * Partial shaders and mods may occupy the position as well
     * <br>
     * We add offset for some known mods here. However, it is not guaranteed to be compatible with all mods shaders.
     * In this case, player have to modify the config to specify the offset.
     * <br>
     * - offset 1 to be compatible with most mods and shaders, from my experience
     * <br>
     * - offset 6 to be compatible with modern ui
     * <br>
     * - offset 7 to be compatible with both shimmer and modern ui
     */
    private static int getUniformBufferObjectOffset() {
        return FHConfig.CLIENT.infraredViewUBOOffset.get();
    }

    private static RenderTarget getOrCreateOverlayTarget(int width, int height) {
        if (overlayTarget == null) {
            overlayTarget = new TextureTarget(width, height, false, Minecraft.ON_OSX);
            overlayTarget.setClearColor(0, 0, 0, 0);
        } else if (overlayTarget.width != width || overlayTarget.height != height) {
            overlayTarget.resize(width, height, Minecraft.ON_OSX);
        }
        return overlayTarget;
    }

    private static ShaderUBO getOrCreateAdjustUBO() {
        if (adjustUBO == null) {
            adjustUBO = new ShaderUBO();
            var size = MAXIMUM_ADJUSTS_SUPPORT * HEAT_AREA_STRUCTURE_SIZE * Float.BYTES; // 4 float per adjuster
            adjustUBO.createBufferData(size, GL30.GL_STREAM_DRAW); // stream -- modified each frame
            // create ubo
            int uboOffset = getUniformBufferObjectOffset();
            adjustUBO.blockBinding(uboOffset);
        }
        return adjustUBO;
    }
}
