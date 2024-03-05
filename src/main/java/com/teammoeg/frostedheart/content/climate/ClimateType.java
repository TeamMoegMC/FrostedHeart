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

public enum ClimateType {
    NONE(0),
    SNOW_BLIZZARD(1),//A special snow before blizzard
    BLIZZARD(1),
    SNOW(2),
    SUN(3),
    CLOUDY(4);

    final int typeId;//Same typeid represent same weather event but with different presentation, for forecasting

    private ClimateType(int typeId) {
        this.typeId = typeId;
    }


}
