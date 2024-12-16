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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

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
        Level level = player.level();
        NutritionRecipe wRecipe = NutritionRecipe.getRecipeFromItem(level, food);
        int nutrition = food.getFoodProperties(player).getNutrition();
        modifyNutrition(player, wRecipe.getNutrition().scale(nutrition));
    }

    public void consume(Player player) {
        // 这个比例可以放到Config里
        float radio = 0.1f * FHConfig.SERVER.nutritionConsumptionRate.get();

        Nutrition nutrition = get();
        modifyNutrition(player, nutrition.scale(radio / nutrition.getNutritionValue()));
    }


    public void award(Player player) {
        //TODO
    }

    public void punishment(Player player) {
        //TODO 营养值过高或过低的惩罚
        if(nutrition.fat<3000){

        }
        if(nutrition.fat>8000){

        }
        if(nutrition.carbohydrate<3000){

        }
        if(nutrition.carbohydrate>8000){

        }
        if(nutrition.protein<3000){

        }
        if(nutrition.protein>8000){

        }
        if(nutrition.vegetable<3000){

        }
        if(nutrition.vegetable>8000){

        }
    }

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
