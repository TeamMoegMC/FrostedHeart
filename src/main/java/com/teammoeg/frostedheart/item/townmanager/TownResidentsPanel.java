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

package com.teammoeg.frostedheart.item.townmanager;

import com.teammoeg.chorda.client.RenderingHint;
import com.teammoeg.chorda.client.cui.base.MouseButton;
import com.teammoeg.chorda.client.cui.base.TooltipBuilder;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.chorda.client.cui.base.UILayer;
import com.teammoeg.chorda.client.cui.base.Verifier;
import com.teammoeg.chorda.client.cui.theme.Coloring;
import com.teammoeg.chorda.client.cui.widgets.TextBox;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.content.town.TeamTown;
import com.teammoeg.frostedheart.content.town.TownNamingModel;
import com.teammoeg.frostedheart.content.town.building.AbstractTownBuilding;
import com.teammoeg.frostedheart.content.town.network.TownResidentNameEditRequestPacket;
import com.teammoeg.frostedheart.content.town.resident.Resident;
import com.teammoeg.frostedheart.content.town.tabs.TownTextLayout;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * 村民页签的内容面板。左侧为可滚动的居民名单，点击选择后
 * 右侧显示该居民的属性、住房/工作分配与各项熟练度。
 * 布局与交互参照 {@link com.teammoeg.frostedheart.content.town.tabs.TownWorkforcePanel}。
 * <p>
 * Content panel of the residents tab. A scrollable resident list on the left;
 * clicking a resident shows their attributes, housing/work assignment and work
 * proficiencies on the right. Layout and interaction follow TownWorkforcePanel.
 */
public class TownResidentsPanel extends UILayer {

    private static final int WIDTH = TownManagerScreen.CONTENT_WIDTH;
    private static final int HEIGHT = TownManagerScreen.CONTENT_HEIGHT;
    private static final int LIST_WIDTH = 96;
    private static final int LIST_TOP = 18;
    private static final int ROW_HEIGHT = 16;
    private static final int VISIBLE_ROWS = (HEIGHT - LIST_TOP - 4) / ROW_HEIGHT;
    private static final int DETAIL_LINE_HEIGHT = 12;
    private static final int DETAIL_TOP = 36;
    private static final int DETAIL_VISIBLE = (HEIGHT - DETAIL_TOP - 4) / DETAIL_LINE_HEIGHT;
    private static final int SCROLLBAR_WIDTH = 6;
    private static final int NAME_LABEL_WIDTH = 36;
    private static final int DETAIL_TEXT_WIDTH = WIDTH - LIST_WIDTH - SCROLLBAR_WIDTH - 13;
    private static final int MIN_THUMB_HEIGHT = 10;

    private final Supplier<TeamTown> townSource;
    private final ResidentNameBox lastNameBox;
    private final ResidentNameBox firstNameBox;

    @Nullable
    private UUID selectedResident;
    private int listScroll;
    private int detailScroll;
    private boolean draggingDetailBar;
    @Nullable
    private UUID editorResidentId;
    private String displayedNameKey = "";
    @Nullable
    private String pendingNameKey;
    private boolean updatingNameEditors;

    public TownResidentsPanel(UIElement parent, int x, int y, Supplier<TeamTown> townSource) {
        super(parent);
        this.townSource = townSource;
        setPos(x, y);
        setSize(WIDTH, HEIGHT);
        setScissorEnabled(false);
        lastNameBox = new ResidentNameBox(this, false);
        firstNameBox = new ResidentNameBox(this, true);
        int editorX = LIST_WIDTH + 5 + NAME_LABEL_WIDTH;
        int editorWidth = WIDTH - editorX - SCROLLBAR_WIDTH - 4;
        lastNameBox.setPos(editorX, 2);
        lastNameBox.setSize(editorWidth, 13);
        firstNameBox.setPos(editorX, 18);
        firstNameBox.setSize(editorWidth, 13);
    }

    @Override
    public void addUIElements() {
        add(lastNameBox);
        add(firstNameBox);
    }

    @Override
    public void alignWidgets() {
    }

