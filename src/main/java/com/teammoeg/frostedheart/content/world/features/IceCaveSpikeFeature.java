package com.teammoeg.frostedheart.content.world.features;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.common.Tags;

/** Vanilla ice-spike taper and roots, reduced to a three-to-six-block underground spike. */
public final class IceCaveSpikeFeature extends Feature<NoneFeatureConfiguration> {
    public IceCaveSpikeFeature(Codec<NoneFeatureConfiguration> codec) { super(codec); }

    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        // A rare candidate may try five nearby columns, but can place only one small spike.
        for (int[] offset : new int[][] {{0, 0}, {-2, 0}, {2, 0}, {0, -2}, {0, 2}}) {
            BlockPos anchor = context.origin().offset(offset[0], 0, offset[1]);
            BlockPos floor = findFloor(level, anchor);
            if (floor != null && IceCaveSpace.isWide(level, floor.above())
                    && tryPlace(level, context.origin(), floor, context.random())) return true;
        }
        return false;
    }

    private static BlockPos findFloor(WorldGenLevel level, BlockPos origin) {
        for (int distance = 0; distance <= 24; distance++) {
            for (int sign : distance == 0 ? new int[] {1} : new int[] {-1, 1}) {
                BlockPos air = origin.above(distance * sign);
                if (IceCaveIceBody.canRead(level, air.below()) && IceCaveIceBody.canRead(level, air.above())
                        && level.getBlockState(air).isAir() && level.getBlockState(air.above()).isAir()
                        && (IceCaveIceBody.isBase(level.getBlockState(air.below()))
                            || level.getBlockState(air.below()).is(Tags.Blocks.ORES))) return air;
            }
        }
        return null;
    }

    static boolean tryPlace(WorldGenLevel level, BlockPos origin, BlockPos floor, RandomSource random) {
        int height = 3 + random.nextInt(4);
        int radius = Math.max(1, height / 4 + random.nextInt(2));
        IceCaveIceBody body = new IceCaveIceBody(level, origin);

        // The vanilla edge rounding, per-layer taper, random perimeter and lower mirror are retained.
        for (int y = 0; y < height; y++) {
            float taper = (1.0F - (float)y / height) * radius;
            int extent = Mth.ceil(taper);
            for (int dx = -extent; dx <= extent; dx++) for (int dz = -extent; dz <= extent; dz++) {
                float x = Mth.abs(dx) - 0.25F;
                float z = Mth.abs(dz) - 0.25F;
                if ((dx != 0 || dz != 0) && x * x + z * z > taper * taper) continue;
                if ((dx == -extent || dx == extent || dz == -extent || dz == extent)
                        && random.nextFloat() > 0.75F) continue;
                BlockPos above = floor.offset(dx, y, dz);
                if (!IceCaveIceBody.canRead(level, above)) return false;
                var old = level.getBlockState(above);
                if (!(y <= 1 && old.is(Tags.Blocks.ORES))) {
                    if ((!old.isAir() && !IceCaveIceBody.isIce(old) && !(y <= 1 && IceCaveIceBody.isBase(old)))
                            || !body.add(above, Blocks.PACKED_ICE.defaultBlockState())) return false;
                }
                if (y != 0 && extent > 1) {
                    BlockPos mirrored = floor.offset(dx, -y, dz);
                    if (!IceCaveIceBody.canRead(level, mirrored)) return false;
                    if (!level.getBlockState(mirrored).is(Tags.Blocks.ORES)
                            && !body.add(mirrored, Blocks.PACKED_ICE.defaultBlockState())) return false;
                }
            }
        }

        // Replace the surface-only Y>50 root walk by a short embedded root at the actual cave floor.
        int roots = Mth.clamp(radius - 1, 0, 1);
        for (int dx = -roots; dx <= roots; dx++) for (int dz = -roots; dz <= roots; dz++) {
            int depth = Math.abs(dx) == 1 && Math.abs(dz) == 1 ? random.nextInt(3) : 1 + random.nextInt(3);
            boolean anchored = false;
            for (int y = 1; y <= 3; y++) {
                BlockPos root = floor.offset(dx, -y, dz);
                if (!IceCaveIceBody.canRead(level, root)) return false;
                var old = level.getBlockState(root);
                boolean ore = old.is(Tags.Blocks.ORES);
                if (IceCaveIceBody.isBase(old) || ore) anchored = true;
                else if (!(y == 1 && old.isAir())) return false;
                if (!ore && !body.add(root, Blocks.PACKED_ICE.defaultBlockState())) return false;
                if (anchored && y >= depth) break;
            }
            if (!anchored) return false;
        }
        // A few snowy cells at the foot, never a broad snow carpet or a raised starting platform.
        for (var direction : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            if (!random.nextBoolean()) continue;
            BlockPos base = floor.relative(direction, radius).below();
            if (!IceCaveIceBody.canRead(level, base) || !IceCaveIceBody.isBase(level.getBlockState(base))) continue;
            if (!body.add(base, Blocks.SNOW_BLOCK.defaultBlockState())) return false;
        }
        return body.place();
    }
}
