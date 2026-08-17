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

import com.teammoeg.chorda.client.cui.base.UILayer;
import com.teammoeg.chorda.client.icon.CIcons;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;

/**
 * 数据统计页签：以折线图展示城镇每日快照（居民、生存与四类营养）。
 * <p>
 * Statistics tab: draws line charts of the town's daily snapshots
 * (residents, survival, and four nutrition channels).
 */
public class TownStatisticsTab extends TownManagerTab {

    public TownStatisticsTab(TownManagerScreen screen) {
        super(screen);
    }

    @Override
    public CIcons.CIcon getContentIcon() {
        return CIcons.getIcon(Items.PAPER);
    }

    @Override
    public Component getTitle() {
        return Component.translatable("gui.frostedheart.town_manager.statistics");
    }

    @Override
    public void build(UILayer layer) {
        layer.add(new TownStatisticsPanel(
                layer,
                TownManagerScreen.CONTENT_X,
                TownManagerScreen.CONTENT_Y,
                screen::getTownData));
    }
}
