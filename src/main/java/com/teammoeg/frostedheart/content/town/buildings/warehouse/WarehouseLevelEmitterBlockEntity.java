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

package com.teammoeg.frostedheart.content.town.buildings.warehouse;

import com.teammoeg.chorda.block.entity.CBlockEntity;
import com.teammoeg.frostedheart.bootstrap.common.FHBlockEntityTypes;
import com.teammoeg.frostedheart.content.town.ITownWithResources;
import com.teammoeg.frostedheart.content.town.TeamTown;
import com.teammoeg.frostedheart.content.town.provider.TeamTownProvider;
import com.teammoeg.frostedheart.content.town.resource.TeamTownResourceActionExecutorHandler;
import com.teammoeg.frostedheart.content.town.resource.TeamTownResourceHolder;
import com.teammoeg.frostedheart.content.town.resource.action.TownResourceActions;
import com.teammoeg.frostedheart.content.town.resource.watcher.IWarehouseStockWatcher;
import com.teammoeg.frostedheart.content.town.resource.watcher.IWarehouseStockWatcherNode;
import com.teammoeg.frostedheart.content.town.transport.WarehouseTopologyListener;
import com.teammoeg.frostedheart.content.town.transport.WarehouseTopologySnapshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * 仓库发信器：归属于一个城镇，监测城镇仓库中某种物品（NBT 精确匹配）的存量，
 * 并按阈值与模式输出红石信号
 * <ul>
 *     <li>{@link WarehouseRedstoneMode#HIGH_SIGNAL}：存量大于等于阈值时输出 15 级信号</li>
 *     <li>{@link WarehouseRedstoneMode#LOW_SIGNAL}：存量低于阈值时输出 15 级信号</li>
 * </ul>
 * 与仓库接口的红石控制模式配合，即可实现"发信器控制的仓库输出"。
 * <p>
 * Warehouse level emitter: belongs to one town and watches the stock of one item
 * (exact NBT match) in the town warehouse, and emits a redstone signal according to a
 * configurable threshold and mode, mirroring the AE2 level emitter. Combined with the
 * warehouse interface's redstone control mode it enables level-emitter-controlled
 * warehouse output.
 */
public class WarehouseLevelEmitterBlockEntity extends CBlockEntity implements IWarehouseStockWatcherNode,
        MenuProvider, WarehouseTopologyListener {
    public static final int STATUS_UNBOUND = 0;
    public static final int STATUS_UNAVAILABLE = 1;
    public static final int STATUS_WORKING = 2;

    @Nullable
    private SimpleItemKey filter;
    private int threshold = 1;
    private WarehouseRedstoneMode mode = WarehouseRedstoneMode.HIGH_SIGNAL;
    private long lastKnownStock;
    private boolean emitterOn;

    private TeamTownProvider townProvider;
    private transient TeamTown topologyListenerTown;
    private int connectionStatus = STATUS_UNBOUND;
    // 由资源持有者分配的 watcher，用于精准订阅物品数量变化
    @Nullable
    private IWarehouseStockWatcher watcher;

    //构造器
    public WarehouseLevelEmitterBlockEntity(BlockPos pos, BlockState state) {
        super(FHBlockEntityTypes.WAREHOUSE_LEVEL_EMITTER.get(), pos, state);
    }

    // --- getters ---
    @Nullable
    public SimpleItemKey getFilter() {
        return filter;
    }

    public int getThreshold() {
        return threshold;
    }

    public WarehouseRedstoneMode getMode() {
        return mode;
    }

    public long getLastKnownStock() {
        return lastKnownStock;
    }

    public boolean isEmitterOn() {
        return emitterOn;
    }

    public int getConnectionStatus() {
        return connectionStatus;
    }

    TeamTownProvider getTownProvider() {
        return townProvider;
    }

    public boolean claimOrAuthorize(ServerPlayer player) {
        TownWarehouseDeviceAccess.ClaimResult result =
                TownWarehouseDeviceAccess.claimOrAuthorize(player, this, townProvider);
        if (!result.allowed()) {
            return false;
        }
        boolean claimed = townProvider == null;
        townProvider = result.provider();
        if (claimed) {
            connectionStatus = STATUS_UNAVAILABLE;
            setChanged();
        }
        registerTopologyListener();
        ensureWatcherAndRefresh();
        return true;
    }


    // ---------- IWarehouseStockWatcherNode 实现 ----------

    public void setFilter(@Nullable SimpleItemKey newFilter) {
        this.filter = newFilter;
        if (level != null) {
            setChanged();
            if (!level.isClientSide) {
                if (watcher != null) {
                    configureWatcher(); // 自动重新 add/remove
                }
                refreshState(); // 立即查一次库存更新红石
            }
        }
    }

    @Override
    public void updateWatcher(IWarehouseStockWatcher newWatcher) {
        this.watcher = newWatcher;
        configureWatcher();
    }

    @Override
    public void onStockChange(SimpleItemKey item, long newAmount) {
        if (level == null || level.isClientSide) return;
        if (resolveTown().map(town -> !town.getWarehouseTopology().isUsable()).orElse(true)) {
            setEmitterOn(false, lastKnownStock);
            return;
        }
        lastKnownStock = newAmount;
        boolean on = shouldEmit(mode, newAmount, threshold);
        setEmitterOn(on, newAmount);
    }

    private void configureWatcher() {
        WarehouseLevelEmitterModel.configureWatcher(watcher, filter);
    }
    public void setFilterFromStack(ItemStack stack) {
        if (!stack.isEmpty()) {
            setFilter(SimpleItemKey.from(stack));
        }
    }

    public void setThreshold(int newThreshold) {
        this.threshold = Math.max(1, newThreshold);
        if (level != null) {
            setChanged();
            if (!level.isClientSide) refreshState();
        }
    }

    public void cycleMode() {
        this.mode = this.mode.nextEmitterMode();
        if (level != null) {
            setChanged();
            if (!level.isClientSide) refreshState();
        }
    }

    private Optional<TeamTown> resolveTown() {
        return TownWarehouseDeviceAccess.resolveTown(townProvider, level);
    }

    private void registerTopologyListener() {
        if (level == null || level.isClientSide || townProvider == null) {
            return;
        }
        resolveTown().ifPresent(town -> {
            if (topologyListenerTown != null) {
                topologyListenerTown.unregisterWarehouseTopologyListener(
                        GlobalPos.of(level.dimension(), worldPosition), this);
            }
            town.registerWarehouseTopologyListener(
                    GlobalPos.of(level.dimension(), worldPosition), this);
            topologyListenerTown = town;
        });
    }

    private void unregisterTopologyListener() {
        if (topologyListenerTown != null && level != null) {
            topologyListenerTown.unregisterWarehouseTopologyListener(
                    GlobalPos.of(level.dimension(), worldPosition), this);
        }
        topologyListenerTown = null;
    }

    @Override
    public void onWarehouseTopologyChanged(WarehouseTopologySnapshot snapshot) {
        if (level == null || level.isClientSide) {
            return;
        }
        if (!snapshot.isUsable() || !level.dimension().equals(snapshot.townDimension())) {
            if (watcher != null) {
                watcher.reset();
                watcher = null;
            }
            connectionStatus = STATUS_UNAVAILABLE;
            setEmitterOn(false, 0);
            return;
        }
        ensureWatcherAndRefresh();
        refreshState();
    }

    private void clearBinding() {
        unregisterTopologyListener();
        // 释放 watcher，它会自动从资源持有者的索引中清除
        if (watcher != null) {
            watcher.reset();
            watcher = null;
        }

        boolean changed = townProvider != null;
        townProvider = null;
        connectionStatus = STATUS_UNBOUND;
        if (changed && level != null) {
            setChanged();
        }
    }

    /**
     * 与仓库建立新的 Watcher 订阅，并且刷新一次当前库存状态。
     */
    public void ensureWatcherAndRefresh() {
        if (watcher != null || level == null || level.isClientSide) return;

        Optional<TeamTown> binding = resolveTown();
        if (binding.isEmpty()) {
            connectionStatus = townProvider == null ? STATUS_UNBOUND : STATUS_UNAVAILABLE;
            setEmitterOn(false, 0);
            return;
        }
        TeamTown teamTown = binding.get();
        if (!teamTown.getWarehouseTopology().isUsable()) {
            connectionStatus = STATUS_UNAVAILABLE;
            setEmitterOn(false, 0);
            return;
        }
        ITownWithResources resourceTown = teamTown;

        TeamTownResourceHolder holder = ((TeamTownResourceActionExecutorHandler) resourceTown.getActionExecutorHandler()).resourceHolder;

        this.watcher = holder.createWatcher(this); // 会回调 updateWatcher

        connectionStatus = STATUS_WORKING;
        refreshState(); // 主动拉取一次当前库存
    }

    // ---------- 状态刷新（仅用于配置变更或主动查询） ----------

    private void refreshState() {
        if (level == null || level.isClientSide) return;

        Optional<TeamTown> binding = resolveTown();
        if (binding.isEmpty()) {
            connectionStatus = townProvider == null ? STATUS_UNBOUND : STATUS_UNAVAILABLE;
            setEmitterOn(false, 0);
            return;
        }
        TeamTown teamTown = binding.get();
        if (!teamTown.getWarehouseTopology().isUsable()) {
            connectionStatus = STATUS_UNAVAILABLE;
            setEmitterOn(false, 0);
            return;
        }
        ITownWithResources resourceTown = teamTown;

        connectionStatus = STATUS_WORKING;
        if (filter == null) {
            setEmitterOn(false, 0);
            return;
        }

        long stock = (long) TownResourceActions.get(resourceTown.getActionExecutorHandler(), filter.toStack(1));
        boolean on = shouldEmit(mode, stock, threshold);
        setEmitterOn(on, stock);
    }

    static boolean shouldEmit(WarehouseRedstoneMode mode, long stock, int threshold) {
        return WarehouseLevelEmitterModel.shouldEmit(mode, stock, threshold);
    }

    private void setEmitterOn(boolean on, long stock) {
        WarehouseLevelEmitterModel.StateChange change =
                WarehouseLevelEmitterModel.compareState(
                        lastKnownStock, emitterOn, stock, on);
        if (!change.changed()) {
            return;
        }
        this.lastKnownStock = stock;
        this.emitterOn = on;
        setChanged();
        if (change.outputChanged() && level != null) {
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
            Direction facing = getBlockState().getValue(WarehouseLevelEmitterBlock.FACING);
            level.updateNeighborsAt(worldPosition.relative(facing), getBlockState().getBlock());
        }
    }

    // ---------- 生命周期 ----------

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            registerTopologyListener();
            // 方块加载时（无论是首次放置还是 chunk 重载），重新连接 watcher
            // 城镇资源常驻，Watcher 机制会自动同步最新库存
            ensureWatcherAndRefresh();
        }
    }

    @Override
    public void onRemoved() {
        if (level != null && !level.isClientSide) {
            unregisterTopologyListener();
            if (level.getBlockState(worldPosition).getBlock() instanceof WarehouseLevelEmitterBlock) {
                // 区块卸载：仅释放 Watcher，保留绑定，onLoad 会重建
                if (watcher != null) {
                    watcher.reset();
                    watcher = null;
                }
            } else {
                // 方块被破坏或替换：彻底清理
                if (watcher != null) {
                    watcher.reset();
                    watcher = null;
                }
                townProvider = null;
                connectionStatus = STATUS_UNBOUND;
                setEmitterOn(false, 0);
            }
        }
        super.onRemoved();
    }


    // ---------- 序列化 ----------

    @Override
    public void readCustomNBT(CompoundTag nbt, boolean descPacket) {
        WarehouseLevelEmitterPersistence.State state =
                WarehouseLevelEmitterPersistence.read(nbt);
        filter = state.filter();
        threshold = state.threshold();
        mode = state.mode();
        lastKnownStock = state.lastKnownStock();
        emitterOn = state.emitterOn();
        townProvider = state.townProvider();
        connectionStatus = townProvider == null ? STATUS_UNBOUND : STATUS_UNAVAILABLE;
    }

    @Override
    public void writeCustomNBT(CompoundTag nbt, boolean descPacket) {
        WarehouseLevelEmitterPersistence.write(nbt,
                new WarehouseLevelEmitterPersistence.State(
                        filter, threshold, mode, lastKnownStock, emitterOn, townProvider));
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new WarehouseLevelEmitterMenu(id, playerInventory, this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.frostedheart.warehouse_level_emitter");
    }

}
