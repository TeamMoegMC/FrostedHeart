package com.teammoeg.frostedheart.trade;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import com.teammoeg.frostedheart.FHContent;

import blusunrize.immersiveengineering.common.gui.IESlot;
import net.minecraft.entity.merchant.villager.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

public class TradeContainer extends Container {
	FHVillagerData data;
	PlayerRelationData pld;
	RelationList relations;
	PolicySnapshot policy;
	VillagerEntity ve;
	ItemStackHandler inv = new ItemStackHandler(12) {

		@Override
		public boolean isItemValid(int slot,ItemStack stack) {
			return policy.buys.stream().anyMatch(t->t.getItem().test(stack))&&super.isItemValid(slot, stack);
		}
		@Override
		protected void onContentsChanged(int slot) {
			super.onContentsChanged(slot);
			recalc();
		}
		
	};
	//client side memory
	LinkedHashMap<String,Integer> order=new LinkedHashMap<>();
	int balance;
	int maxdiscount;
	float discountRatio;
	public TradeContainer(int id, PlayerInventory inventoryPlayer, PacketBuffer pb) {
		this(id, inventoryPlayer,(VillagerEntity) inventoryPlayer.player.getEntityWorld().getEntityByID(pb.readVarInt()));

		data = new FHVillagerData(ve);
		CompoundNBT d=pb.readCompoundTag();
		System.out.println(d);
		data.deserializeFromRecv(d);
		pld = new PlayerRelationData();
		pld.deserialize(pb.readCompoundTag());
		relations = new RelationList();
		relations.read(pb);
		policy = data.getPolicy();
		policy.fetchTrades(data.storage);
	}

	public TradeContainer(int id, PlayerInventory inventoryPlayer,
			VillagerEntity ve /*,PlayerRelationData prd,RelationList rel */) {
		super(FHContent.TRADE_GUI.get(), id);
		// Server does not need such data as server always have access to all data.
		/*
		 * this.pld=prd;
		 * this.data=ve;
		 * this.relations=rel;
		 */
		this.ve = ve;
		for (int i = 0; i < 3; i++)
			for (int j = 0; j < 4; j++)
				addSlot(new SlotItemHandler(inv, j + i * 4, 62 + j * 16, 18 + i * 16));

		for (int i = 0; i < 3; i++)
			for (int j = 0; j < 9; j++)
				addSlot(new Slot(inventoryPlayer, j + i * 9 + 9, 14 + j * 18, 145 + i * 18));
		for (int i = 0; i < 9; i++)
			addSlot(new Slot(inventoryPlayer, i, 14 + i * 18, 203));

	}
	protected boolean tryMergeStack(ItemStack pStack, int pStartIndex, int pEndIndex)
	{
		boolean inAllowedRange = true;
		int allowedStart = pStartIndex;
		for(int i = pStartIndex; i < pEndIndex; i++)
		{
			boolean mayplace = this.inventorySlots.get(i).isItemValid(pStack);
			if(inAllowedRange&&(!mayplace||i==pEndIndex-1))
			{
				if(this.mergeItemStack(pStack, allowedStart, i, false))
					return true;
				inAllowedRange = false;
			}
			else if(!inAllowedRange&&mayplace)
			{
				allowedStart = i;
				inAllowedRange = true;
			}
		}
		return false;
	}
	@Override
	public ItemStack transferStackInSlot(PlayerEntity player, int slot)
	{
		ItemStack itemstack = ItemStack.EMPTY;
		Slot slotObject = super.inventorySlots.get(slot);
		final int slotCount=12;
		if(slotObject!=null&&slotObject.getHasStack())
		{
			ItemStack itemstack1 = slotObject.getStack();
			itemstack = itemstack1.copy();
			if(slot < slotCount)
			{
				if(!this.mergeItemStack(itemstack1, slotCount, this.inventorySlots.size(), true))
				{
					return ItemStack.EMPTY;
				}
			}
			else if(!this.tryMergeStack(itemstack1, 0, slotCount))
			{
				return ItemStack.EMPTY;
			}

			if(itemstack1.isEmpty())
			{
				slotObject.putStack(ItemStack.EMPTY);
			}
			else
			{
				slotObject.onSlotChanged();
			}
		}

		return itemstack;
	}
	public void setPolicy(PolicySnapshot ps) {
		this.policy=ps;
	}
	public void recalc() {
		int poffer=0;
		outer:for(int i=0;i<inv.getSlots();i++) {
			ItemStack is=inv.getStackInSlot(i);
			for(BuyData bd:policy.buys) {
				if(bd.getItem().test(is)) {
					int cnt=Math.min(is.getCount(),bd.store);
					poffer+=cnt*bd.price;
					continue outer;
				}
			}
		}
		int voffer=0;
		for(Entry<String, Integer> entry:order.entrySet()) {
			voffer+=policy.sells.get(entry.getKey()).price*entry.getValue();
		}
		if(voffer>2*poffer)
			balance=-3;
		else if(voffer>1.5f*poffer)
			balance=-2;
		else if(voffer>poffer)
			balance=-1;
		else if(2*voffer<poffer)
			balance=3;
		else if(1.5f*voffer<poffer)
			balance=2;
		else if(1.05f*voffer<poffer)
			balance=1;
		else
			balance=0;
	}
	@Override
	public boolean canInteractWith(PlayerEntity playerIn) {
		return true;
	}

}
