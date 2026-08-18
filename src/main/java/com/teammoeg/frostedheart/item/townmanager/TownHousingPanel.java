/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.item.townmanager;

import com.teammoeg.chorda.client.RenderingHint;
import com.teammoeg.chorda.client.cui.base.MouseButton;
import com.teammoeg.chorda.client.cui.base.TooltipBuilder;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.content.town.TeamTown;
import com.teammoeg.frostedheart.content.town.TownHousingPlan;
import com.teammoeg.frostedheart.content.town.building.AbstractTownBuilding;
import com.teammoeg.frostedheart.content.town.buildings.house.HouseBuilding;
import com.teammoeg.frostedheart.content.town.network.TownHousingEditRequestPacket;
import com.teammoeg.frostedheart.content.town.resident.Resident;
import com.teammoeg.frostedheart.content.town.tabs.TownTextLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/** Drag housing priority and edit guaranteed-ration residents per house. */
public final class TownHousingPanel extends UIElement {
    private static final int WIDTH = TownManagerScreen.CONTENT_WIDTH;
    private static final int HEIGHT = TownManagerScreen.CONTENT_HEIGHT;
    private static final int LIST_TOP = 32;
    private static final int ROW_HEIGHT = 28;
    private static final int VISIBLE_ROWS = 6;
    private static final int HANDLE_RIGHT = 14;
    private static final int BAR_X = 138;
    private static final int BAR_WIDTH = WIDTH - BAR_X - 8;
    private static final int BAR_Y = 18;
    private final Supplier<TeamTown> townSource;
    private TownHousingPlan observed = TownHousingPlan.EMPTY;
    private final List<BlockPos> draftOrder = new ArrayList<>();
    private final Map<BlockPos, Integer> draftGuarantees = new HashMap<>();
    private int scroll;
    @Nullable private BlockPos dragged;
    private int dropIndex;
    @Nullable private BlockPos editingTarget;
    private int targetBeforeDrag;

    public TownHousingPanel(UIElement parent, int x, int y, Supplier<TeamTown> townSource) {
        super(parent);
        this.townSource = townSource;
        setPos(x, y);
        setSize(WIDTH, HEIGHT);
    }

    @Override
    public void render(GuiGraphics graphics, int x, int y, int width, int height,
                       RenderingHint hint) {
        TeamTown town = townSource.get();
        sync(town);
        drawPanel(graphics, x, y, width, height);
        Font font = Minecraft.getInstance().font;
        graphics.drawString(font,
                Component.translatable("gui.frostedheart.town_manager.housing_care"),
                x + 4, y + 4, 0xFFFFAA00, true);
        if (town == null) return;
        int housed = (int) town.getAllResidents().stream()
                .filter(resident -> resident.getHousePos() != null).count();
        int guarantees = draftGuarantees.values().stream().mapToInt(Integer::intValue).sum();
        Component summary = Component.translatable(
                "gui.frostedheart.town_manager.housing_summary",
                housed, town.getAllResidents().size(), guarantees);
        graphics.drawString(font,
                TownTextLayout.ellipsize(font, summary.getString(), width - 8),
                x + 4, y + 17, 0xFFAAAAAA, false);
        renderRows(graphics, font, x, y, town);
        renderDropMarker(graphics, x, y);
    }

    private void sync(@Nullable TeamTown town) {
        if (town == null || dragged != null || editingTarget != null) return;
        TownHousingPlan plan = town.getHousingPlan();
        if (plan.equals(observed)) return;
        observed = plan;
        draftOrder.clear();
        draftGuarantees.clear();
        for (TownHousingPlan.Entry entry : plan.entries()) {
            draftOrder.add(entry.building());
            draftGuarantees.put(entry.building(), entry.guaranteedResidents());
        }
        scroll = Mth.clamp(scroll, 0, maxScroll());
    }

