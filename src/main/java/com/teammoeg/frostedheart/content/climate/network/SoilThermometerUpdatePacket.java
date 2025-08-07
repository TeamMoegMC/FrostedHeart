package com.teammoeg.frostedheart.content.climate.network;

import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedheart.content.climate.render.TemperatureGoogleRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SoilThermometerUpdatePacket(float temperature) implements CMessage {

    public SoilThermometerUpdatePacket(FriendlyByteBuf buffer) {
        this(buffer.readFloat());
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeFloat(temperature);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> TemperatureGoogleRenderer.cachedTemperature = temperature);
        context.get().setPacketHandled(true);
    }
}
