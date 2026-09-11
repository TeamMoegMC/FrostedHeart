package com.teammoeg.frostedresearch.data;

import com.teammoeg.frostedresearch.research.Research;
import com.teammoeg.frostedresearch.research.ResearchCategory;
import com.teammoeg.frostedresearch.research.effects.EffectStats;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class TeamResearchDataResetTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void administrativeResetRevokesStatsAndClearsRepeatLevel() {
        Research research = new Research();
        research.setId("reset-test");
        research.setCategory(ResearchCategory.RESCUE);
		research.setInfinite(true);
        EffectStats stats = new EffectStats("reset_test_stat", 5);
        stats.setNonce("stats");
        research.attachEffect(stats);

        ResearchData progress = new ResearchData(
                250, new boolean[]{true, true}, 3, Map.of(), Map.of("stats", true));
        CompoundTag variants = new CompoundTag();
        variants.putDouble("reset_test_stat", 20);
        TeamResearchData data = new TeamResearchData(
                variants, Map.of(research.getId(), progress), -1, 10, 1, Optional.empty());

        data.resetData(null, research);

        assertFalse(progress.canResearch());
        assertFalse(progress.isCompleted());
        assertFalse(progress.isEffectGranted(stats));
        assertEquals(0, progress.getCommitted());
        assertEquals(0, progress.getLevel());
        assertEquals(0.0D, data.getVariants().getDouble("reset_test_stat"));
        assertEquals(1, data.getUsedInsightLevel(), "reset does not infer an insight refund");
    }

    @Test
    void infiniteRepeatResetKeepsGrantedRewardAndLevelForIncrement() {
        Research research = new Research();
        research.setId("repeat-reset-test");
        research.setCategory(ResearchCategory.RESCUE);
		research.setInfinite(true);
        EffectStats stats = new EffectStats("repeat_reset_test_stat", 5);
        stats.setNonce("stats");
        research.attachEffect(stats);

        ResearchData progress = new ResearchData(
                250, new boolean[]{true, true}, 3, Map.of(), Map.of("stats", true));
        CompoundTag variants = new CompoundTag();
        variants.putDouble("repeat_reset_test_stat", 20);
        TeamResearchData data = new TeamResearchData(
                variants, Map.of(research.getId(), progress), -1, 10, 1, Optional.empty());

        data.resetForRepeat(null, research);

        assertFalse(progress.isCompleted());
        assertFalse(progress.isEffectGranted(stats));
        assertEquals(3, progress.getLevel());
        assertEquals(20.0D, data.getVariants().getDouble("repeat_reset_test_stat"));
    }
}
