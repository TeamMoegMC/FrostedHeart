/*
 * Copyright (c) 2024 TeamMoeg
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
import com.teammoeg.frostedheart.content.town.resource.action.IActionExecutorHandler;
import com.teammoeg.frostedheart.content.town.resource.action.ResourceActionMode;
import com.teammoeg.frostedheart.content.town.resource.action.ResourceActionType;
import com.teammoeg.frostedheart.content.town.resource.action.TownResourceActions;
import com.teammoeg.frostedheart.content.town.warehouse.VirtualItemStack;
import com.teammoeg.frostedheart.content.town.warehouse.WarehouseMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class WarehouseInteractPacket implements CMessage {
	public enum Action {
		EXTRACT, // 取出 (左键/右键点击网格)
		INSERT   // 放入 (拿着物品点击网格)
	}
	private final Action action;//行为 (取出/放入);
	private final boolean isShift; // 是否按住了 Shift
	private final ItemStack targetItem; //取出的目标物品

    public WarehouseInteractPacket(Action action, boolean isShift, ItemStack itemStack) {
		this.action = action;
		this.isShift = isShift;
		this.targetItem = itemStack;
    }

	public WarehouseInteractPacket(FriendlyByteBuf buffer) {
		this.action = buffer.readEnum(Action.class);
		this.isShift = buffer.readBoolean();
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
					double amountToAdd = carried.getCount();
					TownResourceActions.ItemResourceAction action = new TownResourceActions.ItemResourceAction(carried, ResourceActionType.ADD, amountToAdd, ResourceActionMode.MAXIMIZE);
					TownResourceActions.ItemResourceActionResult result = (TownResourceActions.ItemResourceActionResult)executor.execute(action);
					int shouldStack = (int) result.modifiedAmount();
					if (result.modifiedAmount()>=1) {
						ItemStack inserted = carried.copy();
						inserted.shrink(shouldStack);
						player.containerMenu.setCarried(inserted);
						player.containerMenu.broadcastChanges();
					}
				} else if (this.action == Action.EXTRACT) {
					if (targetItem.isEmpty()) return;
					int maxStack = targetItem.getMaxStackSize();

                    // 构建取出 Action
					TownResourceActions.ItemResourceAction action = new TownResourceActions.ItemResourceAction(targetItem,
							ResourceActionType.COST,
                            maxStack,
							ResourceActionMode.MAXIMIZE
					);
					TownResourceActions.ItemResourceActionResult result = (TownResourceActions.ItemResourceActionResult) executor.execute(action);
					int shouldStack = (int) result.modifiedAmount();
					if (result.modifiedAmount()>0) {
						ItemStack extracted = targetItem.copy();
						extracted.setCount(shouldStack);
						player.containerMenu.setCarried(extracted);
						player.containerMenu.broadcastChanges();
					}
				}


				Map<ItemStack, Double> itemMap = TeamTown.from(player).getResourceHolder().getAllItems();
				List<VirtualItemStack> list = new ArrayList<>();
				VirtualItemStack.toClientVisualList(list,itemMap);
				FHNetwork.INSTANCE.sendPlayer(player, new WarehouseS2CPacket(list));
			}
		});
		context.get().setPacketHandled(true);
	}
	@Override
	public void encode(FriendlyByteBuf buffer) {
		buffer.writeEnum(this.action);
		buffer.writeBoolean(this.isShift);
		buffer.writeItem(this.targetItem);
	}
}
