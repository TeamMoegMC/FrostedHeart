package com.teammoeg.frostedresearch.knowledge;

import com.teammoeg.frostedresearch.FRConfig;
import com.teammoeg.frostedresearch.FRMain;
import com.teammoeg.frostedresearch.FRNetwork;
import com.teammoeg.frostedresearch.gui.drawdesk.DrawDeskContainer;
import com.teammoeg.frostedresearch.knowledge.model.KnowledgeKey;
import com.teammoeg.frostedresearch.knowledge.link.LinkRule;
import com.teammoeg.frostedresearch.knowledge.network.KnowledgeWorkbenchReplyPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

/** Transient per-player desk waits. No offline timer or candidate ledger is persisted. */
@Mod.EventBusSubscriber(modid = FRMain.MODID)
public final class KnowledgeLinkSessions {
    public record Session(UUID team, int menu, BlockPos desk, ResourceLocation rule, List<KnowledgeKey> keys, long started, int duration) {
        public Session { keys = List.copyOf(keys); desk = desk.immutable(); }
    }
    public record Update(String state, int elapsed, int duration) {}
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    public static Optional<Session> session(UUID player) { return Optional.ofNullable(SESSIONS.get(player)); }
    public static int duration(KnowledgeService service, ResourceLocation output) {
        return duration(IdeaAcquisitionService.optionalCount(service, output));
    }
    public static int duration(KnowledgeService service, LinkRule rule) {
        int optional = rule.optionalKnowledge().map(conditions -> (int) conditions.stream()
            .filter(c -> service.matchKnowledge(c).isPresent()).count())
            .orElseGet(() -> IdeaAcquisitionService.optionalCount(service, rule.output()));
        return duration(optional);
    }
    private static int duration(int optional) {
        int base = FRConfig.SERVER.knowledgeLinkTicks.get();
        return Math.max(Math.min(base, FRConfig.SERVER.knowledgeLinkMinimumTicks.get()), base
            - optional * FRConfig.SERVER.knowledgeLinkOptionalReduction.get());
    }
    public static KnowledgeService.Status start(UUID player, int menu, BlockPos desk, KnowledgeService service,
            ResourceLocation rule, List<KnowledgeKey> keys, long now) {
        if (SESSIONS.containsKey(player)) return KnowledgeService.Status.ACTIVE;
        var match = service.matchLink(rule, keys).orElse(null);
        if (match == null) return KnowledgeService.Status.NO_MATCH;
        SESSIONS.put(player, new Session(service.teamId(), menu, desk, rule, keys, now, duration(service, match.rule())));
        return KnowledgeService.Status.SUCCESS;
    }
    public static boolean cancel(UUID player) { return SESSIONS.remove(player) != null; }
    public static Update advance(UUID player, KnowledgeService service, long now) {
        Session s = SESSIONS.get(player);
        if (s == null) return new Update("idle", 0, 0);
        boolean valid = s.team.equals(service.teamId()) && service.matchLink(s.rule, s.keys).isPresent();
        if (!valid) { cancel(player); return new Update("cancelled", 0, s.duration); }
        int elapsed = (int)Math.max(0, Math.min(s.duration, now - s.started));
        if (elapsed < s.duration) return new Update("running", elapsed, s.duration);
        cancel(player); // Remove before producing knowledge or invoking callbacks.
        var rule = com.teammoeg.frostedresearch.knowledge.definition.KnowledgeDefinitions.current().links().get(s.rule);
        service.link(s.rule, s.keys);
        var query = rule == null ? null : service.query(KnowledgeKey.idea(rule.output()));
        return new Update(query != null && (query.inArchive() || query.inInbox()) ? "completed" : "cancelled", elapsed, s.duration);
    }
    public static void send(ServerPlayer player, Update update) {
        CompoundTag tag = new CompoundTag(); tag.putString("action", "LINK_PROGRESS"); tag.putString("link_state", update.state);
        tag.putInt("link_elapsed", update.elapsed); tag.putInt("link_duration", update.duration);
        FRNetwork.INSTANCE.sendPlayer(player, new KnowledgeWorkbenchReplyPacket(tag));
    }
    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        Session s = SESSIONS.get(player.getUUID()); if (s == null) return;
        if (!(player.containerMenu instanceof DrawDeskContainer desk) || desk.containerId != s.menu
                || !desk.getBlock().getBlockPos().equals(s.desk) || !desk.stillValid(player)) {
            cancel(player.getUUID()); send(player, new Update("cancelled", 0, s.duration)); return;
        }
        Update update = advance(player.getUUID(), KnowledgeService.forPlayer(player), player.level().getGameTime());
        if (!update.state.equals("running") || update.elapsed % 20 == 0) {
            send(player, update);
            if (update.state.equals("completed")) player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("knowledge.frostedresearch.link.completed"));
        }
    }
    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) { cancel(event.getEntity().getUUID()); }
    public static void clear() { SESSIONS.clear(); }
}
