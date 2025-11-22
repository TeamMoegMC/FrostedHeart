package com.teammoeg.frostedheart.content.town.warehouse;

import com.teammoeg.chorda.menu.CBlockEntityMenu;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.bootstrap.common.FHMenuTypes;
import com.teammoeg.frostedheart.content.town.TeamTown;
import com.teammoeg.frostedheart.content.town.network.WarehouseS2CPacket;
import com.teammoeg.frostedheart.content.town.resource.action.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class WarehouseMenu extends CBlockEntityMenu<WarehouseBlockEntity> {

	public WarehouseMenu(int id, Inventory inventoryPlayer, WarehouseBlockEntity tile) {
		super(FHMenuTypes.WAREHOUSE.get(),tile,id, inventoryPlayer.player,32);

		super.addPlayerInventory(inventoryPlayer, 8, 139, 197);
	}


	@Override
	public void receiveMessage(short btnId, int state) {
		switch(btnId) {
		}
	}
	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		ItemStack originalStack = ItemStack.EMPTY;
		Slot slot = this.slots.get(index);

		if (slot.hasItem()) {
			ItemStack slotStack = slot.getItem();
			originalStack = slotStack.copy();

			if (!player.level().isClientSide) {
				TeamTown town = TeamTown.from(player);
                IActionExecutorHandler executor = town.getActionExecutorHandler();
                //构建存入 Action
                double amountToAdd = slotStack.getCount();
                TownResourceActions.ItemResourceAction action = new TownResourceActions.ItemResourceAction(
                        slotStack,
                        ResourceActionType.ADD,
                        amountToAdd,
                        ResourceActionMode.MAXIMIZE
                );

                var result = (TownResourceActionResults.ItemResourceActionResult) executor.execute(action);
                int insertedCount = (int) result.modifiedAmount();
                if (insertedCount > 0) {
                    // 扣除 Slot 里的数量
                    slotStack.shrink(insertedCount);

                    if (slotStack.isEmpty()) {
                        slot.set(ItemStack.EMPTY);
                    } else {
                        slot.setChanged();
                    }
                    List<VirtualItemStack> list = new ArrayList<>();
                    VirtualItemStack.toClientVisualList(list, town.getResourceHolder().getAllItems());
                    FHNetwork.INSTANCE.sendPlayer((ServerPlayer) player, new WarehouseS2CPacket(list));
                }
            }

			if (slotStack.getCount() == originalStack.getCount()) {
				return ItemStack.EMPTY;
			}
		}

		return originalStack;
	}
	@Override
	public void removed(Player pPlayer) {
		super.removed(pPlayer);
	}
}
