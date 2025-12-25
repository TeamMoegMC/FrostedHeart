package com.teammoeg.frostedheart.content.agriculture;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.bootstrap.common.FHItems;
import com.teammoeg.frostedheart.bootstrap.common.FHMobEffects;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import com.teammoeg.frostedheart.util.Lang;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FHMain.MODID)
public class SneakingPoopMechanic {

    private static final int EFFECT_DURATION = 12000; // 10 minutes (20 ticks * 60 seconds * 10 minutes)
    private static final int POOP_DURATION = 400;
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (false&&event.phase == TickEvent.Phase.END) {
            Player player = event.player;
            Level level = player.level();

            // Only process on server side
            if (level.isClientSide()) {
                return;
            }

            // Check if player is sneaking
            if (FHConfig.SERVER.enablePlayerPooping.get() && !player.isCreative() && !player.isSpectator() && player.isShiftKeyDown()) {
                // First check if player already has the Refreshed effect
                // If yes, don't allow pooping
                if (player.hasEffect(FHMobEffects.REFRESHED.get())) {
                    return;
                }

                // Player is sneaking and doesn't have Refreshed effect, try to poop
                FoodData foodData = player.getFoodData();

                // Check if hunger is full (20 food points), and every 5 second on average
                if (foodData.getFoodLevel() >= 20 && player.getRandom().nextInt(POOP_DURATION) == 0) {
                    // Poop successful

                    // Generate random amount (1-3) of night soil
                    int itemCount = player.getRandom().nextInt(3) + 1; // Random between 1-3

                    // Spawn the items
                    for (int i = 0; i < itemCount; i++) {
                        ItemStack nightSoil = new ItemStack(FHItems.night_soil.get());
                        player.spawnAtLocation(nightSoil);
                    }

                    // Apply Refreshed effect
                    player.addEffect(new MobEffectInstance(
                            FHMobEffects.REFRESHED.get(),  // Our custom effect
                            EFFECT_DURATION,             // Duration (10 minutes)
                            0,                           // Amplifier (0 = level I)
                            false,                       // Is ambient
                            false,                       // Show particles
                            true                         // Show icon
                    ));

                    // Add some sound effect
                    player.playSound(SoundEvents.BUCKET_EMPTY, 1.0F, 1.0F);

                    player.displayClientMessage(Lang.message("refreshed").component(), true);
                }
            }
        }
    }
}
