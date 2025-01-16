package com.teammoeg.chorda.network;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import com.teammoeg.chorda.menu.CContainer;
import com.teammoeg.chorda.util.utility.CContainerData;
import com.teammoeg.chorda.util.utility.CContainerData.OtherDataSlotEncoder;
import com.teammoeg.chorda.util.client.ClientUtils;
import com.teammoeg.chorda.util.io.SerializeUtil;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;

public record CContainerDataSync(List<ContainerDataPair> data) implements CMessage {

	private static record ContainerDataPair(int slotIndex,OtherDataSlotEncoder<?> conv,Object data){
		public ContainerDataPair(FriendlyByteBuf buf,int slotIndex,OtherDataSlotEncoder<?> conv) {
			this(slotIndex,conv,conv.read(buf));
		}
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public void write(FriendlyByteBuf buffer) {
			buffer.writeVarInt(slotIndex);
			CContainerData.encoders.write(buffer, conv);
			((OtherDataSlotEncoder)conv).write(buffer, data);
		}
	}
	
	public CContainerDataSync() {
		this(new ArrayList<>());
	}
	public void add(int slotIndex,OtherDataSlotEncoder<?> conv,Object data) {
		this.data.add(new ContainerDataPair(slotIndex,conv,data));
	}
	
	public void forEach(BiConsumer<Integer,Object> t) {
		data.forEach(o->t.accept(o.slotIndex, o.data));
	}
	public boolean hasData() {
		return !this.data.isEmpty();
	}

	public CContainerDataSync(FriendlyByteBuf buf) {
		this(SerializeUtil.readList(buf, t->new ContainerDataPair(buf,buf.readVarInt(), CContainerData.encoders.read(buf))));
	}

	@Override
	public void encode(FriendlyByteBuf buffer) {
		SerializeUtil.writeList(buffer, data, ContainerDataPair::write);
	}
	@Override
	public void handle(Supplier<Context> context) {
		context.get().enqueueWork(()->{
			if(ClientUtils.getPlayer().containerMenu instanceof CContainer container) {
				container.processPacket(this);
				context.get().setPacketHandled(true);
			}
		});
	}

}
