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
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.chorda.client.cui.base.UILayer;
import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.chorda.client.icon.CIcons.CTextureIcon;
import com.teammoeg.frostedheart.content.town.ui.TownFormat;
import com.teammoeg.frostedheart.content.town.ui.view.BoardView.*;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * 布告板界面的共用摆件：页头告示、关键数字纸片、工作条件、进度、便签叠、照片条、员工页、物品清单、小贴士和调试页。
 * 各建筑界面用它们拼出自己的页面，保证同类信息在每个界面的位置、颜色和手感都一样。
 * <p>
 * Shared pieces of the notice-board screens: the header notice, key-figure notes, working conditions, progress,
 * the note stack, a photo strip, the workers tab, item lists, a tips card and the debug tab. Every building screen
 * is assembled from them, so the same kind of information sits, reads and feels the same everywhere.
 */
public final class BoardWidgets {
    public static final int HEADER_TALL = 30, HEADER_SLIM = 22;

    private BoardWidgets() {
    }

    private static Component b(String key, Object... args) {
        return com.teammoeg.frostedheart.content.town.ui.TownFormat.counted(TownFormat.KEY + key, args);
    }

    public static int statusColor(Status status) {
        return switch (status) {
            case RUNNING -> NoticeBoardTheme.GREEN_FILL;
            case LIMITED -> NoticeBoardTheme.AMBER;
            case STOPPED -> NoticeBoardTheme.RED;
        };
    }

    /** Frost of the smaller sheets on a frozen board: one level below the header's. */
    public static int sheetFrost(int headerFrost) {
        return Math.max(0, headerFrost - 1);
    }

    // ------------------------------------------------------------------ header

    /**
     * 页头告示：图标、名称、副行、状态印戳（悬停列出原因）、镇印和霜。
     * The header notice: icon, name, subtitle, status stamp (hover lists the reasons), town seal and frost.
     *
     * @param stampScale 印戳缩放，用于状态变化时的盖章动画 / stamp scale for the stamping animation
     */
    public static PaperCard header(UILayer layer, int w, boolean tall, Header header, CIcons.CIcon icon, String townName,
                                   Supplier<Float> stampScale) {
        return header(layer, w, tall, header, icon, townName, stampScale, TownFormat.KEY + "status.");
    }

    /** {@link #header} with the status stamp worded from {@code statusKey}, e.g. 宜居 for a house instead of 运转. */
    public static PaperCard header(UILayer layer, int w, boolean tall, Header header, CIcons.CIcon icon, String townName,
                                   Supplier<Float> stampScale, String statusKey) {
        int h = tall ? HEADER_TALL : HEADER_SLIM;
        PaperCard card = new PaperCard(layer).tilt(-0.3f).frost(header::frost).painter((g, x, y, cw, ch) -> {
            icon.draw(g, x + 6, y + (ch - 16) / 2, 16, 16);
            BoardDraw.text(g, header.name().copy().withStyle(ChatFormatting.BOLD), x + 26, y + (tall ? 6 : 7), cw - 120, NoticeBoardTheme.INK);
            if (tall) BoardDraw.text(g, header.subtitle(), x + 26, y + 17, cw - 120, NoticeBoardTheme.MUTED);
            int seal = tall ? 22 : 18;
            BoardDraw.seal(g, townName, x + cw - seal - 6, y + (ch - seal) / 2, seal);
            BoardDraw.stamp(g, Component.translatable(statusKey + header.status().name().toLowerCase(Locale.ROOT)), x + cw - seal - 34, y + ch / 2,
                    statusColor(header.status()), false, -9, stampScale.get());
            BoardDraw.pin(g, NoticeBoardIcons.PIN_BRASS, x + 10, y + 1);
            BoardDraw.pin(g, NoticeBoardIcons.PIN_BRASS, x + cw - 10, y + 1);
        }).content(c -> c.add(new HitArea(c, w - 82, 0, 50, h).tooltip(header.reasons())));
        card.setPosAndSize(0, 0, w, h);
        layer.add(card);
        return card;
    }

    /**
     * 高页头名字后面的「改名」小签，离右边的印章留出空当。
     * The rename tag right after the name on a tall header, kept clear of the stamps on the right.
     */
    public static void renameTag(UILayer layer, int w, Component name, Component tooltip, Runnable onClick) {
        Component label = Component.translatable(TownFormat.KEY + "rename");
        int tagW = BoardDraw.font().width(label) + 21;
        int nameW = Math.min(BoardDraw.font().width(name.copy().withStyle(ChatFormatting.BOLD)), w - 120);
        int x = Math.max(26, Math.min(26 + nameW + 6, w - 100 - tagW));
        HitArea tag = new HitArea(layer, x, 4, tagW, 12);
        layer.add(tag.painter((g, px, py, pw, ph) -> {
            NoticeBoardIcons.TAG.draw(g, px, py, pw, ph);
            BoardDraw.pencil(g, px + 5, py + 2, tag.isMouseOver());
            BoardDraw.text(g, label, px + 16, py + 2, pw - 18, NoticeBoardTheme.INK);
        }).tooltip(List.of(tooltip)).onClick(onClick));
    }

