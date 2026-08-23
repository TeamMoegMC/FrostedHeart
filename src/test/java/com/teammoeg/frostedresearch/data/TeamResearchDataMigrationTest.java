/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.data;

import com.mojang.datafixers.util.Either;
import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.frostedresearch.FHResearch;
import com.teammoeg.frostedresearch.research.Research;
import com.teammoeg.frostedresearch.research.ResearchCategory;
import com.teammoeg.frostedresearch.research.clues.Clue;
import com.teammoeg.frostedresearch.research.clues.CustomClue;
import com.teammoeg.frostedresearch.research.effects.Effect;
import com.teammoeg.frostedresearch.research.effects.EffectStats;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamResearchDataMigrationTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @AfterEach
    void clearDefinitions() {
        FHResearch.clearAll();
        FHResearch.clearCache();
    }

    @Test
    void explicitAliasesMigrateResearchClueEffectAndActiveSelection() {
        Research renamed = renamedResearch();
        FHResearch.register(renamed);
        ClueData clue = new ClueData(true);
        ResearchData oldProgress = new ResearchData(
                1234, new boolean[]{true, false}, 0,
                Map.of("old-clue", clue), Map.of("old-effect", true));
        ResearchData deletedOrphan = new ResearchData(
                77, new boolean[]{false, false}, 0, Map.of(), Map.of());
        TeamResearchData data = new TeamResearchData(
                1, new CompoundTag(),
                Map.of("old-research", oldProgress, "deleted-research", deletedOrphan),
                Either.right("old-research"), 0, 0, Optional.of(new BitSet()));

        data.reconcileDefinitions();

        assertSame(oldProgress, data.rdata.get("new-research"));
        assertFalse(data.rdata.containsKey("old-research"));
        assertSame(deletedOrphan, data.rdata.get("deleted-research"), "unknown records remain orphaned");
        assertSame(clue, oldProgress.getClueData().get("new-clue"));
        assertFalse(oldProgress.getClueData().containsKey("old-clue"));
        assertTrue(oldProgress.isEffectGranted("new-effect"));
        assertFalse(oldProgress.getEffectData().containsKey("old-effect"));
        assertEquals("new-research", data.activeResearchId);
    }

    @Test
    void canonicalRecordsWinWithoutDeletingConflictingLegacyOrphans() {
        Research renamed = renamedResearch();
        FHResearch.register(renamed);
        ClueData canonicalClue = new ClueData(true);
        ClueData legacyClue = new ClueData(false);
        ResearchData canonical = new ResearchData(
                200, new boolean[]{true, false}, 0,
                Map.of("new-clue", canonicalClue, "old-clue", legacyClue),
                Map.of("new-effect", true, "old-effect", false));
        ResearchData legacy = new ResearchData(
                999, new boolean[]{true, true}, 0, Map.of(), Map.of());
        TeamResearchData data = new TeamResearchData(
                2, new CompoundTag(),
                Map.of("new-research", canonical, "old-research", legacy),
                Either.right("new-research"), 0, 0, Optional.empty());

        data.reconcileDefinitions();

        assertSame(canonical, data.rdata.get("new-research"));
        assertSame(legacy, data.rdata.get("old-research"));
        assertSame(canonicalClue, canonical.getClueData().get("new-clue"));
        assertSame(legacyClue, canonical.getClueData().get("old-clue"));
        assertTrue(canonical.isEffectGranted("new-effect"));
        assertFalse(canonical.isEffectGranted("old-effect"));
    }

    @Test
    void missingLegacyRegistrySnapshotClearsOnlyIntegerSelection() {
        ResearchData preserved = new ResearchData(
                88, new boolean[]{true, false}, 0, Map.of(), Map.of());
        TeamResearchData data = new TeamResearchData(
                0, new CompoundTag(), Map.of("orphan", preserved), Either.left(47),
                3, 1, Optional.empty());

        assertNull(data.getCurrentResearchValue());
        assertSame(preserved, data.rdata.get("orphan"));
        assertEquals(3, data.getInsight());
        assertEquals(1, data.getUsedInsightLevel());
    }

    @Test
    void currentSaveWritesSchemaVersionAndStringActiveId() {
        Research research = renamedResearch();
        FHResearch.register(research);
        ResearchData progress = new ResearchData(
                10, new boolean[]{true, false}, 0, Map.of(), Map.of());
        TeamResearchData data = new TeamResearchData(
                2, new CompoundTag(), Map.of(research.getId(), progress),
                Either.right(research.getId()), 0, 0, Optional.empty());

        CompoundTag encoded = (CompoundTag) TeamResearchData.CODEC
                .encodeStart(NbtOps.INSTANCE, data).result().orElseThrow();

        assertEquals(TeamResearchData.CURRENT_SCHEMA_VERSION, encoded.getInt("schemaVersion"));
        assertInstanceOf(StringTag.class, encoded.get("active"));
        assertEquals(research.getId(), encoded.getString("active"));
    }

    private static Research renamedResearch() {
        CustomClue clue = new CustomClue(new Clue.BaseData(
                "", "", "", "new-clue", false, 0.25F, List.of("old-clue")));
        EffectStats effect = new EffectStats(new Effect.BaseData(
                "", List.of(), CIcons.nop(), "new-effect", false, List.of("old-effect")),
                "migration-test", 1, false);
        Research research = new Research(
                CIcons.nop(), ResearchCategory.RESCUE, List.of(), List.of(clue), List.of(),
                Optional.of(List.of(effect)), "", List.of(), List.of(),
                new boolean[6], 1000, 1, List.of("old-research"));
        research.setId("new-research");
        return research;
    }
}
