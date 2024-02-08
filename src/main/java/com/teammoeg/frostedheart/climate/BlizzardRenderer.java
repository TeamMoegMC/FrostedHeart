/*
 * Copyright (c) 2022-2024 TeamMoeg
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

package com.teammoeg.frostedheart.climate;

import java.util.Random;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.gen.Heightmap;

/**
 * Render cool Blizzard!
 * <p>
 * To integrate it to the game,
 * let `render` be called in `com.teammoeg.frostedheart.mixin.client.WorldRendererMixin.inject$renderWeather`
 * with appropriate context after confirming that a blizzard is present in the world.
 * <p>
 * We want to thank BetterWeather's author CorgiTaco, who wrote the code we adapted from.
 * Original Code: https://github.com/CorgiTaco/Better-Weather
 * This file is licensed under the same license LGPL 3.0
 * Original License: https://github.com/CorgiTaco/Better-Weather/blob/Forge-1.16.X/LICENSE.txt
 */
public class BlizzardRenderer {
    private final static float[] rainSizeXMemento = new float[1024];
    private final static float[] rainSizeZMemento = new float[1024];

    public static void render(Minecraft mc,
                              ClientWorld world,
                              LightTexture lightTexture,
                              int ticks,
                              float partialTicks,
                              double cameraX,
                              double cameraY,
                              double cameraZ) {
        float rainStrength = world.getThunderStrength(partialTicks);
        lightTexture.enableLightmap();

        int cameraBlockPosX = MathHelper.floor(cameraX);
        int cameraBlockPosY = MathHelper.floor(cameraY);
        int cameraBlockPosZ = MathHelper.floor(cameraZ);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuffer();
        RenderSystem.enableAlphaTest();
        RenderSystem.disableCull();
        RenderSystem.normal3f(0.0F, 1.0F, 0.0F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.defaultAlphaFunc();
        RenderSystem.enableDepthTest();

        // The blizzard will be rendered in the block range
        // cameraX - renderRadius <= x <= cameraX + renderRadius
        // cameraZ - renderRadius <= z <= cameraZ + renderRadius
        // For y, it is the same rule, while in addition y should > *altitude of first solid block*
        int renderRadius = Minecraft.isFancyGraphicsEnabled() ? 5 : 8;
        RenderSystem.depthMask(Minecraft.isFabulousGraphicsEnabled());

        int i1 = -1;
        float ticksAndPartialTicks = ticks + partialTicks;
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        BlockPos.Mutable blockPos = new BlockPos.Mutable();
        Random random = new Random(
                cameraBlockPosX * cameraBlockPosX * 3121
                        + cameraBlockPosZ * 45238971 ^ cameraBlockPosZ * cameraBlockPosZ * 418711
                        + (int) (ticksAndPartialTicks * 13761)
        );
        for (int currentlyRenderingZ = cameraBlockPosZ - renderRadius;
             currentlyRenderingZ <= cameraBlockPosZ + renderRadius;
             ++currentlyRenderingZ) {
            for (int currentlyRenderingX = cameraBlockPosX - renderRadius;
                 currentlyRenderingX <= cameraBlockPosX + renderRadius;
                 ++currentlyRenderingX) {
                int rainSizeIdx = (currentlyRenderingZ - cameraBlockPosZ + 16) * 32 + currentlyRenderingX - cameraBlockPosX + 16;

                // Size of snowflake
                double rainSizeX = rainSizeXMemento[rainSizeIdx] * 0.5D;
                double rainSizeZ = rainSizeZMemento[rainSizeIdx] * 0.5D;
                blockPos.setPos(currentlyRenderingX, 0, currentlyRenderingZ);

                int altitudeOfHighestSolidBlock = mc.world.getHeight(Heightmap.Type.MOTION_BLOCKING, blockPos.getX(), blockPos.getZ());
                int renderingYLowerBound = Math.max(cameraBlockPosY - renderRadius, altitudeOfHighestSolidBlock);
                int renderingYUpperBound = Math.max(cameraBlockPosY + renderRadius, altitudeOfHighestSolidBlock);

                int posY2 = Math.max(altitudeOfHighestSolidBlock, cameraBlockPosY);

                // If the ``non-blocked'' block is out of render radius,
                // nothing will be rendered.
                if (renderingYLowerBound != renderingYUpperBound) {

                    blockPos.setPos(currentlyRenderingX, renderingYLowerBound, currentlyRenderingZ);

                    if (i1 != 1) {
                        if (i1 >= 0) {
                            tessellator.draw();
                        }

                        i1 = 1;
                        mc.getTextureManager()
                                .bindTexture(new ResourceLocation("minecraft:textures/environment/snow.png"));
                        bufferBuilder.begin(7, DefaultVertexFormats.PARTICLE_POSITION_TEX_COLOR_LMAP);
                    }

                    float f7 = (float) (random.nextDouble()
                            + ticksAndPartialTicks * (float) random.nextGaussian() * 0.03D);
                    float fallSpeed = (float) (random.nextDouble()
                            + ticksAndPartialTicks * (float) random.nextGaussian() * 0.3D);
                    double d3 = currentlyRenderingX + 0.5F - cameraX;
                    double d5 = currentlyRenderingZ + 0.5F - cameraZ;
                    float f9 = (float) ((d3 * d3 + d5 * d5) / (renderRadius * renderRadius));
                    float ticksAndPartialTicks0 = ((1.0F - f9) * 0.3F + 0.5F) * rainStrength;
                    blockPos.setPos(currentlyRenderingX, posY2, currentlyRenderingZ);
                    int k3 = WorldRenderer.getCombinedLight(world, blockPos);
                    int l3 = k3 >> 16 & '\uffff';
                    int i4 = (k3 & '\uffff') * 3;
                    int j4 = (l3 * 3 + 240) / 4;
                    int k4 = (i4 * 3 + 240) / 4;

                    bufferBuilder.pos(
                                    currentlyRenderingX - cameraX - rainSizeX + 0.5D + random.nextGaussian() * 2,
                                    renderingYUpperBound - cameraY,
                                    currentlyRenderingZ - cameraZ - rainSizeZ + 0.5D + random.nextGaussian())
                            .tex(0.0F + f7, renderingYLowerBound * 0.25F - Math.abs(fallSpeed))
                            .color(1.0F, 1.0F, 1.0F, ticksAndPartialTicks0)
                            .lightmap(k4, j4)
                            .endVertex();
                    bufferBuilder.pos(
                                    currentlyRenderingX - cameraX + rainSizeX + 0.5D + random.nextGaussian() * 2,
                                    renderingYUpperBound - cameraY,
                                    currentlyRenderingZ - cameraZ + rainSizeZ + 0.5D + random.nextGaussian())
                            .tex(1.0F + f7, renderingYLowerBound * 0.25F - Math.abs(fallSpeed))
                            .color(1.0F, 1.0F, 1.0F, ticksAndPartialTicks0)
                            .lightmap(k4, j4)
                            .endVertex();
                    bufferBuilder.pos(
                                    currentlyRenderingX - cameraX + rainSizeX + 0.5D + random.nextGaussian() * 2,
                                    renderingYLowerBound - cameraY,
                                    currentlyRenderingZ - cameraZ + rainSizeZ + 0.5D + random.nextGaussian())
                            .tex(1.0F + f7, renderingYUpperBound * 0.25F - Math.abs(fallSpeed))
                            .color(1.0F, 1.0F, 1.0F, ticksAndPartialTicks0)
                            .lightmap(k4, j4)
                            .endVertex();
                    bufferBuilder.pos(
                                    currentlyRenderingX - cameraX - rainSizeX + 0.5D + random.nextGaussian() * 2,
                                    renderingYLowerBound - cameraY,
                                    currentlyRenderingZ - cameraZ - rainSizeZ + 0.5D + random.nextGaussian())
                            .tex(0.0F + f7, renderingYUpperBound * 0.25F - Math.abs(fallSpeed))
                            .color(1.0F, 1.0F, 1.0F, ticksAndPartialTicks0)
                            .lightmap(k4, j4)
                            .endVertex();
                }
            }
        }

        if (i1 >= 0) {
            tessellator.draw();
        }

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.defaultAlphaFunc();
        RenderSystem.disableAlphaTest();
        lightTexture.disableLightmap();
    }

    public BlizzardRenderer() {
        for (int i = 0; i < 32; ++i) {
            for (int j = 0; j < 32; ++j) {
                float f = j - 16;
                float f1 = i - 16;
                float f2 = MathHelper.sqrt(f * f + f1 * f1);
                rainSizeXMemento[i << 5 | j] = -f1 / f2;
                rainSizeZMemento[i << 5 | j] = f / f2;
            }
        }
    }
}