    /**
     * 改名时盖在页头名字上的输入框，旧名字全选。
     * The text box laid over the header name while renaming, with the old name selected.
     */
    public static RenameBox renameBox(UILayer layer, int w, String text, String ghost, int maxLength, Consumer<String> onDone,
                                      Runnable onCancel) {
        RenameBox box = new RenameBox(layer, text, maxLength, onDone, onCancel);
        box.ghostText = ghost;
        box.setPosAndSize(26, 4, Math.max(60, Math.min(180, w - 120)), 13);
        layer.add(box);
        box.setFocused(true);
        return box;
    }

    // ------------------------------------------------------------------ key figures

    /** Widths for notes of the given weights across {@code w} with {@code gap} between them. */
    public static int[] widths(int w, int gap, double... weights) {
        double total = 0;
        for (double weight : weights) total += weight;
        int[] out = new int[weights.length];
        int used = 0;
        for (int i = 0; i < weights.length; i++) {
            out[i] = i == weights.length - 1 ? w - gap * (weights.length - 1) - used : (int) ((w - gap * (weights.length - 1)) * weights[i] / total);
            used += out[i];
        }
        return out;
    }

    public static PaperCard stat(UILayer layer, Stat stat, int x, int y, int w, int h, CTextureIcon pin, float tilt, int frost,
                                 IntConsumer onLink) {
        PaperCard card = new PaperCard(layer).pin(pin).tilt(tilt).frost(() -> frost).painter((g, cx, cy, cw, ch) -> {
            BoardDraw.text(g, stat.title(), cx + 6, cy + 6, cw - 10, NoticeBoardTheme.MUTED);
            Component value = stat.value();
            if (BoardDraw.bigWidth(value) <= cw - 10) BoardDraw.big(g, value, cx + 6, cy + 17, NoticeBoardTheme.INK);
            else BoardDraw.text(g, value.copy().withStyle(ChatFormatting.BOLD), cx + 6, cy + 20, cw - 10, NoticeBoardTheme.INK);
            BoardDraw.text(g, stat.sub(), cx + 6, cy + ch - 11, cw - 10, BoardDraw.toneColor(stat.subTone()));
        });
        if (!stat.tooltip().isEmpty()) card.tooltip(stat::tooltip);
        if (stat.link() >= 0) card.onClick(() -> onLink.accept(stat.link()));
        card.setPosAndSize(x, y, w, h);
        layer.add(card);
        return card;
    }

    /** A key-figure note with two labelled usage bars, for capacity and stock. */
    public static PaperCard usage(UILayer layer, Component title, Component firstLabel, int firstUsed, int firstLimit,
                                  Component secondLabel, int secondUsed, int secondLimit, List<Component> tooltip,
                                  int x, int y, int w, int h, int frost) {
        PaperCard card = new PaperCard(layer).pin(NoticeBoardIcons.PIN_RED).tilt(0.3f).frost(() -> frost).painter((g, cx, cy, cw, ch) -> {
            BoardDraw.text(g, title, cx + 6, cy + 6, cw - 10, NoticeBoardTheme.MUTED);
            usageRow(g, firstLabel, firstUsed, firstLimit, cx + 6, cy + 19, cw - 12);
            if (secondLabel != null) usageRow(g, secondLabel, secondUsed, secondLimit, cx + 6, cy + 31, cw - 12);
        }).tooltip(() -> tooltip);
        card.setPosAndSize(x, y, w, h);
        layer.add(card);
        return card;
    }

    public static void usageRow(GuiGraphics g, Component label, long used, long limit, int x, int y, int w) {
        int labelW = Math.max(26, BoardDraw.font().width(label) + 4);
        BoardDraw.text(g, label, x, y, labelW, NoticeBoardTheme.INK);
        Component numbers = Component.literal(used + " / " + limit);
        int numbersW = BoardDraw.font().width(numbers);
        float fraction = limit > 0 ? used / (float) limit : 0;
        int fill = fraction >= 0.9f ? NoticeBoardTheme.RED : fraction >= 0.6f ? NoticeBoardTheme.AMBER : NoticeBoardTheme.GREEN_FILL;
        int barX = x + labelW + 2, barW = Math.max(10, w - labelW - 2 - numbersW - 6);
        NoticeBoardTheme.INSTANCE.drawProgressBar(g, barX, y + 2, barW, 5, fraction, fill);
        BoardDraw.textRight(g, numbers, x + w, y, NoticeBoardTheme.INK);
    }

