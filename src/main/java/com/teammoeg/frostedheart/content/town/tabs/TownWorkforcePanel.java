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
 */

package com.teammoeg.frostedheart.content.town.tabs;

import com.teammoeg.chorda.client.RenderingHint;
import com.teammoeg.chorda.client.cui.base.MouseButton;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.frostedheart.content.town.resident.Resident;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;

public class TownWorkforcePanel extends UIElement {
    private static final int WIDTH = 160;
    private static final int HEIGHT = 130;
    private static final int LIST_WIDTH = 58;
    private static final int LIST_TOP = 20;
    private static final int ROW_HEIGHT = 16;
    private static final int VISIBLE_RESIDENTS = 6;
    private static final int DETAIL_LINE_HEIGHT = 13;
    private static final int DETAIL_VISIBLE = 9;
    private static final int SCROLLBAR_WIDTH = 6;
    private static final int DETAIL_TEXT_WIDTH =
            WIDTH - LIST_WIDTH - SCROLLBAR_WIDTH - 13;
    private static final int MIN_THUMB_HEIGHT = 10;

    private final Supplier<List<Resident>> residentSource;
    private final Predicate<Resident> canWork;
    private final ToDoubleFunction<Resident> proficiency;
    private final ToDoubleFunction<Resident> productivity;
    private final ToDoubleFunction<Resident> contribution;
    private final ToDoubleFunction<Resident> proficiencyGain;
    private final Component proficiencyName;
    private final Function<Double, Component> contributionText;

    private UUID selectedResident;
    private int residentScroll;
    private int detailScroll;
    private boolean draggingDetails;

    public TownWorkforcePanel(
            UIElement parent, int x, int y,
            Supplier<List<Resident>> residentSource,
            Predicate<Resident> canWork,
            ToDoubleFunction<Resident> proficiency,
            ToDoubleFunction<Resident> productivity,
            ToDoubleFunction<Resident> contribution,
            ToDoubleFunction<Resident> proficiencyGain,
            Component proficiencyName,
            Function<Double, Component> contributionText
    ) {
        super(parent);
        this.residentSource = residentSource;
        this.canWork = canWork;
        this.proficiency = proficiency;
        this.productivity = productivity;
        this.contribution = contribution;
        this.proficiencyGain = proficiencyGain;
        this.proficiencyName = proficiencyName;
        this.contributionText = contributionText;
        setPos(x, y);
        setSize(WIDTH, HEIGHT);
    }

    @Override
    public void render(
            GuiGraphics graphics, int x, int y, int width, int height,
            RenderingHint hint
    ) {
        Font font = Minecraft.getInstance().font;
        List<Resident> residents = residents();
        Resident selected = normalizeSelection(residents);
        drawPanel(graphics, x, y, width, height);
        graphics.fill(x + LIST_WIDTH, y + 1, x + LIST_WIDTH + 1, y + height - 1, 0xFF777777);
        graphics.drawString(font, Component.translatable("gui.frostedheart.town.workers"),
                x + 4, y + 6, 0xFFFFAA00, true);
        renderList(graphics, font, x, y, residents);
        renderDetails(graphics, font, x, y, detailLines(selected));
    }

    private void renderList(
            GuiGraphics graphics, Font font, int x, int y, List<Resident> residents
    ) {
        int maxScroll = Math.max(0, residents.size() - VISIBLE_RESIDENTS);
        residentScroll = Mth.clamp(residentScroll, 0, maxScroll);
        for (int row = 0; row < VISIBLE_RESIDENTS; row++) {
            int index = residentScroll + row;
            if (index >= residents.size()) break;
            Resident resident = residents.get(index);
            int rowY = y + LIST_TOP + row * ROW_HEIGHT;
            if (resident.getUUID().equals(selectedResident)) {
                graphics.fill(x + 2, rowY, x + LIST_WIDTH - 3, rowY + ROW_HEIGHT - 1, 0xA0446688);
            } else if (getMouseX() >= 2 && getMouseX() < LIST_WIDTH - 3
                    && getMouseY() >= LIST_TOP + row * ROW_HEIGHT
                    && getMouseY() < LIST_TOP + (row + 1) * ROW_HEIGHT) {
                graphics.fill(x + 2, rowY, x + LIST_WIDTH - 3, rowY + ROW_HEIGHT - 1, 0x60444444);
            }
            graphics.drawString(font,
                    TownTextLayout.ellipsize(font, resident.toString(), LIST_WIDTH - 9),
                    x + 5, rowY + 4, 0xFFFFFFFF, true);
        }
        if (maxScroll > 0) {
            int trackY = y + LIST_TOP;
            int trackHeight = VISIBLE_RESIDENTS * ROW_HEIGHT;
            int thumbHeight = Math.max(MIN_THUMB_HEIGHT,
                    trackHeight * VISIBLE_RESIDENTS / residents.size());
            int thumbY = trackY + (trackHeight - thumbHeight) * residentScroll / maxScroll;
            graphics.fill(x + LIST_WIDTH - 3, trackY,
                    x + LIST_WIDTH - 1, trackY + trackHeight, 0xFF222222);
            graphics.fill(x + LIST_WIDTH - 3, thumbY,
                    x + LIST_WIDTH - 1, thumbY + thumbHeight, 0xFFAAAAAA);
        }
    }

