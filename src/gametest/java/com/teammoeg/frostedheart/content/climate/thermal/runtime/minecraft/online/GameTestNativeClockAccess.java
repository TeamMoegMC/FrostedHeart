/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.online;

import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Test pacing shares vanilla's deadline; no thermal result participates in that clock. */
@Mixin(value = MinecraftServer.class, remap = false)
public interface GameTestNativeClockAccess {
    @Accessor("nextTickTime") long thermalClock$deadlineMillis();
    @Accessor("nextTickTime") void thermalClock$deadlineMillis(long deadline);
}
