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

package com.teammoeg.frostedheart.content.climate.event;

import com.teammoeg.frostedheart.*;
import com.teammoeg.chorda.dataholders.team.CTeamDataManager;
import com.teammoeg.chorda.dataholders.team.TeamDataHolder;
import com.teammoeg.chorda.math.CMath;
import com.teammoeg.frostedheart.bootstrap.common.FHSpecialDataTypes;
import com.teammoeg.frostedheart.bootstrap.reference.FHTags;
import com.teammoeg.frostedheart.bootstrap.common.FHAttributes;
import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.bootstrap.common.FHCapabilities;
import com.teammoeg.frostedheart.content.climate.ForecastHandler;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.data.ArmorTempData;
import com.teammoeg.frostedheart.content.climate.food.FoodTemperatureHandler;
import com.teammoeg.frostedheart.content.climate.gamedata.climate.WorldClimate;
import com.teammoeg.frostedheart.content.climate.network.FHClimatePacket;
import com.teammoeg.frostedheart.content.climate.player.BodyPartData;
import com.teammoeg.frostedheart.content.climate.player.ClothData;
import com.teammoeg.frostedheart.content.climate.player.EquipmentSlotType;
import com.teammoeg.frostedheart.content.climate.player.EquipmentSlotType.SlotKey;
import com.teammoeg.frostedheart.content.climate.player.PlayerTemperatureData;
import com.teammoeg.frostedheart.content.climate.player.PlayerTemperatureData.BodyPart;
import com.teammoeg.frostedheart.content.climate.player.TemperatureUpdate;
import com.teammoeg.frostedheart.content.waypoint.network.WaypointSyncAllPacket;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import com.teammoeg.frostedheart.mixin.minecraft.temperature.ServerLevelMixin_PlaceExtraSnow;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.SaplingGrowTreeEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.PacketDistributor.PacketTarget;

import java.util.Collection;
import java.util.function.Supplier;

import static com.teammoeg.frostedheart.content.climate.WorldTemperature.SNOW_REACHES_GROUND;

