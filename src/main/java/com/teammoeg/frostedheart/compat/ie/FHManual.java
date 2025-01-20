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
    }
}
