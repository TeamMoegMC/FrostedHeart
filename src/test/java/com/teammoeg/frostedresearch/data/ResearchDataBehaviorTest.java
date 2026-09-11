/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.data;

import com.teammoeg.frostedresearch.research.Research;
import com.teammoeg.frostedresearch.research.ResearchCategory;
import com.teammoeg.frostedresearch.research.clues.CustomClue;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResearchDataBehaviorTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void totalCommittedCombinesDirectPointsAndTriggeredClueContribution() {
        Research research = researchWithClues();
        ResearchData data = new ResearchData(200, new boolean[]{true, false}, 0, Map.of(), Map.of());
        CustomClue optional = (CustomClue) research.getClues().get(1);

        data.setClueTriggered(optional, true);

        assertEquals(700L, data.getTotalCommitted(research));
        assertEquals(0.7F, data.getProgress(research), 0.0001F);
    }

    @Test
    void onlyRequiredCluesBlockCanComplete() {
        Research research = researchWithClues();
        ResearchData data = new ResearchData();
        CustomClue required = (CustomClue) research.getClues().get(0);
        CustomClue optional = (CustomClue) research.getClues().get(1);

        data.setClueTriggered(optional, true);
        assertFalse(data.canComplete(research));

        data.setClueTriggered(required, true);
        assertTrue(data.canComplete(research));
    }

    @Test
    void pointCommitRejectsNegativeAndHandlesZeroAndLongLimits() {
        Research research = researchWithClues();
        ResearchData data = new ResearchData(Long.MAX_VALUE - 5, new boolean[]{true, false}, 0, Map.of(), Map.of());

        assertThrows(IllegalArgumentException.class, () -> data.commitPoints(research, -1, null));
        assertEquals(0, data.commitPoints(research, 0, null));
        assertEquals(Long.MAX_VALUE - 5, data.getCommitted(),
                "legal historical progress remains stored even when it exceeds the current definition");
        assertTrue(Float.isFinite(data.getProgress(research)));
        assertEquals(1.0F, data.getProgress(research));

        ResearchData fresh = new ResearchData();
        assertEquals(Long.MAX_VALUE - 1000, fresh.commitPoints(research, Long.MAX_VALUE, null));
        assertEquals(1000, fresh.getCommitted());
    }

    @Test
    void negativePersistedProgressIsNormalizedWithoutTouchingOtherState() {
        ResearchData data = new ResearchData(-5, new boolean[]{true, true}, 4, Map.of(), Map.of());

        assertEquals(0, data.getCommitted());
        assertTrue(data.canResearch());
        assertTrue(data.isCompleted());
        assertEquals(4, data.getLevel());
    }

    @Test
    void networkCodecRetainsCustomClueNbtAndLongProgress() {
        CompoundTag custom = new CompoundTag();
        custom.putLong("large", 9_007_199_254_740_993L);
        ClueData clue = new ClueData(true);
        clue.setData(custom);
        ResearchData source = new ResearchData(
                5_000_000_000L, new boolean[]{true, false}, 2, Map.of("custom", clue), Map.of("reward", true));

        Tag encoded = ResearchData.NETWORK_CODEC.encodeStart(NbtOps.INSTANCE, source).result().orElseThrow();
        ResearchData decoded = ResearchData.NETWORK_CODEC.parse(NbtOps.INSTANCE, encoded).result().orElseThrow();

        assertEquals(5_000_000_000L, decoded.getCommitted());
        assertEquals(9_007_199_254_740_993L, decoded.getClueData().get("custom").getData().getLong("large"));
        assertTrue(decoded.getClueData().get("custom").isCompleted());
        assertTrue(decoded.isEffectGranted("reward"));
    }

    @Test
    void persistenceCodecReadsLegacyIntAndWritesCurrentLongTag() {
        ResearchData source = new ResearchData(
                123, new boolean[]{true, false}, 0, Map.of(), Map.of());
        CompoundTag legacy = (CompoundTag) ResearchData.CODEC.encodeStart(NbtOps.INSTANCE, source)
                .result().orElseThrow();
        legacy.putInt("committed", 123);

        ResearchData decoded = ResearchData.CODEC.parse(NbtOps.INSTANCE, legacy).result().orElseThrow();
        CompoundTag saved = (CompoundTag) ResearchData.CODEC.encodeStart(NbtOps.INSTANCE, decoded)
                .result().orElseThrow();

        assertEquals(123L, decoded.getCommitted());
        assertInstanceOf(LongTag.class, saved.get("committed"));
    }

    private static Research researchWithClues() {
        Research research = new Research();
        research.setId("behavior-test");
        research.setCategory(ResearchCategory.RESCUE);
        research.attachClue(new CustomClue("required", "Required", "Required desc", "Hint", 0.25F, true));
        research.attachClue(new CustomClue("optional", "Optional", "Optional desc", "Hint", 0.50F, false));
        return research;
    }
}
