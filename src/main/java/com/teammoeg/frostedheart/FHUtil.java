package com.teammoeg.frostedheart;

import com.teammoeg.frostedheart.common.util.NoopStorage;
import net.minecraft.client.Minecraft;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.CapabilityManager;

public class FHUtil {
    public static <T> T notNull()
    {
        return null;
    }
    public static void registerSimpleCapability(Class<?> clazz)
    {
        CapabilityManager.INSTANCE.register(clazz, new NoopStorage<>(), () -> {
            throw new UnsupportedOperationException("Creating default instances is not supported. Why would you ever do this");
        });
    }

    public static World getWorld()
    {
        return Minecraft.getInstance().world;
    }
}
