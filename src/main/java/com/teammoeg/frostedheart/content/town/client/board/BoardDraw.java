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

package com.teammoeg.frostedheart.content.town.client.board;

import java.util.List;

import com.mojang.math.Axis;
import com.teammoeg.chorda.client.icon.CIcons.CTextureIcon;
import com.teammoeg.chorda.client.ui.CGuiHelper;
import com.teammoeg.frostedheart.content.town.ui.view.BoardView.Tone;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * 布告板界面的绘制小工具：文字（截断、换行、放大）、星级、标签、图钉、胶带、霜、冰凌、印戳和镇印。
 * <p>
 * Drawing helpers of the notice-board screens: text that clips, wraps or scales, rating stars, chips, pins, tape,
 * frost, icicles, ink stamps and the town seal.
 */
public final class BoardDraw {
    public static final int LINE = 10;

    private BoardDraw() {
    }

    public static Font font() {
        return Minecraft.getInstance().font;
    }

    public static int toneColor(Tone tone) {
        return switch (tone) {
            case GOOD -> NoticeBoardTheme.GREEN;
            case WARN -> NoticeBoardTheme.AMBER;
            case BAD -> NoticeBoardTheme.RED;
            case MUTED -> NoticeBoardTheme.MUTED;
            default -> NoticeBoardTheme.INK;
        };
    }

    public static int toneFill(Tone tone) {
        return switch (tone) {
            case GOOD -> NoticeBoardTheme.GREEN_FILL;
            case WARN -> NoticeBoardTheme.AMBER;
            case BAD -> NoticeBoardTheme.RED;
            case MUTED -> NoticeBoardTheme.FAINT;
            default -> NoticeBoardTheme.PURPLE;
        };
    }

    /** Draws one line, cut with an ellipsis when wider than {@code width}; returns the width drawn. */
    public static int text(GuiGraphics g, Component text, int x, int y, int width, int color) {
        Font font = font();
        if (width <= 0) return 0;
        if (font.width(text) <= width) {
            g.drawString(font, text, x, y, color, false);
            return font.width(text);
        }
        String ellipsis = "…";
        int room = Math.max(0, width - font.width(ellipsis));
        String cut = font.plainSubstrByWidth(text.getString(), room);
        g.drawString(font, cut + ellipsis, x, y, color, false);
        return width;
    }

    public static void textRight(GuiGraphics g, Component text, int right, int y, int color) {
        g.drawString(font(), text, right - font().width(text), y, color, false);
    }

    public static void textCentered(GuiGraphics g, Component text, int centerX, int y, int color) {
        g.drawString(font(), text, centerX - font().width(text) / 2, y, color, false);
    }

    /** Draws wrapped lines, at most {@code maxLines}; returns the height used. */
    public static int wrapped(GuiGraphics g, Component text, int x, int y, int width, int color, int maxLines) {
        List<FormattedCharSequence> lines = lines(text, width);
        int count = Math.min(lines.size(), maxLines);
        for (int i = 0; i < count; i++)
            g.drawString(font(), lines.get(i), x, y + i * LINE, color, false);
        return count * LINE;
    }

    public static int wrappedHeight(Component text, int width, int maxLines) {
        return Math.min(lines(text, width).size(), maxLines) * LINE;
    }

    /**
     * 按宽度断行，守中文避头尾（见 {@link com.teammoeg.frostedheart.content.town.tabs.TownTextLayout#keepTogether}）。
     * Wraps to the width, keeping the Chinese line-breaking rule (see TownTextLayout.keepTogether).
     */
    public static List<FormattedCharSequence> lines(Component text, int width) {
        return com.teammoeg.frostedheart.content.town.tabs.TownTextLayout.breakLines(font(), text, Math.max(10, width));
    }

