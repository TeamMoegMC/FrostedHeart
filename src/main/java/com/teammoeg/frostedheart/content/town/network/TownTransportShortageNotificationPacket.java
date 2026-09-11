/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.network;

import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedheart.content.town.transport.TownTransportShortageNotice;
import com.teammoeg.frostedheart.content.ui.tips.client.TownTransportShortageTipPresentation;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Bounded S2C payload for one or more morning transport-shortage notices. */
public record TownTransportShortageNotificationPacket(
        long notificationId,
        List<TownTransportShortageNotice> notices
) implements CMessage {
    public static final int MAX_NOTICES = 8;

    public TownTransportShortageNotificationPacket {
        notices = List.copyOf(notices == null ? List.of() : notices);
        if (notices.isEmpty() || notices.size() > MAX_NOTICES) {
            throw new IllegalArgumentException("Transport shortage notice count must be between 1 and "
                    + MAX_NOTICES + ".");
        }
    }

    public TownTransportShortageNotificationPacket(FriendlyByteBuf buffer) {
        this(buffer.readLong(), readNotices(buffer));
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeLong(notificationId);
        buffer.writeVarInt(notices.size());
        for (TownTransportShortageNotice notice : notices) {
            buffer.writeDouble(notice.totalCapacity());
            buffer.writeDouble(notice.reservedCapacity());
            buffer.writeDouble(notice.shortfall());
            buffer.writeDouble(notice.effectiveRateScale());
        }
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context packetContext = context.get();
        if (isClientbound(packetContext.getDirection())) {
            packetContext.enqueueWork(() -> TownTransportShortageTipPresentation.display(
                    notificationId, notices));
        }
        packetContext.setPacketHandled(true);
    }

    static boolean isClientbound(NetworkDirection direction) {
        return direction == NetworkDirection.PLAY_TO_CLIENT;
    }

    private static List<TownTransportShortageNotice> readNotices(FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        if (size <= 0 || size > MAX_NOTICES) {
            throw new DecoderException("Invalid transport shortage notice count: " + size);
        }
        List<TownTransportShortageNotice> result = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            try {
                result.add(new TownTransportShortageNotice(
                        buffer.readDouble(),
                        buffer.readDouble(),
                        buffer.readDouble(),
                        buffer.readDouble()));
            } catch (IllegalArgumentException exception) {
                throw new DecoderException("Invalid transport shortage notice at index " + index,
                        exception);
            }
        }
        return result;
    }
}
