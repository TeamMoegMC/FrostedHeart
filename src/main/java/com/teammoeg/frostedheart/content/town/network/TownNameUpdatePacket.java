/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.network;

import com.teammoeg.chorda.dataholders.team.CClientTeamDataManager;
import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedheart.bootstrap.common.FHSpecialDataTypes;
import com.teammoeg.frostedheart.content.town.TownNamingModel;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Lightweight authoritative town-name update. */
public class TownNameUpdatePacket implements CMessage {
    private final String name;

    public TownNameUpdatePacket(String name) {
        this.name = name;
    }

    public TownNameUpdatePacket(FriendlyByteBuf buffer) {
        this.name = buffer.readUtf(TownNamingModel.MAX_TOWN_NAME_LENGTH);
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(name, TownNamingModel.MAX_TOWN_NAME_LENGTH);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> CClientTeamDataManager.INSTANCE.getInstance()
                .getOptional(FHSpecialDataTypes.TOWN_DATA)
                .ifPresent(town -> town.applyNameUpdate(name)));
        context.get().setPacketHandled(true);
    }
}
