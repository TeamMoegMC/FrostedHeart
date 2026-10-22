package com.teammoeg.frostedheart.content.world.features;

import com.mojang.serialization.Codec;
import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;
import net.minecraftforge.common.Tags;

/** One pass per chunk, including its edges: floors, high walls, ceilings and ordinary lake tops. */
public final class IceCaveSurfaceFeature extends Feature<NoneFeatureConfiguration> {
    public IceCaveSurfaceFeature(Codec<NoneFeatureConfiguration> codec) { super(codec); }

    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        int minX = context.origin().getX() & ~15;
        int minZ = context.origin().getZ() & ~15;
        SimplexNoise cover = new SimplexNoise(RandomSource.create(level.getSeed() ^ 0x1ceca7eL));
        SimplexNoise material = new SimplexNoise(RandomSource.create(level.getSeed() ^ 0x57a1beL));
        SimplexNoise lake = new SimplexNoise(RandomSource.create(level.getSeed() ^ 0x1a4e1ceL));
        Long2ObjectOpenHashMap<BlockState> changes = new Long2ObjectOpenHashMap<>();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = minX; x < minX + 16; x++) for (int z = minZ; z < minZ + 16; z++) {
            int top = Math.min(IceCaveTerrain.MAX_Y, level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 6);
            for (int y = Math.max(IceCaveTerrain.MIN_Y, level.getMinBuildHeight() + 1); y <= top; y++) {
                pos.set(x, y, z);
                BlockState state = level.getBlockState(pos);
                boolean water = state.is(Blocks.WATER) && level.getBlockState(pos.above()).isAir();
                boolean rock = IceCaveTerrain.stone(state);
                boolean air = state.isAir();
                if (!(water || rock || air) || !IceCaveTerrain.inBiome(level, pos)) continue;
                if (water) {
                    // The entire ordinary water surface is sealed, irrespective of the wall coverage mask.
                    double n = lake.getValue(x * .16, z * .16);
                    changes.put(pos.asLong(), n > .45
                            ? (n > .62 ? Blocks.PACKED_ICE : Blocks.ICE).defaultBlockState()
                            : FHBlocks.THIN_ICE_BLOCK.get().defaultBlockState());
                    continue;
                }
                double n = cover.getValue(x * .075, y * .075, z * .075);
                if (n > .42) continue;
                if (rock) {
                    boolean exposed = false;
                    for (Direction side : Direction.values()) {
                        if (level.getBlockState(pos.relative(side)).isAir()) { exposed = true; break; }
                    }
                    if (!exposed || IceCaveTerrain.springNearby(level, pos)) continue;
                    BlockState ice = material(material, x, z);
                    changes.put(pos.asLong(), ice);
                    // A second inward layer varies shell depth without filling narrow passage air or consuming ore.
                    if (n < -.35) for (Direction side : Direction.values()) {
                        BlockPos inner = pos.relative(side);
                        if ((inner.getX() & ~15) == minX && (inner.getZ() & ~15) == minZ
                                && IceCaveTerrain.stone(level.getBlockState(inner))
                                && IceCaveTerrain.inBiome(level, inner) && !IceCaveTerrain.springNearby(level, inner)) {
                            changes.put(inner.asLong(), ice);
                        }
                    }
                } else {
                    // Four original free cells leave two even when both opposite ore faces receive a shell.
                    for (Direction side : Direction.values()) {
                        if (level.getBlockState(pos.relative(side)).is(Tags.Blocks.ORES)
                                && level.getBlockState(pos.relative(side.getOpposite())).isAir()
                                && level.getBlockState(pos.relative(side.getOpposite(), 2)).isAir()
                                && level.getBlockState(pos.relative(side.getOpposite(), 3)).isAir()
                                && !IceCaveTerrain.springNearby(level, pos)) {
                            changes.put(pos.asLong(), material(material, x, z));
                            break;
                        }
                    }
                }
            }
        }
        // Read the original surface before writing, so the scan cannot grow its own next layer recursively.
        changes.long2ObjectEntrySet().forEach(entry -> level.setBlock(BlockPos.of(entry.getLongKey()), entry.getValue(), 2));
        return !changes.isEmpty();
    }

    private static BlockState material(SimplexNoise noise, int x, int z) {
        // A narrow 2-D noise interval makes sparse, generally vertical colour bands on all heights.
        double n = noise.getValue(x * .065, z * .065);
        if (n > .035 && n < .095) return Blocks.PACKED_ICE.defaultBlockState();
        if (n > -.095 && n < -.075) return Blocks.BLUE_ICE.defaultBlockState();
        return Blocks.ICE.defaultBlockState();
    }
}
