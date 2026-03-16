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

import java.util.function.Supplier;

import com.teammoeg.chorda.menu.CBaseMenu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

/**
 * 客户端到服务端的容器操作消息（C2S）。
 * 当客户端在 {@link CBaseMenu} 中触发自定义按钮操作时发送此消息，
 * 服务端收到后验证容器 ID 并将操作分发给对应的容器处理。
 * <p>
 * Client-to-server container operation message (C2S).
 * Sent when the client triggers a custom button operation in a {@link CBaseMenu}.
 * The server validates the container ID and dispatches the operation to the
 * corresponding container for processing.
 *
 * @param containerId 容器 ID，用于服务端校验 / the container ID, used for server-side validation
 * @param buttonId 按钮 ID，标识触发的操作 / the button ID identifying the triggered operation
 * @param state 操作状态值 / the operation state value
 */
public record ContainerOperationMessageC2S(int containerId, short buttonId, int state) implements CMessage {

	/**
	 * 从字节缓冲区反序列化构造容器操作消息。
	 * <p>
	 * Constructs a container operation message by deserializing from the byte buffer.
	 *
	 * @param buf 源字节缓冲区 / the source byte buffer
	 */
	public ContainerOperationMessageC2S(FriendlyByteBuf buf) {
		this(buf.readVarInt(),buf.readShort(),buf.readVarInt());
	}

	/** {@inheritDoc} */
	@Override
	public void encode(FriendlyByteBuf buffer) {
		buffer.writeVarInt(containerId);
		buffer.writeShort(buttonId);
		buffer.writeVarInt(state);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * 在服务端主线程上处理容器操作：验证发送玩家当前打开的容器 ID 是否匹配，
	 * 若匹配且容器为 {@link CBaseMenu} 则调用 {@code receiveMessage} 处理操作。
	 * <p>
	 * Handles the container operation on the server main thread: validates that the
	 * sending player's currently open container ID matches, and if the container is
	 * a {@link CBaseMenu}, calls {@code receiveMessage} to process the operation.
	 */
	@Override
	public void handle(Supplier<Context> context) {
		context.get().enqueueWork(()->{
			Context ctx=context.get();
			ServerPlayer player=ctx.getSender();
			//System.out.print("received operation packet "+this);
			//System.out.print("player container "+player.containerMenu.containerId+ ":"+player.containerMenu);
			if(player.containerMenu.containerId==containerId&&player.containerMenu instanceof CBaseMenu container) {
				//System.out.println("calling message received");
				container.receiveMessage(buttonId, state);
			}
		});
		context.get().setPacketHandled(true);
	}

}
