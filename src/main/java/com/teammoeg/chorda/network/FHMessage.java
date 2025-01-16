package com.teammoeg.chorda.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;

import java.util.function.Supplier;
/**
 * A Message should:<br/>
 * 1. Have a Constructor with one parameter of PacketBuffer as deserializer (IMPORTANT)<br/>
 * 2. Implements methods below<br/>
 * 3. Register class in your network handler. Example: ChordaNetwork<br/>
 * 
 * */
public interface FHMessage {

	void encode(FriendlyByteBuf buffer);

	void handle(Supplier<Context> context);
}