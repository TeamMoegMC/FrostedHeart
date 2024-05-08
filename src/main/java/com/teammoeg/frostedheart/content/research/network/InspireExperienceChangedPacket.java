package com.teammoeg.frostedheart.content.research.network;

import java.util.function.Supplier;

import com.teammoeg.frostedheart.FHCapabilities;
import com.teammoeg.frostedheart.base.network.FHMessage;
import com.teammoeg.frostedheart.content.research.FHResearch;
import com.teammoeg.frostedheart.content.research.inspire.EnergyCore;
import com.teammoeg.frostedheart.util.client.ClientUtils;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent.Context;

public class InspireExperienceChangedPacket implements FHMessage {
	int level;
	int exp;
	int plevel;
	int pexp;
	int researchPoint;
	
	public InspireExperienceChangedPacket(PacketBuffer pb) {
		level=pb.readVarInt();
		exp=pb.readVarInt();
		plevel=pb.readVarInt();
		pexp=pb.readVarInt();
		researchPoint=pb.readVarInt();
	}

	@Override
	public void encode(PacketBuffer buffer) {
		buffer.writeVarInt(level);
		buffer.writeVarInt(exp);
		buffer.writeVarInt(plevel);
		buffer.writeVarInt(pexp);
		buffer.writeVarInt(researchPoint);
	}

	@Override
	public void handle(Supplier<Context> context) {
        context.get().enqueueWork(() -> {
        	PlayerEntity player = DistExecutor.safeCallWhenOn(Dist.CLIENT, () -> ClientUtils::getPlayer);
        	LazyOptional<EnergyCore> ec=FHCapabilities.ENERGY.getCapability(player);
        	ec.ifPresent(s->{
        		s.update(level, exp, plevel, pexp, researchPoint);
        	});
        });
        context.get().setPacketHandled(true);
	}

}