    private void renderDetails(
            GuiGraphics graphics, Font font, int x, int y, List<Line> lines
    ) {
        List<VisualLine> visualLines = wrapDetailLines(font, lines);
        int maxScroll = Math.max(0, visualLines.size() - DETAIL_VISIBLE);
        detailScroll = Mth.clamp(detailScroll, 0, maxScroll);
        int textX = x + LIST_WIDTH + 5;
        for (int row = 0; row < DETAIL_VISIBLE; row++) {
            int index = detailScroll + row;
            if (index >= visualLines.size()) break;
            VisualLine line = visualLines.get(index);
            graphics.drawString(font, line.text(),
                    textX, y + 6 + row * DETAIL_LINE_HEIGHT, line.color(), true);
        }
        renderDetailScrollbar(graphics, x, y, visualLines.size(), maxScroll);
    }

    private List<Line> detailLines(Resident resident) {
        if (resident == null) {
            return List.of(new Line(
                    Component.translatable("gui.frostedheart.town.no_workers"), 0xFFAAAAAA));
        }
        List<Line> lines = new ArrayList<>();
        lines.add(new Line(Component.literal(resident.toString()), 0xFFFFAA00));
        lines.add(stat("gui.frostedheart.town.health", resident.getHealth()));
        lines.add(stat("gui.frostedheart.town.mental", resident.getMental()));
        lines.add(stat("gui.frostedheart.town.strength", resident.getStrength()));
        lines.add(stat("gui.frostedheart.town.intelligence", resident.getIntelligence()));
        lines.add(stat("gui.frostedheart.town.nutrition_fat", resident.getNutrition().fat()));
        lines.add(stat("gui.frostedheart.town.nutrition_carbohydrate", resident.getNutrition().carbohydrate()));
        lines.add(stat("gui.frostedheart.town.nutrition_protein", resident.getNutrition().protein()));
        lines.add(stat("gui.frostedheart.town.nutrition_vegetable", resident.getNutrition().vegetable()));
        lines.add(new Line(Component.empty(), 0xFFFFFFFF));
        lines.add(new Line(proficiencyName.copy().append(Component.literal(
                ": " + Math.round(proficiency.applyAsDouble(resident)) + " / "
                        + Math.round(FHConfig.SERVER.TOWN.RESIDENT_PROGRESSION.maximumWorkProficiency.get()))),
                0xFFFFFF55));

        boolean eligible = canWork.test(resident);
        lines.add(new Line(Component.translatable("gui.frostedheart.town.can_work")
                .append(Component.literal(": "))
                .append(Component.translatable(eligible
                        ? "gui.frostedheart.common.yes"
                        : "gui.frostedheart.common.no")),
                eligible ? 0xFF55FF55 : 0xFFFF5555));
        if (!eligible) {
            if (resident.getHealth() <= 10) {
                lines.add(reason("gui.frostedheart.town.reason.health"));
            }
            if (resident.getMental() <= 5) {
                lines.add(reason("gui.frostedheart.town.reason.mental"));
            }
            if (resident.getHousePos() == null) {
                lines.add(reason("gui.frostedheart.town.reason.no_house"));
            }
        }
        lines.add(new Line(Component.translatable("gui.frostedheart.town.productivity",
                format(productivity.applyAsDouble(resident))), 0xFFFFFFFF));
        lines.add(new Line(contributionText.apply(contribution.applyAsDouble(resident)), 0xFFFFFFFF));
        double gain = eligible ? proficiencyGain.applyAsDouble(resident) : 0.0;
        lines.add(new Line(Component.translatable(
                "gui.frostedheart.town.proficiency_after_work", formatOne(gain)), 0xFF55FF55));
        return lines;
    }

    @Override
    public boolean onMousePressed(MouseButton button) {
        if (!isMouseOver() || button != MouseButton.LEFT) return false;
        if (getMouseX() >= getWidth() - SCROLLBAR_WIDTH - 2 && detailsScrollable()) {
            draggingDetails = true;
            updateDetailScroll();
            return true;
        }
        if (getMouseX() >= LIST_WIDTH || getMouseY() < LIST_TOP) return false;
        int row = (int) ((getMouseY() - LIST_TOP) / ROW_HEIGHT);
        if (row < 0 || row >= VISIBLE_RESIDENTS) return false;
        List<Resident> residents = residents();
        int index = residentScroll + row;
        if (index >= residents.size()) return false;
        selectedResident = residents.get(index).getUUID();
        detailScroll = 0;
        return true;
    }

