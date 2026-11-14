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

package com.teammoeg.frostedheart.content.town.buildings.warehouse.view;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.teammoeg.frostedheart.content.town.TeamTown;
import com.teammoeg.frostedheart.content.town.building.AbstractTownBuilding;
import com.teammoeg.frostedheart.content.town.buildings.warehouse.VirtualItemStack;
import com.teammoeg.frostedheart.content.town.buildings.warehouse.WarehouseBuilding;
import com.teammoeg.frostedheart.content.town.resource.VirtualResourceType;
import com.teammoeg.frostedheart.content.town.ui.TownFormat;
import com.teammoeg.frostedheart.content.town.ui.view.BoardPresenters;
import com.teammoeg.frostedheart.content.town.ui.view.BoardView.*;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/**
 * 仓库的玩家视图：全镇存了多少、还能放多少、吃的够几天、这座仓库出了多少力、存得最多的东西。纯函数，只出整数。
 * <p>
 * The warehouse's player view: how much the town holds and can still hold, how many days the food lasts, what this
 * warehouse adds, and the most-stocked goods. Pure; text holds whole numbers only.
 */
public final class WarehouseViewPresenter {
    public static final String KEY = "gui.frostedheart.board.warehouse.";
    /** The warehouse stamp reads 正常 / 吃紧 / 停用. */
    public static final String STATUS_KEY = KEY + "status.";
    public static final int TAB_OVERVIEW = 0, TAB_STOCK = 1;
    /** Storage at or above this share of its limit is nearly full. */
    public static final int NEARLY_FULL = 90;
    /** Food for fewer days than this is short; fewer than {@link #FOOD_PLENTY} is worth watching. */
    public static final int FOOD_SHORT = 3, FOOD_PLENTY = 7;
    private static final int TOP_ITEMS = 10, MAX_IMPROVEMENTS = 3, MAX_NOTES = 6;

    /** The town's shared storage: items stored against the limit of every working warehouse. */
    public record Capacity(long used, long limit, int percent, Tone tone, Component line) {
    }

    /**
     * @param foodLine 吃的够几天，给库存页的小条用 / how long the food lasts, for the stock tab's strip
     */
    public record WarehouseView(Header header, Capacity capacity, Component foodLine, Tone foodTone, int kinds, List<Stat> stats,
                                List<Chip> conditions, List<Note> notes, List<Note> talk, List<ItemLine> topItems, List<Component> tips) {
    }

    /**
     * @param foodDays 全镇存粮够吃的天数，未知时为空 / days the town's food lasts, null when unknown
     */
    public record Inputs(@Nullable WarehouseBuilding building, BlockPos pos, @Nullable TeamTown town, String townName, long townDay,
                         List<VirtualItemStack> stock, @Nullable Double foodDays, int population) {
    }

    private WarehouseViewPresenter() {
    }

    static Component t(String key, Object... args) {
        return com.teammoeg.frostedheart.content.town.ui.TownFormat.counted(KEY + key, args);
    }

    static Component b(String key, Object... args) {
        return BoardPresenters.b(key, args);
    }

