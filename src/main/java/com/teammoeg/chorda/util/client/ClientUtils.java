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

package com.teammoeg.chorda.util.client;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.Size2i;

/**
 * Rendering not related client functions, used for get/set client data, spawning particles
 * */
public class ClientUtils {
    public static float spgamma;
    public static boolean applyspg;

    public static LocalPlayer getPlayer() {
        return mc().player;
    }

    public static Level getWorld() {
        return mc().level;
    }
    public static float partialTicks() {
    	return mc().getFrameTime();
    }
    public static long gameTick() {
        return getWorld().getLevelData().getGameTime();
    }
    public static Minecraft mc() {
        return Minecraft.getInstance();
    }
    public static Font font() {
        return mc().font;
    }
    public static Size2i screenSize() {return new Size2i(mc().getWindow().getGuiScaledWidth(), mc().getWindow().getGuiScaledHeight());}

    public static void spawnFireParticles(Level worldIn, BlockPos pos) {
        RandomSource random = worldIn.getRandom();
        // Upward flame
        worldIn.addParticle(ParticleTypes.FLAME, pos.getX() + 0.25D + random.nextDouble() / 2.0D * (random.nextBoolean() ? 1 : -1), pos.getY() + 0.4D, pos.getZ() + 0.25D + random.nextDouble() / 2.0D * (random.nextBoolean() ? 1 : -1), 0.0D, 0.01D, 0.0D);
        // Side flame (4 directions)
        worldIn.addParticle(ParticleTypes.FLAME, pos.getX() + 0.25D + random.nextDouble() / 2.0D * (random.nextBoolean() ? 1 : -1), pos.getY() + 0.4D, pos.getZ() + 0.25D + random.nextDouble() / 2.0D * (random.nextBoolean() ? 1 : -1), 0.01D, 0D, 0.0D);
        worldIn.addParticle(ParticleTypes.FLAME, pos.getX() + 0.25D + random.nextDouble() / 2.0D * (random.nextBoolean() ? 1 : -1), pos.getY() + 0.4D, pos.getZ() + 0.25D + random.nextDouble() / 2.0D * (random.nextBoolean() ? 1 : -1), -0.01D, 0D, 0.0D);
        worldIn.addParticle(ParticleTypes.FLAME, pos.getX() + 0.25D + random.nextDouble() / 2.0D * (random.nextBoolean() ? 1 : -1), pos.getY() + 0.4D, pos.getZ() + 0.25D + random.nextDouble() / 2.0D * (random.nextBoolean() ? 1 : -1), 0.0D, 0D, 0.01D);
        worldIn.addParticle(ParticleTypes.FLAME, pos.getX() + 0.25D + random.nextDouble() / 2.0D * (random.nextBoolean() ? 1 : -1), pos.getY() + 0.4D, pos.getZ() + 0.25D + random.nextDouble() / 2.0D * (random.nextBoolean() ? 1 : -1), 0.0D, 0D, -0.01D);
    }

    public static void spawnSmokeParticles(Level worldIn, BlockPos pos) {
        RandomSource random = worldIn.getRandom();
        worldIn.addAlwaysVisibleParticle(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, true, pos.getX() + 0.5D + random.nextDouble() / 3.0D * (random.nextBoolean() ? 1 : -1), pos.getY() + random.nextDouble() + random.nextDouble(), pos.getZ() + 0.5D + random.nextDouble() / 3.0D * (random.nextBoolean() ? 1 : -1), 0.0D, 0.05D, 0.0D);
        worldIn.addParticle(ParticleTypes.SMOKE, pos.getX() + 0.25D + random.nextDouble() / 2.0D * (random.nextBoolean() ? 1 : -1), pos.getY() + 0.4D, pos.getZ() + 0.25D + random.nextDouble() / 2.0D * (random.nextBoolean() ? 1 : -1), 0.002D, 0.01D, 0.0D);
    }

