package com.teammoeg.frostedheart.mixin.primalwinter;

import com.alcatrazescapee.primalwinter.util.WeatherData;
import net.minecraft.world.GameRules;
import net.minecraft.world.server.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WeatherData.class)
public class MixinWeatherData {

//    @Final
//    @Shadow
//    public static final Capability<WeatherData> CAPABILITY = Helpers.notNull();
//    @Shadow
//    private boolean alreadySetWorldToWinter;
//    /**
//     * @author yuesha-yc
//     */
//    @Overwrite
//    public static void trySetEndlessStorm(ServerWorld world) {
//        final WeatherData cap = world.getCapability(CAPABILITY).orElseThrow(() -> new IllegalStateException("Expected WeatherData to exist on World " + world.getDimensionKey() + " / " + world.getDimensionType()));
//        if (!cap.alreadySetWorldToWinter) {
//            cap.alreadySetWorldToWinter = true;
//            if (Config.COMMON.isWinterDimension(world.getDimensionKey().getLocation())) {
//                world.getGameRules().get(GameRules.DO_WEATHER_CYCLE).set(false, world.getServer());
//                world.setWeather(0, Integer.MAX_VALUE, true, true);
//            }
//        }
//    }

//    @Inject(method = "trySetEndlessStorm(Lnet/minecraft/world/server/ServerWorld;)V", at = @At(value = "FIELD", target = "Lcom/alcatrazescapee/primalwinter/util/WeatherData;alreadySetWorldToWinter:Z", ordinal = 1), cancellable = true)
//    @Inject(method = "trySetEndlessStorm(Lnet/minecraft/world/server/ServerWorld;)V", at = @At(value = "HEAD"), cancellable = true)
//    @Inject(method = "trySetEndlessStorm(Lnet/minecraft/world/server/ServerWorld;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getGameRules()Lnet/minecraft/world/GameRules;"), cancellable = true)
    @Inject(method = "trySetEndlessStorm(Lnet/minecraft/world/server/ServerWorld;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/server/ServerWorld;getGameRules()Lnet/minecraft/world/GameRules;"), cancellable = true)
    private static void frostedheart$trySetEndlessStorm(ServerWorld world, CallbackInfo ci) {
        world.getGameRules().get(GameRules.DO_WEATHER_CYCLE).set(true, world.getServer());
        world.setWeather(10000, Integer.MAX_VALUE, false, false);
        ci.cancel();
    }
}
