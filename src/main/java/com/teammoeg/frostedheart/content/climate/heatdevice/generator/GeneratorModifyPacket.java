package com.teammoeg.frostedheart.content.climate.heatdevice.generator;

import java.util.function.Supplier;

import com.teammoeg.frostedheart.base.network.FHMessage;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent.Context;

public class GeneratorModifyPacket implements FHMessage{
	public GeneratorModifyPacket() {
	}
	public GeneratorModifyPacket(PacketBuffer buffer) {
	}
	@Override
	public void encode(PacketBuffer buffer) {
	}
	@Override
	public void handle(Supplier<Context> context) {
		context.get().enqueueWork(() -> {
			ServerPlayerEntity spe=context.get().getSender();
			Container container=spe.openContainer;
			if(container instanceof MasterGeneratorContainer) {
				MasterGeneratorContainer crncontainer=(MasterGeneratorContainer) container;
				((MasterGeneratorTileEntity)crncontainer.tile).onUpgradeMaintainClicked(spe);
			}
		});
		context.get().setPacketHandled(true);
	}

}
