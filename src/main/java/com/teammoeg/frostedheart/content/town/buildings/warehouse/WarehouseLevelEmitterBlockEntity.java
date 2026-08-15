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
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.bootstrap.common.FHBlockEntityTypes;
import com.teammoeg.frostedheart.content.town.ITown;
import com.teammoeg.frostedheart.content.town.ITownWithBuildings;
import com.teammoeg.frostedheart.content.town.ITownWithResources;
import com.teammoeg.frostedheart.content.town.building.AbstractTownBuilding;
import com.teammoeg.frostedheart.content.town.provider.ITownProviderSerializable;
import com.teammoeg.frostedheart.content.town.resource.TeamTownResourceActionExecutorHandler;
import com.teammoeg.frostedheart.content.town.resource.TeamTownResourceHolder;
import com.teammoeg.frostedheart.content.town.resource.action.TownResourceActions;
import com.teammoeg.frostedheart.content.town.resource.watcher.IWarehouseStockWatcher;
import com.teammoeg.frostedheart.content.town.resource.watcher.IWarehouseStockWatcherNode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

/**
 * 仓库发信器：绑定到仓库墙体，监测城镇仓库中某种物品（NBT 精确匹配）的存量，
 * 并按阈值与模式输出红石信号
 * <ul>
 *     <li>{@link WarehouseRedstoneMode#HIGH_SIGNAL}：存量大于等于阈值时输出 15 级信号</li>
 *     <li>{@link WarehouseRedstoneMode#LOW_SIGNAL}：存量低于阈值时输出 15 级信号</li>
 * </ul>
 * 与仓库接口的红石控制模式配合，即可实现"发信器控制的仓库输出"。
 * <p>
 * Warehouse level emitter: binds to a warehouse wall, watches the stock of one item
 * (exact NBT match) in the town warehouse, and emits a redstone signal according to a
 * configurable threshold and mode, mirroring the AE2 level emitter. Combined with the
 * warehouse interface's redstone control mode it enables level-emitter-controlled
 * warehouse output.
 */
public class WarehouseLevelEmitterBlockEntity extends CBlockEntity implements IWarehouseStockWatcherNode, MenuProvider {
    public static final int STATUS_UNBOUND = 0;
    public static final int STATUS_UNAVAILABLE = 1;
    public static final int STATUS_WORKING = 2;

    @Nullable
    private SimpleItemKey filter;
    private int threshold = 1;
    private WarehouseRedstoneMode mode = WarehouseRedstoneMode.HIGH_SIGNAL;
    private long lastKnownStock;
    private boolean emitterOn;

