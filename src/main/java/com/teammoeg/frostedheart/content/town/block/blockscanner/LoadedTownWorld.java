package com.teammoeg.frostedheart.content.town.block.blockscanner;

import com.teammoeg.frostedheart.bootstrap.reference.FHTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** A single main-thread scan. Missing chunks invalidate the candidate instead of loading terrain. */
final class LoadedTownWorld implements LevelReader, AbstractWorld {
    private final ServerLevel level;
    private final BiomeManager biomeManager;
    private final Map<Long, LevelChunk> chunks = new HashMap<>();
    private boolean complete = true;

    LoadedTownWorld(ServerLevel level) {
        this.level = level;
        biomeManager = level.getBiomeManager().withDifferentSource(this);
    }

    boolean hasCompleteData() {
        return complete;
    }

    private LevelChunk loadedChunk(int x, int z) {
        long key = net.minecraft.world.level.ChunkPos.asLong(x, z);
        if (!chunks.containsKey(key)) {
            chunks.put(key, level.getChunkSource().getChunkNow(x, z));
        }
        LevelChunk chunk = chunks.get(key);
        if (chunk == null) {
            complete = false;
        }
        return chunk;
    }

    @Override
    public BlockState getBlockState(BlockPos position) {
        if (position.getY() < getMinBuildHeight() || position.getY() >= getMaxBuildHeight()) {
            return Blocks.BEDROCK.defaultBlockState();
        }
        LevelChunk chunk = loadedChunk(position.getX() >> 4, position.getZ() >> 4);
        return chunk == null ? Blocks.BARRIER.defaultBlockState() : chunk.getBlockState(position);
    }

    @Override
    public BlockType getBlockType(BlockPos position) {
        BlockState state = getBlockState(position);
        if (state.isAir()) {
            return BlockType.AIR;
        }
        if (state.isLadder(this, position, null)) {
            return BlockType.LADDER;
        }
        if (state.is(FHTags.Blocks.TOWN_FURNITURE.get())) {
            return BlockType.DECO;
        }
        VoxelShape shape = state.getCollisionShape(this, position);
        if (shape.isEmpty()) {
            return BlockType.AIR;
        }
        AABB bounds = shape.bounds();
        boolean narrow = bounds.getXsize() < 0.3 && bounds.getYsize() < 0.3 && bounds.getZsize() < 0.3;
        return narrow ? BlockType.AIR : BlockType.WALL;
    }

    @Override
    public int getMaxY(BlockPos position) {
        return Math.min(getMaxBuildHeight() - 1, getHeight(Heightmap.Types.WORLD_SURFACE, position.getX(), position.getZ()));
    }

    @Override
    public int getHeight(Heightmap.Types type, int x, int z) {
        LevelChunk chunk = loadedChunk(x >> 4, z >> 4);
        return chunk == null ? getMinBuildHeight() : chunk.getHeight(type, x & 15, z & 15) + 1;
    }

    @Override
    public BlockEntity getBlockEntity(BlockPos position) {
        LevelChunk chunk = loadedChunk(position.getX() >> 4, position.getZ() >> 4);
        return chunk == null ? null : chunk.getBlockEntity(position);
    }

    @Override
    public FluidState getFluidState(BlockPos position) {
        return getBlockState(position).getFluidState();
    }

    @Override
    public ChunkAccess getChunk(int x, int z, ChunkStatus status, boolean create) {
        return loadedChunk(x, z);
    }

    @Override
    public boolean hasChunk(int x, int z) {
        return loadedChunk(x, z) != null;
    }
    @Override
    public int getHeight() {
        return level.getHeight();
    }
    @Override
    public int getMinBuildHeight() {
        return level.getMinBuildHeight();
    }
    @Override
    public float getShade(net.minecraft.core.Direction direction, boolean shade) {
        return 1.0F;
    }
    @Override
    public LevelLightEngine getLightEngine() {
        return level.getLightEngine();
    }
    @Override
    public WorldBorder getWorldBorder() {
        return level.getWorldBorder();
    }
    @Override
    public List<VoxelShape> getEntityCollisions(Entity entity, AABB bounds) {
        return List.of();
    }
    @Override
    public int getSkyDarken() {
        return level.getSkyDarken();
    }
    @Override
    public BiomeManager getBiomeManager() {
        return biomeManager;
    }
    @Override
    public boolean isClientSide() {
        return false;
    }
    @Override
    public int getSeaLevel() {
        return level.getSeaLevel();
    }
    @Override
    public DimensionType dimensionType() {
        return level.dimensionType();
    }
    @Override
    public RegistryAccess registryAccess() {
        return level.registryAccess();
    }
    @Override
    public FeatureFlagSet enabledFeatures() {
        return level.enabledFeatures();
    }

    @Override
    public Holder<Biome> getUncachedNoiseBiome(int x, int y, int z) {
        LevelChunk chunk = loadedChunk(x >> 2, z >> 2);
        if (chunk != null) {
            return chunk.getNoiseBiome(x, y, z);
        }
        return registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.PLAINS);
    }
}
