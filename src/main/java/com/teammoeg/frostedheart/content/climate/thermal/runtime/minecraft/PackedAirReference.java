/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

/** Primitive address of one Page-local quarter-block air region. */
final class PackedAirReference {
    static final long NONE = -1L;

    private static final long MICROCELL_BITS = 0x3fL << 8;

    private PackedAirReference() {
    }

    static long pack(int pageSlot, int pageBlock, int microcell, int localRegion) {
        if (pageSlot < 0 || pageBlock < 0 || pageBlock >= 4096
                || microcell < 0 || microcell >= 64
                || localRegion < 0 || localRegion >= 256) {
            throw new IllegalArgumentException("air reference fields are out of bounds");
        }
        return ((long) pageSlot << 32)
                | ((long) pageBlock << 14)
                | ((long) microcell << 8)
                | localRegion;
    }

    static int pageSlot(long reference) {
        return (int) (reference >>> 32);
    }

    static int pageBlock(long reference) {
        return (int) ((reference >>> 14) & 0xfffL);
    }

    static int microcell(long reference) {
        return (int) ((reference >>> 8) & 0x3fL);
    }

    static int localRegion(long reference) {
        return (int) (reference & 0xffL);
    }

    static long region(long reference) {
        return reference & ~MICROCELL_BITS;
    }
}
