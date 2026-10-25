package com.teammoeg.frostedheart.content.town.event;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.town.TownRuntimeManager;
import com.teammoeg.frostedheart.content.town.citizen.sync.SyncEngine;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.teammoeg.frostedheart.content.town.building.TownBuildingTypes;
import com.teammoeg.frostedheart.bootstrap.common.FHSpecialDataTypes;
import com.teammoeg.chorda.dataholders.team.CTeamDataManager;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;

/** One explicit server-END ordering for town commits, views and citizen visibility. */
@Mod.EventBusSubscriber(modid = FHMain.MODID)
public final class TownServerEvents {
    private TownServerEvents() {}

    @SubscribeEvent
    public static void resourcesReloaded(OnDatapackSyncEvent event) {
        if (event.getPlayer() != null) {
            return;
        }
        TownBuildingTypes.resourcesReloaded();
        com.teammoeg.frostedheart.content.town.buildings.mine.MineBuilding.invalidateRecipeCache();
        var teams = CTeamDataManager.INSTANCE;
        if (teams == null) {
            return;
        }
        teams.forAllData(FHSpecialDataTypes.TOWN_DATA, (town, holder) -> {
            Runnable repair = () -> {
                town.getBuildingStore().retryPending();
                TownRuntimeManager manager = TownRuntimeManager.current();
                if (manager != null) {
                    manager.install(holder);
                }
            };
            TownRuntimeManager manager = TownRuntimeManager.current();
            if (manager == null || !manager.deferIfBusy(holder, repair)) {
                repair.run();
            }
        });
    }

    @SubscribeEvent
    public static void serverStopped(ServerStoppedEvent event) {
        com.teammoeg.frostedheart.content.town.buildings.mine.MineBuilding.invalidateRecipeCache();
    }
    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        TownRuntimeManager manager = TownRuntimeManager.current();
        if (manager != null) manager.tickEnd();
        SyncEngine.refreshServerVisibility(event.getServer());
    }
}
