/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.transport.device;

import com.teammoeg.chorda.menu.CBlockEntityMenu;
import com.teammoeg.chorda.menu.CCustomMenuSlot.CDataSlot;
import com.teammoeg.frostedheart.bootstrap.common.FHMenuSlots;
import com.teammoeg.frostedheart.bootstrap.common.FHMenuTypes;
import com.teammoeg.frostedheart.content.town.transport.P2PTerminalRole;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

import java.util.UUID;

/** Terminal inventory plus synchronized related-endpoint, filter, and rate controls. */
public class P2PTerminalMenu extends CBlockEntityMenu<P2PTerminalBlockEntity> {
    public static final int ENDPOINT_SCREEN_HEIGHT = 218;
    public static final int BIDIRECTIONAL_SCREEN_HEIGHT = 254;
    public static final int ENDPOINT_PLAYER_INVENTORY_Y = 136;
    public static final int BIDIRECTIONAL_PLAYER_INVENTORY_Y = 172;
    public static final int BUFFER_SLOT_Y = 142;
    private static final short CMD_SET_RATE = 1;
    private static final short CMD_SET_FILTER_ENTRY = 2;
    private static final short CMD_TOGGLE_FILTER_MODE = 3;
    private static final short CMD_UNBIND_UUID_0 = 4;
    private static final short CMD_UNBIND_UUID_3 = 7;

    private final Player menuPlayer;
    private final CDataSlot<P2PTerminalMenuView> view;
    private final int[] pendingUnbindUuid = new int[4];
    private int pendingUnbindMask;

    public P2PTerminalMenu(
            int id,
            Inventory inventory,
            P2PTerminalBlockEntity blockEntity
    ) {
        super(FHMenuTypes.P2P_TERMINAL.get(), blockEntity, id, inventory.player,
                blockEntity.getRole() == P2PTerminalRole.BIDIRECTIONAL
                        ? P2PTerminalBuffer.TOTAL_SLOTS : 0);
        menuPlayer = inventory.player;
        if (blockEntity.getRole() == P2PTerminalRole.BIDIRECTIONAL) {
            for (int slot = 0; slot < P2PTerminalBuffer.TOTAL_SLOTS; slot++) {
                addSlot(new BufferSlot(blockEntity, slot, bufferSlotX(slot), BUFFER_SLOT_Y));
            }
        }
        view = FHMenuSlots.P2P_TERMINAL_MENU_VIEW_ENCODER_SLOT.create(this);
        if (!inventory.player.level().isClientSide) {
            view.bind(blockEntity::getMenuView);
        }
        addPlayerInventory(inventory, 8, playerInventoryY(), playerHotbarY());
    }

    public P2PTerminalRole getRole() {
        return blockEntity.getRole();
    }

    public int screenHeight() {
        return getRole() == P2PTerminalRole.BIDIRECTIONAL
                ? BIDIRECTIONAL_SCREEN_HEIGHT : ENDPOINT_SCREEN_HEIGHT;
    }

    public int playerInventoryY() {
        return getRole() == P2PTerminalRole.BIDIRECTIONAL
                ? BIDIRECTIONAL_PLAYER_INVENTORY_Y : ENDPOINT_PLAYER_INVENTORY_Y;
    }

    public int playerHotbarY() {
        return playerInventoryY() + 58;
    }

    static int bufferSlotX(int slot) {
        int localSlot = slot % P2PTerminalBuffer.PENDING_SLOTS;
        return (slot < P2PTerminalBuffer.PENDING_SLOTS ? 7 : 97) + localSlot * 18;
    }

    public P2PTerminalMenuView getView() {
        P2PTerminalMenuView value = view.getValue();
        return value == null ? blockEntity.getMenuView() : value;
    }

    public void setTransportRate(int rateItemsPerSecond) {
        sendMessage(CMD_SET_RATE, rateItemsPerSecond);
    }

    public void setFilterEntry(boolean sending, int slot) {
        sendMessage(CMD_SET_FILTER_ENTRY, (sending ? 0x100 : 0) | slot);
    }

    public void toggleFilterMode(boolean sending, boolean fuzzy) {
        sendMessage(CMD_TOGGLE_FILTER_MODE,
                (sending ? 0x100 : 0) | (fuzzy ? 1 : 0));
    }

    public void unbindConnection(UUID connectionId) {
        long most = connectionId.getMostSignificantBits();
        long least = connectionId.getLeastSignificantBits();
        sendMessage(CMD_UNBIND_UUID_0, (int) (most >>> 32));
        sendMessage((short) (CMD_UNBIND_UUID_0 + 1), (int) most);
        sendMessage((short) (CMD_UNBIND_UUID_0 + 2), (int) (least >>> 32));
        sendMessage(CMD_UNBIND_UUID_3, (int) least);
    }

    @Override
    public void receiveMessage(short command, int state) {
        if (!(menuPlayer instanceof ServerPlayer serverPlayer) || !stillValid(menuPlayer)) {
            return;
        }
        switch (command) {
            case CMD_SET_RATE -> blockEntity.setTransportRate(serverPlayer, state);
            case CMD_SET_FILTER_ENTRY -> {
                boolean sending = (state & 0x100) != 0;
                int slot = state & 0xff;
                ItemStack carried = getCarried();
                blockEntity.setFilterEntry(serverPlayer, sending, slot, carried);
            }
            case CMD_TOGGLE_FILTER_MODE -> blockEntity.toggleFilterMode(
                    serverPlayer, (state & 0x100) != 0, (state & 1) != 0);
            default -> {
                if (command >= CMD_UNBIND_UUID_0 && command <= CMD_UNBIND_UUID_3) {
                    receiveUnbindUuidPart(serverPlayer, command - CMD_UNBIND_UUID_0, state);
                }
            }
        }
    }

    private void receiveUnbindUuidPart(ServerPlayer player, int part, int value) {
        if (part == 0) {
            pendingUnbindMask = 0;
        }
        pendingUnbindUuid[part] = value;
        pendingUnbindMask |= 1 << part;
        if (pendingUnbindMask != 0b1111) {
            return;
        }
        pendingUnbindMask = 0;
        long most = (long) pendingUnbindUuid[0] << 32
                | Integer.toUnsignedLong(pendingUnbindUuid[1]);
        long least = (long) pendingUnbindUuid[2] << 32
                | Integer.toUnsignedLong(pendingUnbindUuid[3]);
        blockEntity.unbindConnection(player, new UUID(most, least));
    }

    @Override
    public boolean stillValid(Player player) {
        return player == menuPlayer
                && blockEntity.getLevel() == player.level()
                && !blockEntity.isRemoved()
                && player.distanceToSqr(blockEntity.getBlockPos().getX() + 0.5,
                blockEntity.getBlockPos().getY() + 0.5,
                blockEntity.getBlockPos().getZ() + 0.5) <= 64.0;
    }

    private static final class BufferSlot extends SlotItemHandler {
        private final P2PTerminalBlockEntity terminal;
        private final int bufferSlot;

        private BufferSlot(P2PTerminalBlockEntity terminal, int slot, int x, int y) {
            super(terminal.getBuffer().inventory(), slot, x, y);
            this.terminal = terminal;
            this.bufferSlot = slot;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return bufferSlot < P2PTerminalBuffer.PENDING_SLOTS
                    && !terminal.getBuffer().isPendingLocked()
                    && super.mayPlace(stack);
        }

        @Override
        public boolean mayPickup(Player player) {
            return true;
        }
    }
}
