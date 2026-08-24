/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.render.weather;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.bootstrap.reference.FHParticleTypes;
import com.teammoeg.frostedheart.content.climate.gamedata.climate.MutableVisualWeatherSample;
import com.teammoeg.frostedheart.content.climate.gamedata.climate.WhiteCurtainFieldModel;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.client.event.RenderLevelStageEvent;

import java.util.function.Supplier;

/** V1 bounded wall, precipitation and ground-impact renderer. */
public final class SpatialWeatherRenderer {
    public static final SpatialWeatherRenderer INSTANCE = new SpatialWeatherRenderer();
    private static final ResourceLocation SNOW_TEXTURE =
            new ResourceLocation("minecraft", "textures/environment/snow.png");
    private static final ResourceLocation WHITE_CURTAIN_TEXTURE =
            new ResourceLocation(FHMain.MODID, "textures/environment/white_curtain.png");
    private final MutableVisualWeatherSample columnSample = new MutableVisualWeatherSample();
    private final BlockPos.MutableBlockPos effectPos = new BlockPos.MutableBlockPos();
    private ShaderInstance previousShader;
    private final Supplier<ShaderInstance> previousShaderSupplier = () -> previousShader;
    private ResourceKey<Level> quarantinedDimension;
    private long wallQuads;
    private long snowQuads;
    private long batches;

    private SpatialWeatherRenderer() {
    }

    public void render(RenderLevelStageEvent event) {
        ClientWeatherFrame frame = ClientWeatherFrame.INSTANCE;
        Minecraft minecraft = Minecraft.getInstance();
        if (!frame.valid() || frame.ownership() == ClientWeatherFrame.Ownership.FALLBACK
                || minecraft.level == null || minecraft.level.dimension().equals(quarantinedDimension)) {
            return;
        }
        try {
            renderFrame(frame, minecraft.level);
        } catch (RuntimeException exception) {
            quarantinedDimension = minecraft.level.dimension();
            discardOpenBuffer(exception);
            frame.invalidate();
            FHMain.LOGGER.error("Spatial weather renderer failed; using compatibility rendering for this level session", exception);
        }
    }

    private void renderFrame(ClientWeatherFrame frame, ClientLevel level) {
        previousShader = RenderSystem.getShader();
        int previousTexture = RenderSystem.getShaderTexture(0);
        float[] previousColor = RenderSystem.getShaderColor();
        float previousRed = previousColor[0];
        float previousGreen = previousColor[1];
        float previousBlue = previousColor[2];
        float previousAlpha = previousColor[3];
        try {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();
            RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            if (frame.wallCount() > 0) {
                RenderSystem.setShaderTexture(0, WHITE_CURTAIN_TEXTURE);
                renderWalls(frame, level);
            }
            if (frame.ownsPrecipitation()) {
                RenderSystem.setShaderTexture(0, SNOW_TEXTURE);
                renderSnow(frame);
            }
        } finally {
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
            RenderSystem.setShaderTexture(0, previousTexture);
            RenderSystem.setShader(previousShaderSupplier);
            RenderSystem.setShaderColor(previousRed, previousGreen, previousBlue, previousAlpha);
            previousShader = null;
        }
    }

    private static void discardOpenBuffer(RuntimeException renderFailure) {
        try {
            BufferBuilder buffer = Tesselator.getInstance().getBuilder();
            if (buffer.building()) {
                var rendered = buffer.endOrDiscardIfEmpty();
                if (rendered != null) {
                    rendered.release();
                }
            }
        } catch (RuntimeException cleanupFailure) {
            renderFailure.addSuppressed(cleanupFailure);
        }
    }

