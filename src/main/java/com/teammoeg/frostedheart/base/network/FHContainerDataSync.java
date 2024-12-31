package com.teammoeg.frostedheart.base.network;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import com.teammoeg.frostedheart.base.menu.FHBaseContainer;
import com.teammoeg.frostedheart.util.FHContainerData;
import com.teammoeg.frostedheart.util.FHContainerData.OtherDataSlotEncoder;
import com.teammoeg.frostedheart.util.client.ClientUtils;
import com.teammoeg.frostedheart.util.io.SerializeUtil;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;

public record FHContainerDataSync(List<ContainerDataPair> data) implements FHMessage {

	private static record ContainerDataPair(int slotIndex,OtherDataSlotEncoder<?> conv,Object data){
		public ContainerDataPair(FriendlyByteBuf buf,int slotIndex,OtherDataSlotEncoder<?> conv) {
			this(slotIndex,conv,conv.read(buf));
		}
		@SuppressWarnings("unchecked")
		public void write(FriendlyByteBuf buffer) {
			buffer.writeVarInt(slotIndex);
			FHContainerData.encoders.write(buffer, conv);
			((OtherDataSlotEncoder)conv).write(buffer, data);
		}
	}
	
	public FHContainerDataSync() {
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

	public FHContainerDataSync(FriendlyByteBuf buf) {
		this(SerializeUtil.readList(buf, t->new ContainerDataPair(buf,buf.readVarInt(),FHContainerData.encoders.read(buf))));
	}

	@Override
	public void encode(FriendlyByteBuf buffer) {
		SerializeUtil.writeList(buffer, data, ContainerDataPair::write);
	}
	@Override
	public void handle(Supplier<Context> context) {
		context.get().enqueueWork(()->{
			if(ClientUtils.getPlayer().containerMenu instanceof FHBaseContainer container) {
				container.processPacket(this);
				context.get().setPacketHandled(true);
			}
		});
	}

}
