package com.teammoeg.frostedheart.loot;

import com.google.gson.JsonObject;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootContext;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.conditions.ILootCondition;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.loot.GlobalLootModifierSerializer;
import net.minecraftforge.common.loot.LootModifier;
import javax.annotation.Nonnull;
import java.util.List;

public class AddLootModifier extends LootModifier {


    ResourceLocation lt;
    
    private AddLootModifier(ILootCondition[] conditionsIn,ResourceLocation lt) {
        super(conditionsIn);
        this.lt=lt;
    }

    @Nonnull
    @Override
    protected List<ItemStack> doApply(List<ItemStack> generatedLoot, LootContext context) {
    	LootTable loot=context.getLootTable(lt);
    	if(context.addLootTable(loot)) {
    		generatedLoot.addAll(loot.generate(context));
    	}
        return generatedLoot;
    }


    public static class Serializer extends GlobalLootModifierSerializer<AddLootModifier> {
        @Override
        public AddLootModifier read(ResourceLocation location, JsonObject object, ILootCondition[] conditions) {
            return new AddLootModifier(conditions,new ResourceLocation(object.get("loot_table").getAsString()));
        }

        @Override
        public JsonObject write(AddLootModifier instance) {
            JsonObject object = new JsonObject();
            object.addProperty("loot_table",instance.lt.toString());
            return object;
        }
    }
}
