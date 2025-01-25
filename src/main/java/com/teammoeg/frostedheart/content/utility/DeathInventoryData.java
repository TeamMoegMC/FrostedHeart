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

package com.teammoeg.frostedheart.content.utility;

import javax.annotation.Nullable;

import com.teammoeg.chorda.io.NBTSerializable;
import com.teammoeg.frostedheart.bootstrap.common.FHCapabilities;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.NonNullList;
import net.minecraftforge.common.MinecraftForge;
import net.minecraft.nbt.Tag;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.entity.player.PlayerEvent;

public class DeathInventoryData implements NBTSerializable {
    private static class CopyInventory {
        NonNullList<ItemStack> inv = NonNullList.withSize(9, ItemStack.EMPTY);
        NonNullList<ItemStack> armor = NonNullList.withSize(4, ItemStack.EMPTY);
        ItemStack offhand = ItemStack.EMPTY;

        public static CopyInventory deserializeNBT(CompoundTag nbt) {

            return new CopyInventory(nbt);
        }

        private CopyInventory(CompoundTag nbt) {
            super();
            ContainerHelper.loadAllItems(nbt.getCompound("main"), inv);
            ContainerHelper.loadAllItems(nbt.getCompound("armor"), armor);
            this.offhand = ItemStack.of(nbt.getCompound("off"));

        }

        public CopyInventory(Inventory othis) {
            for (int i = 0; i < 9; i++) {
                ItemStack itemstack = othis.items.get(i);
                if (!itemstack.isEmpty()) {
                    if (itemstack.isDamageableItem() || itemstack.getMaxStackSize()==1) {
                        inv.set(i, itemstack);
                        othis.setItem(i, ItemStack.EMPTY);
                    }
                }
            }
            ItemStack offhand = othis.offhand.get(0);
            if (offhand.isDamageableItem() || offhand.getMaxStackSize()==1) {
                this.offhand = offhand;
                othis.offhand.set(0, ItemStack.EMPTY);
            }
            for (int i = 0; i < 4; i++) {
                armor.set(i, othis.armor.get(i));
                othis.armor.set(i, ItemStack.EMPTY);
            }
        }

        public void restoreInventory(Inventory othis) {
            for (int i = 0; i < 9; i++) {
                ItemStack ret = inv.get(i);
                if (!ret.isEmpty())
                    othis.setItem(i, ret);
            }
            for (int i = 0; i < 4; i++) {
                ItemStack ret = armor.get(i);
                if (!ret.isEmpty())
                    othis.armor.set(i, ret);
            }
            if (offhand != null && !offhand.isEmpty()) {
                othis.offhand.set(0, offhand);
            }
        }

        public CompoundTag serializeNBT() {
            CompoundTag cnbto = new CompoundTag();


            cnbto.put("main", ContainerHelper.saveAllItems(new CompoundTag(), inv));
            cnbto.put("armor", ContainerHelper.saveAllItems(new CompoundTag(), armor));
            cnbto.put("off", offhand.serializeNBT());
            return cnbto;
        }
    }
    CopyInventory inv;

    boolean calledClone = false;

    /**
     * Get ClimateData attached to this world
     *
     * @param world server or client
     * @return An instance of ClimateData exists on the world, otherwise return a new ClimateData instance.
     */
    public static DeathInventoryData get(Player world) {

        return getCapability(world).resolve().orElse(null);
    }

    /**
     * Get ClimateData attached to this world
     *
     * @param player server or client
     * @return An instance of ClimateData if data exists on the world, otherwise return empty.
     */
    private static LazyOptional<DeathInventoryData> getCapability(@Nullable Player player) {
    	return FHCapabilities.DEATH_INV.getCapability(player);
    }

    public DeathInventoryData() {
    }

    public void alive(Inventory inv) {
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

    public void death(Inventory inv) {
        this.inv = new CopyInventory(inv);
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {

    }

    public void startClone() {
        calledClone = false;
    }

    public void tryCallClone(Player pe) {
        //System.out.println("Detecting clone event");
        if (!calledClone) {
            //System.out.println("calling clone event");
            MinecraftForge.EVENT_BUS.post(new PlayerEvent.Clone(pe, pe, true));
        }
    }

	@Override
	public void save(CompoundTag cnbt, boolean isPacket) {
        if (inv != null)
            cnbt.put("inv", inv.serializeNBT());
        cnbt.putBoolean("cloned", calledClone);
	}

	@Override
	public void load(CompoundTag nbt, boolean isPacket) {
        inv = null;
        calledClone = nbt.getBoolean("cloned");
        if (nbt.contains("data")) {
            nbt.getList("data", Tag.TAG_COMPOUND).stream().map(t -> (CompoundTag) t).findFirst().ifPresent(e-> inv = CopyInventory.deserializeNBT(e.getCompound("inv")));
        } else if (nbt.contains("inv")) {
            inv = CopyInventory.deserializeNBT(nbt.getCompound("inv"));
        }
	}

}