    private void renderWalls(ClientWeatherFrame frame, ClientLevel level) {
        WeatherQualityProfile profile = frame.profile();
        int remainingQuads = profile.wallSlices() * profile.wallSegments();
        int visibleWalls = Math.min(frame.wallCount(), profile.wallSlices());
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        boolean emitted = false;
        double climateSeconds = ClientWeatherState.INSTANCE.frameClimateSeconds(frame.partialTick());
        double yBottom = level.getMinBuildHeight() - frame.cameraY();
        double yTop = level.getMaxBuildHeight() - frame.cameraY();
        float vScroll = (float) ((climateSeconds * 0.018) % 1.0);
        float worldVBottom = level.getMinBuildHeight() / 32.0F + vScroll;
        float worldVTop = level.getMaxBuildHeight() / 32.0F + vScroll;

        for (int wall = 0; wall < visibleWalls && remainingQuads > 0; wall++) {
            WhiteCurtainFieldModel.VisualKernel kernel = frame.wallKernel(wall);
            double leadingDelta = kernel.leadingSnowDeltaChunks(climateSeconds);
            if (leadingDelta < -3.0 || leadingDelta > kernel.maxDeltaChunks + 3.0) {
                continue;
            }
            int slices = wallSlicesForWall(wall, visibleWalls, profile.wallSlices());
            for (int slice = slices - 1; slice >= 0; slice--) {
                double depth = slice * (profile == WeatherQualityProfile.FAST ? 7.0 : 5.0);
                double frontX = frontX(kernel, leadingDelta) - kernel.moveX * depth;
                double frontZ = frontZ(kernel, leadingDelta) - kernel.moveZ * depth;
                float sliceFade = 1.0F - slice / (float) (slices + 1);
                float textureOffset = (float) (((kernel.visualSeed >>> 8) & 1023) / 1024.0
                        + slice * 0.371);
                float vOffset = (float) (((kernel.visualSeed >>> 24) & 1023) / 1024.0
                        + slice * 0.293);
                int segments = Math.min(profile.wallSegments(), remainingQuads);
                for (int segment = 0; segment < segments; segment++) {
                    double t0 = segment / (double) segments;
                    double t1 = (segment + 1.0) / segments;
                    double x0 = kernel.moveX == 0.0F
                            ? lerp(t0, kernel.minBlockX, kernel.maxBlockXExclusive) : frontX;
                    double x1 = kernel.moveX == 0.0F
                            ? lerp(t1, kernel.minBlockX, kernel.maxBlockXExclusive) : frontX;
                    double z0 = kernel.moveZ == 0.0F
                            ? lerp(t0, kernel.minBlockZ, kernel.maxBlockZExclusive) : frontZ;
                    double z1 = kernel.moveZ == 0.0F
                            ? lerp(t1, kernel.minBlockZ, kernel.maxBlockZExclusive) : frontZ;
                    double distanceSquared = wallSegmentDistanceSquared(
                            frame.cameraX(), frame.cameraZ(), x0, z0, x1, z1);
                    if (distanceSquared > 544.0 * 544.0) {
                        continue;
                    }
                    double distance = Math.sqrt(distanceSquared);
                    float distanceFade = (float) Mth.clamp((544.0 - distance) / 128.0, 0.0, 1.0);
                    float alpha = wallLayerAlpha(sliceFade, distanceFade);
                    double cross0 = kernel.moveX == 0.0F ? x0 : z0;
                    double cross1 = kernel.moveX == 0.0F ? x1 : z1;
                    float u0 = (float) (cross0 / 24.0) + textureOffset;
                    float u1 = (float) (cross1 / 24.0) + textureOffset;
                    emitWallQuad(buffer, frame, x0, z0, x1, z1,
                            yBottom, yTop, u0, u1,
                            worldVBottom + vOffset, worldVTop + vOffset, alpha);
                    emitted = true;
                    wallQuads++;
                    remainingQuads--;
                    if (remainingQuads == 0) {
                        break;
                    }
                }
            }
        }
        if (emitted) {
            tesselator.end();
            batches++;
        } else {
            buffer.endOrDiscardIfEmpty();
        }
    }

    static int wallSlicesForWall(int wallIndex, int visibleWallCount, int totalSlices) {
        if (wallIndex < 0 || wallIndex >= visibleWallCount || visibleWallCount <= 0 || totalSlices <= 0) {
            return 0;
        }
        int base = totalSlices / visibleWallCount;
        return base + (wallIndex < totalSlices % visibleWallCount ? 1 : 0);
    }

    private static void emitWallQuad(BufferBuilder buffer, ClientWeatherFrame frame,
                                     double x0, double z0, double x1, double z1,
                                     double yBottom, double yTop,
                                     float u0, float u1, float vBottom, float vTop, float alpha) {
        buffer.vertex(x0 - frame.cameraX(), yBottom, z0 - frame.cameraZ())
                .uv(u0, vBottom).color(0.76F, 0.83F, 0.90F, alpha).endVertex();
        buffer.vertex(x1 - frame.cameraX(), yBottom, z1 - frame.cameraZ())
                .uv(u1, vBottom).color(0.76F, 0.83F, 0.90F, alpha).endVertex();
        buffer.vertex(x1 - frame.cameraX(), yTop, z1 - frame.cameraZ())
                .uv(u1, vTop).color(0.92F, 0.96F, 1.0F, alpha * 0.82F).endVertex();
        buffer.vertex(x0 - frame.cameraX(), yTop, z0 - frame.cameraZ())
                .uv(u0, vTop).color(0.92F, 0.96F, 1.0F, alpha * 0.82F).endVertex();
    }

