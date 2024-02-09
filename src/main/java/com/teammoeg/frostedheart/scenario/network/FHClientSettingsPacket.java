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

package com.teammoeg.frostedheart.scenario.network;

import java.util.function.Supplier;

import com.teammoeg.frostedheart.scenario.FHScenario;
import com.teammoeg.frostedheart.scenario.runner.ScenarioVariables;

import net.minecraft.client.Minecraft;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

public class FHClientSettingsPacket {
    double scale;

    public FHClientSettingsPacket(PacketBuffer buffer) {
        scale=buffer.readDouble();
    }


    public FHClientSettingsPacket() {
        super();
        this.scale=Minecraft.getInstance().getMainWindow().getGuiScaleFactor();
    }


    public void encode(PacketBuffer buffer) {
        buffer.writeDouble(scale);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            // Update client-side nbt
            ScenarioVariables sv=FHScenario.get(context.get().getSender()).getVaribles();
            sv.getExecutionData().putDouble("uiScale", scale);
            if(sv.getSnapshot()!=null)
            	sv.getSnapshot().putDouble("uiScale", scale);
            	
        });
        context.get().setPacketHandled(true);
    }
}
