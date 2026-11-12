package com.teammoeg.frostedresearch.knowledge;

import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.minecraftforge.registries.ForgeRegistries;

/** Reload-time fallback for the dedicated prototype product when its project pack is absent. */
public final class TemporaryResearchRecipes {
    private static Set<ResourceLocation> prototypeRecipes=Set.of();
    private TemporaryResearchRecipes() {}
    public static Set<ResourceLocation> managed() { return prototypeRecipes; }
    public static void clear() { prototypeRecipes=Set.of(); }
    public static void refresh() {
        var server=ServerLifecycleHooks.getCurrentServer();if(server==null) return;
        Set<ResourceLocation> ids=new HashSet<>();
        for(var recipe:server.getRecipeManager().getRecipes()) {
            var result=recipe.getResultItem(server.registryAccess());
            if(new ResourceLocation("frostedresearch:prototype_tuyere").equals(ForgeRegistries.ITEMS.getKey(result.getItem()))) ids.add(recipe.getId());
        }
        prototypeRecipes=Set.copyOf(ids);TechnologyAccessResolver.invalidateAll();
    }
}
