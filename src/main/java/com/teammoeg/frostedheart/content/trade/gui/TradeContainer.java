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

package com.teammoeg.frostedheart.content.trade.gui;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.teammoeg.frostedheart.FHContainer;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.content.trade.ClientTradeHandler;
import com.teammoeg.frostedheart.content.trade.FHVillagerData;
import com.teammoeg.frostedheart.content.trade.PlayerRelationData;
import com.teammoeg.frostedheart.content.trade.RelationList;
import com.teammoeg.frostedheart.content.trade.network.BargainResponse;
import com.teammoeg.frostedheart.content.trade.network.TradeUpdatePacket;
import com.teammoeg.frostedheart.content.trade.policy.snapshot.BuyData;
import com.teammoeg.frostedheart.content.trade.policy.snapshot.PolicySnapshot;
import com.teammoeg.frostedheart.content.trade.policy.snapshot.SellData;
import com.teammoeg.frostedheart.util.FHUtils;

import net.minecraft.entity.merchant.villager.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

public class TradeContainer extends Container {
    public class DetectionSlot extends Slot {
        boolean isSaleable = false;

        public DetectionSlot(IInventory inventoryIn, int index, int xPosition, int yPosition) {
            super(inventoryIn, index, xPosition, yPosition);
            slots.add(this);
        }

        @Override
        public void onSlotChange(ItemStack oldStackIn, ItemStack newStackIn) {
            super.onSlotChange(oldStackIn, newStackIn);
            redetect();

        }

        @Override
        public void onSlotChanged() {
            super.onSlotChanged();
            redetect();
        }

        private void redetect() {
            isSaleable = false;
            for (BuyData bd : policy.getBuys()) {
                if (bd.getItem().test(this.getStack())) {
                    if (bd.getStore() != 0) {
                        isSaleable = true;
                        break;
                    }
                }
            }
        }

    }
    public static final int RELATION_TO_TRADE = -30;
    public FHVillagerData data;
    public PlayerRelationData pld;
    public RelationList relations;
    public PolicySnapshot policy;

    public VillagerEntity ve;

    ItemStackHandler inv = new ItemStackHandler(12) {

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return policy.getBuys().stream().anyMatch(t -> t.getItem().test(stack)) && super.isItemValid(slot, stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            super.onContentsChanged(slot);
            recalc();
            fireClientUpdateTrade();
        }

    };

    // client side memory
    LinkedHashMap<String, Integer> order = new LinkedHashMap<>();
    List<DetectionSlot> slots = new ArrayList<>();
    public int balance;
    public int maxdiscount;
    public float discountRatio;
    public int bargained;
    public int discountAmount;
    public int relationMinus;

    int voffer, poffer, originalVOffer;

    public TradeContainer(int id, PlayerInventory inventoryPlayer, PacketBuffer pb) {
        this(id, inventoryPlayer,
                (VillagerEntity) inventoryPlayer.player.getEntityWorld().getEntityByID(pb.readVarInt()));

        data = new FHVillagerData(ve);
        CompoundNBT d = pb.readCompoundTag();
        // System.out.println(d);
        data.deserializeFromRecv(d);
        pld = new PlayerRelationData();
        pld.deserialize(pb.readCompoundTag());
        relations = new RelationList();
        relations.read(pb);
        policy = data.getPolicy();
        policy.fetchTrades(data.storage);
    }

