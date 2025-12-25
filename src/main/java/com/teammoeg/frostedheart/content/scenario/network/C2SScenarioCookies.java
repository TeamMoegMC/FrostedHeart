package com.teammoeg.frostedheart.content.scenario.network;

import java.util.function.Supplier;

import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedheart.content.scenario.FHScenario;
import com.teammoeg.frostedheart.content.scenario.runner.Act;
import com.teammoeg.frostedheart.content.scenario.runner.ActNamespace;
import com.teammoeg.frostedheart.content.scenario.runner.IScenarioVaribles;
import com.teammoeg.frostedheart.content.scenario.runner.RunStatus;
import com.teammoeg.frostedheart.content.scenario.runner.ScenarioConductor;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;

public record C2SScenarioCookies(int id,CompoundTag tag) implements CMessage{
	public C2SScenarioCookies(FriendlyByteBuf buffer) {
		this(buffer.readVarInt(),buffer.readNbt());
	}

	@Override
	public void encode(FriendlyByteBuf buffer) {
		buffer.writeVarInt(id);
		buffer.writeNbt(tag);
	}

	@Override
	public void handle(Supplier<Context> context) {
		context.get().enqueueWork(()->{
			ScenarioConductor scenario=FHScenario.get(context.get().getSender());
			IScenarioVaribles data=scenario.getContext().getVarData();
			for(String s:tag.getAllKeys()) {
				data.setPath("client."+s, tag.get(s));
			}
			Act act=scenario.getById(id);
			if(act!=null)
				act.setStatusIf(RunStatus.WAITNETWORK, RunStatus.RUNNING);
			
		});
		context.get().setPacketHandled(true);
	}

}
