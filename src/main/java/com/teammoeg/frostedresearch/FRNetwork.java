package com.teammoeg.frostedresearch;

import com.teammoeg.chorda.network.CBaseNetwork;
import com.teammoeg.frostedresearch.network.FHChangeActiveResearchPacket;
import com.teammoeg.frostedresearch.network.FHDrawingDeskOperationPacket;
import com.teammoeg.frostedresearch.network.FHEffectProgressSyncPacket;
import com.teammoeg.frostedresearch.network.FHEffectTriggerPacket;
import com.teammoeg.frostedresearch.network.FHInsightSyncPacket;
import com.teammoeg.frostedresearch.network.FHResearchAttributeSyncPacket;
import com.teammoeg.frostedresearch.network.FHResearchControlPacket;
import com.teammoeg.frostedresearch.network.FHResearchDataSyncPacket;
import com.teammoeg.frostedresearch.network.FHResearchDataUpdatePacket;
import com.teammoeg.frostedresearch.network.FHResearchRegistrtySyncPacket;
import com.teammoeg.frostedresearch.network.FHResearchSyncEndPacket;
import com.teammoeg.frostedresearch.network.FHResearchSyncPacket;
import com.teammoeg.frostedresearch.network.FHS2CClueProgressSyncPacket;

public class FRNetwork extends CBaseNetwork {
	public static final FRNetwork INSTANCE=new FRNetwork();
	private FRNetwork() {
		super(FRMain.MODID);
	}

	@Override
	public void register() {

        //Research Messages
        registerMessage("research_registry", FHResearchRegistrtySyncPacket.class);
        registerMessage("research_sync", FHResearchSyncPacket.class);
        registerMessage("research_sync_end", FHResearchSyncEndPacket.class);
        registerMessage("research_data", FHResearchDataSyncPacket.class);
        registerMessage("research_data_update", FHResearchDataUpdatePacket.class);
        registerMessage("research_clue", FHS2CClueProgressSyncPacket.class);
        registerMessage("research_attribute", FHResearchAttributeSyncPacket.class);
        registerMessage("effect_trigger", FHEffectTriggerPacket.class);
        registerMessage("research_control", FHResearchControlPacket.class);
        registerMessage("research_select", FHChangeActiveResearchPacket.class);
        registerMessage("research_drawdesk", FHDrawingDeskOperationPacket.class);
        registerMessage("research_effect", FHEffectProgressSyncPacket.class);
       // registerMessage("research_energy_data", FHEnergyDataSyncPacket.class);
        registerMessage("research_insight", FHInsightSyncPacket.class);
	}

}
