/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.town.network;

import com.teammoeg.chorda.dataholders.team.CClientTeamDataManager;
import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedheart.bootstrap.common.FHSpecialDataTypes;
import com.teammoeg.frostedheart.content.town.TownHousingPlan;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Lightweight authoritative replacement of the residential-care plan. */
public final class TownHousingPlanUpdatePacket implements CMessage {
    private static final int MAX_ENTRIES = 1_024;
    private final TownHousingPlan plan;

    public TownHousingPlanUpdatePacket(TownHousingPlan plan) { this.plan = plan; }

    public TownHousingPlanUpdatePacket(FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        if (size < 0 || size > MAX_ENTRIES) throw new IllegalArgumentException("Invalid housing plan size");
        List<TownHousingPlan.Entry> entries = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            entries.add(new TownHousingPlan.Entry(buffer.readBlockPos(), buffer.readVarInt()));
        }
        plan = new TownHousingPlan(entries);
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        if (plan.entries().size() > MAX_ENTRIES) throw new IllegalArgumentException("Housing plan too large");
        buffer.writeVarInt(plan.entries().size());
        for (TownHousingPlan.Entry entry : plan.entries()) {
            buffer.writeBlockPos(entry.building());
            buffer.writeVarInt(entry.guaranteedResidents());
        }
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> CClientTeamDataManager.INSTANCE.getInstance()
                .getOptional(FHSpecialDataTypes.TOWN_DATA)
                .ifPresent(town -> town.applyHousingPlan(plan)));
        context.get().setPacketHandled(true);
    }

    public TownHousingPlan plan() { return plan; }
}
