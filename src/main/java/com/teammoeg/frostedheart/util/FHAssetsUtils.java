package com.teammoeg.frostedheart.util;

import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateItemModelProvider;
import com.tterrag.registrate.util.nullness.NonNullBiConsumer;
import net.minecraft.world.item.Item;

public class FHAssetsUtils {
    
    public static <T extends Item> NonNullBiConsumer<DataGenContext<Item, T>, RegistrateItemModelProvider> handheld(){
        return (ctx, prov) -> prov.handheld(ctx::getEntry);
    }
    
    
}
