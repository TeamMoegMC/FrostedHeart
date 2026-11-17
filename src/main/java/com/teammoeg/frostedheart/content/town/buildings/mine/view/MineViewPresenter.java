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

package com.teammoeg.frostedheart.content.town.buildings.mine.view;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.teammoeg.frostedheart.content.town.TeamTown;
import com.teammoeg.frostedheart.content.town.building.AbstractTownBuilding;
import com.teammoeg.frostedheart.content.town.buildings.mine.MineBaseBuilding;
import com.teammoeg.frostedheart.content.town.buildings.mine.MineBuilding;
import com.teammoeg.frostedheart.content.town.terrainresource.TerrainResourceType;
import com.teammoeg.frostedheart.content.town.ui.TownBuildingLabels;
import com.teammoeg.frostedheart.content.town.ui.TownFormat;
import com.teammoeg.frostedheart.content.town.ui.view.BoardPresenters;
import com.teammoeg.frostedheart.content.town.ui.view.BoardView.*;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;

import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;

/**
 * 矿井的玩家视图：这块地还剩多少矿、连着哪座采矿基地、能挖出些什么。纯函数，只出整数。
 * <p>
 * The mine's player view: how much is left in its ground, which mine base it feeds, and what it yields. Pure; text
 * holds whole numbers only.
 */
public final class MineViewPresenter {
    public static final String KEY = "gui.frostedheart.board.mine.";
    private static final int LOW_DEPOSIT = 20;

    public record MineView(Header header, List<Stat> stats, List<Chip> conditions, List<Note> notes, List<Note> talk,
                           List<ItemLine> composition, Component compositionNote, List<Component> tips) {
    }

    public record Inputs(@Nullable MineBuilding mine, BlockPos pos, @Nullable TeamTown town, String townName, long townDay) {
    }

    private MineViewPresenter() {
    }

    static Component t(String key, Object... args) {
        return com.teammoeg.frostedheart.content.town.ui.TownFormat.counted(KEY + key, args);
    }

    static Component b(String key, Object... args) {
        return BoardPresenters.b(key, args);
    }

