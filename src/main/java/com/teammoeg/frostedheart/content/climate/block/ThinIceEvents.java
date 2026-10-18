package com.teammoeg.frostedheart.content.climate.block;

import com.teammoeg.frostedheart.FHMain;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.IdentityHashMap;
import java.util.Map;

/** Only positions touched this tick are visited; standing ice needs no entity search. */
@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ThinIceEvents {
    private static final Map<ServerLevel, Long2IntOpenHashMap> CONTACTS = new IdentityHashMap<>();

    private ThinIceEvents() {}

    static void contact(ServerLevel level, BlockPos pos, int interval) {
        var contacts = CONTACTS.computeIfAbsent(level, ignored -> new Long2IntOpenHashMap());
        long key = pos.asLong();
        int previous = contacts.get(key);
        if (previous == 0 || interval < previous) contacts.put(key, interval);
    }

    @SubscribeEvent
    public static void endTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) return;
        var contacts = CONTACTS.get(level);
        if (contacts == null || contacts.isEmpty()) {
            ThinIcePhaseData.tick(level);
            return;
        }
        var entries = contacts.long2IntEntrySet().fastIterator();
        while (entries.hasNext()) {
            var entry = entries.next();
            var pos = BlockPos.of(entry.getLongKey());
            if (level.getBlockState(pos).getBlock() instanceof ThinIceBlock ice)
                ice.ordinaryContact(level, pos, entry.getIntValue());
        }
        contacts.clear();
        ThinIcePhaseData.tick(level);
    }

    @SubscribeEvent
    public static void standing(LivingTickEvent event) {
        var entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel level) || !entity.onGround() || !ThinIceBlock.canCrack(entity)) return;
        // 1.20.1 server players need not call move()/stepOn every tick while stationary.
        var pos = entity.getOnPos();
        if (level.getBlockState(pos).getBlock() instanceof ThinIceBlock)
            contact(level, pos, ThinIceBlock.contactInterval(entity));
    }

    @SubscribeEvent
    public static void jump(LivingJumpEvent event) {
        var entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel level) || !ThinIceBlock.canCrack(entity)) return;
        var pos = entity.getOnPos();
        if (level.getBlockState(pos).getBlock() instanceof ThinIceBlock ice)
            ice.impact(level, pos, 1);
    }

    @SubscribeEvent
    public static void unload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) CONTACTS.remove(level);
    }
}
