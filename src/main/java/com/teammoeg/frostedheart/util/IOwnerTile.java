package com.teammoeg.frostedheart.util;

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
        if (te instanceof IOwnerTile) {
            if (((IOwnerTile) te).getStoredOwner() == null) {
                ((IOwnerTile) te).setStoredOwner(id);
            }
        }
    }
}