    @Override
    public void drawBackground(GuiGraphics graphics, int x, int y, int width, int height, RenderingHint hint) {
        Font font = Minecraft.getInstance().font;
        List<Resident> residents = residents();
        Resident selected = normalizeSelection(residents);
        syncNameEditors(selected);
        drawPanel(graphics, x, y, width, height);
        graphics.fill(x + LIST_WIDTH, y + 1, x + LIST_WIDTH + 1, y + height - 1, 0xFF777777);
        graphics.drawString(font,
                Component.translatable("gui.frostedheart.town_manager.residents"),
                x + 4, y + 6, 0xFFFFAA00, true);
        renderList(graphics, font, x, y, residents);
        graphics.drawString(font, Component.translatable("gui.frostedheart.town_manager.last_name"),
                x + LIST_WIDTH + 5, y + 5, 0xFFAAAAAA, false);
        graphics.drawString(font, Component.translatable("gui.frostedheart.town_manager.first_name"),
                x + LIST_WIDTH + 5, y + 21, 0xFFAAAAAA, false);
        renderDetails(graphics, font, x, y, detailLines(selected));
    }

    private void renderList(GuiGraphics graphics, Font font, int x, int y, List<Resident> residents) {
        int maxScroll = Math.max(0, residents.size() - VISIBLE_ROWS);
        listScroll = Mth.clamp(listScroll, 0, maxScroll);
        for (int row = 0; row < VISIBLE_ROWS; row++) {
            int index = listScroll + row;
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
            int trackHeight = VISIBLE_ROWS * ROW_HEIGHT;
            int thumbHeight = Math.max(MIN_THUMB_HEIGHT, trackHeight * VISIBLE_ROWS / residents.size());
            int thumbY = trackY + (trackHeight - thumbHeight) * listScroll / maxScroll;
            graphics.fill(x + LIST_WIDTH - 3, trackY, x + LIST_WIDTH - 1, trackY + trackHeight, 0xFF222222);
            graphics.fill(x + LIST_WIDTH - 3, thumbY, x + LIST_WIDTH - 1, thumbY + thumbHeight, 0xFFAAAAAA);
        }
    }

    private void renderDetails(GuiGraphics graphics, Font font, int x, int y, List<Line> lines) {
        List<VisualLine> visualLines = wrapDetailLines(font, lines);
        int maxScroll = Math.max(0, visualLines.size() - DETAIL_VISIBLE);
        detailScroll = Mth.clamp(detailScroll, 0, maxScroll);
        int textX = x + LIST_WIDTH + 5;
        graphics.enableScissor(x + LIST_WIDTH + 1, y + 1, x + WIDTH - SCROLLBAR_WIDTH - 3, y + HEIGHT - 1);
        for (int row = 0; row < DETAIL_VISIBLE; row++) {
            int index = detailScroll + row;
            if (index >= visualLines.size()) break;
            VisualLine line = visualLines.get(index);
            graphics.drawString(font, line.text(),
                    textX, y + DETAIL_TOP + row * DETAIL_LINE_HEIGHT, line.color(), true);
        }
        graphics.disableScissor();
        if (maxScroll > 0) {
            int trackX = x + WIDTH - SCROLLBAR_WIDTH - 2;
            int trackY = y + 2;
            int trackHeight = HEIGHT - 4;
            int thumbHeight = Math.max(MIN_THUMB_HEIGHT, trackHeight * DETAIL_VISIBLE / visualLines.size());
            int thumbY = trackY + (trackHeight - thumbHeight) * detailScroll / maxScroll;
            graphics.fill(trackX, trackY, trackX + SCROLLBAR_WIDTH, trackY + trackHeight, 0xFF202020);
            graphics.fill(trackX + 1, thumbY, trackX + SCROLLBAR_WIDTH - 1, thumbY + thumbHeight,
                    draggingDetailBar ? 0xFFD0D0D0 : 0xFF909090);
        }
    }

