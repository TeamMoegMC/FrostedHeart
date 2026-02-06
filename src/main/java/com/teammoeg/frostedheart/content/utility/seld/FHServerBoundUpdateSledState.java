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

package com.teammoeg.frostedheart.content.utility.seld;

import com.teammoeg.chorda.menu.DummyMenuProvider;
import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedheart.content.climate.block.ClothesInventoryMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkHooks;

import java.util.function.Supplier;

public class FHServerBoundUpdateSledState implements CMessage {
    public final float clientDx;
    public final float clientDy;
    public final float clientDz;

    public FHServerBoundUpdateSledState(FriendlyByteBuf buffer) {
        this.clientDx = buffer.readFloat();
        this.clientDy = buffer.readFloat();
        this.clientDz = buffer.readFloat();
    }

    public FHServerBoundUpdateSledState(Vec3 movement) {
        this.clientDx = (float) movement.x;
        this.clientDy = (float) movement.y;
        this.clientDz = (float) movement.z;

    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeFloat(this.clientDx);
        buffer.writeFloat(this.clientDy);
        buffer.writeFloat(this.clientDz);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        if (context.get().getSender().getVehicle() instanceof SledEntity sled) {
            sled.setSyncedMovement(this.clientDx, this.clientDy, this.clientDx);
        }
    }
}
