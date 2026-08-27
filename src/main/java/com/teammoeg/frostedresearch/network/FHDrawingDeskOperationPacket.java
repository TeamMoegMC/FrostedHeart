/*
 * Copyright (c) 2026 TeamMoeg
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

package com.teammoeg.frostedresearch.network;

import java.util.function.Supplier;
import java.util.UUID;

import com.teammoeg.chorda.dataholders.team.CTeamDataManager;
import com.teammoeg.chorda.dataholders.team.TeamDataHolder;
import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedresearch.blocks.DrawingDeskTileEntity;
import com.teammoeg.frostedresearch.api.TeamResearchService;
import com.teammoeg.frostedresearch.gui.drawdesk.DrawDeskContainer;
import com.teammoeg.frostedresearch.gui.drawdesk.game.CardPos;
import com.teammoeg.frostedresearch.mixinutil.IOwnerTile;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

// send when data update
public class FHDrawingDeskOperationPacket implements CMessage {
    private final BlockPos pos;
    private final byte op;
    private final CardPos pos1;
    private final CardPos pos2;
    private final UUID recordId;
    private final int candidateIndex;
    private final ResourceLocation topicId;
    private final ResourceLocation protocolId;

    public FHDrawingDeskOperationPacket(BlockPos pos) {
        this(pos, 0, null, null, null, 0, null, null);

    }

    public FHDrawingDeskOperationPacket(BlockPos pos, CardPos p1) {
        this(pos, 1, p1, null, null, 0, null, null);
    }

    public FHDrawingDeskOperationPacket(BlockPos pos, CardPos p1, CardPos p2) {
        this(pos, 2, p1, p2, null, 0, null, null);
    }

    public FHDrawingDeskOperationPacket(BlockPos pos, int op) {
        this(pos, op, null, null, null, 0, null, null);

    }

    public FHDrawingDeskOperationPacket(BlockPos pos, int op, UUID recordId) {
        this(pos, op, null, null, recordId, 0, null, null);
    }

    public FHDrawingDeskOperationPacket(BlockPos pos, int op, int candidateIndex) {
        this(pos, op, null, null, null, candidateIndex, null, null);
    }

    public FHDrawingDeskOperationPacket(BlockPos pos, ResourceLocation topicId, ResourceLocation protocolId) {
        this(pos, 8, null, null, null, 0, topicId, protocolId);
    }

    protected FHDrawingDeskOperationPacket(BlockPos pos, int op, CardPos pos1, CardPos pos2,
            UUID recordId, int candidateIndex, ResourceLocation topicId, ResourceLocation protocolId) {
        super();
        this.pos = pos;
        this.op = (byte) op;
        this.pos1 = pos1;
        this.pos2 = pos2;
        this.recordId = recordId;
        this.candidateIndex = candidateIndex;
        this.topicId = topicId;
        this.protocolId = protocolId;
    }

    public FHDrawingDeskOperationPacket(FriendlyByteBuf buffer) {
        this(decode(buffer));
    }

    private FHDrawingDeskOperationPacket(Decoded decoded) {
        this(decoded.pos(), decoded.operation(), decoded.first(), decoded.second(), decoded.recordId(),
                decoded.candidateIndex(), decoded.topicId(), decoded.protocolId());
    }

    private static Decoded decode(FriendlyByteBuf buffer) {
        try {
            BlockPos pos = buffer.readBlockPos();
            byte operation = buffer.readByte();
            CardPos first = operation > 0 && operation < 3 ? CardPos.valueOf(buffer) : null;
            CardPos second = operation > 1 && operation < 3 ? CardPos.valueOf(buffer) : null;
            UUID recordId = operation == 5 ? buffer.readUUID() : null;
            int candidateIndex = operation == 7 ? buffer.readVarInt() : 0;
            ResourceLocation topicId = operation == 8 ? buffer.readResourceLocation() : null;
            ResourceLocation protocolId = operation == 8 ? buffer.readResourceLocation() : null;
            return new Decoded(pos, operation, first, second, recordId, candidateIndex, topicId, protocolId);
        } catch (RuntimeException e) {
            ResearchNetworkCodec.reject("drawing desk: truncated operation");
            return new Decoded(BlockPos.ZERO, (byte) -1, null, null, null, 0, null, null);
        }
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
        buffer.writeByte(op);
        if (op < 3) {
            if (op > 0)
                pos1.write(buffer);
            if (op > 1)
                pos2.write(buffer);
        }
        if (op == 5) buffer.writeUUID(recordId);
        if (op == 7) buffer.writeVarInt(candidateIndex);
        if (op == 8) {
            buffer.writeResourceLocation(topicId);
            buffer.writeResourceLocation(protocolId);
        }
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer sender = context.get().getSender();
            if (sender == null || !isOperationShapeValid(op, pos1, pos2, recordId,
                    candidateIndex, topicId, protocolId)) return;
            DrawingDeskTileEntity tile = getAuthorizedTile(sender);
            if (tile == null) return;

            boolean changed = true;
            switch (op) {
                case 0 -> {
                    if (tile.getGamePurpose() == DrawingDeskTileEntity.GamePurpose.V2_INSPIRATION) {
                        tile.restartInspirationGame();
                    } else {
                        tile.initGame(sender);
                    }
                }
                case 1 -> changed = tile.tryCombine(sender, pos1, null);
                case 2 -> changed = tile.tryCombine(sender, pos1, pos2);
                case 3 -> tile.submitItem(sender);
                case 4 -> tile.initInspirationGame(sender);
                case 5 -> changed = tile.pinEvidence(sender, recordId);
                case 6 -> tile.clearKnowledgeSession();
                case 7 -> changed = tile.recordIdeaCandidate(sender, candidateIndex);
                case 8 -> changed = TeamResearchService.executeProtocolAction(sender, topicId, protocolId);
                case 9 -> changed = TeamResearchService.acceptNextReadyTopicResults(sender);
                default -> changed = false;
            }
            if (changed) {
                tile.updateGame(sender);
                tile.setChanged();
                tile.syncData();
            }
        });
        context.get().setPacketHandled(true);
    }

    private DrawingDeskTileEntity getAuthorizedTile(ServerPlayer sender) {
        if (!(sender.containerMenu instanceof DrawDeskContainer menu)) return null;
        DrawingDeskTileEntity tile = menu.getBlock();
        TeamDataHolder team = CTeamDataManager.get(sender);
        boolean sameMenuTile = tile != null && tile.getBlockPos().equals(pos);
        boolean loadedTile = sameMenuTile && !tile.isRemoved()
                && sender.serverLevel().getBlockEntity(pos) == tile;
        boolean sameLevel = sameMenuTile && tile.getLevel() == sender.serverLevel();
        boolean withinRange = sameMenuTile
                && sender.distanceToSqr(Vec3.atCenterOf(pos)) <= 64.0D;
        boolean ownerMatches = team != null && sameMenuTile
                && team.getId().equals(IOwnerTile.getOwner(tile));
        return passesAuthorizationChecks(sameMenuTile, loadedTile, sameLevel, withinRange, ownerMatches)
                ? tile
                : null;
    }

    static boolean passesAuthorizationChecks(boolean sameMenuTile, boolean loadedTile,
                                               boolean sameLevel, boolean withinRange,
                                               boolean ownerMatches) {
        return sameMenuTile && loadedTile && sameLevel && withinRange && ownerMatches;
    }

    static boolean isOperationShapeValid(byte operation, CardPos first, CardPos second) {
        return isOperationShapeValid(operation, first, second, null);
    }

    static boolean isOperationShapeValid(byte operation, CardPos first, CardPos second, UUID recordId) {
        return isOperationShapeValid(operation, first, second, recordId, 0);
    }

    static boolean isOperationShapeValid(byte operation, CardPos first, CardPos second,
            UUID recordId, int candidateIndex) {
        return isOperationShapeValid(operation, first, second, recordId, candidateIndex, null, null);
    }

    static boolean isOperationShapeValid(byte operation, CardPos first, CardPos second,
            UUID recordId, int candidateIndex, ResourceLocation topicId, ResourceLocation protocolId) {
        return switch (operation) {
            case 0, 3, 4, 6, 9 -> first == null && second == null && recordId == null
                    && topicId == null && protocolId == null;
            case 8 -> first == null && second == null && recordId == null
                    && topicId != null && protocolId != null;
            case 7 -> first == null && second == null && recordId == null
                    && candidateIndex >= 0 && candidateIndex < 3 && topicId == null && protocolId == null;
            case 5 -> first == null && second == null && recordId != null
                    && topicId == null && protocolId == null;
            case 1 -> first != null && first.isWithinBoard() && second == null
                    && topicId == null && protocolId == null;
            case 2 -> first != null && first.isWithinBoard()
                    && second != null && second.isWithinBoard() && topicId == null && protocolId == null;
            default -> false;
        };
    }

    private record Decoded(BlockPos pos, byte operation, CardPos first, CardPos second,
            UUID recordId, int candidateIndex, ResourceLocation topicId, ResourceLocation protocolId) {
    }
}