    // ------------------------------------------------------------------ conditions and progress

    /** Room a five-star rating takes inside a chip: five 8-pixel stars and a little padding. */
    private static final int STARS_EXTRA = 44;

    public static int chipWidth(Chip chip) {
        return BoardDraw.chipWidth(chip.text()) + (chip.stars() >= 0 ? STARS_EXTRA : 0);
    }

    public static int chipRows(List<Chip> chips, int width) {
        int rows = 1, x = 0;
        for (Chip chip : chips) {
            int chipW = chipWidth(chip);
            if (x > 0 && x + chipW > width) {
                rows++;
                x = 0;
            }
            x += chipW + 4;
        }
        return rows;
    }

    /** The working-conditions sheet; returns its height. */
    public static int conditions(UILayer layer, Component title, List<Chip> chips, int x, int y, int w, int frost) {
        int h = 7 + 12 + chipRows(chips, w - 14) * 13 + 6;
        PaperCard card = new PaperCard(layer).pin(NoticeBoardIcons.PIN_RED).tilt(0.25f).frost(() -> frost).painter((g, cx, cy, cw, ch) -> {
            int px = cx + 7, py = cy + 7;
            BoardDraw.text(g, title.copy().withStyle(ChatFormatting.BOLD), px, py, cw - 14, NoticeBoardTheme.INK);
            py += 12;
            int rowX = px;
            for (Chip chip : chips) {
                int chipW = chipWidth(chip);
                if (rowX > px && rowX + chipW > cx + cw - 6) {
                    rowX = px;
                    py += 13;
                }
                int color = BoardDraw.toneColor(chip.tone());
                BoardDraw.chip(g, chip.text(), rowX, py, color, chip.stars() >= 0 ? STARS_EXTRA : 0);
                if (chip.stars() >= 0) BoardDraw.stars(g, rowX + BoardDraw.chipWidth(chip.text()), py + 1, chip.stars());
                rowX += chipW + 4;
            }
        }).content(c -> {
            // Same flow as the painter, so each chip with a tooltip gets a hover area over it.
            int px = 7, py = 19, rowX = px;
            for (Chip chip : chips) {
                int chipW = chipWidth(chip);
                if (rowX > px && rowX + chipW > w - 6) {
                    rowX = px;
                    py += 13;
                }
                if (!chip.tooltip().isEmpty()) c.add(new HitArea(c, rowX, py, chipW, 12).tooltip(chip.tooltip()));
                rowX += chipW + 4;
            }
        });
        card.setPosAndSize(x, y, w, h);
        layer.add(card);
        return h;
    }

    /** A sheet of progress lines with a bar and a percentage each; returns its height, or 0 when there is no room. */
    public static int progress(UILayer layer, Component title, List<ProgressLine> lines, Component empty, int x, int y, int w, int maxH,
                               int frost, Runnable onClick) {
        int h = Math.min(maxH, 7 + 12 + Math.max(1, lines.size()) * 11 + (lines.isEmpty() ? 10 : 0) + 6);
        if (h < 30) return 0;
        PaperCard card = new PaperCard(layer).pin(NoticeBoardIcons.PIN_BLUE).tilt(-0.25f).frost(() -> frost).painter((g, cx, cy, cw, ch) -> {
            int px = cx + 7, py = cy + 7;
            BoardDraw.text(g, title.copy().withStyle(ChatFormatting.BOLD), px, py, cw - 14, NoticeBoardTheme.INK);
            py += 12;
            if (lines.isEmpty()) {
                BoardDraw.wrapped(g, empty, px, py, cw - 14, NoticeBoardTheme.MUTED, 2);
                return;
            }
            int barW = Math.max(30, cw / 4);
            for (ProgressLine line : lines) {
                if (py + 10 > cy + ch - 2) break;
                int color = line.stopped() ? NoticeBoardTheme.RED : NoticeBoardTheme.INK;
                int textW = cw - 14 - barW - 34;
                BoardDraw.text(g, line.title(), px, py, textW, color);
                NoticeBoardTheme.INSTANCE.drawProgressBar(g, px + textW + 4, py + 2, barW, 5, line.percent() / 100f, BoardDraw.toneFill(line.tone()));
                BoardDraw.textRight(g, line.stopped() ? b("stamp.stop") : Component.literal(line.percent() + "%"), cx + cw - 7, py, color);
                py += 11;
            }
        });
        if (onClick != null) card.onClick(onClick);
        card.setPosAndSize(x, y, w, h);
        layer.add(card);
        return h;
    }

