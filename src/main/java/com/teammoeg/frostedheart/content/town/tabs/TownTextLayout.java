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

package com.teammoeg.frostedheart.content.town.tabs;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/**
 * Shared text layout rules for compact town GUIs.
 */
public final class TownTextLayout {
    private static final String ELLIPSIS = "…";

    private TownTextLayout() {}

    /**
     * Wraps a styled component without converting it to a plain string.
     * An explicitly empty component still occupies one visual line.
     */
    public static List<FormattedCharSequence> wrap(
            Font font, Component text, int maximumWidth
    ) {
        if (text == null) return List.of();
        if (text.getString().isEmpty()) {
            return List.of(FormattedCharSequence.EMPTY);
        }
        List<FormattedCharSequence> wrapped = breakLines(font, text, Math.max(1, maximumWidth));
        return wrapped.isEmpty()
                ? List.of(FormattedCharSequence.EMPTY)
                : wrapped;
    }

    /**
     * 中文排版的断行。含中文的文字逐字排满一行：任意两个汉字之间、空格之后、中西文交界处都可以断，数字和西文单词不拆开，
     * 不换行空格两边不断，右标点不放行首，左括号不留行尾；最后一行只剩一个字时，从上一行借一个字。纯西文仍按单词断行。
     * <p>
     * Line breaking for Chinese: text with Chinese fills each line character by character. It may break between any two
     * Chinese characters, after a space and at a Chinese/Latin seam; numbers and Latin words stay whole, nothing breaks at
     * a no-break space, closing punctuation never opens a line and opening brackets never end one; a last line of one
     * character borrows one from the line above. Text without Chinese keeps word wrapping.
     */
    public static List<FormattedCharSequence> breakLines(Font font, Component text, int maximumWidth) {
        List<Glyph> glyphs = new java.util.ArrayList<>();
        text.getVisualOrderText().accept((index, style, codePoint) -> {
            glyphs.add(new Glyph(codePoint, style));
            return true;
        });
        if (glyphs.stream().noneMatch(glyph -> isCjk(glyph.codePoint()))) return keepTogether(font.split(text, maximumWidth));
        List<List<Glyph>> rows = new java.util.ArrayList<>();
        int start = 0, count = glyphs.size();
        while (start < count) {
            while (start < count && glyphs.get(start).codePoint() == ' ') start++;
            if (start >= count) break;
            int width = 0, lastBreak = -1, end = start;
            boolean overflow = false;
            for (; end < count; end++) {
                Glyph glyph = glyphs.get(end);
                if (glyph.codePoint() == '\n') break;
                if (end > start && canBreakBetween(glyphs.get(end - 1).codePoint(), glyph.codePoint())) lastBreak = end;
                int advance = font.width(FormattedCharSequence.codepoint(glyph.codePoint(), glyph.style()));
                if (width + advance > maximumWidth && end > start) {
                    overflow = true;
                    break;
                }
                width += advance;
            }
            int cut = overflow && lastBreak > start ? lastBreak : end;
            List<Glyph> row = new java.util.ArrayList<>(glyphs.subList(start, cut));
            while (!row.isEmpty() && row.get(row.size() - 1).codePoint() == ' ') row.remove(row.size() - 1);
            rows.add(row);
            start = cut < count && glyphs.get(cut).codePoint() == '\n' ? cut + 1 : cut;
        }
        if (rows.size() >= 2) {
            // No short tail: a last line of one or two characters takes characters from the line above until it holds
            // three, so a word such as 挨饿 is not left alone below. Only Han characters move, and only where a break is
            // allowed; the line above only shortens and the last stays short, so neither can overflow.
            List<Glyph> last = rows.get(rows.size() - 1), above = rows.get(rows.size() - 2);
            while (letters(last) < 3 && above.size() >= 8 && !last.isEmpty()
                    && isHan(above.get(above.size() - 1).codePoint()) && isHan(last.get(0).codePoint())
                    && canBreakBetween(above.get(above.size() - 2).codePoint(), above.get(above.size() - 1).codePoint()))
                last.add(0, above.remove(above.size() - 1));
        }
        List<FormattedCharSequence> out = new java.util.ArrayList<>(rows.size());
        for (List<Glyph> row : rows) {
            List<Glyph> fixed = List.copyOf(row);
            out.add(sink -> {
                for (int index = 0; index < fixed.size(); index++)
                    if (!sink.accept(index, fixed.get(index).style(), fixed.get(index).codePoint())) return false;
                return true;
            });
        }
        return out;
    }

