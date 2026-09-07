/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.topology;

/** Primitive address of one Page-local quarter-block air region. */
final class PackedAirReference {
    static final long NONE = -1L;

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

    static int localRegion(long reference) {
        return (int) (reference & 0xffL);
    }

}