    // ------------------------------------------------------------------ notes

    /** Height that fits the tallest note of the stack, between 84 and {@code maxH}. */
    public static int noteHeight(List<Note> notes, int w, int maxH) {
        int tallest = 0;
        for (Note note : notes) {
            int h = 8 + 16 + BoardDraw.wrappedHeight(note.line(), w - 16, 6);
            if (note.kind() == NoteKind.IMPROVE && !note.action().getString().isEmpty())
                h += 3 + BoardDraw.wrappedHeight(arrow(note.action()), w - 16, 3);
            tallest = Math.max(tallest, h + 24);
        }
        return Math.max(84, Math.min(maxH, tallest));
    }

    public static NoteStack notes(UILayer layer, List<Note> notes, int index, Set<String> fresh, IntConsumer onIndex, IntConsumer onSeen,
                                  int x, int y, int w, int maxH) {
        NoteStack stack = new NoteStack(layer, notes, index, fresh, onIndex, onSeen);
        stack.setPosAndSize(x, y, w, noteHeight(notes, w, maxH));
        layer.add(stack);
        return stack;
    }

    // ------------------------------------------------------------------ people

    /** A row of small photos, for who is on duty today; returns how many fit. */
    public static int photoStrip(UILayer layer, Component title, List<Person> people, int x, int y, int w, Consumer<Person> onSelect) {
        return photoStrip(layer, title, people, x, y, w, 36, 44, onSelect);
    }

    /** {@link #photoStrip} with photos of the given size; returns how many fit. */
    public static int photoStrip(UILayer layer, Component title, List<Person> people, int x, int y, int w, int photoW, int photoH,
                                 Consumer<Person> onSelect) {
        if (people.isEmpty()) return 0;
        layer.add(new HitArea(layer, x, y, w, 10).painter((g, px, py, pw, ph) -> BoardDraw.textOnWood(g, title, px, py, pw, NoticeBoardTheme.ON_WOOD)));
        int px = x, count = 0;
        float[] tilts = {-3f, 2f, -1.5f, 2.5f, -2f, 1f};
        // When not everyone fits, the last slot says how many more there are instead of leaving them out unsaid.
        int fit = Math.max(1, (w + 6) / (photoW + 6));
        int shown = people.size() > fit ? fit - 1 : people.size();
        for (Person person : people) {
            if (count >= shown || px + photoW > x + w) break;
            PhotoCard photo = new PhotoCard(layer, person, tilts[count % tilts.length], () -> false, () -> onSelect.accept(person));
            photo.caption(PhotoCard.captionFor(person, people));
            photo.setPosAndSize(px, y + 14, photoW, photoH);
            layer.add(photo);
            px += photoW + 6;
            count++;
        }
        if (count < people.size()) {
            Component more = Component.literal("+" + (people.size() - count));
            int slotX = px, slotY = y + 14 + photoH / 2 - 5;
            layer.add(new HitArea(layer, slotX, slotY, photoW, 10).painter((g, ax, ay, aw, ah) ->
                    BoardDraw.textOnWood(g, more, ax + 4, ay, NoticeBoardTheme.ON_WOOD)));
        }
        return count;
    }

