package com.teammoeg.frostedheart.content.town.labour;
import java.util.Arrays;

/**
 * 等级消耗表。costs[level] 表示该等级所需的数值，必须严格单调递增，且 costs[0] == 0。
 */
public final class LevelCostTable {

    private final int[] costs;

    public LevelCostTable(int[] costs) {
        if (costs == null || costs.length == 0) {
            throw new IllegalArgumentException("costs must not be empty");
        }
        if (costs[0] != 0) {
            throw new IllegalArgumentException("level 0 must cost 0");
        }
        for (int i = 1; i < costs.length; i++) {
            if (costs[i] <= costs[i - 1]) {
                throw new IllegalArgumentException("costs must be strictly increasing");
            }
        }
        this.costs = Arrays.copyOf(costs, costs.length);
    }

    public int maxLevel() {
        return costs.length - 1;
    }

    public int cost(int level) {
        if (level <= 0) return 0;
        if (level >= costs.length) level = costs.length - 1;
        return costs[level];
    }

    /**
     * 二分查找：返回消耗 <= value 的最大等级。
     */
    public int maxLevelForValue(int value) {
        if (value <= 0) return 0;
        int lo = 0, hi = costs.length - 1, result = 0;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            if (costs[mid] <= value) {
                result = mid;
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }
        return result;
    }
}