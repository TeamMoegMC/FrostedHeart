package com.teammoeg.frostedheart.loot;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.LootContext;
import net.minecraft.loot.conditions.ILootCondition;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.loot.GlobalLootModifierSerializer;
import net.minecraftforge.common.loot.LootModifier;
import net.minecraftforge.registries.ForgeRegistries;

public class DechantLootModifier extends LootModifier {
	List<Enchantment> removed=new ArrayList<>();
    private DechantLootModifier(ILootCondition[] conditionsIn,Collection<ResourceLocation> pairsin) {
        super(conditionsIn);
        pairsin.stream().map(ForgeRegistries.ENCHANTMENTS::getValue).forEach(removed::add);
    }

    @Nonnull
    @Override
    protected List<ItemStack> doApply(List<ItemStack> generatedLoot, LootContext context) {
        generatedLoot.forEach(e->doRemove(e,context));
        return generatedLoot;
    }
    private void doRemove(ItemStack orig,LootContext context) {
    	Map<Enchantment, Integer> enchs=EnchantmentHelper.getEnchantments(orig);
    	orig.removeChildTag("Enchantments");
    	orig.removeChildTag("StoredEnchantments");
    	enchs.keySet().removeIf(removed::contains);
    	if(enchs.size()<0&&orig.getItem()==Items.ENCHANTED_BOOK) {
    		EnchantmentHelper.addRandomEnchantment(context.getRandom(),orig,1,false);
    	}
    	EnchantmentHelper.setEnchantments(enchs,orig);
    }
    public static class Serializer extends GlobalLootModifierSerializer<DechantLootModifier> {
        @Override
        public DechantLootModifier read(ResourceLocation location, JsonObject object, ILootCondition[] conditions) {
        	JsonArray ja=object.get("removed").getAsJsonArray();
        	List<ResourceLocation> changes=new ArrayList<>();
        	for(JsonElement je:ja) {
        		changes.add(new ResourceLocation(je.getAsString()));
        	}
            return new DechantLootModifier(conditions,changes);
        }

        @Override
        public JsonObject write(DechantLootModifier instance) {
            JsonObject object = new JsonObject();
            JsonArray removed=new JsonArray();
            instance.removed.stream().map(ForgeRegistries.ENCHANTMENTS::getKey).map(ResourceLocation::toString).forEach(removed::add);
            object.add("removed",removed);
            return object;
        }
    }
}
