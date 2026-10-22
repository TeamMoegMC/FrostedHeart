package com.teammoeg.frostedheart.content.world.features;

import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.List;
import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.common.Tags;

/** A coherent five-to-ten-block ice lobe, embedded in an exposed cave wall. */
public final class IceCaveIceBlobFeature extends Feature<NoneFeatureConfiguration> {
    public IceCaveIceBlobFeature(Codec<NoneFeatureConfiguration> codec) { super(codec); }

    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        if (!IceCaveIceBody.canRead(level, origin) || !level.getBlockState(origin).isAir()) return false;
        RandomSource random = context.random();
        Direction direction = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        BlockPos wall = null;
        for (int side = 0; side < 4 && wall == null; side++, direction = direction.getClockWise()) {
            for (int distance = 1; distance <= 6; distance++) {
                BlockPos candidate = origin.relative(direction, distance);
                if (!IceCaveIceBody.canRead(level, candidate)) break;
                BlockState state = level.getBlockState(candidate);
                if (!state.isAir()) {
                    if (IceCaveIceBody.isBase(state) || state.is(Tags.Blocks.ORES)) wall = candidate;
                    break;
                }
            }
            if (wall != null) break;
        }
        if (wall == null) return false;

        int diameter = 5 + random.nextInt(6);
        double radius = diameter / 2.0;
        // Offset into rock so the lobe is embedded, with only its outer cap exposed to the cave.
        BlockPos center = wall.relative(direction, Math.max(1, diameter / 4));
        double halfCell = diameter % 2 == 0 ? 0.5 : 0;
        double cx = center.getX() + halfCell;
        double cy = center.getY() + halfCell;
        double cz = center.getZ() + halfCell;
        int extent = diameter / 2;
        BlockState material = chooseMaterial(random);
        IceCaveIceBody body = new IceCaveIceBody(level, origin);
        List<BlockPos> iceCells = new ArrayList<>();
        int embedded = 0;
        int exposed = 0;
        for (int dx = -extent; dx <= extent; dx++) for (int dy = -extent; dy <= extent; dy++) for (int dz = -extent; dz <= extent; dz++) {
            BlockPos pos = center.offset(dx, dy, dz);
            double x = pos.getX() - cx;
            double y = pos.getY() - cy;
            double z = pos.getZ() - cz;
            if (x * x + y * y + z * z > radius * radius) continue;
            if (!IceCaveIceBody.canRead(level, pos)) return false;
            BlockState old = level.getBlockState(pos);
            if (old.isAir()) exposed++;
            else if (old.is(Tags.Blocks.ORES)) {
                // Keep mineral inclusions, including the occasional cave-facing mineral fleck.
                embedded++;
                continue;
            }
            else if (IceCaveIceBody.isBase(old)) embedded++;
            if (!body.add(pos, material)) return false;
            iceCells.add(pos);
        }
        // Reject buried balls and freestanding or nearly unsupported spheres.
        if (exposed == 0 || embedded < exposed || !body.place()) return false;
        // Only real wall blobs receive this extra pass. Combined with the reduced 1/360
        // background, their eligible interiors have about 1.5x the former 1/180 attempts.
        for (BlockPos pos : iceCells) {
            if (random.nextInt(180) == 0) IceCaveDecorationFeature.placeCrystalPocket(level, pos);
        }
        return true;
    }

    private static BlockState chooseMaterial(RandomSource random) {
        if (random.nextBoolean()) return Blocks.ICE.defaultBlockState();
        // Per-lobe family choice: 50% ordinary ice; 25% packed, 12.5% blue, 12.5% FH firm ice.
        return switch (random.nextInt(4)) {
            case 0, 1 -> Blocks.PACKED_ICE.defaultBlockState();
            case 2 -> Blocks.BLUE_ICE.defaultBlockState();
            default -> FHBlocks.FIRM_ICE_BLOCK.get().defaultBlockState();
        };
    }
}
