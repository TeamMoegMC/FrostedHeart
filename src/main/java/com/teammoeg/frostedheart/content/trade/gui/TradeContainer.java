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

import com.teammoeg.chorda.util.CUtils;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.bootstrap.common.FHMenuTypes;
import com.teammoeg.frostedheart.content.climate.gamedata.climate.WorldClimate;
import com.teammoeg.frostedheart.content.trade.*;
import com.teammoeg.frostedheart.content.trade.network.BargainResponse;
import com.teammoeg.frostedheart.content.trade.network.TradeUpdatePacket;
import com.teammoeg.frostedheart.content.trade.policy.snapshot.BuyData;
import com.teammoeg.frostedheart.content.trade.policy.snapshot.PolicySnapshot;
import com.teammoeg.frostedheart.content.trade.policy.snapshot.SellData;

import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

public class TradeContainer extends AbstractContainerMenu {

    public class DetectionSlot extends Slot {
        boolean isSaleable = false;

        public DetectionSlot(Container inventoryIn, int index, int xPosition, int yPosition) {
            super(inventoryIn, index, xPosition, yPosition);
            slots.add(this);
        }

        @Override
        public void onQuickCraft(ItemStack oldStackIn, ItemStack newStackIn) {
            super.onQuickCraft(oldStackIn, newStackIn);
            redetect();

        }

        @Override
        public void setChanged() {
            super.setChanged();
            redetect();
        }

        private void redetect() {
            isSaleable = false;
            for (BuyData bd : policy.getBuys()) {
                if (bd.getItem().test(this.getItem())) {
                    if (bd.getStore() != 0) {
                        isSaleable = true;
                        break;
                    }
                }
            }
        }

    }

    public FHVillagerData data;
    public PlayerRelationData pld;
    public RelationList relations;
    public PolicySnapshot policy;

    public Villager ve;

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

    public TradeContainer(int id, Inventory inventoryPlayer, FriendlyByteBuf pb) {
        this(id, inventoryPlayer,
                (Villager) inventoryPlayer.player.getCommandSenderWorld().getEntity(pb.readVarInt()));

        data = new FHVillagerData(ve);
        CompoundTag d = pb.readNbt();
        // System.out.println(d);
        data.deserializeFromRecv(d);
        pld = new PlayerRelationData(0);
        pld.deserialize(pb.readNbt());
        relations = new RelationList();
        relations.read(pb);
        policy = data.getPolicy();
        policy.fetchTrades(data.storage);
    }

