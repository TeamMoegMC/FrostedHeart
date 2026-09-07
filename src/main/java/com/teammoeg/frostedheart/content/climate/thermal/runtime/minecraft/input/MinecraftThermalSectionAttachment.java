/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.input;

/** Dormant owner slot mixed into every loaded chunk section. */
public interface MinecraftThermalSectionAttachment {
    MinecraftPageManager.SectionOwner frostedheart$getThermalInputOwner();

    void frostedheart$setThermalInputOwner(
            MinecraftPageManager.SectionOwner owner);
}
