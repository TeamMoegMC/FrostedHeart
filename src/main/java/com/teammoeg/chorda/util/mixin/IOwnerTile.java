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

package com.teammoeg.chorda.util.mixin;

import java.util.Optional;
import java.util.UUID;

import com.teammoeg.chorda.block.ManagedOwnerBlockEntity;

import net.minecraft.world.level.block.entity.BlockEntity;

public interface IOwnerTile {
    static UUID getOwner(BlockEntity te) {
        if (te instanceof IOwnerTile) {
            return ((IOwnerTile) te).getStoredOwner();
        }
        return null;
    }

    static void setOwner(BlockEntity te, UUID id) {
        if (te instanceof IOwnerTile) {
            ((IOwnerTile) te).setStoredOwner(id);
            if(te instanceof IOwnerChangeListener) {
            	((IOwnerChangeListener)te).onOwnerChange();
            }
        }
    }

    static void trySetOwner(BlockEntity te, UUID id) {
        if (te instanceof IOwnerTile && !(te instanceof ManagedOwnerBlockEntity)) {
            if (((IOwnerTile) te).getStoredOwner() == null) {
                ((IOwnerTile) te).setStoredOwner(id);
                if(te instanceof IOwnerChangeListener) {
                	((IOwnerChangeListener)te).onOwnerChange();
                }
            }
        }
    }
    static Optional<UUID> tryGetOwner(BlockEntity te) {
        if (te instanceof IOwnerTile ote) {
            return Optional.ofNullable(ote.getStoredOwner());
        }
        return Optional.empty();
    }
    UUID getStoredOwner();

    void setStoredOwner(UUID id) ;
}
