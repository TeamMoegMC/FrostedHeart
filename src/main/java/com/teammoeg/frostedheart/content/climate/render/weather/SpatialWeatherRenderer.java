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
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.client.event.RenderLevelStageEvent;

import java.util.function.Supplier;

/** V1 bounded wall, precipitation and ground-impact renderer. */
public final class SpatialWeatherRenderer {
    private static final ResourceLocation SNOW_TEXTURE =
            new ResourceLocation("minecraft", "textures/environment/snow.png");
    private static final ResourceLocation WHITE_CURTAIN_TEXTURE =
            new ResourceLocation(FHMain.MODID, "textures/environment/white_curtain.png");
    private static final int MAX_SNOW_COLUMNS = WeatherQualityProfile.FANCY.precipitationColumns();
    private static final double WALL_EDGE_FADE_BLOCKS = 48.0;
    private static final double WALL_CULL_DISTANCE_BLOCKS = 544.0;
    private static final double WALL_BELOW_CAMERA_BLOCKS = 64.0;
    private static final double WALL_ABOVE_CAMERA_BLOCKS = 96.0;
    public static final SpatialWeatherRenderer INSTANCE = new SpatialWeatherRenderer();
    private final MutableVisualWeatherSample columnSample = new MutableVisualWeatherSample();
    private final BlockPos.MutableBlockPos effectPos = new BlockPos.MutableBlockPos();
    private final double[] snowWorldX = new double[MAX_SNOW_COLUMNS];
    private final double[] snowWorldZ = new double[MAX_SNOW_COLUMNS];
    private final long[] snowHashes = new long[MAX_SNOW_COLUMNS];
    private ShaderInstance previousShader;
    private final Supplier<ShaderInstance> previousShaderSupplier = () -> previousShader;
    private ResourceKey<Level> quarantinedDimension;
    private long wallQuads;
    private long snowQuads;
    private long batches;
    private int snowLatticeBaseCellX;
    private int snowLatticeBaseCellZ;
    private int snowLatticeSide;
    private int snowLatticeSpacing;
    private boolean snowLatticeValid;

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
            renderFrame(frame, minecraft.level, event.getFrustum(), minecraft.gameRenderer.lightTexture());
        } catch (RuntimeException exception) {
            quarantinedDimension = minecraft.level.dimension();
            discardOpenBuffer(exception);
            frame.invalidate();
            FHMain.LOGGER.error("Spatial weather renderer failed; using compatibility rendering for this level session", exception);
        }
    }

    private void renderFrame(ClientWeatherFrame frame, ClientLevel level,
                             Frustum frustum, LightTexture lightTexture) {
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
            lightTexture.turnOnLightLayer();
            RenderSystem.setShader(GameRenderer::getParticleShader);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            float brightness = weatherBrightness(level.getSkyDarken(frame.partialTick()));
            if (frame.wallCount() > 0) {
                RenderSystem.setShaderTexture(0, WHITE_CURTAIN_TEXTURE);
                renderWalls(frame, level, frustum, brightness);
            }
            if (frame.ownsPrecipitation()) {
                RenderSystem.setShaderTexture(0, SNOW_TEXTURE);
                renderSnow(frame, brightness);
            }
        } finally {
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
            lightTexture.turnOffLightLayer();
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

    private void renderWalls(ClientWeatherFrame frame, ClientLevel level, Frustum frustum, float brightness) {
        WeatherQualityProfile profile = frame.profile();
        int remainingQuads = profile.wallSlices() * profile.wallSegments();
        int visibleWalls = Math.min(frame.wallCount(), profile.wallSlices());
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
        boolean emitted = false;
        double climateSeconds = ClientWeatherState.INSTANCE.frameClimateSeconds(frame.partialTick());
        double belowCamera = WALL_BELOW_CAMERA_BLOCKS;
        double aboveCamera = WALL_ABOVE_CAMERA_BLOCKS;
        double worldYBottom = frame.cameraY() - belowCamera;
        double worldYTop = frame.cameraY() + aboveCamera;
        double yBottom = -belowCamera;
        double yTop = aboveCamera;

        for (int wall = 0; wall < visibleWalls && remainingQuads > 0; wall++) {
            WhiteCurtainFieldModel.VisualKernel kernel = frame.wallKernel(wall);
            double leadingDelta = kernel.leadingSnowDeltaChunks(climateSeconds);
            if (leadingDelta < -3.0 || leadingDelta > kernel.maxDeltaChunks + 3.0) {
                continue;
            }
            int slices = wallSlicesForWall(wall, visibleWalls, profile.wallSlices());
            double sliceSpacing = profile == WeatherQualityProfile.FAST ? 6.0 : 4.5;
            double roughness = profile == WeatherQualityProfile.FAST ? 2.5 : 2.0;
            double baseFrontX = frontX(kernel, leadingDelta);
            double baseFrontZ = frontZ(kernel, leadingDelta);
            if (!wallBoundsVisible(frustum, kernel, baseFrontX, baseFrontZ,
                    slices, sliceSpacing, roughness, worldYBottom, worldYTop)) {
                continue;
            }
            double crossMin = kernel.moveX == 0.0F ? kernel.minBlockX : kernel.minBlockZ;
            double crossMax = kernel.moveX == 0.0F ? kernel.maxBlockXExclusive : kernel.maxBlockZExclusive;
            float flowDirection = (kernel.visualSeed & 1L) == 0L ? 1.0F : -1.0F;
            for (int slice = slices - 1; slice >= 0; slice--) {
                double depth = slice * sliceSpacing;
                double frontX = baseFrontX - kernel.moveX * depth;
                double frontZ = baseFrontZ - kernel.moveZ * depth;
                float sliceFade = 1.0F - slice / (float) (slices + 1);
                float textureOffset = (float) (((kernel.visualSeed >>> 8) & 1023) / 1024.0
                        + slice * 0.371);
                float vOffset = (float) (((kernel.visualSeed >>> 24) & 1023) / 1024.0
                        + slice * 0.293);
                float uScroll = (float) positiveModulo(
                        climateSeconds * (0.006 + slice * 0.0015) * flowDirection, 1.0);
                float vScroll = (float) positiveModulo(
                        climateSeconds * (0.013 + slice * 0.001), 1.0);
                int segments = Math.min(profile.wallSegments(), remainingQuads);
                for (int segment = 0; segment < segments; segment++) {
                    double t0 = segment / (double) segments;
                    double t1 = (segment + 1.0) / segments;
                    double cross0 = lerp(t0, crossMin, crossMax);
                    double cross1 = lerp(t1, crossMin, crossMax);
                    double offset0 = wallBoundaryOffset(kernel.visualSeed, slice, segment, roughness);
                    double offset1 = wallBoundaryOffset(kernel.visualSeed, slice, segment + 1, roughness);
                    double x0 = kernel.moveX == 0.0F ? cross0 : frontX + kernel.moveX * offset0;
                    double x1 = kernel.moveX == 0.0F ? cross1 : frontX + kernel.moveX * offset1;
                    double z0 = kernel.moveZ == 0.0F ? cross0 : frontZ + kernel.moveZ * offset0;
                    double z1 = kernel.moveZ == 0.0F ? cross1 : frontZ + kernel.moveZ * offset1;
                    double distanceSquared = wallSegmentDistanceSquared(
                            frame.cameraX(), frame.cameraZ(), x0, z0, x1, z1);
                    if (distanceSquared > WALL_CULL_DISTANCE_BLOCKS * WALL_CULL_DISTANCE_BLOCKS) {
                        continue;
                    }
                    double distance = Math.sqrt(distanceSquared);
                    float distanceFade = (float) Mth.clamp(
                            (WALL_CULL_DISTANCE_BLOCKS - distance) / 128.0, 0.0, 1.0);
                    float layerAlpha = wallLayerAlpha(sliceFade, distanceFade);
                    float alpha0 = layerAlpha
                            * wallEdgeFade(cross0, crossMin, crossMax, WALL_EDGE_FADE_BLOCKS);
                    float alpha1 = layerAlpha
                            * wallEdgeFade(cross1, crossMin, crossMax, WALL_EDGE_FADE_BLOCKS);
                    if (alpha0 <= 0.001F && alpha1 <= 0.001F) {
                        continue;
                    }
                    float u0 = (float) (cross0 / 12.0) + textureOffset + uScroll;
                    float u1 = (float) (cross1 / 12.0) + textureOffset + uScroll;
                    emitWallQuad(buffer, frame, x0, z0, x1, z1,
                            yBottom, yTop, u0, u1,
                            (float) (worldYBottom / 24.0) + vOffset + vScroll,
                            (float) (worldYTop / 24.0) + vOffset + vScroll,
                            alpha0, alpha1, brightness);
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
                                     float u0, float u1, float vBottom, float vTop,
                                     float alpha0, float alpha1, float brightness) {
        buffer.vertex(x0 - frame.cameraX(), yBottom, z0 - frame.cameraZ())
                .uv(u0, vBottom).color(
                        0.70F * brightness, 0.78F * brightness, 0.86F * brightness, alpha0)
                .uv2(LightTexture.FULL_BRIGHT).endVertex();
        buffer.vertex(x1 - frame.cameraX(), yBottom, z1 - frame.cameraZ())
                .uv(u1, vBottom).color(
                        0.70F * brightness, 0.78F * brightness, 0.86F * brightness, alpha1)
                .uv2(LightTexture.FULL_BRIGHT).endVertex();
        buffer.vertex(x1 - frame.cameraX(), yTop, z1 - frame.cameraZ())
                .uv(u1, vTop).color(
                        0.82F * brightness, 0.88F * brightness, 0.94F * brightness, alpha1 * 0.08F)
                .uv2(LightTexture.FULL_BRIGHT).endVertex();
        buffer.vertex(x0 - frame.cameraX(), yTop, z0 - frame.cameraZ())
                .uv(u0, vTop).color(
                        0.82F * brightness, 0.88F * brightness, 0.94F * brightness, alpha0 * 0.08F)
                .uv2(LightTexture.FULL_BRIGHT).endVertex();
    }

    private void renderSnow(ClientWeatherFrame frame, float brightness) {
        WeatherQualityProfile profile = frame.profile();
        int side = profile == WeatherQualityProfile.FAST ? 16 : 32;
        int spacing = profile == WeatherQualityProfile.FAST ? 2 : 1;
        int baseCellX = snowGridCellStart(frame.cameraX(), spacing, side);
        int baseCellZ = snowGridCellStart(frame.cameraZ(), spacing, side);
        prepareSnowLattice(baseCellX, baseCellZ, side, spacing);
        double time = ClientWeatherState.INSTANCE.frameClimateSeconds(frame.partialTick());
        double radius = side * spacing * 0.5;
        double radiusSquared = radius * radius;
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
        boolean emitted = false;
        int submitted = 0;
        int cells = side * side;
        for (int cell = 0; cell < cells && submitted < profile.precipitationColumns(); cell++) {
            double worldX = snowWorldX[cell];
            double worldZSample = snowWorldZ[cell];
            long hash = snowHashes[cell];
            double dx = worldX - frame.cameraX();
            double dz = worldZSample - frame.cameraZ();
            float radialFade = (float) Mth.clamp(
                    1.0 - (dx * dx + dz * dz) / radiusSquared, 0.0, 1.0);
            if (radialFade <= 0.0F) {
                continue;
            }
            frame.samplePrecipitation(worldX, worldZSample, columnSample);
            if (columnSample.snowIntensity <= 0.02F) {
                continue;
            }
            double inverse = 1.0 / Math.max(1.0, Math.sqrt(dx * dx + dz * dz));
            double rightX = -dz * inverse;
            double rightZ = dx * inverse;
            double width = 0.12 + columnSample.whiteoutIntensity * 0.16;
            double phase = positiveModulo((hash & 0xffff) * 0.001
                    + time * (0.75 + ((hash >>> 16) & 7) * 0.04), 24.0);
            double centerY = frame.cameraY() - 8.0 + phase;
            double halfHeight = 1.5 + columnSample.whiteoutIntensity * 2.0;
            double windLean = columnSample.windIntensity * (1.0 + halfHeight * 0.35);
            float alpha = columnSample.snowIntensity * radialFade * 0.72F;
            emitSnowQuad(buffer, frame, worldX, centerY, worldZSample,
                    rightX * width, rightZ * width,
                    columnSample.windX * windLean, columnSample.windZ * windLean,
                    halfHeight, alpha, brightness);
            emitted = true;
            snowQuads++;
            submitted++;
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
                                     double windX, double windZ, double halfHeight,
                                     float alpha, float brightness) {
        double bottomX = x - windX * 0.5 - frame.cameraX();
        double bottomZ = z - windZ * 0.5 - frame.cameraZ();
        double topX = x + windX * 0.5 - frame.cameraX();
        double topZ = z + windZ * 0.5 - frame.cameraZ();
        double bottom = y - halfHeight - frame.cameraY();
        double top = y + halfHeight - frame.cameraY();
        buffer.vertex(bottomX - rightX, bottom, bottomZ - rightZ)
                .uv(0.0F, 1.0F).color(brightness, brightness, brightness, alpha)
                .uv2(LightTexture.FULL_BRIGHT).endVertex();
        buffer.vertex(bottomX + rightX, bottom, bottomZ + rightZ)
                .uv(1.0F, 1.0F).color(brightness, brightness, brightness, alpha)
                .uv2(LightTexture.FULL_BRIGHT).endVertex();
        buffer.vertex(topX + rightX, top, topZ + rightZ)
                .uv(1.0F, 0.0F).color(brightness, brightness, brightness, alpha)
                .uv2(LightTexture.FULL_BRIGHT).endVertex();
        buffer.vertex(topX - rightX, top, topZ - rightZ)
                .uv(0.0F, 0.0F).color(brightness, brightness, brightness, alpha)
                .uv2(LightTexture.FULL_BRIGHT).endVertex();
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
        snowLatticeValid = false;
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
        return (0.34F + 0.34F * sliceFade) * distanceFade;
    }

    static float weatherBrightness(float skyDarken) {
        return 0.55F + 0.45F * Mth.clamp(skyDarken, 0.0F, 1.0F);
    }


    static float wallEdgeFade(double cross, double min, double max, double fadeBlocks) {
        if (fadeBlocks <= 0.0) {
            return 1.0F;
        }
        double edgeDistance = Math.min(cross - min, max - cross);
        float t = (float) Mth.clamp(edgeDistance / fadeBlocks, 0.0, 1.0);
        return t * t * (3.0F - 2.0F * t);
    }

    private static double wallBoundaryOffset(long seed, int slice, int boundary, double amplitude) {
        long value = mix(seed ^ ((long) slice * 0x9e3779b97f4a7c15L)
                ^ ((long) boundary * 0xbf58476d1ce4e5b9L));
        return signedUnit(value) * amplitude;
    }

    private static boolean wallBoundsVisible(Frustum frustum, WhiteCurtainFieldModel.VisualKernel kernel,
                                             double frontX, double frontZ, int slices,
                                             double sliceSpacing, double roughness,
                                             double worldYBottom, double worldYTop) {
        if (frustum == null) {
            return true;
        }
        double extent = Math.max(0, slices - 1) * sliceSpacing + roughness;
        double normalX0 = frontX + kernel.moveX * roughness;
        double normalX1 = frontX - kernel.moveX * extent;
        double normalZ0 = frontZ + kernel.moveZ * roughness;
        double normalZ1 = frontZ - kernel.moveZ * extent;
        double minX = kernel.moveX == 0.0F ? kernel.minBlockX : Math.min(normalX0, normalX1);
        double maxX = kernel.moveX == 0.0F ? kernel.maxBlockXExclusive : Math.max(normalX0, normalX1);
        double minZ = kernel.moveZ == 0.0F ? kernel.minBlockZ : Math.min(normalZ0, normalZ1);
        double maxZ = kernel.moveZ == 0.0F ? kernel.maxBlockZExclusive : Math.max(normalZ0, normalZ1);
        return frustum.isVisible(new AABB(minX, worldYBottom, minZ, maxX, worldYTop, maxZ));
    }

    private void prepareSnowLattice(int baseCellX, int baseCellZ, int side, int spacing) {
        if (snowLatticeValid && snowLatticeBaseCellX == baseCellX && snowLatticeBaseCellZ == baseCellZ
                && snowLatticeSide == side && snowLatticeSpacing == spacing) {
            return;
        }
        double jitterScale = spacing * 0.28;
        int cell = 0;
        for (int zi = 0; zi < side; zi++) {
            int cellZ = baseCellZ + zi;
            int gridZ = cellZ * spacing;
            for (int xi = 0; xi < side; xi++, cell++) {
                int cellX = baseCellX + xi;
                int gridX = cellX * spacing;
                long hash = mix(cellX, cellZ);
                snowWorldX[cell] = gridX + 0.5 + signedUnit(hash) * jitterScale;
                snowWorldZ[cell] = gridZ + 0.5 + signedUnit(hash >>> 16) * jitterScale;
                snowHashes[cell] = hash;
            }
        }
        snowLatticeBaseCellX = baseCellX;
        snowLatticeBaseCellZ = baseCellZ;
        snowLatticeSide = side;
        snowLatticeSpacing = spacing;
        snowLatticeValid = true;
    }

    private static double signedUnit(long bits) {
        return (bits & 0xffffL) / 32767.5 - 1.0;
    }

    static double wallSegmentDistanceSquared(double cameraX, double cameraZ,
                                             double x0, double z0, double x1, double z1) {
        double segmentX = x1 - x0;
        double segmentZ = z1 - z0;
        double lengthSquared = segmentX * segmentX + segmentZ * segmentZ;
        double t = lengthSquared <= 1.0e-9 ? 0.0 : Mth.clamp(
                ((cameraX - x0) * segmentX + (cameraZ - z0) * segmentZ) / lengthSquared,
                0.0, 1.0);
        double dx = cameraX - (x0 + segmentX * t);
        double dz = cameraZ - (z0 + segmentZ * t);
        return dx * dx + dz * dz;
    }

    private static long mix(int x, int z) {
        return mix(((long) x << 32) ^ (z & 0xffffffffL));
    }

    private static long mix(long value) {
        value = (value ^ (value >>> 30)) * 0xbf58476d1ce4e5b9L;
        value = (value ^ (value >>> 27)) * 0x94d049bb133111ebL;
        return value ^ (value >>> 31);
    }
}
