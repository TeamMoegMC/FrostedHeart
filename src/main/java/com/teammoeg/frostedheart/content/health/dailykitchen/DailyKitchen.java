/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

/*
 * Copyright (c) 2024 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.teammoeg.frostedheart.content.health.dailykitchen;


import com.teammoeg.frostedheart.bootstrap.common.FHCapabilities;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;

public class DailyKitchen {
    /**
     * This function generates 1-3 foods that player wants to eat.It should be called once every morning(in frostedheart.events.FHCommonEvents.sendForecastMessages).
     * It records how many kinds of foods the player have eaten in wantedFoodCapability(It seems that diet mod doesn't record this), eatenFoodsAmount WON'T be changed until this function is called again. So the player will get same effect in one day.
     */
    public static void generateWantedFood(Player player){
    	WantedFoodCapability wantedFoodCapability = FHCapabilities.WANTED_FOOD.getCapability(player).orElse(null);
    	if(wantedFoodCapability==null)return;
    	WantedFoodsGenerator generator = generateWantedFoods(wantedFoodCapability);
    	if (generator != null) {
    		player.displayClientMessage(generator.getWantedFoodsText(), false);
    	}
    }

    /**
     * 根据玩家已吃过的食物种类生成今日"想吃的菜"。吃过的食物由健康事件处理器
     * （HealthCommonEvents#finishUsingItems）在进食时写入 capability（见 {@link WantedFoodCapability#addEatenFood}）。
     * 未达到生成门槛（吃过的食物种类不足 10 种）时返回 null，调用方不应发送提示消息。
     * <p>
     * Generates today's wanted foods based on the kinds of food the player has eaten.
     * Eaten food kinds are recorded into the capability by the health event handler
     * (HealthCommonEvents#finishUsingItems) when eating (see {@link WantedFoodCapability#addEatenFood}).
     * Returns null when the generation threshold (10 distinct eaten food kinds) is not reached,
     * in which case the caller should not send the message.
     *
     * @param wantedFoodCapability 玩家的每日厨房能力 / the player's daily kitchen capability
     * @return 生成器实例（含生成的候选与提示文本），未达标时返回 null / the generator holding generated
     *         candidates and message text, or null if the threshold is not reached
     */
    static WantedFoodsGenerator generateWantedFoods(WantedFoodCapability wantedFoodCapability){
        int eatenFoodsAmount = wantedFoodCapability.getFoodsEaten().size();
        int wantedFoodsAmount = Math.min(eatenFoodsAmount / 10, 3);
        if(wantedFoodsAmount==0) return null;
        
        wantedFoodCapability.setEatenFoodsAmount(eatenFoodsAmount);

        WantedFoodsGenerator generator = new WantedFoodsGenerator(wantedFoodCapability.getFoodsEaten(), eatenFoodsAmount);

        wantedFoodCapability.setWantedFoods(generator.generate());
        return generator;
    }


    public static void tryGiveBenefits(LivingEntityUseItemEvent.Finish event){
        if (event.getEntity() != null && !event.getEntity().level().isClientSide && event.getEntity() instanceof ServerPlayer) {
            Benefits benefits = new Benefits((ServerPlayer) event.getEntity());
            benefits.tryGive(event.getItem());
        }

    }
}




