/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.network.client;

import com.teammoeg.chorda.dataholders.team.CClientTeamDataManager;
import com.teammoeg.frostedresearch.FRMain;
import com.teammoeg.frostedresearch.FRSpecialDataTypes;
import com.teammoeg.frostedresearch.api.ClientKnowledgeDataAPI;
import com.teammoeg.frostedresearch.compat.ResearchJeiBridge;
import com.teammoeg.frostedresearch.knowledge.KnowledgeSyncSnapshot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Installs a decoded full knowledge snapshot without exposing client code to the packet class. */
@OnlyIn(Dist.CLIENT)
public final class ClientKnowledgeSnapshotHandler {
    private ClientKnowledgeSnapshotHandler() {
    }

    public static void install(KnowledgeSyncSnapshot snapshot) {
        ClientKnowledgeDataAPI.reset();
        try {
            CClientTeamDataManager.INSTANCE.getInstance().setData(
                    FRSpecialDataTypes.KNOWLEDGE_DATA, snapshot.teamData());
            ClientKnowledgeDataAPI.install(
                    snapshot.catalogRevision(), snapshot.knowledge(), snapshot.laboratory(), snapshot.technology());
            FRMain.LOGGER.debug("Installed client knowledge snapshot revision {} "
                            + "({} observations, {} ideas, {} comparisons)",
                    snapshot.catalogRevision(), snapshot.knowledge().observations().size(),
                    snapshot.knowledge().ideas().size(), snapshot.knowledge().comparisons().size());
            ResearchJeiBridge.sync();
        } catch (RuntimeException exception) {
            FRMain.LOGGER.error("Failed to install knowledge snapshot", exception);
        }
    }
}
