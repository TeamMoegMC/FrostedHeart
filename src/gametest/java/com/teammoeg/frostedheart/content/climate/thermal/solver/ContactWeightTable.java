/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.solver;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.AirBackingMeter;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;

/** Immutable, lossless weights: a constant, a value palette, or dense doubles per block. */
final class ContactWeightTable {
    private static final int SHIFT = 12;
    private static final int WIDTH = 1 << SHIFT;
    private static final int MASK = WIDTH - 1;
    private final double[][] values;
    private final short[][] paletteIndices;
    private final int size;
    private final int sharedBlocks, uniformBlocks, dictionaryBlocks;
    private final long payloadBytes;

    private ContactWeightTable(Preparation prepared) {
        size = prepared.size; values = prepared.values; paletteIndices = prepared.paletteIndices;
        sharedBlocks = prepared.shared; uniformBlocks = prepared.uniform;
        dictionaryBlocks = prepared.dictionaries; payloadBytes = prepared.payload;
    }
    static final class Preparation {
        final DoubleArrayList source;
        final ContactWeightTable previous;
        final int size, blocks;
        final double[][] values;
        final short[][] paletteIndices;
        Long2IntOpenHashMap palettePositions;
        DoubleArrayList palette;
        int block, shared, uniform, dictionaries;
        long payload;
        Preparation(DoubleArrayList source, ContactWeightTable previous) {
            this.source = source; this.previous = previous; size = source.size(); blocks = (size + WIDTH - 1) >>> SHIFT;
            values = new double[blocks][]; paletteIndices = new short[blocks][];
        }
        boolean run(long deadline) {
            do {
                if (block == blocks) return true;
                encodeBlock(); block++;
            } while (System.nanoTime() < deadline);
            return block == blocks;
        }
        private void encodeBlock() {
            int first = block << SHIFT, count = Math.min(WIDTH, size - first);
            if (sameBlock(source, first, count, previous)) {
                values[block] = previous.values[block];
                paletteIndices[block] = previous.paletteIndices[block];
                shared++;
            } else {
                double value = source.getDouble(first);
                long bits = Double.doubleToRawLongBits(value);
                int offset = 1;
                while (offset < count && Double.doubleToRawLongBits(source.getDouble(first + offset)) == bits) offset++;
                if (offset == count) values[block] = new double[]{value};
                else encodePalette(first, count);
            }
            payload += values[block].length * 8L;
            if (values[block].length == 1) uniform++;
            if (paletteIndices[block] != null) {
                dictionaries++;
                payload += paletteIndices[block].length * 2L;
            }
        }
        private void encodePalette(int first, int count) {
            if (palettePositions == null) {
                palettePositions = new Long2IntOpenHashMap();
                palettePositions.defaultReturnValue(-1);
                palette = new DoubleArrayList();
            } else {
                palettePositions.clear();
                palette.clear();
            }
            short[] codes = new short[count];
            for (int offset = 0; offset < count; offset++) {
                double value = source.getDouble(first + offset);
                long bits = Double.doubleToRawLongBits(value);
                int position = palettePositions.get(bits);
                if (position < 0) {
                    position = palette.size();
                    palettePositions.put(bits, position);
                    palette.add(value);
                }
                codes[offset] = (short) position;
            }
            // Include both array headers when deciding whether a palette pays.
            long codeBytes = (16L + count * 2L + 7L) & ~7L;
            if (codeBytes + palette.size() * 8L + 16L < count * 8L + 16L) {
                values[block] = palette.toDoubleArray();
                paletteIndices[block] = codes;
            } else {
                values[block] = new double[count];
                System.arraycopy(source.elements(), first, values[block], 0, count);
            }
        }
        ContactWeightTable result() {
            if (block != blocks) throw new IllegalStateException("Contact palette is unfinished");
            return new ContactWeightTable(this);
        }
    }

    private static boolean sameBlock(DoubleArrayList source, int first, int count, ContactWeightTable previous) {
        if (previous == null || first >= previous.size || count != Math.min(WIDTH, previous.size - first)) return false;
        for (int offset = 0; offset < count; offset++)
            if (Double.doubleToRawLongBits(source.getDouble(first + offset))
                    != Double.doubleToRawLongBits(previous.get(first + offset))) return false;
        return true;
    }

    double get(int entry) {
        int block = entry >>> SHIFT, offset = entry & MASK;
        double[] blockValues = values[block];
        if (blockValues.length == 1) return blockValues[0];
        short[] codes = paletteIndices[block];
        return blockValues[codes == null ? offset : Short.toUnsignedInt(codes[offset])];
    }

    void recordBacking(AirBackingMeter meter) {
        meter.array(values);
        meter.array(paletteIndices);
    }

    String describe() {
        return "contactWeightsPayloadBytes=" + payloadBytes + ",contactWeightsDensePayloadBytes=" + size * 8L
                + ",contactWeightBlocks=" + values.length + ",contactWeightUniformBlocks=" + uniformBlocks
                + ",contactWeightDictionaryBlocks=" + dictionaryBlocks
                + ",contactWeightSharedBlocks=" + sharedBlocks;
    }
}
