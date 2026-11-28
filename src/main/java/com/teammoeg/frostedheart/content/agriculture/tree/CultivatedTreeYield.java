package com.teammoeg.frostedheart.content.agriculture.tree;

import java.util.Arrays;
import net.minecraft.util.RandomSource;

/** Compact native wood calibration: only generation uses a few binary-search/interpolation operations. */
public final class CultivatedTreeYield {
    private CultivatedTreeYield() {}
    private static final int[] SIZES = {1, 2, 4, 6, 8, 12, 20, 32};
    // Fancy's integer branch thresholds need extra small-size anchors; this is not a height limit.
    private static final int[] FANCY_SIZES = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 20, 24, 32};
    private static final Curve FANCY_OAK = new Curve(FANCY_SIZES, new double[]{2.0, 3.0, 4.0, 4.0, 5.0, 5.0, 7.1953125, 10.046875, 10.1640625, 13.203125, 16.84375, 20.921875, 25.0859375, 26.7109375, 31.5859375, 36.5703125, 56.3828125, 77.1953125, 139.7890625});
    private static final Curve MEGA_JUNGLE_TREE = new Curve(SIZES, new double[]{1.0, 5.0, 13.0, 21.7421875, 30.640625, 48.8046875, 84.0546875, 137.53125});
    private static final Curve ACACIA = new Curve(SIZES, new double[]{1.0, 2.1328125, 4.859375, 7.3984375, 9.4609375, 13.4609375, 21.4609375, 33.4609375});
    private static final Curve DARK_OAK = new Curve(SIZES, new double[]{4.0, 11.46875, 25.40625, 34.5, 42.5, 58.5, 90.5, 138.5});
    private static final Curve MANGROVE = new Curve(SIZES, new double[]{1.0, 2.3515625, 6.2265625, 9.875, 13.5546875, 20.46875, 34.625, 55.8203125});
    private static final Curve TALL_MANGROVE = new Curve(SIZES, new double[]{1.0, 2.4375, 6.84375, 12.234375, 16.8203125, 26.1796875, 44.203125, 71.203125});
    private static final Curve CHERRY = new Curve(SIZES, new double[]{7.7109375, 8.453125, 11.9140625, 14.8515625, 16.8515625, 20.8515625, 28.8515625, 40.8515625});

    private static Curve curve(String shape) {
        return switch (shape) {
            case "fancy_oak" -> FANCY_OAK;
            case "mega_jungle_tree" -> MEGA_JUNGLE_TREE;
            case "acacia" -> ACACIA;
            case "dark_oak" -> DARK_OAK;
            case "mangrove" -> MANGROVE;
            case "tall_mangrove" -> TALL_MANGROVE;
            case "cherry" -> CHERRY;
            default -> null;
        };
    }
    /** Minimum expected wood in a complete size-one native structure, never a synthetic straight replacement. */
    public static double minimumLogs(String shape) {
        var curve = curve(shape);
        return curve == null ? 1 : curve.logs[0];
    }
    /** Expected trunk plus branch logs for an integer size, including extrapolation beyond vanilla heights. */
    public static double expectedLogs(String shape, int height) {
        var curve = curve(shape);
        if (curve == null) return shape.equals("mega_spruce") || shape.equals("mega_pine") ? 4.0 * height - 3 : height;
        return curve.expected(height, shape.equals("fancy_oak"));
    }
    /** Samples adjacent complete sizes; rounding introduces no systematic bias in the calibrated model. */
    public static int height(String shape, double logs, RandomSource random) {
        var curve = curve(shape);
        double size = curve == null ? (shape.equals("mega_spruce") || shape.equals("mega_pine") ? (logs + 3) / 4 : logs)
                : curve.size(logs, shape.equals("fancy_oak"));
        int low = Math.max(1, (int)Math.floor(size));
        double a = expectedLogs(shape, low), b = expectedLogs(shape, low + 1);
        double chance = b > a ? Math.max(0, Math.min(1, (logs - a) / (b - a))) : 0;
        return low + (random.nextDouble() < chance ? 1 : 0);
    }
    /** Reference: native-tree-yield-reference.csv, 128 independent seeds per integer size. */
    private record Curve(int[] sizes, double[] logs) {
        private int upper(double value, boolean byLogs) {
            int found = byLogs ? Arrays.binarySearch(logs, value) : Arrays.binarySearch(sizes, (int)value);
            return Math.max(1, Math.min(sizes.length - 1, found >= 0 ? found : -found - 1));
        }
        private double quadratic() {
            int n = sizes.length;
            double left = (logs[n - 2] - logs[n - 3]) / (sizes[n - 2] - sizes[n - 3]);
            double right = (logs[n - 1] - logs[n - 2]) / (sizes[n - 1] - sizes[n - 2]);
            return Math.max(0, (right - left) / (sizes[n - 1] - sizes[n - 3]));
        }
        private double slope(boolean fancy) {
            int n = sizes.length;
            double secant = (logs[n - 1] - logs[n - 2]) / (sizes[n - 1] - sizes[n - 2]);
            return secant + (fancy ? quadratic() * (sizes[n - 1] - sizes[n - 2]) : 0);
        }
        double expected(int height, boolean fancy) {
            int last = sizes.length - 1;
            if (height > sizes[last]) {
                double extra = height - sizes[last];
                return logs[last] + extra * slope(fancy) + (fancy ? quadratic() * extra * extra : 0);
            }
            int i = upper(height, false);
            return logs[i - 1] + (height - sizes[i - 1]) * (logs[i] - logs[i - 1]) / (sizes[i] - sizes[i - 1]);
        }
        double size(double target, boolean fancy) {
            int last = sizes.length - 1;
            if (target > logs[last]) {
                double extra = target - logs[last], a = fancy ? quadratic() : 0, b = slope(fancy);
                return sizes[last] + (a > 0 ? 2 * extra / (b + Math.sqrt(b * b + 4 * a * extra)) : extra / b);
            }
            int i = upper(target, true);
            return logs[i] > logs[i - 1] ? sizes[i - 1] + (target - logs[i - 1])
                    * (sizes[i] - sizes[i - 1]) / (logs[i] - logs[i - 1]) : sizes[i];
        }
    }
}