    /**
     * 员工页：左侧照片墙（空位画虚线框），右侧选中者的人事卡。
     * The workers tab: a photo wall on the left with dashed empty posts, and the selected person's card on the right.
     */
    public static void workers(UILayer layer, int x, int y, int w, int h, Component headline, List<Person> people, Component slotsLine,
                               int emptySlots, UUID selected, Consumer<UUID> onSelect, Component emptyText, List<Component> efficiencyTooltip) {
        layer.add(new HitArea(layer, x + 2, y, w - 4, 10).painter((g, px, py, pw, ph) -> BoardDraw.textOnWood(g, headline, px, py, pw, NoticeBoardTheme.ON_WOOD)));
        if (people.isEmpty()) {
            // Nobody here yet: a row of empty frames waits on the board, one per post, with a note pinned beside them.
            int photoW = 48, photoH = 58, gapX = 8, frames = Math.min(emptySlots, Math.max(1, (w * 3 / 5 + gapX) / (photoW + gapX)));
            int rowW = frames * photoW + Math.max(0, frames - 1) * gapX;
            int noteW = Math.min(PERSONNEL_MAX_W, w - rowW - 30);
            boolean beside = noteW >= 120;
            int rowX = x + (beside ? 6 : (w - rowW) / 2), rowY = y + 22;
            for (int i = 0; i < frames; i++) {
                int fx = rowX + i * (photoW + gapX);
                layer.add(new HitArea(layer, fx, rowY, photoW, photoH).painter((g, px, py, pw, ph) -> {
                    dashed(g, px, py, pw, ph, NoticeBoardTheme.ON_WOOD);
                    BoardDraw.textCentered(g, b("workers.slot"), px + pw / 2, py + ph / 2 - 4, NoticeBoardTheme.ON_WOOD);
                }).tooltip(List.of(slotsLine)));
            }
            PaperCard empty = new PaperCard(layer).pin(NoticeBoardIcons.PIN_RED).tilt(0.4f).painter((g, cx, cy, cw, ch) -> {
                int used = BoardDraw.wrapped(g, emptyText, cx + 10, cy + 12, cw - 20, NoticeBoardTheme.INK, 3);
                BoardDraw.wrapped(g, slotsLine, cx + 10, cy + 12 + used + 6, cw - 20, NoticeBoardTheme.MUTED, 3);
            });
            if (beside) empty.setPosAndSize(rowX + rowW + 18, rowY + 4, noteW, 62);
            else empty.setPosAndSize(x + w / 6, rowY + photoH + 12, w * 2 / 3, 62);
            layer.add(empty);
            return;
        }
        UUID chosen = people.stream().anyMatch(p -> p.id().equals(selected)) ? selected : people.get(0).id();
        int columns = w >= 470 ? 3 : 2;
        int photoW = 48, photoH = 58, gapX = 8, gapY = 10;
        int wallW = columns * photoW + (columns - 1) * gapX;
        int rows = Math.max(1, (h - 18 - 14) / (photoH + gapY));
        float[] tilts = {-3f, 2.5f, 1.5f, -2f, 2f, -1f};
        int index = 0;
        for (Person person : people) {
            if (index >= rows * columns) break;
            int col = index % columns, row = index / columns;
            PhotoCard photo = new PhotoCard(layer, person, tilts[index % tilts.length], () -> person.id().equals(chosen), () -> onSelect.accept(person.id()));
            photo.caption(PhotoCard.captionFor(person, people));
            photo.setPosAndSize(x + col * (photoW + gapX) + 4, y + 18 + row * (photoH + gapY), photoW, photoH);
            layer.add(photo);
            index++;
        }
        if (emptySlots > 0 && index < rows * columns) {
            int col = index % columns, row = index / columns;
            layer.add(new HitArea(layer, x + col * (photoW + gapX) + 4, y + 18 + row * (photoH + gapY), photoW, photoH)
                    .painter((g, px, py, pw, ph) -> {
                        dashed(g, px, py, pw, ph, NoticeBoardTheme.ON_WOOD);
                        BoardDraw.textCentered(g, b("workers.slot"), px + pw / 2, py + ph / 2 - 4, NoticeBoardTheme.ON_WOOD);
                    }).tooltip(List.of(slotsLine)));
        }
        if (emptySlots == 0)
            layer.add(new HitArea(layer, x + 4, y + h - 10, wallW, 10).painter((g, px, py, pw, ph) ->
                    BoardDraw.textOnWood(g, slotsLine, px, py, pw, NoticeBoardTheme.ON_WOOD)));

        Person person = people.stream().filter(p -> p.id().equals(chosen)).findFirst().orElse(people.get(0));
        // A personnel card reads best at index-card width; on a wide board it sits centred in the space left.
        int room = w - wallW - 22;
        int cardW = Math.min(room, PERSONNEL_MAX_W);
        int cardX = x + wallW + 18 + (room - cardW) / 2;
        PaperCard card = new PaperCard(layer).pin(NoticeBoardIcons.PIN_BRASS).tilt(0.3f).frost(() -> Math.min(1, person.frost()))
                .painter((g, cx, cy, cw, ch) -> personnel(g, person, cx, cy, cw, ch))
                .content(c -> {
                    if (person.efficiency() >= 0) c.add(new HitArea(c, 6, 18, 120, 10).tooltip(efficiencyTooltip));
                });
        card.setPosAndSize(cardX, y + 16, cardW, Math.min(h - 18, personnelHeight(person, cardW)));
        layer.add(card);
    }

    public static void dashed(GuiGraphics g, int x, int y, int w, int h, int color) {
        for (int i = 0; i < w; i += 4) {
            g.fill(x + i, y, x + Math.min(w, i + 2), y + 1, color);
            g.fill(x + i, y + h - 1, x + Math.min(w, i + 2), y + h, color);
        }
        for (int i = 0; i < h; i += 4) {
            g.fill(x, y + i, x + 1, y + Math.min(h, i + 2), color);
            g.fill(x + w - 1, y + i, x + w, y + Math.min(h, i + 2), color);
        }
    }

    public static final int PERSONNEL_MAX_W = 280;

