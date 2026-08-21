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

import com.teammoeg.chorda.client.cui.menu.CUISlotItemHandler;
import com.teammoeg.chorda.menu.CBlockEntityMenu;
import com.teammoeg.chorda.menu.CCustomMenuSlot;
import com.teammoeg.chorda.menu.CCustomMenuSlot.CDataSlot;
import com.teammoeg.frostedheart.bootstrap.common.FHMenuSlots;
import com.teammoeg.frostedheart.bootstrap.common.FHMenuTypes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

public class WarehouseInterfaceMenu extends CBlockEntityMenu<WarehouseInterfaceBlockEntity> {
    static final int SCREEN_HEIGHT = 218;
    static final int TARGET_FILTER_Y = 42;
    static final int INTERFACE_SLOT_Y = 62;
    static final int PLAYER_INVENTORY_Y = 136;
    static final int PLAYER_HOTBAR_Y = 194;

    private static final int CMD_SET_TARGET = 0;
    private static final int CMD_CLEAR_TARGET = 1;
    private static final int CMD_SET_AMOUNT = 2;
    private static final int CMD_CYCLE_REDSTONE_MODE = 3;
    private static final int CMD_SET_TRANSPORT_RATE = 4;

    private final List<CDataSlot<WarehouseInterfaceTarget>> targetData = new ArrayList<>();
    private final CDataSlot<Integer> connectionStatus;
    private final CDataSlot<Integer> redstoneMode;
    private final CDataSlot<WarehouseInterfaceTransportView> transportView;
    private final Player menuPlayer;

    public WarehouseInterfaceMenu(int id, Inventory playerInventory, WarehouseInterfaceBlockEntity blockEntity) {
        super(FHMenuTypes.WAREHOUSE_INTERFACE.get(), blockEntity, id, playerInventory.player,
                WarehouseInterfaceBlockEntity.SLOT_COUNT);
        menuPlayer = playerInventory.player;

        for (int slot = 0; slot < WarehouseInterfaceBlockEntity.SLOT_COUNT; slot++) {
            addSlot(new CUISlotItemHandler(blockEntity.getInventory(), slot, 8 + slot * 18,
                    INTERFACE_SLOT_Y));
        }

        boolean serverSide = !playerInventory.player.level().isClientSide;
        for (int slot = 0; slot < WarehouseInterfaceBlockEntity.SLOT_COUNT; slot++) {
            CDataSlot<WarehouseInterfaceTarget> dataSlot =
                    FHMenuSlots.WAREHOUSE_INTERFACE_TARGET_ENCODER_SLOT.create(this);
            if (serverSide) {
                int targetSlot = slot;
                dataSlot.bind(() -> blockEntity.getTarget(targetSlot));
            }
            targetData.add(dataSlot);
        }

        connectionStatus = CCustomMenuSlot.SLOT_INT.create(this);
        redstoneMode = CCustomMenuSlot.SLOT_INT.create(this);
        transportView = FHMenuSlots.WAREHOUSE_INTERFACE_TRANSPORT_VIEW_ENCODER_SLOT.create(this);
        if (serverSide) {
            connectionStatus.bind(blockEntity::getConnectionStatus);
            redstoneMode.bind(() -> blockEntity.getRedstoneMode().ordinal());
            transportView.bind(blockEntity::getTransportView);
        }

        addPlayerInventory(playerInventory, 8,
                PLAYER_INVENTORY_Y,
                PLAYER_HOTBAR_Y);
    }

    public WarehouseInterfaceTarget getTarget(int slot) {
        if (slot < 0 || slot >= targetData.size()) {
            return null;
        }
        return targetData.get(slot).getValue();
    }

    public int getConnectionStatus() {
        return connectionStatus.getValue();
    }

    public WarehouseRedstoneMode getRedstoneMode() {
        Integer value = redstoneMode.getValue();
        return WarehouseRedstoneMode.byOrdinal(value == null ? 0 : value, WarehouseRedstoneMode.IGNORE);
    }

    public WarehouseInterfaceTransportView getTransportView() {
        WarehouseInterfaceTransportView value = transportView.getValue();
        return value == null ? WarehouseInterfaceTransportView.EMPTY : value;
    }

    public void setTargetFromCarried(int slot) {
        sendTargetCommand(CMD_SET_TARGET, slot, 0);
    }

    public void clearTarget(int slot) {
        sendTargetCommand(CMD_CLEAR_TARGET, slot, 0);
    }

    public void setTargetAmount(int slot, int amount) {
        sendTargetCommand(CMD_SET_AMOUNT, slot, amount);
    }

    public void cycleRedstoneMode() {
        sendTargetCommand(CMD_CYCLE_REDSTONE_MODE, 0, 0);
    }

    public void setTransportRate(int rateItemsPerSecond) {
        sendTargetCommand(CMD_SET_TRANSPORT_RATE, 0, rateItemsPerSecond);
    }

    private void sendTargetCommand(int command, int slot, int value) {
        sendMessage(((command & 0xff) << 8) | (slot & 0xff), value);
    }

    @Override
    public void receiveMessage(short buttonId, int state) {
        if (!isCommandContextValid()) {
            return;
        }
        int command = (buttonId & 0xff00) >> 8;
        int slot = buttonId & 0xff;
        if (command == CMD_SET_TRANSPORT_RATE) {
            blockEntity.setTransportRate(menuPlayer, state);
            return;
        }
        if (command == CMD_CYCLE_REDSTONE_MODE) {
            blockEntity.cycleRedstoneMode();
            return;
        }
        if (slot < 0 || slot >= WarehouseInterfaceBlockEntity.SLOT_COUNT) {
            return;
        }
        switch (command) {
            case CMD_SET_TARGET -> {
                if (!getCarried().isEmpty()) {
                    blockEntity.setTargetFromStack(slot, getCarried());
                }
            }
            case CMD_CLEAR_TARGET -> blockEntity.setTarget(slot, null);
            case CMD_SET_AMOUNT -> blockEntity.setTargetAmount(slot, state);
            default -> {
                return;
            }
        }
    }

    private boolean isCommandContextValid() {
        return menuPlayer != null
                && !menuPlayer.level().isClientSide
                && blockEntity.getLevel() == menuPlayer.level()
                && !blockEntity.isRemoved()
                && blockEntity.getLevel().getBlockEntity(blockEntity.getBlockPos()) == blockEntity
                && menuPlayer.distanceToSqr(
                blockEntity.getBlockPos().getX() + 0.5,
                blockEntity.getBlockPos().getY() + 0.5,
                blockEntity.getBlockPos().getZ() + 0.5) <= 64.0;
    }
}
