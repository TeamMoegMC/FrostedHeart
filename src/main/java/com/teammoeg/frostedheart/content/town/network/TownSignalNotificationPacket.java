/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.network;

import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedheart.content.town.observation.TownSignalEvent;
import com.teammoeg.frostedheart.content.town.observation.TownSignalNotice;
import com.teammoeg.frostedheart.content.ui.tips.client.TownSignalTipPresentation;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Transient S2C notice subset; intentionally excludes detail and episode identifiers. */
public record TownSignalNotificationPacket(long notificationId, List<TownSignalNotice> notices)
        implements CMessage {
    private static final int MAX_PACKET_EVENTS = 64;

    public TownSignalNotificationPacket {
        notices = List.copyOf(notices);
    }

    public TownSignalNotificationPacket(FriendlyByteBuf buffer) {
        this(buffer.readLong(), readNotices(buffer));
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeLong(notificationId);
        buffer.writeVarInt(notices.size());
        for (TownSignalNotice notice : notices) {
            buffer.writeEnum(notice.type());
            buffer.writeEnum(notice.severity());
            buffer.writeVarInt(notice.affectedCount());
        }
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context packetContext = context.get();
        if (packetContext.getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
            packetContext.enqueueWork(() -> TownSignalTipPresentation.display(notificationId, notices));
        }
        packetContext.setPacketHandled(true);
    }

    private static List<TownSignalNotice> readNotices(FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        if (size < 0 || size > MAX_PACKET_EVENTS) {
            throw new DecoderException("Invalid town signal notice count: " + size);
        }
        List<TownSignalNotice> notices = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            notices.add(new TownSignalNotice(
                    buffer.readEnum(TownSignalEvent.Type.class),
                    buffer.readEnum(TownSignalEvent.Severity.class),
                    buffer.readVarInt()));
        }
        return notices;
    }
}
