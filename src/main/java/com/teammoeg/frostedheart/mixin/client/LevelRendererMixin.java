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

package com.teammoeg.frostedheart.mixin.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import com.teammoeg.frostedheart.bootstrap.reference.FHParticleTypes;
import com.teammoeg.frostedheart.bootstrap.reference.FHSoundEvents;
import com.teammoeg.frostedheart.content.climate.render.InfraredViewRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.FogType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

/**
 * Used to render blizzard
 * Set priority to 1 for injecting earlier than Primal Winter.
 *
 * @author alcatrazEscapee, yuesha-yc
 * <p>
 * License: MIT
 */
@OnlyIn(Dist.CLIENT)
@Mixin(value = LevelRenderer.class, priority = 1)
public abstract class LevelRendererMixin {
    @Shadow
    @Final
    private static ResourceLocation SNOW_LOCATION;

    @Shadow
    @Final
    private Minecraft minecraft;
    @Shadow
    private ClientLevel level;
    @Shadow
    private int ticks;
    @Shadow
    private int rainSoundTime;
    private int windSoundTime;

    /**
     * Render blizzard. TODO: Implement directly using primal winter code
     */
//    @Inject(method = "renderSnowAndRain", at = @At("HEAD"), cancellable = true)
//    public void inject$renderWeather(LightTexture manager, float partialTicks, double x, double y, double z, CallbackInfo ci) {
//        if (this.minecraft != null && this.minecraft.gameRenderer != null) {
//            // ClimateData data = ClimateData.get(world);
//            // blizzard when vanilla 'thundering' is true, to save us from doing sync
//            if (level.isThundering()) {
//                BlizzardRenderer.renderBlizzard(minecraft, this.level, manager, ticks, partialTicks, x, y, z);
//                // Road-block injection to remove any Vanilla / Primal Winter weather rendering code
//                ci.cancel();
//            }
//        }
//    }

    /**
     * Capture camera pose for infrared view rendering.
     *
     * @author KilaBash
     */
    @Inject(method = "renderLevel", at = @At(value = "HEAD"))
    private void beforeRenderLever(PoseStack pPoseStack, float pPartialTick, long pFinishNanoTime, boolean pRenderBlockOutline, Camera pCamera, GameRenderer pGameRenderer, LightTexture pLightTexture, Matrix4f pProjectionMatrix, CallbackInfo ci) {
        InfraredViewRenderer.setCameraPose(pPoseStack);
    }

