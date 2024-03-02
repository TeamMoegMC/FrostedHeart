/*
 * Copyright (c) 2021-2024 TeamMoeg
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

package com.teammoeg.frostedheart.climate.network;

import java.util.function.Supplier;

import com.teammoeg.frostedheart.base.network.FHMessage;
import com.teammoeg.frostedheart.util.TemperatureDisplayHelper;
import com.teammoeg.frostedheart.util.client.ClientUtils;
import com.teammoeg.frostedheart.util.client.GuiUtils;
import com.teammoeg.frostedheart.util.io.SerializeUtil;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;

public class FHTemperatureDisplayPacket implements FHMessage {
    private final int[] temp;
    private final String langKey;
    private final boolean isStatus;
    private final boolean isAction;

    public FHTemperatureDisplayPacket(PacketBuffer buffer) {
        langKey = buffer.readString();
        temp = buffer.readVarIntArray();
        boolean[] bs = SerializeUtil.readBooleans(buffer);
        isStatus = bs[0];
        isAction = bs[1];
    }

    public FHTemperatureDisplayPacket(String format, boolean isAction, float... data) {
        this.langKey = format;
        temp = new int[data.length];
        for (int i = 0; i < data.length; i++)
            temp[i] = (int) (data[i] * 10);
        isStatus = true;
        this.isAction = isAction;
    }

    public FHTemperatureDisplayPacket(String format, boolean isAction, int... data) {
        this.langKey = format;
        this.temp = data;
        for (int i = 0; i < temp.length; i++)
            temp[i] *= 10;
        isStatus = true;
        this.isAction = isAction;
    }

    public FHTemperatureDisplayPacket(String format, float... data) {
        this.langKey = format;
        temp = new int[data.length];
        for (int i = 0; i < data.length; i++)
            temp[i] = (int) (data[i] * 10);
        isStatus = false;
        isAction = false;
    }

    public FHTemperatureDisplayPacket(String format, int... data) {
        this.langKey = format;
        this.temp = data;
        for (int i = 0; i < temp.length; i++)
            temp[i] *= 10;
        isStatus = false;
        isAction = false;
    }

    public void encode(PacketBuffer buffer) {
        buffer.writeString(langKey);
        buffer.writeVarIntArray(temp);
        SerializeUtil.writeBooleans(buffer, isStatus, isAction);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            PlayerEntity player = DistExecutor.safeCallWhenOn(Dist.CLIENT, () -> ClientUtils::getPlayer);
            Object[] ss = new Object[temp.length];
            for (int i = 0; i < ss.length; i++) {
                ss[i] = TemperatureDisplayHelper.toTemperatureIntString(temp[i] / 10f);
            }
            
            TranslationTextComponent tosend = GuiUtils.translateMessage(langKey, ss);
            if (isStatus)
                player.sendStatusMessage(tosend, false);
            else
                player.sendMessage(tosend, player.getUniqueID());

        });
        context.get().setPacketHandled(true);
    }
}
