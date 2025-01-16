package com.teammoeg.chorda;

import com.teammoeg.chorda.network.FHContainerDataSync;
import com.teammoeg.chorda.network.FHContainerOperation;
import com.teammoeg.chorda.network.FHMessage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class ChordaNetwork {
    private static SimpleChannel CHANNEL;
    private static Map<Class<? extends FHMessage>, ResourceLocation> classesId = new HashMap<>();

    public static SimpleChannel get() {
        return CHANNEL;
    }

    private static int iid = 0;

    /**
     * Register Message Type, would automatically use method in FHMessage as serializer and &lt;init&gt;(PacketBuffer) as deserializer
     */
    public static synchronized <T extends FHMessage> void registerMessage(String name, Class<T> msg) {
        classesId.put(msg, Chorda.rl(name));
        try {
            Constructor<T> ctor = msg.getConstructor(FriendlyByteBuf.class);
            CHANNEL.registerMessage(++iid, msg, FHMessage::encode, pb -> {
                try {
                    return ctor.newInstance(pb);
                } catch (IllegalAccessException | IllegalArgumentException | InstantiationException |
                         InvocationTargetException e) {
                    e.printStackTrace();
                    throw new RuntimeException("Can not create message " + msg.getSimpleName()+e.getMessage(), e);
                }
            }, FHMessage::handle);
        } catch (NoSuchMethodException | SecurityException e1) {
            Chorda.LOGGER.error("Can not register message " + msg.getSimpleName());
            e1.printStackTrace();
        }
    }

    /**
     * Register Message Type, should provide a deserializer
     */
    public static synchronized <T extends FHMessage> void registerMessage(String name, Class<T> msg, Function<FriendlyByteBuf, T> func) {
        classesId.put(msg, Chorda.rl(name));
        CHANNEL.registerMessage(++iid, msg, FHMessage::encode, func, FHMessage::handle);
        //CHANNEL.registerMessage(++iid,msg,FHMessage::encode,func,FHMessage::handle);
    }

    public static ResourceLocation getId(Class<? extends FHMessage> cls) {
        return classesId.get(cls);
    }

    public static void register() {
        String VERSION = ModList.get().getModContainerById(Chorda.MODID).get().getModInfo().getVersion().toString();
        Chorda.LOGGER.info("FH Network Version: " + VERSION);
        CHANNEL = NetworkRegistry.newSimpleChannel(Chorda.rl("network"), () -> VERSION, VERSION::equals, VERSION::equals);

        //Fundamental Message
        registerMessage("container_operation", FHContainerOperation.class);
        registerMessage("container_sync", FHContainerDataSync.class);

    }

    public static void sendPlayer(ServerPlayer p, FHMessage message) {
        send(PacketDistributor.PLAYER.with(() -> p), message);
    }

    public static void send(PacketDistributor.PacketTarget target, FHMessage message) {
        CHANNEL.send(target, message);
    }

    public static void sendToServer(FHMessage message) {
        CHANNEL.sendToServer(message);
    }

    public static void sendToAll(FHMessage message) {
        send(PacketDistributor.ALL.noArg(), message);
    }

    public static void sendToTrackingChunk(LevelChunk levelChunk, FHMessage packet) {
        send(PacketDistributor.TRACKING_CHUNK.with(() -> levelChunk), packet);
    }
}
