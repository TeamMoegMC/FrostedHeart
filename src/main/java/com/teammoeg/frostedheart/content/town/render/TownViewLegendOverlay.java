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

package com.teammoeg.frostedheart.content.town.render;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.town.TeamTown;
import com.teammoeg.frostedheart.content.town.TeamTownData;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 建筑区域与道路视图打开时左上角的图例：房间、道路和建筑方块的颜色各代表什么，以及道路检查结果还没出来的时候。
 * 色块取自 {@link RoomZoneRenderer} 与 {@link TownRoadRenderer} 的同一组颜色。
 * <p>
 * The legend at the top left while the building area and road view is on: what each colour of rooms, roads and
 * building blocks means, and when the road check has not reported yet. Swatches use the same colours as
 * {@link RoomZoneRenderer} and {@link TownRoadRenderer}.
 */
@Mod.EventBusSubscriber(modid = FHMain.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class TownViewLegendOverlay {
    public static final IGuiOverlay OVERLAY = TownViewLegendOverlay::render;
    private static final String KEY = "hud.frostedheart.town_view.";
    private static final int PAD = 4, LINE = 10, SWATCH = 7, GAP = 3, SPACE = 8;
    private static final int TITLE = 0xFFFFDD66, TEXT = 0xFFFFFFFF, NOTE = 0xFFAAAAAA, CHECKING = 0xFFFFAA55;

    /** A swatch drawn as a floor tile, a thin road bar or a tinted block, or none for words alone. */
    private enum Shape {
        NONE, TILE, BAR, BLOCK
    }

    /** One swatch and its words. */
    private record Part(int color, Shape shape, Component text) {
        static Part words(String key, Object... args) {
            return new Part(0, Shape.NONE, Component.translatable(KEY + key, args));
        }

        int width(Font font) {
            return (shape == Shape.NONE ? 0 : SWATCH + GAP) + font.width(text);
        }
    }

    /** A line of the legend: its parts side by side, in one text colour. */
    private record Row(int textColor, Part... parts) {
        int width(Font font) {
            int width = SPACE * (parts.length - 1);
            for (Part part : parts) width += part.width(font);
            return width;
        }
    }

    private TownViewLegendOverlay() {
    }

    @SubscribeEvent
    public static void register(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("town_view_legend", OVERLAY);
    }

    private static Part swatch(int color, Shape shape, String key) {
        return new Part(color, shape, Component.translatable(KEY + key));
    }

    private static void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!RoomZoneRenderer.visible || minecraft.options.hideGui || minecraft.options.renderDebug
                || minecraft.level == null || minecraft.screen != null) return;
        TeamTown town = TeamTownData.getClientTown().orElse(null);
        if (town == null) return;
        // Until the first check reports, every block is red: say so rather than let it read as cut off.
        boolean checking = TownRoadRenderer.checking(town);
        Row[] rows = {
                new Row(TITLE, Part.words("title", Component.keybind("key.frostedheart.building_area"))),
                new Row(TEXT, swatch(RoomZoneRenderer.ROOM, Shape.TILE, "room"), swatch(RoomZoneRenderer.OVERLAP, Shape.TILE, "overlap"),
                        swatch(RoomZoneRenderer.ROOM_GAP, Shape.BLOCK, "gap")),
                new Row(TEXT, swatch(TownRoadRenderer.ROAD_TOWER, Shape.BAR, "road.tower"),
                        swatch(TownRoadRenderer.ROAD_OTHER, Shape.BAR, "road.other")),
                new Row(TEXT, swatch(TownRoadRenderer.ROAD_LARGEST, Shape.BAR, "road.largest")),
                new Row(TEXT, swatch(TownRoadRenderer.BLOCK_CUT_OFF, Shape.BLOCK, "block")),
                checking ? new Row(CHECKING, Part.words("checking")) : new Row(NOTE, Part.words("note")),
                new Row(NOTE, Part.words("note.update"))
        };
        Font font = minecraft.font;
        int panelWidth = 0;
        for (Row row : rows) panelWidth = Math.max(panelWidth, row.width(font));
        int left = 4, top = 4;
        graphics.fill(left, top, left + panelWidth + PAD * 2, top + PAD * 2 + rows.length * LINE - 2, 0xA0000000);
        for (int i = 0; i < rows.length; i++) {
            int x = left + PAD, y = top + PAD + i * LINE;
            for (Part part : rows[i].parts()) {
                int opaque = part.color() | 0xFF000000;
                switch (part.shape()) {
                    case TILE -> graphics.fill(x, y, x + SWATCH, y + SWATCH, opaque);
                    case BAR -> graphics.fill(x, y + 2, x + SWATCH, y + 5, opaque);
                    case BLOCK -> {
                        // A tinted block: filled like the cube in the world, edged so it reads apart from a floor tile.
                        graphics.fill(x, y, x + SWATCH, y + SWATCH, opaque);
                        graphics.renderOutline(x, y, SWATCH, SWATCH, 0xFF202020);
                    }
                    default -> {
                    }
                }
                int textX = part.shape() == Shape.NONE ? x : x + SWATCH + GAP;
                graphics.drawString(font, part.text(), textX, y, rows[i].textColor(), true);
                x = textX + font.width(part.text()) + SPACE;
            }
        }
    }
}
