package com.teammoeg.frostedheart.content.steamenergy;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.base.network.FHMessage;
import com.teammoeg.frostedheart.util.FHUtils;
import com.teammoeg.frostedheart.util.io.SerializeUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.Collection;
import java.util.function.Supplier;

/**
 * Sends all endpoints and total output and intake of a HeatNetwork to the client.
 */
public class HeatNetworkResponseS2CPacket implements FHMessage {
    BlockPos pos;
    private final float totalEndpointOutput;
    private final float totalEndpointIntake;
    private final Collection<HeatEndpoint> endpoints;


    public HeatNetworkResponseS2CPacket(BlockPos pos, HeatNetwork network) {
        this.pos = pos;
        this.totalEndpointOutput = network.getTotalEndpointOutput();
        this.totalEndpointIntake = network.getTotalEndpointIntake();
        this.endpoints = network.getEndpoints();
    }

    public HeatNetworkResponseS2CPacket(FriendlyByteBuf buffer) {
        pos = buffer.readBlockPos();
        totalEndpointOutput = buffer.readFloat();
        totalEndpointIntake = buffer.readFloat();
        endpoints = SerializeUtil.readList(buffer, HeatEndpoint::readNetwork);
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
        buffer.writeFloat(totalEndpointOutput);
        buffer.writeFloat(totalEndpointIntake);
        SerializeUtil.writeList(buffer, endpoints, HeatEndpoint::writeNetwork);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            // on the client side, update HeatNetwork's fields
            ClientLevel level = Minecraft.getInstance().level;
            BlockEntity be = FHUtils.getExistingTileEntity(level, pos);
            if (be instanceof HeatNetworkProvider hp) {
                FHMain.LOGGER.debug("Server data received. Updating client HeatNetwork data.");
                HeatNetwork network = hp.getNetwork();
                if (network != null) {
                    network.setTotalEndpointOutput(totalEndpointOutput);
                    network.setTotalEndpointIntake(totalEndpointIntake);
                    network.setEndpoints(endpoints);
                    FHMain.LOGGER.debug("Update Complete: " + pos + " " + totalEndpointOutput + " " + totalEndpointIntake);
                }
            }
        });
        context.get().setPacketHandled(true);
    }
}
