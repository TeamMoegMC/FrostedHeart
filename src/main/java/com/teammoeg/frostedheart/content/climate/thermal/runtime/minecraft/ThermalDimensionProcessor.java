/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

/** Worker-confined dimension engine boundary used by the bounded mailbox. */
interface ThermalDimensionProcessor extends AutoCloseable {
    ThermalCompletion process(ThermalInputBatch batch);

    @Override
    void close();
}
