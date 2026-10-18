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

package com.teammoeg.frostedheart.content.ui;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.ui.tips.TipHelper;
import com.teammoeg.frostedheart.content.ui.tips.client.gui.TipOverlay;
import com.teammoeg.frostedheart.item.townmanager.TownManagerClientHelper;

import static com.teammoeg.chorda.client.ClickActions.register;

public class FHClickActions {
    public static void init() {
        register(FHMain.rl("edit_tip"),        "tips.frostedheart.click_action.edit_tip", s -> {
            if (s.startsWith("{")) {
                TipHelper.edit(TipHelper.parse(s), null);
            } else {
                TipHelper.edit(s, null);
            }
        });
        register(FHMain.rl("open_town_events"),
                "tips.frostedheart.click_action.open_town_events", ignored -> {
                    TipOverlay.removeCurrent();
                    TownManagerClientHelper.openEvents();
                });
        register(FHMain.rl("open_town_transport"),
                "tips.frostedheart.click_action.open_town_transport", ignored -> {
                    TipOverlay.removeCurrent();
                    TownManagerClientHelper.openTransportCapacity();
                });
    }
}