    private void renderSnow(ClientWeatherFrame frame) {
        WeatherQualityProfile profile = frame.profile();
        int side = profile == WeatherQualityProfile.FAST ? 16 : 32;
        int spacing = profile == WeatherQualityProfile.FAST ? 2 : 1;
        int baseCellX = snowGridCellStart(frame.cameraX(), spacing, side);
        int baseCellZ = snowGridCellStart(frame.cameraZ(), spacing, side);
        double time = ClientWeatherState.INSTANCE.frameClimateSeconds(frame.partialTick());
        double jitterScale = spacing * 0.28;
        double radius = side * spacing * 0.5;
        double radiusSquared = radius * radius;
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        boolean emitted = false;
        int submitted = 0;
        for (int zi = 0; zi < side && submitted < profile.precipitationColumns(); zi++) {
            int cellZ = baseCellZ + zi;
            int gridZ = cellZ * spacing;
            for (int xi = 0; xi < side && submitted < profile.precipitationColumns(); xi++) {
                int cellX = baseCellX + xi;
                int gridX = cellX * spacing;
                long hash = mix(cellX, cellZ);
                double worldX = gridX + 0.5 + signedUnit(hash) * jitterScale;
                double worldZSample = gridZ + 0.5 + signedUnit(hash >>> 16) * jitterScale;
                frame.samplePrecipitation(worldX, worldZSample, columnSample);
                if (columnSample.snowIntensity <= 0.02F) {
                    continue;
                }
                double dx = worldX - frame.cameraX();
                double dz = worldZSample - frame.cameraZ();
                float radialFade = (float) Mth.clamp(
                        1.0 - (dx * dx + dz * dz) / radiusSquared, 0.0, 1.0);
                if (radialFade <= 0.0F) {
                    continue;
                }
                double inverse = 1.0 / Math.max(1.0, Math.sqrt(dx * dx + dz * dz));
                double rightX = -dz * inverse;
                double rightZ = dx * inverse;
                double width = 0.12 + columnSample.whiteoutIntensity * 0.16;
                double phase = positiveModulo((hash & 0xffff) * 0.001 + time * (0.75 + ((hash >>> 16) & 7) * 0.04), 24.0);
                double centerY = frame.cameraY() - 8.0 + phase;
                double halfHeight = 1.5 + columnSample.whiteoutIntensity * 2.0;
                double windLean = columnSample.windIntensity * (1.0 + halfHeight * 0.35);
                float alpha = columnSample.snowIntensity * radialFade * 0.72F;
                emitSnowQuad(buffer, frame, worldX, centerY, worldZSample,
                        rightX * width, rightZ * width,
                        columnSample.windX * windLean, columnSample.windZ * windLean,
                        halfHeight, alpha);
                emitted = true;
                snowQuads++;
                submitted++;
            }
        }
        if (emitted) {
            tesselator.end();
            batches++;
        } else {
            buffer.endOrDiscardIfEmpty();
        }
    }

    private static void emitSnowQuad(BufferBuilder buffer, ClientWeatherFrame frame,
                                     double x, double y, double z, double rightX, double rightZ,
                                     double windX, double windZ, double halfHeight, float alpha) {
        double bottomX = x - windX * 0.5 - frame.cameraX();
        double bottomZ = z - windZ * 0.5 - frame.cameraZ();
        double topX = x + windX * 0.5 - frame.cameraX();
        double topZ = z + windZ * 0.5 - frame.cameraZ();
        double bottom = y - halfHeight - frame.cameraY();
        double top = y + halfHeight - frame.cameraY();
        buffer.vertex(bottomX - rightX, bottom, bottomZ - rightZ)
                .uv(0.0F, 1.0F).color(1.0F, 1.0F, 1.0F, alpha).endVertex();
        buffer.vertex(bottomX + rightX, bottom, bottomZ + rightZ)
                .uv(1.0F, 1.0F).color(1.0F, 1.0F, 1.0F, alpha).endVertex();
        buffer.vertex(topX + rightX, top, topZ + rightZ)
                .uv(1.0F, 0.0F).color(1.0F, 1.0F, 1.0F, alpha).endVertex();
        buffer.vertex(topX - rightX, top, topZ - rightZ)
                .uv(0.0F, 0.0F).color(1.0F, 1.0F, 1.0F, alpha).endVertex();
    }

