package com.teammoeg.frostedheart.content.agriculture.tree;

import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.bootstrap.common.FHBlockEntityTypes;
import com.teammoeg.frostedheart.content.agriculture.FertilizedDirt;
import com.teammoeg.frostedheart.content.agriculture.Fertilizer.FertilizerGrade;
import com.teammoeg.frostedheart.content.agriculture.Fertilizer.FertilizerType;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;

/** Random-tick growth ledger; no per-tick ticker. Mature targets and growth credit survive chunk saves. */
public final class CultivatedOakBlockEntity extends BlockEntity {
    private double progress;
    private double yieldCredit;
    private int targetHeight;
    private double targetLogs;
    private long treeSeed;
    private String waiting = "growing";

    public CultivatedOakBlockEntity(BlockPos pos, BlockState state) {
        super(FHBlockEntityTypes.CULTIVATED_OAK.get(), pos, state);
    }

    /** One random-tick opportunity; explicit temperature also allows deterministic real-world GameTests. */
    public void advance(ServerLevel level, float temperature) {
        var profile = CultivatedOakProfile.get();
        if (profile == null) { waiting = "profile"; return; }
        if (targetHeight > 0) { tryGenerate(level, profile); return; }
        if (level.getMaxLocalRawBrightness(worldPosition.above()) < profile.minLight()) { waiting = "light"; return; }
        BlockState soil = level.getBlockState(worldPosition.below());
        boolean fertilized = soil.is(FHBlocks.FERTILIZED_DIRT.get());
        FertilizerType type = fertilized ? soil.getValue(FertilizedDirt.FERTILIZER) : null;
        FertilizerGrade grade = fertilized ? soil.getValue(FertilizedDirt.GRADE) : FertilizerGrade.BASIC;
        float protection = type == FertilizerType.PRESERVED ? profile.preservedDegrees() * grade.preserve : 0;
        double suitability = profile.suitability(temperature, protection);
        if (!Double.isFinite(suitability) || suitability < 0) { waiting = "temperature"; return; }
        double speed = .25 + .75 * suitability;
        if (type == FertilizerType.ACCELERATED) speed /= .5 * grade.growSpeed;
        double total = profile.progressPerStage() * 4.0;
        double delta = Math.min(speed, total - progress);
        double yieldFactor = profile.minimumYieldFactor() + (1 - profile.minimumYieldFactor()) * suitability;
        if (type == FertilizerType.INCREASING) yieldFactor *= grade.productivity;
        progress += delta;
        yieldCredit += delta * yieldFactor;
        // Nutrients are paid only for growth actually earned, never for blocked generation attempts.
        if (fertilized) {
            int storage = soil.getValue(FertilizedDirt.STORAGE);
            level.setBlock(worldPosition.below(), storage > 1 ? soil.setValue(FertilizedDirt.STORAGE, storage - 1)
                    : Blocks.DIRT.defaultBlockState(), 3);
        }
        int age = Math.min(4, (int)(progress / profile.progressPerStage()));
        level.setBlock(worldPosition, getBlockState().setValue(CultivatedOakBlock.AGE, age), 3);
        waiting = "growing";
        if (progress >= total) {
            targetLogs = profile.baseLogs() * yieldCredit / progress;
            targetHeight = profile.height(targetLogs);
            treeSeed = level.random.nextLong();
        }
        setChanged();
        // Keep age=4 visible until the next random tick; a failed attempt keeps it indefinitely.
    }

    private void tryGenerate(ServerLevel level, CultivatedOakProfile.Profile profile) {
        var tree = profile.atHeight(targetHeight);
        // Conservative envelope for this profile, including randomized foliage radius/offset.
        int radius = profile.clearanceRadius();
        int offset = profile.canopyOffset();
        int reach = radius + 1;
        for (BlockPos p : BlockPos.betweenClosed(worldPosition.offset(-reach, -1, -reach),
                worldPosition.offset(reach, targetHeight + Math.max(0, offset) + 2, reach))) {
            if (level.isOutsideBuildHeight(p)) { waiting = "space"; return; }
            if (!level.hasChunkAt(p)) { waiting = "chunks"; return; }
            if (p.getY() < worldPosition.getY() || p.equals(worldPosition)) continue;
            BlockState existing = level.getBlockState(p);
            if (!existing.getFluidState().isEmpty() || (!existing.isAir() && !existing.is(net.minecraft.tags.BlockTags.LEAVES)
                    && !existing.canBeReplaced())) {
                waiting = "space"; return;
            }
        }
        CompoundTag saved = saveWithFullMetadata();
        BlockState sapling = getBlockState(), soil = level.getBlockState(worldPosition.below());
        level.setBlock(worldPosition, Blocks.AIR.defaultBlockState(), 2);
        boolean success = Feature.TREE.place(tree, level, level.getChunkSource().getGenerator(),
                RandomSource.create(treeSeed), worldPosition);
        if (success) {
            level.setBlock(worldPosition.below(), soil, 3);
        } else {
            level.setBlock(worldPosition, sapling, 3);
            if (level.getBlockEntity(worldPosition) instanceof CultivatedOakBlockEntity restored) {
                restored.load(saved);
                restored.waiting = "space";
                restored.setChanged();
            }
        }
    }

    public Component status() {
        return Component.translatable("message.frostedheart.cultivated_oak.status",
                getBlockState().getValue(CultivatedOakBlock.AGE), String.format(java.util.Locale.ROOT, "%.1f", progress),
                String.format(java.util.Locale.ROOT, "%.1f", targetLogs), targetHeight,
                Component.translatable("message.frostedheart.cultivated_oak." + waiting));
    }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putDouble("Progress", progress); tag.putDouble("YieldCredit", yieldCredit);
        tag.putInt("TargetHeight", targetHeight); tag.putDouble("TargetLogs", targetLogs); tag.putLong("TreeSeed", treeSeed);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        progress = tag.getDouble("Progress"); yieldCredit = tag.getDouble("YieldCredit");
        targetHeight = tag.getInt("TargetHeight"); targetLogs = tag.getDouble("TargetLogs"); treeSeed = tag.getLong("TreeSeed");
    }
}
