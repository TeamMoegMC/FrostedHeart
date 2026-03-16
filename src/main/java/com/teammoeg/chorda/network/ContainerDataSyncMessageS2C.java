/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.teammoeg.chorda.network;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.io.SerializeUtil;
import com.teammoeg.chorda.menu.CBaseMenu;
import com.teammoeg.chorda.menu.CCustomMenuSlot;
import com.teammoeg.chorda.menu.CCustomMenuSlot.NetworkEncoder;
import com.teammoeg.chorda.menu.CCustomMenuSlot.OtherDataSlotEncoder;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;

/**
 * 服务端到客户端的容器数据同步消息（S2C）。
 * 用于将 {@link CBaseMenu} 中自定义数据槽的变更同步到客户端。
 * 每条消息包含一组 {@code ContainerDataPair}，每对包括槽位索引、编码器和数据。
 * <p>
 * Server-to-client container data synchronization message (S2C).
 * Used to synchronize custom data slot changes in {@link CBaseMenu} to the client.
 * Each message contains a list of {@code ContainerDataPair} entries, each consisting of
 * a slot index, an encoder, and the data payload.
 *
 * @param data 待同步的数据对列表 / the list of data pairs to synchronize
 */
public record ContainerDataSyncMessageS2C(List<ContainerDataPair> data) implements CMessage {

	/**
	 * 容器数据对，将槽位索引与编码器和数据绑定在一起。
	 * <p>
	 * Container data pair that binds a slot index with its encoder and data.
	 *
	 * @param slotIndex 槽位索引 / the slot index
	 * @param conv 网络编码器 / the network encoder
	 * @param data 数据载荷 / the data payload
	 */
	private static record ContainerDataPair(int slotIndex,NetworkEncoder<?> conv,Object data){
		/**
		 * 从字节缓冲区反序列化构造数据对。
		 * <p>
		 * Constructs a data pair by deserializing from the byte buffer.
		 *
		 * @param buf 字节缓冲区 / the byte buffer
		 * @param slotIndex 槽位索引 / the slot index
		 * @param conv 网络编码器 / the network encoder
		 */
		public ContainerDataPair(FriendlyByteBuf buf,int slotIndex,NetworkEncoder<?> conv) {
			this(slotIndex,conv,conv.read(buf));
		}
		/**
		 * 将此数据对序列化写入字节缓冲区。
		 * <p>
		 * Serializes this data pair into the byte buffer.
		 *
		 * @param buffer 目标字节缓冲区 / the target byte buffer
		 */
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public void write(FriendlyByteBuf buffer) {
			buffer.writeVarInt(slotIndex);
			CCustomMenuSlot.Encoders.write(buffer, conv);
			((NetworkEncoder)conv).write(buffer, data);
		}
	}
	
	/**
	 * 构造一个空的容器数据同步消息。
	 * <p>
	 * Constructs an empty container data sync message.
	 */
	public ContainerDataSyncMessageS2C() {
		this(new ArrayList<>());
	}
	/**
	 * 向此消息添加一个数据同步项。
	 * <p>
	 * Adds a data synchronization entry to this message.
	 *
	 * @param slotIndex 槽位索引 / the slot index
	 * @param conv 数据槽编码器 / the data slot encoder
	 * @param data 数据载荷 / the data payload
	 */
	public void add(int slotIndex,OtherDataSlotEncoder<?> conv,Object data) {
		this.data.add(new ContainerDataPair(slotIndex,conv.getEncoder(),data));
	}
	
	/**
	 * 遍历所有数据对，对每对执行指定操作。
	 * <p>
	 * Iterates over all data pairs, applying the given action to each.
	 *
	 * @param t 接收槽位索引和数据的消费函数 / the consumer accepting slot index and data
	 */
	public void forEach(BiConsumer<Integer,Object> t) {
		data.forEach(o->t.accept(o.slotIndex, o.data));
	}
	/**
	 * 检查此消息是否包含待同步的数据。
	 * <p>
	 * Checks whether this message contains any data to synchronize.
	 *
	 * @return 如果包含数据则返回 {@code true} / {@code true} if there is data to synchronize
	 */
	public boolean hasData() {
		return !this.data.isEmpty();
	}

	/**
	 * 从字节缓冲区反序列化构造容器数据同步消息。
	 * <p>
	 * Constructs a container data sync message by deserializing from the byte buffer.
	 *
	 * @param buf 源字节缓冲区 / the source byte buffer
	 */
	public ContainerDataSyncMessageS2C(FriendlyByteBuf buf) {
		this(SerializeUtil.readList(buf, t->new ContainerDataPair(buf,buf.readVarInt(), CCustomMenuSlot.Encoders.read(buf))));
	}

	/** {@inheritDoc} */
	@Override
	public void encode(FriendlyByteBuf buffer) {
		SerializeUtil.writeList(buffer, data, ContainerDataPair::write);
	}
	/**
	 * {@inheritDoc}
	 * <p>
	 * 在客户端主线程上处理数据同步：若当前打开的容器为 {@link CBaseMenu}，则将数据包传递给容器处理。
	 * <p>
	 * Handles data synchronization on the client main thread: if the currently open
	 * container is a {@link CBaseMenu}, the packet is forwarded to the container for processing.
	 */
	@Override
	public void handle(Supplier<Context> context) {
		context.get().enqueueWork(()->{
			if(ClientUtils.getPlayer().containerMenu instanceof CBaseMenu container) {
				container.processPacket(this);
				
			}
		});
		context.get().setPacketHandled(true);
	}

}
