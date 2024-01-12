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

package com.teammoeg.frostedheart.research;

public abstract class AutoIDItem extends FHRegisteredItem {
    private String AssignedID;

    @Override
    public String getLId() {
        return AssignedID + "_" + getNonce();
    }

    public void addID(String id, int index) {
        AssignedID = id + "." + getType() + "." + index;
    }

    public void setRId(int id) {
        super.setRId(id);
    }

    public abstract String getType();

    public abstract String getNonce();
}