    private void renderRows(GuiGraphics graphics, Font font, int x, int y, TeamTown town) {
        for (int row = 0; row < VISIBLE_ROWS; row++) {
            int index = scroll + row;
            if (index >= draftOrder.size()) break;
            BlockPos pos = draftOrder.get(index);
            AbstractTownBuilding value = town.getTownBuildings().get(pos);
            if (!(value instanceof HouseBuilding house)) continue;
            int rowY = y + LIST_TOP + row * ROW_HEIGHT;
            boolean workable = house.isBuildingWorkable();
            int capacity = Math.max(0, house.getMaxResidents());
            int guarantee = Math.max(0, draftGuarantees.getOrDefault(pos, 0));
            int occupancy = (int) town.getAllResidents().stream()
                    .filter(resident -> pos.equals(resident.getHousePos())).count();
            int background = pos.equals(dragged) ? 0xA0446688
                    : workable ? 0x90202020 : 0x90404040;
            graphics.fill(x + 2, rowY, x + WIDTH - 5, rowY + ROW_HEIGHT - 2, background);
            graphics.drawString(font, "≡", x + 4, rowY + 9,
                    workable ? 0xFFCCCCCC : 0xFF777777, false);
            graphics.drawString(font,
                    TownTextLayout.ellipsize(font,
                            Component.translatable("gui.frostedheart.town_manager.house").getString(),
                            44), x + 15, rowY + 3,
                    workable ? 0xFFFFFFFF : 0xFF999999, false);
            graphics.drawString(font, pos.getX() + ", " + pos.getY() + ", " + pos.getZ(),
                    x + 15, rowY + 15, 0xFF888888, false);
            Component detail = Component.translatable(
                    "gui.frostedheart.town_manager.housing_counts",
                    occupancy, guarantee, capacity, Math.round(house.getComfortRating() * 100));
            graphics.drawString(font,
                    TownTextLayout.ellipsize(font, detail.getString(), BAR_WIDTH),
                    x + BAR_X, rowY + 3, workable ? 0xFFFFFFFF : 0xFF888888, false);
            renderBar(graphics, x + BAR_X, rowY + BAR_Y,
                    occupancy, guarantee, capacity, workable);
        }
    }

    private static void renderBar(GuiGraphics graphics, int x, int y,
                                  int occupancy, int guarantee, int capacity,
                                  boolean workable) {
        graphics.fill(x, y, x + BAR_WIDTH, y + 6, 0xFF181818);
        if (!workable || capacity <= 0) {
            graphics.fill(x + 1, y + 1, x + BAR_WIDTH - 1, y + 5, 0xFF555555);
            return;
        }
        int occupiedWidth = BAR_WIDTH * Math.min(occupancy, capacity) / capacity;
        int guaranteeWidth = BAR_WIDTH * Math.min(guarantee, capacity) / capacity;
        graphics.fill(x, y, x + occupiedWidth, y + 6, 0xFF5599CC);
        int marker = Mth.clamp(guaranteeWidth, 0, BAR_WIDTH - 1);
        graphics.fill(x + marker, y - 1, x + marker + 1, y + 7, 0xFFFFCC55);
    }

    @Override
    public boolean onMousePressed(MouseButton button) {
        if (button != MouseButton.LEFT || !isMouseOver()) return false;
        int index = rowAtMouse();
        if (index < 0 || index >= draftOrder.size()) return false;
        BlockPos pos = draftOrder.get(index);
        if (getMouseX() < HANDLE_RIGHT) {
            dragged = pos;
            dropIndex = index;
            return true;
        }
        if (getMouseX() >= BAR_X && getMouseX() <= BAR_X + BAR_WIDTH) {
            AbstractTownBuilding value = building(pos);
            if (!(value instanceof HouseBuilding house) || house.getMaxResidents() <= 0) return false;
            editingTarget = pos;
            targetBeforeDrag = draftGuarantees.getOrDefault(pos, 0);
            updateTarget(house.getMaxResidents());
            return true;
        }
        return false;
    }

