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

import com.teammoeg.chorda.menu.CBlockEntityMenu;
import com.teammoeg.chorda.menu.CCustomMenuSlot;
import com.teammoeg.chorda.menu.CCustomMenuSlot.CDataSlot;
import com.teammoeg.frostedheart.bootstrap.common.FHMenuSlots;
import com.teammoeg.frostedheart.bootstrap.common.FHMenuTypes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * 仓库发信器菜单：同步过滤物品、阈值、模式、连接状态、当前存量与输出状态，
 * 并处理来自界面的配置命令。
 * <p>
 * Warehouse level emitter menu: syncs the filter item, threshold, mode, connection
 * status, current stock, and output state, and handles configuration commands from
 * the screen.
 */
public class WarehouseLevelEmitterMenu extends CBlockEntityMenu<WarehouseLevelEmitterBlockEntity> {
    private static final int CMD_SET_FILTER = 0;
    private static final int CMD_CLEAR_FILTER = 1;
    private static final int CMD_SET_THRESHOLD = 2;
    private static final int CMD_CYCLE_MODE = 3;

    private final CDataSlot<SimpleItemKey> filterData;
    private final CDataSlot<Integer> thresholdData;
    private final CDataSlot<Integer> modeData;
    private final CDataSlot<Integer> connectionStatus;
    private final CDataSlot<Integer> stockData;
    private final CDataSlot<Integer> emitterState;
    private final Player menuPlayer;

    public WarehouseLevelEmitterMenu(int id, Inventory playerInventory, WarehouseLevelEmitterBlockEntity blockEntity) {
        super(FHMenuTypes.WAREHOUSE_LEVEL_EMITTER.get(), blockEntity, id, playerInventory.player, 0);
        menuPlayer = playerInventory.player;

        boolean serverSide = !playerInventory.player.level().isClientSide;
        filterData = FHMenuSlots.SIMPLE_ITEM_KEY_ENCODER_SLOT.create(this);
        thresholdData = CCustomMenuSlot.SLOT_INT.create(this);
        modeData = CCustomMenuSlot.SLOT_INT.create(this);
        connectionStatus = CCustomMenuSlot.SLOT_INT.create(this);
        stockData = CCustomMenuSlot.SLOT_INT.create(this);
        emitterState = CCustomMenuSlot.SLOT_INT.create(this);
        if (serverSide) {
            filterData.bind(blockEntity::getFilter);
            thresholdData.bind(blockEntity::getThreshold);
            modeData.bind(() -> blockEntity.getMode().ordinal());
            connectionStatus.bind(blockEntity::getConnectionStatus);
            stockData.bind(() -> WarehouseLevelEmitterModel.stockForMenu(
                    blockEntity.getLastKnownStock()));
            emitterState.bind(() -> blockEntity.isEmitterOn() ? 1 : 0);
        }

        addPlayerInventory(playerInventory, 8, 84, 142);
    }

    @Nullable
    public SimpleItemKey getFilter() {
        return filterData.getValue();
    }

    public int getThreshold() {
        Integer value = thresholdData.getValue();
        return value == null ? 1 : Math.max(1, value);
    }

    public WarehouseRedstoneMode getMode() {
        Integer value = modeData.getValue();
        return WarehouseRedstoneMode.byOrdinal(value == null ? 0 : value, WarehouseRedstoneMode.HIGH_SIGNAL);
    }

    public int getConnectionStatus() {
        Integer value = connectionStatus.getValue();
        return value == null ? WarehouseLevelEmitterBlockEntity.STATUS_UNBOUND : value;
    }

    public int getCurrentStock() {
        Integer value = stockData.getValue();
        return value == null ? 0 : value;
    }

    public boolean isEmitterOn() {
        Integer value = emitterState.getValue();
        return value != null && value != 0;
    }

    public void setFilterFromCarried() {
        sendMessage(CMD_SET_FILTER, 0);
    }

    public void clearFilter() {
        sendMessage(CMD_CLEAR_FILTER, 0);
    }

    public void setThreshold(int threshold) {
        sendMessage(CMD_SET_THRESHOLD, threshold);
    }

    public void cycleMode() {
        sendMessage(CMD_CYCLE_MODE, 0);
    }

    @Override
    public void receiveMessage(short buttonId, int state) {
        if (!isCommandContextValid()) {
            return;
        }
        switch (buttonId) {
            case CMD_SET_FILTER -> {
                if (!getCarried().isEmpty()) {
                    blockEntity.setFilterFromStack(getCarried());
                }
            }
            case CMD_CLEAR_FILTER -> blockEntity.setFilter(null);
            case CMD_SET_THRESHOLD -> {
                if (state >= 1) {
                    blockEntity.setThreshold(state);
                }
            }
            case CMD_CYCLE_MODE -> blockEntity.cycleMode();
            default -> {
            }
        }
    }

    private boolean isCommandContextValid() {
        return TownWarehouseDeviceAccess.isMenuAccessValid(
                menuPlayer, blockEntity, blockEntity.getTownProvider(), this);
    }

    @Override
    public boolean stillValid(Player player) {
        return player == menuPlayer && isCommandContextValid();
    }
}