    /**
     * 条形图左边一栏的宽度：按最宽的名字定，英文名字也放得下，但最多占三分之一，留给条本身。
     * The width of the label column beside bars: as wide as the widest label, so English names fit, but no more than a
     * third of the room, which the bars keep.
     */
    public static int labelColumn(List<Bar> bars, int room) {
        int widest = 0;
        for (Bar bar : bars) {
            Component label = bar.main() ? bar.label().copy().withStyle(ChatFormatting.BOLD) : bar.label();
            widest = Math.max(widest, BoardDraw.font().width(label));
        }
        return Math.max(26, Math.min(room / 3, widest + 6));
    }

    public static int personnelHeight(Person person, int w) {
        int height = 7 + 12 + 15 + 5 + person.bars().size() * 11 + 3 + 6;
        for (Bar bar : person.bars()) if (bar.groupStart()) height += 4;
        if (!person.reason().getString().isEmpty()) height += BoardDraw.wrappedHeight(person.reason(), w - 16, 2);
        for (Component line : person.lines()) height += BoardDraw.wrappedHeight(line, w - 16, 2);
        return Math.max(90, height);
    }

    public static void personnel(GuiGraphics g, Person person, int x, int y, int w, int h) {
        int px = x + 8, py = y + 7;
        BoardDraw.text(g, Component.literal(person.name()).withStyle(ChatFormatting.BOLD), px, py, w - 60, NoticeBoardTheme.INK);
        py += 12;
        int chipW = BoardDraw.chip(g, person.badge(), px, py, NoticeBoardTheme.PURPLE_INK);
        if (person.efficiency() >= 0)
            BoardDraw.text(g, b("person.efficiency", person.efficiency()), px + chipW + 6, py + 2, w - chipW - 30, NoticeBoardTheme.INK);
        Component duty = b("stamp." + switch (person.duty()) {
            case ON_DUTY -> "on_duty";
            case RESTING -> "resting";
            case OFF -> "off";
            case FREE -> "free";
        });
        int dutyColor = switch (person.duty()) {
            case ON_DUTY -> NoticeBoardTheme.GREEN_FILL;
            case FREE -> NoticeBoardTheme.BLUE;
            default -> NoticeBoardTheme.RED;
        };
        BoardDraw.stamp(g, duty, x + w - 24, y + 14, dutyColor, false, -8, 1);
        py += 15;
        g.fill(px, py, x + w - 8, py + 1, 0xFFB4B2A9);
        py += 5;
        int labelW = labelColumn(person.bars(), w - 16), valueW = 18;
        for (Bar bar : person.bars()) {
            if (bar.groupStart()) {
                for (int dx = px; dx < x + w - 8; dx += 3) g.fill(dx, py, dx + 1, py + 1, 0xFFD3D1C7);
                py += 4;
            }
            if (py + 9 > y + h - 4) break;
            Component label = bar.main() ? bar.label().copy().withStyle(ChatFormatting.BOLD) : bar.label();
            BoardDraw.text(g, label, px, py, labelW, NoticeBoardTheme.INK);
            int barW = w - 16 - labelW - valueW - 4;
            // Colour always tells the value; the bar this job relies on is marked by its bold label instead.
            int fill = BoardDraw.toneFill(bar.tone());
            NoticeBoardTheme.INSTANCE.drawProgressBar(g, px + labelW, py + 2, barW, 5, bar.value() / 100f, fill);
            BoardDraw.textRight(g, Component.literal(Integer.toString(bar.value())), x + w - 8, py, NoticeBoardTheme.INK);
            py += 11;
        }
        py += 3;
        if (!person.reason().getString().isEmpty() && py + 9 <= y + h - 4) {
            py += BoardDraw.wrapped(g, person.reason(), px, py, w - 16, NoticeBoardTheme.RED, 2);
        }
        for (Component line : person.lines()) {
            if (py + 9 > y + h - 4) break;
            py += BoardDraw.wrapped(g, line, px, py, w - 16, NoticeBoardTheme.MUTED, 2);
        }
    }

    // ------------------------------------------------------------------ items, tips, text on wood

    /** A sheet listing items with icon, label and amount; returns its height. */
    public static int items(UILayer layer, Component title, List<ItemLine> lines, Component empty, int x, int y, int w, int maxH,
                            CTextureIcon pin, float tilt, int frost) {
        return items(layer, title, lines, empty, x, y, w, maxH, pin, tilt, frost, PaperCard.Kind.PAPER);
    }

