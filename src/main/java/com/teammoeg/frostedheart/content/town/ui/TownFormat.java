/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.teammoeg.frostedheart.content.town.ui;

import java.util.Locale;

import net.minecraft.network.chat.Component;

/**
 * 城镇界面玩家视图的数字规则：整数优先，能力向下取整，进度不提前显示 100%，合格与否始终按原始值判断。
 * 所有城镇界面都用这里的函数，保证同一个量处处一样。
 * <p>
 * Number rules for the player view of town screens. Whole numbers only; capacities round down; progress never
 * shows 100% before it is complete; pass and fail are always decided on the raw value, and a shown number never
 * contradicts that decision. Every town screen formats through here so one quantity reads the same everywhere.
 */
public final class TownFormat {
    public static final String KEY = "gui.frostedheart.board.";

    /**
     * 带数量的文案：第一个数是 1、并且有「.one」这一条时用它。英文借此写「1 resident」而不是「1 residents」；中文的 .one 和原句
     * 一样。只有第一个数是数量的文案才写 .one，所以别的句子不受影响。
     * <p>
     * A translation with a count: when its first number is one and a ".one" form exists, that form is used. English uses
     * it for "1 resident" rather than "1 residents"; the Chinese ".one" forms repeat the plain text. Only strings whose
     * first number is the count define ".one", so other strings are unaffected.
     */
    public static net.minecraft.network.chat.MutableComponent counted(String key, Object... args) {
        for (Object arg : args) {
            // Counts arrive as numbers, as digit strings, or as components that read as plain digits (hands, say).
            String digits = arg instanceof String text ? text : arg instanceof Component component ? component.getString() : null;
            Long value = arg instanceof Integer || arg instanceof Long || arg instanceof Short ? Long.valueOf(((Number) arg).longValue())
                    : digits != null && digits.matches("\\d+") ? Long.valueOf(digits) : null;
            if (value == null) {
                if (arg instanceof Number) break;
                continue;
            }
            if (value == 1 && net.minecraft.locale.Language.getInstance().has(key + ".one"))
                return Component.translatable(key + ".one", args);
            break;
        }
        return Component.translatable(key, args);
    }
    private static final double EPSILON = 1e-6;

    private TownFormat() {
    }

    /**
     * 按效率折算的人手与产出（人手、运力、捕猎、开采），向下取整。
     * Effective workforce or output (hands, transport capacity, hunts, ore) rounded down.
     */
    public static long whole(double value) {
        return Double.isFinite(value) && value > 0 ? (long) Math.floor(value + 1e-9) : 0;
    }

    /** More than nothing but less than one whole unit. */
    public static boolean underOne(double value) {
        return Double.isFinite(value) && value > EPSILON && value < 1 - 1e-9;
    }

    /** {@link #whole} as text, or "under 1" when there is some but less than one. */
    public static Component hands(double value) {
        return underOne(value) ? Component.translatable(KEY + "less_than_one") : Component.literal(Long.toString(whole(value)));
    }

    /**
     * 个人效率：折合的标准工人数 × 100，向下取整。
     * Personal efficiency: the resident's standard workers times 100, rounded down.
     */
    public static int efficiency(double standardWorkers) {
        return Double.isFinite(standardWorkers) && standardWorkers > 0 ? (int) Math.floor(standardWorkers * 100 + EPSILON) : 0;
    }

    /**
     * 进度百分比：向下取整，没完成时最多 99。
     * Progress as a whole percentage, rounded down; at most 99 until it is complete.
     */
    public static int percent(double fraction) {
        if (!Double.isFinite(fraction) || fraction <= 0) return 0;
        if (fraction >= 1 - EPSILON) return 100;
        return Math.min(99, (int) Math.floor(fraction * 100 + 1e-9));
    }

    public static int percent(double done, double total) {
        if (!(total > 0)) return done > 0 ? 100 : 0;
        return percent(done / total);
    }

    /**
     * 0–1 的质量换成五档星级，四舍五入。
     * A 0–1 quality as five stars, rounded to the nearest star.
     */
    public static int stars(double fraction) {
        if (!Double.isFinite(fraction)) return 0;
        return (int) Math.max(0, Math.min(5, Math.round(Math.max(0, Math.min(1, fraction)) * 5)));
    }

    /** How warm a working room is against its minimum (and maximum) temperature. */
    public enum Warmth {
        WARM, COOL, COLD, HOT,
        /** A livable home that is not warm enough to be comfortable. */
        CHILLY,
        /** A livable home that is too warm to be comfortable. */
        STUFFY;

        public Component text() {
            return Component.translatable(KEY + "warmth." + name().toLowerCase(Locale.ROOT));
        }

        /** The word a home uses, the same on its board, its chip, its comfort line and its residents' cards. */
        public Component homeText() {
            return Component.translatable(KEY + "warmth.home." + name().toLowerCase(Locale.ROOT));
        }

        public boolean failing() {
            return this == COLD || this == HOT;
        }
    }

    /** A temperature as shown: a whole number that never contradicts {@code warmth}. */
    public record Temperature(int shown, Warmth warmth) {
        public Component text() {
            return Component.translatable(KEY + "celsius", shown);
        }
    }

