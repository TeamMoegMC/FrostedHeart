package com.teammoeg.frostedheart.loot;

import java.util.List;
import javax.annotation.Nonnull;

import com.google.gson.JsonObject;

import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootContext;
import net.minecraft.loot.RandomValueRange;
import net.minecraft.loot.conditions.ILootCondition;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.loot.GlobalLootModifierSerializer;
import net.minecraftforge.common.loot.LootModifier;

public class ApplyDamageLootModifier extends LootModifier {
	RandomValueRange dmg;
    private ApplyDamageLootModifier(ILootCondition[] conditionsIn,RandomValueRange rv) {
        super(conditionsIn);
        dmg=rv;
    }

    @Nonnull
    @Override
    protected List<ItemStack> doApply(List<ItemStack> generatedLoot, LootContext context) {
        generatedLoot.forEach(e->{
        	if(e.getDamage()==0&&e.isDamageable()) {
        		e.setDamage((int) (e.getMaxDamage()*dmg.generateFloat(context.getRandom())));
        	}
        });
        return generatedLoot;
    }
    public static class Serializer extends GlobalLootModifierSerializer<ApplyDamageLootModifier> {
        @Override
        public ApplyDamageLootModifier read(ResourceLocation location, JsonObject object, ILootCondition[] conditions) {
            return new ApplyDamageLootModifier(conditions,RandomValueRange.of(object.get("min").getAsFloat(),object.get("max").getAsFloat()));
        }

        @Override
        public JsonObject write(ApplyDamageLootModifier instance) {
            JsonObject object = new JsonObject();
            object.addProperty("min",instance.dmg.getMin());
            object.addProperty("max",instance.dmg.getMax());
            return object;
        }
    }
}
