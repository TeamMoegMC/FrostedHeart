package com.teammoeg.frostedheart.content.foods.DailyKitchen;

import java.util.Random;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.client.util.GuiUtils;
import com.teammoeg.frostedheart.research.inspire.EnergyCore;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;

public class Benefits {
    private final Effect[] basicEffects = new Effect[]{Effects.STRENGTH, Effects.SPEED, Effects.HASTE};
    private final ServerPlayerEntity player;
    private final WantedFoodCapability capability;
    private final int eatenFoodsAmount;
    private final int benefitLevel;
    private final Random random = new Random();
    private final int basicEffectDuration;

    public Benefits(ServerPlayerEntity player){
        this.player = player;
        this.capability = (WantedFoodCapability)player.getCapability(DailyKitchen.WANTED_FOOD_CAPABILITY).orElse(new WantedFoodCapability());
        this.eatenFoodsAmount = capability.getEatenFoodsAmount();
        this.benefitLevel = Math.min((eatenFoodsAmount/10), 7);
        this.basicEffectDuration = Math.min(3600+150*eatenFoodsAmount, 20000);//can't be more than one day
    }


    public void giveEnergy(){
        EnergyCore.addEnergy(player, (int)((5000+EnergyCore.getEnergy(player))*Math.min((float)eatenFoodsAmount/200, 0.5)));
    }

    /**only used when amount<3
     * if amount = 3, use giveBasicEffects(int amount, int[] potionLevel)
     */
    private void giveBasicEffects(int amount){
        if(amount == 1){
            player.addPotionEffect(new EffectInstance(basicEffects[random.nextInt(2)], basicEffectDuration, 0));

        }
        else if(amount == 2) {
            int notGiveEffect = random.nextInt(2);
            for (int i = 0; i < 2; i++) {
                if (i != notGiveEffect) {
                    player.addPotionEffect(new EffectInstance(basicEffects[i], basicEffectDuration, 0));
                }
            }
        }
    }

    private void giveBasicEffects(int amount, int[] potionLevel){
        if(amount <3) {
            this.giveBasicEffects(amount);
        }
        else if(amount == 3)
            for(int i=0; i<3; i++){
                player.addPotionEffect(new EffectInstance(basicEffects[i], basicEffectDuration, potionLevel[i]));
            }
        else FHMain.LOGGER.error("Invalid effect amount input!");
    }

    private void giveHealthRegen(int duration){
        this.player.addPotionEffect(new EffectInstance(Effects.REGENERATION, duration));
    }


    private void giveEffects(){
        switch (benefitLevel){
            case 0: {
                this.giveBasicEffects(1);
                break;
            }
            case 1: {
                this.giveBasicEffects(1);
                this.giveHealthRegen(60);
                break;
            }
            case 2:{
                this.giveBasicEffects(2);
                this.giveHealthRegen(60);
                break;
            }
            case 3:{
                this.giveBasicEffects(2);
                this.giveHealthRegen(100);
                break;
            }
            case 4:{
                this.giveBasicEffects(3, new int[]{0, 0, 0});
                this.giveHealthRegen(100);
                break;
            }
            case 5:{
                this.giveBasicEffects(3, new int[]{0, 1, 0});
                this.giveHealthRegen(120);
                break;
            }
            case 6:{
                this.giveBasicEffects(3, new int[]{0, 1, 1});
                this.giveHealthRegen(160);
                break;
            }
            case 7: {
                this.giveBasicEffects(3, new int[]{0, 1, 1});
                this.giveHealthRegen(200);
                break;
            }
        }
    }

    public void give(){
        this.giveEffects();
        this.giveEnergy();
        capability.countEatenTimes();

        player.sendStatusMessage(GuiUtils.translateMessage("eat_wanted_food"), false);
    }

    public void tryGive(Item food){
        if(capability.getWantedFoods().contains(food) && capability.getEatenTimes() < 4){
            this.give();
        }
    }
}

