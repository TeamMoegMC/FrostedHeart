package com.teammoeg.frostedheart.content.world.features;

import java.util.LinkedHashMap;
import java.util.Map;

import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.bootstrap.common.FHFluids;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.Tags;

/** A bounded, fully checked body, committed only after its entire footprint is known. */
final class IceCaveIceBody {
    private static final ResourceKey<Biome> ICE_CAVES = ResourceKey.create(Registries.BIOME,
            new ResourceLocation("the_winter_rescue", "ice_caves"));
    private final WorldGenLevel level;
    private final BlockPos origin;
    private final Map<BlockPos, BlockState> blocks = new LinkedHashMap<>();

    IceCaveIceBody(WorldGenLevel level, BlockPos origin) {
        this.level = level;
        this.origin = origin;
    }

    static boolean canRead(WorldGenLevel level, BlockPos pos) {
        return !level.isOutsideBuildHeight(pos) && level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4);
    }

    static boolean inRange(BlockPos origin, BlockPos pos) {
        return Math.abs(pos.getX() - origin.getX()) <= 15 && Math.abs(pos.getZ() - origin.getZ()) <= 15
                && Math.abs(pos.getY() - origin.getY()) <= 96;
    }

    static boolean isIce(BlockState state) {
        return state.is(Blocks.ICE) || state.is(Blocks.PACKED_ICE) || state.is(Blocks.BLUE_ICE)
                || state.is(FHBlocks.FIRM_ICE_BLOCK.get());
    }

    static boolean isBase(BlockState state) {
        return state.getFluidState().isEmpty() && !state.is(Tags.Blocks.ORES)
                && (state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(BlockTags.STONE_ORE_REPLACEABLES)
                || state.is(BlockTags.DEEPSLATE_ORE_REPLACEABLES) || state.is(Blocks.CALCITE)
                || IceCaveSpace.isRock(state) || isIce(state) || state.is(Blocks.SNOW_BLOCK));
    }

    boolean add(BlockPos pos, BlockState ice) {
        // At most the vanilla one-chunk decoration neighborhood; never relocate an edge candidate.
        if (!inRange(origin, pos) || !canRead(level, pos) || !level.ensureCanWrite(pos)
                || !level.getBiome(pos).is(ICE_CAVES)) return false;
        BlockState old = level.getBlockState(pos);
        if (!old.getFluidState().isEmpty() || (!old.isAir() && !isBase(old))) return false;
        blocks.put(pos.immutable(), ice);
        return true;
    }

    boolean place() {
        if (blocks.isEmpty()) return false;
        // Also preserve the existing one-block dry spring shore, including diagonals.
        LongOpenHashSet checked = new LongOpenHashSet();
        for (BlockPos pos : blocks.keySet()) {
            for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) for (int dz = -1; dz <= 1; dz++) {
                BlockPos neighbor = pos.offset(dx, dy, dz);
                if (!checked.add(neighbor.asLong())) continue;
                if (!canRead(level, neighbor)) return false;
                var fluid = level.getFluidState(neighbor);
                if (!fluid.isEmpty() && fluid.getType().isSame(FHFluids.HOT_SPRING.get())) return false;
            }
        }
        blocks.forEach((pos, state) -> level.setBlock(pos, state, 2));
        return true;
    }

    static BlockPos findFloor(WorldGenLevel level, BlockPos origin, int search) {
        for (int distance = 0; distance <= search; distance++) {
            for (int sign : distance == 0 ? new int[] {1} : new int[] {-1, 1}) {
                BlockPos air = origin.offset(0, distance * sign, 0);
                if (canRead(level, air.below()) && canRead(level, air.above())
                        && level.getBlockState(air).isAir() && level.getBlockState(air.above()).isAir()
                        && isBase(level.getBlockState(air.below()))) return air;
            }
        }
        return null;
    }
}
