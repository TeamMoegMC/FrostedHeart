/*
 * Copyright (c) 2024 TeamMoeg
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

package com.teammoeg.frostedheart.compat.ftbq;

import dev.ftb.mods.ftblibrary.config.IntConfig;
import dev.ftb.mods.ftblibrary.config.ui.EditStringConfigOverlay;

public class FHGuiProviders {
    public static void setRewardGuiProviders() {
        FHRewardTypes.INSIGHT.setGuiProvider((panel, quest, callback) -> {
            IntConfig c = new IntConfig(1, Integer.MAX_VALUE);
            c.setValue(100);

            EditStringConfigOverlay<Integer> overlay = new EditStringConfigOverlay<>(panel.getGui(), c, accepted -> {
                if (accepted) {
                    callback.accept(new InsightReward(0L, quest, c.getValue()));
                }

                panel.run();
            }, FHRewardTypes.INSIGHT.getDisplayName()).atMousePosition();
            panel.getGui().pushModalPanel(overlay);
        });
    }
}
