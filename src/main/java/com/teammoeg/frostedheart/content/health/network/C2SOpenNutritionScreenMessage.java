package com.teammoeg.frostedheart.content.health.network;

import java.util.function.Supplier;

import com.teammoeg.chorda.menu.DummyMenuProvider;
import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedheart.content.health.screen.HealthStatMenu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;
import net.minecraftforge.network.NetworkHooks;

public record C2SOpenNutritionScreenMessage() implements CMessage {
	public C2SOpenNutritionScreenMessage(FriendlyByteBuf buffer) {
		this();
	}
	@Override
	public void encode(FriendlyByteBuf buffer) {
	}

	@Override
	public void handle(Supplier<Context> context) {
		context.get().enqueueWork(()->{
			NetworkHooks.openScreen(context.get().getSender(), new DummyMenuProvider((id,inv,player)->new HealthStatMenu(id,inv)));
		});
		context.get().setPacketHandled(true);
	}

}
