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

package com.teammoeg.frostedheart.util;

import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.CapabilityManager;

import java.util.function.ToIntFunction;

public class FHUtils {
    public static <T> T notNull() {
        return null;
    }

    public static void registerSimpleCapability(Class<?> clazz) {
        CapabilityManager.INSTANCE.register(clazz, new NoopStorage<>(), () -> {
            throw new UnsupportedOperationException("Creating default instances is not supported. Why would you ever do this");
        });
    }

    public static World getWorld() {
        return Minecraft.getInstance().world;
    }

    public static ToIntFunction<BlockState> getLightValueLit(int lightValue) {
        return (state) -> {
            return state.get(BlockStateProperties.LIT) ? lightValue : 0;
        };
    }
}
