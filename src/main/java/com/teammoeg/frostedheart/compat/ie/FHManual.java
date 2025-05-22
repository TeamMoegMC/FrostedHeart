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

package com.teammoeg.frostedheart.compat.ie;

import blusunrize.immersiveengineering.api.ManualHelper;
import blusunrize.immersiveengineering.client.manual.ManualElementMultiblock;
import blusunrize.lib.manual.ManualEntry;
import blusunrize.lib.manual.ManualInstance;
import blusunrize.lib.manual.Tree;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.bootstrap.common.FHMultiblocks;

import net.minecraft.resources.ResourceLocation;

public class FHManual {
    private static Tree.InnerNode<ResourceLocation, ManualEntry> CATEGORY;

    public static void init() {
        ManualInstance man = ManualHelper.getManual();
        CATEGORY = man.getRoot().getOrCreateSubnode(new ResourceLocation(FHMain.MODID, "main"), 110);
        {
            ManualEntry.ManualEntryBuilder builder = new ManualEntry.ManualEntryBuilder(man);

            builder.addSpecialElement(new ManualEntry.SpecialElementData("generator", 0, () -> new ManualElementMultiblock(man, FHMultiblocks.GENERATOR_T1)));
            builder.readFromFile(new ResourceLocation(FHMain.MODID, "generator"));
            man.addEntry(CATEGORY, builder.create(), 0);
        }
        {
            ManualEntry.ManualEntryBuilder builder = new ManualEntry.ManualEntryBuilder(man);
            builder.addSpecialElement(new ManualEntry.SpecialElementData("generator_2", 0, () -> new ManualElementMultiblock(man, FHMultiblocks.GENERATOR_T2)));
            builder.readFromFile(new ResourceLocation(FHMain.MODID, "generator_t2"));
            man.addEntry(CATEGORY, builder.create(), 1);
        }
        {
            ManualEntry.ManualEntryBuilder builder = new ManualEntry.ManualEntryBuilder(man);
            builder.addSpecialElement(new ManualEntry.SpecialElementData("heat_radiator", 0, () -> new ManualElementMultiblock(man, FHMultiblocks.RADIATOR)));
            builder.readFromFile(new ResourceLocation(FHMain.MODID, "heat_radiator"));
            man.addEntry(CATEGORY, builder.create(), 1);
        }
    }
}
