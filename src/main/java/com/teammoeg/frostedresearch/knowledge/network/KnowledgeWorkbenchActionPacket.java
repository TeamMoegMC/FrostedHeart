/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge.network;

import java.util.List;
import java.util.function.Supplier;

import com.teammoeg.chorda.dataholders.team.CTeamDataManager;
import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedresearch.FRNetwork;
import com.teammoeg.frostedresearch.blocks.DrawingDeskTileEntity;
import com.teammoeg.frostedresearch.gui.drawdesk.DrawDeskContainer;
import com.teammoeg.frostedresearch.knowledge.KnowledgeService;
import com.teammoeg.frostedresearch.knowledge.KnowledgeDiscussion;
import com.teammoeg.frostedresearch.knowledge.KnowledgeService.OperationResult;
import com.teammoeg.frostedresearch.knowledge.item.ItemRetrieval;
import com.teammoeg.frostedresearch.knowledge.item.ResearchNotes;
import com.teammoeg.frostedresearch.knowledge.model.KnowledgeElement;
import com.teammoeg.frostedresearch.knowledge.model.KnowledgeKey;
import com.teammoeg.frostedresearch.knowledge.state.AcquisitionSource;
import com.teammoeg.frostedresearch.mixinutil.IOwnerTile;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

/**
 * Drawing desk commands resolve selected identities against current server state.
 */
public final class KnowledgeWorkbenchActionPacket implements CMessage {
    public enum Action {REFRESH, LEARN, DELETE, PREVIEW_LINK, LINK, RETRIEVE, EXPORT, DREAM_TOPIC, DISCUSS}

    private final BlockPos pos;
    private final java.util.UUID requestId;
    private final Action action;
    private final List<KnowledgeKey> keys;
    private final String rule;
    private final String title;
    private final String remarks;

    public KnowledgeWorkbenchActionPacket(BlockPos pos, Action action, List<KnowledgeKey> keys,
                                          String rule, String title, String remarks) {
        this.pos = pos;
        this.requestId = java.util.UUID.randomUUID();
        this.action = action;
        this.keys = List.copyOf(keys);
        this.rule = rule;
        this.title = title;
        this.remarks = remarks;
    }

