package com.teammoeg.frostedheart.content.town.network;

import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.town.resident.WanderingRefugee;
import com.teammoeg.frostedheart.content.trade.TradeHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkHooks;

import java.util.function.Supplier;

/**
 *  wanderer open trade gui message.<br>
 *  Client to server message.
 */
public class WanderingRefugeeOpenTradeGUIMessage implements CMessage {
    int refugeeID;

    /**
     * decoder
     */
    public WanderingRefugeeOpenTradeGUIMessage(FriendlyByteBuf buffer){
        refugeeID = buffer.readVarInt();
    }

    public WanderingRefugeeOpenTradeGUIMessage(int refugeeID) {
        this.refugeeID = refugeeID;
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(refugeeID);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if(player==null){
                FHMain.LOGGER.error("Network error: player in WanderingRefugeeOpenTradeGUIMessage is null!");
                return;
            }
            WanderingRefugee refugee = (WanderingRefugee) player.level().getEntity(refugeeID);
            if(refugee==null) {
                FHMain.LOGGER.error("Network error: refugee in WanderingRefugeeOpenTradeGUIMessage is null!");
                return;
            }
            refugee.openTradingScreen(player);
        });
    }
}
