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

package com.teammoeg.frostedheart.content.climate.render.weather;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.FHShaders;
import com.teammoeg.frostedheart.content.climate.gamedata.climate.WhiteCurtainFieldModel.VisualKernel;
import com.teammoeg.frostedheart.content.climate.render.FogModification;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.FogType;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.fml.ModList;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import static org.lwjgl.opengl.GL33C.*;

/** Composites extinction along the eye-to-opaque-surface ray directly into the world colour attachment. */
final class CurtainExtinction {
    static final CurtainExtinction INSTANCE = new CurtainExtinction();
    private static final ResourceLocation SNOW = FHMain.rl("textures/environment/white_curtain.png");
    private static final boolean HAS_OCULUS = ModList.get().isLoaded("oculus");
    private static final float BOTTOM = -64.0F, TOP = 96.0F;
    private final CurtainVolume volume = new CurtainVolume();
    private final Matrix4f clip = new Matrix4f(), inverse = new Matrix4f();
    private final Vector4f corner = new Vector4f();
    private float left, right, bottom, top;
    private int framebuffer;

    /** Chooses one fog owner before vanilla computes terrain fog for this frame. */
    static boolean available() {
        Minecraft mc = Minecraft.getInstance();
        return FHShaders.getCurtainExtinction() != null && mc.getMainRenderTarget().getDepthTextureId() > 0
                && glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING) == mc.getMainRenderTarget().frameBufferId
                && mc.gameRenderer.getMainCamera().getFluidInCamera() == FogType.NONE
                && !(HAS_OCULUS && net.irisshaders.iris.api.v0.IrisApi.getInstance().isShaderPackInUse());
    }

    /** Returns whether opaque scenery was composited in its owning framebuffer. */
    boolean render(RenderLevelStageEvent event, ClientWeatherFrame frame) {
        Minecraft mc = Minecraft.getInstance();
        RenderTarget target = mc.getMainRenderTarget();
        int previousFbo = glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING);
        if (previousFbo != target.frameBufferId) return false;
        ShaderInstance shader = FHShaders.getCurtainExtinction();
        clip.set(event.getProjectionMatrix()).mul(event.getPoseStack().last().pose());
        inverse.set(clip).invert();
        int previousProgram = glGetInteger(GL_CURRENT_PROGRAM);
        boolean depth = glIsEnabled(GL_DEPTH_TEST), blend = glIsEnabled(GL_BLEND);
        int srcRgb = glGetInteger(GL_BLEND_SRC_RGB), dstRgb = glGetInteger(GL_BLEND_DST_RGB);
        int srcAlpha = glGetInteger(GL_BLEND_SRC_ALPHA), dstAlpha = glGetInteger(GL_BLEND_DST_ALPHA);
        int equationRgb = glGetInteger(GL_BLEND_EQUATION_RGB), equationAlpha = glGetInteger(GL_BLEND_EQUATION_ALPHA);
        int activeUnit = glGetInteger(GL_ACTIVE_TEXTURE);
        RenderSystem.activeTexture(GL_TEXTURE0);
        int savedDepth = glGetInteger(GL_TEXTURE_BINDING_2D);
        RenderSystem.activeTexture(GL_TEXTURE1);
        int savedMaterial = glGetInteger(GL_TEXTURE_BINDING_2D);
        try {
            if (framebuffer == 0) framebuffer = glGenFramebuffers();
            glBindFramebuffer(GL_DRAW_FRAMEBUFFER, framebuffer);
            // Reattach the live colour name across target resizes/recreation; depth stays on the original FBO.
            glFramebufferTexture2D(GL_DRAW_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, target.getColorTextureId(), 0);
            RenderSystem.disableDepthTest();
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ZERO, GL_ONE);
            glBlendEquationSeparate(GL_FUNC_ADD, GL_FUNC_ADD);
            shader.setSampler("Sampler0", target.getDepthTextureId());
            shader.setSampler("Sampler1", mc.getTextureManager().getTexture(SNOW).getId());
            shader.safeGetUniform("InverseClip").set(inverse);
            int color = FogModification.snowColor(mc.level, frame.partialTick());
            shader.safeGetUniform("StormColor").set(((color >> 16) & 255) / 255F, ((color >> 8) & 255) / 255F,
                    (color & 255) / 255F, frame.profile().fieldProfile().minimumVisibilityBlocks());
            shader.safeGetUniform("ShelterDistance").set((1.0F - ClientWeatherState.INSTANCE.cameraExposure()) * 16.0F);
            double seconds = ClientWeatherState.INSTANCE.frameClimateSeconds(frame.partialTick());
            int count = Math.min(frame.wallCount(), frame.profile().wallSlices());
            for (int i = count - 1; i >= 0; i--) {
                VisualKernel k = frame.wallKernel(i);
                if (!volume.set(k, seconds)) continue;
                float x0 = (float)(volume.minX - frame.cameraX()), x1 = (float)(volume.maxX - frame.cameraX());
                float z0 = (float)(volume.minZ - frame.cameraZ()), z1 = (float)(volume.maxZ - frame.cameraZ());
                if (!projectBounds(x0, x1, z0, z1)) continue;
                shader.safeGetUniform("BoxMin").set(x0, BOTTOM, z0);
                shader.safeGetUniform("BoxMax").set(x1, TOP, z1);
                double cross = frame.cameraX() * k.moveZ + frame.cameraZ() * k.moveX;
                shader.safeGetUniform("SurfaceUV").set(k.moveZ, k.moveX, (float)((cross / 16 + seconds * 0.006) % 1),
                        (float)((frame.cameraY() / 24 + seconds * 0.013) % 1));
                shader.apply();
                BufferBuilder buffer = Tesselator.getInstance().getBuilder();
                buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
                buffer.vertex(left, bottom, 0).endVertex();
                buffer.vertex(right, bottom, 0).endVertex();
                buffer.vertex(right, top, 0).endVertex();
                buffer.vertex(left, top, 0).endVertex();
                BufferUploader.draw(buffer.end());
            }
        } finally {
            shader.clear();
            glUseProgram(previousProgram);
            RenderSystem.activeTexture(GL_TEXTURE0);
            RenderSystem.bindTexture(savedDepth);
            RenderSystem.activeTexture(GL_TEXTURE1);
            RenderSystem.bindTexture(savedMaterial);
            RenderSystem.activeTexture(activeUnit);
            RenderSystem.blendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha);
            glBlendEquationSeparate(equationRgb, equationAlpha);
            if (!blend) RenderSystem.disableBlend();
            if (depth) RenderSystem.enableDepthTest();
            glBindFramebuffer(GL_DRAW_FRAMEBUFFER, previousFbo);
        }
        return true;
    }

    /** Projects the prism to a conservative screen rectangle, including the eye-inside case. */
    private boolean projectBounds(float x0, float x1, float z0, float z1) {
        left = bottom = 1; right = top = -1;
        int behind = 0;
        for (int i = 0; i < 8; i++) {
            clip.transform(corner.set((i & 1) == 0 ? x0 : x1, (i & 2) == 0 ? BOTTOM : TOP,
                    (i & 4) == 0 ? z0 : z1, 1.0F));
            if (corner.w <= 0.001F) {
                behind++;
                continue;
            }
            float x = Mth.clamp(corner.x / corner.w, -1.0F, 1.0F);
            float y = Mth.clamp(corner.y / corner.w, -1.0F, 1.0F);
            left = Math.min(left, x);
            right = Math.max(right, x);
            bottom = Math.min(bottom, y);
            top = Math.max(top, y);
        }
        if (behind == 8) return false;
        if (behind > 0) {
            left = bottom = -1;
            right = top = 1;
        }
        return left < right && bottom < top;
    }

    /** Drops the borrowed attachment on world unload, leaving the game's colour texture owned by Minecraft. */
    void reset() {
        if (framebuffer != 0) glDeleteFramebuffers(framebuffer);
        framebuffer = 0;
    }
}
