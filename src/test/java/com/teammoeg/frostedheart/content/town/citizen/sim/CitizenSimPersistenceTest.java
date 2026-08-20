/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.citizen.sim;

import com.teammoeg.frostedheart.content.town.resident.Resident;
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
        assertEquals(decoded.dir[0], decoded.sdir[0]);
    }

    @Test
    void legacyCitizenSimWithPersistedSdirRestoresCanonicalDirection() {
        CitizenSim source = sampleSim();
        CompoundTag legacy = source.save(new CompoundTag());
        legacy.putByteArray("sdir", new byte[] { (byte) 197 });

        CitizenSim decoded = CitizenSim.load(legacy);

        assertEquals(1, decoded.size());
        assertEquals((byte) 197, decoded.sdir[0]);
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
        sim.add(9, 0, 0, 0);

        assertThrows(IllegalArgumentException.class, () -> sim.add(9, 1, 2, 3));
        assertEquals(1, sim.size());
    }

    @Test
    void invalidRuntimeIdsAreRejected() {
        CitizenSim sim = new CitizenSim(2);

        assertThrows(IllegalArgumentException.class, () -> sim.add(0, 0, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> sim.add(Integer.MAX_VALUE, 0, 0, 0));
        assertEquals(0, sim.size());
    }

    @Test
    void replacingSessionIdPreservesDataAndReverseLookup() {
        CitizenSim sim = sampleSim();

        assertEquals(41, sim.replaceId(0, 77));

        assertEquals(-1, sim.indexOf(41));
        assertEquals(0, sim.indexOf(77));
        assertEquals(77, sim.id[0]);
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
        data.sim.add(13, 0, 0, 0);

        assertTrue(CitizenSimScheduler.removeData(data, 13));
        assertEquals(0, data.sim.size());
        assertEquals(1, dirtyCalls.get());
        assertFalse(CitizenSimScheduler.removeData(data, 13));
        assertEquals(1, dirtyCalls.get());
    }

    @Test
    void transientHomeLayoutSurvivesGrowthAndSwapRemoveButIsNotPersisted() {
        CitizenSim sim = new CitizenSim(1);
        for (int id = 1; id <= 17; id++)
            sim.add(id, id << 10, 0, 0);
        int tail = sim.indexOf(17);
        sim.homePos[tail] = 123456789L;
		sim.homeSlot[tail] = 11;
		sim.presentationFlags[tail] = CitizenSim.PRESENT_ON_VALID_BED;

        assertTrue(sim.remove(1));

        int moved = sim.indexOf(17);
        assertEquals(123456789L, sim.homePos[moved]);
		assertEquals(11, sim.homeSlot[moved]);
		assertEquals(CitizenSim.PRESENT_ON_VALID_BED, sim.presentationFlags[moved]);

        CitizenSim decoded = CitizenSim.load(sim.save(new CompoundTag()));
        int restored = decoded.indexOf(17);
        assertEquals(CitizenSim.NO_HOME_POS, decoded.homePos[restored]);
		assertEquals(-1, decoded.homeSlot[restored]);
		assertEquals(0, decoded.presentationFlags[restored]);
		assertEquals(Resident.AGE_ADULT, decoded.presentationAge(restored));
    }

	@Test
	void transientAgeMirrorUsesTwoBitsAndPreservesOtherPresentationFlags() {
		CitizenSim sim = new CitizenSim(1);
		int index = sim.add(31, 0, 0, 0);

		assertEquals(Resident.AGE_ADULT, sim.presentationAge(index));
		sim.presentationFlags[index] |= CitizenSim.PRESENT_ON_VALID_BED;
		assertTrue(sim.setPresentationAge(index, Resident.AGE_INFANT));
		assertEquals(Resident.AGE_INFANT, sim.presentationAge(index));
		assertTrue((sim.presentationFlags[index] & CitizenSim.PRESENT_ON_VALID_BED) != 0);
		assertTrue(sim.setPresentationAge(index, Resident.AGE_CHILD));
		assertEquals(Resident.AGE_CHILD, sim.presentationAge(index));
		assertTrue(sim.setPresentationAge(index, Resident.AGE_ELDER));
		assertEquals(Resident.AGE_ELDER, sim.presentationAge(index));
		assertTrue(sim.setPresentationAge(index, Resident.AGE_ADULT));
		assertEquals(Resident.AGE_ADULT, sim.presentationAge(index));
		assertFalse(sim.setPresentationAge(index, 99));
	}

    private static CitizenSim sampleSim() {
        CitizenSim sim = new CitizenSim(1);
        copySampleInto(sim);
        return sim;
    }

    private static void copySampleInto(CitizenSim sim) {
        int i = sim.add(41, 1024, 2048, -3072);
        sim.dir[i] = (byte) 203;
        sim.sdir[i] = (byte) 197;
        sim.state[i] = CitizenState.WORK;
        sim.homeX[i] = 11;
        sim.homeZ[i] = -12;
        sim.uuidHi[i] = 123L;
        sim.uuidLo[i] = 456L;
        sim.tx[i] = 4096;
        sim.tz[i] = -6144;
    }

    private static void assertSample(CitizenSim sim) {
        assertEquals(1, sim.size());
        assertEquals(41, sim.id[0]);
        assertEquals(1024, sim.px[0]);
        assertEquals(2048, sim.py[0]);
        assertEquals(-3072, sim.pz[0]);
        assertEquals((byte) 203, sim.dir[0]);
        assertEquals((byte) 203, sim.sdir[0]); // sdir is not persisted, defaults to dir
        assertEquals(CitizenState.WORK, sim.state[0]);
        assertEquals(11, sim.homeX[0]);
        assertEquals(-12, sim.homeZ[0]);
        assertEquals(123L, sim.uuidHi[0]);
        assertEquals(456L, sim.uuidLo[0]);
        assertEquals(4096, sim.tx[0]);
        assertEquals(-6144, sim.tz[0]);
    }
}
