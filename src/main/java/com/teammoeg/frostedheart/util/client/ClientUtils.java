/*
 * Copyright (c) 2021-2024 TeamMoeg
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

package com.teammoeg.frostedheart.util.client;

import java.util.Random;

import com.teammoeg.frostedheart.FHParticleTypes;
import com.teammoeg.frostedheart.content.research.gui.ResearchGui;

import dev.ftb.mods.ftblibrary.ui.BaseScreen;
import dev.ftb.mods.ftblibrary.ui.IScreenWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.particles.IParticleData;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

public class ClientUtils {
    public static float spgamma;
    public static boolean applyspg;

    public static ClientPlayerEntity getPlayer() {
        return mc().player;
    }

    public static World getWorld() {
        return mc().world;
    }
    public static float partialTicks() {
    	return mc().getRenderPartialTicks();
    }
    public static Minecraft mc() {
        return Minecraft.getInstance();
    }
    public static void bindTexture(ResourceLocation texture) {
    	mc().getTextureManager().bindTexture(texture);
    }
    public static FontRenderer font() {
        return mc().fontRenderer;
    }
    public static void refreshResearchGui() {
        Screen cur = mc().currentScreen;
        if (cur instanceof IScreenWrapper) {
            BaseScreen bs = ((IScreenWrapper) cur).getGui();
            if (bs instanceof ResearchGui) {
                bs.refreshWidgets();
            }
        }
        mc().getLanguageManager();
    }

    public static void spawnFireParticles(World worldIn, BlockPos pos) {
        Random random = worldIn.getRandom();
        // Upward flame
        worldIn.addParticle(ParticleTypes.FLAME, pos.getX() + 0.25D + random.nextDouble() / 2.0D * (random.nextBoolean() ? 1 : -1), pos.getY() + 0.4D, pos.getZ() + 0.25D + random.nextDouble() / 2.0D * (random.nextBoolean() ? 1 : -1), 0.0D, 0.01D, 0.0D);
        // Side flame (4 directions)
        worldIn.addParticle(ParticleTypes.FLAME, pos.getX() + 0.25D + random.nextDouble() / 2.0D * (random.nextBoolean() ? 1 : -1), pos.getY() + 0.4D, pos.getZ() + 0.25D + random.nextDouble() / 2.0D * (random.nextBoolean() ? 1 : -1), 0.01D, 0D, 0.0D);
        worldIn.addParticle(ParticleTypes.FLAME, pos.getX() + 0.25D + random.nextDouble() / 2.0D * (random.nextBoolean() ? 1 : -1), pos.getY() + 0.4D, pos.getZ() + 0.25D + random.nextDouble() / 2.0D * (random.nextBoolean() ? 1 : -1), -0.01D, 0D, 0.0D);
        worldIn.addParticle(ParticleTypes.FLAME, pos.getX() + 0.25D + random.nextDouble() / 2.0D * (random.nextBoolean() ? 1 : -1), pos.getY() + 0.4D, pos.getZ() + 0.25D + random.nextDouble() / 2.0D * (random.nextBoolean() ? 1 : -1), 0.0D, 0D, 0.01D);
        worldIn.addParticle(ParticleTypes.FLAME, pos.getX() + 0.25D + random.nextDouble() / 2.0D * (random.nextBoolean() ? 1 : -1), pos.getY() + 0.4D, pos.getZ() + 0.25D + random.nextDouble() / 2.0D * (random.nextBoolean() ? 1 : -1), 0.0D, 0D, -0.01D);
    }

    public static void spawnSmokeParticles(World worldIn, BlockPos pos) {
        Random random = worldIn.getRandom();
        worldIn.addOptionalParticle(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, true, pos.getX() + 0.5D + random.nextDouble() / 3.0D * (random.nextBoolean() ? 1 : -1), pos.getY() + random.nextDouble() + random.nextDouble(), pos.getZ() + 0.5D + random.nextDouble() / 3.0D * (random.nextBoolean() ? 1 : -1), 0.0D, 0.05D, 0.0D);
        worldIn.addParticle(ParticleTypes.SMOKE, pos.getX() + 0.25D + random.nextDouble() / 2.0D * (random.nextBoolean() ? 1 : -1), pos.getY() + 0.4D, pos.getZ() + 0.25D + random.nextDouble() / 2.0D * (random.nextBoolean() ? 1 : -1), 0.002D, 0.01D, 0.0D);
    }

    public static void spawnT2SmokeParticles(World worldIn, BlockPos pos) {
        Random random = worldIn.getRandom();
        worldIn.addOptionalParticle(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, true, pos.getX() + 0.5D + random.nextDouble() / 3.0D * (random.nextBoolean() ? 1 : -1), pos.getY() + 0.5D + random.nextDouble() + random.nextDouble(), pos.getZ() + 0.5D + random.nextDouble() / 3.0D * (random.nextBoolean() ? 1 : -1), 0.0D, 0.12D, 0.0D);
        worldIn.addParticle(ParticleTypes.SMOKE, pos.getX() + 0.5D + random.nextDouble() / 2.0D * (random.nextBoolean() ? 1 : -1), pos.getY() + 0.4D, pos.getZ() + 0.5D + random.nextDouble() / 2.0D * (random.nextBoolean() ? 1 : -1), 0.0D, 0.05D, 0.0D);
    }

    public static void spawnCustomParticles(World worldIn, BlockPos pos, IParticleData particle, double posX, double posY, double posZ, double velX, double velY, double velZ) {
        // Spawn the specified particle type without randomness in position and velocity
        worldIn.addOptionalParticle(particle, true, pos.getX() + posX, pos.getY() + posY, pos.getZ() + posZ, velX, velY, velZ);
        worldIn.addParticle(particle, pos.getX() + posX, pos.getY() + posY, pos.getZ() + posZ, velX, velY, velZ);
    }

    public static void spawnInvertedConeSteam(World worldIn, BlockPos generatorTopPos, Vector3d windVelocity) {
        Random random = worldIn.getRandom();
        double vYMean = 0.02; // Mean speed of the particles upwards
        double speedVar = 0.01; // Variance in speed of the particles

        // To ensure the particle spawn at the center of the block and introduce a bit of randomness
        double x = 0.5 + random.nextGaussian() * 0.1 * 2;
        double y = 0.5 + random.nextGaussian() * 0.1; // to prevent particles from being stuck in the tower
        double z = 0.5 + random.nextGaussian() * 0.1 * 2;

        // Incorporating wind effect in particle velocity
        double vX = windVelocity.x + random.nextGaussian() * speedVar;
        double vY = windVelocity.y + random.nextGaussian() * speedVar + vYMean; // Ensure upward movement with variability
        double vZ = windVelocity.z + random.nextGaussian() * speedVar;

        // Spawn the smoke particles with wind effect
        spawnCustomParticles(worldIn, generatorTopPos, FHParticleTypes.STEAM.get(),
                x, y, z, vX, vY, vZ);
    }


    public static void spawnSteamParticles(World worldIn, BlockPos pos) {
        Random random = worldIn.getRandom();
        worldIn.addOptionalParticle(FHParticleTypes.STEAM.get(), true, pos.getX() + 0.5D + random.nextDouble() / 3.0D * (random.nextBoolean() ? 1 : -1), pos.getY() + 0.5D + random.nextDouble() + random.nextDouble(), pos.getZ() + 0.5D + random.nextDouble() / 3.0D * (random.nextBoolean() ? 1 : -1), 0.0D, 0.12D, 0.0D);
        worldIn.addParticle(FHParticleTypes.STEAM.get(), pos.getX() + 0.5D + random.nextDouble() / 2.0D * (random.nextBoolean() ? 1 : -1), pos.getY() + 0.4D, pos.getZ() + 0.5D + random.nextDouble() / 2.0D * (random.nextBoolean() ? 1 : -1), 0.0D, 0.05D, 0.0D);
    }

    public static void spawnT2FireParticles(World worldIn, BlockPos pos) {
        Random random = worldIn.getRandom();
        // Upward flame
        worldIn.addParticle(ParticleTypes.FLAME, pos.getX() + 0.5D + random.nextDouble() / 2.0D * (random.nextBoolean() ? 1 : -1), pos.getY() + 0.4D, pos.getZ() + 0.5D + random.nextDouble() / 2.0D * (random.nextBoolean() ? 1 : -1), 0.0D, 0.01D, 0.0D);
        // Side flame (4 directions)
        worldIn.addParticle(ParticleTypes.FLAME, pos.getX() + 0.5D + random.nextDouble() / 2.0D * (random.nextBoolean() ? 1 : -1), pos.getY() + 0.4D, pos.getZ() + 0.5D + random.nextDouble() / 2.0D * (random.nextBoolean() ? 1 : -1), 0.01D, 0D, 0.0D);
        worldIn.addParticle(ParticleTypes.FLAME, pos.getX() + 0.5D + random.nextDouble() / 2.0D * (random.nextBoolean() ? 1 : -1), pos.getY() + 0.4D, pos.getZ() + 0.5D + random.nextDouble() / 2.0D * (random.nextBoolean() ? 1 : -1), -0.01D, 0D, 0.0D);
        worldIn.addParticle(ParticleTypes.FLAME, pos.getX() + 0.5D + random.nextDouble() / 2.0D * (random.nextBoolean() ? 1 : -1), pos.getY() + 0.4D, pos.getZ() + 0.5D + random.nextDouble() / 2.0D * (random.nextBoolean() ? 1 : -1), 0.0D, 0D, 0.01D);
        worldIn.addParticle(ParticleTypes.FLAME, pos.getX() + 0.5D + random.nextDouble() / 2.0D * (random.nextBoolean() ? 1 : -1), pos.getY() + 0.4D, pos.getZ() + 0.5D + random.nextDouble() / 2.0D * (random.nextBoolean() ? 1 : -1), 0.0D, 0D, -0.01D);
    }

    public static int screenWidth() {
        return mc().getMainWindow().getScaledWidth();
    }

    public static int screenHeight() {
        return mc().getMainWindow().getScaledHeight();
    }

    public static ResourceLocation getDimLocation() {
        return mc().world.getDimensionKey().getLocation();
    }
}
