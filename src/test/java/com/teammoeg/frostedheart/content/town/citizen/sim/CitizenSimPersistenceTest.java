/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.citizen.sim;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CitizenSimPersistenceTest {

    @Test
    void legacyCitizenSimWithoutCanonicalYawFallsBackToCurrentYaw() {
        CitizenSim source = sampleSim();
        CompoundTag legacy = source.save(new CompoundTag());
        legacy.remove("syaw");

        CitizenSim decoded = CitizenSim.load(legacy);

        assertEquals(1, decoded.size());
        assertEquals(source.dir[0], decoded.dir[0]);
        assertEquals(source.sdir[0], decoded.sdir[0]);
    }

    @Test
    void townSimRoundTripUsesNestedLayout() {
        TownSimData source = new TownSimData();
        copySampleInto(source.sim);

        CompoundTag encoded = TownSimData.toNbt(source);
        TownSimData decoded = new TownSimData();
        decoded.loadFromNbt(encoded);

        assertFalse(encoded.getCompound("sim").isEmpty());
        assertSample(decoded.sim);
    }

    @Test
    void townSimLoadsAccidentallyFlatLegacyLayout() {
        CompoundTag flatLegacy = sampleSim().save(new CompoundTag());
        TownSimData decoded = new TownSimData();

        decoded.loadFromNbt(flatLegacy);

        assertSample(decoded.sim);
    }

    @Test
    void townSimRoundTripPreservesLastActiveDimension() {
        CompoundTag encoded = TownSimData.toNbt(new TownSimData());
        encoded.putString("dimension", "minecraft:the_nether");
        TownSimData decoded = new TownSimData();

        decoded.loadFromNbt(encoded);

        assertEquals("minecraft:the_nether", TownSimData.toNbt(decoded).getString("dimension"));
    }

    @Test
    void corruptDuplicateIdsAndInvalidStateAreSanitized() {
        CompoundTag corrupt = new CompoundTag();
        corrupt.putInt("size", 2);
        corrupt.putIntArray("id", new int[] { 7, 7 });
        corrupt.putIntArray("px", new int[] { 1024, 8192 });
        corrupt.putIntArray("py", new int[] { 2048, 9216 });
        corrupt.putIntArray("pz", new int[] { 3072, 10240 });
        corrupt.putByteArray("state", new byte[] { (byte) 99, CitizenState.WORK });
        corrupt.putLongArray("uuidHi", new long[] { 123L });

        CitizenSim decoded = CitizenSim.load(corrupt);

        assertEquals(1, decoded.size());
        assertEquals(7, decoded.id[0]);
        assertEquals(1024, decoded.px[0]);
        assertEquals(CitizenState.IDLE, decoded.state[0]);
        assertEquals(0L, decoded.uuidHi[0]);
        assertEquals(0L, decoded.uuidLo[0]);
    }

    @Test
    void runtimeDuplicateIdsAreRejected() {
        CitizenSim sim = new CitizenSim(2);
        sim.add(9, 0, 0, 0, (byte) 9);

        assertThrows(IllegalArgumentException.class, () -> sim.add(9, 1, 2, 3, (byte) 9));
        assertEquals(1, sim.size());
    }

    @Test
    void invalidRuntimeIdsAreRejected() {
        CitizenSim sim = new CitizenSim(2);

        assertThrows(IllegalArgumentException.class, () -> sim.add(0, 0, 0, 0, (byte) 0));
        assertThrows(IllegalArgumentException.class,
                () -> sim.add(Integer.MAX_VALUE, 0, 0, 0, (byte) 0));
        assertEquals(0, sim.size());
    }

    @Test
    void replacingSessionIdPreservesDataAndReverseLookup() {
        CitizenSim sim = sampleSim();

        assertEquals(41, sim.replaceId(0, 77));

        assertEquals(-1, sim.indexOf(41));
        assertEquals(0, sim.indexOf(77));
        assertEquals(77, sim.id[0]);
        assertEquals((byte) (77 % BehaviorSystem.SLICE), sim.tickPhase[0]);
        assertEquals(1024, sim.px[0]);
        assertEquals(123L, sim.uuidHi[0]);
        assertEquals(CitizenState.WORK, sim.state[0]);
    }

    @Test
    void corruptDuplicateManagedIdentityIsSanitized() {
        CompoundTag corrupt = new CompoundTag();
        corrupt.putInt("size", 2);
        corrupt.putIntArray("id", new int[] { 21, 22 });
        corrupt.putIntArray("px", new int[] { 1024, 8192 });
        corrupt.putIntArray("py", new int[] { 2048, 9216 });
        corrupt.putIntArray("pz", new int[] { 3072, 10240 });
        corrupt.putLongArray("uuidHi", new long[] { 123L, 123L });
        corrupt.putLongArray("uuidLo", new long[] { 456L, 456L });

        CitizenSim decoded = CitizenSim.load(corrupt);

        assertEquals(1, decoded.size());
        assertEquals(21, decoded.id[0]);
        assertEquals(123L, decoded.uuidHi[0]);
        assertEquals(456L, decoded.uuidLo[0]);
    }

    @Test
    void unifiedRemovalMarksBackingDataDirty() {
        TownSimData data = new TownSimData();
        AtomicInteger dirtyCalls = new AtomicInteger();
        data.setMarkDirty(dirtyCalls::incrementAndGet);
        data.sim.add(13, 0, 0, 0, (byte) 13);

        assertTrue(CitizenSimScheduler.removeData(data, 13));
        assertEquals(0, data.sim.size());
        assertEquals(1, dirtyCalls.get());
        assertFalse(CitizenSimScheduler.removeData(data, 13));
        assertEquals(1, dirtyCalls.get());
    }

    private static CitizenSim sampleSim() {
        CitizenSim sim = new CitizenSim(1);
        copySampleInto(sim);
        return sim;
    }

    private static void copySampleInto(CitizenSim sim) {
        int i = sim.add(41, 1024, 2048, -3072, (byte) 1);
        sim.dir[i] = (byte) 203;
        sim.sdir[i] = (byte) 197;
        sim.state[i] = CitizenState.WORK;
        sim.homeX[i] = 11;
        sim.homeZ[i] = -12;
        sim.uuidHi[i] = 123L;
        sim.uuidLo[i] = 456L;
        sim.tx[i] = 4096;
        sim.ty[i] = 5120;
        sim.tz[i] = -6144;
    }

    private static void assertSample(CitizenSim sim) {
        assertEquals(1, sim.size());
        assertEquals(41, sim.id[0]);
        assertEquals(1024, sim.px[0]);
        assertEquals(2048, sim.py[0]);
        assertEquals(-3072, sim.pz[0]);
        assertEquals((byte) 203, sim.dir[0]);
        assertEquals((byte) 197, sim.sdir[0]);
        assertEquals(CitizenState.WORK, sim.state[0]);
        assertEquals(11, sim.homeX[0]);
        assertEquals(-12, sim.homeZ[0]);
        assertEquals(123L, sim.uuidHi[0]);
        assertEquals(456L, sim.uuidLo[0]);
        assertEquals(4096, sim.tx[0]);
        assertEquals(5120, sim.ty[0]);
        assertEquals(-6144, sim.tz[0]);
    }
}
