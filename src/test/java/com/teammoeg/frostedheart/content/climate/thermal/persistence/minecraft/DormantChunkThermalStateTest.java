/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DormantChunkThermalStateTest {
    @Test
    void savedTemperatureDoesNotJumpWhenNaturalTemperatureChanges() {
        var state = new DormantChunkThermalState(0, 1);
        state.replace(0, new DormantChunkThermalState.SectionEntry(0, 10, 1,
                new byte[]{0}, new long[]{160}, new long[0]));
        assertEquals(20, state.admissionCut(0, 0, 30, -20).meanTemperatureC(0), 1e-9);
        assertEquals(0, state.admissionCut(0, 600, 30, -20).meanTemperatureC(0), 1e-9);
    }

    @Test
    void savingDoesNotAdvanceOrQuantizeDormantCoolingAgain() {
        var state = new DormantChunkThermalState(0, 1);
        state.replace(0, new DormantChunkThermalState.SectionEntry(0, 0, 1,
                new byte[]{0}, new long[]{160}, new long[0]));
        for (int i = 0; i < 20; i++) {
            CompoundTag tag = new CompoundTag(); state.encode(tag);
            assertEquals(4, tag.getCompound("FrostedHeartThermal").getInt("version"));
            state = DormantChunkThermalState.decode(tag, 0, 1);
            assertNotNull(state);
            assertEquals(5, state.admissionCut(0, 600, 30, 0).meanTemperatureC(0), 1e-9);
        }
    }
}
