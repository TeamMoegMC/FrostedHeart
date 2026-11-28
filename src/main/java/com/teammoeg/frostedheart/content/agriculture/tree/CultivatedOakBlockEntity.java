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

/** Random-tick growth ledger; no per-tick ticker. Only compact fixed-point growth and yield credit survive chunk saves. */
public final class CultivatedOakBlockEntity extends BlockEntity {
    private static final int SCALE = 16;
    /** Retained share of the fertilizer bonus above the unfertilized yield. */
    public static final double INCREASING_BONUS_SCALE = .2;
    private short progress;
    private short yieldCredit;
    private String waiting = "growing";

    public CultivatedOakBlockEntity(BlockPos pos, BlockState state) {
        super(FHBlockEntityTypes.CULTIVATED_OAK.get(), pos, state);
    }

    /** One random-tick opportunity; explicit temperature also allows deterministic real-world GameTests. */
    public void advance(ServerLevel level, float temperature) {
        advance(level, temperature, level.random);
    }

    /** Uses the same random source passed to the random-tick entry, like Forge saplings. */
    public void advance(ServerLevel level, float temperature, RandomSource random) {
        var profile = CultivatedOakProfile.get(((CultivatedOakBlock)getBlockState().getBlock()).profileId());
        if (profile == null) { waiting = "profile"; return; }
        if (level.getMaxLocalRawBrightness(worldPosition.above()) < profile.minLight()) { waiting = "light"; return; }
        BlockState soil = level.getBlockState(worldPosition.below());
        boolean fertilized = soil.is(FHBlocks.FERTILIZED_DIRT.get());
        FertilizerType type = fertilized ? soil.getValue(FertilizedDirt.FERTILIZER) : null;
        FertilizerGrade grade = fertilized ? soil.getValue(FertilizedDirt.GRADE) : FertilizerGrade.BASIC;
        float protection = type == FertilizerType.PRESERVED ? profile.preservedDegrees() * grade.preserve : 0;
        double suitability = profile.suitability(temperature, protection);
        if (!Double.isFinite(suitability)) { waiting = "temperature"; return; }
        int total = profile.progressPerStage() * 4 * SCALE;
        boolean mature = progress >= total;
        int requiredHeight = profile.minimumBudget();
        if (mature && suitability <= 0) { waiting = "temperature"; return; }
        if (mature && profile.earnedHeight((double)yieldCredit / total) >= requiredHeight) {
            tryGenerate(level, profile, random);
            return;
        }
        double speed = profile.speed(temperature, protection);
        if (type == FertilizerType.ACCELERATED) speed /= .5 * grade.growSpeed;
        int step = (int)Math.round(speed * SCALE);
        int delta = mature ? step : Math.min(step, Math.max(0, total - progress));
        double yieldFactor = suitability;
        if (type == FertilizerType.INCREASING) yieldFactor *= 1 + (grade.productivity - 1) * INCREASING_BONUS_SCALE;
        progress = (short)Math.min(total, progress + delta);
        yieldCredit = (short)Math.min(Short.MAX_VALUE, yieldCredit + Math.round(delta * yieldFactor));
        // Fertilizer is settled once after successful tree placement, not per growth opportunity.
        int age = Math.min(4, progress / (profile.progressPerStage() * SCALE));
        level.setBlock(worldPosition, getBlockState().setValue(CultivatedOakBlock.AGE, age), 3);
        waiting = age == 4 && profile.earnedHeight((double)yieldCredit / total) < requiredHeight
                ? (suitability <= 0 ? "temperature" : "yield") : "growing";
        setChanged();
        if (mature && profile.earnedHeight((double)yieldCredit / total) >= requiredHeight)
            tryGenerate(level, profile, random);
        // Keep age=4 visible until the next random tick; a failed attempt keeps it indefinitely.
    }

