/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.network;

import com.teammoeg.chorda.dataholders.team.CClientTeamDataManager;
import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedheart.bootstrap.common.FHSpecialDataTypes;
import com.teammoeg.frostedheart.content.town.TownStaffingPlan;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Lightweight authoritative replacement of the player-visible staffing plan. */
public final class TownStaffingPlanUpdatePacket implements CMessage {
    private static final int MAX_ENTRIES = 1_024;
    private final TownStaffingPlan plan;

    public TownStaffingPlanUpdatePacket(TownStaffingPlan plan) {
        this.plan = plan;
    }

    public TownStaffingPlanUpdatePacket(FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        if (size < 0 || size > MAX_ENTRIES) {
            throw new IllegalArgumentException("Invalid staffing plan size: " + size);
        }
        List<TownStaffingPlan.Entry> entries = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            entries.add(new TownStaffingPlan.Entry(
                    buffer.readBlockPos(), buffer.readVarInt()));
        }
        this.plan = new TownStaffingPlan(entries);
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        List<TownStaffingPlan.Entry> entries = plan.entries();
        if (entries.size() > MAX_ENTRIES) {
            throw new IllegalArgumentException("Staffing plan exceeds packet limit.");
        }
        buffer.writeVarInt(entries.size());
        for (TownStaffingPlan.Entry entry : entries) {
            buffer.writeBlockPos(entry.building());
            buffer.writeVarInt(entry.targetWorkers());
        }
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> CClientTeamDataManager.INSTANCE.getInstance()
                .getOptional(FHSpecialDataTypes.TOWN_DATA)
                .ifPresent(town -> town.applyStaffingPlan(plan)));
        context.get().setPacketHandled(true);
    }

    public TownStaffingPlan plan() {
        return plan;
    }
}
