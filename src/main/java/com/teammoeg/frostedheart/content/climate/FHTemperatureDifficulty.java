/*
 * Copyright (c) 2022-2024 TeamMoeg
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

package com.teammoeg.frostedheart.content.climate;

import dev.ftb.mods.ftblibrary.icon.Icon;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Function;

public enum FHTemperatureDifficulty {
    easy(s -> 0.05F, Icon.getIcon("frostedheart:item/flask_i/insulated_flask_i_pouch_green")),
    normal(s -> 0.036F, Icon.getIcon("frostedheart:item/flask_i/insulated_flask_i_pouch_yellow")),
    hard(s -> s.isSprinting() ? 0.036F : 0.024F, Icon.getIcon("frostedheart:item/flask_i/insulated_flask_i_pouch_orange")),
    hardcore(s -> 0F, Icon.getIcon("frostedheart:item/flask_i/insulated_flask_i_pouch_red"));

    public final Function<ServerPlayer, Float> self_heat;
    public final Icon icon;

    FHTemperatureDifficulty(Function<ServerPlayer, Float> self_heat, Icon icon) {
        this.self_heat = self_heat;
        this.icon = icon;
    }
}
