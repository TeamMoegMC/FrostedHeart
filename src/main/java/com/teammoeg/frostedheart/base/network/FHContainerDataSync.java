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

public class FHContainerDataSync implements FHMessage {

	static record ContainerDataPair(int slotIndex,OtherDataSlotEncoder<?> conv,Object data){
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
	List<ContainerDataPair> data=new ArrayList<>();
	public FHContainerDataSync() {
		super();
	}
	public void add(int slotIndex,OtherDataSlotEncoder<?> conv,Object data) {
		this.getData().add(new ContainerDataPair(slotIndex,conv,data));
	}
	
	public void forEach(BiConsumer<Integer,Object> t) {
		data.forEach(o->t.accept(o.slotIndex, o.data));
	}
	public boolean hasData() {
		return !this.getData().isEmpty();
	}

	public FHContainerDataSync(FriendlyByteBuf buf) {
		SerializeUtil.readList(buf, t->new ContainerDataPair(buf,buf.readVarInt(),FHContainerData.encoders.read(buf)));
	}

	@Override
	public void encode(FriendlyByteBuf buffer) {
		SerializeUtil.writeList(buffer, getData(), ContainerDataPair::write);
	}
	@Override
	public void handle(Supplier<Context> context) {
		context.get().enqueueWork(()->{
			if(ClientUtils.getPlayer().containerMenu instanceof FHBaseContainer container) {
				container.processPacket(this);
			}
		});
	}
	public List<ContainerDataPair> getData() {
		return data;
	}

}
