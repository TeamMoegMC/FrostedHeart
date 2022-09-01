package com.teammoeg.frostedheart.network.research;

import com.teammoeg.frostedheart.research.FHResearch;
import com.teammoeg.frostedheart.research.Research;
import com.teammoeg.frostedheart.research.api.ResearchDataAPI;
import com.teammoeg.frostedheart.research.data.ResearchData;
import com.teammoeg.frostedheart.research.data.TeamResearchData;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class FHResearchControlPacket {
    public enum Operator {
        COMMIT_ITEM,
        START,
        PAUSE;
    }

    public final Operator status;
    private final int researchID;


    public FHResearchControlPacket(Operator status, Research research) {
        super();
        this.status = status;
        this.researchID = research.getRId();
    }

    public FHResearchControlPacket(PacketBuffer buffer) {
        researchID = buffer.readVarInt();
        status = Operator.values()[buffer.readVarInt()];
    }

    public void encode(PacketBuffer buffer) {
        buffer.writeVarInt(researchID);
        buffer.writeVarInt(status.ordinal());
    }

    public void handle(Supplier<NetworkEvent.Context> context) {

        context.get().enqueueWork(() -> {
            Research r = FHResearch.researches.getById(researchID);
            ServerPlayerEntity spe = context.get().getSender();
            TeamResearchData trd = ResearchDataAPI.getData(spe);
            switch (status) {
                case COMMIT_ITEM:

                    ResearchData rd = trd.getData(r);
                    if (rd.canResearch()) return;
                    if (rd.commitItem(spe)) {
                        trd.setCurrentResearch(r);
                    }
                    return;
                case START:
                    trd.setCurrentResearch(r);
                    return;
                case PAUSE:
                    trd.clearCurrentResearch(r);
                    return;
            }
        });
        context.get().setPacketHandled(true);
    }
}
