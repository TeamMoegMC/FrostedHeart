package com.teammoeg.frostedheart.mixin.minecraft;

import com.teammoeg.frostedheart.climate.ClimateData;
import com.teammoeg.frostedheart.network.FHClimatePacket;
import com.teammoeg.frostedheart.network.PacketHandler;
import net.minecraft.profiler.IProfiler;
import net.minecraft.util.RegistryKey;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.IServerWorldInfo;
import net.minecraft.world.storage.ISpawnWorldInfo;
import net.minecraftforge.fml.network.PacketDistributor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

@Mixin(ServerWorld.class)
public abstract class MixinServerWorld extends World {

	@Shadow @Final public IServerWorldInfo serverWorldInfo;

	protected MixinServerWorld(ISpawnWorldInfo worldInfo, RegistryKey<World> dimension, DimensionType dimensionType,
							   Supplier<IProfiler> profiler, boolean isRemote, boolean isDebug, long seed) {
		super(worldInfo, dimension, dimensionType, profiler, isRemote, isDebug, seed);
	}
    /**
     * @author khjxiaogu
     * @reason Not allow sleep over weather
     */
	@Overwrite
	private void resetRainAndThunder() {
		
	}

	/**
	 * @author yuesha-yc
	 * @reason this allows us to add our own weather logic since we disabled it
	 * @see GameRulesMixin#disableWeatherCycle
	 */
	@Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/DimensionType;hasSkyLight()Z"))
	private void tick(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
		final ClimateData cap = this.getCapability(ClimateData.CAPABILITY,null).orElseThrow(() -> new IllegalStateException("Expected Climate Data to exist on World " + this.getDimensionKey() + " / " + this.getDimensionType()));

		// vanilla weather params
		int clearTime = this.serverWorldInfo.getClearWeatherTime();
		int thunderTime = this.serverWorldInfo.getThunderTime();
		int rainTime = this.serverWorldInfo.getRainTime();
		boolean isThundering = this.worldInfo.isThundering();
		boolean isRaining = this.worldInfo.isRaining();

		// twr weather params
		int blizzardTime = cap.getBlizzardTime();
		boolean isBlizzard = cap.isBlizzard();

		// if currently is set to clear
		if (clearTime > 0) {
			// decrement clear time
			--clearTime;
			// disable rain or thunder
			thunderTime = isThundering ? 0 : 1;
			rainTime = isRaining ? 0 : 1;
			isThundering = false;
			isRaining = false;
			// disable blizzard
			blizzardTime = isBlizzard ? 0 : 1;
			isBlizzard = false;
		}
		// if currently is not clear
		else {
			// not thundering, time til next thunder is set already
			if (thunderTime > 0) {
				// decrement time til thunder
				--thunderTime;
				// starts thunder when time counts to zero
				if (thunderTime == 0) {
					isThundering = !isThundering;
				}
			} else if (isThundering) {
				// is thundering, set the time til next thunder
				thunderTime = this.rand.nextInt(12000) + 3600;
			} else {
				// is not thundering now, but time til next thunder is not set, set it
				thunderTime = this.rand.nextInt(168000) + 12000;
			}

			// same as thunder logic
			if (rainTime > 0) {
				--rainTime;
				if (rainTime == 0) {
					isRaining = !isRaining;
				}
			} else if (isRaining) {
				rainTime = this.rand.nextInt(12000) + 12000;
			} else {
				rainTime = this.rand.nextInt(168000) + 12000;
			}

			// twr specific blizzard logic
			if (blizzardTime > 0) {
				--blizzardTime;
				if (blizzardTime == 0) {
					isBlizzard = !isBlizzard;
				}
			} else if (isBlizzard) {
				blizzardTime = this.rand.nextInt(12000) + 12000;
			} else {
				blizzardTime = this.rand.nextInt(168000) + 12000;
			}
		}

		this.serverWorldInfo.setThunderTime(thunderTime);
		this.serverWorldInfo.setRainTime(rainTime);
		this.serverWorldInfo.setClearWeatherTime(clearTime);
		this.serverWorldInfo.setThundering(isThundering);
		this.serverWorldInfo.setRaining(isRaining);

		// check if need to sync to client
		if (cap.isBlizzard() != isBlizzard || cap.getBlizzardTime() != blizzardTime) {
			PacketHandler.send(PacketDistributor.DIMENSION.with(() -> this.getDimensionKey()), new FHClimatePacket(cap));
		}

		// server side change
		cap.setBlizzard(isBlizzard);
		cap.setBlizzardTime(blizzardTime);

	}

}
