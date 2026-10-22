package com.teammoeg.frostedheart.content.world.features;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.content.world.FHFeatures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("frostedheart_ice_cave")
@PrefixGameTestTemplate(false)
public final class IceCaveCrystalBoundaryGameTests {
    @GameTest(template = "phase0a_empty", batch = "ice_cave", timeoutTicks = 80)
    public static void raysStopBeforeUnwritableChunksButRetainNeighborPlacements(GameTestHelper helper) {
        for (ChunkPos center : List.of(new ChunkPos(0, 0), new ChunkPos(-4, -3))) {
            var registry = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
            Holder<Biome> biome = registry.getHolderOrThrow(ResourceKey.create(Registries.BIOME, IceCaveTerrain.BIOME));
            List<ChunkAccess> chunks = new ArrayList<>();
            for (int z = -2; z <= 2; z++) for (int x = -2; x <= 2; x++) {
                chunks.add(new ProtoChunk(new ChunkPos(center.x + x, center.z + z),
                        UpgradeData.EMPTY, helper.getLevel(), registry, null));
            }
            BlockPos[] support = {null};
            List<BlockPos> writes = new ArrayList<>();
            WorldGenRegion region = new WorldGenRegion(helper.getLevel(), chunks, ChunkStatus.FEATURES, 1) {
                @Override public BlockState getBlockState(BlockPos pos) {
                    return pos.equals(support[0]) ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState();
                }
                @Override public Holder<Biome> getBiome(BlockPos pos) { return biome; }
                @Override public boolean ensureCanWrite(BlockPos pos) {
                    helper.assertTrue(Math.abs((pos.getX() >> 4) - center.x) <= 1
                                    && Math.abs((pos.getZ() >> 4) - center.z) <= 1,
                            "normal searches never call the diagnostic write guard outside decoration bounds");
                    return super.ensureCanWrite(pos);
                }
                @Override public boolean setBlock(BlockPos pos, BlockState state, int flags, int recursion) {
                    helper.assertTrue(ensureCanWrite(pos) && state.is(FHBlocks.ICE_CRYSTAL.get()),
                            "the feature writes only a small crystal inside the actual vanilla write window");
                    writes.add(pos.immutable());
                    return true;
                }
            };
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos origin = new BlockPos(center.getMinBlockX() + 8, 0, center.getMinBlockZ() + 8)
                        .relative(direction, direction.getStepX() + direction.getStepZ() > 0 ? 7 : 8);
                BlockPos lastWritable = origin.relative(direction, 16);
                helper.assertTrue(region.hasChunk(lastWritable.relative(direction).getX() >> 4,
                                lastWritable.relative(direction).getZ() >> 4), "fixture includes readable but unwritable chunks");
                helper.assertTrue(IceCaveSmallCrystalFeature.canSearch(region, lastWritable)
                                && region.ensureCanWrite(lastWritable)
                                && !IceCaveSmallCrystalFeature.canSearch(region, lastWritable.relative(direction)),
                        "positive and negative chunk edges retain the entire writable neighbor");
                support[0] = null;
                writes.clear();
                helper.assertTrue(!FHFeatures.ICE_CAVE_SMALL_CRYSTAL.get().place(new FeaturePlaceContext<>(Optional.empty(),
                                region, helper.getLevel().getChunkSource().getGenerator(), RandomSource.create(31), origin,
                                FeatureConfiguration.NONE)), "empty rays stop without out-of-range write diagnostics");
                support[0] = lastWritable.relative(direction);
                helper.assertTrue(FHFeatures.ICE_CAVE_SMALL_CRYSTAL.get().place(new FeaturePlaceContext<>(Optional.empty(),
                                region, helper.getLevel().getChunkSource().getGenerator(), RandomSource.create(31), origin,
                                FeatureConfiguration.NONE)) && writes.equals(List.of(lastWritable)),
                        "a support just outside the write window can still hold a crystal on its inside face");
            }
        }
        helper.succeed();
    }
}