    public void tickGroundEffects(Minecraft minecraft, ClientLevel level, Camera camera) {
        ClientWeatherState state = ClientWeatherState.INSTANCE;
        ParticleStatus particleStatus = minecraft.options.particles().get();
        if (!state.tickOwnsPrecipitation() || particleStatus == ParticleStatus.MINIMAL) {
            return;
        }
        float intensity = state.tickCameraSample().snowIntensity;
        float particleScale = particleStatus == ParticleStatus.DECREASED ? 0.5F : 1.0F;
        WeatherQualityProfile profile = state.activeProfile();
        int attempts = Mth.ceil(intensity * profile.terrainQueriesPerTick() * particleScale);
        int cameraY = camera.getBlockPosition().getY();
        int radius = profile == WeatherQualityProfile.FAST ? 10 : 16;
        for (int i = 0; i < attempts; i++) {
            int x = camera.getBlockPosition().getX() + level.random.nextInt(radius * 2 + 1) - radius;
            int z = camera.getBlockPosition().getZ() + level.random.nextInt(radius * 2 + 1) - radius;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
            if (y < cameraY - 12 || y > cameraY + 12) {
                continue;
            }
            effectPos.set(x, y, z);
            level.addParticle(FHParticleTypes.SNOW.get(), x + level.random.nextDouble(), y + 0.05,
                    z + level.random.nextDouble(), 0.0, 0.0, 0.0);
        }
    }

    public void reset() {
        quarantinedDimension = null;
    }

    public boolean isQuarantined(ResourceKey<Level> dimension) {
        return dimension != null && dimension.equals(quarantinedDimension);
    }

    public long wallQuads() {
        return wallQuads;
    }

    public long snowQuads() {
        return snowQuads;
    }

    public long batches() {
        return batches;
    }

    private static double frontX(WhiteCurtainFieldModel.VisualKernel kernel, double delta) {
        if (kernel.moveX > 0.0F) {
            return (kernel.minChunkX + delta) * 16.0 + 8.0;
        }
        if (kernel.moveX < 0.0F) {
            return (kernel.maxChunkX - delta) * 16.0 + 8.0;
        }
        return (kernel.minBlockX + kernel.maxBlockXExclusive) * 0.5;
    }

    private static double frontZ(WhiteCurtainFieldModel.VisualKernel kernel, double delta) {
        if (kernel.moveZ > 0.0F) {
            return (kernel.minChunkZ + delta) * 16.0 + 8.0;
        }
        if (kernel.moveZ < 0.0F) {
            return (kernel.maxChunkZ - delta) * 16.0 + 8.0;
        }
        return (kernel.minBlockZ + kernel.maxBlockZExclusive) * 0.5;
    }

    private static double lerp(double delta, double start, double end) {
        return start + delta * (end - start);
    }

    private static double positiveModulo(double value, double divisor) {
        double result = value % divisor;
        return result < 0.0 ? result + divisor : result;
    }

    static int snowGridStart(double cameraCoordinate, int spacing, int side) {
        return snowGridCellStart(cameraCoordinate, spacing, side) * spacing;
    }

    private static int snowGridCellStart(double cameraCoordinate, int spacing, int side) {
        int cameraCell = Math.floorDiv(Mth.floor(cameraCoordinate), spacing);
        return cameraCell - side / 2;
    }

    static float wallLayerAlpha(float sliceFade, float distanceFade) {
        return (0.24F + 0.30F * sliceFade) * distanceFade;
    }

    private static double signedUnit(long bits) {
        return (bits & 0xffffL) / 32767.5 - 1.0;
    }

    static double wallSegmentDistanceSquared(double cameraX, double cameraZ,
                                             double x0, double z0, double x1, double z1) {
        double dx;
        double dz;
        if (Math.abs(x1 - x0) >= Math.abs(z1 - z0)) {
            dx = axisDistance(cameraX, Math.min(x0, x1), Math.max(x0, x1));
            dz = cameraZ - z0;
        } else {
            dx = cameraX - x0;
            dz = axisDistance(cameraZ, Math.min(z0, z1), Math.max(z0, z1));
        }
        return dx * dx + dz * dz;
    }

    private static double axisDistance(double coordinate, double min, double max) {
        if (coordinate < min) {
            return min - coordinate;
        }
        if (coordinate > max) {
            return coordinate - max;
        }
        return 0.0;
    }

    private static long mix(int x, int z) {
        long value = ((long) x << 32) ^ (z & 0xffffffffL);
        value = (value ^ (value >>> 30)) * 0xbf58476d1ce4e5b9L;
        value = (value ^ (value >>> 27)) * 0x94d049bb133111ebL;
        return value ^ (value >>> 31);
    }
}
