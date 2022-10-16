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

package com.teammoeg.frostedheart.relic;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.util.SerializeUtil;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.UUID;

/**
 * Call setup() at FHMain
 * Attach to target in AttachCapabilityEvents
 */
public class RelicData implements ICapabilitySerializable<CompoundNBT> {
    @CapabilityInject(RelicData.class)
    public static Capability<RelicData> CAPABILITY;
    private final LazyOptional<RelicData> capability;
    public static final ResourceLocation ID = new ResourceLocation(FHMain.MODID, "relic_data");

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
        // serialze hashmap relics
    }

    public RelicData() {
        capability = LazyOptional.of(() -> this);
    }

    public static void setup() {
        CapabilityManager.INSTANCE.register(RelicData.class, new Capability.IStorage<RelicData>() {
            public INBT writeNBT(Capability<RelicData> capability, RelicData instance, Direction side) {
                return instance.serializeNBT();
            }

            public void readNBT(Capability<RelicData> capability, RelicData instance, Direction side, INBT nbt) {
                instance.deserializeNBT((CompoundNBT) nbt);
            }
        }, RelicData::new);
    }

    private static LazyOptional<RelicData> getCapability(@Nullable IWorld world) {
        if (world instanceof World) {
            return ((World) world).getCapability(CAPABILITY);
        }
        return LazyOptional.empty();
    }

    public static RelicData get(IWorld world) {
        return getCapability(world).resolve().orElse(new RelicData());
    }

    // new hash table with key as player uuid and value as relic position
    private LinkedHashMap<UUID, BlockPos> relics = new LinkedHashMap<>();

    public BlockPos getRelicPos(UUID team) {
        return relics.get(team) == null ? BlockPos.ZERO : relics.get(team);
    }

    // call by the relic block in overworld when first enter
    public void addRelicPos(UUID team) {
        if (!relics.containsKey(team)) {
            int gridSize = 512;
            // this allocates on a 1D-line, each region is 512x512.
            // each allocated region is surrounded by 8 empty regions preventing interaction.
            // the pos is at the center of the region.
            BlockPos pos = new BlockPos(relics.size() * gridSize * 2 + gridSize / 2, 0, gridSize / 2);
            relics.put(team, pos);
        }
    }
}
