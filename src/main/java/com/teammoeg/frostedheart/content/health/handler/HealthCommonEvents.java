package com.teammoeg.frostedheart.content.health.handler;

import com.teammoeg.frostedheart.FHCapabilities;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.FHTags;
import com.teammoeg.frostedheart.content.health.dailykitchen.DailyKitchen;
import com.teammoeg.frostedheart.content.research.insight.InsightHandler;
import com.teammoeg.frostedheart.content.utility.transportation.MovementModificationHandler;
import com.teammoeg.frostedheart.util.lang.Lang;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class HealthCommonEvents {
    @SubscribeEvent
    public static void attachToPlayer(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof ServerPlayer) {//server-side only capabilities
            ServerPlayer player = (ServerPlayer) event.getObject();
            if (!(player instanceof FakePlayer)) {
                event.addCapability(new ResourceLocation(FHMain.MODID, "wanted_food"    ), FHCapabilities.WANTED_FOOD.provider());
            }
        }
        //Common capabilities
        event.addCapability(new ResourceLocation(FHMain.MODID, "nutrition"  ), FHCapabilities.PLAYER_NUTRITION.provider());
    }

    @SubscribeEvent
    public static void finishUsingItems(LivingEntityUseItemEvent.Finish event) {
        DailyKitchen.tryGiveBenefits(event);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        InsightHandler.onPlayerTick(event);
    }

    @SubscribeEvent
    public static void punishEatingRawMeat(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity() != null && !event.getEntity().level().isClientSide
                && event.getEntity() instanceof ServerPlayer
                && ForgeRegistries.ITEMS.getHolder(event.getItem().getItem()).get().is(FHTags.Items.RAW_FOOD.tag)) {
            ServerPlayer player = (ServerPlayer) event.getEntity();
            player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 400, 1));
            player.displayClientMessage(Lang.translateKey("message.frostedheart.eaten_poisonous_food"), false);
        }
    }
}
