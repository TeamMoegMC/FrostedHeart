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