    public TradeContainer(int id, Inventory inventoryPlayer,
                          Villager ve /* ,PlayerRelationData prd,RelationList rel */) {
        super(FHMenuTypes.TRADE_GUI.get(), id);
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
                addSlot(new SlotItemHandler(inv, j + i * 4, 62 + j * 16, 4 + i * 16) {

                    @Override
                    public void onQuickCraft(ItemStack oldStackIn, ItemStack newStackIn) {
                        super.onQuickCraft(oldStackIn, newStackIn);
                    }

                    @Override
                    public void setChanged() {
                        super.setChanged();
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

    public boolean stillValid(Player playerIn) {
        return ve.getTradingPlayer() == playerIn;
    }

    public void commitTrade(ServerPlayer pe) {
        recalc();
        int poffer = 0;
        if (balance >= 0) {

            outer:
            for (int i = 0; i < inv.getSlots(); i++) {
                ItemStack is = inv.getStackInSlot(i);
                for (BuyData bd : policy.getBuys()) {
                    if (bd.getItem().test(is)) {
                        int cnt = Math.min(is.getCount(), bd.getStore());
                        if (ve.wantsToPickUp(is)) {
                            ve.getInventory().addItem(ItemHandlerHelper.copyStackWithSize(is, cnt));
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
                PlayerRelationData prd = this.data.getRelationDataForWrite(pe,WorldClimate.getWorldDay(pe.level()));
                prd.totalbenefit += benefits / 10;
            }
            poffer += discountAmount;
            if (relations.sum() > TradeConstants.RELATION_TO_TRADE) {
                for (Entry<String, Integer> entry : order.entrySet()) {
                    SellData sd = policy.getSells().get(entry.getKey());
                    int cnt = Math.min(sd.getStore(), entry.getValue());
                    int price = cnt * sd.getPrice();
                    if (poffer >= price) {
                        poffer -= price;
                        CUtils.giveItem(pe, ItemHandlerHelper.copyStackWithSize(sd.getItem(), cnt));
                        sd.reduceStock(data, cnt);
                        data.totaltraded += price;
                    }

                }
                data.updateLevel();
                order.clear();
            }
            this.setData(data, pe);
            FHNetwork.INSTANCE.sendPlayer(pe, new TradeUpdatePacket(
                    data.serializeForSend(new CompoundTag()), pld.serialize(new CompoundTag()), relations, true));
        }
    }

    public void fireClientUpdateTrade() {
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> ClientTradeHandler::updateTrade);
    }

    public void handleBargain(ServerPlayer pe) {
        if (relations.sum() < TradeConstants.RELATION_TO_BARGAIN)
            return;
        recalc();
        if (order.isEmpty())
            return;

        boolean succeed = false;

        // bargain based on relation
        int playerRelation = Math.min(relations.sum(), TradeConstants.RELATION_MAX);
        int relationSurplus = playerRelation - TradeConstants.RELATION_TO_BARGAIN;
        float probability = (float) relationSurplus / (TradeConstants.RELATION_MAX - TradeConstants.RELATION_TO_BARGAIN);

        if (pe.getRandom().nextFloat() < probability) {
            discountRatio = maxdiscount / ((float) voffer);
            discountRatio = Math.min(0.4f, discountRatio + 0.1f);
            maxdiscount = (int) (voffer * discountRatio);
            bargained++;
            data.bargain++;
            relations = data.getRelationShip(pe);
            succeed = true;
        }
        FHNetwork.INSTANCE.sendPlayer(pe, new BargainResponse(this, succeed));
    }

    @Override
    public void removed(Player pPlayer) {
        this.ve.setTradingPlayer(null);
        if (!pPlayer.isAlive()
                || pPlayer instanceof ServerPlayer && ((ServerPlayer) pPlayer).hasDisconnected()) {
            for (int j = 0; j < inv.getSlots(); ++j) {
                pPlayer.drop(inv.getStackInSlot(j), false);
            }
        } else {
            Inventory inventory = pPlayer.getInventory();
            if (inventory.player instanceof ServerPlayer) {
                for (int i = 0; i < inv.getSlots(); ++i) {
                    inventory.placeItemBackInInventory(inv.getStackInSlot(i));
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
            if (relation < TradeConstants.RELATION_TO_TRADE) {
                relationMinus = 10000000;
            } else if (relation < 0 && voffer > 0) {
                relationMinus = (int) (Mth.lerp(Mth.clamp(-relation / 30f, 0, 1), 0, 0.2) * voffer);
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

    public void setData(FHVillagerData dat, Player pe) {
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
    public ItemStack quickMoveStack(Player player, int slot) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slotObject = super.slots.get(slot);
        final int slotCount = 12;
        if (slotObject != null && slotObject.hasItem()) {
            ItemStack itemstack1 = slotObject.getItem();
            itemstack = itemstack1.copy();
            if (slot < slotCount) {
                if (!this.moveItemStackTo(itemstack1, slotCount, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.tryMergeStack(itemstack1, 0, slotCount)) {
                return ItemStack.EMPTY;
            }

            if (itemstack1.isEmpty()) {
                slotObject.set(ItemStack.EMPTY);
            } else {
                slotObject.setChanged();
            }
        }

        return itemstack;
    }

    protected boolean tryMergeStack(ItemStack pStack, int pStartIndex, int pEndIndex) {
        boolean inAllowedRange = true;
        int allowedStart = pStartIndex;
        for (int i = pStartIndex; i < pEndIndex; i++) {
            boolean mayplace = this.slots.get(i).mayPlace(pStack);
            if (inAllowedRange && (!mayplace || i == pEndIndex - 1)) {
                if (this.moveItemStackTo(pStack, allowedStart, i, false))
                    return true;
                inAllowedRange = false;
            } else if (!inAllowedRange && mayplace) {
                allowedStart = i;
                inAllowedRange = true;
            }
        }
        return false;
    }

    public void update(CompoundTag sdata, CompoundTag splayer, RelationList rels, boolean isReset) {
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