    private void tryGenerate(ServerLevel level, CultivatedOakProfile.Profile profile, RandomSource random) {
        int budget = profile.earnedHeight(progress > 0 ? (double)yieldCredit / progress : 0);
        if (!profile.giantTrees().isEmpty() && budget >= profile.maxHeight()) {
            var group = CultivatedGiantTree.find(level, worldPosition, getBlockState().getBlock());
            if (group != null) {
                String reason = CultivatedGiantTree.generate(level, group, profile, random);
                if (reason != null) {
                    for (BlockPos pos : group.members())
                        if (level.getBlockEntity(pos) instanceof CultivatedOakBlockEntity restored) restored.waiting = reason;
                }
                return;
            }
        }
        if (profile.giantOnly()) { waiting = "square"; return; }
        var tree = profile.atHeight(level, random, budget, false, worldPosition);
        if (tree == null) { waiting = "profile"; return; }
        // Vanilla TreeFeature handles core clearance and any cross-chunk reads/writes.
        CompoundTag saved = saveWithFullMetadata();
        BlockState sapling = getBlockState(), soil = level.getBlockState(worldPosition.below());
        level.setBlock(worldPosition, Blocks.AIR.defaultBlockState(), 2);
        boolean success = Feature.TREE.place(tree, level, level.getChunkSource().getGenerator(),
                random, worldPosition);
        if (success) {
            level.setBlock(worldPosition.below(), soil, 3);
            CultivatedTreeFertilizer.consumeAfterSuccess(level, worldPosition);
        } else {
            level.setBlock(worldPosition, sapling, 3);
            if (level.getBlockEntity(worldPosition) instanceof CultivatedOakBlockEntity restored) {
                restored.load(saved);
                restored.waiting = "space";
                restored.setChanged();
            }
        }
    }

    /** Credit earned so far, including immature saplings; no forecast of future yield. */
    int accumulatedYieldCredit() { return Math.max(0, yieldCredit); }

    /** Authoritative thermometer fields, including cold protection from this sapling's own soil. */
    public com.teammoeg.frostedheart.content.climate.network.SoilThermometerUpdatePacket.TreeInfo thermometerInfo() {
        var profile = CultivatedOakProfile.get(((CultivatedOakBlock)getBlockState().getBlock()).profileId());
        if (profile == null) return null;
        float min = profile.minTemp();
        if (level != null) {
            BlockState soil = level.getBlockState(worldPosition.below());
            if (soil.is(FHBlocks.FERTILIZED_DIRT.get()) && soil.getValue(FertilizedDirt.FERTILIZER) == FertilizerType.PRESERVED)
                min -= profile.preservedDegrees() * soil.getValue(FertilizedDirt.GRADE).preserve;
        }
        return new com.teammoeg.frostedheart.content.climate.network.SoilThermometerUpdatePacket.TreeInfo(
                getBlockState().getValue(CultivatedOakBlock.AGE), progress / (float)SCALE,
                profile.progressPerStage() * 4, waiting, min, profile.maxTemp(), profile.idealMin(), profile.idealMax(), profile.minLight());
    }

    public Component status() {
        return Component.translatable("message.frostedheart.cultivated_oak.status",
                getBlockState().getValue(CultivatedOakBlock.AGE),
                String.format(java.util.Locale.ROOT, "%.2f", progress / (double)SCALE),
                Component.translatable("message.frostedheart.cultivated_oak." + waiting));
    }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putShort("p", progress);
        tag.putShort("v", yieldCredit);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        progress = tag.contains("p", net.minecraft.nbt.Tag.TAG_SHORT) ? tag.getShort("p") : compact(tag.getDouble("Progress"));
        yieldCredit = tag.contains("v", net.minecraft.nbt.Tag.TAG_SHORT) ? tag.getShort("v") : compact(tag.getDouble("YieldCredit"));
    }
    /** Migrates the original double ledger to nonnegative 1/16 units. */
    private static short compact(double value) {
        return (short)Math.max(0, Math.min(Short.MAX_VALUE, Math.round(value * SCALE)));
    }

}
