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

/**
 * Station ring geometry, section slots and station assembly.
 * <p>
 * A station is a flat ring of concentric layers. Each layer's circumference decides how many sections it can hold,
 * and every section occupies exactly one {@link com.teammoeg.frostedheart.content.space.station.StationSlot}. See
 * {@link com.teammoeg.frostedheart.content.space.station.StationShape} for the parameters and
 * {@link com.teammoeg.frostedheart.content.space.station.StationLayout} for the derived coordinates.
 */
package com.teammoeg.frostedheart.content.space.station;