    public KnowledgeWorkbenchActionPacket(FriendlyByteBuf buffer) {
        pos = buffer.readBlockPos();
        requestId = buffer.readUUID();
        action = buffer.readEnum(Action.class);
        keys = buffer.readList(buf -> new KnowledgeKey(buf.readEnum(KnowledgeKey.Kind.class), buf.readUtf()));
        rule = buffer.readUtf();
        title = buffer.readUtf(ResearchNotes.TITLE_LIMIT);
        remarks = buffer.readUtf(ResearchNotes.REMARKS_LIMIT);
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
        buffer.writeUUID(requestId);
        buffer.writeEnum(action);
        buffer.writeCollection(keys, (buf, key) -> {
            buf.writeEnum(key.kind());
            buf.writeUtf(key.id());
        });
        buffer.writeUtf(rule);
        buffer.writeUtf(title, ResearchNotes.TITLE_LIMIT);
        buffer.writeUtf(remarks, ResearchNotes.REMARKS_LIMIT);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player == null) return;
            CompoundTag response = new CompoundTag();
            response.putUUID("request", requestId);
            response.putString("action", action.name());
            DrawingDeskTileEntity desk = desk(player);
            if (desk == null) {
                response.putString("message", "desk.unavailable");
                FRNetwork.INSTANCE.sendPlayer(player, new KnowledgeWorkbenchReplyPacket(response));
                return;
            }
            KnowledgeService service = KnowledgeService.forPlayer(player);
            ListTag results = new ListTag();
            switch (action) {
                case REFRESH -> KnowledgeSnapshotPacket.send(player, CTeamDataManager.get(player));
                case LEARN ->
                        service.learnInbox(keys.stream().distinct().toList()).forEach(result -> addResult(results, result, service));
                case DELETE ->
                        service.deleteInbox(keys.stream().distinct().toList()).forEach(result -> addResult(results, result, service));
                case PREVIEW_LINK -> {
                    ListTag candidates = new ListTag();
                    for (var match : service.previewLinks(keys)) {
                        KnowledgeElement output = service.describe(KnowledgeKey.idea(match.rule().output()));
                        CompoundTag candidate = new CompoundTag();
                        candidate.putString("rule", match.ruleId().toString());
                        candidate.putString("title", output.title());
                        candidate.putString("body", output.body());
                        candidates.add(candidate);
                    }
                    response.put("candidates", candidates);
                }
                case LINK -> {
                    ResourceLocation chosen = ResourceLocation.tryParse(rule);
                    if (chosen != null) {
                        OperationResult result = service.link(chosen, keys);
                        addResult(results, result, service);
                        response.putBoolean("linked", result.status() != KnowledgeService.Status.NO_MATCH);
                    }
                }
                case RETRIEVE -> retrieve(player, desk, service, results, response);
                case EXPORT -> export(desk, service, results, response);
                case DREAM_TOPIC -> {
                    if (keys.size() == 1)
                        addResult(results, service.setDreamTopic(player.getUUID(), keys.get(0)), service);
                }
                case DISCUSS -> {
                    ServerPlayer partner = player.getServer().getPlayerList().getPlayerByName(rule);
                    boolean invited = keys.size() == 1 && partner != null && KnowledgeDiscussion.invite(player, partner, keys.get(0));
                    response.putString("message", invited ? "discussion.invited" : "discussion.unavailable");
                }
            }
            response.put("results", results);
            FRNetwork.INSTANCE.sendPlayer(player, new KnowledgeWorkbenchReplyPacket(response));
        });
        context.get().setPacketHandled(true);
    }

    public java.util.UUID requestId() {
        return requestId;
    }

    private static void retrieve(ServerPlayer player, DrawingDeskTileEntity desk, KnowledgeService service,
                                 ListTag results, CompoundTag response) {
        ItemStack input = desk.getInventory().getStackInSlot(DrawingDeskTileEntity.EXAMINE_SLOT);
        if (input.isEmpty()) {
            response.putString("message", "retrieve.empty");
            return;
        }
        OperationResult result;
        if (ResearchNotes.isNote(input)) {
            // Notes contribute their recorded contents; the carrier itself is never observed.
            var content = ResearchNotes.read(input);
            if (content.isEmpty()) {
                response.putString("message", "note.not_observable");
                return;
            }
            AcquisitionSource receipt = AcquisitionSource.of("research_note", player);
            result = service.receive(content.get().element(), receipt,
                    content.get().originalSource().orElseGet(() -> new AcquisitionSource("unknown", java.util.Optional.empty(), -1)));
        } else {
            result = service.receive(ItemRetrieval.observe(player, input), AcquisitionSource.of("item_retrieval", player));
        }
        addResult(results, result, service);
        if (result.key() != null && service.entry(result.key()) != null) {
            response.put("retrieved_key", com.teammoeg.frostedresearch.knowledge.state.KnowledgeState.encode(KnowledgeKey.CODEC, result.key()));
        }
        response.putString("message", result.succeeded() ? "retrieve.received"
                : result.status() == KnowledgeService.Status.ALREADY_RECEIVED || result.status() == KnowledgeService.Status.ALREADY_OWNED
                ? "retrieve.already_recorded" : "retrieve.unavailable");
    }

    private void export(DrawingDeskTileEntity desk, KnowledgeService service, ListTag results, CompoundTag response) {
        if (keys.size() != 1) {
            response.putString("message", "note.select_one");
            return;
        }
        KnowledgeKey key = keys.get(0);
        OperationResult allowed = service.canExport(key);
        if (!allowed.succeeded()) {
            addResult(results, allowed, service);
            return;
        }
        ItemStack paper = desk.getInventory().getStackInSlot(DrawingDeskTileEntity.PAPER_SLOT);
        ItemStack output = desk.getInventory().getStackInSlot(DrawingDeskTileEntity.EXAMINE_SLOT);
        if (!ResearchNotes.isBlank(paper) || paper.isEmpty()) {
            response.putString("message", "note.blank_required");
            return;
        }
        if (!output.isEmpty()) {
            response.putString("message", "note.output_full");
            return;
        }
        ItemStack written = ResearchNotes.create(service.entry(key), title, remarks);
        if (written.isEmpty()) {
            response.putString("message", "note.not_exportable");
            return;
        }
        paper.shrink(1);
        desk.getInventory().setStackInSlot(DrawingDeskTileEntity.EXAMINE_SLOT, written);
        desk.setChanged();
        desk.syncData();
        response.putBoolean("exported", true);
        response.putString("message", "note.exported");
    }

    private static void addResult(ListTag results, OperationResult result, KnowledgeService service) {
        CompoundTag tag = new CompoundTag();
        tag.putString("status", result.status().name());
        if (result.key() != null) tag.putString("label", service.describe(result.key()).title());
        results.add(tag);
    }

    private DrawingDeskTileEntity desk(ServerPlayer player) {
        if (!(player.containerMenu instanceof DrawDeskContainer menu)) return null;
        DrawingDeskTileEntity desk = menu.getBlock();
        if (desk == null || desk.isRemoved() || !pos.equals(desk.getBlockPos())
                || desk.getLevel() != player.serverLevel() || player.serverLevel().getBlockEntity(pos) != desk
                || player.distanceToSqr(Vec3.atCenterOf(pos)) > 64) return null;
        var team = CTeamDataManager.get(player);
        return team != null && team.getId().equals(IOwnerTile.getOwner(desk)) ? desk : null;
    }
}
