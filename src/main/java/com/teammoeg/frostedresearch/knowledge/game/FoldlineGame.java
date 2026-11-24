package com.teammoeg.frostedresearch.knowledge.game;

import com.google.gson.Gson;
import net.minecraft.resources.ResourceLocation;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.Random;
import java.util.function.IntConsumer;

/** Five by five drafting grid inside a ring of labelled ports. Empty mounts transmit straight through. */
public final class FoldlineGame implements DeskDrawing {
    public static final ResourceLocation ID = new ResourceLocation("frostedresearch", "foldline");
    public static final int STRIDE = 7;
    private static final int[] STEP = {-STRIDE, 1, STRIDE, -1};
    private record Pattern(int[] pads, int[] entries, int[] exits, String draft, String solution) {}
    private static final Pattern[][] PATTERNS = loadPatterns();

    public final int level, budget;
    public final long seed;
    public final int[] pads, entries, exits;
    public final String draft, solution;
    private final int[] mount = new int[STRIDE * STRIDE];
    private final int[] directions;

    private static Pattern[][] loadPatterns() {
        var file = FoldlineGame.class.getResourceAsStream("/data/frostedresearch/foldline/patterns.json");
        try (var reader = new InputStreamReader(Objects.requireNonNull(file), StandardCharsets.UTF_8)) {
            return new Gson().fromJson(reader, Pattern[][].class);
        } catch (java.io.IOException error) {
            throw new java.io.UncheckedIOException(error);
        }
    }

    public FoldlineGame(int level, long seed) {
        this.seed = seed;
        this.level = level;
        budget = 3 + level;
        Random random = new Random(seed);
        Pattern base = PATTERNS[level][random.nextInt(PATTERNS[level].length)];
        int[] x = axis(random), y = axis(random);
        boolean transpose = random.nextBoolean();
        boolean flipMirror = (x[0] > x[2]) != (y[0] > y[2]);
        draft = orient(base.draft, flipMirror);
        solution = orient(base.solution, flipMirror);
        pads = Arrays.stream(base.pads).map(p -> cell(x[p % 3], y[p / 3], transpose)).toArray();
        entries = Arrays.stream(base.entries).map(p -> port(p, x, y, transpose)).toArray();
        exits = Arrays.stream(base.exits).map(p -> port(p, x, y, transpose)).toArray();
        directions = Arrays.stream(entries).map(p -> {
            if (p / STRIDE == 0) return 2;
            if (p % STRIDE == 6) return 3;
            return p / STRIDE == 6 ? 0 : 1;
        }).toArray();
        Arrays.fill(mount, -1);
        for (int i = 0; i < pads.length; i++) mount[pads[i]] = i;
    }

    // Choose three distinct lines on each axis. Their order must preserve intersections.
    private static int[] axis(Random random) {
        int[] lines = random.ints(1, 6).distinct().limit(3).sorted().toArray();
        if (random.nextBoolean()) { int swap = lines[0]; lines[0] = lines[2]; lines[2] = swap; }
        return lines;
    }
    private static String orient(String mirrors, boolean flip) {
        return flip ? mirrors.replace('1', 'x').replace('2', '1').replace('x', '2') : mirrors;
    }
    private static int cell(int x, int y, boolean transpose) {
        return transpose ? y + STRIDE * x : x + STRIDE * y;
    }
    private static int port(int port, int[] x, int[] y, boolean transpose) {
        int line = port % 3, left = x[0] < x[2] ? 0 : 6, top = y[0] < y[2] ? 0 : 6;
        return switch (port / 3) {
            case 0 -> cell(x[line], top, transpose);
            case 1 -> cell(6 - left, y[line], transpose);
            case 2 -> cell(x[line], 6 - top, transpose);
            default -> cell(left, y[line], transpose);
        };
    }
    public String initialLayout() { return draft; }
    public boolean valid(String mirrors) {
        return mirrors.length() == pads.length && mirrors.chars().allMatch(c -> c >= '0' && c <= '2');
    }
    public static int used(String mirrors) { return (int) mirrors.chars().filter(c -> c != '0').count(); }

    /** A ray entering from the edge is reversible and always leaves the grid. */
    public int trace(String mirrors, int ray, IntConsumer visit) {
        int position = entries[ray], direction = directions[ray];
        do {
            if (visit != null) visit.accept(position);
            position += STEP[direction];
            int i = mount[position];
            if (i >= 0) {
                char mirror = mirrors.charAt(i);
                if (mirror == '1') direction ^= 1;
                if (mirror == '2') direction ^= 3;
            }
        } while (position % STRIDE != 0 && position % STRIDE != 6 && position / STRIDE != 0 && position / STRIDE != 6);
        if (visit != null) visit.accept(position);
        return position;
    }
    public boolean solved(String mirrors) {
        if (!valid(mirrors) || used(mirrors) > budget) return false;
        for (int ray = 0; ray < entries.length; ray++)
            if (trace(mirrors, ray, null) != exits[ray]) return false;
        return true;
    }
}
