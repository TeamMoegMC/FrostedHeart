/*
 * Copyright (c) 2022-2024 TeamMoeg
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

package com.teammoeg.frostedheart.world.civilization.federation.observatory;

import java.util.Optional;
import java.util.function.Predicate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

public class ObservatoryStructure extends Structure {
	 public static final Codec<ObservatoryStructure> CODEC = RecordCodecBuilder.<ObservatoryStructure>mapCodec((p_227640_) -> {
	      return p_227640_.group(settingsCodec(p_227640_), StructureTemplatePool.CODEC.fieldOf("start_pool").forGetter((p_227656_) -> {
	         return p_227656_.startPool;
	      })).apply(p_227640_, ObservatoryStructure::new);
	   }).codec();
	public final Holder<StructureTemplatePool> startPool;
    public ObservatoryStructure(StructureSettings p_226558_,Holder<StructureTemplatePool> pool) {
		super(p_226558_);
		startPool=pool;
	}


    @Override
    public GenerationStep.Decoration step() {
        return GenerationStep.Decoration.SURFACE_STRUCTURES;
    }

	@Override
	protected Optional<GenerationStub> findGenerationPoint(GenerationContext pContext) {
		 BlockPos centerOfChunk = new BlockPos(pContext.chunkPos().getMiddleBlockX(), 0, pContext.chunkPos().getMiddleBlockZ());
		 
	        int landHeight = pContext.chunkGenerator().getFirstFreeHeight(centerOfChunk.getX(), centerOfChunk.getZ(), Heightmap.Types.WORLD_SURFACE_WG, pContext.heightAccessor(), pContext.randomState());
	        if (landHeight < 100 || landHeight > 200) return Optional.empty();
	        NoiseColumn columnOfBlocks = pContext.chunkGenerator().getBaseColumn(centerOfChunk.getX(), centerOfChunk.getZ(), pContext.heightAccessor(), pContext.randomState());
	        BlockPos pos=centerOfChunk.above(landHeight);
	        BlockState topBlock = columnOfBlocks.getBlock(pos.getY());
	        if(topBlock.getFluidState().isEmpty())
	        	return JigsawPlacement.addPieces(pContext, this.startPool,Optional.empty(), 32, pos,false, Optional.empty(), 0);
		return Optional.empty();
	}

	@Override
	public StructureType<?> type() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public StructureStart generate(RegistryAccess pRegistryAccess, ChunkGenerator pChunkGenerator, BiomeSource pBiomeSource, RandomState pRandomState,
		StructureTemplateManager pStructureTemplateManager, long pSeed, ChunkPos pChunkPos, int p_226604_, LevelHeightAccessor pHeightAccessor, Predicate<Holder<Biome>> pValidBiome) {
		// TODO Auto-generated method stub
		return super.generate(pRegistryAccess, pChunkGenerator, pBiomeSource, pRandomState, pStructureTemplateManager, pSeed, pChunkPos, p_226604_, pHeightAccessor, pValidBiome);
	}



}
