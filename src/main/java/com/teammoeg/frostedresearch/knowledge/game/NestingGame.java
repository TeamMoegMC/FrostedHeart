package com.teammoeg.frostedresearch.knowledge.game;

import net.minecraft.resources.ResourceLocation;
import java.util.*;

/** Connected polyominoes cut from a shuffled walk on a 6 by 5 grid. */
public final class NestingGame implements DeskDrawing {
    public static final ResourceLocation PACK = new ResourceLocation("frostedresearch:nesting_pack");
    public static final ResourceLocation FILL = new ResourceLocation("frostedresearch:nesting_fill");
    public static final int WIDTH = 6, HEIGHT = 5;
    private static final int AREA = WIDTH * HEIGHT;
    public final boolean fill;
    public final int groove;
    public final int[][][] shapes;
    public final String solution, empty;
    private record Piece(int[][] rotations, int placement) {}

    public NestingGame(boolean fill, long seed) {
        this.fill = fill;
        Random rng = new Random(seed);
        List<Integer> walk = new ArrayList<>(AREA);
        for (int y = 0; y < HEIGHT; y++)
            for (int x = 0; x < WIDTH; x++) walk.add(y * WIDTH + (y % 2 == 0 ? x : WIDTH - 1 - x));
        // Reconnect an endpoint to a neighbouring interior cell, keeping every cell in the walk.
        for (int step = 0; step < 80; step++) {
            int index = 2 + rng.nextInt(AREA - 2);
            int a = walk.get(0), b = walk.get(index);
            if (Math.abs(a % WIDTH - b % WIDTH) + Math.abs(a / WIDTH - b / WIDTH) == 1)
                Collections.reverse(walk.subList(0, index));
            if (rng.nextBoolean()) Collections.reverse(walk);
        }
        int count = 4 + rng.nextInt(2);
        int begin = rng.nextInt(AREA - 4 * count - 2), cursor = begin;
        var pieces = new ArrayList<Piece>();
        for (int i = 0; i < count; i++) {
            int length = 3 + rng.nextInt(2);
            pieces.add(piece(walk.subList(cursor, cursor + length), rng, false));
            cursor += length;
        }
        if (fill) {
            int start = begin + rng.nextInt(cursor - begin - 3);
            pieces.add(piece(walk.subList(start, start + 4), rng, true));
        } else cursor += 2;
        int mask = 0;
        for (int i = begin; i < cursor; i++) mask |= 1 << walk.get(i);
        groove = mask;
        Collections.shuffle(pieces, rng);
        shapes = pieces.stream().map(Piece::rotations).toArray(int[][][]::new);
        var answer = new StringBuilder();
        pieces.forEach(p -> answer.append(encode(p.placement)));
        solution = answer.toString();
        empty = "00".repeat(pieces.size());
    }

    private static Piece piece(List<Integer> cells, Random rng, boolean spare) {
        int left = cells.stream().mapToInt(c -> c % WIDTH).min().orElseThrow();
        int top = cells.stream().mapToInt(c -> c / WIDTH).min().orElseThrow();
        int[] offsets = cells.stream().mapToInt(c -> c % WIDTH - left + WIDTH * (c / WIDTH - top)).toArray();
        int start = rng.nextInt(4);
        int[][] rotations = new int[4][];
        for (int step = 0; step < 4; step++) {
            rotations[(start + step) % 4] = offsets;
            int bottom = Arrays.stream(offsets).max().orElseThrow() / WIDTH;
            offsets = Arrays.stream(offsets).map(c -> bottom - c / WIDTH + WIDTH * (c % WIDTH)).toArray();
        }
        return new Piece(rotations, spare ? 0 : placement(left, top, start));
    }
    public static int placement(int x, int y, int rotation) {
        if (x < 0 || y < 0 || x >= WIDTH || y >= HEIGHT) return -1;
        return 1 + x + y * WIDTH + rotation * AREA;
    }
    public static int rotation(int placement) { return (placement - 1) / AREA; }
    public static int origin(int placement) { return (placement - 1) % AREA; }
    private static String encode(int p) { return HexFormat.of().toHexDigits((byte)p); }
    public static int at(String layout, int i) { return HexFormat.fromHexDigits(layout, i*2, i*2+2); }
    public static String change(String layout, int i, int p) {
        return layout.substring(0, i*2) + encode(p) + layout.substring(i*2+2);
    }
    public int mask(int i, int p) {
        if (p == 0) return 0;
        if (p < 0 || p > 4 * AREA) return -1;
        int base = origin(p), bits = 0;
        for (int c : shapes[i][rotation(p)]) {
            if (base % WIDTH + c % WIDTH >= WIDTH || base + c >= AREA) return -1;
            bits |= 1 << (base + c);
        }
        return bits;
    }
    public int occupied(String layout) {
        if (layout.length() != 2 * shapes.length || !layout.chars().allMatch(HexFormat::isHexDigit)) return -1;
        int all = 0;
        for (int i = 0; i < shapes.length; i++) {
            int part = mask(i, at(layout,i));
            if ((part & ~groove) != 0 || (part & all) != 0) return -1;
            all |= part;
        }
        return all;
    }
    public String initialLayout() { return empty; }
    public boolean valid(String layout) { return occupied(layout) >= 0; }
    public boolean solved(String layout) {
        int occupied = occupied(layout);
        if (occupied < 0) return false;
        if (fill) return occupied == groove;
        for (int i = 0; i < shapes.length; i++) if (at(layout,i)==0) return false;
        return true;
    }
}
