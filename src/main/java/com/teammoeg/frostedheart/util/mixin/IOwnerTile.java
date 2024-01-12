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

package com.teammoeg.frostedheart.util.mixin;

import com.teammoeg.frostedheart.base.block.ManagedOwnerTile;
import net.minecraft.tileentity.TileEntity;

import java.util.UUID;

public interface IOwnerTile {
    public UUID getStoredOwner();

    public void setStoredOwner(UUID id);

    public static UUID getOwner(TileEntity te) {
        if (te instanceof IOwnerTile) {
            return ((IOwnerTile) te).getStoredOwner();
        }
        return null;
    }

    public static void setOwner(TileEntity te, UUID id) {
        if (te instanceof IOwnerTile) {
            ((IOwnerTile) te).setStoredOwner(id);
        }
    }

    public static void trySetOwner(TileEntity te, UUID id) {
        if (te instanceof IOwnerTile&&!(te instanceof ManagedOwnerTile)) {
            if (((IOwnerTile) te).getStoredOwner() == null) {
                ((IOwnerTile) te).setStoredOwner(id);
            }
        }
    }
}
