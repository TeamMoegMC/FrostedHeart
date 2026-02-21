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

package com.teammoeg.frostedheart.content.town.network;

import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.content.town.TeamTown;
import com.teammoeg.frostedheart.content.town.resource.action.*;
import com.teammoeg.frostedheart.content.town.buildings.warehouse.VirtualItemStack;
import com.teammoeg.frostedheart.content.town.buildings.warehouse.WarehouseMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class WarehouseInteractPacket implements CMessage {
	public enum Action {
		EXTRACT, // 取出 (左键/右键点击网格)
		INSERT   // 放入 (拿着物品点击网格)
	}
	private final Action action;//行为 (取出/放入);
	private final boolean isShift;
	private final int button;// 是否按住了 Shift
	private final ItemStack targetItem; //取出的目标物品

    public WarehouseInteractPacket(Action action, boolean isShift, int button,ItemStack itemStack) {
		this.action = action;
		this.isShift = isShift;
		this.button = button;
		this.targetItem = itemStack;
    }

	public WarehouseInteractPacket(FriendlyByteBuf buffer) {
		this.action = buffer.readEnum(Action.class);
		this.isShift = buffer.readBoolean();
        this.button = buffer.readInt();
		this.targetItem = buffer.readItem();
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
		context.get().enqueueWork(() -> {
			ServerPlayer player = context.get().getSender();
			if (player == null) return;

			if (player.containerMenu instanceof WarehouseMenu) {
				IActionExecutorHandler executor = TeamTown.from(player).getActionExecutorHandler();
				ItemStack carried = player.containerMenu.getCarried();
				// 构建存入 Action
				if (this.action == Action.INSERT) {
					ItemStack copyItem = carried.copy();
					if (button==1){
						copyItem.setCount(1);
					}
					TownResourceActions.ItemStackAction action = new TownResourceActions.ItemStackAction(copyItem, ResourceActionType.ADD, ResourceActionMode.MAXIMIZE);
					TownResourceActionResults.ItemStackActionResult result = (TownResourceActionResults.ItemStackActionResult)executor.execute(action);
					if (!result.itemStackModified().isEmpty()) {
						carried.shrink(result.itemStackModified().getCount());
						player.containerMenu.broadcastChanges();
					}
				} else if (this.action == Action.EXTRACT) {
					if (targetItem.isEmpty()) return;
					int amountToTake  = targetItem.getMaxStackSize();
					if (button==1) {
						int halfToTake = (int) Math.ceil(TeamTown.from(player).getResourceHolder().get(targetItem) / 2);
						amountToTake  = (amountToTake  + 1) / 2;
						amountToTake = Math.min(amountToTake, halfToTake);
					}
                    // 构建取出 Action
					//TownResourceActions.ItemResourceAction action = new TownResourceActions.ItemResourceAction(targetItem,
					//		ResourceActionType.COST,
                    //        maxStack,
					//		ResourceActionMode.MAXIMIZE
					//);
					//TownResourceActionResults.ItemResourceActionResult result = (TownResourceActionResults.ItemResourceActionResult) executor.execute(action);
					//尝试ItemStackAction
					TownResourceActions.ItemStackAction action = new TownResourceActions.ItemStackAction(targetItem.copyWithCount(amountToTake), ResourceActionType.COST, ResourceActionMode.MAXIMIZE);
					TownResourceActionResults.ItemStackActionResult result = (TownResourceActionResults.ItemStackActionResult)executor.execute(action);


					//int shouldStack = (int) result.modifiedAmount();
					if (/*shouldStack > 0*/!result.itemStackModified().isEmpty()) {
						//ItemStack extracted = targetItem.copy();
						ItemStack extracted = result.itemStackModified();
						//extracted.setCount(shouldStack);
						// Shift取出
						if (this.isShift) {
							player.getInventory().add(extracted);
							// 检查剩余
							if (!extracted.isEmpty()) {
								// 4. 退款：背包满了，把剩下的存回仓库
//								double refundAmount = extracted.getCount();
/*								TownResourceActions.ItemResourceAction refundAction = new TownResourceActions.ItemResourceAction(
										extracted,
										ResourceActionType.ADD,
										refundAmount,
										ResourceActionMode.MAXIMIZE
								);
								executor.execute(refundAction);*/
								TownResourceActions.ItemStackAction refundAction = new TownResourceActions.ItemStackAction(extracted, ResourceActionType.ADD, ResourceActionMode.MAXIMIZE);
								executor.execute(refundAction);

							}
						}
						else {
							player.containerMenu.setCarried(extracted);
						}
						player.containerMenu.broadcastChanges();
					}
				}


				Map<ItemStack, Double> itemMap = TeamTown.from(player).getResourceHolder().getAllItems();
				List<VirtualItemStack> list = VirtualItemStack.toClientVisualList(itemMap);
				FHNetwork.INSTANCE.sendPlayer(player, new WarehouseS2CPacket(list));
			}
		});
		context.get().setPacketHandled(true);
	}
	@Override
	public void encode(FriendlyByteBuf buffer) {
		buffer.writeEnum(this.action);
		buffer.writeBoolean(this.isShift);
		buffer.writeInt(this.button);
		buffer.writeItem(this.targetItem);
	}
}