    /** Glyphs that are not closing punctuation: what reads as the line's own text. */
    private static int letters(List<Glyph> row) {
        return (int) row.stream().filter(glyph -> NO_LINE_START.indexOf(glyph.codePoint()) < 0).count();
    }

    private static boolean canBreakBetween(int before, int after) {
        if (before == '\u00A0' || after == '\u00A0') return false;
        if (NO_LINE_START.indexOf(after) >= 0 || NO_LINE_END.indexOf(before) >= 0) return false;
        if (before == ' ') return true;
        if (after == ' ') return false;
        return isCjk(before) || isCjk(after);
    }

    /** Chinese characters, CJK and full-width punctuation, kana: text that breaks between any two characters. */
    private static boolean isCjk(int codePoint) {
        return codePoint >= 0x2E80 && codePoint <= 0x9FFF || codePoint >= 0xF900 && codePoint <= 0xFAFF || codePoint >= 0xFF00 && codePoint <= 0xFFEF;
    }

    /** Punctuation that may not open a line, and brackets that may not close one. */
    private static final String NO_LINE_START = "。，、；：？！）」』》〉】”’….,;:!?)]%", NO_LINE_END = "（「『《〈【“‘([";

    /**
     * 守中文的避头尾：句号、逗号、右括号等不放在行首，挂到上一行行尾（悬挂标点，落在纸边的留白里）；左括号、左引号不留在
     * 行尾，移到下一行行首；只剩标点的一行整行并上去。样式逐字保留。
     * <p>
     * Keeps the Chinese line-breaking rule on wrapped lines: closing punctuation never opens a line but hangs at the end of
     * the previous one, in the paper's margin; opening brackets and quotes never end a line but move to the next; a line
     * of punctuation alone joins the one above. Styles are kept glyph by glyph.
     */
    public static List<FormattedCharSequence> keepTogether(List<FormattedCharSequence> split) {
        if (split.size() < 2) return split;
        List<List<Glyph>> rows = new java.util.ArrayList<>();
        for (FormattedCharSequence line : split) {
            List<Glyph> glyphs = new java.util.ArrayList<>();
            line.accept((index, style, codePoint) -> glyphs.add(new Glyph(codePoint, style)));
            rows.add(glyphs);
        }
        boolean moved = false;
        for (int i = 1; i < rows.size(); i++) {
            List<Glyph> previous = rows.get(i - 1), current = rows.get(i);
            while (!current.isEmpty() && NO_LINE_START.indexOf(current.get(0).codePoint()) >= 0) {
                previous.add(current.remove(0));
                moved = true;
            }
            while (previous.size() > 1 && NO_LINE_END.indexOf(previous.get(previous.size() - 1).codePoint()) >= 0) {
                current.add(0, previous.remove(previous.size() - 1));
                moved = true;
            }
        }
        // No widow: a last line of one character and its punctuation takes the character before it as company, so a
        // word such as 联想 is not split with half of it alone below. The last line only grows, so it cannot overflow.
        List<Glyph> last = rows.get(rows.size() - 1), above = rows.get(rows.size() - 2);
        int letters = (int) last.stream().filter(glyph -> NO_LINE_START.indexOf(glyph.codePoint()) < 0 && !Character.isWhitespace(glyph.codePoint())).count();
        if (letters == 1 && above.size() >= 6 && isHan(above.get(above.size() - 1).codePoint()) && isHan(last.get(0).codePoint())) {
            last.add(0, above.remove(above.size() - 1));
            moved = true;
        }
        if (!moved) return split;
        List<FormattedCharSequence> out = new java.util.ArrayList<>(rows.size());
        for (List<Glyph> row : rows) {
            if (row.isEmpty()) continue;
            List<Glyph> fixed = List.copyOf(row);
            out.add(sink -> {
                for (int index = 0; index < fixed.size(); index++)
                    if (!sink.accept(index, fixed.get(index).style(), fixed.get(index).codePoint())) return false;
                return true;
            });
        }
        return out;
    }

    private static boolean isHan(int codePoint) {
        return codePoint >= 0x4E00 && codePoint <= 0x9FFF;
    }

    private record Glyph(int codePoint, net.minecraft.network.chat.Style style) {
    }

    /**
     * Keeps selector labels on one line and makes truncation visible.
     */
    public static String ellipsize(Font font, String value, int maximumWidth) {
        if (value == null || maximumWidth <= 0) return "";
        if (font.width(value) <= maximumWidth) return value;
        int textWidth = maximumWidth - font.width(ELLIPSIS);
        if (textWidth <= 0) {
            return font.plainSubstrByWidth(ELLIPSIS, maximumWidth);
        }
        return font.plainSubstrByWidth(value, textWidth) + ELLIPSIS;
    }
}
