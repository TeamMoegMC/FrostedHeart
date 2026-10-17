package com.teammoeg.frostedresearch.knowledge.network;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;
import java.util.Random;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

class KnowledgeSnapshotTransferTest {
    @Test
    void completeLargeArchiveInstallsOnlyAfterAllFragmentsArrive() throws Exception {
        byte[] records = new byte[5 * 1024 * 1024];
        new Random(42).nextBytes(records);
        CompoundTag snapshot = new CompoundTag();
        snapshot.putByteArray("records", records);
        snapshot.putString("team", "large archive");
        var fragments = new ArrayList<>(KnowledgeSnapshotTransfer.split(snapshot));
        assertTrue(fragments.size() > 16, "Exercise a payload larger than the old 2 MiB NBT envelope");
        assertTrue(fragments.stream().allMatch(f -> f.bytes().length <= KnowledgeSnapshotTransfer.CHUNK_BYTES));
        Collections.shuffle(fragments, new Random(7));
        var accumulator = new KnowledgeSnapshotTransfer.Accumulator();
        for (int index = 0; index < fragments.size() - 1; index++) {
            assertEquals(Optional.empty(), accumulator.accept(fragments.get(index)));
        }
        assertEquals(Optional.of(snapshot), accumulator.accept(fragments.get(fragments.size() - 1)));
        CompoundTag replacement = new CompoundTag();
        replacement.putString("team", "next team");
        assertEquals(Optional.of(replacement), accumulator.accept(KnowledgeSnapshotTransfer.split(replacement).get(0)));
    }
}