    /** {@link #items} on the given kind of paper, e.g. squared paper for a menu or a stock list. */
    public static int items(UILayer layer, Component title, List<ItemLine> lines, Component empty, int x, int y, int w, int maxH,
                            CTextureIcon pin, float tilt, int frost, PaperCard.Kind kind) {
        int rowH = 18;
        int h = Math.min(maxH, 7 + 12 + Math.max(1, lines.size()) * rowH + 4);
        PaperCard card = new PaperCard(layer).kind(kind).pin(pin).tilt(tilt).frost(() -> frost).painter((g, cx, cy, cw, ch) -> {
            int px = cx + 7, py = cy + 7;
            BoardDraw.text(g, title.copy().withStyle(ChatFormatting.BOLD), px, py, cw - 14, NoticeBoardTheme.INK);
            py += 12;
            if (lines.isEmpty()) {
                BoardDraw.wrapped(g, empty, px, py + 2, cw - 14, NoticeBoardTheme.MUTED, 2);
                return;
            }
            for (ItemLine line : lines) {
                if (py + 16 > cy + ch - 2) break;
                g.renderItem(line.icon(), px, py);
                int amountW = BoardDraw.font().width(line.amount());
                BoardDraw.text(g, line.label(), px + 20, py + 4, cw - 34 - amountW, NoticeBoardTheme.INK);
                BoardDraw.textRight(g, line.amount(), cx + cw - 7, py + 4, BoardDraw.toneColor(line.tone()));
                py += rowH;
            }
        }).content(c -> {
            int py = 19;
            for (ItemLine line : lines) {
                if (py + 16 > h - 2) break;
                List<Component> tooltip = line.tooltip().isEmpty() ? List.of(line.icon().getHoverName()) : line.tooltip();
                c.add(new HitArea(c, 7, py, 16, 16).tooltip(tooltip));
                py += rowH;
            }
        });
        card.setPosAndSize(x, y, w, h);
        layer.add(card);
        return h;
    }

    public static final int SLIP_H = 42;

    /**
     * 一张方格小纸条，横排列出物品图标和数量，比如上一顿饭、昨天的收成；点击打开对应的页。返回高度。
     * A slip of squared paper listing items in a row, icon and amount, such as the last meal or yesterday's yield;
     * clicking opens the matching tab. Returns its height.
     */
    public static int itemSlip(UILayer layer, Component title, List<ItemLine> items, Component empty, int x, int y, int w, Runnable onClick) {
        PaperCard slip = new PaperCard(layer).kind(PaperCard.Kind.GRID).pin(NoticeBoardIcons.PIN_GREEN).tilt(-0.5f).painter((g, cx, cy, cw, ch) -> {
            BoardDraw.text(g, title.copy().withStyle(ChatFormatting.BOLD), cx + 8, cy + 7, cw - 16, NoticeBoardTheme.INK);
            if (items.isEmpty()) {
                BoardDraw.text(g, empty, cx + 8, cy + 23, cw - 16, NoticeBoardTheme.MUTED);
                return;
            }
            int px = cx + 8;
            for (ItemLine item : items) {
                int amountW = BoardDraw.font().width(item.amount());
                if (px + 18 + amountW > cx + cw - 8) break;
                g.renderItem(item.icon(), px, cy + 19);
                BoardDraw.text(g, item.amount(), px + 18, cy + 24, amountW,
                        item.tone() == Tone.NORMAL ? NoticeBoardTheme.MUTED : BoardDraw.toneColor(item.tone()));
                px += 18 + amountW + 8;
            }
        }).content(c -> {
            int px = 8;
            for (ItemLine item : items) {
                int amountW = BoardDraw.font().width(item.amount());
                if (px + 18 + amountW > w - 8) break;
                c.add(new HitArea(c, px, 19, 16, 16).tooltip(item.tooltip().isEmpty() ? List.of(item.label()) : item.tooltip()));
                px += 18 + amountW + 8;
            }
        });
        if (onClick != null) slip.onClick(onClick);
        slip.setPosAndSize(x, y, w, SLIP_H);
        layer.add(slip);
        return SLIP_H;
    }

