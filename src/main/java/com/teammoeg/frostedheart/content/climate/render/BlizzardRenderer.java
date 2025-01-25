/*
 * Copyright (c) 2024 TeamMoeg
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

package com.teammoeg.frostedheart.content.climate.render;

import java.util.Random;

import com.mojang.blaze3d.systems.RenderSystem;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.vertex.BufferBuilder;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;

import net.minecraft.client.renderer.LevelRenderer;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;

import static net.minecraft.client.renderer.LevelRenderer.getLightColor;

/**
 * Render cool Blizzard!
 * <p>
 * To integrate it to the game,
 * let `render` be called in `com.teammoeg.frostedheart.mixin.client.LevelRendererMixin.inject$renderWeather`
 * with appropriate context after confirming that a blizzard is present in the world.
 * <p>
 * We want to thank BetterWeather's author CorgiTaco, who wrote the code we adapted from.
 * Original Code: <a href="https://github.com/CorgiTaco/Better-Weather">...</a>
 * This file is licensed under the same license LGPL 3.0
 * Original License: <a href="https://github.com/CorgiTaco/Better-Weather/blob/Forge-1.16.X/LICENSE.txt">...</a>
 */
public class BlizzardRenderer {
    private final static float[] rainSizeXMemento = new float[1024];  // Stores x-axis offsets for snowflake size variation
    private final static float[] rainSizeZMemento = new float[1024];  // Stores z-axis offsets for snowflake size variation

    // Static fields to adjust flake density and size without modifying the method signature
    public static float flakeDensity = 0.1f;  // Controls density of flakes (default: 50%)
    public static float flakeSize = 0.1f;     // Controls size of each flake (default: 50%)

    public BlizzardRenderer() {
        // Precompute offsets for snowflake particle size variations
        // These offsets are used to give each snowflake a unique size and angle,
        // creating more natural snow patterns

        for (int i = 0; i < 32; ++i) { // Loop over a 32x32 grid for snowflake positions
            for (int j = 0; j < 32; ++j) {
                // Calculate distance from the center of the grid (16,16)
                float f = j - 16;       // Horizontal distance from the center
                float f1 = i - 16;      // Vertical distance from the center
                float f2 = Mth.sqrt(f * f + f1 * f1); // Distance to (16,16) for normalization

                // Store precomputed offsets in arrays for use in the render method
                rainSizeXMemento[i << 5 | j] = -f1 / f2; // x-axis offset, normalized
                rainSizeZMemento[i << 5 | j] = f / f2;   // z-axis offset, normalized
            }
        }
    }

