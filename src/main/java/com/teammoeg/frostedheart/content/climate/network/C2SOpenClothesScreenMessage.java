package com.teammoeg.frostedheart.content.climate.network;

import java.util.function.Supplier;

import com.teammoeg.chorda.menu.DummyMenuProvider;
import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedheart.content.climate.block.ClothesInventoryMenu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;
import net.minecraftforge.network.NetworkHooks;

public record C2SOpenClothesScreenMessage() implements CMessage{
	public C2SOpenClothesScreenMessage(FriendlyByteBuf buffer) {
		this();
	}
	@Override
	public void encode(FriendlyByteBuf buffer) {
	}

	@Override
	public void handle(Supplier<Context> context) {
		context.get().enqueueWork(()->{
			NetworkHooks.openScreen(context.get().getSender(), new DummyMenuProvider((id,inv,player)->new ClothesInventoryMenu(id,inv)));
		});
		context.get().setPacketHandled(true);
	}

}
