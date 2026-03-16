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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import com.teammoeg.chorda.Chorda;
import com.teammoeg.chorda.asm.OneArgConstructorFactory;
import com.teammoeg.chorda.util.struct.MutableSupplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.PacketDistributor.PacketTarget;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * 基础网络通道抽象类，封装了 Forge 的 {@link SimpleChannel}，提供消息注册和发送功能。
 * 子类需要实现 {@link #registerMessages()} 来注册具体的消息类型。
 * 消息的反序列化通过 {@link OneArgConstructorFactory} 利用 ASM 字节码生成来实现高性能的构造函数调用。
 * <p>
 * Abstract base network channel class that wraps Forge's {@link SimpleChannel}, providing
 * message registration and sending capabilities. Subclasses must implement {@link #registerMessages()}
 * to register concrete message types. Message deserialization uses {@link OneArgConstructorFactory}
 * with ASM bytecode generation for high-performance constructor invocation.
 */
public abstract class CBaseNetwork {

	/** 底层 Forge 网络通道 / The underlying Forge network channel */
	protected SimpleChannel CHANNEL;
	/**
	 * 注册网络通道。使用模组版本号作为协议版本，确保客户端与服务端版本匹配。
	 * <p>
	 * Registers the network channel. Uses the mod version string as the protocol version
	 * to ensure client-server version matching.
	 */
	public void registerChannel() {
		String VERSION = ModList.get().getModContainerById(modid).get().getModInfo().getVersion().toString();
		Chorda.LOGGER.info(modid+" Network Version: " + VERSION);
        CHANNEL = NetworkRegistry.newSimpleChannel(new ResourceLocation(modid,"network"), () -> VERSION, VERSION::equals, VERSION::equals);
	}
	/**
	 * 初始化网络系统：先注册通道，再注册所有消息类型。
	 * <p>
	 * Initializes the network system: registers the channel first, then registers all message types.
	 */
	public final void register() {

		registerChannel();
		registerMessages();
	};
	/**
	 * 注册该网络通道支持的所有消息类型。子类必须实现此方法。
	 * <p>
	 * Registers all message types supported by this network channel. Subclasses must implement this method.
	 */
	public abstract void registerMessages();
	/** 消息类到资源标识的映射，用于调试和校验 / Map from message class to resource location, used for debugging and validation */
	private Map<Class<? extends CMessage>, ResourceLocation> classesId = new IdentityHashMap<>(100);
	/** 基于 ASM 的高性能构造函数工厂，用于反序列化消息 / ASM-based high-performance constructor factory for message deserialization */
	private final static OneArgConstructorFactory<FriendlyByteBuf,CMessage> accessorFactory=new OneArgConstructorFactory<>(FriendlyByteBuf.class);
	/** 自增消息 ID 计数器 / Auto-incrementing message ID counter */
	private int iid = 0;
	/** 所属模组 ID / The owning mod ID */
	private String modid;
	/**
	 * 获取底层的 {@link SimpleChannel} 实例。
	 * <p>
	 * Returns the underlying {@link SimpleChannel} instance.
	 *
	 * @return 底层网络通道 / the underlying network channel
	 */
	public SimpleChannel get() {
	    return CHANNEL;
	}

	/**
	 * 构造一个基础网络通道。
	 * <p>
	 * Constructs a base network channel.
	 *
	 * @param modid 模组 ID，用于命名空间隔离 / the mod ID, used for namespace isolation
	 */
	public CBaseNetwork(String modid) {
		super();
		this.modid=modid;
	}

	/**
	 * 注册消息类型。自动使用 {@link CMessage#encode} 作为序列化器，
	 * 并通过 ASM 字节码生成调用 {@code <init>(FriendlyByteBuf)} 构造函数作为反序列化器。
	 * <p>
	 * Registers a message type. Automatically uses {@link CMessage#encode} as the serializer,
	 * and invokes the {@code <init>(FriendlyByteBuf)} constructor via ASM bytecode generation
	 * as the deserializer.
	 *
	 * @param <T> 消息类型 / the message type
	 * @param name 消息名称，用于生成资源标识 / the message name, used to generate a resource location
	 * @param msg 消息类 / the message class
	 */
	public synchronized <T extends CMessage> void registerMessage(String name, Class<T> msg) {
	    classesId.put(msg, new ResourceLocation(modid,name));
	    try {
	        CHANNEL.registerMessage(++iid, msg, CMessage::encode, accessorFactory.create(msg), CMessage::handle);
	    } catch (Throwable e1) {
	    	Chorda.LOGGER.error("Can not register message " + msg.getSimpleName());
	        e1.printStackTrace();
	    }
	}

	/**
	 * 注册消息类型，需要手动提供反序列化函数。
	 * 当消息类不具备标准的 {@code <init>(FriendlyByteBuf)} 构造函数时使用此重载。
	 * <p>
	 * Registers a message type with a manually provided deserializer function.
	 * Use this overload when the message class does not have a standard
	 * {@code <init>(FriendlyByteBuf)} constructor.
	 *
	 * @param <T> 消息类型 / the message type
	 * @param name 消息名称，用于生成资源标识 / the message name, used to generate a resource location
	 * @param msg 消息类 / the message class
	 * @param func 反序列化函数 / the deserialization function
	 */
	public synchronized <T extends CMessage> void registerMessage(String name, Class<T> msg, Function<FriendlyByteBuf, T> func) {
	    classesId.put(msg, new ResourceLocation(modid,name));
	    CHANNEL.registerMessage(++iid, msg, CMessage::encode, func, CMessage::handle);
	    //CHANNEL.registerMessage(++iid,msg,CMessage::encode,func,CMessage::handle);
	}

	/**
	 * 获取消息类对应的资源标识。
	 * <p>
	 * Gets the resource location associated with a message class.
	 *
	 * @param cls 消息类 / the message class
	 * @return 该消息类对应的资源标识，若未注册则返回 {@code null} / the resource location for the message class, or {@code null} if not registered
	 */
	public ResourceLocation getId(Class<? extends CMessage> cls) {
	    return classesId.get(cls);
	}
	/**
	 * 检查消息是否已在此通道中注册。仅在开发环境中生效，用于捕获未注册消息的错误。
	 * <p>
	 * Checks whether a message is properly registered in this channel.
	 * Only effective in non-production environments, used to catch unregistered message errors.
	 *
	 * @param message 要检查的消息实例 / the message instance to check
	 */
	public void checkIsProperMessage(CMessage message) {
		if(!FMLEnvironment.production) {
			if(!classesId.containsKey(message.getClass())) {
				new Exception().printStackTrace();
				Chorda.LOGGER.error("Message class "+message.getClass().getSimpleName()+" does not registered in this channel!");
				
			}
		}
	}
	/**
	 * 可变数据包目标的内部辅助类，用于线程本地的玩家数据包发送优化。
	 * <p>
	 * Internal helper class for mutable packet target, used for thread-local player packet sending optimization.
	 */
	private static class MutablePacketTarget{

		MutableSupplier<ServerPlayer> ms=new MutableSupplier<>();
		PacketTarget target=PacketDistributor.PLAYER.with(ms);
	}
	/** 线程本地的可变数据包目标缓存 / Thread-local mutable packet target cache */
	private static final ThreadLocal<MutablePacketTarget> playerSupplier=ThreadLocal.withInitial(MutablePacketTarget::new);
	/**
	 * 向指定玩家发送消息。
	 * <p>
	 * Sends a message to a specific player.
	 *
	 * @param p 目标玩家 / the target player
	 * @param message 要发送的消息 / the message to send
	 */
	public void sendPlayer(ServerPlayer p, CMessage message) {
		checkIsProperMessage(message);
		//MutablePacketTarget supplier=playerSupplier.get();
		send(PacketDistributor.PLAYER.with(()->p),message);
		//supplier.ms.set(p);
		
	    //send(supplier.target, message);
	}

	/**
	 * 向指定的数据包目标发送消息。
	 * <p>
	 * Sends a message to the specified packet target.
	 *
	 * @param target 数据包分发目标 / the packet distribution target
	 * @param message 要发送的消息 / the message to send
	 */
	public void send(PacketDistributor.PacketTarget target, CMessage message) {
		checkIsProperMessage(message);
	    CHANNEL.send(target, message);
	}

	/**
	 * 从客户端向服务端发送消息。
	 * <p>
	 * Sends a message from the client to the server.
	 *
	 * @param message 要发送的消息 / the message to send
	 */
	public void sendToServer(CMessage message) {
		checkIsProperMessage(message);
	    CHANNEL.sendToServer(message);
	}

	/**
	 * 向所有已连接的玩家广播消息。
	 * <p>
	 * Broadcasts a message to all connected players.
	 *
	 * @param message 要广播的消息 / the message to broadcast
	 */
	public void sendToAll(CMessage message) {
		checkIsProperMessage(message);
	    send(PacketDistributor.ALL.noArg(), message);
	}

	/**
	 * 向正在追踪指定区块的所有玩家发送消息。
	 * <p>
	 * Sends a message to all players currently tracking the specified chunk.
	 *
	 * @param levelChunk 目标区块 / the target chunk
	 * @param message 要发送的消息 / the message to send
	 */
	public void sendToTrackingChunk(LevelChunk levelChunk, CMessage message) {
		checkIsProperMessage(message);
	    send(PacketDistributor.TRACKING_CHUNK.with(() -> levelChunk), message);
	}

}