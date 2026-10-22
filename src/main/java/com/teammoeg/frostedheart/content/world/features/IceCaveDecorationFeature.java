package com.teammoeg.frostedheart.content.world.features;

import com.mojang.serialization.Codec;
import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Runs after the ice bodies: supported crystal pockets and ice-preferring functional ceiling icicles. */
public final class IceCaveDecorationFeature extends Feature<NoneFeatureConfiguration> {
    public IceCaveDecorationFeature(Codec<NoneFeatureConfiguration> codec) { super(codec); }

    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        int minX = context.origin().getX() & ~15, minZ = context.origin().getZ() & ~15;
        RandomSource random = context.random();
        boolean changed = false;
        for (int x = minX; x < minX + 16; x++) for (int z = minZ; z < minZ + 16; z++) {
            int top = Math.min(IceCaveTerrain.MAX_Y, level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 6);
            for (int y = Math.max(IceCaveTerrain.MIN_Y, level.getMinBuildHeight() + 1); y <= top; y++) {
                BlockPos pos = new BlockPos(x, y, z);
                var state = level.getBlockState(pos);
                if (!IceCaveTerrain.inBiome(level, pos)) continue;
                if (IceCaveTerrain.ice(state) && random.nextInt(360) == 0) {
                    changed |= placeCrystalPocket(level, pos);
                } else if (state.isAir()) {
                    if (random.nextInt(360) == 0 && IceCaveSpace.isWide(level, pos)
                            && !IceCaveTerrain.springNearby(level, pos)) {
                        // Exposed inlays keep some crystals visible even when their parent body is opaque ice.
                        boolean placed = false;
                        for (Direction side : Direction.values()) {
                            BlockPos supportPos = pos.relative(side);
                            if ((supportPos.getX() & ~15) != minX || (supportPos.getZ() & ~15) != minZ
                                    || !IceCaveTerrain.inBiome(level, supportPos)
                                    || !IceCaveTerrain.ice(level.getBlockState(supportPos))
                                    || !IceCaveTerrain.ice(level.getBlockState(supportPos.relative(side)))) continue;
                            var crystal = FHBlocks.ICE_CRYSTAL.get().defaultBlockState()
                                    .setValue(AmethystClusterBlock.FACING, side.getOpposite());
                            if (!crystal.canSurvive(level, pos)) continue;
                            level.setBlock(supportPos, FHBlocks.GLOWSTONE_CRYSTAL.get().defaultBlockState(), 2);
                            level.setBlock(pos, crystal, 2);
                            changed = placed = true;
                            break;
                        }
                        if (placed) continue;
                    }
                    var support = level.getBlockState(pos.above());
                    boolean ice = IceCaveTerrain.ice(support);
                    if ((ice || IceCaveTerrain.stone(support)) && random.nextFloat() < (ice ? .42F : .09F)) {
                        changed |= IcicleFeature.placeCaveIcicle(level, pos, random);
                    }
                }
            }
        }
        return changed;
    }

    static boolean placeCrystalPocket(WorldGenLevel level, BlockPos pos) {
        var state = level.getBlockState(pos);
        if (!IceCaveTerrain.ice(state) || !level.ensureCanWrite(pos) || !IceCaveTerrain.inBiome(level, pos)
                || !inThickIce(level, pos) || IceCaveTerrain.springNearby(level, pos)) return false;
        for (Direction side : Direction.values()) {
            BlockPos support = pos.relative(side);
            if ((support.getX() >> 4) != (pos.getX() >> 4) || (support.getZ() >> 4) != (pos.getZ() >> 4)
                    || !level.ensureCanWrite(support) || !IceCaveTerrain.inBiome(level, support)
                    || IceCaveTerrain.springNearby(level, support)) continue;
            var crystal = FHBlocks.ICE_CRYSTAL.get().defaultBlockState()
                    .setValue(AmethystClusterBlock.FACING, side.getOpposite());
            var oldSupport = level.getBlockState(support);
            level.setBlock(support, FHBlocks.GLOWSTONE_CRYSTAL.get().defaultBlockState(), 2);
            level.setBlock(pos, Blocks.CAVE_AIR.defaultBlockState(), 2);
            if (crystal.canSurvive(level, pos)) {
                level.setBlock(pos, crystal, 2);
                return true;
            }
            level.setBlock(pos, state, 2);
            level.setBlock(support, oldSupport, 2);
        }
        return false;
    }

    private static boolean inThickIce(WorldGenLevel level, BlockPos pos) {
        // Do not puncture thin lake lids, floor ice, or the retaining skin of a dry trap.
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = pos.relative(direction);
            if (!IceCaveIceBody.canRead(level, neighbor) || !IceCaveTerrain.ice(level.getBlockState(neighbor))) return false;
        }
        return true;
    }
}
