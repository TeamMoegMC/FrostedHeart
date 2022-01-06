/*
 * Copyright (c) 2021 TeamMoeg
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
 */

package com.teammoeg.frostedheart.events;

import javax.annotation.Nonnull;

import com.mojang.brigadier.CommandDispatcher;
import com.teammoeg.frostedheart.FHConfig;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.climate.ClimateData;
import com.teammoeg.frostedheart.climate.ITempAdjustFood;
import com.teammoeg.frostedheart.climate.TemperatureCore;
import com.teammoeg.frostedheart.climate.WorldClimate;
import com.teammoeg.frostedheart.climate.chunkdata.ChunkData;
import com.teammoeg.frostedheart.climate.chunkdata.ChunkDataCapabilityProvider;
import com.teammoeg.frostedheart.command.AddTempCommand;
import com.teammoeg.frostedheart.command.ResearchCommand;
import com.teammoeg.frostedheart.content.agriculture.FHBerryBushBlock;
import com.teammoeg.frostedheart.content.agriculture.FHCropBlock;
import com.teammoeg.frostedheart.content.recipes.RecipeInner;
import com.teammoeg.frostedheart.data.FHDataManager;
import com.teammoeg.frostedheart.data.FHDataReloadManager;
import com.teammoeg.frostedheart.network.FHClimatePacket;
import com.teammoeg.frostedheart.network.FHDatapackSyncPacket;
import com.teammoeg.frostedheart.network.FHResearchDataSyncPacket;
import com.teammoeg.frostedheart.network.FHResearchRegistrtySyncPacket;
import com.teammoeg.frostedheart.network.PacketHandler;
import com.teammoeg.frostedheart.resources.FHRecipeCachingReloadListener;
import com.teammoeg.frostedheart.resources.FHRecipeReloadListener;
import com.teammoeg.frostedheart.util.FHDamageSources;
import com.teammoeg.frostedheart.util.FHNBT;
import com.teammoeg.frostedheart.util.FHUtils;
import com.teammoeg.frostedheart.world.FHFeatures;