    public static void render(Minecraft mc,
                              ClientLevel world,
                              LightTexture lightTexture,
                              int ticks,
                              float partialTicks,
                              double cameraX,
                              double cameraY,
                              double cameraZ) {
        float rainStrength = world.getThunderLevel(partialTicks); // Determines snow intensity based on thunder level
        lightTexture.turnOnLightLayer(); // Turns on custom lighting for the snowflakes

        // Calculate the integer block position of the camera in the world
        int cameraBlockPosX = Mth.floor(cameraX);
        int cameraBlockPosY = Mth.floor(cameraY);
        int cameraBlockPosZ = Mth.floor(cameraZ);

        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuilder();
        RenderSystem.disableCull(); // Prevents culling so flakes behind the camera are also rendered
        RenderSystem.enableBlend();  // Enables blending for transparency of snowflakes
        RenderSystem.defaultBlendFunc(); // Sets default blend function for smoother visuals
        RenderSystem.enableDepthTest(); // Enables depth to give 3D layering to flakes

        // Sets render radius for the blizzard, based on graphic quality settings
        int renderRadius = Minecraft.useFancyGraphics() ? 5 : 8;
        RenderSystem.depthMask(Minecraft.useShaderTransparency());

        int i1 = -1;  // Tracks the render state to control the draw calls
        float ticksAndPartialTicks = ticks + partialTicks; // Combines ticks and partial ticks for smoother animation
        BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();

        // Creates a random seed based on the camera's position, ensuring a consistent snow pattern
        Random random = new Random(
                (long) cameraBlockPosX * cameraBlockPosX * 3121
                        + cameraBlockPosZ * 45238971L ^ (long) cameraBlockPosZ * cameraBlockPosZ * 418711
                        + (int) (ticksAndPartialTicks * 13761)
        );

        // Iterate over a square area around the camera position defined by renderRadius
        for (int currentlyRenderingZ = cameraBlockPosZ - renderRadius;
             currentlyRenderingZ <= cameraBlockPosZ + renderRadius;
             ++currentlyRenderingZ) {
            for (int currentlyRenderingX = cameraBlockPosX - renderRadius;
                 currentlyRenderingX <= cameraBlockPosX + renderRadius;
                 ++currentlyRenderingX) {

                // Control the density of snowflakes by skipping flakes based on random probability
                if (random.nextFloat() > flakeDensity) continue;

                // Compute the index for accessing precomputed size offsets for the snowflakes
                int rainSizeIdx = (currentlyRenderingZ - cameraBlockPosZ + 16) * 32 + currentlyRenderingX - cameraBlockPosX + 16;
                double rainSizeX = rainSizeXMemento[rainSizeIdx] * 0.5D; // Scales flake size along x-axis
                double rainSizeZ = rainSizeZMemento[rainSizeIdx] * 0.5D; // Scales flake size along z-axis

                // Calculate the height of the highest solid block in this position to start rendering snow above it
                blockPos.set(currentlyRenderingX, 0, currentlyRenderingZ);
                int altitudeOfHighestSolidBlock = mc.level.getHeight(Heightmap.Types.MOTION_BLOCKING, blockPos.getX(), blockPos.getZ());

                // Set y-axis bounds for snowflake rendering, starting above the highest solid block
                int renderingYLowerBound = Math.max(cameraBlockPosY - renderRadius, altitudeOfHighestSolidBlock);
                int renderingYUpperBound = Math.max(cameraBlockPosY + renderRadius, altitudeOfHighestSolidBlock);
                int posY2 = Math.max(altitudeOfHighestSolidBlock, cameraBlockPosY);

                if (renderingYLowerBound != renderingYUpperBound) {
                    // Set block position for rendering
                    blockPos.set(currentlyRenderingX, renderingYLowerBound, currentlyRenderingZ);

                    // Begin a new draw call if not already rendering
                    if (i1 != 1) {
                        i1 = 1;
                        mc.getTextureManager().bindForSetup(new ResourceLocation("minecraft:textures/environment/snow.png"));
                        bufferBuilder.begin(Mode.QUADS, DefaultVertexFormat.PARTICLE); // Begin particle rendering
                    }

                    // Randomize flake speed for variation in fall rate
                    float f7 = (float) (random.nextDouble() + ticksAndPartialTicks * (float) random.nextGaussian() * 0.03D);
                    float fallSpeed = (float) (random.nextDouble() + ticksAndPartialTicks * (float) random.nextGaussian() * 0.3D);

                    // Distance from camera for calculating fading effect
                    double d3 = currentlyRenderingX + 0.5F - cameraX;
                    double d5 = currentlyRenderingZ + 0.5F - cameraZ;
                    float f9 = (float) ((d3 * d3 + d5 * d5) / (renderRadius * renderRadius));

                    // Calculate snowflake opacity based on distance from camera and thunder level
                    float ticksAndPartialTicks0 = ((1.0F - f9) * 0.3F + 0.5F) * rainStrength;

                    // Light and color settings based on current world lighting
                    blockPos.set(currentlyRenderingX, posY2, currentlyRenderingZ);
                    int k3 = getLightColor(world, blockPos);
                    int l3 = k3 >> 16 & '\uffff';
                    int i4 = (k3 & '\uffff') * 3;
                    int j4 = (l3 * 3 + 240) / 4;
                    int k4 = (i4 * 3 + 240) / 4;

                    // Render each corner of the snowflake particle quad with randomized position
                    bufferBuilder.vertex(
                                    currentlyRenderingX - cameraX - (rainSizeX + 0.5D + random.nextGaussian() * 2) * flakeSize,
                                    renderingYUpperBound - cameraY,
                                    currentlyRenderingZ - cameraZ - (rainSizeZ + 0.5D + random.nextGaussian() * flakeSize))
                            .uv(0.0F + f7, renderingYLowerBound * 0.25F - Math.abs(fallSpeed))
                            .color(1.0F, 1.0F, 1.0F, ticksAndPartialTicks0)
                            .uv2(k4, j4)
                            .endVertex();
                    bufferBuilder.vertex(
                                    currentlyRenderingX - cameraX + (rainSizeX + 0.5D + random.nextGaussian() * 2) * flakeSize,
                                    renderingYUpperBound - cameraY,
                                    currentlyRenderingZ - cameraZ + (rainSizeZ + 0.5D + random.nextGaussian()) * flakeSize)
                            .uv(1.0F + f7, renderingYLowerBound * 0.25F - Math.abs(fallSpeed))
                            .color(1.0F, 1.0F, 1.0F, ticksAndPartialTicks0)
                            .uv2(k4, j4)
                            .endVertex();
                    bufferBuilder.vertex(
                                    currentlyRenderingX - cameraX + (rainSizeX + 0.5D + random.nextGaussian() * 2) * flakeSize,
                                    renderingYLowerBound - cameraY,
                                    currentlyRenderingZ - cameraZ + (rainSizeZ + 0.5D + random.nextGaussian()) * flakeSize)
                            .uv(1.0F + f7, renderingYUpperBound * 0.25F - Math.abs(fallSpeed))
                            .color(1.0F, 1.0F, 1.0F, ticksAndPartialTicks0)
                            .uv2(k4, j4)
                            .endVertex();
                    bufferBuilder.vertex(
                                    currentlyRenderingX - cameraX - (rainSizeX + 0.5D + random.nextGaussian() * 2) * flakeSize,
                                    renderingYLowerBound - cameraY,
                                    currentlyRenderingZ - cameraZ - (rainSizeZ + 0.5D + random.nextGaussian()) * flakeSize)
                            .uv(0.0F + f7, renderingYUpperBound * 0.25F - Math.abs(fallSpeed))
                            .color(1.0F, 1.0F, 1.0F, ticksAndPartialTicks0)
                            .uv2(k4, j4)
                            .endVertex();
                }
            }
        }

        // End rendering if any snowflakes were rendered
        if (i1 >= 0) {
            tessellator.end();
        }

        // Restore render state
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        lightTexture.turnOffLightLayer();
    }

