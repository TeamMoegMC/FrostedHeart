/*
 * Copyright (c) 2022 TeamMoeg
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

package com.teammoeg.frostedheart.util;

import com.teammoeg.frostedheart.FHMain;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Call setup() at FHMain
 * Attach to target in AttachCapabilityEvents
 * This example attaches to World
 */
public class TemplateCapabilitySerializable implements ICapabilitySerializable<CompoundNBT> {
    @CapabilityInject(TemplateCapabilitySerializable.class)
    public static Capability<TemplateCapabilitySerializable> CAPABILITY;
    private final LazyOptional<TemplateCapabilitySerializable> capability;
    public static final ResourceLocation ID = new ResourceLocation(FHMain.MODID, "template_capability_serializable");

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        return cap == CAPABILITY ? capability.cast() : LazyOptional.empty();
    }

    @Override
    public CompoundNBT serializeNBT() {
        return null;
    }

    @Override
    public void deserializeNBT(CompoundNBT nbt) {

    }

    public TemplateCapabilitySerializable() {
        capability = LazyOptional.of(() -> this);
    }

    public static void setup() {
        CapabilityManager.INSTANCE.register(TemplateCapabilitySerializable.class, new Capability.IStorage<TemplateCapabilitySerializable>() {
            public INBT writeNBT(Capability<TemplateCapabilitySerializable> capability, TemplateCapabilitySerializable instance, Direction side) {
                return instance.serializeNBT();
            }

            public void readNBT(Capability<TemplateCapabilitySerializable> capability, TemplateCapabilitySerializable instance, Direction side, INBT nbt) {
                instance.deserializeNBT((CompoundNBT) nbt);
            }
        }, TemplateCapabilitySerializable::new);
    }

    private static LazyOptional<TemplateCapabilitySerializable> getCapability(@Nullable IWorld world) {
        if (world instanceof World) {
            return ((World) world).getCapability(CAPABILITY);
        }
        return LazyOptional.empty();
    }

    public static TemplateCapabilitySerializable get(IWorld world) {
        return getCapability(world).resolve().orElse(new TemplateCapabilitySerializable());
    }
}