    public TradeContainer(int id, PlayerInventory inventoryPlayer,
                          VillagerEntity ve /* ,PlayerRelationData prd,RelationList rel */) {
        super(FHContainer.TRADE_GUI.get(), id);
        // Server does not need such data as server always have access to all data.
        /*
         * this.pld=prd;
         * this.data=ve;
         * this.relations=rel;
         */

        this.ve = ve;
        //System.out.println(ve.getCustomer());
        //ve.setCustomer(inventoryPlayer.player);
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 4; j++)
                addSlot(new SlotItemHandler(inv, j + i * 4, 62 + j * 16, 18 + i * 16) {

                    @Override
                    public void onSlotChange(ItemStack oldStackIn, ItemStack newStackIn) {
                        super.onSlotChange(oldStackIn, newStackIn);
                    }

                    @Override
                    public void onSlotChanged() {
                        super.onSlotChanged();
                        recalc();
                        fireClientUpdateTrade();
                    }

                });

        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 9; j++)
                addSlot(new DetectionSlot(inventoryPlayer, j + i * 9 + 9, 14 + j * 18, 145 + i * 18));
        for (int i = 0; i < 9; i++)
            addSlot(new DetectionSlot(inventoryPlayer, i, 14 + i * 18, 203));

    }

    public boolean canInteractWith(PlayerEntity playerIn) {
        return ve.getCustomer() == playerIn;
    }

    public void commitTrade(ServerPlayerEntity pe) {
        recalc();
        int poffer = 0;
        if (balance >= 0) {

            outer:
            for (int i = 0; i < inv.getSlots(); i++) {
                ItemStack is = inv.getStackInSlot(i);
                for (BuyData bd : policy.getBuys()) {
                    if (bd.getItem().test(is)) {
                        int cnt = Math.min(is.getCount(), bd.getStore());
                        if (ve.func_230293_i_(is)) {
                            ve.getVillagerInventory().addItem(ItemHandlerHelper.copyStackWithSize(is, cnt));
                        }
                        if (cnt == is.getCount())
                            inv.setStackInSlot(i, ItemStack.EMPTY);
                        else
                            is.shrink(cnt);

                        bd.reduceStock(data, cnt);
                        poffer += cnt * bd.getPrice();
                        continue outer;
                    }
                }
            }
            int benefits = this.poffer - this.originalVOffer;
            System.out.println(benefits);
            if (benefits > 10) {
                PlayerRelationData prd = this.data.getRelationDataForWrite(pe);
                prd.totalbenefit += benefits / 10;
            }
            poffer += discountAmount;
            if (relations.sum() > RELATION_TO_TRADE) {
                for (Entry<String, Integer> entry : order.entrySet()) {
                    SellData sd = policy.getSells().get(entry.getKey());
                    int cnt = Math.min(sd.getStore(), entry.getValue());
                    int price = cnt * sd.getPrice();
                    if (poffer >= price) {
                        poffer -= price;
                        FHUtils.giveItem(pe, ItemHandlerHelper.copyStackWithSize(sd.getItem(), cnt));
                        sd.reduceStock(data, cnt);
                        data.totaltraded += price;
                    }

                }
                data.updateLevel();
                order.clear();
            }
            this.setData(data, pe);
            FHNetwork.send(PacketDistributor.PLAYER.with(() -> pe), new TradeUpdatePacket(
                    data.serializeForSend(new CompoundNBT()), pld.serialize(new CompoundNBT()), relations, true));
        }
    }

    public void fireClientUpdateTrade() {
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> ClientTradeHandler::updateTrade);
    }

    public void handleBargain(ServerPlayerEntity pe) {
        if (relations.sum() < 40)
            return;
        recalc();
        if (order.isEmpty())
            return;

        boolean succeed = false;
        if (true) {// TODO:simulates bargain success
            discountRatio = maxdiscount / ((float) voffer);
            discountRatio = Math.min(0.4f, discountRatio + 0.1f);
            maxdiscount = (int) (voffer * discountRatio);
            bargained++;
            data.bargain++;
            relations = data.getRelationShip(pe);
            succeed = true;
        }
        FHNetwork.send(PacketDistributor.PLAYER.with(() -> pe), new BargainResponse(this, succeed));
    }

    @Override
    public void onContainerClosed(PlayerEntity pPlayer) {
        this.ve.setCustomer(null);
        if (!pPlayer.isAlive()
                || pPlayer instanceof ServerPlayerEntity && ((ServerPlayerEntity) pPlayer).hasDisconnected()) {
            for (int j = 0; j < inv.getSlots(); ++j) {
                pPlayer.dropItem(inv.getStackInSlot(j), false);
            }
        } else {
            PlayerInventory inventory = pPlayer.inventory;
            if (inventory.player instanceof ServerPlayerEntity) {
                for (int i = 0; i < inv.getSlots(); ++i) {
                    inventory.placeItemBackInInventory(pPlayer.world, inv.getStackInSlot(i));
                }
            }
        }

    }

    public void recalc() {
        poffer = 0;
        outer:
        for (int i = 0; i < inv.getSlots(); i++) {
            ItemStack is = inv.getStackInSlot(i);
            for (BuyData bd : policy.getBuys()) {
                if (bd.getItem().test(is)) {
                    int cnt = Math.min(is.getCount(), bd.getStore());
                    poffer += cnt * bd.getPrice();
                    continue outer;
                }
            }
        }
        voffer = 0;
        for (Entry<String, Integer> entry : order.entrySet()) {
            voffer += policy.getSells().get(entry.getKey()).getPrice() * entry.getValue();
        }
        originalVOffer = voffer;
        relationMinus = 0;
        discountAmount = 0;
        int relation = relations.sum();
        if (!order.isEmpty()) {
            if (relation < RELATION_TO_TRADE) {
                relationMinus = 10000000;
            } else if (relation < 0 && voffer > 0) {
                relationMinus = (int) (MathHelper.lerp(MathHelper.clamp(-relation / 30f, 0, 1), 0, 0.2) * voffer);
            }
            voffer += relationMinus;
            if (discountRatio > 0) {
                discountAmount = Math.min((int) (voffer * discountRatio), maxdiscount);
                voffer -= discountAmount;
            }
        }
        // System.out.println(relationMinus);
        if (voffer > 2 * poffer)
            balance = -3;
        else if (voffer > 1.5f * poffer)
            balance = -2;
        else if (voffer > poffer)
            balance = -1;
        else if (2 * voffer < poffer)
            balance = 3;
        else if (1.5f * voffer < poffer)
            balance = 2;
        else if (1.05f * voffer < poffer)
            balance = 1;
        else
            balance = 0;
    }

    public void setData(FHVillagerData dat, PlayerEntity pe) {
        data = dat;
        pld = data.getRelationDataForRead(pe);
        relations = data.getRelationShip(pe);
        policy = data.getPolicy();
        policy.fetchTrades(data.storage);
    }

    public void setOrder(Map<String, Integer> order) {
        this.order.clear();
        this.order.putAll(order);
        this.recalc();
    }

    @Override
    public ItemStack transferStackInSlot(PlayerEntity player, int slot) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slotObject = super.inventorySlots.get(slot);
        final int slotCount = 12;
        if (slotObject != null && slotObject.getHasStack()) {
            ItemStack itemstack1 = slotObject.getStack();
            itemstack = itemstack1.copy();
            if (slot < slotCount) {
                if (!this.mergeItemStack(itemstack1, slotCount, this.inventorySlots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.tryMergeStack(itemstack1, 0, slotCount)) {
                return ItemStack.EMPTY;
            }

            if (itemstack1.isEmpty()) {
                slotObject.putStack(ItemStack.EMPTY);
            } else {
                slotObject.onSlotChanged();
            }
        }

        return itemstack;
    }

    protected boolean tryMergeStack(ItemStack pStack, int pStartIndex, int pEndIndex) {
        boolean inAllowedRange = true;
        int allowedStart = pStartIndex;
        for (int i = pStartIndex; i < pEndIndex; i++) {
            boolean mayplace = this.inventorySlots.get(i).isItemValid(pStack);
            if (inAllowedRange && (!mayplace || i == pEndIndex - 1)) {
                if (this.mergeItemStack(pStack, allowedStart, i, false))
                    return true;
                inAllowedRange = false;
            } else if (!inAllowedRange && mayplace) {
                allowedStart = i;
                inAllowedRange = true;
            }
        }
        return false;
    }

    public void update(CompoundNBT sdata, CompoundNBT splayer, RelationList rels, boolean isReset) {
        this.data.deserializeFromRecv(sdata);
        this.pld.deserialize(splayer);
        policy = data.getPolicy();
        policy.fetchTrades(data.storage);
        relations.copy(rels);
        if (isReset) {
            order.clear();
            balance = 0;
            maxdiscount = 0;
            discountRatio = 0;
            bargained = 0;
            relationMinus = 0;
            discountAmount = 0;
            relationMinus = 0;
            poffer = voffer = originalVOffer = 0;
            recalc();
            ClientTradeHandler.updateAll();
        } else
            recalc();

    }

}
