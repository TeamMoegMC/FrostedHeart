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

package com.teammoeg.frostedheart.content.steamenergy.capabilities;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.steamenergy.charger.ChargerTransfer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public class HeatStorageCapability {
    private static final String STEAM_KEY = FHMain.MODID + ":steam";
    int maxstore;
    ItemStack container;
    private final int chargerPrice;

    public HeatStorageCapability(ItemStack stack, int maxstorage) {
        this(stack, maxstorage, 1);
    }

    /** A price above one opts into automatic dock charging instead of the old instant 1:1 hand transfer. */
    public HeatStorageCapability(ItemStack stack, int maxstorage, int chargerPrice) {
        this.container = stack;
        this.maxstore = maxstorage;
        this.chargerPrice = chargerPrice;
    }

    protected float getEnergy() {
        CompoundTag tag = container == null ? null : container.getTag();
        return tag == null ? 0F : tag.getFloat(STEAM_KEY);
    }

    // TODO: Check
    protected void setEnergy(float energy) {
        if (container != null)
            container.getOrCreateTag().putFloat(STEAM_KEY, energy);
    }

    public float getEnergyStored() {
        return getEnergy();
    }

    public boolean usesChargerDock() {
        return chargerPrice > 1;
    }

    public boolean isFull() {
        return preciseEnergy() >= maxstore;
    }

    /** Accept a reachable whole stored point; return only the station points actually billed. */
    public float chargeAtDock(float available) {
        double before = preciseEnergy();
        int after = ChargerTransfer.wholePointsAfterCharge(before, maxstore, chargerPrice, available);
        if (!(after > before)) return 0F;
        setEnergy(after);
        return ChargerTransfer.stationDebit(before, after, chargerPrice);
    }

    private double preciseEnergy() {
        CompoundTag tag = container == null ? null : container.getTag();
        return tag == null ? 0D : tag.getDouble(STEAM_KEY);
    }

    /** More precise timed consumption on the same numeric NBT key; existing float values remain readable. */
    public double extractPreciseEnergy(double requested) {
        double before = preciseEnergy();
        double actual = Math.min(before, requested);
        if (actual > 0D) container.getOrCreateTag().putDouble(STEAM_KEY, before - actual);
        return actual;
    }

    public float receiveEnergy(float value, boolean simulate) {
        float current = getEnergy();
        float actual = Math.min(maxstore - current, value);
        if (!simulate) {
            setEnergy(actual + current);
        }
        return actual;
    }

    public float extractEnergy(float value, boolean simulate) {
        float current = getEnergy();
        float extracted = Math.min(value, current);
        if (!simulate && extracted != 0F) {
            setEnergy(current - extracted);
        }
        return extracted;
    }
}