    public static WarehouseView present(Inputs in) {
        WarehouseBuilding building = in.building();
        Map<BlockPos, AbstractTownBuilding> buildings = in.town() == null ? Map.of() : in.town().getTownBuildings();
        Capacity capacity = capacity(in.town());
        int kinds = (int) in.stock().stream().filter(stack -> stack.getAmount() > 0).count();
        List<ItemLine> top = topItems(in.stock());
        Integer days = in.foodDays() == null ? null : (int) Math.floor(Math.max(0, in.foodDays()) + 1e-9);
        Tone foodTone = days == null ? Tone.MUTED : days < FOOD_SHORT ? Tone.BAD : days < FOOD_PLENTY ? Tone.WARN : Tone.GOOD;
        Component foodLine = days == null ? t("food.unknown") : days < 1 ? t("food.under_one") : t("food.days", days);
        List<Component> tips = List.of(t("tip.deposit"), t("tip.carry"), t("tip.take"), t("tip.shared"));
        if (building == null) {
            Header header = new Header(Component.translatable("gui.frostedheart.town_manager.building.WarehouseBuilding"), Component.empty(),
                    Status.STOPPED, List.of(b("reason.missing")), 0);
            return new WarehouseView(header, capacity, foodLine, foodTone, kinds, List.of(), List.of(), List.of(), List.of(), top, tips);
        }

        // ---- status: a broken room adds nothing; a full store or short food makes things tight
        // Warehouses have no size minimum, so only the checks every room shares apply.
        List<Component> room = BoardPresenters.roomReasons(building, 1, 1, 1, 1);
        List<Component> limited = new ArrayList<>();
        if (capacity.limit() > 0 && capacity.percent() >= NEARLY_FULL) limited.add(t(capacity.percent() >= 100 ? "reason.full" : "reason.nearly_full"));
        if (days != null && days < FOOD_SHORT) limited.add(t("reason.food"));
        Header header = BoardPresenters.header(building, in.town(), in.pos(), in.townName(), in.townDay(), room, limited, 0);

        // ---- key figures
        // With no room to count against, only what is stored is shown; "224 / 0" would read as nothing.
        Stat stored = new Stat(t("stat.stored"), Component.literal(capacity.limit() <= 0 ? TownFormat.count(capacity.used())
                : TownFormat.count(capacity.used()) + " / " + TownFormat.count(capacity.limit())),
                capacity.line(), capacity.tone(), List.of(t("stat.stored.tooltip")), TAB_STOCK);
        Stat food = new Stat(t("stat.food"), days == null ? t("stat.food.unknown") : days < 1 ? t("stat.food.under_one") : t("stat.food.days", days),
                in.population() > 0 ? t("stat.food.people", in.population()) : t("stat.food.nobody"), foodTone, List.of(t("stat.food.tooltip")));
        long own = building.isBuildingWorkable() ? TownFormat.whole(building.getCapacity()) : 0;
        int share = capacity.limit() <= 0 ? 0 : (int) Math.min(100, Math.round(100.0 * own / capacity.limit()));
        Stat adds = new Stat(t("stat.adds"), t("stat.adds.value", TownFormat.count(own)),
                building.isBuildingWorkable() ? t("stat.adds.share", share) : t("stat.adds.none"), building.isBuildingWorkable() ? Tone.MUTED : Tone.BAD,
                List.of(t("stat.adds.tooltip")));
        int racks = building.getDecorationAmount();
        Stat shelves = new Stat(t("stat.racks"), Component.literal(Integer.toString(racks)), racks > 0 ? t("stat.racks.sub") : t("stat.racks.none"),
                racks > 0 ? Tone.MUTED : Tone.WARN, List.of(t("stat.racks.tooltip")));

        List<Chip> chips = new ArrayList<>();
        chips.add(room.isEmpty() ? Chip.of(t("chip.room.ok"), Tone.GOOD) : Chip.of(t("chip.room.bad"), Tone.BAD));
        chips.add(Chip.of(t("chip.area", building.getArea()), Tone.NORMAL));
        chips.add(Chip.of(t("chip.volume", building.getVolume()), Tone.NORMAL));
        chips.add(Chip.of(t("chip.kinds", kinds), Tone.NORMAL));

        // ---- notes, most urgent first
        Component signer = t("signer");
        List<Note> notes = new ArrayList<>();
        if (!room.isEmpty())
            notes.add(BoardPresenters.improve("structure", t("note.structure", room.get(0)),
                    BoardPresenters.roomAction(building, 1, 1, 1), signer));
        if (capacity.limit() > 0 && capacity.percent() >= NEARLY_FULL)
            notes.add(BoardPresenters.improve("full", t(capacity.percent() >= 100 ? "note.full" : "note.nearly_full"), t("note.full.action"), signer));
        if (days != null && days < FOOD_SHORT)
            notes.add(BoardPresenters.improve("food", days < 1 ? t("note.food.under_one") : t("note.food", days), t("note.food.action"), signer));
        if (room.isEmpty() && racks == 0) notes.add(BoardPresenters.improve("racks", t("note.racks"), t("note.racks.action"), signer));
        notes = BoardPresenters.firstNotes(notes, MAX_IMPROVEMENTS);

        List<Note> talk = new ArrayList<>();
        if (kinds == 0) talk.add(BoardPresenters.chat(t("talk.empty"), signer));
        if (days != null && days >= FOOD_PLENTY) talk.add(BoardPresenters.chat(t("talk.plenty"), signer));
        for (int i = 1; i <= 4; i++) talk.add(BoardPresenters.chat(t("talk.daily." + i), signer));
        int spare = Math.max(0, MAX_NOTES - notes.size());
        if (talk.size() > spare) talk = List.copyOf(talk.subList(0, spare));

        return new WarehouseView(header, capacity, foodLine, foodTone, kinds, List.of(stored, food, adds, shelves), chips, notes, talk, top, tips);
    }

    /** The town's storage use; unknown towns hold nothing. */
    static Capacity capacity(@Nullable TeamTown town) {
        if (town == null) return new Capacity(0, 0, 0, Tone.MUTED, t("stored.none"));
        double limit = town.getResourceHolder().get(VirtualResourceType.MAX_CAPACITY.generateAttribute(0));
        double used = Math.max(0, limit - town.getResourceHolder().getCapacityLeft());
        long shownLimit = TownFormat.whole(limit), shownUsed = (long) Math.ceil(used - 1e-9);
        int percent = shownLimit <= 0 ? 0 : (int) Math.min(100, Math.floor(100 * used / limit + 1e-9));
        Tone tone = shownLimit <= 0 ? Tone.BAD : percent >= NEARLY_FULL ? Tone.BAD : percent >= 70 ? Tone.WARN : Tone.MUTED;
        Component line = shownLimit <= 0 ? t("stored.no_room") : percent >= 100 ? t("stored.full") : percent >= NEARLY_FULL ? t("stored.nearly_full")
                : t("stored.left", TownFormat.count(Math.max(0, shownLimit - shownUsed)));
        return new Capacity(shownUsed, shownLimit, percent, tone, line);
    }

    /** The most-stocked goods, largest first. */
    static List<ItemLine> topItems(List<VirtualItemStack> stock) {
        List<ItemLine> lines = new ArrayList<>();
        stock.stream().filter(stack -> stack.getAmount() > 0)
                .sorted(Comparator.comparingLong(VirtualItemStack::getAmount).reversed().thenComparing(VirtualItemStack::getLowercaseName))
                .limit(TOP_ITEMS)
                .forEach(stack -> lines.add(new ItemLine(stack.getDisplayStack(), stack.getDisplayStack().getHoverName(),
                        Component.literal(TownFormat.count(stack.getAmount())), Tone.NORMAL)));
        return lines;
    }
}