    /**
     * 室温：不合格时向不利方向取整（低于下限向下、高于上限向上），合格时四舍五入且不会越过界线；
     * 离下限不到 {@code coolMargin} 时为「偏冷」。
     * <p>
     * A room temperature against a working range. Failing values round toward the failing side; passing values round
     * to nearest but never across a bound, so 9.9 °C against 10 °C reads 9, not 10. Within {@code coolMargin} of the
     * minimum the room is cool.
     */
    public static Temperature temperature(double celsius, double minimum, double maximum, double coolMargin) {
        if (!Double.isFinite(celsius)) return new Temperature(0, Warmth.COLD);
        if (celsius < minimum) return new Temperature((int) Math.floor(celsius), Warmth.COLD);
        if (celsius > maximum) return new Temperature((int) Math.ceil(celsius), Warmth.HOT);
        int shown = (int) Math.round(celsius);
        if (shown < minimum) shown = (int) Math.ceil(celsius);
        if (shown > maximum) shown = (int) Math.floor(celsius);
        return new Temperature(shown, celsius < minimum + coolMargin ? Warmth.COOL : Warmth.WARM);
    }

    /** How close to its peak the temperature's comfort rating must be for a home to read warm. */
    public static final double HOME_COMFORT_RATING = 0.75;

    /**
     * 住宅的冷暖，全板只用一个词：先看能不能住（太冷、太热），再看离下限近不近（偏冷），最后看舒不舒服（不够暖和、
     * 有点热）；都过关才算暖和。
     * <p>
     * A home's warmth in the one word its board, chip, comfort line and residents' cards share: livable first (too
     * cold, too hot), then how near the cold limit (chilly), then comfort (not warm enough, a bit warm), and warm only
     * when all of them pass. {@code comfortRating} is the temperature's own comfort rating, 1 at the comfortable value.
     */
    public static Temperature homeTemperature(double celsius, double minimum, double maximum, double coolMargin,
                                              double comfortRating, double comfortable) {
        Temperature base = temperature(celsius, minimum, maximum, coolMargin);
        if (base.warmth() != Warmth.WARM || comfortRating >= HOME_COMFORT_RATING) return base;
        return new Temperature(base.shown(), celsius < comfortable ? Warmth.CHILLY : Warmth.STUFFY);
    }

    /** {@link #temperature(double, double, double, double)} with no upper bound. */
    public static Temperature temperature(double celsius, double minimum, double coolMargin) {
        return temperature(celsius, minimum, Double.POSITIVE_INFINITY, coolMargin);
    }

    /**
     * 霜冻等级 0–3：m = 室温 − 最低工作温度；m ≥ 5 为 0，0 ≤ m < 5 为 1，−5 ≤ m < 0 为 2，更冷为 3。
     * <p>
     * Frost level from the margin above the minimum working temperature: clean at 5 °C or more above, corner frost
     * just above, edge frost with icicles just below, heavy frost further below.
     */
    public static int frost(double celsius, double minimum) {
        double margin = celsius - minimum;
        if (Double.isNaN(margin)) return 0;
        if (margin >= 5) return 0;
        if (margin >= 0) return 1;
        if (margin >= -5) return 2;
        return 3;
    }

    /** Lamp light in a room: too dark, enough, or bright. */
    public enum Light {
        DIM, ENOUGH, BRIGHT;

        public Component text() {
            return Component.translatable(KEY + "light." + name().toLowerCase(Locale.ROOT));
        }
    }

    /** Dim below the minimum (and when unknown), bright from four levels above it. */
    public static Light light(double average, double minimum) {
        if (!(average >= minimum)) return Light.DIM;
        return average >= minimum + 4 ? Light.BRIGHT : Light.ENOUGH;
    }

    /** A daily chance as a word; the percentage itself belongs to the debug view. */
    public enum Chance {
        VERY_LOW, LOW, MEDIUM, HIGH, VERY_HIGH;

        public Component text() {
            return Component.translatable(KEY + "chance." + name().toLowerCase(Locale.ROOT));
        }
    }

    /** 20% steps: below 20% very low, then low, medium, high, and very high from 80%. */
    public static Chance chance(double probability) {
        if (!(probability >= 0.2)) return Chance.VERY_LOW;
        if (probability < 0.4) return Chance.LOW;
        if (probability < 0.6) return Chance.MEDIUM;
        if (probability < 0.8) return Chance.HIGH;
        return Chance.VERY_HIGH;
    }

    /** Filled steps of a five-step chance meter: none for no chance, one more for every started fifth. */
    public static int chanceSteps(double probability) {
        if (!(probability > EPSILON)) return 0;
        return (int) Math.max(1, Math.min(5, Math.ceil(probability * 5 - 1e-9)));
    }

    /**
     * 物品个数缩写（向下取整，不出现小数）：999、1k、12k、123k、1M。
     * A compact item count with no decimals: 999, 1k, 12k, 123k, 1M, all rounded down.
     */
    public static String compact(long count) {
        if (count < 1000) return Long.toString(Math.max(0, count));
        if (count < 1_000_000) return (count / 1000) + "k";
        return (count / 1_000_000) + "M";
    }

    /**
     * 有空间时的物品个数：十万以下照实写，再大才缩写成 123k、1M。
     * An item count where there is room: exact below 100,000, then abbreviated like {@link #compact}.
     */
    public static String count(long count) {
        if (count < 100_000) return Long.toString(Math.max(0, count));
        return compact(count);
    }

    /** Whole days a stock lasts at a daily use, rounded down; -1 when nothing is used. */
    public static int days(double stock, double perDay) {
        if (!(perDay > EPSILON)) return -1;
        if (!(stock > 0)) return 0;
        return (int) Math.floor(stock / perDay + 1e-9);
    }
}
