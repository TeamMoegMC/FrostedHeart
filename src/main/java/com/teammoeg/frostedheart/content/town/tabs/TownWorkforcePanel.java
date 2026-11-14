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

    // Sizes follow the panel, so a debug sheet can give it the whole page; the constants are its standalone size.
    private int listWidth() {
        return Math.max(LIST_WIDTH, Math.min(150, getWidth() / 3));
    }

    private int visibleResidents() {
        return Math.max(1, (getHeight() - LIST_TOP - 4) / ROW_HEIGHT);
    }

    private int detailVisible() {
        return Math.max(1, (getHeight() - 10) / DETAIL_LINE_HEIGHT);
    }

    private int detailTextWidth() {
        return Math.max(40, getWidth() - listWidth() - SCROLLBAR_WIDTH - 12);
    }

    private final Supplier<List<Resident>> residentSource;
    private final Predicate<Resident> canWork;
    private final ToDoubleFunction<Resident> proficiency;
    private final ToDoubleFunction<Resident> productivity;
    private final ToDoubleFunction<Resident> contribution;
    private final ToDoubleFunction<Resident> proficiencyGain;
    private final Component proficiencyName;
    private final Function<Double, Component> contributionText;
    private final Function<Resident, List<Component>> extraDetails;

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
        this(parent, x, y, residentSource, canWork, proficiency, productivity, contribution, proficiencyGain,
                proficiencyName, contributionText, resident -> List.of());
    }

    /**
     * @param extraDetails 追加在个人贡献之后的说明行 / extra detail lines shown after the resident's contribution
     */
    public TownWorkforcePanel(
            UIElement parent, int x, int y,
            Supplier<List<Resident>> residentSource,
            Predicate<Resident> canWork,
            ToDoubleFunction<Resident> proficiency,
            ToDoubleFunction<Resident> productivity,
            ToDoubleFunction<Resident> contribution,
            ToDoubleFunction<Resident> proficiencyGain,
            Component proficiencyName,
            Function<Double, Component> contributionText,
            Function<Resident, List<Component>> extraDetails
    ) {
        super(parent);
        this.extraDetails = extraDetails;
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

    private boolean paper;

    /**
     * 画在方格纸上：墨色文字、不带阴影，选中行用淡黄标出。布告板的调试页用它。
     * Drawn on graph paper: ink text without shadows, the chosen row marked in pale yellow. The notice board's debug
     * pages use it.
     */
    public TownWorkforcePanel paper() {
        paper = true;
        return this;
    }

    private int ink(int color) {
        return paper ? TownInfoPanel.paperInk(color) : color;
    }

    @Override
    public void render(
            GuiGraphics graphics, int x, int y, int width, int height,
            RenderingHint hint
    ) {
        Font font = Minecraft.getInstance().font;
        List<Resident> residents = residents();
        Resident selected = normalizeSelection(residents);
        if (paper) com.teammoeg.frostedheart.content.town.client.board.NoticeBoardIcons.GRID.draw(graphics, x, y, width, height);
        else drawPanel(graphics, x, y, width, height);
        graphics.fill(x + listWidth(), y + 1, x + listWidth() + 1, y + height - 1, paper ? 0xFFB4B2A9 : 0xFF777777);
        graphics.drawString(font, Component.translatable("gui.frostedheart.town.workers"),
                x + 4, y + 6, ink(0xFFFFAA00), !paper);
        renderList(graphics, font, x, y, residents);
        renderDetails(graphics, font, x, y, detailLines(selected));
    }

    private void renderList(
            GuiGraphics graphics, Font font, int x, int y, List<Resident> residents
    ) {
        int maxScroll = Math.max(0, residents.size() - visibleResidents());
        residentScroll = Mth.clamp(residentScroll, 0, maxScroll);
        for (int row = 0; row < visibleResidents(); row++) {
            int index = residentScroll + row;
            if (index >= residents.size()) break;
            Resident resident = residents.get(index);
            int rowY = y + LIST_TOP + row * ROW_HEIGHT;
            if (resident.getUUID().equals(selectedResident)) {
                graphics.fill(x + 2, rowY, x + listWidth() - 3, rowY + ROW_HEIGHT - 1, paper ? 0x70FAC775 : 0xA0446688);
            } else if (getMouseX() >= 2 && getMouseX() < listWidth() - 3
                    && getMouseY() >= LIST_TOP + row * ROW_HEIGHT
                    && getMouseY() < LIST_TOP + (row + 1) * ROW_HEIGHT) {
                graphics.fill(x + 2, rowY, x + listWidth() - 3, rowY + ROW_HEIGHT - 1, paper ? 0x20000000 : 0x60444444);
            }
            graphics.drawString(font,
                    TownTextLayout.ellipsize(font, resident.toString(), listWidth() - 9),
                    x + 5, rowY + 4, ink(0xFFFFFFFF), !paper);
        }
        if (maxScroll > 0) {
            int trackY = y + LIST_TOP;
            int trackHeight = visibleResidents() * ROW_HEIGHT;
            int thumbHeight = Math.max(MIN_THUMB_HEIGHT,
                    trackHeight * visibleResidents() / residents.size());
            int thumbY = trackY + (trackHeight - thumbHeight) * residentScroll / maxScroll;
            graphics.fill(x + listWidth() - 3, trackY,
                    x + listWidth() - 1, trackY + trackHeight, paper ? 0xFFD9D5C8 : 0xFF222222);
            graphics.fill(x + listWidth() - 3, thumbY,
                    x + listWidth() - 1, thumbY + thumbHeight, paper ? 0xFF8A7A60 : 0xFFAAAAAA);
        }
    }

    private void renderDetails(
            GuiGraphics graphics, Font font, int x, int y, List<Line> lines
    ) {
        List<VisualLine> visualLines = wrapDetailLines(font, lines);
        int maxScroll = Math.max(0, visualLines.size() - detailVisible());
        detailScroll = Mth.clamp(detailScroll, 0, maxScroll);
        int textX = x + listWidth() + 5;
        for (int row = 0; row < detailVisible(); row++) {
            int index = detailScroll + row;
            if (index >= visualLines.size()) break;
            VisualLine line = visualLines.get(index);
            if (paper) graphics.drawString(font, TownInfoPanel.inked(line.text(), ink(line.color())),
                    textX, y + 6 + row * DETAIL_LINE_HEIGHT, ink(line.color()), false);
            else graphics.drawString(font, line.text(),
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
        for (Component extra : extraDetails.apply(resident)) lines.add(new Line(extra, 0xFFFFFFFF));
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
        if (getMouseX() >= listWidth() || getMouseY() < LIST_TOP) return false;
        int row = (int) ((getMouseY() - LIST_TOP) / ROW_HEIGHT);
        if (row < 0 || row >= visibleResidents()) return false;
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
        if (getMouseX() < listWidth()) {
            int max = Math.max(0, residents().size() - visibleResidents());
            if (max == 0) return false;
            residentScroll = Mth.clamp(
                    residentScroll - (int) Math.signum(scroll), 0, max);
            return true;
        }
        int max = Math.max(0,
                wrappedDetailLines(normalizeSelection(residents())).size() - detailVisible());
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
        int trackX = x + getWidth() - SCROLLBAR_WIDTH - 2;
        int trackY = y + 2;
        int trackHeight = getHeight() - 4;
        int thumbHeight = detailThumbHeight(lineCount, trackHeight);
        int thumbY = trackY + (trackHeight - thumbHeight) * detailScroll / maxScroll;
        graphics.fill(trackX, trackY, trackX + SCROLLBAR_WIDTH,
                trackY + trackHeight, paper ? 0xFFD9D5C8 : 0xFF202020);
        graphics.fill(trackX + 1, thumbY, trackX + SCROLLBAR_WIDTH - 1,
                thumbY + thumbHeight, paper ? (draggingDetails ? 0xFF6B5A40 : 0xFF8A7A60) : draggingDetails ? 0xFFD0D0D0 : 0xFF909090);
    }

    private void updateDetailScroll() {
        int lineCount = wrappedDetailLines(normalizeSelection(residents())).size();
        int max = Math.max(0, lineCount - detailVisible());
        if (max == 0) {
            detailScroll = 0;
            return;
        }
        int trackHeight = getHeight() - 4;
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
        return wrappedDetailLines(normalizeSelection(residents())).size() > detailVisible();
    }

    private List<VisualLine> wrappedDetailLines(Resident resident) {
        return wrapDetailLines(Minecraft.getInstance().font, detailLines(resident));
    }

    private List<VisualLine> wrapDetailLines(Font font, List<Line> lines) {
        List<VisualLine> wrapped = new ArrayList<>();
        for (Line line : lines) {
            for (FormattedCharSequence text :
                    TownTextLayout.wrap(font, line.text(), detailTextWidth())) {
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

    private int detailThumbHeight(int lines, int height) {
        return Math.max(MIN_THUMB_HEIGHT, height * detailVisible() / lines);
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
