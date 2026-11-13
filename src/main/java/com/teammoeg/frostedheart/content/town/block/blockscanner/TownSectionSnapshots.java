package com.teammoeg.frostedheart.content.town.block.blockscanner;

import java.util.*;
import com.teammoeg.frostedheart.FHMain;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Main-thread registry: no level reference is retained by a worker's cache. */
@Mod.EventBusSubscriber(modid=FHMain.MODID)
public final class TownSectionSnapshots {
    public static final int SECTIONS_PER_TICK=2;
    private static final Map<ServerLevel,SectionSnapshotCache> CACHES=new WeakHashMap<>();
    private static final Map<ServerLevel,Long> SERVICED=new WeakHashMap<>();
    private TownSectionSnapshots(){}
    public static SectionSnapshotCache get(ServerLevel level) {
        return CACHES.computeIfAbsent(level,key->{var cache=new SectionSnapshotCache();cache.advance(level.getGameTime());return cache;});
    }
    public static void tick(MinecraftServer server){for(ServerLevel level:server.getAllLevels())service(level);}
    public static void service(ServerLevel level){
        SectionSnapshotCache cache=CACHES.get(level);if(cache==null)return;
        long tick=level.getGameTime();Long previous=SERVICED.put(level,tick);
        if(previous!=null&&previous.longValue()==tick)return;
        cache.service(tick,SECTIONS_PER_TICK,key->copySection(level,key));
    }
    public static byte[] copySection(ServerLevel level,long key){
        SectionPos section=SectionPos.of(key);
        if(level.getChunkSource().getChunkNow(section.x(),section.z())==null)return null;
        var world=new LoadedTownWorld(level);byte[] cells=new byte[4096];var pos=new BlockPos.MutableBlockPos();
        for(int y=0;y<16;y++)for(int z=0;z<16;z++)for(int x=0;x<16;x++){
            pos.set(section.minBlockX()+x,section.minBlockY()+y,section.minBlockZ()+z);
            cells[(y<<8)|(z<<4)|x]=(byte)world.getPathBlockType(pos).ordinal();
        }
        return cells;
    }
    public static void dirty(ServerLevel level,BlockPos pos){
        SectionSnapshotCache cache=CACHES.get(level);if(cache==null)return;
        // Collision shapes at a section edge may depend on neighbouring blocks, including corners and vertical doors.
        for(int x=(pos.getX()-1)>>4;x<=(pos.getX()+1)>>4;x++)
            for(int y=(pos.getY()-1)>>4;y<=(pos.getY()+1)>>4;y++)
                for(int z=(pos.getZ()-1)>>4;z<=(pos.getZ()+1)>>4;z++)cache.markDirty(SectionPos.asLong(x,y,z),level.getGameTime());
    }
    public static void reload(){CACHES.forEach((level,cache)->cache.markAllDirty(level.getGameTime()));}
    public static void clear(){CACHES.values().forEach(SectionSnapshotCache::close);CACHES.clear();SERVICED.clear();}
    public static SectionSnapshotCache.Statistics statistics(ServerLevel level){return get(level).statistics();}
    @SubscribeEvent public static void unloaded(LevelEvent.Unload event){if(event.getLevel() instanceof ServerLevel level){var cache=CACHES.remove(level);SERVICED.remove(level);if(cache!=null)cache.close();}}
    @SubscribeEvent public static void chunkUnloaded(ChunkEvent.Unload event){
        if(!(event.getLevel() instanceof ServerLevel level))return;
        SectionSnapshotCache cache=CACHES.get(level);if(cache==null)return;var chunk=event.getChunk().getPos();
        cache.invalidateMatching(key->SectionPos.x(key)==chunk.x&&SectionPos.z(key)==chunk.z);
        dirtyNeighbours(cache,chunk.x,chunk.z,level.getGameTime());
    }
    @SubscribeEvent public static void chunkLoaded(ChunkEvent.Load event){
        if(event.getLevel() instanceof ServerLevel level){var cache=CACHES.get(level);if(cache!=null){var chunk=event.getChunk().getPos();dirtyNeighbours(cache,chunk.x,chunk.z,level.getGameTime());}}
    }
    private static void dirtyNeighbours(SectionSnapshotCache cache,int x,int z,long tick){
        // Only existing entries are examined; chunk load events never create cache entries or load terrain.
        cache.markMatchingDirty(key->Math.abs(SectionPos.x(key)-x)<=1&&Math.abs(SectionPos.z(key)-z)<=1,tick);
    }
}
