package com.teammoeg.frostedheart.climate.network;

import java.util.function.Supplier;

import com.teammoeg.frostedheart.FHNetwork;

import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkEvent;

public interface FHMessage {

	void encode(PacketBuffer buffer);

	void handle(Supplier<NetworkEvent.Context> context);
	
	default ResourceLocation id() {
		return FHNetwork.getId(this.getClass());
	}
}