/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.item.townmanager;

import com.teammoeg.chorda.client.RenderingHint;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.frostedheart.content.town.TeamTownData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.function.Supplier;

/** Displays the persistent number of completed town days below the editable title. */
final class TownDayLabel extends UIElement {
    private final Supplier<TeamTownData> townDataSource;

    TownDayLabel(UIElement parent, Supplier<TeamTownData> townDataSource) {
        super(parent);
        this.townDataSource = townDataSource;
        setPos(TownManagerScreen.CONTENT_X + 3, TownManagerScreen.CONTENT_Y + 14);
        setSize(TownManagerScreen.CONTENT_WIDTH - 14, 12);
    }

    @Override
    public void render(
            GuiGraphics graphics, int x, int y, int width, int height,
            RenderingHint hint
    ) {
        graphics.fill(x, y, x + width, y + height, 0xE0101010);
        TeamTownData data = townDataSource.get();
        Component text = data == null
                ? Component.translatable("gui.frostedheart.town_manager.town_day_unavailable")
                : Component.translatable("gui.frostedheart.town_manager.town_day", data.getTownDay());
        graphics.drawString(Minecraft.getInstance().font, text, x + 4, y + 2,
                0xFFAAAAAA, false);
    }
}
