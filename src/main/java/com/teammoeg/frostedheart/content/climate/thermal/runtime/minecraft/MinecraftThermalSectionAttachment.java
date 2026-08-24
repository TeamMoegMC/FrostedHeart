/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

/** Dormant owner slot mixed into every loaded chunk section. */
public interface MinecraftThermalSectionAttachment {
    MinecraftThermalInput.SectionOwner frostedheart$getThermalInputOwner();

    void frostedheart$setThermalInputOwner(MinecraftThermalInput.SectionOwner owner);
}
