/*
 * Copyright (c) 2022-2024 TeamMoeg
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

package com.teammoeg.frostedheart.climate.data;

import com.teammoeg.frostedheart.FHMain;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.util.Direction;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.entity.player.PlayerEvent;

import javax.annotation.Nullable;

public class DeathInventoryData implements ICapabilitySerializable<CompoundNBT> {
    private static class CopyInventory {
        NonNullList<ItemStack> inv = NonNullList.withSize(9, ItemStack.EMPTY);
        NonNullList<ItemStack> armor = NonNullList.withSize(4, ItemStack.EMPTY);
        ItemStack offhand = ItemStack.EMPTY;

        public static CopyInventory deserializeNBT(CompoundNBT nbt) {

            return new CopyInventory(nbt);
        }

        private CopyInventory(CompoundNBT nbt) {
            super();
            ItemStackHelper.loadAllItems(nbt.getCompound("main"), inv);
            ItemStackHelper.loadAllItems(nbt.getCompound("armor"), armor);
            this.offhand = ItemStack.read(nbt.getCompound("off"));

        }

        public CopyInventory(PlayerInventory othis) {
            for (int i = 0; i < 9; i++) {
                ItemStack itemstack = othis.mainInventory.get(i);
                if (!itemstack.isEmpty()) {
                    if (itemstack.isDamageable() || !itemstack.getToolTypes().isEmpty()) {
                        inv.set(i, itemstack);
                        othis.setInventorySlotContents(i, ItemStack.EMPTY);
                    }
                }
            }
            ItemStack offhand = othis.offHandInventory.get(0);
            if (offhand.isDamageable() || !offhand.getToolTypes().isEmpty()) {
                this.offhand = offhand;
                othis.offHandInventory.set(0, ItemStack.EMPTY);
            }
            for (int i = 0; i < 4; i++) {
                armor.set(i, othis.armorInventory.get(i));
                othis.armorInventory.set(i, ItemStack.EMPTY);
            }
        }

        public void restoreInventory(PlayerInventory othis) {
            for (int i = 0; i < 9; i++) {
                ItemStack ret = inv.get(i);
                if (!ret.isEmpty())
                    othis.setInventorySlotContents(i, ret);
            }
            for (int i = 0; i < 4; i++) {
                ItemStack ret = armor.get(i);
                if (!ret.isEmpty())
                    othis.armorInventory.set(i, ret);
            }
            if (offhand != null && !offhand.isEmpty()) {
                othis.offHandInventory.set(0, offhand);
            }
        }

        public CompoundNBT serializeNBT() {
            CompoundNBT cnbto = new CompoundNBT();


            cnbto.put("main", ItemStackHelper.saveAllItems(new CompoundNBT(), inv));
            cnbto.put("armor", ItemStackHelper.saveAllItems(new CompoundNBT(), armor));
            cnbto.put("off", offhand.serializeNBT());
            return cnbto;
        }
    }
    @CapabilityInject(DeathInventoryData.class)
    public static Capability<DeathInventoryData> CAPABILITY;
    public static final ResourceLocation ID = new ResourceLocation(FHMain.MODID, "death_inventory");
    private final LazyOptional<DeathInventoryData> capability;
    CopyInventory inv;

    boolean calledClone = false;

    /**
     * Get ClimateData attached to this world
     *
     * @param world server or client
     * @return An instance of ClimateData exists on the world, otherwise return a new ClimateData instance.
     */
    public static DeathInventoryData get(PlayerEntity world) {

        return getCapability(world).resolve().orElse(null);
    }

    /**
     * Get ClimateData attached to this world
     *
     * @param player server or client
     * @return An instance of ClimateData if data exists on the world, otherwise return empty.
     */
    private static LazyOptional<DeathInventoryData> getCapability(@Nullable PlayerEntity player) {
        if (player != null) {
            return player.getCapability(CAPABILITY);
        }
        return LazyOptional.empty();
    }

    /**
     * Setup capability's serialization to disk.
     */
    public static void setup() {
        CapabilityManager.INSTANCE.register(DeathInventoryData.class, new Capability.IStorage<DeathInventoryData>() {
            public void readNBT(Capability<DeathInventoryData> capability, DeathInventoryData instance, Direction side, INBT nbt) {
                instance.deserializeNBT((CompoundNBT) nbt);
            }

            public INBT writeNBT(Capability<DeathInventoryData> capability, DeathInventoryData instance, Direction side) {
                return instance.serializeNBT();
            }
        }, DeathInventoryData::new);
    }

    public DeathInventoryData() {
        capability = LazyOptional.of(() -> this);
    }

    public void alive(PlayerInventory inv) {
        if (this.inv != null) {
            this.inv.restoreInventory(inv);
            this.inv = null;
        }
    }

    public void calledClone() {
        //System.out.println("called clone event");
        calledClone = true;
    }

    public void copy(DeathInventoryData data) {
        this.inv = data.inv;
    }

    public void death(PlayerInventory inv) {
        this.inv = new CopyInventory(inv);
    }

    @Override
    public void deserializeNBT(CompoundNBT nbt) {
        inv = null;
        calledClone = nbt.getBoolean("cloned");
        if (nbt.contains("data"))
            nbt.getList("data", 10).stream().map(t -> (CompoundNBT) t).forEach(e -> {
                inv = CopyInventory.deserializeNBT(e.getCompound("inv"));
            });
        else if (nbt.contains("inv"))
            inv = CopyInventory.deserializeNBT(nbt.getCompound("inv"));
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        if (cap == CAPABILITY)
            return capability.cast();
        return LazyOptional.empty();
    }

    @Override
    public CompoundNBT serializeNBT() {
        CompoundNBT cnbt = new CompoundNBT();
        if (inv != null)
            cnbt.put("inv", inv.serializeNBT());
        cnbt.putBoolean("cloned", calledClone);
        return cnbt;
    }

    public void startClone() {
        calledClone = false;
    }

    public void tryCallClone(PlayerEntity pe) {
        //System.out.println("Detecting clone event");
        if (!calledClone) {
            //System.out.println("calling clone event");
            MinecraftForge.EVENT_BUS.post(new PlayerEvent.Clone(pe, pe, true));
        }
    }

}
