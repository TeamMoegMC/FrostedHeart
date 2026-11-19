package com.teammoeg.frostedheart.content.agriculture.tree;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer.FoliageAttachment;
import net.minecraft.world.level.levelgen.feature.trunkplacers.StraightTrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacer;

/** Transient native-shape adapter. All native trunk/branch logs share a finite wood quota. */
public final class CultivatedNativeTrunkPlacer extends StraightTrunkPlacer {
    private final TrunkPlacer nativeTrunk;
    private final int logQuota;
    private final int width;
    private final int minHeight;

    public CultivatedNativeTrunkPlacer(TrunkPlacer nativeTrunk, int height, int width, int minHeight) {
        // TreeFeature must check clearance for the full budget height, including filled columns.
        super(height, 0, 0);
        this.nativeTrunk = height < minHeight ? new StraightTrunkPlacer(height, 0, 0) : nativeTrunk;
        this.width = width;
        this.minHeight = minHeight;
        this.logQuota = width * width * height;
    }

    @Override
    public boolean isFree(LevelSimulatedReader level, BlockPos pos) { return nativeTrunk.isFree(level, pos); }

    @Override
    public List<FoliageAttachment> placeTrunk(LevelSimulatedReader level, BiConsumer<BlockPos, BlockState> setter,
            RandomSource random, int height, BlockPos origin, TreeConfiguration configuration) {
        Map<BlockPos, BlockState> proposed = new LinkedHashMap<>();
        int nativeHeight = width == 1 && !(nativeTrunk instanceof StraightTrunkPlacer) && height > minHeight
                ? Math.max(minHeight, height - 2) : height;
        List<FoliageAttachment> foliage = nativeTrunk.placeTrunk(level,
                (pos, state) -> proposed.put(pos.immutable(), state), random, nativeHeight, origin, configuration);
        Map<BlockPos, BlockState> logs = new LinkedHashMap<>();
        for (var entry : proposed.entrySet()) {
            if (entry.getValue().is(BlockTags.LOGS) && logs.size() < logQuota) logs.put(entry.getKey(), entry.getValue());
        }
        // Complete native shortened columns / unused fork budget inside the trunk footprint.
        // Nothing is emitted until the entire quota fits, allowing the existing sapling rollback on failure.
        int layers = (logQuota + width * width - 1) / (width * width);
        for (int y = 0; y < layers && logs.size() < logQuota; y++)
            for (int x = 0; x < width && logs.size() < logQuota; x++)
                for (int z = 0; z < width && logs.size() < logQuota; z++) {
                    BlockPos pos = origin.offset(x, y, z);
                    if (!logs.containsKey(pos) && validTreePos(level, pos))
                        logs.put(pos, configuration.trunkProvider.getState(random, pos));
                }
        if (logs.size() != logQuota) return List.of();
        for (var entry : proposed.entrySet()) if (!entry.getValue().is(BlockTags.LOGS)) setter.accept(entry.getKey(), entry.getValue());
        logs.forEach(setter);
        List<FoliageAttachment> supported = new ArrayList<>();
        for (FoliageAttachment attachment : foliage)
            if (logs.containsKey(attachment.pos()) || logs.containsKey(attachment.pos().below())) supported.add(attachment);
        if (supported.isEmpty()) supported.add(new FoliageAttachment(origin.above(Math.min(height, layers)), 0, width == 2));
        return supported;
    }
}
