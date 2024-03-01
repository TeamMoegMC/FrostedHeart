package com.teammoeg.frostedheart.network;

import java.util.function.Supplier;

import com.teammoeg.frostedheart.FHNetwork;

import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkEvent;
/**
 * A Message should:<br/>
 * 1. Have a Constructor with one parameter of PacketBuffer as deserializer (IMPORTANT)<br/>
 * 2. Implements methods below<br/>
 * 3. Register class in {@link FHNetwork}<br/>
 * 
 * */
public interface FHMessage {

	void encode(PacketBuffer buffer);

	void handle(Supplier<NetworkEvent.Context> context);
	
	default ResourceLocation id() {
		return FHNetwork.getId(this.getClass());
	}
}