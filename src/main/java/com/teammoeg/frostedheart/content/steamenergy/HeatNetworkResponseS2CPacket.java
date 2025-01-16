package com.teammoeg.frostedheart.content.steamenergy;

import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedheart.content.climate.render.TemperatureGoogleRenderer;
import com.teammoeg.chorda.util.io.SerializeUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Sends all endpoints and total output and intake of a HeatNetwork to the client.
 */
public class HeatNetworkResponseS2CPacket implements CMessage {
    ClientHeatNetworkData data;

    public HeatNetworkResponseS2CPacket(ClientHeatNetworkData data) {
        this.data = data;
    }

    public HeatNetworkResponseS2CPacket(FriendlyByteBuf buffer) {
        this.data = new ClientHeatNetworkData(
                buffer.readBlockPos(),
                buffer.readFloat(),
                buffer.readFloat(),
                SerializeUtil.readList(buffer, HeatEndpoint::readNetwork)
        );
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(data.pos);
        buffer.writeFloat(data.totalEndpointOutput);
        buffer.writeFloat(data.totalEndpointIntake);
        SerializeUtil.writeList(buffer, data.endpoints, HeatEndpoint::writeNetwork);
        buffer.writeBoolean(data.invalid);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            // on the client side, update HeatNetwork's fields
            TemperatureGoogleRenderer.lastHeatNetworkData = data;
        });
        context.get().setPacketHandled(true);
    }
}
