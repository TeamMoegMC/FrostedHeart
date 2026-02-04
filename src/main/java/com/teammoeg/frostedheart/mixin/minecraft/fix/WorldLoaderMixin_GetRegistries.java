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

package com.teammoeg.frostedheart.mixin.minecraft.fix;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import com.mojang.datafixers.util.Pair;
import com.teammoeg.frostedheart.util.mixin.WorldGenDatapack;

import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.packs.resources.CloseableResourceManager;
import net.minecraft.world.level.WorldDataConfiguration;

@Mixin(WorldLoader.class)
public class WorldLoaderMixin_GetRegistries {
	@Inject(at=@At(value="INVOKE",target="Lnet/minecraft/server/WorldLoader$WorldDataSupplier;get(Lnet/minecraft/server/WorldLoader$DataLoadContext;)Lnet/minecraft/server/WorldLoader$DataLoadOutput;"),method="load",locals=LocalCapture.CAPTURE_FAILHARD)
	
	   private static <D, R> void fh$load(WorldLoader.InitConfig pInitConfig, WorldLoader.WorldDataSupplier<D> pWorldDataSupplier, WorldLoader.ResultFactory<D, R> pResultFactory, Executor pBackgroundExecutor, Executor p_214367_,CallbackInfoReturnable<CompletableFuture<R>> cbi,
		         Pair<WorldDataConfiguration, CloseableResourceManager> pair,
		         CloseableResourceManager closeableresourcemanager,
		         LayeredRegistryAccess<RegistryLayer> layeredregistryaccess,
		         LayeredRegistryAccess<RegistryLayer> layeredregistryaccess1) {
		WorldGenDatapack.registryaccess=layeredregistryaccess1;
		   
	   }
}
