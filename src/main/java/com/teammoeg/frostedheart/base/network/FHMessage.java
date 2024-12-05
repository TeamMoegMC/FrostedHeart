package com.teammoeg.frostedheart.base.network;

import java.util.function.Supplier;

import com.teammoeg.frostedheart.FHNetwork;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent.Context;
/**
 * A Message should:<br/>
 * 1. Have a Constructor with one parameter of PacketBuffer as deserializer (IMPORTANT)<br/>
 * 2. Implements methods below<br/>
 * 3. Register class in {@link FHNetwork}<br/>
 * 
 * */
public interface FHMessage {

	void encode(FriendlyByteBuf buffer);

	void handle(Supplier<Context> context);
	
	default ResourceLocation packetId() {
		return FHNetwork.getId(this.getClass());
	}
}