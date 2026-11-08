package com.teammoeg.frostedheart.content.world.features.deposit;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/** Early finite pools and a bounded final ridge skin. All ore and vegetation use original biome features. */
public final class DepositFeature extends Feature<DepositFeature.Config> {
    public record Config(boolean surfaceOnly, net.minecraft.world.level.block.state.BlockState mineral, net.minecraft.world.level.block.state.BlockState secondary, net.minecraft.world.level.block.state.BlockState rock) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.BOOL.optionalFieldOf("surface_only", false).forGetter(Config::surfaceOnly),
                net.minecraft.world.level.block.state.BlockState.CODEC.optionalFieldOf("mineral", Blocks.CALCITE.defaultBlockState()).forGetter(Config::mineral),
                net.minecraft.world.level.block.state.BlockState.CODEC.optionalFieldOf("secondary", Blocks.CALCITE.defaultBlockState()).forGetter(Config::secondary),
                net.minecraft.world.level.block.state.BlockState.CODEC.optionalFieldOf("rock", Blocks.DEEPSLATE.defaultBlockState()).forGetter(Config::rock)).apply(instance, Config::new));
    }
    public DepositFeature(Codec<Config> codec) { super(codec); }

    @Override public boolean place(FeaturePlaceContext<Config> context) {
        var regions = DepositRegions.resolve(context.level().getLevel().getChunkSource().randomState().sampler().depth());
        if (regions == null) return false;
        var bounds = new ChunkPos(context.origin());
        var layout = regions.layout(bounds.getMiddleBlockX(), bounds.getMiddleBlockZ());
        if (layout == null) return false;
        boolean surface = context.config().surfaceOnly();
        if (surface && !layout.intersectsLocal(bounds, -24, 2, -5, 30)) return false;
        if (!layout.intersectsLocal(bounds, -65, 65, -20, 34)) return false;
        var level = context.level();
        var chunk = level.getChunk(bounds.x, bounds.z);
        var pos = new BlockPos.MutableBlockPos();
        var limestone = context.config().secondary();
        int sedimentWrites = 0, ridgeWrites = 0, waterWrites = 0;
        for (int x = bounds.getMinBlockX(); x <= bounds.getMaxBlockX(); x++) {
            for (int z = bounds.getMinBlockZ(); z <= bounds.getMaxBlockZ(); z++) {
                var column = layout.column(x, z);
                boolean pool = !surface && column.poolQ < 1.85;
                boolean ridge = surface && column.hasRidgeColumn();
                if (!pool && !ridge) continue;
                int bottom = pool ? Math.max(-10, (int) Math.floor(column.floor - 2)) : -6;
                int top = pool ? (int) Math.floor(column.poolLevel + 2) : 17;
                if (ridge) { bottom = Math.min(bottom, -6); top = Math.max(top, 17); }
                for (int w = bottom; w <= top; w++) {
                    double body = ridge ? column.ridgeDistance(column.bankY + w) : -100;
                    if (surface && body < -.8) continue;
                    pos.set(x, column.bankY + w, z);
                    var old = chunk.getBlockState(pos);
                    if (old.is(BlockTags.BASE_STONE_OVERWORLD) || surface && (old.is(Blocks.CALCITE) || old.equals(limestone))) {
                        if (surface) {
                            double seam = column.ridgeSeamDistance(column.bankY + w);
                            // Seams continue into the body; the dark face is a shallow skin, with native ore hosts inside.
                            if (seam <= 1.25 || body < 2.4) {
                                var next = seam < .8 ? context.config().mineral()
                                        : seam <= 1.25 ? limestone : context.config().rock();
                                if (!next.equals(old) && level.setBlock(pos, next, 2)) ridgeWrites++;
                            }
                        } else if (pool && w <= column.poolLevel + 2 && w >= column.floor - 2
                                && (column.sedimentStrength > .3 || column.poolQ < 1.4
                                && DepositLayout.unit(layout.hash(Math.floorDiv(x, 3), 0, Math.floorDiv(z, 3), 61)) < .48)) {
                            var next = DepositLayout.unit(layout.hash(x, column.bankY + w, z, 63)) < .25
                                    ? limestone : context.config().mineral();
                            if (!next.equals(old) && level.setBlock(pos, next, 2)) sedimentWrites++;
                        }
                    } else if (!surface && old.isAir() && column.poolQ < .96 && w > column.floor && w <= Math.floor(column.poolLevel)) {
                        if (level.setBlock(pos, Blocks.WATER.defaultBlockState(), 2)) {
                            chunk.markPosForPostprocessing(pos);
                            waterWrites++;
                        }
                    }
                }
            }
        }
        return sedimentWrites + ridgeWrites + waterWrites > 0;
    }
}
