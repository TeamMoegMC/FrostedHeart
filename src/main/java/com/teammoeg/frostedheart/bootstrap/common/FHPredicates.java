package com.teammoeg.frostedheart.bootstrap.common;

import static com.teammoeg.frostedheart.FHMain.*;

import com.teammoeg.frostedheart.util.utility.BlackListPredicate;

import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.resources.ResourceLocation;

public class FHPredicates {
    public static void init() {
        ItemPredicate.register(new ResourceLocation(MODID, "blacklist"), BlackListPredicate::new);
    }
}
