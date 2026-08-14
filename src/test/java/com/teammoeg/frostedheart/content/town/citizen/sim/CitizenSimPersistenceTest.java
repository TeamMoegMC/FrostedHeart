/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.citizen.sim;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class CitizenSimPersistenceTest {

    @Test
    void legacyCitizenSimWithoutCanonicalYawFallsBackToCurrentYaw() {
        CitizenSim source = sampleSim();
        CompoundTag legacy = source.save(new CompoundTag());
        legacy.remove("syaw");

        CitizenSim decoded = CitizenSim.load(legacy);

        assertEquals(1, decoded.size());
        assertEquals(source.yaw[0], decoded.yaw[0]);
        assertEquals(source.yaw[0], decoded.syaw[0]);
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

    private static CitizenSim sampleSim() {
        CitizenSim sim = new CitizenSim(1);
        copySampleInto(sim);
        return sim;
    }

    private static void copySampleInto(CitizenSim sim) {
        int i = sim.add(41, 1024, 2048, -3072, (byte) 1);
        sim.yaw[i] = (byte) 203;
        sim.syaw[i] = (byte) 197;
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
        assertEquals((byte) 203, sim.yaw[0]);
        assertEquals((byte) 197, sim.syaw[0]);
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