    @Override
    public boolean onMouseDragged(MouseButton button, double dragX, double dragY) {
        if (button != MouseButton.LEFT) return false;
        if (editingTarget != null) {
            AbstractTownBuilding value = building(editingTarget);
            if (value instanceof HouseBuilding house) updateTarget(house.getMaxResidents());
            return true;
        }
        if (dragged == null) return false;
        dropIndex = Mth.clamp(scroll + (int) Math.floor(
                (getMouseY() - LIST_TOP + ROW_HEIGHT / 2.0) / ROW_HEIGHT),
                0, draftOrder.size());
        return true;
    }

    @Override
    public void onMouseReleased(MouseButton button) {
        if (button == MouseButton.LEFT && editingTarget != null) {
            int target = draftGuarantees.getOrDefault(editingTarget, 0);
            if (target != targetBeforeDrag) FHNetwork.INSTANCE.sendToServer(
                    TownHousingEditRequestPacket.setGuarantee(editingTarget, target));
            editingTarget = null;
        }
        if (button == MouseButton.LEFT && dragged != null) {
            int old = draftOrder.indexOf(dragged);
            BlockPos moved = dragged;
            if (old >= 0) {
                draftOrder.remove(old);
                int insertion = dropIndex > old ? dropIndex - 1 : dropIndex;
                insertion = Mth.clamp(insertion, 0, draftOrder.size());
                draftOrder.add(insertion, moved);
                if (insertion != old) {
                    Optional<BlockPos> before = insertion + 1 < draftOrder.size()
                            ? Optional.of(draftOrder.get(insertion + 1)) : Optional.empty();
                    FHNetwork.INSTANCE.sendToServer(
                            TownHousingEditRequestPacket.move(moved, before));
                }
            }
            dragged = null;
        }
        super.onMouseReleased(button);
    }

    @Override
    public boolean onMouseScrolled(double amount) {
        if (!isMouseOver() || maxScroll() <= 0) return false;
        scroll = Mth.clamp(scroll - (int) Math.signum(amount), 0, maxScroll());
        return true;
    }

    @Override
    public void getTooltip(TooltipBuilder tooltip) {
        if (!isMouseOver()) return;
        tooltip.accept(Component.translatable(
                "gui.frostedheart.town_manager.housing_priority_help"));
        tooltip.accept(Component.translatable(
                "gui.frostedheart.town_manager.housing_guarantee_help"));
    }

    private void updateTarget(int capacity) {
        double fraction = Mth.clamp((getMouseX() - BAR_X) / (double) BAR_WIDTH, 0.0, 1.0);
        draftGuarantees.put(editingTarget, (int) Math.round(fraction * capacity));
    }

    private int rowAtMouse() {
        if (getMouseY() < LIST_TOP || getMouseY() >= LIST_TOP + VISIBLE_ROWS * ROW_HEIGHT) return -1;
        return scroll + (int) ((getMouseY() - LIST_TOP) / ROW_HEIGHT);
    }

    private int maxScroll() { return Math.max(0, draftOrder.size() - VISIBLE_ROWS); }

    @Nullable
    private AbstractTownBuilding building(BlockPos pos) {
        TeamTown town = townSource.get();
        return town == null ? null : town.getTownBuildings().get(pos);
    }

    private void renderDropMarker(GuiGraphics graphics, int x, int y) {
        if (dragged == null) return;
        int visible = dropIndex - scroll;
        if (visible < 0 || visible > VISIBLE_ROWS) return;
        int markerY = y + LIST_TOP + visible * ROW_HEIGHT;
        graphics.fill(x + 2, markerY - 1, x + WIDTH - 5, markerY + 1, 0xFFFFFFFF);
    }

    private static void drawPanel(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, 0xE0101010);
        graphics.fill(x, y, x + width, y + 1, 0xFF373737);
        graphics.fill(x, y, x + 1, y + height, 0xFF373737);
        graphics.fill(x, y + height - 1, x + width, y + height, 0xFF8B8B8B);
        graphics.fill(x + width - 1, y, x + width, y + height, 0xFF8B8B8B);
    }
}