    /**
     * Always use rain rendering.
     *
     * @author alcatrazEscapee
     */
    @Redirect(method = "renderSnowAndRain", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/biome/Biome;getPrecipitationAt(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/biome/Biome$Precipitation;"))
    private Biome.Precipitation alwaysUseRainRendering(Biome biome, BlockPos pos) {
        if (FHConfig.CLIENT.weatherRenderChanges.get()) {
            return Biome.Precipitation.RAIN;
        }
        return biome.getPrecipitationAt(pos);
    }

    /**
     * Adjusts the light color for snow.
     *
     * @author alcatrazEscapee
     */
    @Redirect(method = "renderSnowAndRain", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;getLightColor(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;)I"))
    private int getAdjustedLightColorForSnow(BlockAndTintGetter level, BlockPos pos) {
        final int packedLight = LevelRenderer.getLightColor(level, pos);
        if (FHConfig.CLIENT.weatherRenderChanges.get()) {
            // Adjusts the light color via a heuristic that mojang uses to make snow appear more white
            // This targets both paths, but since we always use the rain rendering, it's fine.
            final int lightU = packedLight & 0xffff;
            final int lightV = (packedLight >> 16) & 0xffff;
            final int brightLightU = (lightU * 3 + 240) / 4;
            final int brightLightV = (lightV * 3 + 240) / 4;
            return brightLightU | (brightLightV << 16);
        }
        return packedLight;
    }

    /**
     * Override with snow textures.
     *
     * @author alcatrazEscapee
     */
    @Inject(method = "renderSnowAndRain", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/BufferBuilder;begin(Lcom/mojang/blaze3d/vertex/VertexFormat$Mode;Lcom/mojang/blaze3d/vertex/VertexFormat;)V"))
    private void overrideWithSnowTextures(LightTexture lightTexture, float partialTick, double x, double y, double z, CallbackInfo ci) {
        if (FHConfig.CLIENT.weatherRenderChanges.get()) {
            RenderSystem.setShaderTexture(0, SNOW_LOCATION);
        }
    }

    /**
     * Modify snow amount.
     *
     * @author alcatrazEscapee
     */
    @ModifyConstant(method = "renderSnowAndRain", constant = {@Constant(intValue = 5), @Constant(intValue = 10)})
    private int modifySnowAmount(int constant) {
        // This constant is used to control how much snow is rendered - 5 with default, 10 with fancy graphics. By default, we bump this all the way to 15.
        int density = FHConfig.CLIENT.snowDensity.get();
        int blizzardDensity = FHConfig.CLIENT.blizzardDensity.get();
        return level.isThundering() ? blizzardDensity : density;
    }

    /**
     * Add extra snow particles and sounds.
     * <p>
     * TODO: Adjust wind sound frequency and level based on blizzard intensity.
     *
     * @author alcatrazEscapee
     */
    @Inject(method = "tickRain", at = @At("HEAD"))
    private void addExtraSnowParticlesAndSounds(Camera camera, CallbackInfo ci) {
        if (!FHConfig.CLIENT.snowSounds.get()) {
            // Prevent default rain/snow sounds by setting rainSoundTime to -1, which means the if() checking it will never pass
            rainSoundTime = -1;
        }

        final float rain = level.getRainLevel(1f) / (Minecraft.useFancyGraphics() ? 1f : 2f);
        if (rain > 0f) {
            final Random random = new Random((long) ticks * 312987231L);
            final BlockPos cameraPos = BlockPos.containing(camera.getPosition());
            BlockPos pos = null;

            // Normal snow
            int particleCount = (int) (100.0F * rain * rain) / (minecraft.options.particles().get() == ParticleStatus.DECREASED ? 2 : 1);
            // Snow storm
            particleCount *= 2;
            for (int i = 0; i < particleCount; ++i) {
                final BlockPos randomPos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, cameraPos.offset(random.nextInt(21) - 10, 0, random.nextInt(21) - 10));
                final Biome biome = level.getBiome(randomPos).value();
                if (randomPos.getY() > level.getMinBuildHeight() && randomPos.getY() <= cameraPos.getY() + 10 && randomPos.getY() >= cameraPos.getY() - 10 && biome.coldEnoughToSnow(randomPos)) // Change: use SNOW and coldEnoughToSnow() instead
                {
                    pos = randomPos.below();
                    if (minecraft.options.particles().get() == ParticleStatus.MINIMAL) {
                        break;
                    }

                    final double dx = random.nextDouble(), dz = random.nextDouble();
                    final BlockState state = level.getBlockState(pos);
                    final FluidState fluid = level.getFluidState(pos);
                    final double blockY = state.getCollisionShape(level, pos).max(Direction.Axis.Y, dx, dz);
                    final double fluidY = fluid.getHeight(level, pos);
                    final ParticleOptions particle = !fluid.is(FluidTags.LAVA) && !state.is(Blocks.MAGMA_BLOCK) && !CampfireBlock.isLitCampfire(state) ? FHParticleTypes.SNOW.get() : ParticleTypes.SMOKE;

                    level.addParticle(particle, pos.getX() + dx, pos.getY() + Math.max(blockY, fluidY), pos.getZ() + dz, 0d, 0d, 0d);
                }
            }

            if (pos != null && random.nextInt(3) < rainSoundTime++) {
                rainSoundTime = 0;
                if (pos.getY() > cameraPos.getY() + 1 && level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, cameraPos).getY() > Mth.floor((float) cameraPos.getY())) {
                    level.playLocalSound(pos, SoundEvents.WEATHER_RAIN_ABOVE, SoundSource.WEATHER, 0.05f, 0.2f, false);
                } else {
                    level.playLocalSound(pos, SoundEvents.WEATHER_RAIN, SoundSource.WEATHER, 0.1f, 0.5f, false);
                }
            }

            // Added
            if (windSoundTime-- < 0 && FHConfig.CLIENT.windSounds.get()) {
                final BlockPos playerPos = camera.getBlockPosition();
                final Entity entity = camera.getEntity();
                int light = camera.getEntity().level().getBrightness(LightLayer.SKY, playerPos);
                if (light > 3 && entity.level().isRaining() && entity.level().getBiome(playerPos).value().coldEnoughToSnow(playerPos)) {
                    // In a windy location, play wind sounds
                    float volumeModifier = 0.2f + (light - 3) * 0.01f;
                    // In snow storm, increase volume, added by yuesha-yc
                    if (level.isThundering()) {
                        volumeModifier *= 2;
                    }
                    float pitchModifier = 0.7f;
                    if (camera.getFluidInCamera() != FogType.NONE) {
                        pitchModifier = 0.3f;
                    }
                    // In normal snow
                    windSoundTime = 20 * 6 + random.nextInt(30);
                    // In snow storm, added by yuesha-yc
                    if (level.isThundering()) {
                        windSoundTime = 20 * 3 + random.nextInt(20);
                    }
                    level.playLocalSound(playerPos, FHSoundEvents.WIND.get(), SoundSource.WEATHER, volumeModifier, pitchModifier, false);
                } else {
                    windSoundTime += 5; // check a short time later
                }
            }
        }
    }

}
