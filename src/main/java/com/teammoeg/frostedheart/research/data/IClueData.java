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

package com.teammoeg.frostedheart.research.data;

import java.util.function.Supplier;

import com.teammoeg.frostedheart.research.clues.Clue;
import com.teammoeg.frostedheart.util.Writeable;

public abstract class IClueData implements Writeable{
    Supplier<Clue> clue;

    public IClueData(Supplier<Clue> clue) {
        super();
        this.clue = clue;
    }


}
