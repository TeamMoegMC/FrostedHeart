package com.teammoeg.frostedheart.content.health.capability;

import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.bootstrap.common.FHCapabilities;
import com.teammoeg.frostedheart.content.health.network.PlayerNutritionSyncPacket;
import com.teammoeg.frostedheart.content.health.recipe.NutritionRecipe;
import com.teammoeg.frostedheart.content.water.network.PlayerWaterLevelSyncPacket;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import com.teammoeg.frostedheart.util.io.NBTSerializable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.UUID;

public class NutritionCapability implements NBTSerializable {

    public record Nutrition(float fat , float carbohydrate, float protein , float vegetable){
        public Nutrition(){
            this(0);
        }
        public Nutrition(float v){
            this(v,v,v,v);
        }
        public Nutrition scale(float scale){
            return new Nutrition(fat*scale,carbohydrate*scale,protein*scale,vegetable*scale);
        }
        public Nutrition add(Nutrition nutrition){
            return new Nutrition(fat+nutrition.fat,carbohydrate+nutrition.carbohydrate,protein+nutrition.protein,vegetable+nutrition.vegetable);
        }
        public float getNutritionValue(){
            return fat + carbohydrate + protein + vegetable;
        }
        public boolean isZero(){
            return fat == 0 && carbohydrate == 0 && protein == 0 && vegetable == 0;
        }
    }

    @Override
    public void save(CompoundTag nbt, boolean isPacket) {
        CompoundTag compound = new CompoundTag();
        compound.putFloat("fat", nutrition.fat);
        compound.putFloat("carbohydrate", nutrition.carbohydrate);
        compound.putFloat("protein", nutrition.protein);
        compound.putFloat("vegetable", nutrition.vegetable);
    }

    @Override
    public void load(CompoundTag nbt, boolean isPacket) {
        set(new Nutrition(nbt.getFloat("fat"),nbt.getFloat("carbohydrate"),nbt.getFloat("protein"),nbt.getFloat("vegetable")));
    }

    private Nutrition nutrition = new Nutrition(5000);


    public void addFat(Player player, float add) {
        this.nutrition = new Nutrition(nutrition.fat+add,nutrition.carbohydrate,nutrition.protein,nutrition.vegetable);
        syncToClientOnRestore(player);
    }

    public void addCarbohydrate(Player player, float add) {
        this.nutrition = new Nutrition(nutrition.fat,nutrition.carbohydrate+add,nutrition.protein,nutrition.vegetable);
        syncToClientOnRestore(player);
    }

    public void addProtein(Player player, float add) {
        this.nutrition = new Nutrition(nutrition.fat,nutrition.carbohydrate,nutrition.protein+add,nutrition.vegetable);
        syncToClientOnRestore(player);
    }

    public void addVegetable(Player player, float add) {
        this.nutrition = new Nutrition(nutrition.fat,nutrition.carbohydrate,nutrition.protein,nutrition.vegetable+add);
        syncToClientOnRestore(player);
    }

    public void set(Nutrition temp) {
        this.nutrition = temp;
    }

    public void setFat(float temp) {
        this.nutrition = new Nutrition(temp,nutrition.carbohydrate,nutrition.protein,nutrition.vegetable);
    }

    public void setCarbohydrate(float temp) {
        this.nutrition = new Nutrition(nutrition.fat,temp,nutrition.protein,nutrition.vegetable);
    }

    public void setProtein(float temp) {
        this.nutrition = new Nutrition(nutrition.fat,nutrition.carbohydrate,temp,nutrition.vegetable);
    }

    public void setVegetable(float temp) {
        this.nutrition = new Nutrition(nutrition.fat,nutrition.carbohydrate,nutrition.protein,temp);
    }

    public Nutrition get() {
        return nutrition;
    }

    public static void syncToClient(ServerPlayer player) {
        getCapability(player).ifPresent(t -> FHNetwork.sendPlayer(player, new PlayerNutritionSyncPacket(t.get())));
    }

    public static void syncToClientOnRestore(Player player) {
        if (!player.level().isClientSide()) {
            ServerPlayer serverPlayer = (ServerPlayer) player;
            syncToClient(serverPlayer);
        }
    }

    public void modifyNutrition(Player player, Nutrition nutrition) {
        set(get().add(nutrition));
        syncToClientOnRestore(player);
    }

    public void eat(Player player, ItemStack food) {
        if(!food.isEdible()) return;
        Level level = player.level();
        NutritionRecipe wRecipe = NutritionRecipe.getRecipeFromItem(level, food);
        int nutrition = food.getFoodProperties(player).getNutrition();
        //因为只看食物自己的属性会比较低，加的点数不够一分钟的消耗，所以需要再乘一个系数
        Nutrition n = wRecipe.getNutrition().scale(nutrition).scale(40.0f);
        modifyNutrition(player, n);
    }

    public void consume(Player player) {
        // 这个比例可以放到Config里
        float radio = - 0.1f * FHConfig.SERVER.nutritionConsumptionRate.get();

        Nutrition nutrition = get();
        modifyNutrition(player, nutrition.scale(radio / nutrition.getNutritionValue()));
    }


//    public void award(Player player) {
//
//    }

    public void punishment(Player player) {
        //TODO 营养值过高或过低的惩罚
        int count = 0;

        if(nutrition.fat<3000){
            count++;

        }
        if(nutrition.fat>8000){
            count++;

        }
        if(nutrition.carbohydrate<3000){
            count++;

        }
        if(nutrition.carbohydrate>8000){
            count++;

        }
        if(nutrition.protein<3000){
            count++;

        }
        if(nutrition.protein>8000){
            count++;

        }
        if(nutrition.vegetable<3000){
            count++;

        }
        if(nutrition.vegetable>8000){
            count++;

        }
        int a = count/2;
        if(count>0) {
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 300, count - 1));
        }
        if(a>0) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 300, a));
        }

        // 对生命值上限的修改
        int v = (int) (20 - nutrition.getNutritionValue() / 1000);
        AttributeInstance instance = player.getAttributes().getInstance(Attributes.MAX_HEALTH);
        AttributeModifier modifier = new AttributeModifier(NutritionUUID, "nutrition", -v, AttributeModifier.Operation.ADDITION);
        if(instance.hasModifier(modifier))
            instance.removeModifier(modifier);
        instance.addPermanentModifier(modifier);
    }

    public static final UUID NutritionUUID = UUID.fromString("f3f5f6f7-8f9f-afbf-cfcf-dfdfefeff0f1");

    public static LazyOptional<NutritionCapability> getCapability(@Nullable Player player) {
        return FHCapabilities.PLAYER_NUTRITION.getCapability(player);
    }

    @Nullable
    public static Nutrition getFoodNutrition(Level level,ItemStack food) {
        NutritionRecipe recipe = NutritionRecipe.getRecipeFromItem(level, food);
        if(recipe == null) return null;
        return recipe.getNutrition();
    }
}
