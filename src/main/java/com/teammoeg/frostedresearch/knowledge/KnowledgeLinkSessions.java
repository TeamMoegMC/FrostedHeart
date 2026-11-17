package com.teammoeg.frostedresearch.knowledge;

import com.teammoeg.frostedresearch.FRMain;
import com.teammoeg.frostedresearch.FRNetwork;
import com.teammoeg.frostedresearch.gui.drawdesk.DrawDeskContainer;
import com.teammoeg.frostedresearch.gui.drawdesk.game.*;
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

/** Per-player idea-link puzzles; no timer, offline work or persistent board. */
@Mod.EventBusSubscriber(modid = FRMain.MODID)
public final class KnowledgeLinkSessions {
    public record Session(UUID team, int menu, BlockPos desk, ResourceLocation rule, List<KnowledgeKey> keys,
            ResourceLocation output, String title, int missing, int numbers, ResearchGame game) {
        public Session { keys = List.copyOf(keys); desk = desk.immutable(); }
    }
    public record Update(String state, Session session) {
        public CompoundTag payload() {
            var tag = new CompoundTag();
            tag.putString("action", "LINK_GAME"); tag.putString("link_state", state);
            if (session != null) {
                tag.putInt("link_missing", session.missing); tag.putInt("link_numbers", session.numbers);
                tag.putString("link_title", session.title); tag.put("link_game", session.game.serialize());
            }
            return tag;
        }
    }
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    public static Optional<Session> session(UUID player) { return Optional.ofNullable(SESSIONS.get(player)); }

    /** Link-specific optional conditions override behavior conditions, including an explicit empty list. */
    public static int missingOptional(KnowledgeService service, LinkRule rule) {
        var optional = service.linkOptionalKnowledge(rule);
        return (int) optional.stream().filter(condition -> service.matchKnowledge(condition).isEmpty()).count();
    }
    public static int numbers(KnowledgeService service, LinkRule rule) {
        return Math.min(7, missingOptional(service, rule));
    }
    public static KnowledgeService.Status start(UUID player, int menu, BlockPos desk, KnowledgeService service,
            ResourceLocation rule, List<KnowledgeKey> keys) {
        if (SESSIONS.containsKey(player)) return KnowledgeService.Status.ACTIVE;
        var match = service.matchLink(rule, keys).orElse(null);
        if (match == null) return KnowledgeService.Status.NO_MATCH;
        var output = KnowledgeKey.idea(match.rule().output());
        var query = service.query(output);
        if (query.inArchive()) return KnowledgeService.Status.ALREADY_OWNED;
        if (query.inInbox()) return KnowledgeService.Status.ALREADY_RECEIVED;
        int missing = missingOptional(service, match.rule()), numbers = Math.min(7, missing);
        var game = new ResearchGame(); int level = GenerateInfo.levelForNumbers(numbers);
        game.init(GenerateInfo.all[level], new Random()); game.setLvl(level);
        var requiredKeys = match.rule().inputs().stream().map(slot -> match.bindings().get(slot.name())).toList();
        SESSIONS.put(player, new Session(service.teamId(), menu, desk, rule, requiredKeys, match.rule().output(),
                service.describe(output).title(), missing, numbers, game));
        return KnowledgeService.Status.SUCCESS;
    }
    public static boolean cancel(UUID player) { return SESSIONS.remove(player) != null; }
    public static Update check(UUID player, KnowledgeService service) {
        Session session = SESSIONS.get(player);
        if (session == null) return new Update("idle", null);
        var match = service.matchLink(session.rule, session.keys).orElse(null);
        var output = service.query(KnowledgeKey.idea(session.output));
        if (!session.team.equals(service.teamId()) || match == null || !match.rule().output().equals(session.output)
                || output.inArchive() || output.inInbox()) {
            cancel(player); return new Update("cancelled", null);
        }
        return new Update("running", session);
    }
    public static Update play(UUID player, KnowledgeService service, CardPos first, CardPos second) {
        return play(player, service, first, second, false);
    }
    private static Update play(UUID player, KnowledgeService service, CardPos first, CardPos second, boolean cheat) {
        Update update = check(player, service);
        if (update.session == null) return update;
        Session session = update.session;
        if (first == null || !first.isWithinBoard() || second != null && !second.isWithinBoard()) return update;
        if (cheat && second == null && ResearchGame.FINAL_CARD.equals(first)) session.game.forceWin();
        else session.game.tryCombine(first, second);
        if (!session.game.isFinished()) return update;
        cancel(player); // Remove before producing knowledge or invoking callbacks.
        service.link(session.rule, session.keys);
        var output = service.query(KnowledgeKey.idea(session.output));
        return new Update(output.inArchive() || output.inInbox() ? "completed" : "cancelled", null);
    }
    public static Update reset(UUID player, KnowledgeService service) {
        Update update = check(player, service);
        if (update.session == null) return update;
        Session session = update.session;
        cancel(player);
        start(player, session.menu, session.desk, service, session.rule, session.keys);
        return check(player, service);
    }
    /** Reuses the existing desk card-operation packet; legacy boards and paper/ink stay independent. */
    public static void handleDeskOperation(ServerPlayer player, BlockPos desk, int operation, CardPos first, CardPos second) {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null) { send(player, new Update("idle", null)); return; }
        if (session.menu != player.containerMenu.containerId || !session.desk.equals(desk)) {
            cancel(player.getUUID()); send(player, new Update("cancelled", null)); return;
        }
        var service = KnowledgeService.forPlayer(player);
        Update update = operation == 0 ? reset(player.getUUID(), service)
                : play(player.getUUID(), service, first, second, player.hasPermissions(2));
        send(player, update);
        if (update.state.equals("completed"))
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("knowledge.frostedresearch.link.completed"));
    }
    public static void send(ServerPlayer player, Update update) {
        FRNetwork.INSTANCE.sendPlayer(player, new KnowledgeWorkbenchReplyPacket(update.payload()));
    }
    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        Session session = SESSIONS.get(player.getUUID()); if (session == null) return;
        if (!(player.containerMenu instanceof DrawDeskContainer desk) || desk.containerId != session.menu
                || !desk.getBlock().getBlockPos().equals(session.desk) || !desk.stillValid(player)) {
            cancel(player.getUUID()); send(player, new Update("cancelled", null)); return;
        }
        if (player.level().getGameTime() % 20 == 0) {
            Update update = check(player.getUUID(), KnowledgeService.forPlayer(player));
            if (!update.state.equals("running")) send(player, update);
        }
    }
    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) { cancel(event.getEntity().getUUID()); }
    public static void clear() { SESSIONS.clear(); }
}
