package com.teammoeg.frostedheart.trade.network;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import com.teammoeg.frostedheart.trade.RelationList;
import com.teammoeg.frostedheart.trade.TradeContainer;
import com.teammoeg.frostedheart.util.SerializeUtil;

import net.minecraft.inventory.container.Container;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.network.NetworkEvent;

public class BargainResponse {
	boolean succeed;
	int discount;
	float rdiscount;
	private RelationList relation;
	public BargainResponse(TradeContainer trade,boolean state) {
		super();
		this.relation=trade.relations;
		this.rdiscount=trade.discountRatio;
		this.discount=trade.maxdiscount;
		this.succeed=state;
	}

	public BargainResponse(PacketBuffer buffer) {
		relation=new RelationList();
		relation.read(buffer);
		rdiscount=buffer.readFloat();
		discount=buffer.readVarInt();
		succeed=buffer.readBoolean();
    }

	public void encode(PacketBuffer buffer) {
		relation.write(buffer);
		buffer.writeFloat(rdiscount);
		buffer.writeVarInt(discount);
		buffer.writeBoolean(succeed);
	}
	
	public void handle(Supplier<NetworkEvent.Context> context) {
		context.get().enqueueWork(() -> {
			Container cont=context.get().getSender().openContainer;
			if(cont instanceof TradeContainer) {
				TradeContainer trade=(TradeContainer) cont;
				trade.relations.copy(relation);
				trade.maxdiscount=discount;
				trade.discountRatio=rdiscount;
				
			}
		});
		context.get().setPacketHandled(true);
	}
}
