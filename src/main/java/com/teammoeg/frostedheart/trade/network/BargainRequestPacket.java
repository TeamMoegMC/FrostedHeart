package com.teammoeg.frostedheart.trade.network;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import com.teammoeg.frostedheart.trade.TradeContainer;
import com.teammoeg.frostedheart.util.SerializeUtil;

import net.minecraft.inventory.container.Container;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

public class BargainRequestPacket {
	private Map<String, Integer> offer;

	public BargainRequestPacket(Map<String, Integer> offer) {
		super();
		this.offer = offer;
	}

	public BargainRequestPacket(PacketBuffer buffer) {
		offer=SerializeUtil.readStringMap(buffer,new LinkedHashMap<>(),PacketBuffer::readVarInt);
    }

	public void encode(PacketBuffer buffer) {
		SerializeUtil.writeStringMap(buffer, offer, (p, b) -> b.writeVarInt(p));
	}

	public void handle(Supplier<NetworkEvent.Context> context) {
		context.get().enqueueWork(() -> {
			Container cont=context.get().getSender().openContainer;
			if(cont instanceof TradeContainer) {
				TradeContainer trade=(TradeContainer) cont;
				trade.setOrder(offer);
			}
		});
		context.get().setPacketHandled(true);
	}
}
