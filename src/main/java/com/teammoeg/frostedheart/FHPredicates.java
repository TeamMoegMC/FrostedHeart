package com.teammoeg.frostedheart;

import com.teammoeg.frostedheart.util.utility.BlackListPredicate;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.resources.ResourceLocation;

import static com.teammoeg.frostedheart.FHMain.MODID;

public class FHPredicates {
    public static void init() {
        ItemPredicate.register(new ResourceLocation(MODID, "blacklist"), BlackListPredicate::new);
    }
}
