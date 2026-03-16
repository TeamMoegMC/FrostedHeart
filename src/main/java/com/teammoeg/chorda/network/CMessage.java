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

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.PacketDistributor.PacketTarget;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;
/**
 * Chorda 网络消息接口，所有网络消息必须实现此接口。
 * 实现类需要满足以下要求：
 * <ol>
 *   <li>必须有一个以 {@link FriendlyByteBuf} 为参数的构造函数作为反序列化器（重要）</li>
 *   <li>实现 {@link #encode(FriendlyByteBuf)} 和 {@link #handle(Supplier)} 方法</li>
 *   <li>在网络处理器中注册该消息类（参见 {@link CBaseNetwork}）</li>
 * </ol>
 * <p>
 * Chorda network message interface. All network messages must implement this interface.
 * Implementations must satisfy the following requirements:
 * <ol>
 *   <li>Must have a constructor with a single {@link FriendlyByteBuf} parameter as deserializer (IMPORTANT)</li>
 *   <li>Implement {@link #encode(FriendlyByteBuf)} and {@link #handle(Supplier)} methods</li>
 *   <li>Register the message class in a network handler (see {@link CBaseNetwork})</li>
 * </ol>
 */
public interface CMessage {

	/**
	 * 将消息数据序列化写入字节缓冲区。
	 * <p>
	 * Serializes the message data into the byte buffer.
	 *
	 * @param buffer 目标字节缓冲区 / the target byte buffer
	 */
	void encode(FriendlyByteBuf buffer);

	/**
	 * 处理接收到的消息。在网络线程中调用，需要通过 {@code context.get().enqueueWork()}
	 * 将游戏逻辑调度到主线程执行。处理完毕后需调用 {@code context.get().setPacketHandled(true)}。
	 * <p>
	 * Handles the received message. Called on the network thread; game logic should be
	 * dispatched to the main thread via {@code context.get().enqueueWork()}.
	 * Must call {@code context.get().setPacketHandled(true)} after handling.
	 *
	 * @param context 网络事件上下文供应器 / the network event context supplier
	 */
	void handle(Supplier<Context> context);

	/**
	 * 通过指定的网络通道向目标发送此消息。
	 * <p>
	 * Sends this message through the specified channel to the given target.
	 *
	 * @param channel 网络通道 / the network channel
	 * @param target 数据包分发目标 / the packet distribution target
	 */
	default void send(SimpleChannel channel,PacketTarget target) {
		channel.send(target, this);
	}
	/**
	 * 通过指定的网络通道将此消息发送到服务端。
	 * <p>
	 * Sends this message to the server through the specified channel.
	 *
	 * @param channel 网络通道 / the network channel
	 */
	default void sendToServer(SimpleChannel channel) {
		channel.sendToServer(this);
	}
}