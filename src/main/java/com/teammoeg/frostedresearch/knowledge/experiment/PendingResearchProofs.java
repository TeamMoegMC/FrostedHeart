package com.teammoeg.frostedresearch.knowledge.experiment;

import com.teammoeg.frostedresearch.FRMain;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;
import java.util.*;

/** Only pending world trial identity. Position/run indices avoid world or team scans. */
@Mod.EventBusSubscriber(modid=FRMain.MODID)
public final class PendingResearchProofs extends SavedData {
    private static final String FILE="frostedresearch_pending_trials";
    private static final Map<ServerLevel,PendingResearchProofs> LOADED=new IdentityHashMap<>();
    private final Map<Long,ResearchSampleBinding> positions=new HashMap<>();
    private final Map<ResearchSampleBinding,Long> bindings=new HashMap<>();
    private final Map<UUID,Set<Long>> runs=new HashMap<>();
    public static PendingResearchProofs get(ServerLevel level) {
        return LOADED.computeIfAbsent(level,l -> l.getDataStorage().computeIfAbsent(PendingResearchProofs::load,PendingResearchProofs::new,FILE));
    }
    public Optional<ResearchSampleBinding> at(BlockPos pos) { return Optional.ofNullable(positions.get(pos.asLong())); }
    private void add(long pos,ResearchSampleBinding binding) {
        remove(pos);positions.put(pos,binding);bindings.put(binding,pos);runs.computeIfAbsent(binding.run(),k -> new HashSet<>()).add(pos);
    }
    private void remove(long pos) {
        var old=positions.remove(pos);if(old==null) return;
        bindings.remove(old,pos);var indexed=runs.get(old.run());if(indexed!=null) { indexed.remove(pos);if(indexed.isEmpty()) runs.remove(old.run()); }setDirty();
    }
    public static void put(ServerLevel level,BlockPos pos,ResearchSampleBinding binding) {
        removeBinding(binding);var data=get(level);data.add(pos.asLong(),binding);data.setDirty();
    }
    public static void removeBinding(ResearchSampleBinding binding) {
        var server=ServerLifecycleHooks.getCurrentServer();if(server==null) return;
        for(var level:server.getAllLevels()) { var data=get(level);var position=data.bindings.get(binding);if(position!=null) data.remove(position); }
    }
    public static void removeRun(UUID run) {
        var server=ServerLifecycleHooks.getCurrentServer();if(server==null) return;
        for(var level:server.getAllLevels()) {
            var data=get(level);for(long pos:List.copyOf(data.runs.getOrDefault(run,Set.of()))) data.remove(pos);
        }
    }
    public static void blockChanged(ServerLevel level,BlockPos pos) {
        var data=LOADED.get(level);if(data!=null) data.remove(pos.asLong());
    }
    public static PendingResearchProofs load(CompoundTag tag) {
        var data=new PendingResearchProofs();
        for(var raw:tag.getList("proofs",Tag.TAG_COMPOUND)) {
            var entry=(CompoundTag)raw;ResearchSampleBinding.CODEC.parse(NbtOps.INSTANCE,entry.get("binding")).result()
                .ifPresent(binding -> data.add(entry.getLong("position"),binding));
        }data.setDirty(false);return data;
    }
    @Override public CompoundTag save(CompoundTag tag) {
        var entries=new ListTag();positions.forEach((pos,binding) -> {
            var entry=new CompoundTag();entry.putLong("position",pos);entry.put("binding",ResearchSampleBinding.CODEC.encodeStart(NbtOps.INSTANCE,binding).result().orElseThrow());entries.add(entry);
        });tag.put("proofs",entries);return tag;
    }
    @SubscribeEvent public static void loaded(LevelEvent.Load event) { if(event.getLevel() instanceof ServerLevel level) get(level); }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) { LOADED.clear(); }
}