    private ITownProviderSerializable<? extends ITownWithBuildings> townProvider;
    private BlockPos warehousePos;
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
        if (resolveBinding(false).map(ctx -> !ctx.warehouse().isBuildingWorkable()).orElse(true)) {
            // 建筑无效，强制关闭
            setEmitterOn(false, lastKnownStock);
            return;
        }
        lastKnownStock = newAmount;
        boolean on = (mode == WarehouseRedstoneMode.LOW_SIGNAL) == (newAmount < threshold);
        setEmitterOn(on, newAmount);
    }

    private void configureWatcher() {
        if (watcher == null) return;
        watcher.reset();
        if (filter != null) {
            watcher.addWatch(filter);
        }
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

    // ---------- 绑定管理 ----------

    /**
     * Claims this emitter for a warehouse. A still-valid binding owned by a
     * different warehouse is never stolen.
     */
    public boolean tryBind(ITownProviderSerializable<? extends ITownWithBuildings> provider, BlockPos newWarehousePos) {
        if (provider == null || newWarehousePos == null) {
            return false;
        }
        if (isBoundTo(provider, newWarehousePos)) {
            this.townProvider = provider;
            return true;
        }
        if (warehousePos != null && resolveBinding(false).isPresent()) {
            // 旧绑定仍有效，不抢夺
            return false;
        }

        // 清理旧绑定（包括旧的 watcher）
        clearBinding();

        this.townProvider = provider;
        this.warehousePos = newWarehousePos.immutable();
        this.connectionStatus = STATUS_UNAVAILABLE;
        if (level != null) {
            setChanged();
        }

        // 获取新的 watcher 并配置
        ensureWatcherAndRefresh();
        return true;
    }

    public void unbindIfBoundTo(ITownProviderSerializable<? extends ITownWithBuildings> provider, BlockPos oldWarehousePos) {
        if (isBoundTo(provider, oldWarehousePos)) {
            clearBinding();
        }
    }

    void unbindIfBoundTo(WarehouseBuilding warehouse) {
        if (warehousePos == null || !warehousePos.equals(warehouse.getPos()) || townProvider == null) {
            return;
        }
        ITownWithBuildings town = townProvider.getTown();
        if (town != null && town.getTownBuilding(warehousePos).orElse(null) == warehouse) {
            clearBinding();
        }
    }

    private boolean isBoundTo(ITownProviderSerializable<? extends ITownWithBuildings> provider, BlockPos candidatePos) {
        return warehousePos != null
                && warehousePos.equals(candidatePos)
                && townProvider != null
                && Objects.equals(townProvider.toNBT(), provider.toNBT());
    }

    private Optional<BindingContext> resolveBinding(boolean clearWhenInvalid) {
        if (townProvider == null || warehousePos == null) {
            return Optional.empty();
        }

        ITownWithBuildings town = townProvider.getTown();
        if (town == null) {
            return invalidBinding(clearWhenInvalid);
        }
        Optional<AbstractTownBuilding> building = town.getTownBuilding(warehousePos);
        if (building.isEmpty() || !(building.get() instanceof WarehouseBuilding warehouse)
                || !warehouse.containsEmitter(worldPosition)) {
            return invalidBinding(clearWhenInvalid);
        }

        if (level != null && level.isLoaded(warehousePos)) {
            BlockEntity core = level.getBlockEntity(warehousePos);
            if (!(core instanceof WarehouseBlockEntity)) {
                return invalidBinding(clearWhenInvalid);
            }
        }
        return Optional.of(new BindingContext(town, warehouse));
    }

    private Optional<BindingContext> invalidBinding(boolean clearWhenInvalid) {
        if (clearWhenInvalid) {
            clearBinding();
        }
        return Optional.empty();
    }

    private void clearBinding() {
        // 释放 watcher，它会自动从资源持有者的索引中清除
        if (watcher != null) {
            watcher.reset();
            watcher = null;
        }

        boolean changed = townProvider != null || warehousePos != null;
        townProvider = null;
        warehousePos = null;
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

        Optional<BindingContext> binding = resolveBinding(false);
        if (binding.isEmpty()) {
            connectionStatus = STATUS_UNBOUND;
            setEmitterOn(false, 0);
            return;
        }

        BindingContext ctx = binding.get();
        if (!(ctx.town() instanceof ITownWithResources resourceTown)) {
            connectionStatus = STATUS_UNAVAILABLE;
            setEmitterOn(false, 0);
            return;
        }

        TeamTownResourceHolder holder = ((TeamTownResourceActionExecutorHandler) resourceTown.getActionExecutorHandler()).resourceHolder;

        this.watcher = holder.createWatcher(this); // 会回调 updateWatcher

        connectionStatus = STATUS_WORKING;
        refreshState(); // 主动拉取一次当前库存
    }

    // ---------- 状态刷新（仅用于配置变更或主动查询） ----------

    private void refreshState() {
        if (level == null || level.isClientSide) return;

        Optional<BindingContext> binding = resolveBinding(true);
        if (binding.isEmpty()) {
            connectionStatus = STATUS_UNBOUND;
            setEmitterOn(false, 0);
            return;
        }

        BindingContext ctx = binding.get();
        if (!(ctx.town() instanceof ITownWithResources resourceTown)
                || !ctx.warehouse().isBuildingWorkable()) {
            connectionStatus = STATUS_UNAVAILABLE;
            setEmitterOn(false, 0);
            return;
        }

        connectionStatus = STATUS_WORKING;
        if (filter == null) {
            setEmitterOn(false, 0);
            return;
        }

        long stock = (long) TownResourceActions.get(resourceTown.getActionExecutorHandler(), filter.toStack(1));
        boolean on = (mode == WarehouseRedstoneMode.LOW_SIGNAL) == (stock < threshold);
        setEmitterOn(on, stock);
    }

    private void setEmitterOn(boolean on, long stock) {
        this.lastKnownStock = stock;
        if (on == this.emitterOn) {
            return;
        }
        this.emitterOn = on;
        setChanged();
        if (level != null) {
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
            // 方块加载时（无论是首次放置还是 chunk 重载），重新连接 watcher
            // 城镇资源常驻，Watcher 机制会自动同步最新库存
            ensureWatcherAndRefresh();
        }
    }

    @Override
    public void onRemoved() {
        if (level != null && !level.isClientSide) {
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
                resolveBinding(false).ifPresent(context -> context.warehouse().removeEmitter(worldPosition));
                townProvider = null;
                warehousePos = null;
                connectionStatus = STATUS_UNBOUND;
            }
        }
        super.onRemoved();
    }


    // ---------- 序列化 ----------

    @Override
    public void readCustomNBT(CompoundTag nbt, boolean descPacket) {
        filter = null;
        if (nbt.contains("filter")) {
            SimpleItemKey.CODEC.parse(NbtOps.INSTANCE, nbt.get("filter"))
                    .resultOrPartial(message -> FHMain.LOGGER.warn("Failed to read warehouse level emitter filter: {}", message))
                    .ifPresent(parsed -> filter = parsed);
        }
        threshold = Math.max(1, nbt.getInt("threshold"));
        mode = WarehouseRedstoneMode.byOrdinal(nbt.getInt("redstoneMode"), WarehouseRedstoneMode.HIGH_SIGNAL);
        if (mode == WarehouseRedstoneMode.IGNORE) {
            mode = WarehouseRedstoneMode.HIGH_SIGNAL;
        }
        lastKnownStock = nbt.getLong("lastKnownStock");
        emitterOn = nbt.getBoolean("emitterOn");

        townProvider = null;
        warehousePos = null;
        if (nbt.contains("townProvider") && nbt.contains("warehousePos")) {
            ITownProviderSerializable<? extends ITown> rawProvider =
                    ITownProviderSerializable.fromNBT(nbt.getCompound("townProvider"));
            if (rawProvider != null && ITownWithBuildings.class.isAssignableFrom(rawProvider.getTownType())) {
                townProvider = castTownProvider(rawProvider);
                warehousePos = BlockPos.of(nbt.getLong("warehousePos"));
            }
        }
        connectionStatus = warehousePos == null ? STATUS_UNBOUND : STATUS_UNAVAILABLE;
    }

    @SuppressWarnings("unchecked")
    private static ITownProviderSerializable<? extends ITownWithBuildings> castTownProvider(
            ITownProviderSerializable<? extends ITown> provider) {
        return (ITownProviderSerializable<? extends ITownWithBuildings>) provider;
    }

    @Override
    public void writeCustomNBT(CompoundTag nbt, boolean descPacket) {
        if (filter != null) {
            SimpleItemKey.CODEC.encodeStart(NbtOps.INSTANCE, filter)
                    .resultOrPartial(message -> FHMain.LOGGER.warn("Failed to write warehouse level emitter filter: {}", message))
                    .ifPresent(encoded -> nbt.put("filter", encoded));
        }
        nbt.putInt("threshold", threshold);
        nbt.putInt("redstoneMode", mode.ordinal());
        nbt.putLong("lastKnownStock", lastKnownStock);
        nbt.putBoolean("emitterOn", emitterOn);

        if (townProvider != null && warehousePos != null) {
            nbt.put("townProvider", townProvider.toNBT());
            nbt.putLong("warehousePos", warehousePos.asLong());
        }
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new WarehouseLevelEmitterMenu(id, playerInventory, this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.frostedheart.warehouse_level_emitter");
    }

    private record BindingContext(ITownWithBuildings town, WarehouseBuilding warehouse) {
    }
}
