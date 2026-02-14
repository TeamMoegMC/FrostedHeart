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

package com.teammoeg.frostedheart.item.snowsack.network;

import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedheart.item.snowsack.ui.SnowSackMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * 这个Message仅用于告知服务端玩家切换了SnowSack自动拾取设置，不包含任何其它信息
 */
public class ToggleAutoPickupMessage implements CMessage {
    public ToggleAutoPickupMessage( FriendlyByteBuf buffer) {
    }

    public ToggleAutoPickupMessage() {
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {

    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        if(context.get().getSender() != null){
            if(Objects.requireNonNull(context.get().getSender()).containerMenu instanceof SnowSackMenu snowSackMenu){
                snowSackMenu.toggleAutoPickup();
            }
        }
    }
}