    public static MineView present(Inputs in) {
        MineBuilding mine = in.mine();
        int radius = FHConfig.SERVER.TOWN.MINING.connectionRadiusBlocks.get();
        List<Component> tips = List.of(t("tip.base", radius), t("tip.shared"), t("tip.refill"), t("tip.biome"));
        if (mine == null) {
            Header header = new Header(Component.translatable("gui.frostedheart.town_manager.building.MineBuilding"), Component.empty(),
                    Status.STOPPED, List.of(b("reason.missing")), 0);
            return new MineView(header, List.of(), List.of(), List.of(), List.of(), List.of(), Component.empty(), tips);
        }
        Map<BlockPos, AbstractTownBuilding> buildings = in.town() == null ? Map.of() : in.town().getTownBuildings();
        ChunkPos chunk = new ChunkPos(in.pos());
        double full = TerrainResourceType.ORE.getResourcePerSq();
        double remaining = in.town() == null ? full : in.town().getRemainingTerrainResource(TerrainResourceType.ORE, chunk);
        int percent = full <= 0 ? 0 : TownFormat.percent(remaining / full);
        MineBaseBuilding base = buildings.values().stream().filter(MineBaseBuilding.class::isInstance).map(MineBaseBuilding.class::cast)
                .filter(candidate -> candidate.getLinkedMines().contains(mine.getPos()))
                .min(Comparator.comparingLong(candidate -> candidate.getPos().asLong())).orElse(null);
        Component biome = Component.translatable(Util.makeDescriptionId("biome", mine.getBiomePath()));

        // ---- status: a mine works for a working base while its ground holds ore
        List<Component> room = BoardPresenters.roomReasons(mine, 1, 1, 1, 1);
        List<Component> stopped = new ArrayList<>(room);
        if (base == null) stopped.add(t("reason.no_base"));
        else if (!base.isBuildingWorkable()) stopped.add(t("reason.base_stopped"));
        if (remaining <= 1e-6) stopped.add(t("reason.empty"));
        List<Component> limited = new ArrayList<>();
        if (remaining > 1e-6 && percent < LOW_DEPOSIT) limited.add(t("reason.low"));
        Header header = BoardPresenters.header(mine, in.town(), in.pos(), in.townName(), in.townDay(), stopped, limited, 0);

        // ---- key figures
        Tone depositTone = remaining <= 1e-6 ? Tone.BAD : percent < LOW_DEPOSIT ? Tone.WARN : Tone.GOOD;
        Stat deposit = new Stat(t("stat.deposit"), Component.literal(percent + "%"),
                remaining <= 1e-6 ? t("stat.deposit.empty") : percent < LOW_DEPOSIT ? t("stat.deposit.low") : t("stat.deposit.ok"), depositTone,
                List.of(t("stat.deposit.tooltip")));
        Stat feeds = base == null
                ? new Stat(t("stat.base"), t("stat.base.none"), t("stat.base.none.sub", radius), Tone.BAD, List.of(t("stat.base.tooltip", radius)))
                : new Stat(t("stat.base"), TownBuildingLabels.name(buildings, base.getPos(), base),
                base.isBuildingWorkable() ? t("stat.base.ok") : t("stat.base.stopped"), base.isBuildingWorkable() ? Tone.GOOD : Tone.BAD,
                List.of(t("stat.base.tooltip", radius)));
        Stat ground = new Stat(t("stat.biome"), biome, t("stat.biome.sub"), Tone.MUTED, List.of(t("stat.biome.tooltip")));

        List<Chip> chips = new ArrayList<>();
        // A mine has no room to check: the chip says whether it can be worked at all.
        chips.add(room.isEmpty() ? Chip.of(t("chip.usable"), Tone.GOOD) : Chip.of(t("chip.unusable"), Tone.BAD));
        long sharing = buildings.values().stream().filter(MineBuilding.class::isInstance)
                .filter(other -> new ChunkPos(other.getPos()).equals(chunk)).count();
        if (sharing > 1) chips.add(Chip.of(t("chip.sharing", sharing - 1), Tone.WARN));
        chips.add(Chip.of(t("chip.refill"), Tone.NORMAL));

        // ---- notes
        Component signer = b("note.memo");
        List<Note> notes = new ArrayList<>();
        if (!room.isEmpty()) notes.add(BoardPresenters.improve("structure", room.get(0), BoardPresenters.roomAction(mine, 1, 1, 1), signer));
        if (base == null) notes.add(BoardPresenters.improve("no_base", t("note.no_base", radius), t("note.no_base.action", radius), signer));
        else if (!base.isBuildingWorkable())
            notes.add(BoardPresenters.improve("base_stopped", t("note.base_stopped", TownBuildingLabels.name(buildings, base.getPos(), base)),
                    t("note.base_stopped.action"), signer));
        if (remaining <= 1e-6) notes.add(BoardPresenters.improve("empty", t("note.empty"), t("note.empty.action"), signer));
        else if (percent < LOW_DEPOSIT) notes.add(BoardPresenters.improve("low", t("note.low"), t("note.low.action"), signer));
        notes = BoardPresenters.firstNotes(notes, 3);
        List<Note> talk = List.of(BoardPresenters.chat(t("talk.1"), signer), BoardPresenters.chat(t("talk.2"), signer));

        // ---- what the ground yields, by share of the weights
        Map<Item, Integer> weights = MineBuilding.getWeights(mine.getBiomePath());
        double total = weights.values().stream().filter(weight -> weight > 0).mapToDouble(Integer::doubleValue).sum();
        List<ItemLine> composition = new ArrayList<>();
        weights.entrySet().stream().filter(entry -> entry.getValue() > 0)
                .sorted(Map.Entry.<Item, Integer>comparingByValue().reversed().thenComparing(entry -> entry.getKey().getDescriptionId()))
                .forEach(entry -> {
                    long share = total <= 0 ? 0 : Math.round(entry.getValue() * 100.0 / total);
                    ItemStack stack = new ItemStack(entry.getKey());
                    composition.add(new ItemLine(stack, stack.getHoverName(), share < 1 ? t("share.rare") : Component.literal(share + "%"), Tone.NORMAL));
                });
        Component note = MineBuilding.hasBiomeRecipe(mine.getBiomePath()) ? t("composition.biome", biome) : t("composition.default");
        return new MineView(header, List.of(deposit, feeds, ground), chips, notes, talk, composition, note, tips);
    }
}
