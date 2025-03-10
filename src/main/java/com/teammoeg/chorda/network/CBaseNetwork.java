package com.teammoeg.chorda.network;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Function;

import com.teammoeg.chorda.Chorda;
import com.teammoeg.frostedheart.FHMain;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public abstract class CBaseNetwork {

	protected SimpleChannel CHANNEL;

	public abstract void register();

	private Map<Class<? extends CMessage>, ResourceLocation> classesId = new IdentityHashMap<>(100);
	private final static PacketLoaderFactory accessorFactory=new PacketLoaderFactory();
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
	        Constructor<T> ctor = msg.getDeclaredConstructor(FriendlyByteBuf.class);
	        ctor.setAccessible(true);
	        CHANNEL.registerMessage(++iid, msg, CMessage::encode, accessorFactory.create(ctor), CMessage::handle);
	    } catch (NoSuchMethodException | SecurityException | InvocationTargetException e1) {
	        FHMain.LOGGER.error("Can not register message " + msg.getSimpleName());
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
	public void sendPlayer(ServerPlayer p, CMessage message) {
		checkIsProperMessage(message);
	    send(PacketDistributor.PLAYER.with(() -> p), message);
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