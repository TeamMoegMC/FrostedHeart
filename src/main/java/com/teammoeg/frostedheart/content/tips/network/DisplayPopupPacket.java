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

package com.teammoeg.frostedheart.content.tips.network;

import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.chorda.text.Components;
import com.teammoeg.frostedheart.content.tips.Popup;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record DisplayPopupPacket(String message) implements CMessage {

    public DisplayPopupPacket(Component message) {
        this(Components.getKeyOrElseStr(message));
    }

    public DisplayPopupPacket(FriendlyByteBuf buffer) {
        this(buffer.readUtf());
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(this.message);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> Popup.put(Component.translatable(message)));
        context.get().setPacketHandled(true);
    }
}
