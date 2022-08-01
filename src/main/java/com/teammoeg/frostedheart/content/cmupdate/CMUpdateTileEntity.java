/*
 * Copyright (c) 2021 TeamMoeg
 *
 * This file is part of Steam Powered.
 *
 * Steam Powered is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Steam Powered is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Steam Powered. If not, see <https://www.gnu.org/licenses/>.
 */

package com.teammoeg.frostedheart.content.cmupdate;

import blusunrize.immersiveengineering.common.blocks.IEBaseTileEntity;
import com.teammoeg.frostedheart.FHTileTypes;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;

public class CMUpdateTileEntity extends IEBaseTileEntity implements ITickableTileEntity {
    ResourceLocation registry;
    static Map<String, ResourceLocation> registries = new HashMap<>();
    static Map<String, String> mids = new HashMap<>();

    public static void addReplace(String name, String rl) {
        registries.put(name, new ResourceLocation(rl));
    }

    public static void addModidReplace(String oldid, String newid) {
        mids.put(oldid, newid);
    }

    static {
        addModidReplace("the_winter_rescue", "steampowered");
        addReplace("the_winter_rescue:electrolyzer", "immersiveindustry:electrolyzer");
    }

    public static ResourceLocation getReplacement(String old) {
        ResourceLocation sp = registries.get(old);
        if (sp == null) {
            ResourceLocation rlo = new ResourceLocation(old);
            return new ResourceLocation(mids.get(rlo.getNamespace()), rlo.getPath());
        }
        return sp;
    }

    public CMUpdateTileEntity() {
        super(FHTileTypes.OIL_BURNER.get());
    }

    @Override
    public void readCustomNBT(CompoundNBT nbt, boolean dp) {
        registry = getReplacement(nbt.getString("machineID"));
    }

    @Override
    public void writeCustomNBT(CompoundNBT nbt, boolean dp) {
    }


    @Override
    public void tick() {
        if (this.world != null && !this.world.isRemote) {
            this.world.setBlockState(this.pos, ForgeRegistries.BLOCKS.getValue(registry).getDefaultState());
        }
    }

}
