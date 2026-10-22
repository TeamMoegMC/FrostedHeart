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

/** Vanilla ice-spike taper and roots, reduced to a three-to-six-block underground spike. */
public final class IceCaveSpikeFeature extends Feature<NoneFeatureConfiguration> {
    public IceCaveSpikeFeature(Codec<NoneFeatureConfiguration> codec) { super(codec); }

    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos floor = IceCaveIceBody.findFloor(level, context.origin(), 12);
        if (floor == null || !IceCaveSpace.isWide(level, floor)) return false;
        RandomSource random = context.random();
        int height = 3 + random.nextInt(4);
        int radius = Math.max(1, height / 4 + random.nextInt(2));
        IceCaveIceBody body = new IceCaveIceBody(level, context.origin());

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
                if ((!old.isAir() && !IceCaveIceBody.isIce(old))
                        || !body.add(above, Blocks.PACKED_ICE.defaultBlockState())) return false;
                if (y != 0 && extent > 1
                        && !body.add(floor.offset(dx, -y, dz), Blocks.PACKED_ICE.defaultBlockState())) return false;
            }
        }

        // Replace the surface-only Y>50 root walk by a short embedded root at the actual cave floor.
        int roots = Mth.clamp(radius - 1, 0, 1);
        for (int dx = -roots; dx <= roots; dx++) for (int dz = -roots; dz <= roots; dz++) {
            int depth = Math.abs(dx) == 1 && Math.abs(dz) == 1 ? random.nextInt(3) : 1 + random.nextInt(3);
            for (int y = 1; y <= depth; y++) {
                BlockPos root = floor.offset(dx, -y, dz);
                if (!IceCaveIceBody.canRead(level, root)
                        || !IceCaveIceBody.isBase(level.getBlockState(root))) return false;
                if (!body.add(root, Blocks.PACKED_ICE.defaultBlockState())) return false;
            }
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
