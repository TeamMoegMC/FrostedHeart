/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime;

import java.util.Arrays;

/** Natural-order primitive sorting with bounded runs and a retained merge position. */
public final class IntSortCursor {
    private final int[] values, scratch;
    private int sortedThrough, width = 1024, start, left, middle, right, end, output;
    private int[] source, target;
    private boolean copying;
    private int copyCursor;

    public IntSortCursor(int[] values) {
        this.values = values; scratch = new int[values.length]; source = values; target = scratch;
    }
    public boolean run(long deadline) {
        do {
            if (sortedThrough < values.length) {
                int next = Math.min(values.length, sortedThrough + 1024);
                Arrays.sort(values, sortedThrough, next); sortedThrough = next;
            } else if (copying) {
                int next = Math.min(values.length, copyCursor + 4096);
                System.arraycopy(source, copyCursor, values, copyCursor, next - copyCursor); copyCursor = next;
                if (copyCursor == values.length) return true;
            } else if (width >= values.length) {
                if (source == values) return true;
                copying = true;
            } else {
                if (output == end) {
                    if (start == values.length) {
                        int[] previous = source; source = target; target = previous;
                        width = width > values.length / 2 ? values.length : width * 2;
                        start = output = end = 0; continue;
                    }
                    left = start; middle = Math.min(values.length, start + width);
                    right = middle; end = Math.min(values.length, middle + width); output = start;
                }
                int stop = Math.min(end, output + 4096);
                for (; output < stop; output++) target[output] = right == end || left < middle && source[left] <= source[right]
                        ? source[left++] : source[right++];
                if (output == end) start = end;
            }
        } while (System.nanoTime() < deadline);
        return false;
    }
}
