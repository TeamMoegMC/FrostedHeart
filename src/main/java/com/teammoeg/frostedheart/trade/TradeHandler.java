package com.teammoeg.frostedheart.trade;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.fml.network.NetworkHooks;

public class TradeHandler {
	public static void openTradeScreen(ServerPlayerEntity spe,FHVillagerData vd) {
		vd.update(spe.getServerWorld(), spe);
		NetworkHooks.openGui(spe,vd,e->{
			e.writeVarInt(vd.parent.getEntityId());
			CompoundNBT tag=new CompoundNBT();
			e.writeCompoundTag(vd.serializeForSend(tag));
			tag=new CompoundNBT();
			e.writeCompoundTag(vd.getRelationDataForRead(spe).serialize(tag));
			vd.getRelationShip(spe).write(e);
		});
	}
	
}
