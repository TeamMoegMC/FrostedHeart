package com.teammoeg.frostedheart.trade.network;

import java.util.function.Supplier;

import com.teammoeg.frostedheart.client.util.ClientUtils;
import com.teammoeg.frostedheart.trade.RelationList;
import com.teammoeg.frostedheart.trade.TradeContainer;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;

public class TradeUpdatePacket {
	CompoundNBT data;
	CompoundNBT player;
	RelationList relations;
	boolean isReset;



	public TradeUpdatePacket(CompoundNBT data, CompoundNBT player, RelationList relations, boolean isReset) {
		super();
		this.data = data;
		this.player = player;
		this.relations = relations;
		this.isReset = isReset;
	}

	public TradeUpdatePacket(PacketBuffer buffer) {
		data=buffer.readCompoundTag();
		player=buffer.readCompoundTag();
		relations=new RelationList();
		relations.read(buffer);
		isReset=buffer.readBoolean();
    }

	public void encode(PacketBuffer buffer) {
		buffer.writeCompoundTag(data);
		buffer.writeCompoundTag(player);
		relations.write(buffer);
		buffer.writeBoolean(isReset);
	}
	
	public void handle(Supplier<NetworkEvent.Context> context) {
		context.get().enqueueWork(() -> {
			PlayerEntity player = DistExecutor.safeCallWhenOn(Dist.CLIENT, () -> ClientUtils::getPlayer);
			Container cont=player.openContainer;
			if(cont instanceof TradeContainer) {
				TradeContainer trade=(TradeContainer) cont;
				trade.update(data,this.player,relations,isReset);
			}
		});
		context.get().setPacketHandled(true);
	}
}