    /** A number or short word at twice the size, for the big figure of a stat note. */
    public static void big(GuiGraphics g, Component text, int x, int y, int color) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(2, 2, 1);
        g.drawString(font(), text, 0, 0, color, false);
        g.pose().popPose();
    }

    public static int bigWidth(Component text) {
        return font().width(text) * 2;
    }

    /** The paper strip under captions on the bare board, and the thin shadow below it. */
    private static final int LABEL_PAPER = 0xFFEDE1C7, LABEL_SHADOW = 0x40000000;

    /**
     * 木板上的说明文字：贴一条纸签，用深色墨水写，和便签上的字一样清楚。原先写在木头上的浅色字换成对应的墨水色。
     * <p>
     * A caption on the bare wood, written in dark ink on a strip of paper so it reads as clearly as the notes; the light
     * colours once used for text on wood become their ink equivalents.
     */
    public static void textOnWood(GuiGraphics g, Component text, int x, int y, int width, int color) {
        int w = Math.min(width, font().width(text)) + 6;
        g.fill(x - 3, y - 2, x - 3 + w, y + 9, LABEL_PAPER);
        g.fill(x - 3, y + 9, x - 3 + w, y + 10, LABEL_SHADOW);
        text(g, text, x, y, width, inkOnPaper(color));
    }

    public static void textOnWood(GuiGraphics g, Component text, int x, int y, int color) {
        textOnWood(g, text, x, y, font().width(text), color);
    }

    public static void textOnWood(GuiGraphics g, String text, int x, int y, int color) {
        textOnWood(g, Component.literal(text), x, y, font().width(text), color);
    }

    private static int inkOnPaper(int color) {
        if (color == NoticeBoardTheme.ON_WOOD) return NoticeBoardTheme.INK;
        if (color == 0xFFFAC775) return NoticeBoardTheme.AMBER; // the board's light amber for warnings on wood
        return color;
    }

    /**
     * 画在面板空白处的一张黄色说明便签，一条一行、悬挂缩进；放得下才画，返回高度（放不下为 0）。
     * <p>
     * A yellow how-to note drawn into a panel's empty space, one bullet per line with a hanging indent; drawn only when
     * it fits, returning its height (0 when it does not).
     */
    public static int helpNote(GuiGraphics g, Component title, java.util.List<Component> lines, int x, int y, int w, int maxH) {
        int indent = font().width("·\u00A0"), h = 8 + 12 + 6;
        for (Component line : lines) h += wrappedHeight(line, w - 18 - indent, 3) + 2;
        if (h > maxH || w < 120) return 0;
        NoticeBoardIcons.NOTE.draw(g, x, y, w, h);
        tape(g, x + w / 2 - 12, y - 4, -4);
        int px = x + 9, py = y + 8;
        text(g, title.copy().withStyle(net.minecraft.ChatFormatting.BOLD), px, py, w - 18, NoticeBoardTheme.NOTE_INK);
        py += 12;
        for (Component line : lines) {
            text(g, Component.literal("·"), px, py, indent, NoticeBoardTheme.NOTE_INK);
            py += wrapped(g, line, px + indent, py, w - 18 - indent, NoticeBoardTheme.NOTE_INK, 3) + 2;
        }
        return h;
    }

    public static void stars(GuiGraphics g, int x, int y, int filled) {
        for (int i = 0; i < 5; i++)
            (i < filled ? NoticeBoardIcons.STAR_FULL : NoticeBoardIcons.STAR_EMPTY).draw(g, x + i * 8, y, 8, 8);
    }

    /** A small outlined label; returns its width. */
    public static int chip(GuiGraphics g, Component text, int x, int y, int color) {
        return chip(g, text, x, y, color, 0);
    }

    /** {@link #chip} with {@code extra} pixels of room after the text, e.g. for a star rating; returns its width. */
    public static int chip(GuiGraphics g, Component text, int x, int y, int color, int extra) {
        int w = font().width(text) + 6 + extra;
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + 10, x + w, y + 11, color);
        g.fill(x, y, x + 1, y + 11, color);
        g.fill(x + w - 1, y, x + w, y + 11, color);
        g.drawString(font(), text, x + 3, y + 2, color, false);
        return w;
    }

    public static int chipWidth(Component text) {
        return font().width(text) + 6;
    }

    /**
     * 一支 8×8 像素的小铅笔，笔尖朝左下；列表里的改名按钮用它，悬停时笔杆亮一点。
     * A small pencil of 8 by 8 pixels, tip to the lower left, for rename buttons in lists; its shaft brightens on hover.
     */
    public static void pencil(GuiGraphics g, int x, int y, boolean hover) {
        int shaft = hover ? 0xFFF2B544 : 0xFFD9A23A, shade = 0xFF9A6424;
        for (int i = 1; i <= 5; i++) {
            g.fill(x + i, y + 6 - i, x + i + 2, y + 8 - i, shaft);
            g.fill(x + i, y + 7 - i, x + i + 1, y + 8 - i, shade);
        }
        g.fill(x + 6, y, x + 8, y + 2, 0xFFC8645C);
        g.fill(x, y + 6, x + 2, y + 8, 0xFFF0D6A6);
        g.fill(x, y + 7, x + 1, y + 8, NoticeBoardTheme.INK);
    }

    public static void pin(GuiGraphics g, CTextureIcon pin, int centerX, int y) {
        pin.draw(g, centerX - 4, y - 4, 8, 8);
    }

    public static void tape(GuiGraphics g, int x, int y, float degrees) {
        g.pose().pushPose();
        g.pose().translate(x + 12, y + 4, 0);
        g.pose().mulPose(Axis.ZP.rotationDegrees(degrees));
        NoticeBoardIcons.TAPE.draw(g, -12, -4, 24, 8);
        g.pose().popPose();
    }

    /** Frost along the edge of a paper; level 0 draws nothing. Icicles hang under it from level 2. */
    public static void frost(GuiGraphics g, int x, int y, int w, int h, int level) {
        frost(g, x, y, w, h, FrostFade.shown(level));
    }

    /**
     * 两级之间的霜：相邻两层霜交叉淡入淡出，冰凌按比例长出或缩回，所以结霜和化开都是连续的。
     * Frost between two levels: neighbouring overlays cross-fade and the icicles grow or draw back in proportion, so
     * freezing and thawing are both continuous.
     */
    public static void frost(GuiGraphics g, int x, int y, int w, int h, float level) {
        float clamped = Math.min(3f, level);
        if (clamped <= 0.02f) return;
        int lower = (int) clamped;
        float part = clamped - lower;
        if (lower >= 1) frostLayer(g, lower, x, y, w, h, lower < 3 ? 1 - part : 1);
        if (lower < 3 && part > 0.02f) frostLayer(g, lower + 1, x, y, w, h, part);
        // Icicles: none at level 1, five pixels long at level 2 and eight at level 3, growing in between.
        int length = Math.round(clamped <= 1 ? 0 : clamped <= 2 ? (clamped - 1) * 5 : 5 + (clamped - 2) * 3);
        if (length <= 0) return;
        int strip = Math.max(0, w - 8);
        for (int sx = 0; sx < strip; sx += 32) {
            int piece = Math.min(32, strip - sx);
            NoticeBoardIcons.ICICLES.asPart(0, 0, piece, 8).draw(g, x + 4 + sx, y + h, piece, length);
        }
    }

    private static void frostLayer(GuiGraphics g, int level, int x, int y, int w, int h, float alpha) {
        if (alpha >= 0.99f) NoticeBoardIcons.FROST[level].draw(g, x - 2, y - 2, w + 4, h + 4);
        else if (alpha > 0.02f)
            NoticeBoardIcons.FROST[level].drawTinted(g, x - 2, y - 2, w + 4, h + 4, (Math.round(alpha * 255) << 24) | 0xFFFFFF);
    }

    /**
     * 一枚倾斜的墨印：圆形或方形印框加文字，按颜色染色。{@code scale} 用于盖章动画。
     * A tilted ink stamp: a round or square frame with text, tinted; {@code scale} drives the stamping animation.
     */
    public static void stamp(GuiGraphics g, Component text, int centerX, int centerY, int color, boolean round,
                             float degrees, float scale) {
        Font font = font();
        int textWidth = font.width(text);
        int w = round ? Math.max(24, textWidth + 10) : textWidth + 8;
        int h = round ? w : 13;
        g.pose().pushPose();
        g.pose().translate(centerX, centerY, 50);
        g.pose().mulPose(Axis.ZP.rotationDegrees(degrees));
        g.pose().scale(scale, scale, 1);
        if (round) NoticeBoardIcons.STAMP_ROUND.drawTinted(g, -w / 2, -h / 2, w, h, color);
        else {
            g.fill(-w / 2, -h / 2, w / 2, -h / 2 + 2, color);
            g.fill(-w / 2, h / 2 - 2, w / 2, h / 2, color);
            g.fill(-w / 2, -h / 2, -w / 2 + 2, h / 2, color);
            g.fill(w / 2 - 2, -h / 2, w / 2, h / 2, color);
        }
        g.drawString(font, text, -textWidth / 2, -4, color, false);
        g.pose().popPose();
        CGuiHelper.resetGuiDrawing();
    }

    /** The town's square seal: the first two characters of its name and 镇印, read right to left. */
    public static void seal(GuiGraphics g, String townName, int x, int y, int size) {
        String[] marks = sealMarks(townName);
        String a = marks[0], b = marks[1];
        NoticeBoardIcons.STAMP_SQUARE.drawTinted(g, x, y, size, size, NoticeBoardTheme.RED);
        Font font = font();
        int half = size / 2;
        g.pose().pushPose();
        float scale = Math.min(1f, (half - 3) / 9f);
        g.pose().translate(x, y, 1);
        g.pose().scale(scale, scale, 1);
        float inv = 1f / scale;
        // right column top to bottom: the name; left column: 镇印
        drawSealChar(g, font, a, (half + half / 2f) * inv, (half / 2f + 1) * inv);
        drawSealChar(g, font, b, (half + half / 2f) * inv, (half + half / 2f) * inv);
        drawSealChar(g, font, "镇", (half / 2f) * inv, (half / 2f + 1) * inv);
        drawSealChar(g, font, "印", (half / 2f) * inv, (half + half / 2f) * inv);
        g.pose().popPose();
    }

    /**
     * 镇印上的两个字：中文镇名取前两个汉字；其他镇名取前两个词的首字母。
     * The seal's two marks: the first two Chinese characters of the name, otherwise the initials of its first two words.
     */
    public static String[] sealMarks(String townName) {
        String name = townName == null || townName.isBlank() ? "城镇" : townName.strip();
        int[] ideographs = name.codePoints().filter(Character::isIdeographic).limit(2).toArray();
        if (ideographs.length == 2) return new String[]{new String(Character.toChars(ideographs[0])), new String(Character.toChars(ideographs[1]))};
        if (ideographs.length == 1) return new String[]{new String(Character.toChars(ideographs[0])), "城"};
        String[] words = name.split("\\s+");
        String first = words[0].isEmpty() ? "T" : words[0].substring(0, 1).toUpperCase(java.util.Locale.ROOT);
        String second = words.length > 1 && !words[1].isEmpty() ? words[1].substring(0, 1)
                : words[0].length() > 1 ? words[0].substring(1, 2) : "";
        return new String[]{first, second.toUpperCase(java.util.Locale.ROOT)};
    }

    private static void drawSealChar(GuiGraphics g, Font font, String ch, float cx, float cy) {
        g.drawString(font, ch, (int) (cx - font.width(ch) / 2f), (int) (cy - 4.5f), NoticeBoardTheme.RED, false);
    }
}