    @Override
    public boolean onMouseScrolled(double scroll) {
        if (!isMouseOver()) return false;
        if (getMouseX() < LIST_WIDTH) {
            int max = Math.max(0, residents().size() - VISIBLE_RESIDENTS);
            if (max == 0) return false;
            residentScroll = Mth.clamp(
                    residentScroll - (int) Math.signum(scroll), 0, max);
            return true;
        }
        int max = Math.max(0,
                wrappedDetailLines(normalizeSelection(residents())).size() - DETAIL_VISIBLE);
        if (max == 0) return false;
        detailScroll = Mth.clamp(detailScroll - (int) Math.signum(scroll), 0, max);
        return true;
    }

    @Override
    public boolean onMouseDragged(MouseButton button, double dragX, double dragY) {
        if (!draggingDetails) return super.onMouseDragged(button, dragX, dragY);
        updateDetailScroll();
        return true;
    }

    @Override
    public void onMouseReleased(MouseButton button) {
        draggingDetails = false;
        super.onMouseReleased(button);
    }

    private void renderDetailScrollbar(
            GuiGraphics graphics, int x, int y, int lineCount, int maxScroll
    ) {
        if (maxScroll == 0) {
            detailScroll = 0;
            return;
        }
        int trackX = x + WIDTH - SCROLLBAR_WIDTH - 2;
        int trackY = y + 2;
        int trackHeight = HEIGHT - 4;
        int thumbHeight = detailThumbHeight(lineCount, trackHeight);
        int thumbY = trackY + (trackHeight - thumbHeight) * detailScroll / maxScroll;
        graphics.fill(trackX, trackY, trackX + SCROLLBAR_WIDTH,
                trackY + trackHeight, 0xFF202020);
        graphics.fill(trackX + 1, thumbY, trackX + SCROLLBAR_WIDTH - 1,
                thumbY + thumbHeight, draggingDetails ? 0xFFD0D0D0 : 0xFF909090);
    }

    private void updateDetailScroll() {
        int lineCount = wrappedDetailLines(normalizeSelection(residents())).size();
        int max = Math.max(0, lineCount - DETAIL_VISIBLE);
        if (max == 0) {
            detailScroll = 0;
            return;
        }
        int trackHeight = HEIGHT - 4;
        int thumbHeight = detailThumbHeight(lineCount, trackHeight);
        int travel = trackHeight - thumbHeight;
        if (travel <= 0) return;
        double top = getMouseY() - 2 - thumbHeight / 2.0;
        detailScroll = (int) Math.round(Mth.clamp(top / travel, 0.0, 1.0) * max);
    }

    private Resident normalizeSelection(List<Resident> residents) {
        if (residents.isEmpty()) {
            selectedResident = null;
            residentScroll = 0;
            detailScroll = 0;
            return null;
        }
        for (Resident resident : residents) {
            if (resident.getUUID().equals(selectedResident)) return resident;
        }
        selectedResident = residents.get(0).getUUID();
        detailScroll = 0;
        return residents.get(0);
    }

    private boolean detailsScrollable() {
        return wrappedDetailLines(normalizeSelection(residents())).size() > DETAIL_VISIBLE;
    }

    private List<VisualLine> wrappedDetailLines(Resident resident) {
        return wrapDetailLines(Minecraft.getInstance().font, detailLines(resident));
    }

    private static List<VisualLine> wrapDetailLines(Font font, List<Line> lines) {
        List<VisualLine> wrapped = new ArrayList<>();
        for (Line line : lines) {
            for (FormattedCharSequence text :
                    TownTextLayout.wrap(font, line.text(), DETAIL_TEXT_WIDTH)) {
                wrapped.add(new VisualLine(text, line.color()));
            }
        }
        return wrapped;
    }

    private List<Resident> residents() {
        List<Resident> residents = residentSource.get();
        return residents == null ? List.of() : residents;
    }

    private static Line stat(String key, double value) {
        return new Line(Component.translatable(key)
                .append(Component.literal(": " + Math.round(value) + " / 100")), 0xFFFFFFFF);
    }

    private static Line reason(String key) {
        return new Line(Component.literal("• ").append(Component.translatable(key)), 0xFFFF5555);
    }

    private static int detailThumbHeight(int lines, int height) {
        return Math.max(MIN_THUMB_HEIGHT, height * DETAIL_VISIBLE / lines);
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static String formatOne(double value) {
        return String.format(Locale.ROOT, "%+.1f", value);
    }

    private static void drawPanel(
            GuiGraphics graphics, int x, int y, int width, int height
    ) {
        graphics.fill(x, y, x + width, y + height, 0xE0101010);
        graphics.fill(x, y, x + width, y + 1, 0xFF373737);
        graphics.fill(x, y, x + 1, y + height, 0xFF373737);
        graphics.fill(x, y + height - 1, x + width, y + height, 0xFF8B8B8B);
        graphics.fill(x + width - 1, y, x + width, y + height, 0xFF8B8B8B);
    }

    private record Line(Component text, int color) {}

    private record VisualLine(FormattedCharSequence text, int color) {}
}