    private List<Line> detailLines(@Nullable Resident resident) {
        if (resident == null) {
            return List.of(new Line(
                    Component.translatable("gui.frostedheart.town_manager.no_residents"), 0xFFAAAAAA));
        }
        TeamTown town = townSource.get();
        List<Line> lines = new ArrayList<>();
        lines.add(new Line(Component.translatable("gui.frostedheart.town_manager.age")
                .append(Component.literal(": "))
                .append(Component.translatable(Resident.ageLangKey(resident.getAge()))), 0xFFFFFFFF));
        lines.add(stat("gui.frostedheart.town.health", resident.getHealth()));
        lines.add(stat("gui.frostedheart.town.mental", resident.getMental()));
        lines.add(stat("gui.frostedheart.town.strength", resident.getStrength()));
        lines.add(stat("gui.frostedheart.town.intelligence", resident.getIntelligence()));
        lines.add(stat("gui.frostedheart.town.nutrition_fat", resident.getNutrition().fat()));
        lines.add(stat("gui.frostedheart.town.nutrition_carbohydrate", resident.getNutrition().carbohydrate()));
        lines.add(stat("gui.frostedheart.town.nutrition_protein", resident.getNutrition().protein()));
        lines.add(stat("gui.frostedheart.town.nutrition_vegetable", resident.getNutrition().vegetable()));
        lines.add(new Line(Component.translatable("gui.frostedheart.town_manager.education")
                .append(Component.literal(": "))
                .append(educationLevel(resident.getEducationLevel())), 0xFFFFFFFF));
        lines.add(new Line(Component.empty(), 0xFFFFFFFF));
        lines.add(assignment("gui.frostedheart.town_manager.house", resident.getHousePos(), town));
        lines.add(assignment("gui.frostedheart.town_manager.work", resident.getWorkPos(), town));
        if (!resident.getWorkProficiency().isEmpty()) {
            lines.add(new Line(Component.empty(), 0xFFFFFFFF));
            lines.add(new Line(Component.translatable("gui.frostedheart.town_manager.proficiency"), 0xFFFFFF55));
            for (Map.Entry<String, Double> entry : resident.getWorkProficiency().entrySet()) {
                lines.add(new Line(Component.literal("• ")
                        .append(buildingName(entry.getKey()))
                        .append(Component.literal(": "))
                        .append(statusBar(entry.getValue())), 0xFFFFFFFF,
                        statusTooltip(entry.getValue())));
            }
        }
        return lines;
    }

    private Line assignment(String labelKey, @Nullable BlockPos pos, @Nullable TeamTown town) {
        Component label = Component.translatable(labelKey).append(Component.literal(": "));
        if (pos == null || town == null) {
            return new Line(label.copy().append(
                    Component.translatable("gui.frostedheart.town_manager.none")), 0xFFFF5555);
        }
        AbstractTownBuilding building = town.getTownBuilding(pos).orElse(null);
        Component name = building == null
                ? Component.translatable("gui.frostedheart.town_manager.none")
                : buildingName(building.getClass().getSimpleName());
        return new Line(label.copy().append(name), 0xFF55FF55);
    }

    /**
     * 建筑类型的显示名。优先使用本地化键，缺失时回退到类名。
     * <p>
     * Display name of a building type. Prefers localization, falls back to the
     * class simple name.
     */
    static Component buildingName(String simpleClassName) {
        return Component.translatableWithFallback(
                "gui.frostedheart.town_manager.building." + simpleClassName, simpleClassName);
    }

    @Override
    public boolean onMousePressed(MouseButton button) {
        if (!isMouseOver() || button != MouseButton.LEFT) return false;
        if (super.onMousePressed(button)) return true;
        if (getMouseX() >= WIDTH - SCROLLBAR_WIDTH - 2 && detailScrollable()) {
            draggingDetailBar = true;
            updateDetailScrollFromMouse();
            return true;
        }
        if (getMouseX() >= LIST_WIDTH || getMouseY() < LIST_TOP) return false;
        int row = (int) ((getMouseY() - LIST_TOP) / ROW_HEIGHT);
        if (row < 0 || row >= VISIBLE_ROWS) return false;
        List<Resident> residents = residents();
        int index = listScroll + row;
        if (index >= residents.size()) return false;
        commitResidentName();
        selectedResident = residents.get(index).getUUID();
        editorResidentId = null;
        detailScroll = 0;
        return true;
    }

    @Override
    public boolean onMouseScrolled(double scroll) {
        if (!isMouseOver()) return false;
        if (getMouseX() < LIST_WIDTH) {
            int max = Math.max(0, residents().size() - VISIBLE_ROWS);
            if (max == 0) return false;
            listScroll = Mth.clamp(listScroll - (int) Math.signum(scroll), 0, max);
            return true;
        }
        int max = Math.max(0, wrapDetailLines(Minecraft.getInstance().font,
                detailLines(normalizeSelection(residents()))).size() - DETAIL_VISIBLE);
        if (max == 0) return false;
        detailScroll = Mth.clamp(detailScroll - (int) Math.signum(scroll), 0, max);
        return true;
    }

    @Override
    public boolean onMouseDragged(MouseButton button, double dragX, double dragY) {
        if (!draggingDetailBar) return super.onMouseDragged(button, dragX, dragY);
        updateDetailScrollFromMouse();
        return true;
    }

    @Override
    public void onMouseReleased(MouseButton button) {
        draggingDetailBar = false;
        super.onMouseReleased(button);
    }