    /** A small yellow tips note; returns its height. */
    public static int tips(UILayer layer, Component title, List<Component> tips, int x, int y, int w, int maxH) {
        if (tips.isEmpty()) return 0;
        // As tall as the whole tips that fit; the rest are left out rather than cut.
        int h = 8 + 12;
        // A hanging indent: the bullet stands alone in the margin and every line of the tip starts after it.
        int indent = BoardDraw.font().width("·\u00A0");
        for (Component tip : tips) {
            int need = BoardDraw.wrappedHeight(tip, w - 18 - indent, 3) + 2;
            if (h + need + 6 > maxH) break;
            h += need;
        }
        h += 6;
        if (h < 36) return 0;
        PaperCard card = new PaperCard(layer).kind(PaperCard.Kind.NOTE).pin(NoticeBoardIcons.PIN_GREEN).tilt(0.6f).painter((g, cx, cy, cw, ch) -> {
            int px = cx + 9, py = cy + 8;
            BoardDraw.text(g, title.copy().withStyle(ChatFormatting.BOLD), px, py, cw - 18, NoticeBoardTheme.NOTE_INK);
            py += 12;
            for (Component tip : tips) {
                // A tip is shown whole or not at all; one cut mid-sentence reads worse than one left out.
                int need = BoardDraw.wrappedHeight(tip, cw - 18 - indent, 3);
                if (py + need > cy + ch - 3) break;
                BoardDraw.text(g, Component.literal("·"), px, py, indent, NoticeBoardTheme.NOTE_INK);
                py += BoardDraw.wrapped(g, tip, px + indent, py, cw - 18 - indent, NoticeBoardTheme.NOTE_INK, 3) + 2;
            }
        });
        card.setPosAndSize(x, y, w, h);
        layer.add(card);
        return h;
    }

    /** An action arrow joined to its text by a no-break space, so the arrow never wraps onto a line of its own. */
    public static Component arrow(Component text) {
        return Component.literal("→\u00A0").append(text);
    }

    /** A list bullet joined to its text by a no-break space, so the bullet never wraps onto a line of its own. */
    public static Component bullet(Component text) {
        return Component.literal("·\u00A0").append(text);
    }

    /** Light text on the wooden board, optionally with a tooltip and a click. */
    public static HitArea woodText(UILayer layer, Component text, int x, int y, int w, List<Component> tooltip, Runnable onClick) {
        HitArea area = new HitArea(layer, x, y, w, 10).painter((g, px, py, pw, ph) -> BoardDraw.textOnWood(g, text, px, py, pw, NoticeBoardTheme.ON_WOOD));
        if (tooltip != null && !tooltip.isEmpty()) area.tooltip(tooltip);
        if (onClick != null) area.onClick(onClick);
        layer.add(area);
        return area;
    }

    // ------------------------------------------------------------------ debug

    /** One section of a debug tab: its button label and what it shows. */
    public record DebugSection(Component label, DebugBuilder builder) {
    }

    @FunctionalInterface
    public interface DebugBuilder {
        UIElement build(UILayer layer, int x, int y, int w, int h);
    }

    /** The debug tab: section buttons, a copy-all button and the selected section's original panel. */
    public static void debug(UILayer layer, int y, int w, int h, List<DebugSection> sections, int selected, IntConsumer onSelect,
                             Supplier<String> copyText) {
        int bx = 2;
        for (int i = 0; i < sections.size(); i++) {
            final int section = i;
            Component label = sections.get(i).label();
            int bw = BoardDraw.font().width(label) + 10;
            layer.add(new HitArea(layer, bx, y, bw, 12).painter((g, px, py, pw, ph) -> {
                (selected == section ? NoticeBoardIcons.TAG_HOVER : NoticeBoardIcons.TAG).draw(g, px, py, pw, ph);
                BoardDraw.textCentered(g, label, px + pw / 2, py + 2, NoticeBoardTheme.INK);
            }).onClick(() -> onSelect.accept(section)));
            bx += bw + 4;
        }
        Component copy = b("debug.copy");
        int cw = BoardDraw.font().width(copy) + 10;
        layer.add(new HitArea(layer, w - cw - 2, y, cw, 12).painter((g, px, py, pw, ph) -> {
            NoticeBoardIcons.TAG.draw(g, px, py, pw, ph);
            BoardDraw.textCentered(g, copy, px + pw / 2, py + 2, NoticeBoardTheme.INK);
        }).onClick(() -> {
            Minecraft.getInstance().keyboardHandler.setClipboard(copyText.get());
            Minecraft.getInstance().gui.getChat().addMessage(b("debug.copied"));
        }));
        if (sections.isEmpty()) return;
        DebugSection section = sections.get(Math.max(0, Math.min(sections.size() - 1, selected)));
        UIElement panel = section.builder().build(layer, 0, y + 16, w, h - y - 16);
        // Debug sheets are graph paper, a logbook beside the players' notices.
        if (panel instanceof com.teammoeg.frostedheart.content.town.tabs.TownInfoPanel info) info.ledger();
        else if (panel instanceof com.teammoeg.frostedheart.content.town.tabs.BuildingInfoElement lines) {
            lines.paper();
            lines.setSize(w, h - y - 16);
        }
        else if (panel instanceof com.teammoeg.frostedheart.content.town.tabs.TownWorkforcePanel crew) {
            crew.paper();
            // The crew sheet takes the whole page like the others.
            crew.setSize(w, h - y - 16);
        }
        if (panel != null) layer.add(panel);
    }
}