import blusunrize.immersiveengineering.api.multiblocks.MultiblockHandler.MultiblockFormEvent;
import blusunrize.immersiveengineering.common.blocks.IEBlocks;
import dev.ftb.mods.ftbteams.FTBTeamsAPI;
import net.minecraft.block.Block;
import net.minecraft.block.IGrowable;
import net.minecraft.block.SaplingBlock;
import net.minecraft.command.CommandSource;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.enchantment.UnbreakingEnchantment;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.item.ExperienceOrbEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.resources.DataPackRegistries;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.GenerationStage;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.BonemealEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerXpEvent.PickupXp;
import net.minecraftforge.event.world.BiomeLoadingEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerStartedEvent;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ForgeEvents {
	@SubscribeEvent
	public void onServerStarted(FMLServerStartedEvent event) {

	}

	@SubscribeEvent
	public static void onServerTick(TickEvent.WorldTickEvent event) {

	}

	@SubscribeEvent
	public static void onIEMultiBlockForm(MultiblockFormEvent event) {
		if (event.getPlayer() instanceof FakePlayer) {
			event.setCanceled(true);
			return;
		}
		if (!FHDataManager.testMultiBlock(event.getMultiblock().getUniqueName(), event.getPlayer()))
			event.setCanceled(true);
	}

	@SubscribeEvent
	public static void playerXPPickUp(PickupXp event) {
		PlayerEntity player = event.getPlayer();
		for (ItemStack stack : player.getArmorInventoryList()) {
			if (!stack.isEmpty()) {
				CompoundNBT cn = stack.getTag();
				if (cn == null)
					continue;
				String inner = cn.getString("inner_cover");
				if (inner.isEmpty() || cn.getBoolean("inner_bounded"))
					continue;
				CompoundNBT cnbt = cn.getCompound("inner_cover_tag");
				int crdmg = cnbt.getInt("Damage");
				if(crdmg>0&&FHUtils.getEnchantmentLevel(Enchantments.MENDING,cnbt)>0) {
					event.setCanceled(true);
					ExperienceOrbEntity orb = event.getOrb();
					player.xpCooldown = 2;
					player.onItemPickup(orb, 1);
					
					int toRepair = Math.min(orb.xpValue * 2,crdmg);
					orb.xpValue -= toRepair / 2;
					crdmg=crdmg - toRepair;
					cnbt.putInt("Damage", crdmg);
					cn.put("inner_cover_tag", cnbt);
					if (orb.xpValue > 0) {
						player.giveExperiencePoints(orb.xpValue);
					}
					orb.remove();
					return;
				}
			}
		}
	}
	//not allow repair
	@SubscribeEvent(receiveCanceled = true, priority = EventPriority.LOWEST)
	public static void onItemRepair(AnvilUpdateEvent event) {
		if(event.getLeft().hasTag()) {
			if(event.getLeft().getTag().getBoolean("inner_bounded"))
				event.setCanceled(true);
		}
	}
	@SubscribeEvent(receiveCanceled = true, priority = EventPriority.LOWEST)
	public static void onArmorDamage(LivingHurtEvent event) {
		if (event.getEntityLiving() instanceof PlayerEntity && (event.getSource().isFireDamage()||!event.getSource().isUnblockable())) {
			PlayerEntity player = (PlayerEntity) event.getEntityLiving();
			float p_234563_2_ = event.getAmount();
			DamageSource p_234563_1_ = event.getSource();
			if (p_234563_2_ > 0) {
				p_234563_2_ = p_234563_2_ / 4.0F;
				if (p_234563_1_.isFireDamage())// fire damage more
					p_234563_2_ *= 2;
				else if (p_234563_1_.isExplosion())// explode add a lot
					p_234563_2_ *= 4;
				int amount = (int) p_234563_2_;
				if (amount != p_234563_2_)
					amount += player.getRNG().nextDouble() < (p_234563_2_ - amount) ? 1 : 0;
				if (amount <= 0)
					return;
				for (ItemStack itemstack : player.getArmorInventoryList()) {
					if (itemstack.isEmpty())
						continue;
					CompoundNBT cn = itemstack.getTag();
					if (cn == null)
						continue;
					String inner = cn.getString("inner_cover");
					if (inner.isEmpty())
						continue;
					if(cn.getBoolean("inner_bounded")) {
						int dmg=cn.getInt("inner_damage");
						if(dmg<itemstack.getDamage()) {
							dmg=itemstack.getDamage();
						}
						dmg+=amount;
						if(dmg>=itemstack.getMaxDamage()) {
							cn.remove("inner_cover");
							cn.remove("inner_cover_tag");
							cn.remove("inner_bounded");
							cn.remove("inner_damage");
							player.sendBreakAnimation(MobEntity.getSlotForItemStack(itemstack));
						}else cn.putInt("inner_damage",dmg);
						continue;
					}
					CompoundNBT cnbt = cn.getCompound("inner_cover_tag");
					int i = FHUtils.getEnchantmentLevel(Enchantments.UNBREAKING, cnbt);
					int j = 0;
					if (i > 0)
						for (int k = 0; i > 0 && k < amount; ++k) {
							if (UnbreakingEnchantment.negateDamage(itemstack, i, player.getRNG())) {
								++j;
							}
						}
					amount -= j;
					if (amount <= 0)
						continue;
					int crdmg = cnbt.getInt("Damage");
					crdmg += amount;
					RecipeInner ri = RecipeInner.recipeList.get(new ResourceLocation(inner));

					if (ri != null && ri.getDurability() <= crdmg) {// damaged
						cn.remove("inner_cover");
						cn.remove("inner_cover_tag");
						cn.remove("inner_bounded");
						player.sendBreakAnimation(MobEntity.getSlotForItemStack(itemstack));
					} else {
						cnbt.putInt("Damage", crdmg);
						cn.put("inner_cover_tag", cnbt);
					}
				}

			}
		}
	}

	@SubscribeEvent
	public static void addReloadListeners(AddReloadListenerEvent event) {
		DataPackRegistries dataPackRegistries = event.getDataPackRegistries();
		// IReloadableResourceManager resourceManager = (IReloadableResourceManager)
		// dataPackRegistries.getResourceManager();
		event.addListener(new FHRecipeReloadListener(dataPackRegistries));
		event.addListener(FHDataReloadManager.INSTANCE);
//            resourceManager.addReloadListener(ChunkCacheInvalidationReloaderListener.INSTANCE);
	}

	@SubscribeEvent
	public static void addReloadListenersLowest(AddReloadListenerEvent event) {
		DataPackRegistries dataPackRegistries = event.getDataPackRegistries();
		event.addListener(new FHRecipeCachingReloadListener(dataPackRegistries));
	}

	@SubscribeEvent
	public static void onAttachCapabilitiesWorld(AttachCapabilitiesEvent<World> event) {
		if (!event.getObject().isRemote) {
			World world = event.getObject();
			if (!world.isRemote) {
				if (!event.getCapabilities().containsKey(ClimateData.ID))
					event.addCapability(ClimateData.ID, new ClimateData());
			}

		}
	}

	@SubscribeEvent
	public static void onAttachCapabilitiesChunk(AttachCapabilitiesEvent<Chunk> event) {
		if (!event.getObject().isEmpty()) {
			World world = event.getObject().getWorld();
			ChunkPos chunkPos = event.getObject().getPos();
			ChunkData data;
			if (!world.isRemote) {
				if (!event.getCapabilities().containsKey(ChunkDataCapabilityProvider.KEY))
					event.addCapability(ChunkDataCapabilityProvider.KEY, new ChunkData(chunkPos));
			}

		}
	}

	@SubscribeEvent
	public static void addOreGenFeatures(BiomeLoadingEvent event) {
		if (event.getName() != null) {
			if (event.getCategory() != Biome.Category.NETHER && event.getCategory() != Biome.Category.THEEND) {
				if (event.getCategory() == Biome.Category.RIVER || event.getCategory() == Biome.Category.BEACH) {
					event.getGeneration().withFeature(GenerationStage.Decoration.UNDERGROUND_ORES,
							FHFeatures.copper_gravel);
				}
				for (ConfiguredFeature feature : FHFeatures.FH_ORES)
					event.getGeneration().withFeature(GenerationStage.Decoration.UNDERGROUND_ORES, feature);
			}
		}
	}

	@SubscribeEvent
	public static void beforeCropGrow(BlockEvent.CropGrowEvent.Pre event) {
		Block growBlock = event.getState().getBlock();
		float temp = ChunkData.getTemperature(event.getWorld(), event.getPos());
		if (growBlock instanceof FHCropBlock) {
			event.setResult(Event.Result.DEFAULT);
		} else if (growBlock.matchesBlock(IEBlocks.Misc.hempPlant)) {
			if (temp < WorldClimate.HEMP_GROW_TEMPERATURE) {
				if (event.getWorld().getRandom().nextInt(3) == 0) {
					event.getWorld().setBlockState(event.getPos(), growBlock.getDefaultState(), 2);
				}
				event.setResult(Event.Result.DENY);
			}
		} else {
			if (temp < WorldClimate.VANILLA_PLANT_GROW_TEMPERATURE) {
				// Set back to default state, might not be necessary
				if (event.getWorld().getBlockState(event.getPos()) != growBlock.getDefaultState()
						&& event.getWorld().getRandom().nextInt(3) == 0) {
					event.getWorld().setBlockState(event.getPos(), growBlock.getDefaultState(), 2);
				}
				event.setResult(Event.Result.DENY);
			}
		}
	}

	@SubscribeEvent
	public static void onUseBoneMeal(BonemealEvent event) {
		if (event.getPlayer() instanceof ServerPlayerEntity) {
			PlayerEntity player = event.getPlayer();
			Block growBlock = event.getBlock().getBlock();
			float temp = ChunkData.getTemperature(event.getWorld(), event.getPos());
			if (growBlock instanceof FHCropBlock) {
				int growTemp = ((FHCropBlock) growBlock).getGrowTemperature();
				if (temp < growTemp) {
					event.setCanceled(true);
					player.sendStatusMessage(
							new TranslationTextComponent("message.frostedheart.crop_no_bonemeal", growTemp), false);
				}
			} else if (growBlock instanceof FHBerryBushBlock) {
				int growTemp = ((FHBerryBushBlock) growBlock).getGrowTemperature();
				if (temp < growTemp) {
					event.setCanceled(true);
					player.sendStatusMessage(
							new TranslationTextComponent("message.frostedheart.crop_no_bonemeal", growTemp), false);
				}
			} else if (growBlock.matchesBlock(IEBlocks.Misc.hempPlant)) {
				if (temp < WorldClimate.HEMP_GROW_TEMPERATURE) {
					event.setCanceled(true);
					player.sendStatusMessage(new TranslationTextComponent("message.frostedheart.crop_no_bonemeal",
							WorldClimate.HEMP_GROW_TEMPERATURE), false);
				}
			} else if (temp < WorldClimate.VANILLA_PLANT_GROW_TEMPERATURE) {
				event.setCanceled(true);
				player.sendStatusMessage(new TranslationTextComponent("message.frostedheart.crop_no_bonemeal",
						WorldClimate.VANILLA_PLANT_GROW_TEMPERATURE), false);
			}
		}
	}

	// TODO create grow temperature mappings for every plant in the modpack
	@SubscribeEvent
	public static void onEntityPlaceBlock(BlockEvent.EntityPlaceEvent event) {
		if (event.getEntity() instanceof ServerPlayerEntity) {
			ServerPlayerEntity player = (ServerPlayerEntity) event.getEntity();
			Block growBlock = event.getPlacedBlock().getBlock();
			float temp = ChunkData.getTemperature(event.getWorld(), event.getPos());
			if (growBlock instanceof IGrowable) {
				if (growBlock instanceof SaplingBlock) {
					// TODO: allow planting trees now, maybe i will add some restrictions in the
					// future
				} else if (growBlock instanceof FHCropBlock) {
					int growTemp = ((FHCropBlock) growBlock).getGrowTemperature();
					if (temp < growTemp) {
						event.setCanceled(true);
						player.sendStatusMessage(
								new TranslationTextComponent("message.frostedheart.crop_not_growable", growTemp),
								false);
					}
				} else if (growBlock instanceof FHBerryBushBlock) {
					int growTemp = ((FHBerryBushBlock) growBlock).getGrowTemperature();
					if (temp < growTemp) {
						event.setCanceled(true);
						player.sendStatusMessage(
								new TranslationTextComponent("message.frostedheart.crop_not_growable", growTemp),
								false);
					}
				} else if (growBlock.matchesBlock(IEBlocks.Misc.hempPlant)) {
					if (temp < WorldClimate.HEMP_GROW_TEMPERATURE) {
						event.setCanceled(true);
						player.sendStatusMessage(new TranslationTextComponent("message.frostedheart.crop_not_growable",
								WorldClimate.HEMP_GROW_TEMPERATURE), false);
					}
				} else if (temp < WorldClimate.VANILLA_PLANT_GROW_TEMPERATURE) {
					event.setCanceled(true);
					player.sendStatusMessage(new TranslationTextComponent("message.frostedheart.crop_not_growable",
							WorldClimate.VANILLA_PLANT_GROW_TEMPERATURE), false);
				}
			}
		}
	}

	@SubscribeEvent
	public static void addManualToPlayer(@Nonnull PlayerEvent.PlayerLoggedInEvent event) {
		CompoundNBT nbt = event.getPlayer().getPersistentData();
		CompoundNBT persistent;

		if (nbt.contains(PlayerEntity.PERSISTED_NBT_TAG)) {
			persistent = nbt.getCompound(PlayerEntity.PERSISTED_NBT_TAG);
		} else {
			nbt.put(PlayerEntity.PERSISTED_NBT_TAG, (persistent = new CompoundNBT()));
		}
		if (!persistent.contains(FHNBT.FIRST_LOGIN_GIVE_MANUAL)) {
			persistent.putBoolean(FHNBT.FIRST_LOGIN_GIVE_MANUAL, false);
			event.getPlayer().inventory.addItemStackToInventory(
					new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("ftbquests", "book"))));
			event.getPlayer().inventory.armorInventory.set(3, FHNBT.ArmorNBT(new ItemStack(Items.IRON_HELMET)
					.setDisplayName(new TranslationTextComponent("itemname.frostedheart.start_head"))));
			event.getPlayer().inventory.armorInventory.set(2, FHNBT.ArmorNBT(new ItemStack(Items.IRON_CHESTPLATE)
					.setDisplayName(new TranslationTextComponent("itemname.frostedheart.start_chest"))));
			event.getPlayer().inventory.armorInventory.set(1, FHNBT.ArmorNBT(new ItemStack(Items.IRON_LEGGINGS)
					.setDisplayName(new TranslationTextComponent("itemname.frostedheart.start_leg"))));
			event.getPlayer().inventory.armorInventory.set(0, FHNBT.ArmorNBT(new ItemStack(Items.IRON_BOOTS)
					.setDisplayName(new TranslationTextComponent("itemname.frostedheart.start_foot"))));

			ItemStack breads = new ItemStack(Items.BREAD);
			breads.setCount(16);
			event.getPlayer().inventory.addItemStackToInventory(breads);
		}
	}

	@SubscribeEvent
	public static void syncDataToClient(PlayerEvent.PlayerLoggedInEvent event) {
		if (event.getEntity() instanceof ServerPlayerEntity) {
			ServerWorld serverWorld = ((ServerPlayerEntity) event.getPlayer()).getServerWorld();
			PacketHandler.send(PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) event.getPlayer()),
					new FHDatapackSyncPacket());
			PacketHandler.send(PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) event.getPlayer()),
					new FHResearchRegistrtySyncPacket());
			PacketHandler.send(PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) event.getPlayer()),
					new FHResearchDataSyncPacket(
							FTBTeamsAPI.getPlayerTeam((ServerPlayerEntity) event.getPlayer()).getId()));

			serverWorld.getCapability(ClimateData.CAPABILITY).ifPresent((cap) -> {
				PacketHandler.send(PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) event.getPlayer()),
						new FHClimatePacket(cap));
			});
		}
	}

	@SubscribeEvent
	public static void setKeepInventory(FMLServerStartedEvent event) {
		if (FHConfig.SERVER.alwaysKeepInventory.get()) {
			for (ServerWorld world : event.getServer().getWorlds()) {
				world.getGameRules().get(GameRules.KEEP_INVENTORY).set(true, event.getServer());
			}
		}
	}

	@SubscribeEvent
	public static void punishEatingRawMeat(LivingEntityUseItemEvent.Finish event) {
		if (event.getEntityLiving() != null && !event.getEntityLiving().world.isRemote
				&& event.getEntityLiving() instanceof ServerPlayerEntity
				&& event.getItem().getItem().getTags().contains(FHMain.rl("raw_food"))) {
			ServerPlayerEntity player = (ServerPlayerEntity) event.getEntityLiving();
			player.addPotionEffect(new EffectInstance(Effects.POISON, 400, 1));
			player.addPotionEffect(new EffectInstance(Effects.NAUSEA, 400, 1));
			if (ModList.get().isLoaded("diet") && player.getServer() != null) {
				player.getServer().getCommandManager().handleCommand(player.getCommandSource(),
						"/diet subtract @s proteins 0.01");
			}
			player.sendStatusMessage(new TranslationTextComponent("message.frostedheart.eaten_poisonous_food"), false);
		}
	}

	@SubscribeEvent
	public static void eatingFood(LivingEntityUseItemEvent.Finish event) {
		if (event.getEntityLiving() != null && !event.getEntityLiving().world.isRemote
				&& event.getEntityLiving() instanceof ServerPlayerEntity) {
			ItemStack is = event.getItem();
			Item it = event.getItem().getItem();
			ITempAdjustFood adj = null;
			// System.out.println(it.getRegistryName());
			double tspeed = FHConfig.SERVER.tempSpeed.get();
			if (it instanceof ITempAdjustFood) {
				adj = (ITempAdjustFood) it;
			} else {
				adj = FHDataManager.getFood(is);
			}
			if (adj != null) {
				float current = TemperatureCore.getBodyTemperature((ServerPlayerEntity) event.getEntityLiving());
				float max = adj.getMaxTemp(event.getItem());
				float min = adj.getMinTemp(event.getItem());
				float heat = adj.getHeat(event.getItem());
				if (heat > 1) {
					event.getEntityLiving().attackEntityFrom(FHDamageSources.HYPERTHERMIA_INSTANT, (heat) * 2);
				} else if (heat < -1)
					event.getEntityLiving().attackEntityFrom(FHDamageSources.HYPOTHERMIA_INSTANT, (heat) * 2);
				if (heat > 0) {
					if (current >= max)
						return;
					current += heat * tspeed;
					if (current > max)
						current = max;
				} else {
					if (current <= min)
						return;
					current += heat * tspeed;
					if (current <= min)
						return;
				}
				TemperatureCore.setBodyTemperature((ServerPlayerEntity) event.getEntityLiving(), current);
			}
		}
	}

	@SubscribeEvent
	public static void removeVanillaVillages(WorldEvent.CreateSpawnPosition event) {
		if (event.getWorld() instanceof ServerWorld) {
			ServerWorld serverWorld = (ServerWorld) event.getWorld();
			try {
				serverWorld.getChunkProvider().generator.func_235957_b_().func_236195_a_().keySet()
						.remove(Structure.VILLAGE);
			} catch (UnsupportedOperationException e) {
			}
		}
	}

	@SubscribeEvent
	public static void removeVanillaVillages(WorldEvent.Load event) {
		if (event.getWorld() instanceof ServerWorld) {
			ServerWorld serverWorld = (ServerWorld) event.getWorld();
			try {
				serverWorld.getChunkProvider().generator.func_235957_b_().func_236195_a_().keySet()
						.remove(Structure.VILLAGE);
			} catch (UnsupportedOperationException e) {
			}
		}
	}

	@SubscribeEvent
	public static void onRegisterCommands(RegisterCommandsEvent event) {
		CommandDispatcher<CommandSource> dispatcher = event.getDispatcher();
		AddTempCommand.register(dispatcher);
		ResearchCommand.register(dispatcher);
	}

	public static void attachWorldCapabilities(AttachCapabilitiesEvent<World> event) {
		event.addCapability(ClimateData.ID, new ClimateData());
	}
}
