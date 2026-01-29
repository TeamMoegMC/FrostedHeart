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

package com.teammoeg.chorda.client;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import org.joml.Quaternionf;

import java.time.Duration;
import java.util.function.Function;

/**
 * Rendering not related client functions, used for get/set client data, spawning particles
 * */
public class ClientUtils {
    public static float OverwriteGammaValue;
    public static boolean DoApplyGammaValue;
    private static long previousTick = 0;
	public static final Function<Direction, Quaternionf> DIR_TO_FACING = Util
	.memoize(dir -> new Quaternionf().rotateAxis(-(float) (dir.toYRot() / 180 * Math.PI), 0, 1, 0));

    public static Player getPlayer() {
        return getMc().player;
    }

    public static Level getWorld() {
        return getMc().level;
    }

    public static float partialTicks() {
    	return getMc().getFrameTime();
    }

    public static long gameTick() {
        return getWorld().getLevelData().getGameTime();
    }

    public static Minecraft getMc() {
        return Minecraft.getInstance();
    }
    public static Gui getGui() {
    	return getMc().gui;
    }
    public static LocalPlayer getLocalPlayer() {
        return getMc().player;
    }
    public static void spawnFireParticles2(Level worldIn, BlockPos pos) {
        RandomSource random = worldIn.getRandom();
        // Upward flame
        worldIn.addParticle(ParticleTypes.FLAME, pos.getX() + 0.25D + random.nextDouble() / 2.0D * (random.nextBoolean() ? 1 : -1), pos.getY() + 0.4D, pos.getZ() + 0.25D + random.nextDouble() / 2.0D * (random.nextBoolean() ? 1 : -1), 0.0D, 0.01D, 0.0D);
        // Side flame (4 directions)
        worldIn.addParticle(ParticleTypes.FLAME, pos.getX() + 0.25D + random.nextDouble() / 2, pos.getY() + 0.4D, pos.getZ() + 0.25D + random.nextDouble() / 2, 0.01D, 0D, 0.0D);
        worldIn.addParticle(ParticleTypes.FLAME, pos.getX() + 0.25D + random.nextDouble() / 2, pos.getY() + 0.4D, pos.getZ() + 0.25D + random.nextDouble() / 2, -0.01D, 0D, 0.0D);
        worldIn.addParticle(ParticleTypes.FLAME, pos.getX() + 0.25D + random.nextDouble() / 2, pos.getY() + 0.4D, pos.getZ() + 0.25D + random.nextDouble() / 2, 0.0D, 0D, 0.01D);
        worldIn.addParticle(ParticleTypes.FLAME, pos.getX() + 0.25D + random.nextDouble() / 2, pos.getY() + 0.4D, pos.getZ() + 0.25D + random.nextDouble() / 2, 0.0D, 0D, -0.01D);
    }
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
        return getMc().getWindow().getGuiScaledWidth();
    }

    public static int screenHeight() {
        return getMc().getWindow().getGuiScaledHeight();
    }

    public static int screenCenterX() {
        return screenWidth() / 2;
    }

    public static int screenCenterY() {
        return screenHeight() / 2;
    }

    public static ResourceLocation getDimLocation() {
        return getWorld().dimension().location();
    }

    public static String getBiomeName(Holder<Biome> biomeHolder) {
        if (biomeHolder == null) {
            return "null";
        }
        return biomeHolder.unwrap().map(
                biomeResourceKey -> biomeResourceKey.location().toString(),
                unregistered -> "[unregistered " + unregistered + "]");
    }

    public static Component msToTime(long milliseconds) {
        Duration duration = Duration.ofMillis(milliseconds);
        return secToTime(duration.getSeconds());
    }

    public static Component secToTime(long secondIn) {
        long years = secondIn / (365 * 24 * 60 * 60);
        long remainingSeconds = secondIn % (365 * 24 * 60 * 60);
        long days = remainingSeconds / (24 * 60 * 60);
        remainingSeconds %= (24 * 60 * 60);
        long hours = remainingSeconds / (60 * 60);
        remainingSeconds %= (60 * 60);
        long minutes = remainingSeconds / 60;
        long seconds = remainingSeconds % 60;

        var c = Component.empty();
        if (years > 100) {
            c = Component.translatable("gui.frostedheart.infinity");
        } else {
            if (years   != 0) c.append(Component.translatable("gui.frostedheart.year", years));
            if (days    != 0) c.append(Component.translatable("gui.frostedheart.day", days));
            if (hours   != 0) c.append(Component.translatable("gui.frostedheart.hour", hours));
            if (minutes != 0) c.append(Component.translatable("gui.frostedheart.minute", minutes));
            c.append(Component.translatable("gui.frostedheart.second", seconds));
        }
        return c;
    }

    public static boolean isGameTimeUpdated() {
        boolean flag = previousTick != gameTick();
        previousTick = gameTick();
        return flag;
    }

	public static Font font() {
	    return getMc().font;
	}

    public static void copyToClipboard(String content) {
        getMc().keyboardHandler.setClipboard(content);
    }
}
