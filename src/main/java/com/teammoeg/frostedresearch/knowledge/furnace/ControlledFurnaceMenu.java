package com.teammoeg.frostedresearch.knowledge.furnace;

import com.teammoeg.chorda.dataholders.team.CTeamDataManager;
import com.teammoeg.chorda.menu.CBlockEntityMenu;
import com.teammoeg.frostedresearch.FRContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.items.SlotItemHandler;

public final class ControlledFurnaceMenu extends CBlockEntityMenu<ControlledFurnaceBlockEntity> {
    public static final int SAMPLE_X = 59, SAMPLE_Y = 84, FUEL_X = 200, FUEL_Y = SAMPLE_Y;
    public static final int INVENTORY_X = 57, INVENTORY_Y = 148, HOTBAR_Y = 206;
    public static final int STOP = 0;

    public ControlledFurnaceMenu(int id, Inventory inventory, ControlledFurnaceBlockEntity furnace) {
        super(FRContents.MenuTypes.CONTROLLED_FURNACE.get(), furnace, id, inventory.player,
                ControlledFurnaceBlockEntity.SLOT_COUNT);
        for (int slot = 0; slot < ControlledFurnaceBlockEntity.SLOT_COUNT; slot++) {
            boolean fuel = slot == ControlledFurnaceBlockEntity.FUEL_SLOT;
            addSlot(new SlotItemHandler(furnace.inventory(), slot, fuel ? FUEL_X : SAMPLE_X + slot * 18, fuel ? FUEL_Y : SAMPLE_Y) {
                @Override public boolean mayPlace(ItemStack stack) {
                    return furnace.inventory().isItemValid(getSlotIndex(), stack);
                }
                @Override public int getMaxStackSize(ItemStack stack) {
                    return Math.min(getMaxStackSize(), stack.getMaxStackSize());
                }
                @Override public boolean mayPickup(Player player) { return fuel || !furnace.running(); }
            });
        }
        for (int i = 0; i < 36; i++)
            addSlot(new Slot(inventory, i, INVENTORY_X + (i % 9) * 18, i < 9 ? HOTBAR_Y : INVENTORY_Y + (i / 9 - 1) * 18));
    }

    @Override public boolean stillValid(Player player) {
        return super.stillValid(player) && player.level() == getBlock().getLevel()
                && (player.level().isClientSide || CTeamDataManager.get(player).getId().equals(getBlock().team()))
                && player.distanceToSqr(Vec3.atCenterOf(getBlock().getBlockPos())) <= 64;
    }

    public void start(ServerPlayer player, int temperature, int seconds) {
        if (stillValid(player)) getBlock().start(temperature, seconds);
    }

    @Override public boolean clickMenuButton(Player player, int id) {
        if (id != STOP || !(player instanceof ServerPlayer) || !stillValid(player)) return false;
        getBlock().stop();
        return true;
    }

    @Override public boolean quickMoveIn(ItemStack stack) {
        if (ControlledFurnaceBlockEntity.isFuel(stack))
            return moveItemStackTo(stack, ControlledFurnaceBlockEntity.FUEL_SLOT, ControlledFurnaceBlockEntity.SLOT_COUNT, false);
        if (getBlock().running()) return false;
        boolean moved = false;
        for (int slot = 0; slot < ControlledFurnaceBlockEntity.SAMPLE_SLOTS && !stack.isEmpty(); slot++)
            moved |= moveItemStackTo(stack, slot, slot + 1, false);
        return moved;
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < ControlledFurnaceBlockEntity.SAMPLE_SLOTS && getBlock().running()) return ItemStack.EMPTY;
        return super.quickMoveStack(player, index);
    }
}