    @Override
    public void getTooltip(TooltipBuilder tooltip) {
        super.getTooltip(tooltip);
        if (!isMouseOver() || getMouseX() <= LIST_WIDTH
                || getMouseY() < DETAIL_TOP || getMouseY() >= HEIGHT) return;
        List<VisualLine> lines = wrapDetailLines(Minecraft.getInstance().font,
                detailLines(normalizeSelection(residents())));
        int index = detailScroll + (int) ((getMouseY() - DETAIL_TOP) / DETAIL_LINE_HEIGHT);
        if (index < 0 || index >= lines.size()) return;
        Component lineTooltip = lines.get(index).tooltip();
        if (lineTooltip != null) tooltip.accept(lineTooltip);
    }

    private void updateDetailScrollFromMouse() {
        int lineCount = wrapDetailLines(Minecraft.getInstance().font,
                detailLines(normalizeSelection(residents()))).size();
        int max = Math.max(0, lineCount - DETAIL_VISIBLE);
        if (max == 0) {
            detailScroll = 0;
            return;
        }
        int trackHeight = HEIGHT - 4;
        int thumbHeight = Math.max(MIN_THUMB_HEIGHT, trackHeight * DETAIL_VISIBLE / lineCount);
        int travel = trackHeight - thumbHeight;
        if (travel <= 0) return;
        double top = getMouseY() - 2 - thumbHeight / 2.0;
        detailScroll = (int) Math.round(Mth.clamp(top / travel, 0.0, 1.0) * max);
    }

    private boolean detailScrollable() {
        return wrapDetailLines(Minecraft.getInstance().font,
                detailLines(normalizeSelection(residents()))).size() > DETAIL_VISIBLE;
    }

