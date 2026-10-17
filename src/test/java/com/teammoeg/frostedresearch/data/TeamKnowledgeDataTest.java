package com.teammoeg.frostedresearch.data;

import net.minecraft.nbt.Tag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamKnowledgeDataTest {
    @Test
    void persistsAllFourAcquiredSetsAndKeepsOrphans() {
        TeamKnowledgeData original = new TeamKnowledgeData();
        ResourceLocation finding = id("orphan_finding");
        ResourceLocation design = id("design");
        ResourceLocation construction = id("construction");
        ResourceLocation procedure = id("procedure");
        original.acquireFinding(finding);
        original.acquireDesign(design);
        original.acquireConstruction(construction);
        original.acquireProcedure(procedure);

        Tag encoded = TeamKnowledgeData.CODEC.encodeStart(NbtOps.INSTANCE, original)
                .getOrThrow(false, message -> { throw new AssertionError(message); });
        TeamKnowledgeData decoded = TeamKnowledgeData.CODEC.parse(NbtOps.INSTANCE, encoded)
                .getOrThrow(false, message -> { throw new AssertionError(message); });
        assertEquals(original.findingIds(), decoded.findingIds());
        assertEquals(original.designIds(), decoded.designIds());
        assertEquals(original.constructionIds(), decoded.constructionIds());
        assertEquals(original.procedureIds(), decoded.procedureIds());
    }

    @Test
    void grantsAreIdempotent() {
        TeamKnowledgeData data = new TeamKnowledgeData();
        assertTrue(data.acquireConstruction(id("machine")));
        long revision = data.mutationRevision();
        assertFalse(data.acquireConstruction(id("machine")));
        assertEquals(revision, data.mutationRevision());
    }

    @Test
    void revokeIsIdempotentAndCanRemoveOrphanHistory() {
        TeamKnowledgeData data = new TeamKnowledgeData();
        ResourceLocation orphan = id("orphan");
        data.acquireProcedure(orphan);
        long acquiredRevision = data.mutationRevision();
        assertTrue(data.revokeProcedure(orphan));
        assertTrue(data.mutationRevision() > acquiredRevision);
        long revokedRevision = data.mutationRevision();
        assertFalse(data.revokeProcedure(orphan));
        assertEquals(revokedRevision, data.mutationRevision());
        assertFalse(data.hasProcedure(orphan));
    }

    @Test
    void missingLegacyComponentPayloadDecodesAsEmpty() {
        TeamKnowledgeData decoded = TeamKnowledgeData.CODEC.parse(NbtOps.INSTANCE,
                        new net.minecraft.nbt.CompoundTag())
                .getOrThrow(false, message -> { throw new AssertionError(message); });
        assertTrue(decoded.findingIds().isEmpty());
        assertTrue(decoded.designIds().isEmpty());
        assertTrue(decoded.constructionIds().isEmpty());
        assertTrue(decoded.procedureIds().isEmpty());
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation("test", path);
    }
}
