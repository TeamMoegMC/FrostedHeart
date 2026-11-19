package com.teammoeg.frostedheart.content.agriculture.tree;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;

/** Selects loaded same-species squares and generates the species' own vanilla giant configuration. */
public final class CultivatedGiantTree {
    /** Extra logs per member; two full 2x2 layers yield exactly eight additional logs. */
    public static final int PER_SAPLING_LOG_BONUS = 2;
    private CultivatedGiantTree() {}

    /** Members are ordered NW, NE, SW, SE; score counts only already earned yield credit. */
    public record Group(BlockPos origin, List<BlockPos> members, long credit) {}

    /** At most four candidates and sixteen loaded-block reads; never queries an unloaded member. */
    public static Group find(ServerLevel level, BlockPos trigger, Block species) {
        Group best = null;
        for (int dx = -1; dx <= 0; dx++) for (int dz = -1; dz <= 0; dz++) {
            BlockPos origin = trigger.offset(dx, 0, dz);
            List<BlockPos> members = List.of(origin, origin.east(), origin.south(), origin.east().south());
            if (members.stream().anyMatch(pos -> !level.hasChunkAt(pos))) continue;
            long credit = 0;
            boolean valid = true;
            for (BlockPos pos : members) {
                if (!level.getBlockState(pos).is(species)
                        || !(level.getBlockEntity(pos) instanceof CultivatedOakBlockEntity sapling)) {
                    valid = false; break;
                }
                credit += sapling.accumulatedYieldCredit();
            }
            if (valid && (best == null || credit > best.credit())) best = new Group(origin, members, credit);
        }
        return best;
    }

    /** Average member credit is normalized to a full lifecycle, not to each member's current age. */
    public static int height(Group group, CultivatedOakProfile.Profile profile) {
        int baseHeight = profile.earnedHeight(group.credit() / (4.0 * profile.progressPerStage() * 4 * 16));
        return baseHeight < (profile.giantOnly() ? profile.maxHeight() : 1)
                ? 0 : baseHeight + PER_SAPLING_LOG_BONUS;
    }

    /** Returns a waiting reason on failure, or null after a successful placement. No sapling drops. */
    public static String generate(ServerLevel level, Group group, CultivatedOakProfile.Profile profile, RandomSource random) {
        // Recheck before reading or removing any member; the scanner itself never loads chunks.
        for (BlockPos pos : group.members()) if (!level.hasChunkAt(pos)) return "space";
        int height = height(group, profile);
        if (height < 1) return "yield";
        var tree = profile.atHeight(level, random, height, true);
        if (tree == null) return "profile";
        // Vanilla leaves three columns one block short. Complete their top cells so
        // total logs are exactly 4 * (base height + per-member bonus), not minus three.
        List<BlockPos> topCompletion = tree.trunkPlacer instanceof CultivatedNativeTrunkPlacer ? List.of() : List.of(group.origin().east().above(height - 1),
                group.origin().south().above(height - 1), group.origin().east().south().above(height - 1));
        for (BlockPos pos : topCompletion) {
            if (!level.hasChunkAt(pos)) return "space";
            BlockState state = level.getBlockState(pos);
            if (!state.isAir() && !state.is(net.minecraft.tags.BlockTags.LEAVES)) return "space";
        }
        List<BlockState> states = new ArrayList<>(), soils = new ArrayList<>();
        List<CompoundTag> ledgers = new ArrayList<>();
        for (BlockPos pos : group.members()) {
            if (!(level.getBlockEntity(pos) instanceof CultivatedOakBlockEntity sapling)) return "space";
            states.add(level.getBlockState(pos));
            soils.add(level.getBlockState(pos.below()));
            ledgers.add(sapling.saveWithFullMetadata());
        }
        for (BlockPos pos : group.members()) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
        boolean success = Feature.TREE.place(tree, level, level.getChunkSource().getGenerator(), random, group.origin());
        // Existing root rule applies to all four roots; no extra fertilizer spending or refunding.
        for (int i = 0; i < 4; i++) level.setBlock(group.members().get(i).below(), soils.get(i), 3);
        if (success) {
            for (BlockPos pos : topCompletion) level.setBlock(pos, tree.trunkProvider.getState(random, pos), 3);
            return null;
        }
        for (int i = 0; i < 4; i++) {
            BlockPos pos = group.members().get(i);
            level.setBlock(pos, states.get(i), 3);
            if (level.getBlockEntity(pos) instanceof CultivatedOakBlockEntity restored) {
                restored.load(ledgers.get(i)); restored.setChanged();
            }
        }
        return "space";
    }
}
