/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.async;

import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalCompletion;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalInputBatch;

/** Worker-confined dimension engine boundary used by the bounded mailbox. */
public interface ThermalDimensionProcessor extends AutoCloseable {
    ThermalCompletion process(ThermalInputBatch batch);

    @Override
    void close();
}
