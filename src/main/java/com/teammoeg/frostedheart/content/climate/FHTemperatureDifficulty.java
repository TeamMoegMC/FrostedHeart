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

package com.teammoeg.frostedheart.content.climate;

import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.chorda.client.icon.CIcons.CIcon;

public enum FHTemperatureDifficulty {
    easy(2, CIcons.getIcon("frostedheart:item/flask_i/insulated_flask_i_pouch_green")),
    normal(1, CIcons.getIcon("frostedheart:item/flask_i/insulated_flask_i_pouch_yellow")),
    hard(.5F, CIcons.getIcon("frostedheart:item/flask_i/insulated_flask_i_pouch_orange")),
    hardcore(0F, CIcons.getIcon("frostedheart:item/flask_i/insulated_flask_i_pouch_red"));

    public final float heat_unit;
    public final CIcon icon;

    FHTemperatureDifficulty(float heat_unit, CIcon icon) {
        this.heat_unit = heat_unit;
        this.icon = icon;
    }
}
