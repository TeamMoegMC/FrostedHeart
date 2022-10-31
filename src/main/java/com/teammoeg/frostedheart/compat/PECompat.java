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

package com.teammoeg.frostedheart.compat;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;

import moze_intel.projecte.api.mapper.EMCMapper;
import moze_intel.projecte.api.mapper.IEMCMapper;
import moze_intel.projecte.api.mapper.collector.IMappingCollector;
import moze_intel.projecte.api.nss.NSSItem;
import moze_intel.projecte.api.nss.NormalizedSimpleStack;
import net.minecraft.item.Item;
import net.minecraft.resources.DataPackRegistries;
import net.minecraft.resources.IResourceManager;
import net.minecraftforge.registries.ForgeRegistries;

@EMCMapper
public class PECompat implements IEMCMapper<NormalizedSimpleStack, Long> {

    @Override
    public void addMappings(IMappingCollector<NormalizedSimpleStack, Long> arg0, CommentedFileConfig arg1,
                            DataPackRegistries arg2, IResourceManager arg3) {
        for (Item i : ForgeRegistries.ITEMS.getValues())
            arg0.setValueBefore(NSSItem.createItem(i), 0L);
    }

    @Override
    public String getDescription() {
        return "projecte compat";
    }

    @Override
    public String getName() {
        return "projectec";
    }

}
