package com.teammoeg.frostedheart.content.town.client;

import java.util.Optional;
import java.util.Set;
import com.teammoeg.frostedheart.content.town.event.ITownDataUpdateListener;
import java.util.UUID;
import java.util.function.Consumer;
import com.teammoeg.chorda.dataholders.team.CClientTeamDataManager;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.bootstrap.common.FHSpecialDataTypes;
import com.teammoeg.frostedheart.content.town.TeamTownData;
import com.teammoeg.frostedheart.content.town.network.TownSnapshotRequestPacket;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Connection-local town view. Its server identity is independent of the client holder ID. */
@Mod.EventBusSubscriber(modid = FHMain.MODID, value = Dist.CLIENT)
public final class TownClientUpdates {
    private static UUID holderId;
    private static boolean ready;
    private static boolean pendingRequest;
    private static int ticksUntilRequest;
    private static int retryTicks = 20;
    private TownClientUpdates() {}

    /** A queued packet from a disconnected world must not install into a new one. */
    public static boolean isCurrentConnection(net.minecraft.network.Connection origin) {
        var connection = net.minecraft.client.Minecraft.getInstance().getConnection();
        return connection != null && connection.getConnection() == origin;
    }

    public static boolean canApply(net.minecraft.network.Connection origin) {
        return isCurrentConnection(origin) && ready && currentTown().isPresent();
    }

    public static Optional<TeamTownData> currentTown() {
        return ready ? CClientTeamDataManager.INSTANCE.getInstance().getOptional(FHSpecialDataTypes.TOWN_DATA)
                : Optional.empty();
    }

    public static void pending(UUID serverHolderId) {
        holderId = serverHolderId;
        ready = false;
        pendingRequest = true;
        ticksUntilRequest = 20;
        CClientTeamDataManager.INSTANCE.getInstance().removeData(FHSpecialDataTypes.TOWN_DATA);
        com.teammoeg.frostedheart.content.town.observation.TownOperationalStatusClientCache.reset();
        TeamTownData.fireClientDataChanged();
    }

    public static void install(UUID serverHolderId, TeamTownData candidate) {
        if (!java.util.Objects.equals(holderId, serverHolderId) || candidate == null) {
            com.teammoeg.frostedheart.content.town.observation.TownOperationalStatusClientCache.reset();
        }
        holderId = serverHolderId;
        var holder = CClientTeamDataManager.INSTANCE.getInstance();
        if (candidate == null) holder.removeData(FHSpecialDataTypes.TOWN_DATA);
        else holder.setData(FHSpecialDataTypes.TOWN_DATA, candidate);
        ready = true;
        pendingRequest = false;
        retryTicks = 20;
        TeamTownData.fireClientDataChanged();
    }

    public static void failed(Exception failure) {
        FHMain.LOGGER.error("Town view needs a new full snapshot", failure);
        pending(holderId);
        ticksUntilRequest = 0;
    }

    public static void apply(Consumer<TeamTownData> update) {
        if (!ready) return;
        try { currentTown().ifPresent(update); }
        catch (RuntimeException failure) { failed(failure); }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !pendingRequest) return;
        if (net.minecraft.client.Minecraft.getInstance().getConnection() == null) return;
        if (--ticksUntilRequest > 0) return;
        FHNetwork.INSTANCE.sendToServer(new TownSnapshotRequestPacket());
        ticksUntilRequest = retryTicks;
        retryTicks = Math.min(200, retryTicks * 2);
    }

    @SubscribeEvent
    public static void disconnected(ClientPlayerNetworkEvent.LoggingOut event) {
        ready = false;
        holderId = null;
        pendingRequest = false;
        retryTicks = 20;
        CClientTeamDataManager.INSTANCE.getInstance().removeData(FHSpecialDataTypes.TOWN_DATA);
        com.teammoeg.frostedheart.content.town.observation.TownOperationalStatusClientCache.reset();
        TeamTownData.fireClientDataChanged();
    }

    private static final Set<ITownDataUpdateListener> clientListeners = java.util.concurrent.ConcurrentHashMap.newKeySet();

    public static void addClientListener(ITownDataUpdateListener listener) {
        clientListeners.add(listener);
    }

    /**
     * 移除一个客户端 GUI 监听器（GUI 关闭时调用）。
     * <p>Remove a client-side GUI listener (called when the GUI closes).</p>
     */
    public static void removeClientListener(ITownDataUpdateListener listener) {
        clientListeners.remove(listener);
    }

    /**
     * 全量同步包批内 fire 标记 + 单调递增批次号：批内连续回调（建筑→居民→资源）之间
     * 数据不变（实例替换在 fireClientDataChanged 之前完成），监听器可据此对同一批的
     * 重复回调去重一次；批号供监听器区分「当前批已求值」与「新一轮批」（纯布尔在
     * 连续两次全量包之间无增量回调时会残留为真，跳过新一轮求值）。
     * 仅客户端主线程访问（全量包 enqueueWork / 增量包 handler），无需并发保护。
     */
    private static boolean inClientBatchFire = false;
    private static long clientSyncBatchId = 0;

    public static boolean isInClientBatchFire() {
        return inClientBatchFire;
    }

    public static long getClientSyncBatchId() {
        return clientSyncBatchId;
    }

    /**
     * 通知所有已注册 GUI：建筑、居民、资源、历史和岗位计划均可能已变化。
     * 由全量同步包在替换实例后调用，
     * 保证 GUI 打开瞬间收到的最新全量数据能立即刷新到界面。
     * <p>
     * Notify all registered GUIs that any synchronized town category may have changed.
     * Called by the full-sync packet after the instance is replaced, so the freshest
     * full snapshot is reflected immediately.
     * </p>
     */
    public static void fireClientDataChanged() {
        inClientBatchFire = true;
        clientSyncBatchId++;
        try {
            fireBuildingsChanged();
            fireResidentsChanged();
            fireResourcesChanged();
            fireHistoryChanged();
            fireStaffingChanged();
            fireHousingChanged();
            firePoliciesChanged();
        } finally {
            inClientBatchFire = false;
        }
    }

    public static void fireBuildingsChanged() {
        for (ITownDataUpdateListener listener : clientListeners) {
            listener.onBuildingsChanged();
        }
    }

    public static void fireResidentsChanged() {
        for (ITownDataUpdateListener listener : clientListeners) {
            listener.onResidentsChanged();
        }
    }

    public static void fireResourcesChanged() {
        for (ITownDataUpdateListener listener : clientListeners) {
            listener.onResourcesChanged();
        }
    }

    public static void fireHistoryChanged() {
        for (ITownDataUpdateListener listener : clientListeners) {
            listener.onHistoryChanged();
        }
    }

    /**
     * 通知客户端监听器重新读取权威岗位计划。岗位面板在渲染阶段读取最新数据，因此
     * 回调无需重建整个页面，也不会丢失滚动或拖拽以外的临时 UI 状态。
     */
    public static void fireStaffingChanged() {
        for (ITownDataUpdateListener listener : clientListeners) {
            listener.onStaffingChanged();
        }
    }

    public static void fireHousingChanged() {
        for (ITownDataUpdateListener listener : clientListeners) {
            listener.onHousingChanged();
        }
    }

    public static void firePoliciesChanged() {
        for (ITownDataUpdateListener listener : clientListeners) {
            listener.onPoliciesChanged();
        }
    }


}
