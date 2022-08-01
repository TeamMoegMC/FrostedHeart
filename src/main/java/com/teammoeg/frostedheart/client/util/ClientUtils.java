/*
 * Copyright (c) 2021 TeamMoeg
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
 */

package com.teammoeg.frostedheart.client.util;

import com.teammoeg.frostedheart.client.particles.FHParticleTypes;
import com.teammoeg.frostedheart.research.gui.ResearchGui;
import dev.ftb.mods.ftblibrary.ui.BaseScreen;
import dev.ftb.mods.ftblibrary.ui.IScreenWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Random;

public class ClientUtils {
    public static float spgamma;
    public static boolean applyspg;

    public static Minecraft mc() {
        return Minecraft.getInstance();
    }

    public static World getWorld() {
        return Minecraft.getInstance().world;
    }

    public static PlayerEntity getPlayer() {
        return Minecraft.getInstance().player;
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

    public static void spawnSmokeParticles(World worldIn, BlockPos pos) {
        Random random = worldIn.getRandom();
        worldIn.addOptionalParticle(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, true, pos.getX() + 0.5D + random.nextDouble() / 3.0D * (random.nextBoolean() ? 1 : -1), pos.getY() + random.nextDouble() + random.nextDouble(), pos.getZ() + 0.5D + random.nextDouble() / 3.0D * (random.nextBoolean() ? 1 : -1), 0.0D, 0.05D, 0.0D);
        worldIn.addParticle(ParticleTypes.SMOKE, pos.getX() + 0.25D + random.nextDouble() / 2.0D * (random.nextBoolean() ? 1 : -1), pos.getY() + 0.4D, pos.getZ() + 0.25D + random.nextDouble() / 2.0D * (random.nextBoolean() ? 1 : -1), 0.002D, 0.01D, 0.0D);
    }

    public static void spawnSteamParticles(World worldIn, BlockPos pos) {
        Random random = worldIn.getRandom();
        worldIn.addOptionalParticle(FHParticleTypes.STEAM.get(), true, pos.getX() + 0.5D + random.nextDouble() / 3.0D * (random.nextBoolean() ? 1 : -1), pos.getY() + 0.5D + random.nextDouble() + random.nextDouble(), pos.getZ() + 0.5D + random.nextDouble() / 3.0D * (random.nextBoolean() ? 1 : -1), 0.0D, 0.12D, 0.0D);
        worldIn.addParticle(FHParticleTypes.STEAM.get(), pos.getX() + 0.5D + random.nextDouble() / 2.0D * (random.nextBoolean() ? 1 : -1), pos.getY() + 0.4D, pos.getZ() + 0.5D + random.nextDouble() / 2.0D * (random.nextBoolean() ? 1 : -1), 0.0D, 0.05D, 0.0D);
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
}
