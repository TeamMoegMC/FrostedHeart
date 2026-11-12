package com.teammoeg.frostedresearch.knowledge.furnace;
import com.teammoeg.frostedresearch.knowledge.IdeaAcquisitionService;
import com.teammoeg.frostedresearch.knowledge.definition.*;
import com.teammoeg.frostedresearch.knowledge.model.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;

/** One reload pass; no recursion, per-tick scan or registry tag mutation. */
public final class RefractoryObservationIndex {
    public static final ResourceLocation EVENT=new ResourceLocation("frostedresearch:refractory_component_observed");
    private static final ResourceLocation USE=new ResourceLocation("frostedresearch:refractory_components");
    public static final ResourceLocation BRICKS=new ResourceLocation("the_winter_rescue:research/refractory_bricks");
    public static final ResourceLocation EXTRA=new ResourceLocation("the_winter_rescue:research/refractory_products_extra");
    private static Set<ResourceLocation> items=Set.of(),blocks=Set.of();
    private RefractoryObservationIndex(){}
    public static Set<ResourceLocation> items(){return items;}
    public static Set<ResourceLocation> blocks(){return blocks;}
    public static boolean matches(Observation o){return (o.type()==Observation.Type.ITEM ? items : o.type()==Observation.Type.BLOCK ? blocks : Set.<ResourceLocation>of()).contains(o.object());}
    public static void observed(ServerPlayer p,Observation o){
        List<IdeaAcquisitionService.Signal> signals=new ArrayList<>();
        signals.add(new IdeaAcquisitionService.Signal(IdeaAcquisitionService.OBSERVATION_COMPLETED,KnowledgeElement.observation(o)));
        if(matches(o))signals.add(new IdeaAcquisitionService.Signal(EVENT,KnowledgeElement.observation(o)));
        // M2: one physical operation rolls each idea once, even when two entrances match.
        IdeaAcquisitionService.fire(com.teammoeg.frostedresearch.knowledge.KnowledgeService.forPlayer(p),signals,p.getRandom()::nextDouble);
    }
    public static void clear(){items=blocks=Set.of();KnowledgeDefinitions.unregisterObservationUse(USE);}
    public static void refresh(MinecraftServer server){
        var brickTag=TagKey.create(Registries.ITEM,BRICKS);
        List<ItemStack> bricks=new ArrayList<>();for(Item item:ForgeRegistries.ITEMS)if(item.builtInRegistryHolder().is(brickTag))bricks.add(new ItemStack(item));
        Set<ResourceLocation> foundItems=new HashSet<>(),foundBlocks=new HashSet<>();
        for(Recipe<?> recipe:server.getRecipeManager().getRecipes()){
            if(!(recipe instanceof CraftingRecipe))continue;
            boolean match=recipe.getIngredients().stream().anyMatch(ingredient -> bricks.stream().anyMatch(ingredient::test));
            if(!match)continue;
            ItemStack result=recipe.getResultItem(server.registryAccess());if(result.isEmpty())continue;
            foundItems.add(ForgeRegistries.ITEMS.getKey(result.getItem()));
            if(result.getItem() instanceof BlockItem block)foundBlocks.add(ForgeRegistries.BLOCKS.getKey(block.getBlock()));
        }
        var extraItems=TagKey.create(Registries.ITEM,EXTRA);var extraBlocks=TagKey.create(Registries.BLOCK,EXTRA);
        for(Item item:ForgeRegistries.ITEMS)if(item.builtInRegistryHolder().is(extraItems))foundItems.add(ForgeRegistries.ITEMS.getKey(item));
        for(var block:ForgeRegistries.BLOCKS)if(block.builtInRegistryHolder().is(extraBlocks))foundBlocks.add(ForgeRegistries.BLOCKS.getKey(block));
        boolean changed=!items.equals(foundItems) || !blocks.equals(foundBlocks) || !KnowledgeDefinitions.current().observationUses().containsKey(USE);
        items=Set.copyOf(foundItems);blocks=Set.copyOf(foundBlocks);
        if(!changed)return;
        KnowledgeDefinitions.registerObservationUse(USE,new ObservationUse(){
            @Override public boolean matches(Observation o){return RefractoryObservationIndex.matches(o);}
            @Override public Optional<Boolean> equivalent(Observation a,Observation b){return Optional.of(a.type()==b.type() && a.object().equals(b.object()));}
        });
    }
}
