package com.teammoeg.frostedheart.content.world.features;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.LargeDripstoneConfiguration;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.Tags;

/** Vanilla large-dripstone geometry with ice material and bounded dry-cave substrates. */
public final class IceCaveLargeIceFeature extends Feature<LargeDripstoneConfiguration> {
    private static final int MAX_RADIUS = 6;

    public IceCaveLargeIceFeature(Codec<LargeDripstoneConfiguration> codec) { super(codec); }

    @Override public boolean place(FeaturePlaceContext<LargeDripstoneConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        if (!IceCaveIceBody.canRead(level, origin) || !level.getBlockState(origin).isAir()) return false;
        var config = context.config();
        int search = Math.min(48, config.floorToCeilingSearchRange);
        BlockPos floor = findEnd(level, origin, Direction.DOWN, search);
        BlockPos ceiling = findEnd(level, origin, Direction.UP, search);
        if (floor == null || ceiling == null) return false;
        int clearance = ceiling.getY() - floor.getY() - 1;
        if (clearance < 12 || !wideHall(level, floor.above())) return false;
        int minimum = Math.max(2, Math.min(MAX_RADIUS, config.columnRadius.getMinValue()));
        int maximum = Mth.clamp((int)(clearance * config.maxColumnRadiusToCaveHeightRatio), minimum,
                Math.max(minimum, Math.min(MAX_RADIUS, config.columnRadius.getMaxValue())));
        RandomSource random = context.random();
        int radius = Mth.randomBetweenInclusive(random, minimum, maximum);
        IceShape top = new IceShape(ceiling.below(), false, radius,
                config.stalactiteBluntness.sample(random), config.heightScale.sample(random));
        IceShape bottom = new IceShape(floor.above(), true, radius,
                config.stalagmiteBluntness.sample(random), config.heightScale.sample(random));
        Vec3 wind = Vec3.ZERO;
        if (radius >= config.minRadiusForWind && top.bluntness >= config.minBluntnessForWind
                && bottom.bluntness >= config.minBluntnessForWind) {
            float speed = config.windSpeed.sample(random);
            float angle = Mth.randomBetween(random, 0, (float)Math.PI);
            wind = new Vec3(Mth.cos(angle) * speed, 0, Mth.sin(angle) * speed);
        }
        boolean topFits = top.embed(level, origin, wind);
        boolean bottomFits = bottom.embed(level, origin, wind);
        if (!topFits && !bottomFits) return false;
        IceCaveIceBody body = new IceCaveIceBody(level, origin);
        if (topFits && !top.append(level, body, origin, wind, random)) return false;
        if (bottomFits && !bottom.append(level, body, origin, wind, random)) return false;
        return body.place();
    }

    private static BlockPos findEnd(WorldGenLevel level, BlockPos origin, Direction direction, int search) {
        for (int distance = 1; distance <= search; distance++) {
            BlockPos pos = origin.relative(direction, distance);
            if (!IceCaveIceBody.canRead(level, pos)) return null;
            BlockState state = level.getBlockState(pos);
            if (!state.isAir()) return isRootSupport(state) ? pos : null;
        }
        return null;
    }

    private static boolean isRootSupport(BlockState state) {
        return IceCaveIceBody.isBase(state) || state.is(Tags.Blocks.ORES);
    }

    private static boolean wideHall(WorldGenLevel level, BlockPos floor) {
        // Five-by-five clear space is stricter than the ordinary small-cave classification.
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) for (int dy = 1; dy <= 5; dy++) {
            BlockPos pos = floor.offset(dx, dy, dz);
            if (!IceCaveIceBody.canRead(level, pos) || !level.getBlockState(pos).isAir()) return false;
        }
        return true;
    }

    private static BlockPos windOffset(BlockPos pos, BlockPos origin, Vec3 wind) {
        int distance = origin.getY() - pos.getY();
        return pos.offset(Mth.floor(wind.x * distance), 0, Mth.floor(wind.z * distance));
    }

    private static final class IceShape {
        private BlockPos root;
        private final boolean up;
        private int radius;
        private final double bluntness;
        private final double scale;

        IceShape(BlockPos root, boolean up, int radius, double bluntness, double scale) {
            this.root = root;
            this.up = up;
            this.radius = radius;
            this.bluntness = bluntness;
            this.scale = scale;
        }

        private int height(float distance) {
            // DripstoneUtils.getDripstoneHeight, retained verbatim (the vanilla helper is protected).
            double radial = Math.max(distance, bluntness) / radius * 0.384;
            double profile = 0.75 * Math.pow(radial, 4.0 / 3.0) - Math.pow(radial, 2.0 / 3.0)
                    - (1.0 / 3.0) * Math.log(radial);
            return (int)(Math.max(scale * profile, 0) / 0.384 * radius);
        }

        boolean embed(WorldGenLevel level, BlockPos origin, Vec3 wind) {
            while (radius > 1) {
                for (int retreat = 0; retreat < Math.min(10, height(0)); retreat++) {
                    BlockPos candidate = root.relative(up ? Direction.DOWN : Direction.UP, retreat);
                    BlockPos shifted = windOffset(candidate, origin, wind);
                    boolean embedded = true;
                    // Check the full disk instead of only vanilla's six circumference samples.
                    for (int dx = -radius; dx <= radius && embedded; dx++) for (int dz = -radius; dz <= radius; dz++) {
                        if (dx * dx + dz * dz > radius * radius) continue;
                        BlockPos pos = shifted.offset(dx, 0, dz);
                        if (!IceCaveIceBody.inRange(origin, pos) || !IceCaveIceBody.canRead(level, pos)
                                || !isRootSupport(level.getBlockState(pos))) {
                            embedded = false;
                            break;
                        }
                    }
                    if (embedded) { root = candidate; return true; }
                }
                radius /= 2;
            }
            return false;
        }

        boolean append(WorldGenLevel level, IceCaveIceBody body, BlockPos origin, Vec3 wind, RandomSource random) {
            for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) {
                float radial = Mth.sqrt(dx * dx + dz * dz);
                if (radial > radius) continue;
                int height = height(radial);
                if (random.nextFloat() < 0.2F) height = (int)(height * Mth.randomBetween(random, 0.8F, 1));
                boolean reachedAir = false;
                for (int step = 0; step < height; step++) {
                    BlockPos pos = windOffset(root.offset(dx, up ? step : -step, dz), origin, wind);
                    if (!IceCaveIceBody.inRange(origin, pos) || !IceCaveIceBody.canRead(level, pos)) return false;
                    BlockState state = level.getBlockState(pos);
                    // Vanilla columns stop when they reach the opposite rock surface.
                    if (reachedAir && isRootSupport(state) && !IceCaveIceBody.isIce(state)) break;
                    // An ore inclusion in the embedded root is still solid support, never replaced.
                    if (state.is(Tags.Blocks.ORES)) continue;
                    reachedAir |= state.isAir();
                    if (!body.add(pos, Blocks.ICE.defaultBlockState())) return false;
                }
            }
            return true;
        }
    }
}
