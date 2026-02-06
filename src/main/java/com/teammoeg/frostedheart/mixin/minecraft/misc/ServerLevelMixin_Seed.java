/*
 * Copyright (c) 2026 TeamMoeg
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

package com.teammoeg.frostedheart.mixin.minecraft.misc;

import java.io.File;
import java.io.IOException;
import java.util.function.Supplier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.teammoeg.frostedheart.content.world.dimensionalseed.DimensionalSeed;
import com.teammoeg.frostedheart.util.mixin.SeedSetable;

import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.RandomSequences;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.StructureCheck;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.WritableLevelData;

/**
 * Add Per-world seed
 */
@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin_Seed extends Level implements SeedSetable {

	protected ServerLevelMixin_Seed(WritableLevelData pLevelData, ResourceKey<Level> pDimension,
		RegistryAccess pRegistryAccess, Holder<DimensionType> pDimensionTypeRegistration,
		Supplier<ProfilerFiller> pProfiler, boolean pIsClientSide, boolean pIsDebug, long pBiomeZoomSeed,
		int pMaxChainedNeighborUpdates) {
		super(pLevelData, pDimension, pRegistryAccess, pDimensionTypeRegistration, pProfiler, pIsClientSide, pIsDebug,
			pBiomeZoomSeed, pMaxChainedNeighborUpdates);
	}

	@Shadow
	private MinecraftServer server;
	@Shadow
	private StructureCheck structureCheck;

	@Shadow
	public abstract DimensionDataStorage getDataStorage();

	private static final LevelResource dataFolder = new LevelResource("chorda_data");
	private DimensionalSeed fh$seed;

	@ModifyVariable(at = @At(value = "STORE"), method = "<init>", ordinal = 1)
	public long modifySeed(long seed) {
		fh$initSeeds();
		if (fh$seed.hasSeed())
			return fh$seed.getSeed();
		return seed;
	}

	@Override
	public void setSeed(long newSeed) {
		try {
			fh$seed.setSeed(newSeed);
			ServerLevel level = (ServerLevel) (Object) this;
			biomeManager = new BiomeManager(this, BiomeManager.obfuscateSeed(newSeed));
			level.randomSequences = RandomSequences.load(newSeed, level.randomSequences.save(new CompoundTag()));
			this.getDataStorage().set("random_sequences", level.randomSequences);
			structureCheck.seed = newSeed;
			if (this.getChunkSource() instanceof ServerChunkCache source) {
				RegistryAccess registryaccess = server.registryAccess();
				long i = newSeed;
				if (source.chunkMap.generator instanceof NoiseBasedChunkGenerator noisebasedchunkgenerator) {
					source.chunkMap.randomState = RandomState.create(noisebasedchunkgenerator.generatorSettings().value(), registryaccess.lookupOrThrow(Registries.NOISE), i);
				} else {
					source.chunkMap.randomState = RandomState.create(NoiseGeneratorSettings.dummy(), registryaccess.lookupOrThrow(Registries.NOISE), i);
				}

				source.chunkMap.chunkGeneratorState = source.chunkMap.generator.createState(registryaccess.lookupOrThrow(Registries.STRUCTURE_SET), source.chunkMap.randomState, i);
			}

			File seeds = new File(this.server.getWorldPath(dataFolder).getParent().toFile(), "fh_seeds.dat");
			try {
				seeds.getParentFile().mkdirs();
				CompoundTag tag = new CompoundTag();
				if (seeds.exists())
					tag = NbtIo.readCompressed(seeds);
				tag.put(this.dimension().location().toString(),
					DimensionalSeed.CODEC.encodeStart(NbtOps.INSTANCE, fh$seed).result().orElseGet(CompoundTag::new));
				NbtIo.writeCompressed(tag, seeds);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void fh$initSeeds() {
		if (fh$seed == null) {
			File seeds = new File(this.server.getWorldPath(dataFolder).getParent().toFile(), "fh_seeds.dat");
			if (seeds.exists()) {
				try {
					CompoundTag tag = NbtIo.readCompressed(seeds);
					fh$seed = DimensionalSeed.CODEC
						.parse(NbtOps.INSTANCE, tag.getCompound(this.dimension().location().toString())).result()
						.orElse(null);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			if (fh$seed == null) {
				fh$seed = new DimensionalSeed();
			}
			fh$seed.tryInit(this, server.getWorldData().worldGenOptions().seed());
			if (fh$seed.hasSeed()) {
				biomeManager = new BiomeManager(this, BiomeManager.obfuscateSeed(fh$seed.getSeed()));

			}
		}
	}

	@Inject(at = @At("HEAD"), method = "getSeed", cancellable = true)
	public void fh$getSeed(CallbackInfoReturnable<Long> ret) {
		fh$initSeeds();
		if (fh$seed.hasSeed())
			ret.setReturnValue(fh$seed.getSeed());

	}
}