    public static void renderBlizzard(Minecraft mc,
                                      ClientLevel level,
                                      LightTexture lightTexture,
                                      int ticks,
                                      float partialTicks,
                                      double cameraX,
                                      double cameraY,
                                      double cameraZ) {
        float snowStrength = level.getThunderLevel(partialTicks); // Blizzard intensity based on thunder level
        lightTexture.turnOnLightLayer();

        int camX = Mth.floor(cameraX);
        int camY = Mth.floor(cameraY);
        int camZ = Mth.floor(cameraZ);
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tesselator.getBuilder();

        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.setShader(GameRenderer::getParticleShader);

        int renderRadius = Minecraft.useFancyGraphics() ? 10 : 5;
        RenderSystem.depthMask(Minecraft.useShaderTransparency());

        BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();
        float ticksAndPartialTicks = (float) ticks + partialTicks; // Smoothly interpolates particle position updates
        RandomSource random = RandomSource.create();

        int currentRenderingState = -1;

        for (int z = camZ - renderRadius; z <= camZ + renderRadius; ++z) {
            for (int x = camX - renderRadius; x <= camX + renderRadius; ++x) {
                blockPos.set(x, camY, z);

                Biome biome = level.getBiome(blockPos).value();
                if (!biome.hasPrecipitation()) continue;

                int groundHeight = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
                int minY = Math.max(camY - renderRadius, groundHeight);
                int maxY = camY + renderRadius;
                if (minY >= maxY) continue;

                if (currentRenderingState != 1) {
                    if (currentRenderingState >= 0) tesselator.end();
                    currentRenderingState = 1;
                    RenderSystem.setShaderTexture(0, new ResourceLocation("minecraft:textures/environment/snow.png"));
                    bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
                }

                // Snowflake drift and fall speed using ticksAndPartialTicks for smooth animation
                double offsetX = Math.sin(ticksAndPartialTicks * 0.01 + x) * 0.5D;
                double offsetZ = Math.cos(ticksAndPartialTicks * 0.01 + z) * 0.5D;
                double fallOffset = Math.cos(ticksAndPartialTicks * 0.01 + z) * 0.2D;

                // Snowflake fade based on distance from camera
                double dx = x + 0.5D - cameraX;
                double dz = z + 0.5D - cameraZ;
                float distance = (float) Math.sqrt(dx * dx + dz * dz) / (float) renderRadius;
                float alpha = ((1.0F - distance * distance) * 0.3F + 0.5F) * snowStrength;

                // Light and texture offset
                blockPos.set(x, groundHeight, z);
                int light = getLightColor(level, blockPos);
                int lightX = (light >> 16) & 0xFFFF;
                int lightY = light & 0xFFFF;

                // Render each vertex of the snowflake particle quad, adding smooth movement
                bufferBuilder.vertex(x - cameraX - 0.5D + offsetX, maxY - cameraY, z - cameraZ - 0.5D + offsetZ)
                        .uv(0.0F, minY * 0.25F).color(1.0F, 1.0F, 1.0F, alpha).uv2(lightX, lightY).endVertex();
                bufferBuilder.vertex(x - cameraX + 0.5D + offsetX, maxY - cameraY, z - cameraZ + 0.5D + offsetZ)
                        .uv(1.0F, minY * 0.25F).color(1.0F, 1.0F, 1.0F, alpha).uv2(lightX, lightY).endVertex();
                bufferBuilder.vertex(x - cameraX + 0.5D + offsetX, minY - cameraY, z - cameraZ + 0.5D + offsetZ)
                        .uv(1.0F, maxY * 0.25F).color(1.0F, 1.0F, 1.0F, alpha).uv2(lightX, lightY).endVertex();
                bufferBuilder.vertex(x - cameraX - 0.5D + offsetX, minY - cameraY, z - cameraZ - 0.5D + offsetZ)
                        .uv(0.0F, maxY * 0.25F).color(1.0F, 1.0F, 1.0F, alpha).uv2(lightX, lightY).endVertex();
            }
        }

        if (currentRenderingState >= 0) {
            tesselator.end(); // End the current rendering state
        }

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        lightTexture.turnOffLightLayer();
    }


}