@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClimateCommonEvents {
	@SubscribeEvent
	public static void attachToPlayer(AttachCapabilitiesEvent<Entity> event) {
		// Common capabilities
		
		event.addCapability(new ResourceLocation(FHMain.MODID, "temperature"), FHCapabilities.PLAYER_TEMP.provider());

	}

	@SubscribeEvent
	public static void attachToWorld(AttachCapabilitiesEvent<Level> event) {
		// only attach to dimension with skylight (i.e. overworld)
		if (!event.getObject().dimensionType().hasFixedTime()) {
			event.addCapability(new ResourceLocation(FHMain.MODID, "climate_data"),
					FHCapabilities.CLIMATE_DATA.provider());
		}
	}

	@SubscribeEvent
	public static void attachToItem(AttachCapabilitiesEvent<ItemStack> event) {
		/*
		 * ArmorTempData
		 * amd=ArmorTempData.cacheList.get(event.getObject().getItem()).get(null);
		 * if (amd!=null) {
		 * event.addCapability(new ResourceLocation(FHMain.MODID, "armor_warmth"),new
		 * CurioCapabilityProvider(()->new ArmorTempCurios(amd,event.getObject())));
		 * }
		 */
	}

	@SubscribeEvent
	public static void attachToChunk(AttachCapabilitiesEvent<LevelChunk> event) {
		if (!event.getObject().isEmpty()) {
			Level world = event.getObject().getLevel();
			if (!world.isClientSide) {
				event.addCapability(new ResourceLocation(FHMain.MODID, "chunk_data"),
						FHCapabilities.CHUNK_HEAT.provider());
			}
		}
	}

	@SubscribeEvent
	public static void insulationDataAttr(ItemAttributeModifierEvent event) {
		if (!event.getSlotType().isArmor()/* ||!Mob.getEquipmentSlotForItem(event.getItemStack()).isArmor() */) {
			return;
		}
		ArmorTempData data = ArmorTempData.getData(event.getItemStack(), BodyPart.fromVanilla(event.getSlotType()));

		if (data != null) {
			SlotKey ecs = EquipmentSlotType.fromVanilla(event.getSlotType());
			if (data.getInsulation() != 0)
				event.addModifier(FHAttributes.INSULATION.get(),
						ecs.createAttribute(data.getInsulation(), Operation.ADDITION));
			if (data.getFluidResistance() != 0)
				event.addModifier(FHAttributes.WIND_PROOF.get(),
						ecs.createAttribute(data.getFluidResistance(), Operation.MULTIPLY_TOTAL));
			if (data.getHeatProof() != 0)
				event.addModifier(FHAttributes.HEAT_PROOF.get(),
						ecs.createAttribute(data.getHeatProof(), Operation.MULTIPLY_TOTAL));
		}else {
			Collection<AttributeModifier> attr=event.getItemStack().getItem().getAttributeModifiers(event.getSlotType(),event.getItemStack()).get(Attributes.ARMOR);
			if(!attr.isEmpty()) {
				SlotKey ecs = EquipmentSlotType.fromVanilla(event.getSlotType());
				event.addModifier(FHAttributes.WIND_PROOF.get(),
						ecs.createAttribute(1f, Operation.MULTIPLY_TOTAL));
				event.addModifier(FHAttributes.HEAT_PROOF.get(),
						ecs.createAttribute(.7f, Operation.MULTIPLY_TOTAL));
			}
		}

	}

	/*
	 * @SubscribeEvent
	 * public static void addReloadListeners(AddReloadListenerEvent event) {
	 * event.addListener(FHDataManager.INSTANCE);
	 * }
	 */

	/**
	 * Controls the growth of crops based on the temperature.
	 */
	@SubscribeEvent
	public static void beforeCropGrow(BlockEvent.CropGrowEvent.Pre event) {
		BlockState state = event.getState();
		Block crop = state.getBlock();
		BlockPos pos = event.getPos();
		LevelAccessor level = event.getLevel();
		WorldTemperature.PlantStatus status = WorldTemperature.checkPlantStatus(level, pos, crop);
		if (status.canGrow()) {
			event.setResult(Event.Result.DEFAULT);
		}
		if (status.canSurvive()) {
			if (event.getLevel().getRandom().nextInt(3) == 0) {
				if (state.is(crop) && state != crop.defaultBlockState())
					event.getLevel().setBlock(event.getPos(), crop.defaultBlockState(), 2);
			}
			event.setResult(Event.Result.DENY);
		}
		if (status.willDie()) {
			if (level.getBlockState(pos.below()).is(Blocks.FARMLAND)) {
				level.setBlock(pos.below(), Blocks.DIRT.defaultBlockState(), 2);
			}
			level.setBlock(pos, Blocks.DEAD_BUSH.defaultBlockState(), 2);
			event.setResult(Event.Result.DENY);
		}
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onArmorDamage(LivingHurtEvent event) {
		if (event.getEntity() instanceof Player && (event.getSource().is(DamageTypeTags.IS_FIRE)
				|| !event.getSource().is(DamageTypeTags.BYPASSES_ARMOR))) {
			Player player = (Player) event.getEntity();
			float damage = event.getAmount();
			DamageSource p_234563_1_ = event.getSource();
			if (damage > 0) {
				damage = damage / 8.0F;
				if (p_234563_1_.is(DamageTypeTags.IS_FIRE))// fire damage more
					damage *= 2;
				else if (p_234563_1_.is(DamageTypeTags.IS_EXPLOSION))// explode add a lot
					damage *= 4;
				int amount = (int) damage;
				if (amount != damage)
					amount += player.getRandom().nextDouble() < (damage - amount) ? 1 : 0;
				if (amount <= 0)
					return;
				LazyOptional<PlayerTemperatureData> cap = PlayerTemperatureData.getCapability(player);
				if (cap.isPresent()) {
					RandomSource rs = player.level().random;
					PlayerTemperatureData ptd = cap.resolve().get();
					for (BodyPart part : BodyPart.values()) {
						ItemStackHandler partItem = ptd.getClothesByPart(part);
						float rate = (float) (1-ClothData.sumAttributesPercentage(player.getItemBySlot(part.slot).getAttributeModifiers(part.slot).get(FHAttributes.HEAT_PROOF.get())));
						for (int i = 0; i < partItem.getSlots(); i++) {
							ItemStack itemstack = partItem.getStackInSlot(i);
							if(!itemstack.isEmpty()) {
								if(itemstack.isDamageableItem())
								itemstack.hurtAndBreak(CMath.randomValue(rs, rate * amount), player,
									t -> t.broadcastBreakEvent(part.slot));
								ArmorTempData atd=ArmorTempData.getData(itemstack, part);
								float proof=.7f;
								if(atd!=null)
									proof=atd.getHeatProof();
								rate*=(1-proof);
							}
						}
					}

				}
			}
		}
	}

	@SubscribeEvent
	public static void onServerTick(TickEvent.LevelTickEvent event) {
		if (event.side == LogicalSide.SERVER && event.phase == Phase.START && CTeamDataManager.INSTANCE != null) {
			Level world = event.level;
			if (!world.isClientSide && world instanceof ServerLevel) {
				ServerLevel serverWorld = (ServerLevel) world;

				// Town logic tick
				int i = 0;
				for (TeamDataHolder trd : CTeamDataManager.INSTANCE.getAllData()) {
					if (serverWorld.dimension().equals(trd.getData(FHSpecialDataTypes.GENERATOR_DATA).dimension)) {
						if (serverWorld.getGameTime() % 20 == i % 20) {// Split town calculations to multiple seconds
							if (trd.getTeam().map(t -> t.getOnlineMembers().size()).orElse(0) > 0) {
								trd.getData(FHSpecialDataTypes.TOWN_DATA).tick(serverWorld);
							}
						}
						if (serverWorld.getGameTime() == i + 1000) {
							if (trd.getTeam().map(t -> t.getOnlineMembers().size()).orElse(0) > 0) {
								trd.getData(FHSpecialDataTypes.TOWN_DATA).tickMorning(serverWorld);// execute only once
																									// a day
							}
						}
					}
					i++;
				}

				// Update clock source every second, and check hour data if it needs an update
				if (serverWorld.getGameTime() % 20 == 0) {
					WorldClimate data = WorldClimate.get(serverWorld);
					
					if (data != null) {
						if(FHConfig.SERVER.addInitClimate.get())
							if(!data.isInitialEventAdded()) {
								data.setInitialEventAdded(true);
								if(serverWorld.dimensionTypeRegistration().is(BuiltinDimensionTypes.OVERWORLD)) {
									data.addInitTempEvent(serverWorld);
								}
							}
						data.updateClock(serverWorld);
						data.updateCache(serverWorld);
						data.trimTempEventStream();
					}

				}
			}

		}
	}

	/**
	 * Prevents bonemeal from being used on plants that are not in the right
	 * temperature range.
	 */
	@SubscribeEvent
	public static void onPerformBonemeal(PerformBonemealEvent event) {
		if (event.getLevel() instanceof ServerLevel level) {
			WorldTemperature.PlantStatus status = WorldTemperature.checkPlantStatus(level, event.getPos(),
					event.getState().getBlock());
			if (!status.canFertilize()) {
				event.setCanceled(true);
			}
		}
	}

	@SubscribeEvent
	public static void syncDataToClient(PlayerEvent.PlayerLoggedInEvent event) {
		if (event.getEntity() instanceof ServerPlayer) {
			ServerLevel serverWorld = ((ServerPlayer) event.getEntity()).serverLevel();
			PacketTarget currentPlayer = PacketDistributor.PLAYER.with(() -> (ServerPlayer) event.getEntity());
			FHCapabilities.CLIMATE_DATA.getCapability(serverWorld)
					.ifPresent((cap) -> FHNetwork.INSTANCE.send(currentPlayer, new FHClimatePacket(cap)));

			FHNetwork.INSTANCE.send(currentPlayer, new WaypointSyncAllPacket((ServerPlayer) event.getEntity()));
			// System.out.println("=x-x=");
			// System.out.println(ForgeRegistries.LOOT_MODIFIER_SERIALIZERS.getValue(new
			// ResourceLocation(FHMain.MODID,"add_loot")));
		}
	}

	@SubscribeEvent
	public static void syncDataWhenDimensionChanged(PlayerEvent.PlayerChangedDimensionEvent event) {
		if (event.getEntity() instanceof ServerPlayer) {
			ServerLevel serverWorld = ((ServerPlayer) event.getEntity()).serverLevel();

			FHNetwork.INSTANCE.sendPlayer((ServerPlayer) event.getEntity(),
					new FHClimatePacket(WorldClimate.get(serverWorld)));
		}
	}

	/**
	 * Places extra snow layer on top of existing snow blocks during snowfall.
	 *
	 * Entry point is {@link ServerLevelMixin_PlaceExtraSnow#placeExtraSnow}
	 *
	 * @author AlcatrazEscapee
	 */
	public static void placeExtraSnow(ServerLevel level, ChunkAccess chunk) {
		if (FHConfig.SERVER.enableSnowAccumulationDuringWeather.get()
				&& level.random.nextInt(FHConfig.SERVER.snowAccumulationDifficulty.get()) == 0) {
			int blockX = chunk.getPos().getMinBlockX();
			int blockZ = chunk.getPos().getMinBlockZ();
			BlockPos pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING,
					level.getBlockRandomPos(blockX, 0, blockZ, 15));
			BlockState state = level.getBlockState(pos);
			Biome biome = level.getBiome(pos).value();
			if (level.isRaining() && biome.coldEnoughToSnow(pos)
					&& WorldTemperature.block(level, pos) <= SNOW_REACHES_GROUND
					&& level.getBrightness(LightLayer.BLOCK, pos) < 10 && state.getBlock() == Blocks.SNOW) {
				int layers = state.getValue(BlockStateProperties.LAYERS);
				if (layers < 5) {
					level.setBlockAndUpdate(pos, state.setValue(BlockStateProperties.LAYERS, 1 + layers));
				}

				// Replacing terrain.
				BlockPos belowPos = pos.below();
				BlockState belowState = level.getBlockState(belowPos);
				Supplier<? extends Block> replacement = FHBlocks.SNOWY_TERRAIN_BLOCKS.get(belowState.getBlock());
				if (replacement != null) {
					BlockState state1 = replacement.get().defaultBlockState();
					if (state1.hasProperty(BlockStateProperties.SNOWY)) {
						state1.setValue(BlockStateProperties.SNOWY, true);
					}
					level.setBlockAndUpdate(belowPos, state1);
				} else {
					if (belowState.hasProperty(BlockStateProperties.SNOWY)) {
						belowState.setValue(BlockStateProperties.SNOWY, true);
					}
				}
			}
		}

	}

	@SubscribeEvent
	public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
		FoodTemperatureHandler.onPlayerTick(event);
		ForecastHandler.sendForecastMessages(event);
		TemperatureUpdate.updateTemperature(event);
		TemperatureUpdate.regulateTemperature(event);
	}

	@SubscribeEvent
	public static void startUsingItems(LivingEntityUseItemEvent.Start event) {
		FoodTemperatureHandler.checkFoodBeforeEating(event);
	}

	@SubscribeEvent
	public static void finishUsingItems(LivingEntityUseItemEvent.Finish event) {
		FoodTemperatureHandler.checkFoodAfterEating(event);
	}
	
	@SubscribeEvent
	public static void saplingGrow(SaplingGrowTreeEvent event) {
		BlockPos pos = event.getPos();
		LevelAccessor worldIn = event.getLevel();
		RandomSource rand = event.getRandomSource();
		BlockState sapling = event.getLevel().getBlockState(pos);
		
		if (WorldTemperature.isBlizzardHarming(worldIn, pos)) {
			worldIn.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
			event.setResult(Event.Result.DENY);
		} else if(event.getFeature().is(FHTags.BIG_TREE)) {
			if(!WorldTemperature.canBigTreeGenerate(worldIn, pos, rand)) {
				event.setResult(Event.Result.DENY);
			}
		}else if (!WorldTemperature.canTreeGrow(worldIn, pos, rand))
			event.setResult(Event.Result.DENY);
	}

	@SubscribeEvent
	public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
		if (event.getEntity() instanceof ServerPlayer && !(event.getEntity() instanceof FakePlayer)&&!event.isEndConquered()) {
			ServerLevel serverWorld = ((ServerPlayer) event.getEntity()).serverLevel();
			FHNetwork.INSTANCE.sendPlayer((ServerPlayer) event.getEntity(), new FHClimatePacket(WorldClimate.get(serverWorld)));
			//PlayerTemperatureData.getCapability(event.getEntity()).ifPresent(PlayerTemperatureData::deathResetTemperature);
		}
	}
}
