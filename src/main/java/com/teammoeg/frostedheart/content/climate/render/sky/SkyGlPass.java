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
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;

import javax.annotation.Nullable;

/**
 * A full screen draw with a raw GL program. begin saves the program, the active texture unit, blending, depth
 * testing, and the 2D texture bindings of the units the pass uses, then turns blending and depth testing off. end
 * restores them, raw for the program and through RenderSystem for the rest, so the caches of vanilla and Oculus stay
 * right. All passes share one full screen quad. Render thread only.
 */
@OnlyIn(Dist.CLIENT)
final class SkyGlPass {
    @Nullable
    private static VertexBuffer fullScreenQuad;

    private final int[] previousBindings;
    private int previousProgram;
    private int previousActiveTexture;
    private boolean blending;
    private boolean depthTest;

    SkyGlPass(int textureUnits) {
        previousBindings = new int[textureUnits];
    }

    void begin() {
        previousProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        previousActiveTexture = GlStateManager._getActiveTexture();
        blending = GL11.glIsEnabled(GL11.GL_BLEND);
        depthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        for (int unit = 0; unit < previousBindings.length; unit++) {
            RenderSystem.activeTexture(GL13.GL_TEXTURE0 + unit);
            previousBindings[unit] = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        }
        RenderSystem.disableBlend();
        RenderSystem.disableDepthTest();
    }

    void end() {
        GL20.glUseProgram(previousProgram);
        for (int unit = 0; unit < previousBindings.length; unit++) {
            RenderSystem.activeTexture(GL13.GL_TEXTURE0 + unit);
            RenderSystem.bindTexture(previousBindings[unit]);
        }
        RenderSystem.activeTexture(previousActiveTexture);
        if (blending) {
            RenderSystem.enableBlend();
        }
        if (depthTest) {
            RenderSystem.enableDepthTest();
        }
    }

    /**
     * Draws a quad over the whole screen in clip space with the current program, which receives the position only.
     */
    static void drawFullScreenQuad() {
        VertexBuffer quad = fullScreenQuad();
        quad.bind();
        try {
            quad.draw();
        } finally {
            VertexBuffer.unbind();
        }
    }

    private static VertexBuffer fullScreenQuad() {
        if (fullScreenQuad == null) {
            BufferBuilder builder = Tesselator.getInstance().getBuilder();
            builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
            builder.vertex(-1, 1, 0).endVertex();
            builder.vertex(-1, -1, 0).endVertex();
            builder.vertex(1, -1, 0).endVertex();
            builder.vertex(1, 1, 0).endVertex();
            VertexBuffer quad = new VertexBuffer(VertexBuffer.Usage.STATIC);
            quad.bind();
            quad.upload(builder.end());
            VertexBuffer.unbind();
            fullScreenQuad = quad;
        }
        return fullScreenQuad;
    }
}
