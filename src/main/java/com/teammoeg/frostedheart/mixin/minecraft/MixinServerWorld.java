package com.teammoeg.frostedheart.mixin.minecraft;

import java.util.function.Supplier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import net.minecraft.profiler.IProfiler;
import net.minecraft.util.RegistryKey;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.ISpawnWorldInfo;
@Mixin(ServerWorld.class)
public abstract class MixinServerWorld extends World {

	protected MixinServerWorld(ISpawnWorldInfo worldInfo, RegistryKey<World> dimension, DimensionType dimensionType,
			Supplier<IProfiler> profiler, boolean isRemote, boolean isDebug, long seed) {
		super(worldInfo, dimension, dimensionType, profiler, isRemote, isDebug, seed);
	}
    /**
     * @author khjxiaogu
     * @reason Not allow sleep over weather
     */
	@Overwrite
	private void resetRainAndThunder() {
		
	}

}
