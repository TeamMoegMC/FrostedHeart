/*
 * Copyright (c) 2021-2024 TeamMoeg
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

import com.teammoeg.chorda.team.CTeamDataManager;
import com.teammoeg.frostedheart.*;
import com.teammoeg.chorda.capability.CurioCapabilityProvider;
import com.teammoeg.frostedheart.bootstrap.common.FHSpecialDataTypes;
import com.teammoeg.chorda.team.TeamDataHolder;
import com.teammoeg.chorda.util.CUtils;
import com.teammoeg.frostedheart.bootstrap.common.FHAttributes;
import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.bootstrap.common.FHCapabilities;
import com.teammoeg.frostedheart.content.climate.ArmorTempCurios;
import com.teammoeg.frostedheart.content.climate.ForecastHandler;
import com.teammoeg.frostedheart.content.climate.WorldClimate;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.data.ArmorTempData;
import com.teammoeg.frostedheart.content.climate.food.FoodTemperatureHandler;
import com.teammoeg.frostedheart.content.climate.network.FHClimatePacket;
import com.teammoeg.frostedheart.content.climate.player.EquipmentSlotType;
import com.teammoeg.frostedheart.content.climate.player.PlayerTemperatureData;
import com.teammoeg.frostedheart.content.climate.player.TemperatureUpdate;
import com.teammoeg.frostedheart.content.climate.recipe.InstallInnerRecipe;
import com.teammoeg.frostedheart.content.research.FHResearch;
import com.teammoeg.frostedheart.content.research.api.ResearchDataAPI;
import com.teammoeg.frostedheart.content.research.network.FHResearchDataSyncPacket;
import com.teammoeg.frostedheart.content.waypoint.network.WaypointSyncAllPacket;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import com.teammoeg.frostedheart.mixin.minecraft.temperature.ServerLevelMixin_PlaceExtraSnow;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.DigDurabilityEnchantment;
import net.minecraft.world.item.enchantment.Enchantments;
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
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.AddReloadListenerEvent;
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
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.PacketDistributor.PacketTarget;

import java.util.function.Supplier;

import static com.teammoeg.frostedheart.content.climate.WorldTemperature.SNOW_TEMPERATURE;

@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClimateCommonEvents {
    @SubscribeEvent
    public static void attachToPlayer(AttachCapabilitiesEvent<Entity> event) {
        //Common capabilities
        event.addCapability(new ResourceLocation(FHMain.MODID, "temperature"), FHCapabilities.PLAYER_TEMP.provider());

    }
    @SubscribeEvent
    public static void attachToWorld(AttachCapabilitiesEvent<Level> event) {
        // only attach to dimension with skylight (i.e. overworld)
        if (!event.getObject().dimensionType().hasFixedTime()) {
            event.addCapability(new ResourceLocation(FHMain.MODID, "climate_data"),FHCapabilities.CLIMATE_DATA.provider());
        }
    }

    @SubscribeEvent
    public static void attachToItem(AttachCapabilitiesEvent<ItemStack> event) {
        ArmorTempData amd=ArmorTempData.cacheList.get(event.getObject().getItem());
        if (amd!=null) {
            event.addCapability(new ResourceLocation(FHMain.MODID, "armor_warmth"),new CurioCapabilityProvider(()->new ArmorTempCurios(amd,event.getObject())));
        }
    }

    @SubscribeEvent
    public static void attachToChunk(AttachCapabilitiesEvent<LevelChunk> event) {
        if (!event.getObject().isEmpty()) {
            Level world = event.getObject().getLevel();
            if (!world.isClientSide) {
                event.addCapability(new ResourceLocation(FHMain.MODID, "chunk_data"), FHCapabilities.CHUNK_HEAT.provider());
            }
        }
    }

    @SubscribeEvent
    public static void insulationDataAttr(ItemAttributeModifierEvent event) {
        ArmorTempData data=ArmorTempData.cacheList.get(event.getItemStack().getItem());

        if(data!=null) {
        	EquipmentSlot es=event.getItemStack().getEquipmentSlot();
        	if(es!=null) {
	        	EquipmentSlotType ecs=EquipmentSlotType.fromVanilla(es);
	        	if(event.getSlotType()==es) {
		        	if(data.getInsulation()!=0)
		        		event.addModifier(FHAttributes.INSULATION.get(), new AttributeModifier(ecs.getSlotUUID(0), ecs.getKey(0), data.getInsulation(), Operation.ADDITION));
		        	if(data.getColdProof()!=0)
		        		event.addModifier(FHAttributes.WIND_PROOF.get(), new AttributeModifier(ecs.getSlotUUID(0), ecs.getKey(0), data.getColdProof(), Operation.ADDITION));
		        	if(data.getHeatProof()!=0)
		        		event.addModifier(FHAttributes.HEAT_PROOF.get(), new AttributeModifier(ecs.getSlotUUID(0), ecs.getKey(0), data.getHeatProof(), Operation.ADDITION));
	        	}
        	}
        }
    }


   /* @SubscribeEvent
    public static void addReloadListeners(AddReloadListenerEvent event) {
        event.addListener(FHDataManager.INSTANCE);
    }*/

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
        if (event.getEntity() instanceof Player && (event.getSource().is(DamageTypeTags.IS_FIRE) || !event.getSource().is(DamageTypeTags.BYPASSES_ARMOR))) {
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
                for (ItemStack itemstack : player.getArmorSlots()) {
                    if (itemstack.isEmpty())
                        continue;
                    CompoundTag cn = itemstack.getTag();
                    if (cn == null)
                        continue;
                    String inner = cn.getString("inner_cover");
                    if (inner.isEmpty())
                        continue;
                    if (cn.getBoolean("inner_bounded")) {
                        int dmg = cn.getInt("inner_damage");
                        if (dmg < itemstack.getDamageValue()) {
                            dmg = itemstack.getDamageValue();
                        }
                        dmg += amount;
                        if (dmg >= itemstack.getMaxDamage()) {
                            cn.remove("inner_cover");
                            cn.remove("inner_cover_tag");
                            cn.remove("inner_bounded");
                            cn.remove("inner_damage");
                            player.broadcastBreakEvent(Mob.getEquipmentSlotForItem(itemstack));
                        } else cn.putInt("inner_damage", dmg);
                        continue;
                    }
                    CompoundTag cnbt = cn.getCompound("inner_cover_tag");
                    int i = CUtils.getEnchantmentLevel(Enchantments.UNBREAKING, cnbt);
                    int j = 0;
                    if (i > 0)
                        for (int k = 0; k < amount; ++k) {
                            if (DigDurabilityEnchantment.shouldIgnoreDurabilityDrop(itemstack, i, player.getRandom())) {
                                ++j;
                            }
                        }
                    amount -= j;
                    if (amount <= 0)
                        continue;
                    int crdmg = cnbt.getInt("Damage");
                    crdmg += amount;
                    InstallInnerRecipe ri = InstallInnerRecipe.recipeList.get(new ResourceLocation(inner));

                    if (ri != null && ri.getDurability() <= crdmg) {// damaged
                        cn.remove("inner_cover");
                        cn.remove("inner_cover_tag");
                        cn.remove("inner_bounded");
                        player.broadcastBreakEvent(Mob.getEquipmentSlotForItem(itemstack));
                    } else {
                        cnbt.putInt("Damage", crdmg);
                        cn.put("inner_cover_tag", cnbt);
                    }
                }

            }
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.LevelTickEvent event) {
        if (event.side == LogicalSide.SERVER && event.phase == Phase.START&& CTeamDataManager.INSTANCE!=null) {
            Level world = event.level;
            if (!world.isClientSide && world instanceof ServerLevel) {
                ServerLevel serverWorld = (ServerLevel) world;

                // Town logic tick
                int i = 0;
                for (TeamDataHolder trd : CTeamDataManager.INSTANCE.getAllData()) {
                    if (serverWorld.dimension().equals(trd.getData(FHSpecialDataTypes.GENERATOR_DATA).dimension)) {
                        if (serverWorld.getGameTime() % 20 == i % 20) {//Split town calculations to multiple seconds
                            if (trd.getTeam().map(t -> t.getOnlineMembers().size()).orElse(0) > 0) {
                                trd.getData(FHSpecialDataTypes.TOWN_DATA).tick(serverWorld);
                            }
                        }
                        if(serverWorld.getGameTime() == i + 1000) {
                            if (trd.getTeam().map(t -> t.getOnlineMembers().size()).orElse(0) > 0) {
                                trd.getData(FHSpecialDataTypes.TOWN_DATA).tickMorning(serverWorld);//execute only once a day
                            }
                        }
                    }
                    i++;
                }

                // Update clock source every second, and check hour data if it needs an update
                if (serverWorld.getGameTime() % 20 == 0) {
                    WorldClimate data = WorldClimate.get(serverWorld);
                    if(data!=null) {
	                    data.updateClock(serverWorld);
	                    data.updateCache(serverWorld);
	                    data.trimTempEventStream();
                    }

                }
            }

        }
    }

    /**
     * Prevents bonemeal from being used on plants that are not in the right temperature range.
     */
    @SubscribeEvent
    public static void onPerformBonemeal(PerformBonemealEvent event) {
        if (event.getLevel() instanceof ServerLevel level) {
            WorldTemperature.PlantStatus status = WorldTemperature.checkPlantStatus(level, event.getPos(), event.getState().getBlock());
            if (!status.canFertilize()) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void syncDataToClient(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer) {
            ServerLevel serverWorld = ((ServerPlayer) event.getEntity()).serverLevel();
            PacketTarget currentPlayer=PacketDistributor.PLAYER.with(() -> (ServerPlayer) event.getEntity());
            FHResearch.sendSyncPacket(currentPlayer);
            FHNetwork.send(currentPlayer,new FHResearchDataSyncPacket(ResearchDataAPI.getData((ServerPlayer) event.getEntity()).get()));
            FHCapabilities.CLIMATE_DATA.getCapability(serverWorld).ifPresent((cap) -> FHNetwork.send(currentPlayer,new FHClimatePacket(cap)));

            FHNetwork.send(currentPlayer, new WaypointSyncAllPacket((ServerPlayer) event.getEntity()));
            //System.out.println("=x-x=");
            //System.out.println(ForgeRegistries.LOOT_MODIFIER_SERIALIZERS.getValue(new ResourceLocation(FHMain.MODID,"add_loot")));
        }
    }

    @SubscribeEvent
    public static void syncDataWhenDimensionChanged(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer) {
            ServerLevel serverWorld = ((ServerPlayer) event.getEntity()).serverLevel();

            FHNetwork.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) event.getEntity()),
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
        if (FHConfig.SERVER.enableSnowAccumulationDuringWeather.get() && level.random.nextInt(FHConfig.SERVER.snowAccumulationDifficulty.get()) == 0) {
            int blockX = chunk.getPos().getMinBlockX();
            int blockZ = chunk.getPos().getMinBlockZ();
            BlockPos pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, level.getBlockRandomPos(blockX, 0, blockZ, 15));
            BlockState state = level.getBlockState(pos);
            Biome biome = level.getBiome(pos).value();
            if (level.isRaining() && biome.coldEnoughToSnow(pos) && WorldTemperature.block(level, pos) <= SNOW_TEMPERATURE && level.getBrightness(LightLayer.BLOCK, pos) < 10 && state.getBlock() == Blocks.SNOW) {
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
        BlockPos pos=event.getPos();
        LevelAccessor worldIn=event.getLevel();
        RandomSource rand=event.getRandomSource();
        BlockState sapling=event.getLevel().getBlockState(pos);
        if (WorldTemperature.isBlizzardHarming(worldIn, pos)) {
            worldIn.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
            event.setResult(Event.Result.DENY);
        } else if (!WorldTemperature.canTreeGrow(worldIn, pos, rand))
            event.setResult(Event.Result.DENY);
    }

    @SubscribeEvent
    public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer && !(event.getEntity() instanceof FakePlayer)) {
            ServerLevel serverWorld = ((ServerPlayer) event.getEntity()).serverLevel();
            FHNetwork.sendPlayer( (ServerPlayer) event.getEntity(),
                    new FHClimatePacket(WorldClimate.get(serverWorld)));
            PlayerTemperatureData.getCapability(event.getEntity()).ifPresent(PlayerTemperatureData::reset);
        }
    }
}
