package com.teammoeg.frostedheart.content.town;

import java.util.HashMap;
import java.util.Map;
import com.teammoeg.chorda.dataholders.team.TeamDataHolder;
import com.teammoeg.frostedheart.content.town.citizen.sim.TownSimData;
import com.teammoeg.frostedheart.content.town.transport.WarehouseTopologyListener;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/** One current town's references and transient progress; no duplicate economic facts. */
final class TownRuntime {
    final TeamDataHolder holder;
    final TeamTownData data;
    final Map<GlobalPos, WarehouseTopologyListener> pendingDevices = new HashMap<>();
    TownSimData sim;
    ResourceKey<Level> dimension;
    Long observedWindow;
    boolean binding;
    long nextRepairTick;
    int repairDelay = 20;

    TownRuntime(TeamDataHolder holder, TeamTownData data) {
        this.holder = holder;
        this.data = data;
    }
}