    public static void spawnT2SmokeParticles(Level worldIn, BlockPos pos) {
        RandomSource random = worldIn.getRandom();
        worldIn.addAlwaysVisibleParticle(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, true, pos.getX() + 0.5D + random.nextDouble() / 3.0D * (random.nextBoolean() ? 1 : -1), pos.getY() + 0.5D + random.nextDouble() + random.nextDouble(), pos.getZ() + 0.5D + random.nextDouble() / 3.0D * (random.nextBoolean() ? 1 : -1), 0.0D, 0.12D, 0.0D);
        worldIn.addParticle(ParticleTypes.SMOKE, pos.getX() + 0.5D + random.nextDouble() / 2.0D * (random.nextBoolean() ? 1 : -1), pos.getY() + 0.4D, pos.getZ() + 0.5D + random.nextDouble() / 2.0D * (random.nextBoolean() ? 1 : -1), 0.0D, 0.05D, 0.0D);
    }

    public static void spawnCustomParticles(Level worldIn, BlockPos pos, ParticleOptions particle, double posX, double posY, double posZ, double velX, double velY, double velZ) {
        // Spawn the specified particle type without randomness in position and velocity
        worldIn.addAlwaysVisibleParticle(particle, true, pos.getX() + posX, pos.getY() + posY, pos.getZ() + posZ, velX, velY, velZ);
        worldIn.addParticle(particle, pos.getX() + posX, pos.getY() + posY, pos.getZ() + posZ, velX, velY, velZ);
    }


    public static void spawnT2FireParticles(Level worldIn, BlockPos pos) {
        RandomSource random = worldIn.getRandom();
        // Upward flame
        worldIn.addParticle(ParticleTypes.FLAME, pos.getX() + 0.5D + random.nextDouble() / 2.0D * (random.nextBoolean() ? 1 : -1), pos.getY() + 0.4D, pos.getZ() + 0.5D + random.nextDouble() / 2.0D * (random.nextBoolean() ? 1 : -1), 0.0D, 0.01D, 0.0D);
        // Side flame (4 directions)
        worldIn.addParticle(ParticleTypes.FLAME, pos.getX() + 0.5D + random.nextDouble() / 2.0D * (random.nextBoolean() ? 1 : -1), pos.getY() + 0.4D, pos.getZ() + 0.5D + random.nextDouble() / 2.0D * (random.nextBoolean() ? 1 : -1), 0.01D, 0D, 0.0D);
        worldIn.addParticle(ParticleTypes.FLAME, pos.getX() + 0.5D + random.nextDouble() / 2.0D * (random.nextBoolean() ? 1 : -1), pos.getY() + 0.4D, pos.getZ() + 0.5D + random.nextDouble() / 2.0D * (random.nextBoolean() ? 1 : -1), -0.01D, 0D, 0.0D);
        worldIn.addParticle(ParticleTypes.FLAME, pos.getX() + 0.5D + random.nextDouble() / 2.0D * (random.nextBoolean() ? 1 : -1), pos.getY() + 0.4D, pos.getZ() + 0.5D + random.nextDouble() / 2.0D * (random.nextBoolean() ? 1 : -1), 0.0D, 0D, 0.01D);
        worldIn.addParticle(ParticleTypes.FLAME, pos.getX() + 0.5D + random.nextDouble() / 2.0D * (random.nextBoolean() ? 1 : -1), pos.getY() + 0.4D, pos.getZ() + 0.5D + random.nextDouble() / 2.0D * (random.nextBoolean() ? 1 : -1), 0.0D, 0D, -0.01D);
    }

    public static int screenWidth() {
        return mc().getWindow().getGuiScaledWidth();
    }

    public static int screenHeight() {
        return mc().getWindow().getGuiScaledHeight();
    }

    public static boolean isKeyDown(int key) {
        return InputConstants.isKeyDown(mc().getWindow().getWindow(), key);
    }

    public static ResourceLocation getDimLocation() {
        return getWorld().dimension().location();
    }

    private static long previousTick = 0;
    public static boolean isGameTimeUpdated() {
        boolean flag = previousTick != gameTick();
        previousTick = gameTick();
        return flag;
    }

}