    /**
     * 维持选中态：居民消失时回退到列表首位，列表为空时清空选择。
     * <p>
     * Keeps the selection valid: falls back to the first resident when the
     * selected one disappears, clears selection when the list is empty.
     */
    @Nullable
    private Resident normalizeSelection(List<Resident> residents) {
        if (residents.isEmpty()) {
            selectedResident = null;
            listScroll = 0;
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

    private List<Resident> residents() {
        TeamTown town = townSource.get();
        if (town == null) return List.of();
        return List.copyOf(town.getAllResidents());
    }

    private static Line stat(String key, double value) {
        return new Line(Component.translatable(key)
                .append(Component.literal(": "))
                .append(statusBar(value)), 0xFFFFFFFF, statusTooltip(value));
    }

    private static Component educationLevel(int level) {
        if (level >= 0 && level <= 5) {
            return Component.translatable(
                    "gui.frostedheart.town_manager.education_level." + level);
        }
        return Component.translatable(
                "gui.frostedheart.town_manager.education_level.unknown", level);
    }

    private static Component statusTooltip(double value) {
        double bounded = Math.max(0.0, Math.min(100.0, value));
        String text = bounded == Math.floor(bounded)
                ? String.valueOf((long) bounded)
                : String.format(java.util.Locale.ROOT, "%.1f", bounded);
        return Component.translatable("gui.frostedheart.town_manager.status_value", text);
    }

    private static Component statusBar(double value) {
        double bounded = Math.max(0.0, Math.min(100.0, value));
        int filled = (int) Math.round(bounded / 10.0);
        ChatFormatting activeColor = bounded < 35.0 ? ChatFormatting.RED
                : bounded < 70.0 ? ChatFormatting.GOLD : ChatFormatting.GREEN;
        var bar = Component.empty();
        for (int index = 0; index < 10; index++) {
            bar.append(Component.literal("■").withStyle(index < filled
                    ? activeColor : ChatFormatting.DARK_GRAY));
        }
        return bar;
    }

    private static List<VisualLine> wrapDetailLines(Font font, List<Line> lines) {
        List<VisualLine> wrapped = new ArrayList<>();
        for (Line line : lines) {
            for (FormattedCharSequence text : TownTextLayout.wrap(font, line.text(), DETAIL_TEXT_WIDTH)) {
                wrapped.add(new VisualLine(text, line.color(), line.tooltip()));
            }
        }
        return wrapped;
    }

    private static void drawPanel(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, 0xE0101010);
        graphics.fill(x, y, x + width, y + 1, 0xFF373737);
        graphics.fill(x, y, x + 1, y + height, 0xFF373737);
        graphics.fill(x, y + height - 1, x + width, y + height, 0xFF8B8B8B);
        graphics.fill(x + width - 1, y, x + width, y + height, 0xFF8B8B8B);
    }

    private void syncNameEditors(@Nullable Resident resident) {
        if (resident == null) {
            editorResidentId = null;
            displayedNameKey = "";
            pendingNameKey = null;
            setNameEditorText("", "");
            lastNameBox.setEnabled(false);
            firstNameBox.setEnabled(false);
            return;
        }
        lastNameBox.setEnabled(true);
        firstNameBox.setEnabled(true);
        if (!resident.getUUID().equals(editorResidentId)) {
            editorResidentId = resident.getUUID();
            setNameEditorText(resident.getFirstName(), resident.getLastName());
            displayedNameKey = nameKey(resident.getUUID(), resident.getFirstName(), resident.getLastName());
            pendingNameKey = null;
        } else if (!lastNameBox.isFocused() && !firstNameBox.isFocused()) {
            String authoritativeKey = nameKey(resident.getUUID(), resident.getFirstName(), resident.getLastName());
            if (pendingNameKey != null) {
                if (!authoritativeKey.equals(pendingNameKey)) return;
                pendingNameKey = null;
            }
            if (!authoritativeKey.equals(displayedNameKey)) {
                setNameEditorText(resident.getFirstName(), resident.getLastName());
                displayedNameKey = authoritativeKey;
            }
        }
    }

    private void resetResidentName() {
        Resident selected = normalizeSelection(residents());
        editorResidentId = null;
        syncNameEditors(selected);
    }

    private void commitResidentName() {
        if (editorResidentId == null) return;
        TownNamingModel.normalizeResidentName(firstNameBox.getText(), lastNameBox.getText())
                .ifPresent(name -> {
                    String key = nameKey(editorResidentId, name.firstName(), name.lastName());
                    if (key.equals(pendingNameKey)) return;
                    if (pendingNameKey == null && key.equals(displayedNameKey)) return;
                    pendingNameKey = key;
                    FHNetwork.INSTANCE.sendToServer(new TownResidentNameEditRequestPacket(
                            editorResidentId, name.firstName(), name.lastName()));
                });
    }

    private static String nameKey(UUID residentId, String firstName, String lastName) {
        return residentId + "\u0000" + firstName + "\u0000" + lastName;
    }

    private void setNameEditorText(String firstName, String lastName) {
        updatingNameEditors = true;
        try {
            if (!lastName.equals(lastNameBox.getText())) lastNameBox.setText(lastName, false);
            if (!firstName.equals(firstNameBox.getText())) firstNameBox.setText(firstName, false);
        } finally {
            updatingNameEditors = false;
        }
    }

    private final class ResidentNameBox extends TextBox {
        private boolean focusedLastFrame;

        ResidentNameBox(UILayer parent, boolean required) {
            super(parent);
            setMaxLength(TownNamingModel.MAX_RESIDENT_NAME_PART_LENGTH);
            textColor = Coloring.argb(0xFFFFAA00);
            errorColor = Coloring.argb(0xFFFF5555);
            if (required) {
                setFilter(Verifier.successOrComponent(value -> !value.strip().isEmpty(),
                        () -> Component.translatable("gui.frostedheart.town_manager.first_name_required")));
            }
        }

        @Override
        public void onTextChanged() {
            // TextBox fires this for cursor motion and programmatic updates too.
            // Commit once on Enter, Tab, or focus loss instead.
        }

        @Override
        public void onEnterPressed() {
            commitResidentName();
        }

        @Override
        public void onTabPressed() {
            commitResidentName();
        }

        @Override
        public boolean onKeyPressed(int keyCode, int scanCode, int modifier) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE && isFocused()) resetResidentName();
            return super.onKeyPressed(keyCode, scanCode, modifier);
        }

        @Override
        public void render(GuiGraphics graphics, int x, int y, int width, int height, RenderingHint hint) {
            boolean focused = isFocused();
            if (!focused && focusedLastFrame && !updatingNameEditors) commitResidentName();
            focusedLastFrame = focused;
            super.render(graphics, x, y, width, height, hint);
        }

        @Override
        public void drawTextBox(GuiGraphics graphics, int x, int y, int width, int height, RenderingHint hint) {
            if (!isFocused()) return;
            graphics.fill(x, y, x + width, y + height, 0xFF181818);
            graphics.fill(x, y, x + width, y + 1, 0xFFFFAA00);
            graphics.fill(x, y + height - 1, x + width, y + height, 0xFFFFAA00);
        }

        @Override
        public void getTooltip(TooltipBuilder tooltip) {
            super.getTooltip(tooltip);
            tooltip.accept(Component.translatable(
                    "gui.frostedheart.town_manager.edit_resident_name_hint"));
        }
    }

    private record Line(Component text, int color, @Nullable Component tooltip) {
        private Line(Component text, int color) {
            this(text, color, null);
        }
    }

    private record VisualLine(
            FormattedCharSequence text,
            int color,
            @Nullable Component tooltip
    ) {}
}
