/*
 * Copyright (c) 2024 TeamMoeg
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

public abstract class CBaseNetwork {

	protected SimpleChannel CHANNEL;
	public void registerChannel() {
		String VERSION = ModList.get().getModContainerById(modid).get().getModInfo().getVersion().toString();
		Chorda.LOGGER.info(modid+" Network Version: " + VERSION);
        CHANNEL = NetworkRegistry.newSimpleChannel(new ResourceLocation(modid,"network"), () -> VERSION, VERSION::equals, VERSION::equals);
	}
	public final void register() {
		
		registerChannel();
		registerMessages();
	};
	public abstract void registerMessages();
	private Map<Class<? extends CMessage>, ResourceLocation> classesId = new IdentityHashMap<>(100);
	private final static OneArgConstructorFactory<FriendlyByteBuf,CMessage> accessorFactory=new OneArgConstructorFactory<>(FriendlyByteBuf.class);
	private int iid = 0;
	private String modid;
	public SimpleChannel get() {
	    return CHANNEL;
	}

	public CBaseNetwork(String modid) {
		super();
		this.modid=modid;
	}

	/**
	 * Register Message Type, would automatically use method in CMessage as serializer and &lt;init&gt;(PacketBuffer) as deserializer
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
	 * Register Message Type, should provide a deserializer
	 */
	public synchronized <T extends CMessage> void registerMessage(String name, Class<T> msg, Function<FriendlyByteBuf, T> func) {
	    classesId.put(msg, new ResourceLocation(modid,name));
	    CHANNEL.registerMessage(++iid, msg, CMessage::encode, func, CMessage::handle);
	    //CHANNEL.registerMessage(++iid,msg,CMessage::encode,func,CMessage::handle);
	}

	public ResourceLocation getId(Class<? extends CMessage> cls) {
	    return classesId.get(cls);
	}
	public void checkIsProperMessage(CMessage message) {
		if(!FMLEnvironment.production) {
			if(!classesId.containsKey(message.getClass())) {
				new Exception().printStackTrace();
				Chorda.LOGGER.error("Message class "+message.getClass().getSimpleName()+" does not registered in this channel!");
				
			}
		}
	}
	private static class MutablePacketTarget{
		
		MutableSupplier<ServerPlayer> ms=new MutableSupplier<>();
		PacketTarget target=PacketDistributor.PLAYER.with(ms);
	}
	private static final ThreadLocal<MutablePacketTarget> playerSupplier=ThreadLocal.withInitial(MutablePacketTarget::new);
	public void sendPlayer(ServerPlayer p, CMessage message) {
		checkIsProperMessage(message);
		//MutablePacketTarget supplier=playerSupplier.get();
		send(PacketDistributor.PLAYER.with(()->p),message);
		//supplier.ms.set(p);
		
	    //send(supplier.target, message);
	}

	public void send(PacketDistributor.PacketTarget target, CMessage message) {
		checkIsProperMessage(message);
	    CHANNEL.send(target, message);
	}

	public void sendToServer(CMessage message) {
		checkIsProperMessage(message);
	    CHANNEL.sendToServer(message);
	}

	public void sendToAll(CMessage message) {
		checkIsProperMessage(message);
	    send(PacketDistributor.ALL.noArg(), message);
	}

	public void sendToTrackingChunk(LevelChunk levelChunk, CMessage message) {
		checkIsProperMessage(message);
	    send(PacketDistributor.TRACKING_CHUNK.with(() -> levelChunk), message);
	}

}