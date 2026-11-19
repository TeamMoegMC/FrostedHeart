/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.content.climate.thermal.solver.TiledThermalField;
import com.teammoeg.frostedheart.content.climate.thermal.source.AirMixingRegion;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;

/** Supplies emitter membership for standalone solver fixtures that have no physical-source bindings. */
final class TestOutletMixing {
    private final LongOpenHashSet enabled = new LongOpenHashSet();
    private final AirMixingRegion region = new AirMixingRegion();

    boolean set(TiledThermalField field, BlockPos port, boolean active) {
        if (!(active ? enabled.add(port.asLong()) : enabled.remove(port.asLong()))) return false;
        int r = AirMixingRegion.RADIUS;
        for (int y = (port.getY() - r) & ~3; y <= port.getY() + r; y += 4)
            for (int z = (port.getZ() - r) & ~3; z <= port.getZ() + r; z += 4)
                for (int x = (port.getX() - r) & ~3; x <= port.getX() + r; x += 4) {
                    long section = SectionPos.asLong(x >> 4, y >> 4, z >> 4);
                    if (!field.hasSection(section)) continue;
                    region.reset(x, y, z);
                    var ports = enabled.iterator();
                    while (ports.hasNext()) {
                        long p = ports.nextLong();
                        region.include(BlockPos.getX(p), BlockPos.getY(p), BlockPos.getZ(p));
                    }
                    int brick = ((x & 15) >>> 2) | (z & 12) | ((y & 12) << 2);
                    field.replaceMixing(section, brick, region);
                }
        return true;
    }
}
