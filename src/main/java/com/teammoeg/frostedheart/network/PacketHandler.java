package com.teammoeg.frostedheart.network;

import com.teammoeg.frostedheart.FHMain;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.network.simple.SimpleChannel;

public class PacketHandler
{
    private static final String VERSION = Integer.toString(1);
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(FHMain.rl("network"), () -> VERSION, VERSION::equals, VERSION::equals);

    public static void send(PacketDistributor.PacketTarget target, Object message)
    {
        CHANNEL.send(target, message);
    }

    public static SimpleChannel get()
    {
        return CHANNEL;
    }

    @SuppressWarnings("UnusedAssignment")
    public static void register()
    {
        int id = 0;

        CHANNEL.registerMessage(id++, ChunkWatchPacket.class, ChunkWatchPacket::encode, ChunkWatchPacket::new, ChunkWatchPacket::handle);
        CHANNEL.registerMessage(id++, ChunkUnwatchPacket.class, ChunkUnwatchPacket::encode, ChunkUnwatchPacket::new, ChunkUnwatchPacket::handle);
    }
}